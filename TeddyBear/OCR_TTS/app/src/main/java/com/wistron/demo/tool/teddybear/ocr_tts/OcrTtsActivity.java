package com.wistron.demo.tool.teddybear.ocr_tts;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;
import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.BaiduTranslate;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.GoogleTranslate;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.ImageHelper;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.MicrosoftTranslate;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.Story;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.SyncParentSettingsByAzure;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.TranslateBase;
import com.wistron.demo.tool.teddybear.scene.helper.CameraHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;
import com.wistron.demo.tool.teddybear.scene.view.FlatButton;
import com.wistron.demo.tool.teddybear.scene.view.FlatUI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;

public class OcrTtsActivity extends AppCompatActivity implements View.OnClickListener {
    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    private static final int REQUEST_TAKE_PHOTO = 0;
    private static final int REQUEST_SELECT_IMAGE_IN_ALBUM = 1;

    private static final int MSG_UPDATE_VIEW_ENABLE = 0;
    private static final int MSG_SHOW_TRANSLATE_DIALOG = 1;
    private static final int MSG_DISMISS_TRANSLATE_DIALOG = 2;
    private static final int MSG_START_TO_SPEECH = 3;
    private static final int MSG_ERROR_SPEAK = 4;
    private static final int MSG_START_READ = 5;
    private static final int MSG_TRANSLATE_ERROR = 6;
    private static final int MSG_GO_ON_INITIAL = 7;

    private int mLaunchMode = CommonHelper.LAUNCH_MODE_DEFAULT;

    private SyncParentSettingsByAzure mParentSettings;

    private Uri mUriPhotoTaken;

    private ImageView iv_SelectPic;
    private Button btn_SelectFromPhoto, btn_SelectFromLocal;
    private EditText et_AnalyzeResult;

    private Uri mImageUri;
    private Bitmap mBitmap;

    private VisionServiceClient client;
    private Synthesizer m_syn;
    private AudioTrack audioTrack;
    private Voice voice;

    // Google TTS service
    private TextToSpeech mGoogleTtsEngine;
    private boolean isGoogleTtsInited = false;

    private HashMap<String, String> mLanguagePairs;
    private boolean isStopped = false;

    private String mAudioLanguage = LanguageCodes.English;
    private String mOcrLanguage = mAudioLanguage;
    private String mOcrResult;
    private String mOldTranslator;

    // for Long text
    private int mPlayPosition = -1;

    // AsyncTask
    private OcrDecode mOcrdecode;
    private AsyncTask mTranslateTask;

    // TeddyBear LaunchMode = From gallery
    private ArrayList<Story> mStoryList;
    private int mCurrentStoryIndex = 0;

