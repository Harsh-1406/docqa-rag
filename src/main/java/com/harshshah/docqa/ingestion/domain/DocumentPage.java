package com.harshshah.docqa.ingestion.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "document_pages")
public class DocumentPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "char_count", nullable = false)
    private int charCount;

    protected DocumentPage() {}

    public DocumentPage(UUID documentId, int pageNumber, String text) {
        this.documentId = documentId;
        this.pageNumber = pageNumber;
        this.text = text;
        this.charCount = text.length();
    }

    public Long getId() { return id; }
    public UUID getDocumentId() { return documentId; }
    public int getPageNumber() { return pageNumber; }
    public String getText() { return text; }
    public int getCharCount() { return charCount; }
}