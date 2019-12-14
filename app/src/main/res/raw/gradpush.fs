precision mediump float;

// coordinates on the current texture
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

float average3(float a, float b, float c)
{
	return (a + b + c) / 3.0;
}

vec4 getAverage(vec4 cc, vec4 lightest, vec4 a, vec4 b, vec4 c)
{
	float aR = (cc.r * (1.0 - fPushStrength) + (average3(a.r, b.r, c.r) * fPushStrength)) / 1.0;
	float aG = (cc.g * (1.0 - fPushStrength) + (average3(a.g, b.g, c.g) * fPushStrength)) / 1.0;
	float aB = (cc.b * (1.0 - fPushStrength) + (average3(a.b, b.b, c.b) * fPushStrength)) / 1.0;
	float aA = (cc.a * (1.0 - fPushStrength) + (average3(a.a, b.a, c.a) * fPushStrength)) / 1.0;
	
	return vec4(aR, aG, aB, aA);
}

void main()
{
    // Kernel defination:
	// [tl][tc][tr]
	// [ml][mc][mr]
	// [bl][bc][br]

	// kernel setup:
	// set translation constants
	float xNeg = -1.0;
	float xPro = 1.0;
	float yNeg = -1.0;
	float yPro = 1.0;
	if(vTextureCoord.x <= 0.0)
	{
		xNeg = 0.0;
	}
	
	if(vTextureCoord.x >= vTextureSize.x)
	{
		xPro = 0.0;
	}

	if(vTextureCoord.y <= 0.0)
	{
		yNeg = 0.0;
	}

	if(vTextureCoord.y >= vTextureSize.y)
	{
		yPro = 0.0;
	}
	
	// get colors:
	// top
	vec4 tl = texture2D(sTexture, vTextureCoord + vec2(xNeg, yNeg));
	vec4 tc = texture2D(sTexture, vTextureCoord + vec2(0.0,  yNeg));
	vec4 tr = texture2D(sTexture, vTextureCoord + vec2(xPro, yNeg));

	// center
	vec4 ml = texture2D(sTexture, vTextureCoord + vec2(xNeg, 0.0));
	vec4 mc = texture2D(sTexture, vTextureCoord);
	vec4 mr = texture2D(sTexture, vTextureCoord + vec2(xPro, 0.0));

	// bottom
	vec4 bl = texture2D(sTexture, vTextureCoord + vec2(xNeg, yPro));
	vec4 bc = texture2D(sTexture, vTextureCoord + vec2(0.0,  yPro));
	vec4 br = texture2D(sTexture, vTextureCoord + vec2(xPro, yPro));

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
	// also reset alpha channel since it is no longer needed
	gl_FragColor = vec4(lightest.rgb, 1.0);
}