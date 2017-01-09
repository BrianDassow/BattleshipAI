import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;



public class JGrasp implements Captain {
	public final int[] SHIP_LENGTHS = new int[] { 2, 3, 3, 4, 5 };
	public final int[] SHIP_HIT_IDS = new int[] { 2, 3, 4, 5, 6 };
	public final boolean LOGGING = false;
	public final int AMOUNT_OF_PLACEMENT_METHODS = 2;
	public final int AMOUNT_OF_ATTACK_METHODS = 5;
	

	ArrayList<Game> allGames;
	Opponent myOpponent = new Opponent("");
	
    Random generator;

    Fleet myFleet;
    
    BoardCoordinate[][] currentBoard;
    
    //int[][] board;
    int[] boats;
    Coordinate[] coordHit;
    Coordinate lastAttack;
    boolean seeking;
    int coordCount;
    int seekDirection;
    int currentAttackMethod;
    int currentPlacementMethod;
    int random2;
    int random3;
    int random4;
    long latestTime, beginTime;
    int numberOfMatches;
    
    int[] amountOfPossibleAttacksMod3;
    int[] amountOfPossibleAttacksMod4;
    
    HashMap<Integer, BoardCoordinate> theirPlacementHash, theirAttackHash;
    
    
	ArrayList<PlacementMethod> allPlacementMethods;
	ArrayList<AttackMethod> allAttackMethods;
	

    

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
    	currentBoard = new BoardCoordinate[10][10];
    	for (int i = 0; i < 10; i++) {
    		for (int j = 0; j < 10; j++) {
    			currentBoard[i][j] = new BoardCoordinate(i, j, (i*10)+j);
    		}
    	}
    	
    	generator = new Random();
		currentAttackMethod = 0;
		currentPlacementMethod = 0;
    	
    	if(opponent != myOpponent.name) {
    		myOpponent = new Opponent(opponent);
    		numberOfMatches = 0;
    		allGames = new ArrayList<Game>();

    	}
    	
    	
    	amountOfPossibleAttacksMod3 = new int[3];
    	amountOfPossibleAttacksMod4 = new int[4];
		amountOfPossibleAttacksMod3[0] = 34;
		amountOfPossibleAttacksMod3[1] = amountOfPossibleAttacksMod3[2] = 33;
		amountOfPossibleAttacksMod4[0] = amountOfPossibleAttacksMod4[2] = 25;
		amountOfPossibleAttacksMod4[1] = 26;
		amountOfPossibleAttacksMod4[3] = 24;

