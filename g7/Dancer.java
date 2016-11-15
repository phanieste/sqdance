package sqdance.g7;

import java.util.HashMap;
import java.util.Map;

import sqdance.sim.Point;

public class Dancer {
	int dancerId = -1;
	int beltIndex = -1;


	Map<Integer, Integer> friendToTime;
	Map<Integer, Integer> strangerToTime;
	int soulMate = -1;
	
	
	public Dancer(int dancerId, int beltIndex){
		friendToTime = new HashMap<Integer,Integer>();
		strangerToTime = new HashMap<Integer,Integer>();
		
		this.dancerId = dancerId;
		this.beltIndex = beltIndex;
	}
	
}
