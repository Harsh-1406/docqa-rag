package com.harshshah.docqa.store.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChunkSetRepository extends JpaRepository<ChunkSet, UUID> {
    Optional<ChunkSet> findByDocumentIdAndLabel(UUID documentId, String label);

    List<ChunkSet> findByDocumentIdOrderByCreatedAt(UUID documentId);
}