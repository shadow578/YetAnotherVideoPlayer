package de.shadow578.yetanothervideoplayer.ui.update;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
 * Called by UpdateHelper.
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

    /**
     * Id for permission request WRITE_EXTERNAL_STORAGE
     */
    private final int ID_PERMISSION_WRITE_EXT_STORAGE = 15;

    /**
     * Main progress bar to show update / download progress
     */
    private ProgressBar progressBar;

    /**
     * TextView that shows the updates title
     */
    private TextView updateTitle;

    /**
     * TextView that shows the updates message / changelog
     */
    private TextView updateMessage;

    /**
     * Download manager for downloading app updates
     */
    private DownloadManager downloadManager;

    /**
     * Update to install
     */
    private UpdateInfo update;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        //init activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_activity);

        //find views
        progressBar = findViewById(R.id.update_progress_bar);
        updateTitle = findViewById(R.id.update_title);
        updateMessage = findViewById(R.id.update_message);

        //get services
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        //get intent that called
        Intent callIntent = getIntent();
        if (callIntent == null)
        {
            onUpdateFailed();
            return;
        }

        //get update info from intent
        update = (UpdateInfo) callIntent.getSerializableExtra(EXTRA_UPDATE_INFO);
        if (update == null)
        {
            onUpdateFailed();
            return;
        }

        //update ui
        setUpdateInfo(update);

        //do we have permissions to write external storage (needed for downloading the update)?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            //have permission, all ok
            installUpdate(update);
        }
        else
        {
            //dont have permission, request
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, ID_PERMISSION_WRITE_EXT_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == ID_PERMISSION_WRITE_EXT_STORAGE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && update != null)
        {
            //got permissions
            installUpdate(update);
        }
        else
        {
            //permissions denied
            onUpdateFailed();
        }
    }

    /**
     * Called when the update failed
     */
    private void onUpdateFailed()
    {
        Toast.makeText(this, R.string.update_failed_toast, Toast.LENGTH_LONG).show();
        progressBar.setIndeterminate(false);
        progressBar.setProgress(0);
        updateTitle.setText(R.string.update_failed_toast);
    }

    /**
     * Called when the update is ready to install
     *
     * @param updateApk the apk file that should be installed for this update
     */
    private void onUpdateReady(@NonNull File updateApk)
    {
        //reset update flag
        new UpdateHelper(this).setUpdateAvailableFlag(false);

        //install apk
        Toast.makeText(this, R.string.update_install_apk_toast, Toast.LENGTH_LONG).show();
        installApk(updateApk);
        finish();
    }

    /**
     * set update message and title on the ui
     *
     * @param update the update to get title and message of
     */
    private void setUpdateInfo(@NonNull UpdateInfo update)
    {
        updateTitle.setText(update.getUpdateTitle());
        updateMessage.setText(update.getUpdateDesc());
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

        //check download manager is ok
        if (downloadManager == null)
        {
            Logging.logE("failed to find download manager!");
            onUpdateFailed();
            return;
        }

        //install apk from update
        long id = downloadAndInstallUpdate(downloadManager, updateApk, update.getVersionTag());

        //start download observer
        createDownloadProgressObserver(downloadManager, id, progressBar).start();
    }

    /**
     * download and install the apk file
     *
     * @param dlManager    download manager to use for the download
     * @param apk          the apk to download and install
     * @param downloadDesc description of the download, if null no description is set
     * @return download id of the started download
     */
    private long downloadAndInstallUpdate(@NonNull final DownloadManager dlManager, @NonNull final ApkInfo apk, @Nullable String downloadDesc)
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
                    onUpdateReady(apkFile);
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

        //return download id
        return dlId;
    }

    /**
     * installs a apk file
     *
     * @param apkFile the apk to install
     */
    private void installApk(@NonNull File apkFile)
    {
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(getFileUri(apkFile), MIME_TYPE_APK);
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(installIntent);
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

    /**
     * Creates a thread that observes the download of the given download id on the download manager, and updates the progress bar accordingly
     *
     * @param dlManager   the download manager to observe
     * @param dlId        the download to observe
     * @param progressBar the progress bar that is updated according to the download progress
     * @return the thread to observe the download. Have to call {@link Thread#start()} to start observing.
     */
    private Thread createDownloadProgressObserver(@NonNull final DownloadManager dlManager, final long dlId, @NonNull final ProgressBar progressBar)
    {
        return new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                //repeat until download finishes
                boolean isDownloading = true;
                while (isDownloading)
                {
                    //query download progress
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(dlId);

                    //access data of query
                    try (Cursor dlCursor = dlManager.query(query))
                    {
                        //check cursor is valid
                        if (dlCursor != null && dlCursor.getCount() > 0)
                        {
                            //move to first position
                            dlCursor.moveToFirst();

                            //get downloaded and total bytes + current download status
                            final long bytesDownloaded = dlCursor.getLong(dlCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                            final long bytesTotal = dlCursor.getLong(dlCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                            final int dlStatus = dlCursor.getInt(dlCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));

                            //set downloading flag to false if finished or failed download
                            isDownloading = dlStatus != DownloadManager.STATUS_SUCCESSFUL && dlStatus != DownloadManager.STATUS_FAILED;

                            //calculate progress of download in percent
                            final int downloadProgress = (int) (bytesDownloaded * 100 / bytesTotal);

                            //update ui
                            runOnUiThread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    Logging.logD("p= %d", downloadProgress);

                                    //update progress bar
                                    if (dlStatus == DownloadManager.STATUS_RUNNING
                                            && downloadProgress > 0 && downloadProgress < 100)
                                    {
                                        //update progress bar
                                        progressBar.setIndeterminate(false);
                                        progressBar.setProgress(downloadProgress, true);
                                    }
                                    else
                                    {
                                        //set progress bar to intermediate -> no downloading is happening
                                        progressBar.setIndeterminate(true);
                                    }
                                }
                            });
                        }
                    }

                    //wait a bit before next update
                    try
                    {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException ignored)
                    {
                    }
                }
            }
        });
    }
    //endregion
}
