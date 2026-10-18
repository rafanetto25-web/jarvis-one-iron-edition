
package com.jarvisone.iron;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextToSpeech voz;
    private TextView nucleo;
    private boolean ligado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setGravity(Gravity.CENTER);
        tela.setBackgroundColor(Color.rgb(8, 10, 18));
        tela.setPadding(24, 24, 24, 24);

        TextView titulo = new TextView(this);
        titulo.setText("J.A.R.V.I.S. ONE");
        titulo.setTextColor(Color.rgb(255, 190, 65));
        titulo.setTextSize(28);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setGravity(Gravity.CENTER);
        tela.addView(titulo);

        nucleo = new TextView(this);
        nucleo.setText("◉");
        nucleo.setTextColor(Color.RED);
        nucleo.setTextSize(110);
        nucleo.setGravity(Gravity.CENTER);
        tela.addView(nucleo);

        TextView estado = new TextView(this);
        estado.setText("SISTEMA ONLINE");
        estado.setTextColor(Color.CYAN);
        estado.setTextSize(16);
        estado.setGravity(Gravity.CENTER);
        tela.addView(estado);

        TextView botao = new TextView(this);
        botao.setText("\n  INICIAR JARVIS  \n");
        botao.setTextColor(Color.WHITE);
        botao.setTextSize(20);
        botao.setGravity(Gravity.CENTER);
        botao.setPadding(20, 20, 20, 20);
        tela.addView(botao);

        setContentView(tela);

        voz = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                voz.setLanguage(new Locale("pt", "BR"));
            }
        });

        botao.setOnClickListener(v -> {
            ligado = !ligado;
            nucleo.setText(ligado ? "✺" : "◉");
            nucleo.setTextColor(ligado
                ? Color.rgb(255, 70, 25)
                : Color.RED);

            if (voz != null) {
                voz.speak(
                    ligado ? "Olá. Jarvis online. Como posso ajudar?"
                           : "Sistema em espera.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "jarvis"
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (voz != null) {
            voz.stop();
            voz.shutdown();
        }
        super.onDestroy();
    }
}
