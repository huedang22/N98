package nsSimulation;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;


/*==============================================================================
Implement NS model based on paper Two-lane traffic rules for cellular Automata
*==============================================================================*/

public class RoadNS {

    public static int NUM_LANES = 2;
    public static int RIGHT_LANE = 1;
    public static int LEFT_LANE = 2;
    
    public static int NUM_TYPE_CAR = 2;
    public static int TYPE_CAR_SLOW = 1;
    public static int TYPE_CAR_FAST = 2;
    
    private int numCarsPassingEnd = 0;                // number of cars passing the end of the segment to verify with result in the paper
            
    private ArrayList<CarNS> cars;                // contains cars on the road

    private int[] rightLane;                    // values: current speed of car (or -1 if no car)
    private int[] leftLane;

    private int[] helperRight;
    private int[] helperLeft;
    
    
    public RoadNS(){
        
        // Initialise variables
        cars = new ArrayList<>();
        rightLane = new int[TrafficSimulation.ROAD_SIZE];
        leftLane = new int[TrafficSimulation.ROAD_SIZE];

        helperRight = new int[TrafficSimulation.ROAD_SIZE];
        helperLeft = new int[TrafficSimulation.ROAD_SIZE];

        for (int i = 0; i < rightLane.length; i++) {
            rightLane[i] = -1;
            leftLane[i] = -1;
        }
        
        generateCars();
    }

    
    /*==============================================================================
    Generate cars for the model at the beginning
    - Cars are randomly scattered over the road segment, equally on two lanes
    - Speed of car are randomly generated
    *==============================================================================*/
    public void generateCars(){
        
        // randomly generate position for cars
        Integer[] positionLeft=new Integer[TrafficSimulation.ROAD_SIZE];
        Integer[] positionRight=new Integer[TrafficSimulation.ROAD_SIZE];
        for (int i=0; i<TrafficSimulation.ROAD_SIZE; i++) {
            positionLeft[i] = i;
            positionRight[i] = i;
        }
        Collections.shuffle(Arrays.asList(positionLeft));       // permute the positions and select the first number of positions from the list
        Collections.shuffle(Arrays.asList(positionRight));
        
        // assume lane usage is equal
        int totalCars = TrafficSimulation.NUM_FAST_CARS + TrafficSimulation.NUM_SLOW_CARS;
        int limitNumCarLeftLane = (int)(totalCars/2);
        int limitNumCarRightLane = totalCars - limitNumCarLeftLane;   // may have 1 car more than the left lane
        
       // variables to keep track information of the generation process
        int numCarRightLane_generated=0, numCarLeftLane_generated=0, posIndexLeftLane=0, posIndexRightLane=0;        
        int currentPosition = 0, slow_generated = 0, fast_generated = 0, lane, type_of_car;
        CarNS tmpC;
        Random r = new Random();           
        
        boolean createdBrokenCar = ! TrafficSimulation.HAS_BROKEN_CAR;      // to create or not the broken car
        
        if (TrafficSimulation.DEBUG >= 5) 
            System.out.println("limitLeftLane = " + limitNumCarLeftLane + " limitRightLane = " + limitNumCarRightLane);
        
        for (int i = 0; i < totalCars; i++) {
            
            // randomly choose the lane (unless the limit is reached)
            if (numCarRightLane_generated >= limitNumCarRightLane)              // limit is reached
                lane = LEFT_LANE;
            else if (numCarLeftLane_generated >= limitNumCarLeftLane)           // limit is reached
                lane = RIGHT_LANE;            
            else lane = r.nextInt(NUM_LANES) + 1;                               // randomly select
            
            // retrieve position for the (soon to be generated) car to be placed
            if (lane == RIGHT_LANE) {
                currentPosition = positionRight[posIndexRightLane];
                posIndexRightLane++;
            }
            else {
                currentPosition = positionLeft[posIndexLeftLane];
                posIndexLeftLane++;
            }            
            
            // randomly choose the type of car (unless the limit is reached)
            if (slow_generated == TrafficSimulation.NUM_SLOW_CARS)              // limit is reached
                type_of_car = TYPE_CAR_FAST;
            else if (fast_generated == TrafficSimulation.NUM_FAST_CARS)         // limit is reached
                type_of_car = TYPE_CAR_SLOW;
            else type_of_car = r.nextInt(NUM_TYPE_CAR) + 1;                     // randomly select

            // generate the car and add it to the list of cars
            if (type_of_car == TYPE_CAR_SLOW) {                
//                if (!createdBrokenCar && TrafficSimulation.NUM_SLOW_CARS <= 4*(slow_generated+1)){
                if (!createdBrokenCar){
                    createdBrokenCar = true;                                    // create 1 only, so turn it off for the rest
                    tmpC = new BrokenCarNS(i, lane, currentPosition, TrafficSimulation.MAX_SPEED_FAST_CAR); // broken car
                    slow_generated++;
                } else {
                    tmpC = new SlowCarNS(i, lane, currentPosition, TrafficSimulation.MAX_SPEED_FAST_CAR);   // slow car
                    slow_generated++;                    
                }                
            } else {
                tmpC = new FastCarNS(i, lane, currentPosition, TrafficSimulation.MAX_SPEED_FAST_CAR);
                fast_generated++;
            }
            cars.add(tmpC);

            // save data to the road structure (lanes)
            if (lane == RIGHT_LANE) {
                rightLane[currentPosition] = tmpC.getSpeed();
                numCarRightLane_generated++;
            }
            else {
                leftLane[currentPosition] = tmpC.getSpeed();
                numCarLeftLane_generated++;
            }
        }
        
        if (TrafficSimulation.DEBUG >= 5) 
            System.out.println("LeftLane_generated = " + numCarLeftLane_generated + " RightLane_generated = " + numCarRightLane_generated);
        
        if (TrafficSimulation.DEBUG>=5){
            System.out.println("Left lane\n" + Arrays.toString(leftLane) + "\n");        
            System.out.println("Right lane\n" + Arrays.toString(rightLane) + "\n");
        }
    }

