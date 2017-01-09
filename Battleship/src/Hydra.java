import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;



public class Hydra implements Captain {
	
	interface MyCaptain extends Constants {

	    public void initialize(int numMatches, int numCaptains, String opponent);

	    public Fleet getFleet();

	    public Coordinate makeAttack();

	    public void resultOfAttack(int result);

	    public void opponentAttack(Coordinate coord);

	    public void resultOfGame(int result);
	}	
	
	

	  enum MyIntent {
	       PLACING_SHIPS, SEARCHING_FOR_LARGE_SHIP, SEARCHING_FOR_SMALL_SHIP, SEARCHING_FOR_TINY_SHIP, ATTACKING_SHIP, UNKNOWN
	   };	
	    private enum MY_DIRECTION {

	        VERTICAL, HORIZONTAL
	    }
	    
	private class Strategy {
		String strategy;
		MyCaptain captain;
		int wins;
		int losses;
		double mavg;

		public Strategy(String strategy) {
			this.strategy = strategy;
			wins = 0;
			losses = 0;
			mavg = 0.0;

			if (strategy.equals("CaptainIntelligentlyRandom")){
				captain = new Hydra.MyCaptainIntelligentlyRandom();
			}
			else if (strategy.equals("CaptainSuperIntelligentlyRandom")){
				captain = new Hydra.MyCaptainSuperIntelligentlyRandom();
			}
			else if (strategy.equals("CaptainWellEducatedGuess")){
				captain = new Hydra.MyCaptainWellEducatedGuess();
			}
			else if (strategy.equals("Commodore0v12")){
				captain = new Hydra.MyCommodore0v12();
			}
			else if (strategy.equals("CptCasanova")){
				captain = new Hydra.MyCptCasanova();
			}
			else if (strategy.equals("MalcolmReynolds")){
				captain = new Hydra.MyMalcolmReynolds();
			}
			else if (strategy.equals("MRTwo")){
				captain = new Hydra.MyMRTwo();
			}
		}
		
		public void result(int result) {
			if (result == WON) {
				wins++;
			}
			else {
				losses++;
			}	
			
			int total = wins + losses;
			if (total == 1) {
				mavg = result;
			}
			else {
				mavg = mavg + ((result - mavg) /total);
			}
		}
		
	}
	
	private class Opponent {
		String name;
		ArrayList<Strategy> strategyList;
		public Opponent(String name) {
			this.name=name;
			strategyList = new ArrayList<Strategy>();
			strategyList.add(new Strategy("CaptainIntelligentlyRandom"));
			strategyList.add(new Strategy("CaptainSuperIntelligentlyRandom"));
			strategyList.add(new Strategy("CaptainWellEducatedGuess"));
			strategyList.add(new Strategy("Commodore0v12"));
			strategyList.add(new Strategy("CptCasanova"));
			strategyList.add(new Strategy("MalcolmReynolds"));
			strategyList.add(new Strategy("MRTwo"));
		}
	}
	
	private HashMap<String,Opponent> opponentList;
	
	public Hydra() {
		opponentList = new HashMap<String,Opponent>();
	}
	
	Opponent currentOpponent;
	Strategy currentStrategy;
	
	@Override
	public void initialize(int numMatches, int numCaptains, String name) {
		currentOpponent = opponentList.get(name);
		if (currentOpponent == null ) {
			currentOpponent = new Opponent(name);
			opponentList.put(name,currentOpponent);
		}
		
		currentStrategy = null;
		// check to make sure we have used each strategy at least a minimal number times
		int surveyCount = (int)(0.05 * numMatches);
		int minPlayPerStrategy = surveyCount / currentOpponent.strategyList.size();
		for (Strategy s:currentOpponent.strategyList) {
			if (s.wins + s.losses < minPlayPerStrategy) {
				currentStrategy = s;
				break;
			}
		}
		
		// ok, we have used each strategy at least 100 times, 
		// against this particular opponent, which strategy has been the most successful;
		if (currentStrategy == null) {
			currentStrategy = currentOpponent.strategyList.get(0);  // default to the first strategy just in case
			double bestWinPercentage = 0;
			for (Strategy s:currentOpponent.strategyList) {
//				double winPercentage = (double) s.wins / (double) (s.wins + s.losses);
				double winPercentage = s.mavg;
				if (winPercentage >= bestWinPercentage) {
					currentStrategy = s;
					bestWinPercentage = winPercentage;
				}
			}
		}

		if (currentStrategy == null) {
			// should never get here
			System.out.println("Should not get here");
		}
		else {
			currentStrategy.captain.initialize(numMatches, numCaptains, name);
		}
		
	}

	@Override
	public Fleet getFleet() {
		return currentStrategy.captain.getFleet();
	}

	@Override
	public Coordinate makeAttack() {
		return currentStrategy.captain.makeAttack();
	}

	@Override
	public void resultOfAttack(int result) {
		currentStrategy.captain.resultOfAttack(result);
	}

	@Override
	public void opponentAttack(Coordinate coord) {
		currentStrategy.captain.opponentAttack(coord);

	}

	@Override
	public void resultOfGame(int result) {
		currentStrategy.captain.resultOfGame(result);
		currentStrategy.result(result);

	}

	
/* Previous Engines for use in Strategy */

	public class MyCaptainIntelligentlyRandom implements MyCaptain, Constants {

	    ArrayList<Coordinate>[] enemyShipLocations = new ArrayList[5];
	    // Called before each game to reset your ship locations.
	    Random generator;
	    Fleet myFleet;
	    int[][] myVision = new int[10][10];
	    Coordinate lastAttack;
	    int randomOffset;
	    Stack<MyIntent> currentStates = new Stack<>();
	    ArrayList<Coordinate> currentLargeShipCoords = new ArrayList<>();
	    ArrayList<Coordinate> currentSmallShipCoords = new ArrayList<>();
	    ArrayList<Coordinate> currentTinyShipCoords = new ArrayList<>();
	    int currentShipFound = -1;
	    long totalTurns = 0;
	    long totalGames = 0;
	    String opponent;

	    @Override
	    public void initialize(int numMatches, int numCaptains, String opponent) {
	        this.opponent = opponent;
	        generator = new Random();
	        myFleet = new Fleet();
	        randomOffset = generator.nextInt(5);
	        currentLargeShipCoords.clear();
	        currentSmallShipCoords.clear();
	        currentTinyShipCoords.clear();
	        currentShipFound = -1;
	        lastAttack = new Coordinate(0, 0);
	        for (int i = 0; i < enemyShipLocations.length; i++) {
	            enemyShipLocations[i] = (ArrayList<Coordinate>) (new ArrayList());
	        }
	        currentStates.clear();
	        currentStates.push(MyIntent.PLACING_SHIPS);
	        myVision = new int[10][10];

	        int shipsPlaced = 0;
	        int loopCounter = 0;
	        Coordinate[][] shipLocations = new Coordinate[5][];
	        while (shipsPlaced < 5 && loopCounter < 1000) {
	            int currentShip = generator.nextInt(5);
	            if (shipLocations[currentShip] != null) {
	                continue;
	            }
	            loopCounter++;
	            int currentOrientation = generator.nextInt(2);
	            Coordinate shipPlacement = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	            Coordinate[] shipGridSquares;
	            int currentShipLength;
	            currentShipLength = getShipLength(currentShip);

	            switch (currentOrientation) {
	                case HORIZONTAL:
	                    if (shipPlacement.getX() + currentShipLength > 9) {
	                        continue;
	                    }
	                    break;
	                case VERTICAL:
	                    if (shipPlacement.getY() + currentShipLength > 9) {
	                        continue;
	                    }
	                    break;
	            }

	            shipGridSquares = new Coordinate[currentShipLength];
	            for (int i = 0; i < shipGridSquares.length; i++) {
	                if (currentOrientation == HORIZONTAL) {
	                    shipGridSquares[i] = new Coordinate(shipPlacement.getX() + i, shipPlacement.getY());
	                } else {
	                    shipGridSquares[i] = new Coordinate(shipPlacement.getX(), shipPlacement.getY() + i);
	                }
	            }

	            if (!checkShipConflicts(shipGridSquares, shipLocations)) {
	                if (myFleet.placeShip(shipPlacement, currentOrientation, currentShip)) {
	                    shipLocations[currentShip] = shipGridSquares;
	                    shipsPlaced++;
	                }
	            }
	        }

	        if (shipsPlaced < 5) {
	            for (int i = 0; i < 5; i++) {
	                if (shipLocations[i] == null) {
	                    while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
	                    }
	                }
	            }
	        }

	        currentStates.pop();
	        generateSearchCoordinates();
	        currentStates.push(MyIntent.SEARCHING_FOR_LARGE_SHIP);
	    }

	    private boolean checkShipConflicts(Coordinate[] currentShipSquares, Coordinate[][] placedShips) {
	        for (Coordinate[] placedShipSquares : placedShips) {
	            if (placedShipSquares != null) {
	                for (int i = 0; i < placedShipSquares.length; i++) {
	                    for (int j = 0; j < currentShipSquares.length; j++) {
	                        if (Math.pow(currentShipSquares[j].getX() - placedShipSquares[i].getX(), 2) + Math.pow(currentShipSquares[j].getY() - placedShipSquares[i].getY(), 2) <= 1) {
	                            return true;
	                        }
	                    }
	                }
	            }
	        }

	        return false;
	    }

