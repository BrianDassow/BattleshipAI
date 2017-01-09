
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Random;

/**
 * Yet another captain. I originally wanted to attempt some sort of genetic
 * algorithm for this one, but unfortunately the deadline caught up on me. So
 * instead, I just made CaptainKeeta a little more object oriented, and made a
 * few changes to its attack pattern.
 *
 * @author Daev
 */
public class CaptainCrunch implements Captain {

    private Fleet myFleet;
    Board board;
    Coordinate lastAttack;
    private ArrayList<Opponent> oppArr;
    private Opponent currentOpponent;
    private EnemyFleet eFleet;
    private SpaceFiller filler;
    private PhaseScreen screen;
    private int[] weights;

    /**
     * Constructor for CaptainCrunch.
     */
    public CaptainCrunch() {
        board = new Board();
        eFleet = new EnemyFleet(board);
        screen = new PhaseScreen(board);
        oppArr = new ArrayList<Opponent>();
        filler = new SpaceFiller(eFleet, board);
    }

    /**
     * Initializes everything for a game.
     */
    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {

        int[] defaultWeights = {5, 3, 3, 2, 1}; //default weights for the PlacementComparator.  Favors hitting smaller ships.
        weights = defaultWeights;

        //Finds the correct opponent object or makes one.  I suspect there's an easier way to do this involving data structures, but
        //I was feeling lazy...
        currentOpponent = null;
        for (int i = 0; i < oppArr.size(); i++) {
            if (oppArr.get(i).match(opponent)) {
                currentOpponent = oppArr.get(i);
                i = oppArr.size();
            }
        }
        if (currentOpponent == null) {
            currentOpponent = new Opponent(opponent);
            oppArr.add(currentOpponent);
        }

        eFleet.init();
        filler.init();

        if (currentOpponent.getGames() > 21) {
            screen.setComparison(new PlacementComparator(currentOpponent.getPlacementRecord(), weights));
        }
        screen.init();
    }

    /**
     * Method to generate and return a Fleet object.
     */
    @Override
    public Fleet getFleet() {
        myFleet = new Fleet();
        PlacementPattern p = new PlacementPattern();
        p.placeFleet(myFleet);
        return myFleet;
    }

    /**
     * Method to make an attack.
     */
    @Override
    public Coordinate makeAttack() {
        lastAttack = null;
        if (eFleet.isActive()) {
            lastAttack = eFleet.nextMove();
        } else if (screen.isActive()) {
            lastAttack = screen.nextMove();
        }

        if (lastAttack == null) {
            lastAttack = filler.nextMove();
        }

        return lastAttack;
    }

    /**
     * Method that relays information on the result of an attack.
     */
    @Override
    public void resultOfAttack(int result) {
        board.setEntry(lastAttack.getX(), lastAttack.getY(), result);
        eFleet.resultOfAttack(result, lastAttack);
        filler.resultOfAttack(result, lastAttack);

        if (result / 10 == SUNK_MODIFIER / 10 && currentOpponent.getGames() > 21) {
            weights[result % 10] = 0;
            screen.setComparison(new PlacementComparator(currentOpponent.getPlacementRecord(), weights));
        }
    }

    /**
     * Method that processes opponent attacks.
     */
    @Override
    public void opponentAttack(Coordinate coord) {
    }

    /**
     * Method called at the end of the game.
     */
    @Override
    public void resultOfGame(int result) {
        currentOpponent.getPlacementRecord().addResult(board);
        board.clear();
    }

    /**
     * Basic Comparator for Coordinates. Sorts them by x value then y value.
     *
     * @author Daev
     */
    public class CoordComp implements Comparator<Coordinate> {

        /**
         * Comparison for sorting. Based on x coordinate, then y coordinate.
         */
        @Override
        public int compare(Coordinate arg0, Coordinate arg1) {
            if (arg0.getX() < arg1.getX()) {
                return -1;
            } else if (arg0.getX() == arg1.getX()) {
                if (arg0.getY() < arg1.getY()) {
                    return -1;
                } else if (arg0.getY() == arg1.getY()) {
                    return 0;
                } else {
                    return 1;
                }
            } else {
                return 1;
            }
        }
    }

