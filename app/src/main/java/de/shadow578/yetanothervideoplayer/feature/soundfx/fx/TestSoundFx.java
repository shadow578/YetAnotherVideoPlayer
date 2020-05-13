package de.shadow578.yetanothervideoplayer.feature.soundfx.fx;


import de.shadow578.yetanothervideoplayer.feature.soundfx.SoundFxAudioProcessor;
import de.shadow578.yetanothervideoplayer.jni.kissfft.KissFFT;
import de.shadow578.yetanothervideoplayer.util.Logging;

public class TestSoundFx implements SoundFxAudioProcessor.SoundFx
{
    /**
     * if true, execution time of the fft functions if logged
     * (this will spam the log output)
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean LOG_EXECUTION_TIME = true;

    /**
     * FFT instance
     */
    private KissFFT fft;

    /**
     * input buffer for the fft
     */
    private float[] fftSamplesInTimeDomain;

    /**
     * output buffer for the fft
     */
    private float[] fftSamplesInFrequencyDomain;

    //region SoundFX
    @Override
    public void processAudio(float[][] samples, int channelsCount, int samplesCount, int sampleRate)
    {
        //TODO: this is still for testing, but should work now

        //don't process anything if there is nothing to process
        if (samplesCount <= 0) return;

        //save start of function execution
        long executionStartNs;
        if (LOG_EXECUTION_TIME)
        {
            executionStartNs = System.nanoTime();
        }

        //get size of fft
        int fftSize = getNextPowerOfTwo(samplesCount);

        //initialize fft and arrays if needed
        if (fft == null || fftSamplesInTimeDomain == null || fftSamplesInFrequencyDomain == null || fft.getN() != fftSize)
        {
            //release old fft
            if (fft != null)
            {
                fft.release();
            }

            //init fft
            fft = new KissFFT(fftSize);

            //init buffers
            fftSamplesInTimeDomain = new float[fftSize * 2];
            fftSamplesInFrequencyDomain = new float[fftSize * 2];
        }

        //copy samples to input buffer
        for (int s = 0; s < fftSize; s++)
        {
            //copy sample to the real part of the current "pair"
            if (s < samplesCount)
            {
                fftSamplesInTimeDomain[s * 2] = samples[0][s];
            }
            else
            {
                //no real samples left, pad with 0
                fftSamplesInTimeDomain[s * 2] = 0f;
            }

            //set imaginary part of the current "pair" to zero
            fftSamplesInTimeDomain[(s * 2) + 1] = 0f;
        }

        //compute forward fft inplace
        fft.fft(fftSamplesInTimeDomain, fftSamplesInFrequencyDomain, false);

        //TODO: do processing

        //compute reverse fft inplace
        fft.fft(fftSamplesInFrequencyDomain, fftSamplesInTimeDomain, true);

        //write the samples back to the channels
        for (int s = 0; s < samplesCount; s++)
        {
            //TODO: mute all channels (for testing)
            for (int ch = 0; ch < channelsCount; ch++)
            {
                samples[ch][s] = 0f;
            }

            //write samples in time domain to the output
            //have to scale the output by N
            samples[0][s] = fftSamplesInTimeDomain[s * 2] / fftSize;
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
        return true;
    }
//endregion

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
}
