package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Discipline;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ArtistController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<Discipline> disciplineFilter;

    @FXML
    private TableView<Artist> artistTable;

    @FXML
    private TableColumn<Artist, String> nameColumn;

    @FXML
    private TableColumn<Artist, String> cityColumn;

    @FXML
    private TableColumn<Artist, String> emailColumn;

    @FXML
    private TableColumn<Artist, Integer> yearColumn;

    @FXML
    private TableColumn<Artist, String> disciplineColumn;

    @FXML
    private TableColumn<Artist, String> bioColumn;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField cityField;

    @FXML
    private TextField birthYearField;

    @FXML
    private TextField bioField;

    @FXML
    private ComboBox<Discipline> disciplineField;

    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("contactEmail"));
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("birthYear"));
        disciplineColumn.setCellValueFactory(cellData -> new SimpleStringProperty(firstDiscipline(cellData.getValue())));
        bioColumn.setCellValueFactory(new PropertyValueFactory<>("bio"));

        disciplineFilter.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));
        disciplineField.setItems(FXCollections.observableArrayList(artistService.getAllDisciplines()));

        artistTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldArtist, selectedArtist) -> fillForm(selectedArtist)
        );

        refreshTable();
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText();
        Discipline discipline = disciplineFilter.getValue();
        String disciplineName = discipline != null ? discipline.getName() : null;

        artistTable.setItems(FXCollections.observableArrayList(
                artistService.searchArtists(query, disciplineName, null)
        ));
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        disciplineFilter.setValue(null);
        clearForm();
        refreshTable();
    }

    @FXML
    private void handleAddArtist() {
        try {
            Artist artist = buildArtistFromForm();
            artistService.createArtist(artist);
            refreshTable();
            clearForm();
            showInfo("Artist added successfully.");
        } catch (Exception e) {
            showError("Could not add artist", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateArtist() {
        Artist selectedArtist = artistTable.getSelectionModel().getSelectedItem();

        if (selectedArtist == null) {
            showError("No artist selected", "Please select an artist to update.");
            return;
        }

        try {
            Artist updatedArtist = buildArtistFromForm();
            artistService.updateArtist(updatedArtist);
            refreshTable();
            clearForm();
            showInfo("Artist updated successfully.");
        } catch (Exception e) {
            showError("Could not update artist", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteArtist() {
        Artist selectedArtist = artistTable.getSelectionModel().getSelectedItem();

        if (selectedArtist == null) {
            showError("No artist selected", "Please select an artist to delete.");
            return;
        }

        try {
            artistService.deleteArtist(selectedArtist.getName());
            refreshTable();
            clearForm();
            showInfo("Artist deleted successfully.");
        } catch (Exception e) {
            showError("Could not delete artist", e.getMessage());
        }
    }

    private void refreshTable() {
        artistTable.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));
    }

    private Artist buildArtistFromForm() {
        String name = nameField.getText();
        String email = emailField.getText();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Artist name is required.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Artist email is required.");
        }

        Artist artist = new Artist();
        artist.setName(name.trim());
        artist.setContactEmail(email.trim());
        artist.setCity(cityField.getText());
        artist.setBio(bioField.getText());
        artist.setActive(true);

        if (birthYearField.getText() != null && !birthYearField.getText().isBlank()) {
            artist.setBirthYear(Integer.parseInt(birthYearField.getText().trim()));
        }

        Discipline selectedDiscipline = disciplineField.getValue();
        if (selectedDiscipline != null) {
            artist.getDisciplines().add(selectedDiscipline);
        }

        return artist;
    }

    private void fillForm(Artist artist) {
        if (artist == null) {
            return;
        }

        nameField.setText(artist.getName());
        emailField.setText(artist.getContactEmail());
        cityField.setText(artist.getCity());
        bioField.setText(artist.getBio());
        birthYearField.setText(artist.getBirthYear() != null ? artist.getBirthYear().toString() : "");

        if (artist.getDisciplines() != null && !artist.getDisciplines().isEmpty()) {
            disciplineField.setValue(artist.getDisciplines().get(0));
        } else {
            disciplineField.setValue(null);
        }
    }

    private void clearForm() {
        nameField.clear();
        emailField.clear();
        cityField.clear();
        bioField.clear();
        birthYearField.clear();
        disciplineField.setValue(null);
        artistTable.getSelectionModel().clearSelection();
    }

    private String firstDiscipline(Artist artist) {
        if (artist == null || artist.getDisciplines() == null || artist.getDisciplines().isEmpty()) {
            return "";
        }

        Discipline discipline = artist.getDisciplines().get(0);
        return discipline != null ? discipline.getName() : "";
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
