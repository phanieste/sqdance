package sqdance.g7;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import sqdance.sim.Point;

public class Belt {
	int numDancers;
	Map<Integer,Point> indexToPositionSide;
	ArrayList<Dancer> dancerList;
	
	private Map<Integer,Integer> beltIndexToRowIndex;
	private Map<Integer,Integer> beltIndexToColumnIndex;
	
	Point[][] tablePositions;
	
	boolean beltParity = false;
	
	private static int MaxNumRows = 19;
	private static int MaxNumCols = 39;
	private static int MaxPairNum = 2;
	private static int MaxContinuousDancers = 2;
	
	public Belt(int numDancers){
		if(numDancers%2!=0)
			throw new RuntimeException("Number of Dancers must be even.");
		
		this.numDancers = numDancers;
		
		initializeTablePositions(numDancers+1);
		
		indexToPositionSide = new HashMap<Integer,Point>();
		beltIndexToRowIndex = new HashMap<Integer,Integer>();
		beltIndexToColumnIndex = new HashMap<Integer,Integer>();
		dancerList = new ArrayList<Dancer>();
		
		int numCols = MaxNumCols;
		int numRows = (numDancers/2)/numCols * 2;
		numRows += (numDancers%numCols==0)? 0 : 2;
		
		int row_index = 0;
		int col_index = -1;
		int counter = 0;
		int dancersOnRow = MaxNumCols;	// the number of dancers on current row
		boolean goingRight = true;
		boolean goingDown = true;

		for(int i=0; i<numDancers; i++, counter++){

			// reset the counter, directions and dancersOnRow
			if (counter == dancersOnRow) {

				goingRight = !goingRight;
				if (row_index == numRows - 2) { // second last row
					row_index += 1;
				} else if (row_index == numRows -1) { // last row
					row_index -= 2;
					goingDown = !goingDown;
				} else {
					row_index += goingDown ? 2 : -2;
				}

				dancersOnRow = getNumOfCols(numDancers, numRows, row_index);
				col_index = getNextColStartPos(goingRight, row_index, numRows, dancersOnRow);
				counter = 0;
			}

			col_index += goingRight ? 1 : -1;
			//System.out.println(i + "---" + row_index + ":" + col_index);
			indexToPositionSide.put(i, tablePositions[row_index][col_index]);
			beltIndexToRowIndex.put(i, row_index);
			beltIndexToColumnIndex.put(i, col_index);
			 

		}
		for(int i=0; i<numDancers;i++){
			dancerList.add(new Dancer(i, i));
		}
		
	}
	

	private int getNextColStartPos(boolean goingRight, int row_index, int numRows, int dancersOnRow) {
		if (row_index == numRows - 1) {
			return goingRight ? MaxNumCols-dancersOnRow-1 : dancersOnRow;
		} else {
			return goingRight ? -1 : MaxNumCols;
		}
	}
	private boolean isLastBelt(int row_index, int numRows) {
		return (row_index == numRows - 1) || (row_index == numRows - 2);
	}

	private int getNumOfCols(int numDancers, int numRows, int row_index) {
		if (!isLastBelt(row_index, numRows)) {
			return MaxNumCols;
		} else {
			return (numDancers - (numRows-2)*MaxNumCols)/2;
		}
	}

	public void initializeTablePositions(int numDancers){
		tablePositions = new Point[38][39];
		for(int row=0; row<38; row++){
			for(int column=0; column<39; column++){
				double offSet = (0.01) * (row/2);
				tablePositions[row][column] = new Point(column*0.51,row*0.5001 + offSet);
			}
		}
	}
	
	public Point getPosition(int i){
		return indexToPositionSide.get(i);
	}
	
