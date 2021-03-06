package sqdance.g1;

import sqdance.sim.Point;

import java.io.*;
import java.util.Random;

public class Player implements sqdance.sim.Player {

    // constants
    private static final double INITIAL_X = 0.1;
    private static final double INITIAL_Y = 0.1;
    private static final int MAX_PAIRS_IN_LINE = 35;
    private static final double PARTNER_DIST = 0.51; // distance between dancing partners (will be along x-axis)
    private static final double PAIR_DIST = 0.55; // distance between pairs (the y distance within a row and the x distance between rows)
    private static double LINE_DIST = PARTNER_DIST + PAIR_DIST; // distance between points in the same row between lines
    private static int MAX_LINES = 0;

    private int turnCounter = 0;
    private double conveyorY = 0;
    private double conveyorDist = 0;
    private int numLines = 0;
    private int numPerLine = 0;
    
    // E[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] E = null;

    // random generator
    private Random random = null;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

    private int[] idle_turns;
    
    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
	this.d = d;
	this.room_side = (double) room_side;
	random = new Random();
	E = new int [d][d];
	idle_turns = new int[d];
	for (int i=0 ; i<d ; i++) {
	    idle_turns[i] = 0;
	    for (int j=0; j<d; j++) {
		E[i][j] = i == j ? 0 : -1;
	    }
	}

