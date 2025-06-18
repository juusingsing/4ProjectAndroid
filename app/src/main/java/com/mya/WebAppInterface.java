package com.mya;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.provider.Settings;
import android.os.Build;

// 알람 관련
import android.app.AlarmManager;
import android.app.PendingIntent;

// 날짜 및 시간 관련
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// JSON 관련
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Calendar;
import com.mya.AlarmHelper;

import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;


/**
 * WebView에서 JavaScript로부터 메시지를 받아서 처리하는 클래스
 * JavaScript와 Android 네이티브 코드 간의 상호작용을 관리
 */
public class WebAppInterface {
    private final Context context;
    private final WebViewManager webViewManager;

    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastKnownLocation = null;

    private static final int REQUEST_IMAGE_CAPTURE = 1001;

    AlarmHelper alarmHelper;


    /**
     * 생성자
     * @param context      현재 컨텍스트 (액티비티)
     * @param manager      WebView 관리 객체
     */
    public WebAppInterface(Context context, WebViewManager manager) {
        this.context = context;
        this.webViewManager = manager;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        alarmHelper = new AlarmHelper();  // ← 여기서 초기화 반드시!
    }

    public WebAppInterface(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.webViewManager = null;


        // ✅ 여기에서 위치 업데이트 설정
        startLocationUpdates();
    }

