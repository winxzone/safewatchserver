# 🖥️ SafeWatch Server

This is the backend component of the **SafeWatch** system — a parental monitoring app that tracks children's device activity and generates usage reports for parents. The server receives metrics from the child’s device, stores them securely in a database, and serves them to the parent app upon request.

> 📱 The Android client for this server is available here: https://github.com/winxzone/safewatchapp

---

## 🌐 Features

- ✅ RESTful API for data exchange (HTTPS + JSON)
- 📥 Endpoints for receiving metrics: app usage, notifications, screen unlocks
- 📤 Endpoints for sending reports to parent devices
- 🛡️ Token-based authentication
- 📦 MongoDB for persistent data storage
- 🚀 Built with lightweight, fast Ktor framework

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Framework**: [Ktor](https://ktor.io/)
- **Database**: MongoDB
- **ORM/Driver**: [KMongo](https://litote.org/kmongo/)
- **Authentication**: JWT token-based auth
- **Data Format**: JSON
- **Hosting/Deployment**: Compatible with any JVM hosting (Heroku, Docker, etc.)

---

## How to Run

### Mobile App:
1. Open the project in Android Studio
2. Connect an emulator or Android device
3. Grant necessary permissions on first launch
4. Run the app

### Server:
1. Clone the backend repo: https://github.com/winxzone/safewatchserver
2. Run using Gradle or IntelliJ with Ktor
3. Ensure MongoDB is running locally or provide a remote URI

---

## Author

Illia Nevmyvannyi
GitHub: https://github.com/winxzone
