package model;

import util.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static util.Const.*;

/**
 * Created by yutakase on 2016/11/29.
 */
public class Actor implements Serializable {
    // ID
    private int id;
    // position
    private int[] pos;
    // Capability(能力)ベクトル
    private List<Double> capabilities;
    // 各サービスの能力に対する評価ベクトルのリスト
    private List<List<Double>> features;
    // 各サービスの価格
    private List<Integer> prices;
    // サービス交換可能なActorのID
    private List<Integer> marketActorIdList;
    // 各サービスの購入先ActorのID
    private List<Integer> providerActorIdList;
    // 各サービスの売却先ActorのID
    private List<List<Integer>> consumerActorIdsList;
    // 各サービスにおける購入先の選考希望リスト
    private List<List<PurchaseInfo>> selectProviderList;
    // サービス交換相手が確定したか
    private List<Boolean> isMatches;
    // 均衡価格判定フラグ
    private boolean isChangePrice = true;
    // 売却先ソート用Comparator
    private List<ConsumerComparator> comparators;

    // Copy用
    private Actor() {
        this.pos = new int[DIM];
        this.capabilities = new ArrayList<>(CAPABILITY_COUNT);
        this.features = new ArrayList<>(SERVICE_COUNT);
        this.prices = new ArrayList<>(SERVICE_COUNT);
        this.marketActorIdList = new ArrayList<>();
        this.providerActorIdList = new ArrayList<>(SERVICE_COUNT);
        this.consumerActorIdsList = new ArrayList<>(SERVICE_COUNT);
        this.selectProviderList = new LinkedList<>();
        this.isMatches = new ArrayList<>(SERVICE_COUNT);
    }

    public Actor(int id) {
        this.id = id;

        // 座標を乱数で定義
        this.pos = IntStream
                .generate(() -> (int) CalcUtil.generateRandomDouble(POS_RAND_GENERATOR, 0, FIELD_SIZE))
                .limit(DIM)
                .toArray();

        // Capabilityを乱数で定義
        this.capabilities = Stream
//                .generate(() -> CalcUtil.generateRandomDouble(CAPABILITY_RAND_GENERATOR, MIN_CAPABILITY, MAX_CAPABILITY))// 一様乱数
                .generate(() -> CalcUtil.generateRandomGaussian(CAPABILITY_RAND_GENERATOR, MU_CAPABILITY, SD_CAPABILITY))// 正規乱数
                .limit(CAPABILITY_COUNT)
                .collect(Collectors.toList());

        // 各サービスのCapabilityに対する評価ベクトルのリストを定義
        this.features = IntStream
                .range(0, SERVICE_COUNT)
                .mapToObj(i -> {
                    // 評価ベクトルを乱数で定義
                    return Stream
//                    .generate(() -> CalcUtil.generateRandomDouble(FEATURE_RAND_GENERATOR, MIN_FEATURE, MAX_FEATURE))// 一様乱数
                            .generate(() -> CalcUtil.generateRandomGaussian(FEATURE_RAND_GENERATOR, MU_FEATURE, SD_FEATURE))// 正規乱数
                            .limit(CAPABILITIES_LISTS.get(i).size())
                            .collect(Collectors.toList());
                })
                .collect(Collectors.toList());

        // 各サービスの価格を最低価格で初期化
        this.prices = Stream
                .generate(() -> MIN_PRICE)
                .limit(SERVICE_COUNT)
                .collect(Collectors.toList());

        this.marketActorIdList = new ArrayList<>();

        // 各サービスの購入先を初期化
        this.providerActorIdList = Stream
                .generate(() -> -1)
                .limit(SERVICE_COUNT)
                .collect(Collectors.toList());

        // 売却先ActorIDのリストを初期化
        this.consumerActorIdsList = Stream
                .generate(ArrayList<Integer>::new)
                .limit(SERVICE_COUNT)
                .collect(Collectors.toList());

        this.isMatches = IntStream
                .range(0, SERVICE_COUNT)
                .mapToObj(i -> false)
                .collect(Collectors.toList());

    }

