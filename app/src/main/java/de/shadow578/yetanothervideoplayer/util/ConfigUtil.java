package de.shadow578.yetanothervideoplayer.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * Utility class for configuration an app preferences
 */
public class ConfigUtil
{
    /**
     * Get the shared preferences for the application
     *
     * @param ctx the context for the application preferences
     * @return the app preferences
     */
    public static SharedPreferences getAppConfig(Context ctx)
    {
        return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
    }

    /**
     * Get a boolean from app preferences
     *
     * @param key   the key of the value
     * @param defId the id of the default value in R.bool
     * @param ctx   the context to get the config value with
     * @return the boolean value
     */
    public static boolean getConfigBoolean(Context ctx, String key, int defId)
    {
        boolean def = ctx.getResources().getBoolean(defId);
        boolean value = getAppConfig(ctx).getBoolean(key, def);

        //log the value
        Logging.logD("getPrefBool(): key= %s; val= %b", key, value);
        return value;
    }

    /**
     * Get a int from app preferences
     *
     * @param key   the key of the value
     * @param defId the id of the default value in R.integer
     * @param ctx   the context to get the config value with
     * @return the int value
     */
    public static int getConfigInt(Context ctx, String key, int defId)
    {
        int def = ctx.getResources().getInteger(defId);

        //read value as string: workaround needed because i'm using a EditText to change these values in the settings activity, which
        //changes the type of the preference to string...
        int value = Integer.valueOf(getAppConfig(ctx).getString(key, "" + def));

        //log the value
        Logging.logD("getPrefInt(): key= %s; val= %d", key, value);
        return value;
    }

    /**
     * Set a boolean in the app preferences
     *
     * @param ctx   the context to get the config value with
     * @param key   the key of the value
     * @param value the value to set
     */
    public static void setConfigBoolean(Context ctx, String key, boolean value)
    {
        //get app preferences
        SharedPreferences prefs = getAppConfig(ctx);

        //check prefs are ok
        if (prefs == null)
        {
            Logging.logD("cannot save playback position: ConfigUtil.getAppConfig() returned null!");
            return;
        }

        //set the value and save them
        prefs.edit().putBoolean(key, value).apply();
        Logging.logD("Set boolean key %s to %b", key, value);
    }

    /**
     * Set a integer in the app preferences
     *
     * @param ctx   the context to get the config value with
     * @param key   the key of the value
     * @param value the value to set
     */
    public static void setConfigInt(Context ctx, String key, int value)
    {
        //get app preferences
        SharedPreferences prefs = getAppConfig(ctx);

        //check prefs are ok
        if (prefs == null)
        {
            Logging.logD("cannot save playback position: ConfigUtil.getAppConfig() returned null!");
            return;
        }

        //set the value and save them
        prefs.edit().putInt(key, value).apply();
        Logging.logD("Set int key %s to %d", key, value);
    }
}
