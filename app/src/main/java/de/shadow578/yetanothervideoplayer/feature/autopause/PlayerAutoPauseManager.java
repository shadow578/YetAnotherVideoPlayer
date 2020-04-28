package de.shadow578.yetanothervideoplayer.feature.autopause;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;

import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

import de.shadow578.yetanothervideoplayer.feature.autopause.facedetect.GFaceTrackerCallback;
import de.shadow578.yetanothervideoplayer.feature.autopause.facedetect.GFaceTrackerFactory;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Handles automatic pausing and screen blanking based on sensor states
 */
public class PlayerAutoPauseManager
{
    /**
     * How long must the proximity be NEAR before triggering NEAR_PROXIMITY?
     */
    private final long NEAR_PROXIMITY_THRESHOULD_TIME = 5000;//ms

    /**
     * How long must the light value be low before triggering LOW_LIGHT?
     */
    private final long LOW_LIGHT_THRESHOULD_TIME = 5000;//ms

    /**
     * Below what value the light intensity has to fall to trigger LOW_LIGHT event
     */
    private final float LOW_LIGHT_THRESHOLD = 6f;

    /**
     * The activity this manager is used by
     */
    private Activity context;

    /**
     * Manager callback for pause and error functions
     */
    private PlayerAutoPauseCallback callback;

    /**
     * The common sensor manager instance used for... sensors
     */
    private SensorManager sensorManager;

    /**
     * The listener that listens for events on the proximity sensor
     */
    private ProximitySensorListener proximityListener;

    /**
     * The listener that listens for events on the light sensor
     */
    private LightSensorListener lightListener;

    /**
     * The tracker that tracks the users face to detect inactivity
     */
    private UserFaceTracker faceTracker;

    /**
     * Preview for the face tracker. THIS may be null, so handle with care!
     */
    private DetectorCameraPreview faceTrackerPreview;

    /**
     * create a new auto pause manager
     *
     * @param ctx the context of the manager (activity)
     */
    public PlayerAutoPauseManager(Activity ctx)
    {
        context = ctx;
    }

    //region manager interface

    /**
     * Initialize the Auto pause manager
     *
     * @param pauseCallback the callback to use
     * @return was init ok?
     */
    public boolean initialize(PlayerAutoPauseCallback pauseCallback)
    {
        //check callback is valid
        if (pauseCallback == null) return false;

        //set callback
        callback = pauseCallback;

        //check the context is valid
        if (context == null) return false;

        //get sensor manager
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        //check we have a sensor manager
        return sensorManager != null;
    }

    /**
     * Set the preview component for the face tracker.
     * Does not automatically update anything, so make sure to call activate() only after calling this (when you want a preview)
     *
     * @param preview the preview to use
     */
    public void setFaceTrackerPreview(DetectorCameraPreview preview)
    {
        faceTrackerPreview = preview;
    }

    /**
     * activate the auto pause manager.
     * !! call initialize() before !!
     *
     * @param useProximity  should the proximity sensor be used (pause on face- down)?
     * @param useFaceDetect should face detection be used (pause on look- away)?
     */
    public void activate(boolean useProximity, boolean useFaceDetect)
    {
        Logging.logD("activating auto pause manager with PROX= %b and FACE= %b", useProximity, useFaceDetect);

        //enable proximity
        boolean isLightSensorProximityFallback = false;
        if (useProximity)
        {
            isLightSensorProximityFallback = !enableProximity();
        }

        //enable light sensor for face detection OR as fallback for proximity sensor
        boolean isLightSensorActive = false;
        if (useFaceDetect || isLightSensorProximityFallback)
        {
            isLightSensorActive = enableLight();
        }

        //enable face detection
        if (useFaceDetect)
        {
            if (!enableFaceDetect() && !isLightSensorProximityFallback && isLightSensorActive)
            {
                //face detection failed AND light sensor is not used for proximity, disable light sensor
                disableLight();
            }
        }
    }

    /**
     * deactivate the auto pause manager and release resources
     */
    public void deactivate()
    {
        Logging.logD("deactivating auto pause manager");

        //disable all sub- functions
        disableProximity();
        disableLight();
        disableFaceDetect();
    }
    //endregion

    //region sub- function enables/disables

