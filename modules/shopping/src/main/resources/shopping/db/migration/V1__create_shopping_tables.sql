CREATE TABLE shopping_lists (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE shopping_items (
    id UUID PRIMARY KEY,
    list_id UUID NOT NULL REFERENCES shopping_lists (id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    is_completed BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX shopping_lists_household_idx ON shopping_lists (household_id);
CREATE INDEX shopping_items_list_idx ON shopping_items (list_id);