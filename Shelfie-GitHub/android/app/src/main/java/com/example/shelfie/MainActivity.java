package com.example.shelfie;

import android.content.Intent;
import android.net.Uri;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

// Home screen: book list with search and filters, streak and quote, Volley CRUD; overflow menu is profile, settings, log out.

public class MainActivity extends AppCompatActivity implements BookAdapter.BookClickListener {

    // Declare all variables
    TextView tvWelcome, tvQuote, tvStreak, tvReadingGoal;
    EditText etGoogleBooksSearch;
    MaterialButton btnGoogleBooksSearch;
    MaterialButton btnGoogleBooksPreview;
    MaterialButton btnAddBook, btnJournal, btnFavorites;
    ProgressBar progressReadingGoal;
    SearchView searchBooks;
    Spinner spinnerFilter;
    RecyclerView recyclerBooks;

    SessionManager sm;

    ArrayList<Book> allBooks;
    ArrayList<Book> filteredBooks;
    BookAdapter adapter;

    boolean favoriteOnly= false;

    private RequestQueue volleyQueue;

    //Stable daily rotation for “book of the day” spotlight (first Google Books result)
    private static final String[] GOOGLE_BOOK_SPOTLIGHT_QUERIES = {
            "The Hobbit",
            "Pride and Prejudice",
            "1984 George Orwell",
            "Dune Frank Herbert",
            "The Midnight Library",
            "Educated Tara Westover",
            "Project Hail Mary",
            "Circe Madeline Miller",
            "The Song of Achilles",
            "Klara and the Sun"
    };

