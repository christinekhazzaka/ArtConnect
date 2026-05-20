package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.ExhibitionService;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;

public class ExhibitionController {
    @FXML
    private TableView<Exhibition> exhibitionTable;
    @FXML
    private TableColumn<Exhibition, String> titleColumn;
    @FXML
    private TableColumn<Exhibition, LocalDate> dateColumn;
    @FXML
    private TableColumn<Exhibition, String> descriptionColumn;
    @FXML
    private TableColumn<Exhibition, String> locationColumn;
    @FXML
    private TableColumn<Exhibition, Integer> capacityColumn;
    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<Gallery> galleryField;
    @FXML
    private TextField startDateField;
    @FXML
    private TextField capacityField;
    @FXML
    private TextField descriptionField;
    @FXML
    private TextField searchField;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();
    private final ExhibitionService exhibitionService = ServiceProvider.getExhibitionService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));

        locationColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getGallery() != null ? cellData.getValue().getGallery().getName() : "Unknown"));

        galleryField.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries()));

        exhibitionTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldExhibition, selectedExhibition) -> fillForm(selectedExhibition)
        );

        refreshData();
    }

    @FXML
    private void handleAddExhibition() {
        try {
            Exhibition exhibition = buildExhibitionFromForm();
            exhibitionService.save(exhibition);
            refreshData();
            clearForm();
            showInfo("Exhibition added successfully.");
        } catch (Exception e) {
            showError("Could not add exhibition", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateExhibition() {
        Exhibition selectedExhibition = exhibitionTable.getSelectionModel().getSelectedItem();

        if (selectedExhibition == null) {
            showError("No exhibition selected", "Please select an exhibition to update.");
            return;
        }

        try {
            Exhibition exhibition = buildExhibitionFromForm();
            if (!selectedExhibition.getTitle().equals(exhibition.getTitle())) {
                exhibitionService.delete(selectedExhibition.getTitle());
            }
            exhibitionService.update(exhibition);
            refreshData();
            clearForm();
            showInfo("Exhibition updated successfully.");
        } catch (Exception e) {
            showError("Could not update exhibition", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteExhibition() {
        Exhibition selectedExhibition = exhibitionTable.getSelectionModel().getSelectedItem();

        if (selectedExhibition == null) {
            showError("No exhibition selected", "Please select an exhibition to delete.");
            return;
        }

        try {
            exhibitionService.delete(selectedExhibition.getTitle());
            refreshData();
            clearForm();
            showInfo("Exhibition deleted successfully.");
        } catch (Exception e) {
            showError("Could not delete exhibition", e.getMessage());
        }
    }

    private void refreshData() {
        galleryField.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries()));

        exhibitionTable.setItems(FXCollections.observableArrayList(exhibitionService.getAllExhibitions()));
    }

    @FXML
    private void handleSearch() {
        String query = normalized(searchField.getText());
        exhibitionTable.setItems(FXCollections.observableArrayList(
                exhibitionService.getAllExhibitions().stream()
                        .filter(exhibition -> query.isBlank()
                                || contains(exhibition.getTitle(), query)
                                || contains(exhibition.getDescription(), query)
                                || contains(exhibition.getStartDate() != null ? exhibition.getStartDate().toString() : null, query)
                                || contains(String.valueOf(exhibition.getCapacity()), query)
                                || contains(exhibition.getGallery() != null ? exhibition.getGallery().getName() : null, query))
                        .toList()
        ));
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        refreshData();
    }

    private Exhibition buildExhibitionFromForm() {
        String title = titleField.getText();

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Exhibition title is required.");
        }

        Gallery gallery = galleryField.getValue();

        if (gallery == null) {
            throw new IllegalArgumentException("Gallery is required.");
        }

        Exhibition exhibition = new Exhibition(title.trim(), parseDate(startDateField), null, gallery);
        exhibition.setDescription(descriptionField.getText());
        exhibition.setCapacity(parseCapacity());
        return exhibition;
    }

    private LocalDate parseDate(TextField field) {
        if (field.getText() == null || field.getText().isBlank()) {
            return null;
        }

        return LocalDate.parse(field.getText().trim());
    }

    private void fillForm(Exhibition exhibition) {
        if (exhibition == null) {
            return;
        }

        titleField.setText(exhibition.getTitle());
        galleryField.setValue(exhibition.getGallery());
        startDateField.setText(exhibition.getStartDate() != null ? exhibition.getStartDate().toString() : "");
        capacityField.setText(String.valueOf(exhibition.getCapacity()));
        descriptionField.setText(exhibition.getDescription());
    }

    private void clearForm() {
        titleField.clear();
        galleryField.setValue(null);
        startDateField.clear();
        capacityField.clear();
        descriptionField.clear();
        exhibitionTable.getSelectionModel().clearSelection();
    }

    private int parseCapacity() {
        if (capacityField.getText() == null || capacityField.getText().isBlank()) {
            return 50;
        }

        return Integer.parseInt(capacityField.getText().trim());
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
