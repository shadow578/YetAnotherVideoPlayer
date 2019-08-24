package de.shadow578.yetanothervideoplayer.ui;

import androidx.appcompat.app.AppCompatActivity;

import de.shadow578.yetanothervideoplayer.R;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

public class PlaybackActivity extends AppCompatActivity
{
    //~~ Constants, change to shared prefs OR remove later ~~
    //playback uri, for testing the player
    final String PLAYBACK_URI = "https://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4";

    //auto start player when loading?
    final boolean AUTO_PLAY = true;

    //the apps user agent
    final String USER_AGENT = "exoplayer-yavp";
    //~~ end~~

    /**
     * The View the Player Renders Video to
     */
    PlayerView playerView;

    /**
     * The exoplayer instance
     */
    SimpleExoPlayer player;

    /**
     * Datasource factory of this app
     */
    DataSource.Factory dataSourceFactory;

    /**
     * The cache used to download video data
     */
    Cache downloadCache;

    /**
     * Database provider for cached files
     */
    ExoDatabaseProvider databaseProvider;

    /**
     * The current playback position, used for resuming playback
     */
    long playbackPosition;

    /**
     * The index of the currently played window, used for resuming playback
     */
    int currentPlayWindow;

    /**
     * Should the player start playing once ready when resuming playback?
     */
    boolean playWhenReady;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        //initialize cached files database provider
        if (databaseProvider == null)
        {
            databaseProvider = new ExoDatabaseProvider(this);
        }

        //initialize download cache
        if (downloadCache == null)
        {
            File downloadContentDir = new File(getFilesDir(), "downloads");
            downloadCache = new SimpleCache(downloadContentDir, new NoOpCacheEvictor(), databaseProvider);
        }

        //initialize datasource factory
        DefaultDataSourceFactory ddsf = new DefaultDataSourceFactory(this, new DefaultHttpDataSourceFactory(USER_AGENT));
        dataSourceFactory = new CacheDataSourceFactory(downloadCache, ddsf, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        //get player view
        playerView = findViewById(R.id.pb_PlayerView);


    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (supportMultiWindow())
        {
            initPlayer();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (supportMultiWindow())
        {
            freePlayer();
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (!supportMultiWindow())
        {
            freePlayer();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        hideSysUI();
        if (!supportMultiWindow() || player == null)
        {
            initPlayer();
        }
    }

    /**
     * Starting with API 24, android supports multiple windows
     *
     * @return does this device support multi- window?
     */
    boolean supportMultiWindow()
    {
        return Util.SDK_INT > 23;
    }

    /**
     * Hide System- UI elements
     */
    void hideSysUI()
    {
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    /**
     * Initializes the ExoPlayer to render to the playerView
     */
    void initPlayer()
    {
        //create new simple exoplayer instance
        player = ExoPlayerFactory.newSimpleInstance(this, new DefaultRenderersFactory(this), new DefaultTrackSelector(), new DefaultLoadControl());

        //set the view to render to
        playerView.setPlayer(player);

        //needed for playback resumption
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentPlayWindow, playbackPosition);

        //load media from uri
        Uri playbackUri = Uri.parse(PLAYBACK_URI);
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(playbackUri);
        player.prepare(mediaSource, true, false);
    }

    /**
     * Free resources allocated by the player
     */
    void freePlayer()
    {
        if (player != null)
        {
            //save player state
            playbackPosition = player.getCurrentPosition();
            currentPlayWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();

            //release + null player
            player.release();
            player = null;
        }
    }
}
