package sqdance.g4;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
import java.lang.System.*;

public class Player implements sqdance.sim.Player {

	private static double eps = 1e-7;

	private double minDis = 0.5;
	private double maxDis = 2.0;
	private double safeDis = 0.1;
	private int[] scorePround = {0, 6, 4, 3}; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger
	private int boredTime = 120; // 2 minutes

	private int d = -1;
	private int room_side = -1;

	private int[] soulmate; // initialize to -1
	private int[][] relation; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger, initialize to -1
	private int[][] danced; // cumulatived time in seconds for dance together
	private ArrayList<Integer> foundCouples = new ArrayList<>();
	private int stay = 0;

	public class Dancer{
		int id = -1;
		int soulmate = -1;
		Point next_pos = null;
		Point des_pos = null;
		//only used by singles	
		int pit_id = -1;
	}

	//dancers never stay at pit, legal positions are up/down/left/right eps/3;
	public class Pit{
		Point pos = null;
		Pit prev = null;
		Pit next = null;
		int player_id = -1;
		int pit_id = -1;
	}

	private Dancer[] dancers;
	private boolean connected;
	private Point[] starting_positions;
	private Pit[] pits;
	private int[] target_single_shape; // a list of Pit indexes
	private int state; // 1 represents 1-2 3-4 5-6, 2 represents 1 2-3 4-5 6
	//====================== end =========================

	public void init(int d, int room_side) {
		//System.out.println("init");
		this.d = d;
		this.room_side = room_side;
		this.state = 1;
		
		//data structure initialization
		soulmate = new int[d];
		for (int i = 0; i < d; ++i) soulmate[i] = -1;

		relation = new int[d][d];
		danced = new int[d][d];
		for (int i = 0; i < d; ++i){
			for (int j = 0; j < d; ++j) {
				relation[i][j] = -1;
				danced[i][j] = 0;
			}
		}

		this.connected = true;
		this.starting_positions = new Point[d];
		this.target_single_shape = new int[d];
		this.pits = new Pit[d];
		this.dancers = new Dancer[d];
		Pit prev_pit = null;
		double x = eps;
		double y = eps;
		double increment = 0.5 + eps;
		int i = 0;
		int sign = 1;
		while(i<d){
			//int sign = (((i&1) == 1) ? 1: -1);
			Point curr_pos = new Point(x,y);
			Pit curr_pit = new Pit();
			this.starting_positions[i] = curr_pos;
			this.target_single_shape[i] = i;
			this.pits[i] = curr_pit;
			if(prev_pit != null) prev_pit.next = curr_pit;
			curr_pit.prev = prev_pit;
			curr_pit.pos = curr_pos;
			curr_pit.player_id = i;
			curr_pit.pit_id = i;
			prev_pit = curr_pit;
			Dancer dancer = new Dancer();
			dancer.id = i;
			dancer.pit_id = i;
			this.dancers[i] = dancer;
			if(y + sign * increment <= this.room_side - eps && y + sign * increment >= eps){
				y += sign * increment;
			}
			else{
				x += increment;
				sign = - sign;
			}
			i++;
		}
	}

	public Point[] generate_starting_locations() {
		Point[] actual_starting_locations = this.starting_positions;
		for (int i = 0; i < d; i++) {
			if (i%2 == 0){
				actual_starting_locations[i] = findNearestActualPoint(starting_positions[i],starting_positions[i+1]);
			}
			else{
				actual_starting_locations[i] = findNearestActualPoint(starting_positions[i],starting_positions[i-1]);
			}
			dancers[i].next_pos = actual_starting_locations[i];
		}
		//this.state = 2;
		printPosition(actual_starting_locations);
		return actual_starting_locations;
	}

