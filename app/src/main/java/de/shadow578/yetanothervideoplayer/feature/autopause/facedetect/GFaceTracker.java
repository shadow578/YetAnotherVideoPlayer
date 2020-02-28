package de.shadow578.yetanothervideoplayer.feature.autopause.facedetect;


import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Face tracker that calls a callback as soon as no faces are detected anymore
 */
public class GFaceTracker extends Tracker<Face>
{
    /**
     * Counter to reduce error when a face has the eyes closed
     */
    private int noActiveFaceCounter = 0;

    /**
     * do we currently have a active face?
     */
    private boolean hasActiveFace = false;

    /**
     * callback to call back
     */
    private GFaceTrackerCallback callback;

    /**
     * create a instance of a GFaceTracker
     *
     * @param trackerCallback the callback to use
     */
    GFaceTracker(GFaceTrackerCallback trackerCallback)
    {
        callback = trackerCallback;
    }

    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face)
    {
        //is at leas one eye opened?
        if (face.getIsLeftEyeOpenProbability() > 0.10 && face.getIsRightEyeOpenProbability() > 0.10)
        {
            //reset counter
            noActiveFaceCounter = 0;

            if (!hasActiveFace)
            {
                //tell callback that we got a face
                hasActiveFace = true;
                callback.foundActiveFace();
            }
        }
        else
        {
            //count counter up (this is to reduce error)
            noActiveFaceCounter++;

            //if there was no active face for 2 (detection) frames, tell callback that we no longer have a active face
            if (noActiveFaceCounter > 2 && hasActiveFace)
            {
                hasActiveFace = false;
                callback.lostActiveFace();
            }
        }
    }

    @Override
    public void onDone()
    {
        //tell callback that we no longer have a active face
        if (hasActiveFace)
        {
            hasActiveFace = false;
            callback.lostActiveFace();
        }
    }


    @Override
    public void onNewItem(int faceId, Face item)
    {
    }

    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults)
    {
    }
}
