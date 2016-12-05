package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import model.Actor;
import util.CalcUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static util.Const.*;

/**
 * Created by yutakase on 2016/12/03.
 */
public final class CanvasDrawer {

    // Actor描画のためのGraphicContext
    private static Optional<List<GraphicsContext>> drawActorsTabGCOptional = Optional.empty();

    // フォーカスされているActorのリスト
    private static List<Integer> focusActorIdList = new LinkedList<>();

    private static CanvasMousePressHandler canvasMousePressHandler = new CanvasMousePressHandler();

    // 色
    private static List<Color> focusColors = Arrays.asList(
            new Color(1, 0, 0, 1),
            new Color(0, 1, 0, 1),
            new Color(0, 0, 1, 1));
    private static List<Color> noFocusColors = Arrays.asList(
            new Color(1, 0, 0, NO_FOCUS_COLOR_OPACITY),
            new Color(0, 1, 0, NO_FOCUS_COLOR_OPACITY),
            new Color(0, 0, 1, NO_FOCUS_COLOR_OPACITY));
    private static Color focusBlack = new Color(0, 0, 0, 1);
    private static Color noFocusBlack = new Color(0, 0, 0, NO_FOCUS_COLOR_OPACITY);

    private CanvasDrawer() {
    }

    /**
     * Canvasを登録
     *
     * @param canvases 登録するCanvasのリスト
     */
    public static void setDrawActorsTabCanvases(List<Canvas> canvases) {
        // CanvasにListenerを登録
        canvases.forEach(canvas -> canvas.setOnMousePressed(canvasMousePressHandler));
        // canvasのリストからGraphicContextのリストを生成
        List<GraphicsContext> graphicsContextList = canvases.stream()
                .map(Canvas::getGraphicsContext2D)
                .collect(Collectors.toList());

        drawActorsTabGCOptional = Optional.of(graphicsContextList);
    }

    /**
     * serviceIdのサービスに関してactorとnetworkを描画
     *
     * @param actors    Actorのリスト
     * @param serviceId サービスのID
     */
    public static void drawActorsAndNetwork(List<Actor> actors, int serviceId) {
        drawActorsTabGCOptional.ifPresent(gcList -> {
            GraphicsContext gc = gcList.get(serviceId);
            // Canvas clear
            gc.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
            // actor描画
            actors.forEach(actor -> drawActors(gc, actor, serviceId));
            // ネットワーク描画
            drawNetwork(gc, actors, serviceId);

            // Listenerのactorsを更新
            canvasMousePressHandler.setActors(actors);
        });
    }

    /**
     * Actorのフォーカス状態を変更
     *
     * @param actorId 変更したいActorのID
     */
    public static void changeFocusActorId(int actorId) {
        if (focusActorIdList.contains(actorId)) {
            // フォーカスされていたら解除
            focusActorIdList.remove(Integer.valueOf(actorId));
        } else {
            // フォーカスされていなければ登録
            focusActorIdList.add(actorId);
        }
    }

    /**
     * idのActorがフォーカスされているか判定
     *
     * @param ActorId 判定したいActorのID
     * @return フォーカスされていればT
     */
    private static boolean isFocus(int ActorId) {
        return focusActorIdList.size() == 0 || focusActorIdList.contains(ActorId);
    }

    /**
     * Actorを描画
     */
    private static void drawActors(GraphicsContext gc, Actor actor, int serviceId) {
        // フォーカスされているActorか判定
        boolean isFocus = isFocus(actor.getId());
        // actor描画
        int[] pos = actor.getPos();
        double left = (pos[0] - (ACTOR_CIRCLE_SIZE / 2)) * CANVAS_RATE;
        double top = (pos[1] - (ACTOR_CIRCLE_SIZE / 2)) * CANVAS_RATE;
        Color color = isFocus ? focusBlack : noFocusBlack;
        gc.setStroke(color);
        drawTorusOval(gc, left, top, ACTOR_CIRCLE_SIZE * CANVAS_RATE);

        // 供給Actorは色付きで描画
        if (serviceId != ALL_SERVICES_ID) {
            if (actor.getConsumerActorIdList(serviceId).size() > 0) {
                color = isFocus ? focusColors.get(serviceId) : noFocusColors.get(serviceId);
                gc.setStroke(color);
                drawTorusOval(gc, left, top, ACTOR_CIRCLE_SIZE * CANVAS_RATE);
            }
        }

        // ID描画
        color = isFocus ? Color.BLACK : noFocusBlack;
        gc.setStroke(color);
        gc.strokeText("" + actor.getId(), left, top);
    }

