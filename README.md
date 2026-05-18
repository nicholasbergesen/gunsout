# Gunsout

Personal Android app for diet, workout, and tracking. Built specifically for Nicholas to support a body-recomposition program (~100 kg towards ~80 kg, preserving muscle, recovering from a right knee injury, climbing background).

## Features

**Workout**
- 4-day Upper/Lower split seeded as the active program, with form notes, default rest, supersets, and the pull-up "5x2-3" protocol as a first-class scheme.
- Flexible scheduling: the "Today" screen routes by rotation pointer (not calendar date), with one-tap options to continue, pick a different day, mark rest, or skip the next day. A marked rest day does *not* disturb the rotation.
- Sessions: per-set logging (weight, reps, RPE), previous-best display, in-session swap to an alternate exercise (with optional save-to-program), automatic foreground-service rest timer between sets, progression suggestions (+2.5 / +5 kg on hit, -5% on miss, RPE-based bumps, pull-up graduation, baseline-week suppression).
- Program builder: list, activate, duplicate, rename, delete, full day + exercise + scheme editor with sets/reps/rest/RPE/superset/protocol.
- Exercise library: browse by muscle group, create custom exercises, edit form notes and default rest. Each exercise edit screen also shows a top-working-set weight history chart.

**Diet**
- Daily dashboard against the active meal plan's kcal / protein / carbs / fat targets, with one-tap template logging.
- Seeded 2,200 kcal cut plan with editable smoothie template.
- Meal plan builder: list, activate, duplicate, edit targets, manage templates with macro-source toggle (from-ingredients or manual). Live macro totals when building from ingredients.
- Ingredient library: full CRUD with per-100g macros, default unit (G/ML/PIECE/TBSP/TSP/CUP), gramsPerUnit conversion, archive support. 8 ingredients seeded.
- CalorieNinjas ingredient lookup: optional one-tap pre-fill in the ingredient editor. BYO API key stored encrypted on-device. If the lookup would overwrite values you already typed, the app asks before applying. App is fully usable offline.
- Diet entries are editable: tap an entry to update macros or delete it.
- Date rolls over at midnight automatically and on app resume.

**Supplements**
- Creatine monohydrate 5 g/day seeded and active. One-tap "Mark taken" on the Diet dashboard, daily idempotency, taken-state visible.

**Body**
- Composition log with weight required; body fat %, muscle mass kg, water %, bone mass kg, visceral fat rating all optional.
- Weight trend chart with goal line.
- Auto-adjust kcal target: reads recent weight trend (linear rate over the last 14 days) and suggests a 150 kcal adjustment to keep you on track for your goal weight. One-tap apply changes the active meal plan's target.

**Settings**
- Body weight, goal weight, knee-injury caution, baseline-week toggle.
- Encrypted CalorieNinjas API key field with a "Library" deep-link.
- JSON backup: export and import all user data via the system file picker (SAF). Import is destructive (replaces all current data) and version-aware.

## Tech

- Kotlin 2.1.0, Jetpack Compose, Material 3, MVVM with Hilt, Room 2.6.1 (with versioned migrations and a checked-in schema export), DataStore, EncryptedSharedPreferences, OkHttp + kotlinx-serialization, Navigation-Compose. AGP 8.7.3. minSdk 26, target 36.
- Pure-Kotlin domain modules under `com.gunsout.domain`: `ScheduleResolver`, `MacroCalculator`, `ProgressionEngine`, `KcalTrendAnalyzer`. Fully unit-tested.
- Foreground service `RestTimerService` (SPECIAL_USE foreground service type) for the rest timer between sets so it survives screen sleep.

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

For CI builds, the key is read from the repository's `CALORIE_NINJAS_API_KEY` GitHub Actions secret. **Note**: any APK produced by CI contains the key as a string field in `BuildConfig`; consider rotating the key if you redistribute the APK.

## Releases

Every push to `main` builds a debug-signed APK and publishes it to the [`latest`](https://github.com/nicholasbergesen/gunsout/releases/tag/latest) pre-release. Sideload onto your phone via system settings.

### Sideload-friendly updates

For Android to install a new APK in-place over the previous one (preserving the on-device Room DB, profile, and API key), every build must:

1. Share the **same signing certificate**.
2. Use an **increasing `versionCode`**.
3. Use the **same `applicationId`** (debug builds install as `com.gunsout.debug`).

This repo handles (3) automatically and CI handles (2) by driving `versionCode` and `versionName` from `${{ github.run_number }}`. For (1), the build expects a checked-in debug keystore at `app/gunsout-debug.keystore`.

**One-time setup** (run once per clone, then commit the keystore):

```
./gradlew :app:generateDebugKeystore
git add app/gunsout-debug.keystore
git commit -m "Add stable debug keystore for sideload updates"
git push
```

After that, every CI build is signed with the same key, has a strictly higher `versionCode`, and installs on top of the previous APK without uninstalling. Tap the APK from the [`latest`](https://github.com/nicholasbergesen/gunsout/releases/tag/latest) release in your phone's browser and confirm "Update".

> The first time you adopt this on a phone that already has a CI-built copy of Gunsout from before the stable keystore landed, you must uninstall the old build once. Export your data via Settings → Export JSON first. From the next install onward, updates are in-place.

The keystore is a debug keystore (not a release signing key), uses the conventional `android` / `android` / `androiddebugkey` triple, and is safe to commit.

## Tests

```
./gradlew :app:testDebugUnitTest
```

42 unit tests covering the progression engine, schedule resolver (including the marked-rest-day rotation edge case), macro calculator, kcal-trend analyzer (linear regression robust to a single noisy weigh-in), and CalorieNinjas response parsing.

## Notes

- Single-user, no analytics, no account, no cloud sync. All data lives in Room on-device. Network is only used for the optional CalorieNinjas ingredient lookup.
- Default plan and program values are tuned to Nicholas's stated targets (100 kg current, 80 kg goal, 2200 kcal, 160 g protein, knee caution on). Everything is editable.
