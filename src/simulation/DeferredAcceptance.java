package simulation;

import model.Actor;

import java.util.List;
import java.util.stream.IntStream;

import static util.Const.SERVICE_COUNT;

/**
 * DAアルゴリズム
 * Created by yutakase on 2016/12/09.
 */
public final class DeferredAcceptance {
    private DeferredAcceptance() {
    }

    /**
     * actorsのサービス交換マッチング
     */
    public static void matching(List<Actor> actors) {
        // 選考生成
        actors.parallelStream()
                .forEach(Actor::updateSelectProviderList);

        boolean isAllMatched = false;
        while (!isAllMatched) {
            // マッチングが決定していないActorは現在の第一希望のActorへプロポーズ
            actors.forEach(actor ->
                    IntStream.range(0, SERVICE_COUNT)
                            .filter(serviceId -> !actor.isMatch(serviceId))
                            .forEach(serviceId -> {
                                int providerId = actor.popSelectedProviderId(serviceId);
                                actors.get(providerId).addConsumersId(serviceId, actor.getId());
                            })
            );

            // マッチング情報リセット
            actors.forEach(actor ->
                    IntStream.range(0, SERVICE_COUNT)
                            .forEach(serviceId -> actor.setIsMach(serviceId, false))
            );

            // 拒否 or Keep
            actors.forEach(Actor::limitAndUpdateConsumersId);

            // マッチング情報更新
            actors.forEach(providerActor ->
                    IntStream.range(0, SERVICE_COUNT)
                            .forEach(serviceId ->
                                    providerActor.getConsumerActorIdList(serviceId)
                                            .forEach(consumerActorId -> {
                                                Actor consumerActor = actors.get(consumerActorId);
                                                consumerActor.setProviderActorId(serviceId, providerActor.getId());
                                                consumerActor.setIsMach(serviceId, true);
                                            })
                            )

            );

            // すべてのマッチングが完了したかチェック
            isAllMatched = actors.parallelStream()
                    .allMatch(actor ->
                            IntStream.range(0, SERVICE_COUNT)
                                    .allMatch(actor::isMatch)
                    );
        }
    }
}
