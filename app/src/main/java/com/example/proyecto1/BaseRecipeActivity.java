package com.example.proyecto1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
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

    protected void openImageChooser() {
        // si el permiso está concedido
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(pickIntent);
        }
        // si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_MEDIA_IMAGES)) {
            DialogFragment dialogoGallery = new DialogGallery();
            dialogoGallery.show(getSupportFragmentManager(), "etiqueta1");
        }
        // solicitar permiso
        else {
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.READ_MEDIA_IMAGES},1);
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
            int maxWidth = 800;
            int maxHeight = 800;
            bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, maxHeight, true);

            File directory = getExternalFilesDir(null);
            // asignarle un nombre único
            File imageFile = new File(directory, "recipe_" + System.currentTimeMillis() + ".jpg");

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
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
