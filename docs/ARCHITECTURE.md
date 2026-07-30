# Architecture Overview

Two diagrams: the tools used to build the system, and the system itself.

---

## A. Toolchain

```mermaid
flowchart LR
    subgraph Authoring["Development environment"]
        AS["Android Studio 2026.1.3<br/>bundled JBR = JDK 25"]
        VSC["VS Code + Claude Code<br/>backend and docs"]
    end

    subgraph AndroidSide["Android app"]
        KT["Kotlin 2.2.10<br/>Jetpack Compose, Material 3"]
        GRADLE["Gradle 9.5.0<br/>AGP 9.3.1"]
        APK["app-debug.apk"]
        EMU["Pixel Tablet emulator<br/>Google Play API 35"]
    end

    subgraph BackendSide["Python backend"]
        PY["Python 3.11.9<br/>FastAPI 0.141.1 + Pydantic"]
        UVI["Uvicorn on port 8000"]
        LOGFILE["logs/dashboard.log"]
        PYTEST["pytest 9.1.1 - 16 tests"]
    end

    AS --> KT
    KT --> GRADLE
    GRADLE --> APK
    APK --> EMU
    VSC --> PY
    PY --> UVI
    PY --> PYTEST
    UVI --> LOGFILE
    EMU -- "HTTP / JSON via 10.0.2.2:8000" --> UVI
```

---

## B. Software architecture and data flow

```mermaid
flowchart TD
    subgraph App["Android app - com.example.vehicledashboard"]
        SCREEN["DashboardScreen<br/>Compose UI"]
        VM["DashboardViewModel<br/>StateFlow of DashboardUiState"]
        REPO["DashboardRepository<br/>returns Result"]
        API["DashboardApi<br/>Retrofit + kotlinx.serialization"]
    end

    subgraph Backend["FastAPI backend"]
        GETEP["GET /api/dashboard"]
        POSTEP["POST /api/dashboard/sync"]
        SIM["DashboardSimulator<br/>state is a function of time"]
        VALID["Pydantic models<br/>range validation"]
        LOGGER["logging_config<br/>logs/dashboard.log"]
    end

    SCREEN -- "1. observes state" --> VM
    VM -- "2. poll every 1 s" --> REPO
    REPO --> API
    API -- "3. GET" --> GETEP
    GETEP -- "4. snapshot(now)" --> SIM
    SIM -- "5. JSON, snake_case" --> API
    API -- "6. toDomain() maps to VehicleState" --> REPO
    REPO -- "7. Connected or Offline" --> VM

    SCREEN -- "A. play/pause tapped" --> VM
    VM -- "B. syncDisplayedState(TOGGLE)" --> REPO
    REPO -- "C. POST" --> POSTEP
    POSTEP -- "D. validate" --> VALID
    VALID -- "E. write timestamped line" --> LOGGER
    VALID -- "F. apply media action" --> SIM
    POSTEP -- "G. 200 accepted + values" --> REPO
```

**GET polling flow (1–7).** The backend simulates; the app displays. Steps 1–7
repeat about once per second. The UI never sees JSON field names — `toDomain()`
at step 6 is the only place they exist.

**POST action flow (A–G).** The play/pause button does not flip a local flag. It
reports the action, the backend validates it, logs it with a timestamp, and
applies it to the simulation — so the change arrives back through the normal GET
poll at most one second later. One source of truth, and the log entry is proof
the round trip happened.

---

## Layer responsibilities

| Layer | File(s) | Rule it follows |
|---|---|---|
| UI | `ui/DashboardScreen.kt`, `ui/components/*` | Reads state, reports clicks. No logic. |
| Presentation | `ui/DashboardViewModel.kt`, `ui/DashboardUiState.kt` | Owns the polling loop and the three states. Survives rotation. |
| Data | `data/DashboardRepository.kt` | Hides the data source. Converts failures into values, never throws at the UI. |
| Transport | `data/remote/*` | Retrofit contract, DTOs, and the single DTO→domain mapping. |
| Domain | `domain/VehicleState.kt` | Types the UI can trust: enums and `Instant`, not strings. |
| Simulation | `app/simulator.py` | The only source of vehicle data in the whole system. |
| Contract | `app/models.py` | Ranges from the assignment, enforced by Pydantic. |
| Logging | `app/logging_config.py` | Timestamped, UTC, one JSON object per line. |

The `docs/` folder also contains the UI reference mock-ups that guided the visual
design. They are design references only and are not used as assets in the app.
