package com.project.artconnect.service.impl;

import com.project.artconnect.dao.GalleryDao;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.GalleryService;

import java.util.List;
import java.util.Optional;

public class JdbcGalleryService implements GalleryService {

    private final GalleryDao galleryDao;

    public JdbcGalleryService(GalleryDao galleryDao) {
        this.galleryDao = galleryDao;
    }

    @Override
    public List<Gallery> getAllGalleries() {
        return galleryDao.findAll();
    }

    @Override
    public Optional<Gallery> getGalleryByName(String name) {
        return galleryDao.findByName(name);
    }

    @Override
    public List<Gallery> searchByName(String query) {
        return galleryDao.searchByName(query);
    }

    @Override
    public void save(Gallery gallery) {
        galleryDao.save(gallery);
    }

    @Override
    public void update(Gallery gallery) {
        galleryDao.update(gallery);
    }

    @Override
    public void delete(String galleryName) {
        galleryDao.delete(galleryName);
    }

    @Override
    public List<Exhibition> getExhibitionsByGallery(Gallery gallery) {
        if (gallery == null) {
            return List.of();
        }
        return gallery.getExhibitions(); // To be adapted if we get exhibitions via a specific DAO or logic
    }

    // Additional methods for exhibitions can be added if needed
    // For example, add an exhibition
    public void addExhibitionToGallery(Gallery gallery, Exhibition exhibition, Artwork... artworks) {
        if (gallery != null && exhibition != null) {
            exhibition.setGallery(gallery);
            gallery.addExhibition(exhibition); // Assumes Gallery has a method to add exhibitions

            // Logic for adding artworks to the exhibition
            for (Artwork artwork : artworks) {
                if (artwork != null) {
                    exhibition.getArtworks().add(artwork);
                }
            }

            // Save the updated gallery with the exhibition
            galleryDao.update(gallery);
        }
    }
}
