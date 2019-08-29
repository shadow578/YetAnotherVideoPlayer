package de.shadow578.yetanothervideoplayer.ui.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.shadow578.yetanothervideoplayer.R;

public class ControlQuickSettingsButton extends LinearLayout
{
    String textStr;
    float textIconPadding;

    int backgroundTint;
    int textColor;
    int iconTint;

    Drawable icon;
    float iconMinWidth;

    public ControlQuickSettingsButton(Context context)
    {
        super(context);

        //initialize the control
        init(context);
    }

    public ControlQuickSettingsButton(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        //get styleable attributes
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ControlQuickSettingsButton, 0, 0);

        try
        {
            //get values from attributes
            textStr = a.getString(R.styleable.ControlQuickSettingsButton_text);
            //textStyle = a.
            textIconPadding = a.getDimension(R.styleable.ControlQuickSettingsButton_textIconPadding, 0);

            backgroundTint = a.getColor(R.styleable.ControlQuickSettingsButton_backgroundTint, Color.TRANSPARENT);
            textColor = a.getColor(R.styleable.ControlQuickSettingsButton_textColor, Color.BLACK);
            iconTint = a.getColor(R.styleable.ControlQuickSettingsButton_iconTint, Color.WHITE);

            icon = a.getDrawable(R.styleable.ControlQuickSettingsButton_icon);
            iconMinWidth = a.getDimension(R.styleable.ControlQuickSettingsButton_iconMinWidth, -1f);
            //iconScaleTypeIndex = a.getValue(R.styleable.ControlQuickSettingsButton_iconScaleType, TypedValue.TYPE_);

        }
        finally
        {
            //free attributes
            a.recycle();
        }

        //initialize the control
        init(context);
    }

    /**
     * Initialize and inflate the control
     *
     * @param context the context to inflate in
     */
    private void init(Context context)
    {
        //inflate layout xml
        View root = inflate(context, R.layout.component_quick_settings_button, this);

        //get sub- views
        TextView txt = root.findViewById(R.id.comp_qs_button_text);
        ImageView ico = root.findViewById(R.id.comp_qs_button_img);

        //set values of the views
        root.setBackgroundColor(backgroundTint);

        if (txt != null)
        {
            if (textStr != null && !textStr.isEmpty()) txt.setText(textStr);
            txt.setPaddingRelative((int) textIconPadding, txt.getPaddingTop(), (int) textIconPadding, txt.getPaddingBottom());
            txt.setTextColor(textColor);
        }

        if (ico != null)
        {
            if (icon != null)
            {
                icon.setTint(iconTint);
                ico.setImageDrawable(icon);
            }
            if (iconMinWidth != -1) ico.setMinimumWidth((int) iconMinWidth);

            //use text as content description
            ico.setContentDescription(textStr);
        }
    }
}
