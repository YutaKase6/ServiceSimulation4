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

import static util.Const.ACTOR_COUNT;
import static util.Const.SIMULATION_COUNT;

/**
 * Created by yutakase on 2016/09/24.
 */
public class ServiceSimulation extends Simulation {

    private List<Actor> actors;
    private List<List<Integer>> bestPricesList = new ArrayList<>(ACTOR_COUNT);
    private List<List<Actor>> logList = new ArrayList<>(SIMULATION_COUNT);
    private List<List<List<Integer>>> pricesList = new ArrayList<>(100);

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

        // 価格均衡ループ、最大100回回る
        IntStream.range(0, 100).anyMatch(i -> {
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

        // 各Actorのサービス購入先を決定
        this.actors.parallelStream().forEach(Actor::updateProviders);
        // 各Actorのサービス売却先を登録
        this.actors.parallelStream().forEach(Actor::updateConsumers);

        // ログ生成
        List<Actor> log = actors.stream()
                .map(Actor::deepCopy)
                .collect(Collectors.toList());
        this.logList.add(log);

    }

    @Override
    protected boolean isSimulationFinished() {
        return this.stepCount >= SIMULATION_COUNT;
    }
}
