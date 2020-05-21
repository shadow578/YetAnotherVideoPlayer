package de.shadow578.yetanothervideoplayer.ui.mediapicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.ui.LaunchActivity;
import de.shadow578.yetanothervideoplayer.ui.mediapicker.views.MediaPreviewView;
import de.shadow578.yetanothervideoplayer.util.Logging;

import static android.provider.MediaStore.Video.Media.*;

@Deprecated
public class MediaPickerActivity_OLD extends AppCompatActivity
{
    /**
     * The height of a media preview, in dp
     */
    private final float PREVIEW_HEIGHT_DP = 100;

    /**
     * Container View that the media previews are added to
     */
    private ViewGroup mediaPreviewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //do normal stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mediapicker_activity);

        //get views
        //mediaPreviewContainer = findViewById(R.id.mp_preview_container);


        //request media permissions
        if (!checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 0))
            return;

        //populate preview container with previews for all media
        populateMediaPreview();
    }

    /**
     * Populates the mediaPreviewContainer with a preview for each video on the device
     */
    private void populateMediaPreview()
    {
        Logging.logE("Start pop");

        //get the content resolver for this activity
        ContentResolver resolver = getContentResolver();
        if (resolver == null)
        {
            Logging.logE("getContentResolver() returned null! abort population the media browser!");
            return;
        }

        //get cursor for all videos on the device
        try (Cursor mediaCursor = resolver.query(EXTERNAL_CONTENT_URI, new String[]
                {
                        DATA,
                        DISPLAY_NAME,
                        _ID,
                }, null, null, null))
        {
            //check the cursor is valid
            if (mediaCursor == null)
            {
                Logging.logE("mediaCursor was null! abort populating the media browser!");
                return;
            }

            //move cursor to the first entry
            mediaCursor.moveToFirst();

            //TODO: don't add all entries at once, add them as we scroll down (reduce loading times)
            //add all entries
            do
            {
                //get and check media title
                final String mediaTitle = mediaCursor.getString(mediaCursor.getColumnIndex(DISPLAY_NAME));

                //get media uri
                final String mediaUriStr = mediaCursor.getString(mediaCursor.getColumnIndex(DATA));
                if (mediaUriStr == null) continue;

                final Uri mediaUri = Uri.parse(mediaUriStr);

                // get thumbnail
                final long mediaId = mediaCursor.getLong(mediaCursor.getColumnIndex(_ID));
                final Bitmap mediaThumbnail = MediaStore.Video.Thumbnails.getThumbnail(resolver, mediaId, MediaStore.Video.Thumbnails.MINI_KIND, null);

                //check that both title and uri are valid
                if (mediaTitle == null || mediaUri == null) continue;

                //create the preview view and set title
                MediaPreviewView preview = new MediaPreviewView(this)
                        .setTitle(mediaTitle);

                //set thumbnail
                if (mediaThumbnail != null)
                {
                    //normally add thumbnail to preview
                    preview.setThumbnail(mediaThumbnail);
                }
                else
                {
                    //try to load thumbnail from the media uri
                    //if that fails, skip this element
                    if (!preview.setThumbnailFromMediaUri(mediaUri))
                    {
                        Logging.logW("Failed to add Thumbnail for media element using media uri! skipping element...");
                        continue;
                    }
                }

                //set the onClick listener for the preview to start playing
                //preview.setClickListener(new MediaPreviewView.MediaPreviewClickListener()
                //{
                //    @Override
                //    public void onPreviewClicked(MediaPreviewView preview)
                //    {
                //        startPlayingMedia(mediaUri, mediaTitle);
                //    }
                //});

                preview.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        startPlayingMedia(mediaUri, mediaTitle);
                    }
                });

                //set layoutparams of preview
                preview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) dpToPx(PREVIEW_HEIGHT_DP, this)));

                //add preview to container
                mediaPreviewContainer.addView(preview);
            }
            while (mediaCursor.moveToNext());
        }

        //TODO: add some dummy views to force the scrollview to go into scrolly mode
        for (int i = 0; i < 50; i++)
        {
            TextView dummy = new TextView(this);
            dummy.setText("~IM A DUMMY~");
            mediaPreviewContainer.addView(dummy, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        Logging.logE("end pop");
    }

    //region Util

    /**
     * Convert from density- independent pixels (dp) to device- specific pixels (px)
     *
     * @param dp      the dp to convert
     * @param context the context to convert in
     * @return the corresponding px
     */
    protected float dpToPx(float dp, Context context)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    /**
     * Check if the app was granted the permission.
     * If not granted, the permission will be requested and false will be returned.
     *
     * @param permission the permission to check
     * @param requestId  the request id. Used to check in callback
     * @return was the permission granted?
     */
    private boolean checkAndRequestPermission(@SuppressWarnings("SameParameterValue") String permission, @SuppressWarnings("SameParameterValue") int requestId)
    {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
        {
            //does not have permission, ask for it
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestId);
            return false;
        }
        else
        {
            //has permission
            return true;
        }
    }

    /**
     * Start to play the given media file
     *
     * @param mediaUri the media file to play
     * @param title    the title of the file
     */
    private void startPlayingMedia(Uri mediaUri, String title)
    {
        Intent playIntent = new Intent(this, LaunchActivity.class);
        playIntent.setData(mediaUri);
        if (title != null)
            playIntent.putExtra(Intent.EXTRA_TITLE, title);
        startActivity(playIntent);
    }
    //endregion

}
