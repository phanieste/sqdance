package sqdance.g2;

import java.util.HashMap;

import sqdance.sim.Point;

public class ZigZagStrategySmall implements Strategy {
	private static final double EPSILON = 0.000001;
	private static final double DISTANCE_BETWEEN_DANCERS = 0.51;
	
	//TODO: Set actual number (how many dancers are in each row)
	private static final int DANCERS_IN_A_LINE = 36;
	private int num_swaps;
	
	private Point[] final_positions;
	private HashMap<Integer, Integer> dancer_at_point;
	
	@Override
	public Point[] generate_starting_locations() {
		num_swaps = 0;
		dancer_at_point = new HashMap<>(Player.d);
		
		double y_limit = (DISTANCE_BETWEEN_DANCERS * (DANCERS_IN_A_LINE-1)) - (EPSILON * DANCERS_IN_A_LINE / 2);
		int d = Player.d;
		
		Point[] positions = new Point[d];
		double current_row = 0;
		int dancer = 0;
		while(dancer < Player.d) {
			for(int i = 0; i < DANCERS_IN_A_LINE && dancer < Player.d; i += 2) {
				double yPos = current_row * DISTANCE_BETWEEN_DANCERS;
				double xPos1 = (current_row % 2 == 0) ?
									(DISTANCE_BETWEEN_DANCERS * i) - (EPSILON * i / 2) :
									y_limit - ((DISTANCE_BETWEEN_DANCERS * i) - (EPSILON * i / 2));
				double xPos2 = (current_row % 2 == 0) ?
									xPos1 + DISTANCE_BETWEEN_DANCERS - EPSILON :
									xPos1 - DISTANCE_BETWEEN_DANCERS + EPSILON;
				if (xPos2 < 0) xPos2 = 0;
				
				positions[dancer] = new Point(xPos1 + 0.5, yPos);
				positions[dancer+1] = new Point(xPos2 + 0.5, yPos);
				dancer_at_point.put(dancer, dancer);
				dancer_at_point.put(dancer+1, dancer+1);
				dancer += 2;
			}
			
			current_row++;
		}
		
		final_positions = positions;
		return positions;
	}
	
	//TODO: data structure access and update
	private double getXfromLocation(int i) {
		return final_positions[i].x;
	}
	
	private double getYfromLocation(int i) {
		return final_positions[i].y;
	}
	
	private int getIDfromLocation(int i) {
		return dancer_at_point.get(i);
	}
	
	private void updateLocations(int i, Point new_location, int new_id) {
		final_positions[i] = new Point( new_location.x, new_location.y);
		dancer_at_point.put(i, new_id);
	}
	
    public Point[] playSmallD(Point[] dancers,
    		int[] scores,
    		int[] partner_ids,
    		int[] enjoyment_gained,
    		int[] soulmate,
    		int current_turn) {
    	int d = scores.length;
    	Point[] instructions = new Point[d];
    	boolean complete = true;
    	//complete if soulmates are all found
    	for(int i = 0; i < d; ++ i) {
    		if(soulmate[i] == -1) {
    			complete = false;
    		}
    	}
    	if (complete) {
    		//move to final locations
    		int cur = 0;
    		for(int i = 0; i < d; ++ i) {
    			if(soulmate[i] < i) continue;
    			int j = soulmate[i];
    			double x1 = getXfromLocation(cur);
				double y1 = getYfromLocation(cur);
    			double x2 = getXfromLocation(cur + 1);
				double y2 = getYfromLocation(cur + 1);
				cur += 2;
				instructions[i] = new Vector(x1 - dancers[i].x,
    					y1 - dancers[i].y)
    					.getLengthLimitedVector(2)
    					.getPoint();
				instructions[i] = new Vector(x2 - dancers[j].x,
    					y2 - dancers[2].y)
    					.getLengthLimitedVector(2)
    					.getPoint();
    		}
    	} else {
    		for(int i = 0 ; i < d ; ++ i) {
    			double x1 = getXfromLocation(i);
				double y1 = getYfromLocation(i);
				int id1 = getIDfromLocation(i);
				//System.out.println("id x1 y1 " + id1 +" "+x1 +" " + y1 );
				//System.out.println();
    		}
    		//swap and dance
    		if(current_turn % 2 == 0) {
    			//dance
    			for(int i = 0; i < d; ++i) {
    				instructions[i] = new Point(0,0);
    			}
    		} else {
    			int[] new_id = new int[d];
    			//swap
				for(int i = 0; i < d; i ++) {
					
					double x1 = getXfromLocation(i);
					double y1 = getYfromLocation(i);
					int id1 = getIDfromLocation(i);
					System.out.println(id1);
					int next;
					if (num_swaps % 2 == 0) 
						next = (i % 2 == 0 ? i + 1 : i - 1);
					else {
						if(i == 0 || i == d-1) {
							instructions[id1] = new Point(0, 0);
							new_id[i] = id1;
							//System.out.println("");
							continue;
						}
						next = (i % 2 == 0 ? i - 1 : i + 1);
					}
					
					double x2 = getXfromLocation(next);
					double y2 = getYfromLocation(next);
					int id2 = getIDfromLocation(next);
					new_id[i] = id2;
					System.out.println(id1 + " with " + id2);
					Vector direction = new Vector(x2 - x1, y2 - y1);
					int i_mod = i % (2 * DANCERS_IN_A_LINE);
					if (num_swaps % 2 == 0) {
	    				//swap first and second, third with fourth, ...
						if(i_mod >= DANCERS_IN_A_LINE) {
    						i_mod = DANCERS_IN_A_LINE - 1 - i_mod;
    					}
						if(i_mod % 2 == 0 && i_mod != DANCERS_IN_A_LINE - 1) {
							direction = direction.add(2 * EPSILON, 0);
						}
					} else {
	    				//swap second with third, fourth with fifth, ...
						if(i_mod >= DANCERS_IN_A_LINE) {
    						i_mod = DANCERS_IN_A_LINE - 1 - i_mod;
    					}
						if((i_mod % 2 == 0 && i_mod != 0) 
								|| (i_mod == DANCERS_IN_A_LINE - 1 && DANCERS_IN_A_LINE % 2 == 0) ) {
							direction = direction.add(-2 * EPSILON, 0);
						} 
					}
					instructions[id1] = direction.getPoint();
					
    				
    			}
    			++ num_swaps;

    	    	for(int i = 0; i < d; ++i) {
    	    		int next = new_id[i];
    	    		System.out.println(i +" has " +new_id[i]);
    	    		System.out.println((instructions[next]==null));
    	    		Point new_location = 
    	    				new Point(instructions[next].x + dancers[next].x,
    	    						instructions[next].y + dancers[next].y);
    	    		System.out.println("new loc " + next  + " "+new_location.x);
    	    		updateLocations(i, new_location,new_id[i]);
    	    	}
    		}
    	}
    	return instructions;
    }
    
    
    
    //increment current turn after everyone is done dancing with strangers/friends
    //for medium d
    //for small d just increment it every turn
    @Override
	public Point[] play(Point[] dancers, int[] scores,
			int[] partner_ids, int[] enjoyment_gained,
			int[] soulmate, int current_turn) {
		return playSmallD(dancers,
	    		scores,
	    		partner_ids,
	    		enjoyment_gained,
	    		soulmate,
	    		current_turn);
	}
}
