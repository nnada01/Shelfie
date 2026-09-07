# Shelfie

Shelfie is an Android reading companion app that lets users manage a personal bookshelf, track reading progress, keep a private reading journal, and view reading statistics.

## Features

- User sign-up and login
- Personal bookshelf with add, edit, delete, favorite, and read-status actions
- Google Books search/spotlight integration
- Reading journal with add, edit, delete, and export functionality
- Reading statistics and streak tracking
- Daily reading reminders
- Optional biometric/device-credential journal lock
- Light and dark themes
- Profile and account settings
- PHP/MySQL backend API
- Web-based admin dashboard

## Tech stack

**Android**
- Java
- Android Studio
- Android SDK
- AndroidX
- Material Components
- Volley
- Biometric API

**Backend**
- PHP
- MySQL
- XAMPP/Apache

## Repository structure

```text
Shelfie/
├── android/     # Android Studio application
├── api/         # PHP REST-style API and MySQL schema
├── docs/        # Project reference/documentation
├── .gitignore
└── README.md
```

## Running the Android app

### 1. Requirements

Install:
- Android Studio
- Android SDK
- JDK 11
- XAMPP (for the local PHP/MySQL API)

### 2. Set up the API

Copy the `api/` folder into your XAMPP web root, for example:

```text
xampp/htdocs/shelfie_api/
```

Start **Apache** and **MySQL** from XAMPP.

Create a MySQL database named:

```text
shelfie_db
```

Then import:

```text
api/shelfie.sql
```

into that database using phpMyAdmin.

Check `api/connection.php` and update the MySQL username/password if your local MySQL installation uses different credentials.

### 3. Configure the Android app

Open the `android/` folder in Android Studio.

Create `android/local.properties` based on `android/local.properties.example`.

Set:

```properties
sdk.dir=/path/to/your/Android/sdk
GOOGLE_BOOKS_API_KEY=your_google_books_api_key
```

`local.properties` is intentionally ignored by Git so your local SDK path and API key are not committed.

### 4. Configure the API URL

The Android app uses `ApiConfig.java`.

For the Android emulator, the default URL is:

```text
http://10.0.2.2:8080/shelfie_api/
```

If Apache is running on a different port, change the port accordingly.

For a physical Android phone, replace `10.0.2.2` with your computer's local network IP address, for example:

```text
http://192.168.x.x:8080/shelfie_api/
```

The phone and computer must be on the same network.

### 5. Build and run

Open the project in Android Studio, let Gradle sync, select an emulator or connected Android device, and press **Run**.

## API

The `api/` directory contains the PHP endpoints used by the Android application, including authentication, books, journals, profiles, reading statistics, streaks, account management, and the admin dashboard.

The database schema is provided in:

```text
api/shelfie.sql
```

## Security notes

- The repository is configured to ignore common Android build artifacts and signing files.
- The included API configuration is intended for local development.

## Documentation

See `docs/SHELFIE_GUIDE.md` for a more detailed explanation of the Android source code and project components.

