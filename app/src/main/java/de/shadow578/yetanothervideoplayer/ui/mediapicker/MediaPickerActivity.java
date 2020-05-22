package de.shadow578.yetanothervideoplayer.ui.mediapicker;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.ui.mediapicker.chooser.DeviceMediaChooserFragment;
import de.shadow578.yetanothervideoplayer.ui.mediapicker.chooser.MediaEntry;
import de.shadow578.yetanothervideoplayer.util.Logging;

public class MediaPickerActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener
{
    //region Views

    /**
     * Bottom Navigation of the media picker activity
     */
    BottomNavigationView bottomNav;
    //endregion

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //do normal activity stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mediapicker_activity);

        //get views
        bottomNav = findViewById(R.id.mediapicker_navigation_bar);

        //set this activity as navigation listener
        bottomNav.setOnNavigationItemSelectedListener(this);

        //TODO: we need READ_EXTERNAL_STORAGE permissions before doing this, get them first ;)
        //show media chooser fragment for VIDEO by default
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mediapicker_content_container, new DeviceMediaChooserFragment(MediaEntry.MediaKind.VIDEO))
                .commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
    {
        //create fragment to show
        Fragment contentFragment = null;
        switch (menuItem.getItemId())
        {
            case R.id.mediapicker_navigation_target_videos:
            {
                //show a media chooser fragment for VIDEO
                contentFragment = new DeviceMediaChooserFragment(MediaEntry.MediaKind.VIDEO);
                break;
            }
            case R.id.mediapicker_navigation_target_music:
            {
                //show a media chooser fragment for MUSIC
                contentFragment = new DeviceMediaChooserFragment(MediaEntry.MediaKind.MUSIC);
                break;
            }
            case R.id.mediapicker_navigation_target_more:
            {
                //show "more" page
                contentFragment = new AppMoreFragment();
                break;
            }
        }

        //abort if no fragment was found for this nav item
        if (contentFragment == null)
        {
            Logging.logE("did not find a fragment for menu item id= %d", menuItem.getItemId());
            return false;
        }

        //show fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mediapicker_content_container, contentFragment)
                .commit();
        return true;
    }
}
