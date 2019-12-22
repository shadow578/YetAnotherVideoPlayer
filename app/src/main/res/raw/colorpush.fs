// Anime4K GLSL ES fragment shader
// Stage 2/4: Color PUSH
// pushes color based on luminance in alpha channel.

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

float max3c(vec4 a, vec4 b, vec4 c)
{
	return max(max(a.a, b.a), c.a);
}

float min3c(vec4 a, vec4 b, vec4 c)
{
	return min(min(a.a, b.a), c.a);
}

vec4 getLargest(vec4 cc, vec4 lightest, vec4 a, vec4 b, vec4 c)
{
	//use vertex calculation instead of manual per- component calculation (C# does not have something like this)
	vec4 nc = (cc * (1.0 - fPushStrength)) + (((a + b + c) / 3.0) * fPushStrength);
	
	if(nc.a > lightest.a)
	{
		return nc;
	}
	
	return lightest;
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
	highp float maxD = max3c(br, bc, bl);
	highp float minL = min3c(tl, tc, tr);
	
	if(minL > mc.a && minL > maxD)
	{
		lightest = getLargest(mc, lightest, tl, tc, tr);
	}
	else
	{
		maxD = max3c(tl, tc, tr);
		minL = min3c(br, bc, bl);
		
		if(minL > mc.a && minL > maxD)
		{
			lightest = getLargest(mc, lightest, br, bc, bl);
		}
	}
	
	// Kernel 1+5
	maxD = max3c(mc, ml, bc);
	minL = min3c(mr, tc, tr);
	
	if(minL > maxD)
	{
		lightest = getLargest(mc, lightest, mr, tc, tr);
	}
	else
	{
		maxD = max3c(mc, mr, tc);
		minL = min3c(bl, ml, bc);
		
		if(minL > maxD)
		{
			lightest = getLargest(mc, lightest, bl, ml, bc);
		}
	}
	
	// Kernel 2+6
	maxD = max3c(ml, tl, bl);
	minL = min3c(mr, br, tr);
	
	if(minL > mc.a && minL > maxD)
	{
		lightest = getLargest(mc, lightest, mr, br, tr);
	}
	else
	{
		maxD = max3c(mr, br, tr);
		minL = min3c(ml, tl, bl);
		
		if(minL > mc.a && minL > maxD)
		{
			lightest = getLargest(mc, lightest, ml, tl, bl);
		}
	}
	
	// Kernel 3+7
	maxD = max3c(mc, ml, tc);
	minL = min3c(mr, br, bc);
	
	if(minL > maxD)
	{
		lightest = getLargest(mc, lightest, mr, br, bc);
	}
	else
	{
		maxD = max3c(mc, mr, bc);
		minL = min3c(tc, ml, tl);
		
		if(minL > maxD)
		{
			lightest = getLargest(mc, lightest, tc, ml, tl);
		}
	}

	// set pixel
	gl_FragColor = lightest;
}