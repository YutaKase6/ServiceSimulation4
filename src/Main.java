import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import model.Actor;
import simulation.ServiceSimulation;
import util.ActorUtil;
import util.FileIO;
import view.CanvasDrawer;
import view.JavaFXBuilder;
import view.ScrollBarChangeListener;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static util.Const.*;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // モード選択ダイアログ生成(Simulation or File load)
        ChoiceDialog<String> modeChoiceDialog = JavaFXBuilder.buildModeChoiceDialog();
        // ダイアログ表示、選択されたモードをStringとして取得
        Optional<String> choiceStrOptional = modeChoiceDialog.showAndWait();

        choiceStrOptional.ifPresent(choiceStr -> {
            // ファイル名入力ダイアログ表示
            TextInputDialog fileNameInputDialog = JavaFXBuilder.buildFileNameInputDialog(choiceStr);
            // ダイアログ表示、入力されたファイル名を取得
            Optional<String> fileNameStrOptional = fileNameInputDialog.showAndWait();

            fileNameStrOptional.ifPresent(fileNameStr -> {
                // 入力された内容でスタート
                if (choiceStr.equals(SIMULATION_TEXT)) {
                    // Simulation
                    this.startSimulation(fileNameStr);
                } else if (choiceStr.equals(LOAD_FILE_TEXT)) {
                    // file load
                    this.startFileLoad(fileNameStr, primaryStage);
                }
            });
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void startSimulation(String fileName) {
        ServiceSimulation serviceSimulation = new ServiceSimulation(fileName);
        serviceSimulation.mainLoop();
    }

    private void startFileLoad(String fileNameStr, Stage primaryStage) {
        // 表示
        Group root = JavaFXBuilder.buildRootGroup();
        primaryStage.setTitle(fileNameStr);
        primaryStage.setMaxWidth(SCREEN_WIDTH);
        primaryStage.setMaxHeight(SCREEN_HEIGHT);
        primaryStage.setScene(new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT));
        primaryStage.show();

        // Load file
        Optional<List<List<Actor>>> logOptional = FileIO.loadAgentLog(fileNameStr);
        logOptional.ifPresent(logList -> {
            ScrollBarChangeListener.setActorLogList(logList);

            List<Actor> log = logList.get(0);
            ActorUtil.setActors(log);
            IntStream.range(0, SERVICE_COUNT + 1).forEach(i -> CanvasDrawer.drawActorsAndNetwork(log, i));
            log.forEach(actor -> System.out.println(actor.toString()));
        });

        Optional<List<List<List<Integer>>>> priceLogOptional = FileIO.loadPriceLog("price_" + fileNameStr);
        priceLogOptional.ifPresent(CanvasDrawer::drawPriceLineChart);
    }
}
