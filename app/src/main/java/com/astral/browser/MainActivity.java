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
        Button burn = findViewById(R.id.btnBurn), addTab = findViewById(R.id.btnAddTab);
        Button home = findViewById(R.id.btnHome), tabsButton = findViewById(R.id.btnTabs);

        back.setOnClickListener(v -> { WebView w=current(); if(w.canGoBack()) w.goBack(); else showHome(w); });
        forward.setOnClickListener(v -> { if(current().canGoForward()) current().goForward(); });
        refresh.setOnClickListener(v -> { if (isHome(current())) showHome(current()); else current().reload(); });
        go.setOnClickListener(v -> loadUrlFromInput());
        burn.setOnClickListener(v -> destroySession());
        addTab.setOnClickListener(v -> newTab(true));
        home.setOnClickListener(v -> showHome(current()));
        tabsButton.setOnClickListener(v -> showTabsMenu());
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

    private final class HomeBridge {
        @android.webkit.JavascriptInterface
        public void openMenu() { runOnUiThread(() -> showTabsMenu()); }
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
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><meta name='theme-color' content='#03070b'><style>\n*{box-sizing:border-box}html,body{margin:0;min-height:100%;font-family:Roboto,Arial,sans-serif;background:#03070b;color:#e6fbff}body{min-height:100vh;background-image:radial-gradient(ellipse at 15% 35%,rgba(0,210,240,.09),transparent 38%),radial-gradient(ellipse at 90% 78%,rgba(0,125,180,.10),transparent 35%);background-attachment:fixed}.wrap{max-width:620px;margin:auto;padding:26px 17px 32px;position:relative}.top{display:flex;align-items:center;gap:11px;margin-bottom:24px;padding:10px 12px;border:1px solid #10343d;border-radius:17px;background:linear-gradient(120deg,#081a21,#050b10)}.mark{width:46px;height:46px;display:grid;place-items:center;border-radius:14px;background:linear-gradient(145deg,#0b303b,#07151b);border:1px solid #146477;color:#4beaff;font-size:25px;box-shadow:0 0 18px rgba(39,223,245,.08)}.brand{font-size:21px;font-weight:650;letter-spacing:.3px}.brand span{color:#27dff5}.subtitle{font-size:11px;color:#7a9da8;margin-top:3px;letter-spacing:.6px}.status{margin-left:auto;color:#27dff5;font-size:11px;border:1px solid #14505d;border-radius:12px;padding:8px 10px;background:#071820}.dots{font-size:22px;width:38px;height:38px;padding:0;flex-shrink:0}.welcome{text-align:left;margin:22px 4px 22px;padding:24px 19px;border:1px solid #12505d;border-radius:22px;background:linear-gradient(135deg,rgba(6,64,77,.72),rgba(5,13,20,.95) 62%,rgba(0,29,39,.8));box-shadow:0 10px 30px rgba(0,0,0,.2);position:relative;overflow:hidden}.welcome:after{content:'✦';position:absolute;right:18px;top:7px;font-size:76px;color:rgba(39,223,245,.14);pointer-events:none}.wordmark{font-size:35px;font-weight:800;letter-spacing:1px}.wordmark span{color:#27dff5}.welcome p{margin:8px 0 0;color:#8bb3bd;font-size:13px;max-width:230px;line-height:1.5}.search{display:flex;align-items:center;gap:11px;padding:7px 8px 7px 15px;border:1px solid #176a7a;border-radius:17px;background:linear-gradient(100deg,#07151b,#0a2028);box-shadow:0 0 0 3px rgba(39,223,245,.035),0 8px 24px #0003}.search:focus-within{border-color:#4beaff;box-shadow:0 0 0 2px #27dff522}.glass{font-size:22px;color:#4beaff}.search input{flex:1;min-width:0;border:0;outline:0;background:transparent;color:#fff;font-size:14px;padding:12px 0}.search input::placeholder{color:#72939d}.search button{border:0;border-radius:50%;width:39px;height:39px;background:#063c4a;color:#d9fbff;font-size:19px}.section{margin-top:27px}.heading{display:flex;align-items:center;justify-content:space-between;margin:0 2px 13px}.heading h2{margin:0;font-size:15px;font-weight:700;letter-spacing:.2px}.edit{border:0;background:transparent;color:#4beaff;font-size:12px;padding:6px}.links{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:9px}.link{min-width:0;text-decoration:none;color:#e6fbff;text-align:center;border:1px solid #123640;background:linear-gradient(160deg,#0b1c23,#050d12);border-radius:13px;padding:15px 3px 13px;box-shadow:inset 0 1px 0 rgba(75,234,255,.06),0 7px 16px rgba(0,0,0,.12)}.link:active{background:#0a2630;border-color:#21dff5}.ico{width:39px;height:39px;margin:0 auto 9px;border-radius:50%;display:grid;place-items:center;background:#0d2027;color:#5cecff;font-size:20px;font-weight:600}.yt{color:#ff5478}.wiki{font-family:Georgia,serif;font-size:26px}.google{font-weight:800;color:#4beaff}.git{font-size:22px}.link strong{display:block;font-size:11px;font-weight:500;white-space:nowrap}.link small{display:block;margin-top:5px;font-size:9px;color:#7a9da8;white-space:nowrap}.suggestions{border:1px solid #123640;background:linear-gradient(160deg,#091820,#050d12);border-radius:14px;padding:0 13px;box-shadow:0 0 18px rgba(0,210,240,.035)}.suggest{display:flex;align-items:center;gap:12px;padding:13px 1px;text-decoration:none;color:#f0ebf7;border-bottom:1px solid #112a31}.suggest:last-child{border-bottom:0}.sico{width:34px;height:34px;flex-shrink:0;display:grid;place-items:center;border-radius:50%;background:#0d2027;color:#4beaff;font-size:17px}.slabel{flex:1;font-size:13px}.slabel small{display:block;margin-top:4px;font-size:10px;color:#7a9da8}.arrow{font-size:18px;color:#4beaff}.foot{margin-top:28px;padding-top:15px;border-top:1px solid #10313a;text-align:center;color:#547781;font-size:10px;letter-spacing:1.2px}@media(min-width:500px){.wrap{padding-top:35px}.welcome{margin-top:42px}.link{padding:17px 4px 15px}}\n</style></head><body><main class='wrap'><header class='top'><div class='mark'>✦</div><div><div class='brand'>Astral<span>.</span></div><div class='subtitle'>Navegador web</div></div><div class='status'>● Privado y rápido</div><button class='mini dots' onclick='if(window.AstralNative){AstralNative.openMenu()}' aria-label='Menú del navegador'>⋮</button></header><section class='welcome'><div class='wordmark'>Astral<span> ✦</span></div><p>¿Qué quieres explorar hoy?</p></section><form class='search' action='https://duckduckgo.com/'><span class='glass'>⌕</span><input id='query' name='q' placeholder='Buscar o escribir una dirección' autocomplete='off' autocapitalize='off'><button type='submit' aria-label='Buscar'>↗</button></form><section class='section'><div class='heading'><h2>Accesos rápidos</h2><button class='edit' onclick='editLinks()'>Editar</button></div><div class='links' id='links'><a class='link' href='https://www.youtube.com'><span class='ico yt'>▶</span><strong>YouTube</strong><small>Vídeos</small></a><a class='link' href='https://www.wikipedia.org'><span class='ico wiki'>W</span><strong>Wikipedia</strong><small>Enciclopedia</small></a><a class='link' href='https://www.google.com'><span class='ico google'>G</span><strong>Google</strong><small>Buscador</small></a><a class='link' href='https://github.com'><span class='ico git'>◈</span><strong>GitHub</strong><small>Proyectos</small></a></div></section><section class='section'><div class='heading'><h2>Para descubrir</h2></div><div class='suggestions'><a class='suggest' href='https://open.spotify.com'><span class='sico'>♫</span><span class='slabel'>Spotify<small>open.spotify.com</small></span><span class='arrow'>↗</span></a><a class='suggest' href='https://chatgpt.com'><span class='sico'>✧</span><span class='slabel'>ChatGPT<small>chatgpt.com</small></span><span class='arrow'>↗</span></a><a class='suggest' href='https://www.netflix.com'><span class='sico'>N</span><span class='slabel'>Netflix<small>netflix.com</small></span><span class='arrow'>↗</span></a><a class='suggest' href='https://discord.com'><span class='sico'>◉</span><span class='slabel'>Discord<small>discord.com</small></span><span class='arrow'>↗</span></a></div></section><footer class='foot'>ASTRAL BROWSER · TU NAVEGADOR, A TU MANERA</footer></main><script>function editLinks(){var n=prompt('¿Qué acceso quieres editar? Escribe 1 YouTube, 2 Wikipedia, 3 Google o 4 GitHub.');var i=parseInt(n,10)-1;var a=document.querySelectorAll('#links a');if(!Number.isInteger(i)||i<0||i>=a.length)return;var name=prompt('Nombre:',a[i].querySelector('strong').textContent);if(!name)return;var url=prompt('Dirección web:',a[i].href);if(!url)return;if(!/^https?:\\/\\//i.test(url))url='https://'+url;a[i].href=url;a[i].querySelector('strong').textContent=name;a[i].querySelector('small').textContent=new URL(url).hostname;}</script></body></html>";
        int themeIndex=getSharedPreferences("astral_settings",MODE_PRIVATE).getInt("theme_index",0);
        String[][] themePalette={{"#4beaff","#27dff5","#123c48","#063c4a","#0d2027","#146477"},{"#c084fc","#a78bfa","#352743","#34224a","#211a2b","#6f4c91"},{"#4ade80","#22c55e","#16452c","#0d3924","#0b281b","#22834b"},{"#fb7185","#f43f5e","#4a1c29","#471624","#2c111a","#9f2940"},{"#fbbf24","#f59e0b","#4a3510","#493007","#2a200c","#997017"},{"#7dd3fc","#38bdf8","#193a4a","#103447","#0b2534","#28799c"}};
        themeIndex=Math.max(0,Math.min(themePalette.length-1,themeIndex));
        String[] palette=themePalette[themeIndex];
        html=html.replace("#4beaff",palette[0]).replace("#27dff5",palette[1]).replace("#123c48",palette[2]).replace("#063c4a",palette[3]).replace("#0d2027",palette[4]).replace("#146477",palette[5]);
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
        TextView subtitle = settingsText("Toca una tarjeta para cambiar de pestaña", 12, Color.rgb(122,157,168));
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
            rowBg.setColor(i == activeTab ? Color.rgb(5,54,66) : Color.rgb(7,21,27));
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
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.92f), (int)(getResources().getDisplayMetrics().heightPixels * 0.78f));
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

    private void destroySession(){
        for(WebView w:tabs){w.clearCache(true);w.clearHistory();}
        CookieManager.getInstance().removeAllCookies(null); CookieManager.getInstance().flush();
        showHome(current()); Toast.makeText(this,"Rastro borrado: cookies y caché eliminadas",Toast.LENGTH_SHORT).show();
    }
    private void hideKeyboard(){View v=getCurrentFocus(); if(v!=null){InputMethodManager im=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);if(im!=null)im.hideSoftInputFromWindow(v.getWindowToken(),0);}}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onBackPressed(){if(current().canGoBack())current().goBack();else if(!isHome(current()))showHome(current());else if(tabs.size()>1)closeTab(activeTab);else super.onBackPressed();}
}