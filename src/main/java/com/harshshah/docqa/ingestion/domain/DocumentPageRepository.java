package com.harshshah.docqa.ingestion.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentPageRepository extends JpaRepository<DocumentPage, Long> {
    Optional<DocumentPage> findByDocumentIdAndPageNumber(UUID documentId, int pageNumber);
    List<DocumentPage> findByDocumentIdOrderByPageNumber(UUID documentId);
}