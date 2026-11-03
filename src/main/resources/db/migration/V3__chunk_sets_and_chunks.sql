-- One chunk_set = one document chunked with one configuration and embedded with one model.
CREATE TABLE chunk_sets (
                            id              UUID PRIMARY KEY,
                            document_id     UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                            label           VARCHAR(100) NOT NULL,
                            strategy        VARCHAR(20)  NOT NULL,
                            chunk_size      INT          NOT NULL,
                            chunk_overlap   INT          NOT NULL,
                            embedding_model VARCHAR(100) NOT NULL,
                            dimensions      INT          NOT NULL,
                            chunk_count     INT          NOT NULL,
                            created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                            UNIQUE (document_id, label)
);

CREATE TABLE chunks (
                        id           BIGSERIAL PRIMARY KEY,
                        chunk_set_id UUID         NOT NULL REFERENCES chunk_sets(id) ON DELETE CASCADE,
                        chunk_index  INT          NOT NULL,
                        start_char   INT          NOT NULL,
                        end_char     INT          NOT NULL,
                        start_page   INT          NOT NULL,
                        end_page     INT          NOT NULL,
                        content      TEXT         NOT NULL,
                        embedding    vector(768)  NOT NULL,
                        UNIQUE (chunk_set_id, chunk_index)
);

CREATE INDEX chunks_embedding_hnsw ON chunks USING hnsw (embedding vector_cosine_ops);