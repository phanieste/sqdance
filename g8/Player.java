package sqdance.g8;

import sqdance.sim.Point;

import java.io.*;
import java.util.Random;

public class Player implements sqdance.sim.Player {

    // some constants
    private final double PAIR_DIST = .52; // min distance between pairs 
    private final double PARTNER_DIST = .51; // distance between partners 

    // E[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] E = null;

    // random generator
    private Random random = null;

    // simulation parameters
    private int d = -1;
    private double room_side = -1;

    private int pairs;
    private int square_pairs;

    private int[] idle_turns;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
	this.d = d;
	this.room_side = (double) room_side;
        this.pairs = d / 2;
        this.square_pairs = this.pairs;
	random = new Random();
	E = new int [d][d];
	idle_turns = new int[d];
	for (int i=0 ; i<d ; i++) {
	    idle_turns[i] = 0;
	    for (int j=0; j<d; j++) {
		E[i][j] = i == j ? 0 : -1;
	    }
	}
    }


    /*
     * generate_starting_locations(): place all players in pairs in a square
     *  maximizing distance between pairs
     * Doesn't handle instances where we need multiple squares just yet
     */
    public Point[] generate_starting_locations() {
        Point[] locs = new Point[d];
        
        // determine number of pairs per side
        int side_pairs = pairs / 4; // ideal number of pairs per side of square
        if ((PAIR_DIST + PARTNER_DIST) * side_pairs > room_side) {
            side_pairs = (int) Math.floor(room_side / (PAIR_DIST + PARTNER_DIST));
        }
        square_pairs = side_pairs * 4;

        double startXY = 0.0;
        // square side length
        double square_side = room_side;
        if ((PAIR_DIST + PARTNER_DIST) * side_pairs < room_side) {
            square_side = (PAIR_DIST + PARTNER_DIST) * side_pairs;
            startXY = (room_side - square_side) / 2;
        }
        Point curr = new Point(startXY, startXY);

        int side = 0;
	for (int i = 0 ; i < d ; ++i) {
            if (i == 0) {
                locs[i] = curr;
                continue;
            }

            // determine increment
            double inc;
            if (i % 2 == 0) {
                inc = PAIR_DIST;
            }
            else {
                inc = PARTNER_DIST;
            }

            // begin a new side
            if (i == side_pairs * 8 || (i > side_pairs * 2 && i % (side_pairs * 2) == 1)) {
                side++;
            }

            // determine position of dancer
            double newX, newY;
            if (side == 0) {
                newX = curr.x + inc;
                newY = curr.y;
            }
            else if (side == 1) {
                newX = curr.x;
                newY = curr.y + inc;
            }
            else if (side == 2) {
                newX = curr.x - inc;
                newY = curr.y;
            }
            else if (side == 3) {
                newX = curr.x;
                newY = curr.y - inc;
            }
            else {
                newX = generateRandPos(startXY + 0.6, room_side - startXY - 0.6);
                newY = generateRandPos(startXY + 0.6, room_side - startXY - 0.6);
            } // place extra dancers randomly in the middle

            curr = new Point(newX, newY);
            locs[i] = curr;
	}	
	return locs;
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    /*
     * Basic strategy:
     *  - dance with current partner. if soulmates, move out of the way, else switch partners.
     */
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
	Point[] instructions = new Point[d];
        for (int i = 0; i < d; i++) {
            if (i % 2 == 0) {
                instructions[i] = new Point(0.0,0.0);
            } // even dancers stay in place
            else {
                Point next;
                if (i < square_pairs * 2) {
                    next = dancers[(i + 2) % (square_pairs * 2)];
                }
                else {
                    if (i + 2 > d) {
                        next = dancers[(i + 2) % d + square_pairs * 2];
                    }
                    else {
                        next = dancers[i + 2];
                    }
                }
                Point curr = dancers[i];
                instructions[i] = new Point(next.x - curr.x, next.y - curr.y);
            } // odd dancers move to place occupied by previous odd dancer
        }
        
        /*
	for (int i=0; i<d; i++) {
	    int j = partner_ids[i];
	    Point self = dancers[i];
	    if (enjoyment_gained[i] > 0) { // previously had a dance partner
		idle_turns[i] = 0;
		Point dance_partner = dancers[j];
		// update remaining available enjoyment
		if (E[i][j] == -1 ) {
		    E[i][j] = total_enjoyment(enjoyment_gained[i]) - enjoyment_gained[i];
		}
		else {
		    E[i][j] -= enjoyment_gained[i];
		}
		// stay put and continue dancing if there is more to enjoy
		if (E[i][j] > 0) {
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
	} */
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

    /*
     * generateRandPost(): generate a random position given a min and max bound
     */
    private double generateRandPos(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    /*
     * generateSquare(): generate an array of dancer locations in a square
     */
    private Point[] generateSquare() {
        Point[] locs = new Point[square_pairs * 2];
        
        // determine number of pairs per side
        int side_pairs = square_pairs / 4; // ideal number of pairs per side of square
        if ((PAIR_DIST + PARTNER_DIST) * side_pairs > room_side) {
            side_pairs = (int) Math.floor(room_side / (PAIR_DIST + PARTNER_DIST));
        }

        double startXY = 0.0;
        // square side length
        double square_side = room_side;
        if ((PAIR_DIST + PARTNER_DIST) * side_pairs < room_side) {
            square_side = (PAIR_DIST + PARTNER_DIST) * side_pairs;
            startXY = (room_side - square_side) / 2;
        }
        Point curr = new Point(startXY, startXY);

        int side = 0;
	for (int i = 0 ; i < square_pairs * 2 ; ++i) {
            if (i == 0) {
                locs[i] = curr;
                continue;
            }

            // determine increment
            double inc;
            if (i % 2 == 0) {
                inc = PAIR_DIST;
            }
            else {
                inc = PARTNER_DIST;
            }

            // begin a new side
            if (i == side_pairs * 8 || (i > side_pairs * 2 && i % (side_pairs * 2) == 1)) {
                side++;
            }

            // determine position of dancer
            double newX, newY;
            if (side == 0) {
                newX = curr.x + inc;
                newY = curr.y;
            }
            else if (side == 1) {
                newX = curr.x;
                newY = curr.y + inc;
            }
            else if (side == 2) {
                newX = curr.x - inc;
                newY = curr.y;
            }
            else if (side == 3) {
                newX = curr.x;
                newY = curr.y - inc;
            }
            else {
                newX = -1.0;
                newY = -1.0;
            }

            curr = new Point(newX, newY);
            locs[i] = curr;
	}	
	return locs;
    }

}
