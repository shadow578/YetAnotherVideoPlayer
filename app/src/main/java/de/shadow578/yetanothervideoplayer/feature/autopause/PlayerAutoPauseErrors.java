package de.shadow578.yetanothervideoplayer.feature.autopause;

/**
 * Errors the PlayerAutoPauseManager might use when calling onPAPManagerError()
 */
public enum PlayerAutoPauseErrors
{
    /**
     * Error that is not defined
     */
    UNDEFINED,

    /**
     * Proximity sensing could not find a sensor
     */
    NO_PROXIMITY_SENSOR,

    /**
     * Light intensity sensing could not find a sensor
     * = when NO_PROXIMITY_SENSOR occurred too, then no proximity sensing (fallback sensor missing)
     * = no Face detection
     */
    NO_LIGHT_SENSOR,

    /**
     * Face detection detected low light situation and was disabled
     */
    LOW_LIGHT_DETECTED,

    /**
     * App is missing camera permissions needed for Face detection
     */
    NO_CAMERA_PERMISSION,

    /**
     * Device does not have a front facing camera, thus no face detection
     */
    NO_FRONT_CAMERA,

    /**
     * Device does not have google play services available for face detection
     */
    NO_GMS_SERVICES_AVAILABLE
}
