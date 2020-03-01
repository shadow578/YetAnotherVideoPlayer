package de.shadow578.yetanothervideoplayer.playback;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;

import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Service that plays a video file or stream using ExoPlayer
 */
public class VideoPlaybackService extends Service
{
    /**
     * Allows a Activity (or whatever) to get information about a VideoPlaybackService instance
     */
    public class VideoServiceBinder extends Binder
    {
        /**
         * get the instance of the video playback service
         *
         * @return the video playback service's instance
         */
        public VideoPlaybackService getServiceInstance()
        {
            return VideoPlaybackService.this;
        }
    }

    /**
     * Called when the service is started and bound to a activity
     *
     * @param intent service "start" intent -> this contains nothing
     * @return the binder for the activity so it can get information about the service instance
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        Logging.logD("VideoPlaybackService.onBind()");

        //create the media factory
        mediaFactory = new UniversalMediaSourceFactory(this, Util.getUserAgent(this, getPackageName()));

        //initialize the player
        initializePlayer();

        //create a new binder
        return new VideoServiceBinder();
    }

    /**
     * Called when the service is stopped
     */
    @Override
    public void onDestroy()
    {
        Logging.logD("VideoPlaybackService.onDestroy()");

        //release all
        releasePlayerAndMedia();

        //do normal stuff
        super.onDestroy();
    }


    /**
     * The ExoPlayer instance that is decoding and playing the video
     */
    private SimpleExoPlayer player;

    /**
     * Media source factory for multiple formats
     */
    private UniversalMediaSourceFactory mediaFactory;

    /**
     * the listener that listens for playback service events
     */
    private VideoPlaybackServiceListener eventListener;

    /**
     * the uri the currently loaded media is loaded from
     */
    private Uri currentMediaUri = null;

    /**
     * has the player been initialized yet?
     */
    private boolean isPlayerInitialized = false;

    /**
     * playWhenReady parameter that was used the last time loadMedia() was called
     * used for reloadMedia() function EXCLUSIVELY
     */
    private boolean lastLoadPlayWhenReady = false;

    /**
     * startPosition parameter that was used the last time loadMedia() was called
     * used for reloadMedia() function EXCLUSIVELY
     */
    private long lastLoadStartPosition = 0;

    //region Service Interface

    /**
     * loads the media from the uri and prepares the player for playback
     * The uri can be either a web url or a local file url
     *
     * @param mediaUri the uri of the media to load
     */
    @SuppressWarnings("unused")
    public void loadMedia(@NonNull Uri mediaUri)
    {
        loadMedia(mediaUri, false, 0);
    }

    /**
     * loads the media from the uri and prepares the player for playback
     * The uri can be either a web url or a local file url
     *
     * @param mediaUri      the uri of the media to load
     * @param playWhenReady should playback start as soon as media is ready?
     */
    @SuppressWarnings("unused")
    public void loadMedia(@NonNull Uri mediaUri, boolean playWhenReady)
    {
        loadMedia(mediaUri, playWhenReady, 0);
    }

    /**
     * loads the media from the uri and prepares the player for playback
     * The uri can be either a web url or a local file url
     *
     * @param mediaUri      the uri of the media to load
     * @param playWhenReady should playback start as soon as media is ready?
     * @param startPosition the position that playback starts at
     */
    public void loadMedia(@NonNull Uri mediaUri, boolean playWhenReady, long startPosition)
    {
        //set the uri of the current media
        currentMediaUri = mediaUri;

        //set last parameter values for usage in reloadMedia()
        lastLoadPlayWhenReady = playWhenReady;
        lastLoadStartPosition = startPosition;

        //check if we need permissions to access the uri (=local file) but dont have them
        if (isLocalFile(mediaUri) && !hasStoragePermission())
        {
            //missing permissions
            if (eventListener != null)
                eventListener.onMissingStoragePermissions();

            //abort load
            return;
        }

        //check player is valid
        if (!isPlayerValid()) return;

        //load media from the uri
        player.prepare(mediaFactory.createMediaSource(mediaUri));

        //seek to start position
        seekTo(startPosition);

        //set play when ready
        setPlayWhenReady(playWhenReady);

        //tell event handler that player is valid now
        if (isEventListenerValid() && isPlayerInitialized)
            eventListener.onPlayerInitialized();
    }

