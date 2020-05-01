package de.shadow578.yetanothervideoplayer.feature.controlview.ui;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;


/**
 * View that contains handles animations for double- tap seeking.
 * Just put this view in your layout with width & height set to match_parent and call the appropriate functions to play the animations - it's that simple!
 */
public class DoubleTapSeekOverlay extends FrameLayout
{
    private final long SEEK_AMOUNT_RESET_DELAY_MS = 1500;

    //region Variables
    //region Views
    /**
     * TextView for the seek forward text and animation
     */
    private final TextView seekForwardText;

    /**
     * TextView for teh seek backwards text and animation
     */
    private final TextView seekReverseText;

    //endregion

    /**
     * Animated Drawable on the seekForwardText
     */
    private AnimationDrawable seekForwardAnimation;

    /**
     * Animated Drawable on the seekReverseText
     */
    private AnimationDrawable seekReverseAnimation;

    /**
     * The last direction we seeked to (used for seek amount stacking)
     */
    private boolean lastSeekDirection = false;

    /**
     * How many seconds we are currently seeking in the current direction
     */
    private int seekAmountStack = 0;
    //endregion

    //region Message Handler

    /**
     * Messages used by the delayedHandler
     */
    private static final class Messages
    {
        /**
         * Message to stop the forward seeking animation and hide the textview for forward seeking
         */
        private static final int STOP_SEEK_FORWARD_ANIM = 0;

        /**
         * Message to stop the reverse seeking animation and hide the textview for reverse seeking
         */
        private static final int STOP_SEEK_REVERSE_ANIM = 1;

        /**
         * Message to reset the seek amount stack
         */
        private static final int RESET_SEEK_AMOUNT_STACK = 2;
    }

