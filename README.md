# Vehicle Dashboard

A generic vehicle-dashboard prototype in two parts:

* an **Android tablet app** (Kotlin, Jetpack Compose, Material 3) that displays
  driving data, a media controller and a navigation panel;
* a **Python backend** (FastAPI) that generates the simulated vehicle data and
  receives, validates and logs the values the app is displaying.

There is no real vehicle signal and no external service anywhere in the system.
All data originates from one deterministic simulator in the backend. This is a
conceptual tablet prototype informed by automotive glanceability principles — it
is not an Android Automotive OS application and makes no claim to production
readiness or safety certification.

---

## Repository structure

```
VehicleDashboardAssignment/
├── android-app/            Android Studio project (Kotlin + Compose)
│   ├── app/src/main/java/com/example/vehicledashboard/
│   │   ├── data/           Repository + Retrofit API, DTOs, NetworkModule
│   │   ├── domain/         VehicleState and friends
│   │   ├── ui/             DashboardScreen, ViewModel, UiState, components, theme
│   │   └── MainActivity.kt
│   └── README.md
├── python-backend/         FastAPI service
│   ├── app/                main.py, simulator.py, models.py, logging_config.py
│   ├── tests/test_api.py   16 automated tests
│   ├── logs/               dashboard.log is written here at runtime
│   └── README.md
├── docs/
│   ├── ARCHITECTURE.md     Toolchain + software architecture diagrams
│   └── UI reference mock-ups
├── PRESENTATION_GUIDE.md
└── README.md
```

## Documentation

* [Android app README](android-app/README.md) — architecture, UI decisions, setup, limitations
* [Backend README](python-backend/README.md) — endpoints, simulator, logging, tests
* [Architecture diagrams](docs/ARCHITECTURE.md) — toolchain and data flow
* [Presentation guide](PRESENTATION_GUIDE.md) — demo script and Q&A

---

## Quick start

**1. Backend** (Windows PowerShell):

```powershell
cd "D:\Development Task\VehicleDashboardAssignment\python-backend"
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

`--host 0.0.0.0` is required for the Android emulator to reach the server. For
browser-only testing, `--reload` on the default host is enough, and Swagger UI is
at <http://127.0.0.1:8000/docs>.

**2. Android app:** open `android-app` in Android Studio, select a **Pixel
Tablet** AVD, press **Run ▶**. The app connects to `http://10.0.2.2:8000/`,
which is the emulator's alias for the host machine's loopback address.

**3. Tests:**

```powershell
cd "D:\Development Task\VehicleDashboardAssignment\python-backend"
.\.venv\Scripts\python.exe -m pytest -q      # 16 passed
```

---

## Requirement checklist

### Part 1 — Android app

| Requirement | Where | Status |
|---|---|---|
| Speed 0–250 km/h | `SpeedGaugeCard.kt` — Canvas arc gauge | Runtime verified |
| Battery 0–100 % as progress indicator | `BatteryCard.kt` — number, drawn battery, progress bar | Runtime verified |
| Outside temperature −20…+50 °C | `TemperatureCard.kt` | Runtime verified |
| Driving status Parked/Driving/Charging | `SpeedGaugeCard.kt` status badge | Runtime verified |
| Media: play/pause, track display, progress | `MediaCard.kt` | Runtime verified |
| Navigation: destination, remaining time, distance | `NavigationCard.kt` | Runtime verified |
| Simulated, dynamic data | Backend simulator, polled once per second | Runtime verified |
| Tablet / large-screen layout | Landscape two-column `DashboardScreen.kt` | Runtime verified on Pixel Tablet API 35 |
| Kotlin | Whole app | Yes |
| No brand references | Generic name, palette and package | Runtime verified |

### Part 2 — Python backend

| Requirement | Where | Status |
|---|---|---|
| API server in a Python framework | FastAPI, `app/main.py` | Automated tested |
| Endpoint receiving processed data from the client | `POST /api/dashboard/sync` | Automated tested + runtime verified |
| Returns status and values | `SyncResponse` — `status`, `received_at`, `logged_to`, echoed values | Automated tested |
| Play/pause status and current track name | `media` in both directions | Automated tested |
| Outside temperature | `outside_temperature_c` / `displayed_temperature_c` | Automated tested |
| Driving status | `driving_status` / `displayed_driving_status` | Automated tested |
| Navigation destination, remaining time, distance | `navigation` object | Automated tested |
| Log received data to a file with a timestamp | `logging_config.py` → `logs/dashboard.log` | Automated tested + runtime verified |
| Python | Whole backend | Yes |

