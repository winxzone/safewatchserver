from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
from transformers import pipeline, AutoTokenizer, AutoModelForSequenceClassification
import torch
import requests

app = FastAPI()

# Загрузка модели и токенизатора
model_name = "MaxKazak/ruBert-base-russian-emotion-detection"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(model_name)

# Создаем pipeline, но с нашей моделью и токенизатором
emotion_model = pipeline(
    "text-classification",
    model=model,
    tokenizer=tokenizer,
    return_all_scores=True  # для получения вероятностей по всем классам
)

class EmotionRequest(BaseModel):
    messages: List[str]

class EmotionResult(BaseModel):
    emotion: str
    confidence: float


@app.post("/analyze")
def analyze_emotion(req: EmotionRequest):
    if not req.messages:
        return {"emotion": "unknown", "confidence": 0.0}

    try:
        # Анализируем каждое сообщение, получая распределение по классам
        batch_results = emotion_model(req.messages)

        # Усредним вероятности по всем сообщениям
        cumulative_scores = {}

        for message_scores in batch_results:
            for score_obj in message_scores:
                label = score_obj['label']
                score = score_obj['score']
                cumulative_scores[label] = cumulative_scores.get(label, 0) + score

        # Средние значения вероятностей по всем сообщениям
        for label in cumulative_scores:
            cumulative_scores[label] /= len(req.messages)

        # Определяем эмоцию с максимальной средней уверенностью
        best_label = max(cumulative_scores, key=cumulative_scores.get)
        best_score = cumulative_scores[best_label]

        return {"emotion": best_label.lower(), "confidence": best_score}

    except Exception as e:
        print(f"Emotion analysis failed: {e}")
        return {"emotion": "unknown", "confidence": 0.0}

@app.post("/analyze-individual", response_model=List[EmotionResult])
def analyze_individual_emotions(req: EmotionRequest):
    if not req.messages:
        return []

    try:
        # Анализируем каждое сообщение
        batch_results = emotion_model(req.messages)
        
        individual_results = []
        
        for message_scores in batch_results:
            # Находим лучший результат для этого сообщения
            best_score = max(message_scores, key=lambda x: x['score'])
            
            individual_results.append({
                "emotion": best_score['label'].lower(),
                "confidence": best_score['score']
            })
        
        return individual_results

    except Exception as e:
        print(f"Individual emotion analysis failed: {e}")
        # Возвращаем unknown для каждого сообщения
        return [{"emotion": "unknown", "confidence": 0.0} for _ in req.messages]


# uvicorn emotion_api:app --host 0.0.0.0 --port 8000