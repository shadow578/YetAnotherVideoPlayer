package de.shadow578.yetanothervideoplayer.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.YAVPApp;
import de.shadow578.yetanothervideoplayer.feature.gl.GLAnime4K;
import de.shadow578.yetanothervideoplayer.playback.VideoPlaybackService;
import de.shadow578.yetanothervideoplayer.playback.VideoPlaybackServiceListener;
import de.shadow578.yetanothervideoplayer.ui.components.ControlQuickSettingsButton;
import de.shadow578.yetanothervideoplayer.ui.components.PlayerControlViewWrapper;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.Logging;
import de.shadow578.yetanothervideoplayer.feature.swipe.SwipeGestureListener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.SizeF;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.daasuu.epf.EPlayerView;
import com.daasuu.epf.PlayerScaleType;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.util.Util;
import com.pnikosis.materialishprogress.ProgressWheel;

import java.util.ArrayList;

public class PlaybackActivity extends AppCompatActivity implements YAVPApp.ICrashListener
{
    //region ~~ Constants ~~

    /**
     * Id of permission request for external storage
     */
    private static final int PERMISSION_REQUEST_READ_EXT_STORAGE = 0;

    /**
     * Interval in which the battery level is checked
     */
    private static final int BATTERY_WARN_CHECK_INTERVAL_MS = 10000;

    /**
     * Intent Extra key that tells the player to immediately jump to the given position in the video
     */
    public static final String INTENT_EXTRA_JUMP_TO = "jumpTo";
    //endregion

    //region ~~ Variables ~~
    //region static views
    /**
     * The topmost view of the playback activity
     */
    private View playbackRootView;

    /**
     * The drawer layout that contains the quick settings and shader effect drawers
     */
    private DrawerLayout quickAccessDrawer;

    /**
     * the placeholder layout that the player view will be added to as a child
     */
    private FrameLayout playerViewPlaceholder;

    /**
     * The normal buffering indicator (not pip)
     */
    private ProgressWheel bufferingIndicatorNormal;

    /**
     * the buffering indicator for pip mode
     */
    private ProgressWheel bufferingIndicatorPip;

    /**
     * The play button in the center of the screen
     */
    private ImageButton playButton;

    /**
     * The TextView in the center of the screen, used to show information
     */
    private TextView infoTextView;

    /**
     * The Text View used to display the stream title
     */
    private TextView titleTextView;

    /**
     * The Anime4K Quick Settings button
     */
    private ControlQuickSettingsButton anime4kQSButton;

    //endregion

    /**
     * The Preferences of the app
     */
    private SharedPreferences appPreferences;

    /**
     * The connection to the video playback service
     */
    private VideoServiceConnection playbackServiceConnection;

    /**
     * video playback service that is home to the player
     */
    private VideoPlaybackService playbackService;

    /**
     * The View the Player Renders Video to
     */
    private EPlayerView playerView;

    /**
     * The View that contains and controls the player controls
     */
    private PlayerControlViewWrapper playerControlView;

    /**
     * The uri of the media requested to be played
     */
    private Uri playbackUri;

    /**
     * Battery manager system service. used to send a warning to the screen when battery charge is dropping below a threshold value
     */
    private BatteryManager batteryManager;

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
     * The Anime4K filter instance that may be active currently.
     * Set to null if filter is inactive
     */
    private GLAnime4K anime4KFilter;

    /**
     * the position playback should start at
     */
    private long playbackStartPosition = 0;

    /**
     * Should the playback start as soon as media is ready?
     */
    private boolean playbackPlayWhenReady = false;

    /**
     * The original volume index before the playback activity was opened
     */
    private int originalVolumeIndex;

    /**
     * is the app currently in pip mode?
     */
    private boolean isPictureInPicture = false;

    /**
     * has the current media ended?
     * (= is the replay button visible?)
     */
    private boolean isPlaybackEnded = false;

    /**
     * flag to indicate that a battery low warning was shown to the user
     */
    private boolean wasBatteryWarningShown = false;

