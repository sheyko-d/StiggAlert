package ca.itquality.stiggalert.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import ca.itquality.stiggalert.app.MyApplication;
import ca.itquality.stiggalert.main.data.User;

/**
 * Helper class.
 */
public class Util {

    private static final String LOG_TAG = "StiggAlertDebug";
    private static final String PREF_USER = "User";

    /**
     * Adds a message to LogCat.
     */
    public static void Log(Object text) {
        Log.d(LOG_TAG, text + "");
    }

    @SuppressLint("HardwareIds")
    public static User getDefaultUser() {
        String androidId = Settings.Secure.getString(MyApplication.getContext()
                .getContentResolver(), Settings.Secure.ANDROID_ID);
        String deviceName = Build.MODEL;
        return new User(androidId, deviceName, null);
    }

    public static User getUser() {
        String userTxt = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext())
                .getString(PREF_USER, null);
        if (!TextUtils.isEmpty(userTxt)){
            return new Gson().fromJson(userTxt, User.class);
        } else {
            return null;
        }
    }

    public static void setUser(User user) {
        PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext()).edit()
                .putString(PREF_USER, new Gson().toJson(user)).apply();
    }
}
