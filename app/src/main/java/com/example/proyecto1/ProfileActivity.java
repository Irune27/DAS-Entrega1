package com.example.proyecto1;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileActivity extends AppCompatActivity {
    private ImageView userImageView;
    private String imagePath = "";
    private EditText currentPasswordEditText, newPasswordEditText, confirmEditText;
    private LinearLayout newPasswordSection;

    private int userId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        userId = getIntent().getIntExtra("user_id", -1);
        String username = getIntent().getStringExtra("username");
        String profileImage = getIntent().getStringExtra("profile_image");
        Log.d("Profile Data", "username: " + username + ", profileImage: " + profileImage);

        userImageView = findViewById(R.id.imageProfile);
        String imageUrl = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/" + profileImage;

        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream input = conn.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);

                    runOnUiThread(() -> userImageView.setImageBitmap(bitmap));
                } else {
                    Log.e("ImageError", "Error code: " + responseCode);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        TextView userText = findViewById(R.id.userTextView);
        userText.setText(username);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ImageView editPicture = findViewById(R.id.editPicture);
        editPicture.setOnClickListener(v -> showOptionsDialog());

        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        confirmEditText = findViewById(R.id.confirmPasswordEditText);
        newPasswordSection = findViewById(R.id.layoutNewPasswordSection);

        Button verify = findViewById(R.id.buttonVerify);
        verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyCurrentPassword();
            }
        });

        Button saveNewPassword = findViewById(R.id.buttonSaveNewPassword);
        saveNewPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        Button logout = findViewById(R.id.buttonLogout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // arrancar una nueva pila de actividades y borrar la anterior
                // todo change language
                Toast.makeText(ProfileActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void showOptionsDialog() {
        String[] opciones = {getString(R.string.take_photo), getString(R.string.select_image), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.edit_profile_picture))
                .setItems(opciones, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openCamera();
                            break;
                        case 1:

                            break;
                        case 2:
                            dialog.dismiss();
                            break;
                    }
                });
        builder.show();
    }

    protected void openCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureLauncher.launch(cameraIntent);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
            DialogFragment dialogoCamera = new DialogCamera();
            dialogoCamera.show(getSupportFragmentManager(), "etiqueta8");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 2);
        }
    }

    private final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        userImageView.setImageBitmap(photo);
                        imagePath = saveImageToExternalStorage(photo);

                        if (!imagePath.isEmpty()) {
                            uploadImageToServer(imagePath, userId);
                        } else {
                            Toast.makeText(this, getString(R.string.could_not_save_image), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    protected String saveImageToExternalStorage(Bitmap bitmap) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(this, getString(R.string.external_storage), Toast.LENGTH_SHORT).show();
            return "";
        }
        try {
            bitmap = scaleBitmap(bitmap, 800, 800);
            File directory = getExternalFilesDir(null);
            File imageFile = new File(directory, "user_" + userId + ".jpg");

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("ImageSaveError", "Error code: " + e);
            return "";
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    public void uploadImageToServer(String imagePath, int userId) {
        new Thread(() -> {
            // secuencia única de texto para separar las disntintas partes del body
            String boundary = "===" + System.currentTimeMillis() + "===";
            File imageFile = new File(imagePath);
            String LINE_FEED = "\r\n";
            String urlServer = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/upload_profile_image.php";

            try {
                URL url = new URL(urlServer);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream outputStream = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

                // user_id
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"user_id\"").append(LINE_FEED);
                writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED).append(LINE_FEED);
                writer.append(String.valueOf(userId)).append(LINE_FEED);
                writer.flush();

                // imagen
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"user_")
                        .append(String.valueOf(userId)).append(".jpg\"").append(LINE_FEED);
                writer.append("Content-Type: image/jpeg").append(LINE_FEED).append(LINE_FEED);
                writer.flush();

                FileInputStream inputStream = new FileInputStream(imageFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                inputStream.close();

                writer.append(LINE_FEED).flush();
                writer.append("--").append(boundary).append("--").append(LINE_FEED);
                writer.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d("Upload", "Image uploaded correctly.");

                    // borrar imagen local después de subirla
                    if (imageFile.exists()) {
                        boolean deleted = imageFile.delete();
                        Log.d("Upload", deleted ? "Local image deleted." : "Local image could not be deleted.");
                    }

                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.image_updated), Toast.LENGTH_SHORT).show());

                } else {
                    Log.e("Upload", "Error code: " + responseCode);
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void verifyCurrentPassword() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();

        if (currentPassword.isEmpty()) {
            // todo change language
            Toast.makeText(this, "Introduce tu contraseña actual", Toast.LENGTH_SHORT).show();
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
                        String message = workInfo.getOutputData().getString("message");
                        // todo change message
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                        if (success) {
                            newPasswordSection.setVisibility(View.VISIBLE);
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
                        String message = workInfo.getOutputData().getString("message");
                        // todo change message
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                        if (workInfo.getOutputData().getBoolean("success", false)) {
                            newPasswordSection.setVisibility(View.GONE);
                            currentPasswordEditText.setText("");
                            newPasswordEditText.setText("");
                            confirmEditText.setText("");
                        }
                    }
                });

        WorkManager.getInstance(getApplicationContext()).enqueue(request);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
            else {
                // diálogo que sale si el usuario rechaza dar permisos, justo después
                DialogFragment dialogoCameraInfo = new DialogCameraInfo();
                dialogoCameraInfo.show(getSupportFragmentManager(), "etiqueta9");
            }
            return;
        }
    }
}
