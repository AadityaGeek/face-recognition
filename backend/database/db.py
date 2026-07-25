import os
from dotenv import load_dotenv
from pymongo import MongoClient

load_dotenv()  # take environment variables from .env

mongo_uri = os.getenv("MONGO_URI")
client = MongoClient(mongo_uri)
db = client["face_recognition_db"]
users_collection = db["users"]
