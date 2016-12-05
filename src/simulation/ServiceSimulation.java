package simulation;

import model.Actor;
import util.ActorUtil;
import util.FileIO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static util.Const.SIMULATION_COUNT;

/**
 * Created by yutakase on 2016/09/24.
 */
public class ServiceSimulation extends Simulation {

    private List<Actor> actors;
    private List<List<Actor>> logList = new ArrayList<>(SIMULATION_COUNT);

    private String saveFileName;

    public ServiceSimulation(String saveFileName) {
        // Actorのリスト生成
        this.actors = ActorUtil.createActors();
//        this.actors = ActorUtil.createTestActors();
        this.saveFileName = saveFileName;
    }

    @Override
    protected void init() {
    }

    @Override
    protected void close() {
        FileIO.writeActorLog(this.saveFileName, this.logList);
        System.out.println("Save to " + saveFileName);
    }

    @Override
    protected void step() {
        System.out.println("count: " + this.getStepCount());

        // 各Actorのサービス交換可能なActorを更新
        this.actors.forEach(Actor::updateMarketActors);
        // 各Actorのサービス購入先を決定
        this.actors.forEach(Actor::updateProviders);
        // 各Actorのサービス売却先を登録
        this.actors.forEach(Actor::updateConsumers);

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
