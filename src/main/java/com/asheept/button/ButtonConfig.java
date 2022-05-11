package com.asheept.button;

import org.bukkit.configuration.file.FileConfiguration;

public class ButtonConfig {

    public static int timerTicks = 600;

    static void load(FileConfiguration config) {
        timerTicks = config.getInt("timer-ticks");
    }
}
