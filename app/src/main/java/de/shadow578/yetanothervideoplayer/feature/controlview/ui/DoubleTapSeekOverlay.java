package de.shadow578.yetanothervideoplayer.feature.controlview.ui;

import android.content.Context;
import android.content.res.TypedArray;
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
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class DoubleTapSeekOverlay extends FrameLayout
{
    //region Constants
    /**
     * After how many milliseconds the seek amount is reset (after a reset is requested)
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final long SEEK_AMOUNT_RESET_DELAY_MS = 1000;

    /**
     * Default value for seek stacking
     */
    private final boolean DEFAULT_ENABLE_SEEK_AMOUNT_STACKING = true;

    /**
     * Default animation duration, in ms
     */
    private final int DEFAULT_ANIMATION_DURATION = 750;

    /**
     * Default value for enableFadeOut for seek textviews
     */
    private final boolean DEFAULT_ENABLE_FADE_ANIMATION = true;

    /**
     * Default duration for the fade- out of the seek textviews, in ms
     */
    private final int DEFAULT_FADE_DURATION = 150;
    //endregion

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

    /**
     * duration of the seek animation
     */
    private long animationDurationMs = DEFAULT_ANIMATION_DURATION;

    /**
     * Should the seek amount be stacked?
     * can be overridden by showSeekAnimation's parameter
     */
    private boolean defStackSeekAmount = DEFAULT_ENABLE_SEEK_AMOUNT_STACKING;

    /**
     * should the seek text views be faded before being made invisible?
     */
    private boolean enableFadeOutTexts = DEFAULT_ENABLE_FADE_ANIMATION;

    /**
     * duration for fading out the seek text views before going invisible
     */
    private long textFadeOutDuration = DEFAULT_FADE_DURATION;
    //endregion

    //region Message Handler

    /**
     * Messages used by the delayedHandler
     */
    private static final class Messages
    {
        /**
         * Message to start fading out the textview for forward seeking.
         * After the fade- out is finished, STOP_SEEK_FORWARD_ANIM is called
         */
        private static final int FADE_SEEK_FORWARD_ANIM = 0;

        /**
         * Message to start fading out the textview for reverse seeking.
         * After the fade- out is finished, STOP_SEEK_REVERSE_ANIM is called
         */
        private static final int FADE_SEEK_REVERSE_ANIM = 1;

        /**
         * Message to stop the forward seeking animation and hide the textview for forward seeking (instantly, without animation)
         */
        private static final int STOP_SEEK_FORWARD_ANIM = 2;

        /**
         * Message to stop the reverse seeking animation and hide the textview for reverse seeking (instantly, without animation)
         */
        private static final int STOP_SEEK_REVERSE_ANIM = 3;

        /**
         * Message to reset the seek amount stack
         */
        private static final int RESET_SEEK_AMOUNT_STACK = 4;
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
                case Messages.FADE_SEEK_FORWARD_ANIM:
                {
                    //start fade out animation
                    seekForwardText.animate().alpha(0f).setDuration(textFadeOutDuration).withEndAction(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //make invisible after animation
                            delayedHandler.sendEmptyMessage(Messages.STOP_SEEK_FORWARD_ANIM);
                        }
                    });
                    break;
                }
                case Messages.FADE_SEEK_REVERSE_ANIM:
                {
                    //start fade out animation
                    seekReverseText.animate().alpha(0f).setDuration(textFadeOutDuration).withEndAction(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //make invisible after animation
                            delayedHandler.sendEmptyMessage(Messages.STOP_SEEK_REVERSE_ANIM);
                        }
                    });
                    break;
                }
                case Messages.STOP_SEEK_FORWARD_ANIM:
                {
                    //stop the animation and reset it to frame 0
                    if (seekForwardAnimation != null)
                    {
                        seekForwardAnimation.stop();
                        seekForwardAnimation.selectDrawable(0);
                    }

                    if (seekForwardText != null)
                    {
                        //hide the forward seeking text
                        seekForwardText.setVisibility(GONE);

                        //reset alpha after animation
                        seekForwardText.setAlpha(1f);
                    }

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

                    if (seekReverseText != null)
                    {
                        //hide the reverse seeking text
                        seekReverseText.setVisibility(GONE);

                        //reset alpha after animation
                        seekReverseText.setAlpha(1f);
                    }

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
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DoubleTapSeekOverlay);

        //set values
        setAnimationDuration(a.getInteger(R.styleable.DoubleTapSeekOverlay_seekAnimationDuration, DEFAULT_ANIMATION_DURATION));
        setStackSeekAmount(a.getBoolean(R.styleable.DoubleTapSeekOverlay_enableSeekAmountStacking, DEFAULT_ENABLE_SEEK_AMOUNT_STACKING));
        setEnableFadeOutTextViews(a.getBoolean(R.styleable.CircleRippleAnimationView_enableRippleFadeAnimation, DEFAULT_ENABLE_FADE_ANIMATION));
        setFadeOutDuration(a.getInteger(R.styleable.CircleRippleAnimationView_rippleFadeAnimationDuration, DEFAULT_FADE_DURATION));

        //we're finished, recycle attribute array
        a.recycle();
    }
    //endregion

    //region Interfacing

    /**
     * set the animation duration
     *
     * @param duration the animation duration, in milliseconds
     * @return own instance, for set chaining
     */
    public DoubleTapSeekOverlay setAnimationDuration(long duration)
    {
        animationDurationMs = duration;
        return this;
    }

    /**
     * @return the animation duration, in milliseconds
     */
    public long getAnimationDuration()
    {
        return animationDurationMs;
    }

    /**
     * set if seek amount stacking is enabled
     *
     * @param stackSeekAmount is seek stacking enabled?
     * @return own instance, for set chaining
     */
    public DoubleTapSeekOverlay setStackSeekAmount(boolean stackSeekAmount)
    {
        defStackSeekAmount = stackSeekAmount;
        return this;
    }

    /**
     * @return is seek stacking enabled?
     */
    public boolean shouldStackSeekAmount()
    {
        return defStackSeekAmount;
    }

    /**
     * set if the textviews are faded out with a animation before going invisible
     *
     * @param enable enable fade- out animation?
     * @return own instance, for set chaining
     */
    public DoubleTapSeekOverlay setEnableFadeOutTextViews(boolean enable)
    {
        enableFadeOutTexts = enable;
        return this;
    }

    /**
     * @return is the fade- out animation for the textviews enabled?
     */
    public boolean getEnableFadeOutTextViews()
    {
        return enableFadeOutTexts;
    }

    /**
     * set the duration of the fade- out animation for the textvies, that plays if {@link DoubleTapSeekOverlay#getEnableFadeOutTextViews()} is true
     *
     * @param duration the duration of the animation, in ms
     * @return own instance, for set chaining
     */
    public DoubleTapSeekOverlay setFadeOutDuration(long duration)
    {
        textFadeOutDuration = duration;
        return this;
    }

    /**
     * @return the duration of the fade- out animation for the textviews, in ms
     */
    public long getFadeOutDuration()
    {
        return textFadeOutDuration;
    }

    //endregion

    /**
     * Show the seek animation.
     * also cancels all other seek animations and updates the text shown
     *
     * @param forward     are we seeking forward or backwards.
     * @param seekAmountS by how many seconds we are seeking (absolute, only used for ui text)
     */
    public void showSeekAnimation(boolean forward, int seekAmountS)
    {
        showSeekAnimation(forward, seekAmountS, defStackSeekAmount);
    }

    /**
     * Show the seek animation.
     * also cancels all other seek animations and updates the text shown
     *
     * @param forward         are we seeking forward or backwards.
     * @param seekAmountS     by how many seconds we are seeking (absolute, only used for ui text)
     * @param stackSeekAmount should the seekAmount stack with previous calls?
     */
    public void showSeekAnimation(boolean forward, int seekAmountS, boolean stackSeekAmount)
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

        //do animation stuff
        if (forward)
        {
            //cancel messages that would cancel the seeking animation we want to play
            delayedHandler.removeMessages(Messages.STOP_SEEK_FORWARD_ANIM);
            delayedHandler.removeMessages(Messages.FADE_SEEK_FORWARD_ANIM);

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
            stopSeekForward(animationDurationMs);
        }
        else
        {
            //cancel messages that would cancel the seeking animation we want to play
            delayedHandler.removeMessages(Messages.STOP_SEEK_REVERSE_ANIM);
            delayedHandler.removeMessages(Messages.FADE_SEEK_REVERSE_ANIM);

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
            stopSeekReverse(animationDurationMs);
        }
    }

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
     * Stop the seek forward animation
     * This either sends Message STOP_SEEK_FORWARD_ANIM or FADE_SEEK_FORWARD_ANIM, depending on configuration of the view
     *
     * @param stopDelay by how much the stop of the seek should by delayed by, in ms (<= 0: instantly)
     */
    private void stopSeekForward(long stopDelay)
    {
        if (stopDelay <= 0)
        {
            delayedHandler.sendEmptyMessage(enableFadeOutTexts ? Messages.FADE_SEEK_FORWARD_ANIM : Messages.STOP_SEEK_FORWARD_ANIM);
        }
        else
        {
            delayedHandler.sendEmptyMessageDelayed(enableFadeOutTexts ? Messages.FADE_SEEK_FORWARD_ANIM : Messages.STOP_SEEK_FORWARD_ANIM, stopDelay);
        }
    }

    /**
     * Stop the seek reverse animation
     * This either sends Message STOP_SEEK_REVERSE_ANIM or FADE_SEEK_REVERSE_ANIM, depending on configuration of the view
     *
     * @param stopDelay by how much the stop of the seek should by delayed by, in ms (<= 0: instantly)
     */
    private void stopSeekReverse(long stopDelay)
    {
        if (stopDelay <= 0)
        {
            delayedHandler.sendEmptyMessage(enableFadeOutTexts ? Messages.FADE_SEEK_REVERSE_ANIM : Messages.STOP_SEEK_REVERSE_ANIM);
        }
        else
        {
            delayedHandler.sendEmptyMessageDelayed(enableFadeOutTexts ? Messages.FADE_SEEK_REVERSE_ANIM : Messages.STOP_SEEK_REVERSE_ANIM, stopDelay);
        }
    }
}
