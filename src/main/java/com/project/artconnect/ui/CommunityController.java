package com.project.artconnect.ui;

import com.project.artconnect.model.CommunityMember;
import com.project.artconnect.service.CommunityService;
import com.project.artconnect.util.ServiceProvider;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class CommunityController {
    @FXML
    private TableView<CommunityMember> memberTable;
    @FXML
    private TableColumn<CommunityMember, String> nameColumn;
    @FXML
    private TableColumn<CommunityMember, String> emailColumn;
    @FXML
    private TableColumn<CommunityMember, String> cityColumn;
    @FXML
    private TableColumn<CommunityMember, String> membershipColumn;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField cityField;
    @FXML
    private TextField membershipField;
    @FXML
    private TextField searchField;

    private final CommunityService communityService = ServiceProvider.getCommunityService();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        membershipColumn.setCellValueFactory(new PropertyValueFactory<>("membershipType"));

        memberTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldMember, selectedMember) -> fillForm(selectedMember)
        );

        refreshTable();
    }

    @FXML
    private void handleAddMember() {
        try {
            CommunityMember member = buildMemberFromForm();
            communityService.save(member);
            refreshTable();
            clearForm();
            showInfo("Member added successfully.");
        } catch (Exception e) {
            showError("Could not add member", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateMember() {
        CommunityMember selectedMember = memberTable.getSelectionModel().getSelectedItem();

        if (selectedMember == null) {
            showError("No member selected", "Please select a member to update.");
            return;
        }

        try {
            CommunityMember member = buildMemberFromForm();
            if (!selectedMember.getName().equals(member.getName())) {
                communityService.delete(selectedMember.getName());
            }
            communityService.update(member);
            refreshTable();
            clearForm();
            showInfo("Member updated successfully.");
        } catch (Exception e) {
            showError("Could not update member", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteMember() {
        CommunityMember selectedMember = memberTable.getSelectionModel().getSelectedItem();

        if (selectedMember == null) {
            showError("No member selected", "Please select a member to delete.");
            return;
        }

        try {
            communityService.delete(selectedMember.getName());
            refreshTable();
            clearForm();
            showInfo("Member deleted successfully.");
        } catch (Exception e) {
            showError("Could not delete member", e.getMessage());
        }
    }

    private void refreshTable() {
        memberTable.setItems(FXCollections.observableArrayList(communityService.getAllMembers()));
    }

    @FXML
    private void handleSearch() {
        String query = normalized(searchField.getText());
        memberTable.setItems(FXCollections.observableArrayList(
                communityService.getAllMembers().stream()
                        .filter(member -> query.isBlank()
                                || contains(member.getName(), query)
                                || contains(member.getEmail(), query)
                                || contains(member.getCity(), query)
                                || contains(member.getMembershipType(), query))
                        .toList()
        ));
    }

    @FXML
    private void handleResetSearch() {
        searchField.clear();
        refreshTable();
    }

    private CommunityMember buildMemberFromForm() {
        String name = nameField.getText();
        String email = emailField.getText();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Member name is required.");
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Member email is required.");
        }

        CommunityMember member = new CommunityMember(name.trim(), email.trim());
        member.setCity(cityField.getText());
        member.setMembershipType(normalizeMembershipType(membershipField.getText()));
        return member;
    }

    private void fillForm(CommunityMember member) {
        if (member == null) {
            return;
        }

        nameField.setText(member.getName());
        emailField.setText(member.getEmail());
        cityField.setText(member.getCity());
        membershipField.setText(member.getMembershipType());
    }

    private void clearForm() {
        nameField.clear();
        emailField.clear();
        cityField.clear();
        membershipField.clear();
        memberTable.getSelectionModel().clearSelection();
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

    private String normalizeMembershipType(String membershipType) {
        if (membershipType == null || membershipType.isBlank()) {
            return "BASIC";
        }

        String normalized = membershipType.trim().toUpperCase();
        return switch (normalized) {
            case "PREMIUM" -> "PREMIUM";
            case "BASIC", "FREE" -> "BASIC";
            default -> throw new IllegalArgumentException("Membership must be BASIC or PREMIUM.");
        };
    }
}