    private final ActivityResultLauncher<Intent> addBookLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            getDataFromDB();
                            loadReadingStatsForHome();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Reference all variables
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        applyMainToolbarLogo(toolbar);
        Drawable logo = toolbar.getLogo();
        if (logo != null) {
            DrawableCompat.setTintList(logo, null);
        }
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(ContextCompat.getColor(this, R.color.toolbar_content));
        }

        volleyQueue = Volley.newRequestQueue(this);

        tvWelcome = (TextView) findViewById(R.id.tvWelcome);
        tvQuote = (TextView) findViewById(R.id.tvQuote);
        tvQuote.setMovementMethod(new ScrollingMovementMethod());
        tvStreak = (TextView) findViewById(R.id.tvStreak);
        tvReadingGoal = (TextView) findViewById(R.id.tvReadingGoal);
        progressReadingGoal = findViewById(R.id.progressReadingGoal);

        etGoogleBooksSearch = findViewById(R.id.etGoogleBooksSearch);
        btnGoogleBooksSearch = findViewById(R.id.btnGoogleBooksSearch);
        btnGoogleBooksPreview = findViewById(R.id.btnGoogleBooksPreview);

        btnAddBook = (MaterialButton) findViewById(R.id.btnAddBook);
        btnJournal = (MaterialButton) findViewById(R.id.btnJournal);
        btnFavorites = (MaterialButton) findViewById(R.id.btnFavorites);

        searchBooks = (SearchView) findViewById(R.id.searchBooks);
        spinnerFilter = (Spinner) findViewById(R.id.spinnerFilter);
        recyclerBooks = (RecyclerView) findViewById(R.id.recyclerBooks);

        sm = new SessionManager(MainActivity.this);

        tvWelcome.setText(getString(R.string.welcome_fmt, sm.getUpperFirstName()));
        tvQuote.setText(R.string.google_books_loading);
        bindGoogleBooksPreviewButton(null);
        loadGoogleBookSpotlightOfTheDay();

        btnGoogleBooksSearch.setOnClickListener(v -> searchGoogleBooksFromInput());

        allBooks = new ArrayList<>();
        filteredBooks = new ArrayList<>();

        adapter = new BookAdapter(MainActivity.this, filteredBooks, this);
        recyclerBooks.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        recyclerBooks.setAdapter(adapter);

        btnAddBook.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, AddBookActivity.class);
            addBookLauncher.launch(i);
        });

        btnJournal.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, JournalActivity.class);
            startActivity(i);
        });

        btnFavorites.setOnClickListener(v -> {
            favoriteOnly = !favoriteOnly;
            btnFavorites.setText(favoriteOnly ? R.string.btn_favorites_on : R.string.btn_favorites_off);
            applyFilters();
        });

        searchBooks.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilters();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilters();
                return true;
            }
        });

        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void loadGoogleBookSpotlightOfTheDay() {
        Calendar c = Calendar.getInstance();
        int idx = Math.abs((c.get(Calendar.YEAR) * 366 + c.get(Calendar.DAY_OF_YEAR))
                % GOOGLE_BOOK_SPOTLIGHT_QUERIES.length);
        fetchGoogleBooksVolume(GOOGLE_BOOK_SPOTLIGHT_QUERIES[idx], false);
    }

    private void searchGoogleBooksFromInput() {
        String query = etGoogleBooksSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            etGoogleBooksSearch.setError(getString(R.string.google_books_enter_query));
            return;
        }
        etGoogleBooksSearch.setError(null);
        tvQuote.setText(R.string.google_books_loading);
        bindGoogleBooksPreviewButton(null);
        fetchGoogleBooksVolume(query, true);
    }

    private void bindGoogleBooksPreviewButton(@Nullable String previewUrl) {
        if (previewUrl == null || previewUrl.isEmpty()) {
            btnGoogleBooksPreview.setVisibility(View.GONE);
            btnGoogleBooksPreview.setOnClickListener(null);
            return;
        }
        btnGoogleBooksPreview.setVisibility(View.VISIBLE);
        btnGoogleBooksPreview.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(previewUrl))));
    }

    private void fetchGoogleBooksVolume(String query, boolean toastOnError) {
        String url = GoogleBooksApi.buildVolumesUrl(query, 1);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        GoogleBooksApi.VolumeSummary summary =
                                GoogleBooksApi.summarizeFirstVolume(response);
                        if (summary == null) {
                            tvQuote.setText(R.string.google_books_none);
                            bindGoogleBooksPreviewButton(null);
                        } else {
                            tvQuote.setText(summary.text);
                            bindGoogleBooksPreviewButton(summary.previewUrl);
                        }
                    } catch (Exception e) {
                        tvQuote.setText(R.string.google_books_parse_error);
                        bindGoogleBooksPreviewButton(null);
                    }
                },
                error -> {
                    tvQuote.setText(R.string.google_books_network);
                    bindGoogleBooksPreviewButton(null);
                    if (toastOnError) {
                        Toast.makeText(MainActivity.this, R.string.google_books_network, Toast.LENGTH_SHORT).show();
                    }
                });
        volleyQueue.add(request);
    }

    //reloads welcome label (e.g. after ProfileActivity updates SessionManager), books, and streak
    @Override
    protected void onResume() {
        super.onResume();
        tvWelcome.setText(getString(R.string.welcome_fmt, sm.getUpperFirstName()));
        getDataFromDB();
        markReadingDayThenRefreshStreak();
        loadReadingStatsForHome();
    }

    // calls mark_reading_day.php so the server can record today, then always requests get_streak.php to refresh the label
    private void markReadingDayThenRefreshStreak() {
        String url = ApiConfig.BASE_URL + "mark_reading_day.php?user_id=" + sm.getUserId();
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> getStreakFromDB(),
                error -> getStreakFromDB());
        queue.add(request);
    }

    //GET get_books.php for the current user, rebuilds allBooks, then runs applyFilters for the visible list
    public void getDataFromDB()
    {
        String url = ApiConfig.BASE_URL + "get_books.php?user_id=" + sm.getUserId();
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try{
                        JSONObject obj = new JSONObject(response);
                        JSONArray arr = obj.getJSONArray("books");
                        allBooks.clear();
                        for(int i=0; i<arr.length(); i++)
                        {
                            JSONObject oneObj = arr.getJSONObject(i);

                            Book b = new Book(
                                    oneObj.getInt("id"),
                                    oneObj.getString("title"),
                                    oneObj.getString("author"),
                                    oneObj.getString("category"),
                                    oneObj.getString("status"),
                                    oneObj.getInt("total_pages"),
                                    oneObj.getInt("current_page"),
                                    oneObj.getInt("is_favorite"),
                                    (float) oneObj.optDouble("rating", 0.0),
                                    oneObj.optString("genres", ""),
                                    oneObj.optInt("is_recommended", 0)
                            );

                            allBooks.add(b);
                        }

                        applyFilters();
                    }
                    catch (Exception e){
                        Toast.makeText(MainActivity.this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(MainActivity.this, R.string.toast_network_problem, Toast.LENGTH_SHORT).show());

        queue.add(request);
    }

    //GET get_streak.php and shows the day count on tvStreak, falling back to zero on errors
    public void getStreakFromDB() {
        String url = ApiConfig.BASE_URL + "get_streak.php?user_id=" + sm.getUserId();
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try{
                        JSONObject obj = new JSONObject(response);
                        int streak = obj.getInt("streak");
                        tvStreak.setText(getString(R.string.main_streak_days, streak));
                    }
                    catch (Exception e){
                        tvStreak.setText(getString(R.string.main_streak_days, 0));
                    }
                },
                error -> tvStreak.setText(getString(R.string.main_streak_days, 0)));

        queue.add(request);
    }

    //rebuilds filteredBooks from allBooks using search text, status spinner, and favorites-only toggle
    public void applyFilters() {
        filteredBooks.clear();
        String searchTxt = searchBooks.getQuery().toString().toLowerCase(Locale.getDefault());
        String selectedStatus = spinnerFilter.getSelectedItem().toString();

        for(Book b:allBooks)
        {
            boolean searchMatch =
                    b.getTitle().toLowerCase(Locale.getDefault()).contains(searchTxt) ||
                            b.getAuthor().toLowerCase(Locale.getDefault()).contains(searchTxt);

            boolean statusMatch =
                    selectedStatus.equals("All") ||
                            b.getStatus().equalsIgnoreCase(selectedStatus);

            boolean favoriteMatch =
                    !favoriteOnly || b.getIsFavorite() == 1;

            if(searchMatch && statusMatch && favoriteMatch)
            {
                filteredBooks.add(b);
            }
        }

        adapter.notifyDataSetChanged();
    }

    //starts AddBookActivity for edit with the book fields passed as extras
    @Override
    public void onEditClick(Book b) {
        Intent i = new Intent(MainActivity.this, AddBookActivity.class);
        i.putExtra("id", b.getId());
        i.putExtra("title", b.getTitle());
        i.putExtra("author", b.getAuthor());
        i.putExtra("category", b.getCategory());
        i.putExtra("status", b.getStatus());
        i.putExtra("total_pages", b.getTotalPages());
        i.putExtra("current_page", b.getCurrentPage());
        i.putExtra("is_favorite", b.getIsFavorite());
        i.putExtra("rating", b.getRating());
        i.putExtra("genres", b.getGenres());
        i.putExtra("is_recommended", b.getIsRecommended());

        addBookLauncher.launch(i);
    }
    // Shows a confirmation dialog before calling deleteBook

    @Override
    public void onDeleteClick(Book b) {
        new MaterialAlertDialogBuilder(MainActivity.this)
                .setTitle(R.string.dialog_delete_book_title)
                .setMessage(R.string.dialog_delete_book_body)
                .setPositiveButton(R.string.dialog_positive_delete, (dialog, which) -> deleteBook(b))
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .show();
    }

    // Opens a Google search for the book title if a browser is available

    @Override
    public void onWebsiteClick(Book b) {
        Uri page = Uri.parse("https://www.google.com/search?q=" + b.getTitle());
        Intent i = new Intent(Intent.ACTION_VIEW, page);
        if(i.resolveActivity(getPackageManager()) != null)
        {
            startActivity(i);
        }
        else{
            Toast.makeText(MainActivity.this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    //GET delete_book.php?id= then refreshes the list
    private void deleteBook(Book b) {
        String url = ApiConfig.BASE_URL + "delete_book.php?id=" + b.getId();
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Toast.makeText(MainActivity.this, R.string.toast_book_deleted, Toast.LENGTH_SHORT).show();
                    getDataFromDB();
                    loadReadingStatsForHome();
                },
                error -> Toast.makeText(MainActivity.this, R.string.toast_book_delete_failed, Toast.LENGTH_SHORT).show());

        queue.add(request);
    }

    // GET get_reading_stats.php: home reading goal progress + cache goal in SessionManager
    private void loadReadingStatsForHome() {
        String url = ApiConfig.BASE_URL + "get_reading_stats.php?user_id=" + sm.getUserId();
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (!obj.optBoolean("success", true)) {
                            tvReadingGoal.setText(R.string.main_reading_goal_none);
                            progressReadingGoal.setVisibility(View.GONE);
                            return;
                        }
                        JSONObject rg = obj.optJSONObject("reading_goal");
                        String type = rg != null ? rg.optString("type", "") : "";
                        int value = rg != null ? rg.optInt("value", 0) : 0;
                        sm.updateReadingExtras(type, value);

                        if (type == null || type.isEmpty() || value <= 0) {
                            progressReadingGoal.setVisibility(View.GONE);
                            tvReadingGoal.setText(R.string.main_reading_goal_none);
                            return;
                        }

                        progressReadingGoal.setVisibility(View.VISIBLE);
                        if ("pages_week".equals(type)) {
                            int done = rg != null ? rg.optInt("pages_read_this_week", 0) : 0;
                            tvReadingGoal.setText(getString(R.string.main_reading_goal_pages_week_fmt, done, value));
                            int p = value <= 0 ? 0 : Math.min(100, Math.round(100f * done / (float) value));
                            progressReadingGoal.setProgress(p);
                        } else if ("books_year".equals(type)) {
                            int done = rg != null ? rg.optInt("books_finished_this_year", 0) : 0;
                            tvReadingGoal.setText(getString(R.string.main_reading_goal_books_year_fmt, done, value));
                            int p = value <= 0 ? 0 : Math.min(100, Math.round(100f * done / (float) value));
                            progressReadingGoal.setProgress(p);
                        } else {
                            progressReadingGoal.setVisibility(View.GONE);
                            tvReadingGoal.setText(R.string.main_reading_goal_none);
                        }
                    } catch (Exception e) {
                        tvReadingGoal.setText(R.string.main_reading_goal_none);
                        progressReadingGoal.setVisibility(View.GONE);
                    }
                },
                error -> {
                    tvReadingGoal.setText(R.string.main_reading_goal_none);
                    progressReadingGoal.setVisibility(View.GONE);
                });
        queue.add(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_shelfie, menu);
        MenuItem themeItem = menu.findItem(R.id.action_theme);
        if (themeItem != null) {
            boolean dark = ThemeManager.isDarkModeEnabled(this);
            themeItem.setIcon(dark ? R.drawable.ic_sun : R.drawable.ic_moon);
            themeItem.setTitle(dark ? R.string.menu_light_mode : R.string.menu_dark_mode);
        }
        MenuItem adminItem = menu.findItem(R.id.action_admin_dashboard);
        if (adminItem != null) {
            adminItem.setVisible(sm.isAdmin());
        }
        return true;
    }

    // Toolbar: theme toggle. Overflow: profile, settings, log out (more in Settings).

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_theme) {
            boolean dark = ThemeManager.isDarkModeEnabled(this);
            ThemeManager.setDarkModeEnabled(this, !dark);
            invalidateOptionsMenu();
            recreate();
            return true;
        }

        if (id == R.id.action_admin_dashboard) {
            Uri adminPage = Uri.parse(ApiConfig.adminDashboardUrl());
            Intent openAdmin = new Intent(Intent.ACTION_VIEW, adminPage);
            if (openAdmin.resolveActivity(getPackageManager()) != null) {
                startActivity(openAdmin);
            } else {
                Toast.makeText(this, R.string.toast_no_browser, Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        if (id == R.id.action_logout) {
            sm.logout();
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
            return true;
        }

        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_profile) {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Chooses light or dark drawable for the toolbar logo based on ThemeManager

    private void applyMainToolbarLogo(Toolbar toolbar) {
        boolean dark = ThemeManager.isDarkModeEnabled(this);
        toolbar.setLogo(dark ? R.drawable.icon_dark_toolbar : R.drawable.icon_light_toolbar);
    }

}