    /**
     * enables the proximity sensor and callback
     *
     * @return is proximity monitoring now active?
     */
    private boolean enableProximity()
    {
        Logging.logD("enable proximity sensor...");

        //dont enable again if already enabled
        if (proximityListener != null)
        {
            Logging.logD("skipping enable of proximity monitoring: already active");
            return true;
        }

        //check sensor manager is valid
        if (sensorManager == null)
            throw new IllegalStateException("No sensor manager is available! call initialize() before enabling!");

        //create callback
        proximityListener = new ProximitySensorListener();

        //get proximity sensor
        Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        ////use light sensor as fallback proximity sensor
        //if (proximitySensor == null)
        //    proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //check we now have a sensor, cancel if not
        if (proximitySensor == null)
        {
            //trigger error callback
            internal_onError(PlayerAutoPauseErrors.NO_PROXIMITY_SENSOR);

            //roll back
            disableProximity();
            return false;
        }

        //register callback
        sensorManager.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        //send event
        internal_onEvent(PlayerAutoPauseEvents.PROXIMITY_MONITORING_STARTED);
        return true;
    }

    /**
     * disables the proximity sensor and callback
     */
    private void disableProximity()
    {
        Logging.logD("deactivating proximity sensor callback...");

        //check sensor manager is valid
        if (sensorManager == null)
            throw new IllegalStateException("No sensor manager is available! call initialize() before enabling!");

        //unregister proximity sensor
        if (proximityListener != null)
        {
            sensorManager.unregisterListener(proximityListener);
            proximityListener = null;

            //send event
            internal_onEvent(PlayerAutoPauseEvents.PROXIMITY_MONITORING_STOPPED);
        }
    }

    /**
     * enables the light sensor and callback
     *
     * @return is light monitoring active now?
     */
    private boolean enableLight()
    {
        Logging.logD("enable light sensor...");

        //dont enable again if already enabled
        if (lightListener != null)
        {
            Logging.logD("skipping enable of light monitoring: already active");
            return true;
        }

        //check sensor manager is valid
        if (sensorManager == null)
            throw new IllegalStateException("No sensor manager is available! call initialize() before enabling!");

        //create callback
        lightListener = new LightSensorListener();

        //get light intensity sensor
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //check we now have a sensor, cancel if not
        if (lightSensor == null)
        {
            //trigger error callback
            internal_onError(PlayerAutoPauseErrors.NO_LIGHT_SENSOR);

            //roll back
            disableLight();
            return false;
        }

        //register callback
        sensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        //send event
        internal_onEvent(PlayerAutoPauseEvents.LIGHT_MONITORING_STARTED);
        return true;
    }

    /**
     * disables the light sensor and callback
     */
    private void disableLight()
    {
        Logging.logD("deactivating light sensor callback...");

        //check sensor manager is valid
        if (sensorManager == null)
            throw new IllegalStateException("No sensor manager is available! call initialize() before enabling!");

        //unregister light sensor
        if (lightListener != null)
        {
            sensorManager.unregisterListener(lightListener);
            lightListener = null;

            //send event
            internal_onEvent(PlayerAutoPauseEvents.LIGHT_MONITORING_STOPPED);
        }
    }

    /**
     * enables the face detection and callback
     *
     * @return is face detection active now
     */
    private boolean enableFaceDetect()
    {
        Logging.logD("enabling face detection...");

        //dont enable face tracking again if already enabled
        if (faceTracker != null)
        {
            Logging.logD("skipping enable of face detection: already active");
            return true;
        }

        //create face tracker and start tracking
        faceTracker = new UserFaceTracker();
        boolean runningNow = faceTracker.startTracking();

        //send event
        internal_onEvent(PlayerAutoPauseEvents.FACE_TRACKING_STARTED);

        return runningNow;
    }

    /**
     * disables the face detection and callback
     */
    private void disableFaceDetect()
    {
        Logging.logD("disabling face detection...");

        //stop tracking if active
        if (faceTracker != null)
        {
            //stop and free tracker
            faceTracker.stopTracking();
            faceTracker = null;

            //send event
            internal_onEvent(PlayerAutoPauseEvents.FACE_TRACKING_STOPPED);
        }
    }

    //endregion

    //region wrapper for callback

    /**
     * triggers onEvent() handler on the callback
     *
     * @param event the event to report
     */
    private void internal_onEvent(PlayerAutoPauseEvents event)
    {
        if (callback != null)
        {
            //forward event
            callback.onPAPManagerEvent(event);

            //handle special cases
            if (event == PlayerAutoPauseEvents.LOW_LIGHT)
            {
                //low light situation, stop face tracking if enabled
                disableFaceDetect();

                //IF no proximity sensor is running, light sensor is fallback sensor for that function.
                //in that case, invoke NEAR_PROXIMITY event as well
                if (proximityListener == null)
                {
                    callback.onPAPManagerEvent(PlayerAutoPauseEvents.NEAR_PROXIMITY);
                }
            }
        }

    }

