package de.shadow578.yetanothervideoplayer.ui.test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import de.shadow578.yetanothervideoplayer.R;
import de.shadow578.yetanothervideoplayer.feature.autopause.DetectorCameraPreview;
import de.shadow578.yetanothervideoplayer.feature.autopause.PlayerAutoPauseCallback;
import de.shadow578.yetanothervideoplayer.feature.autopause.PlayerAutoPauseErrors;
import de.shadow578.yetanothervideoplayer.feature.autopause.PlayerAutoPauseEvents;
import de.shadow578.yetanothervideoplayer.feature.autopause.PlayerAutoPauseManager;
import de.shadow578.yetanothervideoplayer.util.Logging;

public class AutoPauseDebugActivity extends AppCompatActivity
{
    /**
     * auto pause manage that is used for testing
     */
    private PlayerAutoPauseManager autoPauseManager;

    /**
     * Switch if camera should be used for tracking
     */
    private Switch swUseCamera;

    /**
     * Switch if proximity should be used for tracking
     */
    private Switch swUseProximity;

    /**
     * Switch to enable / disable the manager
     */
    private Switch swEnableManager;

    /**
     * View that is the placeholder for the camera preview
     */
    private RelativeLayout cameraPreviewPlaceholder;

    /**
     * linear layout that will later contain the messages
     */
    private LinearLayout messageListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_pause_debug);

        //get views
        swUseCamera = findViewById(R.id.apd_switch_use_camera);
        swUseProximity = findViewById(R.id.apd_switch_use_proximity);
        swEnableManager = findViewById(R.id.apd_switch_enable);
        cameraPreviewPlaceholder = findViewById(R.id.apd_preview_placeholder);
        messageListContainer = findViewById(R.id.apd_message_container);

        //listen for changes on enable switch
        swEnableManager.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                addUIMessage("~~~~~~~~~~~~~~~~~~~~", false);

                if (swEnableManager != null && swEnableManager.isChecked())
                {
                    //enable tracking
                    addUIMessage("Starting Tracking...", false);
                    startManager();
                }
                else
                {
                    //disable tracking
                    addUIMessage("Stopping Tracking...", false);
                    stopManager();
                }
            }
        });

        //initialize auto pause manager
        initPauseManager();

        //initialize face tracking preview
        initFacePreview();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        stopManager();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        stopManager();
    }

    /**
     * initializes the autoPauseManager (does NOT start it)
     */
    private void initPauseManager()
    {
        autoPauseManager = new PlayerAutoPauseManager(this);
        if (autoPauseManager.initialize(new PauseManagerCallback()))
        {
            addUIMessage("Auto Pause Manager initialized OK!", false);
        }
        else
        {
            addUIMessage("Auto Pause Manager was not initialized!", true);
        }
    }

    /**
     * initialize the face tracker preview (does NOT start it)
     */
    private void initFacePreview()
    {
        //check we have a placeholder for the preview AND a manager object first
        if (cameraPreviewPlaceholder == null || autoPauseManager == null) return;

        //create preview
        DetectorCameraPreview preview = new DetectorCameraPreview(this);

        //set layout to fill parent
        preview.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        //add preview to placeholder
        cameraPreviewPlaceholder.addView(preview);

        //set preview as preview for manager
        autoPauseManager.setFaceTrackerPreview(preview);
    }

    /**
     * Add a message to the message container
     *
     * @param msg     the message to add
     * @param isError is this message a error message?
     */
    private void addUIMessage(String msg, boolean isError)
    {
        //log message normally
        Logging.logD("ADD to container: " + msg);

        //create a new text view
        TextView t = new TextView(this);
        t.setText(msg);

        //when error, make text red
        if (isError)
        {
            t.setTextColor(Color.RED);
        }

        //add to container at first position
        if (messageListContainer != null)
            messageListContainer.addView(t, 0);
    }

    /**
     * start the autopause manager
     */
    private void startManager()
    {
        //get if camera and proximity should be enabled
        boolean enableProximity = false;
        boolean enableCam = false;
        if (swUseProximity != null) enableProximity = swUseProximity.isChecked();
        if (swUseCamera != null) enableCam = swUseCamera.isChecked();

        //enable manager
        if (autoPauseManager != null)
            autoPauseManager.activate(enableProximity, enableCam);
    }

    /**
     * stop the autopause manager
     */
    private void stopManager()
    {
        if (autoPauseManager != null)
            autoPauseManager.deactivate();
    }

    /**
     * Request camera permissions if they are missing
     */
    private void requestCameraPermissions()
    {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
    }

    /**
     * Callback for the autoPauseManager
     */
    private class PauseManagerCallback implements PlayerAutoPauseCallback
    {

        @Override
        public void onPAPManagerEvent(PlayerAutoPauseEvents event)
        {
            String eventStr = "UNDEFINED";
            switch (event)
            {
                case PROXIMITY_MONITORING_STARTED:
                    eventStr = "Proximity Monitoring Started";
                    break;
                case PROXIMITY_MONITORING_STOPPED:
                    eventStr = "Proximity Monitoring Stopped";
                    break;
                case LIGHT_MONITORING_STARTED:
                    eventStr = "Light Monitoring Started";
                    break;
                case LIGHT_MONITORING_STOPPED:
                    eventStr = "Light Monitoring Stopped";
                    break;
                case FACE_TRACKING_STARTED:
                    eventStr = "Face Tracking Started";
                    break;
                case FACE_TRACKING_STOPPED:
                    eventStr = "Face Tracking Stopped";
                    break;
                case NEAR_PROXIMITY:
                    eventStr = "State = Near Proximity";
                    break;
                case LOW_LIGHT:
                    eventStr = "State = Low Light";
                    break;
                case GOT_ACTIVE_FACE:
                    eventStr = "State = Got Face";
                    break;
                case LOST_ACTIVE_FACE:
                    eventStr = "State = Lost Face";
                    break;
            }

            addUIMessage(eventStr, false);
        }

        @Override
        public void onPAPManagerError(PlayerAutoPauseErrors error)
        {
            String errorStr = "Undefined";
            switch (error)
            {
                case UNDEFINED:
                    errorStr = "ERR: UNDEFINED";

                    //log stacktrace of how we got here
                    Exception e = new Exception("Tracking Exception");
                    e.printStackTrace();
                    break;
                case NO_PROXIMITY_SENSOR:
                    errorStr = "ERR: No Proximity Sensor";
                    break;
                case NO_LIGHT_SENSOR:
                    errorStr = "ERR: No Light Sensor";
                    break;
                case LOW_LIGHT_DETECTED:
                    errorStr = "ERR: Low Light Detected";
                    break;
                case NO_CAMERA_PERMISSION:
                    errorStr = "ERR: No Camera Permissions";

                    //request permissions
                    requestCameraPermissions();
                    break;
                case NO_FRONT_CAMERA:
                    errorStr = "ERR: No Front Camera";
                    break;
                case NO_GMS_SERVICES_AVAILABLE:
                    errorStr = "ERR: No GMS Services";
                    break;
            }

            addUIMessage(errorStr, true);
        }
    }
}
