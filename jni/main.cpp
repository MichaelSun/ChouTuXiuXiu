#include "main.h"
#include "rrimagelib.h"

#include <string>
#include <map>

GLuint gOesProgramHandler = -1;
GLuint gNormalProgramHandler = -1;

int processExifThumnail(const char *inFileName, ExifData *ed) {
	if (!inFileName || !ed) {
		return 0;
	}

	int thumnailSize = ed->size;
	if (ed->data && thumnailSize) {
		free(ed->data);
		ed->data = NULL;
		ed->size = 0;
	}

	return 1;
}

/**
 * Copy exif information, but set exif orientation to normal
 */
int copyExif(const char *inFileName, const char *outFileName) {
	if (!inFileName || !outFileName) {
		return 0;
	}

	ExifData *ed = exif_data_new_from_file(inFileName);
	if (!ed) {
		return 0;
	}

	ExifByteOrder order = exif_data_get_byte_order(ed);
	ExifEntry *entry = exif_data_get_entry(ed, EXIF_TAG_ORIENTATION);
	if (entry) {
		exif_set_short(entry->data, order, 1);
	}

	processExifThumnail(inFileName, ed);

	exif_data_fix(ed);

	JPEGData *jpegData = jpeg_data_new_from_file(outFileName);
	if (!jpegData) {
		exif_data_unref(ed);
		return 0;
	}

	jpeg_data_set_exif_data(jpegData, ed);
	if (!jpeg_data_save_file(jpegData, outFileName)) {
		jpeg_data_unref(jpegData);
		exif_data_unref(ed);
		return 0;
	}

	jpeg_data_unref(jpegData);
	exif_data_unref(ed);
	LOGD("success copy exif data...");

	return 1;
}

int getRotation(int orientation, int needMirror) {
	int rotation = ROTATE_0;

	if (!needMirror) {
		switch (orientation) {
		case 90:
			rotation = ROTATE_90;
			break;
		case 180:
			rotation = ROTATE_180;
			break;
		case 270:
			rotation = ROTATE_270;
			break;
		default:
			break;
		}
	} else {
		switch (orientation) {
		case 90:
			rotation = FLIP_ROTATE_90;
			break;
		case 180:
			rotation = FLIP_ROTATE_180;
			break;
		case 270:
			rotation = FLIP_ROTATE_270;
			break;
		default:
			rotation = FLIP_ROTATE_0;
			break;
		}
	}

	return rotation;
}

void getOrientationMatrix(float orientationM[], int rotation) {
	if (rotation == ROTATE_90) {
		orientationM[0] = 1.1924881E-8;
		orientationM[1] = -1.0;
		orientationM[4] = 1.0;
		orientationM[5] = 1.1924881E-8;
		orientationM[10] = 1.0;
		orientationM[15] = 1.0;
	} else if (rotation == ROTATE_180) {
		orientationM[0] = -1.0;
		orientationM[1] = -8.742278E-8;
		orientationM[4] = 8.742278E-8;
		orientationM[5] = -1.0;
		orientationM[10] = 1.0;
		orientationM[15] = 1.0;
	} else if (rotation == ROTATE_270) {
		orientationM[0] = -4.371139E-8;
		orientationM[1] = 1.0;
		orientationM[4] = -1.0;
		orientationM[5] = -4.371139E-8;
		orientationM[10] = 1.0;
		orientationM[15] = 1.0;
	} else {
		orientationM[0] = 1.0;
		orientationM[5] = 1.0;
		orientationM[10] = 1.0f;
		orientationM[15] = 1.0;
	}
}

int getExifOrientation(const char *inFileName) {
	ExifData *ed = exif_data_new_from_file(inFileName);
	int orientation = 1;
	if (ed) {
		ExifByteOrder order = exif_data_get_byte_order(ed);
		ExifEntry *entry = exif_data_get_entry(ed, EXIF_TAG_ORIENTATION);
		orientation = (entry ? exif_get_short(entry->data, order) : 1);

		exif_data_fix(ed);
		exif_data_unref(ed);
	}

	return orientation;
}

