package com.example.proyecto1;

import android.content.Context;
import android.util.Log;

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

            if (action.equals("register") || action.equals("login")) {
                String username = getInputData().getString("username");
                String password = getInputData().getString("password");

                jsonParam.put("username", username);
                jsonParam.put("password", password);
            } else if (action.equals("get_user_info")) {
                int userId = getInputData().getInt("user_id", -1);
                jsonParam.put("user_id", userId);
            } else if (action.equals("verify_password")) {
                int userId = getInputData().getInt("user_id", -1);
                String currentPassword = getInputData().getString("current_password");

                jsonParam.put("action", action);
                jsonParam.put("user_id", userId);
                jsonParam.put("current_password", currentPassword);
            } else if (action.equals("change_password")) {
                int userId = getInputData().getInt("user_id", -1);
                String newPassword = getInputData().getString("new_password");

                jsonParam.put("action", action);
                jsonParam.put("user_id", userId);
                jsonParam.put("new_password", newPassword);
            } else {
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

            if (action.equals("register") || action.equals("login")) {
                output.putString("message", response.getString("message"));
                try {
                    output.putInt("user_id", response.getInt("user_id"));
                } catch (JSONException e) {
                    output.putInt("user_id", -1);
                }
            } else if (action.equals("get_user_info")) {
                if (success) {
                    output.putString("username", response.getString("username"));
                    output.putString("profile_image", response.getString("profile_image"));
                } else {
                    output.putString("message", response.getString("message"));
                }
            } else {
                output.putString("message", response.getString("message"));
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

