// Common GLSL ES Vertex shader for Anime4K Fragment shaders:
// - colorget.fs
// - colorpush.fs
// - gradget.fs
// - gradpush.fs
	
attribute vec4 aPosition;
attribute vec4 aTextureCoord;

// coordinates on the current texture (range 0.0 - 1.0!)
varying highp vec2 vTextureCoord;

void main()
{
	gl_Position = aPosition;
	vTextureCoord = aTextureCoord.xy;
}