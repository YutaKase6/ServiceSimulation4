package simulation;

import model.Actor;
import util.ActorUtil;
import util.FileIO;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static util.Const.*;

/**
 * Created by yutakase on 2016/09/24.
 */
public class ServiceSimulation extends Simulation {

    private List<Actor> actors;
    private List<List<Integer>> bestPricesList = new ArrayList<>(ACTOR_COUNT);
    private List<List<Actor>> logList = new ArrayList<>(SIMULATION_COUNT);
    private List<List<List<Integer>>> pricesList = new ArrayList<>(BALANCE_PRICE_MAX_COUNT);

    private String saveActorFileName;

    public ServiceSimulation(String saveActorFileName) {
        // Actorのリスト生成
        this.actors = ActorUtil.createActors();
//        this.actors = ActorUtil.createTestActors();
        this.bestPricesList = Stream
                .generate((Supplier<ArrayList<Integer>>) ArrayList::new)
                .limit(ACTOR_COUNT)
                .collect(Collectors.toList());
        this.saveActorFileName = saveActorFileName;
    }

    @Override
    protected void init() {
    }

    @Override
    protected void close() {
        FileIO.writeActorLog(this.saveActorFileName, this.logList);
        FileIO.writePriceLog("price_" + this.saveActorFileName, this.pricesList);
        System.out.println("Save to " + saveActorFileName);
    }

    @Override
    protected void step() {
        System.out.println("count: " + this.getStepCount());

        // 各Actorのサービス交換可能なActorを更新
        this.actors.parallelStream().forEach(Actor::updateMarketActors);

        // 価格均衡ループ、最大BALANCE_PRICE_MAV_COUNT回
        IntStream.range(0, BALANCE_PRICE_MAX_COUNT).anyMatch(i -> {
            // 各Actor毎に価格ループ
            this.actors.parallelStream().forEach(actor -> {
                PriceSimulation priceSimulation = new PriceSimulation(actor);
                priceSimulation.mainLoop();
                priceSimulation.getBestPrices().ifPresent(bestPrices -> this.bestPricesList.set(actor.getId(), bestPrices));
            });

            // 各Actorの価格を更新
            this.actors.parallelStream().forEach(actor -> actor.setPricesAndCheckChangePrices(this.bestPricesList.get(actor.getId())));

            this.actors.stream().filter(Actor::isChangePrice).forEach(actor -> {
                System.out.print(i + " : " + actor.getId() + " " + actor.getPrices().toString() + " ");
            });
            System.out.println();
            this.pricesList.add(this.actors.stream().map(actor -> {
                List<Integer> prices = new ArrayList<>();
                prices.addAll(actor.getPrices());
                return prices;
            }).collect(Collectors.toList()));

            // すべての価格が変化していなければ終了
            return this.actors.parallelStream().allMatch(actor -> !actor.isChangePrice());
        });

        // サービス交換マッチング
        this.deferredAcceptance();

        // ログ生成
        List<Actor> log = actors.stream()
                .map(Actor::deepCopy)
                .collect(Collectors.toList());
        this.logList.add(log);

    }

    /**
     * DAアルゴリズム
     */
    public void deferredAcceptance() {
        // 選考生成
        this.actors.parallelStream().forEach(Actor::updateSelectProviderList);

        while (true) {
            // マッチングが決定していないActorは現在の第一希望のActorへプロポーズ
            this.actors.forEach(actor -> {
                IntStream.range(0, SERVICE_COUNT).filter(serviceId -> !actor.isMatch(serviceId)).forEach(serviceId -> {
                    int providerId = actor.popSelectProviderFirst(serviceId);
                    if (providerId != actor.getId() && providerId != -1) {
                        this.actors.get(providerId).addConsumersId(serviceId, actor.getId());
                    }
                });
            });

            // マッチング情報リセット
            this.actors.forEach(actor -> {
                IntStream.range(0, SERVICE_COUNT).forEach(serviceId -> {
                    actor.setIsMaches(serviceId, false);
                });
            });

            // 拒否 or Keep
            this.actors.forEach(Actor::updateConsumersLimit);
            this.actors.forEach(actor -> {
                IntStream.range(0, SERVICE_COUNT).forEach(serviceId -> {
                    actor.getConsumerActorIdList(serviceId).forEach(consumerActorId -> {
                        this.actors.get(consumerActorId).setIsMaches(serviceId, true);
                    });
                    if (actor.isSelf(serviceId)) {
                        actor.setIsMaches(serviceId, true);
                    }
                });
            });
            boolean isBreak = this.actors.stream().allMatch(actor -> IntStream.range(0, SERVICE_COUNT).allMatch(actor::isMatch));

            if (isBreak) {
                break;
            }
        }

    }

    @Override
    protected boolean isSimulationFinished() {
        return this.stepCount >= SIMULATION_COUNT;
    }
}
