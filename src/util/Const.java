package util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by yutakase on 2016/11/29.
 */
public final class Const {
    private Const() {
    }

    public static final int DIM = 2;
    public static final int ACTOR_COUNT = 100;
    public static final int SERVICE_COUNT = 3;
    public static final int SIMULATION_COUNT = 1;
    public static final int FIELD_SIZE = 100;
    public static final int MARKET_RANGE = 30;

    public static final int DELTA_PRICE = 100;
    public static final int MIN_PRICE = 0;
    public static final int MAX_PRICE = 10000;

    // Actor Parameter
    public static final int CAPABILITY_COUNT = 6;
    public static final double MIN_CAPABILITY = 1;
    public static final double MAX_CAPABILITY = 2000;
    public static final double MIN_FEATURE = 0.1;
    public static final double MAX_FEATURE = 3.0;
    public static final double MOVE_COST = 0.0;

    public static final List<List<Integer>> CAPABILITIES_LISTS;
    static {
        // 存在するサービスの数だけCapabilityのリストを生成
        List<Integer> list0 = Arrays.asList(0, 1);
        List<Integer> list1 = Arrays.asList(2, 3);
        List<Integer> list2 = Arrays.asList(4, 5);

        CAPABILITIES_LISTS = new ArrayList<>(SERVICE_COUNT);
        CAPABILITIES_LISTS.add(list0);
        CAPABILITIES_LISTS.add(list1);
        CAPABILITIES_LISTS.add(list2);
    }

    // View Parameter
    public static final int SCREEN_WIDTH = 1200;
    public static final int SCREEN_HEIGHT = 700;
    public static final int CANVAS_SIZE = 600;
    public static final int ACTOR_CIRCLE_SIZE = 2;
    public static final double CANVAS_RATE = CANVAS_SIZE / FIELD_SIZE;
    public static final double NO_FOCUS_COLOR_OPACITY = 0.2;

    public static final int ALL_SERVICES_ID = SERVICE_COUNT;

    // Actorの座標生成に用いる乱数
    public static final Random POS_RAND_GENERATOR = new Random(1);
    // Capability(能力)の生成に用いる乱数
    public static final Random CAPABILITY_RAND_GENERATOR = new Random(2);
    // Feature(能力に対する重み)の生成に用いる乱数
    public static final Random FEATURE_RAND_GENERATOR = new Random(3);


    public static final String SIMULATION_TEXT = "Simulation";
    public static final String LOAD_FILE_TEXT = "Load file";
}
