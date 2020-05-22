package de.shadow578.yetanothervideoplayer.ui.mediapicker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.ui.AppSettingsActivity;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * The More / About fragment on the main screen
 */
public class AppMoreFragment extends Fragment implements View.OnClickListener
{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        //inflate layout
        View rootView = inflater.inflate(R.layout.mediapicker_fragment_more, container, false);

        //register click listeners
        rootView.findViewById(R.id.more_btn_update_check).setOnClickListener(this);
        rootView.findViewById(R.id.more_btn_settings).setOnClickListener(this);
        rootView.findViewById(R.id.more_btn_donate).setOnClickListener(this);
        rootView.findViewById(R.id.more_btn_about).setOnClickListener(this);
        rootView.findViewById(R.id.more_btn_help).setOnClickListener(this);

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
            case R.id.more_btn_update_check:
            {
                //update check button was clicked
                Toast.makeText(getContext(), "Checking for Updates...",Toast.LENGTH_LONG).show();
                //TODO: add update check
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
        if(linkUri == null)
            throw new IllegalArgumentException("Link ResID was not found or is no valid url!");

        //open link in default browser
        Intent webIntent = new Intent(Intent.ACTION_VIEW, linkUri);
        startActivity(webIntent);
    }
}
