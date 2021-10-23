package com.payments.testing.bdd.util;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.*;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetImageLocationsAndSize extends PDFStreamEngine {

    public static ImageLocAndSize imageLocAndSize;
    public static List<ImageLocAndSize> imageLocAndSizeList = new ArrayList<>();
    public static int pageCount;

    public GetImageLocationsAndSize() {
        // preparing PDFStreamEngine
        addOperator(new Concatenate());
        addOperator(new DrawObject());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new SetMatrix());
    }

    public void getImageLocAndSize(PDDocument document) throws IOException {
        GetImageLocationsAndSize printer = new GetImageLocationsAndSize();
        pageCount = document.getPages().getCount();
        for (PDPage page : document.getPages()) {
            printer.processPage(page);
        }
    }

    public void getImageLocAndSizeForAPage(PDDocument document, int pageNumber) throws IOException {
        GetImageLocationsAndSize printer = new GetImageLocationsAndSize();
        printer.processPage(document.getPage(pageNumber));
    }

    protected void processOperator(Operator operator, List<COSBase> operands)
            throws IOException {
        String operation = operator.getName();
        if ("Do".equals(operation)) {
            COSName objectName = (COSName) operands.get(0);
            // get the PDF object
            PDXObject xobject = getResources().getXObject(objectName);

            // check if the object is an image object
            if (xobject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject) xobject;
                // raw size in pixels
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();

                // displayed size in user space units
                float imageXScale = ctmNew.getScalingFactorX();
                float imageYScale = ctmNew.getScalingFactorY();

                // position of image in the PDF in terms of user space units
                float PDFXImage = ctmNew.getTranslateX();
                float PDFYImage = ctmNew.getTranslateY();

                imageLocAndSize = new ImageLocAndSize(objectName.getName(), imageXScale, imageYScale,
                        PDFXImage, PDFYImage, imageWidth, imageHeight);
                imageLocAndSizeList.add(imageLocAndSize);

            } else if (xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject) xobject;
                showForm(form);
            }
        } else {
            super.processOperator(operator, operands);
        }
    }
}
