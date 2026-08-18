package com.iitp.aidsetimetable;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class ReminderScheduler {
    static final String CHANNEL_ID = "class_reminders_v2";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TEXT = "text";
    static final String EXTRA_STARTS_AT = "startsAt";
    static final String EXTRA_MOODLE_URL = "moodleUrl";
    static final String ACTION_SHOW_REMINDER = "com.iitp.aidsetimetable.SHOW_REMINDER";
    private static final String PREFS_NAME = "class_reminders";
    private static final String PAYLOAD_KEY = "payload";
    private static final String REQUEST_CODES_KEY = "requestCodes";

    private final Context context;
    private final AlarmManager alarmManager;
    private final SharedPreferences prefs;

    ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    int configure(String payload) throws JSONException {
        prefs.edit().putString(PAYLOAD_KEY, payload).apply();
        return scheduleFromPayload(payload);
    }

    int rescheduleSaved() {
        String payload = prefs.getString(PAYLOAD_KEY, "");
        if (payload.isEmpty()) return 0;

        try {
            return scheduleFromPayload(payload);
        } catch (JSONException ignored) {
            return 0;
        }
    }

    boolean scheduleTestInOneMinute() {
        long startsAt = System.currentTimeMillis() + 60_000L;
        int requestCode = reminderRequestCode("test-notification-" + startsAt, 0);

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_SHOW_REMINDER);
        intent.setPackage(context.getPackageName());
        intent.setData(Uri.parse("iitp-reminder://test/" + requestCode));
        intent.putExtra(EXTRA_TITLE, "1 minute reminder test");
        intent.putExtra(EXTRA_TEXT, "This scheduled notification works even after the app is closed.");
        intent.putExtra(EXTRA_STARTS_AT, startsAt);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        return scheduleAlarm(startsAt, requestCode, pendingIntent);
    }

    private int scheduleFromPayload(String payload) throws JSONException {
        cancelScheduled();

        JSONObject root = new JSONObject(payload);
        JSONArray offsets = root.optJSONArray("offsets");
        JSONArray events = root.optJSONArray("events");
        if (offsets == null || events == null || offsets.length() == 0) {
            prefs.edit().putString(REQUEST_CODES_KEY, "[]").apply();
            return 0;
        }

        long now = System.currentTimeMillis();
        List<Integer> requestCodes = new ArrayList<>();

        for (int eventIndex = 0; eventIndex < events.length(); eventIndex += 1) {
            JSONObject event = events.getJSONObject(eventIndex);
            long startsAt = event.optLong("startsAt", 0L);
            if (startsAt <= now) continue;

            for (int offsetIndex = 0; offsetIndex < offsets.length(); offsetIndex += 1) {
                int minutesBefore = offsets.optInt(offsetIndex, 0);
                long triggerAt = startsAt - minutesBefore * 60_000L;
                if (minutesBefore <= 0 || triggerAt <= now) continue;

                int requestCode = reminderRequestCode(event.optString("id"), minutesBefore);
                if (scheduleAlarm(triggerAt, requestCode, notificationIntent(event, startsAt, minutesBefore, requestCode))) {
                    requestCodes.add(requestCode);
                }
            }
        }

        prefs.edit().putString(REQUEST_CODES_KEY, new JSONArray(requestCodes).toString()).apply();
        return requestCodes.size();
    }

    private boolean scheduleAlarm(long triggerAt, int requestCode, PendingIntent pendingIntent) {
        if (alarmManager == null) return false;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && !alarmManager.canScheduleExactAlarms()) {
                scheduleInexactAlarm(triggerAt, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
            return true;
        } catch (RuntimeException ignored) {
            try {
                scheduleInexactAlarm(triggerAt, pendingIntent);
                return true;
            } catch (RuntimeException ignoredAgain) {
                return false;
            }
        }
    }

    private void scheduleInexactAlarm(long triggerAt, PendingIntent pendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private PendingIntent notificationIntent(
            JSONObject event,
            long startsAt,
            int minutesBefore,
            int requestCode
    ) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_SHOW_REMINDER);
        intent.setPackage(context.getPackageName());
        intent.setData(Uri.parse("iitp-reminder://class/" + requestCode));
        intent.putExtra(EXTRA_TITLE, event.optString("title", "Class reminder"));
        intent.putExtra(EXTRA_TEXT, reminderText(event.optString("text", ""), minutesBefore));
        intent.putExtra(EXTRA_STARTS_AT, startsAt);
        intent.putExtra(EXTRA_MOODLE_URL, event.optString("moodleUrl", ""));

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    private String reminderText(String classTime, int minutesBefore) {
        String lead = minutesBefore == 60 ? "1 hour" : minutesBefore + " minutes";
        if (classTime == null || classTime.isEmpty()) {
            return "Starts in " + lead;
        }
        return classTime + " starts in " + lead;
    }

    private void cancelScheduled() {
        String savedCodes = prefs.getString(REQUEST_CODES_KEY, "[]");
        try {
            JSONArray requestCodes = new JSONArray(savedCodes);
            for (int i = 0; i < requestCodes.length(); i += 1) {
                int requestCode = requestCodes.optInt(i, 0);
                if (requestCode == 0) continue;
                cancelAlarm(requestCode);
            }
        } catch (JSONException ignored) {
            // Corrupt reminder state should never block saving new reminders.
        }
    }

    private void cancelAlarm(int requestCode) {
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_SHOW_REMINDER);
        intent.setPackage(context.getPackageName());
        intent.setData(Uri.parse("iitp-reminder://class/" + requestCode));
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private int reminderRequestCode(String eventId, int minutesBefore) {
        return Math.abs((eventId + ":" + minutesBefore).hashCode());
    }
}
