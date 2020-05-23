package de.shadow578.yetanothervideoplayer.ui.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

import de.shadow578.yetanothervideoplayer.BuildConfig;
import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.feature.update.ApkInfo;
import de.shadow578.yetanothervideoplayer.feature.update.UpdateInfo;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Updater activity.
 * Handles downloading and installing app updates.
 * Called by UpdateDialogHelper.
 */
public class UpdateActivity extends AppCompatActivity
{
    /**
     * Extra key for the update info this activity should use to update.
     * must be of type {@link UpdateInfo}
     */
    public static final String EXTRA_UPDATE_INFO = "update_info";

    /**
     * Mime type of a apk file
     */
    private final String MIME_TYPE_APK = "application/vnd.android.package-archive";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        //init activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_activity);

        //get intent that called
        Intent callIntent = getIntent();
        if (callIntent == null)
        {
            onUpdateFailed();
            return;
        }

        //get update info from intent
        UpdateInfo update = (UpdateInfo) callIntent.getSerializableExtra(EXTRA_UPDATE_INFO);
        if (update == null)
        {
            onUpdateFailed();
            return;
        }

        //start update
        installUpdate(update);
    }

    /**
     * Called when the update failed
     */
    private void onUpdateFailed()
    {

    }

    //region Update Download and install logic

    /**
     * install the update
     *
     * @param update the update to install. better be not null ;)
     */
    private void installUpdate(@NonNull UpdateInfo update)
    {
        //get and check apk of the update
        ApkInfo updateApk;
        if (update.getUpdateAssets().length <= 0
                || (updateApk = update.getUpdateAssets()[0]) == null
                || updateApk.getFileSize() <= 0)
        {
            Logging.logE("update apk for update %s seems to be invalid!", update.getUpdateTitle());
            Toast.makeText(this, R.string.update_failed_toast, Toast.LENGTH_LONG).show();
            onUpdateFailed();
            return;
        }

        //install apk from update
        downloadAndInstallUpdate(updateApk, update.getVersionTag());
    }

    /**
     * download and install the apk file
     *
     * @param apk          the apk to download and install
     * @param downloadDesc description of the download, if null no description is set
     */
    private void downloadAndInstallUpdate(@NonNull final ApkInfo apk, @Nullable String downloadDesc)
    {
        //find a output name that does not yet exists
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File apkFilex = new File(downloadsDir, apk.getFilename());
        int n = 0;
        while (apkFilex.exists())
        {
            apkFilex = new File(downloadsDir, n + apk.getFilename());
            n++;
        }
        final File apkFile = apkFilex;

        //create request to download manager
        DownloadManager.Request dlRequest = new DownloadManager.Request(Uri.parse(apk.getDownloadUrl()));
        dlRequest.setTitle(getString(R.string.update_download_manager_title));
        dlRequest.setDestinationUri(Uri.fromFile(apkFilex));
        if (downloadDesc != null && !downloadDesc.isEmpty())
        {
            dlRequest.setDescription(downloadDesc);
        }

        //get download manager service
        final DownloadManager dlManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dlManager == null)
        {
            Logging.logE("failed to find download manager!");
            onUpdateFailed();
            return;
        }

        //enqueue the download of our file
        final long dlId = dlManager.enqueue(dlRequest);

        //create and register a broadcast receiver that will listen for when the file finished downloading
        BroadcastReceiver dlCompleteReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                //check this is the right download id
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != dlId)
                {
                    Logging.logE("Received download complete broadcast for dlId= %d, expected %d", id, dlId);
                    //onUpdateFailed();
                    //return;
                }

                //check the file has the right length and mimetype
                String dlMime = dlManager.getMimeTypeForDownloadedFile(dlId);
                if (apkFile.exists()
                        && apkFile.length() == apk.getFileSize()
                        && dlMime.equalsIgnoreCase(MIME_TYPE_APK))
                {
                    //install the apk
                    Intent installIntent = new Intent(Intent.ACTION_VIEW);
                    installIntent.setDataAndType(getFileUri(apkFile), MIME_TYPE_APK);
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(installIntent);
                }
                else
                {
                    //apk has wrong length or does not exist
                    onUpdateFailed();
                    Logging.logE("update apk %s has wrong size or mimetype: size expected %d, found %d, mimetype expected: %s, found %s", apkFile.toString(),
                            apkFile.length(), apk.getFileSize()
                            , dlMime, MIME_TYPE_APK);
                }

                //delete downloaded file
                //TODO: does this actually work?
                apkFile.deleteOnExit();

                //unregister receiver and call callback
                unregisterReceiver(this);
            }
        };
        registerReceiver(dlCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * get a uri from a file
     *
     * @param file the file to get the uri of
     * @return the uri of the file
     */
    private Uri getFileUri(File file)
    {
        if (Build.VERSION.SDK_INT >= 24)
        {
            return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
        }
        else
        {
            return Uri.fromFile(file);
        }
    }
    //endregion
}
