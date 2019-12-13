precision mediump float;

//coordinates on the current texture
varying highp vec2 vTextureCoord;

//the current texture
uniform lowp sampler2D sTexture;

void main()
{
    //get color on texture at current position
    vec4 c = texture2D(sTexture, vTextureCoord);

    //set current fragment color
    gl_FragColor = c.rgba;
}