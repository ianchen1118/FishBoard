# FishBoard

FishBoard is an Android prototype for fast, offline fisheries data collection. It models a field workflow in which each fish receives a traceable ID, placeholder species and length estimates, reviewer corrections, photo metadata, and an exportable record.

The long-term goal is to pair the app with a fixed fish board and camera setup so that fish can be detected, photographed, identified, measured, and recorded with minimal manual input.

> **Project status:** The end-to-end data workflow is functional, but scanning is currently simulated. Camera capture, calibration, and AI/computer-vision inference are planned features.

## Why FishBoard?

Traditional fisheries sampling can require repeatedly photographing fish, measuring them, identifying species, and entering data by hand. FishBoard is designed around a rapid-scan workflow:

```text
Place fish on board
    -> detect and capture
    -> estimate species and length
    -> save photo and metadata
    -> review or correct results
    -> export the field session
```

The current prototype builds and validates the data layer, review workflow, and export format before real camera and model integration.

## Current Features

- Create field sessions using location and device codes
- Generate readable, session-scoped fish IDs
- Simulate rapid scans with placeholder species, length, and confidence values
- Persist sessions and fish records locally with Room
- Review and correct species and length values
- Add notes and mark records as reviewed
- Export new records or all records as a ZIP package
- Include available photos and report unavailable files in `missing_images.csv`
- Share completed export packages through Android's share sheet
- Track exported records and delete them after confirmation

### Record identifiers

FishBoard uses both a UUID for internal storage and a readable ID for field use.

```text
Session:  YYYYMMDD-LOCATION-DEVICE-S###
Fish:     YYYYMMDD-LOCATION-DEVICE-S###-F######
```

Example:

```text
20260701-PIER01-D03-S001-F000001
```

## Export Format

A generated export is structured as:

```text
FishBoardExport_YYYY_MM_DD_HHMMSS.zip
├── records.csv
├── missing_images.csv          # included only when a photo is unavailable
└── images/
    └── SESSION_ID/
        └── SESSION_ID-F000001.jpg
```

Each CSV record can preserve:

- Internal and display IDs
- Session and sequence information
- Timestamp
- Predicted species and confidence
- Corrected species
- Predicted length and confidence
- Corrected length
- Review and export status
- Photo filename and relative path
- Notes

## Tech Stack

- Kotlin
- Jetpack Compose and Material 3
- Room
- Kotlin coroutines and Flow
- Gradle Kotlin DSL
- JUnit

The app supports Android 6.0 (API 23) and later and currently compiles against API 37.

## Architecture

```text
Compose UI
    ↓
FishBoardRepository
    ↓
Room DAO
    ↓
Local SQLite database

Fish records
    ↓
CSV serialization + linked image files
    ↓
Shareable ZIP export
```

Important source files:

```text
app/src/main/java/com/example/androidtest/
├── MainActivity.kt          # Compose screens and navigation
├── FishModels.kt            # Domain models
├── FishBoardFactories.kt    # Session and simulated-record creation
├── FishBoardDatabase.kt     # Room entities, DAO, and repository
└── FishBoardExport.kt       # CSV, ZIP, and sharing logic
```

A more detailed product and technical direction is available in [docs/project-brief.md](docs/project-brief.md).

## Getting Started

### Prerequisites

- Android Studio
- Android SDK 37
- An Android device or emulator running API 23 or later

### Build and run

1. Clone the repository:

   ```bash
   git clone https://github.com/ianchen1118/FishBoard.git
   cd FishBoard
   ```

2. Open the project in Android Studio and allow Gradle to sync.

3. Select a device or emulator and run the `app` configuration.

You can also build from the command line:

```bash
# macOS or Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

Run the unit tests with:

```bash
# macOS or Linux
./gradlew test

# Windows
gradlew.bat test
```

## Trying the Prototype

1. Tap **Start New Session**.
2. Enter a location code and device code.
3. Tap **Simulate Fish Scan** to generate records.
4. Open **Review Records** to inspect and correct results.
5. Open **Export Data** to create and share a ZIP package.

Because camera capture is not implemented yet, simulated records do not contain real image files. Their missing photos are listed explicitly in `missing_images.csv` instead of being silently omitted.

## Roadmap

- Integrate CameraX for real image capture
- Add board calibration and pixel-to-millimeter conversion
- Support manual head/tail point selection
- Detect fish presence and automate capture
- Add fish segmentation and length estimation
- Integrate species classification with confidence scores
- Preserve model and calibration version metadata
- Improve session recovery and field-ready UI
- Add broader unit and instrumentation test coverage

## Design Principles

- Optimize for rapid field sampling
- Keep the system usable offline
- Preserve raw observations for later review
- Treat manual entry as correction and fallback
- Keep photos separate from metadata while maintaining stable links
- Make missing or uncertain data visible
