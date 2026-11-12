FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY evaluate.py .
ENTRYPOINT ["python", "evaluate.py"]
CMD ["/data/dataset.jsonl", "/data/report"]
