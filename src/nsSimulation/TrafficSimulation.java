package nsSimulation;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

public class TrafficSimulation {

    public static int DEBUG = 1;                          // level of information to print out, 1 only important info, 5 medium, 10 details
    
    // GLOBAL CONSTANTS ////////////////////////////////////////////////////////
    
    // SIMULATION DETAILS
    public static int SIMULATION_STEP_COOLDOWN = 0;           // time delay between update steps, for animation purpose
    public static final int CAR_WIDTH = 10;

    // BROKEN CAR CONSTANTS 
    public static double BREAKING_DOWN_PROBABILITY = 0.3;
    public static final double GETTING_REPAIRED_PROBABILITY = 0;

    // MISCELLANEOUS
    public static final int GLOBAL_MAXIMUM_DECELERATION = 2;
    public static final int GLOBAL_MINIMUM_DECELERATION = 1;
    public static final int GLOBAL_MAXIMUM_ACCELERATION = 2;
    ////////////////////////////////////////////////////////////////////////////

    // CONFIGURATIONS //////////////////////////////////////////////////////////
    public static double DENSITY;
    public static double FAST_CAR_RATIO; // fast/total cars
    public static int NUMBER_OF_ITERATIONS;
    public static boolean GLOBAL_SPEED_RULE = false;
    public static int GLOBAL_MAX_SPEED;

    // parameter of NS model
    public static int MAX_ACCELERATION = 1;               // default is 1 in NS
    public static int MAX_SPEED_FAST_CAR = 5;
    public static int MAX_SPEED_SLOW_CAR = 3;
    public static double PROBABILITY_FLUCTUATION = .25;
    public static int DISTANCE_TO_LOOK_AHEAD = 7;
    public static int SLACK = 3;
    public static boolean APPLY_SYMMETRIC_RULE = true;

    // 
    public static int ROAD_SIZE = 0;                      // number of cells
    public static int NUM_FAST_CARS = 0;
    public static int NUM_SLOW_CARS = 0;
    public static boolean HAS_BROKEN_CAR = false;


