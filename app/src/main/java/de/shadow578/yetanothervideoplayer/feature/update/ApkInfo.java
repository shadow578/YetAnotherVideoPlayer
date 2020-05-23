package de.shadow578.yetanothervideoplayer.feature.update;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * Contains information about a apk that is part of a {@link UpdateInfo}
 */
@SuppressWarnings("unused")
public final class ApkInfo implements Serializable
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
    private final String downloadUrl;

    /**
     * how large is the apk?
     */
    private final long fileSize;

    public ApkInfo(@NonNull String filename, @NonNull String downloadUrl, long fileSize)
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
    public String getDownloadUrl()
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

    @Override
    public String toString()
    {
        return "ApkInfo{" +
                "filename='" + filename + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}
