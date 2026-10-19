
package com.jarvisone.iron;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.PermissionRequest;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView navegador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navegador = new WebView(this);
        navegador.setBackgroundColor(0xFF080A12);

        WebSettings configuracao = navegador.getSettings();
        configuracao.setJavaScriptEnabled(true);
        configuracao.setDomStorageEnabled(true);
        configuracao.setMediaPlaybackRequiresUserGesture(false);

        navegador.setWebViewClient(new WebViewClient());
        navegador.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest pedido) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= 23 &&
                        checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(
                            new String[]{Manifest.permission.RECORD_AUDIO}, 100
                        );
                        Toast.makeText(
                            MainActivity.this,
                            "Permita o microfone e toque em ATIVAR VOZ novamente.",
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
                new String[]{Manifest.permission.RECORD_AUDIO}, 100
            );
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
            navegador.destroy();
        }
        super.onDestroy();
    }
}
