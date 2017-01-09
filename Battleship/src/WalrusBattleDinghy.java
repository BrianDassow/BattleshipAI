
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class WalrusBattleDinghy implements Captain {

    private Random generator;
    private ArrayList<CaptainProfile> captainRepository = new ArrayList<CaptainProfile>();
    private int totalGames = 0;
    private int[] shipLengths = {PATROL_BOAT_LENGTH, DESTROYER_LENGTH, SUBMARINE_LENGTH, BATTLESHIP_LENGTH, AIRCRAFT_CARRIER_LENGTH};
    // Skew Factors for results
    //private double myGuessFactor = 0;
    private double myMissFactor = -.6;
    private double myHitFactor = 1.5;
    private double myPlacementFactor = .8;
    private double nonGuessDecayFactor = .3;
    private double enemyHitFactor = 6;
    private double enemyNonGuessFactor = -.5;
//    private double placementStringency = .009; // See the placement algorithm to adjust.
    private CaptainProfile currentOpponent;
    private Fleet myFleet;
    private int[][] knownBoard;
    private int[][] oppBoard;
    private ArrayList<InfoShip> missingShips;
    private ArrayList<InfoShip> hitShips;
    private Coordinate lastGuess;
    private int gameTurn;

    private boolean isValidSq(int r, int c) {
        return (r >= 0 && r <= 9 && c <= 9 && c >= 0);
    }

    private int getExplodingProb(int row, int col, int dir, ArrayList<InfoShip> relevantShips) {
        if (knownBoard[row][col] != 0) {
            return 0;
        }
        int posCount = 0;
        int plus = 0;
        int minus = 0;
        int longboat = 0;
        for (InfoShip s : relevantShips) {
            longboat = Math.max(longboat, s.length);
        }
        if (dir <= 2) {
            for (int k = 1; k <= longboat - 1; k++) {
                if (!isValidSq(row, col + k)) {
                    break;
                }
                if (knownBoard[row][col + k] / 10 < 2) {
                    plus++;
                } else {
                    break;
                }
            }
            for (int k = 1; k <= longboat - 1; k++) {
                if (!isValidSq(row, col - k)) {
                    break;
                }
                if (knownBoard[row][col - k] / 10 < 2) {
                    minus++;
                } else {
                    break;
                }
            }
            for (InfoShip s : relevantShips) {
                int splus = Math.min(plus, s.length - 1);
                int sminus = Math.min(minus, s.length - 1);
                posCount += Math.max(0, (sminus + splus + 1) - s.length + 1);
            }
        }
        plus = 0;
        minus = 0;
        if (dir >= 2) {
            for (int k = 1; k <= longboat - 1; k++) {
                if (!isValidSq(row + k, col)) {
                    break;
                }
                if (knownBoard[row + k][col] / 10 < 2) {
                    plus++;
                } else {
                    break;
                }
            }
            for (int k = 1; k <= longboat - 1; k++) {
                if (!isValidSq(row - k, col)) {
                    break;
                }
                if (knownBoard[row - k][col] / 10 < 2) {
                    minus++;
                } else {
                    break;
                }
            }
            for (InfoShip s : relevantShips) {
                int splus = Math.min(plus, s.length - 1);
                int sminus = Math.min(minus, s.length - 1);
                posCount += Math.max(0, (sminus + splus + 1) - s.length + 1);
            }
        }
        return posCount;
    }

    private double[][] calculateBaseProbabilities() {
        double[][] baseProbs = new double[10][10];
        if (hitShips.size() == 0) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    baseProbs[i][j] = getExplodingProb(i, j, 2, missingShips);
                }
            }
        } else {
            for (InfoShip ship : hitShips) {
                ArrayList<InfoShip> disShip = new ArrayList<InfoShip>();
                disShip.add(new InfoShip(ship.shipID, ship.length));
                for (Coordinate c : ship.hits) {
                    int x = c.getX();
                    int y = c.getY();
                    double vfFactor = Math.pow(currentOpponent.getVertFavor(), 2) / 2 / currentOpponent.getGames();
                    if (ship.dir <= 2) {
                        baseProbs[x][Math.max(0, y - 1)] += getExplodingProb(x, Math.max(0, y - 1), 1, disShip);
                        baseProbs[x][Math.min(9, y + 1)] += getExplodingProb(x, Math.min(9, y + 1), 1, disShip);
                    }
                    if (ship.dir >= 2) {
                        baseProbs[Math.max(0, x - 1)][y] += getExplodingProb(Math.max(0, x - 1), y, 3, disShip);
                        baseProbs[Math.min(9, x + 1)][y] += getExplodingProb(Math.min(9, x + 1), y, 3, disShip);
                    }
                }
            }
        }
        return baseProbs;
    }

    private void applyOverlay(double[][] original, double[][] overlay) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (original[i][j] != 0) {
                    original[i][j] = Math.max(1, original[i][j] + overlay[i][j] / (gameTurn / 5 + 1));
                }
            }
        }
    }

    private double[][] nopeNopeNope(double[][] whoop, int col, int row) {
        if (isValidSq(col + 1, row)) {
            whoop[col + 1][row] += 100000;
        }
        if (isValidSq(col - 1, row)) {
            whoop[col - 1][row] += 100000;
        }
        if (isValidSq(col, row + 1)) {
            whoop[col][row + 1] += 100000;
        }
        if (isValidSq(col, row - 1)) {
            whoop[col][row - 1] += 100000;
        }
        return whoop;
    }

    private void findTheFairestSeas() {
        double[][] theSeas = calculateBaseProbabilities();
        applyOverlay(theSeas, currentOpponent.getDefenseOverlay());

        String notPlaced = "01234";
        for (int n = 0; n < 5; n++) {
            int rand = generator.nextInt(notPlaced.length());
            int shipID = (int) notPlaced.charAt(rand) - 48;
            notPlaced = notPlaced.substring(0, rand) + notPlaced.substring(rand + 1);
            int len = shipLengths[shipID];
            double fairest = 50000;
            int seaX = 0;
            int seaY = 0;
            int dir = 0;
            // For some strange reason, the interface uses Cartesian coordinates rather than standard Battleship ones.
            // What this means is that the for corners of the array - (0, 0), (0, 9), (9, 0), (9, 9)
            // 								 Actually correspond to - (0, 9), (9, 0), (9, 9), (0, 0)
            // With that said, let row,column represent the array coordinates and y,x represent the Cartesian coordinates.
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    double hFairness = 0;
                    double vFairness = 0;
                    for (int k = 0; k < len; k++) {
                        if (x + k < 10) {
                            hFairness += theSeas[y][x + k];
                        } else {
                            hFairness = Double.MAX_VALUE;
                            break;
                        }
                    }
                    for (int k = 0; k < len; k++) {
                        if (y + k < 10) {
                            vFairness += theSeas[y + k][x];
                        } else {
                            vFairness = Double.MAX_VALUE;
                            break;
                        }
                    }
                    double min = Math.min(fairest, Math.min(hFairness, vFairness));
                    if (min != fairest) {
                        fairest = min;
                        seaX = x;
                        seaY = y;
                        if (hFairness < vFairness) {
                            dir = 0;
                        } else {
                            dir = 1;
                        }
                    }
                }
            }
            if (!myFleet.placeShip(seaX, seaY, dir, shipID)) {
                System.out.println("RED ALERT");
            }
            for (int k = 0; k < len; k++) {
                if (dir == 0) {
                    theSeas[seaY][seaX + k] += 100000;
                    theSeas = nopeNopeNope(theSeas, seaY, seaX + k);
                    currentOpponent.adjustDefense(seaY, seaX + k, myPlacementFactor);
                } else {
                    theSeas[seaY + k][seaX] += 100000;
                    theSeas = nopeNopeNope(theSeas, seaY + k, seaX);
                    currentOpponent.adjustDefense(seaY + k, seaX, myPlacementFactor);
                }
            }
