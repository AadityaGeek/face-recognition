from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routes import register, verify

app = FastAPI()

# ✅ Hard‑coded CORS origins
origins = [
    "https://face-recognition-app-self.vercel.app",
    "http://localhost:5173"
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def home():
    return {"message": "Backend is running!"}

@app.get("/health")
def health():
    return {"status": "ok"}

app.include_router(register.router)
app.include_router(verify.router)