	public Point[] spinBelt(Set<Integer> curDancers){
		Point[] instructions = new Point[numDancers];
		for (int i=0 ; i<numDancers ; ++i) {
			instructions[i] = new Point(0, 0);
		}
		

		/*for (Integer x : curDancers) {
			System.out.print("," + x);
		}

		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();*/
		
		for(int i=0; i<numDancers; i++){
			Dancer oldDancer = dancerList.get(i);
			if (curDancers.contains(oldDancer.dancerId)){
				continue;
			} 
//			Dancer newDancer = dancerList.get((i+1)%numDancers);
			int oldBeltIndex = oldDancer.beltIndex; 
			int newBeltIndex = (oldBeltIndex + 1)%numDancers;

			while(curDancers.contains(beltIndexToDancer(newBeltIndex))){
				newBeltIndex = (newBeltIndex+1)%numDancers;
			}

			Point oldPos = getPosition(oldBeltIndex);
			Point newPos = getPosition(newBeltIndex);
			//oldDancer.beltIndex = newBeltIndex;
			//System.out.println("HHHHH");
			Point instruct = new Point(newPos.x-oldPos.x, newPos.y-oldPos.y); 
			instructions[i] = instruct;
		}

		for(int i=0; i<numDancers ;i++){
			Dancer oldDancer = dancerList.get(i);
//			Dancer newDancer = dancerList.get((i+1)%numDancers);
			if (curDancers.contains(oldDancer.dancerId)) {
				continue;
			} 
			int oldBeltIndex = oldDancer.beltIndex; 
//			int newBeltIndex = newDancer.beltIndex;
			int newBeltIndex = (oldBeltIndex + 1)%numDancers;
			while(curDancers.contains(beltIndexToDancer(newBeltIndex))){
				newBeltIndex = (newBeltIndex+1)%numDancers;
			}
			
//			newBeltIndex -= (i==numDancers-1) ? 1 : 0;
			
			oldDancer.beltIndex = newBeltIndex; 
		}

		//System.out.println(instructions[0])
		return instructions;
	}
	
	public int[] CoordinatePosition(int beltIndex){
		return new int[]{beltIndexToRowIndex.get(beltIndex),beltIndexToColumnIndex.get(beltIndex)};
	}
	
	public int getPartnerBeltIndex(int beltIndex){
		return numDancers-1-beltIndex;
	}
	
	//ToDo: Easy to improve performance here but we're lazy and this is good enough.
	public int beltIndexToDancer(int beltIndex){
		for(Dancer d : dancerList){
			if(d.beltIndex==beltIndex)
				return d.dancerId;
		}
		return -1;
	}

	public int getPartnerDancerID(int dancerId) {
		int beltIndex = dancerList.get(dancerId).beltIndex;
		int partnerBeltIndex = getPartnerBeltIndex(beltIndex);
		return beltIndexToDancer(partnerBeltIndex);
	}


	
	// UGLY !!!!! But I am soooooooooo  sleepy
	public Set<Integer> verifyDancer(Set<Dancer> curDancers) {
		//System.out.println(curDancers.size());
		Set<Integer> validDancers = new HashSet<>();
		if (curDancers.size() == numDancers) { 
			for (Dancer d : curDancers) {
				validDancers.add(d.dancerId);
			}
			return validDancers;
		}
		
		int[][] dancerMatrix = new int[MaxNumRows*2][MaxNumCols];
		Map<String, Integer> posToDancerID = new HashMap<>();

		for (Dancer d : curDancers) {

			int[] pos = CoordinatePosition(d.beltIndex);
			int row_index = pos[0];
			int col_index = pos[1];

			dancerMatrix[row_index][col_index] = 1;
			String key = row_index + "#" + col_index;
			posToDancerID.put(key, d.dancerId);
			validDancers.add(d.dancerId);

		}

		for (int i=0 ; i<dancerMatrix.length ; ++i) {
			int continuousDancers = 0;
			for (int j=0 ; j<dancerMatrix[0].length ; ++j) {
				if (dancerMatrix[i][j] == 1) {
					++continuousDancers;
				} else {
					continuousDancers = 0;
				}

				if (continuousDancers > MaxContinuousDancers) {
					String key = i + "#" + j;
					int dancerId = posToDancerID.get(i + "#" + j);
					validDancers.remove(dancerId);

					dancerMatrix[i][j] = 0;
					continuousDancers = 0;
				}

			}
				
		}

		return validDancers;

	}

	private void printMatrix(int[][] dancerMatrix) {
		for (int i=0 ; i<dancerMatrix.length ; ++i) {
			for (int j=0 ; j<dancerMatrix[0].length ; ++j)
				System.out.print(dancerMatrix[i][j]);
			System.out.println();
		}

		System.out.println();
		System.out.println();
	}
	
}
 