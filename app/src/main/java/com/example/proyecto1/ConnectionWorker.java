package com.example.proyecto1;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionWorker extends Worker {
    public ConnectionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String urlString = getInputData().getString("url");
        String action = getInputData().getString("action");

        HttpURLConnection conn = null;

        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            // construir el JSON con los datos
            JSONObject jsonParam = new JSONObject();

            // fallthrough para agrupar casos: dejar que continúe sin break
            Data inputData = getInputData();
            switch (action) {
                case "register":
                case "login": {
                    String username = inputData.getString("username");
                    String password = inputData.getString("password");
                    jsonParam.put("username", username);
                    jsonParam.put("password", password);
                    break;
                }
                case "get_user_info":
                case "get_recipe_names":
                case "get_recipes": {
                    int userId = inputData.getInt("user_id", -1);
                    jsonParam.put("user_id", userId);
                    break;
                }
                case "verify_password": {
                    int userId = inputData.getInt("user_id", -1);
                    String currentPassword = inputData.getString("current_password");
                    jsonParam.put("action", action);
                    jsonParam.put("user_id", userId);
                    jsonParam.put("current_password", currentPassword);
                    break;
                }
                case "change_password": {
                    int userId = inputData.getInt("user_id", -1);
                    String newPassword = inputData.getString("new_password");
                    jsonParam.put("action", action);
                    jsonParam.put("user_id", userId);
                    jsonParam.put("new_password", newPassword);
                    break;
                }
                case "save_recipe": {
                    String name = inputData.getString("name");
                    String image = inputData.getString("image");
                    String ingredients = inputData.getString("ingredients");
                    String steps = inputData.getString("steps");
                    int userId = inputData.getInt("user_id", -1);
                    jsonParam.put("name", name);
                    jsonParam.put("image", image);
                    jsonParam.put("ingredients", ingredients);
                    jsonParam.put("steps", steps);
                    jsonParam.put("user_id", userId);
                    break;
                }
                case "update_recipe": {
                    int code = inputData.getInt("code", -1);
                    String name = inputData.getString("name");
                    String image = inputData.getString("image");
                    String ingredients = inputData.getString("ingredients");
                    String steps = inputData.getString("steps");
                    jsonParam.put("code", code);
                    jsonParam.put("name", name);
                    jsonParam.put("image", image);
                    jsonParam.put("ingredients", ingredients);
                    jsonParam.put("steps", steps);
                    break;
                }
                case "delete_recipe": {
                    int code = inputData.getInt("code", -1);
                    jsonParam.put("code", code);
                    break;
                }
                default:
                    return Result.failure();
            }

            OutputStream os = conn.getOutputStream();
            os.write(jsonParam.toString().getBytes("UTF-8"));
            os.close();

            // leer la respuesta
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            // procesar la respuesta JSON
            JSONObject response = new JSONObject(sb.toString());
            Data.Builder output = new Data.Builder();
            boolean success = response.getBoolean("success");
            output.putBoolean("success", success);

            // fallthrough para agrupar casos: dejar que continúe sin break
            switch (action) {
                case "register":
                case "login": {
                    output.putString("message", response.getString("message"));
                    try {
                        output.putInt("user_id", response.getInt("user_id"));
                    } catch (JSONException e) {
                        output.putInt("user_id", -1);
                    }
                    break;
                }
                case "get_user_info": {
                    if (success) {
                        output.putString("username", response.getString("username"));
                        output.putString("profile_image", response.getString("profile_image"));
                    } else {
                        output.putString("message", response.getString("message"));
                    }
                    break;
                }
                case "get_recipe_names":
                case "get_recipes": {
                    output.putString("recipes_json", response.getString("recipes_json"));
                    break;
                }
                default: {
                    output.putString("message", response.getString("message"));
                    break;
                }
            }

            return Result.success(output.build());

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}

