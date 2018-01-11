package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.contract.Scores;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.ImageHelper;
import com.wistron.demo.tool.teddybear.scene.helper.CameraHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by king on 16-4-11.
 */
public class FaceEmotionScene extends SceneBase {
    private static final int REQUEST_TAKE_PHOTO = 0;

    // Face
    private Uri mImageUri;
    private Uri mUriPhotoTaken;
    private Bitmap mBitmap;
    private static FaceServiceClient faceServiceClient;

    // Emotion
    private EmotionServiceClient emotionServiceClient;

    private StringBuilder mSpeakContent = new StringBuilder();

    private FaceDetectionTask mFaceDetectionTask;
    private EmotionDetectTask mEmotionDetectionTask;

    public FaceEmotionScene(Context context, Handler mMainHandler) {
        super(context, mMainHandler);
    }

    @Override
    public void stop() {
        super.stop();
        if (mFaceDetectionTask != null) {
            mFaceDetectionTask.cancel(true);
        }
        if (mEmotionDetectionTask != null) {
            mEmotionDetectionTask.cancel(true);
        }
        SceneCommonHelper.closeLED();
    }

    @Override
    public void simulate() {
        super.simulate();
        mSpeakContent.replace(0, mSpeakContent.length(), getString(R.string.luis_assistant_face_emotion_hi));
        takePhotoFromCustomCamera();
    }

