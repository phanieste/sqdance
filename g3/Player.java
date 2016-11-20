package sqdance.g3;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
import java.lang.System.*;

public class Player implements sqdance.sim.Player {
    // We regard these as constants since we don't modify them after the call to init()
	private int D;
	private double ROOM_SIDE;
	private Point[] GRID_POSITIONS;

	private int MAX_DANCE_DURATION = 120;
    private double Y_OFFSET = 0.42;

	private int[] dancerPositions;

	private int danceDuration;

	private double VALID_DISTANCE = 0.5 + 1e-3;
	private double INVALID_DISTANCE = VALID_DISTANCE + 1e-3;

	public void init(int d, int roomSide) {
		this.D = d;
		this.ROOM_SIDE = (double) roomSide;
		this.danceDuration =  0;

		ArrayList<ArrayList<Point>> positions = new ArrayList<ArrayList<Point>>();
        int gridNumber = 0;
        int columnNumber = 0;
        double x = 0;

        while (gridNumber < D) {
            positions.add(new ArrayList<Point>());

            if (columnNumber % 2 == 0) {
                for (double y = 0; y < this.ROOM_SIDE && gridNumber < D;
                     y += INVALID_DISTANCE) {
                    positions.get(columnNumber).add(new Point(x, y));
                    gridNumber += 2;
                }
            } else {
                for (double y = this.ROOM_SIDE - Y_OFFSET; y > 0 && gridNumber < D;
                     y -= INVALID_DISTANCE) {
                    positions.get(columnNumber).add(new Point(x, y));
                    gridNumber += 2;
                }
            }

            columnNumber++;
            x += VALID_DISTANCE + INVALID_DISTANCE;
        }

		this.GRID_POSITIONS = new Point[d];
		this.dancerPositions = new int[d];

		for (int i = 0; i < D; i++){
			this.dancerPositions[i] = i;
		}

		int positionIndex = 0;
		for (int i = 0; i < columnNumber; i++) {
			for (int j = 0; j < positions.get(i).size(); j++) {
				Point p = positions.get(i).get(j);
				if (i % 2 == 1) {
					GRID_POSITIONS[positionIndex] = new Point(p.x + VALID_DISTANCE, p.y);
				} else {
					GRID_POSITIONS[positionIndex] = new Point(p.x, p.y);
				}

                positionIndex++;
            }
		}

		for (int i = columnNumber - 1; i >= 0; --i) {
			for (int j = positions.get(i).size() - 1; j >= 0; --j) {
				Point p = positions.get(i).get(j);
				if (i % 2 == 0){
					GRID_POSITIONS[positionIndex] = new Point(p.x + VALID_DISTANCE, p.y);
				} else {
					GRID_POSITIONS[positionIndex] = new Point(p.x, p.y);
				}

                positionIndex++;
			}
		}	
	}

	public Point[] generate_starting_locations() {
		return GRID_POSITIONS;
	}

	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids,
                        int[] enjoyment_gained) {
        // If we've got some potential enjoyment left, stay put
		if (danceDuration < MAX_DANCE_DURATION){
			danceDuration += 6;

            Point[] no_move = new Point[D];
            for (int i = 0; i < D; i++) {
                no_move[i] = new Point(0.0, 0.0);
            }

            return no_move;
		}

        // Otherwise, move each player to the next position
        danceDuration = 0;
		Point[] updatedLocations = new Point[D];
		for (int i = 0; i < D; i++) {
            this.dancerPositions[i] = (this.dancerPositions[i] + 1) % D;
            updatedLocations[i] = new Point(GRID_POSITIONS[this.dancerPositions[i]].x - dancers[i].x,
                                            GRID_POSITIONS[this.dancerPositions[i]].y - dancers[i].y);
		}

		return updatedLocations;
	}
}
