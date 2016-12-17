package view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
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

    // 現在のCanvasの大きさ
    private static int currentCanvasSize = CANVAS_SIZE;
    // Canvasとフィールドのサイズ比率
    public static double currentCanvasRate = CANVAS_RATE;

    // 現在描画しているActorのリスト
    private static List<Actor> currentActors;
    // 価格均衡までの価格遷移データ
    private static List<List<List<Integer>>> priceList;

    // Actor描画のためのGraphicContext
    private static List<GraphicsContext> drawActorsTabGCList;
    // Actor描画のためのCanvas
    private static List<Canvas> drawActorsCanvases;
    // 価格遷移描画の線グラフ
    private static List<LineChart<Number, Number>> priceLineCharts;
    // 出力用Text
    private static Text printText;

    // フォーカスされているActorのリスト
    private static List<Integer> focusActorIdList = new LinkedList<>();

    // 表示フラグ
    private static boolean showPriceCircle = false;
    private static boolean showCapability = false;

    // 色
    private static final String RED = "F44336";
    private static final String GREEN = "4CAF50";
    private static final String INDIGO = "3F51B5";
    private static final String PURPLE = "9C27B0";
    private static final String LIME_600 = "C0CA33";
    private static final String CYAN = "00BCD4";
    private static final String BROWN = "795548";
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
    private static Color focusBrown = Color.web(BROWN);
    private static Color noFocusBrown = Color.web(BROWN, NO_FOCUS_COLOR_OPACITY);

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
        drawActorsCanvases = canvases;
    }

    /**
     * LineChartを登録
     */
    public static void setPriceLineCharts(List<LineChart<Number, Number>> lineCharts) {
        priceLineCharts = lineCharts;
    }

    /**
     * 出力用Textを登録
     */
    public static void setPrintText(Text text) {
        printText = text;
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
            gc.clearRect(0, 0, currentCanvasSize, currentCanvasSize);
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

    /**
     * 価格遷移グラフ描画
     */
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
     * Actorとその売却先、購入先をフォーカス
     */
    public static void focusRelatedActorId(int actorId) {
        Optional.ofNullable(currentActors).ifPresent(actors -> {
            addFocusActorId(actorId);
            Actor actor = actors.get(actorId);
            actor.getConsumerActorIdsList().forEach(consumerActorIds -> consumerActorIds.forEach(CanvasDrawer::addFocusActorId));
            actor.getProviderActorIdList().forEach(CanvasDrawer::addFocusActorId);
        });
    }

    /**
     * Actorのフォーカス状態を変更
     */
    private static void changeFocusActorId(int actorId) {
        if (focusActorIdList.contains(actorId)) {
            // フォーカスされていたら解除
            focusActorIdList.remove(Integer.valueOf(actorId));
        } else {
            // フォーカスされていなければ登録
            focusActorIdList.add(actorId);
            // 情報を表示
            Optional.ofNullable(currentActors).ifPresent(actors -> CanvasDrawer.printText(actors.get(actorId).toString()));
        }
    }

    /**
     * Actorをフォーカス
     */
    private static void addFocusActorId(int actorId) {
        if (!focusActorIdList.contains(actorId)) {
            focusActorIdList.add(actorId);
            Optional.ofNullable(currentActors).ifPresent(actors -> CanvasDrawer.printText(actors.get(actorId).toString()));
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
     * Text出力
     */
    public static void printText(String text) {
        Optional.ofNullable(printText).ifPresent(textPane -> {
            String textStr = textPane.getText() + text;
            textPane.setText(textStr);
        });
    }

    /**
     * Textクリア
     */
    public static void clearText() {
        Optional.ofNullable(printText).ifPresent(textPane -> {
            String textStr = "";
            textPane.setText(textStr);
        });
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
        if (showPriceCircle) {
            int price = (serviceId != ALL_SERVICES_ID) ? actor.getPrice(serviceId) : actor.getPrices().stream().mapToInt(Integer::intValue).sum();
            double size = (1 + (price / 100.0));
            double left = (pos[0] - (size / 2)) * currentCanvasRate;
            double top = (pos[1] - (size / 2)) * currentCanvasRate;
            Color color = isFocus ? focusBrown : noFocusBrown;
            gc.setStroke(color);
            drawTorusOval(gc, left, top, size * currentCanvasRate);
        }
        double size = ACTOR_CIRCLE_SIZE;
        if (showCapability) {
            double capability = (serviceId != ALL_SERVICES_ID) ? actor.getCapabilities(serviceId).stream().mapToDouble(Double::doubleValue).sum() : (actor.getCapabilities().stream().mapToDouble(Double::doubleValue).sum());
            size = capability / 500;
        }
        double left = (pos[0] - (size / 2)) * currentCanvasRate;
        double top = (pos[1] - (size / 2)) * currentCanvasRate;
        Color color = isFocus ? focusBlack : noFocusBlack;
        gc.setStroke(color);
        drawTorusOval(gc, left, top, size * currentCanvasRate);

        // 供給Actorは色付きで描画
        if (serviceId != ALL_SERVICES_ID) {
            if (actor.getConsumerActorIdList(serviceId).size() > 0) {
                color = isFocus ? mainFocusColors.get(serviceId) : mainNoFocusColor.get(serviceId);
                gc.setStroke(color);
                drawTorusOval(gc, left, top, size * currentCanvasRate);
            }
        }

        // ID描画
        color = isFocus ? Color.BLACK : noFocusBlack;
        gc.setStroke(color);
        gc.strokeText("" + actor.getId(), pos[0] * currentCanvasRate, pos[1] * currentCanvasRate);
    }

    /**
     * サービス交換ネットワークを描画
     */
    private static void drawNetwork(GraphicsContext gc, List<Actor> actors, int serviceId) {
        if (serviceId == ALL_SERVICES_ID) {
            // すべてのサービスのネットワークを描画
            actors.forEach(actor -> IntStream
                    .range(0, SERVICE_COUNT)
                    .forEach(i -> drawArrows(gc, actor, actors, i, true)));
        } else {
            actors.forEach(actor -> drawArrows(gc, actor, actors, serviceId, false));
        }
    }

    /**
     * hostActorからすべての供給先Actorへの矢印を描画
     *
     * @param gc        GraphicContext
     * @param hostActor ネットワークを描画するActor
     * @param actors    全Actorリスト
     * @param serviceId サービスのID
     * @param isAllTab  全サービス表示タブか否か
     */
    private static void drawArrows(GraphicsContext gc, Actor hostActor, List<Actor> actors, int serviceId, boolean isAllTab) {
        hostActor.getConsumerActorIdList(serviceId).forEach(consumerId -> {
            Actor consumerActor = actors.get(consumerId);
            gc.setStroke(selectArrowColor(hostActor, consumerActor, serviceId, isAllTab));
            drawArrow(gc, hostActor, consumerActor);
        });
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
            torusX = left + currentCanvasSize;
        } else if (left + size > currentCanvasSize) {
            torusX = left - currentCanvasSize;
        }
        if (top < 0) {
            torusY = top + currentCanvasSize;
        } else if (top + size > currentCanvasSize) {
            torusY = top - currentCanvasSize;
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
        double x1 = srcPos[0] * currentCanvasRate;
        double y1 = srcPos[1] * currentCanvasRate;
        double x2 = dstPos[0] * currentCanvasRate;
        double y2 = dstPos[1] * currentCanvasRate;

        gc.strokeLine(x1, y1, x2, y2);
    }

    /**
     * Capabilityによって表示する色を決定し返却する
     */
    private static Color selectArrowColor(Actor provider, Actor consumer, int serviceId, boolean isAllTab) {
        boolean isFocus = isFocus(provider.getId()) && isFocus(consumer.getId());
        if (isAllTab) {
            return isFocus ? mainFocusColors.get(serviceId) : mainNoFocusColor.get(serviceId);
        }
        // 2つのCapabilityのうち、どちらの方が価値に大きく反映されているか
        // 1つ目であればMainColorを使用する
        List<Double> capability = provider.getCapabilities(serviceId);
        List<Double> feature = consumer.getFeature(serviceId);
        boolean useMainColor = capability.get(0) * feature.get(0) > capability.get(1) * feature.get(1);

        Color color;
        if (isFocus) {
            color = (useMainColor) ? mainFocusColors.get(serviceId) : subFocusColors.get(serviceId);
        } else {
            color = (useMainColor) ? mainNoFocusColor.get(serviceId) : subNoFocusColors.get(serviceId);
        }
        return color;
    }

    /**
     * Canvasの大きさを変更
     */
    public static void setCanvasSize(int size) {
        drawActorsCanvases.forEach(canvas -> {
            canvas.setWidth(size);
            canvas.setHeight(size);
            currentCanvasSize = size;
            currentCanvasRate = size / FIELD_SIZE;
        });
    }

    public static void changeShowPriceCircle() {
        CanvasDrawer.showPriceCircle = !CanvasDrawer.showPriceCircle;
    }

    public static void changeShowCapability() {
        CanvasDrawer.showCapability = !CanvasDrawer.showCapability;
    }
}
