# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build              # Full build
./gradlew assembleDebug      # Debug APK
./gradlew assembleRelease    # Release APK (requires signing config)
./gradlew test               # Unit tests (not yet implemented)
./gradlew connectedAndroidTest  # Instrumented tests (not yet implemented)
```

## Project Overview

Memory Support Display is an Android app for managing memory support cards (appointments, events, reminders, trips, and family messages). It connects to a backend API at `https://otheruncle.com/memory_display/api/`.

**Package:** `com.otheruncle.memorydisplay`
**Target SDK:** 35 (Android 15) | **Min SDK:** 26 (Android 8.0)
**Language:** 100% Kotlin with Jetpack Compose UI

## Architecture

### Layer Structure
- **data/api/** - Retrofit API service, interceptors, session management, NetworkResult wrapper
- **data/model/** - Data classes with Kotlinx Serialization (Card, User, Auth, etc.)
- **data/repository/** - Repository classes abstracting API calls (AuthRepository, CardsRepository, etc.)
- **data/push/** - Firebase Cloud Messaging integration
- **data/auth/** - Biometric authentication helper
- **di/** - Hilt dependency injection modules
- **ui/** - Compose screens organized by feature (home, create, profile, messages, review)

### Key Patterns
- **Hilt** for dependency injection with `@HiltViewModel` and `@Inject`
- **StateFlow** for reactive UI state in ViewModels
- **NetworkResult<T>** sealed class for API responses (Success, Error, Loading)
- **DataStore** for persistent user preferences
- **SessionManager** handles auth tokens and cookies with AuthInterceptor detecting 401s

### Card System
7 card types handled polymorphically via `CardType` enum and `CardData` sealed class:
- ProfessionalAppointment, FamilyEvent, OtherEvent, FamilyTrip, Reminder, FamilyMessage, Pending

Type-specific create/edit forms route through `EditCardRouter.kt`.

### Navigation
5-tab bottom navigation: Home, Create, Messages, Profile, Review (reviewer-only badge)

Deep links:
- `https://otheruncle.com/memory_display/setup?token={token}`
- `memorydisplay://setup/{token}`

## Key Files

- `MainActivity.kt` - Single activity host for Compose
- `MainViewModel.kt` - App-level state (login status, reviewer role, pending count)
- `ui/navigation/NavGraph.kt` - All 56 navigation routes
- `di/NetworkModule.kt` - Retrofit, OkHttp, and API service setup
- `data/api/SessionManager.kt` - Token and cookie persistence
- `data/api/AuthInterceptor.kt` - 401 detection and session expiry handling

## Dependencies (via gradle/libs.versions.toml)

| Purpose | Library |
|---------|---------|
| DI | Hilt 2.53.1 |
| UI | Jetpack Compose BOM 2024.12.01, Material3 |
| Navigation | Navigation Compose 2.8.5 |
| Networking | Retrofit 2.11.0, OkHttp 4.12.0 |
| Serialization | Kotlinx Serialization 1.7.3 |
| Images | Coil 2.7.0 |
| Storage | DataStore Preferences 1.1.1 |
| Security | Biometric 1.1.0 |
| Push | Firebase BOM 34.7.0 (FCM) |

## Firebase Setup

Requires `google-services.json` in `app/` directory. See `FIREBASE_SETUP.md` for full setup instructions.

Notification channels: `memory_display_default`, `memory_display_reminders`, `memory_display_card_updates`