//	    	for (double[] hh:theSeas) {
//	    		for (double kl:hh)
//	    			System.out.printf("%8.2f", kl);
//	    		System.out.println();
//	    	}
//	    	System.out.println();
        }

    }
    // ----------------------------------------------------------------------------

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        // TODO Auto-generated method stub
        knownBoard = new int[10][10];
        oppBoard = new int[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                knownBoard[i][j] = 0;
                oppBoard[i][j] = 0;
            }
        }

        gameTurn = 0;
        totalGames = numMatches;

        missingShips = new ArrayList<InfoShip>();
        missingShips.add(new InfoShip(AIRCRAFT_CARRIER, AIRCRAFT_CARRIER_LENGTH));
        missingShips.add(new InfoShip(BATTLESHIP, BATTLESHIP_LENGTH));
        missingShips.add(new InfoShip(DESTROYER, DESTROYER_LENGTH));
        missingShips.add(new InfoShip(SUBMARINE, SUBMARINE_LENGTH));
        missingShips.add(new InfoShip(PATROL_BOAT, PATROL_BOAT_LENGTH));
        hitShips = new ArrayList<InfoShip>();

        generator = new Random();
        myFleet = new Fleet();

        for (CaptainProfile cpt : captainRepository) {
            if (opponent.equals(cpt.getName())) {
                currentOpponent = cpt;
            }
        }
        if (currentOpponent == null) {
            currentOpponent = new CaptainProfile(opponent);
        }

        if (Math.pow(Math.random(), .8) < currentOpponent.getWinPercentage()) {
            findTheFairestSeas();
        } else {
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), PATROL_BOAT));
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER));
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE));
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), BATTLESHIP));
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), AIRCRAFT_CARRIER));
        }
    }

    @Override
    public Fleet getFleet() {
        // TODO Auto-generated method stub
        return myFleet;
    }

    @Override
    public Coordinate makeAttack() {
        // TODO Auto-generated method stub
        double[][] probabilityModel = calculateBaseProbabilities();
        applyOverlay(probabilityModel, currentOpponent.getAttackOverlay());
        double highestProb = .00001;
        ArrayList<Coordinate> mostLikely = new ArrayList<Coordinate>();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                double prob = probabilityModel[i][j];
                if (prob >= highestProb) {
                    if (prob > highestProb) {
                        highestProb = prob;
                        mostLikely = new ArrayList<Coordinate>();
                    }
                    mostLikely.add(new Coordinate(i, j));
                }

            }
        }

        int randChoice = generator.nextInt(mostLikely.size());
        Coordinate myGuess = mostLikely.get(randChoice);
        lastGuess = myGuess;
        return myGuess;
    }

    @Override
    public void resultOfAttack(int result) {
        // TODO Auto-generated method stub
        int row, col;
        row = lastGuess.getX();
        col = lastGuess.getY();
        knownBoard[row][col] = result;

        if (result == MISS) {
            currentOpponent.adjustAttack(row, col, myMissFactor);
            return;
        }

        if (Math.abs(result / 10 - 2) < 2) {
            currentOpponent.adjustAttack(row, col, myHitFactor);
            int shipID = result % 10;
            for (int n = 0; n < missingShips.size(); n++) {
                InfoShip s = missingShips.get(n);
                if (s.shipID == shipID) {
                    InfoShip transfer = new InfoShip(s.shipID, s.length);
                    hitShips.add(transfer);
                    missingShips.remove(n);
                    break;
                }
            }
            for (int n = 0; n < hitShips.size(); n++) {
                InfoShip s = hitShips.get(n);
                if (s.shipID == shipID) {
                    s.hits.add(lastGuess);
                    s.determineDirection();
                    if (result / 10 == 2) {
                        currentOpponent.adjustVertFavor(s.dir - 2);
                        for (Coordinate c : s.hits) {
                            knownBoard[c.getX()][c.getY()] = 20 + result % 10;
                        }
                        hitShips.remove(n);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        gameTurn++;
        oppBoard[coord.getY()][coord.getX()] = 1;
        currentOpponent.adjustDefense(coord.getY(), coord.getX(), 3.0 / Math.pow(gameTurn, .55));
        if (myFleet.isShipAt(coord)) {
            currentOpponent.adjustDefense(coord.getY(), coord.getX(), enemyHitFactor);
        }
    }

    @Override
    public void resultOfGame(int result) {
        // TODO Auto-generated method stub
        currentOpponent.incrementGames();
        if (result == WON) {
            currentOpponent.addWin();
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (oppBoard[i][j] == 0) {
                    currentOpponent.adjustDefense(i, j, enemyNonGuessFactor);
                }
                if (knownBoard[i][j] == 0) {
                    currentOpponent.adjustAttack(i, j, nonGuessDecayFactor);
                }
            }
        }

        if (currentOpponent.getGames() % 1001 == 0) {
            currentOpponent.reduceOverlays();
        }

        // Reset current opponent and save their profile to the repository
        for (int cpt = 0; cpt < captainRepository.size(); cpt++) {
            if (currentOpponent.getName().equals(captainRepository.get(cpt).getName())) {
                captainRepository.remove(cpt);
                break;
            }
        }
        captainRepository.add(currentOpponent);

//        if (currentOpponent.getGames() == totalGames)
//        	currentOpponent.printOverlay();

        currentOpponent = null;

    }

    private class CaptainProfile {

        private String oppName;
        private double[][] myDefenseOverlay;
        private double[][] myAttackOverlay;
        private int vertFavor;
        private int games;
        private int myWins;

        public CaptainProfile(String opponent) {
            oppName = opponent;
            vertFavor = 0;
            myDefenseOverlay = new double[10][10];
            myAttackOverlay = new double[10][10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    myDefenseOverlay[i][j] = 0;
                    myAttackOverlay[i][j] = 0;
                }
            }
        }

        public void adjustDefense(Coordinate coord, double factor) {
            myDefenseOverlay[coord.getX()][coord.getY()] += factor;
        }

        public void adjustDefense(int row, int col, double factor) {
            myDefenseOverlay[row][col] += factor;
        }

        public void adjustAttack(int row, int col, double changeFact) {
            myAttackOverlay[row][col] += changeFact;
        }

        public void reduceOverlays() {
            double dHigh = 0;
            double aHigh = 0;
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    dHigh = Math.max(dHigh, myDefenseOverlay[i][j]);
                    aHigh = Math.max(aHigh, myAttackOverlay[i][j]);
                }
            }

            dHigh = 400 / dHigh;
            aHigh = 150 / aHigh;

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    myDefenseOverlay[i][j] *= dHigh;
                    myAttackOverlay[i][j] *= aHigh;
                }
            }
        }

        public void adjustVertFavor(int factor) {
            vertFavor += factor;
        }

        public void incrementGames() {
            games++;
        }

        public void addWin() {
            myWins++;
        }

        public void printOverlay() {
            for (double[] row : myDefenseOverlay) {
                for (double col : row) {
                    System.out.printf("%5.0f", col);
                }
                System.out.println();
            }
        }

        public String getName() {
            return oppName;
        }

        public int getGames() {
            return games;
        }

        public int getVertFavor() {
            return vertFavor;
        }

        public double[][] getDefenseOverlay() {
            return myDefenseOverlay;
        }

        public double[][] getAttackOverlay() {
            return myAttackOverlay;
        }

        public double getWinPercentage() {
            return ((double) myWins) / games;
        }
    }

    private class InfoShip {

        protected int shipID;
        protected int dir;
        protected int length;
        protected ArrayList<Coordinate> hits;

        public InfoShip(int id, int len) {
            shipID = id;
            dir = 2;
            length = len;
            hits = new ArrayList<Coordinate>();
        }

        public void determineDirection() {
            if (hits.size() < 2) {
                return;
            }
            if (hits.get(0).getX() == hits.get(1).getX()) // If both hits are in the same row
            {
                dir = 1;
            } else {
                dir = 3;
            }
        }

        public void resetData() {
            dir = 2;
            hits = new ArrayList<Coordinate>();
        }
    }
}