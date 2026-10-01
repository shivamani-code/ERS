# Emergency Backend Supervisor Server

A Node.js real-time backend service integrating with Firebase Admin SDK to supervise incoming emergency calls and dispatch location triggers.

---

## ⚙️ How It Works

1. Attaches a persistent real-time snapshot listener to Firestore's `incoming_calls` collection:
   ```javascript
   db.collection("incoming_calls").onSnapshot(...)
   ```
2. When a new incoming call record is detected, it reads `caller_number`.
3. Looks up the matching registered user in the `users` collection by phone number.
4. Retrieves the user's `device_token` and creates an active trigger document in `app_triggers`:
   ```javascript
   {
     device_token: deviceToken,
     phone: callerNumber,
     action: "SEND_LOCATION",
     status: "pending",
     timestamp: admin.firestore.FieldValue.serverTimestamp()
   }
   ```
5. The user's phone (`EmergencyLocationApp`) detects this trigger and immediately transmits its live GPS coordinates.

---

## 🚀 Setup & Running

1. **Install Dependencies**:
   ```bash
   npm install
   ```

2. **Add Firebase Service Account Key**:
   * Obtain `serviceAccountKey.json` from the Firebase Console (`Project Settings` > `Service Accounts` > `Generate new private key`).
   * Place it directly in this directory as `serviceAccountKey.json` (see `serviceAccountKey.example.json` for structure).

3. **Start the Supervisor**:
   ```bash
   npm start
   # or
   node server.js
   ```
