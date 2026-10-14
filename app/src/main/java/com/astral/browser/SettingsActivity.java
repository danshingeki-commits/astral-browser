package com.astral.browser;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.webkit.CookieManager;
import androidx.webkit.ProxyController;
import androidx.webkit.ProxyConfig;

public class SettingsActivity extends Activity {
    private final int bg=Color.rgb(11,8,19), panel=Color.rgb(24,18,37), purple=Color.rgb(192,132,252), cyan=Color.rgb(34,211,238), muted=Color.rgb(169,154,190);
    private SharedPreferences prefs;
    private LinearLayout root, content, nav;
    private String active="General";
    private final String[] sections={"General","Diseño","Buscadores","Privacidad y seguridad","Multimedia","Pestañas y grupos","Avanzado"};
    @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("astral_settings",MODE_PRIVATE);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg); buildShell();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private boolean showingDetail=false;
    private void buildShell(){
        root=new LinearLayout(this);
        root.setOrientation(1);
        root.setBackgroundColor(bg);
        setContentView(root);
        showCategories(false);
    }

    private LinearLayout makeHeader(String subtitle, boolean detail){
        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(18),dp(12),dp(18),dp(12));
        top.setBackgroundColor(bg);
        if(detail){
            TextView back=text("‹",36,purple,true);
            back.setGravity(Gravity.CENTER);
            top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(48)));
            back.setOnClickListener(v->showCategories(true));
        }
        LinearLayout titleBox=new LinearLayout(this);
        titleBox.setOrientation(1);
        titleBox.addView(text("ASTRAL",21,Color.WHITE,true));
        titleBox.addView(text(subtitle,13,cyan,false));
        top.addView(titleBox,new LinearLayout.LayoutParams(0,-2,1));
        TextView done=text("LISTO",12,cyan,true);
        done.setPadding(dp(10),dp(12),0,dp(12));
        top.addView(done);
        done.setOnClickListener(v->finish());
        return top;
    }

    private void showCategories(boolean animate){
        showingDetail=false;
        LinearLayout screen=new LinearLayout(this);
        screen.setOrientation(1);
        screen.setBackgroundColor(bg);
        screen.addView(makeHeader("Configuración",false));
        TextView intro=text("TU ASTRAL, A TU MANERA",11,muted,true);
        intro.setPadding(dp(22),dp(18),dp(22),dp(10));
        screen.addView(intro);
        EditText search=new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(muted);
        search.setHint("⌕  Buscar en configuración");
        search.setPadding(dp(15),0,dp(15),0);
        search.setBackground(shape(panel,14));
        LinearLayout.LayoutParams searchParams=new LinearLayout.LayoutParams(-1,dp(48));
        searchParams.setMargins(dp(14),dp(4),dp(14),dp(8));
        screen.addView(search,searchParams);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout list=new LinearLayout(this);
        list.setOrientation(1);
        list.setPadding(dp(14),dp(4),dp(14),dp(24));
        scroll.addView(list);
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        String[] icons={"◇","✧","⌕","⬡","♫","▣","⚙"};
        String[] descriptions={
            "Inicio y datos de navegación",
            "Tema y apariencia del navegador",
            "Elige tu buscador predeterminado",
            "Cookies y protección de datos",
            "Reproducción y permisos de sitios",
            "Organización de tus pestañas",
            "Proxy y opciones avanzadas"
        };
        java.util.ArrayList<View> categoryCards=new java.util.ArrayList<>();
        for(int i=0;i<sections.length;i++){
            final String section=sections[i];
            LinearLayout item=new LinearLayout(this);
            item.setOrientation(0);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(16),dp(13),dp(14),dp(13));
            item.setBackground(shape(panel,16));
            TextView symbol=text(icons[i],24,purple,true);
            symbol.setGravity(Gravity.CENTER);
            item.addView(symbol,new LinearLayout.LayoutParams(dp(42),dp(44)));
            LinearLayout labels=new LinearLayout(this);
            labels.setOrientation(1);
            labels.setPadding(dp(10),0,dp(4),0);
            labels.addView(text(section,15,Color.WHITE,true));
            TextView desc=text(descriptions[i],11,muted,false);
            desc.setPadding(0,dp(4),0,0);
            labels.addView(desc);
            item.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            item.addView(text("›",27,cyan,false));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
            lp.setMargins(0,dp(5),0,dp(5));
            list.addView(item,lp);
            categoryCards.add(item);
            item.setOnClickListener(v->showSection(section));
            item.setAlpha(animate?0f:1f);
            item.setTranslationY(animate?dp(10):0);
            if(animate)item.animate().alpha(1f).translationY(0).setStartDelay(i*35L).setDuration(220).start();
        }
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                String query=s.toString().trim().toLowerCase(java.util.Locale.ROOT);
                for(int i=0;i<categoryCards.size();i++){
                    String searchable=(sections[i]+" "+descriptions[i]).toLowerCase(java.util.Locale.ROOT);
                    categoryCards.get(i).setVisibility(searchable.contains(query)?View.VISIBLE:View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s){}
        });
        transitionTo(screen,animate,false);
    }

    private void transitionTo(View next, boolean animate, boolean forward){
        if(root.getChildCount()==0 || !animate){
            root.removeAllViews();
            root.addView(next,new LinearLayout.LayoutParams(-1,-1));
            next.setAlpha(1f);
            next.setTranslationX(0);
            return;
        }
        View old=root.getChildAt(0);
        float distance=dp(44)*(forward?-1:1);
        old.animate().cancel();
        old.animate().alpha(0f).translationX(distance).setDuration(130).withEndAction(()->{
            root.removeAllViews();
            next.setAlpha(0f);
            next.setTranslationX(-distance);
            root.addView(next,new LinearLayout.LayoutParams(-1,-1));
            next.animate().alpha(1f).translationX(0).setDuration(220).start();
        }).start();
    }
    private void heading(String s,String sub){content.addView(text(s,23,Color.WHITE,true));TextView t=text(sub,13,muted,false);t.setPadding(0,dp(6),0,dp(16));content.addView(t);}
    private void card(String label,String description,View control){LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(14),dp(12),dp(14),dp(12));box.setBackground(shape(panel,16));TextView a=text(label,15,Color.WHITE,true);box.addView(a);if(description!=null&&!description.isEmpty()){TextView d=text(description,12,muted,false);d.setPadding(0,dp(5),0,dp(7));box.addView(d);}if(control!=null){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(4);box.addView(control,p);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));content.addView(box,lp);}
    private void showSection(String section){
        active=section;
        showingDetail=true;
        LinearLayout screen=new LinearLayout(this);
        screen.setOrientation(1);
        screen.setBackgroundColor(bg);
        screen.addView(makeHeader(section,true));
        ScrollView bodyScroll=new ScrollView(this);
        bodyScroll.setFillViewport(false);
        content=new LinearLayout(this);
        content.setOrientation(1);
        content.setPadding(dp(18),dp(18),dp(18),dp(28));
        bodyScroll.addView(content);
        screen.addView(bodyScroll,new LinearLayout.LayoutParams(-1,0,1));
        heading(section,"Personaliza tu experiencia en Astral Browser");
        switch(section){
        case "General":
            card("Página de inicio","Al abrir una pestaña, Astral muestra su página de inicio.",text("Página Astral · activa",14,cyan,true));
            card("Borrar datos de navegación","Elimina cookies, caché e historial de las pestañas.",button("Borrar datos",()->new android.app.AlertDialog.Builder(this).setTitle("Borrar datos").setMessage("Se eliminarán cookies, caché e historial. Puede cerrar sesiones web.").setNegativeButton("Cancelar",null).setPositiveButton("Borrar",(d,w)->{android.webkit.WebStorage.getInstance().deleteAllData();CookieManager.getInstance().removeAllCookies(null);CookieManager.getInstance().flush();Toast.makeText(this,"Datos de navegación borrados",Toast.LENGTH_SHORT).show();}).show()));
            break;
        case "Diseño":
            card("Tema","Elige el aspecto de Astral.",text("Oscuro · Neón morado y cian",14,cyan,true));
            card("Barra de herramientas","La barra de navegación mantiene el acceso a inicio, pestañas y ajustes.",null);
            break;
        case "Buscadores":
            Spinner engine=new Spinner(this);
            String[] engines={"DuckDuckGo · Privacidad","Google","Bing"};
            engine.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,engines));
            String saved=prefs.getString("search_engine","duckduckgo");
            engine.setSelection(saved.equals("google")?1:saved.equals("bing")?2:0);
            card("Motor de búsqueda predeterminado","Se utiliza cuando escribes palabras en la barra de direcciones.",engine);
            card("Guardar buscador","Aplica la selección al próximo uso de la barra.",button("Guardar",()->{String e=engine.getSelectedItemPosition()==1?"google":engine.getSelectedItemPosition()==2?"bing":"duckduckgo";prefs.edit().putString("search_engine",e).apply();Toast.makeText(this,"Buscador guardado",Toast.LENGTH_SHORT).show();}));
            break;
        case "Privacidad y seguridad":
            CheckBox cookies=new CheckBox(this);
            cookies.setText("Permitir cookies de terceros");
            cookies.setTextColor(Color.WHITE);
            cookies.setButtonTintList(android.content.res.ColorStateList.valueOf(purple));
            cookies.setChecked(prefs.getBoolean("third_party_cookies",false));
            card("Cookies","Desactivarlas puede limitar el seguimiento entre sitios, aunque algunas páginas podrían funcionar distinto.",cookies);
            card("Guardar privacidad","Aplica la preferencia de cookies a las pestañas abiertas.",button("Guardar",()->{boolean value=cookies.isChecked();prefs.edit().putBoolean("third_party_cookies",value).apply();Toast.makeText(this,"Preferencia guardada para nuevas pestañas",Toast.LENGTH_SHORT).show();}));
            card("DNS privado","Esta opción se configura en los ajustes de Android y afecta al dispositivo.",button("Abrir ajustes de red",()->{try{startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));}catch(Exception e){Toast.makeText(this,"Abre Ajustes > Red e Internet",Toast.LENGTH_LONG).show();}}));
            break;
        case "Multimedia":
            card("Reproducción web","Los controles de audio y vídeo dependen de cada página.",text("Administrados por el sitio web",14,cyan,true));
            card("Permisos del sitio","Gestiona permisos desde los ajustes de Android si una página no funciona como esperas.",button("Abrir ajustes de la aplicación",()->{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()));startActivity(i);}));
            break;
        case "Pestañas y grupos":
            card("Pestañas abiertas","Crea y cambia de pestaña desde la barra superior.",text("Pestañas independientes",14,cyan,true));
            card("Cerrar pestaña","Usa la × de cada pestaña para cerrarla.",null);
            break;
        case "Avanzado":
            EditText proxy=new EditText(this);
            proxy.setSingleLine(true);proxy.setTextColor(Color.WHITE);proxy.setHintTextColor(muted);proxy.setHint("host:puerto");proxy.setText(prefs.getString("proxy",""));
            card("Servidor proxy","Solo configura un proxy de un proveedor de confianza.",proxy);
            CheckBox enabled=new CheckBox(this);enabled.setText("Activar proxy para Astral");enabled.setTextColor(Color.WHITE);enabled.setButtonTintList(android.content.res.ColorStateList.valueOf(cyan));enabled.setChecked(prefs.getBoolean("proxy_enabled",false));
            card("Proxy","El soporte depende de la versión de Android System WebView.",enabled);
            card("Guardar proxy","Revisa host y puerto antes de guardar.",button("Guardar proxy",()->saveProxy(proxy.getText().toString().trim(),enabled.isChecked())));
            card("Desactivar proxy","Quita la configuración de proxy de WebView.",button("Desactivar",()->{prefs.edit().putBoolean("proxy_enabled",false).apply();try{ProxyController.getInstance().clearProxyOverride(Runnable::run,()->runOnUiThread(()->Toast.makeText(this,"Proxy desactivado",Toast.LENGTH_SHORT).show()));}catch(Exception e){Toast.makeText(this,"No se pudo desactivar el proxy",Toast.LENGTH_SHORT).show();}}));
            break;
        }
        transitionTo(screen,true,true);
    }

    @Override public void onBackPressed(){
        if(showingDetail) showCategories(true);
        else super.onBackPressed();
    }
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(91,45,130)));b.setOnClickListener(v->action.run());return b;}
    private void saveProxy(String value,boolean enabled){prefs.edit().putString("proxy",value).putBoolean("proxy_enabled",enabled).apply();if(!enabled){Toast.makeText(this,"Proxy guardado como desactivado",Toast.LENGTH_SHORT).show();return;}try{String[] p=value.split(":",2);int port=Integer.parseInt(p[1]);if(p[0].trim().isEmpty()||port<1||port>65535)throw new Exception();ProxyConfig c=new ProxyConfig.Builder().addProxyRule(p[0].trim()+":"+port).build();ProxyController.getInstance().setProxyOverride(c,Runnable::run,()->runOnUiThread(()->Toast.makeText(this,"Proxy configurado",Toast.LENGTH_SHORT).show()));}catch(Exception e){Toast.makeText(this,"Formato inválido. Usa host:puerto",Toast.LENGTH_LONG).show();}}
}