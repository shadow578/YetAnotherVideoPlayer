package de.shadow578.yetanothervideoplayer.util;

import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

public class UniversalMediaSourceFactory
{
    /**
     * Datasource factory of this app
     */
    private final DataSource.Factory dataSourceFactory;

    /**
     * The cache used to download video data
     */
    private Cache downloadCache;

    /**
     * Database provider for cached files
     */
    private ExoDatabaseProvider databaseProvider;

    /**
     * Initialize the Universal MediaSource Factory
     *
     * @param context   the context to create the factory in
     * @param userAgent the useragent to use when streaming media
     */
    public UniversalMediaSourceFactory(Context context, String userAgent)
    {
        //initialize cached files database provider
        if (databaseProvider == null)
        {
            databaseProvider = new ExoDatabaseProvider(context);
        }

        //initialize download cache
        if (downloadCache == null)
        {
            File downloadContentDir = new File(context.getFilesDir(), "stream-cache");
            downloadCache = new SimpleCache(downloadContentDir, new NoOpCacheEvictor(), databaseProvider);
        }

        //create http data source factory that allows http -> https redirects
        DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, null,
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true);

        //initialize data source factory
        DefaultDataSourceFactory ddsf = new DefaultDataSourceFactory(context, httpDataSourceFactory);
        dataSourceFactory = new CacheDataSourceFactory(downloadCache, ddsf, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }

    /**
     * Create a MediaSource from the given uri
     *
     * @param uri the uri to create the media source from
     * @return the media source created
     */
    public MediaSource createMediaSource(Uri uri)
    {
        return new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
    }

    /**
     * Release resources allocated by the UniversalMediaSourceFactory     *
     */
    public void release()
    {
        //release cache
        if (downloadCache != null)
        {
            downloadCache.release();
        }

        //close database connection
        if (databaseProvider != null)
        {
            databaseProvider.close();
        }
    }
}
