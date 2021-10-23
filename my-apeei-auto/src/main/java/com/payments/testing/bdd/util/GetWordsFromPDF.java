package com.payments.testing.bdd.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetWordsFromPDF extends PDFTextStripper {

    public GetWordsFromPDF() throws IOException {
    }

    Integer lineNumber = 0;
    private static String termToFind;
    public static Map<Integer, TextFondAndSize> lineNumberCheck = new HashMap<>();

    public void getWordsFromPdf(PDDocument document, String termToExtract, Integer pageNumber) throws IOException {
        PDFTextStripper stripper = new GetWordsFromPDF();
        stripper.setSortByPosition(true);
        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);
        termToFind = termToExtract;
        Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
        stripper.writeText(document, dummy);
    }

    protected void writeString(String str, List<TextPosition> textPositions) throws IOException {
        lineNumber++;
        if (str.contains(termToFind)) {
            int index = str.indexOf(termToFind);
            String font = textPositions.get(index).getFont().toString();
            float fontSize = textPositions.get(index).getFontSize();
            float xPos = textPositions.get(index).getX();
            float yPos = textPositions.get(index).getY();
            float width = textPositions.get(index).getWidth();
            float height = textPositions.get(index).getHeight();
            TextFondAndSize textFondAndSize = new TextFondAndSize(font, fontSize, xPos, yPos, width, height);
            lineNumberCheck.put(lineNumber, textFondAndSize);
        }

    }

}

