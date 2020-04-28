package de.shadow578.yetanothervideoplayer.feature.autopause;

import android.content.Context;
import android.content.res.Configuration;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

import de.shadow578.yetanothervideoplayer.util.Logging;

public class DetectorCameraPreview extends ViewGroup
{
    /**
     * Camera Source this View Previews
     */
    private CameraSource camSource;

    /**
     * The Surface the camera preview is rendered to
     */
    private SurfaceView previewSurface;

    /**
     * Is the preview surface view ready for use?
     */
    private boolean isPreviewSurfaceReady = false;

    /**
     * Is the camera preview currently active?
     */
    private boolean isPreviewActive = false;

    public DetectorCameraPreview(Context ctx)
    {
        super(ctx);
        initSurfaceView();
    }

    /**
     * Set the camera source that is being previewed and start the preview (if ready)
     *
     * @param source the camera source to use
     */
    public void setCameraSource(CameraSource source) throws IOException
    {
        //stop if no source is supplied
        if (source == null)
        {
            stop();
            return;
        }

        //set camera source
        camSource = source;

        //start the preview
        startIfReady();
    }

    /**
     * Release resources of this view
     */
    public void release()
    {
        camSource = null;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom)
    {
        Logging.logE("onLayout DetectorCameraPreview");

        //get size of camera source
        int camWidth = 640;
        int camHeight = 480;
        //if (camSource != null)
        //{
        //    Size camSize = camSource.getPreviewSize();
        //    if (camSize != null)
        //    {
        //        camWidth = camSize.getWidth();
        //        camHeight = camSize.getHeight();
        //    }
        //}

        //swap width and height when in portrait mode
        if (isPortrait())
        {
            int x = camWidth;
            //noinspection SuspiciousNameCombination
            camWidth = camHeight;
            //noinspection SuspiciousNameCombination
            camHeight = x;
        }

        //calculate current layout width and height
        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        //calculate width and height for WIDTH fit
        int cWidth = layoutWidth;
        int cHeight = (int) (((float) layoutWidth / (float) camWidth) * camHeight);

        //use HEIGHT fit if WIDTH fit is too tall
        if (cHeight > layoutHeight)
        {
            cHeight = layoutHeight;
            cWidth = (int) (((float) layoutHeight / (float) camHeight) * camWidth);
        }

        //calculate x and y offsets for centering the preview
        final int cX = (int)(((float)layoutWidth / 2f) - ((float)cWidth / 2f));
        final int cY = (int)(((float)layoutHeight / 2f) - ((float)cHeight / 2f));

        //do layout for children
        for (int ci = 0; ci < getChildCount(); ci++)
        {
            getChildAt(ci).layout(cX, cY, cX + cWidth, cY + cHeight);
        }

        //try to start preview
        tryStartIfReady();
    }

    /**
     * Try to start the preview if everything is ready
     */
    private void tryStartIfReady()
    {
        try
        {
            startIfReady();
        }
        catch (IOException e)
        {
            Logging.logE("Could not start Detector Camera Preview: %s", e.toString());
        }
    }

    /**
     * Start the preview if everything is ready
     */
    private void startIfReady() throws IOException
    {
        //dont start if already running
        if (isPreviewActive) return;

        //start if everything is ready
        if (camSource != null && previewSurface != null && isPreviewSurfaceReady)
        {
            //start camera source
            camSource.start(previewSurface.getHolder());

            //set preview active flag
            isPreviewActive = true;
        }
    }

    /**
     * Stop the preview
     */
    private void stop()
    {
        if (camSource != null)
        {
            camSource.stop();
        }

        //reset preview active flag
        isPreviewActive = false;
    }

    /**
     * Initializes the surface view and sets previewSurface to the newly created view
     */
    private void initSurfaceView()
    {
        previewSurface = new SurfaceView(getContext());
        previewSurface.getHolder().addCallback(new PreviewSurfaceCallback());
        addView(previewSurface);
    }

    /**
     * @return Is the device / view in portrait or landscape orientation?
     */
    private boolean isPortrait()
    {
        return getContext().getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
    }

    /**
     * Callback for the Preview Surface View
     */
    private class PreviewSurfaceCallback implements SurfaceHolder.Callback
    {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder)
        {
            //set ready flag
            isPreviewSurfaceReady = true;

            //try to start
            tryStartIfReady();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder)
        {
            //reset ready flag
            isPreviewSurfaceReady = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2)
        {
            //ignore
        }
    }
}
