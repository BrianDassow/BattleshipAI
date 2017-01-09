import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MalcolmReynolds implements Captain {

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