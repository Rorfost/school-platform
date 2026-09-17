-- Existing albums may contain zero-based or sparse ordering. Move values out of the positive
-- range first so the retained unique constraint cannot conflict while rows are normalized.
ALTER TABLE gallery_images
    DROP CONSTRAINT gallery_images_sort_order_non_negative;

UPDATE gallery_images
SET sort_order = -sort_order - 1;

WITH normalized AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY gallery_album_id
               ORDER BY sort_order DESC, created_at ASC, id ASC
           ) AS normalized_order
    FROM gallery_images
)
UPDATE gallery_images AS image
SET sort_order = normalized.normalized_order
FROM normalized
WHERE image.id = normalized.id;

ALTER TABLE gallery_images
    ADD CONSTRAINT gallery_images_sort_order_positive CHECK (sort_order > 0);
