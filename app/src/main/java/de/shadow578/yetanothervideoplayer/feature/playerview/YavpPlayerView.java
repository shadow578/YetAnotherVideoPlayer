package de.shadow578.yetanothervideoplayer.feature.playerview;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.video.VideoListener;

/**
 * Wrapper for the {@link PlayerView} that adds additional scale types.
 * Does the same as {@link YavpEPlayerView}, but without GL
 */
public class YavpPlayerView extends PlayerView implements VideoListener
{
    /**
     * Scale type for the player
     */
    private PlayerScaleType scaleType = PlayerScaleType.FillWidth;

    /**
     * Aspect ratio of the video
     */
    private float vAspectRatio = 1f;

    public YavpPlayerView(Context context)
    {
        this(context, null);
    }

    public YavpPlayerView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public YavpPlayerView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        //disable controller
        super.setUseController(false);
    }

    /**
     * Deprecated. use {@link YavpPlayerView#setSimplePlayer(SimpleExoPlayer)} instead!
     */
    @Override
    @Deprecated
    public void setPlayer(@Nullable Player player)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * sets the player of this player view
     *
     * @param player the player to set
     */
    public void setSimplePlayer(@Nullable SimpleExoPlayer player)
    {
        //set player normally
        super.setPlayer(player);

        //register as video listener
        if (player != null)
            player.addVideoListener(this);
    }

    /**
     * set the scale type to use.
     *
     * @param scaleType the scale type to use
     */
    public void setPlayerScaleType(PlayerScaleType scaleType)
    {
        this.scaleType = scaleType;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        //this essentially bypasses the logic in EPlayerView because we set its scale type to NONE
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mWidth = getMeasuredWidth();
        int mHeight = getMeasuredHeight();

        //calculate view size
        int width = mWidth;
        int height = mHeight;
        switch (scaleType)
        {
            case FillWidth:
            {
                //scale video to fill width
                height = (int) (width / vAspectRatio);
                break;
            }
            case FillHeight:
            {
                //scale video to fill height
                width = (int) (height * vAspectRatio);
                break;
            }
            case Fit:
            {
                //scale video to fill either width or height, but dont crop anything
                height = (int) (width / vAspectRatio);
                if (height > mHeight)
                {
                    height = mHeight;
                    width = (int) (mHeight * vAspectRatio);
                }
                break;
            }
        }

        //set dimensions
        setMeasuredDimension(width, height);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio)
    {
        vAspectRatio = ((float) width / (float) height) * pixelWidthHeightRatio;
        requestLayout();
    }
}
