"""
Focused tests for the backend.

Four groups, matching the four things that can realistically break:

1. endpoints answer at all (status codes and response shape)
2. the simulator never leaves the ranges required by the assignment
3. invalid client data is rejected instead of being logged
4. received data really lands in the log file, with a timestamp
"""

import json
import re
from datetime import datetime, timedelta, timezone

import pytest

from app.logging_config import LOG_FILE, configure_logging
from app.simulator import CYCLE_SECONDS, DashboardSimulator


# --------------------------------------------------------------------------- 1
# Endpoint responses
# ---------------------------------------------------------------------------


def test_health_returns_ok(client):
    response = client.get("/health")

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["service"] == "vehicle-dashboard-backend"
    assert body["uptime_seconds"] >= 0


def test_get_dashboard_returns_complete_state(client):
    response = client.get("/api/dashboard")

    assert response.status_code == 200
    body = response.json()

    # Every value the dashboard has to display must be present.
    for key in (
        "timestamp",
        "speed_kmh",
        "battery_percent",
        "outside_temperature_c",
        "driving_status",
        "media",
        "navigation",
    ):
        assert key in body

    assert set(body["media"]) == {"is_playing", "track_name", "progress_percent"}
    assert set(body["navigation"]) == {"destination", "remaining_minutes", "distance_km"}


def test_post_sync_returns_status_and_accepted_values(client, valid_sync_payload):
    response = client.post("/api/dashboard/sync", json=valid_sync_payload)

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "accepted"
    # The endpoint must echo the values it accepted, not something else.
    assert body["values"]["displayed_speed_kmh"] == 87
    assert body["values"]["media"]["track_name"] == "Night Drive"
    assert body["logged_to"].endswith("dashboard.log")


# --------------------------------------------------------------------------- 2
# Simulated value ranges
# ---------------------------------------------------------------------------


def test_simulated_values_stay_in_range_over_a_full_cycle():
    """
    Walk through two complete simulation cycles second by second and check
    every value against the limits from the assignment. This is the reason the
    simulator takes a timestamp: no sleeping, no flakiness.
    """
    start = datetime(2026, 1, 1, 12, 0, 0, tzinfo=timezone.utc)
    simulator = DashboardSimulator(started_at=start)

    for second in range(int(CYCLE_SECONDS) * 2):
        state = simulator.snapshot(start + timedelta(seconds=second))

        assert 0 <= state.speed_kmh <= 250
        assert 0 <= state.battery_percent <= 100
        assert -20 <= state.outside_temperature_c <= 50
        assert state.driving_status in ("Parked", "Driving", "Charging")
        assert 0 <= state.media.progress_percent <= 100
        assert state.media.track_name
        assert state.navigation.remaining_minutes >= 0
        assert state.navigation.distance_km >= 0


def test_all_three_driving_statuses_occur_within_one_cycle():
    """A reviewer must be able to see every driving status in a short demo."""
    start = datetime(2026, 1, 1, 12, 0, 0, tzinfo=timezone.utc)
    simulator = DashboardSimulator(started_at=start)

    seen = {
        simulator.snapshot(start + timedelta(seconds=s)).driving_status
        for s in range(int(CYCLE_SECONDS))
    }

    assert seen == {"Parked", "Driving", "Charging"}


def test_vehicle_stands_still_when_not_driving():
    """Sanity check on the state machine: speed is only non-zero while Driving."""
    start = datetime(2026, 1, 1, 12, 0, 0, tzinfo=timezone.utc)
    simulator = DashboardSimulator(started_at=start)

    for second in range(int(CYCLE_SECONDS)):
        state = simulator.snapshot(start + timedelta(seconds=second))
        if state.driving_status != "Driving":
            assert state.speed_kmh == 0


def test_paused_media_does_not_advance():
    """Play/pause is real state, not a cosmetic flag."""
    start = datetime(2026, 1, 1, 12, 0, 0, tzinfo=timezone.utc)
    simulator = DashboardSimulator(started_at=start)

    simulator.apply_media_action("pause", now=start)
    before = simulator.snapshot(start + timedelta(seconds=10)).media.progress_percent
    after = simulator.snapshot(start + timedelta(seconds=30)).media.progress_percent
    assert before == after

    simulator.apply_media_action("play", now=start + timedelta(seconds=30))
    playing = simulator.snapshot(start + timedelta(seconds=45)).media.progress_percent
    assert playing > after


# --------------------------------------------------------------------------- 3
# POST validation
# ---------------------------------------------------------------------------


@pytest.mark.parametrize(
    "field, bad_value",
    [
        ("displayed_speed_kmh", 300),  # above the 250 km/h limit
        ("displayed_speed_kmh", -5),  # negative speed
        ("displayed_battery_percent", 101),  # above 100 %
        ("displayed_temperature_c", 75.0),  # above +50 °C
        ("displayed_temperature_c", -40.0),  # below -20 °C
        ("displayed_driving_status", "Flying"),  # not one of the three states
    ],
)
def test_post_sync_rejects_out_of_range_values(client, valid_sync_payload, field, bad_value):
    payload = dict(valid_sync_payload)
    payload[field] = bad_value

    response = client.post("/api/dashboard/sync", json=payload)

    # 422 = FastAPI/Pydantic rejected the body before our code ran.
    assert response.status_code == 422


def test_post_sync_rejects_missing_fields(client):
    response = client.post("/api/dashboard/sync", json={"displayed_speed_kmh": 50})

    assert response.status_code == 422


def test_media_action_changes_the_simulation(client, valid_sync_payload):
    """Pressing pause in the app must be visible in the next GET /api/dashboard."""
    payload = dict(valid_sync_payload)

    payload["media_action"] = "pause"
    client.post("/api/dashboard/sync", json=payload)
    assert client.get("/api/dashboard").json()["media"]["is_playing"] is False

    payload["media_action"] = "play"
    client.post("/api/dashboard/sync", json=payload)
    assert client.get("/api/dashboard").json()["media"]["is_playing"] is True


# --------------------------------------------------------------------------- 4
# Timestamped logging
# ---------------------------------------------------------------------------


def test_received_data_is_written_to_the_log_file_with_a_timestamp(client, valid_sync_payload):
    configure_logging()  # make sure the handler exists before we read the file

    payload = dict(valid_sync_payload)
    payload["navigation"]["destination"] = "Log Test Destination"

    lines_before = LOG_FILE.read_text(encoding="utf-8").splitlines() if LOG_FILE.exists() else []
    client.post("/api/dashboard/sync", json=payload)
    lines_after = LOG_FILE.read_text(encoding="utf-8").splitlines()

    assert len(lines_after) > len(lines_before), "no new line was written to the log file"

    new_line = lines_after[-1]

    # "2026-07-30T18:22:41.123Z | INFO | RECEIVED | {...}"
    assert re.match(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z \| INFO \| RECEIVED \| ", new_line)

    # The payload itself must be readable back out of the log line.
    logged_json = json.loads(new_line.split("RECEIVED | ", 1)[1])
    assert logged_json["navigation"]["destination"] == "Log Test Destination"
    assert logged_json["displayed_speed_kmh"] == 87
