package com.example.tabula;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

public class GenerateSamplePdf {
    public static void main(String[] args) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Sample Table for Extraction");
                contentStream.endText();

                // Draw table headers
                int y = 650;
                int margin = 50;
                int colWidth = 100;

                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 12);
                drawCell(contentStream, margin, y, "ID");
                drawCell(contentStream, margin + colWidth, y, "Name");
                drawCell(contentStream, margin + 2 * colWidth, y, "Role");

                contentStream.setFont(PDType1Font.HELVETICA, 12);
                y -= 20;
                drawCell(contentStream, margin, y, "1");
                drawCell(contentStream, margin + colWidth, y, "Alice");
                drawCell(contentStream, margin + 2 * colWidth, y, "Engineer");

                y -= 20;
                drawCell(contentStream, margin, y, "2");
                drawCell(contentStream, margin + colWidth, y, "Bob");
                drawCell(contentStream, margin + 2 * colWidth, y, "Designer");

                y -= 20;
                drawCell(contentStream, margin, y, "3");
                drawCell(contentStream, margin + colWidth, y, "Charlie");
                drawCell(contentStream, margin + 2 * colWidth, y, "Manager");
            }

            doc.save("sample.pdf");
            System.out.println("Created sample.pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void drawCell(PDPageContentStream contentStream, int x, int y, String text) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
}
