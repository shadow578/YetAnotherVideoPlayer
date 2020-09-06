package de.shadow578.yetanothervideoplayer.feature.streamfixes;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.annotation.Nullable;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * fixes stream requests from 4anime to (4animu me) since they use nginx secure url with the user agent
 * This is totally *NOT* a hack :P
 */
public class FourAnimeFix implements IStreamRequestFix
{
    /**
     * video url host for basic matching
     */
    private static final String VIDEO_URL_HOST = "4animu.me";

    /**
     * Matches the video url of 4animu including md5 and expires parameters
     * CG1 is the show name (eg. Uzaki-chan-wa-Asobitai!)
     * CG2 is the video name (show + episode + quality; eg. Uzaki-chan-wa-Asobitai!-Episode-08-1080p.mp4)
     * CG3 and CG4 are either md5 or expires url parameter (eg. md5=RbaIYnBTlD1Y7oMgkG0F5Q or expires=1599404106)
     */
    private static final String HTML_VIDEO_URL_REGEX = "https?:\\/\\/.*4animu\\.me\\/(.*)\\/(.*)\\?(md5=.{22}|expires=[0-9]{10})&(md5=[A-Za-z0-9]{22}|expires=[0-9]{10})";

    /**
     * Matches the video name (CG2 of HTML_VIDEO_URL_REGEX) and captures the PURE episode identifier in it.
     * CG1 is episode identifier (eg. Uzaki-chan-wa-Asobitai!-Episode-08)
     * <p>
     * -> remove invalid chars of EPISODE_ID_ILLEGAL_CHARS to get id for EPISODE_PAGE_BASE
     */
    private static final String EPISODE_ID_REGEX = "([A-Za-z0-9_!\\-]*-Episode-[0-9]{2,3})-[0-9]{3,4}p.";

    /**
     * illegal chars to be removed in the raw episode id returned by EPISODE_ID_REGEX CG1.
     */
    private static final String[] EPISODE_ID_ILLEGAL_CHARS = {"!", "_"};

    /**
     * page host of the html episode page.
     * append episode identifier to get full url
     */
    private static final String EPISODE_PAGE_BASE = "https://4anime.to/";

    /**
     * @return the display name of the fix
     */
    public String getDisplayName()
    {
        return "4Anime nginx secure_url";
    }

    /**
     * @return what permissions does this fix need?
     */
    @Override
    public String[] getPermissionsNeeded()
    {
        return new String[]{Manifest.permission.INTERNET};
    }

    /**
     * fix the url to work with the user agent.
     *
     * @param url       the stream url to fix
     * @param userAgent the user agent that will be used for streaming
     * @return the fixed url
     */
    @Override
    public String fixUrl(String url, String userAgent)
    {
        //execute async task
        FourAnimeFixTask.Parameters params = new FourAnimeFixTask.Parameters();
        params.originalUrl = url;
        params.userAgent = userAgent;
        AsyncTask<FourAnimeFixTask.Parameters, Void, String> task = new FourAnimeFixTask().execute(params);

        try
        {
            //Get fixed url
            String fixedUrl = task.get();

            //return the fixed url
            Logging.logD("url= %s ;; fixedUrl= %s", url, fixedUrl);
            return fixedUrl;
        }
        catch (ExecutionException | InterruptedException ex)
        {
            Logging.logE("exception while running FourAnimeFixTask: %s", ex.toString());
            ex.printStackTrace();
            return url;
        }
    }

    /**
     * should fixUrl be called for this url? (matches pattern, fix enabled, ...)
     *
     * @param url   the stream url to check
     * @param prefs global app preferences
     * @return should fixUrl be called?
     */
    @Override
    public boolean shouldFix(String url, SharedPreferences prefs)
    {
        //check if this fix is enabled in config
        boolean isEnabled = prefs.getBoolean(ConfigKeys.KEY_ENABLE_4ANIME_FIX, true);

        //do basic check of url
        boolean isCorrectHost = url.toLowerCase().contains(VIDEO_URL_HOST);

        //is enabled and has correct host?
        return isEnabled && isCorrectHost;
    }

    /**
     * async task that handles fixing 4anime video urls
     */
    private static class FourAnimeFixTask extends AsyncTask<FourAnimeFixTask.Parameters, Void, String>
    {
        /**
         * parameters for the task
         */
        private static class Parameters
        {
            public String originalUrl;
            public String userAgent;
        }

        /**
         * OkHttp client instance
         */
        private final OkHttpClient httpClient = new OkHttpClient();

