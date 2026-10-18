package com.harshshah.docqa.ingestion.service;

import com.harshshah.docqa.ingestion.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    private static final byte[] PDF = "%PDF-1.7 fake".getBytes(StandardCharsets.ISO_8859_1);

    @Mock
    PdfTextExtractor extractor;

    @Mock
    DocumentRepository documents;

    @Mock
    DocumentPageRepository pages;

    @InjectMocks
    IngestionService service;

    @Captor
    ArgumentCaptor<List<DocumentPage>> pagesCaptor;

    @Test
    void rejectsNonPdfBeforeTouchingAnything() {
        // Given, When & Then
        assertThatThrownBy(() -> service.ingest("a.txt", "hello world".getBytes()))
                .isInstanceOf(InvalidDocumentException.class);
        verifyNoInteractions(extractor, documents, pages);
    }

    @Test
    void duplicateUploadReturnsExistingWithoutReparsing() {
        // Given
        Document existing = new Document("a.pdf", 3, 900, "hash");
        when(documents.findBySha256(anyString())).thenReturn(Optional.of(existing));

        // When
        var result = service.ingest("a.pdf", PDF);

        // Then
        assertThat(result.alreadyExisted()).isTrue();
        assertThat(result.document()).isSameAs(existing);
        verifyNoInteractions(extractor, pages);
    }

    @Test
    void rejectsPdfWithAlmostNoText() throws Exception {
        // Given
        when(documents.findBySha256(anyString())).thenReturn(Optional.empty());
        when(extractor.extractPages(PDF)).thenReturn(List.of("", "  "));

        // When & Then
        assertThatThrownBy(() -> service.ingest("scan.pdf", PDF))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("scanned");
        verify(documents, never()).save(any());
    }

    @Test
    void savesDocumentAndOnePageEntityPerPage() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        String text = "x".repeat(150);
        when(documents.findBySha256(anyString())).thenReturn(Optional.empty());
        when(extractor.extractPages(PDF)).thenReturn(List.of(text, text));
        when(documents.save(any(Document.class))).thenAnswer(inv -> {
            Document d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", id);   // JPA would normally generate this
            return d;
        });

        // When
        var result = service.ingest("report.pdf", PDF);

        // Then
        assertThat(result.alreadyExisted()).isFalse();
        assertThat(result.document().getPageCount()).isEqualTo(2);
        assertThat(result.document().getCharCount()).isEqualTo(300);
        verify(pages).saveAll(pagesCaptor.capture());
        assertThat(pagesCaptor.getValue())
                .extracting(DocumentPage::getPageNumber).containsExactly(1, 2);
        assertThat(pagesCaptor.getValue())
                .allMatch(p -> p.getDocumentId().equals(id));
    }
}