package com.mya;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import android.util.Base64;
import android.widget.ImageButton;

import com.mya.AlarmHelper;


public class MainActivity extends AppCompatActivity {

    private WebViewManager webViewManager;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 2000;
    private static final int REQUEST_NOTIFICATION_PERMISSION_CODE = 101;
    public static final int REQUEST_IMAGE_CAPTURE = 1001;

    String walkId = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 위치 권한 요청
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Android 13 (API 33) 이상에서는 알림 권한 요청 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION_CODE);
            }
        }

        // 카메라 권한 요청
        requestCameraPermissionIfNeeded();

        // 시스템 바 여백 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // WebViewManager 초기화
        webViewManager = new WebViewManager(this, findViewById(R.id.webview));

        // 버튼들 초기화 및 클릭 리스너 설정

        ImageButton menuHome= findViewById(R.id.menuHome);
        ImageButton menuDiary= findViewById(R.id.menuDiary);
        ImageButton menuCalendar= findViewById(R.id.menuCalendar);
        ImageButton menuWrite= findViewById(R.id.menuWrite);
        ImageButton menuMy= findViewById(R.id.menuMy);

        menuHome.setOnClickListener(v->webViewManager.homePage());
        menuDiary.setOnClickListener(v->webViewManager.diaryPage());
        menuCalendar.setOnClickListener(v->webViewManager.calendarPage());
        menuWrite.setOnClickListener(v->webViewManager.writePage());
        menuMy.setOnClickListener(v->webViewManager.myPage());

        Log.d("MainActivity", "onCreate 완료");


//        // 1일 간격 반복 알람 설정
//        AlarmHelper alarmHelper = new AlarmHelper();
//        alarmHelper.setRepeatingAlarm(this, 1);
    }

    // 카메라 권한 요청 메서드
    private void requestCameraPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
            }
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "카메라 권한 허용됨");
            } else {
                Log.d("MainActivity", "카메라 권한 거부됨");
                // 거부 시 안내 가능
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "위치 권한 허용됨");
            } else {
                Log.d("MainActivity", "위치 권한 거부됨");
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "알림 권한 허용됨");
            } else {
                Log.d("MainActivity", "알림 권한 거부됨");
                // 거부 시 안내 가능
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            String base64Clean = base64Image.replaceAll("\\s+", "");

            String js = "window.onCameraImageReceived && window.onCameraImageReceived('data:image/jpeg;base64," + base64Clean + "','"+walkId+"')";

            runOnUiThread(() -> webViewManager.getWebView().evaluateJavascript(js, null));
        } else {
            webViewManager.onFileChooserResult(requestCode, resultCode, data);
        }
    }


}
