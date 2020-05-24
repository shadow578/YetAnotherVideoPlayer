package de.shadow578.yetanothervideoplayer.ui.mediapicker.chooser;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Describes a media entry on this device
 */
@SuppressWarnings("unused")
public class MediaEntry
{
    /**
     * Type of media a MediaEntry can contain
     */
    public enum MediaKind
    {
        VIDEO,
        MUSIC
    }

    /**
     * What type of media is this?
     */
    @NonNull
    private final MediaKind kind;

    /**
     * The uri of this media
     */
    @NonNull
    private final Uri uri;

    /**
     * The title of this media
     */
    @NonNull
    private final String title;

    /**
     * how many seconds long is this media?
     */
    private final int duration;

    /**
     * which resolution does this media have?
     * Only videos, otherwise null
     */
    @Nullable
    private final Size videoResolution;

    /**
     * Thumbnail of the media. May be loaded at a later time
     */
    @Nullable
    private Bitmap thumbnail;

    MediaEntry(@NonNull MediaKind kind, @NonNull Uri uri, @NonNull String title, int duration, @Nullable Size videoResolution)
    {
        this.kind = kind;
        this.uri = uri;
        this.title = title;
        this.duration = duration;
        this.videoResolution = videoResolution;
    }

    @NonNull
    MediaKind getKind()
    {
        return kind;
    }

    @NonNull
    public Uri getUri()
    {
        return uri;
    }

    @NonNull
    public String getTitle()
    {
        return title;
    }

    public int getDuration()
    {
        return duration;
    }

    @Nullable
    Size getVideoResolution()
    {
        return videoResolution;
    }

    @Nullable
    Bitmap getThumbnail()
    {
        return thumbnail;
    }

    void setThumbnail(@Nullable Bitmap thumbnail)
    {
        this.thumbnail = thumbnail;
    }

    @NonNull
    @Override
    public String toString()
    {
        return String.format(Locale.US, "[kind= %s, title= %s, dur= %d, res= %s, uri= %s]", kind, title, duration,
                videoResolution == null ? "NULL" : videoResolution.toString(), uri.toString());
    }
}
