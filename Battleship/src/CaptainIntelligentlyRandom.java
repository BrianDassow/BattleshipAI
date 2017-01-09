

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

public class CaptainIntelligentlyRandom implements Captain {

    enum Intent {

        PLACING_SHIPS, SEARCHING_FOR_LARGE_SHIP, SEARCHING_FOR_SMALL_SHIP, SEARCHING_FOR_TINY_SHIP, ATTACKING_SHIP, UNKNOWN
    };
    
    
    @SuppressWarnings("unchecked")
	ArrayList<Coordinate>[] enemyShipLocations = new ArrayList[5];

    Random generator;
    Fleet myFleet;
    int[][] myVision = new int[10][10];
    Coordinate lastAttack;
    int randomOffset;
    Stack<Intent> currentStates = new Stack<>();
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
            enemyShipLocations[i] = (ArrayList<Coordinate>) (new ArrayList<Coordinate>());
        }
        currentStates.clear();
        currentStates.push(Intent.PLACING_SHIPS);
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
        currentStates.push(Intent.SEARCHING_FOR_LARGE_SHIP);
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
            if (currentStates.peek() == Intent.ATTACKING_SHIP) {
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

        if (currentStates.peek() == Intent.SEARCHING_FOR_LARGE_SHIP && currentLargeShipCoords.isEmpty()) {
            currentStates.pop();
            currentStates.push(Intent.SEARCHING_FOR_SMALL_SHIP);
        }
        if (currentStates.peek() == Intent.SEARCHING_FOR_SMALL_SHIP && (currentSmallShipCoords.isEmpty() || (isShipSunk(SUBMARINE) && isShipSunk(DESTROYER) && isShipSunk(BATTLESHIP)))) {
            currentStates.pop();
            currentStates.push(Intent.SEARCHING_FOR_TINY_SHIP);
        }
        if (currentStates.peek() == Intent.SEARCHING_FOR_TINY_SHIP && currentTinyShipCoords.isEmpty()) {
            currentStates.pop();
            currentStates.push(Intent.UNKNOWN);
        }

        if (result == MISS || result == DEFEATED) {
            return;
        }

        if (result % HIT_MODIFIER != result) {
            enemyShipLocations[result % HIT_MODIFIER].add(lastAttack);
        }

        if (result % SUNK_MODIFIER != result && currentStates.peek() == Intent.ATTACKING_SHIP) {
            currentStates.pop();

            for (int i = 0; i < 5; i++) {
                if (!enemyShipLocations[i].isEmpty() && enemyShipLocations[i].size() != getShipLength(i)) {
                    currentShipFound = i;
                }
            }
        } else if (result % HIT_MODIFIER != result) {
            if (currentShipFound == -1) {
                currentStates.push(Intent.ATTACKING_SHIP);
                currentShipFound = result % HIT_MODIFIER;
            } else if (currentShipFound != result % HIT_MODIFIER) {
                currentStates.push(Intent.ATTACKING_SHIP);
                currentShipFound = result % HIT_MODIFIER;
            }

            if (currentStates.peek() != Intent.ATTACKING_SHIP) {
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