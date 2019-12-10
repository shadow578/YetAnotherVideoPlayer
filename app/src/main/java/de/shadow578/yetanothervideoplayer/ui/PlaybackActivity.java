package de.shadow578.yetanothervideoplayer.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.ui.components.ControlQuickSettingsButton;
import de.shadow578.yetanothervideoplayer.ui.components.PlayerControlViewWrapper;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.Logging;
import de.shadow578.yetanothervideoplayer.util.SwipeGestureListener;
import de.shadow578.yetanothervideoplayer.util.UniversalMediaSourceFactory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
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
import android.widget.TextView;
import android.widget.Toast;

import com.daasuu.epf.EPlayerView;
import com.daasuu.epf.PlayerScaleType;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

public class PlaybackActivity extends AppCompatActivity
{
    //region ~~ Constants ~~

    /**
     * Id of permission request for external storage
     */
    private final int PERMISSION_REQUEST_READ_EXT_STORAGE = 0;

    //endregion

    //region ~~ Variables ~~
    /**
     * The topmost view of the playback activity
     */
    private View playbackRootView;

    /**
     * The View the Player Renders Video to
     */
    private EPlayerView playerView;

    /**
     * The View that contains and controls the player controls
     */
    private PlayerControlViewWrapper playerControlView;

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
    private View bufferingSpinner;

    /**
     * The drawer that contains the quick settings
     */
    private DrawerLayout quickSettingsDrawer;

    /**
     * The Uri of the media to play back
     */
    private Uri playbackUri;

    /**
     * The MediaSource to play back
     */
    private MediaSource playbackMedia;

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
    private ExoEventListener componentListener;

    /**
     * The Listener that listens for exoplayer metadata events and updates the ui & logic accordingly
     */
    private ExoMetadataListener metadataListener;

    /**
     * Manages the screen rotation using the buttons
     */
    private ScreenRotationManager screenRotationManager;

    /**
     * The audio manager instance used to adjust media volume by swiping
     */
    private AudioManager audioManager;

    /**
     * Receives events from PIP control items when in pip mode
     */
    private BroadcastReceiver pipBroadcastReceiver;

    /**
     * The Preferences of the app
     */
    private SharedPreferences appPreferences;

    /**
     * Bandwidth meter used to measure the bandwidth and select stream quality accordingly
     */
    private BandwidthMeter bandwidthMeter;

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

    /**
     * Is the replay button currently visible?
     * Used to track if the play button's graphic is currently changed to the replay button graphic
     */
    private boolean replayButtonVisible;

    /**
     * How long the Volume/Brightness info text is visible
     */
    private int infoTextDurationMs;

    /**
     * The original volume index before the playback activity was opened
     */
    private int originalVolumeIndex;

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
                    //get fade out duration
                    int fadeDuration = getResources().getInteger(R.integer.info_text_fade_duration);

                    //start fade out animation
                    Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
                    fadeOut.setInterpolator(new DecelerateInterpolator());
                    fadeOut.setDuration(fadeDuration);
                    infoTextView.setAnimation(fadeOut);

                    //make invisible after animation
                    delayHandler.sendEmptyMessageDelayed(Messages.SET_INFO_TEXT_INVISIBLE, fadeDuration);
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

        //make app fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //set activity layout
        setContentView(R.layout.activity_playback);
        Logging.logD("onCreate of PlaybackActivity called.");

        //get preferences
        appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //get views
        playbackRootView = findViewById(R.id.pb_playbackRootView);
        playerView = findViewById(R.id.pb_playerView);
        playerControlView = findViewById(R.id.pb_playerControlView);
        infoTextView = findViewById(R.id.pb_infoText);
        titleTextView = findViewById(R.id.pb_streamTitle);
        bufferingSpinner = findViewById(R.id.pb_playerBufferingCont);
        quickSettingsDrawer = findViewById(R.id.pb_quick_settings_drawer);

        //set fast-forward and rewind increments
        int seekIncrement = getPrefInt(ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT);
        playerControlView.setFastForwardIncrementMs(seekIncrement);
        playerControlView.setRewindIncrementMs(seekIncrement);

