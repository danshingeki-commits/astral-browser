package com.astral.browser;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Astral Assistant first-stage UI.
 * This version provides local guidance, Android speech input/output, and web search.
 * A real generative model must be connected through a secure backend in a later step.
 */
public class AssistantActivity extends Activity {
    private static final int SPEECH_REQUEST = 701;
    private LinearLayout messages;
    private EditText input;
    private TextToSpeech tts;
    private String pageUrl = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        pageUrl = getIntent().getStringExtra("page_url");
        if (pageUrl == null || pageUrl.isEmpty()) pageUrl = "No hay una página abierta";
        getWindow().setStatusBarColor(Color.rgb(7,10,22));
        getWindow().setNavigationBarColor(Color.rgb(7,10,22));
        tts = new TextToSpeech(this, status -> { if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("es","CO")); });
        buildUi();
        loadHistory();
        if (messages.getChildCount() == 0) {
            addMessage("Astral Assistant", "¡Oe, cole! Soy Astral Assistant ✦. Ya puedo guardar esta conversación en tu teléfono y responder en voz alta. Mi cerebro generativo se conectará en una siguiente etapa.", false);
            addMessage("Página actual", pageUrl, false);
        }
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(18));
        d.setStroke(dp(1), stroke); return d;
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(12),dp(16),dp(12));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(9,18,38),Color.rgb(20,10,39),Color.rgb(5,9,20)}));
        setContentView(root);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = text("‹",32,Color.WHITE); back.setGravity(Gravity.CENTER);
        header.addView(back,new LinearLayout.LayoutParams(dp(42),dp(48)));
        back.setOnClickListener(v -> finish());
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Astral Assistant ✦",21,Color.WHITE); title.setTypeface(null,1);
        TextView sub = text("TU COPILOTO DIGITAL",10,Color.rgb(105,229,255));
        titles.addView(title); titles.addView(sub);
        header.addView(titles,new LinearLayout.LayoutParams(0, -2,1));
        TextView web = text("⌕",25,Color.rgb(105,229,255)); web.setGravity(Gravity.CENTER);
        header.addView(web,new LinearLayout.LayoutParams(dp(42),dp(44)));
        web.setOnClickListener(v -> { if(input.getText().length()>0) searchWeb(input.getText().toString()); else searchWeb("Inteligencia artificial"); });
        root.addView(header);
        TextView intro = text("Pregunta lo que quieras o pide ayuda con la página abierta.",13,Color.rgb(170,188,216));
        intro.setPadding(dp(4),dp(12),dp(4),dp(16)); root.addView(intro);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(false);
        messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); messages.setPadding(0,0,0,dp(10));
        scroll.addView(messages);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout chips = new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL);
        String[] prompts = {"Resumir página","Buscar en Internet"};
        for(String p:prompts){ Button b = new Button(this); b.setText(p); b.setTextSize(11); b.setTextColor(Color.rgb(174,230,255)); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(25,35,65))); chips.addView(b,new LinearLayout.LayoutParams(0,dp(42),1)); b.setOnClickListener(v -> { if(p.startsWith("Resumir")) addMessage("Astral Assistant","La página actual es "+pageUrl+". La conexión al modelo que podrá leer y resumir su contenido se añadirá en la siguiente etapa.",false); else searchWeb("información sobre "+pageUrl); }); }
        root.addView(chips);
        LinearLayout composer = new LinearLayout(this); composer.setGravity(Gravity.CENTER_VERTICAL); composer.setPadding(dp(6),dp(5),dp(6),dp(5)); composer.setBackground(shape(Color.rgb(18,26,47),Color.rgb(51,82,124)));
        input = new EditText(this); input.setTextColor(Color.WHITE); input.setHintTextColor(Color.rgb(135,154,184)); input.setHint("Escribe a Astral..."); input.setTextSize(14); input.setSingleLine(true); input.setBackgroundColor(Color.TRANSPARENT); input.setImeOptions(EditorInfo.IME_ACTION_SEND);
        composer.addView(input,new LinearLayout.LayoutParams(0,dp(48),1));
        Button mic = new Button(this); mic.setText("🎙"); mic.setTextSize(16); mic.setTextColor(Color.rgb(105,229,255)); mic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(27,38,66))); composer.addView(mic,new LinearLayout.LayoutParams(dp(46),dp(46)));
        Button send = new Button(this); send.setText("➤"); send.setTextColor(Color.WHITE); send.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(18,153,190))); composer.addView(send,new LinearLayout.LayoutParams(dp(48),dp(46)));
        root.addView(composer);
        send.setOnClickListener(v -> sendMessage());
        input.setOnEditorActionListener((v, action, event) -> { if(action==EditorInfo.IME_ACTION_SEND){sendMessage();return true;} return false; });
        mic.setOnClickListener(v -> startVoice());
    }
    private void loadHistory() {
        try {
            JSONArray history = new JSONArray(getPreferences(MODE_PRIVATE).getString("chat_history", "[]"));
            for (int i = 0; i < history.length(); i++) {
                JSONObject item = history.getJSONObject(i);
                displayMessage(item.optString("who"), item.optString("body"), item.optBoolean("user"));
            }
        } catch (Exception ignored) { }
    }
    private void saveMessage(String who, String body, boolean user) {
        try {
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            JSONArray history = new JSONArray(prefs.getString("chat_history", "[]"));
            JSONObject item = new JSONObject();
            item.put("who", who); item.put("body", body); item.put("user", user);
            history.put(item);
            // Keep the latest 100 messages to limit local storage.
            JSONArray limited = new JSONArray();
            for (int i = Math.max(0, history.length() - 100); i < history.length(); i++) limited.put(history.get(i));
            prefs.edit().putString("chat_history", limited.toString()).apply();
        } catch (Exception ignored) { }
    }
    private void addMessage(String who, String body, boolean user) {
        displayMessage(who, body, user);
        saveMessage(who, body, user);
        if (!user && tts != null && !body.isEmpty()) tts.speak(body, TextToSpeech.QUEUE_FLUSH, null, "astral_reply");
    }
    private void displayMessage(String who, String body, boolean user) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(11),dp(14),dp(11));
        card.setBackground(shape(user?Color.rgb(16,69,94):Color.rgb(20,28,49),user?Color.rgb(35,178,214):Color.rgb(57,70,112)));
        TextView label=text(who,10,Color.rgb(105,229,255)); label.setTypeface(null,1); card.addView(label);
        TextView content=text(body,14,Color.rgb(239,244,255)); content.setPadding(0,dp(5),0,0); card.addView(content);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(user?dp(285):-1,-2); lp.gravity=user?Gravity.RIGHT:Gravity.LEFT; lp.bottomMargin=dp(10); messages.addView(card,lp);
    }
    private void sendMessage() {
        String q=input.getText().toString().trim(); if(q.isEmpty())return;
        input.setText(""); addMessage("Tú",q,true);
        String lower=q.toLowerCase(Locale.ROOT);
        if(lower.contains("resum")||lower.contains("página actual")) {
            addMessage("Astral Assistant","La página abierta es "+pageUrl+". Para leer y resumir su contenido necesito conectar un modelo de IA con acceso seguro al texto de la página. Esa integración será el siguiente paso.",false);
        } else if(lower.startsWith("buscar ") || lower.contains("en internet")) {
            searchWeb(q.replaceFirst("(?i)^buscar\\s*",""));
        } else {
            addMessage("Astral Assistant","Recibí tu mensaje. La interfaz, la entrada por voz y la búsqueda web ya están preparadas. Para generar respuestas inteligentes de verdad, el siguiente paso es conectar un modelo; entrenar uno desde cero requiere datos, servidores y mucha capacidad de cómputo.",false);
        }
    }
    private void searchWeb(String query) {
        try { Intent i=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://duckduckgo.com/?q="+java.net.URLEncoder.encode(query,"UTF-8"))); startActivity(i); }
        catch(Exception e){ addMessage("Astral Assistant","No pude abrir la búsqueda web en este momento.",false); }
    }
    private void startVoice() {
        try { Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-CO"); i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Habla con Astral Assistant"); startActivityForResult(i,SPEECH_REQUEST); }
        catch(Exception e){ addMessage("Astral Assistant","El reconocimiento de voz no está disponible en este dispositivo.",false); }
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==SPEECH_REQUEST && resultCode==RESULT_OK && data!=null) {
            ArrayList<String> results=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if(results!=null&&!results.isEmpty()){ input.setText(results.get(0)); input.setSelection(input.length()); sendMessage(); }
        }
    }
    @Override protected void onDestroy(){ if(tts!=null){tts.stop();tts.shutdown();} super.onDestroy(); }
}
