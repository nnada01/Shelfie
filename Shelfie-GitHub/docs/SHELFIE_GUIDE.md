# Shelfie — project reference

This file explains what each major source file does, what each public or meaningful function is for, what **nested Android/library calls** inside those functions do (**sections 3A–3B**), what **user-visible strings** in **`strings.xml`** cover, and **how to run the app on your laptop** (**section 10**).

**Scope note:** It is not useful to copy every grammatical sentence from every XML attribute or every line of Java into prose. **Section 3B** traces the important methods invoked *inside* each Shelfie function (Volley, Intents, RecyclerView, Biometric, etc.). For exhaustive line-by-line commentary, read the `.java` sources beside this guide.

**Snapshot updated:** This guide now reflects the current app state including Google Books spotlight/search, admin dashboard menu visibility, change-password and delete-account account controls, and password-rule helpers.

---

## 1. Repository layout (Android app)

| Path | Purpose |
|------|---------|
| `settings.gradle.kts` | Names the root project `Shelfie` and includes the `:app` module. |
| `build.gradle.kts` (root) | Applies the Android application plugin alias for subprojects; common Gradle hook. |
| `gradle/libs.versions.toml` | Version catalog (current pins): **AGP 8.13.2**; **AppCompat 1.7.1**; **Material 1.13.0**; **Activity 1.13.0**; **ConstraintLayout 2.2.1**; **RecyclerView 1.4.0**; **Volley 1.2.1**; **Biometric 1.1.0**; JUnit / AndroidX test libs. |
| `gradle.properties` | Gradle/JVM tuning (if present). |
| `app/build.gradle.kts` | Module config: `applicationId`, SDK levels, Java 11, dependencies from the catalog. Comment explains using the catalog for maintainability. |
| `app/proguard-rules.pro` | ProGuard/R8 rules for release builds (placeholder unless customized). |
| `app/src/main/AndroidManifest.xml` | Permissions, `queries`, application class, activities, receivers, launcher activity. |
| `app/src/main/java/com/example/shelfie/` | All app Java source. |
| `app/src/main/res/` | Layouts, drawables, values (colors, themes, strings, dimens), mipmaps, menus. |

