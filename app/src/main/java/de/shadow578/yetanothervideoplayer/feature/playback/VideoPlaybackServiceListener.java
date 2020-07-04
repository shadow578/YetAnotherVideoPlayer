package de.shadow578.yetanothervideoplayer.feature.playback;


import androidx.annotation.NonNull;

import com.google.android.exoplayer2.metadata.Metadata;

/**
 * a object that listens for events by the Video Playback Service
 */
public interface VideoPlaybackServiceListener
{
    /**
     * the player of the service has been initialized and can now be used to, for example, set the render surface.
     * this is called in onBind() of the service as well as in the end of loadMedia() function
     */
    void onPlayerInitialized();

    /**
     * the media that was attempted to be loaded is in a location that cannot be accessed with the app's current permissions.
     * for local files, this means that {@link android.Manifest.permission#READ_EXTERNAL_STORAGE} is missing and should be requested.
     * for streamed files, {@link android.Manifest.permission#INTERNET} is missing and should be requested.
     * <p>
     * if this event is called, the media loading has been aborted and the video service is NOT ready for playback!
     * use this event to request the permissions, and call reloadMedia() once you have the needed permissions
     *
     * @param permissions the permission(s) that are missing for loading the media
     */
    void onMissingPermissions(@NonNull String[] permissions);

    /**
     * A error occurred in the video playback service!
     *
     * @param error the error that occurred (most likely a ExoPlayerException)
     */
    void onError(Exception error);

    /**
     * The current media was loaded and is ready for playback
     *
     * @param playWhenReady is the player set to play as soon as media is ready?
     */
    void onPlaybackReady(boolean playWhenReady);

    /**
     * The current media finished playback
     */
    void onPlaybackEnded();

    /**
     * the player state changed.
     * !! you dont really need to use this, this is more to keep possibilities open... !!
     *
     * @param playerState the new playback state of the player
     */
    void onPlayerStateChange(int playerState);

    /**
     * The buffering state of the player changed
     *
     * @param isBuffering is the player currently buffering?
     */
    void onBufferingChanged(boolean isBuffering);

    /**
     * The player received new metadata
     *
     * @param metadata the metadata received
     */
    void onNewMetadata(Metadata metadata);
}
