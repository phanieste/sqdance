package sqdance.g7;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sqdance.sim.Point;

public class Belt {
	int numDancers;
	Map<Integer,Point> indexToPositionSide;
	ArrayList<Dancer> dancerList;
	
	Point[][][] tablePositions;
	
	boolean beltParity = false;
	
	private static int numRows = 19;
	private static int numCols = 39;
	private static int pairNum = 2;
	
	public Belt(int numDancers){
		if(numDancers%2!=0)
			throw new RuntimeException("Number of Dancers must be even.");
		
		this.numDancers = numDancers;
		
		initializeTablePositions(numDancers+1);
		
		indexToPositionSide = new HashMap<Integer,Point>();
		dancerList = new ArrayList<Dancer>();
		
		for(int i=0; i<numDancers/2 + 1; i++){
			//indexToPositionSide.put(i, tablePositions[i/(39*2)][i%39][(i%(39*2))/39]);
			//indexToPositionSide.put(i, tablePositions[i/(39*2)][(i%(39*2))/2][i%2]);
			
			//indexToPositionSide.put(i, tablePositions[(i%(numDancers/2 + 1))/numRows][(i%(numDancers/2 + 1))%numRows][i/(numDancers/2 + 1)]);
			
			System.out.println(i);
			//Top side
			indexToPositionSide.put(i, tablePositions[i/numCols][i%numCols][0]);
			//Bottom Side
			if(i/numCols == (numDancers/2 + 1)/numCols){
				System.out.println((numDancers/2 + 1)%numCols - i%numCols);
				indexToPositionSide.put(numDancers/2 + 1 + i, tablePositions[i/numCols][(numDancers/2)%numCols + 1 - i%numCols][1]);
			}
			else
				indexToPositionSide.put(numDancers/2 + 1 + i, tablePositions[i/numCols][numCols - 1 - i%numCols][1]);
			
//			if(i<numDancers/2)
//				dancerList.add(new Dancer(i, i+1));
//			else
//				dancerList.add(new Dancer(i,i));
			//System.out.println("[" + i +"]:\t" + tablePositions[i/(39*2)][(i%(39*2))/2][i%2].x + "," + tablePositions[i/(37*2)][(i%(39*2))/2][i%2].y);
		}
		for(int i=0; i<numDancers;i++){
			dancerList.add(new Dancer(i, i+1));
		}
		
	}
	
	public void initializeTablePositions(int numDancers){
		tablePositions = new Point[19][39][2];
		
		for(int row=0; row<19; row++){
			for(int column=0; column<39; column++){
				for(int pairMem=0; pairMem<2; pairMem++){
					//This will keep the right hand side empty. I'll need this row for later.
					tablePositions[row][column][pairMem] = new Point(column*0.51,row*1.011 + pairMem*0.501);
//					if(pairMem==0)//BottomSide
//						tablePositions[row][column][pairMem] = new Point(column*0.51,row*1.011 + pairMem*0.501);
//					else //Topside
//						tablePositions[row][column][pairMem] = new Point((39-1-column)*0.51,row*1.011 + pairMem*0.501);
				}
			}
		}
	}
	
	public Point getPosition(int i){
		return indexToPositionSide.get(i);
	}
	
	private boolean cycleParity=true;
	public Point[] spinBelt(){
		Point[] instructions = new Point[numDancers];
		
		if(cycleParity){
			for(int i=0; i<numDancers;i++){
				Dancer oldDancer = dancerList.get(i);
				if(oldDancer.beltIndex<numDancers/2+1){
					//Dancer oldDancer = dancerList.get(i);
					int oldBeltIndex = oldDancer.beltIndex; 
					int newBeltIndex = oldDancer.beltIndex+1;
					Point oldPos = getPosition(oldBeltIndex);
					Point newPos = getPosition(newBeltIndex);
					oldDancer.beltIndex = newBeltIndex;
					Point instruct = new Point(newPos.x-oldPos.x, newPos.y-oldPos.y); 
					instructions[i] = instruct;
				}
			}
			cycleParity = !cycleParity;
		}
		else{
			for(int i=0; i<numDancers;i++){
				Dancer oldDancer = dancerList.get(i);
				int oldBeltIndex = oldDancer.beltIndex; 
				
				int newBeltIndex;
				if(oldBeltIndex==numDancers)
					newBeltIndex=1;
				else{
					Dancer newDancer = dancerList.get(i+1);
					newBeltIndex = newDancer.beltIndex;
				}
				
				Point oldPos = getPosition(oldBeltIndex);
				Point newPos = getPosition(newBeltIndex);
				oldDancer.beltIndex = newBeltIndex;
				Point instruct = new Point(newPos.x-oldPos.x, newPos.y-oldPos.y); 
				instructions[i] = instruct;
			}
			cycleParity = !cycleParity;
		}
		return instructions;
	}
	
}
 
