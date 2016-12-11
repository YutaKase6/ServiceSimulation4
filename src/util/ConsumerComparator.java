package util;

import model.Actor;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// 売却先比較
public class ConsumerComparator implements Comparator<Integer>, Serializable {

    private Actor hostActor;
    private List<Actor> actors;
    private int serviceId;

    public ConsumerComparator(Actor hostActor, int serviceId) {
        this.hostActor = hostActor;
        this.serviceId = serviceId;
        ActorUtil.getActors().ifPresent(actors1 -> actors = actors1);
    }

    @Override
    public int compare(Integer id1, Integer id2) {
        if (Optional.ofNullable(actors).isPresent()) {
            Actor consumer1 = actors.get(id1);
            Actor consumer2 = actors.get(id2);
            double profit1 = ActorUtil.calcProfit(hostActor, consumer1, serviceId);
            double profit2 = ActorUtil.calcProfit(hostActor, consumer2, serviceId);
            return Double.compare(profit1, profit2);
        }
        return 0;
    }
}
