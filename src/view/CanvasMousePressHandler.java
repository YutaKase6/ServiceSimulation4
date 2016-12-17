package view;

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import model.Actor;

import java.util.List;
import java.util.Optional;

import static util.Const.ACTOR_CIRCLE_SIZE;

/**
 * Created by yutakase on 2016/12/04.
 */
public class CanvasMousePressHandler implements EventHandler<MouseEvent> {

    private static List<Actor> actors;

    @Override
    public void handle(MouseEvent event) {
        double x = event.getX() / CanvasDrawer.currentCanvasRate;
        double y = event.getY() / CanvasDrawer.currentCanvasRate;

        Optional.ofNullable(actors).ifPresent(actors -> actors.stream()
                .filter(actor -> isInOval(x, y, actor.getPos()[0], actor.getPos()[1], ACTOR_CIRCLE_SIZE))
                .forEach(this::onActorPressed));
    }

    private void onActorPressed(Actor actor) {
        CanvasDrawer.focusRelatedActorId(actor.getId());

        // 再描画
        CanvasDrawer.reDraw();
        System.out.println(actor.toString());

    }

    private boolean isInOval(double x, double y, double ovalX, double ovalY, double ovalR) {
        return (ovalX - x) * (ovalX - x) + (ovalY - y) * (ovalY - y) <= ovalR * ovalR;
    }

    public static void setActors(List<Actor> actors) {
        CanvasMousePressHandler.actors = actors;
    }
}
