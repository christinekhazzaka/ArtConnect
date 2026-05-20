package com.project.artconnect.ui;

import com.project.artconnect.model.Artist;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ArtistService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkshopController {
    @FXML
    private TableView<Workshop> workshopTable;
    @FXML
    private TableColumn<Workshop, String> titleColumn;
    @FXML
    private TableColumn<Workshop, LocalDateTime> dateColumn;
    @FXML
    private TableColumn<Workshop, String> instructorColumn;
    @FXML
    private TableColumn<Workshop, Double> priceColumn;
    @FXML
    private TableColumn<Workshop, String> levelColumn;
    @FXML
    private TableColumn<Workshop, String> locationColumn;
    @FXML
    private TextField titleField;
    @FXML
    private ComboBox<Artist> instructorField;
    @FXML
    private TextField dateField;
    @FXML
    private TextField priceField;
    @FXML
    private TextField levelField;
    @FXML
    private TextField locationField;
    @FXML
    private TextField searchField;

    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();
    private final ArtistService artistService = ServiceProvider.getArtistService();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        levelColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));

        instructorColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getInstructor() != null ? cellData.getValue().getInstructor().getName()
                        : "Unknown"));

        instructorField.setItems(FXCollections.observableArrayList(getAvailableInstructors()));

        workshopTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldWorkshop, selectedWorkshop) -> fillForm(selectedWorkshop)
        );

        refreshTable();
    }

    @FXML
    private void handleAddWorkshop() {
        try {
            Workshop workshop = buildWorkshopFromForm();
            workshopService.save(workshop);
            refreshTable();
            clearForm();
            showInfo("Workshop added successfully.");
        } catch (Exception e) {
            showError("Could not add workshop", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateWorkshop() {
        Workshop selectedWorkshop = workshopTable.getSelectionModel().getSelectedItem();

        if (selectedWorkshop == null) {
            showError("No workshop selected", "Please select a workshop to update.");
            return;
        }

        try {
            Workshop workshop = buildWorkshopFromForm();
            if (!selectedWorkshop.getTitle().equals(workshop.getTitle())) {
                workshopService.delete(selectedWorkshop.getTitle());
            }
            workshopService.update(workshop);
            refreshTable();
            clearForm();
            showInfo("Workshop updated successfully.");
        } catch (Exception e) {
            showError("Could not update workshop", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteWorkshop() {
        Workshop selectedWorkshop = workshopTable.getSelectionModel().getSelectedItem();

        if (selectedWorkshop == null) {
            showError("No workshop selected", "Please select a workshop to delete.");
            return;
        }

        try {
            workshopService.delete(selectedWorkshop.getTitle());
            refreshTable();
            clearForm();
            showInfo("Workshop deleted successfully.");
        } catch (Exception e) {
            showError("Could not delete workshop", e.getMessage());
        }
    }

    private void refreshTable() {
        instructorField.setItems(FXCollections.observableArrayList(getAvailableInstructors()));
        workshopTable.setItems(FXCollections.observableArrayList(workshopService.getAllWorkshops()));
    }

    @FXML
    private void handleSearch() {
        String query = normalized(searchField.getText());
        workshopTable.setItems(FXCollections.observableArrayList(
                workshopService.getAllWorkshops().stream()
                        .filter(workshop -> query.isBlank()
                                || contains(workshop.getTitle(), query)
                                || contains(workshop.getInstructor() != null ? workshop.getInstructor().getName() : null, query)
                                || contains(workshop.getDate() != null ? workshop.getDate().toString() : null, query)
                                || contains(String.valueOf(workshop.getPrice()), query)
                                || contains(workshop.getLevel(), query)
                                || contains(workshop.getLocation(), query))
                        .toList()
        ));
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        refreshTable();
    }

    private Workshop buildWorkshopFromForm() {
        String title = titleField.getText();

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Workshop title is required.");
        }

        Artist instructor = instructorField.getValue();

        if (instructor == null) {
            throw new IllegalArgumentException("Instructor is required.");
        }

        Workshop workshop = new Workshop(title.trim(), parseDate(), instructor, parsePrice());
        workshop.setLevel(normalizeLevel(levelField.getText()));
        workshop.setLocation(locationField.getText());
        workshop.setDurationMinutes(180);
        workshop.setMaxParticipants(10);
        return workshop;
    }

    private LocalDateTime parseDate() {
        if (dateField.getText() == null || dateField.getText().isBlank()) {
            return null;
        }

        return LocalDateTime.parse(dateField.getText().trim());
    }

    private double parsePrice() {
        if (priceField.getText() == null || priceField.getText().isBlank()) {
            return 0.0;
        }

        return Double.parseDouble(priceField.getText().trim());
    }

    private void fillForm(Workshop workshop) {
        if (workshop == null) {
            return;
        }

        titleField.setText(workshop.getTitle());
        instructorField.setValue(workshop.getInstructor());
        dateField.setText(workshop.getDate() != null ? workshop.getDate().toString() : "");
        priceField.setText(String.valueOf(workshop.getPrice()));
        levelField.setText(workshop.getLevel());
        locationField.setText(workshop.getLocation());
    }

    private void clearForm() {
        titleField.clear();
        instructorField.setValue(null);
        dateField.clear();
        priceField.clear();
        levelField.clear();
        locationField.clear();
        workshopTable.getSelectionModel().clearSelection();
    }

    private java.util.List<Artist> getAvailableInstructors() {
        Map<String, Artist> instructors = new LinkedHashMap<>();

        for (Artist artist : artistService.getAllArtists()) {
            if (artist.getName() != null) {
                instructors.put(artist.getName(), artist);
            }
        }

        for (Workshop workshop : workshopService.getAllWorkshops()) {
            Artist instructor = workshop.getInstructor();
            if (instructor != null && instructor.getName() != null) {
                instructors.putIfAbsent(instructor.getName(), instructor);
            }
        }

        return instructors.values().stream().toList();
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

    private String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "BEGINNER";
        }

        String normalized = level.trim().toUpperCase();
        return switch (normalized) {
            case "BEGINNER", "INTERMEDIATE", "ADVANCED" -> normalized;
            default -> throw new IllegalArgumentException("Workshop level must be BEGINNER, INTERMEDIATE, or ADVANCED.");
        };
    }
}
