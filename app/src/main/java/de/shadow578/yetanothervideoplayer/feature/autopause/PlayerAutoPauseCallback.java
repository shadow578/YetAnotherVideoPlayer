package de.shadow578.yetanothervideoplayer.feature.autopause;

/**
 * Callbacks for the PlayerAutoPauseManager
 */
public interface PlayerAutoPauseCallback
{
    /**
     * Called when a event occurs
     *
     * @param event the event that occurred
     */
    void onPAPManagerEvent(PlayerAutoPauseEvents event);

    /**
     * Called when a error occurs
     *
     * @param error the error that occurred
     */
    void onPAPManagerError(PlayerAutoPauseErrors error);
}
