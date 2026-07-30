"""
Simulated vehicle data source.

Design decision
---------------
The whole simulation is driven by ONE input: the current timestamp.
`snapshot(now)` is therefore (almost) a pure function of `now`, which means:

* the values change smoothly and continuously while the server runs,
* the same timestamp always produces the same values, so tests are
  deterministic without mocking `datetime.now()` or sleeping.

The only mutable state is the media player, because play/pause is a *user
action* coming from the Android client - a paused track must not keep
advancing, so its position cannot be derived from wall-clock time alone.
"""

from datetime import datetime, timezone
from math import pi, sin

from .models import DashboardState, DrivingStatus, MediaState, NavigationState

# --- Timeline of one simulated trip -----------------------------------------
# The simulation repeats a 120 s cycle that tells a small story:
#   0 -  15 s : Parked  (in the driveway)
#  15 -  95 s : Driving (accelerate, cruise, decelerate)
#  95 - 120 s : Charging (arrived at the destination, plugged in)
# A short cycle is intentional: a reviewer sees every driving status within
# two minutes instead of waiting for a realistic trip length.
CYCLE_SECONDS = 120.0
PARKED_UNTIL = 15.0
DRIVING_UNTIL = 95.0

# --- Value ranges (all inside the limits required by the assignment) --------
SPEED_MAIN_AMPLITUDE = 180.0  # km/h - dominant half-sine of the speed curve
SPEED_RIPPLE_AMPLITUDE = 30.0  # km/h - small overlay so the needle is not boring

BATTERY_AT_TRIP_START = 82.0  # %
BATTERY_AT_TRIP_END = 61.0  # % - drained by driving, recovered by charging

TEMPERATURE_BASE_C = 17.5  # °C - midpoint of the outside-temperature wave
TEMPERATURE_AMPLITUDE_C = 6.5  # °C - so the range is 11.0 .. 24.0 °C
TEMPERATURE_PERIOD_S = 600.0  # a slow 10-minute wave; temperature is not nervous

# --- Media ------------------------------------------------------------------
# Neutral, invented track titles - no real artists, no brand references.
TRACK_NAMES = (
    "Night Drive",
    "Coastal Highway",
    "Electric Sunrise",
    "City Lights",
)
TRACK_LENGTH_SECONDS = 45.0

# --- Navigation -------------------------------------------------------------
DESTINATION_NAME = "Riverside Charging Hub"
TRIP_DISTANCE_KM = 42.0
AVERAGE_SPEED_KMH = 45.0  # used to turn remaining distance into remaining minutes


def _clamp(value: float, low: float, high: float) -> float:
    """Keep a value inside [low, high] so the Pydantic models can never fail."""
    return max(low, min(high, value))


