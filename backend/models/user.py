from pydantic import BaseModel
from typing import List

class User(BaseModel):
    """
    Pydantic schema representing registered user data.
    """
    user_id: str             # Unique identifier (e.g. registration ID / enrollment number)
    name: str                # User's full name
    age: int                 # User's age
    embedding: List[float]   # 512-dimensional facial feature vector extracted by Facenet
