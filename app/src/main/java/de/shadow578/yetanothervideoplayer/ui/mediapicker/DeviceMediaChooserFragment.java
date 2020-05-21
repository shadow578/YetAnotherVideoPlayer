package de.shadow578.yetanothervideoplayer.ui.mediapicker;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Fragment that shows {@link de.shadow578.yetanothervideoplayer.ui.mediapicker.views.MediaCardView} for each media element on the device that matches the MediaKind given.
 * Make sure that your app has {@link Manifest.permission#READ_EXTERNAL_STORAGE} before you use this fragment
 */
public class DeviceMediaChooserFragment extends Fragment
{
    //region Variables
    /**
     * Type of media this chooser fragment shows
     */
    private final MediaEntry.MediaKind mediaKind;

    /**
     * a list of all media on this device that matches the {@link #mediaKind} of this fragment
     */
    private List<MediaEntry> mediaEntries = new ArrayList<>();

    //endregion

    /**
     * Create a new DeviceMediaChooserFragment that shows the given kind of media
     *
     * @param mediaKind the media type this chooser shows
     */
    DeviceMediaChooserFragment(MediaEntry.MediaKind mediaKind)
    {
        this.mediaKind = mediaKind;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Logging.logE("onCREATE: %s", mediaKind.toString());

        //initialize media for this fragment
        initializeMediaEntries();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        //inflate the fragment
        Logging.logD("onCreateView()");
        View rootView = inflater.inflate(R.layout.mediapicker_fragment_device_media_chooser, container, false);

        //setup adapter for recycler view
        Context ctx = getContext();
        if (ctx != null && mediaEntries != null && mediaEntries.size() > 0)
        {
            //create adapter
            RecyclerMediaEntryAdapter adapter = new RecyclerMediaEntryAdapter(ctx, mediaEntries);

            //setup recycler view for media previews
            RecyclerView mediaCardsView = rootView.findViewById(R.id.mediapicker_media_previews_list);
            mediaCardsView.setLayoutManager(new LinearLayoutManager(ctx));
            mediaCardsView.setAdapter(adapter);
        }

        //return the root view normally
        return rootView;
    }

    /**
     * Use the {@link MediaStore} to get all media matching the {@link #mediaKind} of this fragment
     */
    private void initializeMediaEntries()
    {
        //check we have a valid context to work in
        Context context = getContext();
        if (context == null) return;

        //get content resolver
        ContentResolver contentResolver = getContext().getContentResolver();
        if (contentResolver == null) return;

        //clear old media
        mediaEntries.clear();

        //TODO: DATA is depreciated?
        //query videos
        if (mediaKind == MediaEntry.MediaKind.VIDEO)
        {
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED};
            try (Cursor videoCursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null, null,
                    MediaStore.MediaColumns.DATE_MODIFIED + " ASC"))
            {
                //check cursor not null
                if (videoCursor != null && videoCursor.getCount() > 0)
                {
                    //add media to list
                    addMediaToList(videoCursor, mediaEntries, MediaEntry.MediaKind.VIDEO);
                }
                else
                {
                    //oh no! there is no videos on this device!
                    Logging.logE("NO VIDEOS!");
                }
            }
        }

        //query music
        if (mediaKind == MediaEntry.MediaKind.MUSIC)
        {
            String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED};
            try (Cursor audioCursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null, null,
                    MediaStore.MediaColumns.DATE_MODIFIED + " ASC"))
            {
                //check cursor not null
                if (audioCursor != null && audioCursor.getCount() > 0)
                {
                    //add media to list
                    addMediaToList(audioCursor, mediaEntries, MediaEntry.MediaKind.MUSIC);
                }
                else
                {
                    //uh- oh! we have no audio on this device!
                    Logging.logE("NO MUSIC!");
                }
            }
        }

        //TODO: add media multiple times to make list longer
        Random rnd = new Random();
        while (mediaEntries.size() < 20)
        {
            mediaEntries.add(mediaEntries.get(rnd.nextInt(mediaEntries.size())));
        }

        Logging.logD("initializeMediaEntries() found %d media entries", mediaEntries.size());
    }

    /**
     * Add all media of the cursor to the list of MediaEntries
     * Expects the following projection to be used: DATA, DISPLAY_NAME
     * Other keys may be used for sorting, etc...
     *
     * @param cursor    the cursor, as you get it from {@link ContentResolver#query(Uri, String[], Bundle, CancellationSignal)}
     * @param mediaList the media list to add media entries to
     * @param mediaKind what kind of media is this?
     */
    private void addMediaToList(@NonNull Cursor cursor, @NonNull List<MediaEntry> mediaList, @NonNull MediaEntry.MediaKind mediaKind)
    {
        //move to the first position
        cursor.moveToFirst();

        //add all media to the list of MediaEntries
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        do
        {
            //get basic info (data(=uri), title (=display_name)
            String uriStr = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));

            //parse uri
            Uri uri = Uri.parse(uriStr);

            //get extra metadata for media
            metadataRetriever.setDataSource(getContext(), uri);
            int duration = Integer.parseInt(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000;//ms to s
            Size vidSize = null;
            if (mediaKind == MediaEntry.MediaKind.VIDEO)
            {
                //for resolution, we just take some random frame of the video and get it's resolution
                Bitmap randomFrame = metadataRetriever.getFrameAtTime();
                vidSize = new Size(randomFrame.getWidth(), randomFrame.getHeight());
            }

            //create and add media entry
            MediaEntry entry = new MediaEntry(mediaKind, uri, title, duration, vidSize);
            mediaList.add(entry);
            Logging.logD("add MediaEntry %s; list size: %d", entry.toString(), mediaList.size());
        }
        while (cursor.moveToNext());
    }
}