        //init screen rotation manager
        screenRotationManager = new ScreenRotationManager();
        screenRotationManager.findComponents();

        //get audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //create bandwidth meter
        bandwidthMeter = new DefaultBandwidthMeter.Builder(this).build();

        //Get the intent this activity was created with
        Intent callIntent = getIntent();

        //get uri (in intents data field)
        playbackUri = callIntent.getData();
        if (playbackUri == null)
        {
            //invalid url
            Toast.makeText(this, "Invalid Playback URL! Exiting...", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //get title (in intents EXTRA_TITLE field)
        String title = callIntent.getStringExtra(Intent.EXTRA_TITLE);
        if (title == null || title.isEmpty())
        {
            title = "N/A";
        }
        setTitle(title);

        //setup gesture controls
        setupGestures();

        //check if uri is of local file and request read permission if so
        if (isLocalFileUri(playbackUri))
        {
            Logging.logD("Uri \"%s\" seems to be a local file, requesting read permission...", playbackUri.toString());

            //ask for permissions
            localFilePermissionPending = !checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_READ_EXT_STORAGE);
        }

        //enable / disable autoplay
        playWhenReady = getPrefBool(ConfigKeys.KEY_AUTO_PLAY, R.bool.DEF_AUTO_PLAY);

        //get info text duration once
        infoTextDurationMs = getPrefInt(ConfigKeys.KEY_INFO_TEXT_DURATION, R.integer.DEF_INFO_TEXT_DURATION);

        //prepare media
        if (!localFilePermissionPending)
        {
            //not waiting for permissions
            initMedia(playbackUri);
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
                    initMedia(playbackUri);
                    initPlayer(playbackMedia);
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
            initPlayer(playbackMedia);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Logging.logD("onResume of PlaybackActivity called.");

        if (!supportMultiWindow() || player == null)
        {
            //initialize player onResume without multi-window support (or not yet initialized), because
            //app cannot be visible without being active...
            initPlayer(playbackMedia);
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
    protected void onUserLeaveHint()
    {
        super.onUserLeaveHint();

        //enter pip mode if enabled
        if (getPrefBool(ConfigKeys.KEY_ENTER_PIP_ON_LEAVE, R.bool.DEF_ENTER_PIP_ON_LEAVE) && isPlaying())
            tryGoPip();
    }

    @Override
    public void onBackPressed()
    {
        if (backPressedOnce || !getPrefBool(ConfigKeys.KEY_BACK_DOUBLE_PRESS_EN, R.bool.DEF_BACK_DOUBLE_PRESS_EN))
        {
            //back pressed once already, do normal thing...
            super.onBackPressed();
            return;
        }

        //pressed back the first time:
        //set flag
        backPressedOnce = true;

        //send reset message delayed
        delayHandler.sendEmptyMessageDelayed(Messages.RESET_BACK_PRESSED, getPrefInt(ConfigKeys.KEY_BACK_DOUBLE_PRESS_TIMEOUT, R.integer.DEF_BACK_DOUBLE_PRESS_TIMEOUT));

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
        //check if swipe gestures are enables
        if (!getPrefBool(ConfigKeys.KEY_SWIPE_GESTURE_EN, R.bool.DEF_SWIPE_GESTURES_EN))
        {
            Logging.logD("Not initializing swipe gestures: swipe gestures are disabled!");
            return;
        }

        //get configuration values needed in swipe handler (avoid looking up values constantly)
        final float brightnessAdjustStep = getPrefInt(ConfigKeys.KEY_BRIGHTNESS_ADJUST_STEP, R.integer.DEF_BRIGHTNESS_ADJUST_STEP) / 100.0f;
        final float hardSwipeThreshold = getPrefInt(ConfigKeys.KEY_BRIGHTNESS_HARD_SWIPE_THRESHOLD, R.integer.DEF_BRIGHTNESS_HARD_SWIPE_THRESHOLD);
        final boolean hardSwipeEnable = getPrefBool(ConfigKeys.KEY_BRIGHTNESS_HARD_SWIPE_EN, R.bool.DEF_BRIGHTNESS_HARD_SWIPE_EN);

        //init and set listener
        // playerView.setOnTouchListener(new SwipeGestureListener(TOUCH_DECAY_TIME, SWIPE_THRESHOLD_N, FLING_THRESHOLD_N,
        // new RectF(0, 20, 0, 75))
        int swipeFlingThreshold = getPrefInt(ConfigKeys.KEY_SWIPE_FLING_THRESHOLD, R.integer.DEF_SWIPE_FLING_THRESHOLD);
        playbackRootView.setOnTouchListener(new SwipeGestureListener(getPrefInt(ConfigKeys.KEY_TOUCH_DECAY_TIME, R.integer.DEF_TOUCH_DECAY_TIME), swipeFlingThreshold, swipeFlingThreshold,
                new RectF(getPrefInt(ConfigKeys.KEY_SWIPE_DEAD_ZONE_RECT_LEFT, R.integer.DEF_SWIPE_DEAD_ZONE_LEFT),
                        getPrefInt(ConfigKeys.KEY_SWIPE_DEAD_ZONE_RECT_TOP, R.integer.DEF_SWIPE_DEAD_ZONE_TOP),
                        getPrefInt(ConfigKeys.KEY_SWIPE_DEAD_ZONE_RECT_RIGHT, R.integer.DEF_SWIPE_DEAD_ZONE_RIGHT),
                        getPrefInt(ConfigKeys.KEY_SWIPE_DEAD_ZONE_RECT_BOTTOM, R.integer.DEF_SWIPE_DEAD_ZONE_BOTTOM)))
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
                    if (deltaY > 0)
                    {
                        //swipe up, increase brightness
                        adjustScreenBrightness(brightnessAdjustStep, false);
                    }
                    else
                    {
                        //swipe down, decrease brightness:
                        //check if "hard" swipe, override hard swipe if not enabled
                        boolean hardSwipe = hardSwipeEnable || Math.abs(deltaY) > hardSwipeThreshold;


                        //allow setting brightness to 0 when hard swiping (but ONLY then)
                        adjustScreenBrightness(-brightnessAdjustStep, hardSwipe);
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

            @Override
            public void onNoSwipeClick(View view, PointF clickPos, SizeF screenSize)
            {
                //forward click to player controls
                playerControlView.performClick();
                super.onNoSwipeClick(view, clickPos, screenSize);
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
        showInfoText(getString(R.string.info_brightness_change), brightnessStr);
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
        showInfoText(getString(R.string.info_volume_change), volumePercent);
    }

    //endregion

    //region ~~ Exoplayer setup and lifecycle ~~

    /**
     * Initialize the media for playback
     *
     * @param playbackUri the Uri of the file / stream to play back
     */
    private void initMedia(Uri playbackUri)
    {
        //don't do anything if permissions are pending currently
        if (localFilePermissionPending) return;

        //create media source factory
        if (mediaSourceFactory == null)
        {
            mediaSourceFactory = new UniversalMediaSourceFactory(this, Util.getUserAgent(this, getString(R.string.app_name)));
        }

        //load media from uri
        playbackMedia = createMediaSource(playbackUri);
    }

    /**
     * Initializes the ExoPlayer to render to the playerView
     *
     * @param media the MediaSource to play back
     */
    private void initPlayer(MediaSource media)
    {
        //check if media is valid
        if (media == null) return;

        //create new simple exoplayer instance
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());

        DefaultRenderersFactory rendersFactory = new DefaultRenderersFactory(this);
        LoadControl loadController = new DefaultLoadControl();
        player = ExoPlayerFactory.newSimpleInstance(this, rendersFactory, trackSelector, loadController, null, bandwidthMeter);

        //register Event Listener on player
        componentListener = new ExoEventListener();
        player.addListener(componentListener);

        //register metadata listener on player
        metadataListener = new ExoMetadataListener();
        player.addMetadataOutput(metadataListener);

        //set the view to render to
        playerView.setSimpleExoPlayer(player);

        //fit video to width of screen
        playerView.setPlayerScaleType(PlayerScaleType.RESIZE_FIT_WIDTH);

        //make controls visible
        setUseController(true);

        //prepare media for playback
        player.prepare(media, true, false);

        //resume playback where we left off
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentPlayWindow, playbackPosition);

        //save original volume level
        originalVolumeIndex = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        //restore persistent volume and brightness levels
        restorePersistentVolume();
        restorePersistentBrightness();
    }

    /**
     * Free resources allocated by the player
     * -Don't free cache etc, as playback may be continued later
     * -Do save (+ restore) volume & brightness levels
     */
    private void freePlayer()
    {
        Logging.logD("Freeing player...");
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

        //dispose media resources
        freeMedia();

        //do volume restore
        if (originalVolumeIndex != -1)
        {
            //save current volume and brightness levels for persistence
            savePersistentVolume();
            savePersistentBrightness();

            //restore original volume level
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolumeIndex, 0);
            originalVolumeIndex = -1;
        }
    }

    /**
     * Free all resources allocated for playback (player, cache, ...)
     */
    private void freeMedia()
    {
        Logging.logD("Disposing playback resources...");

        //release media source factory
        if (mediaSourceFactory != null)
        {
            mediaSourceFactory.release();
            mediaSourceFactory = null;
        }
    }

    /**
     * Create a media source from the uri, and inform the user if the media type is NOT supported
     *
     * @param uri the uri of the media
     * @return the media source, or null if not supported
     */
    private MediaSource createMediaSource(Uri uri)
    {
        //create media source using universal factory
        MediaSource source = mediaSourceFactory.createMediaSource(uri);

        //show error message to user if not supported (=null)
        if (source == null)
        {
            //get file extension of the file
            String fileExt = uri.getLastPathSegment();
            if (fileExt == null) fileExt = "dummy.ERROR";
            fileExt = fileExt.substring(fileExt.lastIndexOf('.'));

            //format dialog message
            String dialogMsg = String.format(getString(R.string.dialog_format_not_supported_message), fileExt);

            //show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.dialog_format_not_supported_title)
                    .setMessage(dialogMsg)
                    .setCancelable(false)
                    .setNeutralButton(R.string.dialog_format_not_supported_btn_issue, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            //open issues page + close app
                            Intent issueWebIntent = new Intent(Intent.ACTION_VIEW);
                            issueWebIntent.setData(Uri.parse(getString(R.string.yavp_new_issue_url)));
                            startActivity(issueWebIntent);
                            finish();
                        }
                    })
                    .setPositiveButton(R.string.dialog_format_not_supported_btn_exit, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            //close app
                            finish();
                        }
                    });
            builder.create().show();
        }

