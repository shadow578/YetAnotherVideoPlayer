package de.shadow578.yetanothervideoplayer.gl;

import android.content.Context;

import com.daasuu.epf.EFramebufferObject;
import com.daasuu.epf.EglUtil;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.shadow578.yetanothervideoplayer.util.Logging;

//allow direct use of gl functions
import static android.opengl.GLES20.*;

/**
 * Anime4K filter for ExoPlayerFilter library, implemented in GLSL ES (for OpenGL ES)
 * Implements ExoPlayer's VideoListener to get real video resolution and automatically adjust push strength
 */
public class GLAnime4K extends GLFilterBase implements VideoListener
{
    //region Shader Variables
    //shader sources
    private final String srcCommonVertex, srcColorGet, srcColorPush, srcGradientGet, srcGradientPush;

    //all shaders share the same vertex shader (only doing stuff in fragment shaders)
    private int commonVertexShader;

    //color GET shader
    private int colorGetFragmentShader, colorGetProgram;

    //color PUSH shader
    private int colorPushFragmentShader, colorPushProgram;

    //gradient GET shader
    private int gradientGetFragmentShader, gradientGetProgram;

    //gradient PUSH shader
    private int gradientPushFragmentShader, gradientPushProgram;

    //buffer for rendering of filters
    private EFramebufferObject buffer;
    //endregion

    //resolution of render surface and video
    private int renderWidth, renderHeight, videoWidth, videoHeight;

    //how many passes are executed (more = slower)
    private int a4kPasses = 2;

    //the strength of anime4k push operations
    private float a4kColorPushStrength = 0.33f;
    private float a4kGradPushStrength = 0.33f;

    //should push strength be auto- adjusted based on real video resolution?
    //set by setPushStrength function
    private boolean enableAutoPushStrength = true;

    public GLAnime4K(Context ctx, int resComVertex, int resColorGet, int resColorPush, int resGradGet, int resGradPush)
    {
        srcCommonVertex = readShaderRes(ctx, resComVertex);
        srcColorGet = readShaderRes(ctx, resColorGet);
        srcColorPush = readShaderRes(ctx, resColorPush);
        srcGradientGet = readShaderRes(ctx, resGradGet);
        srcGradientPush = readShaderRes(ctx, resGradPush);
    }

    //region GLFilter code

    /**
     * Set this filter up for use
     */
    @Override
    public void setup()
    {
        //setup shaders:
        //common vertex shader (shared between all programs)
        commonVertexShader = EglUtil.loadShader(srcCommonVertex, GL_VERTEX_SHADER);

        //color get
        colorGetFragmentShader = EglUtil.loadShader(srcColorGet, GL_FRAGMENT_SHADER);
        colorGetProgram = EglUtil.createProgram(commonVertexShader, colorGetFragmentShader);

        //color push
        colorPushFragmentShader = EglUtil.loadShader(srcColorPush, GL_FRAGMENT_SHADER);
        colorPushProgram = EglUtil.createProgram(commonVertexShader, colorPushFragmentShader);

        //gradient get
        gradientGetFragmentShader = EglUtil.loadShader(srcGradientGet, GL_FRAGMENT_SHADER);
        gradientGetProgram = EglUtil.createProgram(commonVertexShader, gradientGetFragmentShader);

        //gradient push
        gradientPushFragmentShader = EglUtil.loadShader(srcGradientPush, GL_FRAGMENT_SHADER);
        gradientPushProgram = EglUtil.createProgram(commonVertexShader, gradientPushFragmentShader);

        //create vertex buffer
        setupVertexBuffer();

        //log setup step
        Logging.logD("[A4K] setup shader finished.");
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
        Logging.logD("[A4K] RENDER size to " + width + " x " + height);

        //update frame buffer w+h
        if (buffer == null) buffer = new EFramebufferObject();
        buffer.setup(width, height);

        //set render width + height for auto push strength
        renderWidth = width;
        renderHeight = height;
        autoAdjustPushStrength();
    }

