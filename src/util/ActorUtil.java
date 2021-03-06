package util;

import model.Actor;
import model.PurchaseInfo;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static util.Const.*;

/**
 * Created by yutakase on 2016/12/04.
 */
public final class ActorUtil {

    // 全Actorのリスト
    private static List<Actor> actors;

    // 購入先比較
    private static Comparator<PurchaseInfo> purchaseInfoComparator = (p1, p2) -> Double.compare(p1.getProfit(), p2.getProfit());


    private ActorUtil() {
    }

    /**
     * Actorのリストを生成
     */
    public static List<Actor> createActors() {
        actors = IntStream
                .range(0, ACTOR_COUNT)
                .mapToObj(Actor::new)
                .collect(Collectors.toList());
        return actors;
    }

    /**
     * Test用Actorのリストを生成
     */
    public static List<Actor> createTestActors() {
        actors = IntStream
                .range(0, ACTOR_COUNT)
                .mapToObj(i -> new Actor(i, "test"))
                .collect(Collectors.toList());
        return actors;
    }

    /**
     * 利得計算
     *
     * @param provider  提供Actor
     * @param consumer  顧客Actor
     * @param serviceId サービスID
     * @return 利得
     */
    public static double calcProfit(Actor provider, Actor consumer, int serviceId) {
        double value = calcValue(provider, consumer, serviceId);
        // 自給の場合は0
        boolean isSelfService = provider.equals(consumer);
        double priceCost = isSelfService ? 0 : provider.getPrice(serviceId);
        double dist = isSelfService ? 0 : CalcUtil.calcDist(provider.getPos(), consumer.getPos());

        return calcProfit(value, priceCost, dist);
    }

    /**
     * 利得計算
     *
     * @param value     価値
     * @param priceCost 価格
     * @param dist      提供Actorまでの距離
     * @return 利得
     */
    private static double calcProfit(double value, double priceCost, double dist) {
        return value - priceCost - dist * MOVE_COST;
    }

    /**
     * 価値計算
     *
     * @param provider  提供Actor
     * @param consumer  顧客Actor
     * @param serviceId サービスID
     * @return 価値
     */
    public static double calcValue(Actor provider, Actor consumer, int serviceId) {
        List<Double> capability = provider.getCapabilities(serviceId);
        List<Double> feature = consumer.getFeature(serviceId);
        return calcValue(capability, feature);
    }

