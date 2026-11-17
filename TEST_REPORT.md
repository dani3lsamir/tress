# Verification — 2026-11-17

Passed:
- 17 existing JVM domain regressions.
- ScheduleTest: daily leap-year rollover, weekly year rollover, monthly Jan 31 -> Feb 28 -> Mar 31 anchor, recurrence idempotency after restore, exact 10-day retention cutoff, preserving completion timestamp during edits, invalid dates/repetition rejection.
- Android resources compiled and linked using AAPT2 34.0.0 against Android API 34, using a temporary manifest with the Gradle namespace injected.
- All production Java sources compiled with JDK 17 against Android API 34 and the actual AAPT-generated R class. Deprecation notes remain for legacy platform APIs.
- Browser beta behavior and responsive layout checked with headless Brave/Playwright. Exact regression command/result is recorded separately in tests/beta.test.cjs; live microphone transcription is not covered by that script.
- Prior browser tests covered dictation event flow with a simulated provider, draft recovery, HTML injection prevention and recurrence/retention. Real voice accuracy was not tested.

Not verified:
- Full Gradle APK assembly and lint: the restricted execution environment could not connect to its Gradle daemon. Direct AAPT2/Javac validation is not a substitute for a complete APK build.
- Emulator/device installation, native UI, widget behavior and instrumented persistence tests.
- Real microphone, offline recognition, bilingual switching and vendor-specific speech services.

No APK or production-readiness claim accompanies this source package. The included Gradle wrapper allows the next application to perform the full build and device checks.

## Review fixes — 2026-11-17

Fixed:
- Android: saving "Fecha y repetición" unchanged reset the monthly anchor (a day-31 task clamped to Feb 28 then repeated on the 28th forever). `Notebook.schedule` now keeps the anchor when date and repetition are unchanged. Regression added to ScheduleTest (fails on the previous code).
- Android: widget taps ("Dictar", a note) were dropped when Tres was already running in the background. MainActivity is `singleTop`, widget intents use CLEAR_TOP|SINGLE_TOP and `onNewIntent` handles the request.
- Android: the editor dialog leaked its window on rotation; it is dismissed in `onDestroy` (the draft is already persisted).
- Android: a failed write while expiring completed notes made the whole store unreadable; expiry persistence is now best-effort on reads.
- Android/HTML: "Se elimina en 1 día" singular; Android shows "Vencida" like the HTML.
- Stored anchor days outside 0..31 are rejected (Android and HTML).
- HTML: two open tabs overwrote each other's notes; a `storage` listener now reloads state. Regression added (fails without the listener).
- HTML: the microphone icon depended on Lucide, which was never loaded; it is now an inline SVG. Removed dead theme/date CSS. Notes not updated today show their date.
- Repository: 16 text files had corrupt `\r\r\n` line endings. Normalized to LF and added `.gitattributes`.

Checks run: `scripts/test-core.sh` (17 + schedule regressions) passed; `tests/beta.test.cjs` passed in Chromium; production Java sources compiled with JDK against the API 34 framework jar (android-all 14) and a stub R class. A full Gradle/APK build and device tests were still not possible here (Google Maven/SDK not reachable).