	public Point[] play(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		// first update partner information and culmulative time danced
		ArrayList<Integer> new_couple_ids = updatePartnerInfo(partner_ids, enjoyment_gained);
		if (true || this.connected && new_couple_ids.size() == 0) {
			if(this.stay >= boredTime){
				swap();
				this.stay = 0;
			}
			else{
				this.stay += 6;
			}
			

			//System.out.println("swaped");
		}
		else{
			if (new_couple_ids.size() > 0) {
				// assert
				this.connected = false;
				// initialize curr_pos of couples
				initializeCurrPosition(new_couple_ids);
				// generate a sequence of pit indexes of de-coupled dancers
				int[] newShape = genShape(new_couple_ids.size());
				// generate new_soulmate_destination
				int[] soulmatePitIndex = findSoulmateIndex(newShape);
				Point[] soulmateActualLocations = findSoulmateDestination(soulmatePitIndex);
				// update new_couple 
				updateNewSoulmateDes(new_couple_ids, soulmateActualLocations);
				foundCouples.addAll(new_couple_ids);
				// update target_single_shape
				this.target_single_shape = newShape;
			}
			//try to connect the dancers
			this.connected = connect();
		} 
		move_couple();
		//generate instructions using target positions and current positions
		return generateInstructions(old_positions);
	}

	ArrayList<Integer> updatePartnerInfo(int[] partner_ids, int[] enjoyment_gained) {
		ArrayList<Integer> couple_ids = new ArrayList<>();
		for(int i = 0; i < d; i++){
			if(enjoyment_gained[i] == 6){
				soulmate[i] = partner_ids[i];
				if(relation[i][partner_ids[i]] != 1 && relation[partner_ids[i]][i] != 1) {
					couple_ids.add(i);
					couple_ids.add(partner_ids[i]);
				}
				relation[i][partner_ids[i]] = 1;
				dancers[i].soulmate = partner_ids[i];
			}
			else if(enjoyment_gained[i] == 4){
				relation[i][partner_ids[i]] = 2;
			}
			else if(enjoyment_gained[i] == 3){
				relation[i][partner_ids[i]] = 3;
			}
			danced[i][partner_ids[i]] += 6;

		}
		return couple_ids;
	}

	// a = (x,y) we want to find least distance between (x+eps/3, y) (x-eps/3, y) (x, y+eps/3) (x, y-eps/3) and b
	Point findNearestActualPoint(Point a, Point b) {
		Point left = new Point(a.x-eps/3,a.y);
		Point right = new Point(a.x+eps/3,a.y);
		Point down = new Point(a.x,a.y-eps/3);
		Point up = new Point(a.x,a.y+eps/3);
		Point a_neighbor = left;
		if (distance(right,b) < distance(a_neighbor,b)) a_neighbor = right;
		if (distance(down,b) < distance(a_neighbor,b)) a_neighbor = down;
		if (distance(up,b) < distance(a_neighbor,b)) a_neighbor = up;
		return a_neighbor;
	}

