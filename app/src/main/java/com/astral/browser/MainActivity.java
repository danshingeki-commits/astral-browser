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
import android.app.DownloadManager;
import android.net.Uri;
import android.database.Cursor;
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
        Button burn = findViewById(R.id.btnBurn), addTab = findViewById(R.id.btnAddTab), menu = findViewById(R.id.btnMenu);
        Button home = findViewById(R.id.btnHome), tabsButton = findViewById(R.id.btnTabs);
        applyBrowserTheme(back,forward,refresh,go,burn,addTab,home,tabsButton,menu);

        back.setOnClickListener(v -> { WebView w=current(); if(w.canGoBack()) w.goBack(); else showHome(w); });
        forward.setOnClickListener(v -> { if(current().canGoForward()) current().goForward(); });
        refresh.setOnClickListener(v -> { if (isHome(current())) showHome(current()); else current().reload(); });
        go.setOnClickListener(v -> loadUrlFromInput());
        burn.setOnClickListener(v -> destroySession());
        addTab.setOnClickListener(v -> newTab(true));
        home.setOnClickListener(v -> showHome(current()));
        tabsButton.setOnClickListener(v -> showTabsMenu());
        menu.setOnClickListener(v -> showTabsMenu());
        etUrl.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrlFromInput(); return true;
            }
            return false;
        });
        CookieManager.getInstance().setAcceptCookie(true);
        restoreTabs();
    }

    private void applyBrowserTheme(Button... controls) {
        int idx=Math.max(0,Math.min(5,getSharedPreferences("astral_settings",MODE_PRIVATE).getInt("theme_index",0)));
        int[][] colors={{75,234,255,39,223,245,6,60,74},{192,132,252,167,139,250,52,34,74},{74,222,128,34,197,94,13,57,36},{251,113,133,244,63,94,71,22,36},{251,191,36,245,158,11,73,48,7},{125,211,252,56,189,248,16,52,71}};
        int[] p=colors[idx]; int accent=Color.rgb(p[0],p[1],p[2]); int fill=Color.rgb(p[6],p[7],p[8]);
        getWindow().setStatusBarColor(Color.rgb(3,7,11)); getWindow().setNavigationBarColor(Color.rgb(3,7,11));
        for(Button b:controls){if(b!=null){b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fill));b.setTextColor(accent);}}
    }

    private final class HomeBridge {
        @android.webkit.JavascriptInterface
        public void openMenu() { runOnUiThread(() -> showTabsMenu()); }
        @android.webkit.JavascriptInterface
        public void navigate(String value) {
            runOnUiThread(() -> {
                etUrl.setText(value == null ? "" : value);
                loadUrlFromInput();
            });
        }
    }

    private WebView current() { return tabs.get(activeTab); }
    private boolean isHome(WebView w) { return w.getTag()!=null && "home".equals(w.getTag()); }

    @SuppressLint("SetJavaScriptEnabled")
    private void newTab(boolean show) {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.rgb(3,7,11));
        WebSettings s=w.getSettings();
        s.setJavaScriptEnabled(getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("javascript_enabled", true)); s.setDomStorageEnabled(true); s.setTextZoom(getSharedPreferences("astral_settings", MODE_PRIVATE).getInt("text_zoom", 100)); s.setJavaScriptCanOpenWindowsAutomatically(!getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("block_popups", true)); s.setMediaPlaybackRequiresUserGesture(!getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("autoplay_media", false));
        s.setSupportZoom(true); s.setBuiltInZoomControls(true); s.setDisplayZoomControls(false);
        applyDesktopMode(w);
        w.addJavascriptInterface(new HomeBridge(), "AstralNative");
        CookieManager.getInstance().setAcceptThirdPartyCookies(w,getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("third_party_cookies", false));
        w.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            try {
                String fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle(fileName);
                request.setDescription("Descargando desde Astral Browser");
                request.setMimeType(mimeType);
                request.addRequestHeader("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) request.addRequestHeader("Cookie", cookie);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (manager != null) { manager.enqueue(request); Toast.makeText(this, "Descarga iniciada: " + fileName, Toast.LENGTH_LONG).show(); }
            } catch (Exception e) { Toast.makeText(this, "No se pudo iniciar la descarga", Toast.LENGTH_SHORT).show(); }
        });
        w.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int p){ if(view==current()){ progressBar.setVisibility(p<100?View.VISIBLE:View.GONE); progressBar.setProgress(p); } }
            @Override public void onReceivedTitle(WebView view,String title){ int i=tabs.indexOf(view); if(i>=0){tabTitles.set(i, title==null||title.isEmpty()?"Nueva pestaña":title); renderTabs();} }
        });
        w.setWebViewClient(new WebViewClient(){
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("ad_block_enabled", true)) {
                    String host = request.getUrl().getHost();
                    if (host != null && isAdHost(host.toLowerCase(java.util.Locale.ROOT))) return new android.webkit.WebResourceResponse("text/plain","utf-8",new java.io.ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, request);
            }
            @Override public void onPageStarted(WebView view,String url,Bitmap icon){ if(!url.startsWith("https://astral.local/")) view.setTag("web"); if(view==current() && !url.startsWith("file:")) etUrl.setText(url.equals("about:blank")||url.startsWith("https://astral.local/")?"":url); }
            @Override public void onPageFinished(WebView view,String url){ if(view==current() && !isHome(view)) etUrl.setText(url); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req){
                String target = req.getUrl().toString();
                String host = req.getUrl().getHost();
                if (host != null && (host.equalsIgnoreCase("spotify.com") || host.endsWith(".spotify.com") || host.equalsIgnoreCase("spotify.link"))) {
                    // Premium pages are web pages; Spotify deep links may open the installed app.
                    if (host.equalsIgnoreCase("open.spotify.com") || host.equalsIgnoreCase("spotify.link")) {
                        try {
                            Intent appIntent = new Intent(Intent.ACTION_VIEW, req.getUrl());
                            appIntent.setPackage("com.spotify.music");
                            if (appIntent.resolveActivity(getPackageManager()) != null) {
                                startActivity(appIntent);
                                return true;
                            }
                        } catch (Exception ignored) { }
                    }
                }
                return false;
            }
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
            GradientDrawable bg=new GradientDrawable(); bg.setColor(i==activeTab?Color.rgb(5,54,66):Color.rgb(7,21,27)); bg.setCornerRadius(dp(14)); chip.setBackground(bg);
            TextView title=new TextView(this); title.setText((tabTitles.get(i).length()>13?tabTitles.get(i).substring(0,12)+"…":tabTitles.get(i))); title.setTextColor(i==activeTab?Color.WHITE:Color.rgb(184,165,208)); title.setTextSize(12); title.setMaxWidth(dp(105)); title.setSingleLine(true);
            chip.addView(title,new LinearLayout.LayoutParams(-2,dp(38)));
            TextView close=new TextView(this); close.setText(" × "); close.setTextColor(Color.rgb(75,234,255)); close.setTextSize(17);
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
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><meta name='theme-color' content='#070914'><style>*{box-sizing:border-box}html,body{margin:0;min-height:100%;font-family:Roboto,Arial,sans-serif;background:#070914;color:#f4f7ff}body{background:radial-gradient(ellipse at 8% 3%,#17324d 0,transparent 34%),radial-gradient(ellipse at 100% 48%,#24133c 0,transparent 34%),#070914}.page{max-width:650px;margin:auto;padding:18px 16px 30px}.nav{display:flex;align-items:center;gap:10px;padding:7px 0 20px}.logo{width:43px;height:43px;border-radius:15px;display:grid;place-items:center;background:linear-gradient(145deg,#37e7f5,#6257ff);color:#06121e;font-size:24px;box-shadow:0 0 25px #37e7f533}.brand{font-size:20px;font-weight:800;letter-spacing:.2px}.brand small{display:block;font-size:10px;color:#8fa8c8;font-weight:500;letter-spacing:1.6px;margin-top:3px}.private{margin-left:auto;border:1px solid #314363;border-radius:20px;padding:8px 10px;font-size:10px;color:#a9c8ff;background:#121a2a}.menu{border:0;background:#172033;color:#d8e5ff;border-radius:12px;width:38px;height:38px;font-size:23px}.hero{position:relative;overflow:hidden;padding:24px 21px 22px;border:1px solid #35496f;border-radius:26px;background:linear-gradient(135deg,#1b3651 0%,#141a31 52%,#241438 100%);box-shadow:0 18px 40px #0005}.hero:after{content:'✦';position:absolute;right:-8px;top:18px;font-size:116px;color:#8bdcff16;transform:rotate(10deg)}.tag{font-size:10px;text-transform:uppercase;letter-spacing:2px;color:#91e9ff}.hero h1{font-size:36px;line-height:1.06;margin:17px 0 10px;letter-spacing:-1px;max-width:300px}.hero h1 span{color:#69eaff}.hero p{font-size:13px;line-height:1.6;color:#bac9e4;max-width:280px;margin:0}.hero-actions{display:flex;gap:9px;margin-top:20px;position:relative;z-index:1}.action{border:1px solid #526c99;border-radius:13px;padding:11px 14px;background:#dffaff;color:#11182b;font-size:12px;font-weight:700}.action.ghost{background:#11192c;color:#d9e7ff;border-color:#384b70}.search{display:flex;align-items:center;gap:10px;margin-top:16px;padding:7px 8px 7px 14px;background:#131a2a;border:1px solid #344765;border-radius:17px;box-shadow:0 10px 24px #0003}.search span{font-size:21px;color:#7be9ff}.search input{min-width:0;flex:1;border:0;outline:0;background:transparent;color:white;font-size:14px;padding:12px 0}.search input::placeholder{color:#8495b2}.search button{border:0;border-radius:12px;width:42px;height:42px;background:linear-gradient(145deg,#36d9ee,#6862ff);color:#071321;font-size:21px}.section{margin-top:26px}.section-head{display:flex;align-items:center;justify-content:space-between;margin:0 2px 13px}.eyebrow{font-size:10px;text-transform:uppercase;letter-spacing:1.8px;color:#8b9dbb;margin-bottom:7px}.section h2{font-size:17px;margin:0;font-weight:750}.sub{font-size:11px;color:#8192b1}.shortcuts{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:9px}.shortcut{display:flex;flex-direction:column;align-items:center;gap:9px;padding:15px 3px 13px;border-radius:18px;border:1px solid #283653;background:linear-gradient(150deg,#151e32,#101421);color:#eef5ff;text-decoration:none;min-width:0}.shortcut .symbol{width:43px;height:43px;border-radius:15px;display:grid;place-items:center;background:#222e49;color:#70e9ff;font-size:23px;font-weight:800}.shortcut strong{font-size:11px;font-weight:600;white-space:nowrap}.shortcut small{font-size:9px;color:#91a1bd}.feature-grid{display:grid;grid-template-columns:1fr 1fr;gap:10px}.feature{padding:16px;border:1px solid #303957;border-radius:19px;background:linear-gradient(140deg,#18223a,#101421);min-height:124px;position:relative;overflow:hidden}.feature:after{content:'✧';position:absolute;right:10px;bottom:-20px;font-size:68px;color:#78dcff14}.feature .symbol{font-size:19px;color:#71eaff}.feature h3{font-size:14px;margin:12px 0 6px}.feature p{font-size:10px;line-height:1.5;color:#9baac5;margin:0}.sites{display:grid;grid-template-columns:1fr 1fr;gap:9px}.site{display:flex;align-items:center;gap:11px;padding:12px;border-radius:16px;border:1px solid #293550;background:#121827;text-decoration:none;color:#f3f6ff;min-width:0}.site .bubble{width:36px;height:36px;flex-shrink:0;border-radius:12px;display:grid;place-items:center;background:#202d48;color:#73eaff;font-size:17px}.site strong{font-size:12px;display:block}.site small{display:block;font-size:9px;color:#8e9fbb;margin-top:4px;overflow:hidden;text-overflow:ellipsis}.site .go{margin-left:auto;color:#7be8ff}.footer{text-align:center;margin-top:30px;padding-top:17px;border-top:1px solid #202b43;color:#637593;font-size:9px;letter-spacing:1.5px}@media(max-width:370px){.private{display:none}.hero h1{font-size:32px}.shortcuts{grid-template-columns:repeat(2,minmax(0,1fr))}.shortcut{flex-direction:row;justify-content:flex-start;padding:10px}.shortcut .symbol{width:35px;height:35px}.shortcut small{display:none}.sites{grid-template-columns:1fr}}</style></head><body><main class='page'><header class='nav'><div class='logo'>✦</div><div class='brand'>ASTRAL<small>WEB EXPLORER</small></div><div class='private'>● PRIVADO</div><button class='menu' onclick='if(window.AstralNative){AstralNative.openMenu()}'>⋮</button></header><section class='hero'><div class='tag'>✦ Tu espacio en internet</div><h1>Internet, a tu <span>manera.</span></h1><p>Descubre páginas, encuentra respuestas y sigue tu propio camino.</p><div class='hero-actions'><button class='action' onclick=\"document.getElementById('query').focus()\">⌕ Buscar ahora</button><button class='action ghost' onclick='if(window.AstralNative){AstralNative.openMenu()}'>▦ Mis pestañas</button></div></section><form class='search' id='searchForm'><span>⌕</span><input id='query' placeholder='Buscar o escribir una dirección' autocomplete='off' autocapitalize='off'><button type='submit'>↗</button></form><section class='section'><div class='section-head'><div><div class='eyebrow'>Tus favoritos rápidos</div><h2>Accesos directos</h2></div><span class='sub'>4 sitios</span></div><div class='shortcuts'><a class='shortcut' href='https://www.youtube.com'><span class='symbol' style='color:#ff637f'>▶</span><strong>YouTube</strong><small>Vídeos</small></a><a class='shortcut' href='https://www.wikipedia.org'><span class='symbol'>W</span><strong>Wikipedia</strong><small>Aprender</small></a><a class='shortcut' href='https://www.google.com'><span class='symbol'>G</span><strong>Google</strong><small>Buscar</small></a><a class='shortcut' href='https://www.spotify.com/premium'><span class='symbol' style='color:#67edaa'>♫</span><strong>Spotify</strong><small>Música</small></a></div></section><section class='section'><div class='section-head'><div><div class='eyebrow'>Diseñado para navegar</div><h2>Tu navegador</h2></div></div><div class='feature-grid'><article class='feature'><div class='symbol'>◈</div><h3>Control en tus manos</h3><p>Accede a tus pestañas, historial y descargas desde el menú.</p></article><article class='feature'><div class='symbol'>✧</div><h3>Tu estilo, tu Astral</h3><p>Personaliza la experiencia desde la configuración.</p></article></div></section><section class='section'><div class='section-head'><div><div class='eyebrow'>Un salto rápido</div><h2>Explora la web</h2></div></div><div class='sites'><a class='site' href='https://chatgpt.com'><span class='bubble'>✧</span><span><strong>ChatGPT</strong><small>chatgpt.com</small></span><span class='go'>↗</span></a><a class='site' href='https://surfshark.com'><span class='bubble'>S</span><span><strong>Surfshark</strong><small>surfshark.com</small></span><span class='go'>↗</span></a><a class='site' href='https://discord.com'><span class='bubble'>◉</span><span><strong>Discord</strong><small>discord.com</small></span><span class='go'>↗</span></a><a class='site' href='https://www.netflix.com'><span class='bubble'>N</span><span><strong>Netflix</strong><small>netflix.com</small></span><span class='go'>↗</span></a></div></section><footer class='footer'>ASTRAL BROWSER · EXPLORE BEYOND</footer></main><script>document.getElementById('searchForm').addEventListener('submit',function(e){e.preventDefault();var v=document.getElementById('query').value.trim();if(v&&window.AstralNative){AstralNative.navigate(v)}else if(v){location.href='https://duckduckgo.com/?q='+encodeURIComponent(v)}});</script></body></html>";
        w.loadDataWithBaseURL("https://astral.local/",html,"text/html","UTF-8",null);
        etUrl.setText("");
    }

    private void loadUrlFromInput(){
        String input=etUrl.getText().toString().trim();
        hideKeyboard();
        if(input.isEmpty()){showHome(current());return;}
        current().setTag("web");
        String candidate=input;
        if(candidate.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            current().loadUrl(candidate);
            return;
        }
        // A host/domain/path is a direct navigation, not a search query.
        // Examples: spotify.com/premium, surfshark.com, example.org/page.
        if(!candidate.contains(" ") && candidate.matches("(?i)^(localhost|(([a-z0-9-]+\\.)+[a-z]{2,})|([0-9]{1,3}\\.){3}[0-9]{1,3})(:[0-9]{1,5})?(/[^ ]*)?(\\?[^ ]*)?(#.*)?$")) {
            current().loadUrl("https://" + candidate);
            return;
        }
        // Bare brand names or phrases stay searches by design.
        try{
            String engine=getSharedPreferences("astral_settings", MODE_PRIVATE).getString("search_engine","duckduckgo");
            String base=engine.equals("google")?"https://www.google.com/search?q=":engine.equals("bing")?"https://www.bing.com/search?q=":"https://duckduckgo.com/?q=";
            current().loadUrl(base+URLEncoder.encode(candidate,"UTF-8"));
        }catch(Exception e){current().loadUrl("https://duckduckgo.com");}
    }
    private void applyDesktopMode(WebView webView) {
        boolean desktop = getSharedPreferences("astral_settings", MODE_PRIVATE).getBoolean("desktop_mode", false);
        String userAgent;
        if (desktop) {
            // Use a genuine desktop-style UA. Merely removing the word "Mobile"
            // leaves Android in the UA and many sites still serve their mobile layout.
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
        } else {
            userAgent = WebSettings.getDefaultUserAgent(this);
        }
        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(userAgent);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(desktop);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
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
        panel.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.rgb(19,25,43),Color.rgb(8,11,20)}));
        panel.setClipToOutline(true);
        TextView title = settingsText("Menú de Astral", 20, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        panel.addView(title);
        TextView subtitle = settingsText("Pestañas abiertas · " + tabs.size() + "  ·  Herramientas del navegador", 12, Color.rgb(150,169,199));
        panel.addView(subtitle);
        LinearLayout quickActions = new LinearLayout(this);
        quickActions.setOrientation(LinearLayout.HORIZONTAL);
        Button privateMode = new Button(this);
        privateMode.setText("◈ Incógnito");
        privateMode.setTextSize(12);
        privateMode.setAllCaps(false);
        privateMode.setTextColor(Color.WHITE);
        privateMode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(5,110,132)));
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
        LinearLayout browserActions = new LinearLayout(this);
        browserActions.setOrientation(LinearLayout.VERTICAL);
        browserActions.setPadding(0, dp(5), 0, dp(5));
        addMenuAction(browserActions, "✦   Astral Assistant", "Chatea, usa voz y busca en Internet", () -> { dialog.dismiss(); Intent assistant = new Intent(this, AssistantActivity.class); String url = current().getUrl(); assistant.putExtra("page_url", isHome(current()) || url == null ? "Página de inicio" : url); startActivity(assistant); });
        addMenuAction(browserActions, "◷   Historial", "Páginas visitadas en esta pestaña", () -> { dialog.dismiss(); showHistory(); });
        addMenuAction(browserActions, "↓   Descargas", "Archivos descargados", () -> { dialog.dismiss(); showDownloads(); });
        addMenuAction(browserActions, "☆   Favoritos", "Tus páginas guardadas", () -> { dialog.dismiss(); showFavorites(); });
        addMenuAction(browserActions, "⌫   Borrar datos de navegación", "Cookies, caché e historial", () -> { dialog.dismiss(); confirmClearData(); });
        addMenuAction(browserActions, "⚙   Configuración", "Ajustes de Astral Browser", () -> { dialog.dismiss(); showSettings(); });
        panel.addView(browserActions, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(8), dp(8));
            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(i == activeTab ? Color.rgb(28,42,68) : Color.rgb(15,21,35));
            rowBg.setCornerRadius(dp(13));
            row.setBackground(rowBg);
            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.VERTICAL);
            TextView tabName = settingsText((i == activeTab ? "●  " : "○  ") + tabTitles.get(i), 14, Color.WHITE);
            tabName.setMaxLines(1);
            tabName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labels.addView(tabName);
            TextView tabUrl = settingsText(isHome(tabs.get(i)) ? "Página de inicio" : (tabs.get(i).getUrl() == null ? "Cargando…" : tabs.get(i).getUrl()), 10, Color.rgb(122,157,168));
            tabUrl.setMaxLines(1);
            tabUrl.setEllipsize(android.text.TextUtils.TruncateAt.END);
            labels.addView(tabUrl);
            row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            Button close = new Button(this);
            close.setText("×");
            close.setTextSize(18);
            close.setTextColor(Color.rgb(75,234,255));
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
        add.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(5,110,132)));
        add.setOnClickListener(v -> { dialog.dismiss(); newTab(true); });
        panel.addView(add, new LinearLayout.LayoutParams(-1, -2));
        dialog.setView(panel);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.rgb(3,7,11)));
            dialog.getWindow().setGravity(android.view.Gravity.TOP | android.view.Gravity.END);
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.78f), android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams menuParams = dialog.getWindow().getAttributes();
            menuParams.y = dp(56);
            dialog.getWindow().setAttributes(menuParams);
        }
    }

    private void addMenuAction(LinearLayout parent, String title, String subtitle, Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(8), dp(8), dp(8));
        TextView primary = settingsText(title, 14, Color.WHITE);
        row.addView(primary);
        TextView secondary = settingsText(subtitle, 11, Color.rgb(122,157,168));
        row.addView(secondary);
        row.setBackgroundResource(android.R.drawable.list_selector_background);
        row.setOnClickListener(v -> action.run());
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void showHistory() {
        WebView w = current();
        android.webkit.WebBackForwardList history = w.copyBackForwardList();
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<String> urls = new ArrayList<>();
        for (int i = history.getSize() - 1; i >= 0; i--) {
            android.webkit.WebHistoryItem item = history.getItemAtIndex(i);
            String url = item.getUrl();
            if (url == null || url.startsWith("https://astral.local/")) continue;
            labels.add((item.getTitle() == null || item.getTitle().isEmpty() ? url : item.getTitle()) + "\\n" + url);
            urls.add(url);
        }
        if (labels.isEmpty()) {
            new android.app.AlertDialog.Builder(this).setTitle("Historial").setMessage("No hay páginas en el historial de esta pestaña todavía.").setPositiveButton("Aceptar", null).show();
            return;
        }
        new android.app.AlertDialog.Builder(this).setTitle("Historial de esta pestaña").setItems(labels.toArray(new String[0]), (d, which) -> current().loadUrl(urls.get(which))).setNeutralButton("Borrar historial", (d, which) -> {
            for (WebView tab : tabs) tab.clearHistory();
            Toast.makeText(this, "Historial de las pestañas borrado", Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Cerrar", null).show();
    }

    private void showDownloads() {
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (manager == null) {
            Toast.makeText(this, "Descargas no disponibles en este dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Long> ids = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        DownloadManager.Query query = new DownloadManager.Query();
        Cursor cursor = manager.query(query);
        if (cursor != null) {
            try {
                int idCol = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
                int titleCol = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE);
                int statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(idCol));
                    int status = cursor.getInt(statusCol);
                    String state = status == DownloadManager.STATUS_SUCCESSFUL ? "Completada" :
                        status == DownloadManager.STATUS_FAILED ? "Fallida" :
                        status == DownloadManager.STATUS_RUNNING ? "En curso" : "Pendiente";
                    labels.add(cursor.getString(titleCol) + " · " + state);
                }
            } finally { cursor.close(); }
        }
        if (labels.isEmpty()) {
            new android.app.AlertDialog.Builder(this).setTitle("Descargas").setMessage("Todavía no hay descargas registradas.").setPositiveButton("Aceptar", null).show();
            return;
        }
        new android.app.AlertDialog.Builder(this).setTitle("Descargas").setItems(labels.toArray(new String[0]), (d, which) -> {
            DownloadManager.Query one = new DownloadManager.Query().setFilterById(ids.get(which));
            Cursor item = manager.query(one);
            if (item != null) {
                try {
                    if (item.moveToFirst() && item.getInt(item.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        Intent view = new Intent(Intent.ACTION_VIEW);
                        view.setDataAndType(manager.getUriForDownloadedFile(ids.get(which)), "*/*");
                        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try { startActivity(view); } catch (Exception e) { Toast.makeText(this, "Archivo descargado en la carpeta Descargas", Toast.LENGTH_LONG).show(); }
                    } else Toast.makeText(this, "La descarga aún no está lista", Toast.LENGTH_SHORT).show();
                } finally { item.close(); }
            }
        }).setNegativeButton("Cerrar", null).show();
    }

    private void confirmClearData() {
        new android.app.AlertDialog.Builder(this).setTitle("Borrar datos de navegación")
            .setMessage("Se borrarán cookies, caché e historial de las pestañas abiertas.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Borrar", (d, which) -> destroySession()).show();
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
        TextView title = settingsText(heading, 13, Color.rgb(75,234,255));
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


    private boolean isAdHost(String host) {
        String[] blocked={"doubleclick.net","googlesyndication.com","googleadservices.com","adservice.google.com","adnxs.com","adsrvr.org","adform.net","taboola.com","outbrain.com","popads.net","popcash.net","propellerads.com","exoclick.com","adsterra.com","trafficjunky.net","criteo.com"};
        for(String domain:blocked)if(host.equals(domain)||host.endsWith("."+domain))return true;
        return false;
    }
    private void restoreTabs(){
        SharedPreferences prefs=getSharedPreferences("astral_settings",MODE_PRIVATE);String saved=prefs.getString("saved_tabs","");
        if(saved.isEmpty()){newTab(true);return;}
        try{JSONArray items=new JSONArray(saved);if(items.length()==0){newTab(true);return;}int selected=Math.max(0,Math.min(items.length()-1,prefs.getInt("saved_active_tab",0)));
            for(int i=0;i<items.length();i++){String url=items.optString(i,"");newTab(i==selected);WebView w=tabs.get(tabs.size()-1);if(url.startsWith("https://")||url.startsWith("http://")){w.setTag("web");w.loadUrl(url);}}
        }catch(Exception ignored){newTab(true);}
    }
    private void saveTabs(){
        if(tabs.isEmpty())return;try{JSONArray items=new JSONArray();for(WebView w:tabs){String url=w.getUrl();if(url==null||url.startsWith("https://astral.local/")||url.equals("about:blank"))url="";items.put(url==null?"":url);}getSharedPreferences("astral_settings",MODE_PRIVATE).edit().putString("saved_tabs",items.toString()).putInt("saved_active_tab",activeTab).apply();}catch(Exception ignored){}
    }
    @Override protected void onStop(){saveTabs();super.onStop();}
    private void destroySession(){
        for(WebView w:tabs){w.clearCache(true);w.clearHistory();}
        getSharedPreferences("astral_settings", MODE_PRIVATE).edit().remove("saved_tabs").remove("saved_active_tab").apply();
        CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush();
        showHome(current()); Toast.makeText(this,"Rastro borrado: cookies y caché eliminadas",Toast.LENGTH_SHORT).show();
    }
    private void hideKeyboard(){View v=getCurrentFocus(); if(v!=null){InputMethodManager im=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);if(im!=null)im.hideSoftInputFromWindow(v.getWindowToken(),0);}}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onBackPressed(){if(current().canGoBack())current().goBack();else if(!isHome(current()))showHome(current());else if(tabs.size()>1)closeTab(activeTab);else super.onBackPressed();}
}