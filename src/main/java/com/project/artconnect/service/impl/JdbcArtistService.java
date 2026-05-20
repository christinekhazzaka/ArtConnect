package com.project.artconnect.service.impl;

import com.project.artconnect.dao.ArtistDao;
import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JdbcArtistService implements ArtistService {

    private final ArtistDao artistDao;

    public JdbcArtistService(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    @Override
    public List<Artist> getAllArtists() {
        return artistDao.findAll();
    }

    @Override
    public Optional<Artist> getArtistByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        return artistDao.findAll()
                .stream()
                .filter(artist -> artist.getName() != null)
                .filter(artist -> artist.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public void createArtist(Artist artist) {
        artistDao.save(artist);
    }

    @Override
    public void updateArtist(Artist artist) {
        artistDao.update(artist);
    }

    @Override
    public void deleteArtist(String name) {
        artistDao.delete(name);
    }

    @Override
    public List<Discipline> getAllDisciplines() {
        return List.of(
                new Discipline("PAINTING"),
                new Discipline("PHOTOGRAPHY"),
                new Discipline("SCULPTURE"),
                new Discipline("MUSIC"),
                new Discipline("OTHER")
        );
    }

    @Override
    public List<Artist> searchArtists(String query, String disciplineName, String city) {
        return artistDao.findAll()
                .stream()
                .filter(artist -> matchesQuery(artist, query))
                .filter(artist -> matchesDiscipline(artist, disciplineName))
                .filter(artist -> matchesCity(artist, city))
                .toList();
    }

    private boolean matchesQuery(Artist artist, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String lowerQuery = query.toLowerCase();

        return containsIgnoreCase(artist.getName(), lowerQuery)
                || containsIgnoreCase(artist.getBio(), lowerQuery)
                || containsIgnoreCase(artist.getContactEmail(), lowerQuery);
    }

    private boolean matchesDiscipline(Artist artist, String disciplineName) {
        if (disciplineName == null || disciplineName.isBlank() || disciplineName.equalsIgnoreCase("All")) {
            return true;
        }

        if (artist.getDisciplines() == null || artist.getDisciplines().isEmpty()) {
            return false;
        }

        return artist.getDisciplines()
                .stream()
                .anyMatch(discipline ->
                        discipline.getName() != null
                                && discipline.getName().equalsIgnoreCase(disciplineName)
                );
    }
    private boolean matchesCity(Artist artist, String city) {
        if (city == null || city.isBlank() || city.equalsIgnoreCase("All")) {
            return true;
        }

        return artist.getCity() != null
                && artist.getCity().equalsIgnoreCase(city);
    }

    private boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }
}