package nsSimulation;

/*==============================================================================
This class is created to store output of a function which contains 2 values
==============================================================================*/

public class SpeedDistance {
    private int speed;
    private int distance;

    public SpeedDistance(int speed, int distance) {
        this.speed = speed;
        this.distance = distance;
    }

    public void setSpeed(int newSpeed) {
        speed = newSpeed;
    }
    
    public int getSpeed() {
        return speed;
    }

    public int getDistance() {
        return distance;
    }
}