    /**
     * triggers onError() handler on the callback
     *
     * @param error the error that occured
     */
    private void internal_onError(PlayerAutoPauseErrors error)
    {
        if (callback != null) callback.onPAPManagerError(error);
    }

    //endregion

    /**
     * Sensor listener for the proximity sensor.
     */
    private class ProximitySensorListener implements SensorEventListener
    {
        /**
         * Countdown to delay pausing a bit (and filter out error)
         */
        private CountDownTimer nearProximityCountdown;

        /**
         * The last reported proximity
         * -1 if no value was reported (yet)
         */
        private float lastProximity = -1f;

        @Override
        public void onSensorChanged(SensorEvent e)
        {
            //check values array is populated (sanity check)
            if (e.values.length <= 0) return;

            //ONLY allow value of 0 from the second reading on to avoid constant pausing if a sensor only reads zero (defective sensor)
            if (lastProximity != -1 || e.values[0] > 0)
            {
                //update proximity value
                lastProximity = e.values[0];
            }

            //check if proximity sensor reports NEAR value (less than 1)
            handleCountdown(lastProximity != -1 && lastProximity <= 1);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {
        }

        /**
         * handles the countdown to delay automatic pausing (decrease error)
         *
         * @param isNear is the proximity currently NEAR?
         */
        private void handleCountdown(boolean isNear)
        {
            if (isNear)
            {
                //near proximity, start countdown
                if (nearProximityCountdown == null)
                {
                    nearProximityCountdown = new CountDownTimer(NEAR_PROXIMITY_THRESHOULD_TIME, NEAR_PROXIMITY_THRESHOULD_TIME)
                    {
                        @Override
                        public void onTick(long l)
                        {
                            //dont care
                        }

                        @Override
                        public void onFinish()
                        {
                            //pause NOW
                            Logging.logD("proximity countdown finished! triggering event now...");
                            internal_onEvent(PlayerAutoPauseEvents.NEAR_PROXIMITY);

                            //reset countdown
                            nearProximityCountdown = null;
                        }
                    };

                    //start the countdown now
                    nearProximityCountdown.start();
                    Logging.logD("proximity countdown started.");
                }
            }
            else
            {
                //far proximity, stop countdown and reset
                if (nearProximityCountdown != null)
                {
                    Logging.logD("proximity countdown reset.");
                    nearProximityCountdown.cancel();
                    nearProximityCountdown = null;
                }
            }
        }
    }

    /**
     * Sensor listener for the light sensor.
     */
    private class LightSensorListener implements SensorEventListener
    {
        /**
         * Countdown to delay pausing a bit (and filter out error)
         */
        private CountDownTimer lowLightCountdown;

        /**
         * The last reported light value
         * -1 if no value was reported (yet)
         */
        private float lastLight = -1f;

        @Override
        public void onSensorChanged(SensorEvent e)
        {
            //check values array is populated (sanity check)
            if (e.values.length <= 0) return;

            //ONLY allow value of 0 from the second reading on to avoid constant pausing if a sensor only reads zero (defective sensor)
            if (lastLight != -1 || e.values[0] > 0)
            {
                //update proximity value
                lastLight = e.values[0];
            }

            //check if light sensor reports LOW light value
            handleCountdown(lastLight != -1 && lastLight <= LOW_LIGHT_THRESHOLD);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {
        }

        /**
         * handles the countdown to delay automatic pausing (decrease error)
         *
         * @param isLowLight is the light value currently LOW?
         */
        private void handleCountdown(boolean isLowLight)
        {
            if (isLowLight)
            {
                //near proximity, start countdown
                if (lowLightCountdown == null)
                {
                    lowLightCountdown = new CountDownTimer(LOW_LIGHT_THRESHOULD_TIME, LOW_LIGHT_THRESHOULD_TIME)
                    {
                        @Override
                        public void onTick(long l)
                        {
                            //dont care
                        }

                        @Override
                        public void onFinish()
                        {
                            //pause NOW
                            Logging.logD("low light countdown finished! triggering event now...");
                            internal_onEvent(PlayerAutoPauseEvents.LOW_LIGHT);

                            //reset countdown
                            lowLightCountdown = null;
                        }
                    };

                    //start the countdown now
                    lowLightCountdown.start();
                    Logging.logD("low light countdown started.");
                }
            }
            else
            {
                //far proximity, stop countdown and reset
                if (lowLightCountdown != null)
                {
                    Logging.logD("low light countdown reset.");
                    lowLightCountdown.cancel();
                    lowLightCountdown = null;
                }
            }
        }
    }

