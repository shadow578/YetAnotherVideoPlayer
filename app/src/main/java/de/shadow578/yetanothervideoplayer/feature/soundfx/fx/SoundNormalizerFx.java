package de.shadow578.yetanothervideoplayer.feature.soundfx.fx;

import de.shadow578.yetanothervideoplayer.feature.soundfx.SoundFxAudioProcessor;

public class SoundNormalizerFx implements SoundFxAudioProcessor.SoundFx
{
    /**
     * is the filter currently enabled?
     */
    private boolean isEnabled = true;

    /**
     * average sample values for each channel
     */
    private float[] averagePerChannel;

    @Override
    public void processAudio(float[][] samples, int channelsCount, int samplesCount, int sampleRate)
    {
        //check there are samples to process first
        if (samplesCount <= 0)
            return;

        //init averagePerChannel if needed
        if (averagePerChannel == null || averagePerChannel.length != channelsCount)
        {
            averagePerChannel = new float[channelsCount];
            for (int i = 0; i < channelsCount; i++)
                averagePerChannel[i] = 0.5f;
        }

        //calculate average and gain per channel
        for (int ch = 0; ch < channelsCount; ch++)
        {
            //calculate average for this channel
            float avg = 0f;
            for (int s = 0; s < samplesCount; s++)
            {
                avg += Math.abs(samples[ch][s]);
            }
            avg /= samplesCount;

            //incorporate previous average
            final float hold = 5f;
            avg = (averagePerChannel[ch] - (averagePerChannel[ch] / hold)) + (avg / hold);
            averagePerChannel[ch] = avg;

            //calculate gain for this channel
            final float avgTarget = 0.5f;
            float gain = avgTarget / avg;

            //apply gain to this channel
            for (int s = 0; s < samplesCount; s++)
            {
                samples[ch][s] *= gain;
            }
        }
    }

    /**
     * Set if this fx is enabled or not
     *
     * @param enabled enable fx?
     */
    public void setEnabled(boolean enabled)
    {
        isEnabled = enabled;
    }

    @Override
    public boolean isEnabled()
    {
        return isEnabled;
    }
}
