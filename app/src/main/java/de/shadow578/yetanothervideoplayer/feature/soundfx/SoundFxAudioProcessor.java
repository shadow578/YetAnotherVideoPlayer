package de.shadow578.yetanothervideoplayer.feature.soundfx;

import com.google.android.exoplayer2.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import de.shadow578.yetanothervideoplayer.feature.soundfx.fx.TestSoundFx;
import de.shadow578.yetanothervideoplayer.util.Logging;

/**
 * Allows to add {@link SoundFx} sound effects to the audio of a exoplayer instance
 */
public class SoundFxAudioProcessor extends BaseAudioProcessor
{
    /**
     * A Sound Effect for the {@link SoundFxAudioProcessor}
     */
    public interface SoundFx
    {
        /**
         * Processes the given samples in the samples array.
         * writes any changes back to the samples array
         *
         * @param samples       the samples to process. float[<channel>][<sample>], channel = 0 is right and channel = 1 is left
         * @param channelsCount how many channels does the samples array have?
         * @param samplesCount  how many samples does the samples array have?
         * @param sampleRate    what is the sample rate of the audio?
         */
        void processAudio(float[][] samples, int channelsCount, int samplesCount, int sampleRate);

        /**
         * @return is this effect enabled? (effect is skipped if not enabled)
         */
        boolean isEnabled();
    }

    /**
     * List of sound effects. Sound effects in this list may be disabled.
     */
    private ArrayList<SoundFx> soundFx = new ArrayList<>();

    public SoundFxAudioProcessor()
    {
        //for testing: TestSoundFx
        addEffect(new TestSoundFx());
    }

    //region Interfacing

    /**
     * Add a {@link SoundFx} sound effect to the processor.
     * If the effect is already in the list of effects, it is not added again.
     *
     * @param effect the sound effect to add
     */
    public void addEffect(SoundFx effect)
    {
        if (!soundFx.contains(effect))
            soundFx.add(effect);
    }

    /**
     * Remove a {@link SoundFx} sound effect from the processor
     *
     * @param effect the effect to remove
     */
    public void removeEffect(SoundFx effect)
    {
        soundFx.remove(effect);
    }
    //endregion

    //region BaseAudioProcessor
    @Override
    public boolean configure(int sampleRateHz, int channelCount, int encoding)
    {
        Logging.logE("Sample Rate: %d; channels: %d", sampleRateHz, channelCount);
        return setInputFormat(sampleRateHz, channelCount, encoding);
    }

    @Override
    public void queueInput(ByteBuffer input)
    {
        //get input info
        int pos = input.position();
        int limit = input.limit();

        //create the output buffer
        int sampleCount = (limit - pos) / (2 * channelCount);
        int outSize = sampleCount * channelCount * 2;
        ByteBuffer output = replaceOutputBuffer(outSize);

        if (hasActiveEffects())
        {
            //put all samples into a float[<channel>][<sample>] for processing
            float[][] samples = new float[channelCount][sampleCount];
            for (int sa = 0; sa < sampleCount; sa++)
                for (int ch = 0; ch < channelCount; ch++)
                {
                    //get raw sample
                    short rawSample = input.getShort((sa * channelCount * 2) + (2 * ch));

                    //convert to normalized float (-1 to 1)
                    float sample = (float) rawSample / (float) Short.MAX_VALUE;

                    //add to samples array
                    samples[ch][sa] = sample;
                }


            //process the samples
            for (SoundFx fx : soundFx)
            {
                if (fx.isEnabled())
                {
                    fx.processAudio(samples, channelCount, sampleCount, sampleRateHz);
                }
            }


            //convert float[<channel>][<sample>] for processing back to a byte buffer
            for (int sa = 0; sa < sampleCount; sa++)
                for (int ch = 0; ch < channelCount; ch++)
                {
                    //get sample from samples array
                    float sample = samples[ch][sa];

                    //convert normalized float to raw sample value again
                    sample *= Short.MAX_VALUE;
                    if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;
                    if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;

                    //write sample to output
                    output.putShort((short) sample);
                }
        }
        else
        {
            //no effects, skip any conversions for processing and copying to output directly
            for (; pos < limit; pos += channelCount * 2)
                for (int ch = 0; ch < channelCount; ch++)
                    output.putShort(input.getShort(pos + (2 * ch)));
        }

        //set input buffer to the end
        input.position(limit);
        output.flip();
    }
    //endregion

    /**
     * @return are any effects in the soundFx list active?
     */
    private boolean hasActiveEffects()
    {
        //check we have any effects first
        if (soundFx == null || soundFx.isEmpty()) return false;

        //check if any effect is enabled
        for (SoundFx fx : soundFx)
        {
            if (fx.isEnabled()) return true;
        }

        return false;
    }
}
