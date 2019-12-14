precision mediump float;

// coordinates on the current texture
varying highp vec2 vTextureCoord;

// the current texture
uniform lowp sampler2D sTexture;

// the size of the current texture
uniform highp vec2 vTextureSize;

// push strenght (0.0-1.0)
//uniform float fPushStrength;

// sobel matrices
const mat3 sobelX = mat3(-1,  0,  1, 
						 -2,  0,  2,
						 -1,  0,  1);
						 
const mat3 sobelY = mat3(-1, -2, -1, 
						  0,  0,  0,
						  1,  2,  1);

float sobel(mat3 mat, mat3 col)
{
	return dot(mat[0], col[0]) + dot(mat[1], col[1]) + dot(mat[2], col[2]);
}

void main()
{
	// get color on texture at current position
    vec4 mc = texture2D(sTexture, vTextureCoord);

	// skip first & last row & collumn
	if(vTextureCoord.x == 0.0 || vTextureCoord.y == 0.0 
	   || vTextureCoord.x == (vTextureSize.x - 1.0)
	   || vTextureCoord.y == (vTextureSize.y - 1.0))
	{
		gl_FragColor = mc;
		return;
	}
	
	// create sobel matrix:
	// [tl][tc][tr]
	// [ml][mc][mr]
	// [bl][bc][br]
	// top
	vec4 tl = texture2D(sTexture, vTextureCoord + vec2(-1.0, -1.0));
	vec4 tc = texture2D(sTexture, vTextureCoord + vec2(0.0,  -1.0));
	vec4 tr = texture2D(sTexture, vTextureCoord + vec2(1.0,  -1.0));

	// center
	vec4 ml = texture2D(sTexture, vTextureCoord + vec2(-1.0, 0.0));
	//vec4 mc = texture2D(sTexture, vTextureCoord);
	vec4 mr = texture2D(sTexture, vTextureCoord + vec2(1.0,  0.0));

	// bottom
	vec4 bl = texture2D(sTexture, vTextureCoord + vec2(-1.0, 1.0));
	vec4 bc = texture2D(sTexture, vTextureCoord + vec2(0.0,  1.0));
	vec4 br = texture2D(sTexture, vTextureCoord + vec2(1.0,  1.0));
	
	// create matrix with alpha values
	mat3 sobAlpha = mat3(tl.a, tc.a, tr.a,
						 ml.a, mc.a, mr.a,
						 bl.a, bc.a, br.a);
	
	// run sobel operation on alpha channel
	float dX = sobel(sobelX, sobAlpha);
	float dY = sobel(sobelY, sobAlpha);
	
	// calculate derivata in highp
	highp float derivata = sqrt(pow(dX, 2.0) + pow(dY, 2.0));
	
	// set pixel based on derivata
	if(derivata > 1.0)
	{
		gl_FragColor = vec4(mc.rgb, 0.0);
	}
	else
	{
		gl_FragColor = vec4(mc.rgb, 1.0 - derivata);
	}
}