package de.shadow578.yetanothervideoplayer.gl;

import com.daasuu.epf.EFramebufferObject;
import com.daasuu.epf.EglUtil;

import de.shadow578.yetanothervideoplayer.util.Logging;

//allow direct use of gl functions
import static android.opengl.GLES20.*;

/**
 * Anime4K filter for ExoPlayerFilter library, implemented in GLSL ES (for OpenGL ES)
 */
public class GLAnime4K extends GLFilterBase
{
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

    /**
     * Set this filter up for use
     */
    @Override
    public void setup()
    {
        //load shader sources
        final String srcColorGet = DEFAULT_FRAGMENT_SHADER;
        final String srcColorPush = DEFAULT_FRAGMENT_SHADER;
        final String srcGradientGet = DEFAULT_FRAGMENT_SHADER;
        final String srcGradientPush = DEFAULT_FRAGMENT_SHADER;

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
     * @param sourceTexture the texture to draw
     * @param target        the frame buffer to draw to. is already active framebuffer when this function is called
     */
    @Override
    public void draw(int sourceTexture, EFramebufferObject target)
    {
        //render x passes of anime4k
        for (int pass = 0; pass < 2; pass++)
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
}
