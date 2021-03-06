package ca.itquality.stiggalert.main;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.Bind;
import butterknife.ButterKnife;
import ca.itquality.stiggalert.R;
import ca.itquality.stiggalert.api.ApiClient;
import ca.itquality.stiggalert.api.ApiInterface;
import ca.itquality.stiggalert.app.MyApplication;
import ca.itquality.stiggalert.main.data.User;
import ca.itquality.stiggalert.settings.SettingsActivity;
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


@SuppressWarnings("deprecation")
public class MainActivity extends SensorsActivity {

    public static final String SENSITIVITY_UPDATED_INTENT
            = "ca.itquality.stiggalert.SENSITIVITY_UPDATED";
    public static final String NICKNAME_UPDATED_INTENT
            = "ca.itquality.stiggalert.NICKNAME_UPDATED";
    public static final String SURVEILLANCE_TOGGLED_INTENT
            = "ca.itquality.stiggalert.SURVEILLANCE_TOGGLED";
    public static final String EXTRA_ENABLED = "Enabled";

    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.main_preview_layout)
    ViewGroup mPreviewLayout;
    @Bind(R.id.main_surveillance_btn)
    Button mSurveillanceBtn;
    @Bind(R.id.main_alert_img)
    ImageView mAlertImg;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;
    private static volatile AtomicBoolean processing = new AtomicBoolean(false);

    private static SurfaceHolder previewHolder = null;
    private static Camera camera = null;
    private static boolean inPreview = false;
    private static long referenceTime;
    private static IMotionDetection detector = null;
    private static int orientation;
    private SurfaceView mPreview;
    private boolean mSurveillanceActive = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initActionBar();
        register();
        refreshToken();
        setProfileChangeListener();
    }

    private void setProfileChangeListener() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SENSITIVITY_UPDATED_INTENT);
        intentFilter.addAction(NICKNAME_UPDATED_INTENT);
        intentFilter.addAction(SURVEILLANCE_TOGGLED_INTENT);
        registerReceiver(mProfileChangeReceiver, intentFilter);
    }

    private BroadcastReceiver mProfileChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SENSITIVITY_UPDATED_INTENT)) {
                //noinspection ConstantConditions
                detector = new RgbMotionDetection(Util.getUser().getSensitivity());
            } else if (intent.getAction().equals(NICKNAME_UPDATED_INTENT)) {
                updateTitle();
            } else if (intent.getAction().equals(SURVEILLANCE_TOGGLED_INTENT)) {
                if (intent.getStringExtra(EXTRA_ENABLED).equals("true")) {
                    stopCamera();
                } else {
                    startCamera();
                }
            }
        }
    };

    private void updateTitle() {
        User user = Util.getUser();
        if (user != null) {
            setTitle(TextUtils.isEmpty(user.getNickname()) ? getString(R.string.app_name)
                    : user.getNickname());
        } else {
            setTitle(R.string.app_name);
        }
    }

    private void updateProfile() {
        User user = Util.getUser();
        if (user != null) {
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

            Call<User> call = apiService.getProfile(user.getAndroidId());
            call.enqueue(new Callback<User>() {
                @Override
                public void onResponse(Call<User> call, Response<User> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Util.setUser(response.body());
                        updateTitle();
                        //noinspection ConstantConditions
                        detector = new RgbMotionDetection(Util.getUser().getSensitivity());
                    }
                }

                @Override
                public void onFailure(Call<User> call, Throwable t) {
                    Util.Log("Server error: " + t.getMessage());
                }
            });
        }
    }

    private void refreshToken() {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

        User user = Util.getUser();
        if (user != null) {
            Call<Void> call = apiService.updateToken(user.getAndroidId(),
                    FirebaseInstanceId.getInstance().getToken());
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Util.Log("Server error: " + t.getMessage());
                }
            });
        }
    }

    private void updateSurveillanceServerState() {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

        User user = Util.getUser();
        if (user != null) {
            Call<Void> call = apiService.updateSurveillance(user.getAndroidId(),
                    mSurveillanceActive);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Util.Log("Server error: " + t.getMessage());
                }
            });
        }
    }

    private void initActionBar() {
        setSupportActionBar(mToolbar);
    }

    private void register() {
        if (Util.getUser() == null) {
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

            Call<Void> call = apiService.register(Util.getDefaultUser());
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Util.setUser(Util.getDefaultUser());
                        startCamera();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Util.Log("Server error: " + t.getMessage());
                }
            });
        }
    }

    private void startCamera() {
        if (Util.getUser() == null) return;

        mSurveillanceActive = true;
        updateSurveillanceUiState();
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
                    camera = Camera.open();
                }
            }
        }
    }

    private void initCamera() {
        referenceTime = System.currentTimeMillis();

        try {
            camera = Camera.open();
        } catch (Exception e) {
            Util.Log("Can't open camera: " + e);
        }

        mPreview = new SurfaceView(this);
        mPreview.setKeepScreenOn(true);
        mPreviewLayout.addView(mPreview);
        previewHolder = mPreview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (Preferences.USE_RGB) {
            //noinspection ConstantConditions
            detector = new RgbMotionDetection(Util.getUser().getSensitivity());
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
        stopCamera();
        try {
            unregisterReceiver(mProfileChangeReceiver);
        } catch (Exception e) {
            // Receiver not registered
        }
    }

    private void stopCamera() {
        try {
            mSurveillanceActive = false;
            updateSurveillanceUiState();
            mAlertImg.setVisibility(View.GONE);
            mPreviewLayout.removeView(mPreview);
            camera.setPreviewCallback(null);
            if (inPreview) camera.stopPreview();
            inPreview = false;
            camera.release();
            camera = null;
        } catch (Exception e) {
            // Camera wasn't started yet
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        startCamera();
        updateProfile();
        updateTitle();
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
                DetectionThread thread = new DetectionThread(data, size.width, size.height,
                        mAlertImg, MainActivity.this);
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

    public void onSurveillanceButtonClicked(View view) {
        if (mSurveillanceActive) {
            stopCamera();
        } else {
            startCamera();
        }
        updateSurveillanceUiState();
    }

    private void updateSurveillanceUiState() {
        mSurveillanceBtn.setText(getString(mSurveillanceActive ? R.string.mail_stop
                : R.string.mail_start));
        updateSurveillanceServerState();
    }

    private static final class DetectionThread extends Thread {

        private byte[] data;
        private int width;
        private int height;
        private ImageView alertImg;
        private Activity activity;

        DetectionThread(byte[] data, int width, int height, ImageView alertImg, Activity activity) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.alertImg = alertImg;
            this.activity = activity;
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
                    setAlertImgVisible(true);

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
                        new SavePhotoTask().execute(previous, original, bitmap);
                    } else {
                        if (GlobalData.isPhoneInMotion()) return;
                    }
                } else {
                    setAlertImgVisible(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                processing.set(false);
            }
            // Log.d(TAG, "END PROCESSING...");

            processing.set(false);
        }

        private void setAlertImgVisible(final boolean visible) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    alertImg.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            });
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

        private void uploadPhotoToServer(String photoPath) {
            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

            MultipartBody.Part photo = getPhoto(photoPath);

            Call<Void> call = apiService.uploadPhoto(Util.getUser().getAndroidId(), photo);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), 1);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateSensitivityOnServer();
    }

    private void updateSensitivityOnServer() {
        User user = Util.getUser();
        if (user != null) {
            user.setSensitivity(PreferenceManager.getDefaultSharedPreferences(MyApplication
                    .getContext()).getInt("setting_sensitivity", 0));
            Util.setUser(user);

            ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
            Call<Void> call = apiService.updateSensitivity(user.getAndroidId(),
                    user.getSensitivityPercent());
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    Util.Log("Updated sensitivity");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Util.Log("Server error: " + t.getMessage());
                }
            });
        }
    }
}