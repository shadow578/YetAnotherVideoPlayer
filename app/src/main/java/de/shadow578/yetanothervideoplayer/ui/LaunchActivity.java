package de.shadow578.yetanothervideoplayer.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import de.shadow578.yetanothervideoplayer.BuildConfig;
import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.feature.update.AppUpdateManager;
import de.shadow578.yetanothervideoplayer.feature.update.DefaultUpdateCallback;
import de.shadow578.yetanothervideoplayer.feature.update.UpdateInfo;
import de.shadow578.yetanothervideoplayer.ui.mediapicker.MediaPickerActivity;
import de.shadow578.yetanothervideoplayer.ui.playback.PlaybackActivity;
import de.shadow578.yetanothervideoplayer.ui.update.UpdateHelper;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.ConfigUtil;
import de.shadow578.yetanothervideoplayer.util.Logging;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import java.util.Locale;

public class LaunchActivity extends AppCompatActivity
{
    /**
     * Intent Extra for the Launcher activity to skip launch delays, eg. use min_splash_screen_duration of 0 ms
     * (boolean extra)
     */
    public static final String EXTRA_LAUNCH_NO_DELAY = "launchNoDelay";

    /**
     * Update manager to check for updates
     */
    private final AppUpdateManager updateManager = new AppUpdateManager(BuildConfig.UPDATE_VENDOR, BuildConfig.UPDATE_REPO);

    /**
     * The Shared prefs of this app
     */
    private SharedPreferences appPreferences;

