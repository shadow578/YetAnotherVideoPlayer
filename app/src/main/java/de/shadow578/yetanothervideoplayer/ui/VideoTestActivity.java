package de.shadow578.yetanothervideoplayer.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;

public class VideoTestActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_test);
    }

    /**
     * Common click handler for buttons in VideoTest activity
     *
     * @param view the view that invoked this handler
     */
    public void onClick(View view)
    {
        //the uri that later contains the uri to stream from
        String uri;

        //select uri by button id
        switch (view.getId())
        {
            case R.id.vtest_btn_mp3:
            {
                //ExoPlayer test media: MP3
                uri = "http://storage.googleapis.com/exoplayer-test-media-0/play.mp3";
                break;
            }
            case R.id.vtest_btn_mp4_2:
            {
                //Techslides.com html5 sample video: MP4
                uri = "http://techslides.com/demos/sample-videos/small.mp4";
                break;
            }
            case R.id.vtest_btn_webm:
            {
                //Techslides.com html5 sample video: WEBM
                uri = "http://techslides.com/demos/sample-videos/small.webm";
                break;
            }
            case R.id.vtest_btn_3gp:
            {
                //Techslides.com html5 sample video: 3GP
                uri = "http://techslides.com/demos/sample-videos/small.3gp";
                break;
            }
            case R.id.vtest_btn_mp4:
            default:
            {
                //default to BuckBunny 320x180 in MP4
                uri = "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4";
                break;
            }
        }

        //have the uri, launch playback activity
        Logging.logD("Selected sample Uri \"%s\" [ID \"%s\"], launching PlaybackActivity...", uri, view.getId());

        //open playback activity with sample uri
        Intent vidPlayIntent = new Intent(this, PlaybackActivity.class);
        vidPlayIntent.setData(Uri.parse(uri));
        startActivity(vidPlayIntent);
    }
}
