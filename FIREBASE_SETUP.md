# Firebase Setup Instructions

To enable push notifications in the Memory Support Display app, you need to set up Firebase Cloud Messaging.

## Steps:

### 1. Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project" (or select existing project)
3. Follow the setup wizard

### 2. Add Android App to Firebase

1. In Firebase Console, click the Android icon to add an Android app
2. Enter package name: `com.otheruncle.memorydisplay`
3. Enter app nickname: "Memory Support Display"
4. Click "Register app"

### 3. Download Configuration File

1. Download the `google-services.json` file
2. Place it in the `app/` directory (same level as `build.gradle.kts`)

### 4. Get Server Key for Backend

1. In Firebase Console, go to Project Settings > Cloud Messaging
2. Copy the "Server key" (or create a service account for HTTP v1 API)
3. Configure your backend to use this key for sending notifications

## Notification Types

The app supports these notification channels:

| Channel | Purpose |
|---------|---------|
| `memory_display_default` | General notifications |
| `memory_display_reminders` | Event reminders (high priority) |
| `memory_display_card_updates` | Card edit notifications |

## Backend Integration

The backend should send notifications with these fields:

```json
{
  "to": "<FCM_TOKEN>",
  "notification": {
    "title": "Notification Title",
    "body": "Notification body text"
  },
  "data": {
    "type": "event_reminder|driver_reminder|card_edited|calendar_review",
    "card_id": "123",
    "... other relevant data"
  }
}
```

## Testing

After setup, the FCM token will be registered with your backend on:
- User login
- New token generated (automatic)
- Account setup completion

Token is unregistered on logout.
