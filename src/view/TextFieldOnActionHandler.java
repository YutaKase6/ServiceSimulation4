package view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import model.Actor;

import java.util.List;
import java.util.Optional;

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
            if (textField.getText().equals("reset")) {
                CanvasDrawer.clearFocusActorIdList();
            } else {
                try {
                    int id = Integer.parseInt(textField.getText());
                    Alert alert = JavaFXBuilder.buildActorInfoAlert(actors.get(id));
                    alert.show();
                    CanvasDrawer.changeFocusActorId(id);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            textField.setText("");
            CanvasDrawer.reDraw();
        });
    }

    public static void setActors(List<Actor> _actors) {
        actors = _actors;
    }
}
