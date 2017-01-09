
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

// "No...the game's not over."
public class Khan implements Captain {
    // diagonostic options

    public static final boolean LOGGING = true;
    public static final int OVERRIDE_PLACEMENT = -1;
    // tuning options
    public static final int OPENING_ATTACKS = 5;
    public static final int MINIMUM_TARGET_SAMPLE = 2500;
    public static final int MINIMUM_ATTEMPTS_AFTER_TARGETING_SWITCH = 5000;
    public static final int MINIMUM_PLACEMENT_SAMPLE = 2500;
    public static final double MINIMUM_MISS_RATE = 0.55;
    public static final double MINIMUM_FLEET_HIT = 32.50;
    public static final int MINIMUM_ATTEMPTS_AFTER_PLACEMENT_SWITCH = 5000;
    public static final int TENDENCY_MINIMUM_GAMES = 1000;
    public static final int TENDENCY_MINIMUM_SHIPHITS = 250;
    public static final double[] TENDENCY_SHIP_WEIGHT = {0.50, 0.33, 0.33, 0.25, 0.20};
    public static final int[] SHIP_LENGTHS = {PATROL_BOAT_LENGTH, DESTROYER_LENGTH, SUBMARINE_LENGTH, BATTLESHIP_LENGTH, AIRCRAFT_CARRIER_LENGTH};
    protected Random generator;
    protected Fleet myFleet;
    ArrayList<ArrayList<BitSet>> possiblePlacements;
    double[][] shipProbability;
    boolean[] sankShips;
    BitSet attacks;
    int lastAttack;
    ArrayList<ArrayList<BitSet>> allPlacements;
    HashMap<String, Opponent> opponentData = new HashMap<String, Opponent>();
    Opponent opponent;
    protected boolean[] myShipsHit;
    protected int attacksUntilAllMyShipsHit;
    protected int opponentAttackCount;
    protected int totalMatches;

    public Khan() {
        attacks = new BitSet(100);
        generator = new Random();
        allPlacements = new ArrayList<ArrayList<BitSet>>(5);
        possiblePlacements = new ArrayList<ArrayList<BitSet>>(5);
        for (int i = 0; i < 5; i++) {
            allPlacements.add(generateShipPlacements(SHIP_LENGTHS[i]));
        }
        sankShips = new boolean[5];
        shipProbability = new double[5][100];
        myShipsHit = new boolean[5];
    }

    public void initialize(int numMatches, int numCaptains, String name) {
        totalMatches = numMatches;

        opponent = opponentData.get(name);
        if (opponent == null) {
            opponent = new Opponent(name);
            opponent.name = name;
            opponentData.put(name, opponent);
        }
        myFleet = new Fleet();
        placeShips();

        attacks.clear();
        possiblePlacements.clear();
        for (int i = 0; i < sankShips.length; i++) {
            sankShips[i] = false;
            possiblePlacements.add((ArrayList<BitSet>) allPlacements.get(i).clone());
        }
        updateDensity();
        Arrays.fill(myShipsHit, false);
        attacksUntilAllMyShipsHit = 0;
        opponentAttackCount = 0;
    }

    public Fleet getFleet() {
        return myFleet;
    }

    public void placeShips() {
        int method = (OVERRIDE_PLACEMENT < 0) ? opponent.currentPlacementMethod.id : OVERRIDE_PLACEMENT;
        switch (method) {
            case Opponent.PLACEMENT_CORNERS:
                placeShipsInCorners();
                break;
            case Opponent.PLACEMENT_UNIFORM:
                mrUniform();
                break;
            case Opponent.PLACEMENT_ADAPTIVE:
                placeShipsAdaptively();
                break;
        }
    }

    public void placeShipsAdaptively() {
        placeShipAdaptive(BATTLESHIP);
        placeShipAdaptive(DESTROYER);
        placeShipAdaptive(SUBMARINE);
        placeShipAdaptive(PATROL_BOAT);
        placeShipRandom(AIRCRAFT_CARRIER);
    }

