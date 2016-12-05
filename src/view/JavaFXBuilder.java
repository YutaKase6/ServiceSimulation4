package view;

import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;

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

        List<Canvas> canvases = Stream
                .generate(() -> buildCanvas(0, 0, CANVAS_SIZE, CANVAS_SIZE))
                .limit(SERVICE_COUNT + 1)
                .collect(Collectors.toList());

        // 描画クラスにcanvasを登録
        CanvasDrawer.setDrawActorsTabCanvases(canvases);

        TabPane tabPane = buildDrawActorsTabPane(canvases);
        root.getChildren().add(tabPane);

        return root;
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

    private static Canvas buildCanvas(int x, int y, int w, int h) {
        Canvas canvas = new Canvas(w, h);
        canvas.setLayoutX(x);
        canvas.setLayoutY(y);
        return canvas;
    }
}

