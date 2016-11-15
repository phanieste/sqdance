package sqdance.g5;

import sqdance.sim.Point;
import java.util.*;

public class Player implements sqdance.sim.Player {

	static int d;
	static int roomSide;

	// Define gaps
	static final double HORIZONTAL_GAP = 0.5 + 0.0001;
	static final double VERTICAL_GAP = 0.5 + 0.001;
	static final double BAR_GAP = 0.5 + 0.01;

	int barNum;

	Map<Integer, Integer> idToBar;
	Map<Integer, Integer> groupToNum;

	List<Bar> bars;

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

		idToBar = new HashMap<>();
		groupToNum = new HashMap<>();
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
		for (int i = 0; i < barNum - 1; i++) {
			int toput;
			if (headCount % 2 == 0) {
				toput = headCount;
			} else {
				if (i % 2 == 0) {
					toput = headCount - 1;
				} else {
					toput = headCount + 1;
				}
			}
			groupToNum.put(i, toput);
			contained += toput;
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
			this.bars.add(new Bar(pop, centerPoint));

			// store the mapping
			int idEnd = contained + pop;
			for (int j = contained; j < idEnd; j++) {
				idToBar.put(pid++, i);
			}
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
		Point[] result;
		if (count % 21 == 0) {
			System.out.println("Now move");
			// move the player with its group
			result = new Point[dancers.length];
			for (int i = 0; i < dancers.length; i++) {
				Point dancer = dancers[i];
				System.out.format("Dancer before movement: (%f, %f)\n", dancer.x, dancer.y);

				int id = i;
				int barId = idToBar.get(id);
				Bar theBar = bars.get(barId);

				Point newLoc = theBar.move(dancer);
				System.out.format("Dancer after movement: (%f, %f)\n", newLoc.x, newLoc.y);

				result[i] = newLoc;
			}

		} else {
			result = new Point[dancers.length];
			for (int i = 0; i < dancers.length; i++) {
				result[i] = new Point(0, 0);
			}
		}
		count++;
		return result;

	}

	public static class Bar {
		Point center;
		Point topLeft;
		Point bottomRight;

		int number;

		List<Point> spots = new ArrayList<>();

		public Bar(int number, Point center) {
			// store params
			this.number = number;
			this.center = center;

			// calculate starting points of the two rows
			int column = number / 2;
			int halfRow;
			Point startLeft;
			Point startRight;
			if (column % 2 == 0) {
				System.out.println("Even number in a column");
				halfRow = column / 2;
				startLeft = new Point(center.x - 0.5 * HORIZONTAL_GAP,
						center.y - halfRow * VERTICAL_GAP + 0.5 * VERTICAL_GAP);
				startRight = new Point(center.x + 0.5 * HORIZONTAL_GAP,
						center.y - halfRow * VERTICAL_GAP + 0.5 * VERTICAL_GAP);

			} else {
				System.out.println("Odd number in a column");
				halfRow = (column - 1) / 2;
				startLeft = new Point(center.x - 0.5 * HORIZONTAL_GAP, center.y - halfRow * VERTICAL_GAP);
				startRight = new Point(center.x + 0.5 * HORIZONTAL_GAP, center.y - halfRow * VERTICAL_GAP);
			}
			System.out.println("Starting points:");
			System.out.format("Left start: (%f, %f)", startLeft.x, startLeft.y);
			System.out.format("Right start: (%f, %f)", startRight.x, startRight.y);

			// store topleft and bottomright
			topLeft = startLeft;
			bottomRight = new Point(startRight.x + 0.5 * HORIZONTAL_GAP, startRight.y + (column - 1) * VERTICAL_GAP);
			System.out.format("Top left: (%f, %f)", topLeft.x, topLeft.y);
			System.out.format("Bottom right: (%f, %f)", bottomRight.x, bottomRight.y);

			// assign people to points
			for (int i = 0; i < column; i++) {
				Point leftPlayer = new Point(startLeft.x, startLeft.y + i * VERTICAL_GAP);
				Point rightPlayer = new Point(startLeft.x + HORIZONTAL_GAP, startLeft.y + i * VERTICAL_GAP);
				spots.add(leftPlayer);
				spots.add(rightPlayer);
			}
			System.out.println("Now " + spots.size() + " players are assigned.");

		}

		public List<Point> getPoints() {
			return this.spots;
		}

		public void setSpots(List<Point> newSpots) {
			if (spots.size() != newSpots.size()) {
				System.out.println("Whaaaaat? The new spot size has changed!");
				return;
			} else {
				this.spots = newSpots;
			}
		}

		public Point move(Point dancer) {
			Point newLoc;
			// check left column
			if (dancer.x < center.x) {
				// if it's the first element in left column, go right
				if (compareDoubles(dancer.y, topLeft.y)) {
					// newLoc = new Point(dancer.x + HORIZONTAL_GAP, dancer.y);
					newLoc = new Point(HORIZONTAL_GAP, 0);
				}
				// else, just go up
				else {
					// newLoc = new Point(dancer.x, dancer.y - VERTICAL_GAP);
					newLoc = new Point(0, -VERTICAL_GAP);
				}
			}
			// right column
			else {
				// if it's the end of the right column
				if (compareDoubles(dancer.y, bottomRight.y)) {
					// newLoc = new Point(dancer.x - HORIZONTAL_GAP, dancer.y);
					newLoc = new Point(-HORIZONTAL_GAP, 0);
				}
				// else go down
				else {
					// newLoc = new Point(dancer.x, dancer.y + VERTICAL_GAP);
					newLoc = new Point(0, VERTICAL_GAP);
				}
			}

			Point newPos = dancer.add(newLoc);
			if (!ToolBox.validatePoint(newPos, Player.roomSide)) {
				System.out.format("Error: Invalid point (%f, %f)\n", newLoc.x, newLoc.y);
				return dancer;
			} else {
				return newLoc;
			}
		}
	}

	public static boolean compareDoubles(double a, double b) {
		if (Math.abs(a - b) < 0.0001)
			return true;
		else
			return false;
	}
}
