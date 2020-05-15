package de.shadow578.yetanothervideoplayer.feature.soundfx.fx;

import de.shadow578.yetanothervideoplayer.feature.soundfx.SoundFxAudioProcessor;
import de.shadow578.yetanothervideoplayer.jni.kissfft.KissFFT;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Sound effect for virtual surround sound, using hrtf to simulate four- channel audio.
 * Requires stereo input/output, also you should be wearing headphones ;)
 */
public class VirtualSurroundFx implements SoundFxAudioProcessor.SoundFx
{
    /**
     * if true, execution time of the fft functions is logged
     * (this will spam the log output)
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean LOG_EXECUTION_TIME = true;

    public static boolean enable = true;

    /**
     * FFT instance
     */
    private KissFFT fft;

    /**
     * FFT arrays for all possible channels
     */
    private float[] fftRightEar_frontLeft, fftRightEar_frontRight, fftLeftEar_frontLeft, fftLeftEar_frontRight;

    /**
     * HRTFs for all channels, in frequency domain already
     */
    private float[] hrtf_front_rightEar, hrtf_front_leftEar;

    /**
     * Two buffers of length fftSize * 2, for fft operations
     */
    private float[] fftBufferA, fftBufferB;

    //region SoundFX
    @Override
    public void processAudio(float[][] samples, int channelsCount, int samplesCount, int sampleRate)
    {
        //don't process anything if there is nothing to process OR the input/output is not stereo
        if (samplesCount <= 0 || channelsCount != 2) return;

        //save start of function execution
        long executionStartNs;
        if (LOG_EXECUTION_TIME)
        {
            executionStartNs = System.nanoTime();
        }

        //get size of fft, use next power of two (kiss_fft should be faster that way)
        int fftSize = getNextPowerOfTwo(samplesCount);

        //initialize fft and arrays if needed
        prepareFFTAndBuffers(fftSize);

        //prepare input
        readInputToFFT(samples, fftRightEar_frontLeft, fftLeftEar_frontLeft, channelsCount, samplesCount, fftSize);
        System.arraycopy(fftRightEar_frontLeft, 0, fftRightEar_frontRight, 0, fftSize * 2);
        System.arraycopy(fftLeftEar_frontLeft, 0, fftLeftEar_frontRight, 0, fftSize * 2);


        //virtual speaker: front right
        calculateChannelHRTF(fftRightEar_frontLeft, fftLeftEar_frontLeft,
                fftBufferA, fftBufferB,
                hrtf_front_rightEar, hrtf_front_leftEar,
                fft, fftSize);

        //virtual speaker: front left
        //for left speaker, we just swap the hrtf's for the ears
        //also we swap right ear and left ear so we can reuse the function
        calculateChannelHRTF(fftLeftEar_frontRight, fftRightEar_frontRight,
                fftBufferA, fftBufferB,
                hrtf_front_rightEar, hrtf_front_leftEar,
                fft, fftSize);


        //combine samples and write to output
        for (int sai = 0; sai < samplesCount; sai++)
        {
            //right channel
            samples[0][sai] = addSamples(fftRightEar_frontRight[sai * 2] / fftSize,
                    fftRightEar_frontLeft[sai * 2] / fftSize);

            //left channel
            samples[1][sai] = addSamples(fftLeftEar_frontRight[sai * 2] / fftSize,
                    fftLeftEar_frontLeft[sai * 2] / fftSize);
        }

        //log execution time of the fft's
        if (LOG_EXECUTION_TIME)
        {
            executionStartNs = System.nanoTime() - executionStartNs;
            Logging.logD("processAudio() took %d ns", executionStartNs);
        }
    }

    @Override
    public boolean isEnabled()
    {
        return enable;
    }
    //endregion

