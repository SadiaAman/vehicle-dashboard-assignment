from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


MediaAction = Literal["none", "play", "pause", "toggle"]
DrivingStatus = Literal["Parked", "Driving", "Charging"]


class MediaState(BaseModel):
    """Current state of the simulated media controller."""

    is_playing: bool
    track_name: str
    progress_percent: int = Field(ge=0, le=100)


class NavigationState(BaseModel):
    """Simplified navigation information."""

    destination: str
    remaining_minutes: int = Field(ge=0)
    distance_km: float = Field(ge=0)


class DashboardSyncRequest(BaseModel):
    """
    Data received from the Android dashboard.

    The Android client sends the values it is currently DISPLAYING plus an
    optional media-controller action (what the driver just pressed).
    The `displayed_` prefix is deliberate: these are the client's values, not
    the backend's own simulation, and the two are logged separately.

    Every field is validated against the ranges from the assignment, so an
    out-of-range value is rejected with HTTP 422 before anything is logged.
    """

    displayed_speed_kmh: int = Field(ge=0, le=250)
    displayed_battery_percent: int = Field(ge=0, le=100)
    displayed_temperature_c: float = Field(ge=-20, le=50)
    displayed_driving_status: DrivingStatus
    media: MediaState
    navigation: NavigationState
    media_action: MediaAction = "none"


class DashboardState(BaseModel):
    """Complete dashboard state returned to the Android application."""

    timestamp: datetime
    speed_kmh: int = Field(ge=0, le=250)
    battery_percent: int = Field(ge=0, le=100)
    outside_temperature_c: float = Field(ge=-20, le=50)
    driving_status: DrivingStatus
    media: MediaState
    navigation: NavigationState


class SyncResponse(BaseModel):
    """
    Acknowledgement returned by POST /api/dashboard/sync.

    The assignment asks this endpoint to "receive the processed data results
    and provide status and values", so the response contains exactly that:
    a status, when the data was accepted, where it was logged, and the
    validated values that were actually stored.
    """

    status: Literal["accepted"] = "accepted"
    received_at: datetime
    logged_to: str
    values: DashboardSyncRequest


class HealthResponse(BaseModel):
    """Minimal health check so the Android app can show a connection state."""

    status: Literal["ok"] = "ok"
    service: str
    uptime_seconds: float