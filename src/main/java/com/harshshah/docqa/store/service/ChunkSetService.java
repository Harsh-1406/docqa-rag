package com.harshshah.docqa.store.service;

import com.harshshah.docqa.common.NotFoundException;
import com.harshshah.docqa.store.domain.ChunkSet;
import com.harshshah.docqa.store.domain.ChunkSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChunkSetService {

    private final ChunkSetRepository chunkSets;

    public ChunkSet require(UUID documentId, String label) {
        return chunkSets.findByDocumentIdAndLabel(documentId, label)
                .orElseThrow(() -> new NotFoundException(
                        "No index for '" + label + "'. Call POST /api/documents/{id}/index first."));
    }
}