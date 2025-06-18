package com.mya;

import android.app.Activity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.view.View;
import android.view.ViewGroup;

/**
 * UI 관련 유틸리티 메서드를 제공하는 클래스
 * 현재는 로그인 상태에 따라 하단바 버튼을 조정하는 기능 제공
 */
public class UiHelper {

    /**
     * 로그인 상태에 따라 하단 바 버튼들의 표시 및 크기를 조정
     *
     * @param activity    현재 액티비티
     * @param isLoggedIn  로그인 여부
     */
    public static void setLoginStatus(Activity activity, boolean isLoggedIn) {
        // 하단 바와 버튼 참조
        LinearLayout bottomBar = activity.findViewById(R.id.bottomBar);
        Button btnMenu = activity.findViewById(R.id.btnMenu);
        Button btnMyPage = activity.findViewById(R.id.btnMyPage);
        Button btnBack = activity.findViewById(R.id.btnBack);
        bottomBar.post(new Runnable() {
            @Override
            public void run() {
                if (isLoggedIn) {
                    // 로그인 상태일 때: 모든 버튼을 균등하게 배치 (가중치 1씩 할당)
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            0, dpToPx(activity, 48), 1f);
                    btnBack.setLayoutParams(params);
                    btnMenu.setLayoutParams(params);
                    btnMyPage.setLayoutParams(params);

                    // 메뉴, 마이페이지 버튼 표시
                    btnMenu.setVisibility(View.VISIBLE);
                    btnMyPage.setVisibility(View.VISIBLE);
                } else {
                    // 비로그인 상태일 때: 뒤로가기 버튼만 전체 너비 사용
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 48));
                    btnBack.setLayoutParams(params);

                    // 메뉴, 마이페이지 버튼 숨김
                    btnMenu.setVisibility(View.GONE);
                    btnMyPage.setVisibility(View.GONE);
                }
            }
        });

        // UI 갱신
        bottomBar.requestLayout();  // 레이아웃 다시 계산
        bottomBar.invalidate();     // 뷰 다시 그림
    }

    /**
     * dp 단위를 픽셀로 변환
     *
     * @param activity  컨텍스트로 사용할 액티비티
     * @param dp        dp 단위 값
     * @return          변환된 픽셀 값
     */
    private static int dpToPx(Activity activity, int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
