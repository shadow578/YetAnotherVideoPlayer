package de.shadow578.yetanothervideoplayer.feature.gl;

import com.daasuu.epf.EFramebufferObject;
import com.daasuu.epf.EglUtil;
import com.daasuu.epf.filter.GlFilter;

import de.shadow578.yetanothervideoplayer.util.Logging;

import java.util.HashMap;

//allow direct use of gl functions
import static android.opengl.GLES20.*;

/**
 * GLFilter base class for custom filters
 */
@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess"})
public class GLFilterBase extends GlFilter
{
    // region GLSL Shaders
    protected static final String DEFAULT_VERTEX_SHADER =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "gl_Position = aPosition;\n" +
                    "vTextureCoord = aTextureCoord.xy;\n" +
                    "}\n";

    protected static final String DEFAULT_FRAGMENT_SHADER =
            "precision mediump float;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "uniform lowp sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "vec4 c = texture2D(sTexture, vTextureCoord);\n" +
                    "gl_FragColor = c.rgba; \n" +
                    "}\n";
    // endregion

    private static final float[] VERTICES_DATA = new float[]{
            // X, Y, Z, U, V
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f
    };
    private final HashMap<String, Integer> handleCache = new HashMap<>();

    private int vertexShader, fragmentShader, program;
    private int vertexBuffer;

    protected final long fpsLogIntervalMs = 5000;
    protected long tLastFrame = 0, tLastLog = 0, fpsFrameCounter = 0;

    protected boolean logFps = false;
    protected long fpsLimit = 60;

    /**
     * Set this filter up for use
     */
    @Override
    public void setup()
    {
        //setup shader program
        vertexShader = EglUtil.loadShader(DEFAULT_VERTEX_SHADER, GL_VERTEX_SHADER);
        fragmentShader = EglUtil.loadShader(DEFAULT_FRAGMENT_SHADER, GL_FRAGMENT_SHADER);
        program = EglUtil.createProgram(vertexShader, fragmentShader);

        //create vertex buffer
        setupVertexBuffer();

        //log setup step
        Logging.logD("setup shader finished.");
    }

    /**
     * Called when the frame size is changed. use this to update frame buffers.
     *
     * @param width  the new width
     * @param height the new height
     */
    @Override
    public void setFrameSize(int width, int height)
    {
        super.setFrameSize(width, height);
        Logging.logD("Set frame size to " + width + " x " + height);
    }

    /**
     * @param sourceTexture the texture to draw
     * @param targetBuffer  the frame buffer to draw to. is already active framebuffer when this function is called
     */
    @Override
    public void draw(int sourceTexture, EFramebufferObject targetBuffer)
    {
        //draw texture to tmpBuffer using program
        drawUsingProgram(program, sourceTexture, targetBuffer);

        //log + limit fps
        updateFpsLogic(logFps, fpsLimit);
    }

    /**
     * Release Gl Resources of this filter
     */
    @Override
    public void release()
    {
        //delete shader program
        glDeleteProgram(program);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        program = 0;
        vertexShader = 0;
        fragmentShader = 0;

        //clear handle cache
        handleCache.clear();

        releaseVertexBuffer();

        //log release
        Logging.logD("Released shader.");
    }

    //region GL Util functions

    /**
     * Draw a texture to a frame buffer using a gl program
     *
     * @param program    the program to use for drawing
     * @param texToDraw  the texture to draw
     * @param drawTarget the frame buffer to draw to
     */
    protected void drawUsingProgram(int program, int texToDraw, EFramebufferObject drawTarget)
    {
        //set drawTarget frame buffer as render target
        drawTarget.enable();

        //use the wanted program
        glUseProgram(program);

        //get attribute handles
        int hndAPosition = getGlHandle(program, "aPosition");
        int hndATextureCoord = getGlHandle(program, "aTextureCoord");

        //setup gl vertex attributes
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glEnableVertexAttribArray(hndAPosition);
        glVertexAttribPointer(hndAPosition, VERTICES_DATA_POS_SIZE, GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_POS_OFFSET);
        glEnableVertexAttribArray(hndATextureCoord);
        glVertexAttribPointer(hndATextureCoord, VERTICES_DATA_UV_SIZE, GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_UV_OFFSET);

        //bind the texture to draw
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texToDraw);

        //set uniform values (texture is default, additionally set custom uniforms
        glUniform1i(getGlHandle(program, "sTexture"), 0);
        setCustomUniforms(program);

        //draw a textured quad using the bound texture
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        //cleanup
        glDisableVertexAttribArray(hndAPosition);
        glDisableVertexAttribArray(hndATextureCoord);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /**
     * Set custom uniform values for the current program.
     * Called in drawUsingProgram right before screen is drawn to buffer, but after sTexture uniform is set.
     * Use getGlHandle to get the handle to the uniform you want to set, then set it using glUniform... function.
     *
     * @param program the program that is used for drawing
     */
    protected void setCustomUniforms(int program)
    {

    }

