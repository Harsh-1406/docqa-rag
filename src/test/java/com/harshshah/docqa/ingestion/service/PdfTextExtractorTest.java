package com.harshshah.docqa.ingestion.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extractsOneStringPerPageInOrder() throws Exception {
        // Given
        byte[] pdf = buildPdf("Page one intro", "Page two revenue 42 million", "Page three risks");

        // When
        List<String> pages = extractor.extractPages(pdf);

        // Then
        assertThat(pages).hasSize(3);
        assertThat(pages.get(0)).contains("Page one intro");
        assertThat(pages.get(1)).contains("revenue 42 million");
        assertThat(pages.get(2)).contains("Page three risks");
    }

    @Test
    void cleanCollapsesBlankLinesAndTrims() {
        // Given & When
        String cleaned = PdfTextExtractor.clean("  Hello  \r\n\r\n\r\n\r\nWorld \n");

        // Then
        assertThat(cleaned).isEqualTo("Hello\n\nWorld");
    }

    private static byte[] buildPdf(String... pageTexts) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(font, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(text);
                    cs.endText();
                }
            }
            var out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}