		random2 = generator.nextInt(2);
		random3 = generator.nextInt(3);
		random4 = generator.nextInt(4);

        
        myFleet = new Fleet();
        seeking = false;
        coordHit = new Coordinate[5];
        boats = new int[5];
        lastAttack = new Coordinate(-1,-1);
        seekDirection = 0;
        coordCount = 1;
        
        
        placeShipController();
       
    }
   
    public void placeShipController() {

    	switch(generator.nextInt(2)) {
    		case 0:
    			cornerPlacement2p0();
    			break;
    		case 1:
    			for (int i = 0; i < 5; i++) {
    				placeShipRandom(i);
    			}
    	}
    	
    }
    
    
    public void placeShipRandom(int shipID) {
		while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), shipID)) {
		}
	}
    
    public void placeShipAdaptive(int shipID) {
		boolean shipPlaced = false;
		int count = 99;
		while (!shipPlaced) {
			int orientation = generator.nextInt(2);
			if (orientation == VERTICAL) {
				if (myFleet.placeShip(theirAttackHash.get(count).x, theirAttackHash.get(count).y - SHIP_LENGTHS[shipID] + 1, orientation, shipID)) {
					shipPlaced = true;
				}
				count--;
			} else if (orientation == HORIZONTAL) {
				if (myFleet.placeShip(theirAttackHash.get(count).x - SHIP_LENGTHS[shipID] + 1, theirAttackHash.get(count).y, orientation, shipID)) {
					shipPlaced = true;
				}
				count--;
			} else {
				if (myFleet.placeShip(theirAttackHash.get(count).x, theirAttackHash.get(count).y, orientation, shipID)) {
					shipPlaced = true;
				}
				count--;
			}
		}
	}
    
	public void cornerPlacement2p0() {
		int offSet = 2;
		ArrayList<Coordinate> Coordinates = new ArrayList<Coordinate>();
		Coordinate coordinate = new Coordinate(0 + generator.nextInt(offSet), 0 + generator.nextInt(offSet));
		Coordinates.add(coordinate);
		coordinate = new Coordinate(0 + generator.nextInt(offSet), 9 - generator.nextInt(offSet));
		Coordinates.add(coordinate);
		coordinate = new Coordinate(9 - generator.nextInt(offSet), 0 + generator.nextInt(offSet));
		Coordinates.add(coordinate);
		coordinate = new Coordinate(9 - generator.nextInt(offSet), 9 - generator.nextInt(offSet));
		Coordinates.add(coordinate);
		
		Collections.shuffle(Coordinates);
		
		for (int i = 0; i < 4; i++) {
			int orientation = generator.nextInt(2);
			if (orientation == VERTICAL && Coordinates.get(i).getY() + SHIP_LENGTHS[i] > 9) {
				if (!myFleet.placeShip(Coordinates.get(i).getX(), Coordinates.get(i).getY() - SHIP_LENGTHS[i] + 1, orientation, i)) {
					System.out.println("failed to place ship " + i + " at coordinates " + Coordinates.get(i).getX() + "," + Coordinates.get(i).getY() + "scenario 1");
					while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
					}
				}
			} else if (orientation == HORIZONTAL && Coordinates.get(i).getX() + SHIP_LENGTHS[i] > 9) {
				if (!myFleet.placeShip(Coordinates.get(i).getX() - SHIP_LENGTHS[i] + 1, Coordinates.get(i).getY(), orientation, i)) {
					System.out.println("failed to place ship " + i + " at coordinates " + Coordinates.get(i).getX() + "," + Coordinates.get(i).getY() + "scenario 2");
					while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
					}
				}
			} else {
				if (!myFleet.placeShip(Coordinates.get(i).getX(), Coordinates.get(i).getY(), orientation, i)) {
					System.out.println("failed to place ship " + i + " at coordinates " + Coordinates.get(i).getX() + "," + Coordinates.get(i).getY() + "scenario 3");
					while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
					}
				}
			}
			
		}
		placeShipRandom(4);
	}


    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    @Override
    public Coordinate makeAttack() {
    		if (seeking) {
        		lastAttack = seekAndDestroy();
        	}
        	else {
        		lastAttack = attackShipController();
        	}
        	
        return lastAttack;
    }
    
    public Coordinate attackShipController() {
    //	if (mod3AttackHits[random3] > 0) {
		//	return intelligentModAttack(3, random3);
		return checkerPattern();
    	
    }
    /*
	public Coordinate modAttack(int modID, int squareID) {//can it not be up to 999 can you create something so it can look through that? kthx also itll be a little faster..
		int count = 0;
		do {
			if (count > 999) {
				if (modID == 3) {
					amountOfPossibleAttacksMod3[squareID] = 0;
				} else {
					amountOfPossibleAttacksMod3[squareID] = 0;
				}
				return attackShipController();
			}
			attackCoordinate = new Coordinate(generator.nextInt(10), generator.nextInt(10));
			count++;
		} while (board[attackCoordinate.getX()][attackCoordinate.getY()] != 0 || ((attackCoordinate.getX() + attackCoordinate.getY()) % modID) != squareID || !myOpponent.canAnyShipBeHere(attackCoordinate.getX(), attackCoordinate.getY()));
		return attackCoordinate;
	}
    */
    public Coordinate checkerPattern() {
   	 int x, y, count = 0;
        do {
            x = (generator.nextInt(10));
            y = (generator.nextInt(10));
            count++;
            if (count == 100)
           	 return randomAttack();
        } while (((x + y) % 2) != 1 || (currentBoard[x][y].state != 0));
        return new Coordinate(x, y);
   }
   
   public Coordinate randomAttack() {
   	int x, y;
		do {
			x = generator.nextInt(10);
			y = generator.nextInt(10);
		} while (currentBoard[x][y].state != 0);
		return new Coordinate(x,y);
	}
    
    public Coordinate seekAndDestroy() {
    	int seeking = -1;

    	int x = 0, y = 0;
    	for (int i = 0; i < 5; i++) {
    		if (boats[i] == 1) {
    			seeking = i;
    			break;
    		}
    	}
    	do {
    		switch(seekDirection) {
    		case 0:
    			x = coordHit[seeking].getX() + coordCount;
    			y = coordHit[seeking].getY();
    			break;
    		case 1:
    			x = coordHit[seeking].getX() - coordCount;
    			y = coordHit[seeking].getY();
    			break;
    		case 2:
    			x = coordHit[seeking].getX();
    			y = coordHit[seeking].getY() + coordCount;
    			break;
    		case 3:
    			x = coordHit[seeking].getX();
    			y = coordHit[seeking].getY() - coordCount;
    			break;
    		}
    		if (x > 9 || x < 0 || y > 9 || y < 0 || currentBoard[x][y].state != 0) {
    			if (x < 10 && x > -1 && y < 10 && y > -1 && currentBoard[x][y].state == SHIP_HIT_IDS[seeking]) {
    				coordCount++;
    			}
    			else {
    				x = coordHit[seeking].getX();
    				y = coordHit[seeking].getY();
        			coordCount = 1;
        			seekDirection++;
    			}
    		}
    		if (seekDirection == 4)  {
    			seekDirection = 0;
    		}
    	}
    	while(currentBoard[x][y].state != 0);
    	coordCount++;

		return new Coordinate(x, y);
    }
    
    

    @Override
    public void resultOfAttack(int result) {
    	int modHit = result % 10;
    	int modSunk = result % 20;

    	if (result == MISS) {
    		currentBoard[lastAttack.getX()][lastAttack.getY()].state = 1;
    		if (seeking) {
    			coordCount = 1;
    			seekDirection++;
    			if (seekDirection == 4)  {
        			seekDirection = 0;
        		}
    		}
    	}
    	if (modHit < 5) {
    		
    		currentBoard[lastAttack.getX()][lastAttack.getY()].state = SHIP_HIT_IDS[modHit];
    		if (boats[modHit] == 0) {
    			boats[modHit] = 1;
    			coordHit[modHit] = lastAttack;
        		seeking = true;
    		}
    	}
    	if (modSunk < 5) {
    		resetSeeker(modHit);
    	}
    	
    	for (int i = 0; i < 5; i++) {
    		if (boats[i] == 1) {
    		}
    	}
    	
    }
    
    public void resetSeeker(int result) {
    	boats[result] = 2;
    	coordCount = 1;
    	seekDirection = 0;
    	seeking = false;
    	for (int i = 0; i < 5; i++) {
    		if (boats[i] == 1) {
    			seeking = true;
    		}
    	}
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        // Add code here to process or record opponent attacks
    }

    @Override
    public void resultOfGame(int result) {
    	
    	//allGames.add(new Game(allGames.size(), currentBoard, currentBoard, true));
    	numberOfMatches++;
    	if (LOGGING) {
			if (numberOfMatches == 1) {
				log("JGRASP");
				beginTime = System.currentTimeMillis();
				latestTime = beginTime;
			}
			/*
			if (trueTotalGames == totalNumberOfMatches) {
				log("Final Statistics: wins: %5.2f%%, sec=%.2f\n", 100.0 * wins / totalNumberOfMatches, (System.currentTimeMillis() - beginTime) / 1000.0);
				for (int i = 0; i < AMOUNT_OF_PLACEMENT_METHODS; i++) {
					log("Placement %30s: used %6.2f%%, accuracy %.4f\n", allPlacementMethods[i].name, 100.0 * allPlacementMethods[i].trueTotalGames / totalNumberOfMatches, allPlacementMethods[i].accuracy);
				}
				System.out.println();
				for (int i = 0; i < AMOUNT_OF_ATTACK_METHODS; i++) {
					log("Attack %30s: used %6.2f%%, accuracy: %6.2f\n", allAttackMethods[i].name, 100.0 * allAttackMethods[i].trueTotalGames / totalNumberOfMatches,allAttackMethods[i].accuracy);
				}
			} else */if (numberOfMatches % 25000 == 0) {
				long now = System.currentTimeMillis();
				System.out.println();
				log("%d UPDATE Overall: %.2f%%, sec=%.2f", numberOfMatches, calculateWins() * 100.0 / numberOfMatches, (now - latestTime) / 1000.0);
				latestTime = now;
			}
		}
    
    }
    
    public void printBoard() { // this shows the actual position
		
		for (int i = 0; i < 10; i++) {// this can move but keep it here until i
			for (int j = 0; j < 10; j++) {// this can move but keep it here until i
			System.out.print(currentBoard[i][j].state);
			}
			System.out.println();
		}
		System.out.println();
	}
    
    
    public void log(String message, Object... arguments) {
		if (LOGGING) {
			DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SS");
			String timeStamp = formatter.format(new Date());
			if (arguments.length == 0) {
				System.out.println(myOpponent.name + "|" + timeStamp + "|" + message);
			} else {
				System.out.printf(myOpponent.name + "|" + timeStamp + "|" + message, arguments);
			}
		}
	}
    
    public int calculateWins() {
    	int count = 0;
    	/*
    	for (int i = 0; i < allGames.size(); i++) {
        	if (allGames.get(i).won) {
        		count++;
        	}
        }
        */
    	return count;
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public class Game {
    	int ID;
    	boolean won;
    	BoardCoordinate[][] myBoard;
    	BoardCoordinate[][] opponentBoard;
    	
    	public Game(int ID, BoardCoordinate[][] myBoard, BoardCoordinate[][] opponentBoard, boolean won) {
    		this.opponentBoard = opponentBoard;
    		this.myBoard = myBoard;
    		this.ID = ID;
    		this.won = won;
    	}
    }
    
	public class PlacementMethod {
		int wins, losses, totalGames, trueTotalGames;
		String name;
		double accuracy;
		int totalAttacks;

		public PlacementMethod(String name) {
			this.name = name;
			trueTotalGames = wins = losses = totalGames = 0;
			accuracy = 100;
		}

		public void recordLoss(int totalAttacks) {
			this.totalAttacks += totalAttacks;
			allAttackMethods.get(currentAttackMethod).losses++;
			losses++;
			accuracy = (double) this.totalAttacks / (double) losses;
		}

		public void reset() {
			totalAttacks = totalGames = wins = losses = 0;
			accuracy = 100;
		}
	}

	public class AttackMethod {
		int wins, losses, totalGames, trueTotalGames;
		int totalAttacks;
		String name;
		int totalMisses;

		double accuracy, accuracyPercent;

		public AttackMethod(String name) {
			totalMisses = 0;
			this.name = name;
			totalAttacks = 0;
			trueTotalGames = wins = losses = totalGames = 0;
			accuracy = accuracyPercent = 0;
		}

		public void recordWin(int totalAttacks) {
			this.totalAttacks += totalAttacks;
			allPlacementMethods.get(currentPlacementMethod).wins++;
			wins++;
			accuracy = (double) this.totalAttacks / (double) wins;
		}

		public void updateAccuracy(int totalAttacks) {
			accuracyPercent = (((double) this.totalAttacks - (double) totalMisses) / (double) this.totalAttacks) * 100.0;
		}

		public void reset() {
			totalAttacks = totalGames = wins = losses = 0;
			accuracy = 0;
		}

	}

	public class ShipInfo {
		BoardCoordinate[][] shipPlacement;
		int shipID, length, vertOrHori, amountOfVPlacements,
				amountOfHPlacements, positiveHAvailSpaces, negativeHAvailSpaces, positiveVAvailSpaces, negativeVAvailSpaces;
		boolean canBeHorizontal, canBeVertical, sunk;
		Coordinate hitLastTurn;
		

		public ShipInfo(int shipID, int length) {
			shipPlacement = new BoardCoordinate[10][10];
			this.shipID = shipID;
			this.length = length;
			vertOrHori = -1;
			canBeHorizontal = false;
			canBeVertical = false;
			amountOfHPlacements = amountOfVPlacements = 0;
			negativeHAvailSpaces = positiveVAvailSpaces = negativeVAvailSpaces = 0;
			sunk = false;
			hitLastTurn = new Coordinate(-1, -1);
			
			initializeShip();
		}
		
		public void initializeShip() {
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					shipPlacement[i][j] = new BoardCoordinate(i, j, (i*10)+j);
				}
			}
		}


		public int calculateHorizontalOrVerticalShip() {
			for (int i = 1 - length; i < length; i++) {
				if ((i + coordHit[shipID].getX()) > -1 && (i + coordHit[shipID].getX() < 10) && i != 0) {
					if (currentBoard[coordHit[shipID].getX() + i][coordHit[shipID].getY()].state == shipID) {
						amountOfHPlacements++;
						canBeVertical = false;
						return HORIZONTAL;
					}
				}

				if ((i + coordHit[shipID].getY()) > -1 && (i + coordHit[shipID].getY() < 10) && i != 0) {
					if (currentBoard[coordHit[shipID].getX()][coordHit[shipID].getY() + i].state == shipID) {
						amountOfVPlacements++;
						canBeHorizontal = false;
						return VERTICAL;
					}
				}
			}
			System.out.println("THISSHOULDNTHAPPEN!!!");
			return 1;
		}

		public int horizontalVsVertical() {
			if (amountOfHPlacements > amountOfVPlacements) {
				return HORIZONTAL;
			}
			return VERTICAL;

		}

		public boolean isHorizontal() {
			if (vertOrHori == -1 || vertOrHori == HORIZONTAL && (negativeHAvailSpaces + positiveHAvailSpaces) >= SHIP_LENGTHS[shipID] - 1) {
				canBeHorizontal = true;
				return true;
			}
			canBeHorizontal = false;
			return false;
		}

		public boolean isVertical() {
			if (vertOrHori == -1 || vertOrHori == VERTICAL && (negativeVAvailSpaces + positiveVAvailSpaces) >= SHIP_LENGTHS[shipID] - 1) {
				canBeVertical = true;
				return true;
			}
			canBeVertical = false;
			return false;
		}
	}

	public class BoardCoordinate {
		boolean attacked;
		int x, y, score, boardID, state;

		public BoardCoordinate(int x, int y, int ID) {
			boardID = ID;
			attacked = false;
			this.x = x;
			this.y = y;
			score = 0;
			state = 0;
		}

		public void attacked(int ID) {
			score += ID;
			attacked = true;
		}

		public void reset() {
			score = 0;
		}

		@Override
		public String toString() {
			return String.format("(%2d, %2d)", x, y);
		}
	}

	public class Opponent {
		BoardCoordinate[][] theirAttackBoard, theirPlacementBoard;
		ShipInfo[] theirShipInfo;
		String name;

		int totalOpponentAttacks;
		int[][] oppAttackBoard;

		public Opponent(String name) {
			this.name = name;
			theirShipInfo = new ShipInfo[5];
			oppAttackBoard = new int[10][10];
			theirAttackBoard = new BoardCoordinate[10][10];
			theirPlacementBoard = new BoardCoordinate[10][10];
			
			initializeOpponent();
		}

		public void initializeOpponent() {
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					theirAttackBoard[i][j] = new BoardCoordinate(i, j, (i*10)+j);
					theirPlacementBoard[i][j] = new BoardCoordinate(i, j, (i*10)+j);
				}
			}
			for (int i = 0; i < 5; i++) {
				theirShipInfo[i] = new ShipInfo(i, SHIP_LENGTHS[i]);
			}
		}

		public void resetBoards() {
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					theirAttackBoard[i][j].reset();
					theirPlacementBoard[i][j].reset();
				}
			}
		}
		
		public void sortBoards() {
			BoardCoordinate[] theirPlacementScores = new BoardCoordinate[100];
			BoardCoordinate[] theirAttackScores = new BoardCoordinate[100];
			
			for(int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					theirPlacementScores[theirPlacementBoard[i][j].boardID] = theirPlacementBoard[i][j];
					theirAttackScores[theirAttackBoard[i][j].boardID] = theirAttackBoard[i][j];
				}
			}
			
			insertionSort(theirPlacementScores, 100);
			insertionSort(theirAttackScores, 100);
			
			for (int i = 0; i < 100; i++) {
				theirPlacementHash.put(i, theirPlacementBoard[theirPlacementScores[i].x][theirPlacementScores[i].y]);
				theirAttackHash.put(i, theirAttackBoard[theirAttackScores[i].x][theirAttackScores[i].y]);
			}
		}

		public void recordOpponentAttack(int x, int y) {
			totalOpponentAttacks--;
			oppAttackBoard[x][y] = 1;
			theirAttackBoard[x][y].attacked(-1);
		}

		public boolean canAnyShipBeHere(int x, int y) {
			int hSpaceCount = 0, vSpaceCount = 0, smallestShipLength = findSmallestShip();

			for (int i = 1 - smallestShipLength; i < smallestShipLength; i++) {
				if ((i + x) > -1 && (i + x) < 10 && i != 0) {
					if (currentBoard[x + i][y].state == 0) {
						hSpaceCount++;
					}
				}
				
				if ((i + y) > -1 && (i + y < 10) && i != 0) {
					if (currentBoard[x][y + i].state == 0) {
						vSpaceCount++;
					}
				}
			}
			if (hSpaceCount >= smallestShipLength - 1 || vSpaceCount >= smallestShipLength - 1) {
				return true;
			}
			return false;
		}

		public int findSmallestShip() {
			for (int i = 0; i < 5; i++) {
				if (theirShipInfo[i].sunk == false) {
					return SHIP_LENGTHS[i];
				}
			}
			return -1;
		}

		public int findLargestShip() {
			for (int i = 4; i > -1; i--) {
				if (theirShipInfo[i].sunk == false) {
					return SHIP_LENGTHS[i];
				}
			}
			return -1;
		}

		public void resetShips() {
			for (int i = 0; i < 5; i++) {
				theirShipInfo[i].vertOrHori = -1;
				theirShipInfo[i].canBeHorizontal = false;
				theirShipInfo[i].canBeVertical = false;
				theirShipInfo[i].negativeHAvailSpaces = 0;
				theirShipInfo[i].positiveHAvailSpaces = 0;
				theirShipInfo[i].negativeVAvailSpaces = 0;
				theirShipInfo[i].positiveVAvailSpaces = 0;
				theirShipInfo[i].sunk = false;
			}
		}

		public boolean canThisShipActuallyBeHere(Coordinate coord, int shipID) {
			if (currentBoard[coord.getX()][coord.getY()].state == shipID || currentBoard[coord.getX()][coord.getY()].state == 0) {
				return true;
			}
			return false;
		}

		public Coordinate getAttackCoordinate(int ID) {
			return new Coordinate(theirPlacementHash.get(ID).x, theirPlacementHash.get(ID).y);
		}
		
	}
	
	public void insertionSort(BoardCoordinate array[], int n) {
		int i, j;
		BoardCoordinate tempCoordinate;
		for (i = 1; i < n; i++) {
			j = i;
			while (j > 0 && array[j - 1].score > array[j].score) {
				tempCoordinate = array[j];
				array[j] = array[j-1];
				array[j-1] = tempCoordinate;
				j--;
			}
		}
	}
}
	/*\
   // \\
  // L \\
 //S   A\\
//L     B\\
\*_______*/