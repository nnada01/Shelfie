package com.example.shelfie;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.regex.Pattern;

// Add or edit a book: validates fields, builds a GET URL with query params, and calls add_book.php or update_book.php

public class AddBookActivity extends AppCompatActivity {

    // Declare all variables
    EditText edTitle, edAuthor, edCategory, edTotalPages, edCurrentPage;
    TextInputLayout tilTitle, tilAuthor, tilCategory, tilTotalPages, tilCurrentPage;
    Spinner spinnerStatus;
    SwitchMaterial switchFavorite;
    RatingBar ratingBook;
    TextView tvRatingError, tvGenresError;
    RadioGroup rgRecommended;
    RadioButton rbRecommendedYes, rbRecommendedNo;
    CheckBox cbGenreFiction, cbGenreMystery, cbGenreRomance, cbGenreFantasy, cbGenreBiography;
    ProgressBar pb;

    int bookId = -1;

    // Binds views, reads optional id extra for edit mode, and either loads existing data or defaults recommended to No
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_book);

        // Reference all variables
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbarAddBook);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(
                    ContextCompat.getColor(this, R.color.toolbar_content));
        }

        edTitle = (EditText) findViewById(R.id.edTitle);
        edAuthor = (EditText) findViewById(R.id.edAuthor);
        edCategory = (EditText) findViewById(R.id.edCategory);
        edTotalPages = (EditText) findViewById(R.id.edTotalPages);
        edCurrentPage = (EditText) findViewById(R.id.edCurrentPage);

        tilTitle = (TextInputLayout) findViewById(R.id.tilTitle);
        tilAuthor = (TextInputLayout) findViewById(R.id.tilAuthor);
        tilCategory = (TextInputLayout) findViewById(R.id.tilCategory);
        tilTotalPages = (TextInputLayout) findViewById(R.id.tilTotalPages);
        tilCurrentPage = (TextInputLayout) findViewById(R.id.tilCurrentPage);

        spinnerStatus = (Spinner) findViewById(R.id.spinnerStatus);
        switchFavorite = (SwitchMaterial) findViewById(R.id.switchFavorite);
        ratingBook = (RatingBar) findViewById(R.id.ratingBook);
        tvRatingError = (TextView) findViewById(R.id.tvRatingError);
        tvGenresError = (TextView) findViewById(R.id.tvGenresError);

        rgRecommended = (RadioGroup) findViewById(R.id.rgRecommended);
        rbRecommendedYes = (RadioButton) findViewById(R.id.rbRecommendedYes);
        rbRecommendedNo = (RadioButton) findViewById(R.id.rbRecommendedNo);

        cbGenreFiction = (CheckBox) findViewById(R.id.cbGenreFiction);
        cbGenreMystery = (CheckBox) findViewById(R.id.cbGenreMystery);
        cbGenreRomance = (CheckBox) findViewById(R.id.cbGenreRomance);
        cbGenreFantasy = (CheckBox) findViewById(R.id.cbGenreFantasy);
        cbGenreBiography = (CheckBox) findViewById(R.id.cbGenreBiography);

        pb = (ProgressBar) findViewById(R.id.progressBarBook);
        pb.setVisibility(View.INVISIBLE);

        ratingBook.setIsIndicator(false);
        ratingBook.setClickable(true);
        ratingBook.setFocusable(true);
        bookId = readIntExtra("id", -1);
        if (bookId != -1) {
            loadBookForEdit();
        } else {
            rbRecommendedNo.setChecked(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    //fills the form from Intent extras when MainActivity launched this screen with a book id
    private void loadBookForEdit() {
        edTitle.setText(readStringExtra("title"));
        edAuthor.setText(readStringExtra("author"));
        edCategory.setText(readStringExtra("category"));

        int totalPages = readIntExtra("total_pages", 0);
        int currentPage = readIntExtra("current_page", 0);

        edTotalPages.setText(totalPages>0 ? String.valueOf(totalPages) : "");
        edCurrentPage.setText(currentPage>0 ? String.valueOf(currentPage) : "");

        String status= readStringExtra("status");
        setSpinnerStatus(status);

        switchFavorite.setChecked(readIntExtra("is_favorite", 0) == 1);

        float rating = readFloatExtra("rating", 0f);
        ratingBook.setRating(rating);

        String genres = readStringExtra("genres");
        clearGenreCheckboxes();
        applyGenresToCheckboxes(genres);

        int isRecommended= readIntExtra("is_recommended", -1);
        if (isRecommended == 1) {
            rbRecommendedYes.setChecked(true);
        } else if (isRecommended == 0) {
            rbRecommendedNo.setChecked(true);
        } else {
            rgRecommended.clearCheck();
        }
    }

    //Maps a status string from the server to the spinner index (Reading, Completed, Paused, Wishlist)
    private void setSpinnerStatus(String status) {
        if (status == null) return;
        if ("Reading".equalsIgnoreCase(status)) {
            spinnerStatus.setSelection(0);
        } else if ("Completed".equalsIgnoreCase(status)) {
            spinnerStatus.setSelection(1);
        } else if ("Paused".equalsIgnoreCase(status)) {
            spinnerStatus.setSelection(2);
        } else if ("Wishlist".equalsIgnoreCase(status)) {
            spinnerStatus.setSelection(3);
        }
    }

    //safe string extra reader so missing keys return empty string instead of crashing
    private String readStringExtra(String key) {
        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(key)) return "";
        Object value = extras.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    // reads an int extra even when the intent stored it as Long, Float, or String (common when passing through Intent)
    private int readIntExtra(String key, int defaultValue) {
        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(key)) return defaultValue;

        Object value = extras.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Float) return Math.round((Float) value);
        if (value instanceof Double) return (int) Math.round((Double) value);
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    //same idea as readIntExtra but for rating values that may arrive as different numeric types
    private float readFloatExtra(String key, float defaultValue) {
        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(key)) return defaultValue;

        Object value = extras.get(key);
        if (value instanceof Float) return (Float) value;
        if (value instanceof Double) return ((Double) value).floatValue();
        if (value instanceof Integer) return ((Integer) value).floatValue();
        if (value instanceof Long) return ((Long) value).floatValue();

        try {
            return Float.parseFloat(String.valueOf(value).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    //Unchecks all genre boxes
    private void clearGenreCheckboxes() {
        cbGenreFiction.setChecked(false);
        cbGenreMystery.setChecked(false);
        cbGenreRomance.setChecked(false);
        cbGenreFantasy.setChecked(false);
        cbGenreBiography.setChecked(false);
    }

    // Adds save and settings actions to the toolbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_book, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_save) {
            saveBook();
            return true;
        }

        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(AddBookActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //validates required fields and numbers, builds add or update URL, sends GET via Volley, then finishes with RESULT_OK
    public void saveBook() {
        pb.setVisibility(View.VISIBLE);
        clearFieldErrors();

        String title = edTitle.getText().toString().trim();
        String author = edAuthor.getText().toString().trim();
        String category = edCategory.getText().toString().trim();
        String totalPages = edTotalPages.getText().toString().trim();
        String currentPage = edCurrentPage.getText().toString().trim();
        String status = spinnerStatus.getSelectedItem().toString();
        String rating = String.valueOf(ratingBook.getRating());
        String genres = getSelectedGenres();
        String isRecommended = rgRecommended.getCheckedRadioButtonId() == R.id.rbRecommendedYes ? "1" : "0";
        String fav = switchFavorite.isChecked() ? "1" : "0";

        boolean hasError = false;

        if (title.isEmpty()) {
            tilTitle.setError(getString(R.string.error_book_title_required));
            hasError = true;
        }

        if (author.isEmpty()) {
            tilAuthor.setError(getString(R.string.error_book_author_required));
            hasError = true;
        }

        if (category.isEmpty()) {
            tilCategory.setError(getString(R.string.error_book_category_required));
            hasError = true;
        }

        if (totalPages.isEmpty()) {
            tilTotalPages.setError(getString(R.string.error_total_pages_required));
            hasError = true;
        }

        int totalPagesInt = 0;
        int currentPageInt = 0;

        if (!totalPages.isEmpty()) {
            try {
                totalPagesInt = Integer.parseInt(totalPages);
                if (totalPagesInt <= 0) {
                    tilTotalPages.setError(getString(R.string.error_total_pages_positive));
                    hasError = true;
                }
            } catch (NumberFormatException e) {
                tilTotalPages.setError(getString(R.string.error_total_pages_invalid));
                hasError = true;
            }
        }

        if (currentPage.isEmpty()) {
            currentPage = "0";
        }
        try {
            currentPageInt = Integer.parseInt(currentPage);
            if (currentPageInt < 0) {
                tilCurrentPage.setError(getString(R.string.error_current_page_invalid));
                hasError = true;
            }
        } catch (NumberFormatException e) {
            tilCurrentPage.setError(getString(R.string.error_current_page_invalid));
            hasError = true;
        }
        if (!hasError && currentPageInt > totalPagesInt) {
            tilCurrentPage.setError(getString(R.string.error_current_page_gt_total));
            hasError = true;
        }

        if (ratingBook.getRating() == 0f) {
            tvRatingError.setText(getString(R.string.error_rating_required));
            tvRatingError.setVisibility(View.VISIBLE);
            hasError = true;
        }
        if (genres.isEmpty()) {
            tvGenresError.setText(getString(R.string.error_genres_required));
            tvGenresError.setVisibility(View.VISIBLE);
            cbGenreFiction.setError(getString(R.string.error_genres_required));
            hasError = true;
        }

        if (hasError) {
            Toast.makeText(AddBookActivity.this, R.string.toast_invalid_form, Toast.LENGTH_SHORT).show();
            pb.setVisibility(View.INVISIBLE);
            return;
        }

        SessionManager sm = new SessionManager(AddBookActivity.this);
        String url;
        if (bookId == -1) {
            url = buildBookUrl(
                    ApiConfig.BASE_URL + "add_book.php",
                    sm.getUserId(),
                    title,
                    author,
                    category,
                    totalPages,
                    currentPage,
                    status,
                    fav,
                    rating,
                    genres,
                    isRecommended
            );
        } else {
            url = buildBookUrl(
                    ApiConfig.BASE_URL + "update_book.php?id=" + bookId,
                    sm.getUserId(),
                    title,
                    author,
                    category,
                    totalPages,
                    currentPage,
                    status,
                    fav,
                    rating,
                    genres,
                    isRecommended
            );
        }

        RequestQueue queue = Volley.newRequestQueue(AddBookActivity.this);
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                response -> {
                    pb.setVisibility(View.INVISIBLE);
                    setResult(RESULT_OK);
                    finish();
                },
                error -> {
                    Toast.makeText(AddBookActivity.this, R.string.toast_network_problem, Toast.LENGTH_SHORT).show();
                    pb.setVisibility(View.INVISIBLE);
                }
        );

        queue.add(request);
    }

    //clears TextInputLayout and inline error views before a new save attempt
    private void clearFieldErrors() {
        tilTitle.setError(null);
        tilAuthor.setError(null);
        tilCategory.setError(null);
        tilTotalPages.setError(null);
        tilCurrentPage.setError(null);
        tvRatingError.setVisibility(View.GONE);
        tvGenresError.setVisibility(View.GONE);
        cbGenreFiction.setError(null);
    }

    //builds the comma-separated genre string from whichever checkboxes are checked
    private String getSelectedGenres() {
        StringBuilder sb=new StringBuilder();
        appendGenreIfChecked(sb, cbGenreFiction, getString(R.string.genre_fiction));
        appendGenreIfChecked(sb, cbGenreMystery, getString(R.string.genre_mystery));
        appendGenreIfChecked(sb, cbGenreRomance, getString(R.string.genre_romance));
        appendGenreIfChecked(sb, cbGenreFantasy, getString(R.string.genre_fantasy));
        appendGenreIfChecked(sb, cbGenreBiography, getString(R.string.genre_biography));
        return sb.toString();
    }

    //appends a label to the builder with comma separation when the checkbox is selected
    private void appendGenreIfChecked(StringBuilder sb, CheckBox checkBox, String label) {
        if (!checkBox.isChecked()) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(label);
    }

    //parses the stored genres string and checks the matching boxes (case-insensitive token match)
    private void applyGenresToCheckboxes(String genres) {
        if (genres == null || genres.trim().isEmpty()) return;
        String normalized = genres.toLowerCase(Locale.getDefault());
        cbGenreFiction.setChecked(containsToken(normalized, getString(R.string.genre_fiction)));
        cbGenreMystery.setChecked(containsToken(normalized, getString(R.string.genre_mystery)));
        cbGenreRomance.setChecked(containsToken(normalized, getString(R.string.genre_romance)));
        cbGenreFantasy.setChecked(containsToken(normalized, getString(R.string.genre_fantasy)));
        cbGenreBiography.setChecked(containsToken(normalized, getString(R.string.genre_biography)));
    }

    //checks if the genre exists as a full item in a comma-separated list,
    //not as part of another word (e.g., "fiction" matches "fiction, mystery"
    //but not "science fiction"). The pattern also safely handles special characters
    private boolean containsToken(String source, String tokenLabel) {
        String token = tokenLabel.toLowerCase(Locale.getDefault());
        String pattern = "(^|\\s*,\\s*)" + Pattern.quote(token) + "(\\s*,\\s*|$)";
        return source.matches(".*" + pattern + ".*");
    }

    //encodes all book fields as query parameters; add_book also receives user_id while update uses id in the base path
    private String buildBookUrl(String base, int userId, String title, String author, String category,
                                String totalPages, String currentPage, String status, String isFavorite,
                                String rating, String genres, String isRecommended) {

        Uri.Builder builder = Uri.parse(base).buildUpon();
        if (base.contains("add_book.php")) {
            builder.appendQueryParameter("user_id", String.valueOf(userId));
        }
        builder.appendQueryParameter("title", title);
        builder.appendQueryParameter("author", author);
        builder.appendQueryParameter("category", category);
        builder.appendQueryParameter("total_pages", totalPages);
        builder.appendQueryParameter("current_page", currentPage.isEmpty() ? "0" : currentPage);
        builder.appendQueryParameter("status", status);
        builder.appendQueryParameter("is_favorite", isFavorite);
        builder.appendQueryParameter("rating", rating);
        builder.appendQueryParameter("genres", genres);
        builder.appendQueryParameter("is_recommended", isRecommended);
        return builder.build().toString();
    }
}