	    public void generateSearchCoordinates() {
	        for (int i = 1; i <= 20; i++) {
	            currentLargeShipCoords.add(new Coordinate((5 * (i - 1) + ((i - 1) / 2) + randomOffset) % 10, ((i - 1) / 2) % 10));
	        }

	        for (int i = 0; i < randomOffset; i++) {
	            currentSmallShipCoords.add(new Coordinate(i, i + 1));
	        }

	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                currentTinyShipCoords.add(new Coordinate(i, j));
	            }
	        }
	    }

	    // Passes your ship locations to the main program.
	    @Override
	    public Fleet getFleet() {
	        return myFleet;
	    }

	    // Makes an attack on the opponent
	    @Override
	    public Coordinate makeAttack() {
	        lastAttack = new Coordinate(0, 0);

	        switch (currentStates.peek()) {
	            case ATTACKING_SHIP:
	                attackingShip();
	                if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	                    lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	                }
	                break;
	            case SEARCHING_FOR_LARGE_SHIP:
	                searchingForShip(currentLargeShipCoords, 5);
	                if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	                    lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	                }
	                break;
	            case SEARCHING_FOR_SMALL_SHIP:
	                searchingForShip(currentSmallShipCoords, 3);
	                if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	                    lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	                }
	                break;
	            case SEARCHING_FOR_TINY_SHIP:
	                searchingForTinyShip();
	                if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	                    lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	                }
	                break;
	            case UNKNOWN:
	                searchingRandomly();
	                if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	                    lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	                }
	                break;
	        }
	        while (myVision[lastAttack.getX()][lastAttack.getY()] != 0) {
	            lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	            if (currentStates.peek() == MyIntent.ATTACKING_SHIP) {
	                currentStates.pop();
	            }
	        }
	        return lastAttack;
	    }

	    private void attackingShip() {
	        // {N,E,S,W}
	        Coordinate[] maxCoordinate = new Coordinate[4];
	        for (int i = 0; i < maxCoordinate.length; i++) {
	            maxCoordinate[i] = enemyShipLocations[currentShipFound].get(0);
	        }
	        for (int i = 1; i < enemyShipLocations[currentShipFound].size(); i++) {
	            Coordinate currentCoordinate = enemyShipLocations[currentShipFound].get(i);
	            if (currentCoordinate.getY() < maxCoordinate[0].getY()) {
	                maxCoordinate[0] = currentCoordinate;
	            }
	            if (currentCoordinate.getX() > maxCoordinate[1].getX()) {
	                maxCoordinate[1] = currentCoordinate;
	            }
	            if (currentCoordinate.getY() > maxCoordinate[2].getY()) {
	                maxCoordinate[2] = currentCoordinate;
	            }
	            if (currentCoordinate.getX() < maxCoordinate[3].getX()) {
	                maxCoordinate[3] = currentCoordinate;
	            }
	        }
	        int[] freeDirections = new int[4];
	        int numberOfFreeSpaces = getShipLength(currentShipFound) - Math.max(maxCoordinate[1].getX() - maxCoordinate[3].getX(), maxCoordinate[2].getY() - maxCoordinate[0].getY()) - 1;
	        for (int i = 0; i <= numberOfFreeSpaces; i++) {
	            boolean horizontalFree = maxCoordinate[1].getX() - maxCoordinate[3].getX() > 0;
	            boolean verticalFree = maxCoordinate[2].getY() - maxCoordinate[0].getY() > 0;
	            if (!horizontalFree && !verticalFree) {
	                horizontalFree = true;
	                verticalFree = true;
	            }
	            for (int j = 0; j <= numberOfFreeSpaces; j++) {
	                if (j != i) {
	                    if (maxCoordinate[3].getX() - i + j < 0 || maxCoordinate[1].getX() - i + j > 9) {
	                        horizontalFree = false;
	                    } else if (i > j && myVision[maxCoordinate[3].getX() - i + j][maxCoordinate[3].getY()] != 0) {
	                        horizontalFree = false;
	                    } else if (i < j && myVision[maxCoordinate[1].getX() - i + j][maxCoordinate[1].getY()] != 0) {
	                        horizontalFree = false;
	                    }

	                    if (maxCoordinate[0].getY() - i + j < 0 || maxCoordinate[2].getY() - i + j > 9) {
	                        verticalFree = false;
	                    } else if (i > j && myVision[maxCoordinate[0].getX()][maxCoordinate[0].getY() - i + j] != 0) {
	                        verticalFree = false;
	                    } else if (i < j && myVision[maxCoordinate[2].getX()][maxCoordinate[2].getY() - i + j] != 0) {
	                        verticalFree = false;
	                    }
	                }
	            }

	            if (horizontalFree) {
	                if (i != numberOfFreeSpaces) {
	                    freeDirections[1]++;
	                }
	                if (i != 0) {
	                    freeDirections[3]++;
	                }
	            }
	            if (verticalFree) {
	                if (i != numberOfFreeSpaces) {
	                    freeDirections[2]++;
	                }
	                if (i != 0) {
	                    freeDirections[0]++;
	                }
	            }
	        }

	        int maxDirection = 0;
	        for (int i = 1; i < 4; i++) {
	            if (freeDirections[i] > freeDirections[maxDirection]) {
	                maxDirection = i;
	            }
	        }

	        switch (maxDirection) {
	            case 0:
	                lastAttack = new Coordinate(maxCoordinate[0].getX(), maxCoordinate[0].getY() - 1);
	                break;
	            case 1:
	                lastAttack = new Coordinate(maxCoordinate[1].getX() + 1, maxCoordinate[1].getY());
	                break;
	            case 2:
	                lastAttack = new Coordinate(maxCoordinate[2].getX(), maxCoordinate[2].getY() + 1);
	                break;
	            case 3:
	                lastAttack = new Coordinate(maxCoordinate[3].getX() - 1, maxCoordinate[3].getY());
	                break;
	        }

	        if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	            lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	        }
	    }

	    private void searchingForShip(ArrayList<Coordinate> possibleSearchCoordinates, int shipSize) {
	        for (int i = 0; i < possibleSearchCoordinates.size(); i++) {
	            Coordinate currentCoord = possibleSearchCoordinates.get(i);
	            if (myVision[currentCoord.getX()][currentCoord.getY()] != 0) {
	                possibleSearchCoordinates.remove(i);
	                i--;
	            }
	        }

	        if (possibleSearchCoordinates.isEmpty()) {
	            switch (shipSize) {
	                case 5:
	                    searchingForShip(currentSmallShipCoords, 3);
	                    return;
	                case 3:
	                    searchingForTinyShip();
	                    return;
	                case 2:
	                    searchingRandomly();
	                    return;
	            }
	        }

	        int coordIndex = generator.nextInt(possibleSearchCoordinates.size());
	        lastAttack = possibleSearchCoordinates.remove(coordIndex);
	        if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	            lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	        }
	    }

	    public void searchingForTinyShip() {
	        Coordinate currentCoord;
	        for (int i = 0; i < currentTinyShipCoords.size(); i++) {
	            currentCoord = currentTinyShipCoords.get(i);
	            int directionsFree = 0;
	            if (myVision[currentCoord.getX()][currentCoord.getY()] == 0) {
	                if (currentCoord.getX() - 1 >= 0 && myVision[currentCoord.getX() - 1][currentCoord.getY()] == 0) {
	                    directionsFree++;
	                }
	                if (currentCoord.getX() + 1 < 10 && myVision[currentCoord.getX() + 1][currentCoord.getY()] == 0) {
	                    directionsFree++;
	                }
	                if (currentCoord.getY() - 1 >= 0 && myVision[currentCoord.getX()][currentCoord.getY() - 1] == 0) {
	                    directionsFree++;
	                }
	                if (currentCoord.getY() + 1 < 10 && myVision[currentCoord.getX()][currentCoord.getY() + 1] == 0) {
	                    directionsFree++;
	                }
	            }

	            if (directionsFree < 2) {
	                currentTinyShipCoords.remove(i);
	                i--;
	            }
	        }

	        if (currentTinyShipCoords.isEmpty()) {
	            searchingRandomly();
	            return;
	        }
	        int coordIndex = generator.nextInt(currentTinyShipCoords.size());
	        lastAttack = currentTinyShipCoords.remove(coordIndex);
	        if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	            lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	        }
	    }

	    public void searchingRandomly() {
	        int x = generator.nextInt(10);
	        int y = generator.nextInt(10);
	        while (myVision[x][y] != 0) {
	            x = generator.nextInt(10);
	            y = generator.nextInt(10);
	        }

	        lastAttack = new Coordinate(x, y);
	        if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	            lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	        }
	    }

	    // Informs you of the result of your most recent attack
	    @Override
	    public void resultOfAttack(int result) {
	        // Add code here to process the success/failure of attacks
	        totalTurns++;
	        if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
	            lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	        }
	        myVision[lastAttack.getX()][lastAttack.getY()] = result;

	        if (currentStates.peek() == MyIntent.SEARCHING_FOR_LARGE_SHIP && currentLargeShipCoords.isEmpty()) {
	            currentStates.pop();
	            currentStates.push(MyIntent.SEARCHING_FOR_SMALL_SHIP);
	        }
	        if (currentStates.peek() == MyIntent.SEARCHING_FOR_SMALL_SHIP && (currentSmallShipCoords.isEmpty() || (isShipSunk(SUBMARINE) && isShipSunk(DESTROYER) && isShipSunk(BATTLESHIP)))) {
	            currentStates.pop();
	            currentStates.push(MyIntent.SEARCHING_FOR_TINY_SHIP);
	        }
	        if (currentStates.peek() == MyIntent.SEARCHING_FOR_TINY_SHIP && currentTinyShipCoords.isEmpty()) {
	            currentStates.pop();
	            currentStates.push(MyIntent.UNKNOWN);
	        }

	        if (result == MISS || result == DEFEATED) {
	            return;
	        }

	        if (result % HIT_MODIFIER != result) {
	            enemyShipLocations[result % HIT_MODIFIER].add(lastAttack);
	        }

	        if (result % SUNK_MODIFIER != result && currentStates.peek() == MyIntent.ATTACKING_SHIP) {
	            currentStates.pop();

	            for (int i = 0; i < 5; i++) {
	                if (!enemyShipLocations[i].isEmpty() && enemyShipLocations[i].size() != getShipLength(i)) {
	                    currentShipFound = i;
	                }
	            }
	        } else if (result % HIT_MODIFIER != result) {
	            if (currentShipFound == -1) {
	                currentStates.push(MyIntent.ATTACKING_SHIP);
	                currentShipFound = result % HIT_MODIFIER;
	            } else if (currentShipFound != result % HIT_MODIFIER) {
	                currentStates.push(MyIntent.ATTACKING_SHIP);
	                currentShipFound = result % HIT_MODIFIER;
	            }

	            if (currentStates.peek() != MyIntent.ATTACKING_SHIP) {
	                currentShipFound = -1;
	            }
	        }
	    }

	    private boolean isShipSunk(int ship) {
	        return enemyShipLocations[ship].size() == getShipLength(ship);
	    }

	    private int getShipLength(int ship) {
	        switch (ship) {
	            case AIRCRAFT_CARRIER:
	                return AIRCRAFT_CARRIER_LENGTH;
	            case BATTLESHIP:
	                return BATTLESHIP_LENGTH;
	            case SUBMARINE:
	                return SUBMARINE_LENGTH;
	            case DESTROYER:
	                return DESTROYER_LENGTH;
	            case PATROL_BOAT:
	                return PATROL_BOAT_LENGTH;
	            default:
	                return 0;
	        }
	    }

	    // Informs you of the position of an attack against you.
	    @Override
	    public void opponentAttack(Coordinate coord) {
	        // Add code here to process or record opponent attacks
	    }

	    // Informs you of the result of the game.
	    @Override
	    public void resultOfGame(int result) {
	        // Add code here to process the result of a game
	        totalGames++;
	        //System.out.println("Current average turns/games: " + (totalTurns/totalGames));
	    }

	}

	public class MyCaptainSuperIntelligentlyRandom implements MyCaptain, Constants {

	    @SuppressWarnings("unchecked")
	    ArrayList<Coordinate>[] enemyShipLocations = new ArrayList[5];
	    // Called before each game to reset your ship locations.
	    Random generator = new Random(System.currentTimeMillis());
	    Fleet myFleet;
	    int[][] myVision = new int[10][10];
	    Coordinate lastAttack;
	    int randomOffset;
	    Stack<MyIntent> currentStates = new Stack<>();
	    ArrayList<Coordinate> currentLargeShipCoords = new ArrayList<>();
	    ArrayList<Coordinate> currentSmallShipCoords = new ArrayList<>();
	    int currentShipFound = -1;
	    long totalTurns = 0;
	    long totalGames = 0;
	    String opponent;
	    int shipDistance = 1;

	    @Override
	    public void initialize(int numMatches, int numCaptains, String opponent) {
	        this.opponent = opponent;
	        myFleet = new Fleet();
	        randomOffset = generator.nextInt(5);
	        currentLargeShipCoords.clear();
	        currentSmallShipCoords.clear();
	        currentShipFound = -1;
	        lastAttack = new Coordinate(0, 0);
	        for (int i = 0; i < enemyShipLocations.length; i++) {
	            enemyShipLocations[i] = new ArrayList<>();
	        }
	        currentStates.clear();
	        currentStates.push(MyIntent.PLACING_SHIPS);
	        myVision = new int[10][10];

	        shipPlacement();

	        currentStates.pop();
	        generateSearchCoordinates();
	        currentStates.push(MyIntent.SEARCHING_FOR_LARGE_SHIP);
	    }

	    private void shipPlacement() {
	        int shipsPlaced = 0;
	        int loopCounter = 0;
	        Coordinate[][] shipLocations = new Coordinate[5][];

	        if (generator.nextFloat() < .97) {
	            //shipDistance = generator.nextInt(2) + 1;
	            while (shipsPlaced < 5 && loopCounter < 1000) {
	                int currentShip = generator.nextInt(5);
	                if (shipLocations[currentShip] != null) {
	                    continue;
	                }
	                loopCounter++;
	                int currentOrientation = generator.nextInt(2);
	                int currentShipLength = getShipLength(currentShip);
	                Coordinate[] shipGridSquares = new Coordinate[currentShipLength];
	                Coordinate shipPlacement;
	                if (currentOrientation == HORIZONTAL) {
	                    shipPlacement = new Coordinate(generator.nextInt(10 - currentShipLength + 1), generator.nextInt(10));
	                } else {
	                    shipPlacement = new Coordinate(generator.nextInt(10), generator.nextInt(10 - currentShipLength + 1));
	                }

	                for (int i = 0; i < shipGridSquares.length; i++) {
	                    if (currentOrientation == HORIZONTAL) {
	                        shipGridSquares[i] = new Coordinate(shipPlacement.getX() + i, shipPlacement.getY());
	                    } else {
	                        shipGridSquares[i] = new Coordinate(shipPlacement.getX(), shipPlacement.getY() + i);
	                    }
	                }

	                if (!checkShipConflicts(shipGridSquares, shipLocations)) {
	                    if (myFleet.placeShip(shipPlacement, currentOrientation, currentShip)) {
	                        shipLocations[currentShip] = shipGridSquares;
	                        shipsPlaced++;
	                    }
	                }
	            }
	        } else {
	            int currentRandomEdgeNumber, orientation, position;

	            while (shipsPlaced < 5 && loopCounter < 10000) {
	                currentRandomEdgeNumber = generator.nextInt(10);
	                while (currentRandomEdgeNumber > 0 && currentRandomEdgeNumber < 9) {
	                    currentRandomEdgeNumber = generator.nextInt(10);
	                }
	                orientation = generator.nextInt(2);
	                if (generator.nextFloat() < .1) {
	                    position = generator.nextInt(10);
	                } else {
	                    position = (10 - getShipLength(4 - shipsPlaced)) * generator.nextInt(2);
	                }

	                switch (orientation) {
	                    case HORIZONTAL:
	                        if (myFleet.placeShip(position, currentRandomEdgeNumber, HORIZONTAL, 4 - shipsPlaced)) {
	                            shipsPlaced++;
	                        }
	                        break;
	                    case VERTICAL:
	                        if (myFleet.placeShip(currentRandomEdgeNumber, position, VERTICAL, 4 - shipsPlaced)) {
	                            shipsPlaced++;
	                        }
	                        break;
	                }

	                loopCounter++;
	            }
	        }

	        if (shipsPlaced < 5) {
	            for (int i = 0; i < 5; i++) {
	                if (shipLocations[i] == null) {
	                    while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
	                    }
	                }
	            }
	        }
	    }

	    private boolean checkShipConflicts(Coordinate[] currentShipSquares, Coordinate[][] placedShips) {
	        for (Coordinate[] placedShipSquares : placedShips) {
	            if (placedShipSquares != null) {
	                for (int i = 0; i < placedShipSquares.length; i++) {
	                    for (int j = 0; j < currentShipSquares.length; j++) {
	                        if (Math.pow(currentShipSquares[j].getX() - placedShipSquares[i].getX(), 2) + Math.pow(currentShipSquares[j].getY() - placedShipSquares[i].getY(), 2) <= shipDistance) {
	                            return true;
	                        }
	                    }
	                }
	            }
	        }

	        return false;
	    }

	    public void generateSearchCoordinates() {
	        for (int i = 1; i <= 20; i++) {
	            currentLargeShipCoords.add(new Coordinate((5 * (i - 1) + ((i - 1) / 2) + randomOffset) % 10, ((i - 1) / 2) % 10));
	        }

	        for (Coordinate parentCoord : currentLargeShipCoords) {
	            currentSmallShipCoords.add(new Coordinate(parentCoord.getX(), (10 + parentCoord.getY() - 2) % 10));
	            currentSmallShipCoords.add(new Coordinate(parentCoord.getX(), (parentCoord.getY() + 2) % 10));
	        }

	        for (int i = 0; i < currentSmallShipCoords.size(); i++) {
	            Coordinate coord1 = currentSmallShipCoords.get(i);
	            for (int j = 0; j < currentSmallShipCoords.size(); j++) {
	                Coordinate coord2 = currentSmallShipCoords.get(j);
	                if (coord1.getX() == coord2.getX() && coord1.getY() == coord2.getY()) {
	                    continue;
	                }

	                //Lower portion
	                if ((coord1.getX() - randomOffset) % 10 < coord1.getY() && ((coord1.getX() == coord2.getX() && coord1.getY() == coord2.getY() - 1)
	                        || (coord1.getY() == coord2.getY() && coord1.getX() == coord2.getX() + 1))) {
	                    currentSmallShipCoords.remove(i);
	                    i--;
	                    break;
	                } //Upper portion
	                else if ((coord1.getX() - randomOffset) % 10 > coord1.getY() && ((coord1.getX() == coord2.getX() && coord1.getY() == coord2.getY() + 1)
	                        || (coord1.getY() == coord2.getY() && coord1.getX() == coord2.getX() - 1))) {
	                    currentSmallShipCoords.remove(i);
	                    i--;
	                    break;
	                }
	            }
	        }
	    }

	    // Passes your ship locations to the main program.
	    @Override
	    public Fleet getFleet() {
	        return myFleet;
	    }

	    // Makes an attack on the opponent
	    @Override
	    public Coordinate makeAttack() {
	        lastAttack = new Coordinate(0, 0);

	        switch (currentStates.peek()) {
	            case ATTACKING_SHIP:
	                attackingShip();
	                ensureValidAttack();
	                break;
	            case SEARCHING_FOR_LARGE_SHIP:
	                searchingForShip(currentLargeShipCoords, 5);
	                ensureValidAttack();
	                break;
	            case SEARCHING_FOR_SMALL_SHIP:
	                searchingForShip(currentSmallShipCoords, 3);
	                ensureValidAttack();
	                break;
	            case SEARCHING_FOR_TINY_SHIP:
	                searchingForTinyShip();
	                ensureValidAttack();
	                break;
	            case UNKNOWN:
	                searchingRandomly();
	                ensureValidAttack();
	                break;
	        }
	        currentLargeShipCoords.remove(lastAttack);
	        currentSmallShipCoords.remove(lastAttack);
	        return lastAttack;
	    }

	    private void attackingShip() {
	        if (isShipSunk(currentShipFound)) {
	            for (int i = 0; i < 5; i++) {
	                if (!enemyShipLocations[i].isEmpty() && enemyShipLocations[i].size() < getShipLength(i)) {
	                    currentShipFound = i;
	                }
	            }

	            if (isShipSunk(currentShipFound)) {
	                currentStates.pop();
	            }

	            makeAttack();
	            return;
	        }

	        // {N,E,S,W}
	        Coordinate[] maxCoordinate = new Coordinate[4];
	        for (int i = 0; i < maxCoordinate.length; i++) {
	            maxCoordinate[i] = enemyShipLocations[currentShipFound].get(0);
	        }
	        for (int i = 1; i < enemyShipLocations[currentShipFound].size(); i++) {
	            Coordinate currentCoordinate = enemyShipLocations[currentShipFound].get(i);
	            if (currentCoordinate.getY() < maxCoordinate[0].getY()) {
	                maxCoordinate[0] = currentCoordinate;
	            }
	            if (currentCoordinate.getX() > maxCoordinate[1].getX()) {
	                maxCoordinate[1] = currentCoordinate;
	            }
	            if (currentCoordinate.getY() > maxCoordinate[2].getY()) {
	                maxCoordinate[2] = currentCoordinate;
	            }
	            if (currentCoordinate.getX() < maxCoordinate[3].getX()) {
	                maxCoordinate[3] = currentCoordinate;
	            }
	        }
	        int[] freeDirections = new int[4];
	        int numberOfFreeSpaces = getShipLength(currentShipFound) - Math.max(maxCoordinate[1].getX() - maxCoordinate[3].getX(), maxCoordinate[2].getY() - maxCoordinate[0].getY()) - 1;
	        for (int i = 0; i <= numberOfFreeSpaces; i++) {
	            boolean horizontalFree = maxCoordinate[1].getX() - maxCoordinate[3].getX() > 0;
	            boolean verticalFree = maxCoordinate[2].getY() - maxCoordinate[0].getY() > 0;
	            if (!horizontalFree && !verticalFree) {
	                horizontalFree = true;
	                verticalFree = true;
	            }
	            for (int j = 0; j <= numberOfFreeSpaces; j++) {
	                if (j != i) {
	                    if (maxCoordinate[3].getX() - i + j < 0 || maxCoordinate[1].getX() - i + j > 9) {
	                        horizontalFree = false;
	                    } else if (i > j && myVision[maxCoordinate[3].getX() - i + j][maxCoordinate[3].getY()] != 0) {
	                        horizontalFree = false;
	                    } else if (i < j && myVision[maxCoordinate[1].getX() - i + j][maxCoordinate[1].getY()] != 0) {
	                        horizontalFree = false;
	                    }

	                    if (maxCoordinate[0].getY() - i + j < 0 || maxCoordinate[2].getY() - i + j > 9) {
	                        verticalFree = false;
	                    } else if (i > j && myVision[maxCoordinate[0].getX()][maxCoordinate[0].getY() - i + j] != 0) {
	                        verticalFree = false;
	                    } else if (i < j && myVision[maxCoordinate[2].getX()][maxCoordinate[2].getY() - i + j] != 0) {
	                        verticalFree = false;
	                    }
	                }
	            }

	            if (horizontalFree) {
	                if (i != numberOfFreeSpaces) {
	                    freeDirections[1]++;
	                }
	                if (i != 0) {
	                    freeDirections[3]++;
	                }
	            }
	            if (verticalFree) {
	                if (i != numberOfFreeSpaces) {
	                    freeDirections[2]++;
	                }
	                if (i != 0) {
	                    freeDirections[0]++;
	                }
	            }
	        }

	        int maxDirection = 0;
	        for (int i = 1; i < 4; i++) {
	            if (freeDirections[i] > freeDirections[maxDirection]) {
	                maxDirection = i;
	            }
	        }

	        switch (maxDirection) {
	            case 0:
	                lastAttack = new Coordinate(maxCoordinate[0].getX(), maxCoordinate[0].getY() - 1);
	                break;
	            case 1:
	                lastAttack = new Coordinate(maxCoordinate[1].getX() + 1, maxCoordinate[1].getY());
	                break;
	            case 2:
	                lastAttack = new Coordinate(maxCoordinate[2].getX(), maxCoordinate[2].getY() + 1);
	                break;
	            case 3:
	                lastAttack = new Coordinate(maxCoordinate[3].getX() - 1, maxCoordinate[3].getY());
	                break;
	        }

	        ensureValidAttack();
	    }

	    private void searchingForShip(ArrayList<Coordinate> possibleSearchCoordinates, int shipSize) {
	        if (possibleSearchCoordinates.isEmpty()) {
	            switch (shipSize) {
	                case 5:
	                    searchingForShip(currentSmallShipCoords, 3);
	                    return;
	                case 3:
	                    if (myVision[lastAttack.getX()][lastAttack.getY()] % HIT_MODIFIER != myVision[lastAttack.getX()][lastAttack.getY()]) {
	                        searchingForTinyShip();
	                    }
	                    return;
	            }
	        }

	        int coordIndex = generator.nextInt(possibleSearchCoordinates.size());
	        lastAttack = possibleSearchCoordinates.remove(coordIndex);
	        ensureValidAttack();
	    }

	    public void searchingForTinyShip() {
	        ArrayList<Coordinate> maxCoords = new ArrayList<>();
	        int maxHeatValue = 0;
	        int[][] tinyShipHeatMap = new int[10][10];

	        for (int i = 0; i < tinyShipHeatMap.length; i++) {
	            for (int j = 0; j < tinyShipHeatMap[i].length; j++) {
	                if (myVision[i][j] == 0) {
	                    if (j < 9 && myVision[i][j + 1] == 0) {
	                        tinyShipHeatMap[i][j + 1] += 1;
	                    }
	                    if (j > 0 && myVision[i][j - 1] == 0) {
	                        tinyShipHeatMap[i][j - 1] += 1;
	                    }
	                    if (i < 9 && myVision[i + 1][j] == 0) {
	                        tinyShipHeatMap[i + 1][j] += 1;
	                    }
	                    if (i > 0 && myVision[i - 1][j] == 0) {
	                        tinyShipHeatMap[i - 1][j] += 1;
	                    }
	                }
	            }
	        }

	        for (int i = 0; i < tinyShipHeatMap.length; i++) {
	            for (int j = 0; j < tinyShipHeatMap[i].length; j++) {
	                if (tinyShipHeatMap[i][j] == maxHeatValue && maxHeatValue > 0) {
	                    maxCoords.add(new Coordinate(i, j));
	                } else if (tinyShipHeatMap[i][j] > maxHeatValue) {
	                    maxHeatValue = tinyShipHeatMap[i][j];
	                    maxCoords.clear();
	                    maxCoords.add(new Coordinate(i, j));
	                }
	            }
	        }
	        if (maxCoords.size() > 0) {
	            lastAttack = maxCoords.get(generator.nextInt(maxCoords.size()));
	        }
//	    	else
//	    	{
//	    		for(int[] heatArray : myVision)
//	    		{
//	    			for(int heatcoord : heatArray)
//	    			{
//	    				System.out.print(heatcoord + ", ");
//	    			}
//	    			System.out.println();
//	    		}
//	    		Thread.dumpStack();
//	    		System.exit(0);
//	    	}
	    }

	    private void ensureValidAttack() {
	        if (lastAttack.getX() < 0 || lastAttack.getY() < 0) {
//	    		System.out.println("Invalid Attack Attempted");
//	    		Thread.dumpStack();
	            lastAttack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	        }
	    }

	    public void searchingRandomly() {
//	    	Thread.dumpStack();
//	    	System.out.println("Something broke");
	        int x = generator.nextInt(10);
	        int y = generator.nextInt(10);
	        while (myVision[x][y] != 0) {
	            x = generator.nextInt(10);
	            y = generator.nextInt(10);
	        }

	        lastAttack = new Coordinate(x, y);
	        ensureValidAttack();
	    }

	    // Informs you of the result of your most recent attack
	    @Override
	    public void resultOfAttack(int result) {
	        // Add code here to process the success/failure of attacks
	        totalTurns++;
	        ensureValidAttack();
	        myVision[lastAttack.getX()][lastAttack.getY()] = result;

	        if (currentStates.peek() == MyIntent.SEARCHING_FOR_LARGE_SHIP && currentLargeShipCoords.isEmpty()) {
	            currentStates.pop();
	            currentStates.push(MyIntent.SEARCHING_FOR_SMALL_SHIP);
	        }
	        if (isShipSunk(AIRCRAFT_CARRIER) && isShipSunk(SUBMARINE) && isShipSunk(DESTROYER) && isShipSunk(BATTLESHIP) && !currentStates.contains(MyIntent.SEARCHING_FOR_TINY_SHIP) && currentStates.peek() != MyIntent.ATTACKING_SHIP) {
	            currentStates.pop();
	            currentStates.push(MyIntent.SEARCHING_FOR_TINY_SHIP);
	        }

	        if (result == MISS || result == DEFEATED) {
	            return;
	        }

	        if (result % HIT_MODIFIER != result) {
	            enemyShipLocations[result % HIT_MODIFIER].add(lastAttack);
	        }

	        if (result % SUNK_MODIFIER != result && currentStates.peek() == MyIntent.ATTACKING_SHIP) {
	            currentStates.pop();

	            for (int i = 0; i < 5; i++) {
	                if (!enemyShipLocations[i].isEmpty() && enemyShipLocations[i].size() < getShipLength(i)) {
	                    currentShipFound = i;
	                }
	            }
	        } else if (result % HIT_MODIFIER != result) {
	            if (currentShipFound == -1) {
	                currentStates.push(MyIntent.ATTACKING_SHIP);
	                currentShipFound = result % HIT_MODIFIER;
	            } else if (currentShipFound != result % HIT_MODIFIER) {
	                currentStates.push(MyIntent.ATTACKING_SHIP);
	                currentShipFound = result % HIT_MODIFIER;
	            }

	            if (currentStates.peek() != MyIntent.ATTACKING_SHIP) {
	                currentShipFound = -1;
	            }
	        }
	    }

	    private boolean isShipSunk(int ship) {
	        return enemyShipLocations[ship].size() >= getShipLength(ship);
	    }

	    private int getShipLength(int ship) {
	        switch (ship) {
	            case AIRCRAFT_CARRIER:
	                return AIRCRAFT_CARRIER_LENGTH;
	            case BATTLESHIP:
	                return BATTLESHIP_LENGTH;
	            case SUBMARINE:
	                return SUBMARINE_LENGTH;
	            case DESTROYER:
	                return DESTROYER_LENGTH;
	            case PATROL_BOAT:
	                return PATROL_BOAT_LENGTH;
	            default:
	                return 0;
	        }
	    }

	    // Informs you of the position of an attack against you.
	    @Override
	    public void opponentAttack(Coordinate coord) {
	        // Add code here to process or record opponent attacks
	    }

	    // Informs you of the result of the game.
	    @Override
	    public void resultOfGame(int result) {
	        // Add code here to process the result of a game
	        totalGames++;
	        //System.out.println("Current average turns/games: " + (totalTurns/totalGames));
	    }
	}
	
	public class MyCaptainWellEducatedGuess implements MyCaptain, Constants {

	    private Random generator;
	    private Fleet myFleet;
	    private Move currentMove;
	    private MyEnemyInformation enemy;

	    @Override
	    public void initialize(int numMatches, int numCaptains, String opponent) {
	        generator = new Random();
	        myFleet = new Fleet();
	        enemy = new MyEnemyInformation();

	        while (!IsValidPlacement(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), AIRCRAFT_CARRIER)) {
	        }
	        while (!IsValidPlacement(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), BATTLESHIP)) {
	        }
	        while (!IsValidPlacement(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {
	        }
	        while (!IsValidPlacement(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {
	        }
	        while (!IsValidPlacement(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), PATROL_BOAT)) {
	        }

	    }

	    private boolean IsValidPlacement(int x, int y, int direction, int model) {
	        Ship possibleShip = new Ship(new Coordinate(x, y), direction, model);

	        //Don't place in the center 4x4 square
	        for (int i = 3; i < 7; i++) {
	            Ship block = new Ship(new Coordinate(3, i), HORIZONTAL, BATTLESHIP);
	            if (possibleShip.intersectsShip(block)) {
	                return false;
	            }
	        }

	        //Make sure each ship has a gap around it
	        for (Ship s : myFleet.fleet) {
	            if (s == null) {
	                continue;
	            }
	            if (direction == HORIZONTAL) {
	                //Check top and bottom
	                for (int newY = y - 1; newY < y + 2; newY += 2) {
	                    for (int newX = x; newX < GetShipLength(model) + x + 1; newX++) {
	                        if (s.isOnShip(new Coordinate(newX, newY))) {
	                            return false;
	                        }
	                    }
	                }

	                //Check l/r sides
	                if (s.isOnShip(new Coordinate(x - 1, y))) {
	                    return false;
	                }
	                if (s.isOnShip(new Coordinate(x + GetShipLength(model) + 1, y))) {
	                    return false;
	                }
	            } else {
	                //Check Left and Right
	                for (int newX = x - 1; newX < x + 2; newX += 2) {
	                    for (int newY = y; newY < GetShipLength(model) + y + 1; newY++) {
	                        if (s.isOnShip(new Coordinate(newX, newY))) {
	                            return false;
	                        }
	                    }
	                }

	                //Check top and bottom sides
	                if (s.isOnShip(new Coordinate(x, y - 1))) {
	                    return false;
	                }
	                if (s.isOnShip(new Coordinate(x, y + GetShipLength(model) + 1))) {
	                    return false;
	                }

	            }

	        }


	        return myFleet.placeShip(x, y, direction, model);
	    }

	    @Override
	    public Fleet getFleet() {
	        // TODO Auto-generated method stub
	        return myFleet;
	    }

	    @Override
	    public Coordinate makeAttack() {
	        currentMove = new Move();
	        currentMove.move = enemy.GetNextMove();
	        return currentMove.move;
	    }

	    @Override
	    public void resultOfAttack(int result) {
	        // TODO Auto-generated method stub
	        currentMove.result = result;
	        enemy.addMove(currentMove);
	        currentMove = null;
	    }

	    @Override
	    public void opponentAttack(Coordinate coord) {
	        // TODO Auto-generated method stub
	    }

	    @Override
	    public void resultOfGame(int result) {
	        // TODO Auto-generated method stub
	    }



	    private class MyEnemyInformation {

	        private Stack<Move> FoundShips = new Stack<>();
	        private ArrayList<Move> moves = new ArrayList<>();
	        private ArrayList<Coordinate> RemainingMoves = new ArrayList<>();
	        private Move originalHit = null;
	        private MY_DIRECTION shipDirection = null;
	        private int lastHitShipBottom, lastHitShipTop;

	        public MyEnemyInformation() {
	            int y = 5;
	            for (int x = 5; x < 105; x++) {
	                RemainingMoves.add(new Coordinate(x % 10, y % 10));
	                y++;
	                if (x % 10 == 0) {
	                    y++;
	                }
	            }
	            int[] rows = new int[10];
	            for (Coordinate c : RemainingMoves) {
	                rows[c.getX()] += 1;
	            }
	        }

	        public void addMove(Move m) {
	            for (Coordinate c : RemainingMoves) {
	                if (c.getX() == m.move.getX() && c.getY() == m.move.getY()) {
	                    RemainingMoves.remove(c);
	                    break;
	                }
	            }
	            moves.add(m);
	        }

	        public Coordinate GetNextMove() {
	            Move lastAttack = null;
	            if (moves.size() > 0) {
	                lastAttack = moves.get(moves.size() - 1);
	            }


	            //Leave if game is over?
	            if (getShipLengths().length == 0) {
	                return RemainingMoves.get(0);
	            }

	            boolean lastAttackHit = lastAttack != null
	                    && lastAttack.result - (lastAttack.result % 10) == HIT_MODIFIER;

	            //If the last hit sunk the last ship
	            if (lastAttack != null && originalHit != null
	                    && lastAttack.result - (lastAttack.result % 20) == SUNK_MODIFIER && lastAttack.result % 10 == originalHit.result % 10) {
	                ////System.out.println("Last attack sunk ship, pursuing new ships");
	                originalHit = null;
	                lastAttackHit = false;
	            }


	            if (!lastAttackHit && originalHit == null && FoundShips.size() > 0) {
	                //System.out.println("Popping a previously found ship");
	                originalHit = FoundShips.pop();
	                lastAttack = originalHit;
	                lastAttackHit = true;
	                shipDirection = null;
	            }

	            // If last attack was a hit, or if we are on a trail
	            if (lastAttackHit || originalHit != null) {
	                // Gather coordinate in one of four coordinate directions
	                if (originalHit == null) {
	                    originalHit = lastAttack;
	                }

	                int shipLength = GetShipLength(originalHit.result % 10);

	                //If you hit a new ship, then the ship we're looking for is no longer in this direction
	                if (lastAttack.result % 20 != originalHit.result % 20 && lastAttackHit) {
	                    lastAttackHit = false;
	                    //Add it to the new attack stack if it wasn't sunk
	                    if (lastAttack.result - (lastAttack.result % 20) != SUNK_MODIFIER) {
	                        //System.out.println("Added this ship to the stack");
	                        FoundShips.push(lastAttack);
	                    }
	                }

	                Coordinate nextAttack = null;
	                // Guess in vertical direction first
	                if (shipDirection == null) {
	                    shipDirection = MY_DIRECTION.VERTICAL;
	                    lastHitShipBottom = -shipLength;
	                    lastHitShipTop = shipLength;
	                }

	                if (shipDirection == MY_DIRECTION.VERTICAL) {

	                    //Move the attack range in for missed attacks
	                    if (!lastAttackHit) {
	                        int distance = lastAttack.move.getY() - originalHit.move.getY();
	                        if (distance < 0) {
	                            lastHitShipBottom = distance;
	                        } else {
	                            lastHitShipTop = distance;
	                        }
	                    }


	                    for (int y = 1; y < lastHitShipTop; y++) {
	                        nextAttack = new Coordinate(originalHit.move.getX(),
	                                originalHit.move.getY() + y);

	                        if (IsValidMove(nextAttack)) {
	                            break;
	                        } else {
	                            nextAttack = null;
	                        }
	                    }
	                    if (nextAttack == null) {
	                        for (int y = -1; y > lastHitShipBottom; y--) {
	                            nextAttack = new Coordinate(originalHit.move.getX(),
	                                    originalHit.move.getY() + y);

	                            if (IsValidMove(nextAttack)) {
	                                break;
	                            } else {
	                                nextAttack = null;
	                            }
	                        }
	                    }
	                }

	                // If there are no attacks possible in the vertical direction,
	                // then it's horizontal
	                if (nextAttack == null && shipDirection == MY_DIRECTION.VERTICAL) {
	                    shipDirection = MY_DIRECTION.HORIZONTAL;
	                    lastHitShipBottom = -shipLength;
	                    lastHitShipTop = shipLength;
	                    lastAttack = originalHit;
	                    lastAttackHit = true;
	                }

	                if (shipDirection == MY_DIRECTION.HORIZONTAL) {
	                    if (!lastAttackHit) {
	                        int distance = lastAttack.move.getX() - originalHit.move.getX();
	                        if (distance < 0) {
	                            lastHitShipBottom = distance;
	                        } else {
	                            lastHitShipTop = distance;
	                        }
	                    }

	                    for (int x = 1; x < lastHitShipTop; x++) {
	                        nextAttack = new Coordinate(originalHit.move.getX() + x,
	                                originalHit.move.getY());

	                        if (IsValidMove(nextAttack)) {
	                            break;
	                        } else {
	                            nextAttack = null;
	                        }
	                    }
	                    if (nextAttack == null) {
	                        for (int x = -1; x > lastHitShipBottom; x--) {
	                            nextAttack = new Coordinate(originalHit.move.getX() + x,
	                                    originalHit.move.getY());

	                            if (IsValidMove(nextAttack)) {
	                                break;
	                            } else {
	                                nextAttack = null;
	                            }
	                        }
	                    }
	                }
	                return nextAttack != null ? nextAttack : RemainingMoves.get(0);

	            } else {
	                originalHit = null;
	                shipDirection = null;
	                // Attack in random direction distancing at least the minimum
	                // ship length away from any other attacks if possible
	                int smallestShip = getSmallestShipLength();
	                Coordinate nextAttack = null;
	                while (nextAttack == null) {
	                    for (Coordinate c : RemainingMoves) {
	                        nextAttack = c;
	                        for (Move m : moves) {
	                            if (Math.abs(m.move.getX() - c.getX()) < smallestShip
	                                    && m.move.getY() == c.getY()) {
	                                nextAttack = null;
	                                break;
	                            }

	                            if (Math.abs(m.move.getY() - c.getY()) < smallestShip
	                                    && m.move.getX() == c.getX()) {
	                                nextAttack = null;
	                                break;
	                            }

	                        }

	                        if (nextAttack != null) {
	                            break;
	                        }
	                    }
	                    smallestShip--;
	                }

	                return nextAttack;

	            }
	        }

	        private boolean IsValidMove(Coordinate move) {
	            for (Move m : moves) {
	                if (m.move.getX() == move.getX() && m.move.getY() == move.getY()) {
	                    return false;
	                }
	            }

	            if (move.getY() < 0 || move.getY() > 9 || move.getX() < 0 || move.getX() > 9) {
	                return false;
	            }

	            return true;
	        }

	        private int getSmallestShipLength() {
	            return getShipLengths()[0];
	        }

	        private Integer[] getShipLengths() {
	            ArrayList<Integer> sizes = new ArrayList<Integer>();
	            sizes.add(2);
	            sizes.add(3);
	            sizes.add(3);
	            sizes.add(4);
	            sizes.add(5);
	            for (Move m : moves) {
	                if (m.result - (m.result % 10) != SUNK_MODIFIER) {
	                    continue;
	                }

	                int model = m.result % 10;
	                int size = GetShipLength(model);
	                sizes.remove(new Integer(size));

	            }
	            Integer[] arrSizes = new Integer[sizes.size()];
	            sizes.toArray(arrSizes);
	            Arrays.sort(arrSizes);
	            return arrSizes;
	        }
	    }

	    private class Move {

	        public Coordinate move;
	        public int result;
	    }

	    private int GetShipLength(int ship) {
	        int shipLength = 0;
	        switch (ship) {
	            case PATROL_BOAT:
	                shipLength = PATROL_BOAT_LENGTH;
	                break;
	            case DESTROYER:
	                shipLength = DESTROYER_LENGTH;
	                break;
	            case SUBMARINE:
	                shipLength = SUBMARINE_LENGTH;
	                break;
	            case BATTLESHIP:
	                shipLength = BATTLESHIP_LENGTH;
	                break;
	            case AIRCRAFT_CARRIER:
	                shipLength = AIRCRAFT_CARRIER_LENGTH;
	                break;
	        }
	        return shipLength;
	    }
	}

	public class MyCommodore0v12 implements MyCaptain, Constants {

	    // Global data
	    int currentRound;
	    final static int FLAG_ROUND = 50;
	    Random generator;
	    Fleet myFleet;
	    /* TIMING STATS
	     ------------
	     NUM_FLEETS = 20, NUM_SHOTS = 50
	     vs. Malcom 50,000 rounds 80 s
	     vs. Chaos  50,000 rounds 65 s
	     */
	    final static int NUM_FLEETS = 20;
	    final static int NUM_SHOTS = 50;
	    final static int SCORE_THRESHHOLD = 10; // should be less than 100
	    int DEBUG;
	    final static int V = 1; // low verbosity
	    final static int VV = 2; // med verbosity
	    final static int VVV = 3; // high verbosity
	    int[][] myBoard;
	    Coordinate myLastShot;
	    String lastOpponent;
	    int[][] opponentAttackMap;
	    int[][] opponentShipMap;
	    int opponentAttackMapTotal;
	    int opponentShipMapTotal;
	    int myState;
	    final static int FIRE = 0;
	    final static int SINK = 1;
	    // SINK state
	    int sinkPatternCounter;
	    int sinkPatternLengths = 16;
	    int[][] sinkPattern = {{1, 0}, {0, 1}, {-1, 0}, {0, -1},
	        {2, 0}, {0, 2}, {-2, 0}, {0, -2},
	        {3, 0}, {0, 3}, {-3, 0}, {0, -3},
	        {4, 0}, {0, 4}, {-4, 0}, {0, -4}};
	    int[] shipLengths = {2, 3, 3, 4, 5};
	    List shipLengthsRemaining;
	    // Targeting
	    Integer[] intInt;
	    HashMap<Integer, S> targetMap;
	    S currentTarget, newTarget;

	    public MyCommodore0v12() {
	        currentRound = 0;
	        DEBUG = 0;
	        opponentAttackMap = new int[10][10];
	        opponentShipMap = new int[10][10];
	        lastOpponent = null;
	        intInt = new Integer[5];
	        for (int i = 0; i < 5; i++) {
	            intInt[i] = new Integer(i);
	        }
	    }

	    // Called before each game to reset your ship locations.
	    @Override
	    public void initialize(int numMatches, int numCaptains, String opponent) {
	        generator = new Random();
	        myFleet = new Fleet();
	        myBoard = new int[10][10];
	        targetMap = new HashMap<>(5);
	        myLastShot = new Coordinate(-1, -1);

	        if (DEBUG >= VVV) {
	            System.out.format("\n\n---------- NEW GAME ----------\n\n");
	        }
	        if (DEBUG >= VVV) {
	            System.out.format("Commodore0: init\n");
	        }


	        if (lastOpponent == null) // first round
	        {
	            lastOpponent = opponent;
	        }
	        if (!opponent.equals(lastOpponent)) { // new opponent, clear heat maps
	            for (int i = 0; i < 10; i++) {
	                opponentAttackMapTotal = 0;
	                Arrays.fill(opponentAttackMap[i], 0);
	                opponentShipMapTotal = 0;
	                Arrays.fill(opponentShipMap[i], 0);
	            }
	        } else {
	            // Update heat map totals
	            opponentAttackMapTotal = 0;
	            opponentShipMapTotal = 0;
	            for (int i = 0; i < 10; i++) {
	                for (int j = 0; j < 10; j++) {
	                    opponentAttackMapTotal += opponentAttackMap[i][j];
	                    opponentShipMapTotal += opponentShipMap[i][j];
	                }
	            }
	            if (DEBUG >= VVV) {
	                System.out.format("Commodore0: init: attackMapTotal = %d, shipMapTotal = %d\n", opponentAttackMapTotal, opponentShipMapTotal);
	            }
	        }
	        lastOpponent = opponent;

	        shipLengthsRemaining = new ArrayList<>(5);
	        for (int i = 0; i < 5; i++) {
	            shipLengthsRemaining.add(new Integer(shipLengths[i]));
	        }

	        if (DEBUG >= VVV) {
	            System.out.format("Commodore0: placing ships.. ");
	        }

	        placeShipsAdaptive(); // Monte Carlo based adaptive strategy

	        //  initialize my board
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                myBoard[i][j] = 0;
	            }
	        }

	        // initialize the state and the pattern counter
	        myState = FIRE;
	        // patternCounter = 0;
	        sinkPatternCounter = 0;

	        if (DEBUG >= VVV) {
	            System.out.format("Commodore0: done\n");
	        }
	    }

	    /* ----------------------- Placement Methods ----------------------- */
	    private void placeShipsAdaptive() {
	        Fleet f = new Fleet();
	        List<S> ShipList;
	        int fleetScore = 0;

	        // loop over ship types, starting with largest

	        for (int type = 4; type >= 0; type--) {
	            // loop over all possible positions of the ship, horiz and vert
	            // for each position, compute score based on opponentAttackMap
	            int l = shipLengths[type];
	            int dir = 1;
	            int tmpScore;
	            S s;
	            ShipList = new ArrayList<>(1);
	            // vertical
	            for (int x = 0; x < 10; x++) {
	                for (int y = 0; y < 10 - l + 1; y++) {
	                    // create ship
	                    s = new S(x, y, dir, type);
	                    // score ship
	                    tmpScore = 0;
	                    for (int j = 0; j < l; j++) {
	                        tmpScore += opponentAttackMap[x][y + j];
	                    }
	                    s.score = tmpScore;
	                    // add ship to list
	                    ShipList.add(s);
	                }
	            }
	            // horizontal
	            dir = 0;
	            for (int y = 0; y < 10; y++) {
	                for (int x = 0; x < 10 - l + 1; x++) {
	                    // create ship
	                    s = new S(x, y, dir, type);
	                    // score ship
	                    tmpScore = 0;
	                    for (int j = 0; j < l; j++) {
	                        tmpScore += opponentAttackMap[x + j][y];
	                    }
	                    s.score = tmpScore;
	                    // add ship to list
	                    ShipList.add(s);
	                }
	            }
	            if (DEBUG >= VV) {
	                System.out.format("Commodore0: ship placing, type %d, #ShipList %d\n", type, ShipList.size());
	            }
	            // now sort ShipList by score
	            Collections.sort(ShipList);
	            int maxScore = ShipList.get(0).score;
	            if (DEBUG >= VV) {
	                System.out.format("Commodore0: ship placing, maxScore %d\n", maxScore);
	            }

	            // filter list
	            List newShipList = new ArrayList<>(1);
	            for (S t : ShipList) {
	                if (type == 4 || !intersectsFleet(f, t)) {
	                    newShipList.add(t);
	                }
	            }
	            ShipList = newShipList;

	            // try to place randomly up to SCORE_THRESHHOLD
	            int failsafe = 0;
	            do {
	                int index = generator.nextInt(SCORE_THRESHHOLD);
	                S t = ShipList.get(index);
	                if (type == 4 || !intersectsFleet(f, t)) {
	                    f.placeShip(t.location(), t.direction(), type);
	                    fleetScore += t.score; // last line edited. Final version 4/2/2012 1:00 AM
	                    break;
	                }
	                failsafe++;
	            } while (failsafe < 100);

	            if (failsafe == 100) {
	                System.out.format("Commodore0v12: SHIP PLACE FAIL!\n");
	                // just place the first one that doesn't intersect the current fleet.
	                for (S t : ShipList) {
	                    if (type == 4 || !intersectsFleet(f, t)) {
	                        f.placeShip(t.location(), t.direction(), type);
	                        fleetScore += t.score;
	                    }
	                }
	            }
	        }
	        if (DEBUG >= VV) {
	            System.out.format("Commodore0: DONE ship placing, fleet Score = %d\n", fleetScore);
	        }
	        if (DEBUG >= VV) {
	            printFleet(f);
	        }
	        myFleet = f;
	    }

	    private boolean intersectsFleet(Fleet f, Ship s) {
	        for (Ship t : f.fleet) {
	            if (t != null && s.intersectsShip(t)) {
	                return true;
	            }
	        }
	        return false;
	    }

	    private void printFleet(Fleet fleet) {
	        Coordinate c;
	        System.out.format("Fleet:\n");
	        for (int i = 0; i < 10; i++) {
	            System.out.format("|");
	            for (int j = 0; j < 10; j++) {
	                c = new Coordinate(i, j);
	                if (fleet.isShipAt(c)) {
	                    System.out.format("#");
	                } else {
	                    System.out.format(" ");
	                }
	            }
	            System.out.format("|\n");
	        }
	    }

	    // Passes your ship locations to the main program.
	    @Override
	    public Fleet getFleet() {
	        return myFleet;
	    }

	    /* -----------------------  Attack Methods ----------------------- */
	    // Find an un-fired upon random spot on the board
	    private Coordinate validShot() {
	        int x, y;
	        do {
	            x = generator.nextInt(10);
	            y = generator.nextInt(10);
	        } while (myBoard[x][y] != 0);
	        return new Coordinate(x, y);
	    }

	    // Make a random attack in a checkerboard pattern
	    private Coordinate makeRandomAttack() {
	        int x = 0, y = 0;
	        Coordinate c;
	        int failsafe, failsafe2;
	        int modulus;
	        int modIndex = 1;

	        if (shipLengthsRemaining.isEmpty()) // this shouldn't happen!
	        {
	            modulus = 10;
	        } else {
	            modulus = ((Integer) Collections.max(shipLengthsRemaining)).intValue();
	        }

	        // Fire on the diagonals mod 5, 4, 3, 3, 2
	        failsafe = 0;
	        //modIndex++;
	        do {
	            c = validShot();
	            failsafe++;
	        } while (((c.getX() + c.getY()) % modulus != modIndex)
	                && failsafe < 500);

	        if (failsafe < 500) {
	            return c;
	        } else { // if no valid shot is found yet, fire on any random square
	            c = validShot();
	            if (DEBUG >= V) {
	                System.out.format(" *** Commodore0: makeAttack: had to make a random shot\n");
	            }
	            return c;
	        }
	    }

	    // Mark the board as a hit and set myLastShot
	    private void markBoard(Coordinate c) {
	        int x = c.getX();
	        int y = c.getY();
	        if (x > 9 || x < 0 || y > 9 || y < 0) { // we went off the board!
	            throw new RuntimeException("attack off the board!");
	        } else {
	            myBoard[x][y] = 1;
	            myLastShot = new Coordinate(x, y);
	        }
	    }

	    // Makes an attack on the opponent
	    @Override
	    public Coordinate makeAttack() {
	        // two states:
	        // FIRE: uses a Monte Carlo method to fire randomly on diagonals where
	        //       (x+y) mod X is 1
	        // SINK: fire around the hit ship until find the direction of the ship, then
	        //       fires along that direction alternating back and forth across the
	        //       known initial hit.
	        int x, y, type;
	        Coordinate ret = new Coordinate(0, 0);

	        if (DEBUG >= VVV) {
	            System.out.format("Commodore0: makeAttack()\n");
	        }
	        switch (myState) {
	            case FIRE:
	                // Monte Carlo method for finding the "best" random shot based on
	                // the opponent's ship map
	                Coordinate bestShot,
	                 tmpShot;
	                int best = 0;
	                int score = 0;
	                long score_sum = 0;
	                int i = 0;

	                bestShot = tmpShot = makeRandomAttack();
	                do {
	                    // higher score is better
	                    score = opponentShipMap[tmpShot.getX()][tmpShot.getY()];
	                    score_sum += score;
	                    if (score > best) {
	                        bestShot = tmpShot;
	                        best = score;
	                    }
	                    tmpShot = makeRandomAttack();
	                } while (++i < NUM_SHOTS);

	                if (DEBUG >= VVV) {
	                    System.out.format("Commodore0: Best shot seems to be: (%d, %d)\n", bestShot.getX(), bestShot.getY());
	                }
	                if (DEBUG >= VVV) {
	                    System.out.format("Commodore0: best shot score: %d, ave: %f\n", best, ((float) score_sum) / ((float) NUM_SHOTS));
	                }

	                ret = bestShot;
	                break;

	            case SINK:
	                type = currentTarget.getModel();
	                int cx = currentTarget.location.getX();
	                int cy = currentTarget.location.getY();
	                int dx = 0,
	                 dy = 0;
	                x = -1;
	                y = -1;
	                int failsafe = 0;

	                if (DEBUG >= VVV) {
	                    System.out.format("Commodore0: state SINK type %d\n",
	                            currentTarget.getModel());
	                }

	                // Check the sink pattern counter
	                if (sinkPatternCounter < sinkPatternLengths) {
	                    // Find a free square to hit in the sink pattern (if possible)
	                    do {
	                        // calculate the next square to hit according to sink pattern
	                        do {
	                            dx = sinkPattern[sinkPatternCounter][0];
	                            dy = sinkPattern[sinkPatternCounter][1];
	                            sinkPatternCounter++;
	                        } while (sinkPatternCounter < sinkPatternLengths
	                                && currentTarget.direction != -1
	                                && (((currentTarget.direction != 0) && (dy == 0))
	                                || ((currentTarget.direction == 0) && (dx == 0))));
	                        x = cx + dx;
	                        y = cy + dy;

	                        if (x > 9 || x < 0 || y > 9 || y < 0) { // we went off the board!
	                            failsafe++;
	                            continue;
	                        } else if (myBoard[x][y] == 0) { // we're on the board and square is free
	                            ret = new Coordinate(x, y);
	                            break;
	                        } else { // we're on the board but the square has already been hit
	                            failsafe++;
	                        }
	                    } while (failsafe < 100
	                            && sinkPatternCounter < sinkPatternLengths);
	                }

	                // Reset to FIRE state if no free square was found
	                // At this point either:
	                // a) we have a valid shot
	                // b) x == y == -1
	                // c) (x,y) is off the board
	                if (x > 9 || x < 0 || y > 9 || y < 0) {
	                    if (DEBUG >= V) {
	                        System.out.format(" *** Commodore0: ERROR state SINK: could not find a valid shot\n");
	                    }
	                    myState = FIRE;
	                    ret = makeRandomAttack();
	                }
	        }
	        if (DEBUG >= VVV) {
	            System.out.format("Commodore0: firing at (%d, %d)\n", ret.getX(), ret.getY());
	        }
	        markBoard(ret);
	        return ret;
	    }

	    // Informs you of the result of your most recent attack
	    @Override
	    public void resultOfAttack(int result) {
	        // change states or update target stack if neccesary
	        int mhs = result - (result % 10);
	        int type = result % 10;

	        switch (myState) {
	            case FIRE:
	                if (result == MISS) {
	                } // bummer
	                if (mhs == HIT_MODIFIER) { // hit
	                    if (DEBUG >= VVV) {
	                        System.out.format("Commodore0: (state %d) HIT a type %d ship!\n", myState, type);
	                    }
	                    currentTarget = new S(myLastShot, -1, type);
	                    opponentShipMap[myLastShot.getX()][myLastShot.getY()]++;
	                    myState = SINK;
	                    sinkPatternCounter = 0;
	                }
	                if (mhs == SUNK_MODIFIER) { // sink, good job!
	                    doSunk(type);
	                }
	                break;
	            case SINK:
	                if (result == MISS) {
	                } // bummer
	                if (mhs == HIT_MODIFIER) { // hit
	                    if (DEBUG >= VVV) {
	                        System.out.format("Commodore0: (state %d) HIT a type %d ship!\n", myState, type);
	                    }
	                    opponentShipMap[myLastShot.getX()][myLastShot.getY()]++;
	                    // we hit the ship we meant to!
	                    if (type == currentTarget.getModel()) {
	                        int dx = myLastShot.getX() - currentTarget.location.getX();
	                        int dy = myLastShot.getY() - currentTarget.location.getY();
	                        if (dx != 0) // direction of ship is horiz
	                        {
	                            currentTarget.direction = 0;
	                        } else {
	                            currentTarget.direction = 1;
	                        }
	                        if (DEBUG >= VVV) {
	                            System.out.format("Commodore0: (state %d) found enemy ship DIRECTION %d\n", myState, currentTarget.direction);
	                        }
	                        //currentTarget.hits++;
	                    } else { // we hit a different type of ship!
	                        newTarget = new S(myLastShot, -1, type);
	                        targetMap.put(intInt[type], newTarget); // put new target on the map
	                        // myState remains the same
	                    }
	                }
	                if (mhs == SUNK_MODIFIER) { // goal!
	                    doSunk(type);
	                }
	                break;
	            default:
	                break;
	        }
	    }

	    private void doSunk(int type) {
	        if (DEBUG >= VV) {
	            System.out.format("Commodore0: (state %d) SUNK a type %d ship!\n", myState, type);
	        }
	        shipLengthsRemaining.remove(new Integer(shipLengths[type])); // remove the length of the ship we just sunk
	        if (DEBUG >= VV) {
	            System.out.format("Commodore0: number of opponent's ships remaining: %d\n", shipLengthsRemaining.size());
	        }
	        if (DEBUG >= VV) {
	            System.out.format("Commodore0: max size remaining: %d\n",
	                    shipLengthsRemaining.isEmpty() ? 0
	                    : Collections.max(shipLengthsRemaining));
	        }

	        opponentShipMap[myLastShot.getX()][myLastShot.getY()]++;

	        if (type == currentTarget.getModel()) { // this is what we expect
	            // check the targetMap
	            if (targetMap.isEmpty()) {
	                sinkPatternCounter = 0;
	                myState = FIRE;
	            } else {
	                Integer t = (Integer) targetMap.keySet().iterator().next();
	                currentTarget = (S) targetMap.get(t);
	                targetMap.remove(t);
	                sinkPatternCounter = 0;
	                myState = SINK;
	            }
	        } // we are in SINK state tring to hit type X but we just somehow
	        // sunk a type Y. The type Y ship should be in targetMap, so we
	        // need to remove it now.
	        else {
	            targetMap.remove(intInt[type]);
	        }
	    }

	    // Informs you of the position of an attack against you.
	    @Override
	    public void opponentAttack(Coordinate coord) {
	        opponentAttackMap[coord.getX()][coord.getY()]++;
	    }

	    // Informs you of the result of the game.
	    @Override
	    public void resultOfGame(int result) {
	        // Don't do anything with this data
	        if (DEBUG >= V) {
	            System.out.format("Commodore0: result = %s", result == 1 ? "WIN\n" : "LOSS\n");
	        }
	        if (DEBUG >= V) { // print opponent maps on 500'th round
	            System.out.format("Commodore0: opponentShipMap\n\n");
	            printBoard(opponentShipMap);
	            System.out.format("\nCommodore0: opponentAttackMap\n\n");
	            printBoard(opponentAttackMap);
	        }
	        currentRound++;
	    }

	    // print a 10x10 array for analysis and debugging purposes
	    private void printBoard(int[][] board) {
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                System.out.format("%6d, ", board[i][j]);
	            }
	            System.out.format("\n");
	        }
	    }

	}

	// simple extension of class Ship
	class S extends Ship implements Comparable<S> {

	    public int score;

	    public S(int x, int y, int d, int type) {
	        super(new Coordinate(x, y), d, type);
	        this.score = 0;
	    }

	    public S(Coordinate c, int d, int type) {
	        super(c, d, type);
	        this.score = 0;
	    }

	    @Override
	    public boolean equals(Object o) {
	        if (!(o instanceof S)) {
	            return false;
	        }
	        S s = (S) o;
	        return ((s.type() == this.getModel())
	                && (s.location().getX() == this.location.getX())
	                && (s.location().getY() == this.location.getY())
	                && (s.direction() == this.direction));
	    }

	    @Override
	    public int hashCode() {
	        return (101 * this.getModel() + 67 * this.direction
	                + 43 * this.location.getX() + 19 * this.location.getY());
	    }

	    @Override
	    public int compareTo(S s) {
	        if (this.score > s.score) {
	            return 1;
	        } else if (this.score == s.score) {
	            return 0;
	        } else {
	            return -1;
	        }
	    }

	    public Coordinate location() {
	        return this.location;
	    }

	    public int direction() {
	        return this.direction;
	    }

	    public int length() {
	        return this.length;
	    }

	    public int type() {
	        return this.getModel();
	    }
	}
	// simple class for targets
	// class BSTarget {