    /**
     * Repeats the last call to loadMedia() (including playWhenReady and startPosition)
     */
    public void reloadMedia()
    {
        if (currentMediaUri == null)
            throw new IllegalStateException("reloadMedia(): currentMediaUri is null! did you try to reload before loading?");

        loadMedia(currentMediaUri, lastLoadPlayWhenReady, lastLoadStartPosition);
    }

    /**
     * @return the uri the currently loaded media is loaded from
     */
    @SuppressWarnings("unused")
    public Uri getCurrentMediaUri()
    {
        return currentMediaUri;
    }

    /**
     * set the listener for playback events
     *
     * @param listener the listener to set
     */
    public void setListener(VideoPlaybackServiceListener listener)
    {
        eventListener = listener;
    }

    /**
     * get the internally used player instance
     * !! Use this ONLY for more special stuff, as the player might not have any media loaded !!
     * !! check getIsPlayerValid() first !!
     * !! eg. to set the video surface to render to... !!
     *
     * @return the internal player instance (this might be null)
     */
    public @Nullable
    SimpleExoPlayer getPlayerInstance()
    {
        return player;
    }

    /**
     * @return is the internal player instance valid for use?
     */
    public boolean getIsPlayerValid()
    {
        return isPlayerValid();
    }

    //region Playback Controls

    /**
     * set if the player is set to play as soon as media is ready for playback
     *
     * @param playing should playback begin asap as media is ready?
     */
    public void setPlayWhenReady(boolean playing)
    {
        if (isPlayerValid())
            player.setPlayWhenReady(playing);
    }

    /**
     * @return is the player set up to play as soon as media is ready for playback?
     */
    public boolean getPlayWhenReady()
    {
        return isPlayerValid() && player.getPlayWhenReady();
    }

    /**
     * @return is the player currently playing back video? (actual state)
     */
    public boolean getIsPlaying()
    {
        return getPlayWhenReady() && player.getPlaybackState() == Player.STATE_READY;
    }

    /**
     * @return the total duration of the currently loaded media, in milliseconds
     */
    public long getMediaDuration()
    {
        return isPlayerValid() ? player.getDuration() : -1;
    }

    /**
     * @return the current position of the player in the currently loaded media
     */
    public long getPlaybackPosition()
    {
        return isPlayerValid() ? player.getContentPosition() : -1;
    }

    /**
     * Seek the current media relative to the current position
     *
     * @param posOffset offset to the current playback position to seek to
     */
    public void seekRelative(long posOffset)
    {
        seekTo(getPlaybackPosition() + posOffset);
    }

    /**
     * Seek the current media to the given position
     *
     * @param pos the position to seek to
     */
    public void seekTo(long pos)
    {
        //check player is valid before doing anything
        if (!isPlayerValid()) return;

        //keep pos inside player bounds
        if (pos < 0) pos = 0;
        if (pos > getMediaDuration()) pos = getMediaDuration();

        //seek the player
        player.seekTo(pos);
    }

