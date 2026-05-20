package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Artwork;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.ArtworkService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ArtworkController {

    @FXML
    private TableView<Artwork> artworkTable;

    @FXML
    private TableColumn<Artwork, String> titleColumn;

    @FXML
    private TableColumn<Artwork, String> typeColumn;

    @FXML
    private TableColumn<Artwork, Integer> creationYearColumn;

    @FXML
    private TableColumn<Artwork, String> descriptionColumn;

    @FXML
    private TableColumn<Artwork, Double> priceColumn;

    @FXML
    private TableColumn<Artwork, String> statusColumn;

    @FXML
    private TableColumn<Artwork, String> artistColumn;

    @FXML
    private TextField titleField;

    @FXML
    private TextField typeField;

    @FXML
    private TextField creationYearField;

    @FXML
    private TextField priceField;

    @FXML
    private TextField descriptionField;

    @FXML
    private ComboBox<Artist> artistField;

    @FXML
    private TextField searchField;

    private final ArtworkService artworkService = ServiceProvider.getArtworkService();
    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        creationYearColumn.setCellValueFactory(new PropertyValueFactory<>("creationYear"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        artistColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getArtist() != null
                        ? cellData.getValue().getArtist().getName()
                        : "Unknown"
        ));

        artistField.setItems(FXCollections.observableArrayList(artistService.getAllArtists()));

        artworkTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldArtwork, selectedArtwork) -> fillForm(selectedArtwork)
        );

        refreshTable();
    }

    @FXML
    private void handleAddArtwork() {
        try {
            Artwork artwork = buildArtworkFromForm();
            artworkService.createArtwork(artwork);
            refreshTable();
            clearForm();
            showInfo("Artwork added successfully.");
        } catch (Exception e) {
            showError("Could not add artwork", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateArtwork() {
        Artwork selectedArtwork = artworkTable.getSelectionModel().getSelectedItem();

        if (selectedArtwork == null) {
            showError("No artwork selected", "Please select an artwork to update.");
            return;
        }

        try {
            Artwork artwork = buildArtworkFromForm();
            artworkService.updateArtwork(artwork);
            refreshTable();
            clearForm();
            showInfo("Artwork updated successfully.");
        } catch (Exception e) {
            showError("Could not update artwork", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteArtwork() {
        Artwork selectedArtwork = artworkTable.getSelectionModel().getSelectedItem();

        if (selectedArtwork == null) {
            showError("No artwork selected", "Please select an artwork to delete.");
            return;
        }

        try {
            artworkService.deleteArtwork(selectedArtwork.getTitle());
            refreshTable();
            clearForm();
            showInfo("Artwork deleted successfully.");
        } catch (Exception e) {
            showError("Could not delete artwork", e.getMessage());
        }
    }

    private void refreshTable() {
        artworkTable.setItems(FXCollections.observableArrayList(artworkService.getAllArtworks()));
    }

    @FXML
    private void handleSearch() {
        String query = normalized(searchField.getText());
        artworkTable.setItems(FXCollections.observableArrayList(
                artworkService.getAllArtworks().stream()
                        .filter(artwork -> query.isBlank()
                                || contains(artwork.getTitle(), query)
                                || contains(artwork.getType(), query)
                                || contains(artwork.getDescription(), query)
                                || contains(artwork.getStatus() != null ? artwork.getStatus().name() : null, query)
                                || contains(artwork.getCreationYear() != null ? artwork.getCreationYear().toString() : null, query)
                                || contains(artwork.getArtist() != null ? artwork.getArtist().getName() : null, query))
                        .toList()
        ));
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        refreshTable();
    }

    private Artwork buildArtworkFromForm() {
        String title = titleField.getText();

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Artwork title is required.");
        }

        Artist artist = artistField.getValue();

        if (artist == null) {
            throw new IllegalArgumentException("Artist is required.");
        }

        Artwork artwork = new Artwork();
        artwork.setTitle(title.trim());
        artwork.setType(typeField.getText());
        artwork.setDescription(descriptionField.getText());
        artwork.setArtist(artist);
        artwork.setStatus(Artwork.Status.FOR_SALE);

        if (creationYearField.getText() != null && !creationYearField.getText().isBlank()) {
            artwork.setCreationYear(Integer.parseInt(creationYearField.getText().trim()));
        }

        if (priceField.getText() != null && !priceField.getText().isBlank()) {
            artwork.setPrice(Double.parseDouble(priceField.getText().trim()));
        }

        return artwork;
    }

    private void fillForm(Artwork artwork) {
        if (artwork == null) {
            return;
        }

        titleField.setText(artwork.getTitle());
        typeField.setText(artwork.getType());
        descriptionField.setText(artwork.getDescription());
        creationYearField.setText(artwork.getCreationYear() != null ? artwork.getCreationYear().toString() : "");
        priceField.setText(String.valueOf(artwork.getPrice()));
        artistField.setValue(artwork.getArtist());
    }

    private void clearForm() {
        titleField.clear();
        typeField.clear();
        descriptionField.clear();
        creationYearField.clear();
        priceField.clear();
        artistField.setValue(null);
        artworkTable.getSelectionModel().clearSelection();
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

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
