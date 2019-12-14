package de.shadow578.yetanothervideoplayer.gl;

import android.content.Context;

import com.daasuu.epf.EFramebufferObject;
import com.daasuu.epf.EglUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.shadow578.yetanothervideoplayer.util.Logging;

//allow direct use of gl functions
import static android.opengl.GLES20.*;

/**
 * Anime4K filter for ExoPlayerFilter library, implemented in GLSL ES (for OpenGL ES)
 */
public class GLAnime4K extends GLFilterBase
{
    //shader sources
    private final String srcColorGet, srcColorPush, srcGradientGet, srcGradientPush;

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

    //how many passes are executed (more = slower)
    private int a4kPasses = 1;

    //the strength of anime4k push operations
    private float a4kPushStrength = 0.33f;

    public GLAnime4K(Context ctx, int resColorGet, int resColorPush, int resGradGet, int resGradPush)
    {
        srcColorGet = readShaderRes(ctx, resColorGet);
        srcColorPush = readShaderRes(ctx, resColorPush);
        srcGradientGet = readShaderRes(ctx, resGradGet);
        srcGradientPush = readShaderRes(ctx, resGradPush);
    }

    /**
     * Set this filter up for use
     */
    @Override
    public void setup()
    {
        //setup shaders:
        //common vertex shader (shared between all programs)
        commonVertexShader = EglUtil.loadShader(DEFAULT_VERTEX_SHADER, GL_VERTEX_SHADER);

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
        Logging.logD("Set frame size to " + width + " x " + height);

        //update frame buffer w+h
        if (buffer == null) buffer = new EFramebufferObject();
        buffer.setup(width, height);
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

        //count fps
        updateAnLogFps();
    }

    /**
     * Sets the following uniforms in every program:
     * -the size of the current texture
     * uniform highp vec2 vTextureSize;
     * <p>
     * -push strenght (0.0-1.0)
     * uniform float fPushStrength;
     *
     * @param program the program that is used for drawing
     */
    @Override
    protected void setCustomUniforms(int program)
    {
        //get handles for vTextureSize and fPushStrenght
        int hndTextureSize = getGlHandle(program, "vTextureSize");
        int hndPushStrength = getGlHandle(program, "fPushStrength");

        //set values of uniforms
        glUniform2f(hndTextureSize, buffer.getWidth(), buffer.getHeight());
        glUniform1f(hndPushStrength, a4kPushStrength);
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
        Logging.logD("Released shader.");
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
            return shaderSrc.toString();
        }
        catch (IOException e)
        {
            Logging.logE("Error loading shader source from Res ID " + res + "! Exception: " + e.toString());
            return DEFAULT_FRAGMENT_SHADER;
        }
    }

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
     * Set Anime4K push strength
     *
     * @param pushStrength a4kPushStrenght
     */
    @SuppressWarnings("unused")
    public void setPushStrength(float pushStrength)
    {
        a4kPushStrength = pushStrength;
    }

    /**
     * Get Anime4K push strenght
     *
     * @return a4kPushStrenght
     */
    @SuppressWarnings("unused")
    public float getPushStrength()
    {
        return a4kPushStrength;
    }
}
