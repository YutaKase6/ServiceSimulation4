package view;

import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import model.Actor;

import java.util.List;
import java.util.Optional;

import static util.Const.*;

/**
 * Created by yutakase on 2016/12/04.
 */
class CanvasMousePressHandler implements EventHandler<MouseEvent> {

    private static List<Actor> actors;

    @Override
    public void handle(MouseEvent event) {
        double x = event.getX() / CANVAS_RATE;
        double y = event.getY() / CANVAS_RATE;

        Optional.ofNullable(actors).ifPresent(actors -> actors.stream()
                .filter(actor -> isInOval(x, y, actor.getPos()[0], actor.getPos()[1], ACTOR_CIRCLE_SIZE))
                .forEach(this::onActorPressed));
    }

    private void onActorPressed(Actor actor) {
        Alert alert = JavaFXBuilder.buildActorInfoAlert(actor);
        alert.show();

        CanvasDrawer.changeFocusActorId(actor.getId());

        // 再描画
        CanvasDrawer.reDraw();
        System.out.println(actor.toString());

    }

    private boolean isInOval(double x, double y, double ovalX, double ovalY, double ovalR) {
        return (ovalX - x) * (ovalX - x) + (ovalY - y) * (ovalY - y) <= ovalR * ovalR;
    }

    public static void setActors(List<Actor> _actors) {
        actors = _actors;
    }
}
