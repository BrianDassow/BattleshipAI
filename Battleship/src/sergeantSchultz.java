/*
 *  Author: Mitchell Zimmer
 *  Date: battleship AI competition for Fall 2014
 *  
 * 
 * 
 * 
 */

import java.util.*;

public class sergeantSchultz implements Captain {

    protected Random generator;
    protected Fleet myFleet;
    
    /* Board for holding attacks and ship positions
    *  The board is cleared before each game.
    * Data: -2 for untouched
    * 		 0 for patrol boat
    *		 1 for Destroyer
    *		 2 for submarine
    *		 3 for Battleship
    *		 4 for Aircraft_carrier
    *		 6 for miss
    */
    protected int[][] stalag13 = new int[10][10];
    
    //Array for holding the "all time stats on the current opponent.
    protected double[][] russianArmy = new double[10][10];
    
    // Structure for holding the stats for the enemy's ships.
    protected int[][][] russianFront = new int[10][10][5];

    //Used for keeping track of my own ship placement.
    protected double[][] england = new double[10][10];
    
    protected int frenchSize;
    protected coordExtend[][] leader = new coordExtend[10][10];
    protected int[][] frenchArmy = new int[10][10];
    protected int[][] UN = new int[10][10];
    protected double[][] France = new double[10][10];
    
    //ArrayList for the new ships that are found on the board. 
    protected LinkedList<Coordinate> newPrisoner = new LinkedList<>();
    
    //Variable tell if a ship is sighted for the ship finishing method.
    protected boolean iSeeNothing;
    
    protected int enemyMoveCount;
    protected int myMoveCount;
    
    protected LinkedList<Coordinate> leftBarrack = new LinkedList<>();
    protected LinkedList<Coordinate> rightBarrack = new LinkedList<>();
    protected LinkedList<Coordinate> upBarrack = new LinkedList<>();
    protected LinkedList<Coordinate> downBarrack = new LinkedList<>();
    protected String currOpp = "";
    protected int shipDirection;
    protected int currShip;
    protected boolean[] shipsLeft = new boolean[5];
    protected int longestShip = 4;
     protected int lastHitX, lastHitY;
    protected int currX, currY;
    protected int gameCount;
    
    //Variables for collecting data.
    protected boolean stats = false;
    protected int oppHitCount;
    protected int oppGameHitCount = 0;
    protected int myHitCount;
    protected int [][] oppHitMap = new int [10][10];
     /*
     * @param numMatches	The number matches you will be engaging in with this
     * opponent.
     * @param numCaptains	The number of opponents you will be facing during the
     * current set of battles.
     * @param opponent	The name of your opponent for this match
     */
    class coordExtend {
    	int x, y;
    	coordExtend (int pX, int pY) {
    		x = pX;
    		y = pY;
    	}
    	private double weight() {
    		boolean[] valid = {false, false, false, false};
    		int sum = frenchArmy[x][y];
    		if (!(y+1>9)) {
    			valid[0] = true;
    			sum += frenchArmy[x][y+1];
    		} if (!(y-1<0)) {
    			valid[1] = true;
    			sum += frenchArmy[x][y-1];
    		} if (!(x+1>9)) {
    			valid [2] = true;
    			sum += frenchArmy[x+1][y];
    		} if (!(x-1<0)) {
    			valid [3] = true;
    			sum += frenchArmy[x-1][y];
    		}
    		int validSq = 1;
    		for(int i=0; i<4; i++) {
    			if (valid[0] = true) {
    				validSq++;
    			}
    		}
    		return (sum / validSq);
    	}
    }
    
    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        generator = new Random();
        myFleet = new Fleet();
        
        if (gameCount %1000 == 0) {
        	frenchArmy = new int[10][10];
        	frenchSize = 0;
        }
        //Code for making the changes for a new opponent. 
         if (!currOpp.equals(opponent)) {
        	currOpp = opponent;
        	russianFront = new int[10][10][6];
        	england = new double[10][10];
        	UN = new int[10][10];
        	leader = new coordExtend[10][10];
        	gameCount = 0;
        	for(int i=0; i<10; i++) {
        		for (int j=0; j<10; j++) {
        			leader[i][j] = new coordExtend(i,j);
        		}
        	}
        }
         
