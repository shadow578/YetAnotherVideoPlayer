package de.shadow578.yetanothervideoplayer.ui.mediapicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashSet;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.ui.LaunchActivity;
import de.shadow578.yetanothervideoplayer.ui.mediapicker.views.MediaPreviewView;
import de.shadow578.yetanothervideoplayer.util.Logging;

public class MediaPickerActivity extends AppCompatActivity implements MediaPreviewView.MediaPreviewClickListener
{

    private ViewGroup mediaPreviewContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //do normal stuff
        super.onCreate(savedInstanceState);

        //inflate content
        setContentView(R.layout.activity_media_picker);

        //get views
        mediaPreviewContainer = findViewById(R.id.mp_preview_container);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        //request media permissions
        if (!checkAndRequestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 0))
            return;

        //populate preview container with previews for all media
        populateMediaPreview();
    }


    private void populateMediaPreview()
    {
        Logging.logE("Start pop");

        //get cursor for all videos on the device
        try (Cursor mediaCursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Video.VideoColumns.DATA, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.VideoColumns._ID}, null, null, null))
        {
            //move cursor to first entry
            mediaCursor.moveToFirst();

            //add all entries
            do
            {
                //get media uri and title
                String uri = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Video.Media.DATA));
                String title = mediaCursor.getString(mediaCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));

                //get thumbnail
                long id = mediaCursor.getLong(mediaCursor.getColumnIndex(MediaStore.Video.Media._ID));
                Bitmap thumbnail = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(), id, MediaStore.Video.Thumbnails.MINI_KIND, null);


                //create new preview
                MediaPreviewView previewView = new MediaPreviewView(this);
                mediaPreviewContainer.addView(previewView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                //set media data of preview
                previewView.setMedia(Uri.parse(uri), title, thumbnail);

                //set click listener
                previewView.setClickListener(this);
            }
            while (mediaCursor.moveToNext());
        }

        Logging.logE("end pop");
    }

    /**
     * Check if the app was granted the permission.
     * If not granted, the permission will be requested and false will be returned.
     *
     * @param permission the permission to check
     * @param requestId  the request id. Used to check in callback
     * @return was the permission granted?
     */
    private boolean checkAndRequestPermission(@SuppressWarnings("SameParameterValue") String permission, @SuppressWarnings("SameParameterValue") int requestId)
    {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
        {
            //does not have permission, ask for it
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestId);
            return false;
        }
        else
        {
            //has permission
            return true;
        }
    }

    boolean once = false;

    @Override
    public void onPreviewClicked(MediaPreviewView preview, Uri mediaUri)
    {
        if (once) return;
        once = true;

        Intent playIntent = new Intent(this, LaunchActivity.class);
        playIntent.setData(mediaUri);
        playIntent.putExtra(Intent.EXTRA_TITLE, "DEV_MEDIA_PICKER");
        startActivity(playIntent);
    }
}
