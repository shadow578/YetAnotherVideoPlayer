package de.shadow578.yetanothervideoplayer.feature.autopause;

/**
 * Events the PlayerAutoPauseManager might use when calling onPAPManagerEvent()
 */
public enum PlayerAutoPauseEvents
{
    /**
     * Proximity monitoring just started
     */
    PROXIMITY_MONITORING_STARTED,

    /**
     * Proximity monitoring has been stopped
     */
    PROXIMITY_MONITORING_STOPPED,

    /**
     * light intensity monitoring just started
     */
    LIGHT_MONITORING_STARTED,

    /**
     * light intensity monitoring has been stopped
     */
    LIGHT_MONITORING_STOPPED,

    /**
     * Face tracking was just started
     */
    FACE_TRACKING_STARTED,

    /**
     * Face tracking has been stopped
     */
    FACE_TRACKING_STOPPED,

    /**
     * Proximity monitoring has detected a near proximity (=pause the video)
     */
    NEAR_PROXIMITY,

    /**
     * Light intensity monitoring has detected a low light situation (=maybe pause the video (if proximity is NOT available?))
     * This is mainly used internally to stop face tracking in low light situations
     */
    LOW_LIGHT,

    /**
     * Face tracing found a active face
     */
    GOT_ACTIVE_FACE,

    /**
     * Face tracking lost a previously active face (=pause the video)
     */
    LOST_ACTIVE_FACE
}
