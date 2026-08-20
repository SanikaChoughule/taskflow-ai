import os
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
from typing import Generator

# Get database URL from environment variable, fallback to sqlite for local dev
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///taskflow_ai.db")

# Create engine with parameters based on DB driver
if DATABASE_URL.startswith("sqlite"):
    engine = create_engine(
        DATABASE_URL,
        connect_args={"check_same_thread": False}
    )
else:
    engine = create_engine(
        DATABASE_URL,
        pool_size=20,
        max_overflow=10,
        pool_timeout=30,
        pool_recycle=1800
    )

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db() -> Generator[Session, None, None]:
    """Dependency generator to provide safe thread-local database sessions."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Auto-create SQLite database tables on first import
from app.db.models import Base
Base.metadata.create_all(bind=engine)
