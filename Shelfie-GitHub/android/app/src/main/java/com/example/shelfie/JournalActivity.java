package com.example.shelfie;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Executor;

//lists journals from the server, optional biometric gate; supports edit, delete, and export-to-file

public class JournalActivity extends AppCompatActivity implements JournalAdapter.JournalClickListener {
    private static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    // Declare all variables
    RecyclerView recyclerJournals;
    ExtendedFloatingActionButton btnAddJournal;

    ArrayList<Journal> journalList;
    JournalAdapter adapter;

    SessionManager sm;
    Journal journalToExport;

    private final ActivityResultLauncher<Intent> addJournalLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if(result.getResultCode() == RESULT_OK)
                        {
                            getJournalsFromDB();
                        }
                    });

    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if(result.getResultCode() == RESULT_OK && result.getData() != null)
                        {
                            Uri uri = result.getData().getData();
                            exportJournalToFile(uri);
                        }
                    });

    //either loads the journal UI immediately or runs biometric enrollment checks before showing content
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!ThemeManager.isBiometricLockEnabled(this)) {
            loadJournalScreen();
            return;
        }

        int authResult = getAuthResult();
        if(authResult == BiometricManager.BIOMETRIC_SUCCESS) {
            showBiometricPrompt();
        } else if (authResult == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            Toast.makeText(JournalActivity.this,
                    R.string.error_biometric_none_enrolled,
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(JournalActivity.this,
                    R.string.error_biometric_unavailable,
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    //returns BiometricManager status for strong biometrics plus device credential
    public int getAuthResult() {
        BiometricManager manager = BiometricManager.from(JournalActivity.this);
        return manager.canAuthenticate(AUTHENTICATORS);
    }

    // Shows system biometric prompt; success continues to loadJournalScreen, errors finish the activity
    public void showBiometricPrompt()
    {
        Executor executor = ContextCompat.getMainExecutor(JournalActivity.this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(JournalActivity.this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        loadJournalScreen();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(JournalActivity.this, errString, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.journal_biometric_title))
                .setSubtitle(getString(R.string.journal_biometric_subtitle))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    //inflates the journal layout, sets up RecyclerView and FAB, and pulls rows from get_journals.php
    public void loadJournalScreen()
    {
        setContentView(R.layout.activity_journal);

        // Reference all variables
        recyclerJournals = (RecyclerView) findViewById(R.id.recyclerJournals);
        btnAddJournal = (ExtendedFloatingActionButton) findViewById(R.id.btnAddJournal);

        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbarJournal);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
        if (toolbar.getOverflowIcon() != null) {
            toolbar.getOverflowIcon().setTint(
                    ContextCompat.getColor(this, R.color.toolbar_content));
        }

        sm = new SessionManager(JournalActivity.this);

        journalList = new ArrayList<>();
        adapter = new JournalAdapter(JournalActivity.this, journalList, this);
        recyclerJournals.setLayoutManager(new LinearLayoutManager(JournalActivity.this));
        recyclerJournals.setAdapter(adapter);

        btnAddJournal.setOnClickListener(v -> {
            Intent i = new Intent(JournalActivity.this, AddJournalActivity.class);
            addJournalLauncher.launch(i);
        });

        getJournalsFromDB();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    //GET get_journals.php, parses the journals array into Journal objects, then refreshes the adapter
    public void getJournalsFromDB()
    {
        String url = ApiConfig.BASE_URL + "get_journals.php?user_id=" + sm.getUserId();
        RequestQueue queue = Volley.newRequestQueue(JournalActivity.this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try{
                        JSONObject obj= new JSONObject(response);
                        JSONArray arr= obj.getJSONArray("journals");
                        journalList.clear();
                        for(int i=0; i<arr.length(); i++)
                        {
                            JSONObject oneObj = arr.getJSONObject(i);

                            Journal j = new Journal(
                                    oneObj.getInt("id"),
                                    oneObj.getString("title"),
                                    oneObj.getString("content"),
                                    oneObj.getString("mood"),
                                    oneObj.getString("entry_date")
                            );

                            journalList.add(j);
                        }
                        adapter.notifyDataSetChanged();
                    }
                    catch (Exception e){
                        Toast.makeText(JournalActivity.this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(JournalActivity.this, R.string.toast_network_problem, Toast.LENGTH_SHORT).show());

        queue.add(request);
    }

    //opens AddJournalActivity with the row fields in extras for edit mode
    @Override
    public void onEditClick(Journal j) {
        Intent i = new Intent(JournalActivity.this, AddJournalActivity.class);
        i.putExtra("id", j.getId());
        i.putExtra("title", j.getTitle());
        i.putExtra("content", j.getContent());
        i.putExtra("mood", j.getMood());
        i.putExtra("entry_date", j.getEntryDate());
        addJournalLauncher.launch(i);
    }

    @Override
    public void onDeleteClick(Journal j) {
        new MaterialAlertDialogBuilder(JournalActivity.this)
                .setTitle(R.string.dialog_delete_journal_title)
                .setMessage(R.string.dialog_delete_journal_body)
                .setPositiveButton(R.string.dialog_positive_delete, (dialog, which) -> deleteJournal(j))
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .show();
    }

    //stores the journal to export and launches the system document creator for a .txt file
    @Override
    public void onExportClick(Journal j) {
        journalToExport = j;

        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, j.getTitle() + ".txt");
        exportLauncher.launch(i);
    }

    //writes title, mood, date, and content into the user-chosen Uri from the storage access framework
    public void exportJournalToFile(Uri uri)
    {
        try{
            OutputStream out = getContentResolver().openOutputStream(uri);
            String text = "Title: " + journalToExport.getTitle() + "\n"
                    + "Mood: " + journalToExport.getMood() + "\n"
                    + "Date: " + journalToExport.getEntryDate() + "\n\n"
                    + journalToExport.getContent();

            out.write(text.getBytes());
            out.close();

            Toast.makeText(JournalActivity.this, R.string.toast_journal_exported, Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            Toast.makeText(JournalActivity.this, R.string.toast_journal_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    //GET delete_journal.php?id= then reloads the list on success
    private void deleteJournal(Journal j) {
        String url = ApiConfig.BASE_URL + "delete_journal.php?id=" + j.getId();
        RequestQueue queue = Volley.newRequestQueue(JournalActivity.this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Toast.makeText(JournalActivity.this, R.string.toast_journal_deleted, Toast.LENGTH_SHORT).show();
                    getJournalsFromDB();
                },
                error -> Toast.makeText(JournalActivity.this, R.string.toast_journal_delete_failed, Toast.LENGTH_SHORT).show());

        queue.add(request);
    }
}
