# Gunsout

Personal Android app for workout, nutrition, creatine, and body tracking. Now multi-user via Google Sign-In, with each Google account on the device getting its own fully isolated local data set. No cloud sync. Built for body-recomposition programs (Nicholas: ~100 kg towards ~80 kg, preserving muscle, recovering from a right knee injury, climbing background).

## Multi-user offline

Each Google account that signs in on the device gets its own copy of the program, exercise library, protein log, creatine settings and checks, body log, and DataStore profile. Signing in as another Google account on the same phone starts fresh and never sees the previous user's rows. There is no cloud component and no cross-user sharing. Reminders are scoped to the currently signed-in user, cancelled on sign-out, and re-armed after sign-in or boot.

Identity uses the Google account `sub` (`GoogleIdTokenCredential.uniqueId`). The auth flow uses Android's Credential Manager; the bottom of `SettingsScreen` carries the signed-in user's email/display name plus a Sign out button.

## Features

**Workout**
- Ten seeded program templates per user, including upper/lower, runner, cyclist, male strength, female strength, beginner full body, hypertrophy PPL, recomposition, and climbing-focused plans.
- Flexible scheduling: the "Today" screen routes by rotation pointer (not calendar date), with one-tap options to continue, pick a different day, mark rest, or skip the next day. A marked rest day does not disturb the rotation.
- Sessions: per-set logging (weight, reps, RPE, optional warmup checkbox), previous-best display, in-session swap to an alternate exercise (with optional save-to-program), automatic foreground-service rest timer between sets, progression suggestions (+2.5 / +5 kg on hit, -5% on miss, RPE-based bumps, pull-up graduation, baseline-week suppression).
- Program builder: list, activate, duplicate, rename, delete, full day + exercise + scheme editor with sets/reps/rest/RPE/superset/protocol.
- Exercise library: browse by muscle group, create custom exercises, edit form notes and default rest. Each exercise edit screen also shows a top-working-set weight history chart.
- Session history: dedicated screen listing completed sessions chronologically; tap through for a per-set summary. Settings can export completed workout and rest sessions as lightweight JSON for external analysis.

**Nutrition**
- Today's protein card tracks positive whole grams against a recommended target of 2.0 g/kg goal body weight. A protein-only manual override remains available in Settings.
- Protein entries require grams and accept an optional meal label. Today's entries are newest-first, editable, and immediately deletable with Snackbar undo.
- A rolling history chart offers 1-week, 1-month, and 1-year ranges. Daily bars keep missing days as gaps; yearly bars show average grams across logged days. Per-day target snapshots keep old results tied to the target that applied at the time.
- Creatine is a reversible daily check with a configurable whole-gram dose (5 g default) and reminder time. The reminder persists across reboot and is suppressed if today's check already exists.
- Calorie guidance remains separate from intake logging: Settings and Body retain the Mifflin-St Jeor target and trend-based adjustment, but Nutrition does not ask for or total consumed calories.
- Date rolls over at midnight automatically and on app resume.

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
- Guidance Targets card with independent kcal and protein suggestions, override editing, and Reset to suggested.
- JSON backup: export and import all of the signed-in user's data (including protein history, historical targets, creatine, profile, theme, and kcal/protein overrides) via the system file picker (SAF). Import is destructive for the current user only, wrapped in a transaction, and gated by a confirmation dialog. Schema version is 8; the importer also accepts schemaVersion 1 through 7.

## Visual design

Theme selection replaces the old light/dark/system toggle. Each signed-in account stores one fixed visual style: Gunmetal Crimson, Clean Light Minimal, Neo-Brutalist, Glassmorphism, Soft Pastel, or Vibrant Gradient. The style controls Material 3 colors, backdrop treatment, system bar contrast, and corner shapes. Theme tokens live in `app/src/main/java/com/nicholasbergesen/gunsout/ui/theme/`.

## Tech

- Kotlin 2.x, Jetpack Compose, Material 3, MVVM with Hilt, Room 2.8.x (with versioned migrations and a checked-in schema export), DataStore, OkHttp + kotlinx-serialization, Navigation-Compose. AGP 9.x. minSdk 26, compileSdk 37, target 36.
- Pure-Kotlin domain modules under `com.nicholasbergesen.gunsout.domain`: `ScheduleResolver`, `CalorieTargetCalculator`, `ProteinTargetCalculator`, `ProteinHistory`, `ProgressionEngine`, and `KcalTrendAnalyzer`. Fully unit-tested.
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

The ingredient catalog and `MealPlan` entity are gone. Legacy backup files (schemaVersion 1 or 2) import cleanly because dropped fields (`mealPlans`, `ingredients`, `mealTemplateIngredients`, `macroSource`, `mealPlanId`) are skipped. Meal-plan targets do not carry forward; set kcal or protein guidance overrides manually after importing. Legacy food rows use the protein value already serialized in each row rather than recomputing anything from removed ingredient joins.

### Upgrading to protein-first nutrition

Room v7 upgrades non-destructively to v8. Existing food entries with positive protein become whole-gram protein entries while preserving date, name, and logged time. Meal templates and non-creatine supplements are discarded. The seeded `creatine_mono` dose, reminder, and checks migrate; historical protein targets remain unknown because earlier versions did not store target snapshots.

Backup imports use the same conversion for schemaVersion 1 through 7. Kcal and protein overrides carry forward independently; legacy carb and fat overrides are ignored.

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

Unit tests cover the progression engine, schedule resolver (including the marked-rest-day rotation edge case), independent calorie/protein target calculations, protein history ranges and missing-data semantics, protein and creatine repositories, kcal-trend analysis, baseline-week resolution, theme token mapping, and backup compatibility. `Migration7To8Test` runs under Robolectric with AndroidX's bundled SQLite driver, executes the real migration, and validates the result against Room's exported v8 schema without requiring an emulator.

## Notes

- Multi-user offline. All data lives in Room on-device, partitioned by Google account `sub`. There is no analytics, no server, and no cloud sync.
- Auth and per-user DataStore files are excluded from Android Auto Backup (`backup_rules.xml` / `data_extraction_rules.xml`).
- Default plan and program values for the first user are tuned to the original target profile (100 kg current, 80 kg goal, 160 g protein, knee caution on). Everything is editable.