**Companion backend (typical location):** `shelfie_api` under XAMPP `htdocs` — PHP + MySQL scripts the app calls via `ApiConfig.BASE_URL`. Documented in [section 8](#8-php-api-shelfie_api).

---

## 2. `AndroidManifest.xml` — what each part does

- **`uses-feature` (camera, telephony):** Declares optional hardware so the app can install on devices without them (`required="false"`).
- **`INTERNET`:** HTTP calls to your PHP API (Volley).
- **`CAMERA` / `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` (maxSdk 32):** Profile photo capture and gallery pick.
- **`USE_BIOMETRIC`:** Optional lock before opening the journal; **`JournalActivity`** may use **strong biometrics or device credential** (PIN/pattern) when the lock is enabled (`BiometricManager.Authenticators.BIOMETRIC_STRONG | DEVICE_CREDENTIAL`).
- **`CALL_PHONE`:** Direct “call support” from **Settings** (dial uses `ACTION_DIAL` and does not require this permission).
- **`POST_NOTIFICATIONS` (API 33+):** Daily reading reminder notifications.
- **`RECEIVE_BOOT_COMPLETED`:** Reschedule alarms after reboot.
- **`<queries>`:** Package visibility for `http(s)`, `tel`, `geo` intents (browser, dialer, maps).
- **`application`:** `ShelfieApp`, backup, icons, Material theme, **`usesCleartextTraffic="true"`** so `http://` to local/dev servers works.
- **Activities:** `SplashActivity` is launcher; others are `exported="false"` except where needed. **`parentActivityName`** wires the system Up affordance for nested screens. Includes account-security activity **`ChangePasswordActivity`** (parent: `SettingsActivity`) and **`ReadingStatsActivity`** for the statistics dashboard.
- **Receivers:** `ReminderReceiver` (alarm → notification), `BootReceiver` (boot → reschedule).

---

## 3. Java classes — file purpose and functions

### `ApiConfig.java`
**Purpose:** Single place for the REST base URL.  

| Symbol | Purpose |
|--------|---------|
| `BASE_URL` | REST base URL (emulator often `http://10.0.2.2:…/shelfie_api/`; physical device uses the laptop LAN IP). |
| `adminDashboardUrl()` | Returns web admin endpoint URL (`BASE_URL + "admin_dashboard.php"`). |
| `GOOGLE_BOOKS_API_KEY` | API key used for Google Books volumes search on the home screen. |

### `ShelfieApp.java` (extends `Application`)
**Purpose:** Runs before any activity.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Applies saved light/dark theme and syncs the reading-reminder alarm (`ReadingReminderScheduler.sync`). |

### `SplashActivity.java`
**Purpose:** Branded delay screen; routes by login state.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Inflates splash layout, after **3000 ms** starts `MainActivity` if `SessionManager.isLoggedIn()`, else `LoginActivity`, then `finish()`. |

### `SessionManager.java`
**Purpose:** `SharedPreferences` file `ShelfieSession` for auth and profile cache.  

| Function | Purpose |
|----------|---------|
| `SessionManager(Context)` | Opens prefs and editor. |
| `saveUser(id, first, last, email)` | Sets `isLoggedIn`, `user_id`, names, `full_name`, `email`. |
| `saveUser(id, first, last, email, isAdmin)` | Same as above plus persists `is_admin` for role-based UI. |
| `updateProfile(first, last, birthday, country, imageUrl)` | Updates name parts, birthday, country, `image_url` (and rebuilds `full_name`). |
| `isAdmin()` | Returns stored `is_admin` flag (used to show/hide admin dashboard menu item). |
| `isLoggedIn()` | Whether a session exists. |
| `getUserId()` | Stored user id or `-1`. |
| `getFullName()` | Built from current first + last. |
| `getFirstName()` / `getLastName()` / `getEmail()` / `getBirthday()` / `getCountry()` / `getImageUrl()` | Field getters. |
| `getUpperFirstName()` | First name uppercased (e.g. display flourishes). |
| `updateReadingExtras(goalType, goalValue)` | Caches reading goal (`pages_week` / `books_year`) from the server for the home banner; removes legacy `public_slug` pref if present. |
| `getReadingGoalType()` / `getReadingGoalValue()` | Read cached goal fields. |
| `buildFullName(first, last)` *(private)* | Trims and joins with a single space; handles one-sided empty. |
| `logout()` | `clear()` prefs. |

### `ThemeManager.java`
**Purpose:** App-wide UI prefs in `shelfie_preferences`.  

| Constant / function | Purpose |
|---------------------|---------|
| `PREFS_NAME`, `KEY_DARK_MODE`, `KEY_BIOMETRIC_LOCK`, `KEY_READING_REMINDERS` | Storage keys. |
| `prefs(Context)` *(private)* | Application-scoped `SharedPreferences`. |
| `applySavedTheme(Context)` | Reads dark flag; sets `AppCompatDelegate` night mode. |
| `isDarkModeEnabled` / `setDarkModeEnabled` | Read/write dark mode and apply immediately. |
| `isBiometricLockEnabled` / `setBiometricLockEnabled` | Journal lock toggle (default **true** for lock in code comment). |
| `isReadingRemindersEnabled` / `setReadingRemindersEnabled` | Reminder toggle (default false). |

### `NetworkErrorHelper.java`
**Purpose:** Human-readable Volley errors.  

| Function | Purpose |
|----------|---------|
| `message(Context, VolleyError)` | `NoConnectionError`/`NetworkError` → `error_network` + URL line with `ApiConfig.BASE_URL`; `TimeoutError` → timeout string; else generic error. |

### `ReadingReminderScheduler.java`
**Purpose:** Daily inexact alarm for reading reminders.  

| Symbol | Purpose |
|--------|---------|
| `REQUEST_CODE` | `7101` — stable id for `PendingIntent`. |
| `sync(Context)` | If reminders disabled, cancels alarm; else schedules next **09:00** (rolls to tomorrow if past), `INTERVAL_DAY`, `RTC_WAKEUP`. |

### `ReminderReceiver.java`
**Purpose:** Alarm callback; posts notification.  

| Function | Purpose |
|----------|---------|
| `onReceive()` | On API 26+, creates channel `reading_channel` (name from `settings_notifications_title`); builds notification with `notification_reading_title` / `notification_reading_text`; `notify(1, ...)`. |

### `BootReceiver.java`
**Purpose:** Restore alarm after reboot.  

| Function | Purpose |
|----------|---------|
| `onReceive()` | If action is `BOOT_COMPLETED`, calls `ReadingReminderScheduler.sync`. |

### `QuoteHelper.java`
**Purpose:** Deterministic “quote of the day” from calendar day-of-year.  

| Function | Purpose |
|----------|---------|
| `getQuoteOfDay()` | Returns one of six fixed English quotes (see [section 6](#6-quotehelper--literal-quote-sentences)). |

### `Book.java` / `Journal.java`
**Purpose:** Plain data holders for list rows and forms.  

| Members | Purpose |
|---------|---------|
| `Book` | Fields: id, title, author, category, status, pages, current page, favorite flag, rating, genres string, recommend flag — exposed via getters. |
| `Journal` | Constructor + getters: id, title, content, mood, entry date. |

### `BookAdapter.java`
**Purpose:** `RecyclerView` rows for books; delegates clicks to activity.  

| Symbol | Purpose |
|--------|---------|
| `BookClickListener` | `onEditClick`, `onDeleteClick`, `onWebsiteClick`. |
| `BookAdapter(...)` | Holds context, list, listener. |
| `BookHolder` | View holder: binds title, author, progress, favorite icon, progress bar, action buttons. |
| `onCreateViewHolder` | Inflates `item_book`. |
| `onBindViewHolder` | Fills row from `Book` at position; wires clicks. |
| `getItemCount()` | List size. |

### `JournalAdapter.java`
**Purpose:** `RecyclerView` rows for journal entries.  

| Symbol | Purpose |
|--------|---------|
| `JournalClickListener` | `onEditClick`, `onDeleteClick`, `onExportClick`. |
| `JournalAdapter`, `JournalHolder`, `onCreateViewHolder`, `onBindViewHolder`, `getItemCount` | Same pattern as `BookAdapter` for `item_journal`. |

### `LoginActivity.java`
**Purpose:** Email/password login → session + `MainActivity`.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Binds views, signup link, login button → `loginUser()`. |
| `loginUser()` | Validates fields; POST `login.php`; parses JSON; `SessionManager.saveUser`; toast; navigate to `MainActivity`. |
| `getParams()` *(anonymous `StringRequest`)* | POST body: email, password. |
| `splitFullName(String)` *(private)* | Splits API `full_name` on first space into first/last when needed. |

### `SignupActivity.java`
**Purpose:** Register, then auto-login.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Form + submit → Volley POST `signup.php`. |
| `refreshPasswordRequirementRows()` | Live password-rule checklist UI while typing (length/upper/lower/special). |
| `getParams()` (signup) | first name, last name, email, password. |
| `loginAfterSignup()` | POST `login.php` with same email/password to seed session. |
| `getParams()` (login) | email, password. |

### `PasswordRules.java`
**Purpose:** Shared client-side password policy evaluator.  

| Symbol / function | Purpose |
|-------------------|---------|
| `MIN_LENGTH` | Minimum password length (`8`). |
| `Result` | Booleans for each rule (`minLength`, `hasUpperCase`, `hasLowerCase`, `hasSpecialChar`) plus `isValid()`. |
| `evaluate(CharSequence)` | Returns rule-by-rule evaluation used by signup/change-password flows. |

### `PasswordUiHelper.java`
**Purpose:** Password requirements UI binder for reusable rule rows.  

| Function | Purpose |
|----------|---------|
| `updateRequirementLabels(...)` | Applies pass/fail markers and colors for each password requirement row. |

### `GoogleBooksApi.java`
**Purpose:** Build Google Books API requests and summarize volume JSON for the home spotlight/search panel.  

| Symbol / function | Purpose |
|-------------------|---------|
| `buildVolumesUrl(query, maxResults)` | Builds `volumes` API URL with narrowed response fields + API key. |
| `summarizeFirstVolume(responseJson)` | Parses first result and returns `VolumeSummary` text + optional preview URL. |
| `VolumeSummary` | Display payload consumed by `MainActivity` (`text`, `previewUrl`). |

### `MainActivity.java`
**Purpose:** Home: welcome, Google Books spotlight/search result, streak, **reading goal** (text + horizontal progress bar with custom tints), search, status filter, book list; **toolbar always shows sun/moon theme toggle**; overflow includes **Profile**, **Settings**, **Log out**, and **Admin dashboard** (admin users only). Add book / journal are the main content buttons (not FAB).  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Toolbar, RecyclerView, adapters, search view, filter spinner, welcome/quote/streak/goal views. |
| `loadGoogleBookSpotlightOfTheDay()` | Builds a deterministic daily query and fetches one Google Books suggestion to display in the top panel. |
| `searchGoogleBooksFromInput()` / `fetchGoogleBooksVolume(...)` | On-demand Google Books lookup from user input; updates summary text and preview button. |
| `bindGoogleBooksPreviewButton(url)` | Shows/hides preview CTA and opens `previewLink` / `infoLink` in browser. |
| `onQueryTextSubmit` / `onQueryTextChange` | Filter list by title/author substring. |
| `onItemSelected` (spinner) | Status filter + `applyFilters()`. |
| `onResume()` | Refreshes list, streak, and reading-goal banner when returning. |
| `markReadingDayThenRefreshStreak()` | GET `mark_reading_day.php` then `getStreakFromDB()`. |
| `getStreakFromDB()` | GET `get_streak.php` for the streak label. |
| `loadReadingStatsForHome()` | GET `get_reading_stats.php` — updates goal text, **progress bar** visibility/progress, and `SessionManager.updateReadingExtras` (goal type/value). |
| `applyFilters()` | Applies search text + status + favorites toggle to adapter list. |
| `onEditClick` / `onDeleteClick` / `onWebsiteClick` | Start `AddBookActivity` with extras, confirm delete + `delete_book.php`, or Google search intent for the title. |
| `deleteBook(Book)` | GET `delete_book.php?id=`. |
| `onCreateOptionsMenu` / `onOptionsItemSelected` | **Theme:** toggle dark mode + `recreate()`. **Overflow:** profile, settings, logout, and admin dashboard (visible only when `SessionManager.isAdmin()` is true). Web/map/dial/call/export/account actions live under **Settings**. |
| `applyMainToolbarLogo(Toolbar)` | Light vs dark toolbar logo from `ThemeManager`. |

### `AddBookActivity.java`
**Purpose:** Create or edit a book; genres, rating, status, favorites, recommendation.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Inflates form; edit mode reads intent extras; save from toolbar. |
| `onSupportNavigateUp()` | System back. |
| `loadBookForEdit()` | Populates fields from intent. |
| `setSpinnerStatus(String)` | Maps API status to spinner index. |
| `readStringExtra` / `readIntExtra` / `readFloatExtra` | Safe intent reading. |
| `clearGenreCheckboxes()` | Unchecks all genre boxes. |
| `onCreateOptionsMenu` / `onOptionsItemSelected` | **Save** → `saveBook()`; overflow **Settings** → `SettingsActivity`. |
| `saveBook()` | Validates; **GET** `add_book.php` or `update_book.php` with query parameters assembled by `buildBookUrl()`. On **edit**, appends **`page_delta`** (non‑negative increase in current page since open) so the server can update weekly reading totals. |
| `initialCurrentPageForEdit` | Set in `loadBookForEdit()` from the book’s `current_page`; used only to compute **`page_delta`** on save. |
| `clearFieldErrors()` | Clears `TextInput` errors. |
| `getSelectedGenres()` | Concatenates checked genre labels. |
| `appendGenreIfChecked` | Helper for comma-separated genres. |
| `applyGenresToCheckboxes` / `containsToken` | Parses stored genre string back to checkboxes. |

### `JournalActivity.java`
**Purpose:** List journal entries; optional **biometric or device-credential** gate when **`ThemeManager`** journal lock is on; edit/delete/export.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | If journal lock **off**, loads the list immediately. If **on**, checks **`BiometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)`** — on success shows **`BiometricPrompt`**; on none enrolled / unavailable, toast and **`finish()`**. |
| `getAuthResult()` | Exposed for prompt flow state if used. |
| `BiometricPrompt` callbacks | Success → load list; errors → finish or message. |
| `onSupportNavigateUp()` | Back. |
| `onEditClick` / `onDeleteClick` / `onExportClick` | Navigate to editor, confirm delete, write entry to file/share. |
| `deleteJournal(Journal)` | API delete for one entry. |

### `AddJournalActivity.java`
**Purpose:** Create/edit one journal entry (title, body, mood, date).  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Form binding; edit mode from intent. |
| `onSupportNavigateUp()` | Back. |
| `onCreateOptionsMenu` / `onOptionsItemSelected` | **Save** → `saveJournal()` (Volley GET `add_journal.php` / `update_journal.php`); overflow **Settings** → `SettingsActivity`. |

### `ProfileActivity.java`
**Purpose:** View/edit profile and photo; optional onboarding mode (required birthday/country); **reading goal** (pages/week or books/year) for stats.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Toolbar, fields (including goal spinner/value), photo buttons, `loadProfile()` if not onboarding. |
| `onSupportNavigateUp()` | Back. |
| `confirmRemoveProfilePhoto()` | Dialog; on confirm clears image, sets `encodedImage` "", **`pendingPhotoRemoval = true`**. |
| `chooseImage()` | Dialog: camera vs gallery. |
| `openCameraWithPermission` / `openGalleryWithPermission` | Runtime permissions then launchers. |
| `launchGalleryPicker()` | `ACTION_PICK` image. |
| `showPlaceholderAvatar()` | Default drawable. |
| `applyBitmapToAvatar(Bitmap)` | Shows bitmap, JPEG→Base64 for upload, clears `pendingPhotoRemoval`. |
| `showDatePicker()` | Sets birthday `yyyy-MM-dd`. |
| `loadProfile()` | GET `get_profile.php`; fills fields including **`reading_goal_type`**, **`reading_goal_value`**; **`sm.updateReadingExtras`** keeps session in sync. |
| `saveProfile()` | Validates; POST `update_profile.php`; on success **`sm.updateReadingExtras`** with goal fields; includes **`remove_profile_image=1`** when user removed photo. |
| `getParams()` | user_id, names, birthday, country, optional remove flag, `image_base64`, **`reading_goal_type`**, **`reading_goal_value`**. |
| `applyGoalTypeToSpinner` / `spinnerGoalTypeToApi` | Maps API values `none` / `pages_week` / `books_year` ↔ spinner index. |
| `safeText(TextInputEditText)` | Trim or empty. |
| `splitName(String)` | Same idea as login: split full name for legacy API fields. |

### `ChangePasswordActivity.java`
**Purpose:** Account security screen to change password with live rule feedback and backend validation.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Toolbar setup + field binding + watcher for live password-rule rows. |
| `refreshPasswordRequirementRows()` | Evaluates new-password text and refreshes requirement markers/colors using `PasswordUiHelper`. |
| `attemptChangePassword()` | Validates current/new/confirm fields, enforces `PasswordRules`, then POSTs `change_password.php`. |
| `textOf(...)` | Null-safe text extraction helper for `TextInputEditText`. |

### `SettingsActivity.java`
**Purpose:** Toggles for reminders, biometric lock, dark mode; **Account** actions (change password, delete account), **Reading & data** actions (stats, export), **Discover & support** (web, maps, dial, direct call). **`CALL_PHONE`** and **`onRequestPermissionsResult`** live here for direct calling.  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Switches bound to `ThemeManager`; notification permission launcher; buttons → **`ChangePasswordActivity`**, account-delete dialog, **`ReadingStatsActivity`**, export dialog, intents for web/map/dial/call. |
| `openBooksWeb` / `openBookstoresMap` / `dialSupport` | Implicit intents with **`resolveActivity`** checks; map uses **`safeStartMap`**. |
| `requestCallPermissionAndCall` / `onRequestPermissionsResult` | Runtime **`CALL_PHONE`** then **`ACTION_CALL`**. |
| `showDeleteAccountDialog` / `requestDeleteAccount` | Password-confirmed account deletion via POST `delete_account.php`; logs out and clears task stack on success. |
| `showExportFormatDialog` / `performExport` | GET **`export_shelf_journal.php`**; share JSON as‑is or CSV via **`ExportHelper.jsonToCsv`**. |
| `onSupportNavigateUp()` | Back. |

### `ReadingStatsActivity.java`
**Purpose:** Reading statistics dashboard from **`get_reading_stats.php`** (summary text, monthly finished books, genre breakdown, streak detail).  

| Function | Purpose |
|----------|---------|
| `onCreate()` | Toolbar with Up (navigation → back dispatcher); binds summary / containers / loading indicator; **`loadStats()`**. |
| `loadStats()` | If **`SessionManager.getUserId()`** is **≤ 0**, toast generic error and **`finish()`**; else Volley GET **`get_reading_stats.php?user_id=`**; **`applyJson`** builds simple bar-style rows in **`LinearLayout`s**. |

### `ExportHelper.java`
**Purpose:** Pure Java helper: converts **`export_shelf_journal.php`** JSON into **CSV** (books + journal sections, escaped fields).  

## 3A. Shared APIs — what the nested calls mean

These patterns appear inside many Shelfie methods. Understanding them makes the per-class notes below easier to follow.

### Android framework (`android.*`)

| API | Role |
|-----|------|
| `super.onCreate(savedInstanceState)` | Lets `AppCompatActivity` restore fragment state and run its own setup before your code runs. |
| `setContentView(layoutResId)` | Inflates the XML layout and attaches it as the activity’s content view so `findViewById` can resolve ids. |
| `findViewById(id)` | Returns the view object for that `@+id` from the current content view (returns `View`; you cast to `TextView`, etc.). |
| `getString(resId)` / `getString(resId, …)` | Loads a localized string from `strings.xml`; the overload fills `printf`-style placeholders (`%1$s`, `%1$d`). |
| `Toast.makeText(context, text, duration).show()` | Shows a short transient message on screen (`LENGTH_SHORT` / `LENGTH_LONG`). |
| `Intent(context, Class)` | Explicit intent: names the exact activity class to start. |
| `Intent(action, Uri)` | Implicit intent: asks the system for any app that handles that action (e.g. `ACTION_VIEW` + `https:` URL). |
| `putExtra(key, value)` | Attaches typed data to an intent for the next activity (`getIntent().getExtras()` / `getIntExtra` etc.). |
| `startActivity(intent)` | Starts an activity; may throw if no app handles an implicit intent (Shelfie often checks `resolveActivity` first). |
| `finish()` | Closes the current activity so the user returns to the previous one in the back stack. |
| `setResult(resultCode)` | Used with `startActivityForResult` / `ActivityResultLauncher`: tells the caller `RESULT_OK` or `RESULT_CANCELED` before `finish()`. |
| `getPackageManager()` | Returns `PackageManager` to query installed apps (`resolveActivity`, `queryIntentActivities`). |
| `getMenuInflater().inflate(menuRes, menu)` | Turns `res/menu/*.xml` into live `MenuItem` objects for the toolbar. |
| `getContentResolver()` | System entry point for reading/writing non-file URIs (images, documents created via Storage Access Framework). |
| `getOnBackPressedDispatcher().onBackPressed()` | Triggers the same back behavior as the system back gesture (used from `onSupportNavigateUp`). |
| `recreate()` | Destroys and immediately recreates the activity so theme/layout changes (e.g. dark mode) apply cleanly. |
| `invalidateOptionsMenu()` | Asks the framework to rebuild the options menu (e.g. after toggling light/dark icon). |
| `ActivityCompat.requestPermissions` / `ContextCompat.checkSelfPermission` | Runtime permission flow (API 23+): check then request; result arrives in `onRequestPermissionsResult`. |

### AndroidX AppCompat / Activity

| API | Role |
|-----|------|
| `setSupportActionBar(toolbar)` | Connects a `Toolbar` to the activity as the action bar so titles and menus work. |
| `getSupportActionBar()` | Returns `ActionBar` to set title, Up (“home”) affordance, etc. |
| `ActionBar.setDisplayHomeAsUpEnabled(true)` | Shows the Up arrow; when combined with manifest `parentActivityName`, Up navigates logically. |
| `onSupportNavigateUp()` | Called when Up is pressed; Shelfie forwards to the back dispatcher. |
| `registerForActivityResult(contract, callback)` | Modern replacement for `startActivityForResult`: registers a launcher you call with `launch(intent)`; callback receives `ActivityResult` with `resultCode` and `data`. |
| `ActivityResultContracts.StartActivityForResult()` | Contract for arbitrary activities returning a result. |
| `ActivityResultContracts.RequestPermission()` | Contract that yields `true`/`false` when the user grants or denies one permission. |
| `ActivityResultContracts.TakePicturePreview()` | Launches in-app camera preview and returns a small `Bitmap` (no file path). |

### Material Components

| API | Role |
|-----|------|
| `MaterialAlertDialogBuilder(context)` | Themed `AlertDialog` with Material styling; `setTitle` / `setMessage` / `setPositiveButton` build the dialog; `show()` displays it. |
| `MaterialToolbar` | Toolbar styled for Material 3; still works with `setSupportActionBar`. |
| `TextInputLayout.setError` / `TextInputEditText.setError` | Shows validation message under or on the field. |
| `SwitchMaterial.setOnCheckedChangeListener` | Fires when the user toggles; receives `(CompoundButton, boolean isChecked)`. |

### Volley (`com.android.volley`)

| API | Role |
|-----|------|
| `Volley.newRequestQueue(context)` | Creates (or reuses internally) a request queue that runs HTTP on background threads. |
| `new StringRequest(method, url, listener, errorListener)` | HTTP request whose body/response is treated as `String` (here: JSON text). |
| `Request.Method.GET` / `POST` | HTTP verb. Shelfie uses GET with query strings for many PHP scripts and POST form body for login/signup/profile. |
| `queue.add(request)` | Enqueues the request; listeners run on the main thread by default. |
| Success lambda / `Response.Listener` | Receives the response body string when HTTP succeeds (status 2xx). |
| `Response.ErrorListener` / `VolleyError` | Called on network failure, timeout, or HTTP error; `NetworkErrorHelper` classifies the error type. |
| `@Override getParams()` on `StringRequest` | For POST, Volley calls this to build `application/x-www-form-urlencoded` body key/value pairs. |

### `org.json`

| API | Role |
|-----|------|
| `new JSONObject(String)` | Parses a JSON object from the response string; throws if malformed (caught in `try/catch`). |
| `optBoolean` / `optString` / `optInt` / `optDouble` | Reads keys with defaults if missing (safer than `getX` which can throw). |
| `getBoolean` / `getString` / `getInt` / `getJSONObject` / `getJSONArray` | Strict accessors when the key must exist. |
| `JSONArray.length()` / `getJSONObject(i)` | Iterates array of books or journals from the API. |

### RecyclerView (`androidx.recyclerview`)

| API | Role |
|-----|------|
| `RecyclerView.setLayoutManager(new LinearLayoutManager(context))` | Vertical (or horizontal) list layout. |
| `RecyclerView.setAdapter(adapter)` | Connects data + view holders; `notifyDataSetChanged()` asks for rebinding all visible rows. |
| `LayoutInflater.from(context).inflate(layoutId, parent, false)` | Inflates one row XML without attaching to parent yet (required pattern for list items). |
| `ViewHolder` constructor `super(itemView)` | Registers the root row view with the RecyclerView machinery. |

### Images and encoding (`android.graphics`, `android.util`)

| API | Role |
|-----|------|
| `Bitmap.compress(format, quality, stream)` | Writes compressed image bytes (here JPEG quality 80) into a `ByteArrayOutputStream`. |
| `Base64.encodeToString(bytes, flags)` | Produces ASCII safe for JSON/form fields (`DEFAULT` includes newlines every 76 chars; servers usually still accept it). |
| `Base64.decode` / `BitmapFactory.decodeByteArray` | Reverses the process for showing a profile image from `image_base64`. |
| `BitmapFactory.decodeStream(InputStream)` | Decodes a gallery `Uri` opened via `ContentResolver.openInputStream`. |
| `ImageView.setImageBitmap` / `setImageResource` | Shows a bitmap or a drawable resource. |

### Biometric (`androidx.biometric`)

| API | Role |
|-----|------|
| `BiometricManager.from(context)` | Entry point for capability checks. |
| `canAuthenticate(authenticators)` | Returns codes like `BIOMETRIC_SUCCESS` or `BIOMETRIC_ERROR_NONE_ENROLLED`. |
| `BiometricPrompt(activity, executor, callback)` | Shows the system PIN/pattern/biometric UI. |
| `PromptInfo.Builder` | Sets title, subtitle, and which authenticator types are allowed. |
| `authenticate(promptInfo)` | Displays the prompt; callbacks fire on success, error, or failure. |

### Alarms and notifications

| API | Role |
|-----|------|
| `Context.getSystemService(ALARM_SERVICE)` | Returns `AlarmManager` for scheduling. |
| `PendingIntent.getBroadcast` | Token the alarm manager holds; when it fires, the system sends the intent to your `BroadcastReceiver`. |
| `FLAG_UPDATE_CURRENT` / `FLAG_IMMUTABLE` | Updates extras on an existing pending intent; `IMMUTABLE` is required on modern Android for security. |
| `setInexactRepeating` | Schedules roughly daily wakeups (battery-friendly; not exact to the second). |
| `NotificationChannel` (API 26+) | Required grouping for notifications; user can change importance per channel in settings. |
| `NotificationCompat.Builder` | Builds a notification compatible with older APIs; `notify(id, notification)` posts it. |

### `SharedPreferences`

| API | Role |
|-----|------|
| `getSharedPreferences(name, MODE_PRIVATE)` | Opens a named key-value file private to the app. |
| `edit()` / `putString` / `putBoolean` / `putInt` / `apply()` | `apply()` writes asynchronously; `commit()` is synchronous (rarely needed here). |
| `getString` / `getBoolean` / `getInt` | Reads with a default if missing. |
| `clear()` | Wipes the file (logout). |

### `AppCompatDelegate`

| API | Role |
|-----|------|
| `setDefaultNightMode(MODE_NIGHT_YES` / `NO`) | Switches entire app between dark and light themes according to Material day/night resources. |

### `Uri` and encoding

| API | Role |
|-----|------|
| `Uri.parse(base).buildUpon()` | Builds URLs with proper query encoding via `appendQueryParameter` (handles spaces and special characters safely). |

---

## 3B. Deep dive — methods invoked inside each Shelfie function

Below, **“calls”** lists important nested APIs (Android, libraries, or other Shelfie methods) and what that step accomplishes.

### `ApiConfig.java`
- **`BASE_URL` constant** — No methods; other classes concatenate script names (`login.php`, etc.) to this prefix.

### `ShelfieApp.onCreate()`
- **`super.onCreate()`** — Standard `Application` initialization.
- **`ThemeManager.applySavedTheme(this)`** — Reads prefs, sets night mode before first activity draws.
- **`ReadingReminderScheduler.sync(this)`** — Cancels or registers alarm so reminders match saved toggle.

### `SplashActivity.onCreate()`
- **`super.onCreate` / `setContentView`** — Shows splash layout.
- **`new SessionManager(this)`** — Loads login flag from prefs.
- **`Handler(Looper.getMainLooper()).postDelayed(Runnable, 3000)`** — Runs navigation on the UI thread after 3 seconds (`Looper.getMainLooper()` ensures main thread).
- **`sm.isLoggedIn()`** — Branches to `MainActivity` vs `LoginActivity`.
- **`startActivity(Intent)` / `finish()`** — Opens next screen and removes splash from back stack.

### `SessionManager` methods
- **Constructor** — `getSharedPreferences("ShelfieSession", …)` and `edit()` cache the editor.
- **`saveUser` / `updateProfile`** — Multiple `editor.put*` then `apply()`; `updateProfile` uses **`buildFullName`** to keep `full_name` consistent.
- **`buildFullName`** — `trim()`, checks empty segments, concatenates with a space.
- **`logout`** — `editor.clear()` removes all session keys.

### `ThemeManager` methods
- **`prefs(context)`** — `getApplicationContext()` avoids leaking an activity context; returns same prefs file from any caller.
- **`applySavedTheme` / `setDarkModeEnabled`** — `getBoolean` / `putBoolean` + **`AppCompatDelegate.setDefaultNightMode`**.
- **`setBiometricLockEnabled` / `setReadingRemindersEnabled`** — Simple `putBoolean` on `shelfie_preferences`.

### `NetworkErrorHelper.message()`
- **`instanceof` checks** on `VolleyError` subclasses — Distinguishes “no network” vs “timeout” vs other HTTP/parser errors.
- **`context.getString(R.string.…, ApiConfig.BASE_URL)`** — Injects the configured URL into the error text for debugging.

### `ReadingReminderScheduler.sync()`
- **`getApplicationContext()`** — Long-lived alarm should not hold an activity.
- **`getSystemService(ALARM_SERVICE)`** — Gets `AlarmManager`.
- **`new Intent(app, ReminderReceiver.class)`** — Explicit broadcast target.
- **`PendingIntent.getBroadcast`** — Wraps intent for the alarm.
- **`am.cancel(pi)`** — Removes scheduled alarms when reminders off.
- **`Calendar.getInstance()` + field setters** — Computes next 9:00 local time; **`add(DAY_OF_YEAR, 1)`** if time already passed today.
- **`setInexactRepeating(RTC_WAKEUP, …, INTERVAL_DAY, pi)`** — Repeating daily alarm.

### `ReminderReceiver.onReceive()`
- **`getSystemService(NOTIFICATION_SERVICE)`** — `NotificationManager`.
- **`Build.VERSION.SDK_INT` check** — Creates **`NotificationChannel`** only on O+.
- **`NotificationCompat.Builder`** — Sets small icon, title, text, auto-cancel; **`manager.notify(1, …)`** shows it.

### `BootReceiver.onReceive()`
- **`Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())`** — Ignores other broadcasts if registered for multiple (here only boot in manifest).
- **`ReadingReminderScheduler.sync`** — Restores alarm.

### `QuoteHelper.getQuoteOfDay()`
- **`Calendar.getInstance().get(Calendar.DAY_OF_YEAR)`** — Stable integer for the current date.
- **Modulo `% quotes.length`** — Cycles through the six strings deterministically per day.

### `Book` / `Journal`
- **Getters only** — Pure Java accessors for fields set in constructor or by JSON parsing in activities.

### `BookAdapter`
- **Constructor** — Stores `Context`, `ArrayList<Book>`, and **`BookClickListener`** reference.
- **`BookHolder` constructor** — **`itemView.findViewById`** for each row widget (scoped to the row, not the activity).
- **`onCreateViewHolder`** — **`LayoutInflater.from(c).inflate(R.layout.item_book, parent, false)`** then `new BookHolder(v)`.
- **`onBindViewHolder`** — **`data.get(position)`**; **`setText`**, **`getString(R.string.book_progress_fmt, …)`** for formatted progress; **`Math.max` / `Math.min`** clamp page values for **`ProgressBar.setMax` / `setProgress`**; **`setVisibility`** for favorite star; **`setOnClickListener`** lambdas call **`listener.onEditClick`** (etc.) with the bound `Book`.
- **`getItemCount`** — **`data.size()`**.

### `JournalAdapter`
- Same RecyclerView pattern as `BookAdapter`, different layout ids and **`JournalClickListener`** (includes export).

### `LoginActivity.onCreate()`
- **`setContentView` / `findViewById`** — Wires email, password, buttons.
- **`SessionManager` constructor** — Prepares prefs for later `saveUser`.
- **`btnLogin.setOnClickListener`** — **`TextUtils.isEmpty(edEmail.getText())`** etc.; **`setError(getString(...))`** for inline validation; else **`loginUser()`**.
- **`tvSignup.setOnClickListener`** — **`Intent` to `SignupActivity`** + **`startActivity`**.

### `LoginActivity.loginUser()`
- **`ApiConfig.BASE_URL + "login.php"`** — Full endpoint URL.
- **`Volley.newRequestQueue`** — HTTP worker.
- **`new StringRequest(POST, url, success, error)`** — Anonymous subclass overrides **`getParams()`** returning **`HashMap`** with `"email"` / `"password"` from **`getText().toString()`**.
- **Success handler** — **`new JSONObject(response)`**; **`getBoolean("success")`**; **`getJSONObject("user")`**; **`optString` / `getInt` / `getString`**; **`splitFullName`** if names empty; **`sm.saveUser`**; **`Toast`**; **`Intent` to `MainActivity`**; **`finish()`**.
- **Error handler** — **`Toast` with `NetworkErrorHelper.message`** for long diagnostic text.
- **`queue.add(request)`** — Starts the request.

### `LoginActivity.splitFullName()`
- **`trim()`**, **`indexOf(' ')`**, **`substring`** — Splits on first space into a two-element array.

### `SignupActivity` (mirror of login with extra fields)
- **`onCreate`** — Validates four fields with **`TextUtils.isEmpty`**; navigates to login link with **`Intent`**.
- **`signupUser()`** — POST to **`signup.php`** with **`getParams`** first/last/email/password; **`optBoolean`/`optString`** on response; toast; on success **`loginAfterSignup()`**.
- **`loginAfterSignup()`** — POST **`login.php`** with same credentials; **`saveUser`**; **`Intent` to `ProfileActivity`** with **`putExtra("is_onboarding", true)`** so profile collects birthday/country.

### `MainActivity.onCreate()`
- **`setContentView(R.layout.activity_main)`** — Home layout.
- **`Toolbar` + `setSupportActionBar` + `applyMainToolbarLogo`** — Branding; **`DrawableCompat.setTintList(logo, null)`** clears tint on the logo drawable; **`toolbar.getOverflowIcon().setTint`** tints the overflow icon.
- **`findViewById`** — Binds welcome, Google Books summary/controls, streak, **reading goal** label + **`ProgressBar`**, buttons, **`SearchView`**, **`Spinner`**, **`RecyclerView`**.
- **`SessionManager`** — User name for welcome.
- **`tvWelcome.setText(getString(R.string.welcome_fmt, sm.getUpperFirstName()))`** — Formatted greeting.
- **`tvQuote.setText(R.string.google_books_loading)`** + **`loadGoogleBookSpotlightOfTheDay()`** — Seeds Google Books spotlight panel and starts first fetch.
- **`btnGoogleBooksSearch`** — Triggers `searchGoogleBooksFromInput()` for user-entered queries.
- **`ArrayList` + `BookAdapter`** — **`filteredBooks`** is what the list displays; **`LinearLayoutManager`** vertical list.
- **`addBookLauncher`** — **`registerForActivityResult(StartActivityForResult)`**; if **`RESULT_OK`**, **`getDataFromDB()`** and **`loadReadingStatsForHome()`** refresh list + goal banner.
- **`btnAddBook`** — Launches **`AddBookActivity`** via launcher (for result).
- **`btnJournal`** — **`startActivity` to `JournalActivity`**.
- **`btnFavorites`** — Toggles **`favoriteOnly`**, swaps button label, **`applyFilters()`**.
- **`searchBooks.setOnQueryTextListener`** — Live search → **`applyFilters()`**.
- **`spinnerFilter.setOnItemSelectedListener`** — Status filter → **`applyFilters()`**.

### `MainActivity.onResume()`
- **`super.onResume()`** — Framework hook.
- **`tvWelcome`** refreshed (name may have changed in Profile).
- **`getDataFromDB()`** — Reload books.
- **`markReadingDayThenRefreshStreak()`** — GET **`mark_reading_day.php`** then **`getStreakFromDB()`**.
- **`loadReadingStatsForHome()`** — GET **`get_reading_stats.php`**; updates goal copy, progress bar, **`SessionManager.updateReadingExtras`**.

### `MainActivity.markReadingDayThenRefreshStreak()`
- **GET URL** with **`sm.getUserId()`** query param.
- **`StringRequest`** — On both success and error callbacks calls **`getStreakFromDB()`** so UI still updates if mark-day fails silently.

### `MainActivity.getDataFromDB()`
- **GET `get_books.php?user_id=`** — Parses **`JSONObject` → `JSONArray` "books"`**; loop **`getJSONObject(i)`**; constructs **`Book`** with **`getInt`/`getString`/`optDouble`/`optString`/`optInt`**; **`allBooks.add`**; **`applyFilters()`** to refresh visible subset.
- **Catch** — Toast generic error; Volley error — network toast.

### `MainActivity.getStreakFromDB()`
- **GET `get_streak.php`** — **`getInt("streak")`**; **`tvStreak.setText(getString(main_streak_days, streak))`**; on parse or Volley error shows **0** days.

### `MainActivity.applyFilters()`
- **`filteredBooks.clear()`** — Rebuild list.
- **`searchBooks.getQuery().toString().toLowerCase(Locale.getDefault())`** — Case-insensitive search text.
- **`spinnerFilter.getSelectedItem().toString()`** — Selected status label (must match **`filter_status_array`** including `"All"`).
- **Loop `allBooks`** — **`contains`** on title/author; status match unless `"All"`; favorite filter when **`favoriteOnly`**; **`filteredBooks.add`** when all conditions pass.
- **`adapter.notifyDataSetChanged()`** — RecyclerView redraw.

### `MainActivity.onEditClick`
- **`Intent` + many `putExtra`** — Passes all book fields to **`AddBookActivity`**; **`addBookLauncher.launch`**.

### `MainActivity.onDeleteClick`
- **`MaterialAlertDialogBuilder`** — Confirm; positive button calls **`deleteBook(b)`**.

### `MainActivity.onWebsiteClick`
- **`Uri.parse("https://www.google.com/search?q=" + title)`** — Encodes search (note: not using `Uri.Builder` so exotic titles could need encoding in a stricter app).
- **`Intent(ACTION_VIEW, page)`** — **`resolveActivity(getPackageManager())`** null-check; else **`startActivity`** or toast no browser.

### `MainActivity.deleteBook`
- **GET `delete_book.php?id=`** — On success toast + **`getDataFromDB()`** + **`loadReadingStatsForHome()`**; on error toast.

### `MainActivity.loadReadingStatsForHome()`
- Parses **`get_reading_stats.php`** JSON: **`reading_goal`** with **`type`**, **`value`**, progress fields (**`pages_read_this_week`** or **`books_finished_this_year`**); toggles **`ProgressBar`** visibility; sets **`tvReadingGoal`** with formatted strings or **`main_reading_goal_none`**.

### `MainActivity.onCreateOptionsMenu`
- **`inflate(R.menu.menu_main_shelfie, menu)`** — Theme + optional admin dashboard + profile + settings + logout.
- **`action_theme`** — Sun vs moon icon and title from **`ThemeManager.isDarkModeEnabled`**.
- **`action_admin_dashboard` visibility** — Set from **`SessionManager.isAdmin()`**.

### `MainActivity.onOptionsItemSelected`
- **`action_theme`** — Toggle dark mode, **`invalidateOptionsMenu`**, **`recreate()`**.
- **`action_admin_dashboard`** — Opens `ApiConfig.adminDashboardUrl()` in browser when available.
- **`action_logout`** — **`sm.logout()`**, **`LoginActivity`**, **`finish()`**.
- **`action_settings`** / **`action_profile`** — **`startActivity`** to **`SettingsActivity`** / **`ProfileActivity`**.

### `MainActivity.applyMainToolbarLogo`
- **`toolbar.setLogo`** — Light vs dark drawable from **`ThemeManager`**.

### `AddBookActivity.onCreate()`
- Toolbar setup like other screens; **`ContextCompat.getColor`** for overflow tint.
- **`readIntExtra("id", -1)`** from intent; if not `-1`, **`loadBookForEdit()`**; else default **`rbRecommendedNo.setChecked(true)`**.
- **`pb.setVisibility(INVISIBLE)`**, **`ratingBook`** interaction flags.

### `AddBookActivity.loadBookForEdit()`
- **`readStringExtra` / `readIntExtra` / `readFloatExtra`** — Populates widgets; **`setSpinnerStatus`**, **`switchFavorite.setChecked`**, **`ratingBook.setRating`**, **`applyGenresToCheckboxes`**, radio group for recommended; sets **`initialCurrentPageForEdit`** from loaded **`current_page`** for later **`page_delta`**.

### `AddBookActivity.readStringExtra` / `readIntExtra` / `readFloatExtra`
- **`getIntent().getExtras()`** — Null-safe; **`containsKey`**; for ints/floats handles **`Integer`/`Long`/`Float`/`Double`** boxed types from intents; **`Integer.parseInt`** / **`Float.parseFloat`** in **`try/catch`**.

### `AddBookActivity.saveBook()`
- **`pb.setVisibility(VISIBLE)`**, **`clearFieldErrors()`** — UX for loading + clean validation state.
- Reads all fields with **`getText().toString().trim()`** (or spinner **`getSelectedItem().toString()`**); **`ratingBook.getRating()`**; **`getSelectedGenres()`**; ternary for recommended and favorite flags.
- Validation: empty checks set **`TextInputLayout.setError`**; **`Integer.parseInt`** for pages with **`NumberFormatException`**; compares current vs total pages; rating non-zero; genres non-empty; on any error **`Toast`**, hide progress, **`return`**.
- **`SessionManager.getUserId()`** — Builds URL via **`buildBookUrl`** for **`add_book.php`** or **`update_book.php?id=`**; on edit, **`Uri.buildUpon().appendQueryParameter("page_delta", …)`** where delta = **`max(0, currentPageInt - initialCurrentPageForEdit)`**.
- **`StringRequest(Request.Method.GET, url, …)`** — On success **`setResult(RESULT_OK)`**, **`finish()`**; on error toast + hide **`pb`**.

### `AddBookActivity.buildBookUrl()`
- **`Uri.parse(base).buildUpon()`** — **`appendQueryParameter`** for each field; adds **`user_id`** only for add endpoint (checks **`base.contains("add_book.php")`**); returns **`builder.build().toString()`**.

### `AddBookActivity.getSelectedGenres` / `appendGenreIfChecked`
- **`StringBuilder`** accumulates comma-separated labels from checked boxes.

### `AddBookActivity.applyGenresToCheckboxes` / `containsToken`
- Lowercases stored string; **`containsToken`** uses **`Pattern.quote`** inside a regex so genre names with special regex chars do not break matching; ensures whole comma-separated tokens match.

### `AddBookActivity.clearFieldErrors`
- Sets all **`TextInputLayout.setError(null)`** and hides error **`TextView`s**.

### `AddBookActivity.onCreateOptionsMenu` / `onOptionsItemSelected`
- **`action_save`** → **`saveBook()`**; **`action_settings`** → **`Intent` to `SettingsActivity`**.

### `JournalActivity.onCreate()`
- If **`!ThemeManager.isBiometricLockEnabled`**, **`loadJournalScreen()`** immediately and **`return`**.
- Else **`getAuthResult()`** — **`canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)`**; on success **`showBiometricPrompt()`**; on **`BIOMETRIC_ERROR_NONE_ENROLLED`** long toast + **`finish()`**; else short toast + **`finish()`**.

### `JournalActivity.showBiometricPrompt()`
- **`ContextCompat.getMainExecutor`** — Runs callback on UI thread.
- **`new BiometricPrompt(this, executor, AuthenticationCallback)`** — **`onAuthenticationSucceeded`** calls **`loadJournalScreen()`**; **`onAuthenticationError`** toast + **`finish()`**.
- **`PromptInfo.Builder`** — Title, subtitle, allowed authenticators; **`authenticate`**.

### `JournalActivity.loadJournalScreen()`
- **`setContentView`** — Only after auth (so journal layout not leaked under lock).
- **`RecyclerView` + `JournalAdapter` + `LinearLayoutManager`**; FAB launches **`AddJournalActivity`** with **`addJournalLauncher`**; **`getJournalsFromDB()`**.

### `JournalActivity.getJournalsFromDB()`
- Same Volley+JSON pattern as books; array key **`"journals"`**; **`Journal` objects**; **`adapter.notifyDataSetChanged()`**.

### `JournalActivity.onExportClick`
- Stores **`journalToExport`**; **`Intent(ACTION_CREATE_DOCUMENT)`** with MIME **`text/plain`** and suggested filename; **`exportLauncher.launch`** (Storage Access Framework).

### `JournalActivity.exportJournalToFile`
- **`getContentResolver().openOutputStream(uri)`** — Writes UTF-8 bytes built from title/mood/date/content; **`out.close()`**; toast success/failure.

### `JournalActivity.deleteJournal`
- GET **`delete_journal.php?id=`** — Toast + reload list.

### `AddJournalActivity.onCreate()`
- Toolbar; **`getIntExtra("id", -1)`**; **`SimpleDateFormat.format(new Date())`** for new entry **`entryDate`**; if edit, fills fields and mood spinner indices with **`if/else` chain** on mood string.

### `AddJournalActivity.onOptionsItemSelected`
- **`action_save`** → **`saveJournal()`**; **`action_settings`** → **`SettingsActivity`**.

### `AddJournalActivity.saveJournal()`
- **`TextUtils.isEmpty`** on title/content; builds **`Uri.Builder`** for **`add_journal.php`** (with **`user_id`**) or **`update_journal.php`** (with **`id`**); appends title/content/mood/entry_date; Volley GET; success **`setResult(RESULT_OK)`** + **`finish()`**.

### `ProfileActivity` — activity result launchers
- **`TakePicturePreview`** — Returns thumbnail bitmap to **`applyBitmapToAvatar`**.
- **`RequestPermission`** for camera — On grant, launches camera preview; else toast.
- **`RequestPermission`** for gallery — On grant, **`launchGalleryPicker()`**; uses **`READ_MEDIA_IMAGES`** on API 33+ else **`READ_EXTERNAL_STORAGE`**.
- **`StartActivityForResult` for `ACTION_PICK`** — **`getData().getData()`** content `Uri`; **`try-with-resources`** on **`openInputStream`**; **`BitmapFactory.decodeStream`**; **`applyBitmapToAvatar`**.

### `ProfileActivity.onCreate()`
- **`MaterialToolbar`**, **`setSupportActionBar`**, Up enabled.
- **`getIntent().getBooleanExtra("is_onboarding", false)`** — Changes title via **`ActionBar.setTitle`**.
- Prefills names/email from **`SessionManager`**; email field typically read-only in layout; birthday click opens picker.

### `ProfileActivity.confirmRemoveProfilePhoto` / `chooseImage`
- **`MaterialAlertDialogBuilder`** chains; list dialog uses **`getString`** for items.

### `ProfileActivity.openCameraWithPermission` / `openGalleryWithPermission`
- **`ContextCompat.checkSelfPermission`** vs **`PackageManager.PERMISSION_GRANTED`**; launch permission request or proceed.

### `ProfileActivity.launchGalleryPicker`
- **`Intent(ACTION_PICK)`**, **`setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")`** — Standard gallery pick.

### `ProfileActivity.applyBitmapToAvatar`
- **`bmp.compress(JPEG, 80, bos)`** + **`Base64.encodeToString`**; clears **`pendingPhotoRemoval`**.

### `ProfileActivity.showDatePicker`
- **`DatePickerDialog`** with **`Calendar.getInstance()`** initial date; lambda **`String.format(Locale, "%04d-%02d-%02d", …)`** — note **`month + 1`** because **`DatePicker` month is 0-based.

### `ProfileActivity.loadProfile`
- GET with **`sm.getUserId()`**; JSON user object; **`splitName`** fallback; **`sm.updateProfile`** caches; **`sm.updateReadingExtras`** for goal fields; image path decodes Base64 or placeholder; **`pendingPhotoRemoval`** reset appropriately.

### `ProfileActivity.saveProfile`
- **`safeText`** trims inputs; field errors for onboarding requirements; validates goal type vs numeric target.
- POST **`update_profile.php`** with anonymous **`getParams`**: includes **`remove_profile_image=1`** when **`pendingPhotoRemoval`**; **`image_base64`**; **`reading_goal_type`**, **`reading_goal_value`**.
- Success: **`sm.updateProfile`**; **`sm.updateReadingExtras`**; clear **`pendingPhotoRemoval`**; toast; onboarding → **`MainActivity`** + **`finish()`**, else **`finish()`**.

### `ProfileActivity.safeText`
- **`getText()`** null-safe → **`toString().trim()`**.

### `SettingsActivity` — notification permission launcher
- On grant: **`ThemeManager.setReadingRemindersEnabled(true)`**, **`ReadingReminderScheduler.sync`**, temporarily **`bindingSwitches`** true to **`setChecked(true)`** without re-entering listener logic.

### `SettingsActivity.onCreate()`
- **`bindingSwitches = true`** while initializing switch states from **`ThemeManager`** — prevents listeners during setup.
- **`switchNotifications`** — API 33+ may launch **`POST_NOTIFICATIONS`** request; else toggles prefs + **`ReadingReminderScheduler.sync`**.
- **`switchBiometric`** / **`switchDarkMode`** — **`ThemeManager`**; dark mode **`recreate()`**.
- **MaterialButtons** — change password, delete account, stats, export (dialog), web, map, dial, direct call (**`support_phone_number`** string).

### `SettingsActivity.showDeleteAccountDialog` / `requestDeleteAccount`
- **`showDeleteAccountDialog`** — Inflates `dialog_delete_account`, validates password non-empty, then proceeds.
- **`requestDeleteAccount`** — POST `delete_account.php`; on success toast + `sm.logout()` + navigate to `LoginActivity` with cleared task.

### `SettingsActivity.performExport` / `requestCallPermissionAndCall`
- Export: Volley GET **`export_shelf_journal.php`**; optional **`ExportHelper.jsonToCsv`**; **`ACTION_SEND`** with text body.
- Call: **`CALL_PHONE`** gate then **`ACTION_CALL`**; **`onRequestPermissionsResult`** retries with **`pendingCallNumber`**.

### `ReadingStatsActivity.loadStats()`
- **`SessionManager.getUserId()`** — if **`<= 0`**, toast and **`finish()`** (invalid session).
- Volley GET **`get_reading_stats.php`** — shows/hides **`progressStatsLoading`**; success → **`applyJson`**.

---

## 4. Resource files (summary)

### Layouts (`res/layout/`)

| File | Role |
|------|------|
| `activity_splash.xml` | Splash branding + progress. |
| `activity_login.xml` / `activity_signup.xml` | Auth screens (often include `content` or header). |
| `activity_main.xml` + `content_main.xml` | Home scaffold + main content (toolbar with **theme** action + overflow, welcome + Google Books spotlight/search panel + optional preview button, streak, **reading goal** label + tinted **`ProgressBar`**, shelf search/filter, **Add book** / **Journal** / **Favorites** buttons, list). |
| `activity_reading_stats.xml` | Reading statistics toolbar + summary, loading indicator, scrollable chart containers. |
| `item_book.xml` | Single book row. |
| `activity_add_book.xml` + `content_add_book.xml` | Add/edit book form. |
| `activity_journal.xml` + `content_journal.xml` | Journal list screen. |
| `item_journal.xml` | Single journal row. |
| `activity_add_journal.xml` + `content_add_journal.xml` | Journal editor. |
| `activity_profile.xml` + `content_profile.xml` | Profile editor + avatar. |
| `activity_change_password.xml` + `include_password_requirements.xml` | Change-password screen with live client-side requirement checklist. |
| `activity_settings.xml` + `content_settings.xml` | Settings switches plus **Account**, **Reading & data**, and **Discover & support** sections. |
| `dialog_delete_account.xml` | Password-confirm delete-account dialog content. |

### Menus (`res/menu/`)

| File | Role |
|------|------|
| `menu_main_shelfie.xml` | **`action_theme`** (`showAsAction="always"`), optional **`action_admin_dashboard`** (shown for admins), plus overflow: profile, settings, logout. |
| `menu_add_book.xml` / `menu_add_journal.xml` | **Save** (`showAsAction="always"`) plus overflow **Settings** on both editors. |

### Drawables / mipmaps
Vector icons (`ic_*`), card/background shapes (`bg_*`), light/dark variants in `drawable-night/`, launcher assets under `mipmap-*` and `mipmap-anydpi-v26/`. **`profile_placeholder.xml`** — default avatar when no photo.

### XML (`res/xml/`)
- No custom XML resources are currently required by the Android app module.

### Values
- **`colors.xml` / `values-night/colors.xml`:** Material 3–style palette (primary green, parchment background, etc.) and night overrides; **`main_reading_goal_bar_fill`** / **`main_reading_goal_bar_track`** tint the home goal **`ProgressBar`**.  
- **`themes.xml` / `values-night/themes.xml`:** `Theme.Shelfie`, splash theme, card/popup styles, status bar behavior.  
- **`dimens.xml`:** Spacing and sizes.  
- **`attrs.xml`:** Custom attributes (e.g. toolbar logo).  
- **`bools.xml` / `values-night/bools.xml`:** Light/dark system bar icon flags.

---

## 5. `strings.xml` — every string resource and its purpose

| Name | Text (summary) | Purpose |
|------|----------------|---------|
| `app_name` | Shelfie | Launcher label. |
| `cd_app_logo` / `cd_app_slogan` | Logo / slogan | TalkBack content descriptions. |
| `login_subtitle` | Sign in to your shelf | Login screen subtitle. |
| `signup_subtitle` | Create your reader profile | Signup subtitle. |
| `hint_email` / `hint_password` / `hint_first_name` / `hint_last_name` | Field hints | Input placeholders. |
| `action_login` / `action_create_account` | Sign in / Create account | Primary buttons. |
| `link_no_account` / `link_have_account` | Cross-navigation between login and signup. |
| `error_network` | Cannot reach server… | Volley no-connection copy. |
| `error_network_url` | Trying: %1$s | Shows `BASE_URL` for debugging. |
| `error_timeout` | Request timed out… | Timeout copy. |
| `error_generic` | Something went wrong… | Generic failure. |
| `signup_success` | Account created | Post-signup toast. |
| `welcome_fmt` | Welcome, %1$s | Main greeting with name. |
| `main_quote_label` / `main_streak_label` | Labels for quote and streak UI. |
| `main_streak_days` | %1$d day(s) | Streak count format. |
| `search_hint` / `main_search_prompt` | Search field hint / helper. |
| `filter_label` | Status | Spinner label. |
| `title_add_book` / `title_journal_screen` / `title_add_journal` | Activity titles. |
| `hint_book_title` … `hint_journal_body` | Form hints for books and journal. |
| `label_reading_status` … `label_recommended` | Book form section labels. |
| `recommended_yes` / `recommended_no` | Recommendation toggle labels. |
| `genre_*` | Fiction, Mystery, … | Genre checkbox labels. |
| `label_mood` | Mood | Journal mood spinner. |
| `btn_add_book` / `btn_journal` / `btn_new_journal_entry` | Main navigation buttons. |
| `btn_favorites_off` / `btn_favorites_on` | Toggle favorites filter. |
| `hint_google_books_search` / `btn_google_books_search` / `google_books_*` | Home Google Books search/spotlight prompt, loading/empty states, and preview button copy. |
| `menu_web` / `menu_dial` / `menu_call_direct` / `menu_map` | Labels for **Settings** screen buttons (not main toolbar). |
| `menu_logout` / `menu_profile` / `menu_theme_toggle` / `menu_admin_dashboard` | Account and theme toolbar actions (admin dashboard is conditional). |
| `menu_light_mode` / `menu_dark_mode` | Theme submenu. |
| `menu_save` / `action_settings` | Save / settings. |
| `title_settings` / `settings_subtitle` | Settings screen header. |
| `settings_notifications_*` | Reading reminders title + description. |
| `settings_biometric_*` | Journal lock title + description. |
| `settings_theme_*` | Theme section copy. |
| `settings_account_section` / `action_change_password` / `settings_change_password_desc` | Account security section and change-password action text. |
| `action_delete_account` / `dialog_delete_account_*` / `toast_account_delete_*` | Account deletion confirmation and outcome feedback. |
| `password_rule_*` / `password_req_*` / `error_password_requirements` / `error_confirm_password_mismatch` | Password policy checklist rows, markers, and validation errors. |
| `settings_privacy_*` | Privacy blurb. |
| `settings_reading_data_*` / `settings_discover_*` | Section headers on Settings. |
| `support_phone_number` | Phone literal for dial/call buttons (`translatable="false"`). |
| `title_reading_stats` / `stats_*` | Reading statistics screen copy (summary, sections, empty states, streak footer note). |
| `menu_reading_stats` / `menu_export_backup` | Button labels on Settings (stats, export). |
| `export_*` / `toast_export_failed` | Export format dialog and share subject. |
| `toast_no_share_handler` | No app available to handle export sharing intent. |
| `main_reading_goal_label` | Reading goal | Visible section label for the home goal area. |
| `main_reading_goal_none` / `main_reading_goal_pages_week_fmt` / `main_reading_goal_books_year_fmt` | Home goal banner copy when none set, pages/week, or books/year. |
| `profile_reading_goal_section` / `profile_reading_goal_type` / `hint_reading_goal_value` | Profile reading goal UI. |
| `cd_search_web` / `cd_edit` / `cd_delete` / `cd_export` | TalkBack labels for row action buttons. |
| `cd_favorite` | TalkBack for the favorite star on a book row (**visibility-only**; toggling favorites is done in **Add/Edit book**, not from the list). |
| `dialog_delete_book_*` / `dialog_delete_journal_*` | Delete confirmation title/body. |
| `dialog_positive_delete` / `dialog_negative_cancel` | Delete dialog buttons (`Keep it` = cancel). |
| `toast_book_deleted` / `toast_book_delete_failed` | Book delete feedback. |
| `toast_journal_deleted` / `toast_journal_delete_failed` | Journal delete feedback. |
| `toast_invalid_form` | Generic form validation. |
| `error_book_*` / `error_rating_required` / `error_genres_required` | Add-book validation messages. |
| `toast_network_problem` | Longer network message for some flows. |
| `title_profile` / `title_profile_onboarding` | Profile toolbar titles. |
| `hint_birthday` / `hint_country` | Profile fields. |
| `action_add_photo` / `action_remove_profile_photo` | Photo buttons. |
| `dialog_remove_profile_photo_*` | Remove photo confirmation. |
| `dialog_positive_remove` | Confirm remove. |
| `toast_profile_photo_removed` | Reminds user to save. |
| `profile_photo_source_title` / `profile_photo_take_picture` / `profile_photo_choose_gallery` | Photo source picker. |
| `permission_camera_denied` / `permission_gallery_denied` | Permission rationale toasts. |
| `cd_profile_photo` | Avatar content description. |
| `toast_profile_loaded` / `toast_profile_saved` / `toast_profile_failed` | Profile API feedback. |
| `error_first_name_required` … `error_country_required` | Profile onboarding validation. |
| `toast_login_success` | After login. |
| `error_login_parsing` | Bad JSON from server. |
| `error_field_*_empty` | Empty field errors on auth forms. |
| `toast_no_browser` / `toast_no_maps` / `toast_no_dial` | Missing activity to handle intent. |
| `toast_call_permission_denied` | `CALL_PHONE` denied. |
| `journal_biometric_*` | Biometric prompt strings. |
| `error_biometric_*` | Setup / availability errors. |
| `toast_journal_exported` / `toast_journal_export_failed` | Export result. |
| `notification_reading_title` / `notification_reading_text` | Reminder notification copy. |
| `toast_notifications_permission_denied` | POST_NOTIFICATIONS denied. |
| `book_progress_fmt` | %1$d / %2$d pages | Progress line in list. |

**Comment in `strings.xml` (line 2):** Reminds you to add `@drawable/icon_simple` / slogan assets — documentation for designers, not shown in UI.

### String arrays

| Array | Items | Purpose |
|-------|--------|---------|
| `book_status_array` | Reading, Completed, Paused, Wishlist | Values stored with a book. |
| `filter_status_array` | All + same four | Main screen filter. |
| `mood_array` | Happy, Calm, Neutral, Sad, Excited, Stressed | Journal mood picker. |
| `reading_goal_type_labels` | None, Pages per week, Books per year | Profile goal type spinner. |

---

## 6. `QuoteHelper` — literal quote sentences

Each line is motivational reading copy; the app picks **one per calendar day** (index = `DAY_OF_YEAR % 6`).

1. *A reader lives a thousand lives before he dies.* — Emphasizes imagination and breadth of experience through reading.  
2. *Books are a uniquely portable magic.* — Highlights convenience and wonder of books.  
3. *Reading is dreaming with open eyes.* — Links reading to creativity and inner vision.  
4. *Small reading progress is still progress.* — Encourages consistency over volume.  
5. *Every page counts.* — Reinforces incremental effort.  
6. *Today a reader, tomorrow a leader.* — Classic encouragement tying reading to growth.

---

## 7. Other important literals (not in `strings.xml`)

| Literal | Where | Meaning |
|---------|--------|---------|
| `ShelfieSession` | `SessionManager` | SharedPreferences file name. |
| `shelfie_preferences` | `ThemeManager` | Theme/reminder/biometric prefs file. |
| `dark_mode_enabled`, `biometric_lock_enabled`, `reading_reminders_enabled` | `ThemeManager` | Keys inside `shelfie_preferences`. |
| `reading_channel` | `ReminderReceiver` | Android notification channel id. |
| `7101` | `ReadingReminderScheduler` | Alarm `PendingIntent` request code. |
| `3000` (ms) | `SplashActivity` | Splash delay. |
| `remove_profile_image` | `ProfileActivity` → POST | **`1`** tells PHP to clear `profile_image`. |
| `com.example.shelfie` | Gradle `namespace` / `applicationId` | Package id. |
| `is_admin` | `SessionManager` | Boolean session flag controlling admin dashboard visibility on the home toolbar. |
| `REQUEST_CALL_PERMISSION` `100` | `SettingsActivity` | Request code for **`CALL_PHONE`**. |
| `page_delta` | `AddBookActivity` → `update_book.php` | Non‑negative page increase since edit screen opened; server uses it for weekly totals. |
| `GOOGLE_BOOKS_API_KEY` | `ApiConfig` | Key used for Google Books metadata lookup from home screen (should be key-restricted in Google Cloud Console). |
| `1.0` / `versionCode 1` | `app/build.gradle.kts` | App version metadata. |
| `minSdk 24`, `targetSdk`/`compileSdk 36` | `app/build.gradle.kts` | Device and build API levels. |

---

## 8. PHP API (`shelfie_api`)

Typical URL base matches `ApiConfig` (emulator: `10.0.2.2`, port **8080** if Apache listens there). Most scripts return JSON. **Schema files (`shelfie.sql`, `schema_extensions.sql`, `admin_dashboard.php`) live only under your web root**, e.g. **`…/xamppfiles/htdocs/shelfie_api/`** — they are **not** part of the Android Gradle project. **New DB:** import **`shelfie.sql`** in phpMyAdmin. **Upgrading an older DB:** run **`schema_extensions.sql`** once if you need **`is_admin`**. `users` includes **`reading_goal_type`**, **`reading_goal_value`**, **`pages_read_week_total`**, **`pages_week_key`**; `books` may include **`completed_at`**. Existing DBs that had **`public_slug`**: run **`migrate_drop_public_slug.sql`** if you still use that migration.

| File | Role |
|------|------|
| `connection.php` | MySQLi connection include. |
| `login.php` | Validates credentials; returns user fields including **`is_admin`** for the Android admin toolbar. |
| `admin_dashboard.php` | Web admin UI (HTML); same folder as other API scripts. Uses **`connection.php`** (`$con`). |
| `shelfie.sql` / `schema_extensions.sql` | Full schema vs incremental upgrade (**`is_admin`**, etc.); import in phpMyAdmin from **`htdocs/shelfie_api/`**. |
| `signup.php` | Creates user; errors if email exists. |
| `get_profile.php` | Returns user row + `image_base64`; includes **reading goal** fields when present. |
| `update_profile.php` | Updates names, birthday, country; **reading goal**; sets image from Base64, **clears image if `remove_profile_image=1`**. |
| `get_books.php` | Lists books for `user_id`. |
| `add_book.php` / `update_book.php` | Create/update book rows; may set **`completed_at`** when status becomes Completed; **`update_book.php`** accepts optional **`page_delta`** to roll **`pages_read_week_total`** forward for the user’s current ISO week (**`pages_week_key`**). **`bind_param` types for the weekly user update must match columns** (e.g. integer totals + string week key) — a type mismatch can leave weekly stats stuck at zero. |
| `delete_book.php` | Deletes by book id. |
| `toggle_favorite.php` | Flips favorite flag. |
| `get_journals.php` | Lists journal entries for user. |
| `add_journal.php` / `update_journal.php` | Create/update entries. |
| `delete_journal.php` | Deletes by entry id. |
| `get_streak.php` | Returns reading streak count. |
| `mark_reading_day.php` | Records activity for streak. |
| `get_reading_stats.php` | Aggregates streak, **total pages across shelf** (capped per book), monthly finished books, genre counts, goal progress; JSON used by **Main** and **ReadingStatsActivity**. |
| `export_shelf_journal.php` | **`?user_id=`** — JSON bundle of books + journals for backup/share. |
| `change_password.php` | Verifies current password and updates to a new one. |
| `delete_account.php` | Deletes user account after password confirmation; app then logs out and returns to login. |

**Common JSON `message` literals returned by PHP** (user may see these only if you surface server messages): e.g. `Invalid profile data`, `Invalid image encoding`, `Profile updated`, `Update failed: …`, `Missing required fields`, `Account created`, `Email already exists`, `Book added` / `Book updated` / `Book deleted`, `Journal added` / `Journal updated` / `Journal deleted`, `Favorite updated`, `Missing user_id`, `Reading day marked`, etc. Exact wording lives in each `.php` file.

**If weekly page counts were wrong before a server fix:** reset or correct **`pages_week_key`** / **`pages_read_week_total`** in MySQL for affected users, or let the next week’s key roll naturally after deploying corrected PHP.

---

## 9. Generated / build outputs

Under `app/build/` (generated manifests, merged resources, data binding classes) — **do not edit**; Gradle regenerates them. Documentation above applies to sources under `app/src/main/`.

---

## 10. How to make the project work and run the app on your laptop

Shelfie is two parts: the **Android app** (this Gradle project) and the **PHP + MySQL API** (typically in XAMPP). Both must agree on **URL**, **port**, and **database** settings.

### 10.1 Prerequisites

1. **Android Studio**  
   Install a recent **Android Studio** (compatible with **Android Gradle Plugin 8.13.x** used in this project). During setup, install:
   - Android SDK (includes **Platform Tools**, **Build-Tools**),
   - At least one **Android Virtual Device (AVD)** *or* prepare a **physical phone** with USB debugging.

2. **JDK**  
   The project targets **Java 11** (`app/build.gradle.kts`). Android Studio bundles a suitable JDK; use **File → Settings → Build, Execution, Deployment → Build Tools → Gradle** and set **Gradle JDK** to the embedded JDK if builds complain.

3. **Backend stack (XAMPP or similar)**  
   Install **XAMPP** (or MAMP/WAMP) with:
   - **Apache** (serves `shelfie_api/*.php`),
   - **MySQL/MariaDB** (stores data).

### 10.2 Database setup

1. Start **MySQL** from the XAMPP control panel.
2. Open **phpMyAdmin** (usually `http://localhost/phpmyadmin`).
3. Import **`shelfie.sql`** from **`htdocs/shelfie_api/`** in phpMyAdmin (creates database and tables).  
   Your **`connection.php`** in that same folder expects database name **`shelfie_db`**, user **`root`**, empty password by default — adjust **`connection.php`** if your MySQL user/password differ.

### 10.3 Deploy the PHP API

1. Keep the entire **`shelfie_api`** API (all **`.php`**, **`shelfie.sql`**, **`schema_extensions.sql`**) under the web root, e.g.  
   `.../xamppfiles/htdocs/shelfie_api/`  
   The Android Studio project does **not** contain this folder; edit and deploy PHP/SQL only there (or your host’s equivalent path).
2. Confirm a script opens in the browser, e.g.  
   `http://localhost/shelfie_api/get_profile.php?user_id=1`  
   (You may see a JSON error until a valid user exists — that still proves Apache is serving the folder.)

**Port note:** Default Apache is often **port 80** (`http://localhost/...`). If you use **8080** (common when 80 is busy), your Android app’s `ApiConfig.BASE_URL` must include **`:8080`**.

### 10.4 Point the Android app at your laptop

Open **`app/src/main/java/com/example/shelfie/ApiConfig.java`**:

- **Android Emulator**  
  The special IP **`10.0.2.2`** is the emulator’s alias for the host machine’s loopback.  
  Example if Apache serves `shelfie_api` on port **8080**:

  `http://10.0.2.2:8080/shelfie_api/`

  If Apache uses port **80**, omit the port:

  `http://10.0.2.2/shelfie_api/`

- **Physical phone (same Wi‑Fi as the laptop)**  
  Use your laptop’s **LAN IP** (e.g. `192.168.1.5`), not `localhost` and not `10.0.2.2`:

  `http://192.168.1.5:8080/shelfie_api/`

  Find the IP: **macOS** System Settings → Network; **Windows** `ipconfig` in Command Prompt.

**Firewall:** Allow incoming connections on Apache’s port from your LAN if the phone cannot reach the laptop.

**Cleartext HTTP:** The manifest sets **`android:usesCleartextTraffic="true"`** so `http://` works for local development. For production you would use **HTTPS** and tighten this.

### 10.5 Open and sync the Android project

1. In Android Studio: **File → Open** → select the **`Shelfie`** folder (the one containing **`settings.gradle.kts`**).
2. Wait for **Gradle sync** to finish. If it fails, check the **Build** tool window for missing SDK components; install suggested packages via the SDK Manager.
3. If a **local.properties** error appears, ensure Android Studio created **`local.properties`** with `sdk.dir=...` (usually automatic).

### 10.6 Run the app

1. Start **Apache** (and **MySQL**) in XAMPP.
2. In Android Studio, choose a **device**:
   - **Emulator:** start an AVD from **Device Manager**, then click **Run** (green triangle).
   - **Phone:** enable **Developer options** → **USB debugging**, connect USB, authorize the computer, select the phone as the run target.
3. Click **Run ▶** (or **Shift+F10**). The app installs and launches **SplashActivity** → then **Login** or **Main** depending on session.

### 10.7 First-time use

1. **Create account** on **Signup** → you should be taken through **Profile** onboarding if that flow is enabled.
2. If login fails with network errors, the toast may show **`Trying: …`** with your `BASE_URL`. Verify:
   - That exact URL opens in the **laptop browser**,
   - Port and path match **`htdocs/shelfie_api/`**,
   - Emulator vs phone uses **`10.0.2.2`** vs **LAN IP** correctly.



*End of reference. For behavior details that change frequently, trust the source file next to the code; update this document when you add screens or API fields.*
