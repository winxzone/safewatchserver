from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import pipeline
from typing import List

app = FastAPI()

# Загружаем модель
emotion_model = pipeline(
    "text-classification",
    model="cointegrated/rubert-tiny2-cedr-emotion-detection",
    tokenizer="cointegrated/rubert-tiny2-cedr-emotion-detection"
)

class EmotionRequest(BaseModel):
    messages: List[str]

@app.post("/analyze")
def analyze_emotion(req: EmotionRequest):
    if not req.messages:
        # Вернуть безопасный ответ
        return {"emotion": "unknown", "confidence": 0.0}

    try:
        results = emotion_model(req.messages)

        scores = {}
        for res in results:
            label = res['label']
            scores[label] = scores.get(label, 0) + res['score']

        for label in scores:
            scores[label] /= len(results)

        best_label = max(scores, key=scores.get)
        best_score = scores[best_label]

        return {"emotion": best_label, "confidence": best_score}

    except Exception as e:
        # Логировать и вернуть безопасный ответ
        print(f"Emotion analysis failed: {e}")
        return {"emotion": "unknown", "confidence": 0.0}



# uvicorn emotion_api:app --host 0.0.0.0 --port 8000