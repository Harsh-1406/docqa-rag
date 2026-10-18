package com.harshshah.docqa.ingestion.service;

import com.harshshah.docqa.common.NotFoundException;
import com.harshshah.docqa.ingestion.domain.Document;
import com.harshshah.docqa.ingestion.domain.DocumentPage;
import com.harshshah.docqa.ingestion.domain.DocumentPageRepository;
import com.harshshah.docqa.ingestion.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentQueryService {

    private final DocumentRepository documents;
    private final DocumentPageRepository pages;

    public List<Document> listDocuments() {
        return documents.findAll();
    }

    public DocumentPage getPage(UUID documentId, int pageNumber) {
        return pages.findByDocumentIdAndPageNumber(documentId, pageNumber)
                .orElseThrow(() -> new NotFoundException(
                        "Page " + pageNumber + " not found for document " + documentId));
    }
}