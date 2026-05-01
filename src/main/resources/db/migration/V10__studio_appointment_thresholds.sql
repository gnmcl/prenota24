-- Studio-level configuration for daily appointment capacity.
-- All three columns are optional: if null, the feature is disabled for that studio.

ALTER TABLE studio ADD COLUMN max_appointments_per_day integer;
ALTER TABLE studio ADD COLUMN warning_threshold        integer;
ALTER TABLE studio ADD COLUMN critical_threshold       integer;

COMMENT ON COLUMN studio.max_appointments_per_day IS 'Numero massimo di appuntamenti al giorno (soglia assoluta)';
COMMENT ON COLUMN studio.warning_threshold         IS 'Numero di appuntamenti da cui scatta l''avviso giallo';
COMMENT ON COLUMN studio.critical_threshold        IS 'Numero di appuntamenti da cui scatta l''avviso rosso';
