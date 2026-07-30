# Vehicle Dashboard — Android App

A Kotlin / Jetpack Compose tablet app that displays a generic vehicle dashboard.
All values come from the Python backend in this repository; the app itself runs
**no simulation of its own**.

This is a conceptual tablet prototype informed by automotive glanceability
principles. It is not an Android Automotive OS application, it is not connected
to any real vehicle signal, and it makes no claim to production readiness or
safety certification.

---

## Implemented functionality

* **Speed** 0–250 km/h on a Canvas-drawn arc gauge with a large numeric readout
* **Battery** 0–100 % as a progress indicator (number, drawn battery, and a
  `LinearProgressIndicator` bar)
* **Outside temperature** −20 °C to +50 °C with a drawn thermometer
* **Driving status** `PARKED` / `DRIVING` / `CHARGING` as a colour-coded badge
* **Media controller** track name, play/pause, and a progress indicator
* **Navigation panel** destination, remaining minutes, remaining distance, and a
  schematic Canvas route
* **Connection states** Loading, Connected and Offline
* Landscape, two-column tablet layout; polls the backend about once per second

---

## Architecture

Simple MVVM with a repository, and one extra thin domain layer.

```
DashboardScreen (Compose)        <- reads state, reports clicks, no logic
        |  StateFlow<DashboardUiState>
DashboardViewModel               <- 1 Hz polling loop, holds the UI state
        |
DashboardRepository              <- returns Result, never throws at the UI
        |
DashboardApi (Retrofit)          <- the HTTP contract
        |
DashboardDto  --toDomain()-->  VehicleState
```

| Package | Contents |
|---|---|
| `data/remote` | `DashboardApi`, `DashboardDto`, `NetworkModule` |
| `data` | `DashboardRepository` |
| `domain` | `VehicleState`, `DrivingStatus`, `MediaState`, `NavigationState`, `MediaAction` |
| `ui` | `DashboardScreen`, `DashboardViewModel`, `DashboardUiState` |
| `ui/components` | `DashboardTopBar`, `SpeedGaugeCard`, `BatteryCard`, `TemperatureCard`, `NavigationCard`, `MediaCard`, `DashboardCard` |
| `ui/theme` | `Color`, `Theme`, `Type` |

### Why these choices

**MVVM + repository.** The ViewModel survives configuration changes, so the
polling loop is not restarted and the last values are not lost when the tablet
rotates. The repository hides *where* data comes from: if the source were ever
swapped for a real vehicle bus, only that class would change.

**StateFlow.** The UI observes one always-available "current value" and
recomposes when it changes, which is exactly how Compose wants to be driven.
`DashboardUiState` is a sealed interface, so `Loading`, `Connected` and
`Offline` are mutually exclusive by construction and `when` forces every case to
be handled at compile time.

**Retrofit + kotlinx.serialization.** Retrofit turns the backend contract into
plain Kotlin `suspend` functions — no callbacks, no manual threading.
kotlinx.serialization generates the parsers at compile time, so there are no
reflection keep-rules and a malformed payload fails immediately and loudly.
Every DTO field carries an explicit `@SerialName`, because the backend speaks
snake_case and Kotlin speaks camelCase.

**A thin domain layer.** `toDomain()` converts the transport shape into types
the UI can trust: a real `DrivingStatus` enum instead of a `String`, a real
`java.time.Instant` instead of text. The JSON field names then exist in exactly
one file.

**No Hilt.** The dependency graph has one node and one edge: a Retrofit
instance. A DI framework would add annotations, generated code and build time
for no benefit at this size. `NetworkModule` is a plain `object` with a `lazy`
property, which gives the same single-instance guarantee.

**No database.** Nothing needs to survive the process. The dashboard is a live
view of a remote state; caching it would add a synchronisation problem and
answer no requirement.

**No real map SDK.** The backend sends a destination, a remaining time and a
remaining distance — no geodata. A real map would be decoration pretending to be
information, and would pull in an API key, network tiles and a large dependency.
The Canvas route is honest about what is actually known: a start, a destination,
and how much of the way is left.