    ////////////////////////////////////////////////////////////////////////////
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {

        //Don't make this value zero, or it'll crash
        NUMBER_OF_ITERATIONS = 3600;    // coresponding to 1 hour

        boolean TEST_MODEL;
        
        TEST_MODEL = false;
        
        if (TEST_MODEL)
            testModel();                // test the model
        else
            getStatisticalData();       // run to get statistical data to compare with our model
    }

    
    /*
    You can change values of variables in this function to test the model
    TrafficSimulation.DEBUG     should be 1 to avoid redundant information in the output
    TrafficSimulation.SIMULATION_STEP_COOLDOWN      0 to disable animation, otherwise which is the delay in ms in updating animation scene
    
    roadLength                  length of the observation road segment in km, it was 75 km in the paper
    
    Several combinations of parameters' values which were used in the paper have been set. 
    To test a combination, uncomment the combination and comment out the others.
    */
    public static void testModel() throws IOException{      
        NUMBER_OF_ITERATIONS = 3600;        // coresponding to 1 hour
        
        TrafficSimulation.DEBUG = 1;                                            // show only important information
        TrafficSimulation.SIMULATION_STEP_COOLDOWN = 0;                         // turn on animation by setting this value different from 0
        
        // parameters related to road segment
        
        double cellLength = 7.5;                    // in meter
        double roadLength = 75;                     // in km, should be a multiple of cellLength 
        ROAD_SIZE = (int) (roadLength * 1000 / cellLength);                     // number of cells
        MAX_ACCELERATION = 1;                       // consider changing this value if cell length is modified
        System.out.println("Test model: ROAD_SIZE = " + ROAD_SIZE + " cells \n");

        // to use in the same scale with our model, set cellLength = 3, 
        // consider changing the MAX_ACCELERATION, masSpeedsFast, maxSpeedsSlow as well
        
        
        // parameters for NS model        

//        // Use the following combinations for section VI, ABC, fig2 in the paper
//        String situation = "section VI, ABC, Fig. 2";
//        APPLY_SYMMETRIC_RULE = false;
//        int[] arrDistanceLookAhead = {16};
//        int[] slacks = {0};
//        double[] trafficDensities = {40, 80, 120, 160, 200};                // number of cars per km per 2 lanes
//        double[] fastCarRatios = {1};                                       // number of fast cars over total cars
//        boolean[] brokenCar = {false};                                      // having broken car or not

        
//        // Use the following combinations for section VIII, A, fig4 in the paper
//        String situation = "section VIII, A, Fig. 4";
//        APPLY_SYMMETRIC_RULE = false;
//        int[] arrDistanceLookAhead = {7};
//        int[] slacks = {3};
//        double[] trafficDensities = {40, 80, 120, 160, 200};                // number of cars per km per 2 lanes
//        double[] fastCarRatios = {1};                                       // number of fast cars over total cars
//        boolean[] brokenCar = {false};                                      // having broken car or not


        // Use the following combinations for section VIII, D, fig7 in the paper
        String situation = "section VIII, D, Fig. 7";
        APPLY_SYMMETRIC_RULE = true;
        int[] arrDistanceLookAhead = {7};
        int[] slacks = {3};
        double[] trafficDensities = {40, 80, 120, 160, 200};                // number of cars per km per 2 lanes
        double[] fastCarRatios = {.9};                                      // number of fast cars over total cars
        boolean[] brokenCar = {false};                                      // having broken car or not

//        // Use the following combinations for your own study
//        String situation = "Not in paper";
//        APPLY_SYMMETRIC_RULE = true;
//        int[] arrDistanceLookAhead = {7};
//        int[] slacks = {3};
//        double[] trafficDensities = {40, 80, 120, 160, 200};                // number of cars per km per 2 lanes
//        double[] fastCarRatios = {.9};                                      // number of fast cars over total cars
//        boolean[] brokenCar = {true};                                      // having broken car or not
        
        int numRepetitions = 5;        
        int totalCars;
        long startTime;

        AnimatedSimulation simulation = new AnimatedSimulation();

        System.out.println(situation + "\n");

        for (boolean broken : brokenCar) {
            for (double density : trafficDensities) {
                for (double ratio : fastCarRatios) {
                    for (int distance : arrDistanceLookAhead) {
                        for (int slack : slacks) {
                            DENSITY = density;
                            totalCars = (int) (roadLength * density);
                            NUM_FAST_CARS = (int) (ratio * totalCars);
                            NUM_SLOW_CARS = totalCars - NUM_FAST_CARS;

                            DISTANCE_TO_LOOK_AHEAD = distance;
                            SLACK = slack;

                            HAS_BROKEN_CAR = broken;
                            if (HAS_BROKEN_CAR) {
                                NUM_SLOW_CARS++;   // broken car is counted in number of slow cars
                            }

                            FAST_CAR_RATIO = ratio;
                            BREAKING_DOWN_PROBABILITY = broken ? 0.3 : 0.0;

                            startTime = System.nanoTime();
                            System.out.println("Density = " + density + " Fast car ratio = " + ratio + " Having broken car = " + broken
                                    + " Distance look ahead = " + distance + " Slack=" + slack + " Symmetry = " + APPLY_SYMMETRIC_RULE + "\n");
                            for (int rep=0; rep<numRepetitions; rep++){
                                simulation.initialiseSimulation(NUMBER_OF_ITERATIONS);
                                simulation.runSimulation(rep);
                                System.out.println();
                            }
                            System.out.println("Running one set simulation: " + (System.nanoTime() - startTime) / Math.pow(10, 9) + " seconds\n");
                            System.out.println();
                        }
                    }
                }
            }
        }
    }
    