    /**
     * set if the player should loop the current media
     *
     * @param looping should the player loop the current media?
     */
    public void setLooping(boolean looping)
    {
        if (isPlayerValid())
            player.setRepeatMode(looping ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
    }

    /**
     * @return is the player set to loop the current media?
     */
    public boolean getLooping()
    {
        return isPlayerValid() && player.getRepeatMode() == Player.REPEAT_MODE_ALL;
    }

    //endregion
    //endregion

    /**
     * Initializes the ExoPlayer and registers listeners
     */
    private void initializePlayer()
    {
        //prepare track selector and stuff for the player
        TrackSelector trackSelector = new DefaultTrackSelector(new AdaptiveTrackSelection.Factory());
        RenderersFactory renderersFactory = new DefaultRenderersFactory(this);
        LoadControl loadControl = new DefaultLoadControl();
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(this).build();

        //build the player instance
        player = ExoPlayerFactory.newSimpleInstance(this, renderersFactory, trackSelector, loadControl, null, bandwidthMeter);

        //register listeners
        player.addListener(new PlayerEventListener());
        player.addMetadataOutput(new PlayerMetadataListener());

        //set init flag
        isPlayerInitialized = true;

        //call event handler IF already ready (probably not...)
        if (isEventListenerValid())
            eventListener.onPlayerInitialized();
    }

    /**
     * releases the current exoplayer instance, media source and media factory
     */
    private void releasePlayerAndMedia()
    {
        Logging.logD("releasing player and media...");

        //release player
        if (player != null)
        {
            player.stop();
            player.release();
            player = null;
            isPlayerInitialized = false;
        }

        //release media factory
        if (mediaFactory != null)
        {
            mediaFactory.release();
            mediaFactory = null;
        }
    }

    //region Player listeners

    /**
     * listens for state events of the player
     */
    private class PlayerEventListener implements Player.EventListener
    {
        /**
         * The last playback state reported by onPlayerStateChanged()
         */
        private int lastPlaybackState = -1;

        /**
         * The players loading state changed
         * !! Loading State is the whole time the exoplayer is buffering media, EVEN if the media is already playing !!
         *
         * @param isLoading is the player loading?
         */
        @Override
        public void onLoadingChanged(boolean isLoading)
        {
        }

        /**
         * the players playback state changed
         *
         * @param playWhenReady is the player set to play as soon as the media is ready?
         * @param playbackState the new playback state of the player
         */
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState)
        {
            //check if player finished buffering (WAS buffering, IS no longer)
            if (lastPlaybackState == Player.STATE_BUFFERING && playbackState != Player.STATE_BUFFERING)
            {
                //finished buffering, call event
                if (isEventListenerValid())
                    eventListener.onBufferingChanged(false);
            }

            //check if player started buffering (WAS not buffering, IS now)
            if (lastPlaybackState != Player.STATE_BUFFERING && playbackState == Player.STATE_BUFFERING)
            {
                //started buffering, call event
                if (isEventListenerValid())
                    eventListener.onBufferingChanged(true);
            }

            //act according to state
            switch (playbackState)
            {
                case Player.STATE_IDLE:
                    //consider IDLE state the same as ENDED
                case Player.STATE_ENDED:
                {
                    //player is not playing and not able to
                    if (isEventListenerValid())
                        eventListener.onPlaybackEnded();
                    break;
                }
                case Player.STATE_READY:
                {
                    //player is playing or ready to play
                    if (isEventListenerValid())
                        eventListener.onPlaybackReady(playWhenReady);
                    break;
                }
                case Player.STATE_BUFFERING:
                {
                    //Player is buffering
                    //not used rn
                    break;
                }
                default:
                {
                    Logging.logD("Received invalid playback state: %d", playbackState);
                    break;
                }
            }

            //forward state change raw to event listener
            if (isEventListenerValid())
                eventListener.onPlayerStateChange(playbackState);

            //set lastState var
            lastPlaybackState = playbackState;
        }

        /**
         * a error occurred in the player
         *
         * @param error the player that occurred
         */
        @Override
        public void onPlayerError(ExoPlaybackException error)
        {
            //the player threw a error
            Logging.logE("onPlayerError(): %s", error.toString());

            //forward to event listener
            if (isEventListenerValid())
                eventListener.onError(error);
        }
    }

    /**
     * Listens for Metadata events of the player
     */
    private class PlayerMetadataListener implements MetadataOutput
    {
        /**
         * The player received new metadata information
         *
         * @param metadata the metadata received.
         */
        @Override
        public void onMetadata(Metadata metadata)
        {
            //don't really care
            if (isEventListenerValid())
                eventListener.onNewMetadata(metadata);
        }

    }
    //endregion

    //region Shared Util

    /**
     * @return is the player object valid?
     */
    private boolean isPlayerValid()
    {
        return player != null && isPlayerInitialized;
    }

    /**
     * @return is the eventListener object valid?
     */
    private boolean isEventListenerValid()
    {
        return eventListener != null;
    }

    /**
     * Check if the uri is a local file
     *
     * @param uri the uri to check
     * @return is the uri a local file?
     */
    private boolean isLocalFile(Uri uri)
    {
        return uri.getScheme() != null && (uri.getScheme().equals("content") || uri.getScheme().equals("file"));
    }

    /**
     * @return does the app have permissions to read external storage?
     */
    private boolean hasStoragePermission()
    {
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }
    //endregion
}
