package com.harshshah.docqa.ingestion.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "char_count", nullable = false)
    private long charCount;

    @Column(nullable = false, length = 64, unique = true)
    private String sha256;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    public Document(String filename, int pageCount, long charCount, String sha256) {
        this.filename = filename;
        this.pageCount = pageCount;
        this.charCount = charCount;
        this.sha256 = sha256;
    }
}