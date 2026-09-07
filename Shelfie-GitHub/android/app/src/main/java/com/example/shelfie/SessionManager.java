package com.example.shelfie;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

//reads and writes login and profile fields in SharedPreferences so the app stays logged in across restarts

public class SessionManager {

    SharedPreferences pref;
    SharedPreferences.Editor editor;

    //opens the ShelfieSession preference file and prepares the editor for writes
    public SessionManager(Context c)
    {
        pref = c.getSharedPreferences("ShelfieSession", Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    //Stores user id, names, email and marks the session logged in after login or signup
    public void saveUser(int id, String firstName, String lastName, String email) {
        saveUser(id, firstName, lastName, email, false);
    }

    /** @param isAdmin when true, the home toolbar can offer the admin dashboard link (server must set is_admin on the user row). */
    public void saveUser(int id, String firstName, String lastName, String email, boolean isAdmin) {
        editor.putBoolean("isLoggedIn", true);
        editor.putInt("user_id", id);
        editor.putString("first_name", firstName);
        editor.putString("last_name", lastName);
        editor.putString("full_name", buildFullName(firstName, lastName));
        editor.putString("email", email);
        editor.putBoolean("is_admin", isAdmin);
        editor.apply();
    }

    public boolean isAdmin() {
        return pref.getBoolean("is_admin", false);
    }

    public void updateProfile(String firstName, String lastName, String birthday, String country, String imageUrl)
    {
        editor.putString("first_name", firstName);
        editor.putString("last_name", lastName);
        editor.putString("full_name", buildFullName(firstName, lastName));
        editor.putString("birthday", birthday);
        editor.putString("country", country);
        editor.putString("image_url", imageUrl);
        editor.apply();
    }

    public void updateReadingExtras(String readingGoalType, int readingGoalValue) {
        editor.remove("public_slug");
        editor.putString("reading_goal_type", readingGoalType == null ? "" : readingGoalType);
        editor.putInt("reading_goal_value", readingGoalValue);
        editor.apply();
    }

    public String getReadingGoalType() {
        return pref.getString("reading_goal_type", "");
    }

    public int getReadingGoalValue() {
        return pref.getInt("reading_goal_value", 0);
    }

    public boolean isLoggedIn()
    {
        return pref.getBoolean("isLoggedIn", false);
    }

    public int getUserId()
    {
        return pref.getInt("user_id", -1);
    }

    public String getFullName()
    {
        return buildFullName(getFirstName(), getLastName());
    }

    public String getFirstName() { return pref.getString("first_name", ""); }

    public String getLastName() { return pref.getString("last_name", ""); }

    public String getEmail()
    {
        return pref.getString("email", "");
    }

    public String getBirthday() { return pref.getString("birthday", ""); }

    public String getCountry() { return pref.getString("country", ""); }

    public String getImageUrl() { return pref.getString("image_url", ""); }

    public String getUpperFirstName() {
        return getFirstName().toUpperCase(Locale.getDefault());
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        if (first.isEmpty()) return last;
        if (last.isEmpty()) return first;
        return first + " " + last;
    }

    public void logout()
    {
        editor.clear();
        editor.apply();
    }
}