    /**
     * Basic map for keeping track of things on the board.
     *
     * @author Daev
     */
    public class Board {

        private int[][] map; //entire map

        /**
         * Creates an empty Board
         */
        public Board() {
            map = new int[10][10];
            clear();
        }

        /**
         * Resets all entries
         */
        public void clear() {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    map[i][j] = 0;
                }
            }
        }

        /**
         * Sets an entry on the board.
         *
         * @param x The x-coordinate
         * @param y The y-coordinate
         * @param e The new entry.
         */
        public void setEntry(int x, int y, int e) {
            map[x][y] = e;
        }

        /**
         * Gets an entry from the board.
         *
         * @param x The x coordinate
         * @param y The y coordinate
         * @return The entry at (x, y)
         */
        public int getEntry(int x, int y) {
            return map[x][y];
        }

        /**
         * Method for figuring out the maximum distance away from a point you
         * can go before hitting a space you have already tried or the edge of
         * the board. This is useful in guessing where ships are once you've hit
         * them, as they will most likely be oriented toward the largest
         * distance.
         *
         * @param x The x coordinate
         * @param y The y coordinate
         * @param rightMod The horizontal modifier. 1 for right, 0 for neutral,
         * -1 for left.
         * @param upMod The vertical modifier. 1 for up, 0 for neutral, -1 for
         * down.
         * @param max The maximum length to check out to. Should be the length
         * of the ship you're looking for.
         * @return The distance.
         */
        public int maxDistance(int x, int y, int rightMod, int upMod, int max) {
            int i = 0;
            for (i = 1; i < max; i++) {
                if (x + i * rightMod > 9 || x + i * rightMod < 0 || y + i * upMod > 9 || y + i * upMod < 0) //out of bounds
                {
                    return i - 1;
                } else if (map[x + i * rightMod][y + i * upMod] != 0) //already tried there
                {
                    return i - 1;
                }
            }
            return i - 1; //should probably be unreachable on a 10 by 10 board.
        }
    }

    /**
     * Class for keeping track of opponent's ship placement patterns. Note that
     * this keeps track of each ship individually - this is primarily an attempt
     * to mess with Seth's strategy of hiding the patrol boat.
     */
    public class PlacementRecord {

        private int[][][] record;

        /**
         * Constructor for a PlacementRecord Object
         */
        public PlacementRecord() {
            record = new int[5][10][10];
        }

        /**
         * Adds the resulting board from a game to the placement record.
         *
         * @param b The board
         */
        public void addResult(Board b) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    int entry = b.getEntry(i, j);
                    if (entry != 0 && entry != MISS && entry != DEFEATED) {
                        record[entry / 10][i][j]++;
                    }
                }
            }
        }

        /**
         * Method for getting the total number of times a certain ship has been
         * placed in a spot.
         *
         * @param x The x-coordinate.
         * @param y The y-coordinate.
         * @param id The id of the ship.
         * @return The sum.
         */
        public int getSum(int x, int y, int id) {
            return record[id][x][y];
        }
    }

    /**
     * Generic attack pattern for a Captain.
     *
     * @author Daev
     */
    public interface AttackPattern {

        /**
         * Initializes the AttackPattern for a new game.
         */
        public void init();

        /**
         * Method for checking whether the AttackPattern has another move to
         * make.
         *
         * @return True if it has a move.
         */
        public boolean isActive();

        /**
         * Method for getting the next attack in this attack pattern.
         *
         * @return The next move.
         */
        public Coordinate nextMove();

        /**
         * Method for making any changes based on the last attack. Note that
         * this is meant to be called for all attack patterns, even when they
         * didn't make the attack.
         *
         * @param result The result of the attack.
         * @param c The Coordinate attacked.
         */
        public void resultOfAttack(int result, Coordinate c);
    }

    /**
     * Attack pattern for destroying a found ship of a particular type.
     *
     * @author Daev
     */
    public class EnemyShip implements AttackPattern {

        private int length;
        private int orientation;
        private boolean active;
        private boolean sunk;
        private final int UNKNOWN = -1;
        private ArrayList<Coordinate> coords;
        private Board board;
        private Random random;

        /**
         * A constructor for making an EnemyShip
         *
         * @param shipLength The length of the ship
         * @param b The board
         */
        public EnemyShip(int shipLength, Board b) {
            board = b;
            length = shipLength;
            random = new Random();
        }

        /**
         * Method to reset the ship's data.
         */
        @Override
        public void init() {
            coords = new ArrayList<Coordinate>();
            orientation = UNKNOWN;
            active = false;
            sunk = false;
        }

        /**
         * Getter for the length of the ship.
         *
         * @return The length.
         */
        public int getLength() {
            return length;
        }

        /**
         * Adds a new coordinate to the ship's location, updates data
         * appropriately. To be called every time the ship is hit.
         *
         * @param c The coordinate
         */
        @Override
        public void resultOfAttack(int result, Coordinate c) {
            if (result / 10 == HIT_MODIFIER / 10) {
                coords.add(c);
                if (coords.size() == 1) {
                    active = true;
                } else if (coords.size() == 2 && length != 2) {
                    if (coords.get(0).getY() == coords.get(1).getY()) {
                        orientation = HORIZONTAL;
                    } else {
                        orientation = VERTICAL;
                    }
                }
                Collections.sort(coords, new CoordComp());
            } else if (result / 10 == SUNK_MODIFIER / 10) {
                active = false;
                sunk = true;
            }
        }

        /**
         * Method to see if the ship has been hit and not sunk.
         */
        @Override
        public boolean isActive() {
            return active;
        }

        /**
         * Method to see if the ship has been sunk.
         *
         * @return True if sunk.
         */
        public boolean isSunk() {
            return sunk;
        }

        /**
         * Method for getting next guess at ship position. DO NOT call if
         * isActive() is false.
         *
         * @param board The board
         * @return The next coordinate to guess
         */
        @Override
        public Coordinate nextMove() {
            if (orientation == UNKNOWN) {
                return guessOrientation();
            } else {
                Coordinate r = findHole();

                if (r == null) {
                    if (orientation == HORIZONTAL) {
                        r = guessHorizontal();
                    } else {
                        r = guessVertical();
                    }

                    if (r == null) {
                        if (random.nextBoolean()) {
                            return new Coordinate(coords.get(coords.size() - 1).getX() + (1 - orientation),
                                    coords.get(coords.size() - 1).getY() + orientation);
                        } else {
                            return new Coordinate(coords.get(0).getX() - (1 - orientation),
                                    coords.get(0).getY() - orientation);
                        }
                    }
                }
                return r;
            }
        }

        /**
         * Method for finding next coordinate without any known orientation.
         *
         * @param board The board
         * @return The next guess.
         */
        private Coordinate guessOrientation() {
            int distLeft = board.maxDistance(coords.get(0).getX(), coords.get(0).getY(), -1, 0, length);
            int distRight = board.maxDistance(coords.get(0).getX(), coords.get(0).getY(), 1, 0, length);
            int distDown = board.maxDistance(coords.get(0).getX(), coords.get(0).getY(), 0, -1, length);
            int distUp = board.maxDistance(coords.get(0).getX(), coords.get(0).getY(), 0, 1, length);

            int numHoriz = distLeft + distRight - length + 1;
            int numVert = distUp + distDown - length + 1;

            if (numHoriz > numVert) {
                if (distLeft > distRight) {
                    return new Coordinate(coords.get(0).getX() - 1, coords.get(0).getY());
                } else {
                    return new Coordinate(coords.get(0).getX() + 1, coords.get(0).getY());
                }
            } else {
                if (distDown > distUp) {
                    return new Coordinate(coords.get(0).getX(), coords.get(0).getY() - 1);
                } else {
                    return new Coordinate(coords.get(0).getX(), coords.get(0).getY() + 1);
                }
            }
        }

        /**
         * Looks for a hole in known parts of the ship.
         *
         * @return A space to guess where there is a hole. Null if there is no
         * hole.
         */
        private Coordinate findHole() {
            for (int i = 0; i < coords.size() - 1; i++) {
                if (Math.abs((coords.get(i).getX() + coords.get(i).getY())
                        - (coords.get(i + 1).getX() + coords.get(i + 1).getY())) > 1) {
                    return new Coordinate((coords.get(i).getX() + coords.get(i + 1).getX()) / 2,
                            (coords.get(i).getY() + coords.get(i + 1).getY()) / 2);
                }
            }
            return null;
        }

        /**
         * Method for finding next coordinate if ship is known to be horizontal
         *
         * @param board The board
         * @return The next guess. Null if there is an even chance of either
         * way.
         */
        private Coordinate guessHorizontal() {
            int distanceFound = coords.get(coords.size() - 1).getX() - coords.get(0).getX();
            int distLeft = board.maxDistance(coords.get(0).getX(), coords.get(0).getY(), -1, 0, length - distanceFound);
            int distRight = board.maxDistance(coords.get(coords.size() - 1).getX(), coords.get(coords.size() - 1).getY(), 1, 0, length - distanceFound);

            if (distRight > distLeft) {
                return new Coordinate(coords.get(coords.size() - 1).getX() + 1, coords.get(coords.size() - 1).getY());
            } else if (distLeft > distRight) {
                return new Coordinate(coords.get(0).getX() - 1, coords.get(0).getY());
            } else {
                return null;
            }
        }

        /**
         * Method for finding next coordinate if ship is known to be vertical
         *
         * @param board The board
         * @return The next guess. Null if there is an even chance of either
         * way.
         */
        private Coordinate guessVertical() {
            int distanceFound = coords.get(coords.size() - 1).getY() - coords.get(0).getY();
            int distDown = board.maxDistance(coords.get(0).getX(), coords.get(0).getY(), 0, -1, length - distanceFound);
            int distUp = board.maxDistance(coords.get(coords.size() - 1).getX(), coords.get(coords.size() - 1).getY(), 0, 1, length - distanceFound);

            if (distUp > distDown) {
                return new Coordinate(coords.get(coords.size() - 1).getX(), coords.get(coords.size() - 1).getY() + 1);
            } else if (distDown > distUp) {
                return new Coordinate(coords.get(0).getX(), coords.get(0).getY() - 1);
            } else {
                return null;
            }
        }
    }

    /**
     * Wrapper class that handles all 5 EnemyShips
     *
     * @author Daev
     */
    public class EnemyFleet implements AttackPattern {

        EnemyShip[] ships;

        /**
         * Constructor for the EnemyFleet
         *
         * @param board
         */
        EnemyFleet(Board board) {
            ships = new EnemyShip[5];
            ships[PATROL_BOAT] = new EnemyShip(PATROL_BOAT_LENGTH, board);
            ships[DESTROYER] = new EnemyShip(DESTROYER_LENGTH, board);
            ships[SUBMARINE] = new EnemyShip(SUBMARINE_LENGTH, board);
            ships[BATTLESHIP] = new EnemyShip(BATTLESHIP_LENGTH, board);
            ships[AIRCRAFT_CARRIER] = new EnemyShip(AIRCRAFT_CARRIER_LENGTH, board);
        }

        /**
         * Method to get the next move from the ship.
         */
        @Override
        public Coordinate nextMove() {
            for (int i = 0; i < ships.length; i++) {
                if (ships[i].isActive()) {
                    Coordinate r = ships[i].nextMove();
                    if (board.getEntry(r.getX(), r.getY()) == 0) {
                        return r;
                    }
                }
            }
            //Should be unreachable if you check if it's active first
            return null;
        }

        /**
         * Method to reset all the ships.
         */
        @Override
        public void init() {
            for (int i = 0; i < 5; i++) {
                ships[i].init();
            }
        }

        /**
         * Method to see if there is an active ship. Note that if this is true,
         * this AttackPattern should take priority.
         */
        @Override
        public boolean isActive() {
            for (int i = 0; i < ships.length; i++) {
                if (ships[i].isActive()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Method for storing the result of an attack. Updates the appropriate
         * ship.
         */
        @Override
        public void resultOfAttack(int result, Coordinate c) {
            if (result / 10 == HIT_MODIFIER / 10 || result / 10 == SUNK_MODIFIER / 10) {
                ships[result % 10].resultOfAttack(result, c);
            }
        }

        /**
         * Method that gives the length of the largest remaining ship.
         *
         * @return The length of the largest ship that has not been sunk.
         */
        public int largestRemaining() {
            int r = 2;
            for (int i = 0; i < ships.length; i++) {
                if (!ships[i].isSunk() && ships[i].getLength() > r) {
                    r = ships[i].getLength();
                }
            }
            return r;
        }
    }

    /**
     * Comparator based on the max distances on the board. Used mainly by
     * SpaceFiller.
     *
     * @author Daev
     */
    public class DistComp implements Comparator<Coordinate> {

        private Board board;
        private int max;

        /**
         * Constructor for the comparator.
         *
         * @param b The board.
         * @param max The fartherst distance to count out to. Usually the length
         * of the largest ship remaining.
         */
        public DistComp(Board b, int max) {
            board = b;
            this.max = max;
        }

        /**
         * Method to compare two coordinates.
         */
        @Override
        public int compare(Coordinate o1, Coordinate o2) {
            int[][] mods = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
            int o1Score = 0;
            int o2Score = 0;
            for (int i = 0; i < 4; i++) {
                o1Score += board.maxDistance(o1.getX(), o1.getY(), mods[i][0], mods[i][1], max);
                o2Score += board.maxDistance(o2.getX(), o2.getY(), mods[i][0], mods[i][1], max);
            }
            return o2Score - o1Score;
        }
    }

    /**
     * Attack pattern that attacks spaces with the largest number of ways the
     * largest remaining ship can fit. Good for tracking down missing ships
     * after initial attack patterns have been exhausted.
     *
     * @author Daev
     */
    public class SpaceFiller implements AttackPattern {

        private LinkedList<Coordinate> spaces;
        private EnemyFleet fleet;
        private Board board;

        /**
         * Constructor for the SpaceFiller.
         *
         * @param f The EnemyFleet. This is only used as an easy way to get the
         * length of the largest remaining ship.
         * @param b The Board.
         */
        public SpaceFiller(EnemyFleet f, Board b) {
            fleet = f;
            board = b;
        }

        /**
         * Method to initialize the SpaceFiller.
         */
        @Override
        public void init() {
            spaces = new LinkedList<Coordinate>();

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    spaces.add(new Coordinate(i, j));
                }
            }
        }

        /**
         * This method will always return true, as this AttackPattern will keep
         * going until there are no more spaces to attack (should never happen).
         */
        @Override
        public boolean isActive() {
            return true;
        }

        /**
         * Method for getting the next move.
         */
        @Override
        public Coordinate nextMove() {
            DistComp d = new DistComp(board, fleet.largestRemaining());
            Collections.sort(spaces, d);
            Coordinate c;
            do {
                c = spaces.pop();
            } while (board.getEntry(c.getX(), c.getY()) != 0);
            return c;
        }

        /**
         * This method doesn't actually do anything...
         */
        @Override
        public void resultOfAttack(int result, Coordinate c) {
        }
    }

    /**
     * Attack pattern that employs diagonal lines that are shifted every game to
     * make adaptation more difficult. Though simple, this attack pattern was
     * used by CaptainKeeta last semester, and seemed to work quite well. Here
     * I've changed it so that the comparator for sorting can be changed part of
     * the way through.
     *
     * @author Daev
     */
    public class PhaseScreen implements AttackPattern {

        private LinkedList<Coordinate> list;
        private int[][] map;
        private int phaseNumber;
        private Board board;
        private final int PHASE_SIZE = 5; //"Density" of diagonals
        private Comparator<Coordinate> Comparison;

        /**
         * Method for setting a special comparison if you want a particular
         * ordering for the points. If one is not given, it will order them
         * randomly. Note that this is reset to null each time init() is called.
         *
         * @param comparison A comparator for sorting coordinates
         */
        public void setComparison(Comparator<Coordinate> comparison) {
            Comparison = comparison;
            reSort();
        }

        /**
         * Constructor for the PhaseScreen.
         *
         * @param b The Board.
         */
        public PhaseScreen(Board b) {
            board = b;
            map = new int[10][10];
            for (int i = 0; i < 10; i++) {
                map[0][i] = i % PHASE_SIZE;
                for (int j = 1; j < 10; j++) {
                    map[j][i] = (map[j - 1][i] + PHASE_SIZE - 1) % PHASE_SIZE;
                }
            }
            phaseNumber = 0;
        }

        /**
         * Initializes the PhaseScreen.
         */
        @Override
        public void init() {
            Comparison = null;
            list = new LinkedList<Coordinate>();
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (map[i][j] == phaseNumber) {
                        list.add(new Coordinate(i, j));
                    }
                }
            }
            phaseNumber = (phaseNumber + 1) % PHASE_SIZE;
            reSort();
        }

        /**
         * Method to check if there are still Coordinates remaining.
         */
        @Override
        public boolean isActive() {
            return !list.isEmpty();
        }

        /**
         * Method for getting the next move.
         */
        @Override
        public Coordinate nextMove() {
            Coordinate r;
            do {
                if (list.isEmpty()) {
                    return null;
                }

                r = list.pop();
            } while (board.getEntry(r.getX(), r.getY()) != 0);
            return r;
        }

        /**
         * This method does nothing.
         */
        @Override
        public void resultOfAttack(int result, Coordinate c) {
        }

        /**
         * Resorts the coordinate.
         */
        private void reSort() {
            Collections.shuffle(list);
            //Note that this is shuffled regardless of whether there is a comparison.
            //This is because Collections.sort is a stable sort, so equal elements do not
            //Have their relative position changed at all.  Thus, shuffling randomizes elements of
            //equal order under the comparison.

            if (Comparison != null) {
                Collections.sort(list, Comparison);
            }
        }
    }

    /**
     * Comparator for sorting coordinates based on likelihood of an enemy ship
     * being there, based on the PlacementRecord.
     *
     * @author Daev
     */
    public class PlacementComparator implements Comparator<Coordinate> {

        private PlacementRecord record;
        private int[] weights;

        /**
         * Constructor got the placement comparator.
         *
         * @param p The placement record
         * @param weights The weights for each type fo ship.
         */
        public PlacementComparator(PlacementRecord p, int[] weights) {
            record = p;
            this.weights = weights;
        }

        /**
         * Method to compare coordinates.
         */
        @Override
        public int compare(Coordinate o1, Coordinate o2) {
            long o1Score = 0; //These likely won't get big enough to be longs unless someone has some really funky ship
            long o2Score = 0; //placement over millions of games (or there are some crazy weights) but I wanted to be safe.
            for (int i = 0; i < 5; i++) {
                o1Score += weights[i] * (long) record.getSum(o1.getX(), o1.getY(), i);
                o2Score += weights[i] * (long) record.getSum(o2.getX(), o2.getY(), i);
            }
            long result = o2Score - o1Score;
            if (result > 0) {
                return 1;
            } else if (result < 0) {
                return -1;
            }
            return 0;
        }
    }

    /**
     * Class that holds data on a single opponent
     *
     * @author Daev
     */
    public class Opponent {

        private PlacementRecord placement;
        private String name;
        private int games;

        /**
         * Contructor for the opponent.
         *
         * @param name The name of the opponent.
         */
        public Opponent(String name) {
            this.name = name;
            games = 1;
            placement = new PlacementRecord();
        }

        /**
         * Method for getting the placement record.
         *
         * @return The placement record.
         */
        public PlacementRecord getPlacementRecord() {
            return placement;
        }

        /**
         * Method for matching to the opponent's name.
         *
         * @param name The name
         * @return True if the same opponent.
         */
        public boolean match(String name) {
            if (this.name.equals(name)) {
                games++;
                return true;
            }
            return false;
        }

        /**
         * A method for getting the number of games started with this opponent
         * (this includes the current one). Technically, this counts the number
         * of times the opponent's name has been matched with the given opponent
         * at the start - this is why this matching is only done once per game
         * in this AI.
         *
         * @return The number of games
         */
        public int getGames() {
            return games;
        }
    }

    /**
     * Class for handling placement of ships. Ripped this one right out of
     * CaptainKeeta, with minimal changes. I planned on replacing this if I had
     * time...
     *
     * @author Daev
     */
    public class PlacementPattern {

        private Random random;
        private int[][] board;
        private int[][] ships;

        /**
         * Initializes placement with a neutral pattern. Odds are weighted to
         * compensate for triangular distribution.
         */
        public PlacementPattern() {
            random = new Random();
            board = new int[10][10];
            ships = new int[5][3];

            placeNeutralShip(AIRCRAFT_CARRIER, AIRCRAFT_CARRIER_LENGTH);
            placeNeutralShip(BATTLESHIP, BATTLESHIP_LENGTH);
            placeNeutralShip(SUBMARINE, SUBMARINE_LENGTH);
            placeNeutralShip(DESTROYER, DESTROYER_LENGTH);
            placeNeutralShip(PATROL_BOAT, PATROL_BOAT_LENGTH);
        }

        /**
         * Method for picking a weighted random, given weights and the total of
         * the weights.
         *
         * @param n The weights.
         * @param total The total of all the weights.
         * @return A random number between 0 and the length of the array - 1.
         */
        private int pickWeightedRandom(int[] n, int total) {
            int randC = random.nextInt(total) + 1;
            for (int i = 0; i < n.length; i++) {
                randC -= n[i];
                if (randC <= 0) {
                    return i;
                }
            }
            return 0;
        }

        /**
         * Method for placing ships evenly
         */
        private void placeNeutralShip(int shipId, int shipLength) {
            int x, y;
            int orient;
            do {
                orient = random.nextInt(2);

                if (orient == HORIZONTAL) {
                    x = oddCoord(shipLength);
                    y = random.nextInt(10);
                } else {
                    y = oddCoord(shipLength);
                    x = random.nextInt(10);
                }
            } while (!checkValid(x, y, orient, shipLength));
            fillMap(x, y, orient, shipId, shipLength);
            ships[shipId][0] = x;
            ships[shipId][1] = y;
            ships[shipId][2] = orient;
        }

        /**
         * Weighted random coordinate
         *
         * @param shipLength Length of the ship
         * @return The coordinate
         */
        private int oddCoord(int shipLength) {
            if (shipLength == 5) {
                int[] o = {4, 2, 1, 1, 2, 4};
                return pickWeightedRandom(o, 14);
            } else if (shipLength == 4) {
                int[] o = {5, 3, 2, 1, 2, 3, 5};
                return pickWeightedRandom(o, 21);
            } else if (shipLength == 3) {
                int[] o = {4, 2, 1, 1, 1, 1, 2, 4};
                return pickWeightedRandom(o, 16);
            } else {
                int[] o = {3, 1, 1, 1, 1, 1, 1, 1, 3};
                return pickWeightedRandom(o, 13);
            }
        }

        /**
         * Checks validity of ship placement
         *
         * @param x The x-coordinate
         * @param y The y-coordinate
         * @param orient The orientation
         * @param length The length
         * @return True if a valid placement.
         */
        private boolean checkValid(int x, int y, int orient, int length) {
            if (orient == HORIZONTAL) {
                for (int i = 0; i < length; i++) {
                    if (board[x + i][y] != 0) {
                        return false;
                    }
                }
                return true;
            } else {
                for (int i = 0; i < length; i++) {
                    if (board[x][y + i] != 0) {
                        return false;
                    }
                }
                return true;
            }
        }

        /**
         * Fills in the map for further error checking. I'm guessing this is for
         * once you've decided to place a ship...? I don't remember.
         *
         * @param x The x-coordinate.
         * @param y The y-coordinate.
         * @param orient The orientation.
         * @param length The length of the ship.
         */
        private void fillMap(int x, int y, int orient, int shipId, int length) {
            if (orient == HORIZONTAL) {
                for (int i = 0; i < length; i++) {
                    board[x + i][y] = shipId + 1; //Have to add one to make Patrol Boat nonzero
                }
            } else {
                for (int i = 0; i < length; i++) {
                    board[x][y + i] = shipId + 1;
                }
            }
        }

        /**
         * Method to place ships on the actual Fleet.
         *
         * @param f The Fleet.
         */
        public void placeFleet(Fleet f) {
            for (int i = 0; i < 5; i++) {
                f.placeShip(ships[i][0], ships[i][1], ships[i][2], i);
            }
        }
    }
}
