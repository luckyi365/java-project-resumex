package com.resumex.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Locale;

/**
 * Service to extract plain text from uploaded files (.pdf, .docx, .txt).
 */
public class ResumeParser {

    /** Maximum allowed file size (10 MB). */
    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024;

    /**
     * Extracts text from the given file based on its extension.
     *
     * @param file the uploaded file
     * @return the extracted plain text
     * @throws ResumeXException if parsing fails or file is invalid
     */
    public String parse(File file) throws ResumeXException {
        if (file == null || !file.exists()) {
            throw new ResumeXException("File does not exist.");
        }
        if (file.length() > MAX_SIZE_BYTES) {
            throw new ResumeXException("File is too large. Maximum size is 10 MB.");
        }

        String name = file.getName().toLowerCase(Locale.ROOT);
        String text;
        try {
            if (name.endsWith(".pdf")) {
                text = parsePdf(file);
            } else if (name.endsWith(".docx")) {
                text = parseDocx(file);
            } else if (name.endsWith(".txt")) {
                text = Files.readString(file.toPath());
            } else {
                throw new ResumeXException("Unsupported file type: " + name + ". Please upload a PDF, DOCX, or TXT.");
            }
        } catch (ResumeXException e) {
            throw e;
        } catch (Exception e) {
            throw new ResumeXException("Failed to read file: " + e.getMessage(), e);
        }

        if (text == null || text.trim().isEmpty()) {
            throw new ResumeXException("No text could be extracted. The document might be an image/scanned PDF without OCR.");
        }
        return text.trim();
    }

    private String parsePdf(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                throw new ResumeXException("Cannot parse encrypted PDF.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
