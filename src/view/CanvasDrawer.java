package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
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

    // 現在描画しているActorのリスト
    private static List<Actor> currentActors;
    private static List<List<List<Integer>>> priceList;

    // Actor描画のためのGraphicContext
    private static List<GraphicsContext> drawActorsTabGCList;

    private static List<LineChart<Number, Number>> priceLineCharts;

    // フォーカスされているActorのリスト
    private static List<Integer> focusActorIdList = new LinkedList<>();

    // 色
    private static final String RED = "F44336";
    private static final String GREEN = "4CAF50";
    private static final String INDIGO = "3F51B5";
    private static final String PURPLE = "9C27B0";
    private static final String LIME_600 = "C0CA33";
    private static final String CYAN = "00BCD4";
    private static List<Color> mainFocusColors = Arrays.asList(
            Color.web(RED),
            Color.web(GREEN),
            Color.web(INDIGO));
    private static List<Color> subFocusColors = Arrays.asList(
            Color.web(PURPLE),
            Color.web(LIME_600),
            Color.web(CYAN));
    private static List<Color> mainNoFocusColor = Arrays.asList(
            Color.web(RED, NO_FOCUS_COLOR_OPACITY),
            Color.web(GREEN, NO_FOCUS_COLOR_OPACITY),
            Color.web(INDIGO, NO_FOCUS_COLOR_OPACITY));
    private static List<Color> subNoFocusColors = Arrays.asList(
            Color.web(PURPLE, NO_FOCUS_COLOR_OPACITY),
            Color.web(LIME_600, NO_FOCUS_COLOR_OPACITY),
            Color.web(CYAN, NO_FOCUS_COLOR_OPACITY));
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
        // canvasのリストからGraphicContextのリストを生成
        drawActorsTabGCList = canvases.stream()
                .map(Canvas::getGraphicsContext2D)
                .collect(Collectors.toList());
    }

    /**
     * LineChartを登録
     */
    public static void setPriceLineCharts(List<LineChart<Number, Number>> lineCharts) {
        priceLineCharts = lineCharts;
    }

    /**
     * serviceIdのサービスに関してactorとnetworkを描画
     *
     * @param actors    Actorのリスト
     * @param serviceId サービスのID
     */
    public static void drawActorsAndNetwork(List<Actor> actors, int serviceId) {
        Optional.ofNullable(drawActorsTabGCList).ifPresent(gcList -> {
            GraphicsContext gc = gcList.get(serviceId);
            // Canvas clear
            gc.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);
            // actor描画
            actors.forEach(actor -> drawActors(gc, actor, serviceId));
            // ネットワーク描画
            drawNetwork(gc, actors, serviceId);
        });

        currentActors = actors;
        // Listenerのactorsを更新
        CanvasMousePressHandler.setActors(currentActors);
        TextFieldOnActionHandler.setActors(currentActors);
    }

    /**
     * 再描画
     */
    public static void reDraw() {
        Optional.ofNullable(currentActors).ifPresent(actors -> IntStream.range(0, SERVICE_COUNT + 1).forEach(i -> drawActorsAndNetwork(actors, i)));
        Optional.ofNullable(priceList).ifPresent(CanvasDrawer::drawPriceLineChart);
    }

    public static void drawPriceLineChart(List<List<List<Integer>>> pricesListList) {
        Optional.ofNullable(priceLineCharts).ifPresent(priceLineChart -> {
            priceLineChart.forEach(lineChart -> lineChart.getData().clear());
            // series初期化
            List<List<XYChart.Series<Number, Number>>> seriesListList = IntStream
                    .range(0, SERVICE_COUNT + 1)
                    .mapToObj(serviceId ->
                            IntStream.range(0, ACTOR_COUNT)
                                    .mapToObj(actorId -> {
                                        XYChart.Series<Number, Number> series = new XYChart.Series<>();
                                        series.setName(String.valueOf(actorId));
                                        return series;
                                    })
                                    .collect(Collectors.toList())
                    )
                    .collect(Collectors.toList());

            // Data追加
            IntStream.range(0, pricesListList.size()).forEach(i -> {
                List<List<Integer>> pricesList = pricesListList.get(i);
                IntStream.range(0, ACTOR_COUNT)
                        .filter(CanvasDrawer::isFocus)
                        .forEach(actorId -> {
                            List<Integer> prices = pricesList.get(actorId);
                            IntStream.range(0, SERVICE_COUNT + 1)
                                    .forEach(serviceId -> {
                                        int price = (serviceId != ALL_SERVICES_ID) ? prices.get(serviceId) : prices.stream().mapToInt(Integer::intValue).sum();
                                        seriesListList.get(serviceId).get(actorId).getData().add(new XYChart.Data<>(i, price));
                                    });
                        });
            });

            IntStream.range(0, SERVICE_COUNT + 1)
                    .forEach(serviceId ->
                            seriesListList.get(serviceId).forEach(series ->
                                    priceLineChart.get(serviceId).getData().add(series)
                            )
                    );
        });
        priceList = pricesListList;
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
     * フォーカス状態をクリア
     */
    public static void clearFocusActorIdList() {
        focusActorIdList.clear();
    }

    /**
     * 矢印の透明度を変更する
     */
    public static void setOpacity(int serviceId, double opacity) {
        Color oldColor = mainFocusColors.get(serviceId);
        double r = oldColor.getRed();
        double g = oldColor.getGreen();
        double b = oldColor.getBlue();
        Color newColor = new Color(r, g, b, opacity);
        mainFocusColors.set(serviceId, newColor);

        newColor = new Color(r, g, b, opacity * NO_FOCUS_COLOR_OPACITY);
        mainNoFocusColor.set(serviceId, newColor);

        oldColor = subFocusColors.get(serviceId);
        r = oldColor.getRed();
        g = oldColor.getGreen();
        b = oldColor.getBlue();
        newColor = new Color(r, g, b, opacity);
        subFocusColors.set(serviceId, newColor);

        newColor = new Color(r, g, b, opacity * NO_FOCUS_COLOR_OPACITY);
        subNoFocusColors.set(serviceId, newColor);
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
                color = isFocus ? mainFocusColors.get(serviceId) : mainNoFocusColor.get(serviceId);
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
                    .forEach(i -> drawArrowsAtAllServiceTab(gc, actor, actors, i)));
        } else {
            actors.forEach(actor -> drawArrows(gc, actor, actors, serviceId));
        }
    }

    /**
     * hostActorからすべての供給先Actorへの矢印を描画
     * 各サービス表示用
     * 各サービス表示用とは色の付け方が異なる
     *
     * @param gc        GraphicContext
     * @param hostActor ネットワークを描画するActor
     * @param actors    全Actorリスト
     * @param serviceId サービスのID
     */
    private static void drawArrows(GraphicsContext gc, Actor hostActor, List<Actor> actors, int serviceId) {
        hostActor.getConsumerActorIdList(serviceId).forEach(consumerId -> {
            Actor consumerActor = actors.get(consumerId);
            gc.setStroke(selectArrowColor(hostActor, consumerActor, serviceId));
            drawArrow(gc, hostActor, consumerActor);
        });
    }

    /**
     * hostActorからすべての供給先Actorへの矢印を描画
     * 全サービス表示用
     * 各サービス表示用とは色の付け方が異なる
     *
     * @param gc        GraphicContext
     * @param hostActor ネットワークを描画するActor
     * @param actors    全Actorリスト
     * @param serviceId サービスのID
     */
    private static void drawArrowsAtAllServiceTab(GraphicsContext gc, Actor hostActor, List<Actor> actors, int serviceId) {
        Color color = isFocus(hostActor.getId()) ? mainFocusColors.get(serviceId) : mainNoFocusColor.get(serviceId);
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

    /**
     * Capabilityによって表示する色を決定し返却する
     */
    private static Color selectArrowColor(Actor provider, Actor consumer, int serviceId) {
        // 2つのCapabilityのうち、どちらの方が価値に大きく反映されているか
        // 1つ目であればMainColorを使用する
        List<Double> capability = provider.getCapabilities(serviceId);
        List<Double> feature = consumer.getFeature(serviceId);
        boolean useMainColor = capability.get(0) * feature.get(0) > capability.get(1) * feature.get(1);

        Color color;
        if (isFocus(provider.getId())) {
            color = (useMainColor) ? mainFocusColors.get(serviceId) : subFocusColors.get(serviceId);
        } else {
            color = (useMainColor) ? mainNoFocusColor.get(serviceId) : subNoFocusColors.get(serviceId);
        }
        return color;
    }
}
