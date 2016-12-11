package view;

import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.Text;
import model.Actor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static util.Const.*;

/**
 * JavaFxコンポーネントを生成するstaticクラス
 * <p>
 * Created by yutakase on 2016/12/03.
 */
public final class JavaFXBuilder {
    private JavaFXBuilder() {
    }

    public static ChoiceDialog<String> buildModeChoiceDialog() {
        List<String> choices = Arrays.asList(SIMULATION_TEXT, LOAD_FILE_TEXT);
        ChoiceDialog<String> choiceDialog = new ChoiceDialog<>(choices.get(1), choices);
        choiceDialog.setHeaderText("select");
        return choiceDialog;
    }

    public static TextInputDialog buildFileNameInputDialog(String headerText) {
        TextInputDialog textInputDialog = new TextInputDialog(".dat");
        textInputDialog.setHeaderText(headerText);
        textInputDialog.setContentText("Input file name");
        return textInputDialog;
    }

    public static Group buildRootGroup() {
        Group root = new Group();

        CanvasMousePressHandler handler = new CanvasMousePressHandler();
        List<Canvas> canvases = Stream
                .generate(() -> buildDrawActorCanvas(0, 0, CANVAS_SIZE, CANVAS_SIZE, handler))
                .limit(SERVICE_COUNT + 1)
                .collect(Collectors.toList());

        // 描画クラスにcanvasを登録
        CanvasDrawer.setDrawActorsTabCanvases(canvases);


        TabPane tabPane = buildDrawActorsTabPane(canvases);
        root.getChildren().add(tabPane);

        ScrollPane scrollPrintTextPane = new ScrollPane();
        scrollPrintTextPane.setLayoutX(CANVAS_SIZE);
        scrollPrintTextPane.setLayoutY(0);
        scrollPrintTextPane.setMaxSize(CANVAS_SIZE, CANVAS_SIZE);
        Text printTextPane = new Text();
        scrollPrintTextPane.setContent(printTextPane);
        CanvasDrawer.setPrintTextPane(printTextPane);
        root.getChildren().add(scrollPrintTextPane);

        FlowPane configFlowPane = buildConfigFlowPane();
        root.getChildren().add(configFlowPane);

        if (SHOW_PRICE_LINE_CHART) {
            List<LineChart<Number, Number>> lineCharts = Stream.generate(() -> buildLineChart(0, 0, CANVAS_SIZE * 2, CANVAS_SIZE)).limit(SERVICE_COUNT + 1).collect(Collectors.toList());
            CanvasDrawer.setPriceLineCharts(lineCharts);
            TabPane lineChartTabPane = buildLineChartTabPane(lineCharts);
            root.getChildren().add(lineChartTabPane);
        }

        return root;
    }

    private static FlowPane buildConfigFlowPane() {
        FlowPane flowPane = new FlowPane(Orientation.VERTICAL);
        flowPane.setLayoutX(0);
        flowPane.setLayoutY(CANVAS_SIZE + 40);

        TextField textField = new TextField();
        textField.setOnAction(new TextFieldOnActionHandler(textField));
        flowPane.getChildren().add(textField);

        BorderPane sliderBorderPane = buildSliderBorderPane();
        flowPane.getChildren().add(sliderBorderPane);

        return flowPane;
    }

    private static TabPane buildDrawActorsTabPane(List<Canvas> canvases) {
        TabPane tabPane = new TabPane();
        for (int i = 0; i < canvases.size(); i++) {
            Tab tab = buildDrawActorsTab(i, canvases.get(i));
            tabPane.getTabs().add(tab);
        }
        tabPane.getSelectionModel().select(ALL_SERVICES_ID);
        return tabPane;
    }

    private static Tab buildDrawActorsTab(int tabId, Canvas canvas) {
        Tab tab = new Tab();
        tab.closableProperty().set(false);
        tab.setId(String.valueOf(tabId));

        String tabText = (tabId == ALL_SERVICES_ID) ? "All Service" : "Service:" + tabId;
        tab.setText(tabText);

        tab.setContent(canvas);

        return tab;
    }

    private static Canvas buildDrawActorCanvas(int x, int y, int w, int h, EventHandler<MouseEvent> handler) {
        Canvas canvas = new Canvas(w, h);
        canvas.setLayoutX(x);
        canvas.setLayoutY(y);
        canvas.setOnMousePressed(handler);
        return canvas;
    }

    private static TabPane buildLineChartTabPane(List<LineChart<Number, Number>> lineCharts) {
        TabPane tabPane = new TabPane();
        tabPane.setLayoutX(CANVAS_SIZE);
        for (int i = 0; i < SERVICE_COUNT + 1; i++) {
            Tab tab = new Tab();
            tab.closableProperty().set(false);
            tab.setId(String.valueOf(i));

            String tabText = (i == ALL_SERVICES_ID) ? "All Service" : "Service:" + i;
            tab.setText(tabText);

            tab.setContent(lineCharts.get(i));
            tabPane.getTabs().add(tab);
        }
        tabPane.getSelectionModel().select(ALL_SERVICES_ID);
        return tabPane;
    }

    private static BorderPane buildSliderBorderPane() {
        BorderPane borderPane = new BorderPane();
        Slider redSlider = buildSlider(0);
        Slider greenSlider = buildSlider(1);
        Slider blueSlider = buildSlider(2);
        borderPane.setTop(redSlider);
        borderPane.setCenter(greenSlider);
        borderPane.setBottom(blueSlider);
        return borderPane;
    }

    private static Slider buildSlider(int id) {
        Slider slider = new Slider(0, 1, 1);
        slider.setSnapToTicks(false);
        slider.setId(String.valueOf(id));
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            CanvasDrawer.setOpacity(id, (Double) newValue);
            CanvasDrawer.reDraw();
        });
        return slider;
    }

    private static LineChart<Number, Number> buildLineChart(int x, int y, int w, int h) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLayoutX(x);
        lineChart.setLayoutY(y);
        lineChart.setMaxWidth(w);
        lineChart.setMaxHeight(h);
        lineChart.setCreateSymbols(false);
        return lineChart;
    }

}

