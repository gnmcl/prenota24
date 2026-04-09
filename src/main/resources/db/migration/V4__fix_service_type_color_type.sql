-- V4__fix_service_type_color_type.sql
-- Converti color da CHAR(7)/bpchar a VARCHAR(7)
-- per compatibilità con la mappatura Hibernate String → varchar
ALTER TABLE service_type ALTER COLUMN color TYPE VARCHAR(7);