    /**
     * Test用
     */
    public Actor(int id, String test) {
        this.id = id;

        // 格子状に配置
        this.pos = new int[]{(id % 10) * (FIELD_SIZE / 10) + FIELD_SIZE / 20, (id / 10) * (FIELD_SIZE / 10) + FIELD_SIZE / 20};

        // Capabilityを乱数で定義
        this.capabilities = Stream
                .generate(() -> (double) id * 20)
                .limit(CAPABILITY_COUNT)
                .collect(Collectors.toList());

        // 各サービスのCapabilityに対する評価ベクトルのリストを定義
        this.features = IntStream
                .range(0, SERVICE_COUNT)
                .mapToObj(i -> {
                    // 評価ベクトルを乱数で定義
                    return Stream
                            .generate(() -> 1.0)
                            .limit(CAPABILITIES_LISTS.get(i).size())
                            .collect(Collectors.toList());
                })
                .collect(Collectors.toList());

        // 各サービスの価格を最低価格で初期化
        this.prices = Stream
                .generate(() -> MIN_PRICE)
                .limit(SERVICE_COUNT)
                .collect(Collectors.toList());

        this.marketActorIdList = new ArrayList<>();

        // 各サービスの購入先を初期化
        this.providerActorIdList = Stream
                .generate(() -> -1)
                .limit(SERVICE_COUNT)
                .collect(Collectors.toList());

        // 売却先ActorIDのリストを初期化
        this.consumerActorIdsList = Stream
                .generate(ArrayList<Integer>::new)
                .limit(SERVICE_COUNT)
                .collect(Collectors.toList());
    }

    /**
     * サービス交換するActorのリストを更新
     */
    public void updateMarketActors() {
        ActorUtil.updateMarketActors(this, this.pos, this.marketActorIdList);
    }

    /**
     * すべてのサービスに関して購入先を選択し、providerListを更新
     */
    public void updateProviders() {
        IntStream.range(0, SERVICE_COUNT)
                .forEach(serviceId -> {
                    Optional<Integer> selectedIdOptional = this.selectProviderId(serviceId);
                    selectedIdOptional.ifPresent(selectedId -> this.providerActorIdList.set(serviceId, selectedId));
                });
    }

    /**
     * serviceIdのサービスの購入先Actorを選択
     */
    public Optional<Integer> selectProviderId(int serviceId) {
        return selectProviderId(serviceId, this.marketActorIdList);
    }

    /**
     * 引数のmarketActorの中からserviceIdのサービスの購入先Actorを選択
     */
    public Optional<Integer> selectProviderId(int serviceId, List<Integer> marketActorIdList) {
        return ActorUtil.selectProvider(this, marketActorIdList, serviceId)
                .map(PurchaseInfo::getProviderId);
    }

    /**
     * 購入先リスト生成
     */
    public void updateSelectProviderList() {
        this.selectProviderList = IntStream
                .range(0, SERVICE_COUNT)
                .mapToObj(serviceId -> {
                    // 購入時利得を計算し、降順にソート
                    Optional<List<PurchaseInfo>> listOptional = ActorUtil.calcProviderSelectList(this, this.marketActorIdList, serviceId);
                    return (listOptional.isPresent()) ? listOptional.get() : new LinkedList<PurchaseInfo>();
                })
                .collect(Collectors.toList());
    }

    /**
     * 購入先リストからデータをポップ
     * データがリストが空のときは-1を返す
     */
    public int popSelectedProviderId(int serviceId) {
        if (selectProviderList.get(serviceId).isEmpty()) return -1;
        return this.selectProviderList.get(serviceId).remove(0).getProviderId();
    }

