package nsSimulation;

import java.awt.Color;
import java.util.Random;

public class SlowCarNS extends CarNS {

    /*
    Constructor
    Create SLOW car at specific position on a specific lane with speed limited by a given speed or its max speed
    The speed is equal or 1 unit less than the limit speed to make it more realistic
    Input:
        ID          ID of car, for debugging purpose
        lane        lane of car at the beginning (initialization)
        position    position of car at the beginning (initialization)
        limitSpeed  max speed at the beginning (initialization) - due to road situation, high density for example
    */
    public SlowCarNS(int ID, int lane, int position, int limitSpeed) {
        super(ID, lane, position);

        Random r = new Random();
        color = new Color(0, r.nextInt(130), 255);
        maxSpeed = TrafficSimulation.MAX_SPEED_SLOW_CAR;
        speed = Math.max(Math.min(maxSpeed, limitSpeed) - r.nextInt(2), 0);            // speed from 0 to [maxSpeed-1, maxSpeed]
    }
}
