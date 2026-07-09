# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

"명언 잠금화면" (Motivation Locker) — a native Android app (Kotlin, single `app` module, no Compose) that replaces the system lock screen with a full-screen quote. Published on Google Play as `com.odom.motivationlocker`.

## Build & Run

Standard Gradle Android project; no custom scripts.

```
./gradlew assembleDebug        # debug APK
./gradlew assembleRelease      # release APK (see app/build.gradle for signing/proguard config)
./gradlew test                 # JVM unit tests (app/src/test)
./gradlew connectedAndroidTest # instrumented tests (app/src/androidTest), needs a device/emulator
./gradlew clean
```

Aside from the stock `ExampleUnitTest`/`ExampleInstrumentedTest`, `QuoteRepositoryTest` is the one real unit test class, covering `QuoteRepository.parseQuotes()`. `testOptions.unitTests.returnDefaultValues = true` is set in `app/build.gradle` — without it, any unmocked `android.jar` stub call from test code (notably `android.util.Log`) throws `RuntimeException` instead of no-oping, which is what `QuoteRepository`'s `Log.w()` calls would otherwise hit under plain JVM unit tests.

- `compileSdk`/`targetSdk` 35, `minSdk` 23, Java/Kotlin target 17.
- `viewBinding` is enabled; there is no Compose.
- Kotlin 1.9.24, `mavenCentral()` (no `jcenter()`), current AndroidX/Play Services versions (appcompat 1.7.0, core-ktx 1.13.1, play-services-ads 23.6.0, etc.) — see `TODO.md` §1 for the full list and rationale.
- The system JDK on this machine is Java 8, which is too old for AGP 8.3.2. Build with a JDK 11+ (e.g. Android Studio's bundled JBR) via `JAVA_HOME`, for example: `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug` (adjust path per machine).

## Architecture

The whole app is four cooperating pieces wired together through `SharedPreferences`, not a ViewModel/repository layer:

1. **`MainActivity`** — the settings screen (`androidx.preference` framework, `SettingPreferencesFragment` backed by `res/xml/pref.xml`). Toggling `useLockScreen` starts/stops `LockScreenService`. All preference changes (language, background/text color, text size, "show source") are written directly into a raw `SharedPreferences` file named `"SETTINGS"` (not the default preference file) via ad-hoc `setInts(context, key, value)`/`getInt(key)` helpers — colors and enum-like settings are stored as `Int`, not through the Preference framework's own persistence.
2. **`LockScreenService`** — a foreground `Service` (required on API 26+, uses `startForeground`) whose only job is to keep a `ScreenOffReceiver` registered for `ACTION_SCREEN_ON` for the life of the app.
3. **`ScreenOffReceiver`** — on `ACTION_SCREEN_ON`, launches `MotivationLockerActivity` on top of everything (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`). This is the actual "lock screen replacement" moment.
4. **`MotivationLockerActivity`** — reads the same `"SETTINGS"` SharedPreferences, loads a random quote from `assets/English.json` or `assets/korean.json` (chosen by the `language` int: `0`=English, `1`=Korean), applies background/text color/size, and dismisses itself on a big-enough swipe gesture (distance-squared threshold against `MotionEvent` down/move deltas). On API 27+ it uses `setShowWhenLocked` + `requestDismissKeyguard`; below that, legacy `WindowManager` flags (`FLAG_SHOW_WHEN_LOCKED`/`FLAG_DISMISS_KEYGUARD`). Both branches only request keyguard dismissal when `KeyguardManager.isKeyguardSecure()` is `false` — on PIN/pattern/password-protected devices the dismiss request is skipped so a swipe just `finish()`es the activity and the system lock screen appears normally, instead of the quote overlapping the PIN entry UI.

### Daily notification

A separate, optional path from the lock-screen replacement above: `pref.xml`'s `dailyNotificationEnabled` switch (default off) schedules `DailyNotificationScheduler` (a `WorkManager` `PeriodicWorkRequest`, 24h interval, `initialDelay` computed to land on the next 10:00 — the time is hardcoded, there is no time-picker UI) which runs `QuoteNotificationWorker`. The worker re-reads the same `"SETTINGS"` `language` value, calls `QuoteRepository.getRandomQuote()`, and posts a `BigTextStyle` notification on its own channel (`IMPORTANCE_DEFAULT` — distinct from `LockScreenService`'s silent `IMPORTANCE_NONE` channel, since this one needs to actually alert). `SettingPreferencesFragment`'s listener for this switch is the only place that requests the Android 13+ `POST_NOTIFICATIONS` runtime permission (contextual, at toggle-on time); `MainActivity.checkPermission()` no longer requests it unconditionally at launch.

Quote assets (`app/src/main/assets/English.json`, `korean.json`) are flat JSON arrays of `{ "id", "quote", "writer" }` objects, parsed with `org.json` (no Moshi/Gson). Loading/parsing/caching is centralized in `QuoteRepository` (an `object` singleton, no DI framework in this app) — `QuoteRepository.getRandomQuote(context, language)` returns a `Quote` data class instance and caches the parsed list per language after the first read. `MotivationLockerActivity.setLanguage()` calls this rather than duplicating JSON parsing per language.

`QuoteRepository` is hardened for future data changes (large hand-curated datasets, community-contributed language files) rather than trusting well-formed input: `parseQuotes(json: String): List<Quote>` is split out as a pure function (`internal`, exercised directly by `QuoteRepositoryTest` without needing an Android `Context`) that skips individual malformed entries instead of failing the whole array; `loadQuotes()` catches `IOException` from a missing/unreadable asset and falls back to the default language (English, index `0`); and `getRandomQuote()` falls back to a hardcoded safe `Quote` (a real entry duplicated from `English.json`, not fabricated content) if a language's list ends up empty after all fallbacks. When adding a new language, extend `QuoteRepository.LANGUAGE_FILES` (language-index → asset filename) — **append only, never reorder/reuse an index**, since the `language` setting is persisted as a raw `Int` in `"SETTINGS"` SharedPreferences and reordering `pref.xml`'s `languageCategory` array (or the map) would silently reassign existing users to the wrong language. As of this writing only the code-side plumbing for multi-language support exists; actual quote content for additional languages (`TODO.md` #6) and expanding past ~240 quotes/language (`TODO.md` #7) are unstarted — that requires sourcing/verifying real, correctly-attributed quotes, which should not be fabricated.

### Ads (AdMob)

- `MainActivity` loads a banner ad and shows it in `activity_main.xml`.
- `AdManager` (constructed per-fragment in `SettingPreferencesFragment`) preloads/shows an interstitial every 3rd color change (`colorChangeCount % 3 == 0`, tracked in the `"SETTINGS"` prefs file — a *different* counter than `AdManager`'s own unused `GENERATE_COUNT_KEY`/`SCAN_COUNT_KEY` constants in its `ad_counter` prefs file, which aren't currently wired to anything).
- Ad unit IDs live in `res/values/strings.xml` as paired `TEST_*`/`REAL_*` strings. As of this writing, both the banner (`MainActivity.loadBanner`) and the interstitial (`AdManager.loadInterstitialAd`) reference the `TEST_*` string resources, not the `REAL_*` ones — see `TODO.md` item 4.

### Known cross-file quirk

`ColorSelectorDialogBuilder.kt` declares `package com.odom.ledscreen` (a leftover from the vendored [HeryLopez/ColorSelector](https://github.com/HeryLopez/ColorSelector) library it's adapted from), while every other file in `app/src/main/java/com/odom/motivationlocker/` — including `ColorSelectorDialogPreference`, which imports it explicitly — uses `com.odom.motivationlocker`. Don't "fix" the package name without checking both sides of that import.

### Localization

Only Korean (`values-ko-rKR`) and default (English) `strings.xml` exist. `TODO.md` lists a planned order for adding more languages (Japanese → Spanish → Indonesian → Brazilian Portuguese), which touches three places together: a new `assets/*.json` quote file, `pref.xml`'s language `ListPreference` entries, and the store listing.
