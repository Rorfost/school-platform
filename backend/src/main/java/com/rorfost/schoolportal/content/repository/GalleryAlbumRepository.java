package com.rorfost.schoolportal.content.repository;

import com.rorfost.schoolportal.content.domain.GalleryAlbum;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GalleryAlbumRepository extends JpaRepository<GalleryAlbum, UUID> {}