    /**
     * Draw frame using Anime4k shaders
     *
     * @param sourceTexture the texture to draw
     * @param target        the frame buffer to draw to. is already active framebuffer when this function is called
     */
    @Override
    public void draw(int sourceTexture, EFramebufferObject target)
    {
        //render x passes of anime4k
        for (int pass = 0; pass < a4kPasses; pass++)
        {
            //get color
            if (pass == 0)
            {
                //first pass, sourceTexture -> buffer
                drawUsingProgram(colorGetProgram, sourceTexture, buffer);
            }
            else
            {
                //already had one pass, target -> buffer
                drawUsingProgram(colorGetProgram, target.getTexName(), buffer);
            }

            //push color
            //buffer -> target
            drawUsingProgram(colorPushProgram, buffer.getTexName(), target);

            //get gradient
            //target -> buffer
            drawUsingProgram(gradientGetProgram, target.getTexName(), buffer);

            //push gradient
            //buffer -> target
            drawUsingProgram(gradientPushProgram, buffer.getTexName(), target);
        }

        //count + limit fps
        //updateFpsLogic(logFps, fpsLimit);
        updateFpsLogic(true, fpsLimit);
    }

    /**
     * Sets the following uniforms depending on program:
     * -the size of the current texture
     * uniform highp vec2 vTextureSize;
     * <p>
     * -push strenght (0.0-1.0)
     * uniform float fPushStrength;
     * <p>
     * uniform          colorget    colorpush   gradget     gradpush
     * vTextureSize     N           Y           Y           Y
     * fPushStrength    N           Y(col)      N           Y(grad)
     * <p>
     * have to do this depending on program since opengl will optimize away unused uniforms
     *
     * @param program the program that is used for drawing
     */
    @Override
    protected void setCustomUniforms(int program)
    {
        //set value of vTextureSize
        if (program != colorGetProgram)
        {
            //every program except color GET has vTextureSize uniform
            glUniform2f(getGlHandle(program, "vTextureSize"), buffer.getWidth(), buffer.getHeight());
        }

        //set value of fPushStrength
        if (program == colorPushProgram)
        {
            //color PUSH program has fPushStrength uniform that translates to color push strength
            glUniform1f(getGlHandle(program, "fPushStrength"), a4kColorPushStrength);
        }

        if (program == gradientPushProgram)
        {
            //gradient PUSH program has fPushStrength uniform that translates to grad push strength
            glUniform1f(getGlHandle(program, "fPushStrength"), a4kGradPushStrength);
        }
    }

    /**
     * Release Gl Resources of this filter
     */
    @Override
    public void release()
    {
        //delete shaders
        glDeleteProgram(colorGetProgram);
        glDeleteProgram(colorPushProgram);
        glDeleteProgram(gradientGetProgram);
        glDeleteProgram(gradientPushProgram);

        glDeleteShader(colorGetFragmentShader);
        glDeleteShader(colorPushFragmentShader);
        glDeleteShader(gradientGetFragmentShader);
        glDeleteShader(gradientPushFragmentShader);

        glDeleteShader(commonVertexShader);

        //set handles to 0
        colorGetProgram = 0;
        colorPushProgram = 0;
        gradientGetProgram = 0;
        gradientPushProgram = 0;

        colorGetFragmentShader = 0;
        colorPushFragmentShader = 0;
        gradientGetFragmentShader = 0;
        gradientPushFragmentShader = 0;

        commonVertexShader = 0;

        //release the vertex buffer
        releaseVertexBuffer();

        //log release
        Logging.logD("[A4K] Released shader.");
    }

    /**
     * Read a fragment shader source from res/raw
     *
     * @param ctx the context to read in
     * @param res the resource id in res/raw
     * @return the loaded shader source. When fails, defaults to DEFAULT_FRAGMENT_SHADER
     */
    private String readShaderRes(Context ctx, int res)
    {
        try
        {
            StringBuilder shaderSrc = new StringBuilder();
            InputStream resStream = ctx.getResources().openRawResource(res);
            BufferedReader reader = new BufferedReader(new InputStreamReader(resStream));
            String ln = reader.readLine();
            while (ln != null)
            {
                shaderSrc.append(ln).append("\n");
                ln = reader.readLine();
            }

            reader.close();
            resStream.close();
            //Logging.logD(shaderSrc.toString());
            return shaderSrc.toString();
        }
        catch (IOException e)
        {
            Logging.logE("[A4K] Error loading shader source from Res ID " + res + "! Exception: " + e.toString());
            return DEFAULT_FRAGMENT_SHADER;
        }
    }
    //endregion

