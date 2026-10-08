package com.example.shelfie;

public class ApiConfig {

    // For emulator
    public static final String BASE_URL = "http://10.0.2.2:8080/shelfie_api/";

    //for real phone use your laptop IP instead:
    //public static final String BASE_URL = "http://192.168.1.16:8080/shelfie_api/";

    //Web admin console: {admin_dashboard.php} lives under XAMPP {htdocs/shelfie_api/} with the other PHP files, not in this Android project
    public static String adminDashboardUrl() {
        return BASE_URL + "admin_dashboard.php";
    }

    // Set GOOGLE_BOOKS_API_KEY in local.properties (not committed to GitHub).
    public static final String GOOGLE_BOOKS_API_KEY = BuildConfig.GOOGLE_BOOKS_API_KEY;
}
