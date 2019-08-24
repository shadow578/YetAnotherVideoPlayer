package de.shadow578.yetanothervideoplayer.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;

public class VideoTestActivity extends AppCompatActivity
{
    //playback uri, for testing the player
    final String PLAYBACK_URI = "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_test);

        //write small log message
        Logging.logD("Launched VideoTestActivity, loading PlaybackActivity with sample video url now...");

        //open playback activity with sample uri
        Intent vidPlayIntent = new Intent(this, PlaybackActivity.class);
        vidPlayIntent.setData(Uri.parse(PLAYBACK_URI));
        startActivity(vidPlayIntent);
    }
}
