package com.payments.testing.bdd.util;

public class ImageLocAndSize {
    private static String ImageName;
    private static float ImageXScale;
    private static float ImageYScale;
    private static float PDFXImage;
    private static float PDFYImage;
    private static int ImageWidth;
    private static int ImageHeight;

    ImageLocAndSize(String imageName, float imageXScale, float imageYScale, float pdfXImage,
                    float pdfYImage, int imageWidth, int imageHeight) {
        ImageName = imageName;
        ImageXScale = imageXScale;
        ImageYScale = imageYScale;
        PDFXImage = pdfXImage;
        PDFYImage = pdfYImage;
        ImageWidth = imageWidth;
        ImageHeight = imageHeight;
    }

    public static String getImageName() {
        return ImageName;
    }
    public static float getImageXScale() {
        return ImageXScale;
    }
    public static float getImageYScale() {
        return ImageYScale;
    }
    public static float getPDFXImage() {
        return PDFXImage;
    }
    public static float getPDFYImage() {
        return PDFYImage;
    }
    public static int getImageWidth() {
        return ImageWidth;
    }
    public static int getImageHeight() {
        return ImageHeight;
    }
}
