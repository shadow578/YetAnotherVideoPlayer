package de.shadow578.yetanothervideoplayer.util;

public final class ConfigKeys
{
    /**
     * double press to exit enable (true/false)
     */
    public static final String KEY_BACK_DOUBLE_PRESS_EN = "BACK_DOUBLE_PRESS_EN";

    /**
     * double press to exit timeout (ms)
     */
    public static final String KEY_BACK_DOUBLE_PRESS_TIMEOUT = "BACK_DOUBLE_PRESS_TIMEOUT";

    /**
     * swipe gesture enable (true/false)
     */
    public static final String KEY_SWIPE_GESTURE_EN = "SWIPE_GESTURE_EN";

    /**
     * blitheness step size (%, 0-100)
     */
    public static final String KEY_BRIGHTNESS_ADJUST_STEP = "BRIGHTNESS_ADJUST_STEP";

    /**
     * hard swipe for auto brightness enable (true/false)
     */
    public static final String KEY_BRIGHTNESS_HARD_SWIPE_EN = "BRIGHTNESS_HARD_SWIPE_EN";

    /**
     * brightness hard swipe threshold (dp)
     */
    public static final String KEY_BRIGHTNESS_HARD_SWIPE_THRESHOLD = "BRIGHTNESS_HARD_SWIPE_THRESHOLD";

    /**
     * info text show duration (ms)
     */
    public static final String KEY_INFO_TEXT_DURATION = "INFO_TEXT_DURATION";

    /**
     * swipe dead zone rect top value (dp)
     */
    public static final String KEY_SWIPE_DEAD_ZONE_RECT_TOP = "SWIPE_DEAD_ZONE_RECT_TOP";

    /**
     * swipe dead zone rect bottom value (dp)
     */
    public static final String KEY_SWIPE_DEAD_ZONE_RECT_BOTTOM = "SWIPE_DEAD_ZONE_RECT_BOTTOM";

    /**
     * swipe dead zone rect left value (dp)
     */
    public static final String KEY_SWIPE_DEAD_ZONE_RECT_LEFT = "SWIPE_DEAD_ZONE_RECT_LEFT";

    /**
     * swipe dead zone rect right value (dp)
     */
    public static final String KEY_SWIPE_DEAD_ZONE_RECT_RIGHT = "SWIPE_DEAD_ZONE_RECT_RIGTH";

    /**
     * touch decay time (ms)
     */
    public static final String KEY_TOUCH_DECAY_TIME = "TOUCH_DECAY_TIME";

    /**
     * swipe + fling threshold values (dp)
     */
    public static final String KEY_SWIPE_FLING_THRESHOLD = "SWIPE_FLING_THRESHOLD";

    /**
     * player auto- start playback (true/false)
     */
    public static final String KEY_AUTO_PLAY = "AUTO_PLAY";

    /**
     * player auto- close (true/false)
     */
    public static final String KEY_CLOSE_WHEN_FINISHED_PLAYING = "CLOSE_WHEN_FINISHED_PLAYING";

    /**
     * player enter pip when minimizing app (true/false)
     */
    public static final String KEY_ENTER_PIP_ON_LEAVE = "ENTER_PIP_ON_LEAVE";

    /**
     * player seek button increment (ms)
     */
    public static final String KEY_SEEK_BUTTON_INCREMENT = "SEEK_BUTTON_INCREMENT";

    /**
     * persistent brightness enable (true/false)
     */
    public static final String KEY_PERSIST_BRIGHTNESS_EN = "PERSIST_BRIGHTNESS_EN";

    /**
     * persistent brightness value (%, 0-100)
     */
    public static final String KEY_PERSIST_BRIGHTNESS = "PERSIST_BRIGHTNESS";

    /**
     * persistent volume enable (true/false)
     */
    public static final String KEY_PERSIST_VOLUME_EN = "PERSIST_VOLUME_EN";

    /**
     * persistent volume value (audio manager volume index)
     */
    public static final String KEY_PERSIST_VOLUME = "PERSIST_VOLUME";

    /**
     * anime4k fps limiter enable (true/false)
     */
    public static final String KEY_ANIME4K_FPS_LIMIT_ENABLE = "ANIME4K_FPS_LIMIT_EN";

    /**
     * fps limit value for anime4k filter
     */
    public static final String KEY_ANIME4K_FPS_LIMIT = "ANIME4K_FPS_LIMIT";

    /**
     * low battery warning enable (warn if battery less than threshold)
     */
    public static final String KEY_BATTERY_WARN_ENABLE = "BATTERY_WARN_EN";

    /**
     * threshold at which a battery warning is shown
     */
    public static final String KEY_BATTERY_WARN_THRESHOLD = "BATTERY_WARN_THRESHOLD";

    /**
     * last played video title value ("resume where i left off")
     */
    public static final String KEY_LAST_PLAYED_TITLE = "LAST_PLAYED_TITLE";

    /**
     * last played video url value ("resume where i left off")
     */
    public static final String KEY_LAST_PLAYED_URL = "LAST_PLAYED_URL";

    /**
     * last played video progress (=position) value ("resume where i left off")
     */
    public static final String KEY_LAST_PLAYED_POSITION = "LAST_PLAYED_POSITION";
}
