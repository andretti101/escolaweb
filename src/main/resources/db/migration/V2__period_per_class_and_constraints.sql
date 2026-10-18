-- =====================================================================
-- V2__period_per_class_and_constraints.sql
-- Integrates period per classroom and active audit columns
-- =====================================================================

-- 1. Academic periods and Class rooms period_type columns
ALTER TABLE academic_periods ADD COLUMN IF NOT EXISTS period_type VARCHAR(20);
ALTER TABLE class_rooms      ADD COLUMN IF NOT EXISTS period_type VARCHAR(20);

-- Populate with existing year period_type or fallback to school_settings
UPDATE academic_periods p SET period_type = COALESCE(
   (SELECT y.period_type FROM academic_years y WHERE y.id = p.academic_year_id),
   (SELECT s.period_type FROM school_settings s ORDER BY s.id LIMIT 1), 'TRIMESTER')
WHERE p.period_type IS NULL;

UPDATE class_rooms c SET period_type = COALESCE(
   (SELECT y.period_type FROM academic_years y WHERE y.id = c.academic_year_id),
   (SELECT s.period_type FROM school_settings s ORDER BY s.id LIMIT 1), 'TRIMESTER')
WHERE c.period_type IS NULL;

ALTER TABLE academic_periods ALTER COLUMN period_type SET NOT NULL;
ALTER TABLE class_rooms      ALTER COLUMN period_type SET NOT NULL;

-- 2. Audit active columns for grades and attendances
ALTER TABLE grades ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT true;
UPDATE grades SET active = true WHERE active IS NULL;
ALTER TABLE grades ALTER COLUMN active SET NOT NULL;

ALTER TABLE attendances ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT true;
UPDATE attendances SET active = true WHERE active IS NULL;
ALTER TABLE attendances ALTER COLUMN active SET NOT NULL;