	//modify the desination positions of active dancers;
	void swap() {
		for (int i = 0; i < this.target_single_shape.length; i++) {
			if (i%2 == 0 && this.state == 1 || i%2 == 1 && this.state == 2) {
				int pit_id = this.target_single_shape[i];
				int dancer_id = pits[pit_id].player_id;
				
				if (i==this.target_single_shape.length-1) {
					System.out.println(pits[pit_id].pos.x -eps/3 + " " + pits[pit_id].pos.y);
					dancers[dancer_id].next_pos = new Point(pits[pit_id].pos.x -eps/3, pits[pit_id].pos.y);
					dancers[dancer_id].pit_id = pit_id;
				} else {
					int pit_id1 = this.target_single_shape[i+1];
					int dancer_id1 = pits[pit_id1].player_id;
					dancers[dancer_id].next_pos = findNearestActualPoint(pits[pit_id1].pos, pits[pit_id].pos);
					dancers[dancer_id].pit_id = pit_id1;
				}
			} else if (i%2 == 1 && this.state == 1 || i%2 == 0 && this.state == 2) {

				int pit_id = this.target_single_shape[i];
				int dancer_id = pits[pit_id].player_id;
				
				if (i==0) {
					System.out.println(pits[pit_id].pos.x -eps/3 + " " + pits[pit_id].pos.y);
					dancers[dancer_id].next_pos = new Point(pits[pit_id].pos.x -eps/3, pits[pit_id].pos.y);
					dancers[dancer_id].pit_id = pit_id;
				} else {
					int pit_id1 = this.target_single_shape[i-1];
					int dancer_id1 = pits[pit_id1].player_id;
					dancers[dancer_id].next_pos = findNearestActualPoint(pits[pit_id1].pos, pits[pit_id].pos);
					dancers[dancer_id].pit_id = pit_id1;
				}
			}
		}
		for (int i = 0; i < this.target_single_shape.length; i++) {
			System.out.println(dancers[pits[this.target_single_shape[i]].player_id].next_pos.x + " " + dancers[pits[this.target_single_shape[i]].player_id].next_pos.y);
		}
		System.out.println("--------------------------------");
		this.state = 3 - this.state;
	}

	class PitsComparator implements Comparator<Integer> {
		@Override
			public int compare(Integer a, Integer b) {
				if (pits[a].pos.x < pits[b].pos.x - eps) return 1;
				if (pits[a].pos.x < pits[b].pos.x + eps) {
					if (pits[a].pos.y < pits[b].pos.y) return 1;
					if (pits[a].pos.y > pits[b].pos.y) return -1;
				}
				if (pits[a].pos.x > pits[b].pos.x) return -1;
				return 0;
			}
	}

	void initializeCurrPosition(ArrayList<Integer> new_couple_ids) {
		for (int i=0; i<new_couple_ids.size(); i++) {
			dancers[new_couple_ids.get(i)].next_pos = pits[dancers[new_couple_ids.get(i)].pit_id].pos;
		}
	}

	// according to the this.dancers, calculate the destination indexes set of de-coupled dancers
	int[] genShape(int numNewCouple) {
		ArrayList<Integer> cur = new ArrayList<Integer>();
		for (int i = 0; i < d; ++i) {
			if (dancers[i].soulmate == -1)
				cur.add(dancers[i].pit_id);
		}

		Collections.sort(cur, new PitsComparator());

		int[] res = new int[cur.size()];
		for (int i = numNewCouple * 2; i < cur.size(); ++i) {
			res[i - numNewCouple * 2] = cur.get(i);
		}

		Arrays.sort(res);
		int tot = cur.size() - numNewCouple * 2;
		int last = cur.get(numNewCouple * 2 - 1);
		Point lastCouplePos = new Point(pits[last].pos.x, pits[last].pos.y);
		for (int i = 1; i < cur.size() - numNewCouple * 2; ++i) {
			for (int j = res[i - 1] + 1; j < res[i]; ++j) {
				if (pits[j].pos.x > lastCouplePos.x + eps ||
					(pits[j].pos.x > lastCouplePos.x - eps && pits[j].pos.y > lastCouplePos.y + eps)) {
					if (tot == cur.size()) System.out.println("genShape error: number of holes is not as expected");
					res[tot++] = j;
				}
			}
		}
		if (tot < cur.size()) {
			for (int j = res[cur.size() - numNewCouple * 2 - 1] + 1; tot < cur.size(); ++j) {
				if (pits[j].pos.x > lastCouplePos.x + eps ||
					(pits[j].pos.x > lastCouplePos.x - eps && pits[j].pos.y > lastCouplePos.y + eps)) {
					res[tot++] = j;
				}
			}
		}
		return res;
	}

	int[] findSoulmateIndex(int[] newShape) {
		int[] residual = new int[this.target_single_shape.length - newShape.length];
		int j = 0;
		int k = 0;
		for (int i=0; i<this.target_single_shape.length; i++) {
			if (target_single_shape[i] != newShape[j]) {
				residual[k] = target_single_shape[i];
				k ++;
			}
			else j++;
		}
		return residual;
	}

