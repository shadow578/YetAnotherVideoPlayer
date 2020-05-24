package de.shadow578.yetanothervideoplayer.ui.mediapicker.chooser;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.ui.LaunchActivity;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Fragment that shows {@link de.shadow578.yetanothervideoplayer.ui.mediapicker.views.MediaCardView} for each media element on the device that matches the MediaKind given.
 * You should get {@link Manifest.permission#READ_EXTERNAL_STORAGE} before you use this fragment. If not, a "request permissions" button will be shown.
 */
public class MediaChooserFragment extends Fragment implements RecyclerMediaEntryAdapter.CardClickListener
{
    /**
     * ID for permission request
     * Request {@link Manifest.permission#READ_EXTERNAL_STORAGE} permissions and reload media in the fragment if the permissions were granted.
     */
    private final int ID_REQUEST_EXT_STORAGE_PERMISSIONS_AND_RELOAD_MEDIA = 0;

    //region Variables
    //region Views
    /**
     * Recycler view that displays media entries
     */
    private RecyclerView mediaCardsRecycler;

    /**
     * container view for no media info ui elements
     */
    private View noMediaInfo;

    /**
     * container view for no permissions info ui elements
     */
    private View noPermissionsInfo;

    /**
     * button in the no permissions container that is used to request storage permissions
     */
    @SuppressWarnings("FieldCanBeLocal")
    private Button requestPermissionsButton;

    //endregion

    /**
     * Type of media this chooser fragment shows
     */
    private MediaEntry.MediaKind mediaKind = MediaEntry.MediaKind.VIDEO;

    /**
     * a list of all media on this device that matches the {@link #mediaKind} of this fragment
     */
    private List<MediaEntry> mediaEntries = new ArrayList<>();
    //endregion

    /**
     * Create a default config fragment with mediaKind = VIDEO
     * <p>
     * This may cause a bug where a music fragment is converted to video IF the fragment is recreated using this constructor.
     * However, this will probably not happen that often ;)
     * Solution, if needed, could be to use savedInstanceState
     */
    public MediaChooserFragment()
    {
        super();
    }

    /**
     * Create a new MediaChooserFragment that shows the given kind of media
     *
     * @param mediaKind the media type this chooser shows
     */
    public MediaChooserFragment(MediaEntry.MediaKind mediaKind)
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
        final View rootView = inflater.inflate(R.layout.mediapicker_fragment_media_chooser, container, false);

        //find views
        mediaCardsRecycler = rootView.findViewById(R.id.mediapicker_media_previews_list);
        noMediaInfo = rootView.findViewById(R.id.mediapicker_no_media_container);
        noPermissionsInfo = rootView.findViewById(R.id.mediapicker_no_permissions_container);
        requestPermissionsButton = rootView.findViewById(R.id.mediapicker_request_permissions_btn);

        //make all views are ok
        Context ctx = getContext();
        if (ctx == null || mediaCardsRecycler == null || noMediaInfo == null || noPermissionsInfo == null || requestPermissionsButton == null)
            return rootView;

        //set click listener on "request permission" button
        requestPermissionsButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                requestStoragePermissions();
            }
        });

        //set visibilities according to state
        initAndUpdateUI();

        //return the root view normally
        return rootView;
    }

    /**
     * initializes and updates the UI according to state.
     */
    private void initAndUpdateUI()
    {
        //get context
        Context ctx = getContext();

        //make sure all views are ok
        if (ctx == null || mediaCardsRecycler == null)
        {
            Logging.logE("Context or mediaCardsRecylcer is null!");
            return;
        }

        //skip if no storage permissions
        if (storagePermissionsMissing())
        {
            Logging.logD("No Storage permissions, show noStoragePermissions info");
            mediaCardsRecycler.setVisibility(View.GONE);
            noMediaInfo.setVisibility(View.GONE);
            noPermissionsInfo.setVisibility(View.VISIBLE);
            return;
        }

        //skip if media list is empty
        if (mediaEntries == null || mediaEntries.size() <= 0)
        {
            Logging.logD("No media entries, show noMediaEntries info");
            mediaCardsRecycler.setVisibility(View.GONE);
            noMediaInfo.setVisibility(View.VISIBLE);
            noPermissionsInfo.setVisibility(View.GONE);
            return;
        }

        //make recycler visible
        mediaCardsRecycler.setVisibility(View.VISIBLE);
        noMediaInfo.setVisibility(View.GONE);
        noPermissionsInfo.setVisibility(View.GONE);

        //create adapter
        RecyclerMediaEntryAdapter adapter = new RecyclerMediaEntryAdapter(ctx, mediaEntries, this);

        //set default thumbnail according to media type
        switch (mediaKind)
        {
            case VIDEO:
                adapter.setPlaceholderThumbnail(ctx.getDrawable(R.drawable.ic_terrain_black_24dp));
                break;
            case MUSIC:
                adapter.setPlaceholderThumbnail(ctx.getDrawable(R.drawable.ic_music_note_black_24dp));
                break;
        }

        //setup recycler view for media previews
        mediaCardsRecycler.setLayoutManager(new LinearLayoutManager(ctx));
        mediaCardsRecycler.setAdapter(adapter);
    }

    /**
     * Use the {@link MediaStore} to get all media matching the {@link #mediaKind} of this fragment
     */
    private void initializeMediaEntries()
    {
        //check we have storage permissions
        if (storagePermissionsMissing()) return;

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
            }
        }

        //finish up
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
            String dispName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));

            //parse uri
            Uri uri = Uri.parse(uriStr);

            //skip if invalid uri
            if (uri == null) continue;

            //get extra metadata for media
            metadataRetriever.setDataSource(getContext(), uri);
            String durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            int duration = -1;
            if (durationStr != null && !durationStr.isEmpty())
            {
                duration = Integer.parseInt(durationStr) / 1000;//ms to s
            }

            //skip if duration not available
            if (duration <= 0) continue;

            //extract title from metadata
            String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title == null || title.isEmpty())
            {
                title = dispName;
            }

            //TODO: get resolution in a different way, this is way too slow
            //Size vidSize = null;
            //if (mediaKind == MediaEntry.MediaKind.VIDEO)
            //{
            //    //for resolution, we just take some random frame of the video and get it's resolution
            //    Bitmap randomFrame = metadataRetriever.getFrameAtTime();
            //    if (randomFrame != null)
            //        vidSize = new Size(randomFrame.getWidth(), randomFrame.getHeight());
            //}

            //create and add media entry
            MediaEntry entry = new MediaEntry(mediaKind, uri, title, duration, null);
            mediaList.add(entry);
            Logging.logD("add MediaEntry %s; list size: %d", entry.toString(), mediaList.size());
        }
        while (cursor.moveToNext());
    }

    /**
     * Called when the media card was clicked
     *
     * @param cardMedia the media entry of the card
     */
    @Override
    public void onMediaCardClicked(MediaEntry cardMedia)
    {
        Logging.logE("Card clicked: %s", cardMedia.toString());

        //launch player activity
        Intent playbackIntent = new Intent(getContext(), LaunchActivity.class);
        playbackIntent.setAction(Intent.ACTION_VIEW);
        playbackIntent.setData(cardMedia.getUri());
        playbackIntent.putExtra(Intent.EXTRA_TITLE, cardMedia.getTitle());
        startActivity(playbackIntent);
    }

    /**
     * @return do we have READ_EXTERNAL_STORAGE permissions granted?
     */
    private boolean storagePermissionsMissing()
    {
        Context ctx = getContext();
        if (ctx == null) return true;
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request {@link Manifest.permission#READ_EXTERNAL_STORAGE} and reload media list if we got the permissions
     */
    private void requestStoragePermissions()
    {
        //we already have permissions, don't do anything
        if (!storagePermissionsMissing()) return;

        //request permissions
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ID_REQUEST_EXT_STORAGE_PERMISSIONS_AND_RELOAD_MEDIA);
    }

    /**
     * Callback for when requested permissions were granted.
     * Used for ID_REQUEST_EXT_STORAGE_PERMISSIONS_AND_RELOAD_MEDIA
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == ID_REQUEST_EXT_STORAGE_PERMISSIONS_AND_RELOAD_MEDIA && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            //have permissions now, reload media
            //first initialize media entries list
            initializeMediaEntries();

            //then update ui
            initAndUpdateUI();
        }
    }
}
