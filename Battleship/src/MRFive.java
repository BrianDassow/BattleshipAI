
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class MRFive implements Captain {

    Random generator;
    Fleet myFleet;
    square[][] board;
    int[] shipLengths;
    boolean hitShips;
    int smallestShip, smallestShipModel, lastX, lastY, numRounds;
    LinkedList<square> queue;
    HashMap<Integer, Integer> remainingShips;
    HashMap<String, opponentInfo> opponents;
    int[][] currentOpponentAP;
    opponentInfo currentOpponent;
    final int NEGATIVE = -1;
    final int POSITIVE = 1;
    final int UNATTACKED = 99999;

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        generator = new Random();
        myFleet = new Fleet();
        board = new square[10][10];
        queue = new LinkedList<>();
        smallestShip = 2;
        smallestShipModel = 0;
        numRounds = 0;
        int offset = generator.nextInt(smallestShip);
        int orientation = 2 * generator.nextInt(2) - 1;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                board[i][j] = new square(i, j);
                if ((i + orientation * j - offset) % smallestShip == 0) {
                    queue.add(board[i][j]);
                }
            }
        }
        remainingShips = new HashMap<>();
        shipLengths = new int[]{2, 3, 3, 4, 5};
        for (int i = 0; i < 5; i++) {
            remainingShips.put(i, shipLengths[i]);
        }
        hitShips = false;
        if (opponents == null) {
            opponents = new HashMap<>();
        }
        if (!opponents.containsKey(opponent)) {
            opponents.put(opponent, new opponentInfo());
        }
        currentOpponent = opponents.get(opponent);
        currentOpponentAP = currentOpponent.attackPattern;
        placeShips();
    }

    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    @Override
    public Coordinate makeAttack() {
        Coordinate attack;
        numRounds++;
        if (!hitShips) {
            int value = 0;
            int maxValue = 0;
            int[][] values = new int[10][10];
            square toAttack = new square(0, 0);
            if (queue.isEmpty()) {
                makeQueue();
            }
            for (square s : queue) {
                values[s.x][s.y] = s.getValue(remainingShips) * currentOpponent.shipProb(smallestShipModel, s.x, s.y);
                maxValue += values[s.x][s.y];
            }
            maxValue = generator.nextInt(maxValue);
            for (square s : queue) {
                value += values[s.x][s.y];
                if (value > maxValue) {
                    toAttack = s;
                    break;
                }
            }
            queue.remove(toAttack);
            attack = new Coordinate(toAttack.x, toAttack.y);
        } else {
            attack = continueAttack();
        }
        lastX = attack.getX();
        lastY = attack.getY();
        return attack;
    }

    @Override
    public void resultOfAttack(int result) {
        board[lastX][lastY].status = result;
        int lCounter = 0, dCounter = 0, rCounter = 0, uCounter = 0;
        if (result / 10 == 1) {
            hitShips = true;
        } else if (result / 10 == 2) {
            remainingShips.remove(result % 10);
            hitShips = false;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (board[i][j].status % 10 == result % 10) {
                        board[i][j].status = result;
                    } else if (board[i][j].status / 10 == 1) {
                        hitShips = true;
                    }
                }
            }
        }
        if (result % 10 < 5 && result / 10 > 0) {
            currentOpponent.shipPattern[result % 10][lastX][lastY]++;
            currentOpponent.shipTotal[result % 10]++;
        }
        for (int i = 1; i < 10; i++) {
            lCounter = (board[i - 1][lastY].status != UNATTACKED) ? 0 : lCounter + 1;
            rCounter = (board[10 - i][lastY].status != UNATTACKED) ? 0 : rCounter + 1;
            dCounter = (board[lastX][i - 1].status != UNATTACKED) ? 0 : dCounter + 1;
            uCounter = (board[lastX][10 - i].status != UNATTACKED) ? 0 : uCounter + 1;
            board[i][lastY].left = lCounter;
            board[10 - i - 1][lastY].right = rCounter;
            board[lastX][i].down = dCounter;
            board[lastX][10 - i - 1].up = uCounter;
        }
        if (!hitShips && result != MISS) {
            makeQueue();
        }
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        currentOpponentAP[coord.getX()][coord.getY()]++;
    }

    @Override
    public void resultOfGame(int result) {
    }

    void placeShips() {
        int minValue, thisValue;
        int[] chosenPatrol;
        LinkedList<int[]> patrol = new LinkedList<>();
        minValue = currentOpponentAP[0][0] + currentOpponentAP[0][1];
        patrol.add(new int[]{0, 0, VERTICAL, minValue});
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                thisValue = currentOpponentAP[i][j] + currentOpponentAP[i][j + 1];
                if (thisValue <= minValue * 21 / 20) {
                    if (thisValue < minValue) {
                        for (Iterator<int[]> it = patrol.iterator(); it.hasNext();) {
                            int[] info = it.next();
                            if (info[3] > thisValue * 21 / 20) {
                                it.remove();
                            }
                        }
                        minValue = thisValue;
                    }
                    patrol.add(new int[]{i, j, VERTICAL, thisValue});
                }
                thisValue = currentOpponentAP[j][i] + currentOpponentAP[j + 1][i];
                if (thisValue <= minValue * 21 / 20) {
                    if (thisValue < minValue) {
                        for (Iterator<int[]> it = patrol.iterator(); it.hasNext();) {
                            int[] info = it.next();
                            if (info[3] > thisValue * 21 / 20) {
                                it.remove();
                            }
                        }
                        minValue = thisValue;
                    }
                    patrol.add(new int[]{j, i, HORIZONTAL, thisValue});
                }
            }
        }
        chosenPatrol = patrol.get(generator.nextInt(patrol.size()));
        myFleet.placeShip(chosenPatrol[0], chosenPatrol[1], chosenPatrol[2], PATROL_BOAT);
        for (int i = 1; i < 5; i++) {
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
            }
        }
    }

    Coordinate continueAttack() {
        int hCounter = 0;
        int vCounter = 0;
        int ship = 0;
        int hitX = 0;
        int hitY = 0;
        int maxValue = 1;
        LinkedList<Coordinate> toAttack = new LinkedList<>();
        boolean keepSearching = true;
        for (int i = 0; i < 10 && keepSearching; i++) {
            for (int j = 0; j < 10 && keepSearching; j++) {
                if (board[i][j].status / 10 == 1) {
                    hitX = i;
                    hitY = j;
                    ship = board[i][j].status % 10;
                    keepSearching = false;
                }
            }
        }
        for (int i = hitX + 1; i < 10; i++) {
            if (board[i][hitY].status % 10 == ship) {
                hCounter++;
            } else {
                break;
            }
        }
        for (int j = hitY + 1; j < 10; j++) {
            if (board[hitX][j].status % 10 == ship) {
                vCounter++;
            } else {
                break;
            }
        }
        if (hCounter > 0) {
            if ((board[hitX + hCounter][hitY].right >= board[hitX][hitY].left && board[hitX + hCounter + 1][hitY].status == UNATTACKED) || board[hitX - 1][hitY].status != UNATTACKED) {
                if (board[hitX + hCounter][hitY].right == board[hitX][hitY].left && generator.nextInt(2) > 0) {
                    return new Coordinate(hitX - 1, hitY);
                } else {
                    return new Coordinate(hitX + hCounter + 1, hitY);
                }
            }
            return new Coordinate(hitX - 1, hitY);
        }
        if (vCounter > 0) {
            if ((board[hitX][hitY + vCounter].up >= board[hitX][hitY].down && board[hitX][hitY + vCounter + 1].status == UNATTACKED) || board[hitX][hitY - 1].status != UNATTACKED) {
                if (board[hitX][hitY + vCounter].up == board[hitX][hitY].down && generator.nextInt(2) > 0) {
                    return new Coordinate(hitX, hitY - 1);
                } else {
                    return new Coordinate(hitX, hitY + vCounter + 1);
                }
            }
            return new Coordinate(hitX, hitY - 1);
        }
        if (board[hitX][hitY].right >= maxValue && board[hitX + 1][hitY].status == UNATTACKED) {
            toAttack.add(new Coordinate(hitX + 1, hitY));
            maxValue = board[hitX][hitY].right;
        }
        if (board[hitX][hitY].left >= maxValue && board[hitX - 1][hitY].status == UNATTACKED) {
            if (board[hitX][hitY].left > maxValue) {
                toAttack = new LinkedList<>();
            }
            toAttack.add(new Coordinate(hitX - 1, hitY));
            maxValue = board[hitX][hitY].left;
        }
        if (board[hitX][hitY].up >= maxValue && board[hitX][hitY + 1].status == UNATTACKED) {
            if (board[hitX][hitY].up > maxValue) {
                toAttack = new LinkedList<>();
            }
            toAttack.add(new Coordinate(hitX, hitY + 1));
            maxValue = board[hitX][hitY].up;
        }
        if (board[hitX][hitY].down >= maxValue && board[hitX][hitY - 1].status == UNATTACKED) {
            if (board[hitX][hitY].down > maxValue) {
                toAttack = new LinkedList<>();
            }
            toAttack.add(new Coordinate(hitX, hitY - 1));
        }
        return toAttack.get(generator.nextInt(toAttack.size()));
    }

    public void makeQueue() {
        LinkedList<LinkedList<square>> components = new LinkedList<>();
        LinkedList<square> unattacked = new LinkedList<>();
        queue = new LinkedList<>();
        smallestShip = 5;
        for (Integer i : remainingShips.keySet()) {
            if (remainingShips.get(i) < smallestShip) {
                smallestShip = remainingShips.get(i);
                smallestShipModel = i;
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (board[i][j].wouldFit(smallestShip)) {
                    unattacked.add(board[i][j]);
                }
            }
        }
        while (!unattacked.isEmpty()) {
            LinkedList<square> component = new LinkedList<>();
            floodFill(unattacked, component, unattacked.get(0));
            for (Iterator<square> it = component.iterator(); it.hasNext();) {
                square s = it.next();
                unattacked.remove(s);
            }
            components.add(component);
        }
        for (LinkedList<square> c : components) {
            int min = 100;
            LinkedList<int[]> bestDiagonals = new LinkedList<>();
            int[] pOffset = new int[smallestShip];
            int[] nOffset = new int[smallestShip];
            int[] index;
            for (square s : c) {
                pOffset[toIndex(s.x + s.y)]++;
                nOffset[toIndex(s.x - s.y)]++;
            }
            for (int i = 0; i < smallestShip; i++) {
                if (pOffset[i] <= min) {
                    if (pOffset[i] < min) {
                        bestDiagonals = new LinkedList<>();
                    }
                    bestDiagonals.add(new int[]{i, POSITIVE});
                    min = pOffset[i];
                }
                if (nOffset[i] <= min) {
                    if (nOffset[i] < min) {
                        bestDiagonals = new LinkedList<>();
                    }
                    bestDiagonals.add(new int[]{i, NEGATIVE});
                    min = nOffset[i];
                }
            }
            index = bestDiagonals.get(generator.nextInt(bestDiagonals.size()));
            for (square s : c) {
                if ((s.x + index[1] * s.y - index[0]) % smallestShip == 0) {
                    queue.add(s);
                }
            }
        }
    }

    int toIndex(int index) {
        return (index % smallestShip >= 0) ? index % smallestShip : (index % smallestShip) + smallestShip;
    }

    private void floodFill(LinkedList<square> unattacked, LinkedList<square> component, square node) {
        component.add(node);
        if (node.left > 0 && unattacked.contains(board[node.x - 1][node.y]) && !component.contains(board[node.x - 1][node.y])) {
            floodFill(unattacked, component, board[node.x - 1][node.y]);
        }
        if (node.right > 0 && unattacked.contains(board[node.x + 1][node.y]) && !component.contains(board[node.x + 1][node.y])) {
            floodFill(unattacked, component, board[node.x + 1][node.y]);
        }
        if (node.down > 0 && unattacked.contains(board[node.x][node.y - 1]) && !component.contains(board[node.x][node.y - 1])) {
            floodFill(unattacked, component, board[node.x][node.y - 1]);
        }
        if (node.up > 0 && unattacked.contains(board[node.x][node.y + 1]) && !component.contains(board[node.x][node.y + 1])) {
            floodFill(unattacked, component, board[node.x][node.y + 1]);
        }
    }

    class square {

        int x, y;
        int left, right, up, down;
        int status;

        square(int x, int y) {
            this.x = x;
            this.y = y;
            left = x;
            right = 9 - x;
            down = y;
            up = 9 - y;
            status = UNATTACKED;
        }

        public boolean wouldFit(int shipLength) {
            if (status == UNATTACKED && (right + left + 1 >= shipLength || up + down + 1 >= shipLength)) {
                return true;
            }
            return false;
        }

        public int getValue(HashMap<Integer, Integer> remainingShips) {
            int value = 0;
            if (status == UNATTACKED) {
                for (int ship : remainingShips.keySet()) {
                    int shipLength = remainingShips.get(ship);
                    int hShips = right + left + 2 - shipLength;
                    int vShips = up + down + 2 - shipLength;
                    if (hShips >= 1) {
                        value += Math.min(Math.min(Math.min(left + 1, right + 1), hShips), shipLength);
                    }
                    if (vShips >= 1) {
                        value += Math.min(Math.min(Math.min(up + 1, down + 1), vShips), shipLength);
                    }
                }
            }
            return value;
        }
    }

    class opponentInfo {

        int[][][] shipPattern;
        int[][] attackPattern;
        int[] shipTotal;

        opponentInfo() {
            attackPattern = new int[10][10];
            shipPattern = new int[5][10][10];
            shipTotal = new int[5];
            for (int model = 0; model < 5; model++) {
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 10; j++) {
                        shipPattern[model][i][j] = 1;
                    }
                }
                shipTotal[model] = 100;
            }
        }

        int shipProb(int shipModel, int x, int y) {
            return (100 * shipPattern[shipModel][x][y]) / shipTotal[shipModel] + 1;
        }
    }
}