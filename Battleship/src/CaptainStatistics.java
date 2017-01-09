
import java.util.HashMap;
import java.util.ArrayList;

/**
 * <p>This class keeps track of how each captain is doing in the battle. It is
 * used to display the statistics in the GUI both in the text box and in the
 * heat maps. It is also printed out at the end of the battle so you can work
 * with that data and see how you did.</p>
 *
 * <p>You should NOT need to do anything to this class in order to participate
 * in the competition and modifying it is discouraged and may make your entry
 * malfunction.</p>
 *
 * @author Seth Dutter - dutters@uwstout.edu
 *
 * @version SPRING.2013
 */
public class CaptainStatistics {

    private HashMap<String, opponentInfo> opponents;
    private int numRounds;
    private int wins;
    private int losses;
    private int[][] shipPlacement;
    private int[][] attackPattern;
    private ArrayList<ArrayList<int[][]>> heatSamples;
    private ArrayList<String> oppNames;
    private String name;

    public CaptainStatistics(String name) {
        this.name = name;
        attackPattern = new int[10][10];
        shipPlacement = new int[10][10];
        opponents = new HashMap<>();
        heatSamples = new ArrayList<ArrayList<int[][]>>();
        oppNames = new ArrayList<String>();
    }

    public int[][] getShipPlacement() {
        int[][] rotatedShipPlacement = new int[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                rotatedShipPlacement[i][j] = shipPlacement[j][9 - i];
            }
        }
        return rotatedShipPlacement;
    }

    public int[][] getAttackPattern() {
        int[][] rotatedAttackPattern = new int[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                rotatedAttackPattern[i][j] = attackPattern[j][9 - i];
            }
        }
        return rotatedAttackPattern;
    }

    public int getNumRounds() {
        return numRounds;
    }

    public void addFinishedGame(String opponent, boolean win, int numRounds) {
        if (win) {
            opponents.get(opponent).addWinAgainst(numRounds);
            wins++;
        } else {
            opponents.get(opponent).addLossAgainst(numRounds);
            losses++;
        }
    }

    public void addNewGame(int[][] shipPlacement, String opponent) {
        if (!opponents.containsKey(opponent)) {
            opponents.put(opponent, new opponentInfo());
        }
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                this.shipPlacement[i][j] += shipPlacement[i][j];
            }
        }
    }

    public void addRound(String opponent, boolean hit, Coordinate attack) {
        numRounds++;
        if (hit) {
            opponents.get(opponent).addHitAgainst();
        } else {
            opponents.get(opponent).addMissAgainst();
        }

        try {
            attackPattern[attack.getX()][attack.getY()]++;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.printf("'%s' attacked '%s' out of bounds at the coordinate %s\n", name, opponent, attack.toString());
        }
    }

    public ArrayList<ArrayList<int[][]>> getSamples() {
        return this.heatSamples;
    }

    public ArrayList<String> getSampleNames() {
        return this.oppNames;
    }

    public void addSample(String oppName, int[][] oldAtk, int[][] oldPlace) {

        int[][] atkHeat = new int[10][10];
        int[][] curHeat = getAttackPattern();
        int[][] placeHeat = new int[10][10];
        int[][] curPlace = getShipPlacement();

        for (int i = 0; i < 100; i++) {
            int x = i / 10;
            int y = i % 10;
            atkHeat[x][y] = curHeat[x][y] - oldAtk[x][y];
            placeHeat[x][y] = curPlace[x][y] - oldPlace[x][y];
        }
        //System.out.println("Old atk "+ oldAtk[4][5]);
        if (this.oppNames.contains(oppName)) {
            this.heatSamples.get(this.oppNames.indexOf(oppName)).add(this.heatSamples.get(this.oppNames.indexOf(oppName)).size() / 2, atkHeat);
            this.heatSamples.get(this.oppNames.indexOf(oppName)).add(placeHeat);
        } else {
            this.oppNames.add(oppName);
            ArrayList<int[][]> temp = new ArrayList<int[][]>();
            temp.add(atkHeat);
            temp.add(placeHeat);
            this.heatSamples.add(temp);
        }
    }

    public String getResultsAgainst(String opponent) {
        if (!opponents.containsKey(opponent)) {
            return null;
        }

        int totalShots = (opponents.get(opponent).hits + opponents.get(opponent).misses);
        double hitFreq = (totalShots == 0 ? 0.0 : opponents.get(opponent).hits / (double) totalShots);

        int wins = opponents.get(opponent).winsAgainst;
        int losses = opponents.get(opponent).lossesAgainst;

        double forWin = (wins == 0 ? 0.0 : opponents.get(opponent).winRounds / (double) wins);
        double forLoss = (losses == 0 ? 0.0 : opponents.get(opponent).lossRounds / (double) losses);

        String results = String.format("Wins Against:   %,d\n"
                + "Losses Against: %,d\n\n",
                opponents.get(opponent).winsAgainst,
                opponents.get(opponent).lossesAgainst);

        results += String.format("Hits:     %,d\n"
                + "Misses:   %,d\n"
                + "Accuracy: %.2f%%\n\n",
                opponents.get(opponent).hits, opponents.get(opponent).misses,
                hitFreq * 100.0);

        results += String.format("Avg. Attacks for a Win:  %.2f\n"
                + "Avg. Attacks for a Loss: %.2f",
                forWin, forLoss);

        return results;
    }

    public void outputStatistics() {
        System.out.println("\n\n---------- Captain -  " + this.name + " ----------");
        System.out.println("Total Wins: " + wins);
        System.out.println("Total Losses: " + losses);
        System.out.println("\nShip Placement Pattern: 0, " + (wins + losses));
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.print(", " + shipPlacement[j][9 - i]);
            }
            System.out.println("");
        }
        System.out.println("\nAttack Pattern: 0, " + (wins + losses));
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.print(", " + attackPattern[j][9 - i]);
            }
            System.out.println("");
        }
        for (String op : opponents.keySet()) {
            System.out.println("\nVersus " + op);
            System.out.println("Wins Against: " + opponents.get(op).winsAgainst);
            System.out.println("Losses Against: " + opponents.get(op).lossesAgainst);
            System.out.println("Hits: " + opponents.get(op).hits);
            System.out.println("Misses: " + opponents.get(op).misses);
            System.out.println("Hit Frequency: " + (double) opponents.get(op).hits / ((double) (opponents.get(op).hits + opponents.get(op).misses)));
            System.out.println("Average Number of Attacks for a  Win: " + (double) opponents.get(op).winRounds / (double) opponents.get(op).winsAgainst);
            System.out.println("Average Number of Attacks for a Loss: " + (double) opponents.get(op).lossRounds / (double) opponents.get(op).lossesAgainst);
        }
    }

    class opponentInfo {

        public int hits;
        public int misses;
        public int numGames;
        public int winsAgainst;
        public int winRounds;
        public int lossesAgainst;
        public int lossRounds;

        public void addWinAgainst(int numRounds) {
            winsAgainst++;
            winRounds += numRounds;
            numGames++;
        }

        public void addLossAgainst(int numRounds) {
            lossesAgainst++;
            lossRounds += numRounds;
            numGames++;
        }

        public void addHitAgainst() {
            hits++;
        }

        public void addMissAgainst() {
            misses++;
        }
    }
}