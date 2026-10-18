package com.harshshah.docqa.ingestion.service;

import com.harshshah.docqa.ingestion.domain.Document;
import com.harshshah.docqa.ingestion.domain.DocumentPage;
import com.harshshah.docqa.ingestion.domain.DocumentPageRepository;
import com.harshshah.docqa.ingestion.domain.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class IngestionService {

    private static final int MIN_TOTAL_CHARS = 200;

    private final PdfTextExtractor extractor;
    private final DocumentRepository documents;
    private final DocumentPageRepository pages;

    public IngestionService(PdfTextExtractor extractor,
                            DocumentRepository documents,
                            DocumentPageRepository pages) {
        this.extractor = extractor;
        this.documents = documents;
        this.pages = pages;
    }

    public record IngestResult(Document document, boolean alreadyExisted) {}

    @Transactional
    public IngestResult ingest(String filename, byte[] bytes) {
        if (bytes.length < 5 || !new String(bytes, 0, 5, StandardCharsets.ISO_8859_1).startsWith("%PDF")) {
            throw new InvalidDocumentException("File is not a PDF.");
        }

        String hash = sha256(bytes);
        var existing = documents.findBySha256(hash);
        if (existing.isPresent()) {
            return new IngestResult(existing.get(), true);
        }

        List<String> pageTexts;
        try {
            pageTexts = extractor.extractPages(bytes);
        } catch (IOException e) {
            throw new InvalidDocumentException("Could not read PDF: " + e.getMessage());
        }

        long totalChars = pageTexts.stream().mapToLong(String::length).sum();
        if (totalChars < MIN_TOTAL_CHARS) {
            throw new InvalidDocumentException(
                    "PDF has almost no extractable text. It may be scanned; OCR is not supported.");
        }

        Document doc = documents.save(new Document(filename, pageTexts.size(), totalChars, hash));
        List<DocumentPage> entities = IntStream.range(0, pageTexts.size())
                .mapToObj(i -> new DocumentPage(doc.getId(), i + 1, pageTexts.get(i)))
                .toList();
        pages.saveAll(entities);

        return new IngestResult(doc, false);
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}