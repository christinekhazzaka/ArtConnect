package com.project.artconnect.service;

import com.project.artconnect.model.Exhibition;

import java.util.List;
import java.util.Optional;

public interface ExhibitionService {
    List<Exhibition> getAllExhibitions();

    Optional<Exhibition> getExhibitionByTitle(String title);

    void save(Exhibition exhibition);

    void update(Exhibition exhibition);

    void delete(String title);
}