    /**
     * A handler that is used to post (delayed) messages and events
     */
    private Handler splashHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lauch_activity);
        Logging.logD("Launch Activity onCreate was called.");

        //get app prefs
        appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (shouldCheckUpdate())
        {
            checkUpdateAndContinueTo();
        }
        else
        {
            continueTo();
        }

        //continueTo();
    }

    /**
     * @return should we check for a update?
     */
    private boolean shouldCheckUpdate()
    {
        //skip if updates are disabled
        if (!ConfigUtil.getConfigBoolean(this, ConfigKeys.KEY_ENABLE_APP_UPDATES, R.bool.DEF_ENABLE_APP_UPDATES))
            return false;

        //get update check frequency
        int updateFrequency = getResources().getInteger(R.integer.update_check_freqency);

        //check if last update is old enough
        return new UpdateHelper(this).getTimeSinceLastUpdateCheck() >= updateFrequency;
    }

    /**
     * Check for a app update, then call continueTo() (if not updating)
     */
    private void checkUpdateAndContinueTo()
    {
        Toast.makeText(this, R.string.launch_update_check_toast, Toast.LENGTH_SHORT).show();
        final UpdateHelper updateHelper = new UpdateHelper(this);
        updateManager.checkForUpdate(new DefaultUpdateCallback()
        {
            @Override
            public void onUpdateCheckFinished(@Nullable UpdateInfo update, boolean failed)
            {
                //check if update check failed
                if (failed)
                {
                    //update failed, just continue on
                    continueTo();
                    return;
                }

                //save the current timestamp as last update check time in shared prefs
                updateHelper.updateTimeOfLastUpdateCheck();

                //check if no update was found
                if (update == null)
                {
                    //no update found, continue on
                    updateHelper.setUpdateAvailableFlag(false);
                    continueTo();
                    return;
                }

                //have a update, set persistent flag show update dialog
                //set a flag in shared prefs that we have a update, in case the user does not update right away
                updateHelper.setUpdateAvailableFlag(true);

                //show update dialog
                updateHelper.showUpdateDialog(update, new UpdateHelper.Callback()
                {
                    @Override
                    public void onUpdateFinished(boolean isUpdating)
                    {
                        if (!isUpdating) continueTo();
                    }
                }, false);
            }
        });
    }

    /**
     * Continue to launch the appropriate activity based on what is given in this activitys launch intent
     */
    private void continueTo()
    {
        //get splash screen duration
        int minSplashDuration = getResources().getInteger(R.integer.min_splash_screen_duration);

        //get intent that was used to create the activity
        final Intent launchIntent = getIntent();
        if (launchIntent.getBooleanExtra(EXTRA_LAUNCH_NO_DELAY, false))
        {
            minSplashDuration = 0;
        }

        //post event to start playback activity delayed
        splashHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                //check if the intent is ACTION_MAIN (called from launcher) or has no data (cannot play if we have no data ;))
                String action = launchIntent.getAction();
                if (action != null
                        && (action.equals(Intent.ACTION_VIEW) || action.equals(Intent.ACTION_SEND))
                        && launchIntent.getData() != null)
                {
                    //our target is playback (have data and ACTION_VIEW or ACTION_SEND)
                    continueToPlayback();
                }
                else
                {
                    //our target is the media picker (ACTION_MAIN or no data)
                    continueToMediaPicker();
                }
            }
        }, minSplashDuration);
    }

    /**
     * Continue to the media picker activity (Action.MAIN + Action.VIEW without data)
     */
    private void continueToMediaPicker()
    {
        //launch media picker
        Intent pickerIntent = new Intent(this, MediaPickerActivity.class);
        startActivity(pickerIntent);
        finish();
    }

    /**
     * Continue to the playback activity (Action.SEND and Action.VIEW)
     */
    private void continueToPlayback()
    {
        //launch the playback activity
        if (launchPlayback(getIntent()))
        {
            //launched ok, close this activity as soon as playback activity closes
            finish();
        }
        else
        {
            //launch failed, show error
            Toast.makeText(getApplicationContext(), "Could not launch Playback Activity!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Launch the playback activity with url and title set by the given intent's data
     * Intent is parsed and can be of type:
     * - ACTION_VIEW (open)
     * - ACTION_SEND (share)
     *
     * @param callingIntent the intent that opened the launch activity. used to parse playback url and title
     * @return if the playback activity was launched ok
     */
    private boolean launchPlayback(Intent callingIntent)
    {
        //dump the intent that called
        dumpIntent(callingIntent, "Calling Intent");

        //parse url and title
        Uri playbackUrl = parsePlaybackUrl(callingIntent);
        if (playbackUrl == null) return false;

        String title = parseTitle(playbackUrl, callingIntent);
        if (title.isEmpty()) return false;

        //construct intent for launching playback activity
        Intent launchIntent = new Intent(this, PlaybackActivity.class);
        launchIntent.setData(playbackUrl);
        launchIntent.putExtra(Intent.EXTRA_TITLE, title);

        //check if the video can be resumed
        if (canResumePlayback(playbackUrl, title))
        {
            Logging.logD("Putting INTENT_EXTRA_JUMP_TO because playback can be resumed.");
            launchIntent.putExtra(PlaybackActivity.INTENT_EXTRA_JUMP_TO, getResumePosition());
        }

        //dump launch intent
        dumpIntent(launchIntent, "Launch Intent");

        //save the playback url as last played
        updateLastPlayed(playbackUrl, title);

        //launch the playback activity
        startActivity(launchIntent);
        return true;
    }

    // region "resume where i left off"- feature

    /**
     * Save the current video as last played in shared prefs.
     *
     * @param url   the video url to save
     * @param title the video title to save
     */
    private void updateLastPlayed(Uri url, String title)
    {
        //set values
        appPreferences.edit().putString(ConfigKeys.KEY_LAST_PLAYED_URL, url.toString())
                .putString(ConfigKeys.KEY_LAST_PLAYED_TITLE, title).apply();
    }

    /**
     * Check if the player can resume playback with the given url and title
     * checks that either url or title match the stored previous video and that there is a position stored to resume at.
     *
     * @param url   the url of the video that is being loaded now
     * @param title the title of the video that is being loaded now
     * @return can we resume the video?
     */
    private boolean canResumePlayback(Uri url, String title)
    {
        //check if there is a playback position to resume stored
        if (appPreferences.getLong(ConfigKeys.KEY_LAST_PLAYED_POSITION, -1) <= 0) return false;

        //check that url or title is the same as the last played
        return url.toString().equalsIgnoreCase(appPreferences.getString(ConfigKeys.KEY_LAST_PLAYED_URL, ""))
                || title.equalsIgnoreCase(appPreferences.getString(ConfigKeys.KEY_LAST_PLAYED_TITLE, ""));
    }

    /**
     * Get the position at which the video should be resumed at
     *
     * @return the position to resume at
     */
    private long getResumePosition()
    {
        return appPreferences.getLong(ConfigKeys.KEY_LAST_PLAYED_POSITION, 0); //TODO: remove a few seconds (10s)
    }

    // endregion

    // region Intent Parsing

    /**
     * Dump the intents data to Logging.logd
     *
     * @param intent the intent to dump
     * @param desc   the description of the intent that is dumped
     */
    private void dumpIntent(Intent intent, String desc)
    {
        Logging.logD("========================================");
        Logging.logD("Dumping Intent " + desc);
        Logging.logD("%s of type %s", intent.toString(), intent.getType());
        Uri data = intent.getData();
        Logging.logD("Data: %s (%s)", (data == null) ? "null" : data.toString(), intent.getDataString());

        //dump extras
        Logging.logD("Extras: ");
        Bundle extras = intent.getExtras();
        if (extras != null)
        {
            for (String key : extras.keySet())
            {
                Logging.logD("   %s = %s", key, extras.get(key));
            }
        }
        else
        {
            Logging.logD("Intent has no extras.");
        }

        Logging.logD("========================================");
    }

    /**
     * Retrieve the Uri to play from a given intent
     *
     * @param intent the intent
     * @return the retried uri, or null if no uri was found
     */
    private Uri parsePlaybackUrl(Intent intent)
    {
        //log intent info
        Logging.logD("call Intent: %s", intent.toString());
        Bundle extra = intent.getExtras();
        if (extra != null)
        {
            Logging.logD("call Intent Extras: ");
            for (String key : extra.keySet())
            {
                Object val = extra.get(key);
                Logging.logD("\"%s\" : \"%s\"", key, (val == null ? "NULL" : val.toString()));
            }
        }

        //get playback uri from intent
        String action = intent.getAction();
        if (action == null || action.equalsIgnoreCase(Intent.ACTION_VIEW))
        {
            //action: open with OR directly open
            return intent.getData();
        }
        else if (action.equalsIgnoreCase(Intent.ACTION_SEND))
        {
            //action: send to
            String type = intent.getType();
            if (type == null) return null;

            if (type.equalsIgnoreCase("text/plain"))
            {
                //share a url from something like chrome, uri is in extra TEXT
                if (intent.hasExtra(Intent.EXTRA_TEXT))
                {
                    return Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT));
                }
            }
            else if (type.startsWith("video/")
                    || type.startsWith("audio/"))
            {
                //probably shared from gallery, uri is in extra STREAM
                if (intent.hasExtra(Intent.EXTRA_STREAM))
                {
                    return (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                }
            }

            //failed to parse
            return null;
        }
        else
        {
            //unknown
            Logging.logW("Received Intent with unknown action: %s", intent.getAction());
            return null;
        }
    }

    /**
     * Parse the name of the streamed file from the playback uri
     *
     * @param uri          the playback uri (fallback to filename)
     * @param invokeIntent the intent that invoked this activity (parse Title Extra)
     * @return the parsed file name, or null if parsing failed
     */
    private String parseTitle(@NonNull Uri uri, @NonNull Intent invokeIntent)
    {
        //prep title
        String title = null;

        //get intent extras
        Bundle extraData = invokeIntent.getExtras();
        if (extraData != null)
        {
            //try to get title from extras
            if (extraData.containsKey(Intent.EXTRA_TITLE))
            {
                //has default title extra, use that
                title = extraData.getString(Intent.EXTRA_TITLE);
                Logging.logD("Parsing title from default EXTRA_TITLE...");
            }
            else
            {
                //check each key if it contains "title" and has a String value that is not null or empty
                for (String key : extraData.keySet())
                {
                    if (key.toLowerCase(Locale.US).contains("title"))
                    {
                        //key contains "title" in some sort, get value
                        Object val = extraData.get(key);

                        //check if value is not null and a string
                        if (val instanceof String)
                        {
                            //convert value to string
                            String valStr = (String) val;

                            //check if string value is not empty
                            if (!valStr.isEmpty())
                            {
                                //could be our title, set it
                                title = valStr;
                                Logging.logD("Parsing title from non- default title extra (\"%s\" : \"%s\")", key, valStr);
                            }
                        }
                    }
                }
            }


            if (title != null) Logging.logD("parsed final title from extra: %s", title);
        }

        //check if got title from extras
        if (title == null || title.isEmpty())
        {
            //no title set yet, try to get the title using the last path segment of the uri
            title = uri.getLastPathSegment();
            if (title != null && !title.isEmpty() && title.indexOf('.') != -1)
            {
                //last path segment worked, remove file extension
                title = title.substring(0, title.lastIndexOf('.'));
                Logging.logD("parse title from uri: %s", title);
            }
        }

        //return title
        return title;
    }
    // endregion
}
