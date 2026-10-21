
package com.jarvisone.iron;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class JarvisVoiceService extends Service {

    private static final String CHANNEL_ID = "jarvis_voice_channel";
    private static final int NOTIFICATION_ID = 701;
    private static final String ACTION_STOP =
            "com.jarvisone.iron.STOP_VOICE";
    private static final String ACTION_COMMAND =
            "com.jarvisone.iron.VOICE_COMMAND";

    private SpeechRecognizer recognizer;
    private boolean listening = false;
    private boolean stopping = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopping = true;
            listening = false;

            if (recognizer != null) {
                recognizer.cancel();
                recognizer.destroy();
                recognizer = null;
            }

            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        stopping = false;

        Notification notification = buildNotification();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        startRecognition();
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Escuta do JARVIS",
                    NotificationManager.IMPORTANCE_LOW
            );

            channel.setDescription(
                    "Notificação enquanto o JARVIS escuta comandos"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("JARVIS ONE")
                .setContentText("Escuta de voz ativada")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void startRecognition() {
        if (stopping || !SpeechRecognizer.isRecognitionAvailable(this)) {
            return;
        }

        try {
            if (recognizer != null) {
                recognizer.destroy();
            }

            recognizer = SpeechRecognizer.createSpeechRecognizer(this);

            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    listening = true;
                }

                @Override
                public void onBeginningOfSpeech() {
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    listening = false;
                }

                @Override
                public void onError(int error) {
                    listening = false;
                    restartRecognition();
                }

                @Override
                public void onResults(Bundle results) {
                    listening = false;

                    ArrayList<String> matches =
                            results.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                            );

                    if (matches != null && !matches.isEmpty()) {
                        processRecognizedText(matches.get(0));
                    }

                    restartRecognition();
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });

            Intent speechIntent = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );

            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "pt-BR"
            );
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                    "pt-BR"
            );
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                    false
            );
            speechIntent.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    3
            );

            recognizer.startListening(speechIntent);
            listening = true;

        } catch (Exception e) {
            listening = false;
            restartRecognition();
        }
    }

    private void processRecognizedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        String normalized = text.toLowerCase(Locale.ROOT)
                .replace("á", "a")
                .replace("à", "a")
                .replace("ã", "a")
                .replace("â", "a")
                .replace("é", "e")
                .replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ô", "o")
                .replace("õ", "o")
                .replace("ú", "u");

        if (!normalized.matches("(?s).*\\bjarvis\\b.*")) {
            return;
        }

        Intent command = new Intent(this, MainActivity.class);
        command.setAction(ACTION_COMMAND);
        command.putExtra("recognized_text", text.trim());
        command.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        try {
            startActivity(command);
        } catch (Exception ignored) {
            // O Android pode bloquear a abertura da tela em segundo plano.
        }
    }

    private void restartRecognition() {
        if (stopping) {
            return;
        }

        if (recognizer != null) {
            try {
                recognizer.cancel();
            } catch (Exception ignored) {
            }
        }

        if (Build.VERSION.SDK_INT >= 29
                && !listening) {
            // A retomada pode depender das restrições do Android.
        }

        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            if (!stopping && recognizer != null) {
                startRecognition();
            }
        }, 700);
    }

    @Override
    public void onDestroy() {
        stopping = true;
        listening = false;

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.destroy();
            recognizer = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
