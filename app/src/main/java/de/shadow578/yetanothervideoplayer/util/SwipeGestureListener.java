package de.shadow578.yetanothervideoplayer.util;

import android.graphics.PointF;
import android.util.SizeF;
import android.view.MotionEvent;
import android.view.View;

public class SwipeGestureListener implements View.OnTouchListener
{
    /**
     * The First contact point of the finger
     */
    private PointF firstContactPoint;

    /**
     * The position of the finger the last time one of the onSwipe events was fired
     */
    private PointF lastSwipePoint;

    /**
     * The time the last swipe point was changed
     */
    private long lastSwipePointMillis;

    /**
     * How long the user can stay without moving his finger before
     * the last position is discarded in favor of the current position
     */
    private final long touchDecayTime;

    /**
     * The Minimum distance the user has to move his finger before the movement is
     * registered as swipe
     * [Normalized, 0.0 - 1.0]
     */
    private final float swipeThresholdN;

    /**
     * The Minimum distance the user has to move his finger before the movement is
     * registered as a fling
     * [Normalized, 0.0 - 1.0]
     */
    private final float flingThresholdN;

    /**
     * Initialize the Swipe Gesture Listener
     *
     * @param _touchDecayTime how long the user can stay in one place before the swipe reference it updated to the current position
     * @param _swipeThreshold the minimum distance the user has to move his finger before the movement is registered as swipe [Normalized, 0.0 - 1.0]
     * @param _flingThreshold the minimum distance the user has to move his finger before the movement is registered as fling [Normalized, 0.0 - 1.0]
     */
    protected SwipeGestureListener(long _touchDecayTime, float _swipeThreshold, float _flingThreshold)
    {
        touchDecayTime = _touchDecayTime;
        swipeThresholdN = _swipeThreshold;
        flingThresholdN = _flingThreshold;
    }

    @Override
    public boolean onTouch(View view, MotionEvent e)
    {
        //get current finger position
        PointF currentPos = new PointF(e.getX(), e.getY());

        //get screen size
        SizeF screenSize = new SizeF(e.getDevice().getMotionRange(MotionEvent.AXIS_X).getRange(), e.getDevice().getMotionRange(MotionEvent.AXIS_Y).getRange());

        //switch on event type
        switch (e.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                //finger pressed down, get initial position
                firstContactPoint = currentPos;
                setLastSwipePoint(currentPos);
                return true;
            }
            case MotionEvent.ACTION_UP:
            {
                //finger lifted, fire fling event if threshold met
                //calculate movement delta
                float deltaX = firstContactPoint.x - currentPos.x;
                float deltaY = firstContactPoint.y - currentPos.y;

                //check if movement fulfills length requirements
                if (Math.abs(deltaX) < (flingThresholdN * screenSize.getWidth())
                        && Math.abs(deltaY) < (flingThresholdN * screenSize.getHeight()))
                    return false;

                //fulfills length requirement, call event
                if (Math.abs(deltaX) > Math.abs(deltaY))
                {
                    //~~ Left/Right Horizontal ~~
                    onHorizontalFling(deltaX, firstContactPoint, currentPos, screenSize);
                }
                else
                {
                    //~~ Up/Down Vertical ~~
                    onVerticalFling(deltaY, firstContactPoint, currentPos, screenSize);
                }
                return true;
            }
            case MotionEvent.ACTION_MOVE:
            {
                //finger moved, update swipe event:
                //check if last position decayed
                if ((System.currentTimeMillis() - lastSwipePointMillis) > touchDecayTime)
                {
                    //last touch decayed, set to current position
                    setLastSwipePoint(currentPos);
                    return false;
                }

                //calculate movement delta
                float deltaX = lastSwipePoint.x - currentPos.x;
                float deltaY = lastSwipePoint.y - currentPos.y;

                //check if movement fulfills length requirements
                if (Math.abs(deltaX) < (swipeThresholdN * screenSize.getWidth())
                        && Math.abs(deltaY) < (swipeThresholdN * screenSize.getHeight()))
                    return false;

                //fulfills length requirement, call event
                if (Math.abs(deltaX) > Math.abs(deltaY))
                {
                    //~~ Left/Right Horizontal ~~
                    onHorizontalSwipe(deltaX, lastSwipePoint, currentPos, firstContactPoint, screenSize);
                }
                else
                {
                    //~~ Up/Down Vertical ~~
                    onVerticalSwipe(deltaY, lastSwipePoint, currentPos, firstContactPoint, screenSize);
                }

                //record current pos for next swipe event
                setLastSwipePoint(currentPos);
                return true;
            }
            default:
            {
                //unknown, ignore
                return false;
            }
        }
    }

    /**
     * Set the last swipe point and record the time
     *
     * @param to the point to set
     */
    private void setLastSwipePoint(PointF to)
    {
        lastSwipePoint = to;
        lastSwipePointMillis = System.currentTimeMillis();
    }

    //region ~~ Overrideable "events" ~~

    /**
     * Called if the user's finger moved at least the minimum swipe distance Horizontally
     * (User pressed down and moved finger horizontally at least x pixels, where x is the set swipe threshold
     *
     * @param deltaX       the distance between start and end point, on the X axis only
     * @param swipeStart   the position the current swipe started
     * @param swipeEnd     the position the current swipe ended (current finger position)
     * @param firstContact the first contact point of the finger
     * @param screenSize   the size of the screen
     */
    @SuppressWarnings({"EmptyMethod", "unused"})
    public void onHorizontalSwipe(float deltaX, PointF swipeStart, PointF swipeEnd, PointF firstContact, SizeF screenSize)
    {

    }

    /**
     * Called if the user's finger moved at least the minimum swipe distance Vertically
     * (User pressed down and moved finger Vertically at least x pixels, where x is the set swipe threshold
     *
     * @param deltaY       the distance between start and end point, on the Y axis only
     * @param swipeStart   the position the current swipe started
     * @param swipeEnd     the position the current swipe ended (current finger position)
     * @param firstContact the first contact point of the finger
     * @param screenSize   the size of the screen
     */
    public void onVerticalSwipe(float deltaY, PointF swipeStart, PointF swipeEnd, PointF firstContact, SizeF screenSize)
    {

    }

    /**
     * Called when a Horizontal fling is released
     * (User pressed down, moved finger horizontally, then released finger)
     *
     * @param deltaX     the distance between start and end point, on the X axis only
     * @param flingStart the position the fling started
     * @param flingEnd   the position the fling ended
     * @param screenSize the size of the screen
     */
    @SuppressWarnings({"EmptyMethod", "unused"})
    public void onHorizontalFling(float deltaX, PointF flingStart, PointF flingEnd, SizeF screenSize)
    {

    }

    /**
     * Called when a vertical fling is released
     * (User pressed down, moved finger vertically, then released finger)
     *
     * @param deltaY     the distance between start and end point, on the Y axis only
     * @param flingStart the position the fling started
     * @param flingEnd   the position the fling ended
     * @param screenSize the size of the screen
     */
    @SuppressWarnings({"EmptyMethod", "unused"})
    public void onVerticalFling(float deltaY, PointF flingStart, PointF flingEnd, SizeF screenSize)
    {

    }

    //endregion
}
