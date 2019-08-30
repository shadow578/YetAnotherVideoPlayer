package de.shadow578.yetanothervideoplayer.ui.components;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import mobi.upod.timedurationpicker.TimeDurationPickerDialog;
import mobi.upod.timedurationpicker.TimeDurationPicker;

/**
 * Basically the (unmodified) mobi.upod.timedurationpicker.TimeDurationPickerDialogFragment class, but using
 * androidX Fragment instead.
 * Usage is the same as original
 *
 * @see mobi.upod.timedurationpicker.TimeDurationPickerDialogFragment
 */
public abstract class TimeDurationPickerDialogFragmentX extends DialogFragment implements TimeDurationPickerDialog.OnDurationSetListener
{
    @NonNull
    @SuppressWarnings("ConstantConditions")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        return new TimeDurationPickerDialog(getActivity(), this, getInitialDuration(), setTimeUnits());
    }

    @SuppressWarnings("SameReturnValue")
    protected long getInitialDuration()
    {
        return 0;
    }

    @SuppressWarnings("SameReturnValue")
    protected int setTimeUnits()
    {
        return TimeDurationPicker.HH_MM_SS;
    }
}
