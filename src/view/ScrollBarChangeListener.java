package view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import model.Actor;
import util.ActorUtil;

import java.util.List;
import java.util.stream.IntStream;

import static util.Const.SERVICE_COUNT;

/**
 * Created by yutakase on 2016/12/18.
 */
public class ScrollBarChangeListener implements ChangeListener<Number> {

    private static List<List<Actor>> actorLogList;

    @Override
    public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        int val = (int) Math.round((Double) newValue);
        List<Actor> actorLogs = actorLogList.get(val);
        IntStream.range(0, SERVICE_COUNT + 1).forEach(i -> CanvasDrawer.drawActorsAndNetwork(actorLogs, i));
        ActorUtil.setActors(actorLogs);
    }

    public static void setActorLogList(List<List<Actor>> actorLogList) {
        ScrollBarChangeListener.actorLogList = actorLogList;
    }

}
