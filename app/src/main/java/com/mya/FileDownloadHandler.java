package com.mya;

import android.app.Activity;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URLDecoder;

/**
 * WebView의 파일 다운로드 기능을 처리하는 헬퍼 클래스
 */
public class FileDownloadHandler {

    // 다운로드 요청을 실행할 Activity 컨텍스트
    private final Activity activity;

    /**
     * 생성자: Activity를 받아서 내부에 저장
     */
    public FileDownloadHandler(Activity activity) {
        this.activity = activity;  // 전달된 Activity 컨텍스트를 저장
    }

    /**
     * 파일명을 추출하는 함수
     * @param contentDisposition 서버에서 제공하는 Content-Disposition 헤더
     * @param url 다운로드할 파일의 URL
     * @param mimeType 파일의 MIME 타입
     * @return 추출된 파일명
     */
    private String extractFilename(String contentDisposition, String url, String mimeType) {
        String filename = null;

        // Content-Disposition이 제공되면, 파일명을 추출
        if (contentDisposition != null) {
            // 정규식을 사용하여 filename을 추출 (UTF-8 인코딩 방식 및 일반 filename 방식 처리)
            Matcher matcher = Pattern.compile("filename\\*=UTF-8''(.+)|filename=\"?([^\";]+)\"?").matcher(contentDisposition);
            if (matcher.find()) {
                // filename이 UTF-8로 인코딩된 경우나, 일반적인 방식으로 제공된 경우를 처리
                filename = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            }
        }

        // filename이 비어 있거나 null이면, URL에서 추측한 파일명을 사용
        if (filename == null || filename.trim().isEmpty()) {
            filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
        }

        // 파일명을 UTF-8로 디코딩 (한글 깨짐 방지)
        try {
            filename = URLDecoder.decode(filename, "UTF-8");
        } catch (Exception ignored) {
            // 디코딩 예외 처리
        }

        return filename;  // 최종적으로 추출한 파일명을 반환
    }

    /**
     * WebView에 설정할 DownloadListener 반환
     * 사용자가 링크 클릭 시 다운로드 요청을 처리함
     */
    public DownloadListener getDownloadListener() {
        return (url, userAgent, contentDisposition, mimeType, contentLength) -> {

            try {
                // URL을 URI로 변환하여 처리
                Uri uri = Uri.parse(url);
                // 파일명을 추출하는 메서드 호출
                String filename = extractFilename(contentDisposition, url, mimeType);

                // 파일명이 확장자를 포함하지 않으면, MIME 타입에서 확장자를 추출하여 추가
                if (!filename.contains(".")) {
                    String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (extension != null) {
                        filename += "." + extension;  // MIME 타입에 맞는 확장자를 추가
                    }
                }

                // 디버그용 로그: 다운로드하려는 파일 URL 및 추출된 파일명 출력
                Log.d("FileDownload", "uri : "+uri);
                Log.d("FileDownload", "filename : "+filename);

                // 다운로드 요청을 위한 DownloadManager.Request 생성
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setMimeType(mimeType);  // 파일의 MIME 타입 설정
                request.addRequestHeader("User-Agent", userAgent);  // User-Agent 헤더 추가
                request.setDescription("Downloading file");  // 다운로드 설명
                request.setTitle(filename);  // 파일명 설정 (다운로드 알림에 표시됨)
                request.allowScanningByMediaScanner();  // 다운로드한 파일이 미디어 스캐너에 의해 인식되도록 설정
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);  // 다운로드 완료 알림

                // 파일 저장 경로 설정: 이미지 파일은 'Pictures' 폴더에, 그 외의 파일은 'Downloads' 폴더에 저장
                if (mimeType.startsWith("image/")) {
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, filename);  // 이미지 파일 경로
                } else {
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);  // 기타 파일 경로
                }

                // DownloadManager를 통해 다운로드 요청을 큐에 추가
                DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Activity.DOWNLOAD_SERVICE);
                if (downloadManager != null) {
                    downloadManager.enqueue(request);  // 다운로드 요청 큐에 추가
                    //Toast.makeText(activity, "Downloading: " + filename, Toast.LENGTH_SHORT).show();  // 다운로드 시작 토스트 메시지
                }
            } catch (Exception e) {
                // 예외 처리: 다운로드 요청 시 오류 발생 시 로그 출력
                Log.e("FileDownload", "e.getMessage : "+e.getMessage());
                e.printStackTrace();  // 오류 내용 출력
            }
        };
    }
}
