package view;

import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import model.Actor;

import java.util.List;

import static util.Const.*;

/**
 * Created by yutakase on 2016/12/04.
 */
class CanvasMousePressHandler implements EventHandler<MouseEvent> {

    private List<Actor> actors;

    @Override
    public void handle(MouseEvent event) {
        double x = event.getX() / CANVAS_RATE;
        double y = event.getY() / CANVAS_RATE;

        actors.stream()
                .filter(actor -> isInOval(x, y, actor.getPos()[0], actor.getPos()[1], ACTOR_CIRCLE_SIZE))
                .forEach(actor -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText("");
                    alert.setContentText(actor.toString());
                    alert.show();

                    CanvasDrawer.changeFocusActorId(actor.getId());

                    // 再描画
                    for (int i = 0; i < SERVICE_COUNT + 1; i++) {
                        CanvasDrawer.drawActorsAndNetwork(this.actors, i);
                    }
                    System.out.println(actor.toString());
                });
    }

    private boolean isInOval(double x, double y, double ovalX, double ovalY, double ovalR) {
        return (ovalX - x) * (ovalX - x) + (ovalY - y) * (ovalY - y) <= ovalR * ovalR;
    }

    public void setActors(List<Actor> actors) {
        this.actors = actors;
    }
}
