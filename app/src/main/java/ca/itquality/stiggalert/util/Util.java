package ca.itquality.stiggalert.util;

import android.util.Log;

/**
 * Helper class.
 */
public class Util {

    private static final String LOG_TAG = "StiggAlertDebug";

    /**
     * Adds a message to LogCat.
     */
    public static void Log(Object text) {
        Log.d(LOG_TAG, text + "");
    }
}