    /**
     * サービス交換ネットワークを描画
     */
    private static void drawNetwork(GraphicsContext gc, List<Actor> actors, int serviceId) {
        if (serviceId == ALL_SERVICES_ID) {
            // すべてのサービスのネットワークを描画
            actors.forEach(actor -> IntStream
                    .range(0, SERVICE_COUNT)
                    .forEach(i -> drawArrows(gc, actor, actors, i)));
        } else {
            actors.forEach(actor -> drawArrows(gc, actor, actors, serviceId));
        }
    }

    /**
     * hostActorからすべての供給先Actorへの矢印を描画
     *
     * @param gc        GraphicContext
     * @param hostActor ネットワークを描画するActor
     * @param actors    全Actorリスト
     * @param serviceId サービスのID
     */
    private static void drawArrows(GraphicsContext gc, Actor hostActor, List<Actor> actors, int serviceId) {
        Color color = isFocus(hostActor.getId()) ? focusColors.get(serviceId) : noFocusColors.get(serviceId);
        gc.setStroke(color);
        hostActor.getConsumerActorIdList(serviceId).forEach(consumerId -> drawArrow(gc, hostActor, actors.get(consumerId)));
    }

    /**
     * トーラス空間を考慮した円を描画
     *
     * @param g    GraphicContext
     * @param left x座標
     * @param top  y座標
     * @param size 大きさ(直径)
     */
    private static void drawTorusOval(GraphicsContext g, double left, double top, double size) {
        // トーラス再現用のもう一つの座標
        Double torusX = null;
        Double torusY = null;

        // 座標計算
        if (left < 0) {
            torusX = left + CANVAS_SIZE;
        } else if (left + size > CANVAS_SIZE) {
            torusX = left - CANVAS_SIZE;
        }
        if (top < 0) {
            torusY = top + CANVAS_SIZE;
        } else if (top + size > CANVAS_SIZE) {
            torusY = top - CANVAS_SIZE;
        }

        // 描画

        g.strokeOval(left, top, size, size);

        if (torusX != null) {
            g.strokeOval(torusX, top, size, size);
        }
        if (torusY != null) {
            g.strokeOval(left, torusY, size, size);
        }
        if (torusX != null && torusY != null) {
            g.strokeOval(torusX, torusY, size, size);
        }
    }

    /**
     * srcActorからdstActorへ矢印を描画
     */
    private static void drawArrow(GraphicsContext gc, Actor srcActor, Actor dstActor) {
        int[] srcActorPos = srcActor.getPos();
        double[] srcActorPosDouble = {srcActorPos[0], srcActorPos[1]};

        int[] dstActorPos = dstActor.getPos();
        double[] dstActorPosDouble = {dstActorPos[0], dstActorPos[1]};
        double[] distVector = CalcUtil.calcDistVector(srcActorPos, dstActorPos);
        double[] arrowDstPos = {srcActorPos[0] + distVector[0], srcActorPos[1] + distVector[1]};

        drawArrow(gc, srcActorPosDouble, arrowDstPos);

        // トーラス処理
        // 矢印が画面外の場合、逆からもう一本矢印を描画
        if ((arrowDstPos[0] < 0 || FIELD_SIZE < arrowDstPos[0]) || (arrowDstPos[1] < 0 || FIELD_SIZE < arrowDstPos[1])) {
            double[] arrowSrcPos = {dstActorPos[0] - distVector[0], dstActorPos[1] - distVector[1]};
            drawArrow(gc, arrowSrcPos, dstActorPosDouble);
        }
    }

    /**
     * src座標からdst座標へ矢印を描画
     */
    private static void drawArrow(GraphicsContext gc, double[] srcPos, double[] dstPos) {
        final double w = 1;
        final double h = w * 1.5;

        double vx = dstPos[0] - srcPos[0];
        double vy = dstPos[1] - srcPos[1];
        double v = Math.sqrt(vx * vx + vy * vy);
        double ux = vx / v;
        double uy = vy / v;

        double[] leftPos = new double[2];
        double[] rightPos = new double[2];
        leftPos[0] = dstPos[0] - uy * w - ux * h;
        leftPos[1] = dstPos[1] + ux * w - uy * h;
        rightPos[0] = dstPos[0] + uy * w - ux * h;
        rightPos[1] = dstPos[1] - ux * w - uy * h;

        drawLine(gc, srcPos, dstPos);
        drawLine(gc, dstPos, leftPos);
        drawLine(gc, dstPos, rightPos);
    }

    /**
     * src座標からdst座標へ直線を描画
     */
    private static void drawLine(GraphicsContext gc, double[] srcPos, double[] dstPos) {
        double x1 = srcPos[0] * CANVAS_RATE;
        double y1 = srcPos[1] * CANVAS_RATE;
        double x2 = dstPos[0] * CANVAS_RATE;
        double y2 = dstPos[1] * CANVAS_RATE;

        gc.strokeLine(x1, y1, x2, y2);
    }
}
