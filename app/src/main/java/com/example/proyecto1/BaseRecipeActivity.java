package com.example.proyecto1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class BaseRecipeActivity extends AppCompatActivity {

    public interface OnImageUploadListener {
        void onImageUploaded(String imagePathOnServer);
    }

    protected ImageView recipeImage;
    protected Uri imageUri = null;
    protected String imagePath = String.valueOf(R.drawable.default_image);
    protected String imagePathOnServer = "recipe_images/default_image.jpg";

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
                    imageUri = result.getData().getData();
                    try {
                        // mostrar la imagen en la pantalla y recuperar la ruta donde se ha guardado
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        recipeImage.setImageBitmap(bitmap);
                        imagePath = AppUtils.saveImageToExternalStorage(this, bitmap, "recipe_");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

    protected final ActivityResultLauncher<Intent> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        // mostrar la imagen en la pantalla y recuperar la ruta donde se ha guardado
                        recipeImage.setImageBitmap(photo);
                        imagePath = AppUtils.saveImageToExternalStorage(this, photo, "recipe_");
                    }
                }
            });

    public void uploadImageToServer(String image, OnImageUploadListener listener) {
        Log.d("Image", "image: " + image);
        if (image.endsWith(".jpg")) {
            String urlServer = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/upload_recipe_image.php";
            String fileName = "recipe_" + System.currentTimeMillis() + ".jpg";

            AppUtils.uploadImageToServer(imagePath, urlServer, null, "image",
                    fileName, new AppUtils.UploadCallback() {
                        @Override
                        public void onSuccess(String serverResponse) {
                            try {
                                JSONObject json = new JSONObject(serverResponse);
                                if (json.getBoolean("success")) {
                                    imagePathOnServer = json.getString("image_path");
                                    Log.d("Upload", "Image path on server: " + imagePathOnServer);

                                    runOnUiThread(() -> listener.onImageUploaded(imagePathOnServer));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("Upload", "Error when uploading image");
                        }
                    });

        } else {
            listener.onImageUploaded(imagePathOnServer);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImageChooser();
                }
                else {
                    // diálogo que sale si el usuario rechaza dar permisos, justo después
                    DialogFragment dialogoGalleryInfo = new DialogGalleryInfo();
                    dialogoGalleryInfo.show(getSupportFragmentManager(), "etiqueta3");
                }
                return;
            }
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                }
                else {
                    // diálogo que sale si el usuario rechaza dar permisos, justo después
                    DialogFragment dialogoCameraInfo = new DialogCameraInfo();
                    dialogoCameraInfo.show(getSupportFragmentManager(), "etiqueta4");
                }
                return;
        }
    }
}
