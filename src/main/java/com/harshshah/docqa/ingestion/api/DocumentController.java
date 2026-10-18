package com.harshshah.docqa.ingestion.api;

import com.harshshah.docqa.ingestion.domain.DocumentPage;
import com.harshshah.docqa.ingestion.service.DocumentQueryService;
import com.harshshah.docqa.ingestion.service.IngestionService;
import com.harshshah.docqa.ingestion.service.InvalidDocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final IngestionService ingestion;
    private final DocumentQueryService queries;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentSummary> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded file is empty.");
        }
        var result = ingestion.ingest(file.getOriginalFilename(), file.getBytes());
        HttpStatus status = result.alreadyExisted() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(DocumentSummary.from(result.document()));
    }

    @GetMapping
    public List<DocumentSummary> list() {
        return queries.listDocuments().stream().map(DocumentSummary::from).toList();
    }

    @GetMapping("/{id}/pages/{page}")
    public PageResponse page(@PathVariable UUID id, @PathVariable int page) {
        DocumentPage p = queries.getPage(id, page);
        return new PageResponse(p.getDocumentId(), p.getPageNumber(), p.getCharCount(), p.getText());
    }
}