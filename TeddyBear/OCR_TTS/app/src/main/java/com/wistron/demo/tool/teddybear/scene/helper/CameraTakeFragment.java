package com.wistron.demo.tool.teddybear.scene.helper;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.R;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CameraTakeFragment extends Fragment {
    private final int MSG_CLEAR_COUNTER = 0;
    private final int MSG_UPDATE_COUNTER = 1;

    private Uri mFile;

    private Camera mCamera;
    private SurfaceView mCameraSurfaceView;
    private SurfaceHolder mHolder;
    private ImageView mThumbnailView;
    private TextView mTakeCounter;

    private final int COUNTER_VALUE = 6;
    private int mCounterIndex = 0;

    public CameraTakeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
                && !getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)
                && !getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            Toast.makeText(getActivity(), "There is no Camera!", Toast.LENGTH_SHORT).show();
            getActivity().finish();
            return;
        }
        mFile = getArguments().getParcelable(MediaStore.EXTRA_OUTPUT);

        handler.postDelayed(updateTimerRunnable, 1000);
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            mCounterIndex++;
            if (mCounterIndex >= COUNTER_VALUE) {
                handler.sendEmptyMessage(MSG_CLEAR_COUNTER);
                if (mCamera != null) {
                    mCamera.takePicture(null, null, null, takePictureCallback);
                }
            } else {
                handler.sendEmptyMessage(MSG_UPDATE_COUNTER);
                handler.postDelayed(this, 1000);
            }
        }
    };

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
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimerRunnable);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera_take, container, false);
        mCameraSurfaceView = (SurfaceView) view.findViewById(R.id.camera_take_preview);
        mHolder = mCameraSurfaceView.getHolder();
        mHolder.addCallback(callback);
        mThumbnailView = (ImageView) view.findViewById(R.id.camera_take_thumbnail);
        mTakeCounter = (TextView) view.findViewById(R.id.camera_take_counter);
        return view;
    }

    private void resetCamera() {
        // TODO Auto-generated method stub
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (Camera.getNumberOfCameras() <= 1) {
                    mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                } else {
                    mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();

                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mCamera == null || mHolder.getSurface() == null) {
                // preview surface does not exist
                return;
            }

            Camera.Parameters mParameters = mCamera.getParameters();
            List<String> mFocusMode = mParameters.getSupportedFocusModes();
            if (mFocusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            List<Size> mPictureSizes = mParameters.getSupportedPictureSizes();
            if (mPictureSizes.size() > 0) {
                Size savedSize = mPictureSizes.get(mPictureSizes.size() / 2);
                mParameters.setPictureSize(savedSize.width, savedSize.height);
                mCamera.setParameters(mParameters);
            }

            // display options
            int mDisplayRotate = 0;
            CameraInfo mCameraInfo = new CameraInfo();
            Camera.getCameraInfo(CameraInfo.CAMERA_FACING_BACK, mCameraInfo);
            int mScreenOrientation = getResources().getConfiguration().orientation;
            int mDisplayOrientation = getActivity().getWindowManager().getDefaultDisplay().getRotation() * 90;
            int mCameraOrientation = mCameraInfo.orientation;
            Log.i("WisCamera---rotation", "CameraOrientation: " + mCameraOrientation + "screenOrientation: " + mScreenOrientation + ",displayOrientation: " + mDisplayOrientation);
            if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                mDisplayRotate = (mCameraOrientation - mDisplayOrientation + 360) % 360;
            } else {
                mDisplayRotate = (mCameraOrientation + mDisplayOrientation) % 360;
                mDisplayRotate = (360 - mDisplayRotate) % 360;
            }
            mCamera.setDisplayOrientation(mDisplayRotate);
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            resetCamera();
        }
    };

    public Camera.PictureCallback takePictureCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            // thumbnail
            //Bitmap mThumbnailBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeByteArray(data,0,data.length), 100, 100, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
            //mThumbnailView.setImageBitmap(mThumbnailBitmap);

            // save picture
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) { // Portrait screen
                Matrix matrix = new Matrix();
                matrix.postRotate(270);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }

            Log.i("King", "encoding path : " + mFile.getEncodedPath());
            try {
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(mFile.getEncodedPath()));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (bitmap != null) {
                bitmap.recycle();
            }

            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    };
}
