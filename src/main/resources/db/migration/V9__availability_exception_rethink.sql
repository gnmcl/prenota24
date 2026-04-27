CREATE TABLE availability_exception_slot (
    exception_id UUID NOT NULL,
    CONSTRAINT fk_avail_exc_slot FOREIGN KEY (exception_id)
        REFERENCES availability_exception(id) ON DELETE CASCADE,
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    CONSTRAINT chk_slot_times CHECK (start_time < end_time));

CREATE INDEX idx_avail_exc_slot_exception ON availability_exception_slot(exception_id);
INSERT INTO availability_exception_slot (exception_id, start_time, end_time)
SELECT id, start_time, end_time FROM availability_exception
WHERE start_time IS NOT NULL AND end_time IS NOT NULL;

ALTER TABLE availability_exception ADD COLUMN is_unavailable_all_day BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE availability_exception SET is_unavailable_all_day = is_unavailable;
ALTER TABLE availability_exception DROP CONSTRAINT chk_avail_exc_times;
ALTER TABLE availability_exception DROP COLUMN is_unavailable;
ALTER TABLE availability_exception DROP COLUMN start_time;
ALTER TABLE availability_exception DROP COLUMN end_time;

