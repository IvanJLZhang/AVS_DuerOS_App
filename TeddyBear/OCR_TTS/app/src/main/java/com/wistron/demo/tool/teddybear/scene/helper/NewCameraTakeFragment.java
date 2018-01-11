package com.wistron.demo.tool.teddybear.scene.helper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class NewCameraTakeFragment extends Fragment {
    private final String TAG = "NewCameraTakeFragment";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private TextureView mPreView;
    private TextView mTakeCounter;
    private SurfaceTexture mSurfaceTexture;
    private CameraManager mCameraManager;
    private CameraDevice mCamera;
    private CaptureRequest.Builder mPreviewBuilder;
    private CaptureRequest.Builder mCaptureBuilder;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;
    private String[] mCameraIdList;
    private String mCameraID;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private final int MSG_CLEAR_COUNTER = 0;
    private final int MSG_UPDATE_COUNTER = 1;
    private Uri mFileUri;
    private final int COUNTER_VALUE = 6;
    private int mCounterIndex = 0;

    public NewCameraTakeFragment() {
        // Required empty public constructor
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCameraManager = (CameraManager) getActivity().getSystemService(Activity.CAMERA_SERVICE);
        try {
            mCameraIdList = mCameraManager.getCameraIdList();  // {"0","1"}
            if (mCameraIdList == null || mCameraIdList.length <= 0) {
                Toast.makeText(getActivity(), "There is no Camera!", Toast.LENGTH_SHORT).show();
                getActivity().finish();
                return;
            } else {
                initial();
            }
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

            Toast.makeText(getActivity(), "Fail to access the Camera!", Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }
    }

    private void initial() {
        mFileUri = getArguments().getParcelable(MediaStore.EXTRA_OUTPUT);
    }

    private void startCounter() {
        handler.postDelayed(updateTimerRunnable, 1000);
    }

    private Runnable updateTimerRunnable = new Runnable() {

        @Override
        public void run() {
            mCounterIndex++;
            if (mCounterIndex >= COUNTER_VALUE) {
                handler.sendEmptyMessage(MSG_CLEAR_COUNTER);
                if (mCamera != null) {
                    capture();
                }
            } else {
                handler.sendEmptyMessage(MSG_UPDATE_COUNTER);
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimerRunnable);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_CLEAR_COUNTER:
                    mTakeCounter.setText("");
                    break;
                case MSG_UPDATE_COUNTER:
                    mTakeCounter.setText(String.valueOf(COUNTER_VALUE - mCounterIndex));
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_new_camera_take, container, false);
        mPreView = (TextureView) view.findViewById(R.id.camera_take_preview);
        mTakeCounter = (TextView) view.findViewById(R.id.camera_take_counter);
        return view;
    }

    private void startBackgroundThread() {
        mHandlerThread = new HandlerThread("NewCameraTakeFragment");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        if (mPreView.isAvailable()) {
            initCamera();
        } else {
            mPreView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        resetCamera();
        stopBackgroundThread();
    }

    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // TODO Auto-generated method stub
            Log.i(TAG, "SurfaceTextureListener  onSurfaceTextureUpdated......");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                                int height) {
            // TODO Auto-generated method stub
            Log.i(TAG, "SurfaceTextureListener  onSurfaceTextureSizeChanged......");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                              int height) {
            // TODO Auto-generated method stub
            Log.i(TAG, "SurfaceTextureListener  onSurfaceTextureAvailable......");
            initCamera();
        }
    };

    private void resetCamera() {
        // TODO Auto-generated method stub
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    public CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureStarted(CameraCaptureSession session,
                                     CaptureRequest request, long timestamp, long frameNumber) {
            // TODO Auto-generated method stub
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult partialResult) {
            // TODO Auto-generated method stub
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            // TODO Auto-generated method stub
            super.onCaptureCompleted(session, request, result);

            /*try {
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }*/
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session,
                                    CaptureRequest request, CaptureFailure failure) {
            // TODO Auto-generated method stub
            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session,
                                               int sequenceId, long frameNumber) {
            // TODO Auto-generated method stub
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session,
                                             int sequenceId) {
            // TODO Auto-generated method stub
            super.onCaptureSequenceAborted(session, sequenceId);
        }

    };

    private OnImageAvailableListener mImageAvailableListener = new OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            // TODO Auto-generated method stub
            Log.i("King", "NewCameraTakeFragment mImageAvailableListener ");
            mHandler.post(new ImageSaver(reader.acquireNextImage()));
        }
    };

    private void setCameraParameters() {
        // TODO Auto-generated method stub
        if (mCamera == null) {
            return;
        }

        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
            StreamConfigurationMap configurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            mSurfaceTexture = mPreView.getSurfaceTexture();
            android.util.Size[] previewSizes = configurationMap.getOutputSizes(SurfaceTexture.class);
            Log.i("King", "Support outputeSize: ");
            for (Size tempSize : previewSizes) {
                Log.i("King", "--> " + tempSize.getHeight() + " * " + tempSize.getWidth());
            }
            android.util.Size previewSize = previewSizes[previewSizes.length > 0 ? previewSizes.length / 2 : 0];
            Log.i("King", "Preview width*height = " + previewSize.getWidth() + " * " + previewSize.getHeight());
            mSurfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            android.util.Size[] jpegCaptureSize = configurationMap.getOutputSizes(ImageFormat.JPEG);
            Log.i("King", "Support JPEG captureSize: ");
            for (Size tempSize : jpegCaptureSize) {
                Log.i("King", "--> " + tempSize.getHeight() + " * " + tempSize.getWidth());
            }
            android.util.Size jpegSize = jpegCaptureSize[jpegCaptureSize.length > 0 ? jpegCaptureSize.length / 2 : 0];
            Log.i("King", "Capture width*height = " + jpegSize.getWidth() + " * " + jpegSize.getHeight());
            mImageReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(),
                    // PixelFormat.RGBA_8888, 2);
                    ImageFormat.JPEG, /*maxImages*/2);
            mImageReader.setOnImageAvailableListener(mImageAvailableListener, mHandler);

            Surface surface = new Surface(mSurfaceTexture);

            // ***************  Preview Builder ******************
            Log.i("King", "setCameraParameters");
            mPreviewBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surface);

            int mDisplayOrientation = getActivity().getWindowManager().getDefaultDisplay().getRotation() * 90;
            int mJpegOrientation = getJpegOrientation(characteristics, mDisplayOrientation);
            mPreviewBuilder.set(CaptureRequest.JPEG_ORIENTATION, mJpegOrientation);
            Log.i("King", "mDisplayOrientation = " + mDisplayOrientation + ", mJpegOrientation= " + mJpegOrientation);

            mCamera.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), mSessionStateCallback, mHandler);

            // ***************  Capture Builder ******************
            mCaptureBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);

            // Orientation
            int captureRotation = ORIENTATIONS.get(getActivity().getWindowManager().getDefaultDisplay().getRotation());
            //mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, (mJpegOrientation+180)%360);
            mCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, mJpegOrientation);
            Log.i("King", "captureRotation = " + captureRotation);

        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private int getJpegOrientation(CameraCharacteristics c, int deviceOrientation) {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN)
            return 0;
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90;

        // Reverse device orientation for front-facing cameras
        boolean facingFront = c.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT;
        if (facingFront) deviceOrientation = -deviceOrientation;

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        int jpegOrientation = (sensorOrientation + deviceOrientation + 360) % 360;

        return jpegOrientation;
    }

    private void initCamera() {
        try {
            if (mCameraIdList.length == 1) {
                mCameraManager.openCamera(mCameraIdList[0], mDeviceStateCallback, mHandler);
            } else {
                for (String cameraId : mCameraIdList) {
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                        mCameraManager.openCamera(cameraId, mDeviceStateCallback, mHandler);
                        break;
                    }
                }
            }
        } catch (SecurityException e) {
            // TODO: handle exception
            e.printStackTrace();

            Toast.makeText(getActivity(), "No permission to open the Camera!", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        } catch (CameraAccessException e) {
            e.printStackTrace();

            Toast.makeText(getActivity(), "Fail to access the Camera!", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        } catch (RuntimeException e) {
            e.printStackTrace();

            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    }

    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.i(TAG, "Camera opened...");
            mCameraID = camera.getId();
            mCamera = camera;
            setCameraParameters();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onClosed(CameraDevice camera) {
            // TODO Auto-generated method stub
            super.onClosed(camera);
        }

    };

    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            // TODO Auto-generated method stub
            try {
                Log.i(TAG, "CameraCaptureSession  onConfigured");
                mCaptureSession = session;
                session.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler);

                startCounter();
            } catch (CameraAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            // TODO Auto-generated method stub
            Log.i(TAG, "CameraCaptureSession  onConfigureFailed");
        }
    };

    private void capture() {
        try {
            Log.i("King", "start capture...");
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // ImageSaver inner class
    private class ImageSaver implements Runnable {
        /**
         * The JPEG image
         */
        private Image mImage;
        /**
         * The file we save the image into.
         */
        private File mFile;

        public ImageSaver(Image image) {
            mImage = image;
            mFile = new File(mFileUri.getEncodedPath());
            mFile.deleteOnExit();
        }

        @Override
        public void run() {
            Log.i("King", "NewCameraTakeFragment ImageSaver save image to " + mFile.getAbsolutePath());
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile, false);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }

    }
}