        return source;
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
            case R.id.pb_screen_rotation_auto:
            {
                //automatic/default screen rotation button
                screenRotationManager.setScreenMode(ScreenRotationManager.SCREEN_LOCK_PORTRAIT);
                break;
            }
            case R.id.pb_screen_rotation_portrait:
            {
                //lock screen to portrait button
                screenRotationManager.setScreenMode(ScreenRotationManager.SCREEN_LOCK_LANDSCAPE);
                break;
            }
            case R.id.pb_screen_rotation_landscape:
            {
                //lock screen to landscape button
                screenRotationManager.setScreenMode(ScreenRotationManager.SCREEN_AUTO);
                break;
            }
            //endregion

            //region ~~ Player Quick Settings ~~
            case R.id.qs_btn_quality:
            {
                //quality choose
                break;
            }
            case R.id.qs_btn_jump_to:
            {
                //jump to position in video:
                //show dialog
                JumpToFragment jumpTo = new JumpToFragment();
                jumpTo.show(getSupportFragmentManager(), player);

                //hide quick settings
                hideQuickSettingsDrawer();
                break;
            }
            case R.id.qs_btn_pip:
            {
                //open pip player
                tryGoPip();
                break;
            }
            case R.id.qs_btn_repeat_tgl:
            {
                //current video repeat toggle
                ControlQuickSettingsButton btnRepeatMode = findViewById(R.id.qs_btn_repeat_tgl);
                if (btnRepeatMode == null) break;

                if (player.getRepeatMode() == Player.REPEAT_MODE_OFF)
                {
                    //enable repeat_one
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);

                    //set button color to active color
                    btnRepeatMode.setIconTint(getColor(R.color.qs_item_icon_active));
                }
                else
                {
                    //disable repeat mode
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);