    /**
     * Get the GL handle for a attribute or uniform
     *
     * @param program the program the uniform / attribute is in
     * @param name    the name of the attribute or uniform
     * @return the handle for the uniform or attribute
     * @throws IllegalStateException when no handle for the given name could be found
     */
    protected int getGlHandle(final int program, final String name)
    {
        //try to get the value from the handle cache
        final Integer cachedHandle = handleCache.get(program + ":" + name);
        if (cachedHandle != null)
        {
            return cachedHandle;
        }

        //not in cache, try to get from gl
        int handle = glGetAttribLocation(program, name);
        if (handle == -1)
        {
            //not a attribute, try uniform
            handle = glGetUniformLocation(program, name);
        }

        //check handle is now valid
        if (handle == -1)
        {
            throw new IllegalStateException("Cannot find handle for " + name);
        }

        //add handle to cache for faster access next time
        handleCache.put(program + ":" + name, handle);
        return handle;
    }

    /**
     * Update the Filter's FPS logic.
     *
     * @param logFps   if true, the average fps rate is periodically logged
     * @param fpsLimit if > 0, the fps is limited to the desired frame rate (using Thread.sleep)
     */
    protected void updateFpsLogic(boolean logFps, long fpsLimit)
    {
        //get current time and time delta (delta in seconds)
        long tNow = System.currentTimeMillis();

        //limit fps
        if (fpsLimit > 0)
        {
            updateAndLimitFps(tNow, fpsLimit);
        }

        //log fps if enabled
        if (logFps)
        {
            updateAndLogFps(tNow);
        }

        //update time of last frame
        tLastFrame = tNow;
    }

    /**
     * Runs FPS Limiting logic and limits fps to the given target (using Thread.sleep)
     *
     * @param now       the current time (milliseconds)
     * @param targetFps the target fps. better be positive
     */
    protected void updateAndLimitFps(long now, long targetFps)
    {
        //calculate time this frame took in seconds
        long tDeltaMs = now - tLastFrame;

        //calculate target frame time
        long tDeltaTargetMs = 1000 / targetFps;

        //check if frame was faster than target
        if (tDeltaMs < tDeltaTargetMs)
        {
            //calculate time to sleep in ms
            //for some reason it always limited to 2x the targeted fps, so here i am doubling the sleep time...
            long tSleepMs = (tDeltaTargetMs - tDeltaMs) * 2;
            //Logging.logE("delta=" + tDeltaMs + "; target=" + tDeltaTargetMs +"; sleep=" + tSleepMs);

            //sanity check: check if sleep time is ACTUALLY positive and kind of in the right range
            if (tSleepMs > 0 && tSleepMs < 2000)
            {
                //sleep to match target frame time
                try
                {
                    Thread.sleep(tSleepMs, 0);
                }
                catch (InterruptedException ignored)
                {
                    //this is for frame rate limiting, dont care if it fails...
                    //worst case is a higher frame rate :P
                    Logging.logE("FPS Limiter Thread.sleep threw InterruptedException! Not stretching frame time!");
                }
            }
        }
    }

    /**
     * Runs FPS update logic and logs fps every 5 seconds
     *
     * @param now the current time (milliseconds)
     */
    protected void updateAndLogFps(long now)
    {
        //count frames
        fpsFrameCounter++;

        //log fps to console every 5 seconds
        long lastLogDuration = (now - tLastLog);
        if (lastLogDuration >= fpsLogIntervalMs)
        {
            Logging.logD("Average FPS: " + (fpsFrameCounter / (lastLogDuration / 1000f)));
            tLastLog = now;
            fpsFrameCounter = 0;
        }
    }

    /**
     * Setup the vertex buffer
     */
    protected void setupVertexBuffer()
    {
        vertexBuffer = EglUtil.createBuffer(VERTICES_DATA);
    }

    /**
     * Release the vertex buffer
     */
    protected void releaseVertexBuffer()
    {
        glDeleteBuffers(1, new int[]{vertexBuffer}, 0);
        vertexBuffer = 0;
    }
    //endregion

    //region fps logic function parameters
    @SuppressWarnings("unused")
    public void setLogFps(boolean en)
    {
        logFps = en;
    }

    @SuppressWarnings("unused")
    public void setFpsLimit(long limit)
    {
        fpsLimit = limit;
    }

    @SuppressWarnings("unused")
    public boolean getLogFps()
    {
        return logFps;
    }

    @SuppressWarnings("unused")
    public long getFpsLimit()
    {
        return fpsLimit;
    }
    // endregion
}
