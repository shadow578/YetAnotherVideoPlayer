package de.shadow578.yetanothervideoplayer.gl;

import com.daasuu.epf.EFramebufferObject;
import com.daasuu.epf.EglUtil;
import com.daasuu.epf.filter.GlFilter;

//allow direct use of gl functions
import java.util.HashMap;

import static android.opengl.GLES20.*;

import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * GLFilter class that applies more than one shader
 * TESTING CLASS DO NOT USE
 *
 * @deprecated DO NOT USE THIS CLASS IN ANY PRODUCTIVE BUILD, IT IS ONLY FOR TESTIN!!
 */
public class GLMultiFilterTEST extends GlFilter
{
    // region Test GLSL Shaders (A removes BLUE, B removes GREEN)
    //TEST_A removes BLUE channel
    static final String VERTEX_TEST_A =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "gl_Position = aPosition;\n" +
                    "vTextureCoord = aTextureCoord.xy;\n" +
                    "}\n";

    static final String FRAGMENT_TEST_A =
            "precision mediump float;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "uniform lowp sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "vec4 c = texture2D(sTexture, vTextureCoord);\n" +
                    "gl_FragColor = vec4(c.r, c.g, 0, c.a); \n" +
                    "}\n";

    //TEST_B removes GREEN channel
    static final String VERTEX_TEST_B =
            "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "gl_Position = aPosition;\n" +
                    "vTextureCoord = aTextureCoord.xy;\n" +
                    "}\n";

    static final String FRAGMENT_TEST_B =
            "precision mediump float;\n" +
                    "varying highp vec2 vTextureCoord;\n" +
                    "uniform lowp sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "vec4 c = texture2D(sTexture, vTextureCoord);\n" +
                    "gl_FragColor = vec4(c.r, 0, c.b, c.a); \n" +
                    "}\n";
    // endregion

    private static final float[] VERTICES_DATA = new float[]{
            // X, Y, Z, U, V
            -1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 0.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 1.0f, 0.0f
    };

    private final HashMap<String, Integer> handleMap = new HashMap<String, Integer>();

    private int vertexShaderA, vertextShaderB, fragmentShaderA, fragmentShaderB, glProgramA, glProgramB;
    private int vertexBufferName;
    EFramebufferObject tmpFrameBuffer;

    @Override
    public void setFrameSize(int width, int height)
    {
        super.setFrameSize(width, height);
        Logging.logD("Frame size set to " + width + "x " + height);

        //setup the frame buffer for the new resolution
        if (tmpFrameBuffer == null)
            tmpFrameBuffer = new EFramebufferObject();
        tmpFrameBuffer.setup(width, height);
    }

    @Override
    public void setup()
    {
        Logging.logD("Setup Multi Filter");

        //setup shader a
        vertexShaderA = EglUtil.loadShader(VERTEX_TEST_A, GL_VERTEX_SHADER);
        fragmentShaderA = EglUtil.loadShader(FRAGMENT_TEST_A, GL_FRAGMENT_SHADER);
        glProgramA = EglUtil.createProgram(vertexShaderA, fragmentShaderA);

        //setup shader b
        vertextShaderB = EglUtil.loadShader(VERTEX_TEST_B, GL_VERTEX_SHADER);
        fragmentShaderB = EglUtil.loadShader(FRAGMENT_TEST_B, GL_FRAGMENT_SHADER);
        glProgramB = EglUtil.createProgram(vertextShaderB, fragmentShaderB);

        //create vertex buffer
        vertexBufferName = EglUtil.createBuffer(VERTICES_DATA);

        //create a frame buffer to draw to temporarily
        if (tmpFrameBuffer == null)
            tmpFrameBuffer = new EFramebufferObject();
    }

    /**
     * @param toDraw the texture to draw
     * @param drawTo the frame buffer to draw to. is already active framebuffer when this function is called
     */
    @Override
    public void draw(int toDraw, EFramebufferObject drawTo)
    {
        //draw texture to tmpBuffer using program a
        drawUsingProgram(glProgramA, toDraw, tmpFrameBuffer);

        //draw tmpBuffer to render target using program B
        drawUsingProgram(glProgramB, tmpFrameBuffer.getTexName(), drawTo);
    }

    @Override
    public void release()
    {
        //might be a good idea to release gl stuff here :P
        //super.release();
    }

    /**
     * Draw a texture to a frame buffer using a gl program
     *
     * @param program    the program to use for drawing
     * @param texToDraw  the texture to draw
     * @param drawTarget the frame buffer to draw to
     */
    private void drawUsingProgram(int program, int texToDraw, EFramebufferObject drawTarget)
    {
        //set drawTarget frame buffer as render target
        drawTarget.enable();

        //use the wanted program
        glUseProgram(program);

        //setup gl vertex attributes
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferName);
        glEnableVertexAttribArray(getAttrHandle("aPosition", program));
        glVertexAttribPointer(getAttrHandle("aPosition", program), VERTICES_DATA_POS_SIZE, GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_POS_OFFSET);
        glEnableVertexAttribArray(getAttrHandle("aTextureCoord", program));
        glVertexAttribPointer(getAttrHandle("aTextureCoord", program), VERTICES_DATA_UV_SIZE, GL_FLOAT, false, VERTICES_DATA_STRIDE_BYTES, VERTICES_DATA_UV_OFFSET);

        //bind the texture to draw
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texToDraw);
        glUniform1i(getAttrHandle("sTexture", program), 0);

        //draw a textured quad using the bound texture
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        //cleanup
        glDisableVertexAttribArray(getAttrHandle("aPosition", program));
        glDisableVertexAttribArray(getAttrHandle("aTextureCoord", program));
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private int getAttrHandle(String name, int program) {
        final Integer value = handleMap.get(name);
        if (value != null) {
            return value.intValue();
        }

        int location = glGetAttribLocation(program, name);
        if (location == -1) {
            location = glGetUniformLocation(program, name);
        }
        if (location == -1) {
            throw new IllegalStateException("Could not get attrib or uniform location for " + name);
        }
        handleMap.put(name, Integer.valueOf(location));
        return location;
    }
}
