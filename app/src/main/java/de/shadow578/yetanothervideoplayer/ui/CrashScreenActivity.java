package de.shadow578.yetanothervideoplayer.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CrashScreenActivity extends AppCompatActivity
{
    // region Intent Extras

    public static final String INTENT_EXTRA_THREAD_NAME = "exThreadName";
    public static final String INTENT_EXTRA_CAUSE_SHORT = "exCauseShort";
    public static final String INTENT_EXTRA_CAUSE_MESSAGE = "exCauseMessage";
    public static final String INTENT_EXTRA_CAUSE_STACKTRACE = "exCauseStacktrace";

    // endregion

    /**
     * The Shared prefs of this app
     */
    SharedPreferences appPreferences;

    /**
     * The "resume playback" button
     */
    Button resumePlaybackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash_screen);

        //get app prefs
        appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //get views
        resumePlaybackButton = findViewById(R.id.crashac_btn_resume);

        //set resume button enable
        resumePlaybackButton.setEnabled(canResumePlayback());

        //get crash info
        Intent i = getIntent();
        if (i == null) return;

        //String crashThreadName = i.getStringExtra(INTENT_EXTRA_THREAD_NAME);
        //String crashCauseShort = i.getStringExtra(INTENT_EXTRA_CAUSE_SHORT);
        //String crashCauseMessage= i.getStringExtra(INTENT_EXTRA_CAUSE_MESSAGE);
        String crashCauseStack = i.getStringExtra(INTENT_EXTRA_CAUSE_STACKTRACE);

        //add crash info to activity
        TextView crashDetails = findViewById(R.id.crashac_txt_crash_details);
        if (crashDetails != null)
            crashDetails.setText(crashCauseStack);
    }

    public void crashac_OnClick(View view)
    {
        switch (view.getId())
        {
            case R.id.crashac_btn_close_app:
            {
                //close the app now
                finish();
                break;
            }
            case R.id.crashac_btn_resume:
            {
                //try to resume playback where we left off
                resumePlayback();
                break;
            }
        }
    }

    // region "resume where i left off"- feature

    /**
     * Resume playback of a video when a crash occured
     */
    private void resumePlayback()
    {
        //check if we can resume
        if (!canResumePlayback()) return;

        //get title and uri to resume
        String resumeTitle = getLastPlayedTitle();
        String resumeUrlStr = getLastPlayedUrl();

        //parse uri from stored string
        Uri resumeUri = Uri.parse(resumeUrlStr);
        if (resumeUri == null) return;

        //launch the launch activity (launch activity handles start position)
        Intent launchIntent = new Intent(this, LaunchActivity.class);
        launchIntent.setData(resumeUri);
        launchIntent.putExtra(Intent.EXTRA_TITLE, resumeTitle);

        //start the launcher and close
        startActivity(launchIntent);
        finish();
    }

    /**
     * Check if the player can resume playback with stored data
     *
     * @return can we resume the video?
     */
    private boolean canResumePlayback()
    {
        //check if we have a title and url set
        if (getLastPlayedUrl() == null || getLastPlayedTitle() == null) return false;

        //check if there is a playback position to resume stored
        return appPreferences.getLong(ConfigKeys.KEY_LAST_PLAYED_POSITION, -1) > 0;
    }

    /**
     * Get the last played url from shared prefs.
     */
    private String getLastPlayedUrl()
    {
        //get shared preferences
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //get value
        return appPreferences.getString(ConfigKeys.KEY_LAST_PLAYED_URL, null);
    }

    /**
     * Get the last played video title from shared prefs.
     */
    private String getLastPlayedTitle()
    {
        //get shared preferences
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //get value
        return appPreferences.getString(ConfigKeys.KEY_LAST_PLAYED_TITLE, null);
    }

    // endregion
}
