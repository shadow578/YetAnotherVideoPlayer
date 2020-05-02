package de.shadow578.yetanothervideoplayer.feature.controlview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerControlView;

import de.shadow578.yetanothervideoplayer.R;

/**
 * A PlayerControlView that implements "tap to hide"
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TapToHidePlayerControlView extends PlayerControlView implements Player.EventListener
{
    /**
     * Default timeout until the controls are automatically hidden, ms
     */
    private final int DEFAULT_AUTO_HIDE_TIMEOUT = 5000;

    /**
     * Action for automatically hiding the controls
     */
    private final Runnable autoHideAction;

    /**
     * Callback for visibility changes and behaviour control
     */
    private VisibilityChangeCallback callback;

    /**
     * Timeout until the controls are automatically hidden, ms
     */
    private int controlsAutoHideTimeout = DEFAULT_AUTO_HIDE_TIMEOUT;

    /**
     * are the controls manually hidden?
     */
    private boolean areControlsHidden = false;

    /**
     * Are we allowed to auto- hide the controls?
     */
    private boolean allowAutoHideControls = true;

    /**
     * is a controls auto- hide pending (because the autoHideAction was called when we weren't allowed to auto- hide them?)
     */
    private boolean pendingAutoHideControls = false;

    //region Constructors
    public TapToHidePlayerControlView(Context context)
    {
        this(context, null);
    }

    public TapToHidePlayerControlView(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public TapToHidePlayerControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, attrs);
    }

    public TapToHidePlayerControlView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, @Nullable AttributeSet playbackAttrs)
    {
        //no normal init
        super(context, attrs, defStyleAttr, playbackAttrs);

        //disable default show timeout
        setShowTimeoutMs(0);

        //set default auto- hide timeout
        setControlsAutoHideTimeout(PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS);

        //set action for hiding
        autoHideAction = new Runnable()
        {
            @Override
            public void run()
            {
                if (allowAutoHideControls)
                {
                    //allowed to hide, do so
                    hide();
                    pendingAutoHideControls = false;
                }
                else
                {
                    //not allowed, set pending flag
                    pendingAutoHideControls = true;
                }
            }
        };

        //apply styleables
        applyStyleAttributes(context, attrs);
    }

    /**
     * Applies styleable values from the attribute set
     *
     * @param context the current context
     * @param attrs   the attribute set of the view
     */
    private void applyStyleAttributes(Context context, AttributeSet attrs)
    {
        //check we have valid context and attributes
        if (context == null || attrs == null) return;

        //get style attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TapToHidePlayerControlView);

        //set values
        setControlsAutoHideTimeout(a.getInteger(R.styleable.TapToHidePlayerControlView_autoHideTimeout, DEFAULT_AUTO_HIDE_TIMEOUT));

        //we're finished, recycle attribute array
        a.recycle();
    }
    //endregion

    //region Interfacing

    /**
     * same as normal setPlayer
     *
     * @param player the player to use
     * @return own instance, for set chaining
     */
    public TapToHidePlayerControlView setPlayerInstance(@Nullable Player player)
    {
        setPlayer(player);
        return this;
    }

    /**
     * Sets the timeout until the controls are automatically hidden after activated
     *
     * @param timeout the timeout to set
     * @return own instance, for set chaining
     */
    public TapToHidePlayerControlView setControlsAutoHideTimeout(int timeout)
    {
        controlsAutoHideTimeout = timeout;
        return this;
    }

    /**
     * Set if the controls are constantly hidden
     *
     * @param controlsHidden are controls hidden constantly?
     * @return own instance, for set chaining
     */
    public TapToHidePlayerControlView setControlsHidden(boolean controlsHidden)
    {
        areControlsHidden = controlsHidden;
        if (controlsHidden)
        {
            hide();
        }
        else
        {
            maybeShowControls(false);
        }

        return this;
    }

    /**
     * set if the controls are allowed to auto- hide
     *
     * @param autoHide auto- hide controls?
     * @return own instance, for set chaining
     */
    public TapToHidePlayerControlView setAllowAutoHideControls(boolean autoHide)
    {
        //set value
        allowAutoHideControls = autoHide;

        //if we are allowed to hide again AND we have a pending auto- hide, call auto hide action
        if (autoHide && pendingAutoHideControls)
        {
            autoHideAction.run();
        }
        return this;
    }

    /**
     * @return are the controls allowed to auto- hide?
     */
    public boolean getAllowAutoHideControls()
    {
        return allowAutoHideControls;
    }

    /**
     * Set the callback for visibility changes and control
     *
     * @param _callback the callback to set
     * @return own instance, for set chaining
     */
    public TapToHidePlayerControlView setVisibilityChangeCallback(VisibilityChangeCallback _callback)
    {
        callback = _callback;
        return this;
    }

    /**
     * @return the timeout until the controls are automatically hidden after activated
     */
    public int getControlsAutoHideTimeout()
    {
        return controlsAutoHideTimeout;
    }

    /**
     * @return are controls hidden constantly?
     */
    public boolean getControlsHidden()
    {
        return areControlsHidden;
    }

    /**
     * Set the player this control view is used with
     *
     * @param player the player to use
     */
    @Override
    public void setPlayer(@Nullable Player player)
    {
        super.setPlayer(player);
        if (player != null)
        {
            player.addListener(this);
            maybeShowControls(true);
        }
    }

    //endregion

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
    {
        maybeShowControls(false);
    }

    @Override
    public boolean performClick()
    {
        super.performClick();
        toggleControlsVisibility();
        return true;
    }

    /**
     * toggle the visibility of the controls
     */
    private void toggleControlsVisibility()
    {
        if (!isVisible())
        {
            //enable controls
            maybeShowControls(true);
        }
        else
        {
            //hide controls
            hide();
        }
    }

    /**
     * Show the controls unless they are disabled for some reason
     *
     * @param isForced is this a forced show (eg. other factors are IGNORED)?
     */
    private void maybeShowControls(boolean isForced)
    {
        //show controls if either forced or controls should be always visible
        //but ONLY if controls are not hidden
        boolean shouldShowIndefinitely = shouldShowControlsIndefinitely();
        if (!areControlsHidden && (isForced || shouldShowIndefinitely))
        {
            showControls(shouldShowIndefinitely);
        }
    }

    /**
     * @return Should the controls be shown constantly?
     */
    private boolean shouldShowControlsIndefinitely()
    {
        Player player = getPlayer();
        if (player == null)
        {
            //no player? always show controls!
            return true;
        }

        //always show controls when player is IDLE or ENDED
        int playbackState = player.getPlaybackState();
        return playbackState == Player.STATE_IDLE
                || playbackState == Player.STATE_ENDED
                || !player.getPlayWhenReady();
    }

    /**
     * Make the controls visible, but also post a message to hide them again if not showing indefinitely.
     *
     * @param showIndefinitely should a message to auto- hide NOT be posted?
     */
    public void showControls(boolean showIndefinitely)
    {
        //show controls
        show();

        //remove all previous hide requests
        removeCallbacks(autoHideAction);

        //post callback to hide controls automatically (after a timeout)
        if (!showIndefinitely)
            postDelayed(autoHideAction, getControlsAutoHideTimeout());
    }

    @Override
    public void show()
    {
        super.show();
        if (callback != null)
            callback.onShow();
    }

    @Override
    public void hide()
    {
        super.hide();
        if (callback != null)
            callback.onHide();
    }

    /**
     * Defines callbacks and behaviour controls for the TapToHidePlayerControlView
     */
    public interface VisibilityChangeCallback
    {
        /**
         * Callback when the controls transition from hidden to visible
         */
        void onShow();

        /**
         * Callback when the controls transition from visible to hidden
         */
        void onHide();
    }
}
