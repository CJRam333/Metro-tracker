# AGENTS.md

This file tells Jules (and any other AI coding agent) how to work in this repo.
Read this fully before starting any task. Follow it even if a specific ticket
doesn't repeat these rules.

## Project

**HMR Companion** — an Android app for Hyderabad Metro Rail commuters who get
absorbed in their phones and miss their stop. Two core features:

1. Manual mode: user picks From/To station, app tracks GPS during the ride and
   notifies them when their destination station is approaching.
2. (Later milestone, not yet started) Share-intent mode: user shares their
   booked metro ticket into the app, which auto-detects the route.

Only build what the current ticket asks for. Do not implement future milestones
ahead of schedule, even if they seem easy or related.

## Tech stack (do not deviate without asking)

- **Language:** Kotlin only. No Java files.
- **Platform:** Native Android. No React Native, Flutter, or cross-platform
  frameworks.
- **Min SDK:** 26 (Android 8.0). Target/compile SDK: latest stable.
- **Architecture:** MVVM.
  - `data/` — repositories, data sources, Room entities/DAOs, DataStore
  - `domain/` — plain Kotlin business logic (e.g. route/distance calculations),
    no Android framework imports here, must be unit-testable without
    instrumentation
  - `ui/` — Activities/Fragments/Composables, ViewModels
- **UI:** Jetpack Compose for any new screens, unless the ticket says otherwise.
- **Async:** Kotlin Coroutines + Flow. No RxJava.
- **DI:** Manual constructor injection is fine at this project's current size.
  Do not introduce Hilt/Dagger unless a ticket explicitly asks for it.
- **Persistence:** Room for structured data (e.g. trip history). Jetpack
  DataStore for simple preferences (e.g. alert distance setting). No
  SharedPreferences directly, no third-party DB libraries.
- **Location:** `FusedLocationProviderClient` from
  `com.google.android.gms:play-services-location`. Background/active-trip
  location tracking MUST run inside a foreground service with a visible,
  persistent notification — this is required by Android 10+ and is not
  optional or skippable.
- **Testing:** Anything placed in `domain/` must have JUnit unit tests with no
  Android dependencies. Aim for tests on any new pure logic.

## Project data

- Station data lives at `app/src/main/assets/hmr_stations.json`. Treat this
  file as static reference data — do not regenerate, reformat, or "improve"
  its structure unless a ticket explicitly asks you to change the schema.
- If a ticket needs the schema explained, read the file directly rather than
  guessing field names.

## Dependencies

- Before adding any new third-party library, state in your plan which library
  and why. Prefer official Jetpack/AndroidX/Google libraries over third-party
  ones. Do not add analytics, ads, or crash-reporting SDKs unless a ticket
  explicitly asks for them.
- No backend/server work in this repo. This app is fully on-device for the
  current roadmap. Do not introduce networking code, REST clients, or remote
  config unless a ticket explicitly asks for it.

## Permissions & manifest

- Only request the Android permissions a ticket's feature actually needs.
  Location features need `ACCESS_FINE_LOCATION`; if a foreground service is
  used, also add `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_LOCATION`
  (Android 14+ requirement).
- Always implement runtime permission requests properly (don't assume
  permission is granted) and handle the denied case with a clear in-app
  message, not a crash.

## Git / PR conventions

- One ticket = one focused PR. Do not bundle unrelated changes.
- Write a clear PR description: what changed, why, and how to manually test it
  on a device/emulator.
- If a ticket is ambiguous or you have to make a judgment call on something not
  covered here, state the assumption explicitly in your plan before writing
  code, and flag it again in the PR description. Do not silently make
  architectural decisions.
- If existing tests fail because of your change, fix them or explain why in
  the PR — never delete or weaken a test just to make it pass.

## What NOT to do

- Don't refactor or "clean up" code outside the scope of the current ticket.
- Don't change the station JSON schema, package structure, or core
  architecture choices listed above without it being explicitly requested.
- Don't add a backend, cloud sync, or analytics.
- Don't implement Milestone 3 (share-intent ticket import) or Milestone 4
  (settings/polish) features until tickets for those milestones exist.

