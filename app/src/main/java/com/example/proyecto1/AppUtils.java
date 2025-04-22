package com.example.proyecto1;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class AppUtils {

    public interface OnRecipesLoadedListener {
        void onRecipesLoaded(ArrayList<String> names, ArrayList<String> images);
    }

    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
        void showRationaleDialog();
    }

    public interface UploadCallback {
        void onSuccess(String serverResponse);
        void onError(Exception e);
    }

    public static void loadRecipesFromLocal(Context context, OnRecipesLoadedListener listener) {
        ArrayList<String> recipeNames = new ArrayList<>();
        ArrayList<String> images = new ArrayList<>();

        // recuperar el nombre y la imagen de todas las recetas de la base de datos local
        String[] projection = {"Name", "Image"};
        Cursor cursor = context.getContentResolver().query(
                RecipeProvider.CONTENT_URI, projection, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                recipeNames.add(cursor.getString(0));
                String image = cursor.getString(1);
                images.add((image != null && !image.isEmpty()) ? image : "");
            } while (cursor.moveToNext());
            cursor.close();
        }

        listener.onRecipesLoaded(recipeNames, images);
    }

    public static void fetchRecipesFromServer(Context context, LifecycleOwner lifecycleOwner,
                                              int userId, OnRecipesLoadedListener listener) {
        // recuperar el nombre y la imagen de todas las recetas de la base de datos remota
        Data inputData = new Data.Builder()
                .putString("action", "get_recipe_names")
                .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/get_recipe_names.php")
                .putInt("user_id", userId)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(request.getId())
                .observe(lifecycleOwner, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        Data output = workInfo.getOutputData();
                        String json = output.getString("recipes_json");

                        if (json != null && !json.isEmpty()) {
                            ArrayList<String> recipeNames = new ArrayList<>();
                            ArrayList<String> images = new ArrayList<>();

                            try {
                                JSONArray array = new JSONArray(json);
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    String name = obj.optString("Name", context.getString(R.string.unnamed));
                                    String image = obj.optString("Image", "");

                                    recipeNames.add(name);
                                    images.add(image);
                                }

                                listener.onRecipesLoaded(recipeNames, images);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
        WorkManager.getInstance(context).enqueue(request);
    }

    public static boolean isMediaStorageAvailable(Context context) {
        // comprobar si hay espacio libre en el almacenamiento externo
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(context, context.getString(R.string.external_storage), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    public static String saveImageToExternalStorage(Context context, Bitmap bitmap, String filenamePrefix) {
        if (!isMediaStorageAvailable(context)) return "";

        try {
            // guardar la imagen (comprimida) en el almacenamiento externo
            bitmap = scaleBitmap(bitmap, 800, 800);

            File directory = context.getExternalFilesDir(null);
            // asignarle un nombre único
            if (filenamePrefix.equals("recipe_")) filenamePrefix += System.currentTimeMillis();
            File imageFile = new File(directory, filenamePrefix + ".jpg");

            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("ImageSaveError", "Error saving image", e);
            return "";
        }
    }

    public static void checkImagePermission(Activity activity, PermissionCallback callback) {
        // comprobar la API, porque el permiso es diferente (Android 13+, API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // si el permiso está concedido
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                callback.onPermissionGranted();
            } // si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
            else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.READ_MEDIA_IMAGES)) {
                callback.showRationaleDialog();
            } // solicitar permiso
            else {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 1);
            }
        }
        // comprobar la API, porque el permiso es diferente (Android 10-12, API 29-32)
        else {
            // si el permiso está concedido
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                callback.onPermissionGranted();
            } // si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
            else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                callback.showRationaleDialog();
            } // solicitar permiso
            else {
                ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    public static void checkCameraPermission(Activity activity, PermissionCallback callback) {
        // si el permiso está concedido
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            callback.onPermissionGranted();
        }
        // si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
        else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.CAMERA)) {
            callback.showRationaleDialog();
        }
        // solicitar permiso
        else {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.CAMERA}, 2);
        }
    }

    public static void uploadImageToServer(String imagePath, String urlServer,
                                           Map<String, String> textParams, String fileFieldName,
                                           String fileName, UploadCallback callback) {
        new Thread(() -> {
            // secuencia única de texto para separar las disntintas partes del body
            String boundary = "===" + System.currentTimeMillis() + "===";
            File imageFile = new File(imagePath);
            String LINE_FEED = "\r\n";

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

                // añadir parámetros de texto (user_id)
                if (textParams != null) {
                    for (Map.Entry<String, String> entry : textParams.entrySet()) {
                        writer.append("--").append(boundary).append(LINE_FEED);
                        writer.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"").append(LINE_FEED);
                        writer.append("Content-Type: text/plain; charset=UTF-8").append(LINE_FEED);
                        writer.append(LINE_FEED).append(entry.getValue()).append(LINE_FEED);
                        writer.flush();
                    }
                }

                // imagen
                writer.append("--").append(boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"").append(fileFieldName)
                        .append("\"; filename=\"").append(fileName).append("\"").append(LINE_FEED);
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
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    callback.onSuccess(response.toString());

                    // borrar imagen local después de subirla
                    if (imageFile.exists()) {
                        boolean deleted = imageFile.delete();
                        Log.d("Upload", deleted ? "Local image deleted." : "Local image could not be deleted.");
                    }
                } else {
                    callback.onError(new IOException("Server returned error code: " + responseCode));
                }
                conn.disconnect();
            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public static void loadImage(Context context, String imagePath, ImageView imageView) {
        if (imagePath.matches("\\d+")) {
            // imagen predeterminada en local
            imageView.setImageResource(Integer.parseInt(imagePath));
        } else if (imagePath.startsWith("recipe_images/") || imagePath.startsWith("user_images/")) {
            // imagen del servidor, cargar con conexión HTTP
            new Thread(() -> {
                try {
                    String imageUrl = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/" + imagePath;
                    URL url = new URL(imageUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream input = conn.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(input);

                        if (context instanceof Activity) {
                            ((Activity) context).runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                        }
                    } else {
                        Log.e("ImageError", "Server returned response code: " + responseCode);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } else if (!imagePath.isEmpty()) {
            // imagen añadida por el usuario en local
            imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
        } else {
            imageView.setImageResource(R.drawable.default_image);
        }
    }

    public static Drawable getMarkerColor(Context context, String type) {
        switch (type) {
            case "restaurant":
                Drawable icon1 = ContextCompat.getDrawable(context, R.drawable.silverware_variant).mutate();
                icon1.setTint(Color.RED);
                return icon1;
            case "cafe":
                Drawable icon2 = ContextCompat.getDrawable(context, R.drawable.coffee).mutate();
                icon2.setTint(Color.BLUE);
                return icon2;
            case "supermarket":
                Drawable icon3 = ContextCompat.getDrawable(context, R.drawable.cart).mutate();
                icon3.setTint(Color.BLACK);
                return icon3;
            default:
                Drawable icon4 = ContextCompat.getDrawable(context, R.drawable.star).mutate();
                icon4.setTint(Color.YELLOW);
                return icon4;
        }
    }

    public static void setLocale(Context context) {
        // establecer el idioma guardado en las preferencias
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", MODE_PRIVATE);
        String language = prefs.getString("language", "es");
        setLocale(context, language);
    }

    public static void setLocale(Context context, String languageCode) {
        // establecer idioma en la configuración
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }
}