                    //set button color to disabled color
                    btnRepeatMode.setIconTint(getColor(R.color.qs_item_icon_default));
                }
                break;
            }
            case R.id.qs_btn_cast:
            {
                //player cast mode
                break;
            }
            case R.id.qs_btn_captions:
            {
                //subtitles toggle
                break;
            }
            case R.id.qs_btn_app_settings:
            {
                //open global app settings
                this.startActivity(new Intent(this, AppSettingsActivity.class));
                break;
            }

            //endregion
            case R.id.pb_quick_settings:
            {
                //open quick settings
                quickSettingsDrawer.openDrawer(GravityCompat.END);
                break;
            }
        }
    }
    //endregion

    //region ~~ PIP Mode ~~

    /**
     * Constants for PIP mode
     */
    private static final class PIPConstants
    {
        /**
         * Intent Action for media controls
         */
        private static final String ACTION_MEDIA_CONTROL = "media_control";

        /**
         * Intent Extra key for the request id
         */
        private static final String EXTRA_REQUEST_ID = "control_type";

        /**
         * Request id to Play/Pause playback
         */
        private static final int REQUEST_PLAY_PAUSE = 0;

        /**
         * Request id to Replay the video
         */
        private static final int REQUEST_REPLAY = 1;

        /**
         * Request id to fast- forward the video
         */
        private static final int REQUEST_FAST_FORWARD = 2;

        /**
         * Request id to rewind the video
         */
        private static final int REQUEST_REWIND = 3;
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode)
    {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);

        //hide controls when entering pip, re-enable when exiting pip
        setUseController(!isInPictureInPictureMode);

        //change the buffering spinner in pip mode
        updateBufferingSpinnerVisibility(isInPictureInPictureMode);

        if (isInPictureInPictureMode)
        {
            //changed to pip mode:
            //register broadcast receiver
            initPipBroadcastReceiverOnce();
            registerReceiver(pipBroadcastReceiver, new IntentFilter(PIPConstants.ACTION_MEDIA_CONTROL));

            //save + reset brightness
            savePersistentBrightness();

            //set screen brightness to 0 (=auto) (kinda hacky, but should work just fine)
            adjustScreenBrightness(-1000, true);
            delayHandler.sendEmptyMessage(Messages.SET_INFO_TEXT_INVISIBLE);
        }
        else
        {
            //changed to normal mode (no PIP):
            //remove broadcast receiver
            unregisterReceiver(pipBroadcastReceiver);

            //restore brightness
            restorePersistentBrightness();
        }
    }

    /**
     * Go into PIP mode
     */
    private void tryGoPip()
    {
        //lock out devices below API26 (don't support PIP)
        if (Util.SDK_INT < 26) return;

        //update pip and enter pip if update succeeded
        PictureInPictureParams params = updatePip();
        if (params != null)
        {
            //can enter pip mode, hide ui elements:
            //hide player controls
            setUseController(false);

            //hide quick settings drawer
            hideQuickSettingsDrawer();

            //hide info text immediately
            delayHandler.sendEmptyMessage(Messages.SET_INFO_TEXT_INVISIBLE);

            //enter pip
            enterPictureInPictureMode(params);
        }
    }

    /**
     * Initializes the Broadcast Receiver used to receive events in PIP mode
     * Initializes the receiver only once
     */
    private void initPipBroadcastReceiverOnce()
    {
        //only init once
        if (pipBroadcastReceiver != null) return;

        //initialize the receiver
        pipBroadcastReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                //ignore any intent that is not ACTION_MEDIA_CONTROL
                if (intent == null || intent.getAction() == null || !intent.getAction().equals(PIPConstants.ACTION_MEDIA_CONTROL))
                    return;

                //do stuff based on request id
                int rId = intent.getIntExtra(PIPConstants.EXTRA_REQUEST_ID, -1);
                switch (rId)
                {
                    case PIPConstants.REQUEST_PLAY_PAUSE:
                    {
                        //play/pause request, toggle playWhenReady
                        player.setPlayWhenReady(!player.getPlayWhenReady());
                        break;
                    }
                    case PIPConstants.REQUEST_REPLAY:
                    {
                        //replay request, set playWhenReady and seek to 0
                        player.seekTo(0);
                        player.setPlayWhenReady(true);
                        break;
                    }
                    case PIPConstants.REQUEST_FAST_FORWARD:
                    {
                        //fast- forward request, fast- forward video
                        player.seekTo(player.getCurrentPosition() + getPrefInt(ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT));
                        break;
                    }
                    case PIPConstants.REQUEST_REWIND:
                    {
                        //rewind request, rewind video
                        player.seekTo(player.getCurrentPosition() - getPrefInt(ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT));
                        break;
                    }
                    default:
                    {
                        //invalid request id, log
                        Logging.logW("Received Invalid PIP Request id: %d", rId);
                        break;
                    }
                }
            }
        };
    }

    /**
     * Update the controls of PIP mode
     * Only enter PIP mode when returned true using enterPictureInPictureMode()
     *
     * @return the set pictureInPictureParams object, or null if not supported
     */
    private PictureInPictureParams updatePip()
    {
        //lock out devices below API26 (don't support PIP)
        if (Util.SDK_INT < 26) return null;

        //create a list with all actions
        ArrayList<RemoteAction> actions = new ArrayList<>();

        //add buttons:
        //reverse button
        actions.add(createPipAction(PIPConstants.REQUEST_REWIND, R.drawable.ic_fast_rewind_48px, R.string.pip_title_f_rewind, R.string.exo_controls_rewind_description));

        //region ~~Play/Pause/Replay Button ~~
        if (player.getPlaybackState() == Player.STATE_ENDED)
        {
            //ended, show replay button
            actions.add(createPipAction(PIPConstants.REQUEST_REPLAY, R.drawable.ic_replay_48px, R.string.pip_title_replay, R.string.exo_controls_play_description));
        }
        else
        {
            //playing or paused
            if (player.getPlayWhenReady())
            {
                //currently playing, show pause button
                actions.add(createPipAction(PIPConstants.REQUEST_PLAY_PAUSE, R.drawable.ic_pause_48px, R.string.pip_title_pause, R.string.exo_controls_pause_description));
            }
            else
            {
                //currently paused, show play button
                actions.add(createPipAction(PIPConstants.REQUEST_PLAY_PAUSE, R.drawable.ic_play_arrow_48px, R.string.pip_title_play, R.string.exo_controls_play_description));
            }
        }
        //endregion

        //fast- forward button
        actions.add(createPipAction(PIPConstants.REQUEST_FAST_FORWARD, R.drawable.ic_fast_forward_48px, R.string.pip_title_f_forward, R.string.exo_controls_fastforward_description));

        //built the pip params
        PictureInPictureParams params = new PictureInPictureParams.Builder().setActions(actions).build();

        //this is how you update action items (etc.) for PIP mode.
        //this call can happen even if not in pip mode. In that case, the params
        //will be used for the next call of enterPictureInPictureMode
        setPictureInPictureParams(params);
        return params;
    }

    /**
     * Create a RemoteAction with MEDIA_CONTROL action and @requestID as extra REQUEST_ID
     *
     * @param requestId            the request id
     * @param resId                the ressource id for the icon
     * @param titleId              the title string id of the action
     * @param contentDescriptionId the description string id of the action
     * @return the remote action, or null if the Android Device does not support PIP (<API26)
     */
    private RemoteAction createPipAction(int requestId, int resId, int titleId, int contentDescriptionId)
    {
        //make absolutely sure that device is >= API26
        if (Util.SDK_INT < 26) return null;

        //create pending intent with MEDIA_CONTROL action and requestID as extra REQUEST_ID
        //PendingIntent pIntent = PendingIntent.getActivity(this, requestId,
        //        new Intent(PIPConstants.ACTION_MEDIA_CONTROL).putExtra(PIPConstants.EXTRA_REQUEST_ID, requestId), 0);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, requestId,
                new Intent(PIPConstants.ACTION_MEDIA_CONTROL).putExtra(PIPConstants.EXTRA_REQUEST_ID, requestId), 0);

        //get the icon of the action by resId
        Icon icon = Icon.createWithResource(this, resId);

        //create the remote action
        return new RemoteAction(icon, getString(titleId), getString(contentDescriptionId), pIntent);
    }

    //endregion

    //region ~~ Utility ~~

    /**
     * Closes the quick settings drawer
     */
    private void hideQuickSettingsDrawer()
    {
        //quickSettingsDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        quickSettingsDrawer.closeDrawer(GravityCompat.END);
    }

    /**
     * Save the current volume as persistent volume
     */
    private void savePersistentVolume()
    {
        if (getPrefBool(ConfigKeys.KEY_PERSIST_VOLUME_EN, R.bool.DEF_PERSIST_VOLUME_EN))
            appPreferences.edit().putInt(ConfigKeys.KEY_PERSIST_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).apply();
    }

    /**
     * Save the current brightness as persistent brightness
     */
    private void savePersistentBrightness()
    {
        if (getPrefBool(ConfigKeys.KEY_PERSIST_BRIGHTNESS_EN, R.bool.DEF_PERSIST_BRIGHTNESS_EN))
            appPreferences.edit().putInt(ConfigKeys.KEY_PERSIST_BRIGHTNESS, (int) Math.floor(getWindow().getAttributes().screenBrightness * 100)).apply();
    }

    /**
     * restore the saved persistent volume level
     */
    private void restorePersistentVolume()
    {
        if (getPrefBool(ConfigKeys.KEY_PERSIST_VOLUME_EN, R.bool.DEF_PERSIST_VOLUME_EN))
        {
            int persistVolume = appPreferences.getInt(ConfigKeys.KEY_PERSIST_VOLUME, originalVolumeIndex);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, persistVolume, 0);
        }
    }

    /**
     * Restore the saved persistent brightness level
     */
    private void restorePersistentBrightness()
    {
        if (getPrefBool(ConfigKeys.KEY_PERSIST_BRIGHTNESS_EN, R.bool.DEF_PERSIST_BRIGHTNESS_EN))
        {
            //get window attributes
            WindowManager.LayoutParams windowAttributes = getWindow().getAttributes();

            //modify screen brightness attribute withing range
            float persistBrightness = (float) appPreferences.getInt(ConfigKeys.KEY_PERSIST_BRIGHTNESS, -100) / 100.0f;
            if (persistBrightness > 0)
            {
                windowAttributes.screenBrightness = Math.min(Math.max(persistBrightness, 0f), 1f);
            }

            //set changed window attributes
            getWindow().setAttributes(windowAttributes);
        }
    }

    /**
     * Get a boolean from shared preferences
     *
     * @param key   the key of the value
     * @param defId the id of the default value in R.bool
     * @return the boolean value
     */
    private boolean getPrefBool(String key, int defId)
    {
        Logging.logD("Getting Boolean Preference \"%s\"...", key);
        boolean def = getResources().getBoolean(defId);
        return appPreferences.getBoolean(key, def);
    }

    /**
     * Get a int from shared preferences
     *
     * @param key   the key of the value
     * @param defId the id of the default value in R.integer
     * @return the int value
     */
    private int getPrefInt(String key, int defId)
    {
        Logging.logD("Getting Integer Preference \"%s\"...", key);
        int def = getResources().getInteger(defId);

        //read value as string: workaround needed because i'm using a EditText to change these values in the settings activity, which
        //changes the type of the preference to string...
        return Integer.valueOf(appPreferences.getString(key, "" + def));
    }

    /**
     * Check if the player is currently playing
     *
     * @return is it?
     */
    private boolean isPlaying()
    {
        return player != null && player.getPlaybackState() == Player.STATE_READY && player.getPlayWhenReady();
    }

    /**
     * Update the visibility of the buffering spinners.
     *
     * @param pip if set, the pip spinner is made visible, otherwise, the normal spinner is visible
     */
    private void updateBufferingSpinnerVisibility(boolean pip)
    {
        //get spinners
        View spinnerNormal = findViewById(R.id.pb_playerBufferingWheel_normal);
        View spinnerPip = findViewById(R.id.pb_playerBufferingWheel_pipmode);

        //enable/disable
        if (spinnerNormal != null) spinnerNormal.setVisibility(pip ? View.INVISIBLE : View.VISIBLE);
        if (spinnerPip != null) spinnerPip.setVisibility(pip ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Set the title shown on the titleTextView
     *
     * @param title the title to show. if set to a empty string, the title label is hidden
     */
    private void setTitle(String title)
    {
        if (title == null || title.isEmpty())
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
     * Set the visibility of the player controller
     *
     * @param useController should the controller be used?
     */
    private void setUseController(boolean useController)
    {
        //skip if player control view is null
        if (playerControlView == null) return;

        if (useController)
        {
            if (playerControlView.getPlayer() != player)
            {
                //has no or wrong player, assign it
                playerControlView.setPlayer(player);
            }

            //show controls
            playerControlView.show();
        }
        else
        {
            //hide controls
            playerControlView.hide();
        }
    }

    /**
     * Set if the player is currently buffering
     * (buffering progress bar visibility)
     *
     * @param isBuffering true if buffering, false if not
     */
    private void setBuffering(boolean isBuffering)
    {
        bufferingSpinner.setVisibility(isBuffering ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Show info text in the middle of the screen, using the InfoText View
     *
     * @param text   the text to show
     * @param format formatting options (using US locale)
     */
    private void showInfoText(String text, Object... format)
    {
        //remove all previous messages related to fading out the info text
        delayHandler.removeMessages(Messages.START_FADE_OUT_INFO_TEXT);
        delayHandler.removeMessages(Messages.SET_INFO_TEXT_INVISIBLE);

        //set text
        infoTextView.setText(String.format(text, format));

        //reset animation and make text visible
        infoTextView.setAnimation(null);
        infoTextView.setVisibility(View.VISIBLE);

        //get how long the text should be visible


        //hide text after delay
        delayHandler.sendEmptyMessageDelayed(Messages.START_FADE_OUT_INFO_TEXT, infoTextDurationMs);
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
    private class ExoEventListener implements Player.EventListener
    {
        @Override
        public void onLoadingChanged(boolean isLoading)
        {
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
        {
            //update buffering progress bar
            setBuffering(playbackState == Player.STATE_BUFFERING);

            //get the play button
            ImageButton playButton = findViewById(R.id.exo_play);
            if (replayButtonVisible)
            {
                //reset graphic to the play button if needed
                playButton.setImageResource(R.drawable.ic_play_arrow_48px);
                replayButtonVisible = false;
            }

            //update PIP controls if in pip mode
            if (isInPictureInPictureMode())
                updatePip();

            switch (playbackState)
            {
                case Player.STATE_IDLE:
                {
                    //player stopped or failed, release screen lock
                    forceScreenOn(false);
                    break;
                }
                case Player.STATE_BUFFERING:
                {
                    //media currently buffering
                    break;
                }
                case Player.STATE_READY:
                {
                    if (playWhenReady)
                    {
                        //currently playing back, engage screen lock
                        forceScreenOn(true);
                    }
                    else
                    {
                        //ready for playback (paused or not started yet), release screen lock
                        forceScreenOn(false);
                    }
                    break;
                }
                case Player.STATE_ENDED:
                {
                    //media playback ended:

                    //release screen lock
                    forceScreenOn(false);

                    if (getPrefBool(ConfigKeys.KEY_CLOSE_WHEN_FINISHED_PLAYING, R.bool.DEF_CLOSE_WHEN_FINISHED_PLAYING))
                    {
                        //close app
                        finish();
                    }
                    else
                    {
                        //change play button graphic to replay button
                        playButton.setImageResource(R.drawable.ic_replay_48px);
                        replayButtonVisible = true;
                    }
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
            //a error occurred:
            //log error
            Logging.logE("ExoPlayer error occurred: %s", error.toString());

            //show dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            builder.setTitle(R.string.dialog_player_error_title)
                    .setMessage(error.toString())
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_player_error_btn_exit, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            //close app
                            finish();
                        }
                    });
            builder.create().show();
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
    private class ScreenRotationManager
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
            btnScreenAuto = findViewById(R.id.pb_screen_rotation_auto);
            btnScreenLockLandscape = findViewById(R.id.pb_screen_rotation_landscape);
            btnScreenLockPortrait = findViewById(R.id.pb_screen_rotation_portrait);
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
