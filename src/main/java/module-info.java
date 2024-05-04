module com.example.sortvisualizer {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.sortvisualizer to javafx.fxml;
    exports com.example.sortvisualizer;
}