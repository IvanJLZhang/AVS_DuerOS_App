package com.wistron.demo.tool.teddybear.avs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;
import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;
import com.willblaschko.android.recorderview.RecorderView;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ee.ioc.phon.android.speechutils.RawAudioRecorder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;

import static com.wistron.demo.tool.teddybear.avs.Common.ARG_CLIENT_ID;
import static com.wistron.demo.tool.teddybear.avs.Common.ARG_CODE;
import static com.wistron.demo.tool.teddybear.avs.Common.ARG_CODE_VERIFIER;
import static com.wistron.demo.tool.teddybear.avs.Common.ARG_GRANT_TYPE;
import static com.wistron.demo.tool.teddybear.avs.Common.ARG_REDIRECT_URI;
import static com.wistron.demo.tool.teddybear.avs.Common.AUTHORIZE_CODE;
import static com.wistron.demo.tool.teddybear.avs.Common.AUTHORIZE_INFO;
import static com.wistron.demo.tool.teddybear.avs.Common.CLIENT_ID;
import static com.wistron.demo.tool.teddybear.avs.Common.CODE_VERIFIER;
import static com.wistron.demo.tool.teddybear.avs.Common.PRODUCT_MATEDATA;
import static com.wistron.demo.tool.teddybear.avs.Common.REDIRECT_URI;

/**
 * Time：16-11-14 10:41
 * Author：bob
 */
public class AVSUseClass {
    private Context context;
    private AlexaManager alexaManager;
    private AlexaAudioPlayer audioPlayer;
    private List<AvsItem> avsQueue = new ArrayList<>();
    private SendLive sendLive = null;
    private ToSpeak mToSpeak;
    private File mLocalFile;

    //refresh token
    private Timer refreshTimer;
    private TimerTask refreshTask;

    private static AVSUseClass instance = null;

    public static AVSUseClass getInstance(Context context) {
        if (instance == null) {
            instance = new AVSUseClass(context);
        }
        return instance;
    }

    private AVSUseClass(Context context) {
        this.context = context;

        Log.i(Common.TAG, "init AVSUseClass");
        initialMetadata();
        initial();

        detectRefreshAndAccessToken();
    }

    private void initialMetadata() {
        Log.i(Common.TAG, "initial meta data");
        InitializeProductMetadata.getInstance(context);
    }

    private void initial() {
        sendLive = new SendLive(context);
        sendLive.setSendLiveListener(sendLiveListener);

        mToSpeak = ToSpeak.getInstance(context).init();
    }