        private void startLocationUpdates() {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                    .setMinUpdateDistanceMeters(1f)
                    .build();

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        lastKnownLocation = locationResult.getLastLocation();
                        Log.d("WebAppInterface", "지속 위치 업데이트: " + lastKnownLocation);
                    }
                }
            };

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } catch (SecurityException e) {
                Log.e("WebAppInterface", "권한 없음", e);
            }
        }

    /**
     * JavaScript에서 호출될 수 있는 메서드
     * 웹 페이지에서 특정 메시지를 수신하고 이를 처리하여 UI 상태를 업데이트함
     *
     * @param message  JSON 형식의 메시지
     */
    @JavascriptInterface
    public void loginCheck(String message) {
        try {
            // 메시지를 JSON 객체로 파싱
            JSONObject obj = new JSONObject(message);

            String type = obj.optString("type");   // 메시지 유형
            Log.d("WebAppInterface", "type: " + type);

            // 메시지 타입이 "ROUTE_CHANGE"일 경우 처리
            if ("ROUTE_CHANGE".equals(type)) {

                String path = obj.optString("path");           // 현재 경로
                String userId = obj.optString("userId");       // 사용자 ID

                // 로그인 여부 판별: userId가 비어있지 않고 로그인 경로가 아님
                boolean isLoggedIn =
                        userId != null && !userId.isEmpty() &&
                                !"/user/login.do".equals(path) && !"/".equals(path);

                Log.d("WebAppInterface", "path: " + path);
                Log.d("WebAppInterface", "User ID: " + userId);
                Log.d("WebAppInterface", "Login Check Status: " + isLoggedIn);

                // MainActivity의 UI 스레드에서 로그인 상태 갱신
                if (context instanceof MainActivity) {
                    ((MainActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // UI 요소 업데이트
                            UiHelper.setLoginStatus((MainActivity) context, isLoggedIn);
                        }
                    });
                }

                // WebViewManager에도 로그인 상태 전달
                webViewManager.setLoginStatus(isLoggedIn);

            } else if ("LOGIN".equals(type)) {
                // 로그인 완료 후 WebView의 히스토리 제거
                WebView webView = webViewManager.getWebView();
                if (webView != null) {
                    ((MainActivity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 히스토리 클리어 (뒤로가기 방지 등 목적)
                            webView.clearHistory();
                        }
                    });
                }
            }
        } catch (Exception e) {
            // 예외 발생 시 로그 출력 및 사용자에게 메시지 표시
            e.printStackTrace();
            Log.e("WebAppInterface", "e message: " + e.getMessage());
            //Toast.makeText(context.getApplicationContext(), "예외 발생: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @JavascriptInterface
    public void receiveMessage(String messageJson) {
        try {
            JSONObject message = new JSONObject(messageJson);
            String type = message.getString("type");

            if ("GET_LOCATION".equals(type)) {
                Log.e("WebAppInterface", "receiveMessage 호출");
                requestLocation();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("WebAppInterface", "위치 권한 없음");
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            try {
                if (webViewManager != null) {
                    WebView webView = webViewManager.getWebView();

                    if (location != null && webView != null) {
                        JSONObject result = new JSONObject();
                        result.put("lat", location.getLatitude());
                        result.put("lng", location.getLongitude());
                        result.put("accuracy", location.getAccuracy());

                        String jsonStr = result.toString();  // JSON 문자열
                        String js = "window.onLocationReceived(" + JSONObject.quote(jsonStr) + ")";
                        webView.post(() -> webView.evaluateJavascript(js, null));
                    } else if (webView != null) {
                        JSONObject error = new JSONObject();
                        error.put("error", "location_null");
                        webView.post(() -> webView.evaluateJavascript("window.onLocationReceived(" + error.toString() + ")", null));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }



//    /**
//     * JavaScript에서 호출될 수 있는 메서드
//     * 웹 페이지에서 특정 메시지를 수신하고 이를 처리하여 UI 상태를 업데이트함
//     *
//     * @param message  JSON 형식의 메시지
//     */
//    @JavascriptInterface
//    public String receiveMessage2(String message) {
//        Log.e("WebAppInterface", "receiveMessage2 호출");
//        try {
//            // 메시지를 JSON 객체로 파싱
//            JSONObject obj = new JSONObject(message);
//
//            String type = obj.optString("type");   // 메시지 유형
//            Log.d("WebAppInterface", "type: " + type);
//
//            if ("GET_LOCATION2".equals(type)) {
//                requestLocationUpdate();  // 위치 갱신 시도
//
//                if (lastKnownLocation != null) {
//                    JSONObject result = new JSONObject();
//                    result.put("lat", lastKnownLocation.getLatitude());
//                    result.put("lng", lastKnownLocation.getLongitude());
//                    result.put("accuracy", lastKnownLocation.getAccuracy());
//                    return result.toString();
//                } else {
//                    Log.e("WebAppInterface", "lastKnownLocation없음lastKnownLocation없음");
//                    return "{\"error\": \"No location available yet\"}";
//                }
//            }
//        } catch (Exception e) {
//            // 예외 발생 시 로그 출력 및 사용자에게 메시지 표시
//            e.printStackTrace();
//            Log.e("WebAppInterface", "e message: " + e.getMessage());
//            //Toast.makeText(context.getApplicationContext(), "예외 발생: " + e.getMessage(), Toast.LENGTH_LONG).show();
//        }
//        return "";
//    }
//
//    @JavascriptInterface
//    public void requestLocationUpdate() {
//        Log.e("WebAppInterface", "requestLocationUpdate 호출");
//        try {
//            fusedLocationClient.getLastLocation()
//                    .addOnSuccessListener(location -> {
//                        if (location != null) {
//                            lastKnownLocation = location;
//                            Log.d("WebAppInterface", "위치 갱신 완료: " + location);
//                            // JS로 콜백 보내고 싶으면 WebView에 evaluateJavascript 써도 됨
//                        } else {
//                            Log.e("WebAppInterface", "위치 받아오지 못함");
//                        }
//                    });
//        } catch (SecurityException e) {
//            e.printStackTrace();
//            Log.e("WebAppInterface", "권한 없음");
//        }
//    }


    @JavascriptInterface
    public void requestLocationUpdate2() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("WebAppInterface", "위치 권한 없음");
            return;
        }

        if (webViewManager == null) {
            Log.e("WebAppInterface", "webViewManager가 null입니다.");
            return;
        }

        WebView webView = webViewManager.getWebView();

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String json = "{\"lat\":" + location.getLatitude() + ",\"lng\":" + location.getLongitude() + "}";
                webView.post(() -> webView.evaluateJavascript("window.onLocationUpdate('" + json + "')", null));
            } else {
                String json = "{\"error\":\"no_location\"}";
                webView.post(() -> webView.evaluateJavascript("window.onLocationUpdate('" + json + "')", null));
            }
        }).addOnFailureListener(e -> {
            String json = "{\"error\":\"location_error\"}";
            webView.post(() -> webView.evaluateJavascript("window.onLocationUpdate('" + json + "')", null));
        });
    }

    // 카메라 실행 요청을 받는 메서드
    @JavascriptInterface
    public void openCamera(String walkId) {
        Log.d("WebAppInterface", "openCamera() 호출됨");
        if (context instanceof Activity) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                ((Activity) context).startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            } else {
                Log.d("WebAppInterface", "카메라 앱 실행 가능한 액티비티 없음");
            }
            ((MainActivity)this.context).walkId = walkId;
        } else {
            Log.d("WebAppInterface", "context가 Activity 아님");
        }
    }

    @JavascriptInterface
    public void getNearbyPlaces(double lat, double lng, int radius, String type) {
        Log.d("WebAppInterface", "getNearbyPlaces 호출됨 - lat: " + lat + ", lng: " + lng + ", type: " + type);
        fetchNearbyPlaces(lat, lng, radius, type);
    }

    /**
     * Google Places API를 사용해 주변 장소 검색
     */
    private void fetchNearbyPlaces(double lat, double lng, int radius, String type) {
        new Thread(() -> {
            try {
                String apiKey = "AIzaSyBkqvUbxVClcx6PG5TGNx035c9_SZWt_-w";  // TODO: 실제 API 키로 대체
                String urlStr = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                        "location=" + lat + "," + lng +
                        "&radius=" + radius +  // 여기 반경 적용
                        "&type=" + type +  // 여기에 검색 유형
                        "&language=ko" +         // << 여기에 language=ko 추가
                        "&key=" + apiKey;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                String jsonResult = sb.toString();
                Log.d("WebAppInterface", "Places API 응답: " + jsonResult);

                // WebView에서 실행
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (webViewManager != null && webViewManager.getWebView() != null) {
                        WebView webView = webViewManager.getWebView();
                        if (webView != null) {
                            webView.evaluateJavascript("window.onNearbyPlaces(" + JSONObject.quote(jsonResult) + ");", null);
                        }
                    }
                });

            } catch (Exception e) {
                Log.e("WebAppInterface", "fetchNearbyPlaces 오류: " + e.getMessage());
            }
        }).start();
    }


    @JavascriptInterface
    public void AlarmSet(String json) {
        try {
            Log.e("WebAppInterface", "AlarmSet 호출성공" + json);

            JSONArray alarmArray = new JSONArray(json);

            for (int i = 0; i < alarmArray.length(); i++) {
                JSONObject obj = alarmArray.getJSONObject(i);
                String type = obj.getString("type");

                if ("SET_ALARM".equals(type)) {
                    AlarmHelper helper = new AlarmHelper();
                    helper.setRepeatingAlarmSet(context, obj);
                    Log.e("WebAppInterface", "알람 " + i + " 세팅 완료");
                }
            }
        } catch (JSONException e) {
            Log.e("WebAppInterface", "JSON 파싱 에러", e);
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("WebAppInterface", "알람 세팅 중 에러", e);
        }
    }


    @JavascriptInterface
    public void Alarm(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            String type = obj.getString("type");

            if ("SET_ALARM".equals(type)) {
                String time = obj.getString("time");
                String message = obj.getString("message");
                scheduleAlarm(time, message);
                Log.e("WebAppInterface", "Alarm 호출성공");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void scheduleAlarm(String isoTime, String message) {
        try {
            // 시간 파싱
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(isoTime);

            if (date != null) {
                Log.d("WebAppInterface", "scheduleAlarm 호출됨, 예약 시간: " + date.getTime() + " (" + date.toString() + "), 현재 시간: " + System.currentTimeMillis());

                //알람예약
                Intent intent = new Intent(context, AlarmReceiver.class);
                intent.putExtra("message", message);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                // Android 12(API 31) 이상부터 정확한 알람 권한 필요
                if (Build.VERSION.SDK_INT >= 31) {  // compileSdk 35 이므로 Build.VERSION_CODES.S 대신 숫자 31 사용 가능
                    if (alarmManager.canScheduleExactAlarms()) {
                        Log.d("WebAppInterface", "정확한 알람 권한 있음, 알람 예약 시도");
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntent);
                    } else {
                        Log.e("Alarm", "정확한 알람 권한이 없어 예약하지 않음.");
                        // 사용자에게 권한 설정 화면으로 유도
                        Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(settingsIntent);
                    }
                } else {
                    Log.d("WebAppInterface", "Android 버전 31 미만, 알람 예약 시도");
                    try {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, date.getTime(), pendingIntent);
                    } catch (SecurityException e) {
                        Log.e("Alarm", "정확한 알람 예약 실패: " + e.getMessage());
                    }
                }

            } else {
                Log.e("WebAppInterface", "scheduleAlarm: 날짜 파싱 실패");
            }
        } catch (ParseException e) {
            Log.e("WebAppInterface", "scheduleAlarm: 파싱 오류", e);
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void cancelAlarm(String alarmIdStr) {
        try {
            int alarmId = Integer.parseInt(alarmIdStr);
            Log.d("WebAppInterface", "cancelAlarm 호출: alarmId=" + alarmId);

            AlarmHelper helper = new AlarmHelper();
            helper.cancelAlarm(context, alarmId);
        }  catch (NumberFormatException e) {
            Log.e("WebAppInterface", "잘못된 alarmId: " + alarmIdStr);
        }

    }


}
