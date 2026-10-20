ALTER TABLE grades ADD COLUMN recovery_score numeric(5,2);
ALTER TABLE grades ADD CONSTRAINT grades_recovery_score_range
    CHECK (recovery_score IS NULL OR (recovery_score >= 0 AND recovery_score <= 10));

ALTER TABLE grade_history ADD COLUMN grade_kind character varying(20) NOT NULL DEFAULT 'ORIGINAL';
ALTER TABLE grade_history ADD CONSTRAINT grade_history_kind_check
    CHECK (grade_kind IN ('ORIGINAL', 'RECOVERY'));
