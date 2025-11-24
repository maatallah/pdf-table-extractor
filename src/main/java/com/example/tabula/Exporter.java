package com.example.tabula;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Exporter {

    public void export(List<TableData> tables, String outputPath, String format) throws IOException {
        switch (format.toLowerCase()) {
            case "csv":
                exportToCsv(tables, outputPath);
                break;
            case "json":
                exportToJson(tables, outputPath);
                break;
            case "xml":
                exportToXml(tables, outputPath);
                break;
            case "xlsx":
                exportToXlsx(tables, outputPath);
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private void exportToCsv(List<TableData> tables, String outputPath) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath)) {
            for (TableData table : tables) {
                for (List<String> row : table.getRows()) {
                    writer.write(String.join(",", row));
                    writer.write("\n");
                }
                writer.write("\n"); // Separate tables
            }
        }
    }

    private void exportToJson(List<TableData> tables, String outputPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(outputPath), tables);
    }

    private void exportToXml(List<TableData> tables, String outputPath) throws IOException {
        XmlMapper mapper = new XmlMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(outputPath), tables);
    }

    private void exportToXlsx(List<TableData> tables, String outputPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Extracted Data");
            int rowNum = 0;

            for (TableData table : tables) {
                for (List<String> rowData : table.getRows()) {
                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;
                    for (String cellData : rowData) {
                        row.createCell(colNum++).setCellValue(cellData);
                    }
                }
                rowNum++; // Empty row between tables
            }

            try (FileOutputStream fileOut = new FileOutputStream(outputPath)) {
                workbook.write(fileOut);
            }
        }
    }
}