    private Handler mMainHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case Common.MSG_SYNC_SUCCESS:
                    break;
                case Common.MSG_SYNC_ERROR:
                    Log.i(Common.TAG, "download token fail, will use default refresh token refresh access " +
                            "token");
                    break;
                case Common.MSG_REFRESH_TOKEN:
                    cancelRefreshTokenTimer();
                    SharedPreferences preferences = context.getSharedPreferences(Common
                            .TOKEN_PREFERENCE_KEY, Context.MODE_PRIVATE);
                    String tokenContent = preferences.getString(Common.PREF_REFRESH_TOKEN, "");
                    //start use preference token refresh access token
                    startRefreshToken(tokenContent);
                    break;
            }
            return false;
        }
    });

    private void detectRefreshAndAccessToken() {
        SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);
        if (preferences.contains(Common.PREF_REFRESH_TOKEN)) {
            String refreshTokenContent = preferences.getString(Common.PREF_REFRESH_TOKEN, "");
            //start use preference token refresh access token
            startRefreshToken(refreshTokenContent);
        } else {
            Log.i(Common.TAG, "First start and no refresh token");
        }
    }

    public boolean detectAccessToken() {
        boolean isHave = false;
        SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);
        String accessToken = preferences.getString(Common.PREF_ACCESS_TOKEN, null);
        if (accessToken != null && accessToken.length() > 0) {
            isHave = true;
        }
        if (!isHave) {
            //Please log in to your account first
            Log.i(Common.TAG, "Please log in to your account first");
            mToSpeak.toSpeak("Please log in to your account first", false);
        }
        return isHave;
    }

    public void cancelRefreshToken() {
        cancelRefreshTokenTimer();
    }

    public void startGetAccessToken() {
        cancelRefreshTokenTimer();

        SharedPreferences preferences = context.getSharedPreferences(AUTHORIZE_INFO, Context.MODE_PRIVATE);
        String authCode = preferences.getString(AUTHORIZE_CODE, "");
        String redirectUri = preferences.getString(REDIRECT_URI, "");
        String clientId = preferences.getString(CLIENT_ID, "");

        Log.i(Common.TAG, "Code: " + authCode + ", URI: " + redirectUri + ", clientId: " + clientId);

        SharedPreferences preferences1 = context.getSharedPreferences(PRODUCT_MATEDATA, Context.MODE_PRIVATE);
        String codeVerifier = preferences1.getString(CODE_VERIFIER, "");

        String url = "https://api.amazon.com/auth/O2/token";
        FormBody.Builder builder = new FormBody.Builder()
                .add(ARG_GRANT_TYPE, "authorization_code")
                .add(ARG_CODE, authCode);
        builder.add(ARG_REDIRECT_URI, redirectUri);
        builder.add(ARG_CLIENT_ID, clientId);

        builder.add(ARG_CODE_VERIFIER, codeVerifier);

        OkHttpClient client = ClientUtil.getTLS12OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                e.printStackTrace();
                Log.i(Common.TAG, "get token faile: " + e.toString());
                startGetAccessToken();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String s = response.body().string();
                Log.i(Common.TAG, "get token success: " + s);
                final TokenResponse tokenResponse = new Gson().fromJson(s, TokenResponse.class);
                //save our tokens to local shared preferences
//                Log.i(Common.TAG, "new access token: " + tokenResponse.access_token);
//                Log.i(Common.TAG, "new refresh token: " + tokenResponse.refresh_token);

                SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                        .MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(Common.PREF_ACCESS_TOKEN, tokenResponse.access_token);
                editor.putString(Common.PREF_REFRESH_TOKEN, tokenResponse.refresh_token);
                editor.apply();

                startRefreshToken(tokenResponse.refresh_token);
            }
        });
    }

    private void startRefreshToken(String refresh_token_content) {
        RefreshToken refreshToken = new RefreshToken(context);
        refreshToken.setRefreshTokenListener(new RefreshToken.RefreshTokenListener() {
            @Override
            public void refreshFail() {
                Log.i(Common.TAG, "refresh token fail");

                SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                        .MODE_PRIVATE);
            }

            @Override
            public void refreshSuccess() {
                Log.i(Common.TAG, "refresh token success");

//                TransportProtocol.getInstance(context);
            }
        });
        Log.i(Common.TAG, "start refresh access token");
        refreshToken.getRefreshToken(refresh_token_content);

        initRefreshTokenTimer();
    }

    public void stop() {
        stopListening();
        cancelRefreshTokenTimer();

        if (gsVoiceRecorder != null) {
            gsVoiceRecorder.stop();
        }
    }

    private static final int AUDIO_RATE = 16000;
    private RawAudioRecorder recorder;
    private RecorderView recorderView;

    private boolean isSendStopRecord = false;
    private boolean isStopCurrentAction = true;

    public boolean isStopCurrentAction() {
        return isStopCurrentAction;
    }

    public void startListening() {
        if (recorder == null) {
            recorder = new RawAudioRecorder(AUDIO_RATE);
        }
        recorder.start();
        isSendStopRecord = true;
        isStopCurrentAction = false;
        sendLive.setStopPreAction(false);
        initPauseRecorder();
        Log.i(Common.TAG, "start record");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sendLive != null) {
                        sendLive.sendAudio(requestBody);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i(Common.TAG, "send live exception: " + e.toString());
                }
            }
        }).start();
    }

    private Timer recorderTimer;
    private TimerTask recorderTask;

    private void initPauseRecorder() {
        recorderTimer = new Timer();
        recorderTask = new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(0);
            }
        };
        recorderTimer.schedule(recorderTask, 30 * 1000);
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            Log.i(Common.TAG, "timer stop recorder");
            stopListening();
            cancelRecorderTimer();
            return false;
        }
    });

    private void cancelRecorderTimer() {
        if (recorderTimer != null) {
            recorderTimer.cancel();
            recorderTimer = null;
        }
        if (recorderTask != null) {
            recorderTask.cancel();
            recorderTask = null;
        }
    }

    //our streaming data requestBody
    private DataRequestBody requestBody = new DataRequestBody() {
        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            //while our recorder is not null and it is still recording, keep writing to POST data
            while (recorder != null && !recorder.isPausing()) {
                if (recorder != null) {
                    final float rmsdb = recorder.getRmsdb();
                    if (recorderView != null) {
                        recorderView.post(new Runnable() {
                            @Override
                            public void run() {
                                recorderView.setRmsdbLevel(rmsdb);
                            }
                        });
                    }
                    if (sink != null && recorder != null) {
                        sink.write(recorder.consumeRecording());
                    }
                }

                //sleep and do it all over again
                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.i(Common.TAG, "prepare stop listen");
            stopListening();
        }
    };

    //tear down our recorder
    private void stopListening() {
        if (isSendStopRecord) {
            isSendStopRecord = false;
        } else {
            return;
        }
        cancelRecorderTimer();
        if (recorder != null) {
            Log.i(Common.TAG, "stop record");
            recorder.stop();
            recorder.release();
            recorder = null;

            if (avsUseListener != null) {
                avsUseListener.stopRecord();
            }
        }
    }

    private boolean isCurrentTestAVS = true;

    public void stopPrevAVS(boolean isCurrentTestAVS) {
        this.isCurrentTestAVS = isCurrentTestAVS;
        stopPreAction();
    }

    private void stopPreAction() {
        isStopCurrentAction = true;
        sendLive.setStopPreAction(true);
//        stopListening();
        stopGSVoiceRecorder();
    }


    //for test GSVoiceRecorder
    private GSVoiceRecorder gsVoiceRecorder = null;
    private ByteArrayOutputStream byteArrayOutputStream = null;

    public void startGSVoiceRecorder() {
        if (gsVoiceRecorder == null) {
            gsVoiceRecorder = new GSVoiceRecorder(callback);
        }
        gsVoiceRecorder.start();

        isSendStopRecord = true;
        isStopCurrentAction = false;
        sendLive.setStopPreAction(false);
    }

    private void stopGSVoiceRecorder() {
        if (isSendStopRecord) {
            isSendStopRecord = false;
        } else {
            return;
        }

        if (gsVoiceRecorder != null) {
            gsVoiceRecorder.stop();
            if (avsUseListener != null) {
                avsUseListener.stopRecord();
            } else {
                Log.i(Common.TAG, "avsUseListener is null");
            }
        } else {
            Log.i(Common.TAG, "gsVoiceRecorder is null");
        }
    }

    GSVoiceRecorder.Callback callback = new GSVoiceRecorder.Callback() {
        @Override
        public void onVoiceStart() {
            super.onVoiceStart();
            Log.i(Common.TAG, "Reset ByteArrayOutputStream");
            byteArrayOutputStream = new ByteArrayOutputStream();
        }

        @Override
        public void onVoice(byte[] data, int size) {
            super.onVoice(data, size);
            try {
                byteArrayOutputStream.write(data);
                Log.i(Common.TAG, "byteArrayOutputStream size: " + byteArrayOutputStream.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onVoiceEnd() {
            super.onVoiceEnd();
            gsVoiceRecorder.stop();
            Log.i(Common.TAG, "AVS: end recording");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendLive.sendAudio(new DataRequestBody() {
                            @Override
                            public void writeTo(BufferedSink sink) throws IOException {
                                try {
                                    sink.write(byteArrayOutputStream.toByteArray());
                                    byteArrayOutputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Log.i(Common.TAG, "write byte exception: " + e.toString());
                                    stopPrevAVS(true);
                                    if (avsUseListener != null) {
                                        avsUseListener.requestFail(0);
                                    }
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i(Common.TAG, "send live exception: " + e.toString());
                    }
                }
            }).start();
        }
    };


    public interface AvsUseListener {
        void stopRecord();

        void requestFail(int code);

        void stopCurAction();
    }

    private AvsUseListener avsUseListener = null;

    public void setAVSUseListener(AvsUseListener listener) {
        avsUseListener = listener;
    }

    private SendLive.SendLiveListener sendLiveListener = new SendLive.SendLiveListener() {
        @Override
        public void sendfailure(int errorCode) {
            if (isStopCurrentAction) {
                if (avsUseListener != null && isCurrentTestAVS) {
                    avsUseListener.stopCurAction();
                }
                return;
            }

            if (isSendStopRecord) {
//                stopListening();
                stopGSVoiceRecorder();
                if (avsUseListener != null) {
                    avsUseListener.requestFail(errorCode);
                }
            }
            isStopCurrentAction = true;
        }

        @Override
        public void playfinish() {
            if (isStopCurrentAction) {
                if (avsUseListener != null && isCurrentTestAVS) {
                    avsUseListener.stopCurAction();
                }
                return;
            }

//            if(!isSendStopRecord){
            stopGSVoiceRecorder();
//            }

            isStopCurrentAction = true;
        }

        @Override
        public void playerror() {
            if (isStopCurrentAction) {
                if (avsUseListener != null && isCurrentTestAVS) {
                    avsUseListener.stopCurAction();
                }
                return;
            }
            isStopCurrentAction = true;
        }

        @Override
        public void saveAudioFail() {
            if (isStopCurrentAction) {
                if (avsUseListener != null && isCurrentTestAVS) {
                    avsUseListener.stopCurAction();
                }
                return;
            }
            isStopCurrentAction = true;
        }

        @Override
        public void stopCurAction() {
            if (isStopCurrentAction) {
                if (avsUseListener != null && isCurrentTestAVS) {
                    avsUseListener.stopCurAction();
                }
                return;
            }
        }
    };


    private void initRefreshTokenTimer() {
        refreshTimer = new Timer();
        refreshTask = new TimerTask() {
            @Override
            public void run() {
                mMainHandler.sendEmptyMessage(Common.MSG_REFRESH_TOKEN);
            }
        };
        refreshTimer.schedule(refreshTask, 5 * 60 * 1000);
    }

    private void cancelRefreshTokenTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    //for JSON parsing of our token responses
    public static class TokenResponse {
        public String access_token;
        public String refresh_token;
        public String token_type;
        public long expires_in;
    }
}
