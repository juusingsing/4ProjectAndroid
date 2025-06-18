package com.mya;

import android.app.Activity;
import android.content.Intent;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.net.Uri;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.view.View;

import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Button;

/**
 * WebView를 관리하고 설정하는 클래스.
 * 파일 선택, 다운로드 처리 및 웹 페이지 로딩과 관련된 작업을 담당.
 */
public class WebViewManager {
    private final Activity activity;              // 현재 액티비티 참조
    private final WebView webView;                // WebView 인스턴스
    private final String baseUrl = "http://192.168.0.32:3000";  // 웹 페이지 기본 URL
    private final FileChooserHandler fileChooserHandler;        // 파일 선택 처리 핸들러
    private final FileDownloadHandler fileDownloadHandler;      // 파일 다운로드 처리 핸들러
    private boolean gLoginCheck = true;           // 로그인 상태 플래그

    /**
     * 생성자
     *
     * @param activity 현재 액티비티
     * @param webView  WebView 인스턴스
     */
    public WebViewManager(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;

        // 파일 선택 및 다운로드 처리 핸들러 초기화
        fileChooserHandler = new FileChooserHandler(activity);
        fileDownloadHandler = new FileDownloadHandler(activity);

        // WebView 설정 초기화
        setupWebView();

        // 초기 페이지 로드
        webView.loadUrl(baseUrl);
    }

    /**
     * WebView의 설정을 초기화하고, 필요한 기능을 추가하는 메서드
     */
    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        // JavaScript 실행 허용
        settings.setJavaScriptEnabled(true);

        // HTML5의 로컬 저장소 기능 허용
        settings.setDomStorageEnabled(true);

        // 파일 시스템 접근 허용 (파일 선택 시 필요)
        settings.setAllowFileAccess(true);

        // 콘텐츠 접근 허용
        settings.setAllowContentAccess(true);

        // HTTP와 HTTPS 혼합 콘텐츠 모두 허용
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // 사용자 에이전트 문자열 지정 (크롬 모바일처럼 위장)
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/119.0.6045.199 Mobile Safari/537.36"
        );

        // JavaScript에서 호출 가능한 인터페이스 객체 추가 (Android라는 이름으로 JS에서 접근)
        webView.addJavascriptInterface(new WebAppInterface(activity, this), "Android");


        // 기본 WebViewClient 설정 (내부 WebView에서 페이지 열기)
        webView.setWebViewClient(new WebViewClient());

        // WebView가 포커스를 받을 수 있도록 설정 (입력 반응 가능)
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        webView.requestFocus(View.FOCUS_DOWN);

        // 파일 선택 처리를 위한 WebChromeClient 연결
        webView.setWebChromeClient(fileChooserHandler.getWebChromeClient());

        // 다운로드 처리 리스너 연결
        webView.setDownloadListener(fileDownloadHandler.getDownloadListener());
    }

    /**
     * 파일 선택 결과를 처리하는 메서드
     *
     * @param requestCode 요청 코드
     * @param resultCode  결과 코드
     * @param data        인텐트 결과 데이터 (선택된 파일 등)
     */
    public void onFileChooserResult(int requestCode, int resultCode, Intent data) {
        fileChooserHandler.handleFileChooserResult(resultCode, data);
    }

    /**
     * 웹 페이지에서 뒤로 갈 수 있는 경우, 이전 페이지로 이동
     * 로그인 페이지일 경우에는 뒤로 가기를 무시함
     */
    public void goBackIfPossible() {
        if (webView.canGoBack()) {
            String currentUrl = webView.getUrl();
            if (currentUrl != null && currentUrl.contains("/login")) {
                return; // 로그인 페이지에서는 뒤로 가지 않음
            }
            webView.goBack(); // 이전 페이지로 이동
        }
    }

    /**
     * 메뉴 버튼 클릭 시 팝업 메뉴를 표시
     *
     * @param anchor 메뉴 버튼 뷰
     */
    public void showMenuPopup(View anchor) {
        Button btnMenu = (Button) anchor;  // anchor 뷰를 버튼으로 캐스팅

        btnMenu.setOnClickListener(v -> {
            if (!gLoginCheck) {
                return;  // 로그인되어 있지 않으면 메뉴 표시 안 함
            }
            PopupMenu popup = new PopupMenu(activity, v);  // 팝업 메뉴 생성
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_popup, popup.getMenu());  // 메뉴 리소스 연결

            // 메뉴 클릭 이벤트 처리
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menuHome) {
                    webView.loadUrl(baseUrl + "/");  // 홈으로 이동
                    return true;
                } else if (item.getItemId() == R.id.menuNotice) {
                    webView.loadUrl(baseUrl + "/board/list.do");  // 게시판으로 이동
                    return true;
                } else {
                    return false;  // 처리되지 않은 항목
                }
            });

            popup.show(); // 팝업 메뉴 표시
        });
    }

    /**
     * 사용자의 마이페이지를 로드하는 메서드
     */
    public void loadMyPage() {
        webView.loadUrl(baseUrl + "/user/view.do");
    }
    public void  homePage(){webView.loadUrl(baseUrl + "/home.do");}
    public void  diaryPage(){webView.loadUrl(baseUrl + "/diary/list.do");}
    public void  calendarPage(){webView.loadUrl(baseUrl + "/calendar.do");}
    public void  writePage(){webView.loadUrl(baseUrl + "/write/list.do");}
    public void  myPage(){webView.loadUrl(baseUrl + "/user/view.do");}
    /**
     * 로그인 상태를 확인하는 getter 메서드
     *
     * @return 로그인 여부
     */
    public boolean isLoggedIn() {
        return gLoginCheck;
    }

    /**
     * 로그인 상태를 설정하는 setter 메서드
     *
     * @param loginStatus 로그인 상태 (true: 로그인, false: 로그아웃)
     */
    public void setLoginStatus(boolean loginStatus) {
        this.gLoginCheck = loginStatus;
    }

    /**
     * 현재 WebView 객체를 반환
     *
     * @return WebView 인스턴스
     */
    public WebView getWebView() {
        return webView;
    }
}
