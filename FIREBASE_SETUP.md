# Firebase Setup for G3SPY

This document explains how to set up Firebase for the G3SPY surveillance system.

## Prerequisites

1. A Google account
2. Basic knowledge of Firebase

## Step 1: Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click on "Add project"
3. Enter a project name (e.g., "G3SPY Surveillance")
4. Follow the steps to create your project
5. Once created, you'll be taken to the project dashboard

## Step 2: Register Android Apps

You'll need to register both the Parent and Child apps in Firebase.

### Register Parent App

1. In the Firebase console, click "Add app" and select Android
2. Enter the package name: `com.g3spy.parent`
3. Enter a nickname (e.g., "G3SPY Parent")
4. Skip the SHA-1 for now (can be added later for authentication)
5. Click "Register app"
6. Download the `google-services.json` file
7. Place the file in the `G3SPYParent/app` directory

### Register Child App

1. Return to the Firebase console, click "Add app" and select Android
2. Enter the package name: `com.g3spy.child`
3. Enter a nickname (e.g., "G3SPY Child")
4. Skip the SHA-1 for now
5. Click "Register app"
6. Download the `google-services.json` file
7. Place the file in the `G3SPYChild/app` directory

## Step 3: Set Up Firestore Database

1. In the Firebase console, go to "Firestore Database"
2. Click "Create database"
3. Choose "Start in test mode" (for development purposes)
4. Select a location closest to your target users
5. Wait for the database to be provisioned

## Step 4: Create Firestore Collections

Create the following collections in Firestore:

1. `locations` - Stores location data from child device
2. `sms_messages` - Stores SMS messages
3. `call_logs` - Stores phone call logs
4. `keylogs` - Stores keystrokes captured
5. `screenshots` - Stores metadata for screenshots
6. `audio_recordings` - Stores metadata for audio recordings
7. `remote_commands` - Stores commands sent from parent to child app
8. `device_status` - Stores child device status information
9. `device_tokens` - Stores FCM tokens for messaging

## Step 5: Set Up Firebase Storage

1. In the Firebase console, go to "Storage"
2. Click "Get started"
3. Choose "Start in test mode" (for development purposes)
4. Select a location closest to your target users
5. Wait for Storage to be provisioned

## Step 6: Create Storage Folders

Create the following folders in Firebase Storage:

1. `/screenshots` - For screenshot images
2. `/audio_recordings` - For audio recording files

## Step 7: Set Up Firebase Cloud Messaging (FCM)

1. In the Firebase console, go to "Messaging"
2. Click "Get started"
3. Note your Server Key for sending messages via the FCM API

## Step 8: Configure Security Rules

For production use, update Firestore and Storage rules to secure your data:

### Firestore Rules

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Secure access to all collections
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Storage Rules

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Step 9: Test the Connection

1. Build and run both apps
2. Check the app logs for successful Firebase initialization
3. Verify data is being stored in Firestore and Storage

## Troubleshooting

- If you encounter connection issues, check your `google-services.json` files
- Ensure you have the correct dependencies in your Gradle files
- Verify your device has an active internet connection
- Check Firebase console for any service outages

## Security Warning

These applications contain powerful surveillance capabilities. Use responsibly and ensure you comply with all applicable laws regarding monitoring and data collection.