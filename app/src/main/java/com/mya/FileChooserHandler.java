package com.mya;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

/**
 * 웹뷰의 파일 선택 기능(input[type="file"])을 처리하기 위한 핸들러 클래스
 */
public class FileChooserHandler {

    private final Activity activity;

    // 파일 선택 결과를 전달받기 위한 콜백
    private ValueCallback<Uri[]> filePathCallback;

    // 파일 선택 Intent 실행 및 결과 수신을 위한 ActivityResultLauncher
    private final ActivityResultLauncher<Intent> launcher;

    /**
     * 생성자에서 ActivityResultLauncher 초기화
     * ActivityResultLauncher는 파일 선택 결과를 비동기적으로 처리하기 위해 사용
     */
    public FileChooserHandler(Activity activity) {
        this.activity = activity;

        // MainActivity는 registerForActivityResult()를 호출할 수 있어야 함
        this.launcher = ((MainActivity) activity).registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // 파일 선택이 취소된 경우 처리
                    if (filePathCallback == null) return;

                    // 선택한 파일의 URI를 전달
                    Uri resultUri = result.getData() != null ? result.getData().getData() : null;
                    // 파일이 선택되었으면 URI 배열로 전달, 없으면 null을 전달
                    filePathCallback.onReceiveValue(resultUri != null ? new Uri[]{resultUri} : null);
                    // 콜백 초기화 (파일 선택이 완료되었으므로)
                    filePathCallback = null;
                });
    }

    /**
     * WebView에 설정할 WebChromeClient 리턴
     * 파일 선택 창이 표시될 때 호출되며, 파일 선택 화면을 표시하기 위한 Intent를 실행
     */
    public WebChromeClient getWebChromeClient() {
        return new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                // 파일 선택 결과를 받을 콜백을 저장
                filePathCallback = callback;
                Log.d("FileChooser", "onShowFileChooser 호출됨");

                try {
                    // 파일 선택 화면을 표시하기 위한 인텐트 생성
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE); // 열 수 있는 파일만 선택하도록 제한
                    intent.setType("*/*"); // 모든 파일 유형 허용 (이미지, 문서 등 모든 파일 선택 가능)
                    // 파일 선택 화면을 띄우기 위해 ActivityResultLauncher를 사용해 인텐트 실행
                    launcher.launch(intent);
                } catch (Exception e) {
                    // 예외가 발생하면 파일 선택을 취소하고 null을 반환
                    Log.e("FileChooser", "e.getMessage : "+e.getMessage());
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;  // 콜백 초기화
                    return false;  // 파일 선택 화면이 정상적으로 표시되지 않음
                }
                return true;  // 파일 선택 창을 표시할 준비가 완료되었음을 나타냄
            }
        };
    }

    /**
     * 예전 방식에서 쓰이던 파일 선택 결과 처리용 메서드. 현재는 사용하지 않음.
     * - 이 메서드는 ActivityResultLauncher로 대체되었기 때문에 실제로는 호출되지 않음
     * - 호환용으로 남겨두었음
     */
    public void handleFileChooserResult(int resultCode, Intent data) {
        // 현재 launcher로 대체되어 이 메서드는 필요 없음. (호환용 dummy 메서드)
    }
}
