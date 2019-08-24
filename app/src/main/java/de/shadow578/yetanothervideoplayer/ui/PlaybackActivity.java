package de.shadow578.yetanothervideoplayer.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;
import de.shadow578.yetanothervideoplayer.util.UniversalMediaSourceFactory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;

public class PlaybackActivity extends AppCompatActivity
{
    //~~ Constants, change to shared prefs OR remove later ~~
    //auto start player when loading?
    final boolean AUTO_PLAY = true;
    //~~ end~~

    private final int PERMISSION_REQUEST_READ_EXT_STORAGE = 0;

    /**
     * The View the Player Renders Video to
     */
    private PlayerView playerView;

    /**
     * The exoplayer instance
     */
    private SimpleExoPlayer player;

    /**
     * The Factory used to create MediaSources from Uris
     */
    private UniversalMediaSourceFactory mediaSourceFactory;

    /**
     * The listener that listens for exoplayer events and updates the ui & logic accordingly
     */
    private ExoComponentListener componentListener;

    /**
     * The uri that this activity was created with (retried from intent)
     */
    private Uri playbackUri;

    /**
     * The current playback position, used for resuming playback
     */
    private long playbackPosition;

    /**
     * The index of the currently played window, used for resuming playback
     */
    private int currentPlayWindow;

    /**
     * Should the player start playing once ready when resuming playback?
     */
    private boolean playWhenReady;

    /**
     * Used when a local file was passed as playback uri and the app currently does not have permisisons
     */
    private boolean localFilePermissionPending;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        Logging.logD("onCreate of PlaybackActivity called.");

        //get player view
        playerView = findViewById(R.id.pb_PlayerView);

        //get intent this activity was called with to retrieve playback uri
        Intent intent = getIntent();