    /**
     * Was the Back button pressed once already?
     * Used for "Press Back again to exit" function
     */
    private boolean wasBackPressedOnce;
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
         * Message id to reset wasBackPressedOnce flag
         */
        private static final int RESET_BACK_PRESSED = 2;

        /**
         * Message to check the battery level. (only call once - this message calls itself)
         */
        private static final int BATTERY_WARN_CHECK = 3;
    }

    /**
     * Shared handler that can be used to invoke methods and/or functions with a delay,
     */
    private final Handler delayHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg)
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
                    wasBackPressedOnce = false;
                    break;
                }
                case Messages.BATTERY_WARN_CHECK:
                {
                    //abort if there is no battery manager
                    if (batteryManager == null) return;

                    //don't send a warning when charging
                    if (!batteryManager.isCharging())
                    {
                        //get battery level and warn threshold in percent
                        int batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                        int batteryThresh = getPrefInt(ConfigKeys.KEY_BATTERY_WARN_THRESHOLD, R.integer.DEF_BATTERY_WARN_THRESHOLD);

                        //check level against threshold
                        Logging.logD("[BatWarn] Battery is at %d %% - warn threshold is %d %%", batteryPercent, batteryThresh);
                        if (batteryPercent <= batteryThresh && !wasBatteryWarningShown)
                        {
                            //send a warning
                            showInfoText(getText(R.string.info_battery_low).toString());
                            wasBatteryWarningShown = true;
                        }
                    }
                    else
                    {
                        //reset warning shown when started charging
                        wasBatteryWarningShown = false;
                    }

                    //call self later
                    delayHandler.sendEmptyMessageDelayed(Messages.BATTERY_WARN_CHECK, BATTERY_WARN_CHECK_INTERVAL_MS);
                    break;
                }
            }
        }
    };

    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Logging.logD("onCreate of PlaybackActivity called.");
        super.onCreate(savedInstanceState);

        //make app fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //set layout
        setContentView(R.layout.activity_playback);

        //get preferences
        appPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //get views
        playbackRootView = findViewById(R.id.pb_playbackRootView);
        //playerView = findViewById(R.id.pb_playerView);
        playerViewPlaceholder = findViewById(R.id.pb_playerViewPlaceholder);
        playerControlView = findViewById(R.id.pb_playerControlView);
        infoTextView = findViewById(R.id.pb_infoText);
        titleTextView = findViewById(R.id.pb_streamTitle);
        quickAccessDrawer = findViewById(R.id.pb_quick_settings_drawer);
        anime4kQSButton = findViewById(R.id.qs_btn_a4k_tgl);
        bufferingIndicatorNormal = findViewById(R.id.pb_playerBufferingWheel_normal);
        bufferingIndicatorPip = findViewById(R.id.pb_playerBufferingWheel_pipmode);
        playButton = findViewById(R.id.exo_play);

        //set fast-forward and rewind increments
        int seekIncrement = getPrefInt(ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT);
        playerControlView.setFastForwardIncrementMs(seekIncrement);
        playerControlView.setRewindIncrementMs(seekIncrement);

        //init screen rotation manager
        screenRotationManager = new ScreenRotationManager();
        screenRotationManager.findComponents();

        //get battery manager service
        if (getPrefBool(ConfigKeys.KEY_BATTERY_WARN_ENABLE, R.bool.DEF_BATTERY_WARN_ENABLE))
        {
            batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

            //queue first check
            delayHandler.sendEmptyMessageDelayed(Messages.BATTERY_WARN_CHECK, BATTERY_WARN_CHECK_INTERVAL_MS);
        }

        //get audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //setup gesture controls
        setupGestures();

        //setup auto- pause manager
        //autoPauseManager = new AutomaticPauseManager(this);
        //if (!autoPauseManager.initialize())
        //{
        //    Logging.logW("Initialize of autoPauseManager failed!");
        //    autoPauseManager = null;
        //}

        //set this activity as a crash handler so we can save the playback position on crashes
        Application app = getApplication();
        if (app instanceof YAVPApp)
        {
            Logging.logD("Set self as crash listener...");
            YAVPApp yavpApp = (YAVPApp) app;
            yavpApp.setCrashListener(this);
        }
        else
        {
            Logging.logW("getApplication() is not instance of YAVPApp!");
        }

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

        //get position to start playback at
        playbackStartPosition = callIntent.getLongExtra(INTENT_EXTRA_JUMP_TO, 0);

        //get auto play when launching
        playbackPlayWhenReady = getPrefBool(ConfigKeys.KEY_AUTO_PLAY, R.bool.DEF_AUTO_PLAY);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults)
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
                    //have permissions now, start playing by reload
                    if (playbackService != null)
                        playbackService.reloadMedia();
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

        //create and bind video playback service
        playbackServiceConnection = new VideoServiceConnection();
        bindService(new Intent(this, VideoPlaybackService.class), playbackServiceConnection, Context.BIND_AUTO_CREATE);

        //get pref for play when ready
        //boolean playWhenReady = getPrefBool(ConfigKeys.KEY_AUTO_PLAY, R.bool.DEF_AUTO_PLAY);

        //load the media
        //playbackService.loadMedia(playbackUri, playWhenReady, playbackStartPosition);

        //update autoPauseManager
        //if (autoPauseManager != null) autoPauseManager.activate();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Logging.logD("onResume of PlaybackActivity called.");

        //restore volume and brightness
        restorePersistentValues(true);

        //update autoPauseManager
        //if (autoPauseManager != null) autoPauseManager.activate();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        Logging.logD("onPause of PlaybackActivity called.");

        //save volume and brightness
        savePersistentValues(true);

        //update autoPauseManager
        //if (autoPauseManager != null) autoPauseManager.deactivate();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Logging.logD("onStop of PlaybackActivity called.");

        //save playback position to prefs to be able to resume later
        savePlaybackPosition();

        //disconnect from the service
        disconnectPlaybackService();

        //cancel everything on the handler
        delayHandler.removeCallbacksAndMessages(null);

        //update autoPauseManager
        //if (autoPauseManager != null) autoPauseManager.deactivate();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        //stop the playback service when the app is stopped
        stopService(new Intent(this, VideoPlaybackService.class));
    }

    /**
     * disconnects the playback service safely
     * !! does not stop the service (no stopService() !!
     */
    private void disconnectPlaybackService()
    {
        Logging.logD("Disconnecting from Playback service...");

        //check if service is bound first
        if (playbackServiceConnection == null || !playbackServiceConnection.isConnected)
        {
            //not connected!
            Logging.logW("Playback service is already disconnected?! skipping disconnect.");
            return;
        }

        //prepare playback service for disconnect
        if (playbackService != null)
        {
            //save playback position and play when ready flag before unbinding service
            //so that we can resume where we left off when brought back from background
            playbackStartPosition = playbackService.getPlaybackPosition();
            playbackPlayWhenReady = playbackService.getPlayWhenReady();

            //remove self as listener
            playbackService.setListener(null);
        }

        //unbind the service
        unbindService(playbackServiceConnection);
    }

    @Override
    protected void onUserLeaveHint()
    {
        super.onUserLeaveHint();

        //enter pip mode if enabled
        if (getPrefBool(ConfigKeys.KEY_ENTER_PIP_ON_LEAVE, R.bool.DEF_ENTER_PIP_ON_LEAVE) && playbackService.getIsPlaying())
            tryGoPip();
    }

    @Override
    public void onBackPressed()
    {
        if (wasBackPressedOnce || !getPrefBool(ConfigKeys.KEY_BACK_DOUBLE_PRESS_EN, R.bool.DEF_BACK_DOUBLE_PRESS_EN))
        {
            //back pressed once already, do normal thing...
            super.onBackPressed();
            return;
        }

        //pressed back the first time, set flag
        wasBackPressedOnce = true;

        //send reset message delayed
        delayHandler.sendEmptyMessageDelayed(Messages.RESET_BACK_PRESSED, getPrefInt(ConfigKeys.KEY_BACK_DOUBLE_PRESS_TIMEOUT, R.integer.DEF_BACK_DOUBLE_PRESS_TIMEOUT));

        //show user a Toast
        Toast.makeText(this, getString(R.string.toast_press_back_again_to_exit), Toast.LENGTH_SHORT).show();
    }

    /**
     * Called on a crash shortly before the app is closed
     * This is part of the apps lifecycle ;)
     *
     * @param ex the exception that caused the crash
     */
    public void onCrash(Throwable ex)
    {
        //save playback position before app closes
        Logging.logD("Saving Playback position on app crash...");
        savePlaybackPosition();
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
        int touchDecayTime = getPrefInt(ConfigKeys.KEY_TOUCH_DECAY_TIME, R.integer.DEF_TOUCH_DECAY_TIME);

        playbackRootView.setOnTouchListener(new SwipeGestureListener(touchDecayTime, swipeFlingThreshold, swipeFlingThreshold,
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
                Logging.logD("HFling: " + deltaX + " dp");
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
                //jump to position in video, show dialog
                JumpToFragment jumpTo = new JumpToFragment();
                jumpTo.show(getSupportFragmentManager(), playbackService.getPlayerInstance());

                //hide quick settings and effect drawers
                quickAccessDrawer.closeDrawers();
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

                //toggle looping state
                playbackService.setLooping(!playbackService.getLooping());

                //update ui
                btnRepeatMode.setIconTint(playbackService.getLooping() ? getColor(R.color.qs_item_icon_active) : getColor(R.color.qs_item_icon_default));
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
                startActivity(new Intent(this, AppSettingsActivity.class));
                break;
            }
            case R.id.qs_btn_a4k_tgl:
            {
                //toggle anime4k on/off
                setAnime4kEnabled(!getIsAnime4kEnabled());

                //update button
                anime4kQSButton.setIconTint(getIsAnime4kEnabled() ? getColor(R.color.qs_item_icon_active) : getColor(R.color.qs_item_icon_default));
                break;
            }
            //endregion

            case R.id.pb_quick_settings:
            {
                //open quick settings
                quickAccessDrawer.openDrawer(GravityCompat.END);
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

        if (isInPictureInPictureMode)
        {
            //changed to pip mode, register broadcast receiver
            initPipBroadcastReceiverOnce();
            registerReceiver(pipBroadcastReceiver, new IntentFilter(PIPConstants.ACTION_MEDIA_CONTROL));

            //TODO: test on real device
            //set screen brightness to 0 (=auto) (kinda hacky, but should work just fine)
            //adjustScreenBrightness(-1000, true);

            //hide info text
            delayHandler.sendEmptyMessage(Messages.SET_INFO_TEXT_INVISIBLE);
        }
        else
        {
            //changed to normal mode, remove pip broadcast receiver
            unregisterReceiver(pipBroadcastReceiver);
        }

        //set flag for outside use
        isPictureInPicture = isInPictureInPictureMode;
    }

    /**
     * Go into PIP mode
     */
    private void tryGoPip()
    {
        //lock out devices below API26 (don't support PIP)
        if (Util.SDK_INT < 26) return;

        //update pip and enter pip if update succeeded
        PictureInPictureParams params = updatePipControls();
        if (params != null)
        {
            //can enter pip mode, hide ui elements:
            //hide player controls
            setUseController(false);

            //hide quick settings and effect drawers
            quickAccessDrawer.closeDrawers();

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
                        playbackService.setPlayWhenReady(!playbackService.getPlayWhenReady());
                        break;
                    }
                    case PIPConstants.REQUEST_REPLAY:
                    {
                        //replay request, set playWhenReady and seek to 0
                        playbackService.seekTo(0);
                        playbackService.setPlayWhenReady(true);
                        break;
                    }
                    case PIPConstants.REQUEST_FAST_FORWARD:
                    {
                        //fast- forward request, fast- forward video
                        playbackService.seekRelative(getPrefInt(ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT));
                        break;
                    }
                    case PIPConstants.REQUEST_REWIND:
                    {
                        //rewind request, rewind video
                        playbackService.seekRelative(-getPrefInt(ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT));
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
    private PictureInPictureParams updatePipControls()
    {
        //lock out devices below API26 (don't support PIP)
        if (Util.SDK_INT < 26) return null;

        //create a list with all actions
        ArrayList<RemoteAction> actions = new ArrayList<>();

        //add buttons:
        //reverse button
        actions.add(createPipAction(PIPConstants.REQUEST_REWIND, R.drawable.ic_fast_rewind_black_48dp, R.string.pip_title_f_rewind, R.string.exo_controls_rewind_description));

        //region ~~Play/Pause/Replay Button ~~
        if (isPlaybackEnded)
        {
            //ended, show replay button
            actions.add(createPipAction(PIPConstants.REQUEST_REPLAY, R.drawable.ic_replay_black_48dp, R.string.pip_title_replay, R.string.exo_controls_play_description));
        }
        else
        {
            //playing or paused
            if (playbackService.getPlayWhenReady())
            {
                //currently playing, show pause button
                actions.add(createPipAction(PIPConstants.REQUEST_PLAY_PAUSE, R.drawable.ic_pause_black_48dp, R.string.pip_title_pause, R.string.exo_controls_pause_description));
            }
            else
            {
                //currently paused, show play button
                actions.add(createPipAction(PIPConstants.REQUEST_PLAY_PAUSE, R.drawable.ic_play_arrow_black_48dp, R.string.pip_title_play, R.string.exo_controls_play_description));
            }
        }
        //endregion

        //fast- forward button
        actions.add(createPipAction(PIPConstants.REQUEST_FAST_FORWARD, R.drawable.ic_fast_forward_black_48dp, R.string.pip_title_f_forward, R.string.exo_controls_fastforward_description));

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
     * Enable / disable anime4k filter
     *
     * @param enable enable anime4k?
     */
    private void setAnime4kEnabled(boolean enable)
    {
        Logging.logD("Setting Anime4K Filter to enabled= %b", enable);
        if (enable)
        {
            if (anime4KFilter == null)
            {
                //filter currently not enabled, enable it
                anime4KFilter = new GLAnime4K(this, R.raw.common, R.raw.colorget, R.raw.colorpush, R.raw.gradget, R.raw.gradpush);

                //set anime4k to one pass only
                anime4KFilter.setPasses(1);

                //set a4k as active filter
                playerView.setGlFilter(anime4KFilter);

                //set a4k as video listener
                if (playbackService.getIsPlayerValid())
                {
                    SimpleExoPlayer playerInstance = playbackService.getPlayerInstance();
                    if (playerInstance != null)
                    {
                        playerInstance.addVideoListener(anime4KFilter);
                    }
                }
                //set fps limiting values
                int fpsLimit = -1;
                if (getPrefBool(ConfigKeys.KEY_ANIME4K_FPS_LIMIT_ENABLE, R.bool.DEF_ANIME4K_FPS_LIMIT_EN))
                {
                    //enable the fps limit
                    fpsLimit = getPrefInt(ConfigKeys.KEY_ANIME4K_FPS_LIMIT, R.integer.DEF_ANIME4K_FPS_LIMIT);
                }
                anime4KFilter.setFpsLimit(fpsLimit);
                Logging.logD("Enabled Anime4K with fps limit= %d", fpsLimit);
            }
        }
        else
        {
            //if filter is currently enabled, disable it
            if (anime4KFilter != null)
            {
                //remove video listener
                if (playbackService.getIsPlayerValid())
                {
                    SimpleExoPlayer playerInstance = playbackService.getPlayerInstance();
                    if (playerInstance != null)
                    {
                        playerInstance.removeVideoListener(anime4KFilter);
                    }
                }

                //remove filter (this calls release on the filter)
                playerView.setGlFilter(null);

                //set variable to null
                anime4KFilter = null;
                Logging.logD("Disabled Anime4K");
            }
        }
    }

    /**
     * @return is the anime4k filter currently enabled?
     */
    private boolean getIsAnime4kEnabled()
    {
        return anime4KFilter != null;
    }

    /**
     * Save the current playback position to save as LAST_PLAYED_POSITION for use in the "resume where i left off" feature
     */
    private void savePlaybackPosition()
    {
        //check playback service is ok
        if (playbackService == null || !playbackService.getIsPlayerValid())
        {
            Logging.logD("Cannot save current playback position: player or playback service invalid!");
            return;
        }

        //get current position
        long pos = playbackService.getPlaybackPosition();

        //save the current position
        savePlaybackPosition(pos);
    }

    /**
     * Save the given playback position to save as LAST_PLAYED_POSITION for use in the "resume where i left off" feature
     *
     * @param positionToSave the playback position to save
     */
    @SuppressLint("ApplySharedPref")
    private void savePlaybackPosition(long positionToSave)
    {
        //check that the prefs are ok
        if (appPreferences == null)
        {
            Logging.logD("cannot save playback position: appPreferences are null!");
            return;
        }

        //save to prefs
        //we use .commit() here since the main thread could close pretty much the moment this function returns.
        //this would not leave enough time for saving the preferences using .apply() (which is async)...
        appPreferences.edit().putLong(ConfigKeys.KEY_LAST_PLAYED_POSITION, positionToSave).commit();
        Logging.logD("Saved LAST_PLAYED_POSITION");
    }

    /**
     * saves the current volume and brightness to app preferences
     *
     * @param restoreOriginalVolume should the original volume be restored?
     */
    private void savePersistentValues(@SuppressWarnings("SameParameterValue") boolean restoreOriginalVolume)
    {
        //save volume
        if (getPrefBool(ConfigKeys.KEY_PERSIST_VOLUME_EN, R.bool.DEF_PERSIST_VOLUME_EN))
            appPreferences.edit().putInt(ConfigKeys.KEY_PERSIST_VOLUME, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)).apply();

        //save brightness
        if (getPrefBool(ConfigKeys.KEY_PERSIST_BRIGHTNESS_EN, R.bool.DEF_PERSIST_BRIGHTNESS_EN))
            appPreferences.edit().putInt(ConfigKeys.KEY_PERSIST_BRIGHTNESS, (int) Math.floor(getWindow().getAttributes().screenBrightness * 100)).apply();

        //restore original volume after saving
        if (restoreOriginalVolume)
        {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolumeIndex, 0);
        }
    }

    /**
     * Restores persistent volume and brightness from app preferences
     *
     * @param saveOriginalVolume should the original volume be saved?
     */
    private void restorePersistentValues(@SuppressWarnings("SameParameterValue") boolean saveOriginalVolume)
    {
        //save original volume before restore
        if (saveOriginalVolume)
        {
            originalVolumeIndex = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        //restore volume
        if (getPrefBool(ConfigKeys.KEY_PERSIST_VOLUME_EN, R.bool.DEF_PERSIST_VOLUME_EN))
        {
            int persistVolume = appPreferences.getInt(ConfigKeys.KEY_PERSIST_VOLUME, originalVolumeIndex);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, persistVolume, 0);
        }

        //restore brightness
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

        //hide text after delay
        int infoTextDuration = getPrefInt(ConfigKeys.KEY_INFO_TEXT_DURATION, R.integer.DEF_INFO_TEXT_DURATION);
        delayHandler.sendEmptyMessageDelayed(Messages.START_FADE_OUT_INFO_TEXT, infoTextDuration);
    }

    /**
     * Check if the app was granted the permission.
     * If not granted, the permission will be requested and false will be returned.
     *
     * @param permission the permission to check
     * @param requestId  the request id. Used to check in callback
     * @return was the permission granted?
     */
    private boolean checkAndRequestPermission(@SuppressWarnings("SameParameterValue") String permission, @SuppressWarnings("SameParameterValue") int requestId)
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
        Logging.logD("setUseController() useController= %b", useController);

        //skip if not everything is ok
        if (playerControlView == null || playbackService == null || !playbackService.getIsPlayerValid())
        {
            Logging.logW("setUseController(): either playerControlView or playbackService is not valid!");
            return;
        }

        if (useController)
        {
            if (playerControlView.getPlayer() != playbackService.getPlayerInstance())
            {
                //player not set, fix that
                playerControlView.setPlayer(playbackService.getPlayerInstance());
            }

            //show controls
            playerControlView.setControlsHidden(false);
            playerControlView.show();
        }
        else
        {
            //hide controls
            playerControlView.setControlsHidden(true);
            playerControlView.hide();
        }
    }

    /**
     * set the visibility of the buffering indicator
     *
     * @param isBuffering is currently buffering? (=visible)
     * @param isPip       is the app in pip mode?
     */
    private void setBufferingIndicatorVisible(boolean isBuffering, boolean isPip)
    {
        if (isBuffering)
        {
            //make right indicator visible
            if (bufferingIndicatorNormal != null)
                bufferingIndicatorNormal.setVisibility(isPip ? View.GONE : View.VISIBLE);
            if (bufferingIndicatorPip != null)
                bufferingIndicatorPip.setVisibility(isPip ? View.VISIBLE : View.GONE);
        }
        else
        {
            //make both invisible
            if (bufferingIndicatorNormal != null) bufferingIndicatorNormal.setVisibility(View.GONE);
            if (bufferingIndicatorPip != null) bufferingIndicatorPip.setVisibility(View.GONE);
        }
    }

    /**
     * Forces the screen to stay on if keepOn is true, otherwise clears the KEEP_SCREEN_ON flag
     *
     * @param keepOn should the screen stay on?
     */
    private void setScreenForcedOn(boolean keepOn)
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

    /**
     * Get a boolean from shared preferences
     *
     * @param key   the key of the value
     * @param defId the id of the default value in R.bool
     * @return the boolean value
     */
    private boolean getPrefBool(String key, int defId)
    {
        boolean def = getResources().getBoolean(defId);
        boolean value = appPreferences.getBoolean(key, def);

        //log the value
        Logging.logD("getPrefBool(): key= %s; val= %b", key, value);
        return value;
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
        int def = getResources().getInteger(defId);

        //read value as string: workaround needed because i'm using a EditText to change these values in the settings activity, which
        //changes the type of the preference to string...
        int value = Integer.valueOf(appPreferences.getString(key, "" + def));

        //log the value
        Logging.logD("getPrefInt(): key= %s; val= %d", key, value);
        return value;
    }
    //endregion

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

    /**
     * The connection to the video playback service
     */
    private class VideoServiceConnection implements ServiceConnection
    {
        /**
         * Is the video playback service currently bound?
         */
        boolean isConnected = false;

        /**
         * called when we connected to the service
         *
         * @param componentName ?
         * @param binder        the service binder we can use to access the service
         */
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder)
        {
            Logging.logD("Service connected!");

            //check binder is right type
            if (binder instanceof VideoPlaybackService.VideoServiceBinder)
            {
                //set service instance
                VideoPlaybackService.VideoServiceBinder serviceBinder = (VideoPlaybackService.VideoServiceBinder) binder;
                playbackService = serviceBinder.getServiceInstance();

                //set flag
                isConnected = true;

                //set callback
                playbackService.setListener(new VideoServiceCallbackListener());

                //load the media
                playbackService.loadMedia(playbackUri, playbackPlayWhenReady, playbackStartPosition);
            }
        }

        /**
         * Called when we disconnected from the service
         *
         * @param componentName ?
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            Logging.logD("Service Disconnected!");

            //set flag
            isConnected = false;
        }
    }

    /**
     * listens for callbacks of the video service
     */
    private class VideoServiceCallbackListener implements VideoPlaybackServiceListener
    {
        /**
         * the player of the service has been initialized and can now be used to, for example, set the render surface.
         * this is called in onBind() of the service as well as in the end of loadMedia() function
         */
        @Override
        public void onPlayerInitialized()
        {
            //create new opengl player view
            playerView = new EPlayerView(playerViewPlaceholder.getContext());

            //adjust layout
            playerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            //set player of view
            playerView.setSimpleExoPlayer(playbackService.getPlayerInstance());

            //we want the video to be scaled to fit the width
            playerView.setPlayerScaleType(PlayerScaleType.RESIZE_FIT_WIDTH);

            //add player view to placeholder
            playerViewPlaceholder.addView(playerView);

            //make controls visible
            setUseController(true);
        }

        /**
         * the media that was attempted to be loaded is on external storage, and the app needs Storage permissions to load the media
         * if this event is called, the media loading has been aborted and the video service is NOT ready for playback!
         * use this event to request the permissions, and call reloadMedia() once you have the needed permissions
         */
        @Override
        public void onMissingStoragePermissions()
        {
            //storage permissions are missing, request them
            if (checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_READ_EXT_STORAGE))
            {
                //we already have the permission?? reload the media then!
                Logging.logD("onMissingStoragePermissions(): we already have those permissions? reload media...");
                playbackService.reloadMedia();
            }
        }

        /**
         * A error occurred in the video playback service!
         *
         * @param error the error that occurred (most likely a ExoPlayerException)
         */
        @Override
        public void onError(Exception error)
        {
            //log error
            Logging.logE("VideoServiceCallbackListener:onError(): %s", error.toString());

            //save playback position to prefs to be able to resume later
            savePlaybackPosition();

            //try to call exception handler for the app (to open exception handler)
            Application app = getApplication();
            if (app instanceof YAVPApp)
            {
                YAVPApp yavp = (YAVPApp) app;
                yavp.uncaughtException(Thread.currentThread(), error);
            }
        }

        /**
         * The current media was loaded and is ready for playback
         *
         * @param playWhenReady is the player set to play as soon as media is ready?
         */
        @Override
        public void onPlaybackReady(boolean playWhenReady)
        {
            //put screen lock while playing
            setScreenForcedOn(playWhenReady);

            //reset play button graphic if still replay button
            if (isPlaybackEnded)
            {
                isPlaybackEnded = false;
                playButton.setImageResource(R.drawable.ic_play_arrow_black_48dp);
            }
        }

        /**
         * The current media finished playback
         */
        @Override
        public void onPlaybackEnded()
        {
            //lift screen lock
            setScreenForcedOn(false);

            //TODO reset playback pos

            //close app if pref is set
            if (getPrefBool(ConfigKeys.KEY_CLOSE_WHEN_FINISHED_PLAYING, R.bool.DEF_CLOSE_WHEN_FINISHED_PLAYING))
            {
                //close app
                finish();
            }
            else
            {
                //change play button graphic to replay button
                playButton.setImageResource(R.drawable.ic_replay_black_48dp);
                isPlaybackEnded = true;
            }
        }

        /**
         * the player state changed.
         * !! you dont really need to use this, this is more to keep possibilities open... !!
         *
         * @param playerState the new playback state of the player
         */
        @Override
        public void onPlayerStateChange(int playerState)
        {
            //update pip controls on every state change
            if (isPictureInPicture)
                updatePipControls();
        }

        /**
         * The buffering state of the player changed
         *
         * @param isBuffering is the player currently buffering?
         */
        @Override
        public void onBufferingChanged(boolean isBuffering)
        {
            setBufferingIndicatorVisible(isBuffering, isPictureInPicture);
        }

        /**
         * The player received new metadata
         *
         * @param metadata the metadata received
         */
        @Override
        public void onNewMetadata(Metadata metadata)
        {

        }
    }
}
