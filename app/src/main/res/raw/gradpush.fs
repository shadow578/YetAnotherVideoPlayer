// Anime4K GLSL ES fragment shader
// Stage 4/4: Gradient PUSH
// pushes gradient based on gradient information in alpha channel.

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
uniform float fPushStrength;

float max3(float a, float b, float c)
{
	return max(max(a, b), c);
}

float min3(float a, float b, float c)
{
	return min(min(a, b), c);
}

vec4 getAverage(vec4 cc, vec4 lightest, vec4 a, vec4 b, vec4 c)
{
	//use vertex calculation instead of manual per- component calculation (C# does not have something like this)
	return (cc * (1.0 - fPushStrength)) + ((a + b + c) / 3.0) * fPushStrength;
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
		if(vTextureCoord.x > 0.499 && vTextureCoord.x < 0.501)
		{
			// draw black line in center (also to hide artifacts)
			gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
			return;
		}
		else if(vTextureCoord.x > 0.5) 
		{
			// skip processing right side of screen
			gl_FragColor = texture2D(sTexture, vTextureCoord);
			return;
		}
	}

    // Kernel defination:
	// [tl][tc][tr]
	// [ml][mc][mr]
	// [bl][bc][br]

	// kernel setup:
	// top
	vec4 tl = sampleTexture(vTextureCoord, vec2(-1.0, -1.0));
	vec4 tc = sampleTexture(vTextureCoord, vec2( 0.0, -1.0));
	vec4 tr = sampleTexture(vTextureCoord, vec2( 1.0, -1.0));

	// center
	vec4 ml = sampleTexture(vTextureCoord, vec2(-1.0, 0.0));
	vec4 mc = sampleTexture(vTextureCoord, vec2( 0.0, 0.0));
	vec4 mr = sampleTexture(vTextureCoord, vec2( 1.0, 0.0));

	// bottom
	vec4 bl = sampleTexture(vTextureCoord, vec2(-1.0, 1.0));
	vec4 bc = sampleTexture(vTextureCoord, vec2( 0.0, 1.0));
	vec4 br = sampleTexture(vTextureCoord, vec2( 1.0, 1.0));

	// default lightest color to center
	vec4 lightest = mc;
		
	// Kernel 0+4
	float maxD = max3(br.a, bc.a, bl.a);
	float minL = min3(tl.a, tc.a, tr.a);
	
	if(minL > mc.a && minL > maxD)
	{
		lightest = getAverage(mc, lightest, tl, tc, tr);
	}
	else
	{
		maxD = max3(tl.a, tc.a, tr.a);
		minL = min3(br.a, bc.a, bl.a);
		
		if(minL > mc.a && minL > maxD)
		{
			lightest = getAverage(mc, lightest, br, bc, bl);
		}
	}
	
	// Kernel 1+5
	maxD = max3(mc.a, ml.a, bc.a);
	minL = min3(mr.a, tc.a, tr.a);
	
	if(minL > maxD)
	{
		lightest = getAverage(mc, lightest, mr, tc, tr);
	}
	else
	{
		maxD = max3(mc.a, mr.a, tc.a);
		minL = min3(bl.a, ml.a, bc.a);
		
		if(minL > maxD)
		{
			lightest = getAverage(mc, lightest, bl, ml, bc);
		}
	}
	
	// Kernel 2+6
	maxD = max3(ml.a, tl.a, bl.a);
	minL = min3(mr.a, br.a, tr.a);
	
	if(minL > mc.a && minL > maxD)
	{
		lightest = getAverage(mc, lightest, mr, br, tr);
	}
	else
	{
		maxD = max3(mr.a, br.a, tr.a);
		minL = min3(ml.a, tl.a, bl.a);
		
		if(minL > mc.a && minL > maxD)
		{
			lightest = getAverage(mc, lightest, ml, tl, bl);
		}
	}
	
	// Kernel 3+7
	maxD = max3(mc.a, ml.a, tc.a);
	minL = min3(mr.a, br.a, bc.a);
	
	if(minL > maxD)
	{
		lightest = getAverage(mc, lightest, mr, br, bc);
	}
	else
	{
		maxD = max3(mc.a, mr.a, bc.a);
		minL = min3(tc.a, ml.a, tl.a);
		
		if(minL > maxD)
		{
			lightest = getAverage(mc, lightest, tc, ml, tl);
		}
	}
	
    // set current fragment color
	// resetting alpha not needed since it is ignored anyway.
	gl_FragColor = lightest;
}