        @Override
        protected String doInBackground(Parameters... params)
        {
            //get params
            if (params.length <= 0 || params[0] == null)
            {
                Logging.logE("no parameters supplied for FourAnimeFixTask!");
                return null;
            }
            Parameters p = params[0];

            //check with HTML_VIDEO_URL regex to check if url is from 4animu.me
            Logging.logD("trying to fix video url= %s", p.originalUrl);
            Pattern vUrlPattern = Pattern.compile(HTML_VIDEO_URL_REGEX);
            Matcher vUrlMatcher = vUrlPattern.matcher(p.originalUrl);

            //abort if is no match (not 4animu)
            if (!vUrlMatcher.matches())
            {
                Logging.logD("skip fixing url= %s: not 4anime url!", p.originalUrl);
                return p.originalUrl;
            }

            //get capture groups (but dont care about md5 and expires)
            String fullUrl = vUrlMatcher.group(0);
            String showName = vUrlMatcher.group(1);
            String videoName = vUrlMatcher.group(2);

            //check all cg are not null
            if (fullUrl == null || fullUrl.isEmpty()
                    || showName == null || showName.isEmpty()
                    || videoName == null || videoName.isEmpty())
            {
                Logging.logE("at least one capture group of HTML_VIDEO_URL_REGES was null or empty! abort fix");
                return p.originalUrl;
            }

            //get url to episode page
            String episodePageUrl = getEpisodePageUrl(videoName);
            if (episodePageUrl == null || episodePageUrl.isEmpty())
            {
                Logging.logE("episode page url for videoName= %s was null or empty! abort fix", videoName);
                return p.originalUrl;
            }

            //get html body of episode page
            String episodePageBody = getPageBody(episodePageUrl, p.userAgent);
            if (episodePageBody == null || episodePageBody.isEmpty())
            {
                Logging.logE("episode page body for videoName= %s pageUrl= %s was null or empty! abort fix", videoName, episodePageUrl);
                return p.originalUrl;
            }

            //find correct url in episode page
            String fixedUrl = getVideoUrlFromBody(episodePageBody);
            if (fixedUrl == null || fixedUrl.isEmpty())
            {
                Logging.logE("fixed url for videoName= %s was null or empty. abort fix", videoName);
                return p.originalUrl;
            }

            return fixedUrl;
        }

        /**
         * get the episode page url using EPISODE_ID_REGEX and EPISODE_PAGE_BASE
         *
         * @param videoName the video name from CG2 of HTML_VIDEO_URL_REGEX
         * @return the episode page url. null if regex fails
         */
        @Nullable
        private String getEpisodePageUrl(String videoName)
        {
            //match video name with EPISODE_ID_REGEX
            Logging.logD("trying to get episode page url for videoName= %s", videoName);
            Pattern eIdPattern = Pattern.compile(EPISODE_ID_REGEX);
            Matcher eIdMatcher = eIdPattern.matcher(videoName);

            //abort if no match
            if (!eIdMatcher.find())
            {
                Logging.logW("could not match episode id for videoName= %s", videoName);
                return null;
            }

            //get raw episode id from CG1
            String eID = eIdMatcher.group(1);

            //abort if episode id is null
            if (eID == null || eID.isEmpty())
            {
                Logging.logE("episode id for videoName= %s is empty!", videoName);
                return null;
            }

            //replace spaces in raw eID with minus
            Logging.logD("raw episode id for videoName= %s parsed as eID= %s", videoName, eID);
            eID = eID.replace(' ', '-');

            //replace illegal chars in raw eID
            for (String illegal : EPISODE_ID_ILLEGAL_CHARS)
                eID = eID.replace(illegal, "");

            //episode id to lowercase
            eID = eID.toLowerCase();

            //build url
            Logging.logD("final episode id for videoName= %s is eID= %s", videoName, eID);
            return EPISODE_PAGE_BASE + eID;
        }

        /**
         * get the pages html from the given url. make request with the given user agent
         *
         * @param url       the url to get
         * @param userAgent the user agent to use
         * @return the response string (html)
         */
        @Nullable
        private String getPageBody(String url, String userAgent)
        {
            try
            {
                //build the request
                Request pageRequest = new Request.Builder()
                        .get()
                        .url(url)
                        .header("User-Agent", userAgent)
                        .build();

                //get response
                Response pageResponse = httpClient.newCall(pageRequest).execute();

                if (!pageResponse.isSuccessful())
                {
                    Logging.logW("response when getting page html for url= %s was not successfull! details: %s", url, pageResponse.toString());
                    return null;
                }
                else
                    return pageResponse.body().string();
            }
            catch (IOException ex)
            {
                Logging.logE("exception while getting page html for url= %s : %s", url, ex.toString());
                ex.printStackTrace();
                return null;
            }
        }

        /**
         * get the video url from the episode page body html
         *
         * @param episodePageBody the episode page html body
         * @return the video url, or null if match failed
         */
        @Nullable
        private String getVideoUrlFromBody(String episodePageBody)
        {
            //check with HTML_VIDEO_URL regex
            Pattern vUrlPattern = Pattern.compile(HTML_VIDEO_URL_REGEX);
            Matcher vUrlMatcher = vUrlPattern.matcher(episodePageBody);

            //find first match for video url
            if (!vUrlMatcher.find())
            {
                Logging.logW("no match for video url in episode body!");
                return null;
            }

            //url is CG0
            return vUrlMatcher.group(0);
        }
    }
}