    /**
     * Handler that can be used to delay actions
     */
    private final Handler delayedHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case Messages.STOP_SEEK_FORWARD_ANIM:
                {
                    //stop the animation and reset it to frame 0
                    if (seekForwardAnimation != null)
                    {
                        seekForwardAnimation.stop();
                        seekForwardAnimation.selectDrawable(0);
                    }

                    //hide the forward seeking text
                    if (seekForwardText != null)
                        seekForwardText.setVisibility(GONE);

                    //reset seek amount if last seek direction was forward
                    if (lastSeekDirection)
                        delayedHandler.sendEmptyMessageDelayed(Messages.RESET_SEEK_AMOUNT_STACK, SEEK_AMOUNT_RESET_DELAY_MS);
                    break;
                }
                case Messages.STOP_SEEK_REVERSE_ANIM:
                {
                    //stop the animation and reset it to frame 0
                    if (seekReverseAnimation != null)
                    {
                        seekReverseAnimation.stop();
                        seekReverseAnimation.selectDrawable(0);
                    }

                    //hide the reverse seeking text
                    if (seekReverseText != null)
                        seekReverseText.setVisibility(GONE);

                    //reset seek amount if last seek direction was backwards
                    if (!lastSeekDirection)
                        delayedHandler.sendEmptyMessageDelayed(Messages.RESET_SEEK_AMOUNT_STACK, SEEK_AMOUNT_RESET_DELAY_MS);
                    break;
                }
                case Messages.RESET_SEEK_AMOUNT_STACK:
                {
                    //reset seek amount
                    seekAmountStack = 0;
                    break;
                }
                default:
                {
                    //invalid message :(
                    Logging.logE("Invalid Message ID %d in DoubleTapSeekOverlay!", msg.what);
                }
            }
        }
    };
    //endregion

    //region Constructors
    public DoubleTapSeekOverlay(@NonNull Context context)
    {
        this(context, null);
    }

    public DoubleTapSeekOverlay(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public DoubleTapSeekOverlay(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    public DoubleTapSeekOverlay(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        //do normal view stuff
        super(context, attrs, defStyleAttr, defStyleRes);

        //inflate layout
        inflate(getContext(), R.layout.layout_double_tap_seek_overlay, this);

        //find views
        seekForwardText = findViewById(R.id.dtso_seek_ffw_text);
        seekReverseText = findViewById(R.id.dtso_seek_frw_text);

        //initialize ui stuff
        initializeSeekAnimations();

        //initially hide forward and reverse texts
        seekForwardText.setVisibility(GONE);
        seekReverseText.setVisibility(GONE);
    }
    //endregion

    /**
     * Initializes and sets the seek animation drawables
     */
    private void initializeSeekAnimations()
    {
        //check views are ok
        if (getContext() == null || seekForwardText == null || seekReverseText == null)
            throw new IllegalStateException("At least one Seek TextView was null!");

        //get animations
        seekForwardAnimation = (AnimationDrawable) getContext().getDrawable(R.drawable.seek_animation_r);
        seekReverseAnimation = (AnimationDrawable) getContext().getDrawable(R.drawable.seek_animation_l);

        //check animations are valid
        if (seekForwardAnimation == null || seekReverseAnimation == null)
            throw new IllegalStateException("At least one seek animation was not found!");

        //all ok now
        //set and setup seek forward animation
        seekForwardText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, seekForwardAnimation, null, null);
        seekForwardAnimation.setCallback(seekForwardText);
        seekForwardAnimation.setVisible(true, true);

        //set and setup seek backwards animation
        seekReverseText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, seekReverseAnimation, null, null);
        seekReverseAnimation.setCallback(seekReverseText);
        seekReverseAnimation.setVisible(true, true);
    }

    /**
     * Show the seek animation.
     * also cancels all other seek animations and updates the text shown
     *
     * @param forward           are we seeking forward or backwards.
     * @param animationLengthMs how long the animation is shown, in milliseconds
     * @param seekAmountS       by how many seconds we are seeking (absolute, only used for ui text)
     * @param stackSeekAmount   should the seekAmount stack with previous calls?
     */
    public void showSeekAnimation(boolean forward, long animationLengthMs, int seekAmountS, boolean stackSeekAmount)
    {
        //cancel seek stack being reset
        delayedHandler.removeMessages(Messages.RESET_SEEK_AMOUNT_STACK);

        //reset seek amount to 0 if seeking in different direction OR stacking is disabled
        if (forward != lastSeekDirection || !stackSeekAmount)
        {
            seekAmountStack = 0;
        }

        //add current seek amount to seek stack
        seekAmountStack += seekAmountS;
        seekAmountS = seekAmountStack;

        //update last seek direction
        lastSeekDirection = forward;

        //do animation suff
        if (forward)
        {
            //cancel messages that would cancel the seeking animation we want to play
            delayedHandler.removeMessages(Messages.STOP_SEEK_FORWARD_ANIM);

            //cancel the animation for the reverse direction instantly
            delayedHandler.sendEmptyMessage(Messages.STOP_SEEK_REVERSE_ANIM);

            //make textview for seeking direction visible and start the animation we want
            //also update the text in the textview
            if (seekForwardText != null && seekForwardAnimation != null)
            {
                //set text
                seekForwardText.setText(String.format(getContext().getString(R.string.dtso_seek_duration_label_f), seekAmountS));

                //make visible and start animation
                seekForwardText.setVisibility(VISIBLE);
                seekForwardAnimation.start();
            }

            //cancel the animation after a delay
            delayedHandler.sendEmptyMessageDelayed(Messages.STOP_SEEK_FORWARD_ANIM, animationLengthMs);
        }
        else
        {
            //cancel messages that would cancel the seeking animation we want to play
            delayedHandler.removeMessages(Messages.STOP_SEEK_REVERSE_ANIM);

            //cancel the animation for the reverse direction instantly
            delayedHandler.sendEmptyMessage(Messages.STOP_SEEK_FORWARD_ANIM);

            //make textview for seeking direction visible and start the animation we want
            //also update the text in the textview
            if (seekReverseText != null && seekReverseAnimation != null)
            {
                //set text
                seekReverseText.setText(String.format(getContext().getString(R.string.dtso_seek_duration_label_f), seekAmountS));

                //make visible and start animation
                seekReverseText.setVisibility(VISIBLE);
                seekReverseAnimation.start();
            }

            //cancel the animation after a delay
            delayedHandler.sendEmptyMessageDelayed(Messages.STOP_SEEK_REVERSE_ANIM, animationLengthMs);
        }
    }

}
