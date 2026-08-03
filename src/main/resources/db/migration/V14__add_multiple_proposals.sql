-- V14: Add support for up to 3 proposed time slots per appointment
-- Slot 1 already exists as proposed_start / proposed_end
-- Slots 2 and 3 are new optional alternatives

ALTER TABLE appointment
    ADD COLUMN proposed_start_2 TIMESTAMPTZ,
    ADD COLUMN proposed_end_2   TIMESTAMPTZ,
    ADD COLUMN proposed_start_3 TIMESTAMPTZ,
    ADD COLUMN proposed_end_3   TIMESTAMPTZ;