    public void placeShipAdaptive(int s) {
        ArrayList<BitSet> l = new ArrayList<BitSet>();
        l.addAll((ArrayList<BitSet>) allPlacements.get(s).clone());
        ArrayList<BitSet> placed = new ArrayList<BitSet>(5);
        for (Ship p : myFleet.fleet) {
            if (p != null) {
                placed.add(bitSetFromShip(p));
            }
        }
        for (int i = l.size() - 1; i >= 0; i--) {
            BitSet placement = l.get(i);
            for (int j = 0; j < placed.size(); j++) {
                BitSet p = placed.get(j);
                if (placement.intersects(p)) {
                    l.remove(i);
                    break;
                }
            }
        }

        int best = Integer.MAX_VALUE;
        ArrayList<BitSet> bestList = new ArrayList<BitSet>();
        for (BitSet bs : l) {
            int score = 0;
            for (int next = bs.nextSetBit(0); next >= 0; next = bs.nextSetBit(next + 1)) {
                score += opponent.attacks[next];
            }
            if (score < best) {
                best = score;
                bestList.clear();
                bestList.add(bs);
            } else if (score == best) {
                bestList.add(bs);
            }
        }

        if (bestList.size() > 0) {
            BitSet bs = bestList.get(generator.nextInt(bestList.size()));
            int id = bs.nextSetBit(0);
            int dir = VERTICAL;
            if (bs.get(id + 1)) {
                dir = HORIZONTAL;
            }
            if (!myFleet.placeShip(col(id), row(id), dir, s)) {
                assert (false) : "Failure to place on adaptive attack for " + s + "," + id + "," + dir;
                placeShipRandom(s);
            }
        } else {
            assert (false) : "Failure to find an adaptive attack for " + s;
            placeShipRandom(s);
        }

    }

    public void placeShipsInCorners() {
        int[] corners = {0, 9, 90, 99};
        ArrayList<Integer> ships = new ArrayList<Integer>();
        for (int i = 0; i < 5; i++) {
            ships.add(i);
        }
        Collections.shuffle(ships);

        int direction = generator.nextInt(2);
        for (int i = 0; i < 4; i++) {
            direction = (direction + 1) % 2;
            int ship = ships.get(i);
            int id = corners[i];

            if (id == 9 && direction == HORIZONTAL) {
                id = id - SHIP_LENGTHS[ship] + 1;
            } else if (id == 90 && direction == VERTICAL) {
                id = id - 10 * (SHIP_LENGTHS[ship] - 1);
            } else if (id == 99) {
                if (direction == HORIZONTAL) {
                    id = id - SHIP_LENGTHS[ship] + 1;
                } else {
                    id = id - 10 * (SHIP_LENGTHS[ship] - 1);
                }
            }
            if (!myFleet.placeShip(col(id), row(id), direction, ship)) {
                assert (false) : "Failure to place on the outer rim! :" + ship + "," + id + "," + direction;
                placeShipRandom(ship);
            }
        }

        // place the last ship in the interior
        while (!myFleet.placeShip(generator.nextInt(6) + 2, generator.nextInt(6) + 2, generator.nextInt(2), ships.get(4))) {
        }
    }