        //get playback uri from intent + log it
        playbackUri = intent.getData();
        if (playbackUri == null)
        {
            //playback uri is null (invalid), abort and show error
            Toast.makeText(this, getText(R.string.toast_invalid_playback_uri), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //intent + intent data (=playback uri) seems ok
        Logging.logD("Received play intent with uri \"%s\".", playbackUri.toString());

        //check if uri is of local file and request read permission if so
        if (isLocalFileUri(playbackUri))
        {
            Logging.logD("Uri \"%s\" seems to be a local file, requesting read permission...", playbackUri.toString());

            //ask for permissions
            localFilePermissionPending = !checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_READ_EXT_STORAGE);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Logging.logD("onStart of PlaybackActivity called.");

        if (supportMultiWindow())
        {
            //initialize player onStart with multi-window support, because
            //app can be visible but NOT active in split window mode
            initPlayer(playbackUri);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Logging.logD("onResume of PlaybackActivity called.");

        hideSysUI();
        if (!supportMultiWindow() || player == null)
        {
            //initialize player onResume without multi-window support (or not yet initialized), because
            //app cannot be visible without being active...
            initPlayer(playbackUri);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Logging.logD("onStop of PlaybackActivity called.");

        if (supportMultiWindow())
        {
            //release player here with multi-window support, because
            //with multi-window support, the app may be visible when onPause is called.
            freePlayer();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Logging.logD("onPause of PlaybackActivity called.");

        if (!supportMultiWindow())
        {
            //release player here because before multi-window support was added,
            //onStop was not guaranteed to be called
            freePlayer();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Logging.logD("onDestroy of PlaybackActivity called.");

        //app closed, free resources if not happened already
        freePlayer();
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        Logging.logD("Request Permission Result received for request id %d", requestCode);

        //check if permission request was granted
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        //check which permission request this callback handles
        switch (requestCode)
        {
            case PERMISSION_REQUEST_READ_EXT_STORAGE:
            {
                if (granted)
                {
                    //have permissions now, init player + start playing
                    localFilePermissionPending = false;
                    initPlayer(playbackUri);
                }
                else
                {
                    //permissions denied, show toast + close app
                    Toast.makeText(this, getText(R.string.toast_no_storage_permissions_granted), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    /**
     * Starting with API 24, android supports multiple windows
     *
     * @return does this device support multi- window?
     */
    @SuppressLint("ObsoleteSdkInt")
    private boolean supportMultiWindow()
    {
        return Util.SDK_INT > 23;
    }

    /**
     * Hide System- UI elements
     */
    private void hideSysUI()
    {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    /**
     * Check if the uri is a local file
     *
     * @param uri the uri to check
     * @return is the uri a local file?
     */
    private boolean isLocalFileUri(Uri uri)
    {
        return uri.getScheme() != null && (uri.getScheme().equals("content") || uri.getScheme().equals("file"));
    }

    /**
     * Check if the app was granted the permission.
     * If not granted, the permission will be requested and false will be returned.
     *
     * @param permission the permission to check
     * @param requestId  the request id. Used to check in callback
     * @return was the permission granted?
     */
    private boolean checkPermission(@SuppressWarnings("SameParameterValue") String permission, @SuppressWarnings("SameParameterValue") int requestId)
    {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
        {
            //does not have permission, ask for it
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestId);
            return false;
        }
        else
        {
            //has permission
            return true;
        }
    }

    /**
     * Initializes the ExoPlayer to render to the playerView
     *
     * @param playbackUri the Uri of the file / stream to play back
     */
    private void initPlayer(Uri playbackUri)
    {
        //don't do anything if permissions are pending currently
        if (localFilePermissionPending) return;

        //create media source factory
        if (mediaSourceFactory == null)
        {
            mediaSourceFactory = new UniversalMediaSourceFactory(this, getText(R.string.app_user_agent_str).toString());
        }

        //create new simple exoplayer instance
        player = ExoPlayerFactory.newSimpleInstance(this, new DefaultRenderersFactory(this), new DefaultTrackSelector(), new DefaultLoadControl());

        //register Event Listener on player
        componentListener = new ExoComponentListener();
        player.addListener(componentListener);

        //set the view to render to
        playerView.setPlayer(player);

        //load media from uri
        MediaSource mediaSource = mediaSourceFactory.createMediaSource(playbackUri);
        player.prepare(mediaSource, true, false);

        //resume playback where we left off
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentPlayWindow, playbackPosition);
    }

    /**
     * Free resources allocated by the player
     */
    private void freePlayer()
    {
        if (player != null)
        {
            //save player state
            playbackPosition = player.getCurrentPosition();
            currentPlayWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();

            //unregister event listener
            player.removeListener(componentListener);

            //release + null player
            player.release();
            player = null;
        }

        //release media source factory
        if (mediaSourceFactory != null)
        {
            mediaSourceFactory.release();
            mediaSourceFactory = null;
        }
    }

    /**
     * Forces the screen to stay on if keepOn is true, otherwise clears the KEEP_SCREEN_ON flag
     *
     * @param keepOn should the screen stay on?
     */
    private void forceScreenOn(boolean keepOn)
    {
        if (keepOn)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        else
        {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private class ExoComponentListener implements Player.EventListener
    {
        @Override
        public void onLoadingChanged(boolean isLoading)
        {
            Logging.logD("isLoading: " + (isLoading ? "True" : "False"));
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
        {
            switch (playbackState)
            {
                case Player.STATE_IDLE:
                {
                    //player stopped or failed
                    forceScreenOn(false);
                    Toast.makeText(getApplicationContext(), "STATE_IDLE", Toast.LENGTH_SHORT).show();
                    break;
                }
                case Player.STATE_BUFFERING:
                {
                    //media currently buffering
                    Toast.makeText(getApplicationContext(), "STATE_BUFFERING", Toast.LENGTH_SHORT).show();
                    break;
                }
                case Player.STATE_READY:
                {
                    if (playWhenReady)
                    {
                        //currently playing back
                        forceScreenOn(true);
                        Toast.makeText(getApplicationContext(), "STATE_READY-PLAYING", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        //ready for playback (paused or not started yet)
                        forceScreenOn(false);
                        Toast.makeText(getApplicationContext(), "STATE_READY", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case Player.STATE_ENDED:
                {
                    //media playback ended
                    Toast.makeText(getApplicationContext(), "STATE_ENDED", Toast.LENGTH_SHORT).show();
                    break;
                }
                default:
                {
                    Logging.logW("Received invalid Playback State in onPlayerStateChanged: %d", playbackState);
                    break;
                }
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error)
        {
            Logging.logE("ExoPlayer error occured: %s", error.toString());
            Toast.makeText(getApplicationContext(), "Internal Error: \r\n" + error.toString(), Toast.LENGTH_LONG).show();
        }
    }
}