    public void nextState() {
        // CALCULATE NEW STATE /////////////////////////////////////////////////
        // clear helper lanes
        for (int i = 0; i < helperRight.length; i++) {
            helperRight[i] = -1;
            helperLeft[i] = -1;
        }

        // move cars (check rules on current road and save new positions in next road)
        for (CarNS car : cars) {
            moveCar(car);
        }

        // END OF CALCULATE NEW STATE //////////////////////////////////////////
        //
        // set new state
        rightLane = helperRight.clone();
        leftLane = helperLeft.clone();
    }

    /*
     * Prints the current state of the road to the console.
     */
    public void printTrafficSituation() {
        String traffic_rightLane = "|", traffic_leftLane = "|";

        for (int i = 0; i < rightLane.length; i++) {
            traffic_rightLane += toSymbol(rightLane[i]);
            traffic_leftLane += toSymbol(leftLane[i]);
        }

        traffic_rightLane += "|";
        traffic_leftLane += "|";

        System.out.println(traffic_leftLane + "\n" + traffic_rightLane + "\n");
    }
    
    /*==========================================================================
    get total traveled distance of all cars so far, except the broken one
    ==========================================================================*/
    public int getTotalTraveledDistance(){
        int totalDistance = 0;
        for (CarNS c : cars) {
            if (!c.getType().equals('E'))
                totalDistance += c.getTraveledDistance();
        }
        return totalDistance;
    }

    
    /*==========================================================================
    get number of cars which have passed the end of road segment so far
    ==========================================================================*/
    public int getNumCarsPassingEnd(){
        return numCarsPassingEnd;
    }
    
    /*
     *
     * @return The list of cars in the road.
     */
    public ArrayList<CarNS> getCars() {
        return cars;
    }

    /*
     * Helper method for printTrafficSituation(). Converts speeds with more than
     * 2 digits to characters (Hex encoding). Does not change 1 digit speeds. If
     * the speed is -1 it returns an underscore.
     * @param speed An integer speed or -1 if no car is present.
     * @return A character that represents the input speed.
     */
    private char toSymbol(int speed) {
        if (speed >= 0 && speed <= 9) {
            return Character.forDigit(speed, 10);
        } else {
            switch (speed) {
                case -1:
                    return '_';
                case 10:
                    return 'A';
                case 11:
                    return 'B';
                case 12:
                    return 'C';

                case 13:
                    return 'D';
                case 14:
                    return 'E';
                case 15:
                    return 'F';
                default:
                    return '?';
            }
        }
    }

