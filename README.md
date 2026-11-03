# AniDex — Android Studio project

A native Kotlin + Jetpack Compose anime tracker starter with a polished dark interface, Android dynamic color, local sample library, episode increment UI, stats, and MAL OAuth 2.0 PKCE/API integration scaffolding.

## Open in Android Studio
1. Install Android Studio (current stable) and JDK 17.
2. Open this folder as a project. Let Gradle sync.
3. Use an Android 16/API 36 SDK platform (the project targets API 36).
4. Run on an Android device/emulator (min API 26).

## Configure MyAnimeList
1. Create an API application at https://myanimelist.net/apiconfig
2. Register the redirect URI `anidex://oauth` if the MAL developer console permits custom URI redirects for your app type. If your app registration requires another redirect configuration, make it match `MalAuth.CALLBACK`.
3. Replace `ADD_YOUR_MAL_CLIENT_ID` in `app/src/main/java/com/anidex/MainActivity.kt` with your MAL client ID.
4. Run and tap the profile icon to authorize.

### Important implementation notes
- Authorization uses the MAL OAuth authorize endpoint with PKCE S256 and an app callback.
- Token exchange uses the MAL token endpoint and stores the access token in memory for this starter.
- List import reads `/v2/users/@me/animelist`; API list status updates use `/v2/anime/{id}/my_list_status`.
- This starter intentionally does not yet persist the refresh/access token securely, implement refresh-token rotation, or automatically complete list import after authorization. The callback path is scaffolded, and the API layer contains the import/update calls; wire the callback event to UI state for a production build.
- The visible sample library is demonstration data, not the user's actual MAL list. The current UI's +1 episode interaction updates local screen state. Connect it to `MalApi.updateStatus(...)` after successful login for live writeback.
- For production, use encrypted token storage (Android Keystore), robust auth state validation, token refresh, error/retry UI, Room persistence, and tests. Do not commit a client secret. MAL native apps should use PKCE and not embed confidential secrets.

## Project structure
- `auth/MalAuth.kt` — PKCE verifier/challenge and browser login
- `auth/AuthCallbackActivity.kt` — deep-link callback handoff
- `data/MalApi.kt` — MAL token exchange, list import, and status update requests
- `MainActivity.kt` — Compose dashboard, library, episode controls, stats

## Build note
The Android Gradle Plugin and Kotlin versions are declared in root `build.gradle.kts`. If Android Studio proposes a compatible patch update, accept it.
