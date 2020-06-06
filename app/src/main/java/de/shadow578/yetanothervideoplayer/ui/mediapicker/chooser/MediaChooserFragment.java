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
import android.provider.MediaStore;
import android.util.Size;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    //region Media Scanning

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

        //populate list
        populateMediaList(context, contentResolver, mediaKind);

        //finish up
        Logging.logD("initializeMediaEntries() found %d media entries for kind %s", mediaEntries.size(), mediaKind.toString());
    }

    /**
     * populates the media list by using scanMedia to scan for media of the given kind in multiple locations
     *
     * @param ctx      the context to scan in
     * @param resolver content resolver used to query media
     * @param kind     the kind of media we're scanning for
     */
    private void populateMediaList(@NonNull Context ctx, @NonNull ContentResolver resolver, @NonNull MediaEntry.MediaKind kind)
    {
        //clear old media first
        mediaEntries.clear();

        //scan in INTERNAL_STORAGE, EXTERNAL_STORAGE and phoneStorage (HTC seems to need this)
        //https://stackoverflow.com/questions/4972968/empty-cursor-from-the-mediastore
        switch (kind)
        {
            case MUSIC:
                scanMedia(ctx, resolver, new Uri[]{
                        /*MediaStore.Audio.Media.INTERNAL_CONTENT_URI, -> this is commented since it also finds ringtones*/
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Audio.Media.getContentUri("phoneStorage")}, kind);
                break;
            case VIDEO:
                scanMedia(ctx, resolver, new Uri[]{
                        /*MediaStore.Video.Media.INTERNAL_CONTENT_URI,*/
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Video.Media.getContentUri("phoneStorage")}, kind);
                break;
        }
    }

    /**
     * Scans the given MediaStore uris for media of the given media kind
     *
     * @param ctx      the context to scan in
     * @param resolver content resolver to use to query media
     * @param scanUris the uris to scan for media (eg. {@link MediaStore.Video.Media#EXTERNAL_CONTENT_URI})
     * @param kind     the kind of media we're scanning for (is set in the generated media entries)
     */
    private void scanMedia(@NonNull Context ctx, @NonNull ContentResolver resolver, @NonNull Uri[] scanUris, @NonNull MediaEntry.MediaKind kind)
    {
        //prepare query options
        //TODO: DATA deprecated?
        String[] projection = {MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED};
        if (kind == MediaEntry.MediaKind.VIDEO)
        {
            projection = new String[]{MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.Video.VideoColumns.RESOLUTION};
        }
        String sort = MediaStore.MediaColumns.DATE_MODIFIED + " DESC";

        //prepare metadata resolver
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

        //scan every uri
        for (Uri scanUri : scanUris)
        {
            Logging.logD("Scanning uri %s for media of kind %s...", scanUri.toString(), kind.toString());
            try (Cursor c = resolver.query(scanUri, projection, null, null, sort))
            {
                //check cursor is ok to use
                if (c == null || c.getCount() <= 0) continue;

                //get indices of data
                int iDATA = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                int iDISPLAY_NAME = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
                int iRESOLUTION = -1;
                if (kind == MediaEntry.MediaKind.VIDEO)
                    iRESOLUTION = c.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.RESOLUTION);

                //move cursor to first position, skip if that fails
                if (!c.moveToFirst()) continue;

                //process cursor positions
                Uri mediaUri;
                String title;
                int duration;
                Size vSize = null;
                do
                {
                    //get fields from position
                    mediaUri = Uri.parse(c.getString(iDATA));
                    title = c.getString(iDISPLAY_NAME);
                    if (kind == MediaEntry.MediaKind.VIDEO)
                        vSize = parseSize(c.getString(iRESOLUTION));

                    //skip if media uri is not valid
                    if (!isMediaUriValid(mediaUri))
                    {
                        Logging.logE("skipping invalid medai uri %s", mediaUri == null ? "NULL" : mediaUri.toString());
                        continue;
                    }

                    //try and get extra data for media
                    duration = 0;
                    try
                    {
                        //set datasource
                        metadataRetriever.setDataSource(ctx, mediaUri);

                        //get raw metadata
                        String titleStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String durationStr = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                        //use title from metadata if it's valid
                        if (titleStr != null && !titleStr.isEmpty())
                            title = titleStr;

                        //use duration from metadata if it's valid
                        //metadata duration is in ms, but we need seconds, so divide by 1000
                        if (durationStr != null && !durationStr.isEmpty())
                            duration = Integer.parseInt(durationStr) / 1000;
                    }
                    catch (IllegalArgumentException metadataEx)
                    {
                        Logging.logE("Error getting metadata for media %s!", mediaUri.toString());
                        metadataEx.fillInStackTrace();
                    }

                    //create and add media entry
                    MediaEntry entry = new MediaEntry(kind, mediaUri, title, duration, vSize);
                    mediaEntries.add(entry);
                    Logging.logD("add entry %s to media list. new size is %d", entry.toString(), mediaEntries.size());
                }
                while (c.moveToNext());
            }
            catch (IllegalArgumentException scanEx)
            {
                Logging.logE("scanning uri %s failed with exception", scanUri.toString());
                scanEx.printStackTrace();
            }
        }
    }

    /**
     * Check if a uri to a media file is valid for use with eg. the media metadata resolver
     *
     * @param mediaUri the uri to check
     * @return is the uri valid for use?
     */
    private boolean isMediaUriValid(@Nullable Uri mediaUri)
    {
        //check uri is not null and path is not empty
        if (mediaUri == null || mediaUri.getPath() == null || mediaUri.getPath().isEmpty())
            return false;

        //check file exists
        File mediaFile = new File(mediaUri.getPath());
        return mediaFile.exists();
    }

    /**
     * parses a size from a string in format WxH
     *
     * @param sizeStr the size string (eg. 1920x1080)
     * @return the size parsed
     */
    @Nullable
    private Size parseSize(@NonNull String sizeStr)
    {
        //compile regex for size parsing
        //matches WxH, W in cg1, H in cg2
        final Pattern sizeRegex = Pattern.compile("^(\\d+)[x×](\\d+)$");

        //match size string against pattern
        Matcher matcher = sizeRegex.matcher(sizeStr);

        //is right format?
        if (!matcher.find()) return null;

        //get match results. do we have two capture groups?
        MatchResult result = matcher.toMatchResult();
        if (result.groupCount() != 2) return null;

        //get width and height from cg1 and cg2
        String widthStr = result.group(1);
        String heightStr = result.group(2);

        //try to parse width and height to int
        int width;
        int height;
        try
        {
            width = Integer.parseInt(widthStr);
            height = Integer.parseInt(heightStr);
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }

        //all ok, return the size
        return new Size(width, height);
    }

    //endregion

    //region Permissions Util

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
    //endregion
}