    /**
     * Calculate and apply a HRTF to the channels.
     *
     * @param rightEarT      output for the right ear; also the signal input, time domain, length = fftSize * 2 (input and output, values are overwritten)
     * @param leftEarT       output for the left ear, time domain, length = fftSize * 2 (only output, values are overwritten, initial fft with values of rightEarT)
     * @param fftBufferRight fft buffer for the right channel. length = fftSize * 2. THIS is just a internal buffer, no important data is written here, so feel free to reuse it ;)
     * @param fftBufferLeft  fft buffer for the left channel. length = fftSize * 2. THIS is just a internal buffer, no important data is written here, so feel free to reuse it ;)
     * @param hrtfRightF     hrtf for the right channel, frequency domain, length = fftSize * 2
     * @param hrtfLeftF      hrtf for the left channel, frequency domain, length = fftSize * 2
     * @param fft            the fft instance to use
     * @param fftSize        the size of the fft
     */
    private void calculateChannelHRTF(float[/*real,imaginary*/] rightEarT, float[/*real,imaginary*/] leftEarT,
                                      float[/*real,imaginary*/] fftBufferRight, float[/*real,imaginary*/] fftBufferLeft,
                                      float[/*real,imaginary*/] hrtfRightF, float[/*real,imaginary*/] hrtfLeftF,
                                      KissFFT fft, int fftSize)
    {
        //check inputs not null
        throwIf(rightEarT == null, "input / output for right ear is null!");
        throwIf(leftEarT == null, "output for left ear is null!");
        throwIf(fftBufferRight == null, "right fft buffer is null!");
        throwIf(fftBufferLeft == null, "left fft buffer is null!");
        throwIf(hrtfRightF == null, "right hrtf is null!");
        throwIf(hrtfLeftF == null, "left hrtf is null!");
        throwIf(fft == null, "fft instance is null!");

        //check length
        throwIf(fftSize < 1, "fftSize has to be at least 1!");
        throwIf(rightEarT.length != (fftSize * 2)
                || leftEarT.length != (fftSize * 2)
                || fftBufferRight.length != (fftSize * 2)
                || fftBufferLeft.length != (fftSize * 2)
                || hrtfRightF.length != (fftSize * 2)
                || hrtfLeftF.length != (fftSize * 2), "fftSize does not match input array length! fft arrays have to have a length of fftSize * 2");

        //get right ear in frequency domain
        //also copy the right ear's fft buffer into the left ear's fft buffer (they would have the same data anyways, but this saves one fft computation)
        fft.fft(rightEarT, fftBufferRight, false);
        System.arraycopy(fftBufferRight, 0, fftBufferLeft, 0, fftSize * 2);

        //apply HRTF for left and right ear
        // Ro = (Ra * Rh) - (Ia * Ih)
        // Io = (Ra * Ih) + (Ia * Rh)
        int r, i;
        for (int ei = 0; ei < fftSize; ei++)
        {
            //calculate indexes for real and imaginary values
            r = ei * 2;
            i = (ei * 2) + 1;

            //right ear
            fftBufferRight[r] = (fftBufferRight[r] * hrtfRightF[r]) - (fftBufferRight[i] * hrtfRightF[i]);
            fftBufferRight[i] = (fftBufferRight[r] * hrtfRightF[i]) + (fftBufferRight[i] * hrtfRightF[r]);

            //left ear
            fftBufferLeft[r] = (fftBufferLeft[r] * hrtfLeftF[r]) - (fftBufferLeft[i] * hrtfLeftF[i]);
            fftBufferLeft[i] = (fftBufferLeft[r] * hrtfLeftF[i]) + (fftBufferLeft[i] * hrtfLeftF[r]);
        }

        //run inverse fft for right and left ear
        fft.fft(fftBufferRight, rightEarT, true);
        fft.fft(fftBufferLeft, leftEarT, true);
    }

    /**
     * Prepare the fft and fft buffers
     * Initializes:
     * - fft
     * - fftFR
     * - fftFL
     * - fftRR
     * - fftRL
     *
     * @param fftSize the fftSize to prepare the fft and buffers for
     */
    private void prepareFFTAndBuffers(int fftSize)
    {
        if (fft == null || fft.getN() != fftSize
                || fftRightEar_frontLeft == null || fftRightEar_frontRight == null
                || fftLeftEar_frontLeft == null || fftLeftEar_frontRight == null
                || fftBufferA == null || fftBufferB == null
                || hrtf_front_leftEar == null || hrtf_front_rightEar == null)
        {
            //release old fft
            if (fft != null)
            {
                fft.release();
            }

            //init fft
            fft = new KissFFT(fftSize);

            //init buffers
            fftBufferA = new float[fftSize * 2];
            fftBufferB = new float[fftSize * 2];

            //init channel buffers
            fftRightEar_frontLeft = new float[fftSize * 2];
            fftRightEar_frontRight = new float[fftSize * 2];
            fftLeftEar_frontLeft = new float[fftSize * 2];
            fftLeftEar_frontRight = new float[fftSize * 2];

            //init hrtf buffers
            hrtf_front_leftEar = new float[fftSize * 2];
            hrtf_front_rightEar = new float[fftSize * 2];

            //write fft samples to fftBuffers, and zero pad them
            //noinspection ConstantConditions
            throwIf(HRTF_SAMPLES.hrtf_fl.length != HRTF_SAMPLES.hrtf_fr.length, "HRTF_SAMPLES constant invalid!");
            for (int i = 0; i < fftSize; i++)
            {
                if (i < HRTF_SAMPLES.hrtf_fl.length)
                {
                    //use real samples
                    fftBufferA[i * 2] = HRTF_SAMPLES.hrtf_fl[i];
                    fftBufferB[i * 2] = HRTF_SAMPLES.hrtf_fr[i];
                }
                else
                {
                    //zero- pad
                    fftBufferA[i * 2] = 0f;
                    fftBufferB[i * 2] = 0f;
                }

                //imaginary always is 0
                fftBufferA[(i * 2) + 1] = 0f;
                fftBufferB[(i * 2) + 1] = 0f;
            }

            //init hrtf's in frequency domain
            fft.fft(fftBufferA, hrtf_front_leftEar, false);
            fft.fft(fftBufferB, hrtf_front_rightEar, false);
        }
    }

