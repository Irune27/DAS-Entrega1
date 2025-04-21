package com.example.proyecto1;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class MyService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager elmanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel canalservicio = new NotificationChannel("IdCanal", "NombreCanal",
                    NotificationManager.IMPORTANCE_HIGH);
            elmanager.createNotificationChannel(canalservicio);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "IdCanal")
                    .setSmallIcon(R.drawable.chef_hat)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.synchronizing))
                    .setAutoCancel(false)
                    .setOngoing(true);
            Notification notification = builder.build();
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            startForeground(1, notification, type);
        }
        new Thread(this::synchronizeRecipes).start();
    }

    private void synchronizeRecipes() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        Data inputData = new Data.Builder()
                .putString("action", "get_recipes")
                .putString("url", "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/get_recipes.php")
                .putInt("user_id", userId)
                .build();

        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(syncRequest);
        WorkManager.getInstance(getApplicationContext())
                .getWorkInfoById(syncRequest.getId())
                .addListener(() -> {
                    try {
                        WorkInfo workInfo;
                        do {
                            workInfo = WorkManager.getInstance(getApplicationContext())
                                    .getWorkInfoById(syncRequest.getId())
                                    .get();
                            Thread.sleep(200);
                            // pequeña pausa para esperar hasta que el estado sea final (isFinished)
                        } while (workInfo != null && !workInfo.getState().isFinished());

                        if (workInfo != null && workInfo.getState().isFinished()) {
                            Data result = workInfo.getOutputData();
                            boolean success = result.getBoolean("success", false);
                            if (success) {
                                String recipesJson = result.getString("recipes_json");
                                JSONArray recetasArray = new JSONArray(recipesJson);

                                ContentResolver resolver = getContentResolver();
                                Uri uri = RecipeProvider.CONTENT_URI;

                                for (int i = 0; i < recetasArray.length(); i++) {
                                    JSONObject receta = recetasArray.getJSONObject(i);

                                    // comprobar si ya existe una receta con ese nombre
                                    String name = receta.getString("Name");
                                    Cursor cursor = resolver.query(uri, new String[]{"Name"},
                                            "Name = ?", new String[]{name}, null);

                                    if (cursor != null && cursor.getCount() == 0) {
                                        // si no existe, añadir la receta
                                        ContentValues values = new ContentValues();
                                        values.put("Name", receta.getString("Name"));

                                        String imagePath = receta.getString("Image");
                                        String localPath = "";
                                        if (imagePath.equals("recipe_images/default_image.jpg")) {
                                            localPath = String.valueOf(R.drawable.default_image);
                                        } else {
                                            String imageUrl = "http://ec2-51-44-167-78.eu-west-3.compute.amazonaws.com/ipalacios017/WEB/" + imagePath;
                                            Bitmap bitmap = downloadImage(imageUrl);

                                            if (bitmap != null) {
                                                localPath = AppUtils.saveImageToExternalStorage(getApplicationContext(), bitmap, "recipe_");
                                            }
                                        }

                                        values.put("Image", localPath);

                                        values.put("Ingredients", receta.getString("Ingredients"));
                                        values.put("Steps", receta.getString("Steps"));

                                        resolver.insert(uri, values);
                                    }
                                    if (cursor != null) {
                                        cursor.close();
                                    }
                                }
                            } else {
                                Log.e("MyService", "Error when synchronizing recipes");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        Intent doneIntent = new Intent(this, MyBroadcastReceiver.class);
                        doneIntent.setAction("sync_completed");
                        sendBroadcast(doneIntent);
                        stopSelf();
                    }
                }, Executors.newSingleThreadExecutor());
    }

    private static Bitmap downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            Log.e("ImageDownload", "Error downloading image: " + imageUrl, e);
            return null;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
