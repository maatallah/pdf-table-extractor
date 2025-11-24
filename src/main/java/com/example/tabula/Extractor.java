package com.example.tabula;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.Rectangle;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;

public class Extractor {

    public List<TableData> extractTables(String inputPath, String pageRange, String area) throws IOException {
        List<TableData> result = new ArrayList<>();
        
        try (PDDocument document = PDDocument.load(new File(inputPath))) {
            ObjectExtractor oe = new ObjectExtractor(document);
            PageIterator iterator = oe.extract(parsePageRange(pageRange, document.getNumberOfPages()));

            while (iterator.hasNext()) {
                Page page = iterator.next();
                
                if (area != null && !area.isEmpty()) {
                    String[] coords = area.split(",");
                    if (coords.length == 4) {
                        float top = Float.parseFloat(coords[0]);
                        float left = Float.parseFloat(coords[1]);
                        float bottom = Float.parseFloat(coords[2]);
                        float right = Float.parseFloat(coords[3]);
                        page = page.getArea(new Rectangle(top, left, right - left, bottom - top));
                    }
                }

                // Try spreadsheet algorithm first (better for bordered tables)
                SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                List<Table> tables = sea.extract(page);
                
                // Fallback to basic if no tables found (better for whitespace-separated tables)
                if (tables.isEmpty()) {
                     BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
                     tables = bea.extract(page);
                }

                for (Table table : tables) {
                    List<List<String>> rows = new ArrayList<>();
                    for (List<technology.tabula.RectangularTextContainer> row : table.getRows()) {
                        List<String> rowData = new ArrayList<>();
                        for (technology.tabula.RectangularTextContainer cell : row) {
                            rowData.add(cell.getText());
                        }
                        rows.add(rowData);
                    }
                    if (!rows.isEmpty()) {
                        result.add(new TableData(rows, page.getPageNumber()));
                    }
                }
            }
            oe.close();
        }
        
        return result;
    }

    private List<Integer> parsePageRange(String pageRange, int totalPages) {
        List<Integer> pages = new ArrayList<>();
        if ("all".equalsIgnoreCase(pageRange)) {
            for (int i = 1; i <= totalPages; i++) {
                pages.add(i);
            }
        } else {
            String[] parts = pageRange.split(",");
            for (String part : parts) {
                if (part.contains("-")) {
                    String[] range = part.split("-");
                    int start = Integer.parseInt(range[0].trim());
                    int end = Integer.parseInt(range[1].trim());
                    for (int i = start; i <= end; i++) {
                        pages.add(i);
                    }
                } else {
                    pages.add(Integer.parseInt(part.trim()));
                }
            }
        }
        return pages;
    }
}
