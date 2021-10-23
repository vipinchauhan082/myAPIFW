package com.payments.testing.bdd.util;

public class TextFondAndSize {
    private static String TextFont;
    private static float FontSize;
    private static float XCord;
    private static float YCord;
    private static float Width;
    private static float Height;

    TextFondAndSize(String textFont, float fontSize, float xCord, float yCord,
                    float width, float height) {
        TextFont = textFont;
        FontSize = fontSize;
        XCord = xCord;
        YCord = yCord;
        Width = width;
        Height = height;
    }

    public static String getTextFont() {
        return TextFont;
    }
    public static float getFontSize() {
        return FontSize;
    }
    public static float getXCord() {
        return XCord;
    }
    public static float getYCord() {
        return YCord;
    }
    public static float getWidth() {
        return Width;
    }
    public static float getHeight() {
        return Height;
    }
}
