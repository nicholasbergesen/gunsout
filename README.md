# Gunsout

Personal Android app for diet, workout, and body tracking. Now multi-user via Google Sign-In, with each Google account on the device getting its own fully isolated local data set. No cloud sync. Built for body-recomposition programs (Nicholas: ~100 kg towards ~80 kg, preserving muscle, recovering from a right knee injury, climbing background).

## Multi-user offline

Each Google account that signs in on the device gets its own copy of the program, exercise library, supplements, body log, food entries, and DataStore profile. Signing in as another Google account on the same phone starts fresh and never sees the previous user's rows. There is no cloud component and no cross-user sharing. Reminders are scoped to the currently signed-in user, cancelled on sign-out, and re-armed after sign-in or boot.

Identity uses the Google account `sub` (`GoogleIdTokenCredential.uniqueId`). The auth flow uses Android's Credential Manager; the bottom of `SettingsScreen` carries the signed-in user's email/display name plus a Sign out button.

## Features

**Workout**
- Ten seeded program templates per user, including upper/lower, runner, cyclist, male strength, female strength, beginner full body, hypertrophy PPL, recomposition, and climbing-focused plans.
- Flexible scheduling: the "Today" screen routes by rotation pointer (not calendar date), with one-tap options to continue, pick a different day, mark rest, or skip the next day. A marked rest day does not disturb the rotation.
- Sessions: per-set logging (weight, reps, RPE, optional warmup checkbox), previous-best display, in-session swap to an alternate exercise (with optional save-to-program), automatic foreground-service rest timer between sets, progression suggestions (+2.5 / +5 kg on hit, -5% on miss, RPE-based bumps, pull-up graduation, baseline-week suppression).
- Program builder: list, activate, duplicate, rename, delete, full day + exercise + scheme editor with sets/reps/rest/RPE/superset/protocol.
- Exercise library: browse by muscle group, create custom exercises, edit form notes and default rest. Each exercise edit screen also shows a top-working-set weight history chart.
- Session history: dedicated screen listing completed sessions chronologically; tap through for a per-set summary. Settings can export completed workout and rest sessions as lightweight JSON for external analysis.

**Diet (simplified)**
- Meals are entered directly as name + macros + kcal (e.g. "burger, 30P / 40C / 25F / 500 kcal"). The ingredient catalog and CalorieNinjas lookup are gone; nothing is composed from raw ingredients anymore.
- Daily targets are computed locally from the user profile using Mifflin-St Jeor BMR + an activity multiplier + a goal delta (CUT / MAINTAIN / BULK). The Settings screen lets you edit age, sex, height, activity level, and goal, then either accept the suggested daily targets or override any of kcal / protein / carbs / fat. A "Reset to suggested" button clears all four overrides.
- Quick-log templates with a long-press fraction picker (0.5x / 1x / 1.5x / 2x).
- An "Add meal" bottom sheet logs a one-off meal in five fields with an optional "Save as template" checkbox (defaults off).
- Editable food entries with confirmation-gated delete and Snackbar undo.
- Date rolls over at midnight automatically and on app resume.

**Supplements**
- Creatine monohydrate 5 g/day seeded per user and active. One-tap "Mark taken" on the Diet dashboard, daily idempotency, taken-state visible.
- Daily reminder time per supplement (Material 3 time picker) backed by `AlarmManager.setInexactRepeating`. Reminders persist across reboot via a boot receiver and are scoped to the currently signed-in user.

**Body**
- Composition log with weight required; body fat %, muscle mass kg, water liters, and visceral fat rating all optional. Upsert by date so a same-day update merges rather than duplicating.
- InBody import supports result-sheet QR codes and InBody app CSV exports, parses locally on device, and imports supported InBody 270 measurements without a network request.
- Multi-series trend chart with toggles for weight / body fat % / muscle / water liters. The y-axis starts at zero, gridlines aid readability, and the weight series shows a goal line.
- Latest card sources directly from the most recent log row.
- Auto-adjust kcal target: reads recent weight trend (linear regression over the last 14 days, requires 4+ logs spanning 7+ days) and suggests a 150 kcal nudge that is applied as a manual kcal override on the daily target.
- Baseline week auto-derives from `Program.createdAt`: the first 7 days are baseline. The Settings toggle remains as a manual force-off override.

**Settings**
- Account card (email + display name + Sign out) at the top.
- Appearance card with six per-account visual themes: Gunmetal Crimson, Clean Light Minimal, Neo-Brutalist, Glassmorphism, Soft Pastel, and Vibrant Gradient.
- Body weight, goal weight, height, age, sex, activity level, goal type, knee-injury caution, baseline-week override.
- Daily Targets card with the four suggestion fields, override editing, and Reset to suggested.
- JSON backup: export and import all of the signed-in user's data (including the DataStore profile, selected theme, and any macro overrides) via the system file picker (SAF). Import is destructive for the current user only, wrapped in a transaction, and gated by a confirmation dialog. Schema version is 7; the importer also accepts schemaVersion 1 through 6 (those legacy files are folded into the current user's data).

## Visual design

