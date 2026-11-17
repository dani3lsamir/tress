# Tres — review and build package

This independent project contains an interactive HTML beta and a native Android implementation. It does not connect to ShivOS. Source, documentation and identifiers are English; the app UI is Spanish.

## Preview
Open `Tres-Beta.html` in a browser. It contains editable example notes. Browser storage is local to that browser and origin; moving or renaming a downloaded HTML file may change its storage identity. The Android app uses a separate private store and starts empty. There is no automatic transfer between the beta and Android.

## Implemented behavior
- Pure black AMOLED only; no theme chooser, header date, weekday or capture slogan.
- Text notes and up to three priorities; edit, complete and restore.
- Optional due date with daily, weekly or monthly repetition. Completing an occurrence creates exactly one next occurrence. Monthly schedules preserve the original day, clamping to the last day of shorter months. Overdue repetitions advance one interval per completion, not automatically to today. Restoring a completed occurrence keeps its already-created successor and cannot generate a duplicate successor.
- Completed notes expire 10 elapsed days after completion, independent of later edits. Legacy completed notes use their last update time as the migration baseline. HTML cleanup runs when opening, returning to the tab and every minute while running. Android cleanup runs on storage reads and changes. Neither implementation promises deletion at an exact time while closed.
- Dictation language is selected in Settings (Spanish or English). Browser speech recognition uses one configured language per session. Native Android uses the installed speech activity. Mixed Spanish/English recognition is not guaranteed. No audio is stored by Tres; a recognition provider may process audio online.
- No reminders, push notifications, account, sync or cloud speech backend.
- Native home screen widget shows three priorities and can complete them or launch dictation. HTML widget is a preview only.

## Android build
Requirements: JDK 17, Android SDK platform 34, build-tools 34.0.0. Android minimum is API 26 (Android 8). Set `ANDROID_HOME` or create an untracked `local.properties` containing `sdk.dir=/absolute/sdk/path` (escape Windows backslashes). Gradle wrapper 8.9 is included with a pinned distribution checksum; first build needs network access to Google/Maven/Gradle repositories.

Windows:
```powershell
.\gradlew.bat assembleDebug lintDebug
.\scripts\test-core.ps1
.\gradlew.bat connectedDebugAndroidTest
```
Linux/macOS:
```sh
chmod +x gradlew scripts/test-core.sh
./gradlew assembleDebug lintDebug
./scripts/test-core.sh
./gradlew connectedDebugAndroidTest
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`. Device tests require an emulator or device. This is a sideload/testing configuration, not a promise of current Play Store eligibility. Release signing keys are intentionally excluded.

## Browser regression tests
Install Playwright in an external tooling environment (`npm install playwright` and `npx playwright install chromium`) and run `node tests/beta.test.cjs`. Alternatively provide `PLAYWRIGHT_MODULE` and `BROWSER_PATH` environment variables. The test opens the local HTML in a fresh browser context.

## Review entry points
`app/src/main/java/app/tres/notes/Notebook.java`: recurrence, retention and priorities.
`Note.java`: immutable note metadata.
`NoteStore.java`: synchronized atomic JSON storage and backward-compatible metadata loading.
`MainActivity.java`: native AMOLED UI, scheduling dialog, draft recovery and dictation.
`PriorityWidget.java`: idempotent widget actions.
`Tres-Beta.html`: self-contained web beta; no build step.
`tests/`: domain and browser tests. `app/src/androidTest/`: Android persistence tests.

The two implementations share requirements, not source or storage. Native scheduling is available through each pending note's “Fecha y repetición” action, while the HTML includes schedule fields in its editor. A reviewer should test feature parity and native layout on actual phones.

See `TEST_REPORT.md` and `REVIEW_HANDOFF.md` for executed checks and remaining validation.
