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
import org.json.JSONArray;
import org.json.JSONObject;

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
        tabsButton.setOnClickListener(v -> showTabsMenu());
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
        s.setJavaScriptEnabled(getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("javascript_enabled", true)); s.setDomStorageEnabled(true); s.setTextZoom(getSharedPreferences("astral_settings", MODE_PRIVATE).getInt("text_zoom", 100)); s.setJavaScriptCanOpenWindowsAutomatically(!getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("block_popups", true)); s.setMediaPlaybackRequiresUserGesture(!getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("autoplay_media", false));
        s.setSupportZoom(true); s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
        applyDesktopMode(w);
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
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><meta name='theme-color' content='#080611'><style>\n*{box-sizing:border-box}html,body{margin:0;min-height:100%;background:#080611;color:#f8f5ff;font-family:Arial,Helvetica,sans-serif}body{background:radial-gradient(ellipse at 50% 0%,#292044 0%,#100c1c 43%,#080611 78%);min-height:100vh}.wrap{max-width:620px;margin:auto;padding:22px 17px 30px;position:relative;overflow:hidden}.wrap:before{content:'';position:absolute;left:-90px;top:180px;width:210px;height:210px;border-radius:50%;background:#7c3aed1b;filter:blur(40px);pointer-events:none}.top{display:flex;align-items:center;justify-content:space-between;margin-bottom:33px;position:relative}.brandline{display:flex;align-items:center;gap:11px}.mark{width:47px;height:47px;border-radius:16px;background:linear-gradient(145deg,#211344,#111a34 60%,#102c37);display:grid;place-items:center;border:1px solid #8b5cf688;box-shadow:0 0 23px #9333ea30,inset 0 0 14px #22d3ee12}.mark svg{width:30px;height:30px;filter:drop-shadow(0 0 5px #22d3ee)}.brand{font-size:20px;font-weight:900;letter-spacing:2.3px}.brand span{color:#22d3ee}.edition{font-size:9px;letter-spacing:1.7px;color:#9b8bad;margin-top:5px}.topbtn{border:1px solid #45305e;background:#171124;color:#d9c6f5;border-radius:13px;padding:11px 13px;font-size:12px}.eyebrow{display:flex;align-items:center;gap:8px;font-size:10px;letter-spacing:2.5px;color:#67e8f9;font-weight:700;margin-bottom:12px}.eyebrow:before{content:'';width:20px;height:1px;background:#22d3ee;box-shadow:0 0 8px #22d3ee}.headline{font-size:31px;line-height:1.13;font-weight:850;letter-spacing:-.9px;margin:0;max-width:370px}.headline em{font-style:normal;background:linear-gradient(90deg,#d8b4fe,#a78bfa 48%,#67e8f9);-webkit-background-clip:text;background-clip:text;color:transparent}.sub{font-size:13px;line-height:1.65;color:#a99abf;margin:13px 0 22px;max-width:330px}.search{display:flex;gap:7px;padding:7px;border:1px solid transparent;background:linear-gradient(#100c1c,#100c1c) padding-box,linear-gradient(105deg,#a855f7,#6d5dfc,#22d3ee) border-box;border-radius:18px;box-shadow:0 0 20px #7c3aed18,0 14px 30px #0005}.search input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:white;font-size:14px;padding:12px 9px}.search input::placeholder{color:#827391}.search button{width:47px;border:0;border-radius:13px;background:linear-gradient(135deg,#a855f7,#6d28d9 65%,#2563eb);color:white;font-size:22px;font-weight:700;box-shadow:0 0 15px #8b5cf744}.hint{display:flex;align-items:center;gap:7px;color:#857694;font-size:10px;margin:11px 5px 29px}.dot{width:6px;height:6px;border-radius:50%;background:#22d3ee;box-shadow:0 0 10px #22d3ee}.sectionhead{display:flex;align-items:center;justify-content:space-between;margin:0 2px 13px}.sectionhead h2{font-size:14px;margin:0;letter-spacing:.2px}.sectionhead span{font-size:10px;color:#a78bfa;letter-spacing:1.3px}.links{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:9px}.link{min-width:0;padding:14px 3px 13px;border:1px solid #3a2a50;background:linear-gradient(155deg,#1c152b,#100d1a);border-radius:17px;color:#e9ddfa;text-decoration:none;text-align:center;font-size:10px;box-shadow:inset 0 1px #ffffff08;transition:transform .15s,border-color .15s}.link:active{transform:scale(.96);border-color:#22d3ee}.ico{display:grid;place-items:center;width:38px;height:38px;margin:0 auto 9px;border-radius:13px;background:#2a1d3c;color:#d7b6ff;font-size:20px;box-shadow:0 0 14px #a855f714}.link:nth-child(2) .ico{background:#102d35;color:#67e8f9}.link:nth-child(3) .ico{background:#30203d;color:#e9b5ff}.link:nth-child(4) .ico{background:#1c2940;color:#a9c9ff}.link small{display:block;color:#9585aa;font-size:9px;margin-top:6px;white-space:nowrap}.feature{margin-top:27px;border:1px solid #47315f;border-radius:20px;padding:17px;background:linear-gradient(115deg,#21152f,#141020 72%);position:relative;overflow:hidden;box-shadow:0 12px 30px #0002}.feature:after{content:'';position:absolute;width:130px;height:130px;border:1px solid #a855f72b;border-radius:50%;right:-46px;top:-45px;box-shadow:0 0 0 17px #a855f70a,0 0 0 34px #22d3ee08;pointer-events:none}.featuretop{display:flex;align-items:center;gap:8px;color:#22d3ee;font-size:10px;letter-spacing:1.7px;font-weight:700}.feature h2{font-size:18px;margin:12px 0 7px}.feature p{font-size:12px;line-height:1.6;color:#a99abf;margin:0;max-width:300px}.feature a{display:inline-block;margin-top:15px;text-decoration:none;color:#f5eaff;background:linear-gradient(100deg,#552a7a,#3b2b68);border:1px solid #8952b7;border-radius:10px;padding:11px 13px;font-size:10px;letter-spacing:.5px;font-weight:700}.bottom{display:flex;justify-content:space-between;gap:12px;margin-top:25px;padding:15px 2px 0;border-top:1px solid #2a2037;color:#756487;font-size:9px;letter-spacing:1.1px}.bottom b{color:#a78ac4;font-weight:600}@media(min-width:500px){.wrap{padding-top:32px}.headline{font-size:37px}.top{margin-bottom:42px}.link{padding:17px 5px 15px}}\n</style></head><body><main class='wrap'><header class='top'><div class='brandline'><div class='mark'><svg viewBox='0 0 32 32' fill='none'><path d='M16 3.5 28 27H22.5L16 14.1 9.5 27H4L16 3.5Z' fill='white'/><path d='M11.5 22h9' stroke='#22d3ee' stroke-width='2.2' stroke-linecap='round'/></svg></div><div><div class='brand'>ASTRAL<span>.</span></div><div class='edition'>BROWSER · NEON EDITION</div></div></div><button class='topbtn' onclick='document.getElementById(&quot;query&quot;).focus()'>⌕ &nbsp; Buscar</button></header><div class='eyebrow'>TU UNIVERSO DIGITAL</div><h1 class='headline'>Explora más.<br><em>Descubre a tu manera.</em></h1><p class='sub'>Todo lo que buscas, en un solo lugar.<br>Bienvenido a tu espacio, bienvenido a Astral.</p><form class='search' action='https://duckduckgo.com/'><input id='query' name='q' placeholder='Buscar o escribir una dirección…' autocomplete='off' autocapitalize='off'><button type='submit' aria-label='Buscar'>↗</button></form><div class='hint'><span class='dot'></span> Una nueva ventana al universo de internet</div><section><div class='sectionhead'><h2>✦ &nbsp; Accesos rápidos</h2><span>TUS SITIOS</span></div><div class='links'><a class='link' href='https://www.youtube.com'><span class='ico'>▶</span>YouTube<small>Vídeos · Música</small></a><a class='link' href='https://www.wikipedia.org'><span class='ico'>W</span>Wikipedia<small>Conocimiento</small></a><a class='link' href='https://www.google.com'><span class='ico'>◎</span>Google<small>Buscar · Explorar</small></a><a class='link' href='https://github.com'><span class='ico'>⌘</span>GitHub<small>Código · Ideas</small></a></div></section><section class='feature'><div class='featuretop'><span>✦</span> MODO ASTRAL</div><h2>Tu próxima aventura empieza aquí.</h2><p>Abre una nueva búsqueda, visita tus sitios favoritos y sigue explorando desde tu propio espacio digital.</p><a href='https://duckduckgo.com/'>EMPEZAR A EXPLORAR &nbsp; ↗</a></section><footer class='bottom'><span>ASTRAL BROWSER</span><b>EXPLORA A TU MANERA ✦</b></footer></main><script>document.getElementById('query').addEventListener('keydown',function(e){if(e.key==='Escape')this.value='';});</script></body></html>";
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
    private void applyDesktopMode(WebView webView) {
        boolean desktop = getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("desktop_mode", false);
        String userAgent = WebSettings.getDefaultUserAgent(this);
        if (desktop) {
            userAgent = userAgent.replace("Mobile", "").replace("Android", "X11; Linux x86_64");
            if (!userAgent.contains("X11; Linux x86_64")) userAgent += " X11; Linux x86_64";
        }
        webView.getSettings().setUserAgentString(userAgent);
        webView.getSettings().setLoadWithOverviewMode(desktop);
        webView.getSettings().setUseWideViewPort(desktop);
    }

    private void showSettings() {
        Intent settings = new Intent(this, SettingsActivity.class);
        settings.putExtra("open_tabs_count", tabs.size());
        startActivity(settings);
    }

    private void showTabsMenu() {
        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).create();
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(12));
        panel.setBackgroundColor(Color.rgb(11, 8, 19));
        TextView title = settingsText("Pestañas abiertas · " + tabs.size(), 20, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        panel.addView(title);
        TextView subtitle = settingsText("Toca una tarjeta para cambiar de pestaña", 12, Color.rgb(169,154,190));
        panel.addView(subtitle);
        LinearLayout quickActions = new LinearLayout(this);
        quickActions.setOrientation(LinearLayout.HORIZONTAL);
        Button privateMode = new Button(this);
        privateMode.setText("◈ Incógnito");
        privateMode.setTextSize(12);
        privateMode.setAllCaps(false);
        privateMode.setTextColor(Color.WHITE);
        privateMode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(91,45,130)));
        privateMode.setOnClickListener(v -> { dialog.dismiss(); startActivity(new Intent(this, IncognitoActivity.class)); });
        Button saveFavorite = new Button(this);
        saveFavorite.setText("☆ Guardar");
        saveFavorite.setTextSize(12);
        saveFavorite.setAllCaps(false);
        saveFavorite.setTextColor(Color.WHITE);
        saveFavorite.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(35,24,49)));
        saveFavorite.setOnClickListener(v -> { dialog.dismiss(); saveCurrentFavorite(); });
        Button openFavorites = new Button(this);
        openFavorites.setText("★ Favoritos");
        openFavorites.setTextSize(12);
        openFavorites.setAllCaps(false);
        openFavorites.setTextColor(Color.WHITE);
        openFavorites.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(35,24,49)));
        openFavorites.setOnClickListener(v -> { dialog.dismiss(); showFavorites(); });
        quickActions.addView(privateMode,new LinearLayout.LayoutParams(0,dp(46),1));
        quickActions.addView(saveFavorite,new LinearLayout.LayoutParams(0,dp(46),1));
        quickActions.addView(openFavorites,new LinearLayout.LayoutParams(0,dp(46),1));
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(-1,-2);
        actionsLp.setMargins(0,dp(8),0,dp(8));
        panel.addView(quickActions,actionsLp);
        Button desktopMode = new Button(this);
        boolean desktopEnabled = getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("desktop_mode", false);
        desktopMode.setText(desktopEnabled ? "▣  Modo escritorio: ACTIVADO" : "▣  Modo escritorio: desactivado");
        desktopMode.setAllCaps(false);
        desktopMode.setTextSize(13);
        desktopMode.setTextColor(Color.WHITE);
        desktopMode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(35,24,49)));
        desktopMode.setOnClickListener(v -> {
            boolean enabled = !getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("desktop_mode", false);
            getSharedPreferences("astral_settings", MODE_PRIVATE).edit().putBoolean("desktop_mode", enabled).apply();
            for (WebView tab : tabs) { applyDesktopMode(tab); if (!isHome(tab) && tab.getUrl() != null) tab.reload(); }
            dialog.dismiss();
            Toast.makeText(this, enabled ? "Modo escritorio activado" : "Modo móvil activado", Toast.LENGTH_SHORT).show();
        });
        panel.addView(desktopMode, new LinearLayout.LayoutParams(-1, dp(46)));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(8), dp(8));
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(i == activeTab ? Color.rgb(48,31,72) : Color.rgb(24,18,37));
            rowBg.setCornerRadius(dp(13));
            row.setBackground(rowBg);
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView tabName = settingsText((i == activeTab ? "●  " : "○  ") + tabTitles.get(i), 14, Color.WHITE);
            tabName.setMaxLines(1);
            tabName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labels.addView(tabName);
            TextView tabUrl = settingsText(isHome(tabs.get(i)) ? "Página de inicio" : (tabs.get(i).getUrl() == null ? "Cargando…" : tabs.get(i).getUrl()), 10, Color.rgb(169,154,190));
            tabUrl.setMaxLines(1);
            tabUrl.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labels.addView(tabUrl);
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            Button close = new Button(this);
            close.setText("×");
            close.setTextSize(18);
            close.setTextColor(Color.rgb(192,132,252));
            close.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(35,24,49)));
            close.setMinWidth(dp(42));
            close.setOnClickListener(v -> { dialog.dismiss(); closeTab(index); showTabsMenu(); });
            row.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
            row.setOnClickListener(v -> { dialog.dismiss(); switchTab(index); });
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            rowLp.setMargins(0, dp(5), 0, dp(5));
            list.addView(row, rowLp);
        }
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button add = new Button(this);
        add.setText("+  Nueva pestaña");
        add.setTextColor(Color.WHITE);
        add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(91,45,130)));
        add.setOnClickListener(v -> { dialog.dismiss(); newTab(true); });
        panel.addView(add, new LinearLayout.LayoutParams(-1, -2));
        dialog.setView(panel);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.rgb(11,8,19)));
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.92f), (int)(getResources().getDisplayMetrics().heightPixels * 0.78f));
        }
    }

    private void saveCurrentFavorite() {
        WebView w=current();
        String url=w.getUrl();
        if(url==null || url.isEmpty() || isHome(w) || url.startsWith("https://astral.local/")) {
            Toast.makeText(this,"Abre una página web antes de guardarla",Toast.LENGTH_SHORT).show(); return;
        }
        android.content.SharedPreferences prefs=getSharedPreferences("astral_favorites",MODE_PRIVATE);
        JSONArray old;
        try { old=new JSONArray(prefs.getString("items","[]")); } catch(Exception e) { old=new JSONArray(); }
        try {
            for(int i=0;i<old.length();i++) if(url.equals(old.getJSONObject(i).optString("url"))) {
                Toast.makeText(this,"Ese favorito ya está guardado",Toast.LENGTH_SHORT).show(); return;
            }
            JSONObject item=new JSONObject(); item.put("title",tabTitles.get(activeTab)); item.put("url",url); old.put(item);
            prefs.edit().putString("items",old.toString()).apply();
            Toast.makeText(this,"Favorito guardado",Toast.LENGTH_SHORT).show();
        } catch(Exception e) { Toast.makeText(this,"No se pudo guardar el favorito",Toast.LENGTH_SHORT).show(); }
    }

    private void showFavorites() {
        android.content.SharedPreferences prefs=getSharedPreferences("astral_favorites",MODE_PRIVATE);
        JSONArray items;
        try { items=new JSONArray(prefs.getString("items","[]")); } catch(Exception e) { items=new JSONArray(); }
        if(items.length()==0) {
            new android.app.AlertDialog.Builder(this).setTitle("Favoritos").setMessage("Todavía no tienes favoritos. Abre una página y toca ☆ Guardar en el menú de pestañas.").setPositiveButton("Entendido",null).show(); return;
        }
        ArrayList<JSONObject> saved=new ArrayList<>();
        ArrayList<String> labels=new ArrayList<>();
        for(int i=0;i<items.length();i++) try {
            JSONObject item=items.getJSONObject(i); saved.add(item);
            labels.add(item.optString("title","Página guardada")+"\n"+item.optString("url",""));
        } catch(Exception ignored) {}
        String[] choices=labels.toArray(new String[0]);
        new android.app.AlertDialog.Builder(this).setTitle("Favoritos · "+saved.size()).setItems(choices,(d,which)->{
            JSONObject item=saved.get(which);String url=item.optString("url","");
            if(!url.isEmpty())current().loadUrl(url);
        }).setNeutralButton("Administrar",(d,w)->manageFavorites()).setNegativeButton("Cerrar",null).show();
    }

    private void manageFavorites() {
        android.content.SharedPreferences prefs=getSharedPreferences("astral_favorites",MODE_PRIVATE);
        JSONArray items;
        try { items=new JSONArray(prefs.getString("items","[]")); } catch(Exception e) { items=new JSONArray(); }
        ArrayList<JSONObject> saved=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
        for(int i=0;i<items.length();i++)try{JSONObject item=items.getJSONObject(i);saved.add(item);labels.add(item.optString("title","Página guardada")+"\n"+item.optString("url",""));}catch(Exception ignored){}
        if(saved.isEmpty()){showFavorites();return;}
        new android.app.AlertDialog.Builder(this).setTitle("Mantén pulsado un favorito para eliminarlo").setItems(labels.toArray(new String[0]),(d,which)->{
            JSONObject item=saved.get(which);
            new android.app.AlertDialog.Builder(this).setMessage("¿Eliminar "+item.optString("title","este favorito")+"?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(dd,ww)->{
                JSONArray updated=new JSONArray();
                for(JSONObject candidate:saved)if(!candidate.optString("url","").equals(item.optString("url","")))updated.put(candidate);
                prefs.edit().putString("items",updated.toString()).apply();showFavorites();
            }).show();
        }).setNegativeButton("Cerrar",null).show();
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