package com.project.artconnect.ui;

import com.project.artconnect.model.Gallery;
import com.project.artconnect.service.GalleryService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class GalleryController {

    @FXML
    private TableView<Gallery> galleryTable;

    @FXML
    private TableColumn<Gallery, String> nameColumn;

    @FXML
    private TableColumn<Gallery, String> addressColumn;

    @FXML
    private TableColumn<Gallery, Double> ratingColumn;

    @FXML
    private TextField nameField;

    @FXML
    private TextField addressField;

    @FXML
    private TextField ratingField;

    @FXML
    private TextField searchField;

    private final GalleryService galleryService = ServiceProvider.getGalleryService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("address"));
        ratingColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("rating"));

        galleryTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldGallery, selectedGallery) -> fillForm(selectedGallery)
        );

        refreshList();
    }

    @FXML
    public void handleAddGallery() {
        String name = nameField.getText();
        String address = addressField.getText();
        Double rating = Double.parseDouble(ratingField.getText());

        Gallery newGallery = new Gallery(name, address, rating);
        galleryService.save(newGallery);
        refreshList();

        clearFields();  // Clear input fields
    }

    @FXML
    public void handleDeleteGallery() {
        Gallery selectedGallery = galleryTable.getSelectionModel().getSelectedItem();
        if (selectedGallery != null) {
            galleryService.delete(selectedGallery.getName());
            refreshList();
            clearFields();
        }
    }

    @FXML
    public void handleUpdateGallery() {
        Gallery selectedGallery = galleryTable.getSelectionModel().getSelectedItem();
        if (selectedGallery != null) {
            selectedGallery.setAddress(addressField.getText());
            selectedGallery.setRating(Double.parseDouble(ratingField.getText()));
            galleryService.update(selectedGallery);

            refreshList();
            clearFields();
        }
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText();
        galleryTable.setItems(FXCollections.observableArrayList(galleryService.searchByName(query)));
    }

    @FXML
    public void handleResetSearch() {
        searchField.clear();
        refreshList();
    }

    private void refreshList() {
        galleryTable.setItems(FXCollections.observableArrayList(galleryService.getAllGalleries()));
    }

    private void fillForm(Gallery gallery) {
        if (gallery == null) {
            return;
        }

        nameField.setText(gallery.getName());
        addressField.setText(gallery.getAddress());
        ratingField.setText(String.valueOf(gallery.getRating()));
    }

    private void clearFields() {
        nameField.clear();
        addressField.clear();
        ratingField.clear();
        galleryTable.getSelectionModel().clearSelection();
    }
}