Theme selection replaces the old light/dark/system toggle. Each signed-in account stores one fixed visual style: Gunmetal Crimson, Clean Light Minimal, Neo-Brutalist, Glassmorphism, Soft Pastel, or Vibrant Gradient. The style controls Material 3 colors, backdrop treatment, system bar contrast, and corner shapes. Theme tokens live in `app/src/main/java/com/nicholasbergesen/gunsout/ui/theme/`.

## Tech

- Kotlin 2.x, Jetpack Compose, Material 3, MVVM with Hilt, Room 2.8.x (with versioned migrations and a checked-in schema export), DataStore, OkHttp + kotlinx-serialization, Navigation-Compose. AGP 9.x. minSdk 26, compileSdk 37, target 36.
- Pure-Kotlin domain modules under `com.nicholasbergesen.gunsout.domain`: `ScheduleResolver`, `MacroTargetCalculator`, `ProgressionEngine`, `KcalTrendAnalyzer`. Fully unit-tested.
- Foreground service `RestTimerService` (SPECIAL_USE foreground service type) for the rest timer between sets so it survives screen sleep.
- Credential Manager + Google Identity for sign-in.

## Build locally

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

### Google Sign-In setup

You will need:

1. A **Web OAuth 2.0 Client ID** from Google Cloud Console (Credentials -> Create Credentials -> OAuth client ID -> Web application). Paste it as `GOOGLE_WEB_CLIENT_ID` into `local.properties` (gitignored), or set it as an env var. The build will fail if this value is empty.
2. **Android OAuth Client IDs** for both `com.nicholasbergesen.gunsout` and `com.nicholasbergesen.gunsout.debug`. Each one binds the **SHA-1** of the keystore that signs the APK to the package name. For the debug build, use the SHA-1 of `app/gunsout-debug.keystore`. These Android client IDs are not referenced in code; they let Credential Manager honor the Web Client ID on signed APKs.

```
GOOGLE_WEB_CLIENT_ID=<your-web-client-id>.apps.googleusercontent.com
```

For CI builds, configure the `GOOGLE_WEB_CLIENT_ID` repo secret.

## Releases

Every push to `main` builds a debug-signed APK and publishes it to the [`latest`](https://github.com/nicholasbergesen/gunsout/releases/tag/latest) pre-release. Sideload onto your phone via system settings.

### Upgrading from a pre-multi-user build

The upgrade from any v1 or v2 single-user build wipes the on-device Room database and re-seeds defaults under the first Google account that signs in. The pre-multi-user build had no concept of user identity, so it was not possible to assign legacy rows to a meaningful account. Export your data via Settings -> Export JSON before upgrading if you want to keep it; you can re-import the file after signing in. The destructive fallback is gated to versions 1, 2, and 3 only, so any later v4 -> v5 schema bump will fail loudly if a migration is missing instead of silently wiping again.

The ingredient catalog and `MealPlan` entity are gone. Legacy backup files (schemaVersion 1 or 2) import cleanly because the dropped fields (`mealPlans`, `ingredients`, `mealTemplateIngredients`, `macroSource`, `mealPlanId`) are skipped silently. Meal-plan-based daily targets do not carry forward into the new manual-override fields; users who relied on a meal plan need to set kcal and macro overrides manually in Settings after importing. `FROM_INGREDIENTS` template macros are not recomputed from their (now-gone) ingredient joins; they import as whatever kcal/macros were already written to the template row at export time.

### Sideload-friendly updates

For Android to install a new APK in-place over the previous one (preserving the on-device Room DB and per-user prefs), every build must:

1. Share the **same signing certificate**.
2. Use an **increasing `versionCode`**.
3. Use the **same `applicationId`** (debug builds install as `com.nicholasbergesen.gunsout.debug`).

This repo handles (3) automatically and CI handles (2) by deriving `versionCode` and `versionName` from the publish job timestamp relative to 2024-01-01 UTC. For (1), the build expects a checked-in debug keystore at `app/gunsout-debug.keystore`.

**One-time setup** (run once per clone, then commit the keystore):

```
./gradlew :app:generateDebugKeystore
git add app/gunsout-debug.keystore
git commit -m "Add stable debug keystore for sideload updates"
git push
```

After that, every CI build is signed with the same key, has a strictly higher `versionCode`, and installs on top of the previous APK without uninstalling.

The keystore is a debug keystore (not a release signing key), uses the conventional `android` / `android` / `androiddebugkey` triple, and is safe to commit.

## Tests

```
./gradlew :app:testDebugUnitTest
```

Unit tests cover the progression engine, schedule resolver (including the marked-rest-day rotation edge case), the new `MacroTargetCalculator` (canonical profiles, null and out-of-range inputs, activity and goal deltas, kcal floor at 1200, override-merge semantics), kcal-trend analyzer (linear regression robust to a single noisy weigh-in), baseline-week resolver (first 7 days from program activation), theme token mapping, and backup theme-style compatibility.

## Notes

- Multi-user offline. All data lives in Room on-device, partitioned by Google account `sub`. There is no analytics, no server, and no cloud sync.
- Auth and per-user DataStore files are excluded from Android Auto Backup (`backup_rules.xml` / `data_extraction_rules.xml`).
- Default plan and program values for the first user are tuned to the original target profile (100 kg current, 80 kg goal, 160 g protein, knee caution on). Everything is editable.
