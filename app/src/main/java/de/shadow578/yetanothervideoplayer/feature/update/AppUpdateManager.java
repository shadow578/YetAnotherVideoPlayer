package de.shadow578.yetanothervideoplayer.feature.update;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Manages app updates using github releases api
 */
public class AppUpdateManager
{
    /**
     * formattable string for the github releases api url
     * %s: repo owner
     * %s: repo name
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final String GITHUB_RELEASES_API_URL_F = "https://api.github.com/repos/%s/%s/releases/latest";

    /**
     * Result of the {@link UpdateCallback#compareVersion(String)} function
     */
    public enum VersionComparison
    {
        /**
         * The version string checked is the same as the apps version (on latest)
         */
        SAME_VERSION,

        /**
         * The version string checked is newer than the apps version (behind latest)
         */
        NEWER_VERSION,

        /**
         * The version string checked is older than the apps version (ahead of latest)
         */
        OLDER_VERSION,

        /**
         * Given version string does not match the expected format
         */
        INVALID
    }

    /**
     * A class that can compare a version string to the current app version
     */
    public interface UpdateCallback
    {
        /**
         * Compare the given version string to the apps current version
         *
         * @param version the version string to check
         * @return the result of the comparison
         */
        VersionComparison compareVersion(String version);

        /**
         * Callback to {@link AppUpdateManager#checkForUpdate(UpdateCallback)} function.
         * Called after update check is done
         *
         * @param update the update to the latest version. Null if no newer version was found
         * @param failed did the update check fail?
         */
        void onUpdateCheckFinished(@Nullable UpdateInfo update, boolean failed);
    }

    /**
     * Name of the repository owner
     * (github.com/OWNER/REPO)
     * eg. shadow578
     */
    @NonNull
    private final String repoOwner;

    /**
     * Name of the repository name
     * (github.com/OWNER/REPO)
     * eg. yetanothervideoplayer
     */
    @NonNull
    private final String repoName;

    /**
     * Create a new Update Manager
     *
     * @param repoOwner owner of the github repository (github.com/OWNER/repo)
     * @param repoName  name of the github repository ((github.com/owner/REPO)
     */
    public AppUpdateManager(@NonNull String repoOwner, @NonNull String repoName)
    {
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    /**
     * Checks for updates and calls {@link UpdateCallback#onUpdateCheckFinished(UpdateInfo, boolean)} once it's done.
     *
     * @param callback callback to compare version strings to the current version
     */
    public void checkForUpdate(@NonNull UpdateCallback callback)
    {
        //get url
        URL updateUrl = getUpdateUrl();
        if (updateUrl == null)
        {
            //cannot create update url, consider update check failed
            callback.onUpdateCheckFinished(null, true);
            return;
        }

        //do update check async
        new UpdateCheckTask().execute(new UpdateCheckTask.Parameters(callback, getUpdateUrl()));
    }

    /**
     * @return the url to the github releases api endpoint for the latest release
     */
    private URL getUpdateUrl()
    {
        try
        {
            return new URL(String.format(GITHUB_RELEASES_API_URL_F, repoOwner, repoName));
        }
        catch (MalformedURLException e)
        {
            Logging.logE("Malformed update url!");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Async task for checking updates.
     */
    private static class UpdateCheckTask extends AsyncTask<UpdateCheckTask.Parameters, Void, JSONObject>
    {
        /**
         * Parameters for {@link UpdateCheckTask}
         */
        private static class Parameters
        {
            @NonNull
            private final UpdateCallback callback;

            @NonNull
            private final URL updateUrl;

            Parameters(@NonNull UpdateCallback callback, @NonNull URL updateUrl)
            {
                this.callback = callback;
                this.updateUrl = updateUrl;
            }
        }

        /**
         * Parameters for the task
         */
        private Parameters parameters;

        @Override
        protected JSONObject doInBackground(Parameters... params)
        {
            //get callback
            if (params.length <= 0 || params[0] == null)
            {
                Logging.logE("no callbacks supplied for UpdateCheckTask!");
                return null;
            }
            parameters = params[0];

            try
            {
                //open connection to github
                URLConnection apiConnection = parameters.updateUrl.openConnection();

                //get githubs response
                StringBuilder jsonResponse = new StringBuilder();
                try (BufferedReader responseReader = new BufferedReader(new InputStreamReader(apiConnection.getInputStream())))
                {
                    //read line by line
                    String ln;
                    while ((ln = responseReader.readLine()) != null)
                    {
                        jsonResponse.append(ln);
                    }
                }

                //parse json into a json object
                return new JSONObject(jsonResponse.toString());
            }
            catch (IOException | JSONException e)
            {
                //log error
                Logging.logE("Error while connecting to github and/or parsing json:");
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject json)
        {
            //check we were successful
            if (json == null)
            {
                Logging.logE("Update Check failed!");
                parameters.callback.onUpdateCheckFinished(null, true);
                return;
            }

            //parse update info from json
            try
            {
                //check if is latest release is draft (ignore if that is the case)
                boolean isDraft = json.getBoolean("draft");
                if (isDraft)
                {
                    parameters.callback.onUpdateCheckFinished(null, false);
                    return;
                }

                //get the latest update
                UpdateInfo update = parseUpdateJson(json);

                //check if update is newer than current version
                if (parameters.callback.compareVersion(update.getVersionTag()) != VersionComparison.NEWER_VERSION)
                {
                    parameters.callback.onUpdateCheckFinished(null, false);
                    return;
                }

                //finish update check
                parameters.callback.onUpdateCheckFinished(update, false);
            }
            catch (JSONException e)
            {
                Logging.logE("Error parsing upate json response!");
                e.printStackTrace();
                parameters.callback.onUpdateCheckFinished(null, true);
            }
        }

        /**
         * Parse a UpdateInfo from json response
         *
         * @param json the json to parse
         * @return the update info that was parsed
         * @throws JSONException thrown on json parse errors
         */
        private UpdateInfo parseUpdateJson(JSONObject json) throws JSONException
        {
/*
UpdateInfo:
versionTag   -> (string)    tag_name
updateTitle  -> (string)    name
updateDesc 	 -> (string)    body
webUrl 		 -> (string)    html_url
isPrerelease -> (bool)      prerelease
updateAssets -> (APKInfo[]) assets

APKInfo:
filename    -> (string) name
downloadUrl -> (string) browser_download_url
fileSize    -> (long)   size
*/

            //get info for UpdateInfo fields
            String tag = json.getString("tag_name");
            String title = json.getString("name");
            String desc = json.getString("body");
            String url = json.getString("html_url");
            boolean isPrerelease = json.getBoolean("prerelease");

            //get apk assets
            List<ApkInfo> apkAssets = new ArrayList<>();
            JSONArray assets = json.getJSONArray("assets");
            for (int i = 0; i < assets.length(); i++)
            {
                //get json values
                JSONObject asset = assets.getJSONObject(i);
                String filename = asset.getString("name");
                String dlUrl = asset.getString("browser_download_url");
                long fileSize = asset.getLong("size");

                //check the asset is a apk
                boolean isApk = asset.getString("content_type").equalsIgnoreCase("application/vnd.android.package-archive");
                if (!isApk) continue;

                //create and add apk info object
                apkAssets.add(new ApkInfo(filename, dlUrl, fileSize));
            }

            //create update info
            return new UpdateInfo(tag, title, desc, url, isPrerelease, apkAssets.toArray(new ApkInfo[0]));
        }
    }
}
