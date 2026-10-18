CREATE TABLE documents (
                           id           UUID PRIMARY KEY,
                           filename     VARCHAR(255) NOT NULL,
                           page_count   INT          NOT NULL,
                           char_count   BIGINT       NOT NULL,
                           sha256       VARCHAR(64)  NOT NULL UNIQUE,
                           uploaded_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE document_pages (
                                id           BIGSERIAL PRIMARY KEY,
                                document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                page_number  INT  NOT NULL,
                                text         TEXT NOT NULL,
                                char_count   INT  NOT NULL,
                                UNIQUE (document_id, page_number)
);