package client;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.shape.Circle;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import static shared.SharedConstants.*;

public class UIController implements ClientConnection.MessageListener {
    private TextArea textArea;
    private TextArea mainJavaArea;
    private TextField usernameField;
    private Button connectButton, disconnectButton;
    private Label statusLabel, statsLabel, errorLabel;
    private ListView<String> userListView;
    private ProgressIndicator syncSpinner;
    private ClientConnection connection;
    private String currentUsername;
    private boolean isApplyingExternalEdit;

    public BorderPane createUI() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d1117;");

        String darkThemeCss =
                ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: #0d1117; }" +
                        ".tab-pane { -fx-background-color: #0d1117; }" +
                        ".tab { -fx-background-color: #161b22; -fx-background-insets: 0 1 0 1; }" +
                        ".tab:selected { -fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-width: 1 1 0 1; }" +
                        ".tab .tab-label { -fx-text-fill: #8b949e; }" +
                        ".tab:selected .tab-label { -fx-text-fill: white; }" +
                        ".list-view { -fx-background-color: #0d1117; -fx-border-color: transparent; }" +
                        ".list-cell { -fx-background-color: #0d1117; -fx-text-fill: #e6edf3; }";
        root.getStylesheets().add("data:text/css," + darkThemeCss.replace(" ", "%20"));

