package sqdance.g8;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
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
    
    private Map<Integer,Integer> square_dancers; // mapping of dancer_id to place in the square
    private Set<Integer> soulmates;

    private int[] idle_turns;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
	this.d = d;
	this.room_side = (double) room_side;
        this.pairs = d / 2;
        this.square_dancers = new HashMap<Integer,Integer>();
        this.soulmates = new HashSet<Integer>();
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

        double startXY = 0.0;
        // square side length
        double square_side = room_side;
        if ((PAIR_DIST + PARTNER_DIST) * side_pairs < room_side) {
            square_side = (PAIR_DIST + PARTNER_DIST) * side_pairs;
            startXY = (room_side - square_side) / 2;
        }
        Point curr = new Point(startXY, startXY);

        int side = 0;
	for (int i = 0 ; i < d ; i++) {
            if (i == 0) {
                locs[i] = curr;
                square_dancers.put(i,i);
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
                square_dancers.put(i,i);
            }
            else if (side == 1) {
                newX = curr.x;
                newY = curr.y + inc;
                square_dancers.put(i,i);
            }
            else if (side == 2) {
                newX = curr.x - inc;
                newY = curr.y;
                square_dancers.put(i,i);
            }
            else if (side == 3) {
                newX = curr.x;
                newY = curr.y - inc;
                square_dancers.put(i,i);
            }
            else {
                newX = generateRandPos(startXY + 0.6, room_side - startXY - 0.6);
                newY = generateRandPos(startXY + 0.6, room_side - startXY - 0.6);
            } // place extra dancers randomly in the middle

            curr = new Point(newX, newY);
            locs[i] = curr;
	}
        //System.out.format("(%f,%f) (%f,%f) (%f,%f)\n", locs[0].x, locs[0].y, locs[1].x, locs[1].y, locs[2].x, locs[2].y);
        //System.out.println(locs.length);
	return locs;
    }

    // play function
    // dancers: array of locations of the dancers
    // scores: cumulative score of the dancers
    // partner_ids: index of the current dance partner. -1 if no dance partner
    // enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
    /*
     * Basic strategy:
     *  - dance with current partner. if soulmates, move out of the way, else switch partners in a round robin
     */
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
	Point[] instructions = new Point[d];

        // handle all soulmates
        for (int i = 0; i < d; i++) {
            int enjoyment = enjoyment_gained[i];
            Point curr = dancers[i];
            double newX, newY;

            if (enjoyment == 6) {
                System.out.println("found a soulmate!");
                int partner = partner_ids[i];
                newX = 0.0;
                newY = 0.0;
                if (!soulmates.contains(i) && !soulmates.contains(partner)) {
                    // add dancer and partner to soulmate set
                    soulmates.add(i);
                    soulmates.add(partner);
                    square_dancers.remove(i);
                    square_dancers.remove(partner);

                    // determine direction to go
                    if (dancers[partner].x == curr.x) {
                        if (curr.y > room_side / 2) {
                            newY = PAIR_DIST;
                        } else {
                            newY = -PAIR_DIST;
                        }
                    }
                    else if (dancers[partner].y == curr.y) {
                        if (curr.x > room_side / 2) {
                        newX = PAIR_DIST;
                        } else {
                            newX = -PAIR_DIST;
                        }
                    }
                }
                instructions[i] = new Point(newX, newY);
                instructions[partner] = new Point(newX, newY);
            }
        }

        Point[] new_square = generateSquare();

        for (int i = 0; i < d; i++) {
            if (soulmates.contains(i)) {
                continue;
            }
            
            Point curr = dancers[i];
          
            Point nextPos;
            if (square_dancers.containsKey(i)) {
                // is a square dancer
                int square_idx = square_dancers.get(i);
                if (square_idx == 0) {
                    // if first dancer in square, hold position
                    nextPos = new_square[square_idx];
                }
                else {
                    // otherwise go to the next position
                    int new_idx = (square_idx + 1) % new_square.length;
                    new_idx = (new_idx == 0 ? new_idx + 1 : new_idx);
                    nextPos = new_square[new_idx];
                    square_dancers.put(i, new_idx);
                }
            }
            else {
                // not a square dancer
                int next = (i + 1) % d;
                while (square_dancers.containsKey(next) || soulmates.contains(next)) {
                    next = (next + 1) % d;
                }
                nextPos = dancers[next];
            }
            if (nextPos.x == -1.0 && nextPos.y == -1.0) {
                instructions[i] = new Point(0.0, 0.0);
            }
            else {
                instructions[i] = new Point(nextPos.x - curr.x, nextPos.y - curr.y);
            }
            instructions[i] = makeValidMove(instructions[i]);
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
        Point[] locs = new Point[square_dancers.size()];
        
        // determine number of pairs per side
        int side_pairs = (int) Math.floor(square_dancers.size() / 8); // ideal number of pairs per side of square
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
	for (int i = 0; i < square_dancers.size(); i++) {
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
        //System.out.format("(%f,%f) (%f,%f) (%f,%f)\n", locs[0].x, locs[0].y, locs[1].x, locs[1].y, locs[2].x, locs[2].y);
        //System.out.println(locs.length);
	return locs;
    }

    /*
     * makeValidMove(): make a move valid (make sure it's within 2.0m)
     */
    private Point makeValidMove(Point move) {
        if (magnitude(move) <= 2.0) {
            return move;
        }
        return new Point(move.x / magnitude(move) * 2.0, move.y / magnitude(move) * 2.0);
    }

    /*
     * magnitude(): Find the magnitude of a point.
     */
    private double magnitude(Point move) {
        return Math.sqrt(move.x * move.x + move.y * move.y);
    }


}
