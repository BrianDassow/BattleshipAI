
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * TODO Put here a description of what this class does.
 *
 * @author John. Created Mar 18, 2013.
 */
public class CaptainArcher implements Captain, Constants {

    private int[][] theirShots = new int[10][10];
    private boolean[][] theirGrid = new boolean[10][10];
    private int[][] hitsHeat = new int[10][10];
    private int[][] shotsHeat = new int[10][10];
    private double[][] avgHeat = new double[10][10];
    private boolean[][] myShips = new boolean[10][10];
    private ArrayList<String[]> hitShips;
    private boolean[] enemyShips = new boolean[5];
    private int[] shipLength = {2, 3, 3, 4, 5};
    private ArrayList<Coordinate> availableShots = new ArrayList<Coordinate>();
    private Coordinate lastShot;
    private String opponent;
    private String lastOpp = "";
    private Random rGen;
    private Fleet myFleet;
    private int matchNumber = 0;
    private int matchTotal;

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        this.lastShot = null;
        this.matchTotal = numMatches;
        for (boolean[] b : this.theirGrid) {
            Arrays.fill(b, false);
        }
        //true mens enemy ship is still alive
        Arrays.fill(this.enemyShips, true);
        this.hitShips = new ArrayList<String[]>();
        for (int g = 0; g < 5; g++) {
            this.hitShips.add(null);
        }
        //see if this is the same opponent, if so keep track of matches
        this.opponent = opponent;
        if (opponent.equals(this.lastOpp)) {
            this.matchNumber++;
        } //else reset all the opponent data
        else {
            this.theirShots = new int[10][10];
            this.matchNumber = 0;
            this.shotsHeat = new int[10][10];
            this.hitsHeat = new int[10][10];
            this.avgHeat = new double[10][10];
            this.lastOpp = opponent;
        }
        //create a list of where my ships are
        for (boolean[] b : this.myShips) {
            Arrays.fill(b, false);
        }
        //Create a new fleet
        this.rGen = new Random();

