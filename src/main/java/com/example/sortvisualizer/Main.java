package com.example.sortvisualizer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {


    private Controller controller;

    public Main() {

        controller = new Controller(this);

    }

    private static final int WINDOW_WIDTH = 1920;
    private static final int WINDOW_HEIGHT = 1080;


    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);


        stage.setTitle("SortVisualizer");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setFullScreen(true);

        stage.setFullScreenExitHint("F11 drücken um den Vollbildmodus zu beenden");

        stage.setOnCloseRequest(event -> {
            if (controller.getTimerThread() != null && controller.getTimerThread().isAlive()) {
                controller.getTimerThread().interrupt(); // Interrupt the timer thread if it is running
            }
            // Perform any other necessary cleanup operations

            Platform.exit(); // Exit the JavaFX application
            System.exit(0); // Terminate the program
        });

        // Set the full-screen exit key combination
        KeyCombination keyCombination = KeyCombination.keyCombination("F11");
        stage.setFullScreenExitKeyCombination(keyCombination);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }




}
