package com.example.proyecto1;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Random;

/**
 * Implementation of App Widget functionality.
 */
public class RecipeWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("RecipeWidget", "Updating widget!");
        AppUtils.setLocale(context);
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.recipe_widget);

            // poner el título del widget en el idioma de la aplicación
            String title = context.getString(R.string.recipe_of_the_day);
            views.setTextViewText(R.id.widgetTitle, title);

            // conseguir los nombres y las imágenes de las recetas en la base de datos local
            String[] projection = {"Name", "Image"};
            Cursor cursor = context.getContentResolver().query(RecipeProvider.CONTENT_URI,
                    projection, null, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                // seleccionar una de las recetas aleatoriamente
                int randomIndex = new Random().nextInt(cursor.getCount());
                cursor.moveToPosition(randomIndex);

                String name = cursor.getString(cursor.getColumnIndexOrThrow("Name"));
                String image = cursor.getString(cursor.getColumnIndexOrThrow("Image"));

                views.setTextViewText(R.id.widgetRecipeName, name);

                // todo cuando se hace la sincronización, está pillando la imagen del servidor,
                // igual que en el MainActivity
                if (image.matches("\\d+")) {
                    int resId = Integer.parseInt(image);
                    views.setImageViewResource(R.id.widgetImage, resId);
                } else {
                    Bitmap bitmap = BitmapFactory.decodeFile(image);
                    views.setImageViewBitmap(R.id.widgetImage, bitmap);
                }

            } else {
                // si no hay recetas en la base de datos local
                views.setTextViewText(R.id.widgetRecipeName, context.getString(R.string.no_recipes_available));
                views.setImageViewResource(R.id.widgetImage, R.drawable.ic_launcher);
            }

            if (cursor != null) {
                cursor.close();
            }

            // hacer que al pulsar sobre el widget se abra la lista de recetas
            Intent intent = new Intent(context, LoginActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
