package com.example.proyecto1;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private ImageView userImageView;
    private String imagePath = "";
    private EditText currentPasswordEditText, newPasswordEditText, confirmEditText;
    private LinearLayout newPasswordSection;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setLocale(this);
        setContentView(R.layout.activity_profile);

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        userId = prefs.getInt("user_id", -1);

        String username = getIntent().getStringExtra("username");
        String profileImage = getIntent().getStringExtra("profile_image");

        userImageView = findViewById(R.id.imageProfile);
        TextView userText = findViewById(R.id.userTextView);
        ImageView editPicture = findViewById(R.id.editPicture);
        newPasswordSection = findViewById(R.id.layoutNewPasswordSection);
        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmEditText = findViewById(R.id.confirmPasswordEditText);
        Button back = findViewById(R.id.buttonBack);
        Button verify = findViewById(R.id.buttonVerify);
        Button saveNewPassword = findViewById(R.id.buttonSaveNewPassword);
        Button logout = findViewById(R.id.buttonLogout);

        AppUtils.loadImage(this, profileImage, userImageView);
        userText.setText(username);

        back.setOnClickListener(v -> finish());
        editPicture.setOnClickListener(v -> showOptionsDialog());
        verify.setOnClickListener(v -> verifyCurrentPassword());
        saveNewPassword.setOnClickListener(v -> changePassword());
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // arrancar una nueva pila de actividades y borrar la anterior
                Toast.makeText(ProfileActivity.this, getString(R.string.logged_out), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void showOptionsDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.select_image), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.edit_profile_picture))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openCamera();
                            break;
                        case 1:
                            openImageChooser();
                            break;
                        case 2:
                            dialog.dismiss();
                            break;
                    }
                });
        builder.show();
    }

    protected void openImageChooser() {
        AppUtils.checkImagePermission(this, new AppUtils.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickImageLauncher.launch(pickIntent);
            }

            @Override
            public void showRationaleDialog() {
                DialogFragment dialogoGallery = new DialogGallery();
                dialogoGallery.show(getSupportFragmentManager(), "etiqueta1");
            }

            @Override
            public void onPermissionDenied() {
                DialogFragment dialogoGalleryInfo = new DialogGalleryInfo();
                dialogoGalleryInfo.show(getSupportFragmentManager(), "etiqueta3");
            }
        });
    }

    protected void openCamera() {
        AppUtils.checkCameraPermission(this, new AppUtils.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureLauncher.launch(cameraIntent);
            }

            @Override
            public void showRationaleDialog() {
                DialogFragment dialogoCamera = new DialogCamera();
                dialogoCamera.show(getSupportFragmentManager(), "etiqueta2");
            }

            @Override
            public void onPermissionDenied() {
                DialogFragment dialogoCameraInfo = new DialogCameraInfo();
                dialogoCameraInfo.show(getSupportFragmentManager(), "etiqueta4");
            }
        });
    }

    protected final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        // mostrar la imagen en la pantalla y recuperar la ruta donde se ha guardado
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        userImageView.setImageBitmap(bitmap);
                        imagePath = AppUtils.saveImageToExternalStorage(this, bitmap, "user_" + userId);

                        // subir la imagen al servidor
                        if (!imagePath.isEmpty()) {
                            uploadImageToServer(imagePath, userId);
                        } else {
                            Toast.makeText(this, getString(R.string.could_not_save_image), Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        userImageView.setImageBitmap(photo);
                        imagePath = AppUtils.saveImageToExternalStorage(this, photo, "user_" + userId);

                        // subir la imagen al servidor
                        if (!imagePath.isEmpty()) {
                            uploadImageToServer(imagePath, userId);
                        } else {
                            Toast.makeText(this, getString(R.string.could_not_save_image), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    public void uploadImageToServer(String imagePath, int userId) {
        String urlServer = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/upload_profile_image.php";
        String fileName = "user_" + userId + ".jpg";

        Map<String, String> params = new HashMap<>();
        params.put("user_id", String.valueOf(userId));

        AppUtils.uploadImageToServer(imagePath, urlServer, params, "image", fileName,
                new AppUtils.UploadCallback() {
                    @Override
                    public void onSuccess(String serverResponse) {
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, getString(R.string.image_updated), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("Upload", "Error when uploading image", e);
                    }
                });
    }

    private void verifyCurrentPassword() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();

        if (currentPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_current_password), Toast.LENGTH_SHORT).show();
            return;
        }

        Data inputData = new Data.Builder()
                .putString("action", "verify_password")
                .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/change_password.php")
                .putInt("user_id", userId)
                .putString("current_password", currentPassword)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplicationContext()).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        boolean success = workInfo.getOutputData().getBoolean("success", false);
                        String serverMessage = workInfo.getOutputData().getString("message");

                        String message;

                        if (success) {
                            newPasswordSection.setVisibility(View.VISIBLE);
                        } else {
                            switch (serverMessage) {
                                case "Connection error":
                                    message = getString(R.string.connection_error);
                                    break;
                                case "Missing data":
                                    message = getString(R.string.fill_all_fields);
                                    break;
                                case "User not found":
                                    message = getString(R.string.user_not_found);
                                    break;
                                default:
                                    message = getString(R.string.incorrect_password);
                                    break;
                            }
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }

    private void changePassword() {
        String newPass = newPasswordEditText.getText().toString().trim();
        String confirmPass = confirmEditText.getText().toString().trim();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, getString(R.string.passwords_not_match), Toast.LENGTH_SHORT).show();
            return;
        }

        Data inputData = new Data.Builder()
                .putString("action", "change_password")
                .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/change_password.php")
                .putInt("user_id", userId)
                .putString("new_password", newPass)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplicationContext()).getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        boolean success = workInfo.getOutputData().getBoolean("success", false);
                        String serverMessage = workInfo.getOutputData().getString("message");
                        String message;

                        if (success) {
                            newPasswordSection.setVisibility(View.GONE);
                            currentPasswordEditText.setText("");
                            newPasswordEditText.setText("");
                            confirmEditText.setText("");
                            Toast.makeText(this, getString(R.string.password_change_ok), Toast.LENGTH_SHORT).show();
                        } else {
                            switch (serverMessage) {
                                case "Connection error":
                                    message = getString(R.string.connection_error);
                                    break;
                                case "Missing data":
                                    message = getString(R.string.fill_all_fields);
                                    break;
                                default:
                                    message = getString(R.string.password_change_error);
                                    break;
                            }
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImageChooser();
                } else {
                    // diálogo que sale si el usuario rechaza dar permisos, justo después
                    DialogFragment dialogoGalleryInfo = new DialogGalleryInfo();
                    dialogoGalleryInfo.show(getSupportFragmentManager(), "etiqueta3");
                }
                return;
            }
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    // diálogo que sale si el usuario rechaza dar permisos, justo después
                    DialogFragment dialogoCameraInfo = new DialogCameraInfo();
                    dialogoCameraInfo.show(getSupportFragmentManager(), "etiqueta4");
                }
                return;
        }
    }
}
