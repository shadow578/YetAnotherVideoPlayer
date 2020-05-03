package de.shadow578.yetanothervideoplayer.ui.mediapicker.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Size;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.util.Logging;

public class MediaPreviewView extends FrameLayout implements View.OnTouchListener
{
    //region Variables
    //region Views

    private ImageView thumbnailView;

    private TextView titleView;

    //endregion

    private Uri mediaUri;

    private MediaPreviewClickListener onClickListener;

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

        setOnTouchListener(this);
    }
    //endregion


    public void setMedia(Uri mediaUrl, String mediaTitle, @Nullable Bitmap mediaThumbnail)
    {
        //get thumbnail with media resolver
        if (mediaThumbnail == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            try
            {
                mediaThumbnail = getContext().getContentResolver().loadThumbnail(mediaUri, new Size(thumbnailView.getWidth(), thumbnailView.getHeight()), null);
            }
            catch (IOException e)
            {
                Logging.logE("IOException while getting thumbnail:");
                e.printStackTrace();
            }
        }

        //set title and thumbnail
        if (mediaThumbnail != null)
            thumbnailView.setImageBitmap(mediaThumbnail);
        titleView.setText(mediaTitle);

        //set media url
        mediaUri = mediaUrl;

    }

    public void setClickListener(MediaPreviewClickListener listener)
    {
        onClickListener = listener;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        if(onClickListener != null)
            onClickListener.onPreviewClicked(this, mediaUri);

        return true;
    }


    public interface MediaPreviewClickListener
    {
        void onPreviewClicked(MediaPreviewView preview, Uri mediaUri);
    }
}
