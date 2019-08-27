package de.shadow578.yetanothervideoplayer.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;
import de.shadow578.yetanothervideoplayer.util.SwipeGestureListener;
import de.shadow578.yetanothervideoplayer.util.UniversalMediaSourceFactory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.SizeF;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Util;

public class PlaybackActivity extends AppCompatActivity
{
    //region ~~ Constants, change to shared prefs OR remove later ~~
    //auto start player when loading?
    private final boolean AUTO_PLAY = true;

    //info text duration for volume and brightness info (ms)
    private final long INFO_TEXT_DURATION_VB = 750;

    //how long the fade out of the info text lasts (ms)
    private final long INFO_TEXT_FADE_DURATION = 500;

    //how fast the "press back again to exit" flag resets (ms)
    private final long BACK_DOUBLE_PRESS_TIMEOUT = 1000;

    //how much to change the screen brightness in one "step"
    private final float BRIGHTNESS_ADJUST_STEP = 0.01f;

    //the threshold for a "hard" swipe. Only with a "hard" swipe, the brightness level can be set to 0 (=auto/device default) (dp)
    private final float BRIGHTNESS_HARD_SWIPE_THRESHOLD = 37.0f;

    //settings for SwipeGestureListener (in setupGestures)
    private final long TOUCH_DECAY_TIME = 1500;
    private final float SWIPE_THRESHOLD_N = 5f;//0.005f;//*1439.00px
    private final float FLING_THRESHOLD_N = 10f;//0.01f;//*1439.00px

    //endregion

    //region ~~ Constants ~~

    /**
     * Id of permission request for external storage
     */
    private final int PERMISSION_REQUEST_READ_EXT_STORAGE = 0;

    //endregion

    //region ~~ Variables ~~

    /**
     * The View the Player Renders Video to
     */
    private PlayerView playerView;

    /**
     * The TextView in the center of the screen, used to show information
     */
    private TextView infoTextView;

    /**
     * The Text View used to display the stream title
     */
    private TextView titleTextView;

    /**
     * The ProgressBar in the center of the screen, used to show that the player is currently buffering
     */
    private ProgressBar bufferingProgressBar;

    /**
     * The uri that this activity was created with (retried from intent)
     */
    private Uri playbackUri;

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
     * The Listener that listens for exoplayer metadata events and updates the ui & logic accordingly
     */
    private ExoMetadataListener metadataListener;

    /**
     * Manages the screen rotation using the buttons
     */
    private PBScreenRotationManager screenRotationManager;

    /**
     * The audio manager instance used to adjust media volume by swiping
     */
    private AudioManager audioManager;

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
     * Used when a local file was passed as playback uri and the app currently does not have permissions
     */
    private boolean localFilePermissionPending;

    /**
     * Was the Back button pressed once already?
     * Used for "Press Back again to exit" function
     */
    private boolean backPressedOnce;

    //endregion

    //region ~~ Message Handler (delayHandler) ~~

    /**
     * Message code constants used by the delayHandler
     */
    private static final class Messages
    {
        /**
         * Message id to start fading out info text
         */
        private static final int START_FADE_OUT_INFO_TEXT = 0;

        /**
         * Message id to make info text invisible
         */
        private static final int SET_INFO_TEXT_INVISIBLE = 1;

        /**
         * Message id to reset backPressedOnce flag
         */
        private static final int RESET_BACK_PRESSED = 2;
    }

