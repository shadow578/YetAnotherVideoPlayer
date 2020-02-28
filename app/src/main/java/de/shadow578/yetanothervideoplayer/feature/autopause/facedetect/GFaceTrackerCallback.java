package de.shadow578.yetanothervideoplayer.feature.autopause.facedetect;

/**
 * Callback for the Google Face Tracker
 */
public interface GFaceTrackerCallback
{
    /**
     * we got a (new) active face
     */
    void foundActiveFace();

    /**
     * we just lost a active face
     */
    void lostActiveFace();
}
