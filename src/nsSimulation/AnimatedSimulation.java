package nsSimulation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

final public class AnimatedSimulation {

    private RoadNS road;
    private static final int cooldown = TrafficSimulation.SIMULATION_STEP_COOLDOWN; //cooldown between steps of the simulation
    private final int carWidth = TrafficSimulation.CAR_WIDTH;
    private final int carHeight = 10;
    private int numRuns;
    
    private int numIterations;
    
    private JFrame frame;
    private DrawPanel drawPanel;

    /**
     * This method initialises and performs the simulation.
     */
    public void initialiseSimulation(int numIterations) {
        this.numIterations = numIterations;
        
        road = new RoadNS();
        
        // set window title and stop running if X is pressed
        frame = new JFrame("Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        if (TrafficSimulation.SIMULATION_STEP_COOLDOWN > 0){
            // create a panel that will contain the painting
            drawPanel = new DrawPanel();
            drawPanel.setPreferredSize(new Dimension(TrafficSimulation.ROAD_SIZE * carWidth, 300));

            // create a panel that makes the scrollbars appear
            JScrollPane jsp = new JScrollPane(drawPanel);
            // put the painting panel inside the scrollable panel
            jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

            // put everything on the frame
            frame.getContentPane().add(BorderLayout.CENTER, jsp);
            frame.setResizable(true);
            frame.setSize(2000, 400);
            frame.setLocationByPlatform(true);
            frame.setVisible(true);
        }
    }

    /**
     * Continuously calculates and presents the next state.
     * @param numberOfIterations The number of states the simulation will run
     * for. If 0, then it never stops running.
     */
    public String runSimulation(int repetition) throws IOException {
        numRuns = 0;

        while (numIterations == 0 || numRuns < numIterations) {
            numRuns++;

            road.nextState(); // calculates the next state
            frame.repaint(); // calls paintComponent(g) to draw the new state
            
            if (TrafficSimulation.DEBUG>=10)
                road.printTrafficSituation();

            // print the current flow every 100 iterations
            if (TrafficSimulation.DEBUG>=5 && numRuns % 100 == 0)
                System.out.println("Iterations=" + numRuns + ": Traveled distance=" + road.getTotalTraveledDistance() + 
                        " NumCarsPassingEnd=" + road.getNumCarsPassingEnd());

            try {
                Thread.sleep(cooldown);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (TrafficSimulation.DEBUG == 1){
            System.out.println("Num Iterations = " + numRuns + " Flow = " + road.getNumCarsPassingEnd());
//            System.out.println("Iterations=" + numRuns + ": Traveled distance=" + road.getTotalTraveledDistance() + 
//                    " NumCarsPassingEnd=" + road.getNumCarsPassingEnd());
        }

        return calculateMeasures(repetition);
    }

    /**
     * Helper panel used to draw the animation. Translates the current data
     * about the cars on the road to an image.
     */
    private class DrawPanel extends JPanel {

        public void paintComponent(Graphics g) {
            setBackground(new Color(240, 240, 185));
            g.setColor(Color.BLACK);

            if (TrafficSimulation.SIMULATION_STEP_COOLDOWN > 0) {

                //Draw the road
                g.fillRect(0, 100, TrafficSimulation.ROAD_SIZE * carWidth, carHeight * 2 + 10); // xpos, ypos, width, height

                //Draw the line that separates the two lanes.
                g.setColor(Color.white);
                for (int i = 0; i < TrafficSimulation.ROAD_SIZE * carWidth / 20; i++) {
                    g.drawLine(i * 20, 100 + carHeight + 5, i * 20 + 10, 100 + carHeight + 5);
                }

                //Draw all cars as color filled round rectangles.
                for (CarNS c : road.getCars()) {
                    g.setColor(c.getColor()); // individual colour for each car
                    if (c.getLane() == 1) {
                        g.fillRoundRect(c.getPosition() * carWidth, 117, carWidth - 5, carHeight, 2, 2);
                    }
                    if (c.getLane() == 2) {
                        g.fillRoundRect(c.getPosition() * carWidth, 103, carWidth - 5, carHeight, 2, 2);
                    }
                }
            }
        }
    }
    
    private String calculateMeasures(int repetition) {
        //Flow is measures in number of cars passing a certain point.
        //Equivalently: Sum over all cars: number of cells traveled / road size
        int totalDistance = 0;
        int totalSlowDistance = 0;
        int totalFastDistance = 0;
        
        int maxSpeedSlow = -1;
        int maxSpeedFast = -1;
        
        int bestFlowFast = -1;
        int bestFlowSlow = -1;
        int worstFlowFast = 999999999;
        int worstFlowSlow = 999999999;
        
        int numSlow = 0;
        int numFast = 0;
        
        for (CarNS c : road.getCars()) {
            String type = c.getType();
            
            if (type.equals("S")) {
                numSlow++;
                bestFlowSlow = c.getTraveledDistance() > bestFlowSlow ? c.getTraveledDistance() : bestFlowSlow;
                worstFlowSlow = c.getTraveledDistance() < worstFlowSlow ? c.getTraveledDistance() : worstFlowSlow;
                
                totalSlowDistance += c.getTraveledDistance();
                
                maxSpeedSlow = c.getMaxReachedSpeed() > maxSpeedSlow ? c.getMaxReachedSpeed() : maxSpeedSlow;
                
            } else if (type.equals("F")) {
                numFast++;
                bestFlowFast = c.getTraveledDistance() > bestFlowFast ? c.getTraveledDistance() : bestFlowFast;
                worstFlowFast = c.getTraveledDistance() < worstFlowFast ? c.getTraveledDistance() : worstFlowFast;
                
                totalFastDistance += c.getTraveledDistance();
                
                maxSpeedFast = c.getMaxReachedSpeed() > maxSpeedFast ? c.getMaxReachedSpeed() : maxSpeedFast;
            }
            
        }
        totalDistance = totalSlowDistance + totalFastDistance;  // do not count that of broken car

        //model, road_block, max_speed_slow, max_speed_fast, fast_car_ratio, density, total_all_cars_distance, total_slow_cars_distance, total_fast_cars_distance, worst_case_distance_slow_cars, worst_cast_distance_fast_cars, best_case_distance_slow_car, best_case_distance_fast_car,num_slow_cars,num_fast_cars,global_rule,repetition,slack,distance_look_ahead,max_speed_slow_car
        return ("NS," + (TrafficSimulation.BREAKING_DOWN_PROBABILITY == 0 ? "0" : "1") + "," + maxSpeedSlow + "," + maxSpeedFast + "," +
			TrafficSimulation.FAST_CAR_RATIO + "," + TrafficSimulation.DENSITY + "," + totalDistance + "," + totalSlowDistance + "," +
			totalFastDistance + "," +  worstFlowSlow + "," + worstFlowFast + "," + bestFlowSlow + "," + bestFlowFast + "," + numSlow + "," +
			numFast + "," + TrafficSimulation.GLOBAL_SPEED_RULE + "," + repetition + "," + TrafficSimulation.SLACK + "," + TrafficSimulation.DISTANCE_TO_LOOK_AHEAD + "," +
			TrafficSimulation.MAX_SPEED_SLOW_CAR);        
    }
    
}
