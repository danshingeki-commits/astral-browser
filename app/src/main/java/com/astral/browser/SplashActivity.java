package com.astral.browser;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openApp = () -> {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(11, 8, 19));
        getWindow().setNavigationBarColor(Color.rgb(11, 8, 19));
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.rgb(11, 8, 19));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_astral_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        logoParams.bottomMargin = dp(22);
        root.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("ASTRAL");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setLetterSpacing(0.20f);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("BROWSER  ·  NEON EDITION");
        subtitle.setTextColor(Color.rgb(34, 211, 238));
        subtitle.setTextSize(10);
        subtitle.setLetterSpacing(0.18f);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-2, -2);
        subtitleParams.topMargin = dp(8);
        root.addView(subtitle, subtitleParams);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, dp(34)));

        android.widget.ProgressBar progress = new android.widget.ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.rgb(192, 132, 252)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(92), dp(3));
        root.addView(progress, progressParams);

        TextView loading = new TextView(this);
        loading.setText("PREPARANDO TU UNIVERSO");
        loading.setTextColor(Color.rgb(126, 108, 149));
        loading.setTextSize(9);
        loading.setLetterSpacing(0.15f);
        loading.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams loadingParams = new LinearLayout.LayoutParams(-2, -2);
        loadingParams.topMargin = dp(15);
        root.addView(loading, loadingParams);

        root.setAlpha(0f);
        setContentView(root);
        root.animate().alpha(1f).setDuration(350).start();
        handler.postDelayed(openApp, 1450L);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(openApp);
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
