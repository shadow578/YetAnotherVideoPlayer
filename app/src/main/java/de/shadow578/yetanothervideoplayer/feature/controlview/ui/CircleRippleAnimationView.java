package de.shadow578.yetanothervideoplayer.feature.controlview.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import de.shadow578.yetanothervideoplayer.R;

/**
 * View that creates a custom ripple effect on one side of the screen
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class CircleRippleAnimationView extends View implements ValueAnimator.AnimatorUpdateListener, ValueAnimator.AnimatorListener
{
    //region Constants
    /**
     * Minimum radius of the ripple circle, in dp
     */
    private final float MIN_RIPPLE_RADIUS_DP = 10f;

    /**
     * Maximum radius of the ripple circle, in dp
     */
    private final float MAX_RIPPLE_RADIUS_DP = 400f;

    /**
     * default color of the arc
     */
    private final int DEFAULT_ARC_COLOR = Color.WHITE;

    /**
     * default color of the ripple circle
     */
    private final int DEFAULT_RIPPLE_COLOR = Color.LTGRAY;

    /**
     * default arc size
     */
    private final int DEFAULT_ARC_SIZE = 80;

    /**
     * Default animation duration, ms
     */
    private final int DEFAULT_ANIMATION_DURATION = 800;
    //endregion

    //region Variables

    /**
     * the paint the background arc is painted with
     */
    private final Paint arcPaint;

    /**
     * the paint the ripple circle is painted with
     */
    private final Paint ripplePaint;

    /**
     * the path of the background arc
     */
    private final Path arcPath = new Path();

    /**
     * the animator that animates the ripple circle
     */
    private final ValueAnimator rippleAnimator;

    /**
     * the minimum ripple radius for this device
     */
    private float minRippleRadius;

    /**
     * the maximum ripple radius for this device
     */
    private float maxRippleRadius;

    /**
     * the size of the background arc
     */
    private float arcSize = DEFAULT_ARC_SIZE;

    /**
     * the current radius of the ripple circle
     */
    private float currentRippleRadius;

    /**
     * how long the ripple animation lasts
     */
    private long rippleAnimationDuration = DEFAULT_ANIMATION_DURATION;

    /**
     * the position of the ripple circles origin
     */
    private PointF rippleOrigin = new PointF(0f, 0f);

    //endregion

    //region Constructors
    public CircleRippleAnimationView(Context context)
    {
        this(context, null);
    }

    public CircleRippleAnimationView(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public CircleRippleAnimationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    public CircleRippleAnimationView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        //do normal stuff
        super(context, attrs, defStyleAttr, defStyleRes);

        //create paint for arc
        arcPaint = new Paint();
        arcPaint.setStyle(Paint.Style.FILL);
        arcPaint.setAntiAlias(true);
        arcPaint.setColor(DEFAULT_ARC_COLOR);

        //create paint for ripple
        ripplePaint = new Paint();
        ripplePaint.setStyle(Paint.Style.FILL);
        ripplePaint.setAntiAlias(true);
        ripplePaint.setColor(DEFAULT_RIPPLE_COLOR);

        //setup min and max radius of the ripple circle
        minRippleRadius = dpToPx(MIN_RIPPLE_RADIUS_DP, context);
        maxRippleRadius = Math.min(dpToPx(MAX_RIPPLE_RADIUS_DP, context), Math.max(getWidth() / 2f, getHeight()));

        //do initial update of the arc's path
        updateArcPath();

        //initialize ripple animator
        rippleAnimator = initializeCircleAnimator();

        //apply styleables
        applyStyleAttributes(context, attrs);

        //initialize as invisible
        setVisibility(GONE);
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
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleRippleAnimationView);

        //set values
        setArcColor(a.getColor(R.styleable.CircleRippleAnimationView_arcColor, DEFAULT_ARC_COLOR));
        setRippleColor(a.getColor(R.styleable.CircleRippleAnimationView_rippleColor, DEFAULT_RIPPLE_COLOR));
        setArcSize(a.getDimensionPixelSize(R.styleable.CircleRippleAnimationView_arcSize, DEFAULT_ARC_SIZE));
        setRippleAnimationDuration(a.getInteger(R.styleable.CircleRippleAnimationView_rippleAnimationDuration, DEFAULT_ANIMATION_DURATION));

        //we're finished, recycle attribute array
        a.recycle();
    }
    //endregion

    //region Interfacing

    /**
     * Set the size of the arc
     *
     * @param newSize the new size of the arc
     * @return own instance, for set chaining
     */
    public CircleRippleAnimationView setArcSize(float newSize)
    {
        arcSize = newSize;
        updateArcPath();
        return this;
    }

    /**
     * @return the current arc size
     */
    public float getArcSize()
    {
        return arcSize;
    }

    /**
     * Set the duration of the circle animation
     *
     * @param duration the duration to set (in milliseconds)
     * @return own instance, for set chaining
     */
    public CircleRippleAnimationView setRippleAnimationDuration(long duration)
    {
        if (duration < 0)
            throw new IllegalArgumentException("duration cannot be less than 0!");

        rippleAnimationDuration = duration;
        rippleAnimator.setDuration(duration);
        return this;
    }

    /**
     * @return the current duration for the circle animation
     */
    public long getRippleAnimationDuration()
    {
        return rippleAnimationDuration;
    }

    /**
     * set the color of the animated ripple effect
     *
     * @param color the color to set
     * @return own instance, for set chaining
     */
    public CircleRippleAnimationView setRippleColor(@ColorInt int color)
    {
        ripplePaint.setColor(color);
        return this;
    }

    /**
     * @return the color of the animated ripple effect
     */
    public int getRippleColor()
    {
        return ripplePaint.getColor();
    }

    /**
     * set the color of the background arc
     *
     * @param color the color to set
     * @return own instance, for set chaining
     */
    public CircleRippleAnimationView setArcColor(@ColorInt int color)
    {
        arcPaint.setColor(color);
        return this;
    }

    /**
     * @return the color of the background arc
     */
    public int getArcColor()
    {
        return arcPaint.getColor();
    }

    /**
     * Update the origin of the animated ripple
     *
     * @param newPosition the new position of the ripple's origin
     * @return own instance, for set chaining
     */
    public CircleRippleAnimationView setRippleOrigin(PointF newPosition)
    {
        //set position of ripple origin
        rippleOrigin = newPosition;

        //update background path for new position
        updateArcPath();

        return this;
    }

    /**
     * @return the origin of the animated ripple
     */
    public PointF getRippleOrigin()
    {
        return rippleOrigin;
    }

    /**
     * Set the ripple origin and start the animation
     *
     * @param rippelOrigin the new origin of the animated ripple
     */
    public void startAnimationAt(PointF rippelOrigin)
    {
        setRippleOrigin(rippelOrigin);
        startAnimation();
    }

    /**
     * Start the animation
     */
    public void startAnimation()
    {
        //make self visible
        setVisibility(VISIBLE);

        //start the animation
        rippleAnimator.start();
    }

    //endregion

    //region AnimationUpdateListener
    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator)
    {
        updateRippleRadiusAndInvalidate((float) valueAnimator.getAnimatedValue());
    }
    //endregion

    //region AnimatorListener

    @Override
    public void onAnimationStart(Animator animator)
    {
        //make sure we're visible
        setVisibility(VISIBLE);
    }

    @Override
    public void onAnimationEnd(Animator animator)
    {
        //make self invisible
        setVisibility(GONE);
    }

    @Override
    public void onAnimationCancel(Animator animator)
    {
        //nothing
    }

    @Override
    public void onAnimationRepeat(Animator animator)
    {
        //nothing
    }
    //endregion

    @Override
    protected void onDraw(Canvas canvas)
    {
        //do normal draw
        super.onDraw(canvas);

        //check we have a canvas to draw on
        if (canvas == null) return;

        //clip the canvas using the arc's path
        canvas.clipPath(arcPath);

        //draw the arc
        canvas.drawPath(arcPath, arcPaint);

        //draw ripple with current radius
        canvas.drawCircle(rippleOrigin.x, rippleOrigin.y, currentRippleRadius, ripplePaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        //do normal size changes first
        super.onSizeChanged(w, h, oldw, oldh);

        //setup min and max radius of the ripple circle
        minRippleRadius = dpToPx(MIN_RIPPLE_RADIUS_DP, getContext());
        maxRippleRadius = Math.min(dpToPx(MAX_RIPPLE_RADIUS_DP, getContext()), Math.max(getWidth() / 2f, getHeight()));

        //update arc path
        updateArcPath();
    }

    /**
     * Create the animator used to initialize the "ripple effect" circle
     *
     * @return the value animator for the circle animation
     */
    private ValueAnimator initializeCircleAnimator()
    {
        //create new animator instance
        ValueAnimator vanim = new ValueAnimator();
        vanim.setFloatValues(0f, 1f);
        vanim.setDuration(rippleAnimationDuration);
        vanim.addUpdateListener(this);
        vanim.addListener(this);
        return vanim;
    }

    /**
     * Updates the shape of the static background curve
     */
    private void updateArcPath()
    {
        //reset previous path
        arcPath.reset();

        //prepare some values for drawing
        float halfWidth = getWidth() / 2f;
        float halfHeight = getHeight() / 2f;
        float widthOffset = isRippleOriginLeft() ? 0f : getWidth();
        float sizeMod = isRippleOriginLeft() ? 1f : -1f;

        //draw arc shape
        arcPath.moveTo(widthOffset, 0f);
        arcPath.lineTo(((halfWidth - arcSize) * sizeMod) + widthOffset, 0f);
        arcPath.quadTo(((halfWidth + arcSize) * sizeMod) + widthOffset, halfHeight,
                ((halfWidth - arcSize) * sizeMod) + widthOffset, getHeight());
        arcPath.lineTo(widthOffset, getHeight());
        arcPath.close();

        //redraw view
        invalidate();
    }

    /**
     * Update the radius of the animated ripple and invalidate the view
     *
     * @param radiusFactor the factor of the radius (0f to 1f)
     */
    private void updateRippleRadiusAndInvalidate(float radiusFactor)
    {
        //clamp value between 0 and 1
        if (radiusFactor < 0f) radiusFactor = 0f;
        if (radiusFactor > 1f) radiusFactor = 1f;

        //calculate new radius of the ripple circle
        currentRippleRadius = ((maxRippleRadius - minRippleRadius) * radiusFactor) + minRippleRadius;

        //invalidate the view to redraw
        invalidate();
    }

    //region Util

    /**
     * Convert from density- independent pixels (dp) to device- specific pixels (px)
     *
     * @param dp      the dp to convert
     * @param context the context to convert in
     * @return the corresponding px
     */
    private float dpToPx(float dp, Context context)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    /**
     * @return is the animated ripple's origin on the left screen side?
     */
    private boolean isRippleOriginLeft()
    {
        return rippleOrigin != null && rippleOrigin.x <= (getWidth() / 2f);
    }

    //endregion
}
