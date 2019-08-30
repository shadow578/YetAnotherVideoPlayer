package de.shadow578.yetanothervideoplayer.ui;

import androidx.fragment.app.FragmentManager;

import com.google.android.exoplayer2.Player;

import de.shadow578.yetanothervideoplayer.ui.components.TimeDurationPickerDialogFragmentX;
import de.shadow578.yetanothervideoplayer.util.Logging;
import mobi.upod.timedurationpicker.TimeDurationPicker;

public class JumpToFragment extends TimeDurationPickerDialogFragmentX
{
    /**
     * The Player to seek in
     */
    private Player player;

    void show(FragmentManager manager, Player _player)
    {
        player = _player;
        super.show(manager, "tag_jump_to_dialog");
    }

    @Override
    protected long getInitialDuration()
    {
        //always start at 0 seconds
        return 0;
    }

    @Override
    protected int setTimeUnits()
    {
        //check if player is valid
        if (player == null)
        {
            Logging.logE("JumpToFragment: player is null!");
            return TimeDurationPicker.HH_MM_SS;
        }

        if (player.getDuration() > (60 * 60 * 1000))
        {
            //video is longer than 1 hour, hour part in duration picker may come in handy
            return TimeDurationPicker.HH_MM_SS;
        }

        return TimeDurationPicker.MM_SS;
    }

    @Override
    public void onDurationSet(TimeDurationPicker view, long duration)
    {
        //check if player is valid
        if (player == null)
        {
            Logging.logE("JumpToFragment: player is null! cannot seek to target");
            return;
        }

        //user choose duration, check bounds
        Logging.logD("Jump-TO: user input %d", duration);
        if (duration < 0) duration = 0;
        if (duration >= player.getDuration()) duration = player.getDuration();

        //seek to position + start playing
        Logging.logD("Jump-TO: seeking to %d", duration);
        player.seekTo(duration);
        player.setPlayWhenReady(true);
    }
}
