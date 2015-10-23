package com.abercrombiefitch.afpromotions;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import java.util.List;

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            Handler handler = new Handler();
            ActivityRunnable activityRunnable = new ActivityRunnable(context);
            handler.post(activityRunnable);
        }
    }

    private class ActivityRunnable implements Runnable {

        private final Context _context;

        public ActivityRunnable(Context context){
            _context = context;
        }

        @Override
        public void run() {
            ActivityManager manager = (ActivityManager)_context.getApplicationContext()
                    .getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> runningTasks = manager.getAppTasks();
            if (runningTasks != null && runningTasks.size() > 0) {
                runningTasks.get(0).finishAndRemoveTask();
                _context.startActivity(
                        new Intent(_context.getApplicationContext(), MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }
    }
}