-- 1) colunas nullable (a app, com ddl-auto=update, não consegue adicionar NOT NULL em tabela com dados)
ALTER TABLE academic_periods ADD COLUMN IF NOT EXISTS period_type VARCHAR(20);
ALTER TABLE class_rooms      ADD COLUMN IF NOT EXISTS period_type VARCHAR(20);

-- 2) popular: tipo do ano; fallback = school_settings
UPDATE academic_periods p SET period_type = COALESCE(
   (SELECT y.period_type FROM academic_years y WHERE y.id = p.academic_year_id),
   (SELECT s.period_type FROM school_settings s ORDER BY s.id LIMIT 1), 'TRIMESTER')
WHERE p.period_type IS NULL;

UPDATE class_rooms c SET period_type = COALESCE(
   (SELECT y.period_type FROM academic_years y WHERE y.id = c.academic_year_id),
   (SELECT s.period_type FROM school_settings s ORDER BY s.id LIMIT 1), 'TRIMESTER')
WHERE c.period_type IS NULL;

-- 3) travar
ALTER TABLE academic_periods ALTER COLUMN period_type SET NOT NULL;
ALTER TABLE class_rooms      ALTER COLUMN period_type SET NOT NULL;

-- 4) (opcional, só depois de validar) ALTER TABLE school_settings DROP COLUMN period_type;
