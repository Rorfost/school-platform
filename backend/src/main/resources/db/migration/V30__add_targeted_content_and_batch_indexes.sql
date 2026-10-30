-- V30__add_targeted_content_and_batch_indexes.sql
-- Sprint 34: Targeted index tuning for multi-tenant content queries.
--
-- Optimization Targets:
-- 1. gallery_images (school_id, gallery_album_id, status)
--    Supports the Sprint 33 batch summary query (findSummariesBySchoolIdAndAlbumIds)
--    which filters by (school_id = :schoolId AND gallery_album_id IN (:albumIds) AND status = 'PUBLISHED').
--    V4 provided gallery_images_published_order_idx ON (gallery_album_id, sort_order) WHERE status = 'PUBLISHED',
--    which lacks school_id as the tenant-leading column. This composite index allows index-only or fast index scans
--    without touching the heap for cover key and published count aggregation.
--
-- 2. gallery_albums (school_id, updated_at DESC)
--    Supports administrative album listing sorted by updated_at descending (findBySchoolId(schoolId, Pageable)).
--    V4 provided gallery_albums_published_list_idx ON (school_id, published_at DESC) WHERE status = 'PUBLISHED',
--    which cannot be used by the admin endpoint (which queries all album statuses ordered by updated_at).

CREATE INDEX IF NOT EXISTS gallery_images_school_album_status_idx
    ON gallery_images (school_id, gallery_album_id, status);
COMMENT ON INDEX gallery_images_school_album_status_idx IS
    'Supports tenant-scoped batch image counting and cover resolution across multiple albums without scanning foreign tenant rows.';

CREATE INDEX IF NOT EXISTS gallery_albums_school_updated_at_idx
    ON gallery_albums (school_id, updated_at DESC);
COMMENT ON INDEX gallery_albums_school_updated_at_idx IS
    'Supports tenant-scoped administrative album listing ordered by last update without table scans.';
