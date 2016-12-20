package view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import model.Actor;
import util.ActorUtil;
import util.CalcUtil;
import util.StringUtil;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static util.Const.ACTOR_COUNT;
import static util.Const.CANVAS_SIZE;
import static util.Const.SERVICE_COUNT;

/**
 * Created by yutakase on 2016/12/06.
 */
public class TextFieldOnActionHandler implements EventHandler<ActionEvent> {

    private static List<Actor> actors;
    private TextField textField;

    public TextFieldOnActionHandler(TextField textField) {
        this.textField = textField;
    }

    @Override
    public void handle(ActionEvent event) {
        Optional.ofNullable(actors).ifPresent(actors -> {
            String[] inputStr = textField.getText().split(" ");
            try {
                switch (inputStr[0]) {
                    case "profit":
                        // 利得詳細表示
                        printProfit(inputStr);
                        break;
                    case "reset":
                        CanvasDrawer.clearFocusActorIdList();
                        CanvasDrawer.clearText();
                        CanvasDrawer.setCanvasSize(CANVAS_SIZE);
                        break;
                    case "zoom":
                        double rate = Double.parseDouble(inputStr[1]);
                        CanvasDrawer.setCanvasSize((int) (CANVAS_SIZE * rate));
                        break;
                    case "price":
                        CanvasDrawer.changeShowPriceCircle();
                        break;
                    case "capability":
                        CanvasDrawer.changeShowCapability();
                        break;
                    case "print":
                        if (inputStr.length == 2) {
                            CanvasDrawer.printText(actors.get(Integer.parseInt(inputStr[1])).toString());
                        } else {
                            CanvasDrawer.clearText();
                        }
                        break;
                    case "path":
                        if (inputStr.length == 2) {
                            IntStream.range(0, ACTOR_COUNT).forEach(actorId -> {
                                List<List<Integer>> pathList = ActorUtil.exploreEcosystem(actors.get(actorId), Integer.parseInt(inputStr[1]));
                                System.out.println(actorId);
                                if (pathList.size() != 0) {
                                    CanvasDrawer.printText(pathList + "\n");
                                }
                            });
                        } else {
                            List<List<Integer>> pathList = ActorUtil.exploreEcosystem(actors.get(Integer.parseInt(inputStr[1])), Integer.parseInt(inputStr[2]));
                            CanvasDrawer.printText(pathList.toString() + "\n");
                        }
                        break;
                    default:
                        int id = Integer.parseInt(inputStr[0]);
                        CanvasDrawer.focusRelatedActorId(id);
                        break;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            textField.setText("");
            CanvasDrawer.reDraw();
        });
    }

    private static void printProfit(String[] inputStr) {
        int id1 = Integer.parseInt(inputStr[1]);
        int id2 = Integer.parseInt(inputStr[2]);
        int serviceId = Integer.parseInt(inputStr[3]);
        Actor provider = actors.get(id1);
        Actor consumer = actors.get(id2);
        double value = ActorUtil.calcValue(provider, consumer, serviceId);
        double dist = CalcUtil.calcDist(provider.getPos(), consumer.getPos());
        double price = provider.getPrice(serviceId);
        double profit = ActorUtil.calcProfit(provider, consumer, serviceId);
        String sb = "provider:" + id1 + ", " + "consumer: " + id2 + "\n" +
                "value: " + StringUtil.formatTo1f(value) + ", " + "dist: " + StringUtil.formatTo1f(dist) + ", " + "price: " + price + "\n" +
                "profit: " + StringUtil.formatTo1f(profit) + "\n";
        CanvasDrawer.printText(sb);
    }

    public static void setActors(List<Actor> actors) {
        TextFieldOnActionHandler.actors = actors;
    }
}
