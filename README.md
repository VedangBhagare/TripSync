# TripSync – Smart Travel Day Planner

**TripSync** is a modern travel planning application designed to streamline your travel experience. It provides seamless integration between Android mobile devices and Wear OS smartwatches, allowing users to manage trips, schedules, and real-time notifications effortlessly.

---

## Features

- **User Authentication**
  - Secure login and registration with **JWT**.
  - Google Sign-In support for quick authentication.

- **Trip Management**
  - Add, edit, and delete trips with detailed itineraries.
  - Store trip-related notes and important reminders.

- **Real-Time Notifications**
  - Receive live updates about upcoming trips directly on your mobile or Wear OS device.
  - Location-based notifications to enhance travel convenience.

- **Wear OS Integration**
  - Glanceable trip updates on your smartwatch.
  - Quick access to itinerary and reminders without needing your phone.

---

## Tech Stack

- **Mobile App**
  - Android (Java)
  - Room Database
  - Retrofit & OkHttp (for API calls)
  - Material Design 3 & ViewBinding

- **Wear OS Companion**
  - Android Wear OS
  - Kotlin/Java support
  - Live data updates from mobile app

- **Backend 
  - RESTful API (JWT authentication)
  - JSON data handling

---

## Project Structure

TripSync/
│
├─ app/                  # Mobile app source code
├─ wear/                 # Wear OS companion app
├─ build.gradle.kts      # Gradle build script
├─ settings.gradle.kts   # Project settings


## Installation & Setup

1. **Clone the repository**

```bash
git clone https://github.com/VedangBhagare/TripSync.git
cd TripSync
```

2. Open in Android Studio

Import the project as a Gradle project.
Ensure ViewBinding is enabled.
Sync Gradle files.

3. Run the App

Connect an Android device or emulator.
Build and run the app from Android Studio.
Optionally, install the Wear OS companion on a Wear OS emulator/device.

## Contact

Vedang Bhagare
Email: vedang.24.bhagare@gmail.com


