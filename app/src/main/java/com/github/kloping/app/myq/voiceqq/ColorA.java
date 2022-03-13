package com.github.kloping.app.myq.voiceqq;

import android.graphics.Color;

/**
 * @author kloping
 */
public enum ColorA {
    RESET("\u001B[0m", Color.parseColor("#FF1C1D2C")),

    WHITE("\u001B[30m", Color.WHITE),
    RED("\u001B[31m", Color.RED),
    EMERALD_GREEN("\u001B[32m", Color.GREEN),
    GOLD("\u001B[33m", Color.YELLOW),
    BLUE("\u001B[34m", Color.BLUE),
    PURPLE("\u001B[35m", Color.BLACK),
    GREEN("\u001B[36m", Color.GREEN),

    GRAY("\u001B[90m", Color.GRAY),
    LIGHT_RED("\u001B[91m", Color.RED),
    LIGHT_GREEN("\u001B[92m", Color.parseColor("#FF009732")),
    LIGHT_YELLOW("\u001B[93m", Color.YELLOW),
    LIGHT_BLUE("\u001B[94m", Color.BLUE),
    LIGHT_PURPLE("\u001B[95m", Color.BLACK),
    LIGHT_CYAN("\u001B[96m", Color.CYAN);

    private String m1;
    private int color;

    public static Object[] getColorString(String m1) {
        int color = Color.BLACK;
        if (m1.startsWith("\u001B["))
            for (ColorA a : ColorA.values()) {
                if (m1.startsWith(a.getM1())) {
                    try {
                        int i = m1.lastIndexOf(ColorA.RESET.getM1());
                        if (i > 0)
                            m1 = m1.substring(a.getM1().length(), i);
                        color = a.getColor();
                    } catch (Exception ignored) {
                    }
                    continue;
                }
            }
        return new Object[]{m1, color};
    }

    public int getColor() {
        return color;
    }

    public String getM1() {
        return m1;
    }

    ColorA(String m1, int color) {
        this.m1 = m1;
        this.color = color;
    }
}