    /**
     * すべてのサービスに関して売却先を選択し、consumerListを更新
     */
    public void updateConsumers() {
        IntStream.range(0, SERVICE_COUNT)
                .forEach(serviceId -> {
                    Optional<List<Integer>> consumersIdListOptional = ActorUtil.countConsumer(this, serviceId);
                    consumersIdListOptional.ifPresent(consumersIdList -> this.consumerActorIdsList.set(serviceId, consumersIdList));
                });
    }

    /**
     * ActorIDを売却先に追加
     */
    public void addConsumersId(int serviceId, int actorId) {
        this.consumerActorIdsList.get(serviceId).add(actorId);
    }

    /**
     * すべてのサービスに関して売却先を選択し、consumerListを更新
     */
    public void limitAndUpdateConsumersId() {
        // 初回時、ソート用comparatorを初期化
        if (!Optional.ofNullable(comparators).isPresent()) {
            comparators = IntStream
                    .range(0, SERVICE_COUNT)
                    .mapToObj(i -> new ConsumerComparator(this, i))
                    .collect(Collectors.toList());
        }

        IntStream.range(0, SERVICE_COUNT)
                .forEach(serviceId -> {
                    // 売却先を利得順にソート
                    List<Integer> consumersList = this.consumerActorIdsList.get(serviceId);
                    consumersList.sort(comparators.get(serviceId).reversed());
                    // 上限まで抜き出し
                    List<Integer> limitConsumerActorIdsList = consumersList
                            .stream()
                            .limit(Const.MAX_CONSUMERS)
                            .collect(Collectors.toList());
                    // 更新
                    this.consumerActorIdsList.set(serviceId, limitConsumerActorIdsList);
                });
    }

    /**
     * 売却先ActorのFeatureベクトル方向にCapabilityを成長
     */
    public void growthCapability() {
        double growthRate = 100;
        IntStream.range(0, SERVICE_COUNT).forEach(serviceId -> {
            Optional<List<Double>> normalizedConsumersFeatureOptional = ActorUtil.calcConsumersFeature(this.getConsumerActorIdList(serviceId), serviceId);
            normalizedConsumersFeatureOptional.ifPresent(normalizedConsumersFeature -> {
                List<Double> curCapability = this.getCapabilities(serviceId);
                List<Double> newCapability = IntStream.range(0, curCapability.size()).mapToDouble(i -> curCapability.get(i) + normalizedConsumersFeature.get(i) * growthRate).boxed().collect(Collectors.toList());
                this.setCapabilities(newCapability, serviceId);
            });
        });
    }

    /**
     * 引数の価格とActorのの現在の価格がしきい値以下かどうか判定し、価格変化フラグを更新する
     */
    public void checkChangePrices(List<Integer> prices) {
        this.isChangePrice = IntStream
                .range(0, SERVICE_COUNT)
                .anyMatch(i -> Math.abs(this.prices.get(i) - prices.get(i)) > BALANCE_PRICE_THRESHOLD);
    }

