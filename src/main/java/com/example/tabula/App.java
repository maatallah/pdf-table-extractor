package com.example.tabula;

import org.apache.commons.cli.*;
import java.io.File;
import java.util.List;

public class App {
    public static void main(String[] args) {
        Options options = new Options();

        Option input = new Option("i", "input", true, "Input PDF file path");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "Output file path");
        output.setRequired(true);
        options.addOption(output);

        Option format = new Option("f", "format", true, "Output format (csv, json, xml, xlsx)");
        format.setRequired(true);
        options.addOption(format);

        Option pages = new Option("p", "pages", true, "Page range (e.g., 1-3, 5, all)");
        pages.setRequired(false);
        options.addOption(pages);

        Option area = new Option("a", "area", true, "Extraction area (top,left,bottom,right)");
        area.setRequired(false);
        options.addOption(area);

        Option feedback = new Option("fb", "feedback", true, "Path to feedback YAML file");
        feedback.setRequired(false);
        options.addOption(feedback);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("pdf-table-extractor", options);
            System.exit(1);
            return;
        }

        String inputPath = cmd.getOptionValue("input");
        String outputPath = cmd.getOptionValue("output");
        String formatValue = cmd.getOptionValue("format");
        String pageRange = cmd.getOptionValue("pages", "all");
        String areaValue = cmd.getOptionValue("area");
        String feedbackPath = cmd.getOptionValue("feedback");

        System.out.println("Input: " + inputPath);
        System.out.println("Output: " + outputPath);
        System.out.println("Format: " + formatValue);
        System.out.println("Pages: " + pageRange);

        try {
            Extractor extractor = new Extractor();
            // TODO: Configure extractor with feedback if provided
            
            List<TableData> tables = extractor.extractTables(inputPath, pageRange, areaValue);
            
            Exporter exporter = new Exporter();
            exporter.export(tables, outputPath, formatValue);
            
            System.out.println("Extraction complete. Output saved to " + outputPath);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
