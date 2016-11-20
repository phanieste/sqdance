package sqdance.g7;

import sqdance.sim.Point;

import java.io.*;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

public class Player implements sqdance.sim.Player {

	// E[i][j]: the remaining enjoyment player j can give player i
	// -1 if the value is unknown (everything unknown upon initialization)
	private int[][] E = null;

	// random generator
	private Random random = null;

	// simulation parameters
	private int d = -1;
	private double room_side = -1; 

	private int[] idle_turns;

	private Belt belt;

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
	}

	// setup function called once to generate initial player locations
	// note the dance caller does not know any player-player relationships, so order doesn't really matter in the Point[] you return. Just make sure your player is consistent with the indexing

	public Point[] generate_starting_locations() {
		belt = new Belt(d);	

		//	Point[] L  = new Point [d];
		//	for (int i = 0 ; i < d ; ++i) {
		//	    int b = 1000 * 1000 * 1000;
		//	    double x = random.nextInt(b + 1) * room_side / b;
		//	    double y = random.nextInt(b + 1) * room_side / b;
		//	    L[i] = new Point(x, y);
		//	}	

		Point[] L = new Point[d];
		for(int i=0; i<d; i++){
			L[i] = belt.getPosition(belt.dancerList.get(i).beltIndex);
		}
		return L;
	}
    

	// play function
	// dancers: array of locations of the dancers
	// scores: cumulative score of the dancers
	// partner_ids: index of the current dance partner. -1 if no dance partner
	// enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
	boolean danceTurn = true;
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		Point[] instructions = new Point[d];	
		for(int i=0; i<d; i++)
			instructions[i] = new Point(0,0);

		if(!danceTurn){
			Set<Integer> curDancers = getCurDancers(enjoyment_gained);
			instructions = belt.spinBelt(curDancers);
		}
		danceTurn = !danceTurn;
		return instructions;
	}


	private Set<Integer> getCurDancers(int[] enjoyment_gained) {
		Set<Dancer> res_dancers = new HashSet<>();
		for (Dancer d : belt.dancerList) {
			int partnerId = belt.getPartnerDancerID(d.dancerId);
			
			if (d.dancerStatus == Dancer.WILL_DANCE) {
				d.classifyDancer(partnerId, enjoyment_gained[d.dancerId]);
			} 

			d.determineStatus(partnerId);

			if (d.dancerStatus == Dancer.WILL_DANCE) {
				res_dancers.add(d);
			}
		}

		Set<Integer> res = belt.verifyDancer(res_dancers);
		return res;
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
