package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.led_control.LedForRecording;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.game_quiz.ReadQuiz;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.wistron.demo.tool.teddybear.scene.SceneActivity.mCurSttEngine;

public class PlayGameScene extends SceneBase {
    private MicrophoneRecognitionClient micClient = null;
    private ReadQuiz readQuiz;

    private boolean luisGet = false;

    private String speechQuestion = "";
    private String[] completeText;
    private final int MSG_RIGHT = 1;
    private final int MSG_WRONG = 2;
    private final int MSG_REPEAT = 3;
    private final int MSG_I_DONOT_KNOW = 4;
    private final int MSG_GAME_OVER = 5;

    private int userWrongTimes;
    private int mScore;

    private Handler quizHandler;

    public PlayGameScene(Context context, Handler mMainHandler) {
        super(context, mMainHandler);
    }

    @Override
    public void stop() {
        super.stop();
        if (micClient != null) {
            micClient.endMicAndRecognition();
            try {
                micClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            micClient = null;
        }

        SceneCommonHelper.closeLED();
        updateLog("Game is over.Thanks for playing");
    }

    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();

        if (quizHandler == null) {
            quizHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case MSG_RIGHT:
                            userAnswerRight();
                            updateLog("User answer right!");
                            nextQuiz();
                            break;
                        case MSG_WRONG:
                            updateLog("User answer wrong!");
                            userAnswerWrong();
                            break;
                        case MSG_REPEAT:
                            textToSpeech(speechQuestion);
                            Log.v("berlin", "repeat qustion.......................");
                            waitForUserAnswer();
                            break;
                        case MSG_I_DONOT_KNOW:
                            userWrongTimes = 2;
                            Log.v("berlin", "straight answer now.......................");
                            userAnswerWrong();
                            break;
                        case MSG_GAME_OVER:
                            textToSpeech(getString(R.string.luis_assistant_game_over));
                            stop();
                            break;
                        default:
                            break;
                    }
                }
            };
        }

        initialQuizGame();
    }

    private void initialQuizGame() {
        userWrongTimes = 0;
        readQuiz = new ReadQuiz();
        readQuiz.setRoundNow(1);
        switch (SceneCommonHelper.getSpeakingLanguageSetting(context)) {
            case CommonHelper.LanguageRegion.REGION_ENGLISH_US:
                readQuiz.readRoundQuiz(context, "quiz.txt");
                break;
            case CommonHelper.LanguageRegion.REGION_CHINESE_CN:
                readQuiz.readRoundQuiz(context, "quiz_cn.txt");
                break;
            default:
                readQuiz.readRoundQuiz(context, "quiz.txt");
                break;
        }

        speechQuestion = readQuiz.getQuestion();
        mScore = 0;
        textToSpeech(String.format("%1$s %2$s %3$s", getString(R.string.luis_assistant_game_start),
                String.format(getString(R.string.luis_assistant_game_question_number), 1), speechQuestion));
        waitForUserAnswer();
    }

    private void waitForUserAnswer() {
        SceneCommonHelper.blinkLED();

        if (!isSceneStopped) {
            if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_MICROSOFT) {
                //added on 21st July.
                if (micClient == null) {
                    micClient = SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                            (Activity) context,
                            SceneCommonHelper.getSpeakingLanguageSetting(context),
                            mSpeechRecognitionEvent,
                            context.getString(SubscriptionKey.getSpeechPrimaryKey()),
                            SceneCommonHelper.getString(context, SubscriptionKey.getLuisMemoGameAppId()),
                            context.getString(SubscriptionKey.getLuisSubscriptionKey()));
                    micClient.setAuthenticationUri(getString(SubscriptionKey.getSpeechAuthenticationUri()));
                }
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (!isSceneStopped) {
                            updateLog("Please answer:");
                            ((SceneActivity) context).stopWakeupService();
                            micClient.startMicAndRecognition();
                        }
                    }
                });
            } else if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_GOOGLE
                    || mCurSttEngine == SceneCommonHelper.STT_ENGINE_GOOGLE_CLOUD) {
                ((SceneActivity) context).startToListenCmd(false, speechQuestion);
            }
        }
    }

    @Override
    public void updateSttResult(String result) {
        super.updateSttResult(result);
        decodeAnswer(result);
    }

    private void decodeAnswer(String result) {
        String sceneTitle = "";
        try {
            Log.i("King", "GameLuis result = " + result);
            if (!TextUtils.isEmpty(result)) {
                JSONObject toJason = new JSONObject(result);
                if (toJason.get(LuisHelper.TAG_QUERY) != null) {
                    sceneTitle = toJason.getString(LuisHelper.TAG_QUERY);

                    JSONArray intents = toJason.getJSONArray(LuisHelper.TAG_INTENTS);
                    String intent = intents.getJSONObject(0).getString(LuisHelper.TAG_INTENT);
                    Log.i("King", "SceneActivity DecodeSceneByLUISTask:: intent = " + intent);
                    switch (intent) {
                        case LuisHelper.INTENT_GAME_READ_ANSWER:
                            luisGet = true;
                            quizHandler.sendMessage(quizHandler.obtainMessage(MSG_I_DONOT_KNOW));
                            return;
                        case LuisHelper.INTENT_GAME_OVER:
                            luisGet = true;
                            quizHandler.sendMessage(quizHandler.obtainMessage(MSG_GAME_OVER));
                            return;
                        case LuisHelper.INTENT_GAME_REPEAT:
                            luisGet = true;
                            quizHandler.sendMessage(quizHandler.obtainMessage(MSG_REPEAT));
                            return;
                        default:
                            luisGet = false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("King", "AssistantActivity result = " + e.getMessage());
        }

        String quiz_answer = readQuiz.getAnswer();
        String useranswer = sceneTitle;

        Log.v("berlin", "luis get result: " + luisGet);
        if (!luisGet) {
            if (1 == readQuiz.getRoundNow() && 1 == readQuiz.getRandomNumber()) {
                if (useranswer.contains("八")) {
                    quizHandler.sendMessage(quizHandler.obtainMessage(MSG_RIGHT));
                    return;
                }
            }

            if (2 == readQuiz.getRoundNow() && 2 == readQuiz.getRandomNumber()) {
                if (useranswer.toLowerCase().contains("usa") ||
                        useranswer.toLowerCase().contains("america") ||
                        useranswer.toLowerCase().contains("united states")) {
                    quizHandler.sendMessage(quizHandler.obtainMessage(MSG_RIGHT));
                    return;
                }
            }

            if (3 == readQuiz.getRoundNow()) {
                if (quiz_answer.toLowerCase().equals("true") &&
                        (useranswer.toLowerCase().contains("yes") || useranswer.equals("对") || useranswer.equals("是") || useranswer.equals("是的"))) {
                    quizHandler.sendMessage(quizHandler.obtainMessage(MSG_RIGHT));
                    return;

                }
                if (quiz_answer.toLowerCase().equals("false") &&
                        (useranswer.toLowerCase().contains("no") || useranswer.contains("不对") || useranswer.contains("不是"))) {
                    quizHandler.sendMessage(quizHandler.obtainMessage(MSG_RIGHT));
                    return;

                }
            }
            if (useranswer.toLowerCase().contains(quiz_answer.toLowerCase())) {
                quizHandler.sendMessage(quizHandler.obtainMessage(MSG_RIGHT));
                return;
            }
        }
        quizHandler.sendMessage(quizHandler.obtainMessage(MSG_WRONG));
    }

    private ISpeechRecognitionServerEvents mSpeechRecognitionEvent = new ISpeechRecognitionServerEvents() {
        @Override
        public void onPartialResponseReceived(String s) {

        }

        @Override
        public void onFinalResponseReceived(RecognitionResult recognitionResult) {
            if (null != micClient) {
                // we got the final result, so it we can end the mic reco.  No need to do this
                // for dataReco, since we already called endAudio() on it as soon as we were done
                // sending all the data.
                micClient.endMicAndRecognition();
            }
            updateLog("********* Final Response Received *********");

            for (int i = 0; i < recognitionResult.Results.length; i++) {
                updateLog("[" + i + "]" + " Confidence=" + recognitionResult.Results[i].Confidence +
                        " Text=\"" + recognitionResult.Results[i].DisplayText + "\"");
            }
            luisGet = false;

            SceneCommonHelper.openLED();

            if (recognitionResult.Results.length == 0) {
                quizHandler.sendMessage(quizHandler.obtainMessage(MSG_WRONG));
            }

            ((SceneActivity) context).startWakeupService();
        }

        public void onIntentReceived(String result) {
            decodeAnswer(result);
            return;
        }

        @Override
        public void onError(int i, String s) {
            updateLog("Error received by onError()");
            updateLog("Error code: " + SpeechClientStatus.fromInt(i) + " " + s);
            updateLog("Error text: " + s);

            micClient.endMicAndRecognition();
            updateLog("Please retry!");

            LedForRecording.recordingStop(context);
            ((SceneActivity) context).startWakeupService();
        }

        @Override
        public void onAudioEvent(boolean b) {
            if (!b) {
                micClient.endMicAndRecognition();
                updateLog("berlin--- Stop listening voice input!  \n  please wait...");
                LedForRecording.recordingStop(context);
            } else {
                LedForRecording.recordingStart(context);
            }
        }
    };

    /* User's answer is right. Then continue next quiz and mScore++   */
    private void userAnswerRight() {
        mScore++;
        updateLog("Great!");
        textToSpeech(getString(R.string.luis_assistant_game_answer_right));
    }

    private void nextQuiz() {
        userWrongTimes = 0;
        updateLog("Round " + readQuiz.getRoundNow() + " is over");
        if (readQuiz.getRoundNow() < 3) {
            readQuiz.readQuizNextQustion();
            textToSpeech(String.format(getString(R.string.luis_assistant_game_question_number), readQuiz.getRoundNow()));

            switch (SceneCommonHelper.getSpeakingLanguageSetting(context)) {
                case CommonHelper.LanguageRegion.REGION_ENGLISH_US:
                    readQuiz.readRoundQuiz(context, "quiz.txt");
                    break;
                case CommonHelper.LanguageRegion.REGION_CHINESE_CN:
                    readQuiz.readRoundQuiz(context, "quiz_cn.txt");
                    break;
                default:
                    readQuiz.readRoundQuiz(context, "quiz.txt");
            }
            speechQuestion = readQuiz.getQuestion();
            textToSpeech(speechQuestion);
            //wait for user's answer

            waitForUserAnswer();
        } else {
            readQuiz.setRoundNow(1);
            completeText = getStringArray(R.array.luis_assistant_game_result);
            if (mScore > 2) {
                textToSpeech(completeText[1] + getString(R.string.luis_assistant_game_completed));
            } else {
                textToSpeech(completeText[mScore] + getString(R.string.luis_assistant_game_completed));
                //game is over
                updateLog("the mScore is " + mScore);
                stop();
            }
        }
    }

    private void textToSpeech(String string) {
        //Todo read the text
        toSpeak(string, false);
    }

    private void userAnswerWrong() {
        if (userWrongTimes < 2) {
            userWrongTimes++;
            textToSpeech(String.format("%1$s %2$s", getString(R.string.luis_assistant_game_answer_wrong), speechQuestion));

            waitForUserAnswer();
        } else {
            textToSpeech(getString(R.string.luis_assistant_game_actual_answer) + readQuiz.getAnswer());

            nextQuiz();
        }
    }
}
