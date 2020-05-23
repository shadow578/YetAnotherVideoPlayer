package de.shadow578.yetanothervideoplayer.ui.update;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.feature.update.UpdateInfo;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.ConfigUtil;

/**
 * Helper for creating a dialog from a {@link UpdateInfo} + handling the actions
 */
public class UpdateDialogHelper
{
    /**
     * Callback for {@link UpdateDialogHelper#showUpdateDialog(UpdateInfo, Callback)}
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

    public UpdateDialogHelper(@NonNull Context ctx)
    {
        this.ctx = ctx;
    }

    /**
     * Creates and shows a update dialog.
     * Gives the user the option to dismiss (+ ignore the update) and install the update.
     * Handles preference flags, install, etc. internally. Just fire & forget
     * When the user chooses to install the update, a new {@link UpdateActivity} is created and the app is restarted.
     * So make sure everything worth saving is saved ;)
     *
     * @param update   the update to create the dialog for (should have at least one apk asset)
     * @param callback callback that is called once the update is finished (either because it was dismissed or finished installing)
     */
    public void showUpdateDialog(@NonNull final UpdateInfo update, @Nullable final Callback callback)
    {
        //check if we should show a dialog at all
        if (dontShowUpdateDialog(update))
        {
            if (callback != null) callback.onUpdateFinished(false);
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
                            callback.onUpdateFinished(false);
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
}