    /**
     * 価値計算
     *
     * @param capability 能力ベクトル
     * @param feature    評価ベクトル
     * @return 価値
     */
    private static double calcValue(List<Double> capability, List<Double> feature) {
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
        Optional.ofNullable(actors).ifPresent(actors -> {
            marketActors.clear();
            // 取引可能な範囲にいるActorのIDのを登録
            marketActors.addAll(actors.stream().parallel()
                    .filter(targetActor -> !targetActor.equals(hostActor) && isMarketRange(hostPos, targetActor.getPos()))
                    .mapToInt(Actor::getId)
                    .boxed()
                    .collect(Collectors.toList()));
        });
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
        return Optional.ofNullable(actors).map(actors -> {
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
     * serviceIdのサービスに関する選考希望のリストを生成
     */
    public static Optional<List<PurchaseInfo>> calcProviderSelectList(Actor hostActor, List<Integer> marketActorIdList, int serviceId) {
        return Optional.ofNullable(actors).map(actors -> {
            // 自給時の利得を計算
            PurchaseInfo selfPurchase = calcPurchaseInfo(hostActor, hostActor, serviceId);

            // 交換可能な範囲のActorとの利得を計算
            List<PurchaseInfo> selectList = marketActorIdList.stream()
                    .map(marketActorId -> calcPurchaseInfo(actors.get(marketActorId), hostActor, serviceId))
                    .collect(Collectors.toCollection(LinkedList::new));

            // 自給を追加し降順にsort
            selectList.add(selfPurchase);
            selectList.sort(purchaseInfoComparator.reversed());

            return selectList;
        });
    }

    /**
     * サービス交換による購入情報を計算
     */
    private static PurchaseInfo calcPurchaseInfo(Actor provider, Actor consumer, int serviceId) {
        int price = (provider.equals(consumer)) ? 0 : provider.getPrice(serviceId);
        double profit = ActorUtil.calcProfit(provider, consumer, serviceId);
        return new PurchaseInfo(provider.getId(), profit, price);
    }

    /**
     * サービス交換の循環を探索
     *
     * @param actor      探索したいActor
     * @param limitCount 探索回数の上限
     * @return 循環しているPathのリスト
     */
    public static List<List<Integer>> exploreEcosystem(Actor actor, int limitCount) {
        List<List<Integer>> pathList = new ArrayList<>();
        Optional.ofNullable(actors).ifPresent(actors -> {
            // サービス交換のPathを保存するList
            List<Integer> path = new ArrayList<>();
            int actorId = actor.getId();
            path.add(actorId);
            List<Integer> consumerIds = actors.get(actorId).getConsumerActorIdsList()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(id -> id != actorId)
                    .collect(Collectors.toList());
            consumerIds.forEach(consumerId -> recursiveExploreConsumerActor(actors.get(consumerId), actorId, actorId, path, pathList, limitCount, 0));
        });
        return pathList;
    }

    /**
     * サービス交換を再帰的に探索
     * 循環が発見できればPathListに追加
     *
     * @param actor
     * @param providerId 提供者
     * @param targetId   循環を探しているActorのID
     * @param path       現在地点までのPath
     * @param pathList   循環Pathを保存するリスト
     * @param limitCount 深さ上限
     * @param count      現在の深さ
     */
    private static void recursiveExploreConsumerActor(Actor actor, int providerId, int targetId, List<Integer> path, List<List<Integer>> pathList, int limitCount, int count) {
        if (count == limitCount) return;

        int actorId = actor.getId();
        path.add(actorId);
        if (actorId == targetId) {
            // 循環発見
            List<Integer> pathCopy = new ArrayList<>(path.size());
            pathCopy.addAll(path);
            pathList.add(pathCopy);
        } else {
            // さらに探索
            List<Integer> consumerIds = actor.getConsumerActorIdsList()
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(id -> id != actorId)
                    .filter(id -> (providerId == targetId) || (id != providerId))
                    .collect(Collectors.toList());
            consumerIds.forEach(consumerId -> recursiveExploreConsumerActor(actors.get(consumerId), actorId, targetId, path, pathList, limitCount, count + 1));
        }
        path.remove(path.size() - 1);
    }

    /**
     * hostActorの売却先ActorのIDのリストを計算
     */
    public static Optional<List<Integer>> countConsumer(Actor hostActor, int serviceId) {
        return Optional.ofNullable(actors).map(actors -> {
            List<Integer> consumerIdList = new ArrayList<>();
            // サービス交換可能な各Actorに対して
            hostActor.getMarketActorIdList().forEach(marketActorId -> {
                // 各Actorの購入先ActorのIDを計算
                Actor marketActor = actors.get(marketActorId);
                Optional<Integer> selectedActorIdOptional = marketActor.selectProviderId(serviceId);
                selectedActorIdOptional.ifPresent(id -> {
                    // 選択されたActorがhostActorならばconsumerListに追加
                    if (id == hostActor.getId()) {
                        consumerIdList.add(marketActorId);
                    }
                });
            });
            return consumerIdList;
        });
    }

    /**
     * hostActorの価格がpriceの時の売却先ActorのIDのリストを計算
     * hostActor抜きで購入先を計算させ、最後に価格を変更したhostActorと比較し、売却先かどうか判断する
     */
    public static Optional<List<Integer>> countConsumerSimulate(Actor hostActor, int price, int serviceId) {
        return Optional.ofNullable(actors).map(actors -> {
            List<Integer> consumerIdList = new ArrayList<>();
            // サービス交換可能な各Actorに対して
            hostActor.getMarketActorIdList().forEach(marketActorId -> {
                Actor marketActor = actors.get(marketActorId);
                // 交換先Actorの交換可能Actorリスト
                List<Integer> marketActorsIdListOfMarketActor = new ArrayList<>();
                marketActorsIdListOfMarketActor.addAll(marketActor.getMarketActorIdList());
                // 自分抜きで購入先を選択させる
                if (marketActorsIdListOfMarketActor.contains(hostActor.getId())) {
                    marketActorsIdListOfMarketActor.remove(Integer.valueOf(hostActor.getId()));
                }
                Optional<Integer> selectedActorIdOptional = marketActor.selectProviderId(serviceId, marketActorsIdListOfMarketActor);

                selectedActorIdOptional.ifPresent(selectedId -> {
                    // 選択された購入による利得
                    double selectedProfit = calcPurchaseInfo(actors.get(selectedId), marketActor, serviceId).getProfit();
                    // 自分との交換による相手の利得
                    double value = ActorUtil.calcValue(hostActor.getCapabilities(serviceId), marketActor.getFeature(serviceId));
                    double dist = CalcUtil.calcDist(hostActor.getPos(), marketActor.getPos());
                    double hostActorProfit = ActorUtil.calcProfit(value, price, dist);

                    // 自分との交換の利得のほうが大きければ売却先としてListに追加
                    if (hostActorProfit > selectedProfit) {
                        consumerIdList.add(marketActorId);
                    }
                });
            });
            return consumerIdList;
        });
    }

    /**
     * 全consumerのFeatureの合成ベクトルの向きベクトルを返す
     */
    public static Optional<List<Double>> calcConsumersFeature(List<Integer> consumerIds, int serviceId) {
        if (consumerIds.size() == 0) return Optional.empty();
        return Optional.ofNullable(actors).map(actors -> {
            // featureベクトルをすべて合成
            List<Double> consumerFeatureSum = IntStream.range(0, CAPABILITIES_LISTS.get(serviceId).size())
                    .mapToDouble(dim -> consumerIds.stream().mapToDouble(consumerId -> actors.get(consumerId).getFeature(serviceId).get(dim)).sum())
                    .boxed()
                    .collect(Collectors.toList());
            // 正規化
            double consumerFeatureSumSize = Math.sqrt(consumerFeatureSum.stream().mapToDouble(elem -> elem * elem).sum());
            return consumerFeatureSum.stream().mapToDouble(elem -> elem / consumerFeatureSumSize).boxed().collect(Collectors.toList());
        });
    }

    /**
     * 購入先の詳細を文字列にして返す
     */
    public static String providerToString(Actor hostActor) {
        StringBuilder sb = new StringBuilder();
        IntStream.range(0, SERVICE_COUNT)
                .forEach(serviceId -> {
                    sb.append("serviceID: ")
                            .append(serviceId)
                            .append("\n");
                    calcProviderSelectList(hostActor, hostActor.getMarketActorIdList(), serviceId).ifPresent(purchaseInfos ->
                            purchaseInfos.stream()
                                    .filter(purchaseInfo -> {
                                        sb.append(purchaseInfo.getProviderId())
                                                .append(": ")
                                                .append(StringUtil.formatTo1f(purchaseInfo.getProfit()))
                                                .append("\n");
                                        return purchaseInfo.getProviderId() == hostActor.getProviderId(serviceId);
                                    })
                                    .findFirst()
                    );
                });
        return sb.toString();
    }

    /**
     * 売却先の詳細を文字列にして返す
     */
    public static String consumersToString(Actor provider, List<List<Integer>> consumerActorsIdList) {
        StringBuilder sb = new StringBuilder();
        Optional.ofNullable(actors).ifPresent(actors1 ->
                IntStream.range(0, SERVICE_COUNT)
                        .forEach(serviceId -> {
                            sb.append("serviceID: ")
                                    .append(serviceId)
                                    .append("\n");
                            consumerActorsIdList.get(serviceId).forEach(consumerId -> {
                                Actor consumer = actors1.get(consumerId);
                                double value = calcValue(provider, consumer, serviceId);
                                double profit = calcProfit(provider, consumer, serviceId);
                                sb.append(consumerId)
                                        .append(": ")
                                        .append(StringUtil.formatTo1f(value))
                                        .append(" -> ")
                                        .append(StringUtil.formatTo1f(profit))
                                        .append("\n");
                            });
                        })
        );
        return sb.toString();
    }

    public static void setActors(List<Actor> actors) {
        ActorUtil.actors = actors;
    }

    public static Optional<List<Actor>> getActors() {
        return Optional.ofNullable(actors);
    }
}

