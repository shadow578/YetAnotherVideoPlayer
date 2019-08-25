package de.shadow578.yetanothervideoplayer.util;

import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

@SuppressWarnings("WeakerAccess")
public abstract class PanGestureListener implements OnTouchListener
{
    /**
     * Represents the direction of a swipe/pan/gesture
     */
    public enum GestureDirection
    {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    /**
     * The Gesture Detector used to detect swiping, panning, etc
     */
    private final GestureDetector gestureDetector;

    /**
     * Initialize the Listener with default constrains
     *
     * @param context the context to detect gestures in
     */
    public PanGestureListener(Context context)
    {
        this(context, 10, 0, 0);
    }

    /**
     * Initialize the Listener
     *
     * @param context               the context to detect gestures in
     * @param lengthThreshold       the minimum length of gestures. Gestures with a length below this threshold will be ignored
     * @param ignoreHorizontalEdges how many pixels are ignored on the horizontal (left/right) edges
     * @param ignoreVerticalEdges   how many pixels are ignored on the vertical (top/bottom) edges
     */
    public PanGestureListener(Context context, float lengthThreshold, float ignoreHorizontalEdges, float ignoreVerticalEdges)
    {
        //init gesture detector
        gestureDetector = new GestureDetector(context, new GestureInterpreter(lengthThreshold, ignoreHorizontalEdges, ignoreVerticalEdges));
    }

    /**
     * Called when the GestureInterpreter detects a pan within the set constrains
     *
     * @param gestureDirection the direction of the pan
     * @param panInfo          information about the pan
     */
    public void onPan(GestureDirection gestureDirection, PanEventInfo panInfo)
    {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        return gestureDetector.onTouchEvent(event);
    }

    /**
     * Interprets gestures based on the events of the Gesture Detector
     */
    private final class GestureInterpreter extends SimpleOnGestureListener
    {
        /**
         * How many pixels on the bottom/top edges are ignored
         */
        private final float pxIgnoreVEdge;

        /**
         * How many pixels on the right/left edges are ignored
         */
        private final float pxIgnoreHEdge;

        /**
         * How many pixels long must a gesture be before being detected?
         */
        private final float lengthThreshold;

        /**
         * Initialize the Interpreter
         *
         * @param _lengthThreshold the minimum length of gestures. Gestures with a length below this threshold will be ignored
         * @param ignoreHorizontal how many pixels are ignored on the horizontal (left/right) edges
         * @param ignoreVertical   how many pixels are ignored on the vertical (top/bottom) edges
         */
        public GestureInterpreter(float _lengthThreshold, float ignoreHorizontal, float ignoreVertical)
        {
            //set variables
            lengthThreshold = _lengthThreshold;
            pxIgnoreVEdge = ignoreVertical;
            pxIgnoreHEdge = ignoreHorizontal;
        }

        /**
         * Called when scrolling (/panning) is detected
         *
         * @param e1        the start point of the scroll
         * @param e2        the end point of the scroll
         * @param distanceX the distance scrolled on the X axis
         * @param distanceY the distance scrolled on the Y axis
         * @return true if the event was consumed. Scroll ends and a new is started if event is consumed
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            //calc length of scroll
            float length = (float) Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));

            //check if length is above threshold
            if (length < lengthThreshold) return false;

            //get start + end position as pointF's
            PointF startPos = new PointF(e1.getX(), e1.getY());
            PointF endPos = new PointF(e2.getX(), e2.getY());

            //get range of X and Y axis the points lie on
            float rangeX = e1.getDevice().getMotionRange(MotionEvent.AXIS_X).getRange();
            float rangeY = e1.getDevice().getMotionRange(MotionEvent.AXIS_Y).getRange();

            //calculate coordinates of ignored areas
            float ignoreXLow = pxIgnoreVEdge;
            float ignoreYLow = pxIgnoreHEdge;
            float ignoreXHigh = rangeX - pxIgnoreVEdge;
            float ignoreYHigh = rangeY - pxIgnoreHEdge;

            //check if start and end position is in ignored area
            if (startPos.x < ignoreXLow || startPos.x > ignoreXHigh || startPos.y < ignoreYLow || startPos.y > ignoreYHigh
                    || endPos.x < ignoreXLow || endPos.x > ignoreXHigh || endPos.y < ignoreYLow || endPos.y > ignoreYHigh)
            {
                //outside of bounds, discard event
                return true;
            }

            //~~ Start + End Pos are confirmed to be in bounds, and minimum length threshold is met ~~
            //detect scroll direction
            GestureDirection direction;
            if (Math.abs(distanceX) > Math.abs(distanceY))
            {
                if (distanceX > 0)
                {
                    direction = GestureDirection.LEFT;
                }
                else
                {
                    direction = GestureDirection.RIGHT;
                }
            }
            else
            {
                if (distanceY > 0)
                {
                    direction = GestureDirection.UP;
                }
                else
                {
                    direction = GestureDirection.DOWN;
                }
            }

            //create the event info
            PanEventInfo eventInfo = new PanEventInfo();
            eventInfo.distanceX = distanceX;
            eventInfo.distanceY = distanceY;
            eventInfo.totalLength = length;
            eventInfo.startPos = startPos;
            eventInfo.endPos = endPos;
            eventInfo.axisRangeX = rangeX;
            eventInfo.axisRangeY = rangeY;

            //call event
            onPan(direction, eventInfo);

            //consume event
            return true;
        }
    }

    /**
     * Contains information about a pan event
     */
    public class PanEventInfo
    {
        /**
         * The Distance of the pan on the X axis
         * X = Horizontal = Left/Right
         */
        public float distanceX;

        /**
         * The Distance of the pan on the Y axis
         * Y = Vertical = Up/Down
         */
        public float distanceY;

        /**
         * The total length of the pan (X&Y)
         */
        public float totalLength;

        /**
         * The Position where the pan started
         */
        public PointF startPos;

        /**
         * The Position where the pan ended
         */
        public PointF endPos;

        /**
         * The Range of the X axis the values lie on
         * X = Horizontal = Left/Right
         */
        public float axisRangeX;

        /**
         * The Range of the Y axis the values lie on
         * Y = Vertical = Up/Down
         */
        public float axisRangeY;

        /**
         * Get the Pan X Distance in normalized form in range 0.0 - 1.0
         * X = Horizontal = Left/Right
         *
         * @return the pan distance in range 0.0 - 1.0, where 0.0 = 0 pixels, and 1.0 = full size of the screen
         */
        public float getXDistanceNormalized()
        {
            return distanceX / axisRangeX;
        }

        /**
         * Get the Pan Y Distance in normalized form in range 0.0 - 1.0
         * Y = Vertical = Up/Down
         *
         * @return the pan distance in range 0.0 - 1.0, where 0.0 = 0 pixels, and 1.0 = full size of the screen
         */
        public float getYDistanceNormalized()
        {
            return distanceY / axisRangeY;
        }

        /**
         * Get the Pan Length in normalized form in range 0.0 - 1.0
         *
         * @return the pan distance in range 0.0 - 1.0, where 0.0 = 0 pixels, and 1.0 = full size of the screen
         */
        public float getTotalLengthNormalized()
        {
            return (float) Math.sqrt(Math.pow(getXDistanceNormalized(), 2) + Math.pow(getYDistanceNormalized(), 2));
        }
    }
}
