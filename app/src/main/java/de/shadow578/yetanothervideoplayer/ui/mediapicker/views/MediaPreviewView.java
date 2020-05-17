package de.shadow578.yetanothervideoplayer.ui.mediapicker.views;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Size;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * A View that previews a media element. Is clickable
 */
public class MediaPreviewView extends LinearLayout //implements View.OnTouchListener
{
    //region Variables
    //region Views

    /**
     * The image view showing the media thumbnail
     */
    private ImageView thumbnailView;

    /**
     * The text view displaying the media title
     */
    private TextView titleView;

    //endregion


    //endregion

    //region Constructors
    public MediaPreviewView(@NonNull Context context)
    {
        this(context, null);
    }

    public MediaPreviewView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public MediaPreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        this(context, attrs, defStyleAttr, 0);
    }

    public MediaPreviewView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        //do normal view stuff
        super(context, attrs, defStyleAttr, defStyleRes);

        //inflate layout
        inflate(context, R.layout.layout_media_preview_view, this);

        //get views
        thumbnailView = findViewById(R.id.mpv_thumbnail);
        titleView = findViewById(R.id.mpv_media_title);

        //make self clickable
        setClickable(true);
        setFocusable(true);

    }
    //endregion


    //region Interfacing

    //TODO: missing set/get for resolution and duration texts

    /**
     * set the title of the media this view previews
     *
     * @param title the title to set
     * @return own instance, for set chaining
     */
    public MediaPreviewView setTitle(@NonNull String title)
    {
        titleView.setText(title);
        return this;
    }

    /**
     * @return the title of the media this view previews
     */
    public String getTitle()
    {
        return titleView.getText().toString();
    }

    /**
     * set the thumbnail of this view using a media url.
     *
     * @param mediaUri the url of the media to get the thumbnail of
     * @return was a thumbnail found and set?
     */
    public boolean setThumbnailFromMediaUri(@NonNull Uri mediaUri)
    {
        //prepare a bitmap to hold the thumbnail
        Bitmap thumbnail = null;

        //try to use ContentResolver.loadThumbnail first
        //this requires Android Q, so check for that first
        ContentResolver contentResolver = getContext().getContentResolver();
        if (contentResolver != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            try
            {
                thumbnail = contentResolver.loadThumbnail(mediaUri, new Size(150, 100), null);
            }
            catch (IOException e)
            {
                //log error
                Logging.logE("IOException on ContentResolver.loadThumbnail:");
                e.printStackTrace();

                //reset thumbnail
                thumbnail = null;
            }
        }

        //try to create a thumbnail using ThumbnailUtils
        String mediaPath = mediaUri.getPath();
        if (thumbnail == null && mediaPath != null)
        {
            //get thumbnail
            thumbnail = ThumbnailUtils.createVideoThumbnail(mediaPath, MediaStore.Video.Thumbnails.MINI_KIND);
        }

        //set thumbnail if not null
        if (thumbnail != null)
        {
            setThumbnail(thumbnail);
            return true;
        }

        return false;
    }

    /**
     * set this views thumbnail image
     *
     * @param thumbnail the thumbnail to set
     * @return own instance, for set chaining
     */
    public MediaPreviewView setThumbnail(@NonNull Bitmap thumbnail)
    {
        thumbnailView.setImageBitmap(thumbnail);
        return this;
    }

    //endregion
}
