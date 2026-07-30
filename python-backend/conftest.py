"""
Shared pytest setup.

Placing conftest.py in the project root makes pytest add this folder to
sys.path, so `from app.main import app` works without installing the package.
"""

import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture
def client() -> TestClient:
    """
    A test client that calls the real FastAPI app in-process.

    No server has to be started and no network is involved, which keeps the
    test suite fast and reliable.
    """
    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture
def valid_sync_payload() -> dict:
    """A realistic, valid POST body - the baseline for the validation tests."""
    return {
        "displayed_speed_kmh": 87,
        "displayed_battery_percent": 74,
        "displayed_temperature_c": 19.5,
        "displayed_driving_status": "Driving",
        "media": {
            "is_playing": True,
            "track_name": "Night Drive",
            "progress_percent": 42,
        },
        "navigation": {
            "destination": "Riverside Charging Hub",
            "remaining_minutes": 18,
            "distance_km": 13.4,
        },
        "media_action": "none",
    }
