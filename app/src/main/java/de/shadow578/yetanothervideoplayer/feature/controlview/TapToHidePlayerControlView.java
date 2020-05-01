package de.shadow578.yetanothervideoplayer.feature.controlview;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerControlView;

/**
 * A PlayerControlView that implements "tap to hide"
 */
@SuppressWarnings("unused")
public class TapToHidePlayerControlView extends PlayerControlView implements Player.EventListener
{

    /**
     * Action to hide the controls
     */
    private final Runnable hideAction;

    /**
     * Timeout until the controls are automatically hidden
     */
    private int controlsAutoHideTimeout = 5000;//ms

    /**
     * are the controls manually hidden?
     */
    private boolean areControlsHidden = false;

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

        //set default auto- hide timeout
        setControlsAutoHideTimeout(PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS);

        //set action for hiding
        hideAction = new Runnable()
        {
            @Override
            public void run()
            {
                hide();
            }
        };
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
    private void showControls(boolean showIndefinitely)
    {
        //show controls
        show();

        //remove all previous hide requests
        removeCallbacks(hideAction);

        //post callback to hide controls automatically (after a timeout)
        if (!showIndefinitely)
            postDelayed(hideAction, getControlsAutoHideTimeout());
    }
}