        MAX_LINES = (int)((room_side - INITIAL_X) / LINE_DIST);
    }

    // setup function called once to generate initial player locations
    // note the dance caller does not know any player-player relationships, so order doesn't really matter in the Point[] you return. Just make sure your player is consistent with the indexing

    public Point[] generate_starting_locations() {
	Point[] L  = new Point [d];
        int numPairs = d/2;
        if (numPairs <= MAX_LINES * MAX_PAIRS_IN_LINE) {
            // find out how many lines the dancers will take up
            numLines = (numPairs / MAX_PAIRS_IN_LINE) + 1;
            double conveyorLength = numLines * LINE_DIST + PARTNER_DIST;
            int numInConveyor = (int)Math.ceil(conveyorLength);
            numPerLine = 0;

            // if will take more than 1 line, figure out how many in each line
            if (numLines > 1) {
                numPerLine = (d - numInConveyor) / numLines;
                if (numPerLine % 2 != 0) {
                    numPerLine--;
                }
            }

            // recalculate num in conveyor
            numInConveyor = d - (numPerLine * numLines);
            System.out.println("Num lines = " + numLines);
            System.out.println("Num per line = " + numPerLine);
            System.out.println("Num in conveyor = " + numInConveyor);

            // calculate how far apart each conveyor dancer needs to be
            conveyorLength = ((numLines - 1) * LINE_DIST) - PARTNER_DIST;
            conveyorDist = conveyorLength / (numInConveyor - 1);
            
            int i = 0;
            double currX = INITIAL_X;

            // fill in lines
            for (int line = 0; line < numLines; line++) {
                double currY = INITIAL_Y;
                for (int pair = 0; pair < numPairs; pair++) {
                    if (pair * 2 >= numPerLine) {
                        break;
                    }

                    if (i >= d) {
                        return L;
                    }
                    
                    L[i] = new Point(currX, currY);
                    i++;
                    L[i] = new Point(currX + PARTNER_DIST, currY);
                    i++;
                    currY += PAIR_DIST;
                }
                currX += LINE_DIST;
            }

            // fill in conveyor
            conveyorY = (numPerLine / 2) * PAIR_DIST + INITIAL_Y;
            double conveyorX = INITIAL_X + PARTNER_DIST;
            for (int j = 0; j < numInConveyor; j++) {
                L[i] = new Point(conveyorX, conveyorY);
                i++;
                conveyorX += conveyorDist;
            }
        }
        else {
            for (int i = 0 ; i < d ; ++i) {
                int b = 1000 * 1000 * 1000;
                double x = random.nextInt(b + 1) * room_side / b;
                double y = random.nextInt(b + 1) * room_side / b;
                L[i] = new Point(x, y);
            }	
        }
	return L;
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        Point[] instructions = new Point[d];
        int numPairs = d / 2;
        if (numPairs <= MAX_LINES * MAX_PAIRS_IN_LINE) {        
            instructions = simpleLines(dancers, scores, partner_ids, enjoyment_gained);
        }
        else {
            instructions = defaultPlayer(dancers, scores, partner_ids, enjoyment_gained);
        }
        
        turnCounter++;
        return instructions;
    }

    private Point[] simpleLines(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        Point[] instructions = new Point[d];

        if ((turnCounter % 20) != 0 || turnCounter == 0) {
            for (int i = 0; i < d; i++) {
                instructions[i] = new Point(0,0);
            }
            return instructions;
        }

        Point nextMove = null;
        double conveyorDistY = conveyorY - ((numPerLine/2 - 1) * PAIR_DIST) - INITIAL_Y;
        for (int i = 0; i < d; i++) {
            double currY = dancers[i].y;
            double currX = dancers[i].x;

            double diffConveyorY = conveyorY - currY;
            if (diffConveyorY < 0.00001 && diffConveyorY > -0.00001) {
                // dancer on the conveyor
                double diffX = (INITIAL_X + PARTNER_DIST) - currX;
                if (diffX < 0.00001 && diffX > -0.00001) {
                    // at the left most point of conveyor
                    nextMove = new Point(-PARTNER_DIST, -conveyorDistY);
                }
                else {
                    nextMove = new Point(-conveyorDist, 0);
                }
            }
            else {
                // dancer not on the conveyor, in a normal line
                boolean onLeft = false;

                int currLine = (int)((currX - INITIAL_X) / LINE_DIST);
                double currLineStart = currLine * LINE_DIST + INITIAL_X;
                double diffFromLeft = currX - currLineStart;
                
                if (diffFromLeft < 0.0001 && diffFromLeft > -0.0001) {
                    onLeft = true;
                }

                // find highest and lowest y in this dancer's line
                // TODO: optimize by finding highest/lowest Y for each line at beginning...
                double highestY = 0;
                double lowestY = room_side;
                for (int j = 0; j < d; j++) {
                    int line = (int)((dancers[j].x - INITIAL_X) / LINE_DIST);
                    // make sure dancers[j] is not on the conveyor
                    diffConveyorY = conveyorY - dancers[j].y;
                    if (diffConveyorY < 0.00001 && diffConveyorY > -0.00001) {
                        continue;
                    }
                    
                    if (line == currLine) {
                        if (dancers[j].y > highestY) {
                            highestY = dancers[j].y;
                        }
                        if (dancers[j].y < lowestY) {
                            lowestY = dancers[j].y;
                        }
                    }
                } // end get highest/lowest y for currLine

                double diffHighestY = highestY - currY;
                double diffLowestY = currY - lowestY;
                if (onLeft && (diffLowestY < 0.00001) && (diffLowestY > -0.00001)) {
                    // is at top left of line
                    nextMove = new Point(PARTNER_DIST, 0);
                }
                else if (!onLeft && (diffHighestY < 0.00001) && (diffHighestY > -0.00001)) {
                    // is at bottom right of line
                    if (currLine == (numLines - 1)) {
                        // if it's the last line, move to conveyor
                        nextMove = new Point(-PARTNER_DIST, conveyorDistY);
                    }
                    else {
                        // otherwise, move to the next line
                        nextMove = new Point(PAIR_DIST, 0);
                    }
                }
                else if (onLeft) {
                    // is anywhere else on left of line, move up
                    nextMove = new Point(0, -PAIR_DIST);
                }
                else {
                    // is anywhere on the right of a line
                    nextMove = new Point(0, PAIR_DIST);
                }
            } // end else not in conveyor

            instructions[i] = nextMove;                
        } // end for (int i = 0; i < d; i++)
            
        
        return instructions;
    }
    
    private Point[] defaultPlayer(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
	Point[] instructions = new Point[d];
	for (int i=0; i<d; i++) {
	    int j = partner_ids[i];
	    Point self = dancers[i];
        int avg_enjoy = 0;
        int counter = 0;
	    if (enjoyment_gained[i] > 0) { // previously had a dance partner
		idle_turns[i] = 0;
		Point dance_partner = dancers[j];
		// update remaining available enjoyment
		if (E[i][j] == -1 ) {
		    E[i][j] = total_enjoyment(enjoyment_gained[i]) - enjoyment_gained[i];
            counter++;
            avg_enjoy += E[i][j];
		}
		else {
		    E[i][j] -= enjoyment_gained[i];
            counter ++;
            avg_enjoy += E[i][j];
		}
		// stay put and continue dancing if there is more to enjoy
		if (E[i][j] > avg_enjoy) {
		    instructions[i] = new Point(0.0, 0.0);
		    continue;
		}
	    }
	    Point m = null;	    
	    if (++idle_turns[i] > 21) { // if stuck at current position without enjoying anything
		idle_turns[i] = 0;
	    } else { // stay put if there's another potential dance partner in range
		double closest_dist = Double.MAX_VALUE;
		int closest_index = -1;
		for (int t=0; t<d; t++) {
		    // skip if no more to enjoy
		    if (E[i][t] == 0) continue;
		    // compute squared distance
		    Point p = dancers[t];		
		    double dx = self.x - p.x;
		    double dy = self.y - p.y;
		    double dd = dx * dx + dy * dy;
		    // stay put and try to dance if new person around or more enjoyment remaining.		
		    if (dd >= 0.25 && dd < 4.0) {
			m = new Point(0.0, 0.0);
			break;
		    }
		}
	    }
	    // move randomly if no move yet
	    if (m == null) {
		double dir = random.nextDouble() * 2 * Math.PI;
		double dx = 1.9 * Math.cos(dir);
		double dy = 1.9 * Math.sin(dir);
		m = new Point(dx, dy);
		if (!self.valid_movement(m, room_side))
		    m = new Point(0,0);
	    }
	    instructions[i] = m;
	}
	return instructions;
    }
    
    private int total_enjoyment(int enjoyment_gained) {
	switch (enjoyment_gained) {
	case 3: return 60; // stranger
	case 4: return 200; // friend
	case 6: return 10800; // soulmate
	default: throw new IllegalArgumentException("Not dancing with anyone...");
	}	
    }
}