        this.myFleet = new Fleet();
        for (int shipType = 0; shipType < 5; shipType++) {
            boolean placed = false;
            //if we don't have enough data on the opponent, randomly distribute the ships
            if (this.matchNumber <= (numMatches / 100)) {
                while (!placed) {
                    int baseCoord = this.rGen.nextInt(100);
                    placed = true;
                    if (!this.myFleet.placeShip(new Coordinate(baseCoord % 10, baseCoord / 10 - this.shipLength[shipType]), VERTICAL, shipType)) {
                        if (!this.myFleet.placeShip(new Coordinate(baseCoord % 10 - this.shipLength[shipType], baseCoord / 10), HORIZONTAL, shipType)) {
                            placed = false;
                        }
                    }
                }
            } //else pot the ships where they have shot least
            else {
                int[] placement = leastShotPlace(this.shipLength[shipType]);
                this.myFleet.placeShip(placement[0], placement[1], placement[2], shipType);
            }
        }

    }

    private int[] leastShotPlace(int shipLen) {
        int bestRect = -1;
        Coordinate rectCoord = null;
        int orientation = 1;
        //try best verticle rectangle
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j <= (10 - shipLen); j++) {
                int testRect = 0;
                boolean testOk = true;
                for (int k = shipLen - 1; k >= 0; k--) {

                    if (this.myShips[i][j + k]) {
                        testOk = false;
                        continue;
                    }
                    testRect = +this.theirShots[i][j + k];
                }
                if ((bestRect == -1 || testRect < bestRect) && testOk) {
                    bestRect = testRect;
                    rectCoord = new Coordinate(i, j);
                }
            }
        }
        //try best horizontal rectangle
        for (int i = 0; i <= (10 - shipLen); i++) {
            for (int j = 0; j < 10; j++) {
                int testRect = 0;
                boolean testOk = true;
                for (int k = shipLen - 1; k >= 0; k--) {
                    if (this.myShips[i + k][j]) {
                        testOk = false;
                        continue;
                    }
                    testRect = +this.theirShots[i + k][j];
                }
                if (testRect < bestRect && testOk) {
                    bestRect = testRect;
                    rectCoord = new Coordinate(i, j);
                    orientation = 0;
                }
            }
        }


        int[] ret = {rectCoord.getX(), rectCoord.getY(), orientation};
        //System.out.println(rectCoord.getX()+" "+rectCoord.getY()+" - "+orientation);
        if (orientation == 0) {
            for (int k = shipLen - 1; k >= 0; k--) {
                this.myShips[rectCoord.getX() + k][rectCoord.getY()] = true;
            }
        } else {
            for (int k = shipLen - 1; k >= 0; k--) {
                this.myShips[rectCoord.getX()][rectCoord.getY() + k] = true;
            }

        }
        return ret;
    }

    @Override
    public Fleet getFleet() {
        // TODO Auto-generated method stub.
        return this.myFleet;
    }

    private void shotHere(Coordinate c) {
        this.shotsHeat[c.getX()][c.getY()]++;
        this.theirGrid[c.getX()][c.getY()] = true;
    }

    @Override
    public Coordinate makeAttack() {
        if (this.availableShots.size() > 0) {
            //System.out.println("there were available shots!");
            this.lastShot = this.availableShots.get(0);
            this.availableShots.remove(0);
            shotHere(this.lastShot);
            return this.lastShot;
        } else {
            if (this.matchNumber > 50) {
                Coordinate shot = makeEducatedShot();
                this.lastShot = shot;
                shotHere(shot);
                return shot;
            } else {
                Coordinate shot = makeGuessShot();
                this.lastShot = shot;
                shotHere(shot);
                return shot;
            }
        }

    }

    /**
     * TODO Put here a description of what this method does.
     *
     * @return
     */
    private Coordinate makeGuessShot() {
        // TODO Auto-generated method stub.
        if (this.lastShot == null) {
            int guess = rGen.nextInt(100);
            while (!checkCoord(guess / 10, guess % 10)) {
                guess = rGen.nextInt(100);
            }
            return new Coordinate(guess / 10, guess % 10);
        } else {
            if (surroundCoord(this.lastShot) < 3) {
                if (checkCoord(this.lastShot.getX() + 1, this.lastShot.getY() - 1)) {
                    return new Coordinate(this.lastShot.getX() + 1, this.lastShot.getY() - 1);
                } else if (checkCoord(this.lastShot.getX() + 1, this.lastShot.getY() + 1)) {
                    return new Coordinate(this.lastShot.getX() + 1, this.lastShot.getY() + 1);
                } else if (checkCoord(this.lastShot.getX() - 1, this.lastShot.getY() + 1)) {
                    return new Coordinate(this.lastShot.getX() - 1, this.lastShot.getY() + 1);
                } else if (checkCoord(this.lastShot.getX() - 1, this.lastShot.getY() - 1)) {
                    return new Coordinate(this.lastShot.getX() - 1, this.lastShot.getY() - 1);
                } else {
                    int guess = rGen.nextInt(100);
                    while (!checkCoord(guess / 10, guess % 10)) {
                        guess = rGen.nextInt(100);
                    }
                    return new Coordinate(guess / 10, guess % 10);
                }
            } else {
                int guess = rGen.nextInt(100);
                while (!checkCoord(guess / 10, guess % 10)) {
                    guess = rGen.nextInt(100);
                }
                return new Coordinate(guess / 10, guess % 10);
            }
        }

    }

    /**
     * TODO Put here a description of what this method does.
     *
     * @param lastShot2
     * @return
     */
    private int surroundCoord(Coordinate last) {
        int retVal = 0;
        if (!checkCoord(last.getX() + 1, last.getY() + 1)) {
            retVal++;
        }
        if (!checkCoord(last.getX(), last.getY() + 1)) {
            retVal++;
        }
        if (!checkCoord(last.getX() - 1, last.getY() + 1)) {
            retVal++;
        }
        if (!checkCoord(last.getX() + 1, last.getY() - 1)) {
            retVal++;
        }
        if (!checkCoord(last.getX(), last.getY() - 1)) {
            retVal++;
        }
        if (!checkCoord(last.getX() - 1, last.getY() - 1)) {
            retVal++;
        }
        if (!checkCoord(last.getX() + 1, last.getY())) {
            retVal++;
        }
        if (!checkCoord(last.getX() - 1, last.getY())) {
            retVal++;
        }
        return retVal;
    }
    //make a shot based on enemy ship placements (covert intel)

    private Coordinate makeEducatedShot() {
        double heatFactor = 100.0 * (double) (this.matchNumber) / (double) (this.matchTotal);

        double[][] heat = new double[10][10];

        for (int s = 0; s < 5; s++) {
            if (this.enemyShips[s]) {
                int shipLen = this.shipLength[s];

                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j <= (10 - shipLen); j++) {
                        boolean testOk = true;
                        for (int k = shipLen - 1; k >= 0; k--) {
                            if (this.theirGrid[i][j + k]) {
                                testOk = false;
                                continue;
                            }
                        }
                        if (testOk) {
                            for (int k = shipLen - 1; k >= 0; k--) {
                                heat[i][j + k]++;
                            }
                        }
                    }
                }
                //try best horizontal rectangle
                for (int i = 0; i <= (10 - shipLen); i++) {
                    for (int j = 0; j < 10; j++) {
                        boolean testOk = true;
                        for (int k = shipLen - 1; k >= 0; k--) {
                            if (this.theirGrid[i + k][j]) {
                                testOk = false;
                                continue;
                            }
                        }
                        if (testOk) {
                            for (int k = shipLen - 1; k >= 0; k--) {
                                heat[i + k][j]++;
                            }
                        }
                    }
                }

            }
        }

        //apply factors
        for (int q = 0; q < 100; q++) {
            heat[q % 10][q / 10] *= this.avgHeat[q % 10][q / 10] * heatFactor;
        }
        double best = 0;
        int bestX = 0;
        int bestY = 0;
        for (int q = 0; q < 100; q++) {
            if (heat[q % 10][q / 10] > best) {
                best = heat[q % 10][q / 10];
                bestX = q % 10;
                bestY = q / 10;
            }
        }
        //for method error possibility
        if (!checkCoord(bestX, bestY)) {
            return makeGuessShot();
        }
        return new Coordinate(bestX, bestY);

    }

    /**
     * TODO Put here a description of what this method does.
     *
     * @param biggestSquare
     * @return
     */
    @Override
    public void resultOfAttack(int result) {
        if (result != MISS && result != 107) {

            this.hitsHeat[this.lastShot.getX()][this.lastShot.getY()]++;
            if (result >= 20) {
                this.hitShips.set(result % 20, null);
                this.availableShots.clear();

                //System.out.println("I sunk a ship!");
            } else {
                if (this.hitShips.get(result % 10) != null) {
                    if (this.hitShips.get(result % 10)[0].equals("1")) {
                        for (int gg = 3; gg < this.hitShips.get(result % 10).length; gg++) {
                            if (this.hitShips.get(result % 10)[gg].equals("f")) {
                                this.hitShips.get(result % 10)[gg] = "" + lastShot.getY();
                                //System.out.println("replaced an f");
                                break;
                            }
                        }
                    } else {
                        for (int gg = 3; gg < this.hitShips.get(result % 10).length; gg++) {
                            if (this.hitShips.get(result % 10)[gg].equals("f")) {
                                this.hitShips.get(result % 10)[gg] = "" + lastShot.getX();
                                //System.out.println("replaced an f");
                                break;
                            }
                        }
                    }
                } else {
                    int shipMod = result % 10;
                    if (shipMod == 0) {
                        this.hitShips.set(shipMod, new String[]{"0", "" + lastShot.getY(), "" + lastShot.getX(), "f"});
                    } else if (shipMod == 1 || shipMod == 2) {
                        this.hitShips.set(shipMod, new String[]{"0", "" + lastShot.getY(), "" + lastShot.getX(), "f", "f"});
                    } else if (shipMod == 3) {
                        this.hitShips.set(shipMod, new String[]{"0", "" + lastShot.getY(), "" + lastShot.getX(), "f", "f", "f"});
                    } else {
                        this.hitShips.set(shipMod, new String[]{"0", "" + lastShot.getY(), "" + lastShot.getX(), "f", "f", "f", "f"});
                    }

                }
            }
        }
        if (result != 107) {
            while (buildShots()) {
            };
        }
    }

    /**
     * TODO Put here a description of what this method does.
     *
     */
    /**
     * TODO Put here a description of what this method does.
     *
     * @return
     *
     */
    private boolean buildShots() {
        // TODO Auto-generated method stub.
        boolean goagain = false;
        this.availableShots.clear();
        //System.out.println("building some shots!");
        ArrayList<Integer> oneAways = new ArrayList<Integer>();
        for (int i = 0; i < 5; i++) {
            if (this.hitShips.get(i) != null) {
                String[] curShip = this.hitShips.get(i);
                //System.out.println("ship left: "+i);
                //check to make sure ship can fit in that direction if there is only one hit (check "f"'s)
                int fes = 0;
                for (String s : curShip) {
                    if (s.equals("f")) {
                        fes++;
                    }
                }

                //if the ship has only been hit once
                if (fes == curShip.length - 3) {
                    //System.out.println("ship was only hit once");
                    //right-left
                    if (curShip[0].equals("0")) {
                        //System.out.println("checking the R-L coords");
                        int y = Integer.parseInt(curShip[1]);
                        boolean moreright = true;
                        boolean moreleft = true;
                        int avcoords = 0;
                        for (int b = 1; b <= fes; b++) {
                            if (checkCoord(Integer.parseInt(curShip[2]) + b, y) && moreright) {
                                //System.out.println("I can go one more to the right");
                                this.availableShots.add(new Coordinate(Integer.parseInt(curShip[2]) + b, y));
                                avcoords++;
                                if (b == 1) {
                                    oneAways.add(this.availableShots.size() - 1);
                                }
                            } else {
                                moreright = false;
                            }
                            if (checkCoord(Integer.parseInt(curShip[2]) - b, y) && moreleft) {
                                //System.out.println("I can go one more to the left");
                                this.availableShots.add(new Coordinate(Integer.parseInt(curShip[2]) - b, y));
                                avcoords++;
                                if (b == 1) {
                                    oneAways.add(this.availableShots.size() - 1);
                                }
                            } else {
                                moreleft = false;
                            }
                        }
                        if (avcoords < fes) {
                            //System.out.println("There was not enough space for the ship this way");
                            for (int d = 0; d < avcoords; d++) {
                                if (oneAways.contains(this.availableShots.size() - 1)) {
                                    oneAways.remove((Integer) (this.availableShots.size() - 1));
                                }
                                this.availableShots.remove(this.availableShots.size() - 1);

                            }
                            curShip[0] = "1";
                            String middle = curShip[1];
                            curShip[1] = curShip[2];
                            curShip[2] = middle;
                            goagain = true;
                        }

                    } else {
                        int x = Integer.parseInt(curShip[1]);
                        boolean moreup = true;
                        boolean moredown = true;
                        int avcoords = 0;
                        for (int b = 1; b <= fes; b++) {
                            if (checkCoord(x, Integer.parseInt(curShip[2]) + b) && moredown) {
                                this.availableShots.add(new Coordinate(x, Integer.parseInt(curShip[2]) + b));
                                avcoords++;
                                if (b == 1) {
                                    oneAways.add(this.availableShots.size() - 1);
                                }
                            } else {
                                moredown = false;
                            }
                            if (checkCoord(x, Integer.parseInt(curShip[2]) - b) && moreup) {
                                this.availableShots.add(new Coordinate(x, Integer.parseInt(curShip[2]) - b));
                                avcoords++;
                                if (b == 1) {
                                    oneAways.add(this.availableShots.size() - 1);
                                }
                            } else {
                                moreup = false;
                            }
                        }
                        if (avcoords < fes) {
                            for (int d = 0; d < avcoords; d++) {
                                if (oneAways.contains(this.availableShots.size() - 1)) {
                                    oneAways.remove((Integer) (this.availableShots.size() - 1));
                                }
                                this.availableShots.remove(this.availableShots.size() - 1);
                            }
                            curShip[0] = "0";
                            String middle = curShip[1];
                            curShip[1] = curShip[2];
                            curShip[2] = middle;
                            goagain = true;
                        }
                    }

                } //already hit more than once, add which shots you can still make
                else {
                    //System.out.println("ship's been hit more than once");
                    int min = Integer.parseInt(curShip[2]);
                    int max = Integer.parseInt(curShip[2]);
                    for (int ah = 3; ah < curShip.length; ah++) {
                        if (!curShip[ah].equals("f")) {
                            if (Integer.parseInt(curShip[ah]) < min) {
                                min = Integer.parseInt(curShip[ah]);
                            }
                            if (Integer.parseInt(curShip[ah]) > max) {
                                max = Integer.parseInt(curShip[ah]);
                            }
                        }
                    }
                    if (curShip[0].equals("0")) {
                        int y = Integer.parseInt(curShip[1]);
                        if (checkCoord(min - 1, y)) {
                            this.availableShots.add(new Coordinate(min - 1, y));
                            oneAways.add(this.availableShots.size() - 1);
                        }
                        if (checkCoord(max + 1, y)) {
                            this.availableShots.add(new Coordinate(max + 1, y));
                            oneAways.add(this.availableShots.size() - 1);
                        }
                    } else {
                        int x = Integer.parseInt(curShip[1]);
                        if (checkCoord(x, min - 1)) {
                            this.availableShots.add(new Coordinate(x, min - 1));
                            oneAways.add(this.availableShots.size() - 1);
                        }
                        if (checkCoord(x, max + 1)) {
                            this.availableShots.add(new Coordinate(x, max + 1));
                            oneAways.add(this.availableShots.size() - 1);
                        }
                    }
                }

                //else you can do nothing
                //else switch direction and add those spots to possible hits.

            }
        }
        for (Integer index : oneAways) {
            if (this.avgHeat[this.availableShots.get(index).getX()][this.availableShots.get(index).getY()] > this.avgHeat[this.availableShots.get(0).getX()][this.availableShots.get(0).getY()]) {
                this.availableShots.add(0, this.availableShots.remove(index.intValue()));
            }
        }


        return goagain;
    }

    /**
     * TODO Put here a description of what this method does.
     *
     * @param i
     * @param y
     * @return
     */
    private boolean checkCoord(int x, int y) {
        // TODO Auto-generated method stub.
        if (x < 10 && y < 10 && x >= 0 && y >= 0) {
            if (!this.theirGrid[x][y]) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        // TODO Auto-generated method stub.
        this.theirShots[coord.getX()][coord.getY()]++;

    }

    @Override
    //recalculates the heat of each cell
    public void resultOfGame(int result) {
        for (int i = 0; i < 100; i++) {
            if (this.shotsHeat[i / 10][i % 10] != 0) {
                this.avgHeat[i / 10][i % 10] = (double) (this.hitsHeat[i / 10][i % 10]) / (double) (this.shotsHeat[i / 10][i % 10]);
            } else {
                this.avgHeat[i / 10][i % 10] = 0;
            }
        }

    }
}
