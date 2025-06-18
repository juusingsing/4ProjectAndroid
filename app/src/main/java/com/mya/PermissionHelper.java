package com.mya;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.net.Uri;
import android.widget.Toast;

public class PermissionHelper {

    private static final int REQUEST_CODE = 100;

    public static void requestStoragePermission(Activity activity) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 이상일 때
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivityForResult(intent, REQUEST_CODE);
                } catch (Exception e) {
                    // 일부 기기에서는 위 인텐트가 없을 수 있으므로 fallback 처리
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        activity.startActivityForResult(intent, REQUEST_CODE);
                    } catch (Exception ex) {
                        Toast.makeText(activity, "파일 권한 설정 화면을 열 수 없습니다.\n설정에서 수동으로 권한을 부여해주세요.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else {
            // Android 10 이하에서는 기존 퍼미션 요청
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE);
            }
        }
    }
}

