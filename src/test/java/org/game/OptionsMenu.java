package org.game;

import java.awt.*;

public class OptionsMenu {
    private static float volume = 0.5f;
    private static Dimension resolution = new Dimension(1920, 1080);

    public static float getVolume() {
        return volume;
    }

    public static void setVolume(float newVolume) {
        volume = newVolume;
    }

    public static Dimension getResolution() {
        return resolution;
    }

    public static void setResolution(Dimension newResolution) {
        resolution = newResolution;
    }
}
