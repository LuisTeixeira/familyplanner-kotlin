CREATE TABLE events (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    created_by UUID NOT NULL,
    title TEXT NOT NULL,
    event_date DATE NOT NULL,
    event_time TIME,
    timezone TEXT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    description TEXT,
    address TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX events_household_starts_at_idx ON events (household_id, starts_at);