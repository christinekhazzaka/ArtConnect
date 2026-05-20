package com.project.artconnect.service.impl;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.persistence.JdbcExhibitionDao;
import com.project.artconnect.service.ExhibitionService;

import java.util.List;
import java.util.Optional;

public class JdbcExhibitionService implements ExhibitionService {
    private final JdbcExhibitionDao exhibitionDao;

    public JdbcExhibitionService(JdbcExhibitionDao exhibitionDao) {
        this.exhibitionDao = exhibitionDao;
    }

    @Override
    public List<Exhibition> getAllExhibitions() {
        return exhibitionDao.findAll();
    }

    @Override
    public Optional<Exhibition> getExhibitionByTitle(String title) {
        return exhibitionDao.findByTitle(title);
    }

    @Override
    public void save(Exhibition exhibition) {
        exhibitionDao.save(exhibition);
    }

    @Override
    public void update(Exhibition exhibition) {
        exhibitionDao.update(exhibition);
    }

    @Override
    public void delete(String title) {
        exhibitionDao.delete(title);
    }
}
