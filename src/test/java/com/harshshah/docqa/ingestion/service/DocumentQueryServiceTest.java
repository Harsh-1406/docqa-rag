package com.harshshah.docqa.ingestion.service;

import com.harshshah.docqa.common.NotFoundException;
import com.harshshah.docqa.ingestion.domain.DocumentPage;
import com.harshshah.docqa.ingestion.domain.DocumentPageRepository;
import com.harshshah.docqa.ingestion.domain.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentQueryServiceTest {

    @Mock DocumentRepository documents;
    @Mock DocumentPageRepository pages;
    @InjectMocks DocumentQueryService service;

    @Test
    void getPageReturnsPageWhenPresent() {
        UUID id = UUID.randomUUID();
        DocumentPage page = new DocumentPage(id, 5, "some text");
        when(pages.findByDocumentIdAndPageNumber(id, 5)).thenReturn(Optional.of(page));

        assertThat(service.getPage(id, 5)).isSameAs(page);
    }

    @Test
    void getPageThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(pages.findByDocumentIdAndPageNumber(id, 999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPage(id, 999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }
}