         for (int i=0; i <10;i++) {
        	 for(int j=0;j<10;j++) {
        		 stalag13[i][j] = -2;
        	 }
         }
        //resetting the finishing variables.
        iSeeNothing = true;
        myMoveCount = 0;
        shipDirection = 0;
        for(int i =0; i< 5 ; i++) {
        	shipsLeft[i] = true;
        }
        // Defense -  Placement of the ships onto the fleet object.
        
        
        	for(int i=0; i<10; i++) {
            	for(int j=0; j<10; j++) {
            		France[i][j] = leader[i][j].weight();
            	}
            }
        	Coordinate low = new Coordinate(0,0);
        	for(int i=0; i<10; i++) {
            	for(int j=0; j<10; j++) {
            		if (France[i][j] <= France[low.getX()][low.getY()]) {
            			low = new Coordinate(i,j);
            		}
            	}
        	}
        	//while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), PATROL_BOAT)) {}
        	PatrolBoatPlace(low);
        
        for(int i=1; i<5; i++) {
        	while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
            }
        }
    }

    private void PatrolBoatPlace(Coordinate pC) {
    	
	    	if (!myFleet.placeShip(pC.getX(), pC.getY(), generator.nextInt(2), PATROL_BOAT)) {
	    		while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), PATROL_BOAT)) {}
	    	}
    	
    }
    
    /**
     * @return A valid fleet representing my ship placements for this round.
     */
    @Override
    public Fleet getFleet() {
        return myFleet;
    }
  
    // #gradeASniffer
    private Coordinate releaseTheDogs(int pLongestShip) {
    	int shipLength = getShipLength(pLongestShip);
    	if (shipLength == 5) {
    		shipLength = 4;
    	}
    	int[][] mode = new int[10][10];
    	for(int i =0; i < 10 ; i++) {
    		int hSum = 0;
    		int vSum = 0;
    		for(int j =0; j< 10 ; j++) {
    			//Calculating the heat for the Vertical section.
    			if (stalag13[i][j] == -2) {
    				vSum++;
    			} else {
    				vSum = 0;
    			}
    			if (vSum == shipLength) {
    				for(int k=0; k<shipLength; k++) {
    					mode[i][j-k]++;
    				}
    				vSum--;
    			}
    			//Calculating the section for the Horizontal section.
    			if (stalag13[j][i] == -2) {
    				hSum++;
    			} else {
    				hSum = 0;
    			}
    			if (hSum == shipLength) {
    				for(int k=0; k<shipLength; k++) {
    					mode[j-k][i]++;
    				}
    				hSum--;
    			}
    		}
    	} // The array is now filled with that heat for the possible ship placements(more possibilities more heat).
    	//printArray(mode);
    	//Code for finding the space that will have the most chance of a ship being there.
    	Coordinate largestValue = new Coordinate(0,0);
    	for(int i =0; i < 10 ; i++) {
    		for(int j =0; j<10; j++) {
    			if (mode[i][j] > mode[largestValue.getX()][largestValue.getY()]) {
    				largestValue = new Coordinate(i,j);
    			}
    		}
    	}
    	if (largestValue.getX() == 0 && largestValue.getY() == 0) {
    		return randomAttack();
    	}
    	return largestValue;
    }
    
    /**
     * @return A valid coordinate on the playing field.
     */
    @Override
    public Coordinate makeAttack() {
    	myMoveCount++;
    	Coordinate nextMove;
    	if (!iSeeNothing) {
    		nextMove = bomb();
    	} else {
    		if (!newPrisoner.isEmpty()) {
    			lastHitX = newPrisoner.getFirst().getX();
    			lastHitY = newPrisoner.getFirst().getY();
    			investigate(getShipLength(stalag13[lastHitX][lastHitY]));
    			newPrisoner.removeFirst();
    			nextMove = bomb();
        		shipDirection = 0;
    		} else {
    			if (myMoveCount < 3) {
    				nextMove = winter();
    			} else {
    				nextMove = releaseTheDogs(longestShip);
    			}
    		}
    	}
    	currX = nextMove.getX();
		currY = nextMove.getY();
		return nextMove;
    }
    
    private Coordinate winter() {
    	Coordinate lowest = new Coordinate(0,0);
    	for(int i =0; i< 10 ; i++) {
    		for(int j =0; j< 10;j++) {
    			if (russianFront[i][j][0] < russianFront[lowest.getX()][lowest.getY()][0]) {
    				lowest = new Coordinate(i,j);
    			}
    		}
    	}
    	return lowest;
    }
    
    
    //chooses a random non attacked coordinate.
    private Coordinate randomAttack() {
    	int x, y;
		do {
			x = (generator.nextInt(10));
			y = (generator.nextInt(10));
		} while (stalag13[x][y] > -2);
		// Setting the value
		currX = x; currY = y;
		return new Coordinate(currX, currY);
    }

    private Coordinate bomb() {
    	 if (!upBarrack.isEmpty()) {
    		 shipDirection = 1;
    		 return upBarrack.removeFirst();
    	 } else if (!leftBarrack.isEmpty()) {
    		 shipDirection = 2;
    		 return leftBarrack.removeFirst();
    	 } else if (!downBarrack.isEmpty()) {
    		 shipDirection = 3;
    		 return downBarrack.removeFirst();
    	 } else if (!rightBarrack.isEmpty()) {
    		 shipDirection = 4;
    		 return rightBarrack.removeFirst();
    	 }
    	return randomAttack();
    }
   /*
    * use method for filling maps of positions
    * void just populates the linked Lists
    */
    private void investigate(int shipLength) {
    	// Filling the LinkedList with all the valid points that are possible for the current hit point.
    	upBarrack.clear(); downBarrack.clear(); rightBarrack.clear(); leftBarrack.clear();
       	//Filling the rightBarracks
    	for(int i = 1; lastHitX + i <= 9 && i < shipLength; i++) {
    		if (stalag13[lastHitX + i][lastHitY] == -2 || stalag13[lastHitX + i][lastHitY] ==  stalag13[lastHitX][lastHitY]) {
    			rightBarrack.add(new Coordinate(lastHitX + i, lastHitY));
    		} else {
    			break;
    		}
    	}
    	//Filling the leftBarrack
    	for(int i = 1; lastHitX - i >= 0 && i < shipLength; i++) {
    		if (stalag13[lastHitX - i][lastHitY] == -2 || stalag13[lastHitX - i][lastHitY] ==  stalag13[lastHitX][lastHitY]) {
    			leftBarrack.add(new Coordinate(lastHitX - i, lastHitY));
    		} else {
    			break;
    		}
    	}
    	//Filling the upBarrack
    	for(int i = 1; lastHitY + i <= 9 && i < shipLength;i++) {
    		if (stalag13[lastHitX][lastHitY + i] == -2 || stalag13[lastHitX][lastHitY + i] ==  stalag13[lastHitX][lastHitY]) {
    			upBarrack.add(new Coordinate(lastHitX, lastHitY + i));
    		} else {
    			break;
    		}
    	}
    	//Filling the downBarrack
    	for(int i = 1; lastHitY - i >= 0 && i < shipLength; i++) {
    		if (stalag13[lastHitX][lastHitY - i] == -2 || stalag13[lastHitX][lastHitY - i] ==  stalag13[lastHitX][lastHitY]) {
    			downBarrack.add(new Coordinate(lastHitX, lastHitY - i));
    		} else {
    			break;
    		}
    	}
    	
    	if (upBarrack.size() + downBarrack.size() + 2 < shipLength) {
    		upBarrack.clear();
    		downBarrack.clear();
    	}
    	if (leftBarrack.size() + rightBarrack.size() + 2 < shipLength) {
    		leftBarrack.clear();
    		rightBarrack.clear();
    	}	
    	
    }
    //Given a ship type method return the length of the ship.
    private int getShipLength(int pShipType) {
		if (pShipType == 0) {
			return 2;
		} else if (pShipType == 1 || pShipType == 2) {
			return 3;
		} else if (pShipType == 3) {
			return 4;
		} else {
			return 5;
		}
    }
    
    @Override
    public void resultOfAttack(int result) {
    		stalag13[currX][currY] = result % 10;
    	//printLinkedLists();
    	//Code for processing the attack if it was a hit.
    	if (result >= 10 && result < 20) {
    		if (iSeeNothing) {
    			iSeeNothing = false;
    			currShip = result %10;
    			lastHitX = currX;
    			lastHitY = currY;
    			investigate(getShipLength(result % 10));
    		//Checking if it is the same ship that we are trying to sink or not.
    		} else if (stalag13[lastHitX][lastHitY] == result % 10) {
    			if (shipDirection % 2 == 1) {
        			rightBarrack.clear();
        			leftBarrack.clear();
        		} else if (shipDirection % 2 == 0) {
        			upBarrack.clear();
        			downBarrack.clear();
        		}
    		} else {
    			
    			if (shipDirection == 1) {
    				upBarrack.clear();
    			} else if (shipDirection == 2) {
    				leftBarrack.clear();
    			} else if (shipDirection == 3) {
    				downBarrack.clear();
    			} else if (shipDirection == 4) {
    				rightBarrack.clear();
    			}
    			newPrisoner.addLast(new Coordinate(currX, currY));
    		}
    		//saving the point with the value for the ship that is there.
    		stalag13[currX][currY] = result % 10;
    	}
    	//Turning off the boolean for the finishing Method.
    	//The ship should be sunk.
    	else if (result >= 20 && result < 26) {
    		iSeeNothing = true;
    		shipDirection = 0;
    		stalag13[currX][currY] = result % 20;
    		shipsLeft[result % 20] = false;
    		
    		for (int j=4; j>=0;j--) {
    			if (shipsLeft[j] == true) {
    				longestShip = j;
    			}
    		}
    	} else if (result == 106) {	
    		//If the hit was a miss and there is ship that is trying to be sunk the List should be emptied.
    		if (!iSeeNothing) {
    			if (shipDirection == 1) {
    				upBarrack.clear();
    			} else if (shipDirection == 2) {
    				leftBarrack.clear();
    			} else if (shipDirection == 3) {
    				downBarrack.clear();
    			} else if (shipDirection == 4) {
    				rightBarrack.clear();
    			}
    		}
    	}
    }

    /**
     * @param coord The spot on the board where your opponent just attacked.
     */
    @Override
    public void opponentAttack(Coordinate coord) {
    	//Save for keeping the stats of the opponent
    	if (myMoveCount > 30) {
    		frenchSize++;
    		UN[coord.getX()][coord.getY()] = 1;
    	}
    }

    /**
     * @param result A code from Constants that will equal WON or LOST.
     */
    @Override
    public void resultOfGame(int result) {
        // Add code here to process the result of a game
    	gameCount++;
    	for(int i =0; i< 10 ; i++) {
    		for(int j=0; j< 10; j++) {
    			if (stalag13[i][j] >=0 && stalag13[i][j] < 6) {
    				russianFront[i][j][stalag13[i][j]]++;
    			}
    			int sum =0;
    			for(int k=0; k<4;k++) {
    				russianFront[i][j][5] += russianFront[i][j][k];
    			}
    			russianFront[i][j][5] = russianFront[i][j][5] / 4;
    		}
    	}
    	for(int i=0; i<10; i++) {
    		for(int j=0; j<10; j++) {
    			if (!(UN[i][j] == 1)) {
    				frenchArmy[i][j]++;
    			}
    		}
    	}
    }
    
    
    //*********************************************************************************************
    // All the Methods that are used for testing/showing the values of structures in debugging.   *
    //*********************************************************************************************
    
    //Method for printing an array.
    private void printArray(int A[][]) {
    	for(int i =0; i< A.length ; i++) {
    		for(int j =0; j< A[0].length;j++) {
    			System.out.print(A[i][j] + " ");
    		}
    		System.out.println(" ");
    	}
    }
    
    //Method for printing out the LinkedLists in the sinking algorithm.
    private void printLinkedLists() {
    	System.out.print("Up: ");
    	for (int i =0; i < upBarrack.size();i++) {
    		System.out.print(upBarrack.get(i).toString());
    	}
    	System.out.print(" Left: ");
    	for (int i =0; i < leftBarrack.size();i++) {
    		System.out.print(leftBarrack.get(i).toString());
    	}
    	System.out.print(" Down: ");
    	for (int i =0; i < downBarrack.size();i++) {
    		System.out.print(downBarrack.get(i).toString());
    	}
    	System.out.print(" Right: ");
    	for (int i =0; i < rightBarrack.size();i++) {
    		System.out.print(rightBarrack.get(i).toString());
    	}
    }
}
