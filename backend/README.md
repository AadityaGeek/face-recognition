# Face Recognition & Verification Backend

FastAPI asynchronous backend service for AI-powered face registration, real-time motion-based liveness verification, and biometric face matching using DeepFace (Facenet) and MongoDB.

---

## 🛠️ Technology Stack

* **Framework**: FastAPI (Python 3.11)
* **Face Recognition & Biometrics**: DeepFace (`Facenet` model, `opencv` detector backend)
* **Computer Vision**: OpenCV (`opencv-python-headless`)
* **Database**: MongoDB (via PyMongo & BSON Binary storage)
* **QR Code Generation**: `qrcode` library & `Pillow`
* **Deployment**: Docker & Railway

---

## 📁 Directory Structure

```text
backend/
├── main.py              # Application entrypoint, CORS configuration, startup model warmup & health check routes
├── requirements.txt     # Python dependencies
├── Dockerfile           # Docker container production configuration
├── railway.json         # Railway deployment manifest
├── database/
│   └── db.py            # MongoDB client connection setup
├── models/
│   └── user.py          # Pydantic schema for User profile data
├── routes/
│   ├── register.py      # /register & /check-user-id endpoints
│   └── verify.py        # /verify (Liveness & DeepFace vector match) endpoint
└── utils/
    └── image_utils.py   # Frame resizing & vector math helper functions
```

---

## 🔄 Verification & Validation Code Flow

### 1. User Registration Flow (`POST /register`)

```text
[ Upload Image + Form Data ] 
             │
             ▼
[ Decode & Resize Image ] ───► (Fail? Return "Invalid image")
             │
             ▼
[ DeepFace Embedding ] ─────► Extract 128-d Facenet embedding vector
             │
             ▼
[ Duplicate Check ] ────────► Cosine similarity against stored DB embeddings
                             └─► If similarity ≥ 40% (0.40), reject as duplicate
             │
             ▼
[ MongoDB Save ] ───────────► Save User profile + 128-d vector + Binary image bytes
             │
             ▼
[ QR Code Creation ] ───────► Generate Base64 PNG QR Code containing user_id
```

1. **Input Payload**: `file` (Multipart image), `name` (Form string), `age` (Form int), `user_id` (Form string).
2. **Image Decoding & Resizing**: Converts uploaded bytes into an OpenCV BGR matrix (`cv2.imdecode`) and resizes (`max_dim=640`) for fast processing.
3. **Biometric Embedding**: Computes 128-d facial vector embedding using `DeepFace.represent(img, model_name="Facenet", detector_backend="opencv")`.
4. **Duplicate Prevention**: Queries existing stored vectors in MongoDB using projection (`{"embedding": 1, ...}`) and calculates Cosine Similarity ($\frac{\vec{v_1} \cdot \vec{v_2}}{\|\vec{v_1}\| \|\vec{v_2}\|}$). If similarity $\ge 0.40$, registration is rejected to prevent duplicate profiles.
5. **Persistence**: Saves profile metadata, embedding vector, and binary image payload (`Binary(file_bytes)`) to MongoDB `users` collection.
6. **QR Generation**: Encodes `user_id` into a PNG QR code, returned as a Base64 string.

---

### 2. Biometric Verification & Liveness Flow (`POST /verify`)

```text
[ Upload Video/Photo + user_id ]
             │
             ▼
[ Motion Liveness Check ] ──► Frame-by-frame absdiff across sub-sampled frames
                             └─► Motion score < 0.5? Return ("is_live": False)
             │
             ▼
[ Fetch Stored Vector ] ────► Fast MongoDB lookup for pre-computed 128-d embedding
             │
             ▼
[ In-Memory Vector Match ] ─► Cosine distance d = 1.0 - cos_sim(v_candidate, v_stored)
             │
             ▼
[ Threshold Evaluation ] ───► similarity = (1.0 - distance) * 100%
                             └─► distance ≤ 0.40 (similarity ≥ 60%)?
                                    ├─ YES ──► "verified": True + User Details
                                    └─ NO  ──► "verified": False + Error Message
```

1. **Input Payload**: `user_id` (Form string), `file` (Multipart video `.mp4` or photo `.jpg`).
2. **Motion-Based Liveness Detection**:
   - `detect_motion()` opens video via `cv2.VideoCapture`.
   - Sub-samples up to 15 frames downscaled to 320px for high-speed calculation.
   - Computes mean absolute frame difference (`cv2.absdiff`) across consecutive grayscale frames.
   - If movement score `max_diff < 0.5`, liveness fails (`"is_live": False`). Single frame photo uploads pass by default.
3. **Optimized Vector Lookup**: Queries MongoDB `users` collection for `user_id` returning only the pre-computed `embedding` vector (bypassing heavy binary image retrieval).
4. **Sub-Second Biometric Matching**:
   - Extracts 128-d vector from uploaded frame via `DeepFace.represent()`.
   - Computes Cosine Distance ($d$) directly in memory ($d = 1.0 - \frac{\vec{u} \cdot \vec{v}}{\|\vec{u}\| \|\vec{v}\|}$).
   - Converts Cosine Distance to similarity percentage: `similarity = max(0.0, 1.0 - distance) * 100%`.
5. **Decision Logic**:
   - Verified if `distance <= 0.40` ($\ge 60\%$ facial similarity).
   - Returns JSON with `verified` (boolean), `is_live` (boolean), `score_percent`, `threshold_percent`, and user profile details.

---

## ⚡ Performance Optimizations

* **Startup Model Warmup**: DeepFace model weights are pre-loaded during FastAPI application startup (`@app.on_event("startup")`), eliminating cold-start delays on initial requests.
* **Vector-Based Lookup**: Avoids stored image binary retrieval by matching against 128-d stored embedding vectors directly.
* **Smart Downscaling**: Downscales incoming frames to `640px` (and `320px` for motion checks) to maintain high accuracy while minimizing CPU usage.

---

## ⚙️ How to Set Up & Run

### Prerequisites

* Python 3.11+
* MongoDB Instance (Local MongoDB or MongoDB Atlas URI)

---

### Step 1: Navigate to Backend Directory

```bash
cd backend
```

### Step 2: Create & Activate Virtual Environment

**Windows (PowerShell):**
```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

**Linux / macOS:**
```bash
python3 -m venv .venv
source .venv/bin/activate
```

### Step 3: Install Dependencies

```bash
pip install --upgrade pip
pip install -r requirements.txt
```

---

### Step 4: Environment Configuration

Create a `.env` file in the `backend/` directory:

```env
MONGO_URI=mongodb+srv://<username>:<password>@cluster.mongodb.net/
ALLOWED_ORIGINS=http://localhost:5173,https://your-frontend.vercel.app
```

---

### Step 5: Run Local Development Server

Start FastAPI with Uvicorn auto-reload:

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The API will be available at:
* **Interactive Docs (Swagger UI)**: `http://localhost:8000/docs`
* **Health Check**: `http://localhost:8000/health`
* **User ID Check**: `http://localhost:8000/check-user-id?user_id=123`

---

## 🐳 Docker Deployment

### Build Docker Image locally:
```bash
docker build -t face-recognition-backend .
```

### Run Container locally:
```bash
docker run -d -p 8000:8000 --env-file .env face-recognition-backend
```

---

## ☁️ Railway Deployment

1. Connect repository to Railway.
2. Set root directory to `backend/`.
3. Add environment variable `MONGO_URI` in Railway dashboard.
4. Railway will automatically build using the included `Dockerfile` and `railway.json`.
