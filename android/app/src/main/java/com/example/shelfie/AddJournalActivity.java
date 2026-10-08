package com.example.shelfie;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

//Create or edit a journal entry; sends GET to add_journal.php or update_journal.php with query parameters
public class AddJournalActivity extends AppCompatActivity {
    //Declare all variables
    EditText edJournalTitle, edJournalContent;
    Spinner spinnerMood;
    ProgressBar pb;

    int journalId = -1;
    String entryDate;

    //binds toolbar and fields, sets today as entry date for new entries, or loads extras when editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_journal);

        // Reference all variables
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbarAddJournal);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(
                    ContextCompat.getColor(this, R.color.toolbar_content));
        }

        edJournalTitle = (EditText) findViewById(R.id.edJournalTitle);
        edJournalContent = (EditText) findViewById(R.id.edJournalContent);
        spinnerMood = (Spinner) findViewById(R.id.spinnerMood);
        pb = (ProgressBar) findViewById(R.id.progressBarJournal);

        pb.setVisibility(View.INVISIBLE);

        journalId = getIntent().getIntExtra("id", -1);
        entryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if(journalId != -1)
        {
            edJournalTitle.setText(getIntent().getStringExtra("title"));
            edJournalContent.setText(getIntent().getStringExtra("content"));
            entryDate = getIntent().getStringExtra("entry_date");

            String mood = getIntent().getStringExtra("mood");

            if(mood.equals("Happy")) spinnerMood.setSelection(0);
            else if(mood.equals("Calm")) spinnerMood.setSelection(1);
            else if(mood.equals("Neutral")) spinnerMood.setSelection(2);
            else if(mood.equals("Sad")) spinnerMood.setSelection(3);
            else if(mood.equals("Excited")) spinnerMood.setSelection(4);
            else if(mood.equals("Stressed")) spinnerMood.setSelection(5);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    //inflates the save item and any other toolbar actions from menu_add_journal
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_journal, menu);
        return true;
    }

    // save persists via Volley; settings opens SettingsActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.action_save)
        {
            saveJournal();
            return true;
        }

        if(item.getItemId() == R.id.action_settings)
        {
            startActivity(new Intent(AddJournalActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //requires title and content, builds the correct PHP URL for add vs update, then GETs it and finishes with RESULT_OK
    public void saveJournal()
    {
        pb.setVisibility(View.VISIBLE);

        String title = edJournalTitle.getText().toString();
        String content = edJournalContent.getText().toString();
        String mood = spinnerMood.getSelectedItem().toString();

        if(TextUtils.isEmpty(title) || TextUtils.isEmpty(content))
        {
            Toast.makeText(AddJournalActivity.this, R.string.toast_invalid_form, Toast.LENGTH_SHORT).show();
            pb.setVisibility(View.INVISIBLE);
        }
        else{
            SessionManager sm = new SessionManager(AddJournalActivity.this);

            Uri.Builder urlBuilder;
            if (journalId == -1) {
                urlBuilder = Uri.parse(ApiConfig.BASE_URL + "add_journal.php").buildUpon()
                        .appendQueryParameter("user_id", String.valueOf(sm.getUserId()));
            } else {
                urlBuilder = Uri.parse(ApiConfig.BASE_URL + "update_journal.php").buildUpon()
                        .appendQueryParameter("id", String.valueOf(journalId));
            }
            String url = urlBuilder
                    .appendQueryParameter("title", title)
                    .appendQueryParameter("content", content)
                    .appendQueryParameter("mood", mood)
                    .appendQueryParameter("entry_date", entryDate)
                    .build()
                    .toString();

            RequestQueue queue = Volley.newRequestQueue(AddJournalActivity.this);
            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> {
                        pb.setVisibility(View.INVISIBLE);
                        setResult(RESULT_OK);
                        finish();
                    },
                    error -> {
                        Toast.makeText(AddJournalActivity.this, R.string.toast_network_problem, Toast.LENGTH_SHORT).show();
                        pb.setVisibility(View.INVISIBLE);
                    });

            queue.add(request);
        }
    }
}
