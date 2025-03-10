package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

public class TodoApp extends Application {
    
    // Lade SQLite-Treiber beim Starten der Klasse
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite-Treiber konnte nicht geladen werden: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Konstanten für Datenbankverbindung
    private static final String DB_URL = "jdbc:sqlite:todo.db";
    
    // UI Komponenten
    private ListView<Task> taskListView;
    private ListView<Task> historyListView;
    private ObservableList<Task> tasks;
    private ObservableList<Task> completedTasks;
    private TextArea descriptionArea;
    private boolean isDarkMode = false;
    
    // Labels für Überschriften
    private Label tasksLabel;
    private Label descriptionLabel;
    private Label historyLabel;
    
    // CSS Stile für Dark/Light Mode
    private static final String LIGHT_STYLE = "-fx-background-color: #f8f8f8; -fx-text-fill: #333333;";
    private static final String DARK_STYLE = "-fx-background-color: #333333; -fx-text-fill: #f8f8f8;";
    private static final String LIGHT_LIST_STYLE = "-fx-control-inner-background: #ffffff;";
    private static final String DARK_LIST_STYLE = "-fx-control-inner-background: #444444;";
    
    // Hauptmethode
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        // Initialisiere die Datenbank
        initializeDatabase();
        
        // Lade die Tasks aus der Datenbank
        tasks = FXCollections.observableArrayList();
        completedTasks = FXCollections.observableArrayList();
        loadTasksFromDatabase();
        
        // Erstelle die UI
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Toolbar mit Buttons
        HBox toolbar = createToolbar();
        root.setTop(toolbar);
        
        // Task ListView und Beschreibungsbereich
        VBox centerBox = new VBox(10);
        centerBox.setPadding(new Insets(5));
        
        // Überschriften als Klassenfelder
        tasksLabel = new Label("Aktive Tasks");
        tasksLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Task ListView
        taskListView = new ListView<>(tasks);
        taskListView.setCellFactory(new Callback<ListView<Task>, ListCell<Task>>() {
            @Override
            public ListCell<Task> call(ListView<Task> param) {
                return new TaskListCell();
            }
        });
        
        // Beschreibungsbereich
        descriptionLabel = new Label("Beschreibung");
        descriptionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        descriptionArea = new TextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setPrefHeight(100);
        descriptionArea.setWrapText(true);
        
