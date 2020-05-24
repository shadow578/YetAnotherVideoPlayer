package de.shadow578.yetanothervideoplayer.ui.mediapicker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import java.util.Date;

import de.shadow578.yetanothervideoplayer.BuildConfig;
import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.feature.update.AppUpdateManager;
import de.shadow578.yetanothervideoplayer.feature.update.DefaultUpdateCallback;
import de.shadow578.yetanothervideoplayer.feature.update.UpdateInfo;
import de.shadow578.yetanothervideoplayer.ui.AppSettingsActivity;
import de.shadow578.yetanothervideoplayer.ui.PlayerDebugActivity;
import de.shadow578.yetanothervideoplayer.ui.update.UpdateDialogHelper;
import de.shadow578.yetanothervideoplayer.util.ConfigKeys;
import de.shadow578.yetanothervideoplayer.util.ConfigUtil;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * The More / About fragment on the main screen
 */
public class AppMoreFragment extends Fragment implements View.OnClickListener
{
    /**
     * Update manager to check for updates
     */
    private final AppUpdateManager updateManager = new AppUpdateManager(BuildConfig.UPDATE_VENDOR, BuildConfig.UPDATE_REPO);

    /**
     * Button to start a update (check)
     */
    private Button updateCheckButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        //inflate layout
        View rootView = inflater.inflate(R.layout.mediapicker_fragment_more, container, false);

        //register click listeners
        updateCheckButton = rootView.findViewById(R.id.more_btn_update_check);
        updateCheckButton.setOnClickListener(this);
        rootView.findViewById(R.id.more_btn_settings).setOnClickListener(this);
        rootView.findViewById(R.id.more_btn_donate).setOnClickListener(this);
        rootView.findViewById(R.id.more_btn_about).setOnClickListener(this);
        rootView.findViewById(R.id.more_btn_help).setOnClickListener(this);

        //register click listener for "debug yavp" button and hide the button if not a debug build
        final Button debugButton = rootView.findViewById(R.id.more_btn_debug);
        debugButton.setOnClickListener(this);
        debugButton.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);

        //change check for update button if a update is available
        if (getContext() != null && ConfigUtil.getConfigBoolean(getContext(), ConfigKeys.KEY_UPDATE_AVAILABLE, R.bool.DEF_UPDATE_AVAILABLE))
        {
            //update text
            updateCheckButton.setText(R.string.more_update_available);
        }

        //return inflated view
        return rootView;
    }

    /**
     * Called when a button in mediapicker_fragment_more was clicked
     *
     * @param view the view that was clicked
     */
    @Override
    public void onClick(View view)
    {
        Logging.logD("moreFragment_onClick()");
        switch (view.getId())
        {
            case R.id.more_btn_debug:
            {
                //debug yavp button was clicked
                startActivity(new Intent(getContext(), PlayerDebugActivity.class));
                break;
            }
            case R.id.more_btn_update_check:
            {
                //update check button was clicked
                checkForUpdate();
                break;
            }
            case R.id.more_btn_settings:
            {
                //settings button was clicked
                Intent settingsIntent = new Intent(getContext(), AppSettingsActivity.class);
                startActivity(settingsIntent);
                break;
            }
            case R.id.more_btn_donate:
            {
                //donation link was clicked
                openResLink(R.string.app_donate_url);
                break;
            }
            case R.id.more_btn_about:
            {
                //about link was clicked
                //TODO: directly go to "about" page
                Toast.makeText(getContext(), "Click on \"About YAVP\" :P", Toast.LENGTH_LONG).show();
                Intent settingsIntent = new Intent(getContext(), AppSettingsActivity.class);
                startActivity(settingsIntent);
                break;
            }
            case R.id.more_btn_help:
            {
                //help link was clicked
                openResLink(R.string.app_help_url);
                break;
            }
        }
    }

    /**
     * Opens a link defined as a string resource in a browser
     *
     * @param linkRes the link to open
     */
    private void openResLink(@StringRes int linkRes)
    {
        //get link as uri
        Uri linkUri = Uri.parse(getString(linkRes));
        if (linkUri == null)
            throw new IllegalArgumentException("Link ResID was not found or is no valid url!");

        //open link in default browser
        Intent webIntent = new Intent(Intent.ACTION_VIEW, linkUri);
        startActivity(webIntent);
    }

    /**
     * Uses a {@link de.shadow578.yetanothervideoplayer.feature.update.AppUpdateManager} to check for updates manually
     */
    private void checkForUpdate()
    {
        //disable the update button while searching
        updateCheckButton.setEnabled(false);

        //check for a update
        Toast.makeText(getContext(), R.string.more_update_check_start_toast, Toast.LENGTH_SHORT).show();
        updateManager.checkForUpdate(new DefaultUpdateCallback()
        {
            @Override
            public void onUpdateCheckFinished(@Nullable UpdateInfo update, boolean failed)
            {
                //check if update check failed
                Context ctx = getContext();
                if (failed || ctx == null)
                {
                    Toast.makeText(getContext(), R.string.more_update_check_failed_toast, Toast.LENGTH_LONG).show();
                    return;
                }

                //save the current timestamp as last update check time in shared prefs
                ConfigUtil.setConfigInt(getContext(), ConfigKeys.KEY_LAST_UPDATE_CHECK, (int) new Date().getTime());

                //do we have a update?
                if (update != null)
                {
                    //have a update, set persistent flag show update dialog
                    //set a flag in shared prefs that we have a update, in case the user does not update right away
                    ConfigUtil.setConfigBoolean(getContext(), ConfigKeys.KEY_UPDATE_AVAILABLE, true);

                    //show update dialog
                    new UpdateDialogHelper(ctx).showUpdateDialog(update, null, true);
                }
                else
                {
                    //no update found, tell user that
                    Toast.makeText(getContext(), R.string.more_no_update_toast, Toast.LENGTH_LONG).show();
                }

                //unlock button
                updateCheckButton.setEnabled(true);
            }
        });
    }
}