**No Material icons dependency.** Every glyph — play, pause, battery,
thermometer, route, markers — is drawn with Compose Canvas. That removes a large
dependency and keeps the visual language consistent.

---

## Driver-readable UI decisions

The tip in the assignment — *think of the driver behind the wheel* — drove these:

* **Hierarchy by size.** Speed is the largest element on screen (116 sp). Battery
  and temperature are secondary (64 sp). Section labels are small, wide-spaced
  and muted so they never compete with the values.
* **Contrast.** Pure white numbers on a near-black surface (`#090C10`). The theme
  is **dark only** and **dynamic colour is disabled**: Material You would recolour
  the dashboard from the user's wallpaper, and the meaning of green must never
  depend on a wallpaper.
* **Colour with a second signal.** Green/red status is always accompanied by a
  word (`CONNECTED` / `OFFLINE`, `DRIVING` / `PARKED`), so the state survives
  colour-blindness.
* **Redundant encoding.** Battery is a number, a filling shape, *and* a bar — the
  rough level is readable without reading.
* **Minimal distraction.** One interactive control on the whole screen
  (play/pause). No odometer, phone, settings, RPM or warning lights: the app
  shows what the backend actually knows and nothing else.
* **Large touch target.** The play/pause button is 88 dp, far above the 48 dp
  minimum, because a driver aims badly in a moving vehicle.
* **Motion that helps.** Values are animated between the one-second polls, so
  the gauge sweeps like an instrument instead of stepping once per second.

### Layout

Landscape, two columns. Left (44 %): the speed gauge, with battery and outside
temperature beneath it. Right (56 %): navigation, then media. The split follows
where attention is actually spent — glanced-at values on the left, read panels
on the right — rather than how the data is grouped in the API.

---

## Play/pause behaviour

The button **does not toggle a local flag.** It sends `media_action: "toggle"`
to `POST /api/dashboard/sync`; the backend owns the media state and the change
comes back with the next poll. This keeps a single source of truth and is what
makes the round trip demonstrable. That POST is also what causes the backend to
write a timestamped entry into its log file.

## Loading / Connected / Offline

| State | When | What is shown |
|---|---|---|
| `Loading` | Before the first successful response | Spinner and the backend URL |
| `Connected` | Poll succeeded | The dashboard; the top bar shows a green `CONNECTED` |
| `Offline` | Poll failed | If values were received earlier, the dashboard stays on screen with a red `OFFLINE` badge — these are the last values *actually received*, never invented. If nothing was ever received, a panel shows the URL and the exact `uvicorn` command to start the backend. |

---

## Setup

### Requirements

* Android Studio (verified with **2026.1.3**, bundled JBR = JDK 25)
* Android SDK Platform **37**
* An emulator system image for the tablet

### Toolchain versions actually used

| Component | Version |
|---|---|
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.5.0 |
| Kotlin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| Retrofit | 2.12.0 |
| OkHttp | 4.12.0 (pinned — see below) |
| kotlinx-serialization-json | 1.9.0 |
| AndroidX Lifecycle | 2.9.4 |

> **Note on AGP 9:** this project has **no `org.jetbrains.kotlin.android`
> plugin** — AGP 9 has built-in Kotlin support. Adding that plugin, as older
> tutorials suggest, will break the build.

> **Note on OkHttp:** Retrofit 2.12 still declares OkHttp **3.14.9** (the
> pre-Kotlin release) as its floor, whose `MediaType` has no
> `String.toMediaType()` extension. OkHttp 4.12.0 is therefore pinned
> explicitly. This was found by reading the actual dependency tree after a real
> compile error.

### Emulator

Create a **Pixel Tablet** AVD in Android Studio's Device Manager and download a
system image. Runtime verification for this project was done on a **Pixel Tablet
with the Google Play API 35 x86_64 image**.

---

## Build and run

**1. Start the backend first** — it must bind all interfaces, not just localhost:

