# Gunsout

Personal Android app for diet, workout, and tracking. Built specifically for Nicholas to support a body-recomposition program (~100 kg towards ~80 kg, preserving muscle, recovering from a right knee injury, climbing background).

## Features in this build

- **4-day Upper/Lower split** seeded as the active program, with form notes, default rest times, supersets, and the pull-up "5x2-3" protocol as a first-class scheme.
- **Flexible scheduling**: the "Today" screen routes by rotation (not date), with one-tap options to continue, pick a different day, mark rest, or skip the next day.
- **Workout sessions**: per-set logging (weight, reps, RPE), previous-best display, progression suggestions (`+2.5 / +5 kg` on hit, `-5%` on miss, RPE-based bumps, pull-up graduation). Baseline-week banner suppresses suggestions until you flip the toggle in Settings.
- **Diet dashboard** with active meal plan (2,200 kcal / 160 g protein default, editable later), kcal / protein / carbs / fat totals vs targets, quick-log of seeded smoothie template.
- **Supplements**: creatine monohydrate 5 g/day seeded and active. Tap "Mark taken" on the diet screen.
- **Body composition log**: weight required, body fat %, muscle mass kg, water %, bone mass kg, visceral fat rating all optional. Includes a simple weight trend chart with a goal line.
- **Settings**: edit current and goal weight, toggle knee-injury caution, toggle baseline week.

## Skipped in this initial cut (tracked for follow-ups)

- Program builder UI (you can still train against the seeded 4-day split).
- Meal plan builder UI and ingredient library UI (seeded templates available; no in-app CRUD yet).
- CalorieNinjas ingredient lookup (network layer not wired).
- Foreground-service rest timer.
- Polish, accessibility audit, unit tests.

## Tech

- Kotlin 2.1.0, Jetpack Compose, Material 3, MVVM with Hilt, Room 2.6.1, DataStore, Navigation-Compose. AGP 8.7.3. minSdk 26, target 36.

## Build locally

```
./gradlew :app:assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

Open the project in Android Studio for emulator runs and debugging.

## Releases

Every push to `main` builds a debug-signed APK and publishes it to the [`latest`](https://github.com/nicholasbergesen/gunsout/releases/tag/latest) pre-release. Sideload onto your phone via system settings.

## Notes

- This is a personal app: no analytics, no account, no cloud sync, no network calls in the current build. All data lives in Room on-device.
- Default plan and program values are tuned to Nicholas's stated targets (100 kg current, 80 kg goal, 2200 kcal, 160 g protein, knee caution on). Everything is editable in Settings or via the seeded entities.
