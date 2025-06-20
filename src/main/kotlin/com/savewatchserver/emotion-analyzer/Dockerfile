FROM python:3.13.3

WORKDIR /app

COPY . .

RUN pip install --upgrade pip
RUN pip install -r requirements.txt

EXPOSE 8000

CMD ["uvicorn", "emotion_api:app", "--host", "0.0.0.0", "--port", "8000"]
