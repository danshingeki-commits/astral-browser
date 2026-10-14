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
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><meta name='theme-color' content='#05030d'><style>\n*{box-sizing:border-box}html,body{margin:0;min-height:100%;font-family:Arial,Helvetica,sans-serif;color:#f8f5ff;background:#05030d}body{background:radial-gradient(ellipse at 50% 18%,#28105a 0%,#100827 40%,#05030d 76%);min-height:100vh;overflow-x:hidden}.scene{position:fixed;inset:0;z-index:0;pointer-events:none;overflow:hidden;background:radial-gradient(ellipse at 76% 73%,#d000ff22,transparent 30%),radial-gradient(ellipse at 12% 32%,#5924ff30,transparent 35%),linear-gradient(180deg,#090516,#070414 60%,#03030b)}.scene:before{content:'';position:absolute;inset:12% -10% 0;background:linear-gradient(165deg,transparent 44%,#170b45 45%,#070a24 60%,#030611 80%);clip-path:polygon(0 35%,9% 18%,16% 38%,25% 12%,37% 43%,46% 24%,55% 47%,68% 16%,79% 39%,91% 10%,100% 35%,100% 100%,0 100%);opacity:.9}.scene:after{content:'';position:absolute;left:10%;right:10%;bottom:0;height:36%;background:repeating-linear-gradient(174deg,transparent 0 14px,#6b21a833 15px,#22d3ee22 16px,transparent 18px 32px);transform:perspective(180px) rotateX(25deg);transform-origin:bottom;mask-image:linear-gradient(transparent,#000)}.stars{position:absolute;inset:0;background-image:radial-gradient(#fff 1px,transparent 1.6px),radial-gradient(#b78aff 1px,transparent 1.7px),radial-gradient(#22d3ee 1px,transparent 1.8px);background-size:83px 93px,137px 121px,171px 163px;background-position:10px 8px,45px 60px,82px 21px;opacity:.48}.wrap{max-width:650px;position:relative;z-index:1;margin:auto;padding:25px 18px 36px}.top{display:flex;align-items:center;gap:12px;margin-bottom:28px}.logo{width:54px;height:54px;border:1px solid #8b5cf6;border-radius:17px;background:linear-gradient(145deg,#26124b,#07172a);display:grid;place-items:center;box-shadow:0 0 25px #7c3aed55,inset 0 0 20px #22d3ee1b}.logo svg{width:36px;filter:drop-shadow(0 0 7px #a855f7)}.brand{font-family:Georgia,serif;font-size:29px;font-weight:bold;letter-spacing:.2px;text-shadow:0 0 14px #a855f755}.topright{margin-left:auto;display:flex;gap:10px}.mini{border:1px solid #5b21b6;background:#120a27;color:#d8b4fe;border-radius:13px;padding:10px 12px;font-size:15px}.hero{text-align:center;padding:10px 0 24px}.orb{width:178px;height:118px;margin:0 auto 13px;border:1px solid #a855f7;border-bottom-color:#22d3ee;border-radius:100px 100px 38px 38px;box-shadow:0 -5px 32px #a855f73b,inset 0 0 28px #7c3aed25;display:flex;align-items:center;justify-content:center;position:relative}.orb:after{content:'';position:absolute;inset:10px;border:1px solid #22d3ee33;border-radius:inherit}.astralword{font-size:clamp(32px,9vw,48px);font-weight:1000;font-style:italic;letter-spacing:-2px;line-height:.9;background:linear-gradient(115deg,#67e8f9,#c4b5fd 43%,#a855f7 80%);color:transparent;background-clip:text;-webkit-background-clip:text;text-shadow:0 0 28px #8b5cf755}.browserword{font-size:12px;letter-spacing:6px;font-weight:bold;color:#22d3ee;margin-top:11px}.tag{font-size:10px;letter-spacing:4px;color:#c4b5fd;margin:0 0 26px}.search{display:flex;align-items:center;gap:10px;padding:8px 9px 8px 17px;border:1px solid transparent;border-radius:50px;background:linear-gradient(#080719,#080719) padding-box,linear-gradient(100deg,#a855f7,#7c3aed,#22d3ee) border-box;box-shadow:0 0 23px #7c3aed32,0 0 5px #22d3ee22}.glass{font-size:27px;color:#c4b5fd}.search input{flex:1;min-width:0;border:0;outline:0;background:transparent;color:white;font-size:15px;padding:11px 2px}.search input::placeholder{color:#a5a0c9}.search button{width:43px;height:43px;border:0;border-radius:50%;background:linear-gradient(135deg,#a855f7,#2563eb,#22d3ee);color:white;font-size:22px;box-shadow:0 0 17px #a855f755}.section{margin-top:28px}.heading{display:flex;align-items:center;justify-content:space-between;margin:0 2px 13px}.heading h2{font-size:17px;font-weight:600;margin:0;color:#d8b4fe}.heading h2 span{color:#22d3ee;margin-right:9px}.edit{border:0;background:transparent;color:#67e8f9;font-size:13px;padding:6px}.links{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:9px}.link{min-width:0;text-align:center;text-decoration:none;color:white;border:1px solid transparent;border-radius:20px;background:linear-gradient(#0b0a1d,#100821) padding-box,linear-gradient(145deg,#8b5cf6,#22d3ee 58%,#a855f7) border-box;padding:13px 2px 12px;box-shadow:0 0 17px #7c3aed18}.ico{width:43px;height:43px;margin:0 auto 9px;display:grid;place-items:center;font-size:27px;font-weight:bold;color:#fff}.yt{color:#ff2727;font-size:34px}.wiki{font-family:Georgia,serif;font-size:34px}.google{font-size:31px;background:conic-gradient(#4285f4 0 25%,#34a853 25% 50%,#fbbc05 50% 75%,#ea4335 75%);color:transparent;background-clip:text}.git{font-size:30px}.link strong{display:block;font-size:12px;font-weight:500;white-space:nowrap}.link small{display:block;margin-top:7px;font-size:9px;color:#b7addd;white-space:nowrap}.suggestions{border:1px solid #3559a6;border-radius:20px;background:#08091dcc;box-shadow:0 0 24px #2563eb19;padding:4px 14px}.suggest{display:flex;align-items:center;gap:12px;padding:12px 2px;text-decoration:none;color:#f4efff;border-bottom:1px solid #28244a}.suggest:last-child{border-bottom:0}.sico{width:36px;height:36px;flex-shrink:0;display:grid;place-items:center;border:1px solid #3f3b8c;border-radius:50%;background:#17163b;color:#22d3ee;font-size:19px}.slabel{flex:1;font-size:13px}.slabel small{display:block;margin-top:4px;font-size:10px;color:#a6a2cb}.arrow{font-size:22px;color:#c084fc}.foot{text-align:center;margin-top:29px;color:#8375ac;font-size:9px;letter-spacing:2px}.foot b{color:#22d3ee;font-weight:600}@media(min-width:500px){.wrap{padding-top:34px}.hero{padding-top:18px}.orb{width:210px;height:130px}.link{padding:18px 4px 16px}.link strong{font-size:13px}.link small{font-size:10px}}\n</style></head><body><div class='scene'><div class='stars'></div></div><main class='wrap'><header class='top'><div class='logo'><svg viewBox='0 0 40 40' fill='none'><path d='M20 2 24 15 38 8 28 20 38 29 23 26 17 39 16 24 2 28 12 18 5 8 18 14Z' fill='url(#g)' stroke='#22d3ee' stroke-width='1'/><defs><linearGradient id='g' x1='4' y1='3' x2='36' y2='37'><stop stop-color='#22d3ee'/><stop offset='.55' stop-color='#a855f7'/><stop offset='1' stop-color='#4f46e5'/></linearGradient></defs></svg></div><div class='brand'>Astral</div><div class='topright'><button class='mini' onclick='document.getElementById(\"query\").focus()' aria-label='Buscar'>⌕</button><button class='mini' onclick='location.href=\"https://astral.local/\"' aria-label='Inicio'>⋮</button></div></header><section class='hero'><div class='orb'><div><div class='astralword'>ASTRAL</div><div class='browserword'>BROWSER</div></div></div><div class='tag'>✦ NAVEGA SIN LÍMITES ✦</div><form class='search' action='https://duckduckgo.com/'><span class='glass'>⌕</span><input id='query' name='q' placeholder='Buscar o escribir una URL' autocomplete='off' autocapitalize='off'><button type='submit' aria-label='Buscar'>✦</button></form></section><section class='section'><div class='heading'><h2><span>✦</span>Accesos rápidos</h2><button class='edit' onclick='editLinks()'>Editar ✎</button></div><div class='links' id='links'><a class='link' href='https://www.youtube.com'><span class='ico yt'>▶</span><strong>YouTube</strong><small>Vídeos · Música</small></a><a class='link' href='https://www.wikipedia.org'><span class='ico wiki'>W</span><strong>Wikipedia</strong><small>La enciclopedia libre</small></a><a class='link' href='https://www.google.com'><span class='ico google'>G</span><strong>Google</strong><small>Buscar · Explorar</small></a><a class='link' href='https://github.com'><span class='ico git'>◈</span><strong>GitHub</strong><small>Código · Proyectos</small></a></div></section><section class='section'><div class='heading'><h2><span>ϟ</span>Sugerencias</h2></div><div class='suggestions'><a class='suggest' href='https://open.spotify.com'><span class='sico'>⌕</span><span class='slabel'>Spotify Web<small>open.spotify.com</small></span><span class='arrow'>⌄</span></a><a class='suggest' href='https://chatgpt.com'><span class='sico'>⌕</span><span class='slabel'>ChatGPT<small>chatgpt.com</small></span><span class='arrow'>↗</span></a><a class='suggest' href='https://www.netflix.com'><span class='sico'>⌕</span><span class='slabel'>Netflix<small>netflix.com</small></span><span class='arrow'>↗</span></a><a class='suggest' href='https://discord.com'><span class='sico'>⌕</span><span class='slabel'>Discord<small>discord.com</small></span><span class='arrow'>↗</span></a></div></section><footer class='foot'>ASTRAL BROWSER · <b>EXPLORA SIN LÍMITES ✦</b></footer></main><script>function editLinks(){var n=prompt('¿Qué acceso rápido quieres editar? Escribe 1 YouTube, 2 Wikipedia, 3 Google o 4 GitHub.');var i=parseInt(n,10)-1;var a=document.querySelectorAll('#links a');if(!Number.isInteger(i)||i<0||i>=a.length)return;var name=prompt('Nombre del acceso:',a[i].querySelector('strong').textContent);if(!name)return;var url=prompt('Dirección web:',a[i].href);if(!url)return;if(!/^https?:\\/\\//i.test(url))url='https://'+url;a[i].href=url;a[i].querySelector('strong').textContent=name;a[i].querySelector('small').textContent=new URL(url).hostname;}</script></body></html>";
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