package de.shadow578.yetanothervideoplayer.util;

import android.util.Log;

import java.util.Locale;

public final class Logging
{
    /**
     * The tag used for log messages
     */
    private static final String TAG = "YAVP";

    /**
     * Log a Debug/Verbose message
     *
     * @param msg    the message to log
     * @param format formatting options for the message
     */
    public static void logD(String msg, Object... format)
    {
        Log.d(TAG, String.format(Locale.US, msg, format));
    }

    /**
     * Log a Warning message
     *
     * @param msg    the message to log
     * @param format formatting options for the message
     */
    public static void logW(String msg, Object... format)
    {
        Log.w(TAG, String.format(Locale.US, msg, format));
    }

    /**
     * Log a Error message
     *
     * @param msg    the message to log
     * @param format formatting options for the message
     */
    public static void logE(String msg, Object... format)
    {
        Log.e(TAG, String.format(Locale.US, msg, format));
    }
}
