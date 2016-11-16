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
    
    private Map<Integer,Integer> circle_dancers; // mapping of dancer_id to place in the circle 
    private Map<Integer,Integer> soulmates; // mapping of soulmate ids to place in circle
    private Map<Integer,Integer> soulmates_values; // mapping of place in circle to soulmate id

    private Point[] soulmate_circle; // set of locations that create a soulmate circle

    private boolean swap; // flag that indicates when to swap vs when to stay

    private int[] idle_turns;

    // init function called once with simulation parameters before anything else is called
    public void init(int d, int room_side) {
	this.d = d;
	this.room_side = (double) room_side;
        this.pairs = d / 2;
        this.circle_dancers = new HashMap<Integer,Integer>();
        this.soulmates = new HashMap<Integer,Integer>();
        this.soulmates_values = new HashMap<Integer,Integer>();
        if(d>180){this.soulmate_circle = generateCircle(d, 0);}
        else{this.soulmate_circle = generateCircle(d, 2);}
        this.swap = false;
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
     * generate_starting_locations(): place all players in pairs in a circle 
     *  maximizing distance between pairs
     *  doesn't handle odd # of dancers yet
     */
    public Point[] generate_starting_locations() {
        int total_dancers = d;
        Point center = new Point(room_side / 2, room_side/2);
        Point[] locs = new Point[total_dancers];

        // theta is 360/(# of dancers)
        double theta = 2 * Math.PI / Math.ceil(total_dancers / 2);
        // length of chord is 2*r*sin(theta/2) = 0.52 (for inner circle)
        double inner_rad = PAIR_DIST / (2 * Math.sin(theta / 2));
        double outer_rad = inner_rad + PARTNER_DIST;

        for (int i = 0; i < total_dancers; i++) {
            if (i < total_dancers / 2) {
                locs[i] = center.add(polarToCart(outer_rad, theta * i));
            }
            else {
                locs[i] = center.add(polarToCart(inner_rad, theta * -Math.floor(i - total_dancers/2)));
            }
            circle_dancers.put(i,i);
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
     *  - dance with current partner. if soulmates, move out of the way, else switch partners in a round robin
     */
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
	Point[] instructions = new Point[d];
        
        // track the new soulmates we get to properly reassign circle positions
        Set<Integer> new_soulmates = new HashSet<Integer>();
        
        // handle all soulmates
        for (int i = 0; i < d; i++) {
            int enjoyment = enjoyment_gained[i];
            Point curr = dancers[i];

            if (enjoyment == 6) {
                int partner = partner_ids[i];
                if (!soulmates.containsKey(i) && !soulmates.containsKey(partner)) {
                    int curr_idx = circle_dancers.remove(i);
                    int partner_idx = circle_dancers.remove(partner);
                    new_soulmates.add(curr_idx);
                    new_soulmates.add(partner_idx);
                    
                    // if there are dancers in the spot being filled, move them over
                    if (curr_idx < partner_idx) {
                        vacateSpaces(i, curr_idx, partner, (curr_idx == 0 ? d/2 : d - curr_idx));
                    }
                    else {
                        vacateSpaces(i, (partner_idx == 0 ? d/2 : d - partner_idx), partner, partner_idx);
                    }
                }
            }
        }

        Point[] new_circle = generateCircle(circle_dancers.size(), 0.0);

        for (int i = 0; i < d; i++) {
            Point curr = dancers[i];
            Point nextPos;

            if (circle_dancers.containsKey(i)) {
                // is a circle dancer
                nextPos = (swap ? swapPartners(i, new_circle, new_soulmates) : curr);
            }
            else {
                // not a circle dancer (so must be a soulmate)
                int next_idx = soulmates.get(i);
                nextPos = (soulmate_circle[next_idx]);
            }
            instructions[i] = new Point(nextPos.x - curr.x, nextPos.y - curr.y);
            instructions[i] = makeValidMove(instructions[i]);
        } 

        swap = !swap;
        return instructions;
    }

    /*
     * swapPartners(i, new_circle): swap partners in the circle
     * 
     * i (int): the current dancer's id
     * new_circle (Point[]): an array of locations that comprise the new circle
     * new_soulmates (Set<Integer>): an array of indexes of new soulmates who were just removed 
     *   from the circle (so we adjust for those removed dancers)
     */
    private Point swapPartners(int i, Point[] new_circle, Set<Integer> new_soulmates) {
        int circle_idx = circle_dancers.get(i);
        Point nextPos;
        
        // adjust circle position for newly-removed soulmates
        int diff = 0;
        for (Integer idx : new_soulmates) {
            if (circle_idx > idx) {
                diff++;
            }
        }
        circle_idx = circle_idx - diff;

        if (circle_idx == 0) {
            // if first dancer in circle, hold position
            nextPos = new_circle[circle_idx];
            circle_dancers.put(i, circle_idx);
        }
        else {
            // otherwise go to the next position
            int new_idx = (circle_idx + 1) % new_circle.length;
            new_idx = (new_idx == 0 ? new_idx + 1 : new_idx);
            nextPos = new_circle[new_idx];
            circle_dancers.put(i, new_idx);
        }
        return nextPos;
    }

    /*
     * generateCircle(total_dancers, rad_inc): 
     *   return a set of locations for a double-layered round-robin circle
     *   index 0 is on the outer circle along the normal/zero vector
     *   
     *   total_dancers (int) - total # of dancers in circle
     *   rad_inc (double) - increase in inner radius size from calculated default
     */
    private Point[] generateCircle(int total_dancers, double rad_inc) {
        Point center = new Point(room_side / 2, room_side/2);
        Point[] locs = new Point[total_dancers];

        // theta is 360/(# of dancers)
        double theta = 2 * Math.PI / Math.ceil(total_dancers / 2);
        // length of chord is 2*r*sin(theta/2) = 0.52 (for inner circle)
        double inner_rad = PAIR_DIST / (2 * Math.sin(theta / 2)) + rad_inc;
        double outer_rad = inner_rad + PARTNER_DIST;

        for (int i = 0; i < total_dancers; i++) {
            if (i < total_dancers / 2) {
                locs[i] = center.add(polarToCart(outer_rad, theta * i));
            }
            else {
                locs[i] = center.add(polarToCart(inner_rad, theta * -Math.floor(i - total_dancers/2)));
            }
        }
        return locs;
    }

    /*
     * makeValidMove(): make a move valid (make sure it's within 2.0m)
     */
    private Point makeValidMove(Point move) {
        if (magnitude(move) < 2.0) {
            return move;
        }
        return new Point(move.x / magnitude(move) * 1.9999, move.y / magnitude(move) * 1.9999);
    }

    /*
     * vacateSpaces(): vacate spaces in the soulmate circle by shifting over
     */
    private void vacateSpaces(int curr_id, int curr_idx, int partner_id, int partner_idx) {
        int inner, outer;
        if (curr_idx > partner_idx) {
            inner = curr_idx;
            outer = partner_idx;
        }
        else {
            inner = partner_idx;
            outer = curr_idx;
        }

        if (soulmates_values.containsKey(inner) && soulmates_values.containsKey(outer)) {
            int next_curr = soulmates_values.get(inner);
            int next_partner = soulmates_values.get(outer);
            vacateSpaces(next_curr, (inner - 1) % (d / 2) + d / 2, next_partner, (outer + 1) % (d / 2));
        }
        soulmates.put(curr_id, curr_idx);
        soulmates_values.put(curr_idx, curr_id);
        soulmates.put(partner_id, partner_idx);
        soulmates_values.put(partner_idx, partner_id);
    }


    /*
     * magnitude(): Find the magnitude of a point.
     */
    private double magnitude(Point move) {
        return Math.sqrt(move.x * move.x + move.y * move.y);
    }

    /*
     * polarToCart(): convert polar r, theta to cartesian x,y point
     */
    private Point polarToCart(double r, double theta) {
       return new Point(r * Math.cos(theta), r * Math.sin(theta));
    }


}
