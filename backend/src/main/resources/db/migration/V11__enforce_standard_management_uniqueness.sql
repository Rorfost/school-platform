CREATE UNIQUE INDEX standards_school_normalized_display_name_unique
    ON standards (school_id, lower(btrim(display_name)));

CREATE UNIQUE INDEX standards_school_sort_order_unique
    ON standards (school_id, sort_order);

COMMENT ON INDEX standards_school_normalized_display_name_unique IS
    'Prevents case-only duplicate standard names within a school.';

COMMENT ON INDEX standards_school_sort_order_unique IS
    'Keeps the automatically assigned standard display order unambiguous within a school.';