    private void takePhoto() {
        // launch system Camera app
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            // Save the photo taken to a temporary file.
            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                File file = File.createTempFile("IMG_", ".jpg", storageDir);
                mUriPhotoTaken = Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
                ((Activity) context).startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // launch custom Camera helper
    private void takePhotoFromCustomCamera() {
        //take photo..so open LED. it will blink when it is token.
        SceneCommonHelper.openLED();

        Intent intent = new Intent(context, CameraHelper.class);
        // Save the photo taken to a temporary file.
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File file = File.createTempFile("IMG_", ".jpg", storageDir);
            Log.i("King", "taken camera path: " + file);
            mUriPhotoTaken = Uri.fromFile(file);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
            ((Activity) context).startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int readPictureDegree() {
        int degree = 90;
        try {
            ExifInterface exifInterface = new ExifInterface(mUriPhotoTaken.getEncodedPath());
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i("King", "picture rotation = " + degree);
        return degree;
    }

    private Bitmap rotateBitmap(int angle, Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return newBitmap;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null || data.getData() == null) {
                        mImageUri = mUriPhotoTaken;
                    } else {
                        mImageUri = data.getData();
                        // mImageUri = (Bitmap) data.getExtras().get("data");
                    }
                    mBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(
                            mImageUri, context.getContentResolver());

                    //mBitmap = rotateBitmap(readPictureDegree(),mBitmap);

                    if (mBitmap != null) {
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

                        // Start a background task to detect faces in the image.
                        if (!isSceneStopped) {
                            SceneCommonHelper.blinkLED();
                            mFaceDetectionTask = new FaceDetectionTask();
                            mFaceDetectionTask.execute(inputStream);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    private class FaceDetectionTask extends AsyncTask<InputStream, String, Face[]> {
        private boolean mSucceed = true;

        @Override
        protected Face[] doInBackground(InputStream... params) {
            // Get an instance of face service client to detect faces in image.
            if (faceServiceClient == null) {
                faceServiceClient = new FaceServiceRestClient(context.getString(SubscriptionKey.getFaceKey()));
            }
            try {
                publishProgress("Detecting...");

                // Start detection.
                return faceServiceClient.detect(
                        params[0],  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        true,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[]{
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender,
                                FaceServiceClient.FaceAttributeType.Glasses,
                                FaceServiceClient.FaceAttributeType.Smile,
                                FaceServiceClient.FaceAttributeType.HeadPose
                        });
            } catch (Exception e) {
                mSucceed = false;
                publishProgress(e.getMessage());
                //addLog(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            updateLog("\n\n--- Start face detection:");
        }

        @Override
        protected void onProgressUpdate(String... progress) {

        }

        @Override
        protected void onPostExecute(Face[] result) {
            // Show the result on screen when detection is done.
            Log.i("King", "face detection finished... isSceneStopped = " + isSceneStopped);
            if (!isSceneStopped) {
                setUiAfterDetection(result, mSucceed);
            }
        }
    }

    private void setUiAfterDetection(Face[] result, boolean succeed) {
        if (succeed) {
            // The information about the detection result.
            String detectionResult;
            if (result != null && result.length > 0) {
                detectionResult = result.length + " face"
                        + (result.length != 1 ? "s" : "") + " detected";

                DecimalFormat formatter = new DecimalFormat("#0.0");
                String face_description = "Age: " + Math.round(result[0].faceAttributes.age) + "\n"
                        + "Gender: " + result[0].faceAttributes.gender;

                if (result[0].faceAttributes.gender.equals("male")) {
                    mSpeakContent.append(String.format(getString(R.string.luis_assistant_face_emotion_face_msg),
                            getString(R.string.luis_assistant_face_emotion_face_mr),
                            String.valueOf(Math.round(result[0].faceAttributes.age)),
                            getString(R.string.luis_assistant_face_emotion_face_male)));
                } else {
                    mSpeakContent.append(String.format(getString(R.string.luis_assistant_face_emotion_face_msg),
                            getString(R.string.luis_assistant_face_emotion_face_ms),
                            String.valueOf(Math.round(result[0].faceAttributes.age)),
                            getString(R.string.luis_assistant_face_emotion_face_female)));

                }

                updateLog("Face detection Success. " + detectionResult + "\n" + face_description);
                if (!isSceneStopped) {
                    startRecognizeEmotions();
                }
            } else {
                detectionResult = "0 face detected";
                updateLog(detectionResult);
                toSpeakThenDone(getString(R.string.luis_assistant_face_emotion_no_face));
            }
        } else {
            mImageUri = null;
            mBitmap = null;
        }
    }


    private void startRecognizeEmotions() {
        if (emotionServiceClient == null) {
            emotionServiceClient = new EmotionServiceRestClient(context.getString(SubscriptionKey.getEmotionKey()));
        }

        // Do emotion detection using auto-detected faces.
        try {
            mEmotionDetectionTask = new EmotionDetectTask(false);
            mEmotionDetectionTask.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.d("emotion", "Start emotion detection with auto-face detection");

        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        long startTime = System.currentTimeMillis();
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE STARTS HERE
        // -----------------------------------------------------------------------

        List<RecognizeResult> result = null;
        //
        // Detect emotion by auto-detecting faces in the image.
        //
        result = emotionServiceClient.recognizeImage(inputStream);

        String json = gson.toJson(result);
        Log.d("result", json);

        Log.d("emotion", String.format("Detection done. Elapsed time: %d ms", (System.currentTimeMillis() - startTime)));
        // -----------------------------------------------------------------------
        // KEY SAMPLE CODE ENDS HERE
        // -----------------------------------------------------------------------
        return result;
    }

    private class EmotionDetectTask extends AsyncTask<String, String, List<RecognizeResult>> {
        // Store error message
        private Exception e = null;
        private boolean useFaceRectangles = false;

        public EmotionDetectTask(boolean useFaceRectangles) {
            this.useFaceRectangles = useFaceRectangles;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updateLog("\n\n--- Start emotions recognition.");
        }

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            try {
                return processWithAutoFaceDetection();
            } catch (Exception e) {
                this.e = e;    // Store error
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            // Display based on error existence
            Log.i("King", "face detection finished... isSceneStopped = " + isSceneStopped);
            if (isSceneStopped) {
                return;
            }

            updateLog("Recognizing emotions with auto-detected face rectangles...");
            if (e != null) {
                updateLog("Error during recognizing emotions: " + e.getMessage());
                this.e = null;
            } else {
                String emotion = getString(R.string.luis_assistant_face_emotion_no_emotion);
                if (result.size() > 0) {
                    double emotionValue = 0;
                    Scores scores = result.get(0).scores;
                    if (scores.anger > emotionValue) {
                        emotionValue = scores.anger;
                        emotion = String.format(getString(R.string.luis_assistant_face_emotion_emotion_msg), getString(R.string.luis_assistant_face_emotion_emotion_angry));
                    }
                    if (scores.contempt > emotionValue) {
                        emotionValue = scores.contempt;
                        emotion = String.format(getString(R.string.luis_assistant_face_emotion_emotion_msg), getString(R.string.luis_assistant_face_emotion_emotion_contemptuous));
                    }
                    if (scores.disgust > emotionValue) {
                        emotionValue = scores.disgust;
                        emotion = String.format(getString(R.string.luis_assistant_face_emotion_emotion_msg), getString(R.string.luis_assistant_face_emotion_emotion_disgustful));
                    }
                    if (scores.fear > emotionValue) {
                        emotionValue = scores.fear;
                        emotion = String.format(getString(R.string.luis_assistant_face_emotion_emotion_msg), getString(R.string.luis_assistant_face_emotion_emotion_fearful));
                    }
                    if (scores.happiness > emotionValue) {
                        emotionValue = scores.happiness;
                        emotion = String.format(getString(R.string.luis_assistant_face_emotion_emotion_msg), getString(R.string.luis_assistant_face_emotion_emotion_happy));
                    }
                    if (scores.neutral > emotionValue) {
                        emotionValue = scores.neutral;
                        emotion = String.format(getString(R.string.luis_assistant_face_emotion_emotion_msg), getString(R.string.luis_assistant_face_emotion_emotion_neutral));
                    }
                    if (scores.sadness > emotionValue) {
                        emotionValue = scores.sadness;
                        emotion = String.format(getString(R.string.luis_assistant_face_emotion_emotion_msg), getString(R.string.luis_assistant_face_emotion_emotion_sad));
                    }
                    if (scores.surprise > emotionValue) {
                        emotionValue = scores.surprise;
                        emotion = String.format(getString(R.string.luis_assistant_face_emotion_emotion_msg), getString(R.string.luis_assistant_face_emotion_emotion_surprising));
                    }
                }

                mSpeakContent.append(emotion);
                updateLog("Recognize result: " + emotion);

                updateLog("\n\n--- Teddy response: " + mSpeakContent.toString());
                toSpeak(mSpeakContent.toString());
            }

            mImageUri = null;
            mBitmap = null;
            deleteTempFile();
            SceneCommonHelper.closeLED();
        }
    }

    private void deleteTempFile() {
        if (mUriPhotoTaken != null) {
            File tempFile = new File(mUriPhotoTaken.getEncodedPath());
            Log.i("King", "tempFile = " + tempFile.getAbsolutePath());
            tempFile.deleteOnExit();
        }
    }
}
