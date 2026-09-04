-- DACN Commit 6: hybrid rule-based + LLM-assisted recommendation history.
-- Run exactly once after V008.
-- AI is advisory only. Hard constraints and final invitation decisions remain in backend/human workflow.

SET NAMES utf8mb4;

USE wedding_staff_management;

CREATE TABLE ai_recommendation_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    replacement_request_id BIGINT NOT NULL,
    mode VARCHAR(30) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(100) NULL,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_reason VARCHAR(120) NULL,
    summary VARCHAR(2000) NOT NULL,
    deterministic_snapshot LONGTEXT NOT NULL,
    context_snapshot LONGTEXT NOT NULL,
    ai_result_json LONGTEXT NULL,
    requested_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ai_recommendation_request
        FOREIGN KEY (replacement_request_id) REFERENCES replacement_requests(id),
    CONSTRAINT fk_ai_recommendation_requested_by
        FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT chk_ai_recommendation_mode CHECK (
        mode IN ('AI_ASSISTED', 'DETERMINISTIC_FALLBACK')
    )
) DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_ai_recommendation_request_time
    ON ai_recommendation_runs(replacement_request_id, created_at);
CREATE INDEX idx_ai_recommendation_mode_time
    ON ai_recommendation_runs(mode, created_at);