    /**
     * Shared handler that can be used to invoke methods and/or functions with a delay,
     */
    private final Handler delayHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(@NonNull Message msg)
        {
            switch (msg.what)
            {
                case Messages.START_FADE_OUT_INFO_TEXT:
                {
                    //start fade out animation
                    Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setInterpolator(new DecelerateInterpolator());
                    fadeOut.setDuration(INFO_TEXT_FADE_DURATION);
                    infoTextView.setAnimation(fadeOut);

                    //make invisible after animation
                    delayHandler.sendEmptyMessageDelayed(Messages.SET_INFO_TEXT_INVISIBLE, INFO_TEXT_FADE_DURATION);
                    break;
                }
                case Messages.SET_INFO_TEXT_INVISIBLE:
                {
                    //set invisible + clear animation
                    infoTextView.setVisibility(View.INVISIBLE);
                    infoTextView.setAnimation(null);
                    break;
                }
                case Messages.RESET_BACK_PRESSED:
                {
                    backPressedOnce = false;
                    break;
                }
            }
        }
    };

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        Logging.logD("onCreate of PlaybackActivity called.");

        //get views
        playerView = findViewById(R.id.pb_playerView);
        infoTextView = findViewById(R.id.pb_infoText);
        titleTextView = findViewById(R.id.pb_streamTitle);
        bufferingProgressBar = findViewById(R.id.pb_playerBufferingProgress);

        //init screen rotation manager
        screenRotationManager = new PBScreenRotationManager();
        screenRotationManager.findComponents();

        //get audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //setup gestures
        setupGestures();

        //get intent this activity was called with to retrieve playback uri
        Intent intent = getIntent();

        //get playback uri from intent + log it
        playbackUri = intent.getData();
        if (playbackUri == null)
        {
            //playback uri is null (invalid), abort and show error
            Toast.makeText(this, getString(R.string.toast_invalid_playback_uri), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //intent + intent data (=playback uri) seems ok
        Logging.logD("Received play intent with uri \"%s\".", playbackUri.toString());

        //set initial title to filename parsed from uri
        setTitle(parseFileNameFromUri(playbackUri));

        //check if uri is of local file and request read permission if so
        if (isLocalFileUri(playbackUri))
        {
            Logging.logD("Uri \"%s\" seems to be a local file, requesting read permission...", playbackUri.toString());

            //ask for permissions
            localFilePermissionPending = !checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_READ_EXT_STORAGE);
        }
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
                    Toast.makeText(this, getString(R.string.toast_no_storage_permissions_granted), Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    //region ~~ Application Lifecycle ~~

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

    @Override
    public void onBackPressed()
    {
        if (backPressedOnce)
        {
            //back pressed once already, do normal thing...
            super.onBackPressed();
            return;
        }

        //pressed back the first time:
        //set flag
        backPressedOnce = true;

        //send reset message delayed
        delayHandler.sendEmptyMessageDelayed(Messages.RESET_BACK_PRESSED, BACK_DOUBLE_PRESS_TIMEOUT);

        //show user a Toast
        Toast.makeText(this, getString(R.string.toast_press_back_again_to_exit), Toast.LENGTH_SHORT).show();
    }

    //endregion

    //region ~~ Swipe Gestures ~~

    /**
     * Setup Gesture controls for Volume and Brightness
     */
    private void setupGestures()
    {
        //init and set listener
        playerView.setOnTouchListener(new SwipeGestureListener(TOUCH_DECAY_TIME, SWIPE_THRESHOLD_N, FLING_THRESHOLD_N,
                new RectF(0, 20, 0, 75))
        {
            @Override
            public void onHorizontalSwipe(float deltaX, PointF swipeStart, PointF swipeEnd, PointF firstContact, SizeF screenSize)
            {
            }

            @Override
            public void onVerticalSwipe(float deltaY, PointF swipeStart, PointF swipeEnd, PointF firstContact, SizeF screenSize)
            {
                //check which screen size the swipe originated from
                if (firstContact.x > (screenSize.getWidth() / 2))
                {
                    //swipe on right site of screen, adjust volume
                    if (deltaY > 0.0)
                    {
                        //swipe up, increase volume
                        adjustVolume(true);
                    }
                    else
                    {
                        //swipe down, decrease volume
                        adjustVolume(false);
                    }
                }
                else
                {
                    //swipe on left site of screen, adjust brightness
                    if (deltaY > 0.0)
                    {
                        //swipe up, increase brightness
                        adjustScreenBrightness(BRIGHTNESS_ADJUST_STEP, false);
                    }
                    else
                    {
                        //swipe down, decrease brightness:
                        //check if "hard" swipe
                        boolean hardSwipe = Math.abs(deltaY) > BRIGHTNESS_HARD_SWIPE_THRESHOLD;

                        //allow setting brightness to 0 when hard swiping (but ONLY then)
                        adjustScreenBrightness(-BRIGHTNESS_ADJUST_STEP, hardSwipe);
                    }
                }
            }

            @Override
            public void onHorizontalFling(float deltaX, PointF flingStart, PointF flingEnd, SizeF screenSize)
            {
            }

            @Override
            public void onVerticalFling(float deltaY, PointF flingStart, PointF flingEnd, SizeF screenSize)
            {
            }
        });
    }

    /**
     * Adjust the screen brightness
     *
     * @param adjust    the amount to adjust the brightness by. (range of brightness is 0.0 to 1.0)
     * @param allowZero if set to true, setting the brightness to zero (=device default/auto) is allowed. Otherwise, minimum brightness is clamped to 0.01
     */
    private void adjustScreenBrightness(float adjust, boolean allowZero)
    {
        //get window attributes
        WindowManager.LayoutParams windowAttributes = getWindow().getAttributes();

        //check if brightness is already zero (overrides allowZero)
        boolean alreadyZero = windowAttributes.screenBrightness == 0.0f;

        //modify screen brightness attribute withing range
        //allow setting it to zero if allowZero is set or the value was previously zero too
        windowAttributes.screenBrightness = Math.min(Math.max(windowAttributes.screenBrightness + adjust, ((allowZero || alreadyZero) ? 0.0f : 0.01f)), 1f);

        //set changed window attributes
        getWindow().setAttributes(windowAttributes);

        //show info text for brightness
        String brightnessStr = ((int) Math.floor(windowAttributes.screenBrightness * 100)) + "%";
        if (windowAttributes.screenBrightness == 0)
        {
            brightnessStr = getString(R.string.info_brightness_auto);
        }
        showInfoText(INFO_TEXT_DURATION_VB, getString(R.string.info_brightness_change), brightnessStr);
    }

    /**
     * Adjust the media volume by one volume step
     *
     * @param raise should the volume be raises (=true) or lowered (=false) by one step?
     */
    private void adjustVolume(boolean raise)
    {
        //check audioManager
        if (audioManager == null)
        {
            Logging.logW("audioManager is null, cannot adjust media volume!");
            return;
        }

        //adjusts volume without ui
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, (raise ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER), 0);

        //show info text for volume:
        //get volume + range
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int minVolume = 0;
        if (Util.SDK_INT > 28)
        {
            //get minimum stream volume if above api28
            minVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
        }

        //calculate volume in percent
        int volumePercent = (int) Math.floor(((float) currentVolume - (float) minVolume) / ((float) maxVolume - (float) minVolume) * 100);

        //show info text
        showInfoText(INFO_TEXT_DURATION_VB, getString(R.string.info_volume_change), volumePercent);
    }

    //endregion

    //region ~~ Exoplayer setup and lifecycle ~~

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
            mediaSourceFactory = new UniversalMediaSourceFactory(this, getString(R.string.app_user_agent_str));
        }

        //create new simple exoplayer instance
        player = ExoPlayerFactory.newSimpleInstance(this, new DefaultRenderersFactory(this), new DefaultTrackSelector(), new DefaultLoadControl());


        //register Event Listener on player
        componentListener = new ExoComponentListener();
        player.addListener(componentListener);

        //register metadata listener on player
        metadataListener = new ExoMetadataListener();
        player.addMetadataOutput(metadataListener);

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

            //unregister listeners
            player.removeListener(componentListener);
            player.removeMetadataOutput(metadataListener);

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

    //endregion

    //region ~~ Button Handling ~~

    /**
     * Common click handler for buttons in Playback activity
     *
     * @param view the view that invoked this handler
     */
    public void playback_OnClick(View view)
    {
        switch (view.getId())
        {
            //region ~~ Screen Rotation Buttons (cycle modes >auto>portrait>landscape>auto>...)~~
            case R.id.pb_screen_roation_auto:
            {
                //automatic/default screen rotation button
                screenRotationManager.setScreenMode(PBScreenRotationManager.SCREEN_LOCK_PORTRAIT);
                break;
            }
            case R.id.pb_screen_roation_portrait:
            {
                //lock screen to portrait button
                screenRotationManager.setScreenMode(PBScreenRotationManager.SCREEN_LOCK_LANDSCAPE);
                break;
            }
            case R.id.pb_screen_roation_landscape:
            {
                //lock screen to landscape button
                screenRotationManager.setScreenMode(PBScreenRotationManager.SCREEN_AUTO);
                break;
            }
            //endregion
        }
    }
    //endregion

    //region ~~ Utility ~~

    /**
     * Set the title shown on the titleTextView
     *
     * @param title the title to show. if set to a empty string, the title label is hidden
     */
    private void setTitle(String title)
    {
        if (title.isEmpty())
        {
            //no title, hide title label
            titleTextView.setText("N/A");
            titleTextView.setVisibility(View.INVISIBLE);
        }
        else
        {
            //show title label
            titleTextView.setText(title);
            titleTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Parse the name of the streamed file from the playback uri
     *
     * @param uri the playback uri
     * @return the parsed file name, or a empty string if parsing failed
     */
    private String parseFileNameFromUri(@NonNull Uri uri)
    {
        //sample uri: "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4".

        //try to get the title using the last path segment of the uri
        String fileName = uri.getLastPathSegment();
        if (fileName != null && !fileName.isEmpty())
        {
            Logging.logD("parse filename from uri: %s", fileName);

            //last path segment worked, remove file extension and re- format some


            //return filename parsed from last path segment
            return fileName;
        }

        return "";
    }

    /**
     * Set if the player is currently buffering
     * (buffering progress bar visibility)
     *
     * @param isBuffering true if buffering, false if not
     */
    private void setBuffering(boolean isBuffering)
    {
        bufferingProgressBar.setVisibility(isBuffering ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Show info text in the middle of the screen, using the InfoText View
     *
     * @param duration how long to show the info text box, in milliseconds
     * @param text     the text to show
     * @param format   formatting options (using US locale)
     */
    private void showInfoText(@SuppressWarnings("SameParameterValue") long duration, String text, Object... format)
    {
        //remove all previous messages related to fading out the info text
        delayHandler.removeMessages(Messages.START_FADE_OUT_INFO_TEXT);
        delayHandler.removeMessages(Messages.SET_INFO_TEXT_INVISIBLE);

        //set text
        infoTextView.setText(String.format(text, format));

        //reset animation and make text visible
        infoTextView.setAnimation(null);
        infoTextView.setVisibility(View.VISIBLE);

        //hide text after delay
        delayHandler.sendEmptyMessageDelayed(Messages.START_FADE_OUT_INFO_TEXT, duration);
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

    //endregion

    /**
     * Updates the UI (and logic states) in response to ExoPlayer Events
     */
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
            //update buffering progress bar
            setBuffering(playbackState == Player.STATE_BUFFERING);

            switch (playbackState)
            {
                case Player.STATE_IDLE:
                {
                    //player stopped or failed, release screen lock
                    forceScreenOn(false);
                    Toast.makeText(getApplicationContext(), "STATE_IDLE", Toast.LENGTH_SHORT).show();
                    break;
                }
                case Player.STATE_BUFFERING:
                {
                    //media currently buffering
                    //Toast.makeText(getApplicationContext(), "STATE_BUFFERING", Toast.LENGTH_SHORT).show();
                    break;
                }
                case Player.STATE_READY:
                {
                    if (playWhenReady)
                    {
                        //currently playing back, engage screen lock
                        forceScreenOn(true);
                        //Toast.makeText(getApplicationContext(), "STATE_READY-PLAYING", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        //ready for playback (paused or not started yet), release screen lock
                        forceScreenOn(false);
                        //Toast.makeText(getApplicationContext(), "STATE_READY", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case Player.STATE_ENDED:
                {
                    //media playback ended, release screen lock
                    forceScreenOn(false);
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
            Logging.logE("ExoPlayer error occurred: %s", error.toString());
            Toast.makeText(getApplicationContext(), "Internal Error: \r\n" + error.toString(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Updates the UI (and logic states) in response to ExoPlayer MetaData Events
     */
    private class ExoMetadataListener implements MetadataOutput
    {
        @Override
        public void onMetadata(Metadata metadata)
        {
            //received metadata?? how to decode?? actually getting any??
            Logging.logD("receive metadata: %s", metadata.toString());
            Toast.makeText(getApplicationContext(), "Receive- Metadata!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Contains functionality to set the screen orientation with three buttons
     */
    private class PBScreenRotationManager
    {
        //region ~~ Constants ~~

        /**
         * screen set to follow device
         */
        private static final int SCREEN_AUTO = 0;

        /**
         * Screen locked to portrait mode
         */
        private static final int SCREEN_LOCK_PORTRAIT = 1;

        /**
         * Screen locked to landscape mode
         */
        private static final int SCREEN_LOCK_LANDSCAPE = 2;
        //endregion

        /**
         * The Image Button to set the screen to auto/default mode
         */
        ImageButton btnScreenAuto;

        /**
         * The Image Button to set the screen to lock landscape
         */
        ImageButton btnScreenLockLandscape;

        /**
         * The Image Button to set the screen to locked portrait
         */
        ImageButton btnScreenLockPortrait;

        /**
         * Initially find the components that are the three buttons
         */
        private void findComponents()
        {
            btnScreenAuto = findViewById(R.id.pb_screen_roation_auto);
            btnScreenLockLandscape = findViewById(R.id.pb_screen_roation_landscape);
            btnScreenLockPortrait = findViewById(R.id.pb_screen_roation_portrait);
        }

        /**
         * Set the screen mode + set button visibility
         *
         * @param mode the mode to set
         */
        private void setScreenMode(int mode)
        {
            //act according to mode to set
            switch (mode)
            {
                case SCREEN_AUTO:
                {
                    //enable auto button + reset requested orientation
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);

                    btnScreenAuto.setVisibility(View.VISIBLE);
                    btnScreenLockLandscape.setVisibility(View.INVISIBLE);
                    btnScreenLockPortrait.setVisibility(View.INVISIBLE);

                    Logging.logD("Changed Screen mode to AUTO");
                    break;
                }
                case SCREEN_LOCK_PORTRAIT:
                {
                    //enable lock-portrait button + set requested orientation to portrait
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                    btnScreenLockPortrait.setVisibility(View.VISIBLE);
                    btnScreenAuto.setVisibility(View.INVISIBLE);
                    btnScreenLockLandscape.setVisibility(View.INVISIBLE);

                    Logging.logD("Changed Screen mode to PORTRAIT");
                    break;
                }
                case SCREEN_LOCK_LANDSCAPE:
                {
                    //enable lock-landscape button + set requested orientation to landscape
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

                    btnScreenLockLandscape.setVisibility(View.VISIBLE);
                    btnScreenAuto.setVisibility(View.INVISIBLE);
                    btnScreenLockPortrait.setVisibility(View.INVISIBLE);

                    Logging.logD("Changed Screen mode to LANDSCAPE");
                    break;
                }
            }
        }
    }
}
