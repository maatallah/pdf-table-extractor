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

    private List<String> firstHeaderRow = null; // Track first header across pages

    public List<TableData> extractTables(String inputPath, String pageRange, String area, FeedbackConfig config)
            throws IOException {
        List<TableData> result = new ArrayList<>();
        File inputFile = new File(inputPath);
        firstHeaderRow = null; // Reset for each extraction

        // Determine file-specific overrides
        FeedbackConfig.FileConfig fileConfig = null;
        if (config != null && config.getFiles() != null) {
            for (FeedbackConfig.FileConfig fc : config.getFiles()) {
                if (inputFile.getName().equals(fc.getFilename())) {
                    fileConfig = fc;
                    break;
                }
            }
        }

        try (PDDocument document = PDDocument.load(inputFile)) {
            ObjectExtractor oe = new ObjectExtractor(document);
            PageIterator iterator = oe.extract(parsePageRange(pageRange, document.getNumberOfPages()));

            while (iterator.hasNext()) {
                Page page = iterator.next();

                // Apply area override from config if present, otherwise use CLI arg
                String currentArea = area;

                if (fileConfig != null && fileConfig.getOverrides() != null
                        && fileConfig.getOverrides().getArea() != null) {
                    List<Float> a = fileConfig.getOverrides().getArea();
                    if (a.size() == 4) {
                        page = page
                                .getArea(new Rectangle(a.get(0), a.get(1), a.get(3) - a.get(1), a.get(2) - a.get(0)));
                        currentArea = null;
                    }
                }

                if (currentArea != null && !currentArea.isEmpty()) {
                    String[] coords = currentArea.split(",");
                    if (coords.length == 4) {
                        float top = Float.parseFloat(coords[0]);
                        float left = Float.parseFloat(coords[1]);
                        float bottom = Float.parseFloat(coords[2]);
                        float right = Float.parseFloat(coords[3]);
                        page = page.getArea(new Rectangle(top, left, right - left, bottom - top));
                    }
                }

                List<Table> tables = new ArrayList<>();
                boolean useLattice = config != null && config.getGlobal() != null
                        && config.getGlobal().isLattice_mode();
                boolean useStream = config != null && config.getGlobal() != null && config.getGlobal().isStream_mode();

                // Use configured algorithm
                if (useLattice) {
                    SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                    tables.addAll(sea.extract(page));
                } else if (useStream) {
                    BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
                    tables.addAll(bea.extract(page));
                } else {
                    // Default behavior: Try spreadsheet (lattice) first, then basic (stream)
                    SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                    tables = sea.extract(page);
                    if (tables.isEmpty()) {
                        BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
                        tables = bea.extract(page);
                    }
                }

                for (Table table : tables) {
                    List<List<String>> rows = new ArrayList<>();
                    boolean startFound = false;
                    String startMarker = (fileConfig != null && fileConfig.getOverrides() != null)
                            ? fileConfig.getOverrides().getStartMarker()
                            : null;
                    String endMarker = (fileConfig != null && fileConfig.getOverrides() != null)
                            ? fileConfig.getOverrides().getEndMarker()
                            : null;

                    for (List<technology.tabula.RectangularTextContainer> row : table.getRows()) {
                        List<String> rowData = new ArrayList<>();
                        StringBuilder rowText = new StringBuilder();

                        for (technology.tabula.RectangularTextContainer cell : row) {
                            // Replace comma decimal separator with period
                            String cellText = cell.getText().replace(",", ".");
                            rowData.add(cellText);
                            rowText.append(cellText).append(" ");
                        }

                        String rowString = rowText.toString().trim();

                        // Skip empty rows
                        if (rowString.isEmpty()) {
                            continue;
                        }

                        // Filter out page headers (e.g., "Page 1 of 2")
                        if (rowString.matches(".*Page\\s+\\d+\\s+of\\s+\\d+.*")) {
                            continue;
                        }

                        // Check for end marker first
                        if (endMarker != null && rowString.contains(endMarker)) {
                            break; // Stop processing rows
                        }

                        // Check for start marker
                        if (startMarker != null && !startFound) {
                            if (rowString.contains(startMarker)) {
                                startFound = true;
                                // Store first header if not already stored
                                if (firstHeaderRow == null) {
                                    firstHeaderRow = new ArrayList<>(rowData);
                                    rows.add(rowData); // Include the header row only once
                                }
                                // Skip duplicate headers on subsequent pages
                                else {
                                    continue;
                                }
                            }
                            continue; // Skip rows before start marker
                        }

                        // If no markers configured, or start marker found
                        if (startMarker == null || startFound) {
                            rows.add(rowData);
                        }
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