    // Google OCR (Reserved)
    private TextRecognizer googleTextRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FlatUI.initDefaultValues(this);
        FlatUI.setDefaultTheme(FlatUI.SKY);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.ocr_tts_activity);

        findView();
        initial();
    }

    private TextToSpeech.OnInitListener googleTtsInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            isGoogleTtsInited = true;
            Log.i("King", "Google TTS init status  = " + status + " [0 is SUCCESS; -1 is ERROR; -2 is STOPPED]");
            mGoogleTtsEngine.setPitch(0.8f);
            mGoogleTtsEngine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    Log.i("King", "GoogleTts onStart");
                    mainHandler.sendEmptyMessage(MSG_START_TO_SPEECH);
                }

                @Override
                public void onDone(String utteranceId) {
                    Log.i("King", "GoogleTts onDone");
                    mainHandler.sendEmptyMessage(MSG_UPDATE_VIEW_ENABLE);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.i("King", "GoogleTts onError");
                    mainHandler.sendEmptyMessage(MSG_UPDATE_VIEW_ENABLE);
                }
            });
        }
    };

    private Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_VIEW_ENABLE:
                    if (mPlayPosition >= 0 && mPlayPosition < et_AnalyzeResult.getText().toString().length()) {
                        playDecodeResult();
                    } else {
                        enableViewState(true);
                        et_AnalyzeResult.setText(et_AnalyzeResult.getText().toString());
                        if (mLaunchMode == CommonHelper.LAUNCH_MODE_TAKE_PHOTO) {
                            finish();
                        } else if (mLaunchMode == CommonHelper.LAUNCH_MODE_FROM_GALLERY) {
                            mCurrentStoryIndex++;
                            if (mCurrentStoryIndex < mStoryList.size()) {
                                mImageUri = Uri.fromFile(new File(mStoryList.get(mCurrentStoryIndex).getPath()));
                                SceneCommonHelper.blinkLED();
                                startOcrDecode();
                            } else {
                                finish();
                            }
                        }
                    }
                    break;
                case MSG_SHOW_TRANSLATE_DIALOG:
                    updateAnalyzeState(getString(R.string.state_analyze_translate));
                    break;
                case MSG_DISMISS_TRANSLATE_DIALOG:
                    et_AnalyzeResult.setText(removeBlackChar(msg.obj.toString()));
                    playDecodeResult();
                    break;
                case MSG_TRANSLATE_ERROR:
                    enableViewState(true);
                    updateAnalyzeState(getString(R.string.state_analyze_translate_error));
                    break;
                case MSG_START_TO_SPEECH:
                    hideAnalyzeState();
                    findViewById(R.id.btnStopRead).setEnabled(true);
                    break;
                case MSG_ERROR_SPEAK:
                    enableViewState(true);
                    updateAnalyzeState(getString(R.string.state_analyze_voice_error));
                    break;
                case MSG_START_READ:
                    startRead();
                    break;
                case MSG_GO_ON_INITIAL:
                    goOnInitial();
                    break;
            }

        }
    };

    private void initial() {
        mGoogleTtsEngine = new TextToSpeech(this, googleTtsInitListener);

        mLanguagePairs = new LinkedHashMap<>();
        /*mLanguagePairs.put("en", LanguageCodes.English);
        mLanguagePairs.put("zh", LanguageCodes.ChineseSimplified);
        mLanguagePairs.put("cht", LanguageCodes.ChineseTraditional);*/
        mLanguagePairs.put(LanguageCodes.English, "en");
        mLanguagePairs.put(LanguageCodes.ChineseSimplified, "zh");
        mLanguagePairs.put(LanguageCodes.ChineseTraditional, "cht");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean parentControl = sharedPreferences.getBoolean(OcrTtsSettingsFragment.KEY_PARENT_CONTROL, true);
        if (parentControl) {
            if (mParentSettings == null) {
                mParentSettings = new SyncParentSettingsByAzure(this);
                mParentSettings.setOnSyncFinishListener(mSyncFinishListener);
            }

            findViewById(R.id.btnStartRead).setEnabled(false);
            updateAnalyzeState(getString(R.string.state_analyze_sync_settings_start));

            mParentSettings.startSync();
        } else {
            goOnInitial();
        }
    }

    private void goOnInitial() {
        findViewById(R.id.btnStartRead).setEnabled(true);
        mLaunchMode = getIntent().getIntExtra(CommonHelper.EXTRA_LAUNCH_MODE, CommonHelper.LAUNCH_MODE_DEFAULT);
        if (mLaunchMode == CommonHelper.LAUNCH_MODE_TAKE_PHOTO) {
            SceneCommonHelper.blinkLED();
            if (btn_SelectFromPhoto.isEnabled()) {
                btn_SelectFromPhoto.performClick();
            }
        } else if (mLaunchMode == CommonHelper.LAUNCH_MODE_FROM_GALLERY) {
            SceneCommonHelper.blinkLED();
            mStoryList = (ArrayList<Story>) getIntent().getSerializableExtra(CommonHelper.EXTRA_STORIES);
            mImageUri = Uri.fromFile(new File(mStoryList.get(mCurrentStoryIndex).getPath()));
            startOcrDecode();
        }
    }

    private void findView() {
        iv_SelectPic = (ImageView) findViewById(R.id.selectedImage);
        et_AnalyzeResult = (EditText) findViewById(R.id.analyzeResult);
        et_AnalyzeResult.setMovementMethod(ScrollingMovementMethod.getInstance());
        btn_SelectFromPhoto = (Button) findViewById(R.id.btnSelectImgFromPhoto);
        btn_SelectFromLocal = (Button) findViewById(R.id.btnSelectImgFromLocal);
        btn_SelectFromPhoto.setOnClickListener(this);
        btn_SelectFromLocal.setOnClickListener(this);
        findViewById(R.id.btnStartRead).setOnClickListener(this);
        findViewById(R.id.btnStopRead).setOnClickListener(this);
        findViewById(R.id.btnTranslate).setOnClickListener(this);
        findViewById(R.id.translate_switch).setOnClickListener(this);
        iv_SelectPic.setOnClickListener(this);

        //set view fonts
        Typeface fontTypeface = Typeface.createFromAsset(this.getAssets(), "fonts/calibril.ttf");

        et_AnalyzeResult.setTypeface(fontTypeface);
        btn_SelectFromLocal.setTypeface(fontTypeface);
        btn_SelectFromPhoto.setTypeface(fontTypeface);
        ((FlatButton) findViewById(R.id.btnStartRead)).setTypeface(fontTypeface);
        ((FlatButton) findViewById(R.id.btnStopRead)).setTypeface(fontTypeface);
    }

    private void enableViewState(boolean enable) {
        btn_SelectFromPhoto.setEnabled(enable);
        btn_SelectFromLocal.setEnabled(enable);
        findViewById(R.id.btnStartRead).setEnabled(enable);
        if (audioTrack != null && AudioTrack.PLAYSTATE_PLAYING == audioTrack.getPlayState()) {
            findViewById(R.id.btnStopRead).setEnabled(true);
        } else {
            findViewById(R.id.btnStopRead).setEnabled(false);
        }
        findViewById(R.id.translate_from).setEnabled(enable);
        findViewById(R.id.translate_switch).setEnabled(enable);
        findViewById(R.id.translate_to).setEnabled(enable);
        findViewById(R.id.btnTranslate).setEnabled(enable);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        /*Intent returnIntent = new Intent();
        returnIntent.putExtra(CommonHelper.EXTRA_KEY_CODE, keyCode);
        setResult(CommonHelper.OCR_RESULT_CODE, returnIntent);*/
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        finish();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ocr_tts_settings, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.ocr_tts_menu_item_settings).setEnabled(findViewById(R.id.btnStartRead).isEnabled());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.ocr_tts_menu_item_settings) {
            Intent intent = new Intent(this, OcrTtsSettingsActivity.class);
            intent.putExtra(CommonHelper.EXTRA_AUDIO_LANGUAGE, mAudioLanguage);
            startActivity(intent);
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("ImageUri", mUriPhotoTaken);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUriPhotoTaken = savedInstanceState.getParcelable("ImageUri");
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Save the photo taken to a temporary file.
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                File file = File.createTempFile("IMG_", ".jpg", storageDir);
                mUriPhotoTaken = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            } catch (IOException e) {
                et_AnalyzeResult.setText("Error encountered. Exception is: " + e.getMessage());
            }
        }
    }

    // launch custom Camera helper
    private void takePhotoFromCustomCamera() {
        Intent intent = new Intent(this, CameraHelper.class);
        // Save the photo taken to a temporary file.
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File file = File.createTempFile("IMG_", ".jpg", storageDir);
            Log.i("King", "taken camera path: " + file);
            mUriPhotoTaken = Uri.fromFile(file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        } catch (IOException e) {
            e.printStackTrace();
            et_AnalyzeResult.setText("Error encountered. Exception is: " + e.getMessage());
        }
    }

    // When the button of "Select a Photo in Album" is pressed.
    private void selectImageInAlbum() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_SELECT_IMAGE_IN_ALBUM);
        }
    }

    private SyncParentSettingsByAzure.OnSyncFinishListener mSyncFinishListener = new SyncParentSettingsByAzure.OnSyncFinishListener() {
        @Override
        public void onSyncParentSuccess() {
            String state = getString(R.string.state_analyze_sync_settings_success);
            updateAnalyzeState(state);
            Toast.makeText(OcrTtsActivity.this, state, Toast.LENGTH_LONG).show();
            waitToGoOnInitial();
        }

        @Override
        public void onSyncParentFail(String error) {
            String state = String.format(getString(R.string.state_analyze_sync_settings_fail), error);
            updateAnalyzeState(state);
            Toast.makeText(OcrTtsActivity.this, state, Toast.LENGTH_LONG).show();
            waitToGoOnInitial();
        }
    };

    private void waitToGoOnInitial() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainHandler.sendEmptyMessage(MSG_GO_ON_INITIAL);
            }
        }, 500);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSelectImgFromPhoto:
                takePhotoFromCustomCamera();
                break;
            case R.id.btnSelectImgFromLocal:
                selectImageInAlbum();
                break;
            case R.id.btnStartRead:
                if (isStopped) {
                    return;
                }

                startRead();
                break;
            case R.id.btnStopRead:
                stopPlay();
                if (mLaunchMode == CommonHelper.LAUNCH_MODE_FROM_GALLERY) {
                    mCurrentStoryIndex = mStoryList.size();
                }
                break;
            case R.id.translate_switch:
                switchTranslate();
                break;
            case R.id.btnTranslate:
                translate();
                break;
            case R.id.selectedImage:
                viewImage();
                break;
            default:
                break;
        }
    }

    private void startRead() {
        et_AnalyzeResult.scrollTo(0, 0);
        if (mOcrResult == null) {
            mOcrResult = et_AnalyzeResult.getText().toString();
        }
        translate(et_AnalyzeResult.getText().toString());
    }

    @Override
    protected void onDestroy() {
        SceneCommonHelper.closeLED();

        isStopped = true;
        if (mParentSettings != null) {
            mParentSettings.stop();
        }
        if (mTranslateTask != null) {
            mTranslateTask.cancel(true);
        }

        findViewById(R.id.btnStopRead).performClick();
        super.onDestroy();
        if (audioTrack != null) {
            audioTrack.release();
        }
        if (m_syn != null) {
            m_syn = null;
        }

        if (mGoogleTtsEngine != null) {
            mGoogleTtsEngine.stop();
            mGoogleTtsEngine.shutdown();
        }

        // Google OCR (Reserved)
        if (googleTextRecognizer != null) {
            googleTextRecognizer.release();
            googleTextRecognizer = null;
        }
    }

    private void viewImage() {
        if (mImageUri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(mImageUri, "image/*");
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
            case REQUEST_SELECT_IMAGE_IN_ALBUM:
                if (resultCode == RESULT_OK) {
                    // stop previous voice
                    stopPlay();
                    // reset
                    mOcrResult = null;
                    et_AnalyzeResult.setText("");
                    et_AnalyzeResult.scrollTo(0, 0);

                    hideAnalyzeState();

                    if (data == null || data.getData() == null) {
                        mImageUri = mUriPhotoTaken;
                    } else {
                        mImageUri = data.getData();
                        // mImageUri = (Bitmap) data.getExtras().get("data");
                    }
                    startOcrDecode();
                }
                break;
            default:
                break;
        }
    }

    private void startOcrDecode() {
        mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                mImageUri, getContentResolver());
        Log.i("King", "mImageUri = " + mImageUri + ", mBitmap = " + mBitmap);
        if (mBitmap != null) {
            // Show the image on screen.
            iv_SelectPic.setImageBitmap(mBitmap);
            findViewById(R.id.selectedImageError).setVisibility(View.GONE);

            // Add detection log.
            Log.d("AnalyzeActivity", "Image: " + mImageUri + " resized to " + mBitmap.getWidth()
                    + "x" + mBitmap.getHeight());

            doRecognize();
            //doGoogleTextRecognize(); // Reserved
        } else {
            findViewById(R.id.selectedImageError).setVisibility(View.VISIBLE);
            et_AnalyzeResult.setText("");
        }
    }

    private void updateAnalyzeState(String state) {
        ((TextView) findViewById(R.id.analyzeState)).setText(state);
        findViewById(R.id.analyzeState).setVisibility(View.VISIBLE);
    }

    private void hideAnalyzeState() {
        findViewById(R.id.analyzeState).setVisibility(View.GONE);
    }

    // Analyze Picture result
    public void doRecognize() {
        enableViewState(false);
        updateAnalyzeState(getString(R.string.state_analyze_image));

        try {
            mOcrdecode = new OcrDecode();
            mOcrdecode.execute();
        } catch (Exception e) {
            updateAnalyzeState("Error encountered. Exception is: " + e.toString());
            enableViewState(true);
        }
    }

    private String process() throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        if (client == null) {
            client = new VisionServiceRestClient(getString(SubscriptionKey.getComputerVisionKey()));
        }
        OCR ocr = this.client.recognizeText(inputStream, LanguageCodes.AutoDetect, true);

        String result = gson.toJson(ocr);
        return result;
    }

    private class OcrDecode extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;

        public OcrDecode() {
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                return process();
            } catch (Exception e) {
                this.e = e;    // Store error
            }

            return null;
        }

        @Override
        protected void onPostExecute(String data) {
            super.onPostExecute(data);
            if (isStopped) {
                return;
            }
            // Display based on error existence
            if (e != null) {
                updateAnalyzeState("Error: " + e.getMessage());
                enableViewState(true);
                this.e = null;
            } else {
                Gson gson = new Gson();
                OCR r = gson.fromJson(data, OCR.class);
                mOcrLanguage = mAudioLanguage = r.language;

                // reserved
                /*Iterator<String> keySet = mLanguagePairs.keySet().iterator();
                for(int i=0; i<mLanguagePairs.size();i++){
                    String key = keySet.next();
                    if (mAudioLanguage.equals(mLanguagePairs.get(key))){
                        Log.i("King", "select fromTranslate to position: "+i);
                        ((Spinner) findViewById(R.id.translate_from)).setSelection(i);
                        break;
                    }
                }*/

                StringBuilder result = new StringBuilder();
                for (Region reg : r.regions) {
                    for (Line line : reg.lines) {
                        for (Word word : line.words) {
                            result.append(word.text);
                            if (!(mAudioLanguage.equals(LanguageCodes.ChineseSimplified)
                                    || mAudioLanguage.equals(LanguageCodes.ChineseTraditional)
                                    || mAudioLanguage.equals(LanguageCodes.Japanese))) {
                                result.append(" ");
                            }
                        }
                        result.append("\n");
                    }
                    result.append("\n\n");
                }

                /*et_AnalyzeResult.setText(result);
                playDecodeResult();*/

                mOcrResult = removeBlackChar(result.toString());
                Log.i("King", "OCR result = " + mOcrResult);
                et_AnalyzeResult.setText(mOcrResult);
                if (TextUtils.isEmpty(mOcrResult.trim())) {
                    ToSpeak mToSpeak = ToSpeak.getInstance(OcrTtsActivity.this);
                    if (mLaunchMode == CommonHelper.LAUNCH_MODE_TAKE_PHOTO) {
                        mToSpeak.toSpeak(SceneCommonHelper.getString(OcrTtsActivity.this, R.string.luis_assistant_ocr_error_from_photo), true);
                        finish();
                    } else if (mLaunchMode == CommonHelper.LAUNCH_MODE_FROM_GALLERY) {
                        if (mCurrentStoryIndex < mStoryList.size() - 1) {
                            mToSpeak.toSpeak(SceneCommonHelper.getString(OcrTtsActivity.this, R.string.luis_assistant_ocr_error_gallery_to_next), true);
                        } else {
                            mToSpeak.toSpeak(SceneCommonHelper.getString(OcrTtsActivity.this, R.string.luis_assistant_ocr_error_from_gallery), true);
                        }
                        mainHandler.sendEmptyMessage(MSG_UPDATE_VIEW_ENABLE);
                    } else {
                        mainHandler.sendEmptyMessage(MSG_UPDATE_VIEW_ENABLE);
                    }
                } else {
                    findViewById(R.id.btnStartRead).performClick();
                }
            }
        }
    }

    private String getSplitContentToRead(String content) {
        String splitContent;

        int totalLength = content.length();
        int startPosition = mPlayPosition;
        int LENGTH_TO_READ = CommonHelper.LENGTH_TO_READ_MAX_800;
        if (CommonHelper.mLanguageLimitationPair.containsKey(mAudioLanguage)) {
            LENGTH_TO_READ = CommonHelper.mLanguageLimitationPair.get(mAudioLanguage);
        }
        if (mPlayPosition + LENGTH_TO_READ >= totalLength) {
            splitContent = content.substring(mPlayPosition);
            mPlayPosition = totalLength;
        } else {
            int offset;
            for (offset = mPlayPosition + LENGTH_TO_READ; offset < totalLength; offset++) {
                String temp = content.substring(offset, offset + 1);
                if (CommonHelper.PUNCTUATION_CONTAINS.contains(temp)) {
                    offset++;
                    break;
                }
            }
            if (offset >= totalLength) {
                splitContent = content.substring(mPlayPosition);
                mPlayPosition = totalLength;
            } else {
                splitContent = content.substring(mPlayPosition, offset);
                mPlayPosition = offset;
            }
        }

        SpannableStringBuilder spannable = new SpannableStringBuilder(content);
        ForegroundColorSpan span = new ForegroundColorSpan(Color.BLUE);
        spannable.setSpan(span, startPosition, mPlayPosition, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        et_AnalyzeResult.setText(spannable);

        int scrollPosition = mPlayPosition;
        et_AnalyzeResult.setSelection(scrollPosition, scrollPosition); // auto scroll

        return splitContent;
    }

    // Google tts: set language
    private boolean setGoogleTtsLanguage(String locale) {
        int setTtsLanguageResult = -1;
        Set<Locale> mAvailableLanguages = mGoogleTtsEngine.getAvailableLanguages();
        for (Locale tempLocale : mAvailableLanguages) {
            if (locale.equalsIgnoreCase(tempLocale.toString())) {
                setTtsLanguageResult = mGoogleTtsEngine.setLanguage(tempLocale);
                break;
            }
        }

        if (setTtsLanguageResult == TextToSpeech.LANG_MISSING_DATA || setTtsLanguageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.i("King", "The language is not supported!");
            return false;
        } else {
            Log.i("King", "TTS language set success!");
            return true;
        }
    }

    // start to speech
    private void playDecodeResult() {
        SceneCommonHelper.openLED();

        final String content = et_AnalyzeResult.getText().toString().trim();

        if (!TextUtils.isEmpty(content)) {
            enableViewState(false);
            updateAnalyzeState(getString(R.string.state_analyze_voice));

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String mSettingsTranslator = sharedPreferences.getString(OcrTtsSettingsFragment.KEY_TRANSLATOR, TranslateBase.TRANSLATOR_BAIDU);
            if (TranslateBase.TRANSLATOR_GOOGLE.equals(mSettingsTranslator)) {
                String mSettingsVoiceLanguage = sharedPreferences.getString(OcrTtsSettingsFragment.KEY_GOOGLE_VOICE_LANGUAGE, getString(R.string.preference_google_voice_language_default_value));
                if (setGoogleTtsLanguage(mSettingsVoiceLanguage)) {
                    String splitContent = getSplitContentToRead(content);
                    mGoogleTtsEngine.speak(CommonHelper.getSpeakContent(splitContent), TextToSpeech.QUEUE_ADD, null, "" + mPlayPosition);
                } else {
                    finish();
                }
            } else {
                //m_syn.SpeakToAudio(content);

                if (mPlayPosition == 0) {
                    voice = CommonHelper.getTtsVoice(this, mAudioLanguage);
                }

                if (m_syn == null) {
                    m_syn = new Synthesizer(getString(SubscriptionKey.getSpeechPrimaryKey()));
                    m_syn.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
                }
                m_syn.SetVoice(voice, null);

                if (audioTrack == null) {
                    audioTrack = new AudioTrack(3, 16000, 2, 2, AudioTrack.getMinBufferSize(16000, 2, 2), 1);
                }

                final String splitContent = getSplitContentToRead(content);
                AsyncTask.execute(new Runnable() {
                    public void run() {
                        if (isStopped) {
                            return;
                        }
                        byte[] sound = null;
                        try {
                            sound = m_syn.SpeakSSML(CommonHelper.formatMicrosoftTTSToSSML(voice, CommonHelper.getSpeakContent(splitContent)));
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            mainHandler.sendEmptyMessage(MSG_UPDATE_VIEW_ENABLE);
                            return;
                        }
                        if (sound != null && sound.length != 0) {
                            mainHandler.sendEmptyMessage(MSG_START_TO_SPEECH);
                            if (audioTrack.getState() == 1) {
                                audioTrack.play();
                                audioTrack.write(sound, 0, sound.length);
                                if (audioTrack != null && audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                                    audioTrack.stop();
                                }
                                mainHandler.sendEmptyMessage(MSG_UPDATE_VIEW_ENABLE);
                            }
                        } else {
                            mainHandler.sendEmptyMessage(MSG_ERROR_SPEAK);
                        }
                    }
                });
            }
        } else {
            enableViewState(true);
            updateAnalyzeState(getString(R.string.state_analyze_content_empty));
            finish();
        }
    }

    private void stopPlay() {
        if (audioTrack != null) {
            if (AudioTrack.PLAYSTATE_PLAYING == audioTrack.getPlayState()
                    || AudioTrack.PLAYSTATE_PAUSED == audioTrack.getPlayState()) {
                audioTrack.stop();
                enableViewState(true);
            }
        }
        mPlayPosition = -1;
    }

    // Translate, reserved
    private void translate() {
        Spinner fromSpinner = (Spinner) findViewById(R.id.translate_from);
        Spinner toSpinner = (Spinner) findViewById(R.id.translate_to);
        final String fromLanguage = (String) fromSpinner.getSelectedItem();
        final String toLanguage = (String) toSpinner.getSelectedItem();
        String toLocaleLanguage = mLanguagePairs.get(toLanguage);
        if (!fromLanguage.equals(toLanguage) && (mAudioLanguage == null || !mAudioLanguage.equals(toLocaleLanguage))) {
            mAudioLanguage = toLocaleLanguage;
            final String content = et_AnalyzeResult.getText().toString().trim();
            Log.i("King", "content = " + content);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mainHandler.sendEmptyMessage(MSG_SHOW_TRANSLATE_DIALOG);
                    TranslateBase mTranslate = new BaiduTranslate(OcrTtsActivity.this, fromLanguage, toLanguage);
                    String translate = mTranslate.translate(content);
                    Log.i("King", "translateResult = " + translate);
                    Message message = mainHandler.obtainMessage(MSG_DISMISS_TRANSLATE_DIALOG);
                    message.obj = translate;
                    mainHandler.sendMessage(message);
                }
            }).start();
        }
    }

    private void translate(String content) {
        mPlayPosition = 0;
        enableViewState(false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean mSettingsTranslateEnabler = sharedPreferences.getBoolean(OcrTtsSettingsFragment.KEY_TRANSLATE_ENABLER, true);
        if (!mSettingsTranslateEnabler) {
            mAudioLanguage = mOcrLanguage;
            content = mOcrResult;
        }

        if (!TextUtils.isEmpty(content)) {
            String mSettingsTranslator = sharedPreferences.getString(OcrTtsSettingsFragment.KEY_TRANSLATOR, TranslateBase.TRANSLATOR_BAIDU);
            String mSettingsVoiceLanguage;

            String ocrLanguage;
            String fromLanguage, toLanguage;
            if (TranslateBase.TRANSLATOR_GOOGLE.equals(mSettingsTranslator)) {
                mSettingsVoiceLanguage = sharedPreferences.getString(OcrTtsSettingsFragment.KEY_GOOGLE_VOICE_LANGUAGE, getString(R.string.preference_google_voice_language_default_value));

                ocrLanguage = CommonHelper.mLanguageGoogleTranslatePairs.get(mOcrLanguage);
                fromLanguage = CommonHelper.mLanguageGoogleTranslatePairs.get(mAudioLanguage);
                toLanguage = CommonHelper.mRegionGoogleTranslatePairs.get(mSettingsVoiceLanguage);
            } else if (TranslateBase.TRANSLATOR_MICROSOFT.equals(mSettingsTranslator)) {
                mSettingsVoiceLanguage = sharedPreferences.getString(OcrTtsSettingsFragment.KEY_VOICE_LANGUAGE, getString(R.string.preference_voice_language_default_value));

                ocrLanguage = CommonHelper.mLanguageMicrosoftTranslatePairs.get(mOcrLanguage);
                fromLanguage = CommonHelper.mLanguageMicrosoftTranslatePairs.get(mAudioLanguage);
                toLanguage = CommonHelper.mRegionMicrosoftTranslatePairs.get(mSettingsVoiceLanguage);
            } else {
                mSettingsVoiceLanguage = sharedPreferences.getString(OcrTtsSettingsFragment.KEY_VOICE_LANGUAGE, getString(R.string.preference_voice_language_default_value));

                ocrLanguage = CommonHelper.mLanguageBaiduTranslatePairs.get(mOcrLanguage);
                fromLanguage = CommonHelper.mLanguageBaiduTranslatePairs.get(mAudioLanguage);
                toLanguage = CommonHelper.mRegionBaiduTranslatePairs.get(mSettingsVoiceLanguage);
            }

            Log.i("King", "mSettingsTranslateEnabler = " + mSettingsTranslateEnabler + " , fromLanguage = " + fromLanguage + ", toLanguage = " + toLanguage + ", ocrLanguage = " + ocrLanguage);
            Log.i("King", "mSettingsTranslator = " + mSettingsTranslator + ", mOldTranslator = " + mOldTranslator);
            final String from = fromLanguage;
            final String to = toLanguage;
            if (mSettingsTranslateEnabler && !ocrLanguage.equals(toLanguage)  // 原OCR不用翻译
                    && (!fromLanguage.equals(toLanguage) || !mSettingsTranslator.equals(mOldTranslator))) { // 源语言和目标语言不一样， 或者 源语言和目标语言一样，但Translator发生了变化，需要翻译
                if (!isStopped) {
                    mTranslateTask = new TranslateTask().execute(mSettingsTranslator, from, to);
                }
            } else {
                if (ocrLanguage != null) {
                    if (ocrLanguage.equals(toLanguage)) {
                        mAudioLanguage = mOcrLanguage;
                        content = mOcrResult;
                    }

                    et_AnalyzeResult.setText(removeBlackChar(content));
                    if (null == CommonHelper.mLanguageRegionPair.get(mAudioLanguage)) {  // the language doesn't support to speech now.
                        updateAnalyzeState(String.format(getString(R.string.state_analyze_language_not_support), mAudioLanguage));
                        enableViewState(true);
                        finish();
                    } else {
                        playDecodeResult();
                    }
                } else {
                    updateAnalyzeState(String.format(getString(R.string.state_analyze_language_not_support), mAudioLanguage));
                    enableViewState(true);
                    finish();
                }
            }
        } else {
            updateAnalyzeState(getString(R.string.state_analyze_content_empty));
            enableViewState(true);
            finish();
        }
    }

    private class TranslateTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            String mSettingsTranslator = params[0];
            String from = params[1];
            String to = params[2];

            mainHandler.sendEmptyMessage(MSG_SHOW_TRANSLATE_DIALOG);

            TranslateBase mTranslate = null;
            if (TranslateBase.TRANSLATOR_GOOGLE.equals(mSettingsTranslator)) {
                mTranslate = new GoogleTranslate(OcrTtsActivity.this, from, to);
            } else if (TranslateBase.TRANSLATOR_MICROSOFT.equals(mSettingsTranslator)) {
                mTranslate = new MicrosoftTranslate(OcrTtsActivity.this, MicrosoftTranslate.MICROSOFT_LANGUAGE_AUTO, to);
            } else {
                mTranslate = new BaiduTranslate(OcrTtsActivity.this, BaiduTranslate.BAIDU_LANGUAGE_AUTO, to);
            }
            Log.i("King", "old mAudioLanguage = " + mAudioLanguage);
            String translate = mTranslate.translate(mOcrResult);
            if (translate != null) {
                // convert mAudioLanguage
                HashMap<String, String> mPairs = null;
                if (TranslateBase.TRANSLATOR_GOOGLE.equals(mSettingsTranslator)) {
                    mPairs = CommonHelper.mLanguageGoogleTranslatePairs;
                } else if (TranslateBase.TRANSLATOR_MICROSOFT.equals(mSettingsTranslator)) {
                    mPairs = CommonHelper.mLanguageMicrosoftTranslatePairs;
                } else {
                    mPairs = CommonHelper.mLanguageBaiduTranslatePairs;
                }
                for (String key : mPairs.keySet()) {
                    String value = mPairs.get(key);
                    if (value.equals(to)) {
                        mAudioLanguage = key;
                        break;
                    }
                }
                // save current translator
                mOldTranslator = mSettingsTranslator;

                Log.i("King", "New mAudioLanguage = " + mAudioLanguage);
                Message message = mainHandler.obtainMessage(MSG_DISMISS_TRANSLATE_DIALOG);
                message.obj = translate;
                mainHandler.sendMessage(message);
            } else {
                Log.i("King", "Translate error!");
                mainHandler.sendEmptyMessage(MSG_TRANSLATE_ERROR);
            }
            return null;
        }
    }

    private String removeBlackChar(String text) {
        String result = text;
        // 中文去空格, 去換行
        if (mAudioLanguage.equals(LanguageCodes.ChineseSimplified)
                || mAudioLanguage.equals(LanguageCodes.ChineseTraditional)
                || mAudioLanguage.equals(LanguageCodes.Japanese)) {
            result = result.replaceAll("\\s*", "");
        } else if (mAudioLanguage.equals(LanguageCodes.English)) {
            result = result.replaceAll("[\\f\\n\\r\\t]+", " ");
        }
        return result;
    }

    private void switchTranslate() {
        Spinner fromSpinner = (Spinner) findViewById(R.id.translate_from);
        Spinner toSpinner = (Spinner) findViewById(R.id.translate_to);
        int fromPosition = fromSpinner.getSelectedItemPosition();
        int toPosition = toSpinner.getSelectedItemPosition();
        fromSpinner.setSelection(toPosition);
        toSpinner.setSelection(fromPosition);
    }

    // receiver the broadcast action for Scenario done
    private boolean isRegisteredDoneBroadcast = false;

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter mFilter = new IntentFilter(CommonHelper.ACTION_KEYWORD_DETECTED);
        registerReceiver(scenarioFinish, mFilter);
        isRegisteredDoneBroadcast = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRegisteredDoneBroadcast) {
            unregisterReceiver(scenarioFinish);
            isRegisteredDoneBroadcast = false;
        }
    }

    private BroadcastReceiver scenarioFinish = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CommonHelper.ACTION_KEYWORD_DETECTED)) {
                finish();
            }
        }
    };

    // Google OCR (Reserved)
    public void doGoogleTextRecognize() {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Log.i("King", "doGoogleTextRecognize: google play service is unavailable");
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        } else {
            googleTextRecognizer = new TextRecognizer.Builder(this).build();
            googleTextRecognizer.setProcessor(new OcrDetectorProcessor());
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Frame outputFrame = new Frame.Builder().
                                setBitmap(mBitmap)
                                .build();
                        //googleTextRecognizer.receiveFrame(outputFrame);

                        SparseArray<TextBlock> items = googleTextRecognizer.detect(outputFrame);
                        for (int i = 0; i < items.size(); ++i) {
                            TextBlock item = items.valueAt(i);
                            if (item != null && item.getValue() != null) {
                                Log.d("King", "Google OCR :: Text detected! " + item.getValue());
                            }
                        }
                    } catch (Throwable t) {
                        Log.e("King", "googleTextRecognizer Exception thrown from receiver.", t);
                    }
                }
            });
        }
    }

    public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {
        /**
         * Called by the detector to deliver detection results.
         * If your application called for it, this could be a place to check for
         * equivalent detections by tracking TextBlocks that are similar in location and content from
         * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
         * multiple detections.
         */
        @Override
        public void receiveDetections(Detector.Detections<TextBlock> detections) {
            SparseArray<TextBlock> items = detections.getDetectedItems();
            for (int i = 0; i < items.size(); ++i) {
                TextBlock item = items.valueAt(i);
                if (item != null && item.getValue() != null) {
                    Log.d("King", "Google OCR :: Text detected! " + item.getValue());
                }
            }
        }

        /**
         * Frees the resources associated with this detection processor.
         */
        @Override
        public void release() {
        }
    }
}