package com.astral.browser;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.provider.Settings;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.ScrollView;
import androidx.webkit.ProxyController;
import androidx.webkit.ProxyConfig;
import android.graphics.drawable.GradientDrawable;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private EditText etUrl;
    private ProgressBar progressBar;
    private FrameLayout webContainer;
    private LinearLayout tabsBar;
    private final ArrayList<WebView> tabs = new ArrayList<>();
    private final ArrayList<String> tabTitles = new ArrayList<>();
    private int activeTab = 0;
    private final String HOME_URL = "astral://home";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etUrl = findViewById(R.id.etUrl);
        progressBar = findViewById(R.id.progressBar);
        webContainer = findViewById(R.id.webContainer);
        tabsBar = findViewById(R.id.tabsBar);
        Button back = findViewById(R.id.btnBack), forward = findViewById(R.id.btnForward);
        Button refresh = findViewById(R.id.btnRefresh), go = findViewById(R.id.btnSearch);
        Button burn = findViewById(R.id.btnBurn), addTab = findViewById(R.id.btnAddTab);
        Button home = findViewById(R.id.btnHome), tabsButton = findViewById(R.id.btnTabs), settingsButton = findViewById(R.id.btnSettings);

        back.setOnClickListener(v -> { WebView w=current(); if(w.canGoBack()) w.goBack(); else showHome(w); });
        forward.setOnClickListener(v -> { if(current().canGoForward()) current().goForward(); });
        refresh.setOnClickListener(v -> { if (isHome(current())) showHome(current()); else current().reload(); });
        go.setOnClickListener(v -> loadUrlFromInput());
        burn.setOnClickListener(v -> destroySession());
        addTab.setOnClickListener(v -> newTab(true));
        home.setOnClickListener(v -> showHome(current()));
        tabsButton.setOnClickListener(v -> { renderTabs(); Toast.makeText(this, tabs.size()+" pestaña(s) abiertas", Toast.LENGTH_SHORT).show(); });
        settingsButton.setOnClickListener(v -> showSettings());
        etUrl.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrlFromInput(); return true;
            }
            return false;
        });
        CookieManager.getInstance().setAcceptCookie(true);
        newTab(true);
    }

    private WebView current() { return tabs.get(activeTab); }
    private boolean isHome(WebView w) { return w.getTag()!=null && "home".equals(w.getTag()); }

    @SuppressLint("SetJavaScriptEnabled")
    private void newTab(boolean show) {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.rgb(11,8,19));
        WebSettings s=w.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setSupportZoom(true); s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w,getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("third_party_cookies", false));
        w.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int p){ if(view==current()){ progressBar.setVisibility(p<100?View.VISIBLE:View.GONE); progressBar.setProgress(p); } }
            @Override public void onReceivedTitle(WebView view,String title){ int i=tabs.indexOf(view); if(i>=0){tabTitles.set(i, title==null||title.isEmpty()?"Nueva pestaña":title); renderTabs();} }
        });
        w.setWebViewClient(new WebViewClient(){
            @Override public void onPageStarted(WebView view,String url,Bitmap icon){ if(!url.startsWith("https://astral.local/")) view.setTag("web"); if(view==current() && !url.startsWith("file:")) etUrl.setText(url.equals("about:blank")||url.startsWith("https://astral.local/")?"":url); }
            @Override public void onPageFinished(WebView view,String url){ if(view==current() && !isHome(view)) etUrl.setText(url); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req){ return false; }
        });
        tabs.add(w); tabTitles.add("Nueva pestaña");
        webContainer.addView(w,new FrameLayout.LayoutParams(-1,-1));
        if(show){ activeTab=tabs.size()-1; switchTab(activeTab); showHome(w); }
        renderTabs();
    }

    private void switchTab(int index) {
        if(index<0||index>=tabs.size()) return;
        activeTab=index;
        for(int i=0;i<tabs.size();i++) tabs.get(i).setVisibility(i==index?View.VISIBLE:View.GONE);
        etUrl.setText(isHome(current())?"":current().getUrl());
        renderTabs();
    }

    private void renderTabs() {
        tabsBar.removeAllViews();
        for(int i=0;i<tabs.size();i++){
            final int index=i;
            LinearLayout chip=new LinearLayout(this); chip.setGravity( android.view.Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(10),0,dp(4),0);
            GradientDrawable bg=new GradientDrawable(); bg.setColor(i==activeTab?Color.rgb(48,31,72):Color.rgb(24,18,37)); bg.setCornerRadius(dp(14)); chip.setBackground(bg);
            TextView title=new TextView(this); title.setText((tabTitles.get(i).length()>13?tabTitles.get(i).substring(0,12)+"…":tabTitles.get(i))); title.setTextColor(i==activeTab?Color.WHITE:Color.rgb(184,165,208)); title.setTextSize(12); title.setMaxWidth(dp(105)); title.setSingleLine(true);
            chip.addView(title,new LinearLayout.LayoutParams(-2,dp(38)));
            TextView close=new TextView(this); close.setText(" × "); close.setTextColor(Color.rgb(192,132,252)); close.setTextSize(17);
            close.setOnClickListener(v -> closeTab(index)); chip.addView(close);
            chip.setOnClickListener(v -> switchTab(index));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(38)); lp.setMargins(dp(4),dp(4),dp(3),dp(4)); tabsBar.addView(chip,lp);
        }
        TextView count=findViewById(R.id.tabCount); count.setText(String.valueOf(tabs.size()));
        Button tabsButton=findViewById(R.id.btnTabs); if(tabsButton!=null) tabsButton.setText("▢ "+tabs.size());
    }

    private void closeTab(int index){
        if(tabs.size()==1){ WebView w=current(); w.stopLoading(); w.clearHistory(); showHome(w); tabTitles.set(0,"Nueva pestaña"); renderTabs(); return; }
        WebView w=tabs.remove(index); tabTitles.remove(index); webContainer.removeView(w); w.destroy();
        if(activeTab>=tabs.size()) activeTab=tabs.size()-1; else if(index<activeTab) activeTab--;
        switchTab(activeTab);
    }

    private void showHome(WebView w){
        w.setTag("home");
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>*{box-sizing:border-box}body{margin:0;background:radial-gradient(ellipse at 50% 18%,#281342 0%,#100b1c 48%,#0b0813 100%);color:#f8f5ff;font-family:Arial,sans-serif;min-height:100vh;display:flex;justify-content:center}.wrap{width:100%;max-width:520px;padding:48px 22px 35px;text-align:center}.orb{margin:12px auto 16px;width:82px;height:82px;border-radius:26px;background:linear-gradient(135deg,#a855f7,#22d3ee);display:flex;align-items:center;justify-content:center;box-shadow:0 0 42px #9333ea55;font-size:39px;font-weight:800;color:#100b1c}.brand{font-size:32px;font-weight:800;letter-spacing:1px}.brand span{color:#c084fc}.tag{color:#a99abf;font-size:13px;margin:8px 0 30px}.search{display:flex;padding:6px;border:1px solid #47305e;background:#1a1329;border-radius:18px;box-shadow:0 8px 32px #0004}.search input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:white;font-size:15px;padding:12px}.search button{border:0;border-radius:13px;background:linear-gradient(135deg,#a855f7,#6d28d9);color:white;padding:0 16px;font-size:18px}.label{margin:31px 0 14px;text-align:left;font-size:12px;color:#a99abf;letter-spacing:1.4px}.links{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}.link{padding:14px 4px;border:1px solid #2d2240;background:#171122;border-radius:16px;color:#e9ddfa;text-decoration:none;font-size:12px}.ico{display:block;font-size:23px;margin-bottom:9px}.foot{margin-top:38px;color:#706080;font-size:11px}</style></head><body><div class='wrap'><div class='orb'><svg viewBox='0 0 100 100' width='82' height='82' aria-label='Avatar anime de cabello azul'><defs><linearGradient id='hair' x1='0' y1='0' x2='1' y2='1'><stop stop-color='#9deaff'/><stop offset='1' stop-color='#3987ff'/></linearGradient></defs><path d='M17 45 Q9 13 38 10 Q65 0 80 23 Q95 42 82 71 L72 83 L28 83 L17 65Z' fill='url(#hair)'/><path d='M28 41 Q27 24 49 24 Q70 25 72 47 L68 68 Q50 84 32 67Z' fill='#ffe2d6'/><path d='M26 41 Q25 22 45 19 L39 38 L31 55Z M45 21 Q67 18 75 41 L66 43 L58 31 L50 46 L43 39Z' fill='#65c8ff'/><path d='M37 48 Q42 44 46 48 M55 48 Q60 44 64 48' stroke='#344c9d' stroke-width='3' fill='none' stroke-linecap='round'/><circle cx='42' cy='53' r='3' fill='#344c9d'/><circle cx='59' cy='53' r='3' fill='#344c9d'/><path d='M46 63 Q51 68 57 62' stroke='#d76d85' stroke-width='2' fill='none' stroke-linecap='round'/><path d='M20 34 Q7 37 17 55 L29 59 L31 45Z M78 33 Q91 37 83 55 L72 59 L69 44Z' fill='#3987ff'/></svg></div><div class='brand'>ASTRAL<span>.</span></div><div class='tag'>Tu espacio. Tu universo. Tu navegador.</div><form class='search' action='https://duckduckgo.com/'><input name='q' placeholder='Busca o escribe una dirección…' autocomplete='off'><button type='submit'>⌕</button></form><div class='label'>ACCESOS RÁPIDOS</div><div class='links'><a class='link' href='https://www.youtube.com'><span class='ico'>▶</span>YouTube</a><a class='link' href='https://www.wikipedia.org'><span class='ico'>◈</span>Wiki</a><a class='link' href='https://www.google.com'><span class='ico'>◎</span>Google</a><a class='link' href='https://github.com'><span class='ico'>⌘</span>GitHub</a></div><div class='foot'>ASTRAL BROWSER · EXPLORA A TU MANERA</div></div></body></html>";
        w.loadDataWithBaseURL("https://astral.local/",html,"text/html","UTF-8",null);
        etUrl.setText("");
    }

    private void loadUrlFromInput(){
        String input=etUrl.getText().toString().trim(); hideKeyboard();
        if(input.isEmpty()){showHome(current());return;}
        current().setTag("web");
        if(input.startsWith("http://")||input.startsWith("https://")) current().loadUrl(input);
        else if(input.contains(".")&&!input.contains(" ")) current().loadUrl("https://"+input);
        else { try{String engine=getSharedPreferences("astral_settings", MODE_PRIVATE).getString("search_engine","duckduckgo"); String base=engine.equals("google")?"https://www.google.com/search?q=":engine.equals("bing")?"https://www.bing.com/search?q=":"https://duckduckgo.com/?q="; current().loadUrl(base+URLEncoder.encode(input,"UTF-8"));}catch(Exception e){current().loadUrl("https://duckduckgo.com");} }
    }
    private void showSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void addSettingsHeading(LinearLayout panel, String heading) {
        TextView title = settingsText(heading, 13, Color.rgb(192,132,252));
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, dp(18), 0, dp(7));
        panel.addView(title);
        View line = new View(this);
        line.setBackgroundColor(Color.rgb(63, 43, 82));
        panel.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private TextView settingsText(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setPadding(0, dp(5), 0, dp(5));
        return text;
    }

    private void clearProxy() {
        getSharedPreferences("astral_settings", MODE_PRIVATE).edit().putBoolean("proxy_enabled", false).apply();
        try {
            ProxyController.getInstance().clearProxyOverride(Runnable::run,
                () -> runOnUiThread(() -> Toast.makeText(this, "Proxy desactivado", Toast.LENGTH_SHORT).show()));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo quitar el proxy: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void destroySession(){
        for(WebView w:tabs){w.clearCache(true);w.clearHistory();}
        CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush();
        showHome(current()); Toast.makeText(this,"Rastro borrado: cookies y caché eliminadas",Toast.LENGTH_SHORT).show();
    }
    private void hideKeyboard(){View v=getCurrentFocus(); if(v!=null){InputMethodManager im=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);if(im!=null)im.hideSoftInputFromWindow(v.getWindowToken(),0);}}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onBackPressed(){if(current().canGoBack())current().goBack();else if(!isHome(current()))showHome(current());else if(tabs.size()>1)closeTab(activeTab);else super.onBackPressed();}
}