    /**
     * Prepare the input for the fft.
     * If we have 1 input channel (mono), we mirror right and left channel.
     * If we have 2 input channels (stereo), we use them as is.
     * outputs for fft are zero- padded, imaginary part is initialized to zero.
     * <p>
     * !! fftxx Arrays are the channels in TIME domain !!
     *
     * @param inputSamples the samples input. ONLY supports mono or stereo!
     * @param rightEarT    front right fft channel, in [real, imaginary] pairs. Length = fftSize * 2, zero- padded, imaginary = 0
     * @param leftEarT     front left fft channel, in [real, imaginary] paris. Length = fftSize * 2, zero- padded, imaginary = 0
     * @param channelCount how many channels of audio the input has (ONLY mono or stereo!)
     * @param sampleCount  how many samples the input has
     * @param fftSize      the size of the fft (fftRight and fftLeft must have length = fftSize * 2)
     */
    private void readInputToFFT(float[/*ch*/][/*sample*/] inputSamples,
                                float[/*real,imaginary*/] rightEarT,
                                float[/*real,imaginary*/] leftEarT,
                                int channelCount, int sampleCount, int fftSize)
    {
        //check inputs are not null
        throwIf(inputSamples == null, "input Samples is null!");
        throwIf(rightEarT == null, "fft front right is null!");
        throwIf(leftEarT == null, "fft front left is null!");

        //check fft arrays have right length
        throwIf(rightEarT.length != (fftSize * 2), "fft front right has the wrong size!");
        throwIf(leftEarT.length != (fftSize * 2), "fft front left has wrong size!");

        //check fftSize is at least the amount of samples
        throwIf(fftSize < sampleCount, "fftSize has to be bigger or equal to sampleCount!");

        //check we have either mono or stereo input
        throwIf(channelCount != 1 && channelCount != 2, "require either mono or stereo input!");

        //check if we are stereo
        boolean isStereo = channelCount == 2;

        //channel 0 is right and 1 is left
        int siR = 0;
        int siL = isStereo ? 1 : 0;

        //copy input samples to fft
        for (int fftI = 0; fftI < fftSize; fftI++)
        {
            if (fftI < sampleCount)
            {
                //write "real" samples
                rightEarT[fftI * 2] = inputSamples[siR][fftI];
                leftEarT[fftI * 2] = inputSamples[siL][fftI];
            }
            else
            {
                //no real samples, zero- pad
                rightEarT[fftI * 2] = 0f;
                leftEarT[fftI * 2] = 0f;
            }

            //fill imaginary parts to be 0
            rightEarT[(fftI * 2) + 1] = 0f;
            leftEarT[(fftI * 2) + 1] = 0f;
        }
    }

    /**
     * add two samples
     *
     * @param a sample a to add
     * @param b sample b to add
     * @return the added sample
     */
    private float addSamples(float a, float b)
    {
        return (a + b) / 2f;
    }

    /**
     * Get the next power of two for the given value
     *
     * @param value the value to get the next power of two of
     * @return the next power of two
     */
    private int getNextPowerOfTwo(int value)
    {
        value -= 1;
        value |= value >> 16;
        value |= value >> 8;
        value |= value >> 4;
        value |= value >> 2;
        value |= value >> 1;
        return value + 1;
    }

    /**
     * Throw a IllegalArgumentException if the condition is true
     *
     * @param condition the condition
     * @param message   the message of the Exception thrown
     */
    private void throwIf(boolean condition, String message)
    {
        if (condition) throw new IllegalArgumentException(message);
    }
}

/**
 * Class that contains constant hrtf data
 */
