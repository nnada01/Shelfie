package com.example.shelfie;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

//Reading statistics dashboard: simple bar-style charts from get_reading_stats.php JSON
public class ReadingStatsActivity extends AppCompatActivity {

    private TextView tvStatsSummary;
    private TextView tvStreakDetail;
    private LinearLayout containerMonthly;
    private LinearLayout containerGenres;
    private ProgressBar progressStatsLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_stats);

        MaterialToolbar toolbar = findViewById(R.id.toolbarStats);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        tvStatsSummary = findViewById(R.id.tvStatsSummary);
        tvStreakDetail = findViewById(R.id.tvStreakDetail);
        containerMonthly = findViewById(R.id.containerMonthly);
        containerGenres = findViewById(R.id.containerGenres);
        progressStatsLoading = findViewById(R.id.progressStatsLoading);

        loadStats();
    }

    private void loadStats() {
        SessionManager sm = new SessionManager(this);
        int uid = sm.getUserId();
        if (uid <= 0) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressStatsLoading.setVisibility(View.VISIBLE);
        String url = ApiConfig.BASE_URL + "get_reading_stats.php?user_id=" + uid;
        Volley.newRequestQueue(this).add(new StringRequest(Request.Method.GET, url,
                response -> {
                    progressStatsLoading.setVisibility(View.GONE);
                    try {
                        applyJson(new JSONObject(response));
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressStatsLoading.setVisibility(View.GONE);
                    Toast.makeText(this,
                            NetworkErrorHelper.message(ReadingStatsActivity.this, error),
                            Toast.LENGTH_LONG).show();
                }));
    }

    private void applyJson(JSONObject obj) throws Exception {
        if (!obj.optBoolean("success", true)) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        int pages = obj.optInt("total_pages_progress", 0);
        int streak = obj.optInt("current_streak", 0);
        int legacyNoDate = obj.optInt("finished_books_without_completed_at", 0);

        tvStatsSummary.setText(getString(R.string.stats_summary_fmt, pages, streak));

        containerMonthly.removeAllViews();
        JSONArray monthly = obj.optJSONArray("books_finished_per_month");
        int maxM = 1;
        if (monthly != null) {
            for (int i = 0; i < monthly.length(); i++) {
                maxM = Math.max(maxM, monthly.getJSONObject(i).optInt("count", 0));
            }
            for (int i = 0; i < monthly.length(); i++) {
                JSONObject row = monthly.getJSONObject(i);
                String ym = row.optString("month", "");
                int cnt = row.optInt("count", 0);
                addBarRow(containerMonthly, ym, cnt, maxM);
            }
        }
        if (containerMonthly.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.stats_empty_monthly);
            TextViewCompat.setTextAppearance(empty, androidx.appcompat.R.style.TextAppearance_AppCompat_Body2);
            containerMonthly.addView(empty);
        }

        containerGenres.removeAllViews();
        JSONObject genres = obj.optJSONObject("genre_breakdown");
        int maxG = 1;
        if (genres != null) {
            Iterator<String> keys = genres.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                maxG = Math.max(maxG, genres.optInt(k, 0));
            }
            Iterator<String> keys2 = genres.keys();
            while (keys2.hasNext()) {
                String k = keys2.next();
                int v = genres.optInt(k, 0);
                addBarRow(containerGenres, k, v, maxG);
            }
        }
        if (containerGenres.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.stats_empty_genres);
            TextViewCompat.setTextAppearance(empty, androidx.appcompat.R.style.TextAppearance_AppCompat_Body2);
            containerGenres.addView(empty);
        }

        JSONArray hist = obj.optJSONArray("streak_history_90d");
        int logged = 0;
        if (hist != null) {
            for (int i = 0; i < hist.length(); i++) {
                if (hist.getJSONObject(i).optInt("read", 0) == 1) {
                    logged++;
                }
            }
        }
        String extra = legacyNoDate > 0
                ? getString(R.string.stats_streak_footer_legacy, legacyNoDate)
                : "";
        tvStreakDetail.setText(getString(R.string.stats_streak_detail_fmt, logged) + extra);
    }

    private void addBarRow(LinearLayout parent, String label, int value, int max) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        row.setPadding(0, pad, 0, pad);

        TextView tv = new TextView(this);
        tv.setText(String.format(Locale.getDefault(), "%s — %d", label, value));
        TextViewCompat.setTextAppearance(tv, androidx.appcompat.R.style.TextAppearance_AppCompat_Caption);
        row.addView(tv);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        int p = max <= 0 ? 0 : Math.round(100f * value / (float) max);
        bar.setMax(100);
        bar.setProgress(p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (10 * getResources().getDisplayMetrics().density));
        lp.topMargin = (int) (4 * getResources().getDisplayMetrics().density);
        bar.setLayoutParams(lp);
        row.addView(bar);

        parent.addView(row);
    }
}
