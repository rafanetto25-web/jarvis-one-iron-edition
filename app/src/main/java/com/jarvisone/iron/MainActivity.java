
package com.jarvisone.iron;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_MICROPHONE = 100;
    private WebView navegador;
    private String pendingCommand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navegador = new WebView(this);
        navegador.setBackgroundColor(0xFF080A12);

        WebSettings configuracao = navegador.getSettings();
        configuracao.setJavaScriptEnabled(true);
        configuracao.setDomStorageEnabled(true);
        configuracao.setMediaPlaybackRequiresUserGesture(false);

        navegador.addJavascriptInterface(new JarvisBridge(), "AndroidJarvis");

        navegador.setWebViewClient(new WebViewClient());

        navegador.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest pedido) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= 23 &&
                            checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                                    != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                REQUEST_MICROPHONE
                        );
                        Toast.makeText(
                                MainActivity.this,
                                "Permita o microfone e tente novamente.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        pedido.grant(pedido.getResources());
                    }
                });
            }
        });

        setContentView(navegador);
        navegador.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MICROPHONE
            );
        }

        handleVoiceCommand(getIntent());
    }

    private void handleVoiceCommand(Intent intent) {
        if (intent == null ||
                !"com.jarvisone.iron.VOICE_COMMAND".equals(intent.getAction())) {
            return;
        }

        String text = intent.getStringExtra("recognized_text");
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        pendingCommand = text;

        if (navegador != null) {
            navegador.post(() -> {
                if (navegador != null) {
                    navegador.evaluateJavascript(
                            "window.jarvisReceiveCommand && " +
                            "window.jarvisReceiveCommand(" +
                            org.json.JSONObject.quote(text) + ");",
                            null
                    );
                    pendingCommand = null;
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleVoiceCommand(intent);
    }

    private class JarvisBridge {
        @JavascriptInterface
        public void startVoiceService() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 23 &&
                        checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                                != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_MICROPHONE
                    );
                    return;
                }

                Intent service = new Intent(
                        MainActivity.this,
                        JarvisVoiceService.class
                );

                try {
                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(service);
                    } else {
                        startService(service);
                    }
                    Toast.makeText(
                            MainActivity.this,
                            "Serviço de voz solicitado.",
                            Toast.LENGTH_SHORT
                    ).show();
                } catch (Exception e) {
                    Toast.makeText(
                            MainActivity.this,
                            "Não foi possível iniciar a escuta.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
        }

        @JavascriptInterface
        public void stopVoiceService() {
            runOnUiThread(() -> {
                Intent service = new Intent(
                        MainActivity.this,
                        JarvisVoiceService.class
                );
                service.setAction("com.jarvisone.iron.STOP_VOICE");
                startService(service);
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_MICROPHONE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                    this,
                    "Microfone autorizado.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (navegador != null && navegador.canGoBack()) {
            navegador.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (navegador != null) {
            navegador.removeJavascriptInterface("AndroidJarvis");
            navegador.destroy();
            navegador = null;
        }
        super.onDestroy();
    }
}
