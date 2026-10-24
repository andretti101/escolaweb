-- 1. Histórico de Frequência e Notas: cascatear exclusão do registro pai e manter histórico com changed_by = NULL se o autor for removido
ALTER TABLE attendance_history
    DROP CONSTRAINT IF EXISTS fki5389e1a64knk452xs0vslyjy,
    ADD CONSTRAINT fki5389e1a64knk452xs0vslyjy FOREIGN KEY (attendance_id) REFERENCES attendances(id) ON DELETE CASCADE,
    DROP CONSTRAINT IF EXISTS fkl17wrd7jjj5jy9q7vvp2klnpc,
    ADD CONSTRAINT fkl17wrd7jjj5jy9q7vvp2klnpc FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE grade_history
    DROP CONSTRAINT IF EXISTS fk45ak73yeaf9nls14n95lxt152,
    ADD CONSTRAINT fk45ak73yeaf9nls14n95lxt152 FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE CASCADE,
    DROP CONSTRAINT IF EXISTS fk2cdt7rpftl2u8p9bypq7lrw85,
    ADD CONSTRAINT fk2cdt7rpftl2u8p9bypq7lrw85 FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL;

-- 2. Tokens e recibos de leitura vinculados ao usuário: cascatear na remoção do usuário
ALTER TABLE refresh_tokens
    DROP CONSTRAINT IF EXISTS fk1lih5y2npsf8u5o3vhdb9y0os,
    ADD CONSTRAINT fk1lih5y2npsf8u5o3vhdb9y0os FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE password_reset_tokens
    DROP CONSTRAINT IF EXISTS fkk3ndxg5xp6v7wd4gjyusp15gq,
    ADD CONSTRAINT fkk3ndxg5xp6v7wd4gjyusp15gq FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE chat_read_receipts
    DROP CONSTRAINT IF EXISTS fkcskmyxxex3midim34mwqap5ng,
    ADD CONSTRAINT fkcskmyxxex3midim34mwqap5ng FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE chat_hidden_messages
    DROP CONSTRAINT IF EXISTS fk2x1e46e7v8y401aukddni2hec,
    ADD CONSTRAINT fk2x1e46e7v8y401aukddni2hec FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
