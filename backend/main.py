import os
from dotenv import load_dotenv

# Load environment variables from .env file before app initialization
load_dotenv()

# Force TensorFlow to run on CPU & suppress verbose log messages
os.environ["CUDA_VISIBLE_DEVICES"] = "-1"
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes import register, verify

# Read environment mode
APP_ENV = os.getenv("APP_ENV")

# Initialize FastAPI application
app = FastAPI(
    title="Face Recognition & Verification Backend",
    description="FastAPI asynchronous backend service for AI-powered face registration, real-time motion-based liveness verification, and biometric face matching using DeepFace (Facenet) and MongoDB.",
    version="1.0.0",

    docs_url="/docs" if APP_ENV == "development" else None,
    redoc_url="/redoc" if APP_ENV == "development" else None,
    openapi_url="/openapi.json" if APP_ENV == "development" else None
)

# Configure Cross-Origin Resource Sharing (CORS)
allowed_origins_env = os.getenv("ALLOWED_ORIGINS")
if allowed_origins_env:
    origins = [origin.strip() for origin in allowed_origins_env.split(",")]
else:
    origins = [
        "https://face-recognition-app-self.vercel.app",
        "http://localhost:5173",
        "*"
    ]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- System API Routes ---

@app.get(
    "/",
    summary="Root Status",
    description="Returns a simple status message confirming that the backend API is up and running.",
    tags=["System"]
)
def home():
    return {"message": "Backend is running!"}

@app.get(
    "/health",
    summary="Health Check",
    description="Health check endpoint used by uptime monitors and deployment environment checks.",
    tags=["System"]
)
def health():
    return {"status": "ok"}

# --- Include Module Routers ---
app.include_router(register.router)
app.include_router(verify.router)

# --- Startup Event: Warm up DeepFace Model ---
@app.on_event("startup")
async def warmup_deepface_model():
    """
    Pre-loads and warms up the DeepFace Facenet model weights during application startup
    to prevent latency spikes on the first API user request.
    """
    try:
        import numpy as np
        from deepface import DeepFace
        print("Warming up DeepFace Facenet model on server startup...")
        dummy_img = np.zeros((100, 100, 3), dtype=np.uint8)
        DeepFace.represent(
            dummy_img,
            model_name="Facenet",
            detector_backend="opencv",
            enforce_detection=False
        )
        print("DeepFace Facenet model warmed up successfully!")
    except Exception as e:
        print(f"Model warmup notice: {e}")


