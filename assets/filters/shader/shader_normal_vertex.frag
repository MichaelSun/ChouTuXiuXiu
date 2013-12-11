precision mediump float;

uniform vec2 uAspectRatio;
uniform vec2 uAspectRatioPreview;
uniform mat4 uOrientationM;
uniform vec2 uTranslate;
uniform float uScale;

attribute vec2 position;

varying vec2 textureCoordinate;

void main()
{
	gl_Position = vec4(position, 0.0, 1.0);
	gl_Position.xy *= uAspectRatio / uAspectRatioPreview;
	gl_Position.xy *= uScale;
	gl_Position.xy += uTranslate;
	
	textureCoordinate = ((uOrientationM * vec4(position, 0.0, 1.0) + 1.0) * 0.5).xy;
}