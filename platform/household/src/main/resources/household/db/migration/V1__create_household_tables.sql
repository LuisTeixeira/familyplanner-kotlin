CREATE TABLE households (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE household_members (
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (household_id, user_id)
);

CREATE TABLE invitations (
    id UUID PRIMARY KEY,
    household_id UUID NOT NULL REFERENCES households (id) ON DELETE CASCADE,
    invited_by UUID NOT NULL,
    invited_user_id UUID NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX invitations_invited_user_idx ON invitations (invited_user_id, status);
CREATE INDEX invitations_household_user_idx ON invitations (household_id, invited_user_id, status);