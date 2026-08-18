package com.iitp.aidsetimetable;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        createChannel(notificationManager);

        String title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE);
        String text = intent.getStringExtra(ReminderScheduler.EXTRA_TEXT);
        String moodleUrl = intent.getStringExtra(ReminderScheduler.EXTRA_MOODLE_URL);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (moodleUrl != null && !moodleUrl.isEmpty()) {
            openIntent.setData(Uri.parse(moodleUrl));
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) (System.currentTimeMillis() % Integer.MAX_VALUE),
                openIntent,
                flags
        );

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, ReminderScheduler.CHANNEL_ID)
                : new android.app.Notification.Builder(context);

        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title == null || title.isEmpty() ? "Class reminder" : title)
                .setContentText(text == null || text.isEmpty() ? "Your class starts soon." : text)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setShowWhen(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setColor(Color.rgb(15, 47, 104));
        }

        notificationManager.notify(
                (int) (intent.getLongExtra(ReminderScheduler.EXTRA_STARTS_AT, System.currentTimeMillis())
                        % Integer.MAX_VALUE),
                builder.build()
        );
    }

    private void createChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel = new NotificationChannel(
                ReminderScheduler.CHANNEL_ID,
                "Class reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifications before scheduled classes");
        notificationManager.createNotificationChannel(channel);
    }
}