    public void placeShipRandom(int ship) {
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
        }
    }

    /**
     * *************************REBUILT - MALCOLMREYNOLDS UNIFORM PLACEMENT******************************************************
     */
    public void mrUniform() {
        ArrayList<Ship> list = buildMrUniformMap();
        for (int i = 0; i < 5; i++) {
            Ship ship = getRandomShip(list, i);
            if (!myFleet.placeShip(ship.location, ship.direction, i)) {
                assert (false) : "Failed to place ship " + i;
            }
        }
    }

    public ArrayList<Ship> buildMrUniformMap() {
        ArrayList<Ship> ships = new ArrayList<Ship>();
        Ship[] placed = myFleet.getFleet();
        for (int i = 0; i < 5; i++) {
            if (placed[i] != null) {
                ships.add(placed[i]);
            }
        }
        for (int i = 4; i > -1; i--) {
            if (placed[i] != null) {
                continue;
            }
            while (ships.size() < (5 - i) * 4) {
                Ship testShip = null;
                do {
                    testShip = new Ship(new Coordinate(generator.nextInt(10), generator.nextInt(10)), generator.nextInt(2), i);
                } while (!testShip.isValid());
                boolean doesIntersect = false;
                for (Ship s : ships) {
                    if (s.intersectsShip(testShip)) {
                        doesIntersect = true;
                        break;
                    }
                }
                if (!doesIntersect) {
                    ships.add(testShip);
                }
            }
        }
        return ships;
    }

    Ship getRandomShip(ArrayList<Ship> ships, int model) {
        ArrayList<Ship> list = new ArrayList<Ship>();
        for (Ship s : ships) {
            if (s.getModel() == model) {
                list.add(s);
            }
        }
        return list.get(generator.nextInt(list.size()));
    }

    /**
     * *************************MALCOLMREYNOLDS UNIFORM PLACEMENT******************************************************
     */
    public int makeTrulyRandomAttack() {
        int attack = -1;
        do {
            attack = generator.nextInt(100);
        } while (attacks.get(attack));
        return attack;
    }

    public int getNeighborScore(int r, int c, int dr, int dc) {
        int score = 0;
        int i = r + dr;
        int j = c + dc;
        if (i >= 0 && i < 10 && j >= 0 && j < 10 && !attacks.get(id(i, j))) {
            score += 1; // 
            i += dr;
            j += dc;
            // if in the same direction it still is free, increase the score
            if (i >= 0 && i < 10 && j >= 0 && j < 10 && !attacks.get(id(i, j))) {
                score += 1;
            }
        }
        return score;
    }

    public int makeIntelligentRandomAttack() {

        int[] score = new int[100];
        int[] drdc = {-1, 0, 1};

        // for each open spot, calculate its spatial score
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                int s = 0;
                // previously attacked spots should be score with zero
                if (!attacks.get(id(r, c))) {
                    for (int dr : drdc) {
                        for (int dc : drdc) {
                            if (dc != 0 || dr != 0) {
                                s += getNeighborScore(r, c, dr, dc);
                            }
                        }
                    }
                }
                score[id(r, c)] = s;
            }
        }

        int highScore = Integer.MIN_VALUE;
        ArrayList<Integer> highIndices = new ArrayList<Integer>();
        for (int i = 0; i < score.length; i++) {
            if (score[i] > highScore) {
                highScore = score[i];
                highIndices.clear();
                highIndices.add(i);
            } else if (score[i] == highScore) {
                highIndices.add(i);
            }
        }
        return highIndices.get(generator.nextInt(highIndices.size()));
    }

    public int makeOpeningAttack() {
        int attack = -1;
        if (attacks.cardinality() < OPENING_ATTACKS) {
            if (attacks.cardinality() == 0) {
                attack = makeTrulyRandomAttack();
            } else {
                attack = makeIntelligentRandomAttack();
            }
        }
        return attack;
    }

    public int makeProbabilityAttack() {
        int attack = -1;
        double best = -1;
        ArrayList<Integer> bestList = new ArrayList<Integer>();
        for (int id = 0; id < 100; id++) {
            if (!attacks.get(id)) {
                double p = 1;
                for (int s = 0; s < 5; s++) {
                    if (!sankShips[s]) {
                        double q = shipProbability[s][id];
                        if (opponent.currentTargetingMethod.id == Opponent.TARGETING_DENSITY || q == 1.0) {
                            p *= (1 - q);
                        } else if (opponent.currentTargetingMethod.id == Opponent.TARGETING_ADAPTIVE) {
                            p *= (1 - q * opponent.tendency(s, id));
                        }
                    }
                }
                p = 1.0 - p;
                if (p == 1.0) {
                    best = p;
                    bestList.clear();
                    bestList.add(id);
                    break;
                } else if (p > best) {
                    best = p;
                    bestList.clear();
                    bestList.add(id);
                } else if (p == best) {
                    bestList.add(id);
                }
            }
        }
        if (bestList.size() > 0) {
            attack = bestList.get(generator.nextInt(bestList.size()));
        }
        return attack;
    }

    public Coordinate makeAttack() {
        lastAttack = makeOpeningAttack();
        if (lastAttack < 0) {
            lastAttack = makeProbabilityAttack();
        }
        if (lastAttack < 0) {
            assert (false) : "This should never happen, unable to find a probabilty attack.";
            lastAttack = makeTrulyRandomAttack();  // failsafe
        }
        return id(lastAttack);
    }

    public void pruneList(boolean hit, int ship) {
        for (int i = 0; i < possiblePlacements.size(); i++) {
            if (!sankShips[i]) {
                ArrayList<BitSet> l = possiblePlacements.get(i);
                for (int j = l.size() - 1; j >= 0; j--) {
                    if (((!hit || (hit && i != ship)) && l.get(j).get(lastAttack)) || // if we missed entirely, or we hit a different ship at lastAttack
                            (hit && ship == i && !l.get(j).get(lastAttack))) {             // if we hit this ship at lastAttack, and this placement doesn't contain lastAttack
                        l.remove(j);
                    }
                }
            }
        }
        updateDensity();
    }

    public void updateDensity() {
        for (int i = 0; i < 5; i++) {
            if (!sankShips[i]) {
                Arrays.fill(shipProbability[i], 0);
                ArrayList<BitSet> l = possiblePlacements.get(i);
                double value = 1.0 / l.size();
                for (BitSet bs : l) {
                    for (int next = bs.nextSetBit(0); next >= 0; next = bs.nextSetBit(next + 1)) {
                        shipProbability[i][next] += value;
                    }
                }
            }
        }
    }

    public void resultOfAttack(int result) {
        assert (!attacks.get(lastAttack)) : "What the hell, we attacked the same spot twice.";
        attacks.set(lastAttack);
        if (result == MISS) {
            opponent.recordAttack(lastAttack, -1);
            pruneList(false, -1);
        } else {
            int hitShip = result % HIT_MODIFIER;
            opponent.recordAttack(lastAttack, hitShip);
            if (result / SUNK_MODIFIER >= 1) {
                sankShips[hitShip] = true;
            }
            pruneList(true, hitShip);
        }
    }

    public void opponentAttack(Coordinate coord) {
        opponentAttackCount++;
        opponent.recordOpponentAttack(coord, !myFleet.isShipAt(coord), opponentAttackCount);
        Ship[] ships = myFleet.getFleet();
        for (int s = 0; s < ships.length; s++) {
            if (ships[s].isOnShip(coord)) {
                myShipsHit[s] = true;
            }
        }
        boolean allHit = true;
        for (boolean hit : myShipsHit) {
            if (!hit) {
                allHit = false;
                break;
            }
        }
        if (attacksUntilAllMyShipsHit == 0 && allHit) {
            attacksUntilAllMyShipsHit = opponentAttackCount;
        }
    }

    public void resultOfGame(int result) {
        opponent.result(result, attacksUntilAllMyShipsHit);
    }

    public abstract class Method implements Comparable<Method> {

        int id;
        String name;
        double mavgWin;
        double mavgFleetHit;
        int games;
        int gamesFleetHit;

        public Method(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public void result(int result, int attacksUntilAllMyShipsHit) {
            games++;
            mavgWin = (games == 1) ? result : mavgWin + ((result - mavgWin) / games);
            if (attacksUntilAllMyShipsHit > 0) {
                gamesFleetHit++;
                mavgFleetHit = (gamesFleetHit == 1) ? attacksUntilAllMyShipsHit : mavgFleetHit + ((attacksUntilAllMyShipsHit - mavgFleetHit) / gamesFleetHit);
            }
        }

        public abstract double score();

        public Method evaluate(Method[] methods, Double separation) {
            Method best = null;
            if (methods.length > 0) {
                ArrayList<Method> list = new ArrayList<Method>(Arrays.asList(methods));
                Collections.sort(list);
                best = list.get(0);
                if (methods.length > 1 && separation != null) {
                    Method second = list.get(1);
                    if (best.score() - second.score() < separation.doubleValue()) {
                        best = null;
                    }
                }
            }
            return best;
        }
        // sort in reverse order

        public int compareTo(Method other) {
            if (other.score() > this.score()) {
                return 1;
            } else if (other.score() < this.score()) {
                return -1;
            }
            return 0;
        }

        public String toString() {
            return name;
        }
    }

    public class TargetingMethod extends Method {

        double mavgAccuracy;
        int attacks;

        public TargetingMethod(int id, String name) {
            super(id, name);
        }

        private void recordHitOrMiss(int hit) {
            attacks++;
            mavgAccuracy = (attacks == 1) ? hit : mavgAccuracy + ((hit - mavgAccuracy) / attacks);
        }

        public void recordHit() {
            recordHitOrMiss(1);
        }

        public void recordMiss() {
            recordHitOrMiss(0);
        }

        public double score() {
            return mavgAccuracy;
        }
    }

    public class PlacementMethod extends Method {

        double mavgMissRate;
        int attacks;

        public PlacementMethod(int id, String name) {
            super(id, name);
        }

        private void recordHitOrMiss(int miss) {
            attacks++;
            mavgMissRate = (attacks == 1) ? miss : mavgMissRate + ((miss - mavgMissRate) / attacks);
        }

        public void recordHit() {
            recordHitOrMiss(0);
        }

        public void recordMiss() {
            recordHitOrMiss(1);
        }

        public double score() {
            return 0.40 * mavgMissRate + 0.60 * (mavgFleetHit / 100);
        }
    }

    private class Opponent {

        public static final int PLACEMENT_CORNERS = 0;
        public static final int PLACEMENT_UNIFORM = 1;
        public static final int PLACEMENT_ADAPTIVE = 2;
        public static final int TARGETING_DENSITY = 0;
        public static final int TARGETING_ADAPTIVE = 1;
        String name;
        int[] hitLocation;
        int[][] hitShipLocation;
        int[] hitCount;
        int[] attacks;
        int hits;
        int games;
        int wins;
        TargetingMethod[] targeting;
        TargetingMethod currentTargetingMethod;
        boolean targetingSampling;
        PlacementMethod[] placement;
        PlacementMethod currentPlacementMethod;
        boolean placementSampling;
        int gamesSinceLastPlacementSwitch;
        int gamesSinceLastTargetingSwitch;
        long begin, latest;

        public Opponent(String name) {
            this.name = name;
            hitLocation = new int[100];
            hitShipLocation = new int[5][100];
            hitCount = new int[5];
            attacks = new int[100];
            hits = 0;
            games = 0;
            placement = new PlacementMethod[3];
            placement[PLACEMENT_CORNERS] = new PlacementMethod(PLACEMENT_CORNERS, "Corners");
            placement[PLACEMENT_UNIFORM] = new PlacementMethod(PLACEMENT_UNIFORM, "Uniform");
            placement[PLACEMENT_ADAPTIVE] = new PlacementMethod(PLACEMENT_ADAPTIVE, "Adaptive");
            currentPlacementMethod = placement[PLACEMENT_CORNERS];
            placementSampling = true;
            targeting = new TargetingMethod[2];
            targeting[TARGETING_DENSITY] = new TargetingMethod(TARGETING_DENSITY, "Density");
            targeting[TARGETING_ADAPTIVE] = new TargetingMethod(TARGETING_ADAPTIVE, "Adaptive");
            currentTargetingMethod = targeting[TARGETING_DENSITY];
            targetingSampling = true;
        }

        // This is called when an opponent attacks
        public void recordOpponentAttack(Coordinate c, boolean miss, int attackNumber) {
            attacks[id(c)]++;
            if (miss) {
                currentPlacementMethod.recordMiss();
            } else {
                currentPlacementMethod.recordHit();
            }
        }

        // this is called when we have a verified strike, confirming an opponents placement
        public void recordAttack(int id, int ship) {
            if (ship < 0) {
                currentTargetingMethod.recordMiss();
            } else {
                currentTargetingMethod.recordHit();
                hits++;
                hitLocation[id]++;
                hitShipLocation[ship][id]++;
                hitCount[ship]++;
            }
        }

        public void result(int result, int attacksUntilAllMyShipsHit) {
            if (games == 0) {
                log("And so it begins.");
                begin = System.currentTimeMillis();
                latest = begin;
            }
            games++;
            if (result == WON) {
                wins++;
            }
            gamesSinceLastPlacementSwitch++;
            gamesSinceLastTargetingSwitch++;
            currentPlacementMethod.result(result, attacksUntilAllMyShipsHit);
            currentTargetingMethod.result(result, attacksUntilAllMyShipsHit);

            if (targetingSampling) {
                if (currentTargetingMethod.games > MINIMUM_TARGET_SAMPLE) {
                    log("Targeting Method " + currentTargetingMethod + " has accuracy %6.3f\n", currentTargetingMethod.mavgAccuracy * 100.0);
                    if (currentTargetingMethod.id < (targeting.length - 1)) {
                        currentTargetingMethod = targeting[currentTargetingMethod.id + 1];
                        currentTargetingMethod.games = 0;
                    } else {
                        TargetingMethod best = (TargetingMethod) currentTargetingMethod.evaluate(targeting, new Double(0.001));
                        if (best != null) {
                            currentTargetingMethod = best;
                            targetingSampling = false;
                            log("Chose Targeting Method " + currentTargetingMethod);
                        } else {
                            log("Insufficient separation in targeting, extending sampling...");
                            currentTargetingMethod = targeting[0];
                            currentTargetingMethod.games = 0;
                        }
                    }
                }
            } else {
                if (gamesSinceLastTargetingSwitch > MINIMUM_ATTEMPTS_AFTER_TARGETING_SWITCH) {
                    evaluateTargeting();
                }
            }

            if (placementSampling) {
                int cutoff = MINIMUM_PLACEMENT_SAMPLE / 5;
                if (currentPlacementMethod.games > MINIMUM_PLACEMENT_SAMPLE
                        || (currentPlacementMethod.games > cutoff && currentPlacementMethod.mavgMissRate < MINIMUM_MISS_RATE)
                        || (currentPlacementMethod.games > cutoff && currentPlacementMethod.mavgFleetHit < MINIMUM_FLEET_HIT)) {

                    log("Method %s Sample Complete, Score %5.4f (mavg=%5.4f, miss=%5.4f, fh=%5.2f) after %d games\n",
                            currentPlacementMethod.name, currentPlacementMethod.score(), currentPlacementMethod.mavgWin,
                            currentPlacementMethod.mavgMissRate, currentPlacementMethod.mavgFleetHit, currentPlacementMethod.games);
                    selectNextPlacementMethod();
                }
            } else {
                if (gamesSinceLastPlacementSwitch > MINIMUM_ATTEMPTS_AFTER_PLACEMENT_SWITCH) {
                    evaluatePlacement();
                }
            }

            if (LOGGING) {
                if (games == totalMatches) {
                    log("Final Statistics: wins: %5.2f%%, sec=%.2f\n", 100.0 * wins / totalMatches, (System.currentTimeMillis() - begin) / 1000.0);
                    for (int i = 0; i < placement.length; i++) {
                        PlacementMethod pm = placement[i];
                        log("Placement %10s: used %6.2f%%, score %.4f(mavg: %6.2f%%, miss: %5.2f%%, fh: %4.2f)\n",
                                pm.name, 100.0 * pm.games / games, pm.score(), 100.0 * pm.mavgWin, 100.0 * pm.mavgMissRate, pm.mavgFleetHit);
                    }
                    for (int i = 0; i < targeting.length; i++) {
                        TargetingMethod tm = targeting[i];
                        log("Targeting %10s: used %6.2f%%, accuracy: %6.2f%%\n",
                                tm.name, 100.0 * tm.games / games, 100.0 * tm.mavgAccuracy);
                    }
                } else if (games % 25000 == 0 && !targetingSampling && !placementSampling) {
                    PlacementMethod pm = currentPlacementMethod;
                    TargetingMethod tm = currentTargetingMethod;
                    long now = System.currentTimeMillis();
                    log("%d UPDATE Overall: %.2f%%, sec=%.2f, Placement %s score %.4f:(mavg=%5.4f,miss=%5.4f,fh=%5.2f), %s Accuracy=%.4f\n",
                            games, wins * 100.0 / games, (now - latest) / 1000.0, pm, pm.score(), pm.mavgWin, pm.mavgMissRate, pm.mavgFleetHit, tm, tm.mavgAccuracy);
                    latest = now;
                }
            }

        }

        public void selectNextPlacementMethod() {
            boolean success = false;
            if (currentPlacementMethod.id < placement.length - 1) {
                currentPlacementMethod = placement[currentPlacementMethod.id + 1];
                success = true;
            }
            if (!success) {
                placementSampling = false;
                PlacementMethod bm = currentPlacementMethod;
                for (int i = 0; i < placement.length; i++) {
                    PlacementMethod m = placement[i];
                    if (m.score() > bm.score()) {
                        bm = m;
                    }
                }
                currentPlacementMethod = bm;
                log("Final Decision: %s with score %.4f:(mavg=%5.4f,miss=%5.4f,fh=%5.2f)\n", bm.name, bm.score(), bm.mavgWin, bm.mavgMissRate, bm.mavgFleetHit);
            }
        }

        public void evaluatePlacement() {
            PlacementMethod cm = currentPlacementMethod;
            PlacementMethod bm = (PlacementMethod) cm.evaluate(placement, null);
            if (bm != currentPlacementMethod) {
                log("Abandon Ship! Method %s with score %.4f (mavg=%.4f,miss=%.4f,fh=%4.2f) appears better than current %s with score %.4f (mavg=%.4f,miss=%.4f,fh=%4.2f) after %d games.\n",
                        bm.name, bm.score(), bm.mavgWin, bm.mavgMissRate, bm.mavgFleetHit,
                        cm.name, cm.score(), cm.mavgWin, cm.mavgMissRate, cm.mavgFleetHit, gamesSinceLastPlacementSwitch);
                currentPlacementMethod = bm;
                gamesSinceLastPlacementSwitch = 0;
            }
        }

        public void evaluateTargeting() {
            TargetingMethod ct = currentTargetingMethod;
            TargetingMethod bt = (TargetingMethod) ct.evaluate(targeting, null);
            if (bt != currentTargetingMethod) {
                log("Reload the Cannons! Switching to %s with accuracy %.4f, appears better than %s with %.4f after %d games\n",
                        bt.name, bt.mavgAccuracy, ct, ct.mavgAccuracy, gamesSinceLastTargetingSwitch);
                currentTargetingMethod = bt;
                gamesSinceLastTargetingSwitch = 0;
            }
        }

        public double tendency(int ship, int id) {
            double score = 1.0;
            if (games >= TENDENCY_MINIMUM_GAMES && hits > 0) {
                score = hitLocation[id] * 1.0 / hits;
                if (hitCount[ship] >= TENDENCY_MINIMUM_SHIPHITS) {
                    double weight = TENDENCY_SHIP_WEIGHT[ship];
                    score = (1 - weight) * score + weight * (hitShipLocation[ship][id] * 1.0 / hitCount[ship]);
                }
            }
            return score;
        }
    }

    public static ArrayList<BitSet> generateShipPlacements(int length) {
        ArrayList<BitSet> list = new ArrayList<BitSet>(200);
        for (int r = 0; r < 10; r++) {  // horizontal placements
            for (int c = 0; c < 10 - length + 1; c++) {
                BitSet placement = new BitSet(100);
                int id = id(r, c);
                placement.set(id, id + length);
                list.add(placement);
            }
        }
        for (int r = 0; r < 10 - length + 1; r++) { // vertical placements
            for (int c = 0; c < 10; c++) {
                BitSet placement = new BitSet(100);
                for (int i = 0; i < length; i++) {
                    placement.set(id(r + i, c));
                }
                list.add(placement);
            }
        }
        return list;
    }

    static BitSet bitSetFromShip(Ship ship) {
        BitSet bs = new BitSet(100);
        int step = ship.direction == VERTICAL ? 10 : 1;
        for (int i = 0, next = id(ship.location); i < ship.length; i++) {
            bs.set(next);
            next += step;
        }
        return bs;
    }

    static int id(int r, int c) {
        return r * 10 + c;
    }
    // note the reversal!

    static int id(Coordinate c) {
        return c.getY() * 10 + c.getX();
    }

    static Coordinate id(int id) {
        return new Coordinate(id / 10, id % 10);
    }

    static int col(int id) {
        return id % 10;
    }

    static int row(int id) {
        return id / 10;
    }

    void log(String message, Object... arguments) {
        if (LOGGING) {
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SS");
            String timeStamp = formatter.format(new Date());
            if (arguments.length == 0) {
                System.out.println(opponent.name + "|" + timeStamp + "|" + message);
            } else {
                System.out.printf(opponent.name + "|" + timeStamp + "|" + message, arguments);
            }
        }
    }
}