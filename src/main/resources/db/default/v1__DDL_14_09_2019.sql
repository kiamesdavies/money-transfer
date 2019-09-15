
DROP TABLE IF EXISTS "journal";

CREATE TABLE IF NOT EXISTS "journal" (
                                                "ordering" BIGINT AUTO_INCREMENT,
                                                "persistence_id" VARCHAR(255) NOT NULL,
                                                "sequence_number" BIGINT NOT NULL,
                                                "deleted" BOOLEAN DEFAULT FALSE,
                                                "tags" VARCHAR(255) DEFAULT NULL,
                                                "message" BYTEA NOT NULL,
                                                PRIMARY KEY("persistence_id", "sequence_number")
);

CREATE UNIQUE INDEX "journal_ordering_idx" ON "journal"("ordering");

DROP TABLE IF EXISTS "snapshot";

CREATE TABLE IF NOT EXISTS "snapshot" (
                                                 "persistence_id" VARCHAR(255) NOT NULL,
                                                 "sequence_number" BIGINT NOT NULL,
                                                 "created" BIGINT NOT NULL,
                                                 "snapshot" BYTEA NOT NULL,
                                                 PRIMARY KEY("persistence_id", "sequence_number")
);

