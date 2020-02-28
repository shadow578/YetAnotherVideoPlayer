package de.shadow578.yetanothervideoplayer.feature.autopause.facedetect;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

/**
 * Factory to create GFaceTrackers
 */
public class GFaceTrackerFactory implements MultiProcessor.Factory<Face>
{
    /**
     * Callback for new face tracker instances
     */
    private GFaceTrackerCallback callback;

    /**
     * create a new GFaceTracker factory
     *
     * @param trackerCallback the callback to use for all trackers
     */
    public GFaceTrackerFactory(GFaceTrackerCallback trackerCallback)
    {
        callback = trackerCallback;
    }

    @Override
    public Tracker<Face> create(Face face)
    {
        return new GFaceTracker(callback);
    }
}
