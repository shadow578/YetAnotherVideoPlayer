// Anime4K GLSL ES fragment shader
// Stage 1/4: Color GET (also known as Compute Color and Compute Luminance)
// computes luminance and stores in alpha channel

precision mediump float;

// coordinates on the current texture (range 0.0 - 1.0!)
varying highp vec2 vTextureCoord;

// the current texture
uniform lowp sampler2D sTexture;

// the size of the current texture
//uniform highp vec2 vTextureSize;

// push strenght (0.0-1.0)
//uniform float fPushStrength;

float getLuminance(vec4 c)
{
	const vec3 W = vec3(0.2125, 0.7154, 0.0721);
    return dot(c.rgb, W);
}

void main()
{
    // get color on texture at current position
    vec4 c = texture2D(sTexture, vTextureCoord);

    // calculate luminance
    float pxLuminance = getLuminance(c);

    // clamp to range 0 - 1
    pxLuminance = clamp(pxLuminance, 0.0, 1.0);

    // set current fragment color
    gl_FragColor = vec4(c.rgb, pxLuminance);
}