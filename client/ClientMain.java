package client;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class ClientMain extends Application {
    private UIController controller;
    @Override
    public void start(Stage primaryStage) {
        controller = new UIController();
        Scene scene = new Scene(controller.createUI(), 1000, 700);
        primaryStage.setTitle("Real-Time Collaborative Text Editor - Client");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setOnCloseRequest(e -> {
            System.out.println("[ClientMain] Window closing, disconnecting...");
            if (controller != null) {
                controller.handleDisconnect();
            }
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
        System.out.println("[ClientMain] Client application started");
    }
    @Override
    public void stop() throws Exception {
        System.out.println("[ClientMain] Application stopping...");
        if (controller != null) {
            controller.handleDisconnect();
        }
        super.stop();
    }
    public static void main(String[] args) {
        launch(args);
    }
}