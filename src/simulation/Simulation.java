package simulation;

/**
 * Created by yutakase on 2016/06/08.
 */
public abstract class Simulation {

    protected int stepCount = 0;

    public void mainLoop() {
        stepCount = 0;
        init();
        while (!isSimulationFinished()) {
            step();
            stepCount++;
        }
        close();
    }

    protected abstract void init();

    protected abstract void close();

    protected abstract void step();

    protected abstract boolean isSimulationFinished();

    protected void countStep() {
        stepCount++;
    }

    protected int getStepCount() {
        return stepCount;
    }

}
