ALTER TABLE appointments RENAME TO appointment;

--SINCE IT VERY BEGINNING I DECIDED TO JUST DELETE OLD ROWS INSTEAD OF CREATING ARCHIVE TABLE OR ADDING TEMPLATES
--must never be run with real data in the database
DELETE FROM appointment;

ALTER TABLE appointment RENAME COLUMN date_time TO starts_at;
ALTER TABLE appointment
 ALTER COLUMN starts_at TYPE timestamptz
 USING starts_at AT TIME ZONE 'Europe/Warsaw';

ALTER TABLE appointment ALTER COLUMN id SET DATA TYPE BIGINT;
ALTER TABLE appointment ALTER COLUMN id DROP DEFAULT;
ALTER TABLE appointment ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY;

ALTER TABLE appointment ADD COLUMN ends_at timestamptz NOT NULL CONSTRAINT starts_before_ends CHECK(ends_at > starts_at);

ALTER TABLE appointment DROP COLUMN barber_name;
ALTER TABLE appointment RENAME COLUMN phone_number TO client_phone_number;
CREATE INDEX idx_client_phone_number ON appointment (client_phone_number, starts_at) WHERE status = 'BOOKED';
ALTER TABLE appointment ADD COLUMN barber_id BIGINT NOT NULL REFERENCES barber(id);

ALTER TABLE appointment ADD COLUMN created_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE appointment ADD COLUMN service_id BIGINT REFERENCES service(id);

ALTER TABLE appointment ADD CONSTRAINT valid_status CHECK(status IN ('BOOKED', 'CANCELLED'));

CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE appointment ADD CONSTRAINT no_overlapping_appointments
     EXCLUDE USING gist (
        barber_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
     ) WHERE (status <> 'CANCELLED');

CREATE INDEX idx_appointment_barber_start ON appointment (barber_id, starts_at);