    /*
    find the speed of the car in front on a given lane and the distance with it from a given position
    Input:
        lane        the lane where the car in front will be checked
        position    the position where the current car is supposed to be
    Output:
        a variable which contain the speed of the car in front, and the number of cells in between from that car's position to the given position
        if there is no such car, speed will be max_speed of the system, distance = infinitive
    */
    private SpeedDistance getStatusWithFrontCar(int lane, int position) {
        int speed = 0, distance = 0;
        int[] arr;
        
        if (lane==RIGHT_LANE) arr = rightLane;
        else arr = leftLane;
            
        for (int i = position + 1; i < TrafficSimulation.ROAD_SIZE; i++) {
            if (arr[i] != -1){
                speed = arr[i];
                distance = i - position - 1;
                return new SpeedDistance(speed, distance);
            }          
        }

        // the consideration car is at the end of the road, therefore continue searching from the beginning of the lane
        for (int i = 0; i < TrafficSimulation.ROAD_SIZE; i++) {
            if (arr[i] != -1){
                speed = arr[i];
                distance = i + TrafficSimulation.ROAD_SIZE - position - 1;
                return new SpeedDistance(speed, distance);              
            }
        }
        
        return new SpeedDistance(TrafficSimulation.MAX_SPEED_FAST_CAR, Integer.MAX_VALUE);  // no car
    }

    /*
    find the speed of the car behind on a given lane and the distance with it from a given position
    Input:
        lane        the lane where the car behind will be checked
        position    the position where the current car is supposed to be
    Output:
        a variable which contain the speed of the car behind, and the number of cells in between from that car's position to the given position
        if there is no such car, speed will be max_speed of the system, distance = infinitive
    */
    private SpeedDistance getStatusWithBehindCar(int lane, int position) {
        int speed = 0, distance = 0;
        int[] arr;
        
        if (lane==RIGHT_LANE) arr = rightLane;
        else arr = leftLane;
            
        for (int i = position - 1; i >= 0; i--) {
            if (arr[i] != -1){
                speed = arr[i];
                distance = position - i - 1;
                return new SpeedDistance(speed, distance);
            }          
        }

        // the consideration car is at the begining of the road, therefore continue searching from the end of the lane
        for (int i = TrafficSimulation.ROAD_SIZE-1; i >= 0; i--) {
            if (arr[i] != -1){
                speed = arr[i];
                distance = position + TrafficSimulation.ROAD_SIZE - i - 1;
                return new SpeedDistance(speed, distance);              
            }
        }
        
        return new SpeedDistance(TrafficSimulation.MAX_SPEED_FAST_CAR, Integer.MAX_VALUE);  // no car
    }
    
    /*
    Update step for a specific car
    The car will check rules to change lane and move forward
    The status of road is then updated
    
    Input: 
        car     the car in consideration
    */
    private void moveCar(CarNS car) {
        int lane = car.getLane();
        int position = car.getPosition();
        int speed = car.getSpeed();
        int otherLane = Math.floorMod(2*lane,3);

        // get neighbors information
        SpeedDistance withCarFront = getStatusWithFrontCar(lane, position);
        SpeedDistance withCarFrontNextLane = getStatusWithFrontCar(otherLane, position-1);
        SpeedDistance withCarBehindNextLane = getStatusWithBehindCar(otherLane, position+1);
        
        // apply rules
        int newSpeed = car.adaptSpeed(withCarFront, withCarFrontNextLane, withCarBehindNextLane);

        // counting for statistical purpose
        if (position + newSpeed >= TrafficSimulation.ROAD_SIZE) numCarsPassingEnd += 1;
        
        // update road 
        int newPosition = Math.floorMod(position + newSpeed, TrafficSimulation.ROAD_SIZE);
        int newLane = car.lane;
        
        car.setPosition(newPosition);       //new speed, lane are already updated during the call to adaptSpeed();

        if (TrafficSimulation.DEBUG >=20 ) 
            System.out.println("Car " + car.getID() + " old speed " + speed + " new speed " + newSpeed + " old lane " + lane + " new lane " + newLane + "\n");
        
        if (newLane == LEFT_LANE) {
            helperLeft[newPosition] = newSpeed;
        } else {
            helperRight[newPosition] = newSpeed;
        }        
    }
    
}
