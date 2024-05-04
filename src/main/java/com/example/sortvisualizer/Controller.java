package com.example.sortvisualizer;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class Controller implements Initializable {

    // ich hab es zuerst probiert mit normalen panes für die bars die in einer Hbox sind, das war aber ziemlich umständlich und nervig
    // dann hab ich BarChart gefunden was für säulen diagramme gedacht ist, ich hab dann einfach alle bars weiß gefärbt und verbunden mit dem array size slider
    // ich hab auch den slider werten globale final variablen gegeben damit man sie leicht ändern kann
    // ich habe mit fxml/scenebuilder gearbeitet deswegen habe ich nur die Main, Controller und das hello-view.fxml files und kein richtiges model
    // wenn mein program noch größer geworden wäre hätte ich die sort algorithmen alle auslagern können, hier finde ich, aber hat es wenig sinn gemacht
    // da es das program nur komplizierter macht
    // und so hält es das program "kleiner" und "simpler"


    private Main main;

    private static final String[] allSorts = {
            "Bubble Sort",
            "Merge Sort",
            "Quicksort (LR pointers)",
            "Selection Sort",
            "Gnome Sort",
            "Bitonic Sort",
            "Comb Sort",
            "Cocktail Shaker Sort",
            "Radix Sort (LSD)",
            "Bogo Sort",
            "Insertion Sort",
            "Shell Sort",
            "Spread Sort"
    };

    private final static int MAX_ARRAY_SIZE = 400;
    private final static int MIN_ARRAY_SIZE = 2;

    private final static int MAX_DELAY_SIZE = 1000;
    private final static int MIN_DELAY_SIZE = 1;

    private long startTime;
    private Thread timerThread;

    @FXML
    private BorderPane borderPane;

    @FXML
    private ToolBar toolBarForAllItems;

    @FXML
    private ChoiceBox<String> sortSelectChoiceBox;

    @FXML
    private Label arraySizeLabel;
    @FXML
    private Slider arraySizeSlider;
    @FXML
    private Label arraySizeNumberLabel;

    @FXML
    private Label delaySizeLabel;
    @FXML
    private Slider delaySizeSlider;
    @FXML
    private Label delaySizeNumberLabel;

    @FXML
    private Label statusPromptLabel;
    @FXML
    private Label currentStatusLabel;

    @FXML
    private Label algorithmPromptLabel;
    @FXML
    private Label currentAlgorithmLabel;

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private Button startButton;

    @FXML
    private Button randomizeBarsButton;

    @FXML
    private Button exitButton;

    @FXML
    private Label timePromptLabel;
    @FXML
    private Label timeSizeNumberLabel;

    private int currentIndex = -1; // speichert index von der aktuellen bar

    public Controller(Main main) {
        this.main = main;
    }

    public Controller() {
    } // Standardkonstruktor

    public void onExitButtonClick() {
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt(); // interrupt timer thread, wenn noch läuft
        }

        Platform.exit(); // schließt die javafx application thread
        System.exit(0); // exit das programm
    }

    public void onRandomizeBarsButtonClick() {
        int[] array = generateRandomArray((int) arraySizeSlider.getValue());
        updateBarChart(array);
        highlightCurrentBar(-1);
    }

    public void onStartButtonClick() {
        startButton.setDisable(true);
        sortSelectChoiceBox.setDisable(true);
        arraySizeSlider.setDisable(true);
        delaySizeSlider.setDisable(true);
        randomizeBarsButton.setDisable(true);

        String selectedSort = sortSelectChoiceBox.getValue();

        startTime = System.nanoTime();  // misst die zeit


        switch (selectedSort) { // enhanced switch
            case "Bubble Sort" -> bubbleSort();
            case "Merge Sort" -> mergeSort();
            case "Quicksort (LR pointers)" -> quickSortLRPointers();
            case "Selection Sort" -> selectionSort();
            case "Gnome Sort" -> gnomeSort();
            case "Bitonic Sort" -> bitonicSort();
            case "Cocktail Shaker Sort" -> cocktailShakerSort();
            case "Insertion Sort" -> insertionSort();
            case "Radix Sort (LSD)" -> radixSortLSD();
            case "Bogo Sort" -> bogoSort();
            case "Shell Sort" -> shellSort();
            case "Comb Sort" -> combSort();
            case "Spread Sort" -> pairwiseSort();
            default -> {
            }

        }

        // timer thread start
        timerThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                long elapsedTime = System.nanoTime() - startTime;
                double seconds = (double) elapsedTime / 1_000_000_000.0;

                Platform.runLater(() -> {
                    timeSizeNumberLabel.setText(String.format("%.3fs", seconds));
                });

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });

        timerThread.start();
    }


    private void pairwiseSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("SPREAD SORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        // task vom typ Void der den sortiervorgang ausführt
        // ein Task ist eine abstract class in javafx die die Ausführung einer Operation in einem separaten Thread ermöglicht
        // hier wird ein Task<Void> verwendet, da nix zurückgegeben wird
        // der task wird verwendet, um das sorten im Hintergrund auszuführen damit die restliche GUI nicht stecken bleibt
        // Task vs Service
        // einen task kann man nur einmal ausführen und einen service kann man immer wieder ausführen lassen
        // wenn ich zb wie hier einen task nochmal ausführen lassen will muss ich jedes mal einen neuen Task instanziieren
        // also währe ein service vielleicht hier schlauer gewesen

        Task<Void> pairwiseSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int n = array.length;
                int comparisons = (int) (Math.log(n) / Math.log(2));

                boolean sorted = false;
                while (!sorted) {
                    sorted = true;
                    for (int i = 0; i < n - 1; i += 2) {
                        if (array[i] > array[i + 1]) {
                            swap(array, i, i + 1);
                            sorted = false;
                        }

                        currentIndex = i;
                        Platform.runLater(() -> {
                            updateBarChart(array);
                            highlightCurrentBar(currentIndex);
                        });

                        Thread.sleep(delayValue);
                    }
                    for (int i = 1; i < n - 1; i += 2) {
                        if (array[i] > array[i + 1]) {
                            swap(array, i, i + 1);
                            sorted = false;
                        }

                        currentIndex = i;
                        Platform.runLater(() -> {
                            updateBarChart(array);
                            highlightCurrentBar(currentIndex);
                        });

                        Thread.sleep(delayValue);
                    }
                }

                currentIndex = -1;

                // eine javafx application rennt in dem "Application thread" der alle GUI elemente managed
                // wenn man jetzt aber zumbeispiel mehere funktionen hat die gleichzeitg ausgeführt werden sollen kann es zu einem freeze von der gui kommen
                // es muss immer gewartet werden bis die eine funktion fertig ist bevor man eine andere ausführt, dafür gibt es threads
                // also wenn zb ein Button1 etwas rechnet und somit zb ein label updated, sollte man das rechnen in einen seperaten thread hineingeben und alles was die
                // gui betrifft wird mit Platform.runLater in den Application thread verschoben so dass nichts freezed


                Platform.runLater(() -> {
                    updateBarChart(array);
                    highlightCurrentBar(currentIndex);
                });

                return null;
            }
        };

        pairwiseSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);

            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = 1;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));

                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {
                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);

                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame opacityKeyFrame = new KeyFrame(delayTime, e -> node.setOpacity(targetOpacity));
                timeline.getKeyFrames().add(opacityKeyFrame);
            }

            timeline.play();
            timerThread.interrupt();
        });

        Thread pairwiseSortThread = new Thread(pairwiseSortTask);

        // standardmäßig sind threads in java "user threads"
        // wenn der main thread (main thread == ein user thread) fertig ist
        // müssen alle user threads auch fertig sein, erst dann wird die JVM terminiert
        // "daemon threads" hingegen müssen nicht mit ihrem code fertig werden, die JVM wird einfach beendet

        pairwiseSortThread.setDaemon(true);
        pairwiseSortThread.start();
    }

    private void combSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("COMB SORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> combSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int gap = array.length;
                boolean swapped = true;
                while (gap > 1 || swapped) {
                    gap = (int) (gap / 1.3); // Shrink the gap size
                    if (gap < 1) {
                        gap = 1;
                    }
                    swapped = false;
                    for (int i = 0; i + gap < array.length; i++) {
                        if (array[i] > array[i + gap]) {
                            swap(array, i, i + gap);
                            swapped = true;
                        }

                        currentIndex = i;
                        Platform.runLater(() -> {
                            updateBarChart(array);
                            highlightCurrentBar(currentIndex);
                        });

                        Thread.sleep(delayValue);
                    }
                }
                return null;
            }
        };

        combSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);

            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = 1;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));

                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {
                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);

                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame opacityKeyFrame = new KeyFrame(delayTime, e -> node.setOpacity(targetOpacity));
                timeline.getKeyFrames().add(opacityKeyFrame);
            }

            timeline.play();
            timerThread.interrupt();
        });

        Thread combSortThread = new Thread(combSortTask);
        combSortThread.setDaemon(true);
        combSortThread.start();
    }


    private void shellSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("Shell Sort");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> shellSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int n = array.length;
                int gap = n / 2;

                while (gap > 0) {
                    for (int i = gap; i < n; i++) {
                        int temp = array[i];
                        int j = i;

                        while (j >= gap && array[j - gap] > temp) {
                            array[j] = array[j - gap];
                            j -= gap;

                            currentIndex = j;
                            Platform.runLater(() -> {
                                updateBarChart(array);
                                highlightCurrentBar(currentIndex);
                            });

                            Thread.sleep(delayValue);
                        }

                        array[j] = temp;
                    }

                    gap /= 2;
                }

                return null;
            }
        };

        shellSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);

            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = 1;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));

                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {
                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);

                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame opacityKeyFrame = new KeyFrame(delayTime, e -> node.setOpacity(targetOpacity));
                timeline.getKeyFrames().add(opacityKeyFrame);
            }

            timeline.play();
            timerThread.interrupt();
        });

        Thread shellSortThread = new Thread(shellSortTask);
        shellSortThread.setDaemon(true);
        shellSortThread.start();
    }


    //für radixSortLSD
    private void radixSortLSD() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("RADIX SORT (LSD)");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> radixSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int maxNumber = getMaxNumber(array);
                int numDigits = getNumDigits(maxNumber);

                // Perform LSD Radix Sort
                for (int exp = 1; maxNumber / exp > 0; exp *= 10) {
                    countingSort(array, exp);

                    currentIndex = -1;
                    Platform.runLater(() -> {
                        updateBarChart(array);
                        highlightCurrentBar(currentIndex);
                    });

                    Thread.sleep(delayValue);
                }

                return null;
            }
        };

        radixSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);

            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = 1;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));

                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {
                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);

                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame opacityKeyFrame = new KeyFrame(delayTime, e -> node.setOpacity(targetOpacity));
                timeline.getKeyFrames().add(opacityKeyFrame);
            }

            timeline.play();
            timerThread.interrupt();
        });

        Thread radixSortThread = new Thread(radixSortTask);
        radixSortThread.setDaemon(true);
        radixSortThread.start();
    }

    //für radixSortLSD
    private int getMaxNumber(int[] array) {
        int max = Integer.MIN_VALUE;
        for (int number : array) {
            if (number > max) {
                max = number;
            }
        }
        return max;
    }

    //für radixSortLSD
    private int getNumDigits(int number) {
        return (int) Math.floor(Math.log10(number)) + 1;
    }

    //für radixSortLSD
    private void countingSort(int[] array, int exp) {
        int n = array.length;
        int[] output = new int[n];
        int[] count = new int[10];

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        for (int i = 0; i < n; i++) {
            int digit = (array[i] / exp) % 10;
            count[digit]++;
        }

        for (int i = 1; i < 10; i++) {
            count[i] += count[i - 1];
        }


        for (int i = n - 1; i >= 0; i--) {

            int digit = (array[i] / exp) % 10;

            output[count[digit] - 1] = array[i];

            count[digit]--;




            currentIndex = i; // die current bar


            Platform.runLater(() -> {
                updateBarChart(array);
                highlightCurrentBar(currentIndex);
            });

            try {
                Thread.sleep(delayValue);
            } catch (InterruptedException e) {
                break;
            }
        }

        for (int i = 0; i < n; i++) {
            array[i] = output[i];
        }
    }


    //für cocktailShakerSort
    private void cocktailShakerSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("COCKTAIL SHAKER SORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> cocktailShakerSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int start = 0;
                int end = array.length - 1;
                boolean swapped;

                do {
                    swapped = false;

                    // Forward pass (similar to Bubble Sort)
                    for (int i = start; i < end; i++) {
                        if (array[i] > array[i + 1]) {
                            swap(array, i, i + 1);
                            swapped = true;
                        }

                        currentIndex = i + 1;
                        Platform.runLater(() -> {
                            updateBarChart(array);
                            highlightCurrentBar(currentIndex);
                        });

                        Thread.sleep(delayValue);
                    }

                    if (!swapped) {
                        break;
                    }

                    swapped = false;
                    end--;

                    // Backward pass
                    for (int i = end; i > start; i--) {
                        if (array[i] < array[i - 1]) {
                            swap(array, i, i - 1);
                            swapped = true;
                        }

                        currentIndex = i - 1;
                        Platform.runLater(() -> {
                            updateBarChart(array);
                            highlightCurrentBar(currentIndex);
                        });

                        Thread.sleep(delayValue);
                    }

                    start++;
                } while (swapped);

                return null;
            }
        };

        cocktailShakerSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);

            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = 1;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));

                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {
                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);

                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame opacityKeyFrame = new KeyFrame(delayTime, e -> node.setOpacity(targetOpacity));
                timeline.getKeyFrames().add(opacityKeyFrame);
            }

            timeline.play();
            timerThread.interrupt();
        });

        Thread cocktailShakerSortThread = new Thread(cocktailShakerSortTask);
        cocktailShakerSortThread.setDaemon(true);
        cocktailShakerSortThread.start();
    }


    //für insertionSort

    //kleine visuelle fehler immer wenn der delay zu klein ist
    private void insertionSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("INSERTION SORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> insertionSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 1; i < array.length; i++) {
                    int key = array[i];
                    int j = i - 1;
                    while (j >= 0 && array[j] > key) {
                        array[j + 1] = array[j];
                        j--;

                        currentIndex = j + 1;
                        int finalJ = j;
                        Platform.runLater(() -> {
                            swapBars(finalJ + 1, finalJ + 2); // Swap bars visually
                            highlightCurrentBar(currentIndex);
                        });

                        Thread.sleep(delayValue);
                    }
                    array[j + 1] = key;

                    currentIndex = i;
                    Platform.runLater(() -> {
                        updateBarChart(array);
                        highlightCurrentBar(currentIndex);
                    });

                    Thread.sleep(delayValue);
                }
                return null;
            }
        };

        insertionSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);

            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = 1;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));

                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {
                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);

                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame opacityKeyFrame = new KeyFrame(delayTime, e -> node.setOpacity(targetOpacity));
                timeline.getKeyFrames().add(opacityKeyFrame);
            }

            timeline.play();
            timerThread.interrupt();
        });

        Thread insertionSortThread = new Thread(insertionSortTask);
        insertionSortThread.setDaemon(true);
        insertionSortThread.start();
    }

    //für insertionSort
    private void swapBars(int index1, int index2) {
        if (index1 >= 0 && index1 < barChart.getData().get(0).getData().size()
                && index2 >= 0 && index2 < barChart.getData().get(0).getData().size()) {
            XYChart.Data<String, Number> data1 = barChart.getData().get(0).getData().get(index1);
            XYChart.Data<String, Number> data2 = barChart.getData().get(0).getData().get(index2);

            double x1 = data1.getNode().getLayoutX(); // Get the X position of bar 1
            double x2 = data2.getNode().getLayoutX(); // Get the X position of bar 2

            double y1 = data1.getYValue().doubleValue();
            double y2 = data2.getYValue().doubleValue();

            data1.setYValue(y2);
            data2.setYValue(y1);

            // Update the X position of the bars
            data1.getNode().setLayoutX(x2);
            data2.getNode().setLayoutX(x1);
        }
    }





    //für bitonicSort
    private void bitonicSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("BITONIC SORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> bitonicSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                bitonicSort(array, 0, array.length, true);
                return null;
            }
        };

        bitonicSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);




            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = (i + 1) * increment;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));


                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {

                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {

                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);


                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame delayKeyFrame = new KeyFrame(delayTime);
                timeline.getKeyFrames().add(delayKeyFrame);
            }

            timeline.play();


            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
            timeSizeNumberLabel.setText(String.format("%.3fs", durationInSeconds));


            timerThread.interrupt();
        });

        Thread thread = new Thread(bitonicSortTask);
        thread.start();
    }

    //für bitonicSort
    private void bitonicSort(int[] array, int low, int length, boolean ascending) {
        if (length > 1) {
            int mid = length / 2;

            bitonicSort(array, low, mid, !ascending);
            bitonicSort(array, low + mid, length - mid, ascending);
            bitonicMerge(array, low, length, ascending);
        }
    }

    //für bitonicSort
    private void bitonicMerge(int[] array, int low, int length, boolean ascending) {
        if (length > 1) {
            int k = greatestPowerOfTwoLessThan(length);

            for (int i = low; i < low + length - k; i++) {
                compareAndSwap(array, i, i + k, ascending);
            }

            bitonicMerge(array, low, k, ascending);
            bitonicMerge(array, low + k, length - k, ascending);
        }
    }

    //für bitonicSort
    private void compareAndSwap(int[] array, int i, int j, boolean ascending) {
        if (ascending == (array[i] > array[j])) {
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;

            currentIndex = j;
            Platform.runLater(() -> {
                updateBarChart(array);
                highlightCurrentBar(currentIndex);
            });

            try {
                String delayText = delaySizeNumberLabel.getText();
                int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());
                Thread.sleep(delayValue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //für bitonicSort
    private int greatestPowerOfTwoLessThan(int n) {
        int k = 1;
        while (k < n) {
            k = k << 1;
        }
        return k >> 1;
    }


    //für gnomeSort
    private void gnomeSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("GNOME SORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> gnomeSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                int index = 0;
                while (index < array.length) {
                    if (index == 0 || array[index] >= array[index - 1]) {
                        index++;
                    } else {
                        swap(array, index, index - 1);
                        index--;
                    }

                    final int currentIndex = index;
                    Platform.runLater(() -> {
                        updateBarChart(array);
                        highlightCurrentBar(-1);
                    });

                    Thread.sleep(delayValue);
                }
                return null;
            }
        };

        gnomeSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);

            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = 1;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));

                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {
                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);

                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame opacityKeyFrame = new KeyFrame(delayTime, e -> node.setOpacity(targetOpacity));
                timeline.getKeyFrames().add(opacityKeyFrame);
            }

            timeline.play();
            timerThread.interrupt();
        });

        Thread gnomeSortThread = new Thread(gnomeSortTask);
        gnomeSortThread.setDaemon(true);
        gnomeSortThread.start();
    }


    //für bubbleSort
    private void bubbleSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("BUBBLESORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> bubbleSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                boolean isSorted = false;
                int lastUnsortedIndex = array.length - 1;
                while (!isSorted) {
                    isSorted = true;
                    for (int j = 0; j < lastUnsortedIndex; j++) {
                        if (array[j] > array[j + 1]) {

                            int temp = array[j];
                            array[j] = array[j + 1];
                            array[j + 1] = temp;


                            currentIndex = j + 1;
                            Platform.runLater(() -> {
                                updateBarChart(array);
                                highlightCurrentBar(currentIndex);
                            });


                            Thread.sleep(delayValue);

                            isSorted = false;
                        }
                    }
                    lastUnsortedIndex--;
                }
                return null;
            }
        };

        bubbleSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);


            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = (i + 1) * increment;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));


                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {

                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {

                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);


                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame delayKeyFrame = new KeyFrame(delayTime);
                timeline.getKeyFrames().add(delayKeyFrame);
            }

            timeline.play();


            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
            timeSizeNumberLabel.setText(String.format("%.3fs", durationInSeconds));


            timerThread.interrupt();
        });

        Thread thread = new Thread(bubbleSortTask);
        thread.start();
    }

    //für bubbleSort
    public void highlightCurrentBar(int index) {

        for (XYChart.Data<String, Number> data : barChart.getData().get(0).getData()) {
            Node node = data.getNode();
            node.setStyle("-fx-bar-fill: white;");
        }


        if (index != -1) {
            XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(index);
            Node node = data.getNode();
            node.setStyle("-fx-bar-fill: #ff0000;");
        }
    }


    //glaube generell für alle
    public int[] getArrayFromChart() {
        int[] array = new int[barChart.getData().get(0).getData().size()];

        for (int i = 0; i < array.length; i++) {
            array[i] = (int) barChart.getData().get(0).getData().get(i).getYValue();
        }

        return array;
    }


    //für mergeSort
    private void mergeSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("MERGESORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> mergeSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                mergeSort(array, 0, array.length - 1);
                return null;
            }
        };

        mergeSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);


            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = (i + 1) * increment;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));


                int finalI = i; // braucht man wegen lambda
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {

                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);


                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame delayKeyFrame = new KeyFrame(delayTime);
                timeline.getKeyFrames().add(delayKeyFrame);
            }

            timeline.play();


            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
            timeSizeNumberLabel.setText(String.format("%.3fs", durationInSeconds));


            timerThread.interrupt();
        });

        Thread thread = new Thread(mergeSortTask);
        thread.start();
    }

    //für mergeSort
    private void mergeSort(int[] array, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;

            mergeSort(array, left, mid);
            mergeSort(array, mid + 1, right);

            merge(array, left, mid, right);

            Platform.runLater(() -> {
                updateBarChart(array);
                highlightCurrentBar(left, right);
                markBoundaries(array, left, mid, right);
            });

            try {
                String delayText = delaySizeNumberLabel.getText();
                int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());
                Thread.sleep(delayValue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //für mergeSort
    private void merge(int[] array, int left, int mid, int right) {
        int[] temp = new int[right - left + 1];
        int i = left, j = mid + 1, k = 0;

        while (i <= mid && j <= right) {
            if (array[i] <= array[j]) {
                temp[k++] = array[i++];
            } else {
                temp[k++] = array[j++];
            }
        }

        while (i <= mid) {
            temp[k++] = array[i++];
        }

        while (j <= right) {
            temp[k++] = array[j++];
        }

        for (i = left; i <= right; i++) {
            array[i] = temp[i - left];
        }
    }

    //für mergeSort
    private void highlightCurrentBar(int left, int right) {
        for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
            XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
            Node node = data.getNode();
            String style = "-fx-bar-fill: white;";

            if (i >= left && i <= right) {
                style = "-fx-bar-fill: #ff0000;"; // momentane bar rot
            } else if (i == (right + 1)) {
                style = "-fx-bar-fill: #3eff00;"; // grün nächste bar
            }

            node.setStyle(style);
        }
    }

    //für mergeSort
    private void markBoundaries(int[] array, int left, int mid, int right) {
        for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
            XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
            Node node = data.getNode();
            String style = "-fx-bar-fill: white;";

            if (i == left || i == right) {
                style = "-fx-bar-fill: #00ff00;"; // grün für links und rechts bounds
            } else if (i == mid) {
                style = "-fx-bar-fill: #0000ff;"; // blau für die mitte
            }

            node.setStyle(style);
        }
    }


    //für quickSortLRPointers
    private void quickSortLRPointers() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("QUICKSORT (LR pointers)");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> quickSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                quickSortLRPointers(array, 0, array.length - 1);
                return null;
            }
        };

        quickSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);


            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = (i + 1) * increment;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));


                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {

                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {

                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);


                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame delayKeyFrame = new KeyFrame(delayTime);
                timeline.getKeyFrames().add(delayKeyFrame);
            }

            timeline.play();


            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
            timeSizeNumberLabel.setText(String.format("%.3fs", durationInSeconds));


            timerThread.interrupt();
        });

        Thread thread = new Thread(quickSortTask);
        thread.start();
    }

    //für quickSortLRPointers
    private void quickSortLRPointers(int[] array, int left, int right) {
        if (left < right) {
            int pivotIndex = partition(array, left, right);

            Platform.runLater(() -> {
                updateBarChart(array);
                highlightCurrentBar(left, right, pivotIndex);
            });

            try {
                String delayText = delaySizeNumberLabel.getText();
                int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());
                Thread.sleep(delayValue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            quickSortLRPointers(array, left, pivotIndex - 1);
            quickSortLRPointers(array, pivotIndex + 1, right);
        }
    }

    //für quickSortLRPointers
    private int partition(int[] array, int left, int right) {
        int pivot = array[right];
        int i = left - 1;

        for (int j = left; j < right; j++) {
            if (array[j] <= pivot) {
                i++;
                swap(array, i, j);
            }
        }

        swap(array, i + 1, right);
        return i + 1;
    }

    //für quickSortLRPointers
    public void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    //für quickSortLRPointers
    private void highlightCurrentBar(int left, int right, int pivotIndex) {
        for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
            XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
            Node node = data.getNode();
            String style = "-fx-bar-fill: white;";

            if (i == left || i == right) {
                style = "-fx-bar-fill: #00ff00;"; // grün für die anderen pointer
            } else if (i == pivotIndex) {
                style = "-fx-bar-fill: #0000ff;"; // blau für die mitte
            }

            node.setStyle(style);
        }
    }


    //für selectionSort
    private void selectionSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("SELECTION SORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> selectionSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < array.length - 1; i++) {
                    int minIndex = i;

                    for (int j = i + 1; j < array.length; j++) {
                        if (array[j] < array[minIndex]) {
                            minIndex = j;
                        }
                    }

                    if (minIndex != i) {
                        // Swap elements
                        int temp = array[i];
                        array[i] = array[minIndex];
                        array[minIndex] = temp;

                        currentIndex = minIndex;
                        Platform.runLater(() -> {
                            updateBarChart(array);
                            highlightCurrentBar(currentIndex);
                        });

                        Thread.sleep(delayValue);
                    }
                }

                return null;
            }
        };

        selectionSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);

            // Delay and color bars one by one
            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = (i + 1) * increment;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));

                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {
                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {
                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);

                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame delayKeyFrame = new KeyFrame(delayTime);
                timeline.getKeyFrames().add(delayKeyFrame);
            }

            timeline.play();

            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
            timeSizeNumberLabel.setText(String.format("%.3fs", durationInSeconds));

            timerThread.interrupt();
        });

        Thread thread = new Thread(selectionSortTask);
        thread.start();
    }


    //für bogoSort
    private void bogoSort() {
        int[] array = getArrayFromChart();
        currentAlgorithmLabel.setText("BOGOSORT");
        currentAlgorithmLabel.setTextFill(Color.GREEN);
        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);

        String delayText = delaySizeNumberLabel.getText();
        int delayValue = Integer.parseInt(delayText.split("ms")[0].trim());

        Task<Void> bogoSortTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (!isSorted(array)) {

                    shuffleArray(array);


                    Platform.runLater(() -> {
                        updateBarChart(array);
                        highlightRandomBar();
                    });


                    Thread.sleep(delayValue);
                }
                return null;
            }
        };

        bogoSortTask.setOnSucceeded(event -> {
            startButton.setDisable(false);
            sortSelectChoiceBox.setDisable(false);
            arraySizeSlider.setDisable(false);
            delaySizeSlider.setDisable(false);
            randomizeBarsButton.setDisable(false);
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
            currentAlgorithmLabel.setText("NONE");
            currentAlgorithmLabel.setTextFill(Color.RED);
            highlightCurrentBar(-1);


            int duration = 500;
            int delay = 10;

            Timeline timeline = new Timeline();
            double increment = 1.0 / barChart.getData().get(0).getData().size();
            for (int i = 0; i < barChart.getData().get(0).getData().size(); i++) {
                XYChart.Data<String, Number> data = barChart.getData().get(0).getData().get(i);
                Node node = data.getNode();
                double targetOpacity = (i + 1) * increment;
                Duration keyFrameTime = Duration.millis(duration / barChart.getData().get(0).getData().size() * (i + 1));


                int finalI = i;
                KeyFrame colorKeyFrame = new KeyFrame(keyFrameTime, e -> {

                    node.setStyle("-fx-bar-fill: #3eff00;");

                    if (finalI < barChart.getData().get(0).getData().size() - 1) {

                        XYChart.Data<String, Number> nextData = barChart.getData().get(0).getData().get(finalI + 1);
                        Node nextNode = nextData.getNode();
                        nextNode.setStyle("-fx-bar-fill: #ff0000;");
                    }
                });
                timeline.getKeyFrames().add(colorKeyFrame);


                Duration delayTime = keyFrameTime.add(Duration.millis(delay));
                KeyFrame delayKeyFrame = new KeyFrame(delayTime);
                timeline.getKeyFrames().add(delayKeyFrame);
            }

            timeline.play();


            long endTime = System.nanoTime();
            double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
            timeSizeNumberLabel.setText(String.format("%.3fs", durationInSeconds));


            timerThread.interrupt();
        });

        Thread thread = new Thread(bogoSortTask);
        thread.start();
    }

    //für bogoSort
    private void shuffleArray(int[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    //für bogoSort
    public void highlightRandomBar() {
        Random random = new Random();
        int randomIndex = random.nextInt(barChart.getData().get(0).getData().size());
        highlightCurrentBar(randomIndex);
    }


    //init standartwerte und ein teil GUI
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Default stuff
        sortSelectChoiceBox.getItems().setAll(allSorts);
        sortSelectChoiceBox.setValue(allSorts[0]); // default value for choice box

        // Tooltip for Bubble Sort //text block nachschauen und erklären
        Tooltip bubbleSortTooltip = new Tooltip("""
                Bubble Sort Algorithm

                Bubble Sort is a simple comparison-based sorting algorithm.
                It repeatedly steps through the list, compares adjacent elements,
                and swaps them if they are in the wrong order.
                The largest unsorted element 'bubbles' to its correct position in each iteration.
                Bubble Sort has an average time complexity of O(n^2) and is suitable for small-sized arrays.
                """);

        bubbleSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Merge Sort
        Tooltip mergeSortTooltip = new Tooltip("""
                Merge Sort Algorithm

                Merge Sort is a divide-and-conquer algorithm that recursively
                divides the input list into smaller sublists, sorts them,
                and then merges them to obtain a sorted list.
                It uses a temporary array to merge the sublists efficiently.
                Merge Sort has an average time complexity of O(n log n) and is suitable for large-sized arrays.
                """);

        mergeSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Quicksort (LR pointers)
        Tooltip quicksortTooltip = new Tooltip("""
                Quicksort (LR pointers) Algorithm

                Quicksort is a highly efficient divide-and-conquer algorithm.
                It selects a pivot element, partitions the array around the pivot,
                and recursively sorts the subarrays.
                The LR pointers indicate the left and right boundaries of the partitioned subarray.
                Quicksort has an average time complexity of O(n log n) and is suitable for various array sizes.
                """);

        quicksortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Selection Sort
        Tooltip selectionSortTooltip = new Tooltip("""
                Selection Sort Algorithm

                Selection Sort is a simple comparison-based sorting algorithm.
                It repeatedly selects the minimum element from the unsorted portion
                of the array and swaps it into its correct position.
                Selection Sort has an average time complexity of O(n^2) and is suitable for small-sized arrays.
                """);

        selectionSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Gnome Sort
        Tooltip gnomeSortTooltip = new Tooltip("""
                Gnome Sort Algorithm

                Gnome Sort is a comparison-based sorting algorithm inspired by
                the sorting technique used by gnome household figurines.
                It works by repeatedly swapping adjacent elements if they are in the wrong order.
                Gnome Sort has an average time complexity of O(n^2) and is suitable for small-sized arrays.
                """);

        gnomeSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Bitonic Sort
        Tooltip bitonicSortTooltip = new Tooltip("""
                Bitonic Sort Algorithm

                Bitonic Sort is a parallel sorting algorithm based on bitonic sequences.
                It divides the array into bitonic sequences, recursively sorts each half
                in different orders, and then merges the sorted sequences together.
                Bitonic Sort has an average time complexity of O(log^2(n)) and is suitable for large-sized arrays.
                """);

        bitonicSortTooltip.setShowDuration(Duration.INDEFINITE);


        // Tooltip for Cocktail Shaker Sort
        Tooltip cocktailSortTooltip = new Tooltip("""
                Cocktail Shaker Sort Algorithm

                Cocktail Shaker Sort, also known as bidirectional bubble sort,
                is a variation of the bubble sort algorithm.
                It sorts the array in both directions, alternatively moving the
                largest and smallest elements towards their correct positions.
                Cocktail Shaker Sort has an average time complexity of O(n^2) and is suitable for small-sized arrays.
                """);

        cocktailSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Radix Sort (LSD)
        Tooltip radixSortTooltip = new Tooltip("""
                Radix Sort (LSD) Algorithm

                Radix Sort is a non-comparative sorting algorithm that
                sorts integers by grouping them by individual digits.
                It starts sorting from the least significant digit (LSD)
                to the most significant digit (MSD).
                Radix Sort has an average time complexity of O(kn),
                where k is the number of digits in the largest element,
                and it is suitable for sorting integers.
                """);

        radixSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Comb Sort
        Tooltip combSortTooltip = new Tooltip("""
                Comb Sort Algorithm

                Comb Sort is an improvement over Bubble Sort.
                It works by comparing and swapping elements with a specific gap size.
                The gap size starts with the array size and reduces after each iteration
                until it reaches 1.
                Comb Sort has an average time complexity of O(n^2) and is suitable for small-sized arrays.
                """);

        combSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Insertion Sort
        Tooltip insertionSortTooltip = new Tooltip("""
                Insertion Sort Algorithm

                Insertion Sort is a simple sorting algorithm that builds the final
                sorted array one item at a time. It iterates through the array
                and inserts each element into its correct position.
                Insertion Sort has an average time complexity of O(n^2) and is suitable for small-sized arrays.
                """);

        insertionSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Shell Sort
        Tooltip shellSortTooltip = new Tooltip("""
                Shell Sort Algorithm

                Shell Sort is a variation of Insertion Sort that breaks the array
                into smaller subarrays and sorts them individually.
                It starts with a larger gap between elements and reduces the gap
                until it reaches 1.
                Shell Sort has an average time complexity depending on the chosen gap sequence and is suitable for various array sizes.
                """);

        shellSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Bogo Sort
        Tooltip bogoSortTooltip = new Tooltip("""
                Bogo Sort Algorithm

                Bogo Sort, also known as permutation sort or monkey sort,
                is an extremely inefficient and unreliable sorting algorithm.
                It randomly shuffles the array until it happens to be sorted.
                Bogo Sort has an average time complexity of O((n+1)!) and is not suitable for practical use.
                """);

        bogoSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Tooltip for Spread Sort
        Tooltip spreadSortTooltip = new Tooltip("""
                Spread Sort Algorithm

                Spread Sort is a sorting algorithm that partitions the array into subarrays
                and independently sorts them using different sorting algorithms.
                It achieves speed and efficiency by leveraging the strengths of various sorting algorithms.
                """);

        spreadSortTooltip.setShowDuration(Duration.INDEFINITE);

        // Set tooltips for the sorts
        sortSelectChoiceBox.setTooltip(null); // Clear any existing tooltip
        sortSelectChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.equals("Bubble Sort")) {
                    sortSelectChoiceBox.setTooltip(bubbleSortTooltip);
                } else if (newValue.equals("Merge Sort")) {
                    sortSelectChoiceBox.setTooltip(mergeSortTooltip);
                } else if (newValue.equals("Quicksort (LR pointers)")) {
                    sortSelectChoiceBox.setTooltip(quicksortTooltip);
                } else if (newValue.equals("Selection Sort")) {
                    sortSelectChoiceBox.setTooltip(selectionSortTooltip);
                } else if (newValue.equals("Gnome Sort")) {
                    sortSelectChoiceBox.setTooltip(gnomeSortTooltip);
                } else if (newValue.equals("Bitonic Sort")) {
                    sortSelectChoiceBox.setTooltip(bitonicSortTooltip);
                } else if (newValue.equals("Cocktail Shaker Sort")) {
                    sortSelectChoiceBox.setTooltip(cocktailSortTooltip);
                } else if (newValue.equals("Radix Sort (LSD)")) {
                    sortSelectChoiceBox.setTooltip(radixSortTooltip);
                } else if (newValue.equals("Comb Sort")) {
                    sortSelectChoiceBox.setTooltip(combSortTooltip);
                } else if (newValue.equals("Insertion Sort")) {
                    sortSelectChoiceBox.setTooltip(insertionSortTooltip);
                } else if (newValue.equals("Shell Sort")) {
                    sortSelectChoiceBox.setTooltip(shellSortTooltip);
                } else if (newValue.equals("Bogo Sort")) {
                    sortSelectChoiceBox.setTooltip(bogoSortTooltip);
                } else if (newValue.equals("Spread Sort")) {
                    sortSelectChoiceBox.setTooltip(spreadSortTooltip);
                } else {
                    sortSelectChoiceBox.setTooltip(null);
                }
            } else {
                sortSelectChoiceBox.setTooltip(null);
            }
        });


        arraySizeNumberLabel.setText(String.valueOf(MIN_ARRAY_SIZE));
        arraySizeSlider.setMin(MIN_ARRAY_SIZE);
        arraySizeSlider.setMax(MAX_ARRAY_SIZE);

        delaySizeNumberLabel.setText(MIN_DELAY_SIZE + "ms");
        delaySizeSlider.setMin(MIN_DELAY_SIZE);
        delaySizeSlider.setMax(MAX_DELAY_SIZE);

        currentStatusLabel.setText("UNSORTED");
        currentStatusLabel.setTextFill(Color.RED);
        currentStatusLabel.setStyle("-fx-font-family: Consolas");
        currentStatusLabel.setStyle("-fx-font-size: 16");

        currentAlgorithmLabel.setText("NONE");
        currentAlgorithmLabel.setTextFill(Color.RED);
        currentAlgorithmLabel.setStyle("-fx-font-family: Consolas");
        currentAlgorithmLabel.setStyle("-fx-font-size: 16");

        timeSizeNumberLabel.setText("0.000s");
        currentAlgorithmLabel.setStyle("-fx-font-family: Consolas");
        currentAlgorithmLabel.setStyle("-fx-font-size: 16");


        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        xAxis.setOpacity(0);
        yAxis.setOpacity(0);


        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("");

        updateBarChart(generateRandomArray((int) arraySizeSlider.getValue()));

        barChart.setLegendVisible(false);
        barChart.setStyle("-fx-background-color: black;");

        barChart.setAnimated(false);
        barChart.setHorizontalGridLinesVisible(false);
        barChart.setVerticalGridLinesVisible(false);
        barChart.setHorizontalZeroLineVisible(false);
        barChart.setVerticalZeroLineVisible(false);
        barChart.setStyle("-fx-background-color: black");
        barChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");


        for (Node node : barChart.lookupAll(".default-color0.chart-bar")) {
            node.setStyle("-fx-bar-fill: white;");
        }

        delaySizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            delaySizeNumberLabel.setText(String.format("%.0fms", newValue));
        });

        arraySizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int newSize = newValue.intValue();


            arraySizeNumberLabel.setText(String.format("%d", newSize));


            int[] array = generateRandomArray(newSize);


            updateBarChart(array);


            for (Node node : barChart.lookupAll(".default-color0.chart-bar")) {
                node.setStyle("-fx-bar-fill: white;");
            }
        });
    }

    // shuffled die bars
    private int[] generateRandomArray(int size) {
        int[] array = new int[size];
        List<Integer> uniqueValues = new ArrayList<>();
        Random random = new Random();


        for (int i = 1; i <= size; i++) {
            uniqueValues.add(i);
        }


        Collections.shuffle(uniqueValues);


        for (int i = 0; i < size; i++) {
            array[i] = uniqueValues.get(i);
        }


        for (int i = size - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }

        return array;
    }

    public void updateBarChart(int[] array) {
        ObservableList<BarChart.Data<String, Number>> barChartData = FXCollections.observableArrayList();


        for (int i = 0; i < array.length; i++) {
            BarChart.Data<String, Number> data = new BarChart.Data<>(String.valueOf(i), array[i]);
            barChartData.add(data);
        }


        barChart.getData().clear();
        barChart.getData().add(new BarChart.Series<>(barChartData));


        if (isSorted(array)) {
            currentStatusLabel.setText("SORTED");
            currentStatusLabel.setTextFill(Color.GREEN);
        } else {
            currentStatusLabel.setText("UNSORTED");
            currentStatusLabel.setTextFill(Color.RED);
        }


        borderPane.setCenter(barChart);
    }

    private boolean isSorted(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] > array[i + 1]) {
                return false;
            }
        }
        return true;
    }

    public Thread getTimerThread() {
        return timerThread;
    }


    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setTimerThread(Thread timerThread) {
        this.timerThread = timerThread;
    }

    public BorderPane getBorderPane() {
        return borderPane;
    }

    public void setBorderPane(BorderPane borderPane) {
        this.borderPane = borderPane;
    }

    public ToolBar getToolBarForAllItems() {
        return toolBarForAllItems;
    }

    public void setToolBarForAllItems(ToolBar toolBarForAllItems) {
        this.toolBarForAllItems = toolBarForAllItems;
    }

    public ChoiceBox<String> getSortSelectChoiceBox() {
        return sortSelectChoiceBox;
    }

    public void setSortSelectChoiceBox(ChoiceBox<String> sortSelectChoiceBox) {
        this.sortSelectChoiceBox = sortSelectChoiceBox;
    }

    public Label getArraySizeLabel() {
        return arraySizeLabel;
    }

    public void setArraySizeLabel(Label arraySizeLabel) {
        this.arraySizeLabel = arraySizeLabel;
    }

    public Slider getArraySizeSlider() {
        return arraySizeSlider;
    }

    public void setArraySizeSlider(Slider arraySizeSlider) {
        this.arraySizeSlider = arraySizeSlider;
    }

    public Label getArraySizeNumberLabel() {
        return arraySizeNumberLabel;
    }

    public void setArraySizeNumberLabel(Label arraySizeNumberLabel) {
        this.arraySizeNumberLabel = arraySizeNumberLabel;
    }

    public Label getDelaySizeLabel() {
        return delaySizeLabel;
    }

    public void setDelaySizeLabel(Label delaySizeLabel) {
        this.delaySizeLabel = delaySizeLabel;
    }

    public Slider getDelaySizeSlider() {
        return delaySizeSlider;
    }

    public void setDelaySizeSlider(Slider delaySizeSlider) {
        this.delaySizeSlider = delaySizeSlider;
    }

    public Label getDelaySizeNumberLabel() {
        return delaySizeNumberLabel;
    }

    public void setDelaySizeNumberLabel(Label delaySizeNumberLabel) {
        this.delaySizeNumberLabel = delaySizeNumberLabel;
    }

    public Label getStatusPromptLabel() {
        return statusPromptLabel;
    }

    public void setStatusPromptLabel(Label statusPromptLabel) {
        this.statusPromptLabel = statusPromptLabel;
    }

    public Label getCurrentStatusLabel() {
        return currentStatusLabel;
    }

    public void setCurrentStatusLabel(Label currentStatusLabel) {
        this.currentStatusLabel = currentStatusLabel;
    }

    public Label getAlgorithmPromptLabel() {
        return algorithmPromptLabel;
    }

    public void setAlgorithmPromptLabel(Label algorithmPromptLabel) {
        this.algorithmPromptLabel = algorithmPromptLabel;
    }

    public Label getCurrentAlgorithmLabel() {
        return currentAlgorithmLabel;
    }

    public void setCurrentAlgorithmLabel(Label currentAlgorithmLabel) {
        this.currentAlgorithmLabel = currentAlgorithmLabel;
    }

    public BarChart<String, Number> getBarChart() {
        return barChart;
    }

    public void setBarChart(BarChart<String, Number> barChart) {
        this.barChart = barChart;
    }

    public Button getStartButton() {
        return startButton;
    }

    public void setStartButton(Button startButton) {
        this.startButton = startButton;
    }

    public Button getRandomizeBarsButton() {
        return randomizeBarsButton;
    }

    public void setRandomizeBarsButton(Button randomizeBarsButton) {
        this.randomizeBarsButton = randomizeBarsButton;
    }

    public Button getExitButton() {
        return exitButton;
    }

    public void setExitButton(Button exitButton) {
        this.exitButton = exitButton;
    }

    public Label getTimePromptLabel() {
        return timePromptLabel;
    }

    public void setTimePromptLabel(Label timePromptLabel) {
        this.timePromptLabel = timePromptLabel;
    }

    public Label getTimeSizeNumberLabel() {
        return timeSizeNumberLabel;
    }

    public void setTimeSizeNumberLabel(Label timeSizeNumberLabel) {
        this.timeSizeNumberLabel = timeSizeNumberLabel;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }





}
