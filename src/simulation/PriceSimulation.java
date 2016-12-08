package simulation;

import model.Actor;
import util.ActorUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static util.Const.*;

/**
 * Created by yutakase on 2016/12/05.
 */
public class PriceSimulation extends Simulation {

    private Actor hostActor;
    private int price = MIN_PRICE;
    private List<Integer> bestPrices;
    private List<Integer> bestPayoff;

    public PriceSimulation(Actor hostActor) {
        this.hostActor = hostActor;
    }

    @Override
    protected void init() {
        this.bestPrices = Stream
                .generate(() -> MIN_PRICE)
                .limit(SERVICE_COUNT)
                .collect(Collectors.toList());
        this.bestPayoff = Stream
                .generate(() -> 0)
                .limit(SERVICE_COUNT)
                .collect(Collectors.toList());
    }

    @Override
    protected void close() {

    }

    @Override
    protected void step() {
        // 各サービス
        IntStream.range(0, SERVICE_COUNT)
                .forEach(serviceId -> {
                    // 価格を変更したときの売却先Actorのリストを生成
                    Optional<List<Integer>> consumersIdListOptional = ActorUtil.countConsumerSimulate(this.hostActor, this.price, serviceId);
                    consumersIdListOptional.ifPresent(consumersIdList -> {
                        // 売却数制限
                        int consumerCount = (consumersIdList.size() < MAX_CONSUMERS) ? consumersIdList.size() : MAX_CONSUMERS;
                        // 売上計算
                        int payoff = consumerCount * this.price;
                        // 売上最大の価格に更新
                        if (payoff > bestPayoff.get(serviceId)) {
                            bestPrices.set(serviceId, this.price);
                            bestPayoff.set(serviceId, payoff);
                        }
                    });
                });
        this.price += DELTA_PRICE;
    }

    @Override
    protected boolean isSimulationFinished() {
        return this.price > MAX_PRICE;
    }

    public Optional<List<Integer>> getBestPrices() {
        return Optional.ofNullable(this.bestPrices);
    }
}
