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

        // 能力上昇
        if (this.getStepCount() != 0) {
            this.actors.parallelStream().forEach(Actor::growthCapability);
        }

        // 各Actorのサービス交換可能なActorを更新
        this.actors.parallelStream()
                .forEach(Actor::updateMarketActors);

        // 価格均衡ループ、最大BALANCE_PRICE_MAV_COUNT回
        IntStream.range(0, BALANCE_PRICE_MAX_COUNT)
                .anyMatch(i -> {
                    // 各Actor毎に価格ループ
                    this.actors.parallelStream().forEach(actor -> {
                        // 売上最大となる価格をシミュレーション
                        PriceSimulation priceSimulation = new PriceSimulation(actor);
                        priceSimulation.mainLoop();
                        // 売上最大の価格を更新
                        priceSimulation.getBestPrices().ifPresent(bestPrices -> this.bestPricesList.set(actor.getId(), bestPrices));
                    });

                    // 各Actorの価格を更新
                    this.actors.parallelStream().forEach(actor -> {
                        List<Integer> newPrices = this.bestPricesList.get(actor.getId());
                        // 価格が前Stepと変化したか判定
                        actor.checkChangePrices(newPrices);
                        actor.setPrices(newPrices);
                    });

                    // 価格が変動している様子を表示
                    this.actors.stream().filter(Actor::isChangePrice).forEach(actor -> {
                        System.out.print(i + " : " + actor.getId() + " " + actor.getPrices().toString() + " ");
                    });
                    System.out.println();

                    // 価格が均衡していく仮定を保存
                    List<List<Integer>> pricesList = this.actors.stream()
                            .map(actor -> {
                                List<Integer> prices = new ArrayList<>();
                                prices.addAll(actor.getPrices());
                                return prices;
                            })
                            .collect(Collectors.toList());
                    this.pricesList.add(pricesList);

                    // すべての価格が変化していなければ終了
                    return this.actors.parallelStream().allMatch(actor -> !actor.isChangePrice());
                });

        // サービス交換マッチング
        DeferredAcceptance.matching(this.actors);

        // ログ生成
        List<Actor> log = this.actors.stream()
                .map(Actor::deepCopy)
                .collect(Collectors.toList());
        this.logList.add(log);
    }


    @Override
    protected boolean isSimulationFinished() {
        return this.stepCount >= SIMULATION_COUNT;
    }
}
