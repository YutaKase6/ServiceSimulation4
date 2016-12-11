package view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import model.Actor;

import java.util.List;
import java.util.Optional;

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
            try {
                int id = Integer.parseInt(textField.getText());
                CanvasDrawer.changeFocusActorId(id);
            } catch (NumberFormatException e) {
                if (textField.getText().equals("reset")) {
                    CanvasDrawer.clearFocusActorIdList();
                    CanvasDrawer.clearText();
                }
            }
            textField.setText("");
            CanvasDrawer.reDraw();
        });
    }

    public static void setActors(List<Actor> actors) {
        TextFieldOnActionHandler.actors = actors;
    }
}
