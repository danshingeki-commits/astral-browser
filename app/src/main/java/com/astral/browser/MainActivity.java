package com.astral.browser;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;
    private final String HOME_URL = "https://duckduckgo.com";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        etUrl = findViewById(R.id.etUrl);
        progressBar = findViewById(R.id.progressBar);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnForward = findViewById(R.id.btnForward);
        Button btnRefresh = findViewById(R.id.btnRefresh);
        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnBurn = findViewById(R.id.btnBurn);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSaveFormData(false);
        settings.setDatabaseEnabled(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                etUrl.setText(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                etUrl.setText(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) webView.goBack();
        });

        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) webView.goForward();
        });

        btnRefresh.setOnClickListener(v -> webView.reload());
        btnSearch.setOnClickListener(v -> loadUrlFromInput());

        etUrl.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrlFromInput();
                return true;
            }
            return false;
        });

        btnBurn.setOnClickListener(v -> destroySession());
        webView.loadUrl(HOME_URL);
    }

    private void loadUrlFromInput() {
        String input = etUrl.getText().toString().trim();
        hideKeyboard();
        if (input.isEmpty()) {
            webView.loadUrl(HOME_URL);
            return;
        }

        if (input.startsWith("http://") || input.startsWith("https://")) {
            webView.loadUrl(input);
        } else if (input.contains(".") && !input.contains(" ")) {
            webView.loadUrl("https://" + input);
        } else {
            try {
                String query = URLEncoder.encode(input, "UTF-8");
                webView.loadUrl("https://duckduckgo.com/?q=" + query);
            } catch (UnsupportedEncodingException e) {
                webView.loadUrl("https://duckduckgo.com/?q=" + input);
            }
        }
    }

    private void destroySession() {
        webView.clearCache(true);
        webView.clearHistory();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        webView.loadUrl(HOME_URL);
        etUrl.setText("");
        Toast.makeText(this, "🔥 Rastro destruido: Cookies y caché eliminadas", Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
