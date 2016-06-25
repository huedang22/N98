package nsSimulation;

import java.awt.*;
import java.util.Random;

public class BrokenCarNS extends CarNS {

    private final double breakDownProb = TrafficSimulation.BREAKING_DOWN_PROBABILITY;
    private final double getFixedProb = TrafficSimulation.GETTING_REPAIRED_PROBABILITY;
    private boolean isBrokenDown;
    private Random r;

    /**
    Create BROKEN car at a specific position on a specific lane with speed limited by a given speed or its max speed
    The speed is equal or 1 unit less than the limit speed to make it more realistic

    Input:
        ID          ID of car, for debugging purpose
        lane        lane of car at the beginning 
        position    position of car at the beginning 
        limitSpeed  max speed at the beginning - due to road situation, high density for example
     */
    public BrokenCarNS(int ID, int lane, int position, int limitSpeed) {
        super(ID, lane, position); // calls the parent constructor
        r = new Random();

        maxSpeed = TrafficSimulation.MAX_SPEED_SLOW_CAR;
        speed = Math.max(Math.min(maxSpeed, limitSpeed) - r.nextInt(2), 0);            // speed is from 0 to [maxSpeed-1, maxSpeed]                
        isBrokenDown = false;
        color = new Color(0, 255, 0);
    }

    @Override
    public int adaptSpeed(SpeedDistance carFront, SpeedDistance carFrontNextLane, SpeedDistance carBehindNextLane){
            
        if (isBrokenDown) { // if it broke decelerate by 1 until it stops
            speed = speed == 0 ? 0 : speed - 1;
        } else speed = super.adaptSpeed(carFront, carFrontNextLane, carBehindNextLane);

        float rand = r.nextFloat();
        if (isBrokenDown) {
            if (getFixedProb > 0 && rand > 1 - getFixedProb) // car gets fixed with a small probability
            isBrokenDown = false;
        } else {
            if (rand < breakDownProb) // car breaks down with a small probability
            isBrokenDown = true;
        }      
        
        setMaxReachedSpeed(speed);
        return speed;
    }    
}
