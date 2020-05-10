package de.shadow578.yetanothervideoplayer.feature.soundfx.fx;

import de.shadow578.yetanothervideoplayer.feature.soundfx.SoundFxAudioProcessor;

public class TestSoundFx implements SoundFxAudioProcessor.SoundFx
{

    @Override
    public void processAudio(float[][] samples, int channelsCount, int samplesCount, int sampleRate)
    {
        for (int s = 0; s < samplesCount; s++)
            samples[0][s] = 0f;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }
}
