// Anime4K GLSL ES fragment shader
// Stage 3/4: Gradient GET (also known as Compute Gradient)
// computes gradient (edges) and stores into alpha channel

// DEMO_MODE skips processing the RIGHT half of the screen completely
#define DEMO_MODE false


precision mediump float;

// coordinates on the current texture (range 0.0 - 1.0!)
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

vec2 transCoord(vec2 texCoord, vec2 pxAmount)
{
	//normalize pxAmount (range 0 - width OR 0 - height to range 0-1)
	vec2 texAmount = vec2(pxAmount.x / vTextureSize.x, pxAmount.y / vTextureSize.y);
	
	//translate given texture coordinates
	return texCoord + texAmount;
}

vec4 sampleTexture(vec2 texCoord, vec2 pxOffset)
{
	return texture2D(sTexture, transCoord(texCoord, pxOffset));
}

void main()
{
	// DEMO mode: only apply for half of the screen
	if(DEMO_MODE)
	{
		if(vTextureCoord.x > 0.5) 
		{
			//skip processing right side of screen
			gl_FragColor = texture2D(sTexture, vTextureCoord);
			return;
		}
	}

	// get color on texture at current position
	vec4 mc = sampleTexture(vTextureCoord, vec2(0.0,  0.0));

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
	vec4 tl = sampleTexture(vTextureCoord, vec2(-1.0, -1.0));
	vec4 tc = sampleTexture(vTextureCoord, vec2( 0.0, -1.0));
	vec4 tr = sampleTexture(vTextureCoord, vec2( 1.0, -1.0));

	// center
	vec4 ml = sampleTexture(vTextureCoord, vec2(-1.0, 0.0));
	//vec4 mc = sampleTexture(vTextureCoord, vec2(0.0,  0.0));
	vec4 mr = sampleTexture(vTextureCoord, vec2( 1.0, 0.0));

	// bottom
	vec4 bl = sampleTexture(vTextureCoord, vec2(-1.0, 1.0));
	vec4 bc = sampleTexture(vTextureCoord, vec2( 0.0, 1.0));
	vec4 br = sampleTexture(vTextureCoord, vec2( 1.0, 1.0));
	
	
	// Using function from Anime4K 0.9:
	//Horizontal Gradient
	//[-1  0  1]
	//[-2  0  2]
	//[-1  0  1]
	float xGrad = (-tl.a + tr.a - ml.a - ml.a + mr.a + mr.a - bl.a + br.a);
	
	//Vertical Gradient
	//[-1 -2 -1]
	//[ 0  0  0]
	//[ 1  2  1]
	float yGrad = (-tl.a - tc.a - tc.a - tr.a + bl.a + bc.a + bc.a + br.a);
	
	//Computes the luminance's gradient and saves it in the unused alpha channel
	float lum = 1.0 - clamp(sqrt(xGrad * xGrad + yGrad * yGrad), 0.0, 1.0);
	
	gl_FragColor = vec4(mc.rgb, lum);
		
	// Using Sobel function:
	// create matrix with alpha values
	//mat3 sobAlpha = mat3(tl.a, tc.a, tr.a,
	//					 ml.a, mc.a, mr.a,
	//					 bl.a, bc.a, br.a);
	//
	//// run sobel operation on alpha channel
	//float dX = sobel(sobelX, sobAlpha);
	//float dY = sobel(sobelY, sobAlpha);
	//
	//// calculate derivata in highp
	//highp float derivata = sqrt(pow(dX, 2.0) + pow(dY, 2.0));
	//
	//// set pixel based on derivata
	//if(derivata > 1.0)
	//{
	//	gl_FragColor = vec4(mc.rgb, 0.0);
	//}
	//else
	//{
	//	gl_FragColor = vec4(mc.rgb, 1.0 - derivata);
	//}
}