class DashboardSimulator:
    """Produces a complete, plausible dashboard state for any point in time."""

    def __init__(self, started_at: datetime | None = None) -> None:
        # Everything is measured relative to this start time.
        self._started_at = started_at or datetime.now(timezone.utc)

        # Mutable media state (see module docstring for why this is not pure).
        self._is_playing = True
        self._media_position_s = 0.0
        self._media_clock = self._started_at

    # ------------------------------------------------------------------ media
    def _advance_media(self, now: datetime) -> None:
        """Move the play head forward by the real time elapsed - only if playing."""
        elapsed = (now - self._media_clock).total_seconds()
        # Guard against negative deltas (a test may pass timestamps out of order).
        if self._is_playing and elapsed > 0:
            self._media_position_s += elapsed
        self._media_clock = now

    def apply_media_action(self, action: str, now: datetime | None = None) -> None:
        """
        Handle a play/pause request coming from the Android client.

        The play head is advanced *before* the state changes, otherwise the time
        spent playing before a pause would be lost.
        """
        now = now or datetime.now(timezone.utc)
        self._advance_media(now)

        if action == "play":
            self._is_playing = True
        elif action == "pause":
            self._is_playing = False
        elif action == "toggle":
            self._is_playing = not self._is_playing
        # "none" -> the client only reported its state, nothing to change.

    def _media_state(self) -> MediaState:
        track_index = int(self._media_position_s // TRACK_LENGTH_SECONDS) % len(TRACK_NAMES)
        position_in_track = self._media_position_s % TRACK_LENGTH_SECONDS
        progress = position_in_track / TRACK_LENGTH_SECONDS * 100.0
        return MediaState(
            is_playing=self._is_playing,
            track_name=TRACK_NAMES[track_index],
            progress_percent=int(_clamp(progress, 0, 100)),
        )

    # ------------------------------------------------------- driving dynamics
    def _driving_status(self, t_cycle: float) -> DrivingStatus:
        if t_cycle < PARKED_UNTIL:
            return "Parked"
        if t_cycle < DRIVING_UNTIL:
            return "Driving"
        return "Charging"

    def _drive_progress(self, t_cycle: float) -> float:
        """How far through the driving phase we are, as 0.0 .. 1.0."""
        if t_cycle <= PARKED_UNTIL:
            return 0.0
        if t_cycle >= DRIVING_UNTIL:
            return 1.0
        return (t_cycle - PARKED_UNTIL) / (DRIVING_UNTIL - PARKED_UNTIL)

    def _speed_kmh(self, t_cycle: float) -> int:
        """
        Speed curve for the driving phase.

        Two sine waves added together: a half-sine that starts and ends at 0
        (so the car accelerates from and returns to standstill) plus a smaller
        ripple, so the needle moves the way a real drive looks.
        """
        if not (PARKED_UNTIL <= t_cycle < DRIVING_UNTIL):
            return 0

        p = self._drive_progress(t_cycle)
        speed = SPEED_MAIN_AMPLITUDE * sin(pi * p) + SPEED_RIPPLE_AMPLITUDE * sin(3 * pi * p)
        return int(_clamp(speed, 0, 250))

    def _battery_percent(self, t_cycle: float) -> int:
        """Full while parked, drains linearly while driving, recharges while charging."""
        drop = BATTERY_AT_TRIP_START - BATTERY_AT_TRIP_END

        if t_cycle < PARKED_UNTIL:
            battery = BATTERY_AT_TRIP_START
        elif t_cycle < DRIVING_UNTIL:
            battery = BATTERY_AT_TRIP_START - drop * self._drive_progress(t_cycle)
        else:
            # Charging phase: climb back to the starting level so the next cycle
            # continues seamlessly instead of jumping.
            charge_progress = (t_cycle - DRIVING_UNTIL) / (CYCLE_SECONDS - DRIVING_UNTIL)
            battery = BATTERY_AT_TRIP_END + drop * charge_progress

        return int(round(_clamp(battery, 0, 100)))

    def _temperature_c(self, t_total: float) -> float:
        """A slow sine wave - outside temperature changes gradually, not per second."""
        wave = sin(2 * pi * t_total / TEMPERATURE_PERIOD_S)
        temperature = TEMPERATURE_BASE_C + TEMPERATURE_AMPLITUDE_C * wave
        return round(_clamp(temperature, -20, 50), 1)

    def _navigation_state(self, t_cycle: float) -> NavigationState:
        """
        Remaining distance shrinks while driving and is 0 once we arrive
        (the charging phase happens *at* the destination).
        """
        status = self._driving_status(t_cycle)
        if status == "Charging":
            distance_km = 0.0
        else:
            distance_km = TRIP_DISTANCE_KM * (1.0 - self._drive_progress(t_cycle))

        remaining_minutes = int(round(distance_km / AVERAGE_SPEED_KMH * 60))
        return NavigationState(
            destination=DESTINATION_NAME,
            remaining_minutes=max(0, remaining_minutes),
            distance_km=round(distance_km, 1),
        )

    # --------------------------------------------------------------- snapshot
    def snapshot(self, now: datetime | None = None) -> DashboardState:
        """Build the complete dashboard state for `now` (defaults to 'right now')."""
        now = now or datetime.now(timezone.utc)
        self._advance_media(now)

        t_total = (now - self._started_at).total_seconds()
        t_cycle = t_total % CYCLE_SECONDS  # position inside the repeating trip

        return DashboardState(
            timestamp=now,
            speed_kmh=self._speed_kmh(t_cycle),
            battery_percent=self._battery_percent(t_cycle),
            outside_temperature_c=self._temperature_c(t_total),
            driving_status=self._driving_status(t_cycle),
            media=self._media_state(),
            navigation=self._navigation_state(t_cycle),
        )
