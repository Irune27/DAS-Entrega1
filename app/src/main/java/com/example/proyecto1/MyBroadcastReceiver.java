package com.example.proyecto1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, context.getString(R.string.sync_completed), Toast.LENGTH_SHORT).show();
        Log.d("MyBroadcastReceiver", "Message received");
    }
}
