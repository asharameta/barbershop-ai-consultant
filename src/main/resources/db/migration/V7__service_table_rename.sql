ALTER TABLE service RENAME TO grooming;

ALTER TABLE appointment RENAME COLUMN service_id TO grooming_id;