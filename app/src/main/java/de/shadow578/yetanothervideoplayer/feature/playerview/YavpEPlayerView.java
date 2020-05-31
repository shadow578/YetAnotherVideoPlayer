package de.shadow578.yetanothervideoplayer.feature.playerview;

import android.content.Context;
import android.util.AttributeSet;

import com.daasuu.epf.EPlayerView;

/**
 * Wrapper for the {@link EPlayerView} that adds additional scale types
 */
public class YavpEPlayerView extends EPlayerView
{
    /**
     * Different scale types for the {@link YavpEPlayerView}
     */
    public enum PlayerScaleType
    {
        /**
         * Scale the video to the size of the player view
         */
        None,

        /**
         * Scale the video so that either the width or height matches the size of the player view, but don't crop the video
         */
        Fit,

        /**
         * Scale the video so that the width matches the width of the player view, and crop the height as needed
         */
        FillWidth,

        /**
         * Scale the video so that the height matches the height of the player view, and crop the width as needed
         */
        FillHeight,
    }

    /**
     * Scale type for the player
     */
    private PlayerScaleType scaleType = PlayerScaleType.FillWidth;

    /**
     * Aspect ratio of the video
     */
    private float vAspectRatio = 1f;

    //region Constructors
    public YavpEPlayerView(Context context)
    {
        this(context, null);
    }

    public YavpEPlayerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        //disable scaling in super
        super.setPlayerScaleType(com.daasuu.epf.PlayerScaleType.RESIZE_NONE);
    }
    //endregion

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

    /**
     * set the scale type to use.
     * Deprecated, use {@link YavpEPlayerView#setPlayerScaleType(PlayerScaleType)} instead.
     *
     * @param playerScaleType the scale type to use
     */
    @Override
    @Deprecated
    public void setPlayerScaleType(com.daasuu.epf.PlayerScaleType playerScaleType)
    {
        throw new UnsupportedOperationException();
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
