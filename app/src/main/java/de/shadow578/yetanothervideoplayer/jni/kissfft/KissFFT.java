package de.shadow578.yetanothervideoplayer.jni.kissfft;

import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * JNI Wrapper for Kiss FFT
 * see src/main/cpp/kiss_fft_jni.cpp for the native code
 */
public class KissFFT
{
    //region JNI
    static
    {
        //load native lib
        System.loadLibrary("kiss_fft_jni");
    }

    /**
     * initialize the fft
     *
     * @param n data points in the fft
     */
    private native void nInitFFT(int n);

    /**
     * free the fft
     */
    private native void nFreeFFT();

    /**
     * nFFT(float[]:input, int:is_inverse)
     * compute fft for the input array, output to a new float array of length fftN * 2
     *
     * @param input      the input array, in pairs of [real, imaginary] (e.g. input[0] is first real, input[1] is fist imaginary, input[2] is second real, ...)
     * @param is_inverse do we want to perform a inverse fft? (0 = fft, 1 = ifft)
     * @return the output of the fft, in pairs of [real, imaginary] (same format and length as input array)
     */
    private native float[/*[real,imaginary]*/] nFFT(float[/*[real,imaginary]*/] input, int is_inverse);

    /**
     * nFFTInplace(float[]:input, float[]:output, int:is_inverse)
     * compute fft for the input array, output to the output array
     *
     * @param input      the input array, in pairs of [real, imaginary] (e.g. input[0] is first real, input[1] is fist imaginary, input[2] is second real, ...)
     * @param output     the output array, same format and length as the input array
     * @param is_inverse do we want to perform a inverse fft? (0 = fft, 1 = ifft)
     * @return was fft performed ok?
     */
    private native boolean nFFTInplace(float[/*[real,imaginary]*/] input, float[/*[real,imaginary]*/] output, int is_inverse);
    //endregion

    /**
     * input length of this fft instance
     */
    private int fftN;

    /**
     * Create a new fft with input length n
     *
     * @param n the input length of the fft
     */
    public KissFFT(int n)
    {
        Logging.logD("Init KissFFT for n= %d", n);
        nInitFFT(n);
        fftN = n;
    }

    @Override
    protected void finalize() throws Throwable
    {
        if (fftN > 0)
        {
            release();
        }
        super.finalize();
    }

    /**
     * Release the native fft parts
     */
    public void release()
    {
        //clean up fft
        Logging.logD("Free KissFFT");
        nFreeFFT();
        fftN = -1;
    }

    /**
     * @return the length n of the fft instance
     */
    public int getN()
    {
        return fftN;
    }

    /**
     * Compute the FFT or iFFT of the input array using native kiss_fft
     *
     * @param input     the input to compute the FFT or iFFT of, in pairs of [real, imaginary] (e.g. input[0] is first real, input[1] is fist imaginary, input[2] is second real, ...)
     * @param isInverse should be compute inverse fft? (false = FFT, true = iFFT)
     * @return the FFT or iFFT result
     */
    public float[/*[real,imaginary]*/] fft(float[/*[real,imaginary]*/] input, boolean isInverse)
    {
        //check input
        if (input == null || input.length <= 0)
            throw new IllegalArgumentException("Input array is null or empty!");

        if ((input.length % 2) != 0)
            throw new IllegalArgumentException("Input array has to be PAIRS of [real, imaginary], and as such has to be even!");

        //call jni function
        return nFFT(input, isInverse ? 1 : 0);
    }

    /**
     * Compute the FFT or iFFT of the input array using native kiss_fft
     *
     * @param input     the input to compute the FFT or iFFT of, in pairs of [real, imaginary] (e.g. input[0] is first real, input[1] is fist imaginary, input[2] is second real, ...)
     * @param output    the output of the fft
     * @param isInverse should be compute inverse fft? (false = FFT, true = iFFT)
     */
    public void fft(float[/*[real,imaginary]*/] input, float[/*[real,imaginary]*/] output, boolean isInverse)
    {
        //check input
        if (input == null || input.length <= 0)
            throw new IllegalArgumentException("Input array is null or empty!");

        //check output
        if (output == null || output.length <= 0)
            throw new IllegalArgumentException("Output array is null or empty!");

        //check output length matches input
        if (output.length != input.length)
            throw new IllegalArgumentException("Output length has to be equal input length!");

        //make sure length is even (pairs)
        if ((input.length % 2) != 0)
            throw new IllegalArgumentException("Input array has to be PAIRS of [real, imaginary], and as such has to be even!");

        //call jni function
        if (!nFFTInplace(input, output, isInverse ? 1 : 0))
        {
            throw new RuntimeException("Error in native FFT call! (See log for info)");
        }
    }
}
