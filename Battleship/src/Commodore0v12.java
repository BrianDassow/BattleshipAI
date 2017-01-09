import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Commodore0v12 implements Captain {

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
    List<Integer> shipLengthsRemaining;
    // Targeting
    Integer[] intInt;
    HashMap<Integer, S> targetMap;
    S currentTarget, newTarget;

    public Commodore0v12() {
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
            List<S> newShipList = new ArrayList<>(1);
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
        for (Ship t : f.getFleet()) {
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
        //int x = 0, y = 0;
        Coordinate c;
        int failsafe; // failsafe2;
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
        int x, y; //, type;
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
                //type = currentTarget.getModel();
                int cx = currentTarget.getLocation().getX();
                int cy = currentTarget.getLocation().getY();
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
                                && currentTarget.getDirection() != -1
                                && (((currentTarget.getDirection() != 0) && (dy == 0))
                                || ((currentTarget.getDirection() == 0) && (dx == 0))));
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
                        int dx = myLastShot.getX() - currentTarget.getLocation().getX();
                        //int dy = myLastShot.getY() - currentTarget.getLocation().getY();
                        if (dx != 0) // direction of ship is horiz
                        {
                            currentTarget.setDirection(0);
                        } else {
                            currentTarget.setDirection(1);
                        }
                        if (DEBUG >= VVV) {
                            System.out.format("Commodore0: (state %d) found enemy ship DIRECTION %d\n", myState, currentTarget.getDirection());
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

    public void setDirection(int direction) {
    	this.direction = direction;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof S)) {
            return false;
        }
        S s = (S) o;
        return ((s.type() == this.getModel())
                && (s.location().getX() == this.getLocation().getX())
                && (s.location().getY() == this.getLocation().getY())
                && (s.direction() == this.getDirection()));
    }

    @Override
    public int hashCode() {
        return (101 * this.getModel() + 67 * this.getDirection()
                + 43 * this.getLocation().getX() + 19 * this.getLocation().getY());
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
        return this.getLocation();
    }

    public int direction() {
        return this.getDirection();
    }

    public int length() {
        return this.getLength();
    }

    public int type() {
        return this.getModel();
    }
}
// simple class for targets
// class BSTarget {
//     public BSTarget(int x, int y, int type) {
// 	this.x = x;
// 	this.y = y;
// 	this.hits = 1;
// 	this.dir = -1; // unknown
// 	this.x2 = -1;
// 	this.y2 = -1;
// 	this.type = type;
// 	if (type >= 0 && type <= 4) {
// 	    this.length = this.shipLengths[type];
// 	}
// 	else {
// 	    this.length = -1; // error?
// 	}
//     }
//     public int x, y; // location of initial hit
//     public int x2, y2; // location of subsequent hit
//     public int hits;
//     public int dir; // 0 = horiz, 1 = vert
//     public int type;
//     public int[] shipLengths = { 2, 3, 3, 4, 5};
//     public int length;
//     public int orientation;
// }