```powershell
cd "D:\Development Task\VehicleDashboardAssignment\python-backend"
.\.venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000
```

**2. Run the app** — in Android Studio, select the Pixel Tablet AVD and press
**Run ▶**.

Command line alternative (from `android-app/`):

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app\build\outputs\apk\debug\app-debug.apk`.

### Why `10.0.2.2` and not `127.0.0.1`

Inside the emulator, `127.0.0.1` means *the emulated device itself*, so a server
running on your PC is unreachable at that address. The emulator provides a
special alias, **`10.0.2.2`**, which is routed to the host machine's loopback.
The base URL is therefore `http://10.0.2.2:8000/`, defined once in
`NetworkModule.BASE_URL`. A physical device on the same Wi-Fi would use the PC's
LAN IP instead.

Android also blocks plain HTTP by default. Rather than enabling cleartext
globally with `usesCleartextTraffic="true"`, `res/xml/network_security_config.xml`
permits it **only** for `10.0.2.2` and `localhost`, so the exception cannot
quietly weaken a real deployment.

---

## Testing and verified results

| What | Status |
|---|---|
| `gradlew assembleDebug` | **BUILD SUCCESSFUL** — compiled |
| Install and run on Pixel Tablet API 35 | **Runtime verified** |
| Landscape layout renders without clipping | **Runtime verified** |
| `GET /api/dashboard` polled, returns 200 | **Runtime verified** |
| Speed, battery, temperature, status, media, navigation update dynamically | **Runtime verified** |
| Play/pause triggers `POST /api/dashboard/sync`, returns 200 | **Runtime verified** |
| Timestamped `RECEIVED` entries written to `dashboard.log` | **Runtime verified** |
| No brand references anywhere | **Runtime verified** |

**There are no automated Android tests.** The two files Android Studio generated
(`ExampleUnitTest`, `ExampleInstrumentedTest`) are untouched template stubs and
test nothing of this app. Verification of the Android side was manual and is
described above. Automated tests exist only for the backend (16 passing).

---

## Known limitations

* **No automated Android tests** (see above). Unit tests for the DTO→domain
  mapping and a Compose UI test for the three states would be the first
  additions.
* **No periodic synchronisation.** `POST /sync` fires only on play/pause, so the
  backend log grows on interaction rather than continuously. This was a
  deliberate scope decision.
* **Media is limited by the API.** The backend sends no elapsed time, no track
  duration and no artist, so none are displayed — showing invented values would
  look better and be wrong. Skip forward/back buttons are omitted because the
  backend accepts no such action, and dead buttons are worse than absent ones.
* **Route progress is inferred.** The API gives remaining distance but not total
  trip length, so `NavigationCard` normalises against the largest distance seen
  in the session. Accurate from the first parked reading onward.
* **Landscape only.** From API 36, Android may ignore `screenOrientation` on
  large screens; the layout is designed for landscape and will be cramped in
  portrait. No portrait variant was implemented.
* **No offline data source.** By design — the backend is the only simulation.
* **Fixed base URL**, no settings screen. Changing it means editing
  `NetworkModule`.
* **Hard-coded UI strings**, no localisation or string resources.
* **Default launcher icon** from the project template.
* No authentication, no TLS, no persistence, no accessibility audit.

---

## AI use (Android)

AI (Claude Code) assisted with the architecture discussion, generated the Kotlin
and Compose code together with its explanatory comments, suggested the debugging
path for the build failure, supported the UI implementation, and structured this
document. I chose and approved the scope and the visual design, created the
Android Studio project with the New Project wizard, installed and configured the
Pixel Tablet emulator, ran the Gradle builds, installed and operated the app on
the emulator, tested play/pause, confirmed the 200 responses for both endpoints,
inspected the timestamped log entries, and identified the destination-text
overlap defect and requested its correction. Two real problems were found and
fixed this way rather than assumed: the OkHttp 3.14.9 transitive dependency, and
a missing `lineHeight` that made a two-line destination overlap on the real
tablet layout.
