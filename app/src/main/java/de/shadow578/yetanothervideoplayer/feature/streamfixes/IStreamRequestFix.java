package de.shadow578.yetanothervideoplayer.feature.streamfixes;

import android.content.SharedPreferences;

/**
 * allows format / request fixes to urls. Input a (wrong) url, return the (fixed) url. simple as that ;)
 */
public interface IStreamRequestFix
{
    /**
     * fix the url to work with the user agent.
     *
     * @param url       the stream url to fix
     * @param userAgent the user agent that will be used for streaming
     * @return the fixed url
     */
    String fixUrl(String url, String userAgent);

    /**
     * should fixUrl be called for this url? (matches pattern, fix enabled, ...)
     *
     * @param url   the stream url to check
     * @param prefs global app preferences
     * @return should fixUrl be called?
     */
    boolean shouldFix(String url, SharedPreferences prefs);

    /**
     * @return the display name of the fix
     */
    String getDisplayName();

    /**
     * @return what permissions does this fix need?
     */
    String[] getPermissionsNeeded();
}
