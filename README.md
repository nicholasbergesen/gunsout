# Gunsout

Personal Android app for diet, workout, and tracking. Built specifically for Nicholas to support a body-recomposition program (~100 kg towards ~80 kg, preserving muscle, recovering from a right knee injury, climbing background).

## Features

**Workout**
- 4-day Upper/Lower split seeded as the active program, with form notes, default rest, supersets, and the pull-up "5x2-3" protocol as a first-class scheme.
- Flexible scheduling: the "Today" screen routes by rotation pointer (not calendar date), with one-tap options to continue, pick a different day, mark rest, or skip the next day.
- Sessions: per-set logging (weight, reps, RPE), previous-best display, progression suggestions (+2.5 / +5 kg on hit, -5% on miss, RPE-based bumps, pull-up graduation, baseline-week suppression).
- **Program builder**: list, activate, duplicate, rename, delete, full day + exercise + scheme editor with sets/reps/rest/RPE/superset/protocol.
- **Exercise library**: browse by muscle group, create custom exercises, edit form notes and default rest.

**Diet**
- Daily dashboard against the active meal plan's kcal / protein / carbs / fat targets, with one-tap template logging.
- Seeded 2,200 kcal cut plan with editable smoothie template.
- **Meal plan builder**: list, activate, duplicate, edit targets, manage templates with macro-source toggle (from-ingredients or manual). Live macro totals when building from ingredients.
- **Ingredient library**: full CRUD with per-100g macros, default unit (G/ML/PIECE/TBSP/TSP/CUP), gramsPerUnit conversion, archive support. 8 ingredients seeded.
- **CalorieNinjas ingredient lookup**: optional one-tap pre-fill in the ingredient editor. BYO API key stored encrypted on-device. App is fully usable offline.

**Supplements**
- Creatine monohydrate 5 g/day seeded and active. One-tap "Mark taken" on the Diet dashboard, daily idempotency, taken-state visible.

**Body**
- Composition log with weight required; body fat %, muscle mass kg, water %, bone mass kg, visceral fat rating all optional.
- Weight trend chart with goal line.

**Settings**
- Body weight, goal weight, knee-injury caution, baseline-week toggle.
- Encrypted CalorieNinjas API key field with a "Library" deep-link.

## Tech

- Kotlin 2.1.0, Jetpack Compose, Material 3, MVVM with Hilt, Room 2.6.1, DataStore, EncryptedSharedPreferences, OkHttp + kotlinx-serialization, Navigation-Compose. AGP 8.7.3. minSdk 26, target 36.
- Pure-Kotlin domain modules under `com.gunsout.domain`: `ScheduleResolver`, `MacroCalculator`, `ProgressionEngine`. Fully unit-tested.

## Build locally

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

### CalorieNinjas API key

Put your key into `local.properties` (gitignored):
```
CALORIE_NINJAS_API_KEY=your_key_here
```
or set the env var `CALORIE_NINJAS_API_KEY` before running the build. The app's seeder copies the value into encrypted on-device storage on first run. The Settings screen also lets you set or change the key manually.

For CI builds, the key is read from the repository's `CALORIE_NINJAS_API_KEY` GitHub Actions secret. **Note**: any APK produced by CI will contain the key as a string field in `BuildConfig`; consider rotating the key if you redistribute the APK.

## Releases

Every push to `main` builds a debug-signed APK and publishes it to the [`latest`](https://github.com/nicholasbergesen/gunsout/releases/tag/latest) pre-release. Sideload onto your phone via system settings.

## Tests

```
./gradlew :app:testDebugUnitTest
```

Covers the progression engine, schedule resolver, macro calculator, and CalorieNinjas response parsing.

## Notes

- Single-user, no analytics, no account, no cloud sync. All data lives in Room on-device. Network is only used for the optional CalorieNinjas ingredient lookup.
- Default plan and program values are tuned to Nicholas's stated targets (100 kg current, 80 kg goal, 2200 kcal, 160 g protein, knee caution on). Everything is editable.
