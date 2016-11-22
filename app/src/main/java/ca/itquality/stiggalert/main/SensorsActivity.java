package ca.itquality.stiggalert.main;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.itquality.stiggalert.util.motion_detection.data.GlobalData;


/**
 * This class extends Activity and processes sensor data and location data. It
 * is used to detect when the phone is in motion, so we do not try to detect
 * motion.
 *
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class SensorsActivity extends Activity implements SensorEventListener {

    private static final String TAG = "SensorsActivity";
    private static final AtomicBoolean computing = new AtomicBoolean(false);

    private static final float grav[] = new float[3]; // Gravity (a.k.a
    // accelerometer data)
    private static final float mag[] = new float[3]; // Magnetic

    private static final float gravThreshold = 0.5f;
    private static final int DELAY_AFTER_MOTION = 5 * 1000;

    private static SensorManager sensorMgr = null;
    private static List<Sensor> sensors = null;
    private static Sensor sensorGrav = null;

    private static float prevGrav = 0.0f;
    private long mLastTimeInMotion;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStart() {
        super.onStart();

        try {
            sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);

            sensors = sensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER);
            if (sensors.size() > 0) sensorGrav = sensors.get(0);

            sensorMgr.registerListener(this, sensorGrav, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception ex1) {
            try {
                if (sensorMgr != null) {
                    sensorMgr.unregisterListener(this, sensorGrav);
                    sensorMgr = null;
                }
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStop() {
        super.onStop();

        try {
            try {
                sensorMgr.unregisterListener(this, sensorGrav);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            sensorMgr = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSensorChanged(SensorEvent evt) {
        if (!computing.compareAndSet(false, true)) return;

        if (evt.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            grav[0] = evt.values[0];
            grav[1] = evt.values[1];
            grav[2] = evt.values[2];
        } else if (evt.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mag[0] = evt.values[0];
            mag[1] = evt.values[1];
            mag[2] = evt.values[2];
        }

        float gravity = grav[0] + grav[1] + grav[2];

        float gravityDiff = Math.abs(gravity - prevGrav);

        if ((Float.compare(prevGrav, 0.0f) != 0 && (gravityDiff
                > gravThreshold))) {
            GlobalData.setPhoneInMotion(true);
            mLastTimeInMotion = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - mLastTimeInMotion > DELAY_AFTER_MOTION) {
                GlobalData.setPhoneInMotion(false);
            }
        }

        prevGrav = gravity;

        computing.set(false);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