        root.setLeft(createSidebar());
        root.setTop(createModernHeader());
        root.setCenter(createTabbedEditor());
        root.setRight(createCollaboratorPanel());
        return root;
    }

    private TabPane createTabbedEditor() {
        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: #0d1117;");

        Tab sharedTab = new Tab("main.txt", createEnhancedEditor());
        sharedTab.setClosable(false);

        Tab mainJavaTab = new Tab("Main.java", createSyntaxEditor());
        mainJavaTab.setClosable(false);

        tabPane.getTabs().addAll(sharedTab, mainJavaTab);
        return tabPane;
    }

    private VBox createSyntaxEditor() {
        VBox container = new VBox(0);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: #0d1117;");

        mainJavaArea = new TextArea();
        mainJavaArea.setPromptText("public class Main {\n    public static void main(String[] args) {\n        \n    }\n}");
        mainJavaArea.setStyle(
                "-fx-control-inner-background: #0d1117; " +
                        "-fx-text-fill: #dcdcaa; " +
                        "-fx-font-family: 'JetBrains Mono', 'Consolas', monospace; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: #30363d; " +
                        "-fx-border-radius: 8 8 0 0; " +
                        "-fx-background-radius: 8 8 0 0;"
        );
        VBox.setVgrow(mainJavaArea, Priority.ALWAYS);

        HBox errorPanel = new HBox(10);
        errorPanel.setPadding(new Insets(8, 20, 8, 20));
        errorPanel.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 8 8; -fx-background-radius: 0 0 8 8;");

        errorLabel = new Label("✓ No issues detected");
        errorLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");
        errorPanel.getChildren().add(errorLabel);

        mainJavaArea.textProperty().addListener((obs, old, newValue) -> {
            // CRITICAL: Only send changes if we're not applying a remote edit
            if (!isApplyingExternalEdit && connection != null && connection.isConnected()) {
                System.out.println("[DEBUG] Java editor change. Old length: " + old.length() + ", New length: " + newValue.length());
                handleTextChange("JAVA", old, newValue);
            }
            performLinterCheck(newValue);
        });

        container.getChildren().addAll(mainJavaArea, errorPanel);
        return container;
    }

    private void performLinterCheck(String code) {
        StringBuilder errors = new StringBuilder();
        int parens = 0, braces = 0;
        for(char c : code.toCharArray()) {
            if (c == '(') parens++; else if (c == ')') parens--;
            if (c == '{') braces++; else if (c == '}') braces--;
        }
        if (parens != 0) errors.append("• Missing parenthesis ");
        if (braces != 0) errors.append("• Unclosed braces ");
        if (code.contains("System.out") && !code.contains(";")) errors.append("• Missing semicolon ");

        Platform.runLater(() -> {
            if (errors.length() > 0) {
                errorLabel.setText("Issues: " + errors.toString());
                errorLabel.setStyle("-fx-text-fill: #f85149; -fx-font-size: 11px;");
            } else {
                errorLabel.setText("✓ Syntax looks good");
                errorLabel.setStyle("-fx-text-fill: #3fb950; -fx-font-size: 11px;");
            }
        });
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(25);
        sidebar.setPadding(new Insets(25, 15, 25, 15));
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setStyle("-fx-background-color: #161b22; -fx-border-color: #30363d; -fx-border-width: 0 1 0 0;");
        String[] icons = {"📁", "🔍", "⚙️", "🛠️", "❓"};
        for (String icon : icons) {
            Label lbl = new Label(icon);
            lbl.setStyle("-fx-font-size: 22px; -fx-text-fill: #8b949e; -fx-cursor: hand;");
            sidebar.getChildren().add(lbl);
        }
        return sidebar;
    }

    private HBox createModernHeader() {
        HBox header = new HBox(20);
        header.setPadding(new Insets(15, 30, 15, 30));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(22, 27, 34, 0.8); -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");
        Label logoLabel = new Label("COLLAB");
        logoLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white; -fx-letter-spacing: 2px;");
        Label editorLabel = new Label("IDE");
        editorLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #58a6ff; -fx-letter-spacing: 2px;");
        HBox logoBox = new HBox(2, logoLabel, editorLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        usernameField = new TextField();
        usernameField.setPromptText("Username...");
        usernameField.setPrefWidth(150);
        usernameField.setStyle("-fx-background-color: #0d1117; -fx-text-fill: white; -fx-border-color: #30363d; -fx-border-radius: 6; -fx-background-radius: 6;");
        connectButton = createStyledButton("Connect", "#238636");
        connectButton.setOnAction(e -> handleConnect());
        disconnectButton = createStyledButton("Exit", "#da3633");
        disconnectButton.setDisable(true);
        disconnectButton.setOnAction(e -> handleDisconnect());
        header.getChildren().addAll(logoBox, spacer, usernameField, connectButton, disconnectButton);
        return header;
    }

    private StackPane createEnhancedEditor() {
        StackPane container = new StackPane();
        container.setPadding(new Insets(20));
        VBox editorWrapper = new VBox(0);
        editorWrapper.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-radius: 8; -fx-background-radius: 8;");
        editorWrapper.setEffect(new DropShadow(15, Color.BLACK));

        HBox editorToolbar = new HBox(15);
        editorToolbar.setPadding(new Insets(10, 20, 10, 20));
        editorToolbar.setStyle("-fx-background-color: #161b22; -fx-background-radius: 8 8 0 0; -fx-border-color: #30363d; -fx-border-width: 0 0 1 0;");
        Label fileInfo = new Label("main.txt • Shared");
        fileInfo.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 12px;");
        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);
        syncSpinner = new ProgressIndicator();
        syncSpinner.setMaxSize(14, 14);
        syncSpinner.setVisible(false);
        syncSpinner.setStyle("-fx-progress-color: #58a6ff;");
        editorToolbar.getChildren().addAll(fileInfo, s, syncSpinner);

        textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPromptText("// Connect to start collaborative coding...");
        textArea.setStyle("-fx-control-inner-background: #0d1117; -fx-text-fill: #e6edf3; -fx-font-family: 'JetBrains Mono', 'Consolas', monospace; -fx-font-size: 14px; -fx-border-width: 0;");

        textArea.textProperty().addListener((obs, old, newValue) -> {
            // CRITICAL: Only send changes if we're not applying a remote edit
            if (!isApplyingExternalEdit && connection != null && connection.isConnected()) {
                System.out.println("[DEBUG] Local change detected. Old length: " + old.length() + ", New length: " + newValue.length());
                handleTextChange("TXT", old, newValue);
            }
            updateStats(newValue);
        });

        HBox footer = new HBox(20);
        footer.setPadding(new Insets(5, 20, 5, 20));
        footer.setStyle("-fx-background-color: #161b22; -fx-background-radius: 0 0 8 8;");
        statsLabel = new Label("UTF-8 | Line 1, Col 1");
        statsLabel.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 11px;");
        footer.getChildren().add(statsLabel);

        VBox.setVgrow(textArea, Priority.ALWAYS);
        editorWrapper.getChildren().addAll(editorToolbar, textArea, footer);
        container.getChildren().add(editorWrapper);
        return container;
    }

    private VBox createCollaboratorPanel() {
        VBox panel = new VBox(15);
        panel.setPrefWidth(250);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: #0d1117; -fx-border-color: #30363d; -fx-border-width: 0 0 0 1;");
        Label title = new Label("TEAM");
        title.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");
        userListView = new ListView<>();
        userListView.setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
        userListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox cell = new HBox(10);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    Circle dot = new Circle(4, Color.web("#3fb950"));
                    Label name = new Label(item);
                    name.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 13px;");
                    cell.getChildren().addAll(dot, name);
                    setGraphic(cell);
                }
                setStyle("-fx-background-color: transparent;");
            }
        });
        panel.getChildren().addAll(title, userListView);
        return panel;
    }

    private Button createStyledButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        b.setPadding(new Insets(6, 15, 6, 15));
        return b;
    }

    private void updateStats(String text) {
        int chars = text.length();
        int lines = text.split("\n").length;
        if(statsLabel != null) statsLabel.setText(String.format("Chars: %d | Lines: %d | UTF-8", chars, lines));
    }

    private void handleConnect() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) username = DEFAULT_USERNAME;
        currentUsername = username;
        connection = new ClientConnection(this);
        if (connection.connect(SERVER_HOST, SERVER_PORT)) {
            connection.sendMessage(buildMessage(MSG_JOIN, username));
            toggleUIState(true);
        } else {
            showAlert("Connection Failed", "Could not reach the collab server.", Alert.AlertType.ERROR);
        }
    }

    public void handleDisconnect() {
        if (connection != null) connection.disconnect();
        toggleUIState(false);
        userListView.getItems().clear();
    }

    private void toggleUIState(boolean connected) {
        Platform.runLater(() -> {
            connectButton.setDisable(connected);
            disconnectButton.setDisable(!connected);
            usernameField.setDisable(connected);
            textArea.setEditable(connected);
            mainJavaArea.setEditable(connected);
        });
    }

    private void handleTextChange(String fileType, String oldValue, String newValue) {
        int oldLen = oldValue.length(), newLen = newValue.length(), pos = 0;
        while (pos < Math.min(oldLen, newLen) && oldValue.charAt(pos) == newValue.charAt(pos)) pos++;
        if (newLen > oldLen) {
            String added = newValue.substring(pos, pos + (newLen - oldLen));
            connection.sendMessage(buildMessage(MSG_EDIT, fileType, String.valueOf(pos), added, currentUsername));
        } else if (newLen < oldLen) {
            connection.sendMessage(buildMessage(MSG_DELETE, fileType, String.valueOf(pos), String.valueOf(oldLen - newLen), currentUsername));
        }
        showSyncAnimation();
    }

    private void showSyncAnimation() {
        syncSpinner.setVisible(true);
        new Timeline(new KeyFrame(Duration.millis(600), e -> syncSpinner.setVisible(false))).play();
    }

    @Override
    public void onMessageReceived(String message) {
        String[] parts = parseMessage(message);
        if (parts.length < 2) return;

        Platform.runLater(() -> {
            String command = parts[0];

            if (command.equals(MSG_USER_LIST)) {
                userListView.getItems().clear();
                for (String u : parts[1].split(USER_LIST_DELIMITER)) userListView.getItems().add(u);
                return;
            }

            // For EDIT, DELETE, and SYNC, parts[1] is the FileType
            String fileType = parts[1];
            TextArea targetArea = fileType.equals("JAVA") ? mainJavaArea : textArea;

            switch (command) {
                case MSG_EDIT -> handleRemoteEdit(targetArea, parts);
                case MSG_DELETE -> handleRemoteDelete(targetArea, parts);
                case MSG_SYNC -> {
                    isApplyingExternalEdit = true;
                    targetArea.setText(parts[2]);
                    isApplyingExternalEdit = false;
                }
            }
        });
    }

    private void handleRemoteEdit(TextArea target, String[] parts) {
        if (parts.length < 4) return;
        int pos = Integer.parseInt(parts[2]);
        String txt = parts[3];
        isApplyingExternalEdit = true;
        int caret = target.getCaretPosition();
        target.insertText(pos, txt);
        target.positionCaret(caret >= pos ? caret + txt.length() : caret);
        isApplyingExternalEdit = false;
    }

    private void handleRemoteDelete(TextArea target, String[] parts) {
        if (parts.length < 4) return;
        int pos = Integer.parseInt(parts[2]);
        int len = Integer.parseInt(parts[3]);
        isApplyingExternalEdit = true;
        int caret = target.getCaretPosition();
        target.deleteText(pos, pos + len);
        int newCaret = (caret > pos + len) ? caret - len : (caret > pos ? pos : caret);
        target.positionCaret(newCaret);
        isApplyingExternalEdit = false;
    }

    private void showAlert(String t, String m, Alert.AlertType at) {
        Platform.runLater(() -> { Alert a = new Alert(at); a.setHeaderText(null); a.setTitle(t); a.setContentText(m); a.show(); });
    }

    @Override public void onConnectionLost() { handleDisconnect(); }
}