    //region ExoPlayer Video Listener

    /**
     * Exoplayer video listener function.
     * Called when the video resolution (real, not scaled) changes.
     * This is required to adjust the push strength according to the scaling factor of the video.
     *
     * @param width                    the new width of the video
     * @param height                   the new height of the video
     * @param unappliedRotationDegrees rotation of the video (not used)
     * @param pixelWidthHeightRatio    aspect ratio of the video's pixels
     */
    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio)
    {
        Logging.logD("[A4K] VIDEO size changed to " + width + " x " + height);

        //set video resolution
        videoWidth = width;
        videoHeight = height;
        autoAdjustPushStrength();
    }
    //endregion

    //region auto- push strength

    /**
     * Auto- adjust the push strength based on render resolution and video resolution
     * does nothing if enableAutoPushStrength is false
     */
    private void autoAdjustPushStrength()
    {
        //abort if render OR video resolution are not yet set
        if (renderWidth <= 0 || renderHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return;

        //abort if auto push strength is disabled
        if (!enableAutoPushStrength) return;

        //calculate scaling factor based on resolution of render surface and video
        float wScale = renderWidth / videoWidth;
        float hScale = renderHeight / videoHeight;
        float scale = (wScale + hScale) / 2f;

        //calculate push factors based on scale
        a4kColorPushStrength = clamp(scale / 6f, 0f, 1f);
        a4kGradPushStrength = clamp(scale / 2f, 0f, 1f);

        //log the change
        Logging.logD("[A4K] Auto- Adjusting Push Strength to COL= %.2f and GRAD= %.2f. (RENDER: %d x %d; VIDEO: %d x %d)", a4kColorPushStrength, a4kGradPushStrength, renderWidth, renderHeight, videoWidth, videoHeight);
    }

    /**
     * Clamp a value between min and max
     *
     * @param a   the value to clamp
     * @param min the minimum value
     * @param max the maximum value
     * @return the clamped value
     */
    @SuppressWarnings("SameParameterValue")
    private float clamp(float a, float min, float max)
    {
        if (a < min) return min;
        if (a > max) return max;
        return a;
    }

    //endregion

    //region Parameter Interface

    /**
     * Set Anime4K passes count
     *
     * @param passes a4kPasses
     */
    @SuppressWarnings("unused")
    public void setPasses(int passes)
    {
        a4kPasses = passes;
    }

    /**
     * Get Anime4K passes count
     *
     * @return a4kPasses
     */
    @SuppressWarnings("unused")
    public int getPasses()
    {
        return a4kPasses;
    }

    /**
     * enable/disable auto push strength state
     *
     * @param en enable auto push strength?
     */
    @SuppressWarnings("unused")
    public void setEnableAutoPushStrength(boolean en)
    {
        enableAutoPushStrength = en;
    }

    /**
     * Manually Set Anime4K color push strength
     * Disables automatic adjustment of push strength.
     *
     * @param pushStrength a4kColorPushStrength
     */
    @SuppressWarnings("unused")
    public void setColorPushStrength(float pushStrength)
    {
        a4kColorPushStrength = pushStrength;
        enableAutoPushStrength = false;
    }

    /**
     * Get Anime4K color push strenght
     *
     * @return a4kColorPushStrength
     */
    @SuppressWarnings("unused")
    public float getColorPushStrength()
    {
        return a4kColorPushStrength;
    }

    /**
     * Manually Set Anime4K gradient push strength
     * Disables automatic adjustment of push strength.
     *
     * @param pushStrength a4kGradPushStrength
     */
    @SuppressWarnings("unused")
    public void setGradientPushStrength(float pushStrength)
    {
        a4kGradPushStrength = pushStrength;
        enableAutoPushStrength = false;
    }

    /**
     * Get Anime4K push strenght
     *
     * @return a4kGradPushStrength
     */
    @SuppressWarnings("unused")
    public float getGradientPushStrength()
    {
        return a4kGradPushStrength;
    }
    //endregion
}
