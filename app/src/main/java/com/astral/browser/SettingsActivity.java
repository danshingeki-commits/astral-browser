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
import org.json.JSONArray;
import org.json.JSONObject;

public class SettingsActivity extends Activity {
    private final int bg=Color.rgb(3,7,11), panel=Color.rgb(7,21,27), purple=Color.rgb(75,234,255), cyan=Color.rgb(39,223,245), muted=Color.rgb(122,157,168);
    private SharedPreferences prefs;
    private LinearLayout root, content, nav;
    private String active="General";
    private final String[] sections={"General","Diseño","Buscadores","Privacidad y seguridad","Multimedia","Pestañas y grupos","Accesibilidad","Avanzado","Red y proxy"};
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
        titleBox.addView(text("ASTRAL BROWSER",20,Color.WHITE,true));
        titleBox.addView(text("NEON EDITION  •  "+subtitle.toUpperCase(java.util.Locale.ROOT),11,cyan,true));
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
        LinearLayout hero=new LinearLayout(this);
        hero.setOrientation(1);
        hero.setPadding(dp(18),dp(18),dp(18),dp(18));
        GradientDrawable heroBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(7,45,56),Color.rgb(9,19,29),Color.rgb(10,12,20)});
        heroBg.setCornerRadius(dp(22)); heroBg.setStroke(dp(1),Color.rgb(25,113,132)); hero.setBackground(heroBg);
        TextView intro=text("CENTRO DE CONTROL  /  ASTRAL OS",11,cyan,true);
        hero.addView(intro);
        TextView heroTitle=text("Tu navegador.\nTus reglas.",27,Color.WHITE,true);
        heroTitle.setPadding(0,dp(10),0,dp(5)); hero.addView(heroTitle);
        TextView heroDesc=text("Personaliza apariencia, privacidad y conexión desde un solo lugar.",13,muted,false);
        heroDesc.setPadding(0,0,0,dp(12)); hero.addView(heroDesc);
        LinearLayout metrics=new LinearLayout(this); metrics.setOrientation(0);
        metrics.addView(metric("TEMA","NEÓN"),new LinearLayout.LayoutParams(0,dp(48),1));
        metrics.addView(metric("MOTOR",prefs.getString("search_engine","duckduckgo").toUpperCase(java.util.Locale.ROOT)),new LinearLayout.LayoutParams(0,dp(48),1));
        metrics.addView(metric("PROXY",prefs.getBoolean("proxy_enabled",false)?"ACTIVO":"LISTO"),new LinearLayout.LayoutParams(0,dp(48),1));
        hero.addView(metrics);
        LinearLayout.LayoutParams heroLp=new LinearLayout.LayoutParams(-1,-2); heroLp.setMargins(dp(14),dp(12),dp(14),dp(12)); screen.addView(hero,heroLp);
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
        String[] icons={"⌂","✧","⌕","⬡","♫","▣","Aa","◈","⌁"};
        String[] descriptions={
            "Inicio y datos de navegación",
            "Tema y apariencia del navegador",
            "Elige tu buscador predeterminado",
            "Cookies y protección de datos",
            "Reproducción y permisos de sitios",
            "Organización de tus pestañas",
            "Zoom y lectura de páginas",
            "Opciones técnicas y perfiles proxy",
            "Perfiles, servidor y estado de conexión"
        };
        java.util.ArrayList<View> categoryCards=new java.util.ArrayList<>();
        for(int i=0;i<sections.length;i++){
            final String section=sections[i];
            LinearLayout item=new LinearLayout(this);
            item.setOrientation(0);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(16),dp(15),dp(14),dp(15));
            item.setBackground(shape(Color.rgb(7,21,27),14)); item.setElevation(dp(1));
            TextView symbol=text(icons[i],25,(i==7||i==8?cyan:purple),true);
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
            LinearLayout right=new LinearLayout(this); right.setOrientation(1); right.setGravity(Gravity.CENTER);
            TextView badge=text(i==8?(prefs.getBoolean("proxy_enabled",false)?"ON":"HTTP"):(i==1?"UI":i==3?"SAFE":"OPEN"),9,cyan,true);
            badge.setGravity(Gravity.CENTER); badge.setPadding(dp(7),dp(4),dp(7),dp(4)); badge.setBackground(shape(Color.rgb(8,39,48),8));
            right.addView(badge); TextView arrow=text("↗",18,cyan,true); arrow.setGravity(Gravity.CENTER); right.addView(arrow);
            item.addView(right);
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
    private View metric(String label,String value){
        LinearLayout m=new LinearLayout(this); m.setOrientation(1); m.setPadding(dp(9),dp(7),dp(6),dp(5));
        m.setBackground(shape(Color.rgb(4,28,36),9));
        m.addView(text(label,9,muted,true)); TextView v=text(value.length()>13?value.substring(0,13):value,11,cyan,true); v.setPadding(0,dp(3),0,0); m.addView(v); return m;
    }
    private void heading(String s,String sub){
        TextView eyebrow=text("CONFIGURACIÓN / "+s.toUpperCase(java.util.Locale.ROOT),10,cyan,true);
        eyebrow.setLetterSpacing(.12f); eyebrow.setPadding(0,0,0,dp(8)); content.addView(eyebrow);
        content.addView(text(s,26,Color.WHITE,true));
        TextView t=text(sub,13,muted,false);t.setPadding(0,dp(6),0,dp(18));content.addView(t);
    }
    private void card(String label,String description,View control){LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(14),dp(12),dp(14),dp(12));GradientDrawable cardBg=shape(panel,14); cardBg.setStroke(dp(1),Color.rgb(17,56,66)); box.setBackground(cardBg);
        View accent=new View(this); accent.setBackground(shape(cyan,2)); box.addView(accent,new LinearLayout.LayoutParams(dp(28),dp(3)));
        TextView a=text(label,15,Color.WHITE,true); a.setPadding(0,dp(9),0,0); box.addView(a);if(description!=null&&!description.isEmpty()){TextView d=text(description,12,muted,false);d.setPadding(0,dp(5),0,dp(7));box.addView(d);}if(control!=null){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(4);box.addView(control,p);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(10));content.addView(box,lp);}
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
        case "Red y proxy":
            card("ESTADO DE RED","Configuración guardada en este dispositivo.",text(prefs.getBoolean("proxy_enabled",false)?"● Proxy activado para WebView":"○ Conexión directa / proxy apagado",14,cyan,true));
            card("PERFILES PROXY","Guarda varios servidores y selecciona uno en las opciones avanzadas.",button("Administrar proxy",()->showSection("Avanzado")));
            card("LÍMITE DE CONEXIÓN","Astral aplica proxy HTTP al tráfico de WebView. No oculta automáticamente la IP de otras aplicaciones ni sustituye una VPN.",null);
            card("DNS DEL DISPOSITIVO","Abre las opciones de red de Android para administrar DNS privado.",button("Abrir ajustes de red",()->{try{startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));}catch(Exception e){Toast.makeText(this,"Abre los ajustes de red de Android",Toast.LENGTH_LONG).show();}}));
            break;
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
            CheckBox autoplay=new CheckBox(this);autoplay.setText("Permitir reproducción automática de audio y vídeo");autoplay.setTextColor(Color.WHITE);autoplay.setButtonTintList(android.content.res.ColorStateList.valueOf(cyan));autoplay.setChecked(prefs.getBoolean("autoplay_media",false));
            card("Reproducción automática","Desactivada por defecto para evitar que los sitios reproduzcan contenido sin tocarlo.",autoplay);
            card("Guardar preferencia","Se aplica a las pestañas que abras después de guardar.",button("Guardar multimedia",()->{prefs.edit().putBoolean("autoplay_media",autoplay.isChecked()).apply();Toast.makeText(this,"Preferencia multimedia guardada",Toast.LENGTH_SHORT).show();}));
            card("Permisos del sitio","Gestiona permisos desde los ajustes de Android si una página no funciona como esperas.",button("Abrir ajustes de la aplicación",()->{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()));startActivity(i);}));
            break;
        case "Pestañas y grupos":
            int openTabs=getIntent().getIntExtra("open_tabs_count",1);
            card("Pestañas abiertas","Recuento actual del navegador.",text(openTabs+" pestaña(s) abiertas",18,cyan,true));
            card("Administrar pestañas","Vuelve al navegador y toca el botón ▢ de la barra inferior para ver, cambiar o cerrar pestañas.",button("Volver al navegador",()->finish()));
            card("Cerrar pestaña","Usa la × de cada tarjeta en el menú de pestañas. Siempre queda al menos una pestaña disponible.",null);
            break;
        case "Accesibilidad":
            Spinner zoom=new Spinner(this);
            String[] zoomOptions={"80%","90%","100%","110%","125%","150%"};
            zoom.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,zoomOptions));
            int currentZoom=prefs.getInt("text_zoom",100);int selectedZoom=2;
            for(int i=0;i<zoomOptions.length;i++){if(Integer.parseInt(zoomOptions[i].replace("%",""))==currentZoom)selectedZoom=i;}
            zoom.setSelection(selectedZoom);
            card("Tamaño del texto","Ajusta la escala del texto en páginas web nuevas.",zoom);
            card("Guardar accesibilidad","El cambio se aplica al abrir nuevas pestañas.",button("Guardar zoom",()->{int[] values={80,90,100,110,125,150};prefs.edit().putInt("text_zoom",values[zoom.getSelectedItemPosition()]).apply();Toast.makeText(this,"Zoom guardado",Toast.LENGTH_SHORT).show();}));
            break;
        case "Avanzado":
            CheckBox javascript=new CheckBox(this);javascript.setText("Activar JavaScript");javascript.setTextColor(Color.WHITE);javascript.setButtonTintList(android.content.res.ColorStateList.valueOf(purple));javascript.setChecked(prefs.getBoolean("javascript_enabled",true));
            card("JavaScript","Algunos sitios dejan de funcionar si lo desactivas. Se aplica a pestañas nuevas.",javascript);
            CheckBox popups=new CheckBox(this);popups.setText("Bloquear ventanas emergentes automáticas");popups.setTextColor(Color.WHITE);popups.setButtonTintList(android.content.res.ColorStateList.valueOf(cyan));popups.setChecked(prefs.getBoolean("block_popups",true));
            card("Ventanas emergentes","Reduce las ventanas abiertas automáticamente por páginas web.",popups);
            card("Guardar seguridad web","Guarda JavaScript y el bloqueo de ventanas emergentes para pestañas nuevas.",button("Guardar preferencias",()->{prefs.edit().putBoolean("javascript_enabled",javascript.isChecked()).putBoolean("block_popups",popups.isChecked()).apply();Toast.makeText(this,"Preferencias web guardadas",Toast.LENGTH_SHORT).show();}));
            Spinner profiles=new Spinner(this);
            refreshProxyProfiles(profiles);
            card("Perfiles guardados","Selecciona un perfil para cargar su servidor.",profiles);
            EditText profileName=new EditText(this);
            profileName.setSingleLine(true);profileName.setTextColor(Color.WHITE);profileName.setHintTextColor(muted);profileName.setHint("Nombre del perfil");
            card("Nombre del perfil","Ejemplo: Casa, Datos móviles o Trabajo.",profileName);
            EditText proxy=new EditText(this);
            proxy.setSingleLine(true);proxy.setTextColor(Color.WHITE);proxy.setHintTextColor(muted);proxy.setHint("host:puerto");proxy.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            proxy.setText(prefs.getString("proxy",""));
            card("Servidor proxy HTTP","Formato host:puerto (por ejemplo, proxy.ejemplo.net:8080). Este campo no admite SOCKS ni autenticación de usuario/contraseña.",proxy);
            profiles.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
                @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
                    try{JSONArray a=new JSONArray(prefs.getString("proxy_profiles","[]"));if(position>=0&&position<a.length()){JSONObject p=a.getJSONObject(position);profileName.setText(p.optString("name",""));proxy.setText(p.optString("server",""));}}
                    catch(Exception ignored){}
                }
            });
            card("Guardar perfil","Guarda o actualiza el perfil con ese nombre.",button("Guardar perfil",()->{
                String name=profileName.getText().toString().trim(),server=proxy.getText().toString().trim();
                if(name.isEmpty()||!validProxyAddress(server)){Toast.makeText(this,"Escribe un nombre y un host:puerto válido",Toast.LENGTH_LONG).show();return;}
                try{JSONArray a=new JSONArray(prefs.getString("proxy_profiles","[]"));boolean found=false;
                    for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);if(name.equalsIgnoreCase(p.optString("name",""))){p.put("server",server);found=true;break;}}
                    if(!found){JSONObject p=new JSONObject();p.put("name",name);p.put("server",server);a.put(p);}
                    prefs.edit().putString("proxy_profiles",a.toString()).putString("proxy",server).apply();
                    refreshProxyProfiles(profiles);Toast.makeText(this,"Perfil guardado",Toast.LENGTH_SHORT).show();
                }catch(Exception e){Toast.makeText(this,"No se pudo guardar el perfil",Toast.LENGTH_SHORT).show();}
            }));
            card("Eliminar perfil","Elimina el perfil seleccionado de este dispositivo.",button("Eliminar perfil",()->{
                int pos=profiles.getSelectedItemPosition();
                try{JSONArray a=new JSONArray(prefs.getString("proxy_profiles","[]"));if(pos<0||pos>=a.length()){Toast.makeText(this,"Selecciona un perfil",Toast.LENGTH_SHORT).show();return;}
                    a.remove(pos);prefs.edit().putString("proxy_profiles",a.toString()).apply();refreshProxyProfiles(profiles);
                    profileName.setText("");proxy.setText("");Toast.makeText(this,"Perfil eliminado",Toast.LENGTH_SHORT).show();
                }catch(Exception e){Toast.makeText(this,"No se pudo eliminar el perfil",Toast.LENGTH_SHORT).show();}
            }));
            CheckBox enabled=new CheckBox(this);enabled.setText("Activar proxy para Astral");enabled.setTextColor(Color.WHITE);enabled.setButtonTintList(android.content.res.ColorStateList.valueOf(cyan));enabled.setChecked(prefs.getBoolean("proxy_enabled",false));
            card("Conexión proxy · HTTP","Se aplica al contenido web de Astral mediante WebView. No es una VPN y no enruta otras aplicaciones del teléfono.",enabled);
            card("Aplicar proxy","Valida el host y puerto antes de aplicar la configuración.",button("Aplicar perfil",()->saveProxy(proxy.getText().toString().trim(),enabled.isChecked())));
            card("Desactivar proxy","Quita la configuración de proxy de WebView.",button("Desactivar",()->{prefs.edit().putBoolean("proxy_enabled",false).apply();try{ProxyController.getInstance().clearProxyOverride(Runnable::run,()->runOnUiThread(()->Toast.makeText(this,"Proxy desactivado",Toast.LENGTH_SHORT).show()));}catch(Exception e){Toast.makeText(this,"No se pudo desactivar el proxy",Toast.LENGTH_SHORT).show();}}));
            break;
        }
        transitionTo(screen,true,true);
    }

    @Override public void onBackPressed(){
        if(showingDetail) showCategories(true);
        else super.onBackPressed();
    }
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(5,110,132)));b.setOnClickListener(v->action.run());return b;}
    private boolean validProxyAddress(String value){
        try{String[] p=value.trim().split(":",2);if(p.length!=2||p[0].trim().isEmpty())return false;int port=Integer.parseInt(p[1].trim());return port>=1&&port<=65535&&!p[0].contains(" ");}
        catch(Exception e){return false;}
    }
    private void refreshProxyProfiles(Spinner spinner){
        java.util.ArrayList<String> names=new java.util.ArrayList<>();
        try{JSONArray a=new JSONArray(prefs.getString("proxy_profiles","[]"));for(int i=0;i<a.length();i++)names.add(a.getJSONObject(i).optString("name","Perfil "+(i+1)));}
        catch(Exception ignored){}
        if(names.isEmpty())names.add("Sin perfiles guardados");
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    private void saveProxy(String value,boolean enabled){prefs.edit().putString("proxy",value).putBoolean("proxy_enabled",enabled).apply();if(!enabled){Toast.makeText(this,"Proxy guardado como desactivado",Toast.LENGTH_SHORT).show();return;}try{if(!validProxyAddress(value))throw new Exception();String[] p=value.split(":",2);int port=Integer.parseInt(p[1].trim());ProxyConfig c=new ProxyConfig.Builder().addProxyRule(p[0].trim()+":"+port).build();ProxyController.getInstance().setProxyOverride(c,Runnable::run,()->runOnUiThread(()->Toast.makeText(this,"Proxy configurado",Toast.LENGTH_SHORT).show()));}catch(Exception e){Toast.makeText(this,"Formato inválido. Usa host:puerto",Toast.LENGTH_LONG).show();}}
}