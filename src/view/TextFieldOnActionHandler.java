package view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import model.Actor;
import util.ActorUtil;
import util.CalcUtil;
import util.StringUtil;

import java.util.List;
import java.util.Optional;

import static util.Const.CANVAS_SIZE;

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
                if (inputStr.length == 3) {
                    // 利得詳細表示
                    printProfit(inputStr);
                } else if (inputStr[0].equals("reset")) {
                    CanvasDrawer.clearFocusActorIdList();
                    CanvasDrawer.clearText();
                    CanvasDrawer.setCanvasSize(CANVAS_SIZE);
                } else if (inputStr[0].equals("zoom")) {
                    double rate = Double.parseDouble(inputStr[1]);
                    CanvasDrawer.setCanvasSize((int) (CANVAS_SIZE * rate));
                } else if (inputStr[0].equals("price")) {
                    CanvasDrawer.changeShowPriceCircle();
                } else if (inputStr[0].equals("capability")) {
                    CanvasDrawer.changeShowCapability();
                } else if (inputStr[0].equals("print")) {
                    if (inputStr.length == 2) {
                        CanvasDrawer.printText(actors.get(Integer.parseInt(inputStr[1])).toString());
                    } else {
                        CanvasDrawer.clearText();
                    }
                } else {
                    int id = Integer.parseInt(inputStr[0]);
                    CanvasDrawer.focusRelatedActorId(id);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            textField.setText("");
            CanvasDrawer.reDraw();
        });
    }

    private static void printProfit(String[] inputStr) {
        int id1 = Integer.parseInt(inputStr[0]);
        int id2 = Integer.parseInt(inputStr[1]);
        int serviceId = Integer.parseInt(inputStr[2]);
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
