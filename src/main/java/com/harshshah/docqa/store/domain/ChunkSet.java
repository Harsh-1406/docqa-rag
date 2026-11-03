package com.harshshah.docqa.store.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chunk_sets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChunkSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String strategy;

    @Column(name = "chunk_size", nullable = false)
    private int chunkSize;

    @Column(name = "chunk_overlap", nullable = false)
    private int chunkOverlap;

    @Column(name = "embedding_model", nullable = false)
    private String embeddingModel;

    @Column(nullable = false)
    private int dimensions;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ChunkSet(UUID documentId, String label, String strategy, int chunkSize, int chunkOverlap,
                    String embeddingModel, int dimensions, int chunkCount) {
        this.documentId = documentId;
        this.label = label;
        this.strategy = strategy;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.embeddingModel = embeddingModel;
        this.dimensions = dimensions;
        this.chunkCount = chunkCount;
    }
}