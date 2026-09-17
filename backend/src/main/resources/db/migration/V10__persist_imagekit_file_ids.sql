ALTER TABLE study_materials ADD COLUMN imagekit_file_id VARCHAR(120);
ALTER TABLE notices ADD COLUMN attachment_imagekit_file_id VARCHAR(120);
ALTER TABLE gallery_images ADD COLUMN imagekit_file_id VARCHAR(120);
ALTER TABLE downloads ADD COLUMN imagekit_file_id VARCHAR(120);

COMMENT ON COLUMN study_materials.imagekit_file_id IS 'ImageKit file identifier for direct deletion; NULL denotes legacy path lookup.';
COMMENT ON COLUMN notices.attachment_imagekit_file_id IS 'ImageKit attachment identifier for direct deletion; NULL denotes legacy path lookup.';
COMMENT ON COLUMN gallery_images.imagekit_file_id IS 'ImageKit file identifier for direct deletion; NULL denotes legacy path lookup.';
COMMENT ON COLUMN downloads.imagekit_file_id IS 'ImageKit file identifier for direct deletion; NULL denotes legacy path lookup.';
