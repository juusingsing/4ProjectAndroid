package com.mya;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.TimeZone;

public class AlarmHelper {

    @JavascriptInterface
    public void setRepeatingAlarmSet(Context context, JSONObject obj) {
        Log.d("AlarmHelper", "setRepeatingAlarmSet 호출됨");
        try {
            int alarmId = obj.getInt("alarmId");            // 알람아이디
            int year = obj.getInt("year");                  // 년
            String monthStr = obj.getString("month");          // 월
            int monthInt = getMonthFromName(monthStr);
            int day = obj.getInt("day");                    // 일
            int hour = obj.getInt("hour");                  // 시
            int min = obj.getInt("min");                    // 분
            int alarmCycle = obj.getInt("alarmCycle");      // 주기
            String message = obj.getString("message");      // 메시지

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);


            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("message", message);
            intent.putExtra("alarmId", alarmId);  // 알람 ID 전달
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            calendar.set(year, monthInt, day, hour, min, 0);
            calendar.set(Calendar.MILLISECOND, 0);           // 밀리초 0으로 초기화

            // 알람 시간 과거일 경우 다음 주기로 조정
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, alarmCycle);
            }

            Log.d("setRepeatingAlarmSet세팅", "alarmId: " + alarmId);
            Log.d("setRepeatingAlarmSet세팅", calendar.getTime().toString());

            long intervalMillis = 1000L * 60 * alarmCycle; // 1일간격
            Log.d("intervalMillis세팅", String.valueOf(alarmCycle)+"분 주기");
//            long intervalMillis = 1000L * 60 * 60* 24 * alarmCycle; // 1일간격
//            Log.d("intervalMillis세팅", String.valueOf(alarmCycle)+"일 주기");

            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    intervalMillis,
                    pendingIntent
            );

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getMonthFromName(String monthName) {
        switch(monthName.toLowerCase()) {
            case "january": return Calendar.JANUARY;
            case "february": return Calendar.FEBRUARY;
            case "march": return Calendar.MARCH;
            case "april": return Calendar.APRIL;
            case "may": return Calendar.MAY;
            case "june": return Calendar.JUNE;
            case "july": return Calendar.JULY;
            case "august": return Calendar.AUGUST;
            case "september": return Calendar.SEPTEMBER;
            case "october": return Calendar.OCTOBER;
            case "november": return Calendar.NOVEMBER;
            case "december": return Calendar.DECEMBER;
            default: throw new IllegalArgumentException("Invalid month name: " + monthName);
        }
    }

    @JavascriptInterface
    public void cancelAlarm(Context context, int alarmId) {
        Log.d("AlarmHelper", "cancelAlarm 호출됨, alarmId=" + alarmId);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 알람 예약 취소
        alarmManager.cancel(pendingIntent);

        // 알림 취소    이미 떠있는 알람 없애는것.
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(alarmId);

        // 토스트 메시지 띄우기
        Toast.makeText(context, "알람이 취소되었습니다. ID: " + alarmId, Toast.LENGTH_SHORT).show();
    }


    @JavascriptInterface
    public void setRepeatingAlarm(Context context, int daysInterval) {
        Log.d("AlarmHelper", "setRepeatingAlarm 호출됨");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("message", "산책 시간이에요!");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
        calendar.set(2025, Calendar.JUNE, 4, 19, 25, 0);
        calendar.set(Calendar.MILLISECOND, 0);           // 밀리초 0으로 초기화


        Log.d("setRepeatingAlarm세팅", calendar.getTime().toString());

        long intervalMillis = 1000L * 60 * daysInterval; // 1분간격
//        long intervalMillis = 1000L * 60 * 60 * 24 * daysInterval;  1000L = 1초  X 60초 X 60분 X 24시간

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                intervalMillis,
                pendingIntent
        );

    }

}
