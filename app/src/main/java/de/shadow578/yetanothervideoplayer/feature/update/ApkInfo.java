package de.shadow578.yetanothervideoplayer.feature.update;

import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * Contains information about a apk that is part of a {@link UpdateInfo}
 */
@SuppressWarnings("unused")
public final class ApkInfo
{
    /**
     * filename of the apk
     */
    @NonNull
    private final String filename;

    /**
     * download url of the apk
     */
    @NonNull
    private final Uri downloadUrl;

    /**
     * how large is the apk?
     */
    private final long fileSize;

    ApkInfo(@NonNull String filename, @NonNull Uri downloadUrl, long fileSize)
    {
        this.filename = filename;
        this.downloadUrl = downloadUrl;
        this.fileSize = fileSize;
    }

    /**
     * @return filename of the apk
     */
    @NonNull
    public String getFilename()
    {
        return filename;
    }

    /**
     * @return download url of the apk
     */
    @NonNull
    public Uri getDownloadUrl()
    {
        return downloadUrl;
    }

    /**
     * @return how large is the apk?
     */
    public long getFileSize()
    {
        return fileSize;
    }
}
