package com.astral.browser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.URLEncoder;
import java.util.ArrayList;

public class IncognitoActivity extends AppCompatActivity {
    private final int bg=Color.rgb(11,8,19), panel=Color.rgb(24,18,37), purple=Color.rgb(192,132,252), cyan=Color.rgb(34,211,238);
    private final ArrayList<WebView> tabs=new ArrayList<>();
    private final ArrayList<String> titles=new ArrayList<>();
    private int active=0;
    private EditText address;
    private ProgressBar progress;
    private FrameLayout webHost;
    private LinearLayout tabStrip;
    private boolean exiting=false;

    @Override protected void onCreate(Bundle state) {
        if(Build.VERSION.SDK_INT>=28) WebView.setDataDirectorySuffix("astral_incognito");
        super.onCreate(state);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        CookieManager.getInstance().setAcceptCookie(false);
        buildUi();
        createTab(true);
    }

    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private TextView label(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Button button(String value){Button b=new Button(this);b.setText(value);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));b.setMinWidth(dp(0));b.setPadding(dp(4),0,dp(4),0);return b;}

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);setContentView(root);
        LinearLayout banner=new LinearLayout(this);banner.setGravity(Gravity.CENTER_VERTICAL);banner.setPadding(dp(14),dp(9),dp(14),dp(9));banner.setBackgroundColor(Color.rgb(24,14,36));
        TextView mark=label("◈",23,purple,true);banner.addView(mark);
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);names.setPadding(dp(10),0,0,0);
        names.addView(label("ASTRAL · INCOGNITO",14,Color.WHITE,true));
        names.addView(label("La sesión privada se elimina al salir",10,cyan,false));
        banner.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        Button exit=button("Salir");exit.setTextColor(cyan);exit.setOnClickListener(v->finish());banner.addView(exit);
        root.addView(banner,new LinearLayout.LayoutParams(-1,-2));
        HorizontalScrollView tabsScroll=new HorizontalScrollView(this);tabsScroll.setHorizontalScrollBarEnabled(false);
        tabStrip=new LinearLayout(this);tabStrip.setGravity(Gravity.CENTER_VERTICAL);tabStrip.setPadding(dp(4),dp(3),dp(4),dp(3));tabsScroll.addView(tabStrip);
        root.addView(tabsScroll,new LinearLayout.LayoutParams(-1,dp(46)));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(5),dp(4),dp(5),dp(4));bar.setBackgroundColor(Color.rgb(22,15,35));
        Button back=button("‹");Button forward=button("›");Button go=button("↵");
        address=new EditText(this);address.setSingleLine(true);address.setTextSize(13);address.setTextColor(Color.WHITE);address.setHintTextColor(Color.rgb(169,154,190));address.setHint("Buscar en privado o escribir URL");address.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);address.setImeOptions(EditorInfo.IME_ACTION_GO);address.setPadding(dp(10),0,dp(10),0);address.setBackground(shape(panel,12));
        bar.addView(back,new LinearLayout.LayoutParams(dp(37),dp(40)));bar.addView(forward,new LinearLayout.LayoutParams(dp(37),dp(40)));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(40),1);ap.setMargins(dp(4),0,dp(4),0);bar.addView(address,ap);bar.addView(go,new LinearLayout.LayoutParams(dp(42),dp(40)));
        root.addView(bar);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(false);progress.setMax(100);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(cyan));progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        webHost=new FrameLayout(this);root.addView(webHost,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);bottom.setPadding(dp(8),dp(4),dp(8),dp(4));bottom.setBackgroundColor(Color.rgb(16,11,26));
        Button home=button("⌂ Inicio");Button newTab=button("+ Pestaña");Button count=button("▢ 0");Button clear=button("✦ Borrar sesión");
        for(Button b:new Button[]{home,newTab,count,clear}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(43),1);p.setMargins(dp(3),0,dp(3),0);bottom.addView(b,p);}
        root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(52)));
        back.setOnClickListener(v->{if(current().canGoBack())current().goBack();});
        forward.setOnClickListener(v->{if(current().canGoForward())current().goForward();});
        go.setOnClickListener(v->navigate());
        address.setOnEditorActionListener((v,id,event)->{if(id==EditorInfo.IME_ACTION_GO||(event!=null&&event.getKeyCode()==KeyEvent.KEYCODE_ENTER)){navigate();return true;}return false;});
        home.setOnClickListener(v->showPrivateHome(current()));
        newTab.setOnClickListener(v->createTab(true));
        count.setOnClickListener(v->showTabPicker());
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Borrar sesión privada").setMessage("Se borrará el historial de esta sesión y se abrirá una pestaña nueva.").setNegativeButton("Cancelar",null).setPositiveButton("Borrar",(d,w)->clearSession()).show());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createTab(boolean show){
        WebView w=new WebView(this);w.setBackgroundColor(bg);
        WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(false);s.setCacheMode(WebSettings.LOAD_NO_CACHE);s.setSaveFormData(false);s.setSupportMultipleWindows(false);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setMediaPlaybackRequiresUserGesture(true);s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(false);CookieManager.getInstance().setAcceptThirdPartyCookies(w,false);
        w.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int value){if(view==current()){progress.setProgress(value);progress.setVisibility(value>=100?View.GONE:View.VISIBLE);}}
            @Override public void onReceivedTitle(WebView view,String title){int i=tabs.indexOf(view);if(i>=0){titles.set(i,title==null||title.isEmpty()?"Pestaña privada":title);renderTabs();}}
        });
        w.setWebViewClient(new WebViewClient(){
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon){if(view==current()&&address!=null)address.setText(url.startsWith("https://astral.local/")?"":url);}
            @Override public void onPageFinished(WebView view,String url){if(view==current()&&address!=null)address.setText(url.startsWith("https://astral.local/")?"":url);}
        });
        tabs.add(w);titles.add("Nueva pestaña");webHost.addView(w,new FrameLayout.LayoutParams(-1,-1));
        if(show){active=tabs.size()-1;switchTab(active);showPrivateHome(w);}else w.setVisibility(View.GONE);
        renderTabs();
    }
    private WebView current(){return tabs.get(active);}
    private void switchTab(int index){if(index<0||index>=tabs.size())return;active=index;for(int i=0;i<tabs.size();i++)tabs.get(i).setVisibility(i==active?View.VISIBLE:View.GONE);address.setText(current().getUrl()==null?"":(current().getUrl().startsWith("https://astral.local/")?"":current().getUrl()));renderTabs();}
    private void renderTabs(){
        tabStrip.removeAllViews();
        for(int i=0;i<tabs.size();i++){final int idx=i;Button b=button((i==active?"● ":"○ ")+(i+1)+"  ×");b.setTextSize(12);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(i==active?Color.rgb(66,37,86):panel));b.setOnClickListener(v->{if(idx<tabs.size())closeTab(idx);});b.setOnLongClickListener(v->{switchTab(idx);return true;});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(38));p.setMargins(dp(3),0,dp(3),0);tabStrip.addView(b,p);}
    }
    private void closeTab(int index){if(index<0||index>=tabs.size())return;if(tabs.size()==1){current().stopLoading();current().clearHistory();current().clearCache(true);showPrivateHome(current());titles.set(0,"Nueva pestaña");renderTabs();return;}WebView w=tabs.remove(index);titles.remove(index);webHost.removeView(w);w.stopLoading();w.clearHistory();w.clearCache(true);w.destroy();if(active>=tabs.size())active=tabs.size()-1;else if(index<active)active--;switchTab(active);}
    private void showPrivateHome(WebView w){String html="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'><style>body{margin:0;background:radial-gradient(ellipse at top,#382052,#0b0813 70%);color:#fff;font-family:Arial;text-align:center;padding:18vh 22px 0}h1{letter-spacing:3px}h1 span{color:#c084fc}.icon{font-size:60px;color:#c084fc}p{color:#b8a8cb;line-height:1.6;font-size:14px}.note{margin-top:35px;padding:16px;border:1px solid #473057;border-radius:15px;background:#191326;font-size:12px;color:#cbb9df}</style></head><body><div class='icon'>◈</div><h1>ASTRAL <span>PRIVADO</span></h1><p>Tu sesión privada está abierta.</p><div class='note'>Las cookies están desactivadas y el historial de esta sesión se limpia al salir. Las páginas que visites y tu proveedor de internet aún podrían ver tu actividad.</div></body></html>";w.loadDataWithBaseURL("https://astral.local/",html,"text/html","UTF-8",null);address.setText("");}
    private void navigate(){String input=address.getText().toString().trim();((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(address.getWindowToken(),0);if(input.isEmpty()){showPrivateHome(current());return;}String url;if(input.startsWith("http://")||input.startsWith("https://"))url=input;else if(input.contains(".")&&!input.contains(" "))url="https://"+input;else try{url="https://duckduckgo.com/?q="+URLEncoder.encode(input,"UTF-8");}catch(Exception e){url="https://duckduckgo.com/";}current().loadUrl(url);}
    private void showTabPicker(){String[] choices=new String[tabs.size()+1];for(int i=0;i<tabs.size();i++)choices[i]=(i==active?"● ":"")+"Pestaña "+(i+1)+" · "+titles.get(i);choices[tabs.size()]="+ Nueva pestaña";new AlertDialog.Builder(this).setTitle("Pestañas privadas ("+tabs.size()+")").setItems(choices,(d,which)->{if(which==tabs.size())createTab(true);else switchTab(which);}).show();}
    private void clearSession(){for(WebView w:tabs){w.stopLoading();w.clearHistory();w.clearCache(true);w.clearFormData();}CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();android.webkit.WebStorage.getInstance().deleteAllData();while(tabs.size()>1)closeTab(tabs.size()-1);showPrivateHome(current());Toast.makeText(this,"Sesión privada borrada",Toast.LENGTH_SHORT).show();}
    @Override public void onBackPressed(){if(current().canGoBack())current().goBack();else if(tabs.size()>1)closeTab(active);else finish();}
    @Override protected void onDestroy(){if(!exiting){exiting=true;for(WebView w:new ArrayList<>(tabs)){try{w.stopLoading();w.clearHistory();w.clearCache(true);w.clearFormData();w.destroy();}catch(Exception ignored){}}tabs.clear();titles.clear();CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();android.webkit.WebStorage.getInstance().deleteAllData();}super.onDestroy();}
}