package sqdance.g2;

import sqdance.sim.Point;

import java.io.*;
import java.util.Random;
import java.util.List;

public class Player implements sqdance.sim.Player {

    // remainingEnjoyment[i][j]: the remaining enjoyment player j can give player i
    // -1 if the value is unknown (everything unknown upon initialization)
    private int[][] remainingEnjoyment = null;
    
    //s soulmate, f friend, x stranger
    private char[][] relation = null;
    
    private int[] soulmate = null;
    // random generator
    private Random random = null;

    // simulation parameters
    static int d = -1;
    static double room_side = -1;

    private int[] idle_turns;

    public void init(int d, int room_side) {
        this.d = d;
        this.room_side = (double) room_side;
        soulmate = new int[d];
        for(int i = 0 ; i < d ; ++ i) soulmate[i] = -1;
        random = new Random();
        remainingEnjoyment = new int [d][d];
        relation = new char[d][d];
        idle_turns = new int[d];
        for (int i=0 ; i<d ; i++) {
            idle_turns[i] = 0;
            for (int j=0; j<d; j++) {
                remainingEnjoyment[i][j] = i == j ? 0 : -1;
            }
        }
    }

    public Point[] generate_starting_locations() {
        return SquareSpiralStrategy.init();
    }
    
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        Point[] instructions;
        playUpdateInformation(dancers, scores, partner_ids, enjoyment_gained);
        instructions = SquareSpiralStrategy.move(dancers,
        		scores,
        		partner_ids,
        		enjoyment_gained,
        		remainingEnjoyment,
        		relation,
        		soulmate);
        return instructions;
    }

    public Point[] initStrategy0() {
        Point[] L  = new Point [d];
        for (int i = 0 ; i < d ; ++i) {
            int b = 1000 * 1000 * 1000;
            double x = random.nextInt(b + 1) * room_side / b;
            double y = random.nextInt(b + 1) * room_side / b;
            L[i] = new Point(x, y);
        }        
        return L;
    }

    // Use this method to update information variables like remainingEnjoyment
    // Preserve signature of this function as the same as play() - FIXME why are we doing this?
    public Point[] playUpdateInformation(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
        for (int i=0; i<d; i++) {
            int j = partner_ids[i];
            Point self = dancers[i];

            // ## Update Variables
            if (enjoyment_gained[i] > 0) { // previously had a dance partner
                idle_turns[i] = 0;
                Point dance_partner = dancers[j];
                // update remaining available enjoyment
                if (remainingEnjoyment[i][j] == -1 ) {
                    remainingEnjoyment[i][j] = total_enjoyment(enjoyment_gained[i]) - enjoyment_gained[i];
                }
                else {
                    remainingEnjoyment[i][j] -= enjoyment_gained[i];
                }
                relation[i][j] 
                		= relation[j][i] 
                		= getRelation(enjoyment_gained[i]);
                if(relation[i][j]=='s') {
                	soulmate[i] = j;
                }
            }
        }
        
        return null;
    }

    private char getRelation(int enjoyment) {
    	switch (enjoyment) {
        case 3: return 'x'; // stranger
        case 4: return 'f'; // friend
        case 6: return 's'; // soulmate
        default: throw new IllegalArgumentException("Not dancing with anyone...");
    }      
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