final class HRTF_SAMPLES
{
    /**
     * HRTF data for front right channel, right ear, [real, imaginary] pairs, imaginary = 0
     * elev = 0, azimuth = 35
     * elev0/H0e035a:right
     */
    static final float[] hrtf_fr = {3.051758E-05f, -9.155273E-05f, 0f, -3.051758E-05f, 0f, 0f, -3.051758E-05f, 0f, 3.051758E-05f, -3.051758E-05f, 0.0001220703f, -0.0002441406f, 0.0005187988f, -0.0008239746f, 0.001159668f, -0.001525879f, 0.001708984f, -0.001190186f, 0.00177002f, 0.003540039f, 0.07894897f, 0.05496216f, -0.0843811f, 0.04910278f, -0.01040649f, -0.04812622f, 0.1194763f, 0.0473938f, 0.05877686f, 0.1812744f, 0.1342773f, -0.04138184f, 0.0178833f, 0.07678223f, -0.06491089f, -0.07385254f, -0.003570557f, -0.02050781f, -0.05352783f, -0.02703857f, -0.006286621f, -0.008636475f, -0.01412964f, -0.001068115f, 0.01098633f, 0.001190186f, 0.001678467f, 0.0002441406f, -0.007873535f, -0.005126953f, -0.005859375f, -0.008026123f, 0.003570557f, -0.001922607f, -0.01358032f, -0.0140686f, -0.02481079f, -0.03250122f, -0.02755737f, -0.03268433f, -0.03244019f, -0.02752686f, -0.02813721f, -0.02648926f, -0.02243042f, -0.02084351f, -0.02212524f, -0.01919556f, -0.01629639f, -0.01321411f, -0.0128479f, -0.01394653f, -0.01318359f, -0.0138855f, -0.01242065f, -0.01208496f, -0.009429932f, -0.006866455f, -0.00491333f, -0.004119873f, -0.006744385f, -0.005493164f, -0.00567627f, -0.005615234f, -0.006378174f, -0.006591797f, -0.006866455f, -0.008636475f, -0.008422852f, -0.007781982f, -0.006500244f, -0.006134033f, -0.006164551f, -0.006347656f, -0.006500244f, -0.005279541f, -0.004547119f, -0.004730225f, -0.003326416f, -0.002502441f, -0.003387451f, -0.004852295f, -0.004333496f, -0.003662109f, -0.003570557f, -0.002960205f, -0.003234863f, -0.002838135f, -0.003051758f, -0.003051758f, -0.002258301f, -0.002319336f, -0.002105713f, -0.002593994f, -0.00213623f, -0.001403809f, -0.002105713f, -0.001403809f, -0.001281738f, -0.000579834f, 0.0003662109f, 0.000579834f, 0.0002441406f, 9.155273E-05f, 0.0002441406f, -0.0004577637f, -0.0004882813f, -0.0005493164f};

    /**
     * HRTF data for front left channel, left ear
     * elev = 0, azimuth = 35
     * elev0/H0e035a:left
     */
    static final float[] hrtf_fl = {-0.001403809f, 0.0004272461f, 0.000213623f, -0.002349854f, 0.004577637f, -0.007415771f, 0.008728027f, 0.04992676f, 0.4217224f, -0.08560181f, -0.3598938f, 0.06256104f, -0.120636f, 0.3086548f, 0.1569519f, 0.1011353f, 0.6803589f, 0.2357178f, -0.3146057f, -0.01489258f, 0.1029358f, -0.2836609f, -0.160614f, -0.1195374f, -0.08529663f, -0.03036499f, -0.1094666f, 0.04547119f, 0.01022339f, -0.06161499f, 0.02340698f, 0.04217529f, -0.1153564f, -0.02075195f, -0.003265381f, -0.08087158f, 0.009674072f, 0.003967285f, 0.0050354f, 0.03936768f, -0.003540039f, -0.02700806f, -0.03512573f, -0.1175232f, -0.1016235f, -0.03149414f, -0.06552124f, -0.06185913f, -0.02276611f, -0.0133667f, -0.006439209f, -0.01403809f, -0.006744385f, 0.005279541f, -0.002838135f, -0.0173645f, -0.003356934f, -0.0001220703f, 0.003112793f, 0.002563477f, -0.00177002f, -0.003295898f, -0.007141113f, -0.007598877f, -0.007415771f, -0.003479004f, -0.006561279f, -0.01074219f, -0.01330566f, -0.01028442f, -0.007507324f, -0.005310059f, -0.00668335f, -0.007385254f, -0.006011963f, -0.007385254f, -0.008575439f, -0.003326416f, -0.002746582f, -0.0007629395f, -0.00402832f, -0.007293701f, -0.004150391f, -0.00491333f, -0.00491333f, -0.004089355f, -0.003509521f, -0.005065918f, -0.002929688f, -0.0009765625f, 0.002166748f, 0.005340576f, 0.003601074f, -0.0004577637f, -0.001953125f, -0.002716064f, -0.001251221f, -0.001068115f, 0.0005187988f, 0.001495361f, -0.002166748f, -0.006713867f, -0.004486084f, 0.000579834f, 0.00177002f, 0.002105713f, -0.0003967285f, -0.001556396f, -0.001586914f, -0.002258301f, -0.0003051758f, 0.0008544922f, -0.0006713867f, -0.0004577637f, 0.0005187988f, 0.001953125f, 0.003570557f, 0.00479126f, 0.003570557f, 0.002563477f, 0.002563477f, 0.003173828f, 0.004547119f, 0.006011963f, 0.005584717f, 0.004852295f, 0.004486084f, 0.002838135f};

}