//	     public BSTarget(int x, int y, int type) {
//	 	this.x = x;
//	 	this.y = y;
//	 	this.hits = 1;
//	 	this.dir = -1; // unknown
//	 	this.x2 = -1;
//	 	this.y2 = -1;
//	 	this.type = type;
//	 	if (type >= 0 && type <= 4) {
//	 	    this.length = this.shipLengths[type];
//	 	}
//	 	else {
//	 	    this.length = -1; // error?
//	 	}
//	     }
//	     public int x, y; // location of initial hit
//	     public int x2, y2; // location of subsequent hit
//	     public int hits;
//	     public int dir; // 0 = horiz, 1 = vert
//	     public int type;
//	     public int[] shipLengths = { 2, 3, 3, 4, 5};
//	     public int length;
//	     public int orientation;
	// }	
	public class MyCptCasanova implements MyCaptain {

	    static final int GRIDSIZE = 10;
	    private Random generator;
	    private Fleet myFleet;
	    private byte[][] myGrid;
	    private byte[][] hisGrid;
	    private int[][] shotStats = {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
	    private int[][] boatStats = {{0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};
	    private ArrayList<Coordinate> primary;
	    private ArrayList<Coordinate> secondary;
	    private Queue<Hit> hitQueue;
	    private Coordinate lastAttack;
	    private byte currentTarget = -1;
	    private Queue<StringBuffer> last6Turns;
	    private StringBuffer turn;
	    private int turnCount;

	    @Override
	    public void initialize(int numMatches, int numCaptains, String opponent) {
	        //turn=new StringBuffer();
	        //last6Turns=new LinkedList<StringBuffer>();
	        //log("###################GAME START########################");
	        hisGrid = new byte[10][10];
	        myGrid = new byte[10][10];
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                hisGrid[i][j] = 0;
	                myGrid[i][j] = 0;
	            }
	        }
	        turnCount = 0;
	        hitQueue = new LinkedList<>();
	        primary = new ArrayList<>();
	        secondary = new ArrayList<>();
	        generator = new Random();
	        myFleet = new Fleet();
	        int attackSetModifier = generator.nextInt(4);

	        // generate sets
	        boolean primaryUpper = false;
	        int offset = attackSetModifier;
	        if (offset > 1) {
	            primaryUpper = true;
	            offset -= 2;
	        }

	        for (int col = 0; col < GRIDSIZE; col++) {
	            for (int row = 0; row < GRIDSIZE; row++) {
	                if (row == offset) {
	                    cordSetAdder(primaryUpper, col, row);
	                    primaryUpper = !primaryUpper;
	                    offset += 2;
	                    //	System.out.println("Offset at 1 =" + offset);
	                    if (offset == 9) {
	                        cordSetAdder(primaryUpper, col, 9);
	                        offset -= 9;
	                        //System.out.println("Offest at 2 =" + offset);
	                    } else if (offset > 9) {
	                        offset -= 9;
	                        //	System.out.println("Offest at 3 =" + offset);
	                    }
	                }
	            }
	        }
	        Collections.shuffle(primary);
	        Collections.shuffle(secondary);
	        // System.out.println(primary.toString());
	        // Each type of ship must be placed on the board.
	        // the .place methods return whether it was possible to put a ship at the indicated position.
	        randomShipPlacer();


	    }

	    @Override
	    public Fleet getFleet() {
	        return myFleet;
	    }

	    @Override
	    public Coordinate makeAttack() {
	        /*		String event="Current hitQueue\n";
	         for(Hit h: hitQueue){
	         if(h!=null){
	         event=event+h.toString()+"\n____________________________\n";
	         }

	         }*/
	        ////log(event);
	        Coordinate attack;

	        if (hitQueue.isEmpty()) {
	            currentTarget = -1;

	        } else {

	            currentTarget = (byte) hitQueue.peek().getShipID();
	        }

	        if (currentTarget != -1) {
	            attack = hitQueue.peek().nextAttack();
	            if (primary.contains(attack)) {
	                primary.remove(attack);
	                //TODO check this remove code to see if it checks exsistance for us also see if it uses equals method
	            }
	            if (secondary.contains(attack)) {
	                secondary.remove(attack);
	                //TODO check this remove code to see if it checks exsistance for us also see if it uses equals method
	            }
	        } else if (primary.isEmpty()) {
	            attack = secondary.remove(0);
	        } else {
	            attack = primary.remove(0);
	        }

	        lastAttack = attack;
	        //log("attacking location " + attack);
	        return attack;
	    }

	    @Override
	    public void resultOfAttack(int result) {

	        hisGrid[lastAttack.getY()][lastAttack.getX()] = -1;

	        int shipid;

	        //log("result of attack is " + result);
	        if (result <= 14) {
	            shipid = result - Constants.HIT_MODIFIER;
	            hisGrid[lastAttack.getY()][lastAttack.getX()] = (byte) (shipid + 1);
	            if (hitQueue.isEmpty()) {
	                hitQueue.add(new Hit(lastAttack, shipid));
	            } else {
	                if (shipid != currentTarget) {
	                    hitQueue.peek().lastShotHit(false);
	                    boolean found = false;
	                    for (Hit h : hitQueue) {
	                        if (h.getShipID() == shipid) {
	                            found = true;
	                            h.addOtherHit(lastAttack);
	                            if (h.getOrgLocation().getX() == lastAttack.getX()) {
	                                h.setUpDown();
	                            } else {
	                                h.setLR();
	                            }
	                        }
	                    }
	                    if (found == false) {
	                        hitQueue.add(new Hit(lastAttack, shipid));
	                    }
	                } else {
	                    hitQueue.peek().lastShotHit(true);
	                }
	            }

	        } else if (result <= 24) {
	            shipid = result - Constants.SUNK_MODIFIER;
	            if (shipid == currentTarget) {
	                hisGrid[lastAttack.getY()][lastAttack.getX()] = (byte) (shipid + 1);
	                //log("removing ship " + shipid+ "from hitQueue" );
	                hitQueue.poll();
	            } else {
	                Hit temp = null;
	                for (Hit h : hitQueue) {
	                    if (h.getShipID() == shipid) {
	                        temp = h;
	                    }
	                }
	                hitQueue.remove(temp);
	            }

	        } else if (result == 106 && currentTarget != -1) {
	            hisGrid[lastAttack.getY()][lastAttack.getX()] = -1;
	            hitQueue.peek().lastShotHit(false);
	        } else if (result == 107) {
	            //TODO some condition for loosing or winning whichever this means;
	        } else {
	            hisGrid[lastAttack.getY()][lastAttack.getX()] = -1;
	        }
	        turnCount++;
	        //turnLog(turnCount);

	    }

	    @Override
	    public void opponentAttack(Coordinate coord) {

	        shotStats[coord.getY()][coord.getX()]++;

	    }

	    @Override
	    public void resultOfGame(int result) {
	        //log("$$$$$$$$$$$$$$$$GAME END$$$$$$$$$$$$$$");
	    }

	    private void cordSetAdder(boolean pOrF, int col, int row) {
	        if (pOrF) {
	            secondary.add(new Coordinate(col, row));
	        } else {
	            primary.add(new Coordinate(col, row));
	        }
	    }

	    private boolean validAttack(Coordinate c) {
	        //log("in validAttack coord is " + c);
	        if (c.getX() > 9 || c.getX() < 0 || c.getY() > 9 || c.getY() < 0) {
	            //log("invalid out of bounds");
	            return false;
	        } else if (hisGrid[c.getY()][c.getX()] != 0) {
	            //log("invalid coord allready used");
	            return false;
	        } else //log("Valid attack");
	        {
	            return true;
	        }

	    }

	    private void dataDump() {
	        for (StringBuffer s : last6Turns) {
	            System.out.println(s.toString());
	        }
	        System.out.println("HisGrid");
	        System.out.println("   0,  1,  2,  3,  4,  5,  6,  7,  8,  9");
	        for (int i = 0; i < hisGrid.length; i++) {
	            System.out.print(i + "|");
	            for (int j = 0; j < hisGrid[i].length; j++) {
	                if (hisGrid[i][j] == -1) {
	                    System.out.print(hisGrid[i][j] + ", ");
	                } else {
	                    System.out.print(" " + hisGrid[i][j] + ", ");
	                }
	            }
	            System.out.println();
	        }

	        System.out.println("\nHitStack\n");
	        for (Hit h : hitQueue) {
	            System.out.println(h.toString());
	        }
	        System.out.println("\nLastAttack: " + lastAttack);
	        System.out.println();
	    }

	    private void turnLog(int c) {
	        turn.append("______________________________END_OF_TURN_");
	        turn.append(c);
	        turn.append("______________________________\n");
	        last6Turns.add(turn);
	        if (last6Turns.size() > 15) {
	            last6Turns.remove();
	        }
	        //turn=new StringBuffer();
	    }

	    private void log(String event) {
	        turn.append(event);
	        turn.append("\n");
	    }

	    private void randomShipPlacer() {
	        for (int ship = 4; ship >= 0; ship--) {
	            int shipLength = 0;
	            switch (ship) {
	                case 0:
	                    shipLength = Constants.PATROL_BOAT_LENGTH;
	                    break;
	                case 1:
	                    shipLength = Constants.DESTROYER_LENGTH;
	                    break;
	                case 2:
	                    shipLength = Constants.SUBMARINE_LENGTH;
	                    break;
	                case 3:
	                    shipLength = Constants.BATTLESHIP_LENGTH;
	                    break;
	                case 4:
	                    shipLength = Constants.AIRCRAFT_CARRIER_LENGTH;
	                    break;
	            }

	            int x;
	            int y;
	            Bag grabBag = new Bag();
	            boolean unplaced;
	            do {
	                unplaced = true;
	                x = generator.nextInt(10);
	                y = generator.nextInt(10);

	                for (int i = 0; i < 2; i++) {
	                    int k = 0;
	                    int plusVal = -1;
	                    int minusVal = -1;
	                    while (k <= shipLength && (minusVal == -1 || plusVal == -1)) {
	                        if (i == 0) {
	                            if (x - k < 0 && minusVal == -1) {
	                                minusVal = k;
	                            } else {
	                                if (minusVal == -1) {
	                                    if (myGrid[x - k][y] != 0) {
	                                        minusVal = k;
	                                    }
	                                }
	                            }
	                            if (x + k > 9 && plusVal == -1) {
	                                plusVal = k;
	                            } else {
	                                if (plusVal == -1) {
	                                    if (myGrid[x + k][y] != 0) {
	                                        plusVal = k;
	                                    }
	                                }
	                            }
	                        } else {
	                            if (y - k < 0 && minusVal == -1) {
	                                minusVal = k;
	                            } else {
	                                if (minusVal == -1) {
	                                    if (myGrid[x][y - k] != 0) {
	                                        minusVal = k;
	                                    }
	                                }
	                            }
	                            if (y + k > 9 && plusVal == -1) {
	                                plusVal = k;
	                            } else {
	                                if (plusVal == -1) {
	                                    if (myGrid[x][y + k] != 0) {
	                                        plusVal = k;
	                                    }
	                                }
	                            }

	                        }
	                        k++;
	                    }
	                    if (minusVal == -1) {
	                        minusVal = shipLength;
	                    }
	                    if (plusVal == -1) {
	                        plusVal = shipLength;
	                    }

	                    if (plusVal + minusVal > shipLength) {
	                        int iterations;
	                        if (minusVal < plusVal) {
	                            iterations = minusVal;
	                        } else {
	                            iterations = plusVal;
	                        }
	                        if (i == 0) {
	                            for (int j = 0; j < iterations; j++) {
	                                grabBag.addPosition(new Pos((x - (minusVal - 1)) + j, y, i));
	                            }
	                        } else {
	                            for (int j = 0; j < iterations; j++) {
	                                grabBag.addPosition(new Pos(x, (j + (y - (minusVal - 1))), i));
	                            }
	                        }
	                    }

	                }
	                if (grabBag.isEmpty()) {
	                    unplaced = true;
	                } else {
	                    Pos p = null;
	                    while (unplaced && !grabBag.isEmpty()) {
	                        p = grabBag.pick();
	                        unplaced = !(myFleet.placeShip(p.getX(), p.getY(), p.getDir(), ship));
	                    }
	                    if (unplaced == false) {
	                        for (int i = 0; i < shipLength; i++) {
	                            if (p.getDir() == 0) {
	                                myGrid[p.getX() + i][p.getY()] = (byte) (ship + 1);
	                            } else {
	                                myGrid[p.getX()][p.getY() + i] = (byte) (ship + 1);
	                            }
	                        }
	                    }
	                }
	            } while (unplaced);



	        }
	    }

	    public class Hit {

	        private Coordinate orgHitLocation;
	        private ArrayList<Coordinate> otherhitLocations;
	        private int savedDirection;	//saved direction to shoot in
	        private boolean[] direction = {true, true, true, true};	//0 = left, 1 = right, 2 = up, 3 = down
	        private int shotsMade;
	        private int shipID;
	        private boolean UD = false, LR = false;
	        private boolean lastShotHit;

	        public Hit(Coordinate cord, int ship) {
	            orgHitLocation = cord;
	            shipID = ship;
	            shotsMade = 1;
	            savedDirection = 0;
	            otherhitLocations = new ArrayList<>(1);

	        }

	        public Coordinate getOrgLocation() {
	            return orgHitLocation;
	        }

	        public int getShipID() {
	            return shipID;
	        }

	        public boolean isUD() {
	            return UD;
	        }

	        public void setUpDown() {
	            this.UD = true;
	        }

	        public void setLR() {
	            this.LR = true;
	        }

	        public boolean isLR() {
	            return LR;
	        }

	        public void addOtherHit(Coordinate c) {
	            otherhitLocations.add(c);
	        }

	        /**
	         * If the last shot taken was a hit, the number of shots taken is
	         * increased and makes it so that the computer can't shoot in a
	         * direction that the ship can't be (if the ship was hit to the right of
	         * the original spot, the ship can't be up or down
	         *
	         * @param hit boolean if the ship was hit or not
	         */
	        public void lastShotHit(boolean hit) {
	            lastShotHit = hit;
	            //log("*************Entering Last Shot *********************"+this.toString());
	            if (lastShotHit) {
	                //log("Last shot was true");
	                shotsMade++;
	                if (savedDirection == 0 || savedDirection == 1) {
	                    //log("setting Up and down false");
	                    LR = true;
	                    direction[2] = false;
	                    direction[3] = false;
	                } else if (savedDirection == 2 || savedDirection == 3) {
	                    //log("setting right and left false");
	                    UD = true;
	                    direction[0] = false;
	                    direction[1] = false;
	                }
	            } else {
	                //log("Last shot was FALSE");
	                shotsMade = 1;
	                direction[savedDirection] = false;

	                if (savedDirection == 0) {
	                    savedDirection = 1;
	                } else if (savedDirection == 1) {
	                    savedDirection = 2;
	                } else if (savedDirection == 2) {
	                    savedDirection = 3;
	                } else if (savedDirection == 3) {
	                    savedDirection = 0;
	                }

	                //log("savedDirection now =" + savedDirection);
	            }
	        }

	        /**
	         * Calculates the next shot to take
	         *
	         * @return Returns an array holding row and column values
	         */
	        public Coordinate nextAttack() {
	            //log("**************Entering nextAttack***********"+this.toString());
	            int[] cords = new int[2];
	            Coordinate attack;

	            boolean invalidPick = false;

	            do {
	                if (direction[0] == true) //left
	                {
	                    //log("Direction right was true");
	                    cords[0] = orgHitLocation.getX() - shotsMade;
	                    cords[1] = orgHitLocation.getY();
	                } else if (direction[1] == true)///right
	                {
	                    //log("Direction left was true");
	                    cords[0] = orgHitLocation.getX() + shotsMade;
	                    cords[1] = orgHitLocation.getY();
	                } else if (direction[2] == true)/// up
	                {
	                    //log("Direction up was true");
	                    cords[0] = orgHitLocation.getX();
	                    cords[1] = orgHitLocation.getY() - shotsMade;
	                } else if (direction[3] == true)//down
	                {
	                    //log("Direction down was true");
	                    cords[0] = orgHitLocation.getX();
	                    cords[1] = orgHitLocation.getY() + shotsMade;
	                } else if (otherhitLocations.isEmpty()) {

	                    System.out.println("All directions false! ERROR!");
	                    System.out.println("Dumping Data.....");
	                    dataDump();

	                    return null;
	                } else {
	                    //log("############Double Hit##################");
	                    orgHitLocation = otherhitLocations.remove(0);
	                    //log("Switching orignal coordnates to " + orgHitLocation);
	                    if (LR) {
	                        //log("Setting direction 0 and 1 true");
	                        direction[0] = true;
	                        direction[1] = true;
	                        savedDirection = 0;
	                        shotsMade = 1;
	                    } else if (UD) {
	                        //log("Setting direction 2 and 3 true");
	                        direction[2] = true;
	                        direction[3] = true;
	                        savedDirection = 2;
	                        shotsMade = 1;
	                    } else {
	                        //log("Error UD and LR false");
	                        System.out.println("Dumping Data...");
	                        dataDump();
	                    }
	                    return nextAttack();
	                }
	                attack = new Coordinate(cords[0], cords[1]);

	                if (!validAttack(attack)) {
	                    //log(attack + " Was invalid!");
	                    invalidPick = true;
	                    lastShotHit(false);
	                } else {
	                    invalidPick = false;

	                }
	            } while (invalidPick);

	            //log("nextAttack validated Coordinate " + attack);
	            return attack;
	        }

	        @Override
	        public String toString() {
	            return "\nValues are: ShipID:" + shipID + "\n\torgHitLocation"
	                    + orgHitLocation + "\n\tshotsMade: " + shotsMade + "\n\tDirection:" + direction[0] + ", " + direction[1] + ", " + direction[2] + ", " + direction[3]
	                    + "\n\tsavedDirection: " + savedDirection + "\n\tlastShotHit: " + lastShotHit + "\n\tLR:" + LR + " and  UD " + UD + "\n\totherHitlocations"
	                    + otherhitLocations;
	        }
	    }

	    public class Bag {

	        private ArrayList<Pos> bag;

	        public Bag() {
	            bag = new ArrayList<>();
	        }

	        public void addPosition(Pos p) {
	            bag.add(p);
	        }

	        public Pos pick() {
	            if (bag.isEmpty()) {
	                return null;
	            }
	            for (int i = 0; i < 4; i++) {
	                Collections.shuffle(bag);
	            }
	            return bag.remove(0);
	        }

	        public int getSize() {
	            return bag.size();
	        }

	        public boolean isEmpty() {
	            return bag.isEmpty();
	        }
	    }

	    public class Pos {

	        private int x, y, dir;

	        public Pos(int x, int y, int dir) {
	            this.x = x;
	            this.y = y;
	            this.dir = dir;
	        }

	        public int getX() {
	            return x;
	        }

	        public int getY() {
	            return y;
	        }

	        public int getDir() {
	            return dir;
	        }
	    }
	}

	public class MyMalcolmReynolds implements MyCaptain, Constants {

	    int[][] baseGrid;
	    int[][] attackGrid;
	    int offset;
	    Random generator;
	    Fleet myFleet;
	    HashMap<Integer, Integer> remainingShips;
	    Coordinate lastAttack;
	    boolean hitShips;

	    @Override
	    public void initialize(int numMatches, int numCaptains, String opponent) {
	        generator = new Random();
	        offset = generator.nextInt(30);
	        myFleet = new Fleet();
	        baseGrid = new int[10][10];
	        attackGrid = new int[10][10];
	        hitShips = false;
	        remainingShips = new HashMap<>();
	        remainingShips.put(PATROL_BOAT, PATROL_BOAT_LENGTH);
	        remainingShips.put(SUBMARINE, SUBMARINE_LENGTH);
	        remainingShips.put(DESTROYER, DESTROYER_LENGTH);
	        remainingShips.put(BATTLESHIP, BATTLESHIP_LENGTH);
	        remainingShips.put(AIRCRAFT_CARRIER, AIRCRAFT_CARRIER_LENGTH);
	        processAttack();
	        placeShips();
	    }

	    @Override
	    public Fleet getFleet() {
	        return myFleet;
	    }

	    @Override
	    public Coordinate makeAttack() {
	        Coordinate attack;
	        if (hitShips) {
	            attack = continueAttack();
	        } else {
	            attack = probableCoordinate();
	        }
	        lastAttack = new Coordinate(attack.getX(), attack.getY());
	        return attack;
	    }

	    @Override
	    public void resultOfAttack(int result) {
	        attackGrid[lastAttack.getX()][lastAttack.getY()] = result;
	        if (result / 10 == 2) {
	            remainingShips.remove(result % 10);
	            for (int i = 0; i < 10; i++) {
	                for (int j = 0; j < 10; j++) {
	                    if (attackGrid[i][j] == result - 10 || attackGrid[i][j] == result) {
	                        attackGrid[i][j] = MISS;
	                    }
	                }
	            }
	            hitShips = false;
	        }
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                if (attackGrid[i][j] / 10 == 1) {
	                    hitShips = true;
	                    return;
	                }
	            }
	        }
	        processAttack();
	    }

	    @Override
	    public void opponentAttack(Coordinate coord) {
	    }

	    @Override
	    public void resultOfGame(int result) {
	    }

	    void placeShips() {
	        HashMap<Ship, int[]> ships = new HashMap<>();
	        int[] shipInfo;
	        for (int i = 4; i > -1; i--) {
	            while (ships.size() < (5 - i) * 4) {
	                shipInfo = new int[3];
	                shipInfo[0] = generator.nextInt(10);
	                shipInfo[1] = generator.nextInt(10);
	                shipInfo[2] = generator.nextInt(2);
	                Ship testShip = new Ship(new Coordinate(shipInfo[0], shipInfo[1]), shipInfo[2], i);
	                while (!testShip.isValid()) {
	                    shipInfo[0] = generator.nextInt(10);
	                    shipInfo[1] = generator.nextInt(10);
	                    shipInfo[2] = generator.nextInt(2);
	                    testShip = new Ship(new Coordinate(shipInfo[0], shipInfo[1]), shipInfo[2], i);
	                }
	                boolean doesIntersect = false;
	                for (Ship s : ships.keySet()) {
	                    if (s.intersectsShip(testShip)) {
	                        doesIntersect = true;
	                    }
	                }
	                if (!doesIntersect) {
	                    ships.put(testShip, shipInfo);
	                }
	            }
	        }
	        for (int i = 0; i < 5; i++) {
	            shipInfo = getRandomShip(ships, i);
	            myFleet.placeShip(shipInfo[0], shipInfo[1], shipInfo[2], i);
	        }
	    }

	    int[] getRandomShip(HashMap<Ship, int[]> ships, int model) {
	        int random = generator.nextInt(ships.size());
	        int counter = 1;
	        for (Ship s : ships.keySet()) {
	            if (random == counter && s.getModel() == model) {
	                return ships.get(s);
	            }
	            counter++;
	            if (counter > random) {
	                return getRandomShip(ships, model);
	            }
	        }
	        return new int[3];
	    }

	    Coordinate continueAttack() {
	        int x = 0, y = 0, vCounter = 0, hCounter = 0, x1 = 0, y1 = 0, maxValue = 0, attackValue;
	        boolean keepSearching = true;
	        for (int i = 0; i < 10 && keepSearching; i++) {
	            for (int j = 0; j < 10 && keepSearching; j++) {
	                if (attackGrid[i][j] / 10 == 1) {
	                    x = i;
	                    y = j;
	                    keepSearching = false;
	                }
	            }
	        }
	        attackValue = attackGrid[x][y];
	        for (int i = 0; i < 10; i++) {
	            if (attackGrid[x][i] == attackValue) {
	                vCounter++;
	            }
	            if (attackGrid[i][y] == attackValue) {
	                hCounter++;
	            }
	        }
	        if (vCounter > 1) {
	            int firstHit = 0;
	            int lastHit = 0;
	            boolean foundFirst = false;
	            for (int i = 0; i < 10; i++) {
	                if (attackGrid[x][i] == attackValue) {
	                    if (!foundFirst) {
	                        firstHit = i;
	                        foundFirst = true;
	                    } else {
	                        lastHit = i;
	                    }
	                }
	            }
	            for (int i = firstHit; i < lastHit; i++) {
	                if (attackGrid[x][i] == 0) {
	                    return new Coordinate(x, i);
	                }
	            }
	            if (lastHit < 9 && attackGrid[x][lastHit + 1] == 0) {
	                x1 = x;
	                y1 = lastHit + 1;
	                maxValue = baseGrid[x1][y1];
	            }
	            if (firstHit > 0 && baseGrid[x][firstHit - 1] > maxValue && attackGrid[x][firstHit - 1] == 0) {
	                x1 = x;
	                y1 = firstHit - 1;
	            }
	            return new Coordinate(x1, y1);
	        } else if (hCounter > 1) {
	            int firstHit = 0;
	            int lastHit = 0;
	            boolean foundFirst = false;
	            for (int i = 0; i < 10; i++) {
	                if (attackGrid[i][y] == attackValue) {
	                    if (!foundFirst) {
	                        firstHit = i;
	                        foundFirst = true;
	                    } else {
	                        lastHit = i;
	                    }
	                }
	            }
	            for (int i = firstHit; i < lastHit; i++) {
	                if (attackGrid[i][y] == 0) {
	                    return new Coordinate(i, y);
	                }
	            }
	            if (lastHit < 9 && attackGrid[lastHit + 1][y] == 0) {
	                x1 = lastHit + 1;
	                y1 = y;
	                maxValue = baseGrid[x1][y1];
	            }
	            if (firstHit > 0 && baseGrid[firstHit - 1][y] > maxValue && attackGrid[firstHit - 1][y] == 0) {
	                x1 = firstHit - 1;
	                y1 = y;
	            }
	            return new Coordinate(x1, y1);
	        }
	        if (x < 9 && baseGrid[x + 1][y] > maxValue && attackGrid[x + 1][y] == 0) {
	            x1 = x + 1;
	            y1 = y;
	            maxValue = baseGrid[x + 1][y];
	        }
	        if (x > 0 && baseGrid[x - 1][y] > maxValue && attackGrid[x - 1][y] == 0) {
	            x1 = x - 1;
	            y1 = y;
	            maxValue = baseGrid[x - 1][y];
	        }
	        if (y < 9 && baseGrid[x][y + 1] > maxValue && attackGrid[x][y + 1] == 0) {
	            x1 = x;
	            y1 = y + 1;
	            maxValue = baseGrid[x][y + 1];
	        }
	        if (y > 0 && baseGrid[x][y - 1] > maxValue && attackGrid[x][y - 1] == 0) {
	            x1 = x;
	            y1 = y - 1;
	        }
	        return new Coordinate(x1, y1);
	    }

	    int rightDistance(int x, int y) {
	        int counter = 0;
	        if (attackGrid[x][y] / 10 == MISS) {
	            return counter;
	        }
	        while (counter + x < 10) {
	            if (attackGrid[counter + x][y] == MISS) {
	                return counter;
	            }
	            counter++;
	        }
	        return counter;
	    }

	    int upDistance(int x, int y) {
	        int counter = 0;
	        if (attackGrid[x][y] == MISS) {
	            return counter;
	        }
	        while (counter + y < 10) {
	            if (attackGrid[x][counter + y] == MISS) {
	                return counter;
	            }
	            counter++;
	        }
	        return counter;
	    }

	    int[] distribution(int spaceLength, int shipLength) {
	        int[] dist = new int[spaceLength];
	        int iterations;
	        if (spaceLength > 2 * shipLength) {
	            iterations = shipLength;
	        } else {
	            iterations = spaceLength - shipLength + 1;
	        }
	        for (int i = 0; i < iterations; i++) {
	            for (int j = i; j < spaceLength - i; j++) {
	                dist[j]++;
	            }
	        }
	        return dist;
	    }

	    void addToGrid(int[] dist, int[][] grid, int x, int y, int direction) {
	        int length = dist.length;
	        if (direction == HORIZONTAL) {
	            for (int i = 0; i < length; i++) {
	                grid[x + i][y] += dist[i];
	            }
	        } else if (direction == VERTICAL) {
	            for (int i = 0; i < length; i++) {
	                grid[x][y + i] += dist[i];
	            }
	        }
	    }

	    Coordinate probableCoordinate() {
	        int smallestShip = 5;
	        int max = 0;
	        ArrayList<Coordinate> probableSquares = new ArrayList<>();
	        for (int ship : remainingShips.keySet()) {
	            if (remainingShips.get(ship) < smallestShip) {
	                smallestShip = remainingShips.get(ship);
	            }
	        }
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                if ((i + j + offset) % smallestShip == 0 && baseGrid[i][j] > max) {
	                    max = baseGrid[i][j];
	                }
	            }
	        }
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                if ((i + j + offset) % smallestShip == 0 && (double) baseGrid[i][j] > 0.95d * (double) max) {
	                    probableSquares.add(new Coordinate(i, j));
	                }
	            }
	        }
	        return probableSquares.get(generator.nextInt(probableSquares.size()));
	    }

	    void processAttack() {
	        baseGrid = new int[10][10];
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                if (attackGrid[j][i] != MISS) {
	                    int rd = rightDistance(j, i);
	                    if (rd != 0) {
	                        for (int ship : remainingShips.keySet()) {
	                            addToGrid(distribution(rd, remainingShips.get(ship)), baseGrid, j, i, HORIZONTAL);
	                        }
	                    }
	                    j += rd;
	                }
	            }
	            for (int j = 0; j < 10; j++) {
	                if (attackGrid[i][j] != MISS) {
	                    int ud = upDistance(i, j);
	                    if (ud != 0) {
	                        for (int ship : remainingShips.keySet()) {
	                            addToGrid(distribution(ud, remainingShips.get(ship)), baseGrid, i, j, VERTICAL);
	                        }
	                        j += ud;
	                    }
	                }
	            }
	        }
	    }
	}
	
	public class MyMRTwo implements MyCaptain, Constants {

	    HashMap<Integer, Integer> remainingShips;
	    int[][] metricGrid;
	    int[] shipLengths;
	    square[][] myGrid;
	    Random generator;
	    Fleet myFleet;
	    int lastX, lastY;
	    boolean hitShips;

	    @Override
	    public void initialize(int numMatches, int numCaptains, String opponent) {
	        generator = new Random();
	        myFleet = new Fleet();
	        metricGrid = new int[10][10];
	        myGrid = new square[10][10];
	        remainingShips = new HashMap<>();
	        hitShips = false;
	        shipLengths = new int[]{2, 3, 3, 4, 5};
	        lastX = -1;
	        int orientation = generator.nextInt(2);
	        for (int i = 0; i < 5; i++) {
	            remainingShips.put(i, shipLengths[i]);
	        }
	        for (int i = 0; i < 5; i++) {
	            while (!myFleet.placeShip((orientation == HORIZONTAL) ? shipLengths[i] * generator.nextInt(10 / shipLengths[i]) : generator.nextInt(10),
	                    (orientation == VERTICAL) ? shipLengths[i] * generator.nextInt(10 / shipLengths[i]) : generator.nextInt(10), orientation, i)) {
	            }
	        }
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                myGrid[i][j] = new square(i, j);
	                metricGrid[i][j] = myGrid[i][j].getValue(remainingShips);
	            }
	        }
	    }

	    @Override
	    public Fleet getFleet() {
	        return myFleet;
	    }

	    @Override
	    public Coordinate makeAttack() {
	        Coordinate attack;
	        if (lastX == -1) {
	            attack = new Coordinate(generator.nextInt(10), generator.nextInt(10));
	        } else if (!hitShips) {
	            attack = getAttack();
	        } else {
	            attack = continueAttack();
	        }
	        lastX = attack.getX();
	        lastY = attack.getY();
	        return attack;
	    }

	    @Override
	    public void resultOfAttack(int result) {
	        myGrid[lastX][lastY].status = result;
	        metricGrid[lastX][lastY] = 0;
	        if (result / 10 == 1) {
	            hitShips = true;
	        }
	        if (result / 10 == 2) {
	            remainingShips.remove(result % 10);
	            hitShips = false;
	            for (int i = 0; i < 10; i++) {
	                for (int j = 0; j < 10; j++) {
	                    if (myGrid[i][j].status % 10 == result % 10) {
	                        myGrid[i][j].status = result;
	                    } else if (myGrid[i][j].status / 10 == 1) {
	                        hitShips = true;
	                    }
	                    metricGrid[i][j] = myGrid[i][j].getValue(remainingShips);
	                }
	            }
	            return;
	        }
	        for (int i = 0; i < myGrid[lastX][lastY].right; i++) {
	            myGrid[lastX + i + 1][lastY].left = i;
	            metricGrid[lastX + i + 1][lastY] = myGrid[lastX + i + 1][lastY].getValue(remainingShips);
	        }
	        for (int i = 0; i < myGrid[lastX][lastY].left; i++) {
	            myGrid[lastX - i - 1][lastY].right = i;
	            metricGrid[lastX - i - 1][lastY] = myGrid[lastX - i - 1][lastY].getValue(remainingShips);
	        }
	        for (int i = 0; i < myGrid[lastX][lastY].up; i++) {
	            myGrid[lastX][lastY + i + 1].down = i;
	            metricGrid[lastX][lastY + i + 1] = myGrid[lastX][lastY + i + 1].getValue(remainingShips);
	        }
	        for (int i = 0; i < myGrid[lastX][lastY].down; i++) {
	            myGrid[lastX][lastY - i - 1].up = i;
	            metricGrid[lastX][lastY - i - 1] = myGrid[lastX][lastY - i - 1].getValue(remainingShips);
	        }
	    }

	    @Override
	    public void opponentAttack(Coordinate coord) {
	    }

	    @Override
	    public void resultOfGame(int result) {
	    }

	    Coordinate getAttack() {
	        int maxValue = 0;
	        ArrayList<Coordinate> maxCoords = new ArrayList<>();
	        for (int i = 0; i < 10; i++) {
	            for (int j = 0; j < 10; j++) {
	                if (metricGrid[i][j] == maxValue) {
	                    maxCoords.add(new Coordinate(i, j));
	                } else if (metricGrid[i][j] > maxValue) {
	                    maxCoords = new ArrayList<>();
	                    maxCoords.add(new Coordinate(i, j));
	                    maxValue = metricGrid[i][j];
	                }
	            }
	        }
	        return maxCoords.get(generator.nextInt(maxCoords.size()));
	    }

	    Coordinate continueAttack() {
	        int hCounter = 0, vCounter = 0, ship = 0, hitX = 0, hitY = 0, x = 0, y = 0, maxValue = 0;
	        boolean keepSearching = true;
	        for (int i = 0; i < 10 && keepSearching; i++) {
	            for (int j = 0; j < 10 && keepSearching; j++) {
	                if (myGrid[i][j].status / 10 == 1) {
	                    hitX = i;
	                    hitY = j;
	                    ship = myGrid[i][j].status % 10;
	                    keepSearching = false;
	                }
	            }
	        }
	        for (int i = hitX + 1; i < 10; i++) {
	            if (myGrid[i][hitY].status % 10 == ship) {
	                hCounter++;
	            } else {
	                break;
	            }
	        }
	        for (int j = hitY + 1; j < 10; j++) {
	            if (myGrid[hitX][j].status % 10 == ship) {
	                vCounter++;
	            } else {
	                break;
	            }
	        }
	        if (hCounter > 0 || (vCounter == 0 && myGrid[hitX][hitY].up + myGrid[hitX][hitY].down + 1 < shipLengths[ship])) {
	            if (hitX == 0) {
	                return new Coordinate(hitX + hCounter + 1, hitY);
	            }
	            if (hitX + hCounter + 1 > 9) {
	                return new Coordinate(hitX - 1, hitY);
	            }
	            if (metricGrid[hitX + hCounter + 1][hitY] > metricGrid[hitX - 1][hitY]) {
	                return new Coordinate(hitX + hCounter + 1, hitY);
	            }
	            return new Coordinate(hitX - 1, hitY);
	        }
	        if (vCounter != 0 || (hCounter == 0 && myGrid[hitX][hitY].left + myGrid[hitX][hitY].right + 1 < shipLengths[ship])) {
	            if (hitY == 0) {
	                return new Coordinate(hitX, hitY + vCounter + 1);
	            }
	            if (hitY + vCounter + 1 > 9) {
	                return new Coordinate(hitX, hitY - 1);
	            }
	            if (metricGrid[hitX][hitY + vCounter + 1] > metricGrid[hitX][hitY - 1]) {
	                return new Coordinate(hitX, hitY + vCounter + 1);
	            }
	            return new Coordinate(hitX, hitY - 1);
	        }
	        if (hitX < 9 && metricGrid[hitX + 1][hitY] > maxValue) {
	            x = hitX + 1;
	            y = hitY;
	            maxValue = metricGrid[x][y];
	        }
	        if (hitX > 0 && metricGrid[hitX - 1][hitY] > maxValue) {
	            x = hitX - 1;
	            y = hitY;
	            maxValue = metricGrid[x][y];
	        }
	        if (hitY < 9 && metricGrid[hitX][hitY + 1] > maxValue) {
	            x = hitX;
	            y = hitY + 1;
	            maxValue = metricGrid[x][y];
	        }
	        if (hitY > 0 && metricGrid[hitX][hitY - 1] > maxValue) {
	            x = hitX;
	            y = hitY - 1;
	        }
	        return new Coordinate(x, y);
	    }
	}

	class square {

	    int x, y, left, right, up, down, status;

	    square(int x, int y) {
	        this.x = x;
	        this.y = y;
	        left = x;
	        right = 9 - x;
	        down = y;
	        up = 9 - y;
	        status = 99999;
	    }

	    int getValue(HashMap<Integer, Integer> remainingShips) {
	        int score = 0;
	        if (status == 99999) {
	            int lrud = left * right + up * down + left + right + up + down;
	            for (int ship : remainingShips.keySet()) {
	                int s = remainingShips.get(ship);
	                score += lrud - s * (left / s + right / s + up / s + down / s);
	            }
	            score++;
	        }
	        return score;
	    }
	}
	}
