from fastapi import APIRouter, UploadFile, Form
from deepface import DeepFace
import qrcode, io, base64
from bson.binary import Binary
import numpy as np
import cv2

import time
from database.db import users_collection
from models.user import User
from utils.image_utils import resize_frame, cosine_similarity

router = APIRouter(tags=["User Registration"])

# threshold for duplicate detection (cosine similarity)
DUPLICATE_THRESHOLD = 0.4  # 40%


@router.get(
    "/check-user-id",
    summary="Check User ID Availability",
    description="Checks whether a given `user_id` already exists in MongoDB database.",
    response_description="JSON object indicating if the user_id exists"
)
def check_user_id(user_id: str):
    user = users_collection.find_one({"user_id": user_id}, {"_id": 1})
    return {"exists": user is not None, "user_id": user_id}

@router.post(
    "/register",
    summary="Register New User Biometrics",
    description=(
        "Registers a new user by decoding the submitted facial image, extracting 512-d feature embeddings "
        "using DeepFace (Facenet), verifying uniqueness against existing database records, saving user details to MongoDB, "
        "and returning a Base64-encoded QR code."
    ),
    response_description="Status of registration and generated QR code base64 string"
)
def register_user(
    file: UploadFile,
    name: str = Form(...),
    age: int = Form(...),
    user_id: str = Form(...)
):
    t_start = time.perf_counter()
    print(f"\n--- [REGISTER START] user_id: {user_id} ---")

    # Step 1: Read and decode image from upload buffer
    t0 = time.perf_counter()
    file_bytes = file.file.read()
    np_arr = np.frombuffer(file_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)

    if img is None:
        print("  [ERROR] Invalid image provided.")
        return {"success": False, "error": "Invalid image"}

    # Resize image to max 640px for optimal speed and accuracy balance
    img = resize_frame(img, max_dim=640)
    print(f"  [1/4] Image Read, Decode & Resize: {(time.perf_counter() - t0)*1000:.1f} ms")

    # Step 2: Extract 512-d facial embedding using DeepFace (Facenet)
    try:
        t0 = time.perf_counter()
        embedding = DeepFace.represent(
            img,
            model_name="Facenet",
            detector_backend="opencv",
            enforce_detection=True
        )[0]["embedding"]
        print(f"  [2/4] DeepFace Feature Extraction: {(time.perf_counter() - t0)*1000:.1f} ms")
    except Exception as e:
        print(f"  [ERROR] Face embedding failed: {str(e)}")
        return {"success": False, "error": "No face detected in the image. Please provide a clear image with a visible face."}

    # Step 3: Check for duplicate registered faces (Projection skips fetching heavy raw image data)
    t0 = time.perf_counter()
    for existing in users_collection.find({}, {"embedding": 1, "user_id": 1, "name": 1, "age": 1}):
        if "embedding" not in existing:
            continue
        sim = cosine_similarity(embedding, existing["embedding"])
        if sim >= DUPLICATE_THRESHOLD:
            print(f"  [WARN] Duplicate face detected! Matches user_id: {existing['user_id']}")
            return {
                "success": False,
                "error": "User already registered",
                "existing_user": {
                    "user_id": existing["user_id"],
                    "name": existing["name"],
                    "age": existing["age"]
                }
            }
    print(f"  [3/4] Duplicate Check in DB: {(time.perf_counter() - t0)*1000:.1f} ms")

    # Step 4: Construct user record and persist into MongoDB
    t0 = time.perf_counter()
    user = User(
        user_id=user_id,
        name=name,
        age=age,
        embedding=embedding
    )

    users_collection.insert_one({
        **user.dict(),
        "image_data": Binary(file_bytes)
    })

    # Step 5: Generate user registration QR code (Base64 PNG string)
    qr = qrcode.make(user_id)
    buf = io.BytesIO()
    qr.save(buf, format="PNG")
    qr_base64 = base64.b64encode(buf.getvalue()).decode("utf-8")
    print(f"  [4/4] Save to DB & QR Generation: {(time.perf_counter() - t0)*1000:.1f} ms")

    t_total = (time.perf_counter() - t_start) * 1000
    print(f"--- [REGISTER COMPLETE] Total Time: {t_total:.1f} ms ---\n")

    return {"success": True, "qr_code": qr_base64}
