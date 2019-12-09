package de.shadow578.yetanothervideoplayer.ui;

import androidx.appcompat.app.AppCompatActivity;
import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class LaunchActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        Logging.logD("Launch Activity onCreate was called.");

        //get splash screen duration
        int splashDuration = 500;//TODO: hard coded duration, use the one in values (min_splash_screen_duration) instead

        //post event to start playback activity delayed
        Handler splashHandler = new Handler();
        splashHandler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                //get intent this activity was created with
                Intent callingIntent = getIntent();

                //create intent for launching playback activity
                Logging.logD("Calling Intent: " + callingIntent.toString());
                Intent playbackLaunchIntent = new Intent(getApplicationContext(), PlaybackActivity.class);
                playbackLaunchIntent.putExtras(callingIntent);
                playbackLaunchIntent.setData(callingIntent.getData());
                playbackLaunchIntent.setAction(callingIntent.getAction());

                Logging.logD("Launch Intent: " + playbackLaunchIntent.toString());

                //start the playback activity
                startActivity(playbackLaunchIntent);

                //close this activity as soon as playback activity closes
                finish();
            }
        }, splashDuration);
    }
}
