package ca.itquality.stiggalert.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.itquality.stiggalert.R;
import ca.itquality.stiggalert.api.ApiClient;
import ca.itquality.stiggalert.api.ApiInterface;
import ca.itquality.stiggalert.app.MyApplication;
import ca.itquality.stiggalert.util.Util;
import ca.itquality.stiggalert.util.motion_detection.data.GlobalData;
import ca.itquality.stiggalert.util.motion_detection.data.Preferences;
import ca.itquality.stiggalert.util.motion_detection.detection.AggregateLumaMotionDetection;
import ca.itquality.stiggalert.util.motion_detection.detection.IMotionDetection;
import ca.itquality.stiggalert.util.motion_detection.detection.LumaMotionDetection;
import ca.itquality.stiggalert.util.motion_detection.detection.RgbMotionDetection;
import ca.itquality.stiggalert.util.motion_detection.image.ImageProcessing;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * This class extends Activity to handle a picture preview, process the frame
 * for motion, and then save the file to the SD card.
 *
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
@SuppressWarnings("deprecation")
public class MainActivity extends SensorsActivity {

    private static final String TAG = "StiggAlertDebug";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private static volatile AtomicBoolean processing = new AtomicBoolean(false);

    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static boolean inPreview = false;
    private static long referenceTime = 0;
    private static IMotionDetection detector = null;
    private static int orientation;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestCameraPermission();

        getOrientation();
    }

    private void getOrientation() {
        orientation = getResources().getConfiguration().orientation;
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            initCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0]
                        == PackageManager.PERMISSION_GRANTED) {
                    initCamera();
                }
            }
        }
    }

    private void initCamera() {
        SurfaceView preview = (SurfaceView) findViewById(R.id.preview);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (Preferences.USE_RGB) {
            detector = new RgbMotionDetection();
        } else if (Preferences.USE_LUMA) {
            detector = new LumaMotionDetection();
        } else {
            // Using State based (aggregate map)
            detector = new AggregateLumaMotionDetection();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        super.onPause();

        camera.setPreviewCallback(null);
        if (inPreview) camera.stopPreview();
        inPreview = false;
        camera.release();
        camera = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            camera = Camera.open();
        }
    }

    private PreviewCallback previewCallback = new PreviewCallback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            if (size == null) return;

            if (!GlobalData.isPhoneInMotion()) {
                DetectionThread thread = new DetectionThread(data, size.width, size.height);
                thread.start();
            }
        }
    };

    private SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                setAutoFocus();
                setCameraDisplayOrientation(Camera.CameraInfo.CAMERA_FACING_BACK, camera);
                camera.setPreviewDisplay(previewHolder);
                camera.setPreviewCallback(previewCallback);
            } catch (Throwable t) {
                Util.Log("Exception in setPreviewDisplay: " + t);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (!inPreview) {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = getBestPreviewSize(width, height, parameters);
                if (size != null) {
                    parameters.setPreviewSize(size.width, size.height);
                }
                camera.setParameters(parameters);
                camera.startPreview();
                inPreview = true;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Ignore
        }
    };

    private void setAutoFocus() {
        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
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

    private static Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) result = size;
                }
            }
        }

        return result;
    }

    private static final class DetectionThread extends Thread {

        private byte[] data;
        private int width;
        private int height;

        DetectionThread(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            if (!processing.compareAndSet(false, true)) return;

            // Log.d(TAG, "BEGIN PROCESSING...");
            try {
                // Previous frame
                int[] pre = null;
                if (Preferences.SAVE_PREVIOUS) pre = detector.getPrevious();

                // Current frame (with changes)
                // long bConversion = System.currentTimeMillis();
                int[] img;
                if (Preferences.USE_RGB) {
                    img = ImageProcessing.decodeYUV420SPtoRGB(data, width, height);
                } else {
                    img = ImageProcessing.decodeYUV420SPtoLuma(data, width, height);
                }
                // long aConversion = System.currentTimeMillis();
                // Log.d(TAG, "Converstion="+(aConversion-bConversion));

                // Current frame (without changes)
                int[] org = null;
                if (Preferences.SAVE_ORIGINAL && img != null) org = img.clone();

                if (img != null && detector.detect(img, width, height)) {
                    // The delay is necessary to avoid taking a picture while in
                    // the
                    // middle of taking another. This problem can causes some
                    // phones
                    // to reboot.
                    long now = System.currentTimeMillis();
                    if (now > (referenceTime + Preferences.PICTURE_DELAY)) {
                        referenceTime = now;

                        Bitmap previous = null;
                        if (Preferences.SAVE_PREVIOUS && pre != null) {
                            if (Preferences.USE_RGB)
                                previous = ImageProcessing.rgbToBitmap(pre, width, height,
                                        orientation);
                            else previous = ImageProcessing.lumaToGreyscale(pre, width, height);
                        }

                        Bitmap original = null;
                        if (Preferences.SAVE_ORIGINAL && org != null) {
                            if (Preferences.USE_RGB)
                                original = ImageProcessing.rgbToBitmap(org, width, height,
                                        orientation);
                            else original = ImageProcessing.lumaToGreyscale(org, width, height);
                        }

                        Bitmap bitmap = null;
                        if (Preferences.SAVE_CHANGES) {
                            if (Preferences.USE_RGB)
                                bitmap = ImageProcessing.rgbToBitmap(img, width, height,
                                        orientation);
                            else bitmap = ImageProcessing.lumaToGreyscale(img, width, height);
                        }

                        if (GlobalData.isPhoneInMotion()) return;

                        Looper.prepare();
                        Util.Log("Take picture");
                        new SavePhotoTask().execute(previous, original, bitmap);
                    } else {
                        if (GlobalData.isPhoneInMotion()) return;

                        Log.i(TAG, "Not taking picture because not enough time has passed since the creation of the Surface");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                processing.set(false);
            }
            // Log.d(TAG, "END PROCESSING...");

            processing.set(false);
        }
    }

    private static final class SavePhotoTask extends AsyncTask<Bitmap, Integer, Integer> {

        /**
         * {@inheritDoc}
         */
        @Override
        protected Integer doInBackground(Bitmap... data) {
            for (Bitmap bitmap : data) {
                String name = String.valueOf(System.currentTimeMillis());
                if (bitmap != null) save(name, bitmap);
            }
            return 1;
        }

        private void save(String name, Bitmap bitmap) {
            File photo = new File(Environment.getExternalStorageDirectory(), name + ".jpg");
            if (photo.exists()) photo.delete();

            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                fos.close();
            } catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            uploadPhotoToServer(photo.getAbsolutePath());
        }

        @SuppressLint("HardwareIds")
        private void uploadPhotoToServer(String photoPath) {
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

            MultipartBody.Part photo = getPhoto(photoPath);

            String androidId = Settings.Secure.getString(MyApplication.getContext()
                    .getContentResolver(), Settings.Secure.ANDROID_ID);
            Call<Void> call = apiService.uploadPhoto(androidId, photo);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Util.Log("uploaded");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Util.Log("Server error: " + t.getMessage());
                }
            });
        }

        private MultipartBody.Part getPhoto(String photoPath) {
            File file = new File(photoPath);
            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"),
                    file);
            return MultipartBody.Part.createFormData("photo", file.getName(),
                    requestFile);
        }
    }
}