    /**
     * Actorインスタンスをコピー
     *
     * @return インスタンスのディープコピー
     */
    public Actor deepCopy() {
        Actor copyActor = new Actor();

        copyActor.id = this.id;

        System.arraycopy(this.pos, 0, copyActor.pos, 0, this.pos.length);

        copyActor.capabilities.addAll(this.capabilities);

        for (List<Double> featureVec : this.features) {
            List<Double> featureVecCopy = new ArrayList<>(featureVec.size());
            featureVecCopy.addAll(featureVec);
            copyActor.features.add(featureVecCopy);
        }

        copyActor.prices.addAll(this.prices);

        copyActor.marketActorIdList.addAll(this.marketActorIdList);

        copyActor.providerActorIdList.addAll(this.providerActorIdList);

        for (List<Integer> consumersIds : this.consumerActorIdsList) {
            List<Integer> consumerIdsCopy = new ArrayList<>(consumersIds.size());
            consumerIdsCopy.addAll(consumersIds);
            copyActor.consumerActorIdsList.add(consumerIdsCopy);
        }

        copyActor.isMatches.addAll(this.isMatches);

        copyActor.isChangePrice = this.isChangePrice;

        return copyActor;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("id: ")
                .append(this.id)
                .append("\n");

        stringBuilder.append("pos: ")
                .append(Arrays.toString(this.pos))
                .append(" \n");

        stringBuilder.append("capabilities: ")
                .append(Arrays.toString(IntStream.range(0, SERVICE_COUNT).mapToObj(i -> Arrays.toString(this.getCapabilities(i).stream().map(StringUtil::formatTo1f).toArray())).toArray()))
                .append("\n");

        stringBuilder.append("features: ")
                .append(Arrays.toString(this.features.stream().map(feature -> Arrays.toString(feature.stream().map(StringUtil::formatTo1f).toArray())).toArray()))
                .append("\n");

        stringBuilder.append("prices: ")
                .append(this.prices.toString())
                .append("\n");

        stringBuilder.append("marketActors: ")
                .append(this.marketActorIdList.toString())
                .append("\n");

        stringBuilder.append("providerId: ")
                .append(this.providerActorIdList.toString())
                .append("\n");

        stringBuilder.append("providerSelectList: ")
                .append("\n")
                .append(ActorUtil.providerToString(this))
                .append("\n");

        stringBuilder.append("consumerId: ")
                .append(this.consumerActorIdsList.toString())
                .append("\n");

        stringBuilder.append("consumers")
                .append("\n")
                .append(ActorUtil.consumersToString(this, this.consumerActorIdsList))
                .append("\n");

        return stringBuilder.toString();
    }

    public int getId() {
        return this.id;
    }

    public int[] getPos() {
        return this.pos;
    }

    /**
     * サービスに対応するCapabilityのリストを返す
     *
     * @param serviceId サービスのid
     * @return Capabilityのリスト
     */
    public List<Double> getCapabilities(int serviceId) {
        // サービスに使用するCapabilityのIDリスト
        List<Integer> capabilitiesIdList = CAPABILITIES_LISTS.get(serviceId);
        return capabilitiesIdList.stream()
                .map(id -> this.capabilities.get(id))
                .collect(Collectors.toList());
    }

    public void setCapabilities(List<Double> capabilities, int serviceId) {
        List<Integer> capabilitiesIdList = CAPABILITIES_LISTS.get(serviceId);
        IntStream.range(0, capabilitiesIdList.size())
                .forEach(i -> this.capabilities.set(capabilitiesIdList.get(i), capabilities.get(i)));
    }

    public List<Double> getCapabilities() {
        return this.capabilities;
    }

    public List<Double> getFeature(int serviceId) {
        return this.features.get(serviceId);
    }

    public int getPrice(int serviceId) {
        return this.prices.get(serviceId);
    }

    public void setPrices(List<Integer> prices) {
        this.prices = prices;
    }

    public List<Integer> getPrices() {
        return this.prices;
    }

    public List<Integer> getMarketActorIdList() {
        return this.marketActorIdList;
    }

    public int getProviderId(int serviceId) {
        return this.providerActorIdList.get(serviceId);
    }

    public List<Integer> getProviderActorIdList() {
        return this.providerActorIdList;
    }

    public void setProviderActorId(int serviceId, int actorId) {
        this.providerActorIdList.set(serviceId, actorId);
    }

    public List<Integer> getConsumerActorIdList(int serviceId) {
        return this.consumerActorIdsList.get(serviceId);
    }

    public List<List<Integer>> getConsumerActorIdsList() {
        return this.consumerActorIdsList;
    }

    public void setIsMach(int serviceId, boolean isMatch) {
        this.isMatches.set(serviceId, isMatch);
    }

    public boolean isMatch(int serviceId) {
        return this.isMatches.get(serviceId);
    }

    public boolean isChangePrice() {
        return this.isChangePrice;
    }

}

