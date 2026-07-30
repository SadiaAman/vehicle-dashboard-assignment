"""
FastAPI application for the vehicle dashboard backend.

The API surface is intentionally tiny - three endpoints:

    GET  /health              -> is the backend reachable?
    GET  /api/dashboard       -> the simulated vehicle data (source of truth)
    POST /api/dashboard/sync  -> receives what the client displays, logs it,
                                 and answers with a status plus the values

Why both a GET and a POST?
The assignment says the backend "just provides simulated data" but also asks
for an endpoint that "receives the processed data results from the Android
client". Those are two different directions, so they are two endpoints. The
GET keeps the simulation in ONE place (the backend), the POST fulfils the
receive-and-log requirement and carries the driver's play/pause action back.

This module stays deliberately thin: validate (Pydantic) -> simulate
(simulator.py) -> log (logging_config.py) -> respond.
"""

from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI

from .logging_config import LOG_FILE, configure_logging
from .models import (
    DashboardState,
    DashboardSyncRequest,
    HealthResponse,
    SyncResponse,
)
from .simulator import DashboardSimulator

SERVICE_NAME = "vehicle-dashboard-backend"

# One simulator instance is shared by all requests, so every client sees the
# same vehicle. All endpoints below are `async def` and contain no `await`,
# which means they run to completion on the single event-loop thread - no lock
# is required around the simulator's mutable media state.
simulator = DashboardSimulator()
logger = configure_logging()

_started_at = datetime.now(timezone.utc)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Startup/shutdown hook - used only to mark the log file boundaries."""
    logger.info("BACKEND STARTED | service=%s | log_file=%s", SERVICE_NAME, LOG_FILE)
    yield
    logger.info("BACKEND STOPPED | service=%s", SERVICE_NAME)


app = FastAPI(
    title="Vehicle Dashboard Backend",
    description="Simulated vehicle data for the Android dashboard client.",
    version="1.0.0",
    lifespan=lifespan,
)


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    """Cheap liveness check - the Android app uses it to show a connection state."""
    uptime = (datetime.now(timezone.utc) - _started_at).total_seconds()
    return HealthResponse(service=SERVICE_NAME, uptime_seconds=round(uptime, 1))


@app.get("/api/dashboard", response_model=DashboardState)
async def get_dashboard() -> DashboardState:
    """
    Return the current simulated vehicle state.

    The Android client polls this endpoint; because the simulation is derived
    from the current time, consecutive calls return smoothly changing values.
    """
    return simulator.snapshot()


@app.post("/api/dashboard/sync", response_model=SyncResponse)
async def sync_dashboard(request: DashboardSyncRequest) -> SyncResponse:
    """
    Receive the state currently displayed by the Android client.

    Steps:
    1. FastAPI + Pydantic have already validated the payload against the
       required ranges; an invalid value never reaches this function (HTTP 422).
    2. Apply the media action, so pressing play/pause in the app actually
       changes the simulation the app is polling.
    3. Write one timestamped line into the log file.
    4. Answer with status + the accepted values.
    """
    received_at = datetime.now(timezone.utc)

    simulator.apply_media_action(request.media_action, now=received_at)

    # model_dump_json() keeps the log line machine-readable (one JSON object
    # per line), which is far easier to grep or re-import than free text.
    logger.info("RECEIVED | %s", request.model_dump_json())

    return SyncResponse(
        received_at=received_at,
        logged_to=str(LOG_FILE),
        values=request,
    )