    /**
     * class that tracks faces in the front camera (eg. of the user)
     */
    private class UserFaceTracker implements GFaceTrackerCallback
    {
        /**
         * the detector that ...well... detects the faces
         */
        private FaceDetector detector;

        /**
         * the camera that we are detecting faces in (the front camera)
         */
        private CameraSource detectorCam;

        /**
         * Do we currently have a active face (that we could loose?)
         */
        private boolean hasActiveFace = false;

        /**
         * start the face tracking
         *
         * @return is tracking active now?
         */
        boolean startTracking()
        {
            //check device has play services available
            int gmsStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
            if (gmsStatus != ConnectionResult.SUCCESS)
            {
                //play services are not available
                //show google play error dialog
                GoogleApiAvailability.getInstance().getErrorDialog(context, gmsStatus, /*GMS_HANDLE*/4525);

                //invoke error callback
                internal_onError(PlayerAutoPauseErrors.NO_GMS_SERVICES_AVAILABLE);
                return false;
            }

            //initialize camera and tracking
            if (!initCamAndDetector())
            {
                //init failed, but function already called callback - so we just return now
                return false;
            }

            //try to start the tracking
            try
            {
                if (faceTrackerPreview != null)
                {
                    //start detector with preview
                    Logging.logD("Start Face Tracker with preview...");
                    faceTrackerPreview.setCameraSource(detectorCam);
                }
                else
                {
                    //start detector without preview
                    Logging.logD("Start Face Tracker without preview...");
                    detectorCam.start();
                }
                return true;
            }
            catch (IOException e)
            {
                //log the error
                Logging.logE("Error starting face tracking camera source: %s", e.toString());

                //invoke error handler
                internal_onError(PlayerAutoPauseErrors.UNDEFINED);

                //release cam and tracking
                releaseAll();
                return false;
            }
        }

        /**
         * initializes detector and detectorCam for tracking
         *
         * @return was the cam and detector created ok?
         */
        private boolean initCamAndDetector()
        {
            //check for cam permissions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                //missing permissions
                internal_onError(PlayerAutoPauseErrors.NO_CAMERA_PERMISSION);
                return false;
            }

            //check if front cam exists
            if (!hasFrontCam())
            {
                internal_onError(PlayerAutoPauseErrors.NO_FRONT_CAMERA);
                return false;
            }

            //build the detector
            detector = new FaceDetector.Builder(context)
                    .setTrackingEnabled(false)
                    .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                    .build();
            detector.setProcessor(new MultiProcessor.Builder<>(new GFaceTrackerFactory(this)).build());

            //check decoder is operational
            if (!detector.isOperational())
            {
                //should never happen...
                internal_onError(PlayerAutoPauseErrors.UNDEFINED);
                return false;
            }

            //create cam source
            detectorCam = new CameraSource.Builder(context, detector)
                    .setRequestedPreviewSize(640, 480)
                    .setRequestedFps(30.0f)
                    .setAutoFocusEnabled(true)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .build();

            //all ok
            return true;
        }

        /**
         * stop face tracking and release resources
         */
        void stopTracking()
        {
            releaseAll();
        }

        /**
         * Release camera source and tracking stuff
         */
        private void releaseAll()
        {
            //release preview if we have one
            if (faceTrackerPreview != null) faceTrackerPreview.release();

            //release detector
            if (detector != null) detector.release();

            //release cam source
            if (detectorCam != null)
            {
                detectorCam.stop();
                detectorCam.release();
            }
        }

        /**
         * @return has the device a front camera?
         */
        private boolean hasFrontCam()
        {
            return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        }

        @Override
        public void foundActiveFace()
        {
            //we have the active face!
            hasActiveFace = true;
            internal_onEvent(PlayerAutoPauseEvents.GOT_ACTIVE_FACE);
        }

        @Override
        public void lostActiveFace()
        {
            //we lost the active face, stop playback
            if (hasActiveFace)
            {
                hasActiveFace = false;
                internal_onEvent(PlayerAutoPauseEvents.LOST_ACTIVE_FACE);
            }
        }
    }
}
