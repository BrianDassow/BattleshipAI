import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class MRTwo implements Captain {

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