### Documentation deliverables

| Requirement | Where |
|---|---|
| Brief README per project | [android-app/README.md](android-app/README.md), [python-backend/README.md](python-backend/README.md) |
| Architecture decisions and reasoning | Both READMEs + [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Known limitations / unimplemented features | Both READMEs and below |
| Graphical toolchain + architecture overview | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) (Mermaid) |
| AI usage statement | Below, and per-project in each README |

**Verification vocabulary used throughout:** *compiled* = the build succeeded;
*runtime verified* = observed running on the Pixel Tablet API 35 emulator;
*automated tested* = covered by the 16 passing backend tests.

---

## Scope and time management

The assignment expects roughly five hours and explicitly invites simplification.
**Actual development time was not tracked, so no duration is claimed here.** What
follows is how the work was scoped against that expectation.

Deliberately **in scope**: one screen, one data source, three endpoints, the six
required data groups, one interactive control, and automated tests for the
backend where they are cheap and meaningful.

Deliberately **left out** to stay within a five-hour-shaped scope — each is a
choice, not an oversight:

* automated Android tests (the backend is the part with logic worth asserting);
* periodic POST synchronisation (play/pause alone demonstrates the round trip);
* a portrait layout, a settings screen, and string localisation;
* an offline data source on the Android side (the backend must stay the single
  simulation source);
* skip forward/back, elapsed/duration and artist in the media panel, because the
  backend provides no such data;
* a real map SDK, authentication, TLS and persistence.

**Environment setup is not implementation.** Installing the Android SDK, creating
and downloading the Pixel Tablet system image (~GB-scale), the first Gradle
distribution download and the initial dependency resolution took substantial
wall-clock time on this machine but involved no design or coding. They are
reported separately from the implementation scope and are not counted as
development effort.

---

## Known limitations

* **No automated Android tests.** The generated `ExampleUnitTest` and
  `ExampleInstrumentedTest` are untouched template stubs.
* **Logging happens on interaction.** `POST /api/dashboard/sync` is triggered by
  the play/pause button only; there is no automatic periodic synchronisation, so
  the log grows when the control is used.
* **Media data is limited by the API** — no elapsed time, duration or artist, so
  none are displayed; no skip controls, because the backend accepts no such
  action.
* **Route progress is inferred** from the largest remaining distance seen in the
  session, because the API sends no total trip length.
* **Landscape only**; from API 36 Android may ignore the orientation lock on
  large screens.
* **No authentication, no TLS, no persistence, no accessibility audit.** Plain
  HTTP is permitted only for `10.0.2.2` and `localhost`.
* **In-memory backend state** resets on restart; one shared simulator instance
  serves all clients.
* The simulated 120-second trip cycle is tuned for demonstration, not realism.

Not used anywhere in this project: Android Automotive OS, gRPC, HAR, a real map
SDK, real vehicle signals, external APIs, authentication or TLS.

---

## AI use

AI (Claude Code) was used throughout and its contribution is stated plainly:
**the application and test code was AI-generated under my direction**, together
with its explanatory comments and the structure of this documentation. AI also
assisted with the architecture discussion, debugging suggestions and the UI
implementation.

What I did myself: I set and approved the scope and the visual design; created
the Android Studio project; installed and configured the Pixel Tablet emulator;
ran the backend test suite and read its output; inspected the endpoints in the
Swagger UI; ran the Gradle builds; installed and operated the app on the
emulator; tested play/pause; confirmed the 200 responses for the GET and POST
endpoints; read the timestamped entries in `dashboard.log`; identified the
destination-text overlap defect on the real tablet and requested its correction;
and reviewed the final result.

Two problems were found by testing rather than assumption, and are worth naming
because they shaped the code: a compile failure traced through the Gradle
dependency tree to Retrofit's transitive **OkHttp 3.14.9** (fixed by pinning
OkHttp 4.12.0), and a two-line destination overlapping on the real tablet layout
(fixed with an explicit `lineHeight` and proportional widths). A backend test
failure also caught a logging bug where a custom date format silently dropped
milliseconds; the logger was fixed rather than the test weakened.


