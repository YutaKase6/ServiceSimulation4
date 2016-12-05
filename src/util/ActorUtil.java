package util;

import model.Actor;
import model.PurchaseInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static util.Const.*;

/**
 * Created by yutakase on 2016/12/04.
 */
public final class ActorUtil {

    // 全Actorのリスト
    private static Optional<List<Actor>> actorsOptional;

    private static Comparator<PurchaseInfo> purchaseInfoComparator = (p1, p2) -> Double.compare(p1.getProfit(), p2.getProfit());

    private ActorUtil() {
    }

    /**
     * Actorのリストを生成
     */
    public static List<Actor> createActors() {
        List<Actor> actors = IntStream
                .range(0, ACTOR_COUNT)
                .mapToObj(Actor::new)
                .collect(Collectors.toList());
        actorsOptional = Optional.of(actors);
        return actors;
    }

    /**
     * Test用Actorのリストを生成
     */
    public static List<Actor> createTestActors() {
        List<Actor> actors = IntStream
                .range(0, ACTOR_COUNT)
                .mapToObj(i -> new Actor(i, "test"))
                .collect(Collectors.toList());
        actorsOptional = Optional.of(actors);
        return actors;
    }

    /**
     * 利得計算
     *
     * @param value     価値
     * @param priceCost 価格
     * @param dist      提供Actorまでの距離
     * @return 利得
     */
    public static double calcProfit(double value, double priceCost, double dist) {
        return value - priceCost - dist * MOVE_COST;
    }

    /**
     * 価値計算
     *
     * @param capability 能力ベクトル
     * @param feature    評価ベクトル
     * @return 価値
     */
    public static double calcValue(List<Double> capability, List<Double> feature) {
        return CalcUtil.dotProduct(capability, feature);
    }

    /**
     * トーラス空間上の座標に変換
     *
     * @param p 座標
     * @return トーラス空間上の座標
     */
    public static int convertTorusPos(int p) {
        if (p > FIELD_SIZE - 1) {
            return p - FIELD_SIZE;
        } else if (p < 0) {
            return p + FIELD_SIZE;
        } else {
            return p;
        }
    }

    /**
     * 他のActorを探索し
     * 市場範囲にいるActorを登録
     * Actorの座標を別に渡すのは，Actorの現在地とは異なる場所での計算(シミュレーション)を可能にするため
     * marketActorsも同様
     *
     * @param hostActor    中心となるActor
     * @param hostPos      中心となるActorの座標
     * @param marketActors 市場範囲にいるActorのIDを格納するリスト
     */
    public static void updateMarketActors(Actor hostActor, int[] hostPos, List<Integer> marketActors) {
        actorsOptional.ifPresent(actors -> {
            marketActors.clear();
            // 取引可能な範囲にいるActorのIDのを登録
            marketActors.addAll(actors.stream().parallel()
                            .filter(targetActor -> !targetActor.equals(hostActor) && isMarketRange(hostPos, targetActor.getPos()))
                            .mapToInt(Actor::getId)
                            .boxed()
                            .collect(Collectors.toList()));});
    }

    /**
     * 取引可能な距離かどうか
     *
     * @param posA 座標A
     * @param posB 座標B
     * @return T or F
     */
    public static boolean isMarketRange(int[] posA, int[] posB) {
        double dist = CalcUtil.calcDist(posA, posB);
        return dist <= MARKET_RANGE;
    }

    /**
     * serviceIdのサービスに関する最大利得と最大利得となるActorのIDのを計算
     */
    public static Optional<PurchaseInfo> selectProvider(Actor hostActor, List<Integer> marketActorIdList, int serviceId) {
        return actorsOptional.map(actors -> {
            // 自給時の利得を計算
            PurchaseInfo selfPurchase = calcPurchaseInfo(hostActor, hostActor, serviceId);

            // 交換可能Actorの中で利得最大となるActorを計算
            Optional<PurchaseInfo> maxProfitPurchaseOptional = marketActorIdList.stream()
                    .map(marketActorId -> calcPurchaseInfo(actors.get(marketActorId), hostActor, serviceId))
                    .max(purchaseInfoComparator);

            // 自給が購入か、利得が大きい方を返却する
            if (maxProfitPurchaseOptional.isPresent()) {
                PurchaseInfo maxProfitPurchase = maxProfitPurchaseOptional.get();
                return (maxProfitPurchase.getProfit() > selfPurchase.getProfit()) ? maxProfitPurchase : selfPurchase;
            } else {
                // 交換可能なActorがいない場合、自給の結果を返す
                return selfPurchase;
            }
        });
    }

    /**
     * サービス交換による購入情報を計算
     */
    private static PurchaseInfo calcPurchaseInfo(Actor provider, Actor consumer, int serviceId) {
        List<Double> consumerFeatures = consumer.getFeature(serviceId);
        if (provider.equals(consumer)) {
            // 自給計算
            double selfValue = ActorUtil.calcValue(consumer.getCapabilities(serviceId), consumerFeatures);
            int selfPrice = 0;
            double selfDist = 0;
            double selfProfit = ActorUtil.calcProfit(selfValue, selfPrice, selfDist);
            return new PurchaseInfo(consumer.getId(), selfProfit, selfPrice);
        } else {
            List<Double> providerCapabilities = provider.getCapabilities(serviceId);
            double value = ActorUtil.calcValue(providerCapabilities, consumerFeatures);
            int price = provider.getPrice(serviceId);
            double dist = CalcUtil.calcDist(consumer.getPos(), provider.getPos());
            double profit = ActorUtil.calcProfit(value, price, dist);
            return new PurchaseInfo(provider.getId(), profit, price);
        }
    }

    /**
     * hostActorの売却先ActorのIDのリストを計算
     */
    public static Optional<List<Integer>> countConsumer(Actor hostActor, int serviceId) {
        return actorsOptional.map(actors -> {
            List<Integer> consumerIdList = new ArrayList<>();
            hostActor.getMarketActorIdList().forEach(marketActorId -> {
                // 各Actorの購入先ActorのIDを計算
                Actor marketActor = actors.get(marketActorId);
                Optional<Integer> selectedActorIdOptional = marketActor.selectProvider(serviceId);
                // 選択されたActorがhostActorならばconsumerListに追加
                selectedActorIdOptional.ifPresent(id -> {
                    if (id == hostActor.getId()) {
                        consumerIdList.add(marketActorId);
                    }
                });
            });
            return consumerIdList;
        });
    }

    public static Optional<List<Actor>> getActorsOptional() {
        return actorsOptional;
    }
}
