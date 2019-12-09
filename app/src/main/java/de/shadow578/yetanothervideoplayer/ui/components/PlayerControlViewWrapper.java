package de.shadow578.yetanothervideoplayer.ui.components;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerControlView;

/**
 * Basically the same as PlayerControlView, but implements "hide on click" functionality
 */
public class PlayerControlViewWrapper extends PlayerControlView implements Player.EventListener
{
    int controlsShowTimeoutMs;

    public PlayerControlViewWrapper(Context context)
    {
        this(context, null);
    }

    public PlayerControlViewWrapper(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public PlayerControlViewWrapper(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, attrs);
    }

    public PlayerControlViewWrapper(Context context, @Nullable AttributeSet attrs, int defStyleAttr, @Nullable AttributeSet playbackAttrs)
    {
        super(context, attrs, defStyleAttr, playbackAttrs);
        setControlsShowTimeoutMs(PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS);
    }

    public void setControlsShowTimeoutMs(int to)
    {
        controlsShowTimeoutMs = to;
    }

    public int getControlsShowTimeoutMs()
    {
        return controlsShowTimeoutMs;
    }

    @Override
    public void setPlayer(@Nullable Player player)
    {
        super.setPlayer(player);
        if (player != null)
        {
            player.addListener(this);
            maybeShowControls(false);
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
    {
        maybeShowControls(false);
    }

    @Override
    public boolean performClick()
    {
        super.performClick();
        return toggleControlsVisibility();
    }

    private boolean toggleControlsVisibility()
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
        return true;
    }

    private void maybeShowControls(boolean isForced)
    {
        boolean wasShowingIndefinitely = isVisible() && getShowTimeoutMs() <= 0;
        boolean shouldShowIndefinitely = shouldShowControlsIndefinitely();
        if (isForced || wasShowingIndefinitely || shouldShowIndefinitely)
        {
            showControls(shouldShowIndefinitely);
        }
    }

    private boolean shouldShowControlsIndefinitely()
    {
        Player player = getPlayer();
        if (player == null)
        {
            return true;
        }

        int playbackState = player.getPlaybackState();
        return playbackState == Player.STATE_IDLE
                || playbackState == Player.STATE_ENDED
                || !player.getPlayWhenReady();
    }

    private void showControls(boolean showIndefinitely)
    {
        setShowTimeoutMs(showIndefinitely ? 0 : getControlsShowTimeoutMs());
        show();
    }
}
