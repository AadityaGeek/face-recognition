# Use an official Python image (TensorFlow supports up to 3.11/3.12)
FROM python:3.11-slim

# Set working directory
WORKDIR /app

# Install system dependencies (for deepface, opencv, etc.)
RUN apt-get update && apt-get install -y \
    build-essential \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements first (for caching)
COPY requirements.txt .

# Install Python dependencies
RUN pip install --upgrade pip \
    && pip install -r requirements.txt

# Copy project files
COPY . .

# Expose port (Render will map $PORT)
EXPOSE 8000

# Start command (Render will inject $PORT)
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
