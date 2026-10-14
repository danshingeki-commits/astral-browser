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
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><meta name='theme-color' content='#0b0813'><style>*{box-sizing:border-box}html{min-height:100%;background:#0b0813}body{margin:0;background:radial-gradient(ellipse at 50% -8%,#382052 0%,#171025 34%,#0b0813 72%);color:#f8f5ff;font-family:Arial,Helvetica,sans-serif;min-height:100vh}.wrap{width:100%;max-width:600px;margin:auto;padding:25px 19px 34px}.top{display:flex;align-items:center;justify-content:space-between;margin-bottom:34px}.brandline{display:flex;align-items:center;gap:11px}.mark{width:43px;height:43px;border-radius:14px;background:linear-gradient(145deg,#b36aff,#6c38c8 55%,#22d3ee);display:grid;place-items:center;box-shadow:0 0 28px #a855f744;border:1px solid #ffffff22}.mark svg{width:27px;height:27px}.brand{font-size:19px;font-weight:900;letter-spacing:2px}.brand span{color:#c084fc}.edition{font-size:9px;letter-spacing:2px;color:#9e8ab8;margin-top:4px}.topbtn{border:1px solid #49365d;background:#1b1428;color:#d9c6f5;border-radius:12px;padding:10px 12px;font-size:12px}.eyebrow{font-size:10px;letter-spacing:2.4px;color:#22d3ee;font-weight:700;margin-bottom:10px}.headline{font-size:30px;line-height:1.12;font-weight:800;letter-spacing:-.8px;margin:0;max-width:360px}.headline em{font-style:normal;color:#c084fc}.sub{font-size:13px;line-height:1.55;color:#a99abf;margin:12px 0 23px}.search{display:flex;gap:7px;padding:7px;border:1px solid #50366c;background:#191326;border-radius:18px;box-shadow:0 12px 34px #0005}.search input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:white;font-size:14px;padding:12px 9px}.search input::placeholder{color:#827391}.search button{width:47px;border:0;border-radius:13px;background:linear-gradient(135deg,#b35cff,#7036cb);color:white;font-size:22px;font-weight:700}.hint{display:flex;align-items:center;gap:6px;color:#827391;font-size:10px;margin:10px 5px 29px}.dot{width:6px;height:6px;border-radius:50%;background:#22d3ee;box-shadow:0 0 10px #22d3ee}.sectionhead{display:flex;align-items:center;justify-content:space-between;margin:0 2px 13px}.sectionhead h2{font-size:14px;margin:0;letter-spacing:.2px}.sectionhead span{font-size:10px;color:#8e7aa5;letter-spacing:1.3px}.links{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:9px}.link{min-width:0;padding:14px 3px 12px;border:1px solid #2d2340;background:linear-gradient(155deg,#1c1529,#13101d);border-radius:17px;color:#e9ddfa;text-decoration:none;text-align:center;font-size:10px;transition:transform .15s,border-color .15s}.link:active{transform:scale(.96);border-color:#a855f7}.ico{display:grid;place-items:center;width:37px;height:37px;margin:0 auto 9px;border-radius:13px;background:#2a1d3c;color:#d7b6ff;font-size:19px}.link:nth-child(2) .ico{background:#102d35;color:#67e8f9}.link:nth-child(3) .ico{background:#30203d;color:#e9b5ff}.link:nth-child(4) .ico{background:#1c2940;color:#a9c9ff}.feature{margin-top:28px;border:1px solid #38284b;border-radius:20px;padding:17px;background:linear-gradient(115deg,#21152f,#15101f 72%);position:relative;overflow:hidden}.feature:after{content:'';position:absolute;width:130px;height:130px;border:1px solid #a855f72b;border-radius:50%;right:-46px;top:-45px;box-shadow:0 0 0 17px #a855f70a,0 0 0 34px #22d3ee08}.featuretop{display:flex;align-items:center;gap:8px;color:#22d3ee;font-size:10px;letter-spacing:1.7px;font-weight:700}.feature h2{font-size:18px;margin:12px 0 7px}.feature p{font-size:12px;line-height:1.5;color:#a99abf;margin:0;max-width:290px}.feature a{display:inline-block;margin-top:15px;text-decoration:none;color:#eadcff;background:#5b2d82;border:1px solid #8e4bc0;border-radius:10px;padding:10px 13px;font-size:11px;font-weight:700}.bottom{display:flex;justify-content:space-between;gap:12px;margin-top:27px;padding:16px 2px 0;border-top:1px solid #2a2037;color:#756487;font-size:9px;letter-spacing:1.2px}.bottom b{color:#a78ac4;font-weight:600}@media(min-width:500px){.wrap{padding-top:35px}.headline{font-size:36px}.top{margin-bottom:44px}.link{padding:17px 5px 14px}}</style></head><body><main class='wrap'><header class='top'><div class='brandline'><div class='mark'><svg viewBox='0 0 32 32' fill='none'><path d='M16 3.5 28 27H22.5L16 14.1 9.5 27H4L16 3.5Z' fill='white'/><path d='M11.5 22h9' stroke='#22d3ee' stroke-width='2.2' stroke-linecap='round'/></svg></div><div><div class='brand'>ASTRAL<span>.</span></div><div class='edition'>BROWSER · NEON EDITION</div></div></div><button class='topbtn' onclick='document.getElementById(\"query\").focus()'>⌕ &nbsp; Buscar</button></header><div class='eyebrow'>TU UNIVERSO DIGITAL</div><h1 class='headline'>Explora más.<br><em>Descubre a tu manera.</em></h1><p class='sub'>Todo lo que buscas, en un solo lugar.<br>Bienvenido a tu espacio, bienvenido a Astral.</p><form class='search' action='https://duckduckgo.com/'><input id='query' name='q' placeholder='Buscar o escribir una dirección…' autocomplete='off' autocapitalize='off'><button type='submit' aria-label='Buscar'>↗</button></form><div class='hint'><span class='dot'></span> Una nueva ventana al universo de internet</div><section><div class='sectionhead'><h2>Accesos rápidos</h2><span>TUS SITIOS</span></div><div class='links'><a class='link' href='https://www.youtube.com'><span class='ico'>▶</span>YouTube</a><a class='link' href='https://www.wikipedia.org'><span class='ico'>◇</span>Wikipedia</a><a class='link' href='https://www.google.com'><span class='ico'>◎</span>Google</a><a class='link' href='https://github.com'><span class='ico'>⌘</span>GitHub</a></div></section><section class='feature'><div class='featuretop'><span>✦</span> MODO ASTRAL</div><h2>Tu próxima aventura empieza aquí.</h2><p>Abre una nueva búsqueda, visita tus sitios favoritos y sigue explorando sin límites.</p><a href='https://duckduckgo.com/'>EMPEZAR A EXPLORAR &nbsp; ↗</a></section><footer class='bottom'><span>ASTRAL BROWSER</span><b>EXPLORA A TU MANERA ✦</b></footer></main><script>document.getElementById('query').addEventListener('keydown',function(e){if(e.key==='Escape')this.value='';});</script></body></html>";
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