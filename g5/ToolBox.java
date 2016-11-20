package sqdance.g5;

import sqdance.g5.Player;
import sqdance.sim.*;

public class ToolBox {

	public static boolean validatePoint(Point loc, double roomSide) {
		return loc.x >= 0 && loc.y >= 0 && loc.x < roomSide && loc.y < roomSide;
	}

	public static boolean compareDoubles(double a, double b) {
		if (Math.abs(a - b) < 0.01)
			return true;
		else
			return false;
	}

	public static boolean comparePoints(Point a, Point b) {
		if (a == null || b == null)
			return false;
		return compareDoubles(a.x, b.x) && compareDoubles(a.y, b.y);
	}

	public static int findRelativeIdInBar(Point p, Bar b) {
		// check whether it's in this bar
		double diffX = p.x - b.center.x;
		if (Math.abs(diffX) > 0.26) {
			System.out.println("Point " + p + "is not in bar centering at " + b.center);
			return -1;
		}

		// calculate the row number
		double diffY = p.y - b.topLeft.y;
		int row = (int) Math.round(diffY / Player.VERTICAL_GAP);
		int index;
		if (p.x < b.center.x)
			return row * 2;
		else
			return row * 2 + 1;
	}

	public static Point pointsDifferencer(Point a, Point b) {
		return new Point(b.x - a.x, b.y - a.y);
	}
}