GLuint loadTexture(GLuint textureId, rrimage* data) {
	if (!data) {
		LOGD("loadTexture: png is null");
		return -1;
	}

	// 有些三通道图片使用RGB模式会出问题，不知道为什么（对齐？），这里统一使用RGBA传入
	GLenum format = GL_RGBA;
	int width = data->width;
	int height = data->height;
	int needAddAlpha = (data->channels == 3);
	unsigned char *p = data->pixels;
	if (needAddAlpha) {
		p = (unsigned char *) malloc(width * height * 4);
		unsigned char *sptr = data->pixels;
		unsigned char *dptr = p;
		int m, n;
		for (m = 0; m < height; m++) {
			for (n = 0; n < width; n++) {
				*dptr++ = *sptr++;
				*dptr++ = *sptr++;
				*dptr++ = *sptr++;
				*dptr++ = 255;
			}
		}
	}

	if (textureId == -1) {
		GLuint textureIds[1];
		glGenTextures(1, textureIds);

		glBindTexture(GL_TEXTURE_2D, textureIds[0]);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA,
				GL_UNSIGNED_BYTE, p);
		textureId = textureIds[0];
	} else {
		glBindTexture(GL_TEXTURE_2D, textureId);
		glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA,
				GL_UNSIGNED_BYTE, p);
	}

	if (needAddAlpha) {
		free(p);
	}

	return textureId;
}

GLuint loadShader(const char *shaderSrc, GLenum type) {
	GLuint shader;
	GLint compiled;

	shader = glCreateShader(type);
	if (shader == 0) {
		return 0;
	}

	glShaderSource(shader, 1, &shaderSrc, NULL);
	glCompileShader(shader);
	glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
	if (!compiled) {
		GLint infoLen = 0;
		glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
		if (infoLen > 1) {
			char* infoLog = (char *) malloc(sizeof(char) * infoLen);
			glGetShaderInfoLog(shader, infoLen, NULL, infoLog);
			LOGD("load shader error: %s, type = %d", infoLog, type);
			free(infoLog);
		}
		glDeleteShader(shader);
		return 0;
	}

	return shader;
}

int useProgram(const char *vertexShaderSource,
		const char *fragmentShaderSource) {
	GLuint vertexShaderHandler = loadShader(vertexShaderSource,
			GL_VERTEX_SHADER);
	GLuint fragmentShaderHandler = loadShader(fragmentShaderSource,
			GL_FRAGMENT_SHADER);
	GLuint programHandler = glCreateProgram();
	if (programHandler != 0) {
		glAttachShader(programHandler, vertexShaderHandler);
		glAttachShader(programHandler, fragmentShaderHandler);
		glLinkProgram(programHandler);
		int linkStatus;
		glGetProgramiv(programHandler, GL_LINK_STATUS, &linkStatus);
		if (linkStatus != GL_TRUE) {
			GLint infoLen = 0;
			glGetProgramiv(programHandler, GL_INFO_LOG_LENGTH, &infoLen);
			if (infoLen > 1) {
				char* infoLog = (char *) malloc(sizeof(char) * infoLen);
				glGetProgramInfoLog(programHandler, infoLen, NULL, infoLog);
				LOGD("Error linking program:\n%s\n", infoLog);
				free(infoLog);
			}
			glDeleteProgram(programHandler);
			return 0;
		}
	}

	return programHandler;
}

void renderQuad(int programHandler) {
	glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
	glClear (GL_COLOR_BUFFER_BIT);

	GLfloat vVertices[] = { -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f };
	int positionHandler = glGetAttribLocation(programHandler, "position");
	glVertexAttribPointer(positionHandler, 2, GL_FLOAT, GL_FALSE, 0, vVertices);
	glEnableVertexAttribArray(positionHandler);

	glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	glDisableVertexAttribArray(positionHandler);
	glBindTexture(GL_TEXTURE_2D, 0);
}

jobject getContext(JNIEnv *env, jobject object) {
	jclass cls = env->GetObjectClass(object);
	jmethodID getContextMethod = env->GetMethodID(cls, "getContext",
			"()Landroid/content/Context;");
	jobject jContext = env->CallObjectMethod(object, getContextMethod);
	return jContext;
}

void bindUniformMatrix(JNIEnv *env, int programHandler, jfloatArray jArr,
		const char *uniformName) {
	if (!jArr) {
		return;
	}
	float *arr = env->GetFloatArrayElements(jArr, 0);
	int length = env->GetArrayLength(jArr);
	if (length == 2) {
		glUniform2fv(glGetUniformLocation(programHandler, uniformName), 1, arr);
	} else if (length == 16) {
		glUniformMatrix4fv(glGetUniformLocation(programHandler, uniformName), 1,
				GL_FALSE, arr);
	}
	env->ReleaseFloatArrayElements(jArr, arr, 0);
}

