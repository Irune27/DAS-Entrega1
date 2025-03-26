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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BaseRecipeActivity extends AppCompatActivity {

    protected ImageView recipeImage;
    protected Uri imageUri = null;
    protected String imagePath = String.valueOf(R.drawable.default_image);

    private void launchGallery() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(pickIntent);
    }

    private void showPermissionDialog() {
        DialogFragment dialogoGallery = new DialogGallery();
        dialogoGallery.show(getSupportFragmentManager(), "etiqueta1");
    }

    protected void openImageChooser() {
        // comprobar la API, porque el permiso es diferente (Android 13+, API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // si el permiso está concedido
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                launchGallery();
            }
            // si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
            else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {
                showPermissionDialog();
            }
            // solicitar permiso
            else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 1);
            }
        }
        // comprobar la API, porque el permiso es diferente (Android 10-12, API 29-32)
        else {
            // si el permiso está concedido
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                launchGallery();
            }
            // si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
            else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showPermissionDialog();
            }
            // solicitar permiso
            else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    protected void openCamera() {
        // si el permiso está concedido
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureLauncher.launch(cameraIntent);
        }
        // si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
            DialogFragment dialogoCamera = new DialogCamera();
            dialogoCamera.show(getSupportFragmentManager(), "etiqueta2");
        }
        // solicitar permiso
        else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA},2);
        }
    }

    protected final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    try {
                        // mostrar la imagen en la pantalla y recuperar la ruta donde se ha guardado
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        recipeImage.setImageBitmap(bitmap);
                        imagePath = saveImageToExternalStorage(bitmap);
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
                        imagePath = saveImageToExternalStorage(photo);
                    }
                }
            });

    protected String saveImageToExternalStorage(Bitmap bitmap) {
        // comprobar si hay espacio libre en el almacenamiento externo
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(this, this.getString(R.string.external_storage), Toast.LENGTH_SHORT).show();
            return "";
        }
        try {
            // guardar la imagen (comprimida) en el almacenamiento externo
            bitmap = scaleBitmap(bitmap, 800, 800);

            File directory = getExternalFilesDir(null);
            // asignarle un nombre único
            File imageFile = new File(directory, "recipe_" + System.currentTimeMillis() + ".jpg");

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("ImageSaveError", "Error saving image", e);
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
