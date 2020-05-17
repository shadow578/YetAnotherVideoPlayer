package de.shadow578.yetanothervideoplayer.ui.mediapicker.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import java.util.Locale;

import de.shadow578.yetanothervideoplayer.R;

/**
 * CardView for Media Previews
 * Shows Thumbnail, Title, Playback Duration and Video Resolution to the user.
 */
public class MediaCardView extends CardView
{

    //region Views

    /**
     * Shows the media thumbnail / artwork
     */
    ImageView thumbnail;

    /**
     * Shows the media title / display name
     */
    TextView title;

    /**
     * Shows media resolution
     * Invisible for non- video files
     */
    TextView resolution;

    /**
     * Shows media duration
     */
    TextView duration;

    //endregion

    //region Constructor
    public MediaCardView(@NonNull Context context)
    {
        this(context, null);
    }

    public MediaCardView(@NonNull Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public MediaCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        //do normal cardview suff
        super(context, attrs, defStyleAttr);

        //inflate layout
        inflate(getContext(), R.layout.mediapicker_mediacardview_layout, this);

        //get views
        thumbnail = findViewById(R.id.mediacardview_thumbnail);
        title = findViewById(R.id.mediacardview_title);
        resolution = findViewById(R.id.mediacardview_resolution);
        duration = findViewById(R.id.mediacardview_duration);
    }
    //endregion

    //region getter / setter

    /**
     * Set the media thumbnail shown
     *
     * @return own instance, for set chaining
     */
    public MediaCardView setMediaThumbnail(Bitmap thumbnail)
    {
        this.thumbnail.setImageBitmap(thumbnail);
        return this;
    }

    /**
     * Set the media thumbnail shown
     *
     * @return own instance, for set chaining
     */
    public MediaCardView setMediaThumbnail(Drawable thumbnail)
    {
        this.thumbnail.setImageDrawable(thumbnail);
        return this;
    }

    /**
     * Set the media title shown
     *
     * @return own instance, for set chaining
     */
    public MediaCardView setMediaTitle(CharSequence title)
    {
        this.title.setText(title);
        return this;
    }

    /**
     * Set the media duration shown
     *
     * @return own instance, for set chaining
     */
    public MediaCardView setMediaDuration(long seconds)
    {
        duration.setText(String.format(Locale.US, "%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, (seconds % 60)));
        return this;
    }

    /**
     * Set the media resolution shown
     *
     * @return own instance, for set chaining
     */
    public MediaCardView setMediaResolution(int width, int height)
    {
        resolution.setText(String.format(Locale.US, "%dx%d", width, height));
        return this;
    }

    /**
     * Set if the media resolution is visible (default is true, set to false for eg. audio)
     *
     * @return own instance, for set chaining
     */
    public MediaCardView setShowMediaResolution(boolean showMediaResolution)
    {
        resolution.setVisibility(showMediaResolution ? VISIBLE : GONE);
        return this;
    }
    //endregion
}
