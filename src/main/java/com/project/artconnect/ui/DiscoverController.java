package com.project.artconnect.ui;

import com.project.artconnect.model.Exhibition;
import com.project.artconnect.model.Workshop;
import com.project.artconnect.service.ExhibitionService;
import com.project.artconnect.service.WorkshopService;
import com.project.artconnect.util.ServiceProvider;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import java.util.List;

public class DiscoverController {
    @FXML
    private FlowPane discoverPane;
    @FXML
    private TextField searchField;

    private final ExhibitionService exhibitionService = ServiceProvider.getExhibitionService();
    private final WorkshopService workshopService = ServiceProvider.getWorkshopService();

    @FXML
    public void initialize() {
        renderCards(exhibitionService.getAllExhibitions(), workshopService.getAllWorkshops());
    }

    @FXML
    private void handleSearch() {
        String query = normalized(searchField.getText());

        List<Exhibition> exhibitions = exhibitionService.getAllExhibitions().stream()
                .filter(exhibition -> query.isBlank()
                        || contains(exhibition.getTitle(), query)
                        || contains(exhibition.getDescription(), query)
                        || contains(exhibition.getGallery() != null ? exhibition.getGallery().getName() : null, query))
                .toList();

        List<Workshop> workshops = workshopService.getAllWorkshops().stream()
                .filter(workshop -> query.isBlank()
                        || contains(workshop.getTitle(), query)
                        || contains(workshop.getInstructor() != null ? workshop.getInstructor().getName() : null, query)
                        || contains(workshop.getLocation(), query)
                        || contains(workshop.getLevel(), query))
                .toList();

        renderCards(exhibitions, workshops);
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        renderCards(exhibitionService.getAllExhibitions(), workshopService.getAllWorkshops());
    }

    private void renderCards(List<Exhibition> exhibitions, List<Workshop> workshops) {
        discoverPane.getChildren().clear();
        exhibitions.stream().limit(3).forEach(this::addExhibitionCard);
        workshops.stream().limit(3).forEach(this::addWorkshopCard);
    }

    private void addExhibitionCard(Exhibition e) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: #e3f2fd; -fx-border-color: #2196f3; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setPrefWidth(250);
        card.getChildren().addAll(
                new Label("FEATURED EXHIBITION"),
                new Label(e.getTitle()) {
                    {
                        setStyle("-fx-font-weight: bold;");
                    }
                },
                new Label("Date: " + (e.getStartDate() != null ? e.getStartDate() : "")),
                new Label("Gallery: " + (e.getGallery() != null ? e.getGallery().getName() : "Unknown")));
        discoverPane.getChildren().add(card);
    }

    private void addWorkshopCard(Workshop w) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: #f1f8e9; -fx-border-color: #4caf50; -fx-border-radius: 5; -fx-background-radius: 5;");
        card.setPrefWidth(250);
        card.getChildren().addAll(
                new Label("UPCOMING WORKSHOP"),
                new Label(w.getTitle()) {
                    {
                        setStyle("-fx-font-weight: bold;");
                    }
                },
                new Label("Instructor: " + (w.getInstructor() != null ? w.getInstructor().getName() : "Unknown")),
                new Label("Price: $" + w.getPrice()));
        discoverPane.getChildren().add(card);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
