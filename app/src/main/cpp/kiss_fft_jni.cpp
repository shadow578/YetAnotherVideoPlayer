#include <stdint.h>
/**
 * JNI Wrapper for Kiss FFT
 * see src/main/java/de/shadow578/yetanothervideoplayer/jni/kissfft/KissFFT.java for the java code
 */

#include <jni.h>
#include <android/log.h>
#include "./kissfft/kiss_fft.h"

extern "C" {

#define TAG "KISSFFT_NATIVE"

/**
 * The N the fft is currently initialized for
 */
int fftN = -1;

/**
 * FFT forward configuration
 */
kiss_fft_cfg forwardCfg = NULL;

/**
 * FFT inverse configuration
 */
kiss_fft_cfg inverseCfg = NULL;

/**
 * nInit(int:n)
 * initialize the fft
 *
 * @param env JNI environment
 * @param n data points in the fft
 */
JNIEXPORT void JNICALL
Java_de_shadow578_yetanothervideoplayer_jni_kissfft_KissFFT_nInitFFT(JNIEnv  __unused *env, jobject,
                                                                     jint n) {

    if (n < 1) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "n cannot be less than 1!");
        return;
    }

    //free fft config if possible
    if (forwardCfg != NULL) free(forwardCfg);
    if (inverseCfg != NULL) free(inverseCfg);

    //create forward and reverse config for n
    forwardCfg = kiss_fft_alloc(n, 0, 0, 0);
    inverseCfg = kiss_fft_alloc(n, 1, 0, 0);

    fftN = n;
}

/**
 * nFree(void)
 * free the fft
 *
 * @param env JNI environment
 */
JNIEXPORT void JNICALL
Java_de_shadow578_yetanothervideoplayer_jni_kissfft_KissFFT_nFreeFFT(JNIEnv __unused *env,
                                                                     jobject) {

    //check we can actually free something (nothing bad happends, but we can write a warning :P)
    if (fftN == -1) {
        __android_log_write(ANDROID_LOG_WARN, TAG, "cannot free FFT: fft was already freed!");
    }

    //free fft config if possible
    if (forwardCfg != NULL) free(forwardCfg);
    if (inverseCfg != NULL) free(inverseCfg);
    forwardCfg = NULL;
    inverseCfg = NULL;

    fftN = -1;
}

/**
 * nFFT(float[]:input, int:is_inverse)
 * compute fft for the input array, output to a new float array of length n * 2
 *
 * @param env JNI environment
 * @param input      the input array, in pairs of [real, imaginary] (e.g. input[0] is first real, input[1] is fist imaginary, input[2] is second real, ...)
 * @param is_inverse do we want to perform a inverse fft? (0 = fft, 1 = ifft)
 * @return the output of the fft, in pairs of [real, imaginary] (same format and length as input array)
 */
JNIEXPORT jfloatArray JNICALL
Java_de_shadow578_yetanothervideoplayer_jni_kissfft_KissFFT_nFFT(JNIEnv *env, jobject,
                                                                 jfloatArray input,
                                                                 jint is_inverse) {

    //check input is not null
    if (input == NULL) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "input array is nullptr!");
        return JNI_FALSE;
    }

    //check input length matches the N we're configured for
    int lenInput = env->GetArrayLength(input);
    if (lenInput != (fftN * 2)) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "input length does not match N!");
        return JNI_FALSE;
    }

    //check fft configs are valid
    if (forwardCfg == NULL || inverseCfg == NULL) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "fft config is invalid! call nInit() first!");
        return JNI_FALSE;
    }

    //create pointer to input array
    float *inputPtr = env->GetFloatArrayElements(input, 0);

    //create output array and get pointer to it
    jfloatArray output = env->NewFloatArray(fftN * 2);
    float *outputPtr = env->GetFloatArrayElements(output, 0);

    //get config for fft
    kiss_fft_cfg cfg;
    if (!is_inverse) {
        cfg = forwardCfg;
    } else {
        cfg = inverseCfg;
    }

    //call kiss fft
    //we can just cast the input and output to kiss_fft_cpx because we defined kiss_fft_scalar to be float
    kiss_fft(cfg, (kiss_fft_cpx *) inputPtr, (kiss_fft_cpx *) outputPtr);

    //release pointers to input and output arrays
    env->ReleaseFloatArrayElements(input, inputPtr, 0);
    env->ReleaseFloatArrayElements(output, outputPtr, 0);

    //return the output
    return output;
}

/**
 * nFFTInplace(float[]:input, float[]:output, int:is_inverse)
 * compute fft for the input array, output to the output array
 *
 * @param env JNI environment
 * @param input      the input array, in pairs of [real, imaginary] (e.g. input[0] is first real, input[1] is fist imaginary, input[2] is second real, ...)
 * @param output     the output array, same format and length as the input array
 * @param is_inverse do we want to perform a inverse fft? (0 = fft, 1 = ifft)
 * @return was fft performed ok?
 */
JNIEXPORT jboolean JNICALL
Java_de_shadow578_yetanothervideoplayer_jni_kissfft_KissFFT_nFFTInplace(JNIEnv *env, jobject,
                                                                        jfloatArray input,
                                                                        jfloatArray output,
                                                                        jint is_inverse) {
    //check input is not null
    if (input == NULL) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "input array is nullptr!");
        return JNI_FALSE;
    }

    //check output is not null
    if (output == NULL) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "output array is nullptr!");
        return JNI_FALSE;
    }

    //check array lengths are the same
    int lenInput = env->GetArrayLength(input);
    int lenOutput = env->GetArrayLength(output);
    if (lenInput != lenOutput) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "output length does not match input length!");
        return JNI_FALSE;
    }

    //check input length matches the N we're configured for
    if (lenInput != (fftN * 2)) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "input length does not match N!");
        return JNI_FALSE;
    }

    //check fft configs are valid
    if (forwardCfg == NULL || inverseCfg == NULL) {
        __android_log_write(ANDROID_LOG_ERROR, TAG, "fft config is invalid! call nInit() first!");
        return JNI_FALSE;
    }

    //create pointers to input and output arrays
    float *inputPtr = env->GetFloatArrayElements(input, 0);
    float *outputPtr = env->GetFloatArrayElements(output, 0);

    //get config for fft
    kiss_fft_cfg cfg;
    if (!is_inverse) {
        cfg = forwardCfg;
    } else {
        cfg = inverseCfg;
    }

    //call kiss fft
    //we can just cast the input and output to kiss_fft_cpx because we defined kiss_fft_scalar to be float
    kiss_fft(cfg, (kiss_fft_cpx *) inputPtr, (kiss_fft_cpx *) outputPtr);

    //release pointers to input and output arrays
    env->ReleaseFloatArrayElements(input, inputPtr, 0);
    env->ReleaseFloatArrayElements(output, outputPtr, 0);

    //no errors :)
    return JNI_TRUE;
}
}