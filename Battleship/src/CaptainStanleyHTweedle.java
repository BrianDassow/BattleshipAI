
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeSet;

public class CaptainStanleyHTweedle implements Captain, Constants {

    protected Random generator;
    protected Fleet myFleet;
    final int[] shipLengths = new int[]{2, 3, 3, 4, 5};
    final int fleetSize = 10000;
    ArrayList<square> board;
    ArrayList<ArrayList<localShip>> shipModels;
    ArrayList<square> queue;
    square lastAttack;
    int numOpponentAttacks, numMyAttacks;
    int smallestShipLength;
    fleetPlacement placement;
    TreeSet<fleetPlacement> possibleFleets;
    int[] numShips;
    boolean[] hitShips;
    boolean readQueue;
    String currentOpponent;

    CaptainStanleyHTweedle() {
        generator = new Random();
        lastAttack = new square(0, 0);
        currentOpponent = new String();
        queue = new ArrayList<>();
    }

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        myFleet = new Fleet();
        numOpponentAttacks = 0;
        numMyAttacks = 0;
        numShips = new int[5];
        hitShips = new boolean[]{false, false, false, false, false};
        readQueue = true;

        if (!currentOpponent.equals(opponent)) {
            currentOpponent = opponent;
            board = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                board.add(new square(i % 10, i / 10));
            }
            shipModels = new ArrayList<>();
            for (int model = 0; model < 5; model++) {
                shipModels.add(new ArrayList<localShip>());
                for (int i = 0; i <= 10 - shipLengths[model]; i++) {
                    for (int j = 0; j < 10; j++) {
                        shipModels.get(model).add(new localShip(i, j, model, HORIZONTAL));
                        shipModels.get(model).add(new localShip(j, i, model, VERTICAL));
                    }
                }
            }
            generateFleets();
        }

        for (int i = 0; i < 100; i++) {
            board.get(i).reset();
        }

        for (int model = 0; model < 5; model++) {
            for (localShip ls : shipModels.get(model)) {
                ls.init();
            }
        }

        smallestShipLength = 4;
        int offset = generator.nextInt(smallestShipLength);
        int orientation = 2 * generator.nextInt(2) - 1;
        queue.clear();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if ((i + orientation * j - offset) % smallestShipLength == 0) {
                    queue.add(board.get(i + 10 * j));
                }
            }
        }

        placement = possibleFleets.pollLast();
        for (int ship = 0; ship < 5; ship++) {
            myFleet.placeShip(placement.thisPlacement[ship] % 10, (placement.thisPlacement[ship] / 10) % 10, (placement.thisPlacement[ship] / 100), ship);
        }
    }

    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    @Override
    public Coordinate makeAttack() {
        double min = 90000000000000.0d;
        double squareValue;
        if (readQueue) {
            if (numMyAttacks < 7) {
                lastAttack = queue.get(generator.nextInt(queue.size()));
            } else {
                for (square s : queue) {
                    squareValue = s.getProb();
                    if (squareValue < min) {
                        lastAttack = s;
                        min = squareValue;
                    }
                }
            }
            queue.remove(lastAttack);
        } else {
            for (square s : board) {
                squareValue = s.getProb();
                if (squareValue < min) {
                    lastAttack = s;
                    min = squareValue;
                }
            }
        }
        numMyAttacks++;
        return new Coordinate(lastAttack.x, lastAttack.y);
    }

    @Override
    public void resultOfAttack(int result) {
        if (result == MISS) {
            board.get(lastAttack.x + 10 * lastAttack.y).missAttack();
        } else if (result / 10 == 1) {
            board.get(lastAttack.x + 10 * lastAttack.y).hitAttack(result % 10);
            hitShips[result % 10] = true;
            readQueue = false;
        } else if (result / 10 == 2) {
            board.get(lastAttack.x + 10 * lastAttack.y).sunk(result % 10);
            hitShips[result % 10] = false;
            readQueue = !(hitShips[0] || hitShips[1] || hitShips[2] || hitShips[3] || hitShips[4]);
            if (readQueue) {
                makeQueue();
            }
        }
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        numOpponentAttacks++;
    }

    @Override
    public void resultOfGame(int result) {
        if (result == LOST) {
            placement.score = numOpponentAttacks;
            double[] endingShips = new double[5];
            for (int model = 0; model < 5; model++) {
                endingShips[model] = numShips[model] > 0 ? 1.0 / (double) numShips[model] : 0.0;
            }
            for (square s : board) {
                for (int model = 0; model < 5; model++) {
                    for (localShip ls : s.ships.get(model)) {
                        ls.occurences += endingShips[model];
                        ls.kill(s);
                    }
                }
            }
        } else if (numOpponentAttacks > placement.score) {
            placement.score = numOpponentAttacks;
        }
        possibleFleets.add(placement);
    }

    void generateFleets() {
        Fleet tempFleet;
        possibleFleets = new TreeSet<>(new Comparator<fleetPlacement>() {
            @Override
            public int compare(fleetPlacement fp1, fleetPlacement fp2) {
                if (fp1.score != fp2.score) {
                    return fp1.score - fp2.score;
                } else {
                    return fp1.identifier - fp2.identifier;
                }
            }
        });
        int x, y, orientation;
        for (int i = 0; i < fleetSize; i++) {
            int[] temp = new int[5];
            tempFleet = new Fleet();
            for (int ship = 0; ship < 5; ship++) {
                do {
                    x = generator.nextInt(10);
                    y = generator.nextInt(10);
                    orientation = generator.nextInt(2);
                } while (!tempFleet.placeShip(x, y, orientation, ship));
                temp[ship] = x + 10 * y + 100 * orientation;
            }
            possibleFleets.add(new fleetPlacement(temp, i));
        }
    }

    public void makeQueue() {
        ArrayList<ArrayList<square>> components = new ArrayList<>();
        ArrayList<square> unattacked = new ArrayList<>();
        queue.clear();
        smallestShipLength = AIRCRAFT_CARRIER_LENGTH;
        int smallestShipModel = AIRCRAFT_CARRIER;
        for (int ship = 0; ship < 5; ship++) {
            if (numShips[ship] > 0 && shipLengths[ship] < smallestShipLength) {
                smallestShipLength = shipLengths[ship];
                smallestShipModel = ship;
            }
        }
        for (int i = 0; i < 100; i++) {
            if (!board.get(i).ships.get(smallestShipModel).isEmpty()) {
                unattacked.add(board.get(i));
            }
        }
        while (!unattacked.isEmpty()) {
            ArrayList<square> component = new ArrayList<>();
            floodFill(unattacked, component, unattacked.get(0));
            for (square s : component) {
                unattacked.remove(s);
            }
            components.add(component);
        }
        for (ArrayList<square> c : components) {
            int min = 100;
            ArrayList<int[]> bestDiagonals = new ArrayList<>();
            int[] pOffset = new int[smallestShipLength];
            int[] nOffset = new int[smallestShipLength];
            int[] index;
            for (square s : c) {
                pOffset[toIndex(s.x + s.y)]++;
                nOffset[toIndex(s.x - s.y)]++;
            }
            for (int i = 0; i < smallestShipLength; i++) {
                if (pOffset[i] <= min) {
                    if (pOffset[i] < min) {
                        bestDiagonals = new ArrayList<>();
                    }
                    bestDiagonals.add(new int[]{i, 1});
                    min = pOffset[i];
                }
                if (nOffset[i] <= min) {
                    if (nOffset[i] < min) {
                        bestDiagonals = new ArrayList<>();
                    }
                    bestDiagonals.add(new int[]{i, -1});
                    min = nOffset[i];
                }
            }
            index = bestDiagonals.get(generator.nextInt(bestDiagonals.size()));
            for (square s : c) {
                if ((s.x + index[1] * s.y - index[0]) % smallestShipLength == 0) {
                    queue.add(s);
                }
            }
        }
    }

    private void floodFill(ArrayList<square> unattacked, ArrayList<square> component, square node) {
        component.add(node);
        if (node.x > 0 && unattacked.contains(board.get(node.left)) && !component.contains(board.get(node.left))) {
            floodFill(unattacked, component, board.get(node.left));
        }
        if (node.x < 9 && unattacked.contains(board.get(node.right)) && !component.contains(board.get(node.right))) {
            floodFill(unattacked, component, board.get(node.right));
        }
        if (node.y > 0 && unattacked.contains(board.get(node.down)) && !component.contains(board.get(node.down))) {
            floodFill(unattacked, component, board.get(node.down));
        }
        if (node.y < 9 && unattacked.contains(board.get(node.up)) && !component.contains(board.get(node.up))) {
            floodFill(unattacked, component, board.get(node.up));
        }
    }

    int toIndex(int index) {
        return (index % smallestShipLength >= 0) ? index % smallestShipLength : (index % smallestShipLength) + smallestShipLength;
    }

    class fleetPlacement {

        int[] thisPlacement;
        int score;
        int identifier;

        fleetPlacement(int[] thisPlacement, int identifier) {
            this.thisPlacement = thisPlacement;
            score = 500;
            this.identifier = identifier;
        }
    }

    class square {

        boolean isAttacked;
        int x, y;
        int left, right, up, down;
        ArrayList<ArrayList<localShip>> ships;
        double[] occurenceTotals;

        square(int x, int y) {
            isAttacked = false;
            this.x = x;
            this.y = y;
            left = x - 1 + 10 * y;
            right = x + 1 + 10 * y;
            up = x + 10 * (y + 1);
            down = x + 10 * (y - 1);
            occurenceTotals = new double[5];
            ships = new ArrayList<>();
            for (int model = 0; model < 5; model++) {
                ships.add(new ArrayList<localShip>());
            }
        }

        void reset() {
            isAttacked = false;
            occurenceTotals = new double[5];
            for (int model = 0; model < 5; model++) {
                ships.get(model).clear();
            }
        }

        void addShip(localShip ls) {
            ships.get(ls.model).add(ls);
            occurenceTotals[ls.model] += ls.occurences;
        }

        double getProb() {
            if (isAttacked == false) {
                double prob = 1;
                for (int model = 0; model < 5; model++) {
                    if (numShips[model] > 0 && occurenceTotals[model] > 0.0) {
                        prob *= (double) (numShips[model] - ships.get(model).size()) / occurenceTotals[model];
                    }
                }
                return prob;
            } else {
                return 90000000000000.0d;
            }
        }

        void missAttack() {
            for (int model = 0; model < 5; model++) {
                for (localShip ls : ships.get(model)) {
                    ls.kill(this);
                }
                ships.get(model).clear();
            }
            isAttacked = true;
        }

        void hitAttack(int hitModel) {
            for (int model = 0; model < 5; model++) {
                if (model != hitModel) {
                    for (localShip ls : ships.get(model)) {
                        ls.kill(this);
                    }
                    ships.get(model).clear();
                }
            }
            for (square s : board) {
                if (!s.isAttacked && !s.equals(this)) {
                    s.ships.get(hitModel).clear();
                    s.occurenceTotals[hitModel] = 0;
                }
            }
            numShips[hitModel] = 0;
            for (localShip ls : ships.get(hitModel)) {
                numShips[hitModel]++;
                for (square s : ls.onSquares) {
                    if (!s.isAttacked && !s.equals(this)) {
                        board.get(s.x + s.y * 10).addShip(ls);
                    }
                }
            }
            isAttacked = true;
        }

        void sunk(int sunkModel) {
            ships.get(sunkModel).get(0).occurences++;
            missAttack();
            for (square s : board) {
                if (!s.isAttacked && !s.equals(this)) {
                    s.ships.get(sunkModel).clear();
                }
            }
            numShips[sunkModel] = 0;
        }
    }

    class localShip {

        int x, y, model, length, orientation;
        ArrayList<square> onSquares;
        double occurences;

        localShip(int x, int y, int model, int orientation) {
            this.x = x;
            this.y = y;
            this.model = model;
            this.orientation = orientation;
            length = shipLengths[model];
            occurences = 25000.0d / (double) (220 - 20 * length);
        }

        void init() {
            onSquares = new ArrayList<>();
            numShips[model]++;
            if (orientation == HORIZONTAL) {
                for (int i = x; i < x + length; i++) {
                    onSquares.add(board.get(i + 10 * y));
                    board.get(i + 10 * y).addShip(this);
                }
            } else {
                for (int i = y; i < y + length; i++) {
                    onSquares.add(board.get(x + 10 * i));
                    board.get(x + 10 * i).addShip(this);
                }
            }
        }

        void kill(square callingSquare) {
            numShips[model]--;
            for (square s : onSquares) {
                if (!s.equals(callingSquare)) {
                    s.ships.get(model).remove(this);
                }
                s.occurenceTotals[model] -= occurences;
            }
        }
    }
}