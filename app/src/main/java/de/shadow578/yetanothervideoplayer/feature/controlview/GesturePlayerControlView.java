package de.shadow578.yetanothervideoplayer.feature.controlview;

import android.app.Activity;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.SizeF;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Util;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.feature.controlview.ui.DoubleTapSeekOverlay;
import de.shadow578.yetanothervideoplayer.feature.swipe.SwipeGestureListener;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.ConfigUtil;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * A PlayerControlView that implements swipe gestures, hide -on- click, and double- tap to seek (with nice UI effects)
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class GesturePlayerControlView extends FrameLayout
{
    /**
     * Should we ignore setVisibility calls?
     * This allows us to prevent the superclass from changing our visibility
     */
    private boolean ignoreSetVisibility = false;

    /**
     * PlayerControlView that actually contains and controls the player.
     * It is just a child of this view, so we always have access to the parent view (and thus can use onTouch and TouchListener for gestures)
     * Is initialized and added in constructor
     */
    private TapToHidePlayerControlView playerControls;

    /**
     * The audio manager instance used to adjust media volume by swiping
     */
    private AudioManager audioManager;

    /**
     * The TextView in the center of the screen, used to show information
     */
    private TextView infoTextView;

    /**
     * Seek Overlay for UI Effects while double- tap seeking
     */
    private DoubleTapSeekOverlay seekOverlay;

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
            }
        }
    };

    //endregion

    //region constructors
    public GesturePlayerControlView(Context context)
    {
        this(context, null);
    }

    public GesturePlayerControlView(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public GesturePlayerControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    public GesturePlayerControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        //do normal view stuff
        super(context, attrs, defStyleAttr, defStyleRes);

        //inflate our layout
        inflate(getContext(), R.layout.layout_gesture_player_control_view, this);

        //get views
        playerControls = findViewById(R.id.ctl_playercontrols);
        infoTextView = findViewById(R.id.ctl_infotext);
        seekOverlay = findViewById(R.id.ctl_seekoverlay);

        //setup gesture controls
        setupGestures();
    }
    //endregion

    //region Interfacing

    /**
     * Set the player this control view is used with
     *
     * @param player the player to use
     */
    public GesturePlayerControlView setPlayer(Player player)
    {
        playerControls.setPlayer(player);
        return this;
    }

    /**
     * @return the player this control view is used with
     */
    public Player getPlayer()
    {
        return playerControls.getPlayer();
    }

    /**
     * @return the underlying player control view
     */
    public TapToHidePlayerControlView getPlayerControls()
    {
        return playerControls;
    }

    /**
     * Shows the playback controls.
     */
    public void show()
    {
        playerControls.show();
    }

    /**
     * Hides the player controls
     */
    public void hide()
    {
        playerControls.hide();
    }

    //endregion

    //region Swipe Gestures

    /**
     * Setup Gesture controls for Volume and Brightness
     */
    private void setupGestures()
    {
        //get audio manager
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

        //get configuration values needed in swipe handler (avoid looking up values constantly)
        final boolean enableSwipeGestures = ConfigUtil.getConfigBoolean(getContext(), ConfigKeys.KEY_SWIPE_GESTURES_EN, R.bool.DEF_SWIPE_GESTURES_EN);
        final int touchDecayTime = ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_SWIPE_DECAY_TIME, R.integer.DEF_TOUCH_DECAY_TIME);
        final int swipeFlingThreshold = ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_SWIPE_FLING_THRESHOLD, R.integer.DEF_SWIPE_FLING_THRESHOLD);
        final int doubleTapDecayTime = ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_DOUBLE_TAP_DECAY_TIME, R.integer.DEF_DOUBLE_TAP_DECAY_TIME);
        final int doubleTapMaxDistance = ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_DOUBLE_TAP_MAX_RADIUS, R.integer.DEF_DOUBLE_TAP_MAX_RADIUS);

        final RectF swipeIgnore = new RectF(ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_SWIPE_DEAD_ZONE_RECT_LEFT, R.integer.DEF_SWIPE_DEAD_ZONE_LEFT),
                ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_SWIPE_DEAD_ZONE_RECT_TOP, R.integer.DEF_SWIPE_DEAD_ZONE_TOP),
                ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_SWIPE_DEAD_ZONE_RECT_RIGHT, R.integer.DEF_SWIPE_DEAD_ZONE_RIGHT),
                ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_SWIPE_DEAD_ZONE_RECT_BOTTOM, R.integer.DEF_SWIPE_DEAD_ZONE_BOTTOM));

        final float brightnessAdjustStep = ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_BRIGHTNESS_ADJUST_STEP, R.integer.DEF_BRIGHTNESS_ADJUST_STEP) / 100.0f;
        final float hardSwipeThreshold = ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_BRIGHTNESS_HARD_SWIPE_THRESHOLD, R.integer.DEF_BRIGHTNESS_HARD_SWIPE_THRESHOLD);
        final boolean hardSwipeEnable = ConfigUtil.getConfigBoolean(getContext(), ConfigKeys.KEY_BRIGHTNESS_HARD_SWIPE_EN, R.bool.DEF_BRIGHTNESS_HARD_SWIPE_EN);

        //init and set listener
        setOnTouchListener(new SwipeGestureListener(touchDecayTime, doubleTapDecayTime, swipeFlingThreshold, swipeFlingThreshold, doubleTapMaxDistance, swipeIgnore){
            @Override
            public void onVerticalSwipe(float deltaY, PointF swipeStart, PointF swipeEnd, PointF firstContact, SizeF screenSize)
            {
                //ignore if swipe gestures are disabled
                if (!enableSwipeGestures)
                {
                    super.onVerticalSwipe(deltaY, swipeStart, swipeEnd, firstContact, screenSize);
                    return;
                }

                //check which screen size the swipe originated from
                if (isRightScreenSide(firstContact, screenSize))
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
            public void onNoSwipeClick(View view, PointF clickPos, SizeF screenSize)
            {
                //forward click to player controls
                playerControls.performClick();

                //also perform click on original view
                //super.onNoSwipeClick(view, clickPos, screenSize);
            }

            @Override
            protected void onDoubleClick(float distanceSquared, long tapDeltaTime, PointF firstTouchPos, PointF secondTouchPos, SizeF screenSize)
            {
                //get fast- forward and rewind increments
                int seekAmount = ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_SEEK_BUTTON_INCREMENT, R.integer.DEF_SEEK_BUTTON_INCREMENT);

                //check on which side of the screen the double click ended
                if (isRightScreenSide(secondTouchPos, screenSize))
                {
                    //double click on right side, seek forward
                    //TODO: add effects?
                    if(seekOverlay != null)
                        seekOverlay.showSeekAnimation(true,  520, (seekAmount / 1000), true);
                }
                else
                {
                    //double click on left side, seek backwards
                    //TODO: add effects?
                    if(seekOverlay != null)
                        seekOverlay.showSeekAnimation(false,  520, (seekAmount / 1000), true);

                    //invert seek increment for seeking backwards
                    seekAmount *= -1;
                }

                //get and check player for seeking
                Player player = playerControls.getPlayer();
                if (player == null) return;

                //calculate position to seek to
                long seekPositionAbs = player.getCurrentPosition() + seekAmount;

                //limit seeking to bounds of player
                long playDuration = player.getDuration();
                if (seekPositionAbs < 0) seekPositionAbs = 0;
                if (seekPositionAbs > playDuration && playDuration != C.TIME_UNSET)
                    seekPositionAbs = playDuration;

                //seek the player
                player.seekTo(seekPositionAbs);
            }

            /**
             * @param point the point to check
             * @param screenSize the size of the screen
             * @return is the point on the right side of the screen?
             */
            private boolean isRightScreenSide(PointF point, SizeF screenSize)
            {
                return point.x > (screenSize.getWidth() / 2);
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
        //get host activity and it's window
        Activity hostActivity = getHostActivity();
        if (hostActivity == null) return;

        //get and check window
        Window hostWindow = hostActivity.getWindow();
        if (hostWindow == null) return;

        //get window attributes
        WindowManager.LayoutParams windowAttributes = hostWindow.getAttributes();

        //check if brightness is already zero (overrides allowZero)
        boolean alreadyZero = windowAttributes.screenBrightness == 0.0f;

        //modify screen brightness attribute withing range
        //allow setting it to zero if allowZero is set or the value was previously zero too
        windowAttributes.screenBrightness = Math.min(Math.max(windowAttributes.screenBrightness + adjust, ((allowZero || alreadyZero) ? 0.0f : 0.01f)), 1f);

        //set changed window attributes
        hostWindow.setAttributes(windowAttributes);

        //show info text for brightness
        String brightnessStr = ((int) Math.floor(windowAttributes.screenBrightness * 100)) + "%";
        if (windowAttributes.screenBrightness == 0)
        {
            brightnessStr = getContext().getString(R.string.info_brightness_auto);
        }
        showInfoText(getContext().getString(R.string.info_brightness_change), brightnessStr);
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
        showInfoText(getContext().getString(R.string.info_volume_change), volumePercent);
    }

    //endregion

    //region Popup Info Text

    /**
     * Show info text in the middle of the screen, using the InfoText View
     *
     * @param text   the text to show
     * @param format formatting options (using US locale)
     */
    public void showInfoText(String text, Object... format)
    {
        //check infotext is not null
        if (infoTextView == null) return;

        //remove all previous messages related to fading out the info text
        delayHandler.removeMessages(Messages.START_FADE_OUT_INFO_TEXT);
        delayHandler.removeMessages(Messages.SET_INFO_TEXT_INVISIBLE);

        //set text
        infoTextView.setText(String.format(text, format));

        //reset animation and make text visible
        infoTextView.setAnimation(null);
        infoTextView.setVisibility(View.VISIBLE);

        //hide text after delay
        int infoTextDuration = ConfigUtil.getConfigInt(getContext(), ConfigKeys.KEY_INFO_TEXT_DURATION, R.integer.DEF_INFO_TEXT_DURATION);
        delayHandler.sendEmptyMessageDelayed(Messages.START_FADE_OUT_INFO_TEXT, infoTextDuration);
    }

    /**
     * Hide the info text
     *
     * @param instant should the info text fade out or be instantly hidden?
     */
    public void hideInfoText(boolean instant)
    {
        if (instant)
        {
            delayHandler.sendEmptyMessage(Messages.SET_INFO_TEXT_INVISIBLE);
        }
        else
        {
            delayHandler.sendEmptyMessage(Messages.START_FADE_OUT_INFO_TEXT);
        }
    }

    //endregion

    //region Utility

    /**
     * @return the activity this view is a part of. This might be null, so check for that!
     */
    private Activity getHostActivity()
    {
        if (!(getContext() instanceof Activity))
            return null;

        return (Activity) getContext();
    }
    //endregion
}
