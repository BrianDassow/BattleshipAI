
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Captain Nemo Entry for Battleship AI competition, spring 2013
 *
 * @author Steven Cain
 */
public class CaptainNemo implements Captain {

    private Grid parityGrid;
    private HashMap<Integer, Integer> shipLengths;
    private Random generator;
    private Fleet myFleet;
    private Map<String, Opponent> Opponents;
    private String currentOpponentName;
    private int turn;
    private ArrayList<Integer> opponentsRemainingShips;
    private Coordinate lastAttack;
    private Grid misses;
    private Grid alreadyAttacked;
    private HashMap<Integer, ArrayList<Coordinate>> opponentsDamagedShips;

    private static class Segment {

        public int start;
        public int length = 0;

        Segment(int start) {
            this.start = start;
        }
    }

    private static class Opponent {

        public int gamesPlayed = 0;
        public int gamesWon = 0;
        public boolean useAltStrategy = false;
        public ArrayList<Coordinate> altStratCoordPriorities;
        public Grid fleetLayouts;
        public Grid aimLayout;
        public Grid myPersona;
        public Grid myFleetPlacement;
        public int[] survivors;
        public String OpponentName;

        Opponent(String name) {
            OpponentName = name;
            altStratCoordPriorities = new ArrayList<Coordinate>();
            fleetLayouts = new Grid();
            aimLayout = new Grid();
            myPersona = new Grid();
            myFleetPlacement = new Grid();
            survivors = new int[]{0, 0, 0, 0, 0};
        }
    }

    public static class Grid {

        private int[][] values;

        Grid() {
            values = new int[10][10];
            this.zeroGrid();
        }

        Grid(int[][] vals) {
            this.values = vals;
        }

        public int getValue(Coordinate coord) {
            return values[coord.getX()][coord.getY()];
        }

        public int getValue(int x, int y) {
            return values[x][y];
        }

        public void setValue(Coordinate coord, int value) {
            this.values[coord.getX()][coord.getY()] = value;
        }

        public void addValue(Coordinate coord, int value) {
            this.values[coord.getX()][coord.getY()] += value;
        }

        public void increment(Coordinate coord) {
            this.values[coord.getX()][coord.getY()]++;
        }

        public int[][] getArray() {
            return values;
        }

