package com.example;

public class App {

    public static void main(String[] args) {
        // Configurable file paths
        String inputFile = "zbx_export_templates.xml";
        String outputDirectory = "output";

        DomTemplateSplitterService splitterService = new DomTemplateSplitterService();

        try {
            splitterService.split(inputFile, outputDirectory);
            System.out.println("Templates split successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
