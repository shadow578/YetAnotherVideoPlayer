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

    View rootView;
    TextView textView;
    ImageView iconView;

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
        rootView = inflate(context, R.layout.component_quick_settings_button, this);

        //get sub- views
        textView = rootView.findViewById(R.id.comp_qs_button_text);
        iconView = rootView.findViewById(R.id.comp_qs_button_img);

        //set values of the views
        update();
    }

    /**
     * Update the ui component's values
     */
    private void update()
    {
        rootView.setBackgroundColor(backgroundTint);

        if (textView != null)
        {
            if (textStr != null && !textStr.isEmpty()) textView.setText(textStr);
            textView.setPaddingRelative((int) textIconPadding, textView.getPaddingTop(), (int) textIconPadding, textView.getPaddingBottom());
            textView.setTextColor(textColor);
        }

        if (iconView != null)
        {
            if (icon != null)
            {
                icon.setTint(iconTint);
                iconView.setImageDrawable(icon);
            }
            if (iconMinWidth != -1) iconView.setMinimumWidth((int) iconMinWidth);

            //use text as content description
            iconView.setContentDescription(textStr);
        }
    }

    public String getTextStr()
    {
        return textStr;
    }

    public void setTextStr(String textStr)
    {
        this.textStr = textStr;
        update();
    }

    public float getTextIconPadding()
    {
        return textIconPadding;
    }

    public void setTextIconPadding(float textIconPadding)
    {
        this.textIconPadding = textIconPadding;
        update();
    }

    public int getBackgroundTint()
    {
        return backgroundTint;
    }

    public void setBackgroundTint(int backgroundTint)
    {
        this.backgroundTint = backgroundTint;
        update();
    }

    public int getTextColor()
    {
        return textColor;
    }

    public void setTextColor(int textColor)
    {
        this.textColor = textColor;
        update();
    }

    public int getIconTint()
    {
        return iconTint;
    }

    public void setIconTint(int iconTint)
    {
        this.iconTint = iconTint;
        update();
    }

    public Drawable getIcon()
    {
        return icon;
    }

    public void setIcon(Drawable icon)
    {
        this.icon = icon;
        update();
    }

    public float getIconMinWidth()
    {
        return iconMinWidth;
    }

    public void setIconMinWidth(float iconMinWidth)
    {
        this.iconMinWidth = iconMinWidth;
        update();
    }
}
