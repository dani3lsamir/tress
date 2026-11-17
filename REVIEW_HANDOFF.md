# Reviewer handoff

Goal: review and compile Tres, an AMOLED-only notes app with three priorities, Spanish/English dictation, dates, recurrence and ten-day completed-note retention.

Preserve: no header date or weekday, no theme selector, no capture slogan; local-only text storage; no automatic recurrence duplicates; completedAt must not move when text is edited; errors must not overwrite corrupt storage.

Run the commands in README.md. Inspect migration from the original six-field note format; simulate month ends, leap years, restore/recomplete and ten-day expiry. Test AtomicFile failures, concurrent widget/activity writes, rotation during dictation/editor saves, permission/service failures, 320px layouts and accessibility. Do not claim bilingual auto-switching based on selecting one locale. Android API 34 offers provider-dependent switching extras, but those are not enabled here.

HTML and native UI differ in layout and use separate storage. Do not silently replace the user's HTML notes when installing the native app. Export/import and reminders are not implemented. Audit deprecated APIs and target SDK requirements before any store release.

No signing keys, user notes, private files, machine-specific SDK path, caches or build outputs are included. All example notes in HTML are synthetic.
