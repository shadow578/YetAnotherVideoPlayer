package de.shadow578.yetanothervideoplayer.ui.update;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.feature.update.UpdateInfo;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.ConfigUtil;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Helper for creating a dialog from a {@link UpdateInfo} + handling the actions
 */
@SuppressWarnings("WeakerAccess")
public class UpdateHelper
{
    /**
     * Callback for {@link UpdateHelper#showUpdateDialog(UpdateInfo, Callback, boolean)}
     */
    public interface Callback
    {
        /**
         * Called when the update is finished or no update was started
         *
         * @param isUpdating is the app currently updating? if true, the app will restart soon.
         */
        void onUpdateFinished(boolean isUpdating);
    }

    /**
     * Context of this helper
     */
    @NonNull
    private final Context ctx;

    public UpdateHelper(@NonNull Context ctx)
    {
        this.ctx = ctx;
    }

    //region External Utilities

    /**
     * @return the timestamp of the last update check
     */
    public long getTimeOfLastUpdateCheck()
    {
        return ConfigUtil.getConfigInt(ctx, ConfigKeys.KEY_LAST_UPDATE_CHECK, R.integer.DEF_LAST_UPDATE_CHECK);
    }

    /**
     * @return the time in seconds since the last update check
     */
    public long getTimeSinceLastUpdateCheck()
    {
        //calculate time since last check
        long delta = getCurrentTimestamp() - getTimeOfLastUpdateCheck();

        //force a update if negative
        if (delta < 0)
        {
            delta = Long.MAX_VALUE;
        }

        return delta;
    }

    /**
     * updates the last update check timestamp to the current time
     */
    public void updateTimeOfLastUpdateCheck()
    {
        ConfigUtil.setConfigInt(ctx, ConfigKeys.KEY_LAST_UPDATE_CHECK, (int) getCurrentTimestamp());
    }

    /**
     * @return the (persistent) update availability flag
     */
    public boolean getUpdateAvailableFlag()
    {
        return ConfigUtil.getConfigBoolean(ctx, ConfigKeys.KEY_UPDATE_AVAILABLE, R.bool.DEF_UPDATE_AVAILABLE);
    }

    /**
     * set the (persistent) update availability flag
     *
     * @param updateAvailable new value of the flag
     */
    public void setUpdateAvailableFlag(boolean updateAvailable)
    {
        ConfigUtil.setConfigBoolean(ctx, ConfigKeys.KEY_UPDATE_AVAILABLE, updateAvailable);
    }

    /**
     * @return the current timestamp
     */
    private long getCurrentTimestamp()
    {
        return Calendar.getInstance().getTimeInMillis() / 1000;
    }

    //endregion

    //region Update Dialog

    /**
     * Creates and shows a update dialog.
     * Gives the user the option to dismiss (+ ignore the update) and install the update.
     * Handles preference flags, install, etc. internally. Just fire & forget
     * When the user chooses to install the update, a new {@link UpdateActivity} is created and the app is restarted.
     * So make sure everything worth saving is saved ;)
     *
     * @param update    the update to create the dialog for (should have at least one apk asset)
     * @param callback  callback that is called once the update is finished (either because it was dismissed or finished installing)
     * @param forceShow if true, the users "ignore this update" settings are ignored
     */
    public void showUpdateDialog(@NonNull final UpdateInfo update, @Nullable final Callback callback, boolean forceShow)
    {
        //check if we should show a dialog at all
        if (dontShowUpdateDialog(update) && !forceShow)
        {
            if (callback != null) callback.onUpdateFinished(false);
            return;
        }

        //inflate custom view for the "ignore this version" checkbox that sits between description and the buttons
        //and get the checkbox in the layout
        View dialogAddin = View.inflate(ctx, R.layout.update_dialog_addin_layout, null);
        final CheckBox ignoreThisVersion = dialogAddin.findViewById(R.id.update_ignore_version);

        //get and shorten message
        String msg = update.getUpdateDesc();
        if (msg.length() > 150)
        {
            msg = msg.substring(0, msg.indexOf('\n', 140)) + "\n...";
        }

        //build a dialog that shows update title and description, with options to dismiss and update, + checkbox for ignoring this version
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ctx)
                .setTitle(update.getUpdateTitle())
                .setMessage(msg)
                .setView(dialogAddin)
                .setOnDismissListener(new DialogInterface.OnDismissListener()
                {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface)
                    {
                        dismissUpdate(update, ignoreThisVersion.isChecked());
                        if (callback != null)
                            callback.onUpdateFinished(false);
                    }
                })
                .setNegativeButton(R.string.update_dialog_dismiss, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dismissUpdate(update, ignoreThisVersion.isChecked());
                        if (callback != null)
                            callback.onUpdateFinished(false);
                    }
                })
                .setPositiveButton(R.string.update_dialog_install, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        installUpdate(update, callback);
                    }
                })
                .setNeutralButton(R.string.update_dialog_open_web, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        openUpdateInWeb(update);
                        if (callback != null)
                            callback.onUpdateFinished(false);
                    }
                });

        //try to show dialog
        try
        {
            builder.show();
        }
        catch (WindowManager.BadTokenException ignore)
        {
            Logging.logE("UpdateHelper: the window that called seems to have closed! catching error gracefully");
        }
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
        //open update activity, add update info as extra
        Intent updateIntent = new Intent(ctx, UpdateActivity.class);
        updateIntent.putExtra(UpdateActivity.EXTRA_UPDATE_INFO, update);
        ctx.startActivity(updateIntent);

        //call callback
        if (callback != null)
            callback.onUpdateFinished(true);
    }

    /**
     * Open the weburl of the update
     *
     * @param update the update
     */
    private void openUpdateInWeb(@NonNull UpdateInfo update)
    {
        //open link in default browser
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(update.getWebUrl()));
        ctx.startActivity(webIntent);
    }
    //endregion
}
