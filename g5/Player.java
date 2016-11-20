package sqdance.g5;

import sqdance.sim.Point;
import java.util.*;

public class Player implements sqdance.sim.Player {

	static int d;
	static int roomSide;

	// Define gaps
	public static final double HORIZONTAL_GAP = 0.5 + 0.0001;
	public static final double VERTICAL_GAP = 0.5 + 0.001;
	public static final double BAR_GAP = 0.5 + 0.01;

	// Define intervals
	static final int MOVE_INTERVAL = 20 + 1;
	static int SWAP_INTERVAL;

	// number of bars
	int barNum;

	// map dancer id to bar
	public static Map<Integer, Integer> idToBar = new HashMap<>();;

	// store population of each group
	Map<Integer, Integer> groupToNum;

	// store bars
	public static List<Bar> bars;

	// store known soulmates
	List<Integer> soulmates;

	// Define dance period
	int count = 1;

	@Override
	public void init(int d, int room_side) {
		// store params
		Player.d = d;
		Player.roomSide = room_side;

		// decide the number of bars
		int BAR_VOLUME = 80;
		barNum = (int) Math.ceil((d + 0.0) / BAR_VOLUME);
		System.out.format("Need %d bars\n", barNum);

		groupToNum = new HashMap<>();
		soulmates = new ArrayList<>();
	}

	@Override
	public Point[] generate_starting_locations() {
		// decide the center of each bar
		double firstX = 0.25 + 0.001;
		double firstY = 10.0;

		// calculate the people assigned to each bar
		int headCount = d / barNum;

		// ensure this is even number

		this.bars = new ArrayList<>();
		int pid = 0;

		// Decide the population of each group
		int contained = 0;
		int groupPop = 0;
		for (int i = 0; i < barNum - 1; i++) {
			int toput;
			if (headCount % 2 == 0) {
				toput = headCount;
				groupPop = toput;
			} else {
				if (i % 2 == 0) {
					toput = headCount - 1;
					groupPop = toput;
				} else {
					toput = headCount + 1;
				}
			}
			groupToNum.put(i, toput);
			contained += toput;
		}

		if (groupPop == 0) {
			System.out.println("Error: 0 people in the group!");
		} else {
			/*
			 * Decide the interval of swapping
			 */
//			 SWAP_INTERVAL = groupPop * MOVE_INTERVAL - 1;
			// SWAP_INTERVAL = 20;
			SWAP_INTERVAL = 1801;
			/* Disabled swapping for now */

			System.out.println("The small group decides when to swap: " + SWAP_INTERVAL);
		}

		// last group
		groupToNum.put(barNum - 1, d - contained);

		// Put people to group
		for (int i = 0; i < barNum; i++) {
			System.out.println("Index " + i);
			// Find center point
			Point centerPoint = new Point(firstX + i * (BAR_GAP + HORIZONTAL_GAP), firstY);
			System.out.format("Center point is: (%f, %f)", centerPoint.x, centerPoint.y);

			// decide head count
			int pop = groupToNum.get(i);

			System.out.format("The bar is to have %d people\n", pop);
			Bar newBar = new Bar(pop, centerPoint, i);
			this.bars.add(newBar);

			// Set bar flags
			newBar.setBottomConnected(true);
			newBar.setUpConnected(true);

			if (i == 0)
				newBar.setBottomConnected(false);

			if (i == barNum - 1) {
				if (i % 2 == 0) {
					newBar.setUpConnected(false);
				} else {
					newBar.setBottomConnected(false);
				}
			}

			if (i % 2 == 0)
				newBar.setEven(true);
			else
				newBar.setEven(false);

			// store the mapping
			int idEnd = contained + pop;
			for (int j = contained; j < idEnd; j++) {
				idToBar.put(pid++, i);
			}
		}

		// debug
		for (int i = 0; i < barNum; i++) {
			Bar b = bars.get(i);
			b.debrief();
		}

		// generate return values
		List<Point> result = new LinkedList<>();
		for (int i = 0; i < barNum; i++) {
			Bar theBar = bars.get(i);
			List<Point> thePoints = theBar.getPoints();
			result.addAll(thePoints);
		}

		return result.toArray(new Point[this.d]);
	}

	@Override
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		Point[] result = new Point[dancers.length];

		// find all moves that are affected by a soulmate pairing
		// Map<Integer, Point> soulmateMoves = getSoulmateMoves(dancers,
		// partner_ids, enjoyment_gained);
		Map<Integer, Point> soulmateMoves = getDummySoulmateMoves(dancers, partner_ids, enjoyment_gained);
		/* Disable soulmate movement for now */