        public void zeroGrid() {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    values[i][j] = 0;
                }
            }
        }

        public void addGrid(Grid a) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    values[i][j] += a.getValue(new Coordinate(i, j));
                }
            }
        }

        public void multiplyGrid(Grid a) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    values[i][j] += a.getValue(new Coordinate(i, j));
                }
            }
        }

        public void cancelGridSquares(Grid a) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (a.getValue(new Coordinate(i, j)) != 0) {
                        values[i][j] = 0;
                    }
                }
            }
        }

        public int getMax() {
            int r = 0;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (values[i][j] > r) {
                        r = values[i][j];
                    }
                }
            }
            return r;
        }

        public int getMin() {
            int r = this.values[0][0];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (this.values[i][j] < r) {
                        r = this.values[i][j];
                    }
                }
            }
            return r;
        }

        public Grid getNormalizedGrid() {
            return this.getNormalizedGrid(0, 100);
        }

        public Grid getNormalizedGrid(int rangeMin, int rangeMax) {
            float max = (float) getMax();
            float min = (float) getMin();
            Grid r = new Grid();

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    r.setValue(new Coordinate(i, j), rangeMin + Math.round((values[i][j] - min) / (max - min) * (rangeMax - rangeMin)));
                }
            }
            return r;
        }
    }

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        currentOpponentName = opponent;
        generator = new Random();
        myFleet = new Fleet();
        turn = 0;
        misses = new Grid();
        alreadyAttacked = new Grid();
        opponentsRemainingShips = new ArrayList<Integer>();
        opponentsRemainingShips.add(PATROL_BOAT);
        opponentsRemainingShips.add(DESTROYER);
        opponentsRemainingShips.add(SUBMARINE);
        opponentsRemainingShips.add(BATTLESHIP);
        opponentsRemainingShips.add(AIRCRAFT_CARRIER);
        opponentsDamagedShips = new HashMap<Integer, ArrayList<Coordinate>>();

        if (shipLengths == null) {
            shipLengths = new HashMap<Integer, Integer>();
            shipLengths.put(PATROL_BOAT, PATROL_BOAT_LENGTH);
            shipLengths.put(DESTROYER, DESTROYER_LENGTH);
            shipLengths.put(SUBMARINE, SUBMARINE_LENGTH);
            shipLengths.put(BATTLESHIP, BATTLESHIP_LENGTH);
            shipLengths.put(AIRCRAFT_CARRIER, AIRCRAFT_CARRIER_LENGTH);
        }

        if (parityGrid == null) {
            parityGrid = new Grid();
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 5; j++) {
                    parityGrid.setValue(new Coordinate(i, j * 2 + i % 2), 1);
                }
            }

        }

        if (Opponents == null) {
            Opponents = new HashMap<String, Opponent>();
        }
        if (!Opponents.containsKey(opponent)) {
            Opponents.put(opponent, new Opponent(opponent));
        }

        // determine if we need to use the alternate strategy
        // the rules for doing so are somewhat arbitrary
        Opponent o = Opponents.get(currentOpponentName);
        if (o.gamesPlayed == 1000) {
            // check if opponent is winning the match
            if ((double) o.gamesWon / 1000 > 0.5) {
                int sum = 0;
                for (int n : o.survivors) {
                    sum += n;
                }
                // if they won the majority of their games with less than one ship remaining
                if (((double) sum - o.gamesWon) / o.gamesWon < 0.2) {
                    // and if that ship was the patrol boat
                    if ((double) o.survivors[PATROL_BOAT] / o.gamesWon > 0.9) {
                        // we assume they are actively hiding their patrol boat, therefore
                        // use alternate strategy for when only a patrol boat remains
                        o.useAltStrategy = true;
                    }

                }
            }
        }
        if (o.useAltStrategy) {
            // if we need to use the alternative strategy, that means they are actively
            // hiding their patrol boat. so we construct a list of the 50 most likely
            // hiding spots
            o.altStratCoordPriorities = new ArrayList<Coordinate>();
            Grid persona = new Grid();
            persona.addGrid(o.myPersona);
            while (o.altStratCoordPriorities.size() < 50) {
                int min = persona.getMin();
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 10; j++) {
                        if (persona.getValue(i, j) == min) {
                            o.altStratCoordPriorities.add(new Coordinate(i, j));
                            persona.setValue(new Coordinate(i, j), persona.getValue(i, j) + o.gamesPlayed);
                        }
                    }
                }
            }
        }

        // now its time to put together our fleet
        // find the least attacked pair of squares for the patrol boat
        int[][] oppAtt = Opponents.get(currentOpponentName).aimLayout.getNormalizedGrid(0, 1000).getArray();
        ArrayList<int[]> positions = new ArrayList<int[]>();
        int lowestScore = (oppAtt[0][0] + oppAtt[0][1]);
        int score;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                score = oppAtt[i][j] + oppAtt[i][j + 1];
                if (score < lowestScore) {
                    lowestScore = score;
                }
                score = oppAtt[j][i] + oppAtt[j + 1][i];
                if (score < lowestScore) {
                    lowestScore = score;
                }
            }
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                score = oppAtt[i][j] + oppAtt[i][j + 1];
                if (score <= lowestScore) {
                    positions.add(new int[]{i, j, VERTICAL});
                }
                score = oppAtt[j][i] + oppAtt[j + 1][i];
                if (score <= lowestScore) {
                    positions.add(new int[]{j, i, HORIZONTAL});
                }
            }
        }
        int[] bestSquare = positions.get(generator.nextInt(positions.size()));
        myFleet.placeShip(bestSquare[0], bestSquare[1], bestSquare[2], PATROL_BOAT);

        // now do the same thing for the two 3 length boats
        lowestScore = oppAtt[0][0] + oppAtt[0][1] + oppAtt[0][2];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 8; j++) {
                score = oppAtt[i][j] + oppAtt[i][j + 1] + oppAtt[i][j + 2];
                if (score < lowestScore) {
                    lowestScore = score;
                }
                score = oppAtt[j][i] + oppAtt[j + 1][i] + oppAtt[j + 2][i];
                if (score < lowestScore) {
                    lowestScore = score;
                }
            }
        }

        positions = new ArrayList<int[]>();
        // we don't want a single lowest scoring spot, but a list of spots to pick randomly
        // the ratio of 6/5 is pretty arbitrary.
        lowestScore = lowestScore * 6 / 5;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 8; j++) {
                score = oppAtt[i][j] + oppAtt[i][j + 1] + oppAtt[i][j + 2];
                if (score <= lowestScore) {
                    positions.add(new int[]{i, j, VERTICAL});
                }
                score = oppAtt[j][i] + oppAtt[j + 1][i] + oppAtt[j + 2][i];
                if (score <= lowestScore) {
                    positions.add(new int[]{j, i, HORIZONTAL});
                }
            }
        }
        int[] i = positions.get(generator.nextInt(positions.size()));

        if (!myFleet.placeShip(i[0], i[1], i[2], DESTROYER)) {
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {
            }
        }
        i = positions.get(generator.nextInt(positions.size()));
        if (!myFleet.placeShip(i[0], i[1], i[2], SUBMARINE)) {
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {
            }
        }

        // place the largest two ships randomly
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), BATTLESHIP)) {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), AIRCRAFT_CARRIER)) {
        }
    }

    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    @Override
    public Coordinate makeAttack() {
        // determine the most common common squares by taking into account all
        // possible permutations
        Grid distribution = calculatePermutations();
        Coordinate thisAttack = null;

        if (opponentsDamagedShips.isEmpty()) {
            // there are no damaged ships, so we need to hunt for them
            // check if we should use alternate strategy
            if (opponentsRemainingShips.size() == 1 && opponentsRemainingShips.get(0) == PATROL_BOAT && Opponents.get(currentOpponentName).useAltStrategy) {

                for (Coordinate c : Opponents.get(currentOpponentName).altStratCoordPriorities) {
                    if (thisAttack == null && distribution.getValue(c) != 0) {
                        thisAttack = c;
                    }
                }

            }
            if (thisAttack == null) {
                // get all the squares with the highest value
                ArrayList<Coordinate> targets = new ArrayList<Coordinate>();
                int max = distribution.getMax();

                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 10; j++) {
                        if (distribution.getValue(i, j) == max) {
                            targets.add(new Coordinate(i, j));
                        }
                    }
                }
                // target a random coordinate if there is more than one
                thisAttack = targets.get(generator.nextInt(targets.size()));
            }
        } else {
            // we have found a ship! lets attack it
            // make a list of all adjacent squares to the ones i've already hit
            ArrayList<Coordinate> targets = new ArrayList<Coordinate>();
            for (ArrayList<Coordinate> coords : opponentsDamagedShips.values()) {
                for (Coordinate coord : coords) {

                    if (coord.getY() - 1 >= 0) {
                        Coordinate c = new Coordinate(coord.getX(), coord.getY() - 1);
                        if (alreadyAttacked.getValue(c) == 0) {
                            targets.add(c);
                        }
                    }
                    if (coord.getY() + 1 < 10) {
                        Coordinate c = new Coordinate(coord.getX(), coord.getY() + 1);
                        if (alreadyAttacked.getValue(c) == 0) {
                            targets.add(c);
                        }
                    }
                    if (coord.getX() - 1 >= 0) {
                        Coordinate c = new Coordinate(coord.getX() - 1, coord.getY());
                        if (alreadyAttacked.getValue(c) == 0) {
                            targets.add(c);
                        }
                    }
                    if (coord.getX() + 1 < 10) {
                        Coordinate c = new Coordinate(coord.getX() + 1, coord.getY());
                        if (alreadyAttacked.getValue(c) == 0) {
                            targets.add(c);
                        }
                    }
                }
            }
            for (ArrayList<Coordinate> coords : opponentsDamagedShips.values()) {
                // prioritize coords that form a straight line
                if (coords.size() >= 2) {
                    if (coords.get(0).getX() == coords.get(1).getX()) {
                        for (Coordinate coord : targets) {
                            if (coord.getX() == coords.get(0).getX()) {
                                distribution.setValue(coord, distribution.getValue(coord) + 100);
                            }
                        }
                    } else if (coords.get(0).getY() == coords.get(1).getY()) {
                        for (Coordinate coord : targets) {
                            if (coord.getY() == coords.get(0).getY()) {
                                distribution.setValue(coord, distribution.getValue(coord) + 100);
                            }
                        }
                    }
                }
            }
            // target the most probable square from that list
            Coordinate mostProbable = targets.get(0);
            int max = distribution.getValue(mostProbable);
            for (Coordinate coord : targets) {
                if (distribution.getValue(coord) > max) {
                    mostProbable = coord;
                    max = distribution.getValue(coord);
                }
            }
            thisAttack = mostProbable;
        }
        alreadyAttacked.increment(thisAttack);
        Opponents.get(currentOpponentName).myPersona.increment(thisAttack);

        lastAttack = thisAttack;
        return lastAttack;
    }

    @Override
    public void resultOfAttack(int result) {

        if (result == MISS) {
            misses.increment(lastAttack);
        }
        if (result / HIT_MODIFIER > 0) { // it is either a hit or a sunk ship
            Opponents.get(currentOpponentName).fleetLayouts.increment(lastAttack);
            int shipId = result % HIT_MODIFIER;
            if (!opponentsDamagedShips.containsKey(shipId)) {
                opponentsDamagedShips.put(shipId, new ArrayList<Coordinate>());
            }
            opponentsDamagedShips.get(shipId).add(lastAttack);

            if (result / SUNK_MODIFIER > 0) {
                // if we sunk a ship, remove it from the hashmaps and set all the
                // squares the ship occupied to misses for the permutation calculation
                opponentsRemainingShips.remove(new Integer(shipId));
                misses.increment(lastAttack);
                for (Coordinate c : opponentsDamagedShips.get(shipId)) {
                    misses.increment(c);
                }
                opponentsDamagedShips.remove(shipId);
            }
        }
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        turn++;
        // keep track of what my opponent knows about me
        if (myFleet.isShipAt(coord)) {
            Opponents.get(currentOpponentName).myFleetPlacement.increment(coord);
        }
        int v = Opponents.get(currentOpponentName).aimLayout.getValue(coord);
        Opponents.get(currentOpponentName).aimLayout.setValue(coord, (60 - turn >= 0) ? v + ((60 - turn)) : v);

    }

    @Override
    public void resultOfGame(int result) {
        if (result == LOST) {
            Opponents.get(currentOpponentName).gamesWon++;
        } else {
            // we won, so remove the last ship from the list of remaining ships
            opponentsRemainingShips.clear();
        }
        // if we lost(there's remaining ships), collect data about how what ships evaded us
        for (int shipId : opponentsRemainingShips) {
            Opponents.get(currentOpponentName).survivors[shipId]++;
        }
        Opponents.get(currentOpponentName).gamesPlayed++;
    }

    private Grid calculatePermutations() {
        // the core of my strategy

        // we start with a grid of zeroes
        int[][] vals = new int[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                vals[i][j] = 0;
            }
        }

        // iterate through each column (for vertical ships)
        for (int column = 0; column < 10; column++) {
            // divide each column into segments of empty squares
            ArrayList<Segment> vsegs = new ArrayList<Segment>();
            for (int row = 0; row < 10; row++) {
                if (misses.getValue(new Coordinate(column, row)) == 0) {
                    Segment seg = new Segment(row);
                    for (seg.length = 0; row < 10 && misses.getValue(new Coordinate(column, row)) == 0; row++) {
                        seg.length++;
                    }
                    vsegs.add(seg);
                }
            }
            // with each segment for this column, calculate the permutations for each ship

            for (Segment seg : vsegs) {
                for (int shipId : opponentsRemainingShips) {
                    int shipLength = shipLengths.get(new Integer(shipId));
                    int[] segData = new int[seg.length];
                    if (seg.length >= shipLength) {
                        for (int i = 0; i < seg.length; i++) {
                            segData[i] = shipLength;
                        }
                        for (int i = 0; i < shipLength - 1; i++) {
                            segData[i] -= (shipLength - (i + 1));
                            segData[(seg.length - 1) - i] -= (shipLength - i - 1);
                        }
                    }
                    // add the segment's permutation data to the total
                    for (int i = 0; i < seg.length; i++) {
                        vals[column][seg.start + i] += segData[i];
                    }
                }
            }
        }
        // then iterate through each row (for horizontal ships)
        for (int row = 0; row < 10; row++) {
            // divide each row into segments of empty squares
            ArrayList<Segment> hsegs = new ArrayList<Segment>();
            for (int column = 0; column < 10; column++) {
                if (misses.getValue(new Coordinate(column, row)) == 0) {
                    Segment seg = new Segment(column);
                    for (seg.length = 0; column < 10 && misses.getValue(new Coordinate(column, row)) == 0; column++) {
                        seg.length++;
                    }
                    hsegs.add(seg);
                }
            }

            // with each segment for this row, calculate the permutations for each ship
            for (Segment seg : hsegs) {
                for (int shipId : opponentsRemainingShips) {
                    int shipLength = shipLengths.get(new Integer(shipId));
                    int[] segData = new int[seg.length];
                    if (seg.length >= shipLength) {
                        for (int i = 0; i < seg.length; i++) {
                            segData[i] = shipLength;
                        }
                        for (int i = 0; i < shipLength - 1; i++) {
                            segData[i] -= (shipLength - (i + 1));
                            segData[(seg.length - 1) - i] -= (shipLength - i - 1);
                        }
                    }
                    // add the segment's permutation data to the total
                    for (int i = 0; i < seg.length; i++) {
                        vals[seg.start + i][row] += segData[i];
                    }
                }
            }
        }
        return new Grid(vals);
    }
}
