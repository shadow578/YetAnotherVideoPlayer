package de.shadow578.yetanothervideoplayer.ui.update;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

import de.shadow578.yetanothervideoplayer.BuildConfig;
import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.feature.update.ApkInfo;
import de.shadow578.yetanothervideoplayer.feature.update.UpdateInfo;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.ConfigUtil;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Helper for creating a dialog from a {@link UpdateInfo} + handling the actions
 */
public class UpdateDialogHelper
{
    /**
     * Mime type of a apk file
     */
    private final String MIME_TYPE_APK = "application/vnd.android.package-archive";

    /**
     * Callback for {@link UpdateDialogHelper#showUpdateDialog(UpdateInfo, Callback)}
     */
    public interface Callback
    {
        /**
         * Called when the update is finished or no update was started
         */
        void onUpdateFinished();
    }

    /**
     * Context of this helper
     */
    @NonNull
    private final Context ctx;

    public UpdateDialogHelper(@NonNull Context ctx)
    {
        this.ctx = ctx;
    }

    /**
     * Creates and shows a update dialog.
     * Gives the user the option to dismiss (+ ignore the update) and install the update.
     * Handles preference flags, install, etc. internally. Just fire & forget
     *
     * @param update   the update to create the dialog for (should have at least one apk asset)
     * @param callback callback that is called once the update is finished (either because it was dismissed or finished installing)
     */
    public void showUpdateDialog(@NonNull final UpdateInfo update, @Nullable final Callback callback)
    {
        //check if we should show a dialog at all
        if (dontShowUpdateDialog(update))
        {
            if (callback != null) callback.onUpdateFinished();
            return;
        }

        //inflate custom view for the "ignore this version" checkbox that sits between description and the buttons
        //and get the checkbox in the layout
        View dialogAddin = View.inflate(ctx, R.layout.update_dialog_addin_layout, null);
        final CheckBox ignoreThisVersion = dialogAddin.findViewById(R.id.update_ignore_version);

        //build a dialog that shows update title and description, with options to dismiss and update, + checkbox for ignoring this version
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(update.getUpdateTitle())
                .setMessage(update.getUpdateDesc())
                .setView(dialogAddin)
                .setPositiveButton(R.string.update_dialog_install, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        installUpdate(update, callback);
                    }
                })
                .setNegativeButton(R.string.update_dialog_dismiss, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dismissUpdate(update, ignoreThisVersion.isChecked());
                        if (callback != null)
                            callback.onUpdateFinished();
                    }
                })
                .show();
    }

    /**
     * Check if we should show a dialog for this update
     *
     * @param update the update to check for
     * @return should be show a dialog for this update?
     */
    private boolean dontShowUpdateDialog(@NonNull UpdateInfo update)
    {
        //dont show dialog if IGNORE_UPDATE_VERSION equals the update version tag
        return ConfigUtil.getConfigString(ctx, ConfigKeys.KEY_IGNORE_UPDATE_VERSION, R.string.DEF_IGNORE_UPDATE_VERSION).equalsIgnoreCase(update.getVersionTag());
    }

    /**
     * Dismisses this update
     *
     * @param update        the update that is being dismissed
     * @param ignoreVersion should we ignore this version?
     */
    private void dismissUpdate(@NonNull UpdateInfo update, boolean ignoreVersion)
    {
        if (ignoreVersion)
        {
            ConfigUtil.setConfigString(ctx, ConfigKeys.KEY_IGNORE_UPDATE_VERSION, update.getVersionTag());
        }
    }

    /**
     * try to install the update
     *
     * @param update the update to install
     */
    private void installUpdate(@NonNull UpdateInfo update, @Nullable final Callback callback)
    {
        //get and check apk of the update
        ApkInfo updateApk;
        if (update.getUpdateAssets().length <= 0
                || (updateApk = update.getUpdateAssets()[0]) == null
                || updateApk.getFileSize() <= 0)
        {
            Logging.logE("update apk for update %s seems invalid!", update.getVersionTag());
            return;
        }

        //install apk, reset update available flag is successful
        downloadAndInstallApkAsset(updateApk, new Callback()
        {
            @Override
            public void onUpdateFinished()
            {
                //reset flag
                ConfigUtil.setConfigBoolean(ctx, ConfigKeys.KEY_UPDATE_AVAILABLE, false);

                //call original callback
                if (callback != null)
                    callback.onUpdateFinished();
            }
        });
    }

    /**
     * download and install the apk file.
     *
     * @param apk      the apk to download and install
     * @param callback callback to call once we finish updating
     */
    private void downloadAndInstallApkAsset(@NonNull final ApkInfo apk, @NonNull final Callback callback)
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
        DownloadManager.Request dlRequest = new DownloadManager.Request(apk.getDownloadUrl());
        dlRequest.setTitle(ctx.getString(R.string.update_download_manager_title));
        //dlRequest.setDescription("UPDATE_DESCRIPTION");
        dlRequest.setDestinationUri(Uri.fromFile(apkFilex));

        //get download manager service
        final DownloadManager dlManager = (DownloadManager) ctx.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dlManager == null)
        {
            Logging.logE("failed to find download manager!");
            callback.onUpdateFinished();
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
                    ctx.startActivity(installIntent);
                }
                else
                {
                    //apk has wrong length or does not exist
                    Logging.logE("update apk %s has wrong size or mimetype: size expected %d, found %d, mimetype expected: %s, found %s", apkFile.toString(),
                            apkFile.length(), apk.getFileSize()
                            , dlMime, MIME_TYPE_APK);
                }

                //delete downloaded file
                //TODO: does this actually work?
                apkFile.deleteOnExit();

                //unregister receiver and call callback
                ctx.unregisterReceiver(this);
                callback.onUpdateFinished();
            }
        };
        ctx.registerReceiver(dlCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
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
            return FileProvider.getUriForFile(ctx, BuildConfig.APPLICATION_ID + ".provider", file);
        }
        else
        {
            return Uri.fromFile(file);
        }
    }
}
