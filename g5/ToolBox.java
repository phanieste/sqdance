package sqdance.g5;

import sqdance.sim.*;

public class ToolBox {

	public static boolean validatePoint(Point loc, double roomSide) {
		return loc.x >= 0 && loc.y >= 0 && loc.x < roomSide && loc.y < roomSide;
	}
}
