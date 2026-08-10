import os
import sys
from dotenv import load_dotenv
from pymongo import MongoClient

# Load environment variables from .env file
load_dotenv()

# Read MongoDB connection string from environment
mongo_uri = os.getenv("MONGO_URI")
if not mongo_uri:
    print("WARNING: MONGO_URI environment variable is missing!", file=sys.stderr)

# Initialize MongoDB client and select target database & collection
client = MongoClient(mongo_uri) if mongo_uri else None
db = client["face_recognition_db"] if client else None
users_collection = db["users"] if db is not None else None

# Create a unique index on user_id for fast queries and preventing duplicate IDs
if users_collection is not None:
    try:
        users_collection.create_index("user_id", unique=True)
    except Exception as e:
        pass
