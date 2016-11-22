package ca.itquality.stiggalert.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;

import ca.itquality.stiggalert.app.MyApplication;
import ca.itquality.stiggalert.util.Util;
import ca.itquality.stiggalert.util.motion_detection.detection.IMotionDetection;
import ca.itquality.stiggalert.util.motion_detection.detection.RgbMotionDetection;
import ca.itquality.stiggalert.util.motion_detection.image.ImageProcessing;

@SuppressLint("ViewConstructor")
@SuppressWarnings("deprecation")
public class ImageSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;

    public ImageSurfaceView(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            setAutoFocus();
            setCameraDisplayOrientation(Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] bytes, Camera camera) {
                    int[] rgb = ImageProcessing.decodeYUV420SPtoRGB(bytes, 100, 100);
                    IMotionDetection detector = new RgbMotionDetection();
                    boolean detected = detector.detect(rgb, 100, 100);
                    Util.Log("Detected: " + detected);
                }
            });
            mCamera.startPreview();
        } catch (IOException e) {
            Util.Log("Can't init mCamera: " + e);
        }
    }

    private void setAutoFocus() {
        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);
    }

    private void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        WindowManager windowService = (WindowManager) MyApplication.getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowService.getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.mCamera.stopPreview();
        this.mCamera.release();
    }
}