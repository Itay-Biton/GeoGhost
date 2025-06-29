
# GeoGhost

**GeoGhost** is a zero-config Android GPS spoofing library.

It launches its own setup UI **before your app**, lets users choose a static or route-based mock location, and handles everything from permissions to background location services — no configuration or code required.

---

## Installation

GeoGhost is distributed via [JitPack](https://jitpack.io). To install:

1. Add JitPack to your **project-level `build.gradle`**:
```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
````

2. Add the library to your **app module**:

```gradle
dependencies {
    implementation 'com.github.Itay-Biton:GeoGhost:1.0.0'
}
```

---

## Usage

### Required: Set as Mock Location App

Before using GeoGhost, you **must set your app as the mock location provider** in Developer Options:

1. Open **Settings** → **About phone**
2. Tap **Build number** 7 times to enable Developer Options
3. Go to **Settings** → **System** → **Developer options**
4. Tap **Select mock location app**
5. Choose your app (the one that includes GeoGhost)

> **Important:** To return to normal GPS usage, go back to Developer Options and set **no mock app selected**.

---

### Default Flow (Zero Setup)

Just run your app — GeoGhost will:

1. Automatically launch a setup screen **before your main activity**
2. Let the user choose either:

   * A **static point**, or
   * A **walk/drive route** between two points
3. Start spoofing GPS and then launch your main app

No code needed to integrate.

---

### Manual Control (Optional)

GeoGhost exposes simple API functions if you want to control spoofing manually:

```kotlin
GeoGhost.start()         // Starts spoofing
GeoGhost.stop()          // Stops spoofing
GeoGhost.resetLocation() // Clears setup state (shows setup UI on next launch)
GeoGhost.isSpoofing()    // Returns true if spoofing is active
```

---

## How It Works (Internals)

### Setup UI

GeoGhost shows a full-screen setup before your app launches:

* Lets the user configure either a fixed point or a moving route
* Taps "Start App" to begin spoofing and enter your main activity

### Spoofing Service

GeoGhost uses a foreground service to:

* Simulate movement using `FusedLocationProviderClient`
* Drive along a real-world route (via [OSRM](https://project-osrm.org/))
* Vary speed based on selected mode: walk or drive

### Mock & Permission Handling

GeoGhost:

* Requests all required permissions automatically
* Validates and enables mock mode as needed
* Uses persistent state to track spoofing and setup flow

---

## Demo App

A sample demo (`GeoGhostProject`) is included, featuring:

* MapLibre-based live map with marker
* Toggle button for spoofing on/off
* Automatic centering on location

---

## Notes

* **Mock mode is active while spoofing** — for normal GPS use, disable the mock app in Developer Options.
* Use `GeoGhost.resetLocation()` if you want to re-run the setup screen manually.
* GeoGhost stores the spoofing state using `SharedPreferences` across launches.

---

## License

MIT License © \[Your Name or Organization]