    private static Boolean getStatisticalData() throws FileNotFoundException, UnsupportedEncodingException, IOException{
        TrafficSimulation.DEBUG = 1;                                            // show only important information
        TrafficSimulation.SIMULATION_STEP_COOLDOWN = 0;                         // turn on animation by setting this value different from 0
        
        // parameters related to road segment        
        double cellLength = 7.5;                    // in meter
        double roadLength = 7.5;                     // in km, should be a multiple of cellLength 
        ROAD_SIZE = (int) (roadLength * 1000 / cellLength);                     // number of cells
        MAX_ACCELERATION = 1;                       // consider changing this value if cell length is modified
        System.out.println("getStatisticalData: ROAD_SIZE = " + ROAD_SIZE + " cells \n");

        // use model in section VIII, D in the paper
        APPLY_SYMMETRIC_RULE = true;
        int[] arrDistanceLookAhead = {7};
        int[] slacks = {3};
        
        int numRepetitions = 5;                     // repeat each model xxx times
        int totalCars;
        long startTime;

        GLOBAL_SPEED_RULE = false;
        String filename = "simulations.csv";        
        Boolean success;
        AnimatedSimulation simulation = new AnimatedSimulation();

        double[] trafficDensities = {0.05, 0.1, 0.15, .3, .4};
        double[] fastCarRatios = {0, 0.25, 0.50, 0.75, 1.0};
        int[] maxSpeedsSlow = {3, 6, 9};
        int[] maxSpeedsFast = {4, 8, 11};
        boolean[] globalRules = {false};
        boolean[] brokenCar = {true, false};

        PrintWriter writer;
        try {
            writer = new PrintWriter(filename, "UTF-8");
            writer.println("model, road_block, max_speed_slow, max_speed_fast, fast_car_ratio, density, total_all_cars_distance, total_slow_cars_distance, total_fast_cars_distance, worst_case_distance_slow_cars, worst_cast_distance_fast_cars, best_case_distance_slow_car, best_case_distance_fast_car,num_slow_cars,num_fast_cars,global_speed_rule,speed_slow,speed_fast,repetition,slack,distance_look_ahead");
            for (int distance : arrDistanceLookAhead) {
                for (int slack : slacks) {
                    DISTANCE_TO_LOOK_AHEAD = distance;
                    SLACK = slack;
                    for (double density : trafficDensities) {
                        for (double ratio : fastCarRatios) {
                            for (int slow : maxSpeedsSlow) {
                                for (int fast : maxSpeedsFast) {
                                    for (boolean global : globalRules) {
                                        for (boolean broken : brokenCar) {
                                            if (slow <= fast) {
                                                DENSITY = density;
                                                totalCars = (int) (ROAD_SIZE * density);
                                                NUM_FAST_CARS = (int) (ratio * totalCars);
                                                NUM_SLOW_CARS = totalCars - NUM_FAST_CARS;
                                                FAST_CAR_RATIO = ratio;
                                                GLOBAL_SPEED_RULE = global;
                                                if (global) {
                                                    GLOBAL_MAX_SPEED = (int) (0.75 * slow);
                                                }
                                                MAX_SPEED_SLOW_CAR = slow;
                                                MAX_SPEED_FAST_CAR = fast;
                                                HAS_BROKEN_CAR = broken;
                                                if (HAS_BROKEN_CAR) {
                                                    NUM_SLOW_CARS++;   // broken car is counted in number of slow cars
                                                }
                                                BREAKING_DOWN_PROBABILITY = broken ? 0.3 : 0.0;
                                                startTime = System.nanoTime();
                                                for (int repetition = 0; repetition < numRepetitions; repetition++) {
                                                    simulation.initialiseSimulation(NUMBER_OF_ITERATIONS);
                                                    writer.println(simulation.runSimulation(repetition));
                                                }
                                                System.out.println("Running one set simulation: " + (System.nanoTime() - startTime) / Math.pow(10, 9) + " seconds\n");
                                                System.out.println();
                                            }
                                            writer.flush();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            writer.close();
            success = true;
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            success = false;
        }
        return success;
        
    }
    
}