		System.out.println("Soulmate moves: " + soulmateMoves.size());

		// for those soulmates the move is decided
		for (Integer key : soulmateMoves.keySet()) {
			System.out.println("Move: " + key + ": " + soulmateMoves.get(key).x + ", " + soulmateMoves.get(key).y);
			result[key] = soulmateMoves.get(key);
		}

		// When a cycle has completed we perform a swap inside the bar
		if (count % SWAP_INTERVAL == 0) {
			System.out.println("Turn " + count + ", swap within bar.");

			result = new Point[dancers.length];

			for (int i = 0; i < dancers.length; i++) {
				if (soulmateMoves.containsKey(i))
					continue;

				// find the bar this dancer is in
				int id = i;
				Point dancer = dancers[i];
				int barId = idToBar.get(id);
				Bar theBar = bars.get(barId);

				// decide how the swapping goes in the bar
				Point newLoc = theBar.innerSwap(dancer);
				result[i] = newLoc;
			}

		}
		// For every 21 turns we spend 1 turn to move
		else if (count % MOVE_INTERVAL == 0) {
			System.out.println("Turn " + count + ",move.");
			// move the player with its group
			result = new Point[dancers.length];

			for (int i = 0; i < dancers.length; i++) {
				if (soulmateMoves.containsKey(i))
					continue;

				Point dancer = dancers[i];
				// System.out.format("Dancer before movement: (%f, %f)\n",
				// dancer.x, dancer.y);

				int id = i;
				int barId = idToBar.get(id);
				Bar theBar = bars.get(barId);
				Point newLoc = theBar.move(dancer, id, idToBar);
				result[i] = newLoc;

			}
		} else {
			result = new Point[dancers.length];
			for (int i = 0; i < dancers.length; i++) {
				if (soulmateMoves.containsKey(i))
					continue;
				result[i] = new Point(0, 0);
			}
		}
		count++;
		return result;
	}

	private Map<Integer, Point> getDummySoulmateMoves(Point[] dancers, int[] partner_ids, int[] enjoyment_gained) {
		return new HashMap<>();
	}

	// gets the soulmate moves
	private Map<Integer, Point> getSoulmateMoves(Point[] dancers, int[] partner_ids, int[] enjoyment_gained) {
		Map<Integer, Point> soulmateMoves = new HashMap<Integer, Point>();
		int[] nextPair = { -1, -1 }; // presets the next found soulmate to not
										// found

		// loops through already known soulmates to check that they are at the
		// bottom of the bar
		boolean allSet = true;
		for (int i = 0; i < soulmates.size(); i++) {
			int danceID = soulmates.get(i);
			Bar theBar = bars.get(idToBar.get(danceID));
			if (theBar.inPlace(dancers[danceID])) { // in place checks that
													// they're under the bottom
				soulmateMoves.put(danceID, new Point(0, 0));
			} else {
				allSet = false;
			}
		}

		// allSet false means currently moving a pair down so ignor any new
		// soulmates, true means time to check for a new pair
		if (allSet) {
			boolean newPair = false;
			for (int i = 0; i < dancers.length; i++) {
				// loops through dancers, if enjoyment is 6, with a soulmate
				if (enjoyment_gained[i] == 6 && !soulmates.contains(i)) {
					// picks the first pair it finds and doesn't over ride
					if (!newPair) {
						newPair = true;
						nextPair[0] = i;
						nextPair[1] = partner_ids[i];
						soulmates.add(i);
						soulmates.add(partner_ids[i]);
					}
				}
			}
		}
		// continues following the already found soulmate pair since it is not
		// at the bottom
		else if (soulmates.size() > 0) {
			nextPair[0] = soulmates.get(soulmates.size() - 2);
			nextPair[1] = soulmates.get(soulmates.size() - 1);
		}

		System.out.print("Next pair: " + nextPair[0] + ", " + nextPair[1]);
		if (nextPair[0] >= 0 && nextPair[1] >= 0) {
			// prints current location of pair
			System.out.print("(" + dancers[nextPair[0]].x + ", " + dancers[nextPair[0]].y + ")");
			System.out.println("(" + dancers[nextPair[1]].x + ", " + dancers[nextPair[1]].y + ")");
			Bar theBar = bars.get(idToBar.get(nextPair[0]));
			soulmateMoves.putAll(theBar.doSoulmateMove(dancers, nextPair[0], nextPair[1])); // moves
																							// //
																							// involved
		}
		return soulmateMoves;
	}
}
