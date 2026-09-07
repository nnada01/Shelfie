package com.example.shelfie;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // Declare all variables
    private TextInputEditText edFirstName, edLastName, edEmail, edBirthday, edCountry;
    private TextInputEditText edReadingGoalValue;
    private Spinner spinnerReadingGoal;
    private ImageView imgAvatar;
    private MaterialButton btnSave;

    private String encodedImage = "";
    private boolean pendingPhotoRemoval;
    private SessionManager sm;
    private boolean isOnboarding;

    private final ActivityResultLauncher<Void> takePicturePreviewLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    applyBitmapToAvatar(bitmap);
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    takePicturePreviewLauncher.launch(null);
                } else {
                    Toast.makeText(this, R.string.permission_camera_denied, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> galleryPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchGalleryPicker();
                } else {
                    Toast.makeText(this, R.string.permission_gallery_denied, Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri == null) {
                        return;
                    }
                    try (InputStream is = getContentResolver().openInputStream(uri)) {
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        applyBitmapToAvatar(bmp);
                    } catch (Exception e) {
                        Toast.makeText(ProfileActivity.this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Binds views, optional onboarding title, photo buttons, and loads server profile when not onboarding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Reference all variables
        MaterialToolbar toolbar = (MaterialToolbar) findViewById(R.id.toolbarProfile);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();

        imgAvatar = (ImageView) findViewById(R.id.imgAvatar);
        imgAvatar.setImageResource(R.drawable.profile_placeholder);
        edFirstName = (TextInputEditText) findViewById(R.id.edFirstName);
        edLastName = (TextInputEditText) findViewById(R.id.edLastName);
        edEmail = (TextInputEditText) findViewById(R.id.edEmail);
        edBirthday = (TextInputEditText) findViewById(R.id.edBirthday);
        edCountry = (TextInputEditText) findViewById(R.id.edCountry);
        edReadingGoalValue = (TextInputEditText) findViewById(R.id.edReadingGoalValue);
        spinnerReadingGoal = (Spinner) findViewById(R.id.spinnerReadingGoal);
        btnSave = (MaterialButton) findViewById(R.id.btnSaveProfile);

        ((MaterialButton) findViewById(R.id.btnChangePhoto)).setOnClickListener(v -> chooseImage());
        ((MaterialButton) findViewById(R.id.btnRemovePhoto)).setOnClickListener(v -> confirmRemoveProfilePhoto());

        sm = new SessionManager(this);

        isOnboarding = getIntent().getBooleanExtra("is_onboarding", false);
        if (ab != null) {
            if (isOnboarding) {
                ab.setTitle(R.string.title_profile_onboarding);
                ab.setDisplayHomeAsUpEnabled(false);
                toolbar.setNavigationIcon(null);
                getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Toast.makeText(ProfileActivity.this,
                                R.string.toast_onboarding_save_profile,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                ab.setDisplayHomeAsUpEnabled(true);
            }
        }

        edFirstName.setText(sm.getFirstName());
        edLastName.setText(sm.getLastName());
        edEmail.setText(sm.getEmail());
        edBirthday.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveProfile());

        if (!isOnboarding) {
            loadProfile();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (isOnboarding) {
            Toast.makeText(this, R.string.toast_onboarding_save_profile, Toast.LENGTH_SHORT).show();
            return true;
        }
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    // asks confirmation then clears the in-memory image and shows the placeholder until save
    private void confirmRemoveProfilePhoto() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_remove_profile_photo_title)
                .setMessage(R.string.dialog_remove_profile_photo_body)
                .setPositiveButton(R.string.dialog_positive_remove, (d, w) -> {
                    showPlaceholderAvatar();
                    encodedImage = "";
                    pendingPhotoRemoval = true;
                    Toast.makeText(this, R.string.toast_profile_photo_removed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .show();
    }

    //offers camera or gallery; each path checks runtime permission before opening the picker or preview
    private void chooseImage() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_photo_source_title)
                .setItems(new CharSequence[]{
                        getString(R.string.profile_photo_take_picture),
                        getString(R.string.profile_photo_choose_gallery)
                }, (dialog, which) -> {
                    if (which == 0) {
                        openCameraWithPermission();
                    } else {
                        openGalleryWithPermission();
                    }
                })
                .setNegativeButton(R.string.dialog_negative_cancel, null)
                .show();
    }

    // Launches TakePicturePreview if CAMERA is granted, otherwise requests it
    private void openCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            takePicturePreviewLauncher.launch(null);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // Uses READ_MEDIA_IMAGES on API 33+ or READ_EXTERNAL_STORAGE below that, then opens the gallery picker

    private void openGalleryWithPermission() {
        String readPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, readPerm) == PackageManager.PERMISSION_GRANTED) {
            launchGalleryPicker();
        } else {
            galleryPermissionLauncher.launch(readPerm);
        }
    }

    //starts ACTION_PICK for images and waits for pickImageLauncher
    private void launchGalleryPicker() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        pickImageLauncher.launch(pick);
    }

    //resets the ImageView to the default drawable when there is no custom photo
    private void showPlaceholderAvatar() {
        imgAvatar.setImageResource(R.drawable.profile_placeholder);
    }

    //shows the bitmap on the avatar and JPEG-compresses it to Base64 for the next profile POST
    private void applyBitmapToAvatar(Bitmap bmp) {
        if (bmp == null) {
            return;
        }
        imgAvatar.setImageBitmap(bmp);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos);
        encodedImage = Base64.encodeToString(bos.toByteArray(), Base64.DEFAULT);
        pendingPhotoRemoval = false;
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) ->
                        edBirthday.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    //GET get_profile.php, fills the form, updates SessionManager cache, and decodes image_base64 when present
    private void loadProfile() {
        String url = ApiConfig.BASE_URL + "get_profile.php?user_id=" + sm.getUserId();
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("success", false)) {
                            JSONObject u = obj.getJSONObject("user");
                            String first = u.optString("first_name", "");
                            String last = u.optString("last_name", "");
                            if (first.isEmpty() && last.isEmpty()) {
                                String[] nameParts = splitName(u.optString("full_name", ""));
                                first = nameParts[0];
                                last = nameParts[1];
                            }
                            edFirstName.setText(first);
                            edLastName.setText(last);
                            edBirthday.setText(u.optString("birthday", ""));
                            edCountry.setText(u.optString("country", ""));
                            applyGoalTypeToSpinner(u.optString("reading_goal_type", ""));
                            int gv = u.optInt("reading_goal_value", 0);
                            edReadingGoalValue.setText(gv > 0 ? String.valueOf(gv) : "");
                            sm.updateProfile(first,
                                    last,
                                    u.optString("birthday", ""),
                                    u.optString("country", ""),
                                    u.optString("image_url", ""));
                            sm.updateReadingExtras(
                                    u.optString("reading_goal_type", ""),
                                    gv);

                            String img64 = u.optString("image_base64", "");
                            if (!img64.isEmpty()) {
                                byte[] data = Base64.decode(img64, Base64.DEFAULT);
                                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                if (bmp != null) {
                                    imgAvatar.setImageBitmap(bmp);
                                    pendingPhotoRemoval = false;
                                } else {
                                    showPlaceholderAvatar();
                                    pendingPhotoRemoval = false;
                                }
                            } else {
                                showPlaceholderAvatar();
                                encodedImage = "";
                                pendingPhotoRemoval = false;
                            }
                            Toast.makeText(ProfileActivity.this, R.string.toast_profile_loaded, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ProfileActivity.this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ProfileActivity.this, R.string.error_generic, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(ProfileActivity.this,
                        NetworkErrorHelper.message(ProfileActivity.this, error),
                        Toast.LENGTH_LONG).show());

        queue.add(request);
    }

    //validates required fields, POSTs update_profile.php with text and image_base64, then finishes or goes to Main on onboarding

    private void saveProfile() {
        String first = safeText(edFirstName);
        String last = safeText(edLastName);
        String birthday = safeText(edBirthday);
        String country = safeText(edCountry);

        if (first.isEmpty()) {
            edFirstName.setError(getString(R.string.error_first_name_required));
            return;
        }
        if (last.isEmpty()) {
            edLastName.setError(getString(R.string.error_last_name_required));
            return;
        }
        if (isOnboarding && birthday.isEmpty()) {
            edBirthday.setError(getString(R.string.error_birthday_required));
            return;
        }
        if (isOnboarding && country.isEmpty()) {
            edCountry.setError(getString(R.string.error_country_required));
            return;
        }

        String apiGoalType = spinnerGoalTypeToApi(spinnerReadingGoal.getSelectedItemPosition());
        String goalValStr = safeText(edReadingGoalValue);
        int goalVal = 0;
        if (!goalValStr.isEmpty()) {
            try {
                goalVal = Integer.parseInt(goalValStr);
            } catch (NumberFormatException e) {
                edReadingGoalValue.setError(getString(R.string.error_total_pages_invalid));
                return;
            }
        }
        if (!apiGoalType.equals("none") && goalVal <= 0) {
            edReadingGoalValue.setError(getString(R.string.error_total_pages_positive));
            return;
        }

        final String apiGoalTypeFinal = apiGoalType;
        final int goalValFinal = apiGoalType.equals("none") ? 0 : goalVal;

        String url = ApiConfig.BASE_URL + "update_profile.php";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        if (obj.optBoolean("success", false)) {
                            String imageUrl = pendingPhotoRemoval ? "" : obj.optString("image_url", "");
                            sm.updateProfile(first, last, birthday, country, imageUrl);
                            sm.updateReadingExtras(
                                    apiGoalTypeFinal.equals("none") ? "" : apiGoalTypeFinal,
                                    goalValFinal);
                            pendingPhotoRemoval = false;
                            Toast.makeText(ProfileActivity.this, R.string.toast_profile_saved, Toast.LENGTH_SHORT).show();
                            if (isOnboarding) {
                                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                                finish();
                            } else {
                                finish();
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this, R.string.toast_profile_failed, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ProfileActivity.this, R.string.toast_profile_failed, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(ProfileActivity.this,
                        NetworkErrorHelper.message(ProfileActivity.this, error),
                        Toast.LENGTH_LONG).show()) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("user_id", String.valueOf(sm.getUserId()));
                map.put("first_name", first);
                map.put("last_name", last);
                map.put("birthday", birthday);
                map.put("country", country);
                if (pendingPhotoRemoval) {
                    map.put("remove_profile_image", "1");
                }
                map.put("image_base64", encodedImage);
                map.put("reading_goal_type", apiGoalTypeFinal);
                map.put("reading_goal_value", String.valueOf(goalValFinal));
                return map;
            }
        };
        queue.add(request);
    }

    private String safeText(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    //splits full_name from the API into first and last using the first space (same idea as login splitFullName)

    private void applyGoalTypeToSpinner(String type) {
        if (type == null || type.isEmpty() || "none".equalsIgnoreCase(type)) {
            spinnerReadingGoal.setSelection(0);
            return;
        }
        if ("pages_week".equals(type)) {
            spinnerReadingGoal.setSelection(1);
        } else if ("books_year".equals(type)) {
            spinnerReadingGoal.setSelection(2);
        } else {
            spinnerReadingGoal.setSelection(0);
        }
    }

    private String spinnerGoalTypeToApi(int position) {
        if (position <= 0) {
            return "none";
        }
        if (position == 1) {
            return "pages_week";
        }
        return "books_year";
    }

    private String[] splitName(String fullName) {
        String cleaned = fullName == null ? "" : fullName.trim();
        if (cleaned.isEmpty()) return new String[]{"", ""};
        int firstSpace = cleaned.indexOf(' ');
        if (firstSpace == -1) return new String[]{cleaned, ""};
        return new String[]{
                cleaned.substring(0, firstSpace).trim(),
                cleaned.substring(firstSpace + 1).trim()
        };
    }
}
