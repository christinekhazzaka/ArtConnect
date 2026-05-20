package com.project.artconnect.dao;

import com.project.artconnect.model.Gallery;
import java.util.List;
import java.util.Optional;

public interface GalleryDao {
    List<Gallery> findAll();
    Optional<Gallery> findByName(String name);
    List<Gallery> searchByName(String query);
    void save(Gallery gallery);
    void update(Gallery gallery);
    void delete(String galleryName);
}