GLuint initializeProgram(JNIEnv *env, jobject object) {
	jobject jContext = getContext(env, object);
	if (!jContext) {
		return -1;
	}

	jclass cls = env->GetObjectClass(jContext);
	jmethodID getAssetManager = env->GetMethodID(cls, "getAssets",
			"()Landroid/content/res/AssetManager;");
	jobject assetManager = env->CallObjectMethod(jContext, getAssetManager);
	AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
	if (!mgr) {
		return -1;
	}

	const char *vertexShaderSource = const_cast<char*>(read_string_from_asset(
			mgr, "filters/shader/shader_normal_vertex.frag"));
	const char *fragmentShaderSource = const_cast<char*>(read_string_from_asset(
			mgr, "filters/shader/shader_normal_fragment.frag"));
	GLuint programHandler = useProgram(vertexShaderSource,
			fragmentShaderSource);
	LOGD("programHandler = %d", programHandler);

	return programHandler;
}

/*
 * 针对bitmap，android bitmap为4个通道且像素排列顺序为ARGB
 */
void processGaussianFilter(rrimage *data, int radius, double sigma) {
	if (!data || data->channels != 4) {
		return;
	}

	int n = 2 * radius + 1;

	double cd[n];
	double sigmaX = sigma > 0 ? sigma : ((n - 1) * 0.5 - 1) * 0.3 + 0.8;
	double scale2X = -0.5 / (sigmaX * sigmaX);
	double sum = 0;

	int i, j;
	for (i = 0; i < n; i++) {
		double x = i - (n - 1) * 0.5;
		double t = exp(scale2X * x * x);
		cd[i] = t;
		sum += cd[i];
	}

	sum = 1. / sum;
	for (i = 0; i < n; i++) {
		cd[i] *= sum;
	}

	int width = data->width;
	int height = data->height;
	int channels = data->channels;
	int stride = data->stride;

	unsigned char *sptr = data->pixels;

	int row, col;
	int row_offset, col_offset;
	unsigned char tr, tg, tb, ta;
	double sum_r = 0;
	double sum_g = 0;
	double sum_b = 0;
	double sum_a = 0;
	for (row = 0; row < height; row++) {
		for (col = 0; col < width; col++) {
			for (i = -radius; i <= radius; i++) {
				if (col + i < 0) {
					col_offset = 0;
				} else if (col + i > width - 1) {
					col_offset = width - 1;
				} else {
					col_offset = col + i;
				}

				sum_a += (*(sptr + row * stride + col_offset * channels)
						* cd[i + radius]);
				sum_r += (*(sptr + row * stride + col_offset * channels + 1)
						* cd[i + radius]);
				sum_g += (*(sptr + row * stride + col_offset * channels + 2)
						* cd[i + radius]);
				sum_b += (*(sptr + row * stride + col_offset * channels + 3)
						* cd[i + radius]);
			}

			*(sptr + row * stride + col * channels) = (unsigned char) sum_a;
			*(sptr + row * stride + col * channels + 1) = (unsigned char) sum_r;
			*(sptr + row * stride + col * channels + 2) = (unsigned char) sum_g;
			*(sptr + row * stride + col * channels + 3) = (unsigned char) sum_b;

			sum_r = 0;
			sum_g = 0;
			sum_b = 0;
			sum_a = 0;
		}
	}

	for (row = 0; row < height; row++) {
		for (col = 0; col < width; col++) {
			for (i = -radius; i <= radius; i++) {
				if (row + i < 0) {
					row_offset = 0;
				} else if (row + i > height - 1) {
					row_offset = height - 1;
				} else {
					row_offset = row + i;
				}

				sum_a += *(sptr + row_offset * stride + col * channels)
						* cd[i + radius];
				sum_r += *(sptr + row_offset * stride + col * channels + 1)
						* cd[i + radius];
				sum_g += *(sptr + row_offset * stride + col * channels + 2)
						* cd[i + radius];
				sum_b += *(sptr + row_offset * stride + col * channels + 3)
						* cd[i + radius];
			}

			*(sptr + row * stride + col * channels) = (unsigned char) sum_a;
			*(sptr + row * stride + col * channels + 1) = (unsigned char) sum_r;
			*(sptr + row * stride + col * channels + 2) = (unsigned char) sum_g;
			*(sptr + row * stride + col * channels + 3) = (unsigned char) sum_b;

			sum_r = 0;
			sum_g = 0;
			sum_b = 0;
			sum_a = 0;
		}
	}
}

JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_normalOnDrawFrame(
		JNIEnv *env, jobject object, jint filterType, jint textureId,
		jfloatArray uAspectRatio, jfloatArray uAspectRatioPreview,
		jfloatArray uTranslate, jfloat uScale, jfloatArray uOrientationM) {
	if (gNormalProgramHandler == -1) {
		gNormalProgramHandler = initializeProgram(env, object);
	}
	GLuint programHandler = gNormalProgramHandler;
	glUseProgram(programHandler);

	glActiveTexture (GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, textureId);
	glUniform1i(glGetUniformLocation(programHandler, "inputImageTexture"), 0);
	bindUniformMatrix(env, programHandler, uAspectRatio, "uAspectRatio");
	bindUniformMatrix(env, programHandler, uAspectRatioPreview,
			"uAspectRatioPreview");
	bindUniformMatrix(env, programHandler, uTranslate, "uTranslate");
	bindUniformMatrix(env, programHandler, uOrientationM, "uOrientationM");
	glUniform1f(glGetUniformLocation(programHandler, "uScale"), uScale);
	renderQuad(programHandler);
	return 1;
}

JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_oesOnDrawFrame(
		JNIEnv *env, jobject object, jint oesTextureId,
		jfloatArray mTransformM) {
	if (gOesProgramHandler == -1) {
		jobject jContext = getContext(env, object);
		if (!jContext) {
			return 0;
		}

		jclass cls = env->GetObjectClass(jContext);
		jmethodID getAssetManager = env->GetMethodID(cls, "getAssets",
				"()Landroid/content/res/AssetManager;");
		jobject assetManager = env->CallObjectMethod(jContext, getAssetManager);
		AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
		if (!mgr) {
			return 0;
		}

		// compile and link shader program
		const char *vertexShaderSource =
				const_cast<char*>(read_string_from_asset(mgr,
						"filters/shader/shader_copy_oes_vertex.frag"));
		const char *fragmentShaderSource =
				const_cast<char*>(read_string_from_asset(mgr,
						"filters/shader/shader_copy_oes_fragment.frag"));
		gOesProgramHandler = useProgram(vertexShaderSource,
				fragmentShaderSource);
	}

	glUseProgram(gOesProgramHandler);

	glActiveTexture (GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_EXTERNAL_OES, oesTextureId);
	glUniform1i(glGetUniformLocation(gOesProgramHandler, "sTexture"), 0);

	bindUniformMatrix(env, gOesProgramHandler, mTransformM, "uTransformM");

	renderQuad(gOesProgramHandler);

	return 1;
}

JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_saveCameraPhoto(
		JNIEnv *env, jobject object, jstring jInFilePath, jstring jOutFilePath,
		jint jOrientation, jint minWidth, jint jNeedMirror, jint isSquare) {
	const char* inFilePath = env->GetStringUTFChars(jInFilePath, 0);
	int rotation = getRotation(jOrientation, 0);
	//相机PictureSize不会有超大图，此处可以不必压缩
	rrimage *src = read_image_with_compress_by_area(inFilePath, 0, 0, 0, 0, 0,
			0, ROTATE_0);
	int imageWidth = src->width;
	int imageHeight = src->height;
	int width, height;

	if (isSquare) {
		int temp = MIN(imageWidth, imageHeight);
		width = temp > minWidth ? minWidth : temp;
		height = width;
	} else {
		compress_strategy(imageWidth, imageHeight, &width, &height, minWidth);
		if (rotation == ROTATE_90 || rotation == ROTATE_270) {
			int temp = width;
			width = height;
			height = temp;
		}
	}LOGD("image input path is %s", inFilePath);LOGD("image rotation = %d", rotation);LOGD("image width = %d, image height = %d", imageWidth, imageHeight);LOGD("out image width = %d, height = %d", width, height);
	float aspectRatioPreview[2], translate[2], aspectRatio[2];
	float orientationM[] = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
	getOrientationMatrix(orientationM, rotation);

	if (rotation == ROTATE_90) {
		aspectRatioPreview[1] = MIN(imageWidth, imageHeight)
				/ (float) imageWidth;
		aspectRatioPreview[0] = MIN(imageWidth, imageHeight)
				/ (float) imageHeight;
		aspectRatio[1] = MIN(width, height) / (float) width;
		aspectRatio[0] = MIN(width, height) / (float) height;

		translate[0] = aspectRatio[0] / aspectRatioPreview[0] - 1.0;
		translate[1] = aspectRatio[1] / aspectRatioPreview[1] - 1.0;
	} else if (rotation == ROTATE_180) {
		aspectRatio[0] = MIN(width, height) / (float) width;
		aspectRatio[1] = MIN(width, height) / (float) height;
		aspectRatioPreview[0] = MIN(imageWidth, imageHeight)
				/ (float) imageWidth;
		aspectRatioPreview[1] = MIN(imageWidth, imageHeight)
				/ (float) imageHeight;

		translate[0] = 1.0 - aspectRatio[0] / aspectRatioPreview[0];
		translate[1] = 1.0 - aspectRatio[1] / aspectRatioPreview[1];
	} else if (rotation == ROTATE_270) {
		aspectRatioPreview[1] = MIN(imageWidth, imageHeight)
				/ (float) imageWidth;
		aspectRatioPreview[0] = MIN(imageWidth, imageHeight)
				/ (float) imageHeight;
		aspectRatio[1] = MIN(width, height) / (float) width;
		aspectRatio[0] = MIN(width, height) / (float) height;

		translate[0] = 1.0 - aspectRatio[0] / aspectRatioPreview[0];
		translate[1] = 1.0 - aspectRatio[1] / aspectRatioPreview[1];
	} else {
		aspectRatio[0] = MIN(width, height) / (float) width;
		aspectRatio[1] = MIN(width, height) / (float) height;
		aspectRatioPreview[0] = MIN(imageWidth, imageHeight)
				/ (float) imageWidth;
		aspectRatioPreview[1] = MIN(imageWidth, imageHeight)
				/ (float) imageHeight;

		translate[0] = aspectRatio[0] / aspectRatioPreview[0] - 1.0;
		translate[1] = aspectRatio[1] / aspectRatioPreview[1] - 1.0;
	}

	if (jNeedMirror) {
		translate[0] = -translate[0];
		translate[1] = -translate[1];
	}

	if (!isSquare) {
		aspectRatio[0] = 1.0;
		aspectRatio[1] = 1.0;
		aspectRatioPreview[0] = 1.0;
		aspectRatioPreview[1] = 1.0;

		translate[0] = 0.0;
		translate[1] = 0.0;
	}

	const EGLint configAttribs[] = { EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
			EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8,
			EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_NONE };
	const EGLint surfaceAttribs[] = { EGL_WIDTH, width, EGL_HEIGHT, height,
			EGL_NONE };
	const EGLint contextAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE,
			EGL_NONE };

	EGLDisplay display = EGL_NO_DISPLAY;
	EGLSurface surface = EGL_NO_SURFACE;
	EGLContext context = EGL_NO_CONTEXT;

	EGLConfig config;
	EGLint numConfigs;

	if ((display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
		LOGD("eglGetDisplay() returned error %d", eglGetError());
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}
	if (!eglInitialize(display, 0, 0)) {
		LOGD("eglInitialize() returned error %d", eglGetError());
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}
	if (!eglChooseConfig(display, configAttribs, &config, 1, &numConfigs)) {
		LOGD("eglChooseConfig() returned error %d", eglGetError());
		eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
		eglDestroyContext(display, context);
		eglDestroySurface(display, surface);
		eglTerminate(display);
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}

	if (!(context = eglCreateContext(display, config, 0, contextAttribs))) {
		LOGD("eglCreateContext() returned error %d", eglGetError());
		eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
		eglDestroyContext(display, context);
		eglDestroySurface(display, surface);
		eglTerminate(display);
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}

	if (!(surface = eglCreatePbufferSurface(display, config, surfaceAttribs))) {
		LOGD("eglCreatePbufferSurface() returned error %d", eglGetError());
		eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
		eglDestroyContext(display, context);
		eglDestroySurface(display, surface);
		eglTerminate(display);
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}

	if (!eglMakeCurrent(display, surface, surface, context)) {
		LOGD("eglMakeCurrent() returned error %d", eglGetError());
		eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
		eglDestroyContext(display, context);
		eglDestroySurface(display, surface);
		eglTerminate(display);
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}

	GLuint textureId = loadTexture(-1, src);
	free_rrimage(src);

	glClearColor(0, 0, 0, 1);
	glViewport(0, 0, width, height);

	GLuint programHandler = initializeProgram(env, object);
	glUseProgram(programHandler);
	glActiveTexture (GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, textureId);
	glUniform1i(glGetUniformLocation(programHandler, "inputImageTexture"), 0);

	glUniform2fv(glGetUniformLocation(programHandler, "uAspectRatioPreview"), 1,
			aspectRatioPreview);
	glUniform2fv(glGetUniformLocation(programHandler, "uAspectRatio"), 1,
			aspectRatio);
	glUniform2fv(glGetUniformLocation(programHandler, "uTranslate"), 1,
			translate);
	glUniformMatrix4fv(glGetUniformLocation(programHandler, "uOrientationM"), 1,
			GL_FALSE, orientationM);
	glUniform1f(glGetUniformLocation(programHandler, "uScale"), 1.0f);
	renderQuad(programHandler);

	unsigned char *pixels = (unsigned char *) malloc(width * height * 4);
	glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

	glDeleteTextures(1, &textureId);

	eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
	eglDestroyContext(display, context);
	eglDestroySurface(display, surface);
	eglTerminate(display);

	rrimage *data = init_rrimage();
	data->width = width;
	data->height = height;
	data->channels = 4;
	data->stride = width * 4;
	data->pixels = pixels;

	const char *outFilePath = env->GetStringUTFChars(jOutFilePath, 0);
	write_image(outFilePath, data);
	free_rrimage(data);
	copyExif(inFilePath, outFilePath);

	env->ReleaseStringUTFChars(jOutFilePath, outFilePath);
	env->ReleaseStringUTFChars(jInFilePath, inFilePath);

	return 1;
}

JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_saveCameraPhotoWithoutGL(
		JNIEnv *env, jobject object, jstring jInFilePath, jstring jOutFilePath,
		jint orientation, jint mirror, jint left, jint top, jint width,
		jint height, jint minWidth) {
	const char* inFilePath = env->GetStringUTFChars(jInFilePath, 0);
	int rotation = getRotation(orientation, mirror);
	rrimage *data = read_image_with_compress_by_area(inFilePath,
			compress_strategy, minWidth, left, top, width, height, rotation);
	LOGD("%d, %d, %d, %d", left, top, width, height);LOGD("%d, %d, %d", data->width, data->height, data->channels);LOGD("%d, %d", orientation, rotation);

	const char *outFilePath = env->GetStringUTFChars(jOutFilePath, 0);
	write_image(outFilePath, data);
	free_rrimage(data);
	copyExif(inFilePath, outFilePath);

	env->ReleaseStringUTFChars(jOutFilePath, outFilePath);
	env->ReleaseStringUTFChars(jInFilePath, inFilePath);

	return 1;
}

JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_cropPhoto(
		JNIEnv *env, jobject object, jstring jInFilePath, jstring jOutFilePath,
		jfloatArray mAspectRatio, jfloatArray mTranslate, jfloat mScale,
		jint minWidth, jint isSquare) {
	const char* inFilePath = env->GetStringUTFChars(jInFilePath, 0);
	int rotation = getExifOrientation(inFilePath);
	rrimage *src = read_image_with_compress_by_area(inFilePath,
			compress_strategy, minWidth, 0, 0, 0, 0, ROTATE_0);
	int imageWidth = src->width;
	int imageHeight = src->height;
	int width, height;

	float aspectPreviewRatio[2], translate[2], aspectRatio[2];
	float orientationM[] = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
			0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
	getOrientationMatrix(orientationM, rotation);

	if (isSquare) {
		int temp = MIN(imageWidth, imageHeight);
		width = temp > minWidth ? minWidth : temp;
		height = width;

		aspectRatio[0] = 1.0f;
		aspectRatio[1] = 1.0f;

		float *envTranslate = env->GetFloatArrayElements(mTranslate, 0);
		float *envAspectRatio = env->GetFloatArrayElements(mAspectRatio, 0);
		if (rotation == ROTATE_0) {
			aspectPreviewRatio[0] = MIN(imageWidth, imageHeight)
					/ (float) imageWidth;
			aspectPreviewRatio[1] = MIN(imageWidth, imageHeight)
					/ (float) imageHeight;

			translate[0] = envTranslate[0] / envAspectRatio[0];
			translate[1] = -envTranslate[1] / envAspectRatio[1];
		} else if (rotation == ROTATE_90) {
			aspectPreviewRatio[1] = MIN(imageWidth, imageHeight)
					/ (float) imageWidth;
			aspectPreviewRatio[0] = MIN(imageWidth, imageHeight)
					/ (float) imageHeight;

			translate[1] = -envTranslate[1] / envAspectRatio[1];
			translate[0] = envTranslate[0] / envAspectRatio[0];
		} else if (rotation == ROTATE_180) {
			aspectPreviewRatio[0] = MIN(imageWidth, imageHeight)
					/ (float) imageWidth;
			aspectPreviewRatio[1] = MIN(imageWidth, imageHeight)
					/ (float) imageHeight;

			translate[0] = envTranslate[0] / envAspectRatio[0];
			translate[1] = -envTranslate[1] / envAspectRatio[1];
		} else if (rotation == ROTATE_270) {
			aspectPreviewRatio[1] = MIN(imageWidth, imageHeight)
					/ (float) imageWidth;
			aspectPreviewRatio[0] = MIN(imageWidth, imageHeight)
					/ (float) imageHeight;

			translate[1] = -envTranslate[1] / envAspectRatio[1];
			translate[0] = envTranslate[0] / envAspectRatio[0];
		}
		env->ReleaseFloatArrayElements(mTranslate, envTranslate, 0);
		env->ReleaseFloatArrayElements(mAspectRatio, envAspectRatio, 0);
	} else {
		if (rotation == ROTATE_90 || rotation == ROTATE_270) {
			width = imageHeight;
			height = imageWidth;
		} else {
			width = imageWidth;
			height = imageHeight;
		}

		aspectRatio[0] = 1.0;
		aspectRatio[1] = 1.0;
		aspectPreviewRatio[0] = 1.0;
		aspectPreviewRatio[1] = 1.0;

		translate[0] = 0.0;
		translate[1] = 0.0;
	}LOGD("image input path is %s", inFilePath);LOGD("image rotation = %d", rotation);LOGD("image width = %d, image height = %d", imageWidth, imageHeight);LOGD("out image width = %d, height = %d", width, height);LOGD("translate[0] = %f, translate[1] = %f", translate[0], translate[1]);LOGD("aspectRatioPreview[0]=%f, aspectRatioPreview[1]=%f",
			aspectPreviewRatio[0], aspectPreviewRatio[1]);LOGD("aspectRatio[0]=%f, aspectRatio[1]=%f, scale=%f", aspectRatio[0],
			aspectRatio[1], mScale);

	const EGLint configAttribs[] = { EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
			EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8,
			EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_NONE };
	const EGLint surfaceAttribs[] = { EGL_WIDTH, width, EGL_HEIGHT, height,
			EGL_NONE };
	const EGLint contextAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE,
			EGL_NONE };

	EGLDisplay display = EGL_NO_DISPLAY;
	EGLSurface surface = EGL_NO_SURFACE;
	EGLContext context = EGL_NO_CONTEXT;

	EGLConfig config;
	EGLint numConfigs;

	if ((display = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
		LOGD("eglGetDisplay() returned error %d", eglGetError());
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}
	if (!eglInitialize(display, 0, 0)) {
		LOGD("eglInitialize() returned error %d", eglGetError());
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}
	if (!eglChooseConfig(display, configAttribs, &config, 1, &numConfigs)) {
		LOGD("eglChooseConfig() returned error %d", eglGetError());
		eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
		eglDestroyContext(display, context);
		eglDestroySurface(display, surface);
		eglTerminate(display);
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}

	if (!(context = eglCreateContext(display, config, 0, contextAttribs))) {
		LOGD("eglCreateContext() returned error %d", eglGetError());
		eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
		eglDestroyContext(display, context);
		eglDestroySurface(display, surface);
		eglTerminate(display);
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}

	if (!(surface = eglCreatePbufferSurface(display, config, surfaceAttribs))) {
		LOGD("eglCreatePbufferSurface() returned error %d", eglGetError());
		eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
		eglDestroyContext(display, context);
		eglDestroySurface(display, surface);
		eglTerminate(display);
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}

	if (!eglMakeCurrent(display, surface, surface, context)) {
		LOGD("eglMakeCurrent() returned error %d", eglGetError());
		eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
		eglDestroyContext(display, context);
		eglDestroySurface(display, surface);
		eglTerminate(display);
		env->ReleaseStringUTFChars(jInFilePath, inFilePath);
		return 0;
	}

	GLuint textureId = loadTexture(-1, src);
	free_rrimage(src);

	glClearColor(0, 0, 0, 1);
	glViewport(0, 0, width, height);

	GLuint programHandler = initializeProgram(env, object);
	glUseProgram(programHandler);
	glActiveTexture (GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, textureId);
	glUniform1i(glGetUniformLocation(programHandler, "inputImageTexture"), 0);

	glUniform2fv(glGetUniformLocation(programHandler, "uAspectRatioPreview"), 1,
			aspectPreviewRatio);
	glUniform2fv(glGetUniformLocation(programHandler, "uAspectRatio"), 1,
			aspectRatio);
	glUniformMatrix4fv(glGetUniformLocation(programHandler, "uOrientationM"), 1,
			GL_FALSE, orientationM);
	glUniform2fv(glGetUniformLocation(programHandler, "uTranslate"), 1,
			translate);
	glUniform1f(glGetUniformLocation(programHandler, "uScale"), mScale);
	renderQuad(programHandler);

	unsigned char *pixels = (unsigned char *) malloc(width * height * 4);
	glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

	eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
	eglDestroyContext(display, context);
	eglDestroySurface(display, surface);
	eglTerminate(display);

	rrimage *data = init_rrimage();
	data->width = width;
	data->height = height;
	data->channels = 4;
	data->stride = width * 4;
	data->pixels = pixels;

	const char *outFilePath = env->GetStringUTFChars(jOutFilePath, 0);
	write_image(outFilePath, data);
	free_rrimage(data);
	copyExif(inFilePath, outFilePath);

	glDeleteTextures(1, &textureId);

	env->ReleaseStringUTFChars(jOutFilePath, outFilePath);
	env->ReleaseStringUTFChars(jInFilePath, inFilePath);

	return 1;
}

JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_cropPhotoWithoutGL(
		JNIEnv *env, jobject object, jstring jInFilePath, jstring jOutFilePath,
		jint minWidth, jint left, jint top, jint width, jint height,
		jint orientation) {
	const char* inFilePath = env->GetStringUTFChars(jInFilePath, 0);
	orientation = getRotation(orientation, 0);
	rrimage *data = read_image_with_compress_by_area(inFilePath,
			compress_strategy, minWidth, left, top, width, height, orientation);
	int imageWidth = data->width;
	int imageHeight = data->height;
	LOGD("imageWidth = %d, imageHeight = %d", imageWidth, imageHeight);LOGD("orientation = %d", orientation);

	const char *outFilePath = env->GetStringUTFChars(jOutFilePath, 0);
	write_image(outFilePath, data);
	free_rrimage(data);
	copyExif(inFilePath, outFilePath);

	env->ReleaseStringUTFChars(jOutFilePath, outFilePath);
	env->ReleaseStringUTFChars(jInFilePath, inFilePath);

	LOGD("process success...");

	return 1;
}

JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_processGaussianBlur(
		JNIEnv *env, jobject object, jobject bitmap) {
	AndroidBitmapInfo info;
	void* pixels = NULL;

	if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
		return 0;
	}
	if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888
			|| info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
		LOGD("bitmap format is not RGBA_8888\n");
		return 0;
	}
	if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
		return 0;
	}
	if (!pixels) {
		AndroidBitmap_unlockPixels(env, bitmap);
		return 0;
	}

	int width = info.width;
	int height = info.height;
	int stride = info.stride;

	LOGD("width=%d, height=%d, stride=%d", width, height, stride);

	rrimage *src = init_rrimage();
	src->width = width;
	src->height = height;
	src->channels = 4;
	src->stride = stride;
	src->pixels = (unsigned char *) pixels;

	long start = clock();
	processGaussianFilter(src, 120, 0);
	long end = clock();
	LOGD("gaussian time is: %ld", (end - start));

	free(src);

	AndroidBitmap_unlockPixels(env, bitmap);

	return 1;
}

JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_onDestroy(
		JNIEnv *env, jobject object) {
	gNormalProgramHandler = -1;
	gOesProgramHandler = -1;
}

JNIEXPORT void JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_test
(JNIEnv *env, jobject object) {
	rrimage *data = read_image_with_compress_by_area("/sdcard/Pictures/comic/10.png",
			0, 0, 0, 0, 800, 800, ROTATE_90);
	LOGD("%d, %d, %d", data->width, data->height, data->channels);
	write_image("/sdcard/1.jpg", data);
}

/*
 * Class:     com_canruoxingchen_uglypic_util_jni_NativeImageUtil
 * Method:    mergePhoto
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIII)I
 */
JNIEXPORT jint JNICALL Java_com_canruoxingchen_uglypic_util_jni_NativeImageUtil_mergePhoto
  (JNIEnv *env, jobject object, jstring origPath, jstring processedPath, jstring outPath,
		  jint origWidth, jint origHeight, jint processedWidth, jint processedWidth) {

}
}
