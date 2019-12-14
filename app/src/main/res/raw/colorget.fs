precision mediump float;

// coordinates on the current texture
varying highp vec2 vTextureCoord;

// the current texture
uniform lowp sampler2D sTexture;

// the size of the current texture
uniform highp vec2 vTextureSize;

// push strenght (0.0-1.0)
uniform float fPushStrength;

float getLuminance(vec4 c)
{
    float mx = c.r;
    float mn = c.r;

    if(c.g > mx) mx = c.g;
    if(c.b > mx) mx = c.b;

    if(c.g < mn) mn = c.g;
    if(c.b < mn) mn = c.b;

    return (mx + mn) / 2.0;
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