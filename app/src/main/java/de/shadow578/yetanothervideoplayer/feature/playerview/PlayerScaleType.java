package de.shadow578.yetanothervideoplayer.feature.playerview;

/**
 * Different scale types for the {@link YavpPlayerView} and {@link YavpEPlayerView}
 */
public enum PlayerScaleType
{
    /**
     * Scale the video to the size of the player view
     */
    None,

    /**
     * Scale the video so that either the width or height matches the size of the player view, but don't crop the video
     */
    Fit,

    /**
     * Scale the video so that the width matches the width of the player view, and crop the height as needed
     */
    FillWidth,

    /**
     * Scale the video so that the height matches the height of the player view, and crop the width as needed
     */
    FillHeight,
}
