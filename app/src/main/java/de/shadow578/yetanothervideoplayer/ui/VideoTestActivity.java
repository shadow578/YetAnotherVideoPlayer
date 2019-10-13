package de.shadow578.yetanothervideoplayer.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;

public class VideoTestActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener
{
    private Switch swOpenDirect;
    private Switch swOpenShare;
    private Switch swInsertTitles;
    private Switch swUseNonStandardTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_test);

        //get views
        swOpenDirect = findViewById(R.id.vtest_tgl_open_direct);
        swOpenShare = findViewById(R.id.vtest_tgl_open_share);
        swInsertTitles = findViewById(R.id.vtest_tgl_add_title_extra);
        swUseNonStandardTitle = findViewById(R.id.vtest_tgl_non_standard_title);

        //add listeners to enable / disable switches on the fly
        swOpenDirect.setOnCheckedChangeListener(this);
        swInsertTitles.setOnCheckedChangeListener(this);
    }

    /**
     * Common Switch state change listener for switches in VideoTest activity
     *
     * @param compoundButton the switch that changed
     * @param isChecked      is the switch not checked (=enabled)?
     */
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked)
    {
        switch (compoundButton.getId())
        {
            case R.id.vtest_tgl_open_direct:
            {
                //open direct switch changed, enable/disable open by share toggle
                swOpenShare.setEnabled(!isChecked);
                break;
            }
            case R.id.vtest_tgl_add_title_extra:
            {
                //insert titles switch changed, enable/disable non standard title toggle
                swUseNonStandardTitle.setEnabled(isChecked);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Common click handler for buttons in VideoTest activity
     *
     * @param view the view that invoked this handler
     */
    public void videoTest_OnClick(View view)
    {
        //the uri that later contains the uri to stream from
        String uri;

        //the title to add to the intent
        String title;

        //region ~~ select url and title by button id ~~
        switch (view.getId())
        {
            case R.id.vtest_btn_mp3:
            {
                //ExoPlayer test media: MP3
                uri = "http://storage.googleapis.com/exoplayer-test-media-0/play.mp3";
                title = "ExoPlayer Test - MP3";
                break;
            }
            case R.id.vtest_btn_mp4_2:
            {
                //Techslides.com html5 sample video: MP4
                uri = "http://techslides.com/demos/sample-videos/small.mp4";
                title = "Sample MP4";
                break;
            }
            case R.id.vtest_btn_webm:
            {
                //Techslides.com html5 sample video: WEBM
                uri = "http://techslides.com/demos/sample-videos/small.webm";
                title = "Sample WEBM";
                break;
            }
            case R.id.vtest_btn_3gp:
            {
                //Techslides.com html5 sample video: 3GP
                uri = "http://techslides.com/demos/sample-videos/small.3gp";
                title = "Sample 3GP";
                break;
            }
            case R.id.vtest_btn_dash:
            {
                //DASH IF Sample: Tears of Steel
                uri = "https://dash.akamaized.net/dash264/TestCasesIOP33/adapatationSetSwitching/5/manifest.mpd";
                title = "Tears of Steel";
                break;
            }
            case R.id.vtest_btn_dash_2:
            {
                //DASH IF Sample: Elephants Dream
                uri = "https://dash.akamaized.net/dash264/TestCases/1a/netflix/exMPD_BIP_TC1.mpd";
                title = "Elephants Dream";
                break;
            }
            case R.id.vtest_btn_dash_and_subs:
            {
                //DASH + TTML subtitles
                uri = "http://media.axprod.net/dash/ED_TTML_NEW/Clear/Manifest_sub_in.mpd";
                title = "Demo (DASH + TTML Subs)";
                break;
            }
            case R.id.vtest_btn_hls:
            {
                //bitdash/bitmovin: sintel
                uri = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8";
                title = "bitmovin - sintel";
                break;
            }
            case R.id.vtest_btn_settings:
            {
                //open settings
                startActivity(new Intent(this, AppSettingsActivity.class));
                return;
            }
            case R.id.vtest_btn_mp4:
            default:
            {
                //default to BuckBunny 320x180 in MP4
                uri = "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4";
                title = "Big Buck Bunny";
                break;
            }
        }

        //endregion

        //we url + title is now set, check which method launching shall be used
        //get values
        boolean openDirect = swOpenDirect.isChecked();
        boolean openShare = swOpenShare.isChecked() && !openDirect;
        boolean insertTitle = swInsertTitles.isChecked();
        boolean useStandardExtra = !swUseNonStandardTitle.isChecked() && insertTitle;

        //all ok, create the intent
        Logging.logD("Launching PlaybackActivity with URI=\"%s\"; Title=\"%s\" - Open Direct: %b; Open Share: %b Insert Title: %b; Use Standard Extra: %b",
                uri, title, openDirect, openShare, insertTitle, useStandardExtra);
        Intent playIntent;

        //region ~~ Create the Intent according to switch states ~~
        if (openDirect)
        {
            //directly open activity
            playIntent = new Intent(this, PlaybackActivity.class);
        }
        else
        {
            //open using action_x
            if (openShare)
            {
                //open using ACTION_SEND (like "share with yavp" would do)
                playIntent = new Intent(Intent.ACTION_SEND);
                playIntent.setType("text/plain");
            }
            else
            {
                //open activity using ACTION_VIEW (like other apps (Gallery) would do)
                playIntent = new Intent(Intent.ACTION_VIEW);
            }
        }

        //add url to intent as data (or extra if sharing)
        if (openShare)
        {
            playIntent.putExtra(Intent.EXTRA_TEXT, uri);
        }
        else
        {
            playIntent.setData(Uri.parse(uri));
        }

        //add title
        if (insertTitle)
        {
            if (useStandardExtra)
            {
                //use the android standard extra key
                playIntent.putExtra(Intent.EXTRA_TITLE, title);
            }
            else
            {
                //use some custom extra key
                playIntent.putExtra("title", title);
            }
        }

        //endregion

        //start the activity
        startActivity(playIntent);
    }
}