	Point[] findSoulmateDestination(int[] soulmatePitIndex) {
		Point[] soulmateActualLocations = new Point[soulmatePitIndex.length];
		for (int i=0; i<soulmatePitIndex.length; i++) {
			if (i%2==0) soulmateActualLocations[i] = findNearestActualPoint(this.pits[soulmatePitIndex[i]].pos, this.pits[soulmatePitIndex[i+1]].pos);
			else soulmateActualLocations[i] = findNearestActualPoint(this.pits[soulmatePitIndex[i]].pos, this.pits[soulmatePitIndex[i-1]].pos);
		}
		return soulmateActualLocations;
	}

	void updateNewSoulmateDes(ArrayList<Integer> couple_ids, Point[] soulmateActualLocations) {
		for (int i=0; i<couple_ids.size(); i++) {
			this.dancers[couple_ids.get(i)].des_pos = soulmateActualLocations[i];
		}
	}

	// update single dancer's next position using target_single_shape, return true target_single_shape is connected;
	boolean connect() {
		int single_index = 0;
		boolean connected = true;
		for(int i = 0; i < d; i++){
			if(dancers[i].soulmate != -1) continue;
			int target_pit_id = this.target_single_shape[single_index++];
			Pit curr_pit = pits[dancers[i].pit_id];
			Pit pointer = curr_pit;
			boolean stop = false;
			while(!stop){
				if(pointer.pit_id < target_pit_id){
					pointer = pointer.next;
				}
				else if(pointer.pit_id > target_pit_id){
					pointer = pointer.prev;
				}
				stop = distance(pointer.pos,curr_pit.pos) > 2 || pointer.pit_id == target_pit_id;
			}
			if(!samepos(pointer.pos,curr_pit.pos)) connected = false;
			dancers[i].pit_id = pointer.pit_id;
			dancers[i].next_pos = pointer.pos;
			this.pits[pointer.pit_id].player_id = i;
		}
		return connected;
	}

	double distance(Point p1,Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx*dx+dy*dy);
	}

	// according to the information of the dancers, 
	void move_couple() {
		for (Integer index : foundCouples) {
			Point curr = this.dancers[index].next_pos;
			Point des = this.dancers[index].des_pos;
			this.dancers[index].next_pos = findNextPosition(curr, des);
		}
	}

	Point findNextPosition(Point curr, Point des) {
		if (distance(curr,des) < 2) return des;
		else {
			double x = des.x - curr.x;
			double y = des.y-curr.y;
			Point next = new Point(curr.x + (2-eps)*x/Math.sqrt(x*x+y*y), curr.y + (2-eps)*y/Math.sqrt(x*x+y*y));
			return next;
		}
	}

	// generate instruction according to this.dancers
	private Point[] generateInstructions(Point[] old_positions){
		Point[] movement = new Point[d];
		for(int i = 0; i < d; i++){
			//System.out.println("i: " + i);
			movement[i] = new Point(dancers[i].next_pos.x-old_positions[i].x,dancers[i].next_pos.y-old_positions[i].y);


			//if(i == 0 || i == 2 || i == 85 || i == 87) System.out.println(old_positions[i].x+","+old_positions[i].y);
		}
		return movement;
	}

	private boolean samepos(Point p1,Point p2){
		return Math.abs(p1.x - p2.x) < eps && Math.abs(p1.y - p2.y) < eps;
	}

	void printPosition(Point[] points){
		for(int i = 0; i < d; i++){
			if(i == 0 || i == 2 || i == 85 || i == 87) System.out.println(points[i].x+","+points[i].y);
		}
		/*
		for(Point p:points){
			System.out.println(p.x+","+p.y);
		}
		*/
	}
}
