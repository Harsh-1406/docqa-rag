package com.harshshah.docqa.ingestion.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfTextExtractor {

    /** Returns one cleaned string per page. Index 0 is page 1. */
    public List<String> extractPages(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int pageCount = doc.getNumberOfPages();
            List<String> pages = new ArrayList<>(pageCount);
            for (int p = 1; p <= pageCount; p++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);   // helps keep table rows together
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                pages.add(clean(stripper.getText(doc)));
            }
            return pages;
        }
    }

    static String clean(String raw) {
        return raw
                .replace("\r\n", "\n")
                .replace('\u00A0', ' ')          // non-breaking space
                .replaceAll("[ \\t]+\n", "\n")   // trailing spaces
                .replaceAll("\n{3,}", "\n\n")    // collapse blank-line runs
                .trim();
    }
}