        // Task Selection Listener
        taskListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                descriptionArea.setText(newSelection.getDescription());
            } else {
                descriptionArea.setText("");
            }
        });
        
        // History ListView
        historyLabel = new Label("Zuletzt erledigte Tasks");
        historyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        historyListView = new ListView<>(completedTasks);
        historyListView.setCellFactory(new Callback<ListView<Task>, ListCell<Task>>() {
            @Override
            public ListCell<Task> call(ListView<Task> param) {
                return new HistoryListCell();
            }
        });
        historyListView.setPrefHeight(150);
        
        // Add components to center box
        centerBox.getChildren().addAll(
            tasksLabel, taskListView, 
            descriptionLabel, descriptionArea,
            historyLabel, historyListView
        );
        
        // Setze die Box in den Hauptbereich
        root.setCenter(centerBox);
        
        // Erstelle die Scene und zeige das Fenster an
        Scene scene = new Scene(root, 700, 700);
        primaryStage.setTitle("Todo-Liste");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Prüfe und starte fällige Benachrichtigungen
        checkAndScheduleNotifications();
    }
    
    // Methode zum Erstellen der Toolbar
    private HBox createToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(5));
        
        // Button zum Hinzufügen von Tasks
        Button addButton = new Button("Neuer Task");
        addButton.setOnAction(e -> showAddTaskDialog());
        
        // Button zum Umschalten zwischen Dark/Light Mode
        Button themeButton = new Button("Dark Mode");
        themeButton.setOnAction(e -> {
            isDarkMode = !isDarkMode;
            applyTheme(themeButton);
        });
        
        toolbar.getChildren().addAll(addButton, themeButton);
        return toolbar;
    }
    
    // Theme anwenden
    private void applyTheme(Button themeButton) {
        Scene scene = themeButton.getScene();
        if (isDarkMode) {
            themeButton.setText("Light Mode");
            scene.getRoot().setStyle(DARK_STYLE);
            taskListView.setStyle(DARK_LIST_STYLE);
            historyListView.setStyle(DARK_LIST_STYLE);
            descriptionArea.setStyle(DARK_LIST_STYLE);
            
            // Überschriften im Dunkelmodus - weiße Schrift
            tasksLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
            descriptionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
            historyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
        } else {
            themeButton.setText("Dark Mode");
            scene.getRoot().setStyle(LIGHT_STYLE);
            taskListView.setStyle(LIGHT_LIST_STYLE);
            historyListView.setStyle(LIGHT_LIST_STYLE);
            descriptionArea.setStyle(LIGHT_LIST_STYLE);
            
            // Überschriften im Hellmodus - dunkle Schrift
            tasksLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;");
            descriptionLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;");
            historyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333333;");
        }
    }
    
    // Dialog zum Hinzufügen neuer Tasks
    private void showAddTaskDialog() {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Neuen Task hinzufügen");
        dialog.setHeaderText("Bitte gib die Details für den neuen Task ein.");
        
        // Buttons einrichten
        ButtonType saveButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Erstelle das Layout für den Dialog
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField titleField = new TextField();
        titleField.setPromptText("Titel");
        
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Beschreibung");
        
        DatePicker datePicker = new DatePicker(LocalDate.now());
        
        // Zeiteingabe
        ComboBox<Integer> hourBox = new ComboBox<>();
        for (int i = 0; i < 24; i++) {
            hourBox.getItems().add(i);
        }
        hourBox.setValue(LocalTime.now().getHour());
        
        ComboBox<Integer> minuteBox = new ComboBox<>();
        for (int i = 0; i < 60; i++) {
            minuteBox.getItems().add(i);
        }
        minuteBox.setValue(LocalTime.now().getMinute());
        
        HBox timeBox = new HBox(5);
        timeBox.getChildren().addAll(hourBox, new Label(":"), minuteBox);
        
        ComboBox<String> repeatBox = new ComboBox<>();
        repeatBox.getItems().addAll("Einmalig", "Täglich", "Wöchentlich", "Monatlich", "Jährlich");
        repeatBox.setValue("Einmalig");
        
        grid.add(new Label("Titel:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Beschreibung:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Datum:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Zeit:"), 0, 3);
        grid.add(timeBox, 1, 3);
        grid.add(new Label("Wiederholung:"), 0, 4);
        grid.add(repeatBox, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        
        // Fokus auf Titelfeld setzen
        Platform.runLater(titleField::requestFocus);
        
        // Konvertiere den Ergebniswert
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String title = titleField.getText();
                String description = descriptionArea.getText();
                LocalDate dueDate = datePicker.getValue();
                int hour = hourBox.getValue();
                int minute = minuteBox.getValue();
                LocalDateTime dueDateTime = LocalDateTime.of(dueDate, LocalTime.of(hour, minute));
                String repeat = repeatBox.getValue();
                
                if (title != null && !title.isEmpty()) {
                    Task newTask = new Task(0, title, description, dueDateTime, repeat, false);
                    saveTaskToDatabase(newTask);
                    return newTask;
                }
            }
            return null;
        });
        
        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(task -> {
            // Aktualisiere die Benachrichtigungen
            scheduleNotification(task);
        });
    }
    
    // Dialog zum Bearbeiten bestehender Tasks
    private void showEditTaskDialog(Task task) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Task bearbeiten");
        dialog.setHeaderText("Bearbeite die Details für den Task.");
        
        // Buttons einrichten
        ButtonType saveButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Erstelle das Layout für den Dialog
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField titleField = new TextField(task.getTitle());
        TextArea descriptionArea = new TextArea(task.getDescription());
        DatePicker datePicker = new DatePicker(task.getDueDateTime().toLocalDate());
        
        // Zeiteingabe
        ComboBox<Integer> hourBox = new ComboBox<>();
        for (int i = 0; i < 24; i++) {
            hourBox.getItems().add(i);
        }
        hourBox.setValue(task.getDueDateTime().getHour());
        
        ComboBox<Integer> minuteBox = new ComboBox<>();
        for (int i = 0; i < 60; i++) {
            minuteBox.getItems().add(i);
        }
        minuteBox.setValue(task.getDueDateTime().getMinute());
        
        HBox timeBox = new HBox(5);
        timeBox.getChildren().addAll(hourBox, new Label(":"), minuteBox);
        
        ComboBox<String> repeatBox = new ComboBox<>();
        repeatBox.getItems().addAll("Einmalig", "Täglich", "Wöchentlich", "Monatlich", "Jährlich");
        repeatBox.setValue(task.getRepeatType());
        
        CheckBox completedCheckBox = new CheckBox("Erledigt");
        completedCheckBox.setSelected(task.isCompleted());
        
        grid.add(new Label("Titel:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Beschreibung:"), 0, 1);
        grid.add(descriptionArea, 1, 1);
        grid.add(new Label("Datum:"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Zeit:"), 0, 3);
        grid.add(timeBox, 1, 3);
        grid.add(new Label("Wiederholung:"), 0, 4);
        grid.add(repeatBox, 1, 4);
        grid.add(completedCheckBox, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        
        // Fokus auf Titelfeld setzen
        Platform.runLater(titleField::requestFocus);
        
        // Konvertiere den Ergebniswert
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String title = titleField.getText();
                String description = descriptionArea.getText();
                LocalDate dueDate = datePicker.getValue();
                int hour = hourBox.getValue();
                int minute = minuteBox.getValue();
                LocalDateTime dueDateTime = LocalDateTime.of(dueDate, LocalTime.of(hour, minute));
                String repeat = repeatBox.getValue();
                boolean completed = completedCheckBox.isSelected();
                
                if (title != null && !title.isEmpty()) {
                    task.setTitle(title);
                    task.setDescription(description);
                    task.setDueDateTime(dueDateTime);
                    task.setRepeatType(repeat);
                    
                    // Überprüfe, ob sich der Completed-Status geändert hat
                    boolean wasCompleted = task.isCompleted();
                    task.setCompleted(completed);
                    
                    // Aktualisiere die Listen basierend auf dem Completed-Status
                    if (!wasCompleted && completed) {
                        // Task wurde als erledigt markiert
                        tasks.remove(task);
                        
                        // Füge den Task zur Verlaufsliste hinzu und halte sie auf max. 10 Items
                        completedTasks.add(0, task); // Am Anfang einfügen
                        while (completedTasks.size() > 10) {
                            completedTasks.remove(completedTasks.size() - 1); // Entferne das älteste Element
                        }
                    } else if (wasCompleted && !completed) {
                        // Task wurde als nicht erledigt markiert
                        completedTasks.remove(task);
                        tasks.add(task);
                    }
                    
                    updateTaskInDatabase(task);
                    return task;
                }
            }
            return null;
        });
        
        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(updatedTask -> {
            // Aktualisiere die Beschreibung im Beschreibungsbereich, falls der Task aktuell ausgewählt ist
            Task selectedTask = taskListView.getSelectionModel().getSelectedItem();
            if (selectedTask != null && selectedTask.getId() == updatedTask.getId()) {
                descriptionArea.setText(updatedTask.getDescription());
            }
            
            // Aktualisiere die Benachrichtigungen
            scheduleNotification(updatedTask);
        });
    }
    
    // Initialisiere die Datenbank
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Erstelle die Tasks-Tabelle, falls sie nicht existiert
            String sql = "CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL," +
                    "description TEXT," +
                    "due_date TEXT," + 
                    "repeat_type TEXT," +
                    "completed INTEGER" +
                    ")";
            stmt.execute(sql);
            
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorDialog("Datenbankfehler", "Fehler beim Initialisieren der Datenbank: " + e.getMessage());
        }
    }
    
    // Lade Tasks aus der Datenbank
    private void loadTasksFromDatabase() {
        tasks.clear();
        completedTasks.clear();
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tasks ORDER BY due_date DESC")) { // Sortiere nach Datum absteigend
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String title = rs.getString("title");
                String description = rs.getString("description");
                LocalDateTime dueDateTime = LocalDateTime.parse(rs.getString("due_date"), formatter);
                String repeatType = rs.getString("repeat_type");
                boolean completed = rs.getInt("completed") == 1;
                
                Task task = new Task(id, title, description, dueDateTime, repeatType, completed);
                
                // Füge Task zur entsprechenden Liste hinzu
                if (completed) {
                    completedTasks.add(task);
                    // Begrenze die History auf 10 Einträge
                    if (completedTasks.size() > 10) {
                        completedTasks.remove(completedTasks.size() - 1);
                    }
                } else {
                    tasks.add(task);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorDialog("Datenbankfehler", "Fehler beim Laden der Tasks: " + e.getMessage());
        }
    }
    
    // Speichere einen neuen Task in der Datenbank
    private void saveTaskToDatabase(Task task) {
        String sql = "INSERT INTO tasks (title, description, due_date, repeat_type, completed) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getDueDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            pstmt.setString(4, task.getRepeatType());
            pstmt.setInt(5, task.isCompleted() ? 1 : 0);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setId(generatedKeys.getInt(1));
                    }
                }
            }
            
            // Füge den Task am Anfang der Liste hinzu (für neue Sortierung)
            if (!task.isCompleted()) {
                Platform.runLater(() -> {
                    tasks.add(0, task);
                });
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorDialog("Datenbankfehler", "Fehler beim Speichern des Tasks: " + e.getMessage());
        }
    }
    
    // Aktualisiere einen Task in der Datenbank
    private void updateTaskInDatabase(Task task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, due_date = ?, repeat_type = ?, completed = ? WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getDueDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            pstmt.setString(4, task.getRepeatType());
            pstmt.setInt(5, task.isCompleted() ? 1 : 0);
            pstmt.setInt(6, task.getId());
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorDialog("Datenbankfehler", "Fehler beim Aktualisieren des Tasks: " + e.getMessage());
        }
    }
    
    // Lösche einen Task aus der Datenbank
    private void deleteTaskFromDatabase(Task task) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, task.getId());
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorDialog("Datenbankfehler", "Fehler beim Löschen des Tasks: " + e.getMessage());
        }
    }
    
    // Benachrichtigung für einen Task planen
    private void scheduleNotification(Task task) {
        // In einer realen Anwendung würde hier ein System für Benachrichtigungen implementiert werden
        // Für dieses Beispiel verwenden wir einen einfachen Ansatz mit einem separaten Thread
        
        if (task.isCompleted()) {
            return; // Keine Benachrichtigungen für erledigte Tasks
        }
        
        // Überprüfe, ob das Fälligkeitsdatum in der Vergangenheit liegt
        if (task.getDueDateTime().isBefore(LocalDateTime.now())) {
            // Für wiederholende Tasks, plane sofort den nächsten Termin und erstelle einen neuen Task
            if (!task.getRepeatType().equals("Einmalig")) {
                Platform.runLater(() -> {
                    rescheduleTask(task);
                });
            }
            return;
        }
        
        Thread notificationThread = new Thread(() -> {
            try {
                // Berechne die Zeit bis zum Fälligkeitsdatum
                long millisToWait = java.time.temporal.ChronoUnit.MILLIS.between(
                        LocalDateTime.now(),
                        task.getDueDateTime());
                
                if (millisToWait > 0) {
                    Thread.sleep(millisToWait);
                    
                    // Zeige die Benachrichtigung an
                    Platform.runLater(() -> {
                        showNotification("Task fällig", "Der Task \"" + task.getTitle() + "\" ist jetzt fällig!");
                        
                        // Für wiederholende Tasks, plane den nächsten Termin
                        if (!task.getRepeatType().equals("Einmalig")) {
                            rescheduleTask(task);
                        }
                    });
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        notificationThread.setDaemon(true);
        notificationThread.start();
    }
    
    // Prüfe und plane alle Benachrichtigungen
    private void checkAndScheduleNotifications() {
        for (Task task : tasks) {
            // Überprüfe, ob der Task in der Vergangenheit liegt und sich wiederholt
            if (task.getDueDateTime().isBefore(LocalDateTime.now()) && 
                !task.getRepeatType().equals("Einmalig")) {
                
                // Erstelle eine Kopie der Task-Liste, um ConcurrentModificationException zu vermeiden
                scheduleNotification(task);
            } else {
                scheduleNotification(task);
            }
        }
    }
    
    // Wiederholenden Task neu planen
    private void rescheduleTask(Task task) {
        LocalDateTime newDueDateTime = null;
        
        switch (task.getRepeatType()) {
            case "Täglich":
                newDueDateTime = task.getDueDateTime().plusDays(1);
                break;
            case "Wöchentlich":
                newDueDateTime = task.getDueDateTime().plusWeeks(1);
                break;
            case "Monatlich":
                newDueDateTime = task.getDueDateTime().plusMonths(1);
                break;
            case "Jährlich":
                newDueDateTime = task.getDueDateTime().plusYears(1);
                break;
            default:
                return; // Kein wiederholender Task
        }
        
        // Erstelle einen neuen Task für die Wiederholung
        Task newTask = new Task(0, task.getTitle(), task.getDescription(), newDueDateTime, task.getRepeatType(), false);
        saveTaskToDatabase(newTask);
        
        // Die Hinzufügung des Tasks zur Liste geschieht jetzt in saveTaskToDatabase
        
        // Plane die Benachrichtigung für den neuen Task
        scheduleNotification(newTask);
    }
    
    // Zeige eine Benachrichtigung an
    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    
    // Zeige einen Fehlerdialog
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // CustomListCell für die Anzeige von Tasks
    private class TaskListCell extends ListCell<Task> {
        private final HBox content;
        private final Label titleLabel;
        private final Label dateLabel;
        private final Label repeatLabel;
        private final CheckBox completedCheckBox;
        private final Button editButton;
        private final Button deleteButton;
        
        public TaskListCell() {
            content = new HBox(10);
            content.setPadding(new Insets(5));
            
            VBox textVBox = new VBox(5);
            titleLabel = new Label();
            titleLabel.setStyle("-fx-font-weight: bold;");
            dateLabel = new Label();
            repeatLabel = new Label();
            textVBox.getChildren().addAll(titleLabel, dateLabel, repeatLabel);
            
            completedCheckBox = new CheckBox();
            completedCheckBox.setOnAction(e -> {
                Task task = getItem();
                if (task != null) {
                    task.setCompleted(completedCheckBox.isSelected());
                    
                    // Wenn als erledigt markiert, verschiebe aus Hauptliste in Verlaufsliste
                    if (completedCheckBox.isSelected()) {
                        tasks.remove(task);
                        
                        // Füge den Task zur Verlaufsliste hinzu und halte sie auf max. 10 Items
                        completedTasks.add(0, task); // Am Anfang einfügen
                        while (completedTasks.size() > 10) {
                            completedTasks.remove(completedTasks.size() - 1); // Entferne das älteste Element
                        }
                    }
                    
                    updateTaskInDatabase(task);
                    updateItem(task, false);
                }
            });
            
            editButton = new Button("Bearbeiten");
            editButton.setOnAction(e -> {
                Task task = getItem();
                if (task != null) {
                    showEditTaskDialog(task);
                }
            });
            
            deleteButton = new Button("Löschen");
            deleteButton.setOnAction(e -> {
                Task task = getItem();
                if (task != null) {
                    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION,
                            "Möchtest du diesen Task wirklich löschen?", ButtonType.YES, ButtonType.NO);
                    confirmDialog.setTitle("Task löschen");
                    confirmDialog.setHeaderText(null);
                    
                    Optional<ButtonType> result = confirmDialog.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES) {
                        deleteTaskFromDatabase(task);
                        tasks.remove(task);
                    }
                }
            });
            
            HBox buttonsBox = new HBox(5);
            buttonsBox.getChildren().addAll(editButton, deleteButton);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            content.getChildren().addAll(completedCheckBox, textVBox, spacer, buttonsBox);
        }
        
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            
            if (empty || task == null) {
                setGraphic(null);
            } else {
                titleLabel.setText(task.getTitle());
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                dateLabel.setText("Fällig am: " + task.getDueDateTime().format(formatter));
                
                repeatLabel.setText("Wiederholung: " + task.getRepeatType());
                
                completedCheckBox.setSelected(task.isCompleted());
                
                if (task.isCompleted()) {
                    titleLabel.setStyle("-fx-font-weight: bold; -fx-strikethrough: true;");
                } else {
                    titleLabel.setStyle("-fx-font-weight: bold;");
                }
                
                setGraphic(content);
            }
        }
    }
    
    // CustomListCell für die Anzeige von erledigten Tasks in der History
    private class HistoryListCell extends ListCell<Task> {
        private final HBox content;
        private final Label titleLabel;
        private final Label dateLabel;
        private final Button restoreButton;
        
        public HistoryListCell() {
            content = new HBox(10);
            content.setPadding(new Insets(5));
            
            VBox textVBox = new VBox(5);
            titleLabel = new Label();
            titleLabel.setStyle("-fx-font-weight: bold; -fx-strikethrough: true;");
            dateLabel = new Label();
            textVBox.getChildren().addAll(titleLabel, dateLabel);
            
            restoreButton = new Button("Wiederherstellen");
            restoreButton.setOnAction(e -> {
                Task task = getItem();
                if (task != null) {
                    // Markiere als nicht erledigt
                    task.setCompleted(false);
                    updateTaskInDatabase(task);
                    
                    // Verschiebe von History zur Hauptliste
                    completedTasks.remove(task);
                    tasks.add(task);
                }
            });
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            content.getChildren().addAll(textVBox, spacer, restoreButton);
        }
        
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            
            if (empty || task == null) {
                setGraphic(null);
            } else {
                titleLabel.setText(task.getTitle());
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                dateLabel.setText("Erledigt am: " + task.getDueDateTime().format(formatter));
                
                setGraphic(content);
            }
        }
    }
}