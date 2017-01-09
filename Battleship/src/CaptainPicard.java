
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * TODO Put here a description of what this class does.
 *
 * @author John. Created Mar 18, 2013.
 */
public class CaptainPicard implements Captain, Constants {

    //where the opponent has shot
    private int[][] theirShots = new int[10][10];
    //a map of where i have hit ships
    private int[][] hitsHeat = new int[10][10];
    //a map of where i have shot
    private int[][] shotsHeat = new int[10][10];
    // a map of where I have put my ships
    private int[][] placeHeat = new int[10][10];
    //an array of battleship lengths
    private int[] shipLength = {2, 3, 3, 4, 5};
    //the current match number
    private int matchNumber = 0;
    //the total number of matches being played
    private int matchTotal;
    //a seed for ship placing
    private int seed;
    //current win rate
    private int wins = 0;
    //my heatmap
    private double[][] avgHeat = new double[10][10];
    //my heat factor
    private double heatFactor = 0;
    //the best current horizontal enemy ship position
    private double cur_ver = 0;
    //the best verticle enemy ship position
    private double cur_hor = 0;
    //the grid that I am sooting on, to be sure i dont shoot the same spot twice
    private boolean[][] theirGrid = new boolean[10][10];
    //a map of where my ships are, used for placement
    private boolean[][] myShips = new boolean[10][10];
    //the status of the enemy ships 
    private boolean[] enemyShips = new boolean[5];
    //more detailed stats of the enemy ship
    private ArrayList<String[]> hitShips;
    //the shots that I can make (based on hitting ships)
    private ArrayList<Coordinate> availableShots = new ArrayList<Coordinate>();
    //the spot where i last fired
    private Coordinate lastShot;
    //the name of my opponent
    private String lastOpp = "";
    // a random number generator
    private Random rGen;
    //my fleet object
    private Fleet myFleet;

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {

        this.lastShot = null;
        this.matchTotal = numMatches;

        //reset enemy grid
        for (boolean[] b : this.theirGrid) {
            Arrays.fill(b, false);
        }
        //true mens enemy ship is still alive
        Arrays.fill(this.enemyShips, true);
        //reset hittign shits
        this.hitShips = new ArrayList<String[]>();
        for (int g = 0; g < 5; g++) {
            this.hitShips.add(null);
        }
        //Create a new generator
        this.rGen = new Random();
        //match count increment
        if (opponent.equals(this.lastOpp)) {
            this.matchNumber++;
            if (this.matchNumber % 10000 == 0) {
                double win_rate = (double) this.wins / (double) 10000;
                if (win_rate < .51) {
                    this.seed++;
                    this.seed = this.seed % 3;
                }
                this.wins = 0;
            }
        } //else reset all the opponent data
        else {
            this.theirShots = new int[10][10];
            this.matchNumber = 0;
            this.shotsHeat = new int[10][10];
            this.hitsHeat = new int[10][10];
            this.avgHeat = new double[10][10];
            this.lastOpp = opponent;
            this.seed = 2;
            //reset where i've been placing ships
            for (int[] b : this.placeHeat) {
                Arrays.fill(b, 0);
            }

        }
        //reset a map of where my ships are
        for (boolean[] b : this.myShips) {
            Arrays.fill(b, false);
        }


        //make a new fleet
        this.myFleet = new Fleet();

        placeShips();

        this.heatFactor = 100.0 * (double) (this.matchNumber) / (double) (this.matchTotal);
    }

    private void placeShips() {

        switch (this.seed) {
            case 0:
                evenDistributeTouchingPlace();
                break;
            case 1:
                evenDistributePlace();
                break;
            case 2:
                learningPlace();
                break;
            default:
                evenDistributeTouchingPlace();
                break;
        }

    }

    private boolean placeShip(int x, int y, int direc, int shipType) {

        if (this.myFleet.placeShip(x, y, direc, shipType)) {
            int shipLen = this.shipLength[shipType];
            if (direc == 0) {
                for (int k = 0; k < shipLen; k++) {
                    this.myShips[x + k][y] = true;
                    this.placeHeat[x + k][y]++;
                }
            } else {
                for (int k = 0; k < shipLen; k++) {
                    this.myShips[x][y + k] = true;
                    this.placeHeat[x][y + k]++;
                }

            }
            return true;
        }
        return false;



    }

    private boolean isShip(int x, int y) {
        if (x < 10 && x >= 0 && y < 10 && y >= 0) {
            return this.myShips[x][y];
        }
        return false;

    }

    private void learningPlace() {
        for (int shipType = 0; shipType < 5; shipType++) {
            int[] placement = leastShotPlace(this.shipLength[shipType]);
            placeShip(placement[0], placement[1], placement[2], shipType);
        }
    }

    private void evenDistributePlace() {

        if (this.matchNumber > 100) {
            //a little random to avoid repeating same pattern
            int r = this.rGen.nextInt(2);

            for (int shipType = 0; shipType < 5; shipType++) {

                int shipLen = this.shipLength[shipType];
                int bestRect = -1;
                Coordinate rectCoord = null;

                if (r == 1) {
                    //try best verticle rectangle
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j <= (10 - shipLen); j++) {
                            int testRect = 0;
                            boolean testOk = true;
                            for (int k = 0; k < shipLen; k++) {

                                if (this.myShips[i][j + k]) {
                                    testOk = false;
                                    break;
                                }
                                testRect = +this.placeHeat[i][j + k];
                            }
                            if (testOk && (bestRect == -1 || testRect < bestRect)) {
                                bestRect = testRect;
                                rectCoord = new Coordinate(i, j);
                            }
                        }
                    }

                } else {
                    //try best horizontal rectangle
                    for (int i = 0; i <= (10 - shipLen); i++) {
                        for (int j = 0; j < (10); j++) {
                            int testRect = 0;
                            boolean testOk = true;
                            for (int k = 0; k < shipLen; k++) {
                                if (this.myShips[i + k][j]) {
                                    testOk = false;
                                    break;
                                }
                                testRect = +this.placeHeat[i + k][j];
                            }
                            if (testOk && (bestRect == -1 || testRect < bestRect)) {
                                bestRect = testRect;
                                rectCoord = new Coordinate(i, j);
                            }
                        }
                    }

                }

                //boolean return ignored. Should always be ok, since methods above check
                placeShip(rectCoord.getX(), rectCoord.getY(), r, shipType);

            }
        } else {
            notTouchingPlace();
        }

    }

    private void evenDistributeTouchingPlace() {

        if (this.matchNumber > 100) {
            //a little random to avoid repeating same pattern
            int r = this.rGen.nextInt(2);

            for (int shipType = 0; shipType < 5; shipType++) {

                int shipLen = this.shipLength[shipType];
                int bestRect = -1;
                Coordinate rectCoord = null;

                if (r == 1) {
                    //try best verticle rectangle
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j <= (10 - shipLen); j++) {
                            int testRect = 0;
                            boolean testOk = true;
                            for (int k = 0; k < shipLen; k++) {

                                if (this.myShips[i][j + k]) {
                                    testOk = false;
                                    break;
                                }
                                if (i == 0) {
                                    if (this.myShips[i + 1][j + k]) {
                                        testOk = false;
                                        break;
                                    }
                                } else if (i == 9) {
                                    if (this.myShips[i - 1][j + k]) {
                                        testOk = false;
                                        break;
                                    }
                                } else {
                                    if (this.myShips[i + 1][j + k] || this.myShips[i - 1][j + k]) {
                                        testOk = false;
                                        break;
                                    }
                                }
                                testRect = +this.placeHeat[i][j + k];
                            }
                            if (testOk && (bestRect == -1 || testRect < bestRect)) {
                                bestRect = testRect;
                                rectCoord = new Coordinate(i, j);
                            }
                        }
                    }

                }
                if (bestRect == -1) {
                    r = 0;
                }
                if (r == 0) {
                    //try best horizontal rectangle
                    for (int i = 0; i <= (10 - shipLen); i++) {
                        for (int j = 0; j < (10); j++) {
                            int testRect = 0;
                            boolean testOk = true;
                            for (int k = 0; k < shipLen; k++) {
                                if (this.myShips[i + k][j]) {
                                    testOk = false;
                                    break;
                                }
                                if (j == 0) {
                                    if (this.myShips[i + k][j + 1]) {
                                        testOk = false;
                                        break;
                                    }
                                } else if (j == 9) {
                                    if (this.myShips[i + k][j - 1]) {
                                        testOk = false;
                                        break;
                                    }
                                } else {
                                    if (this.myShips[i + k][j + 1] || this.myShips[i + k][j - 1]) {
                                        testOk = false;
                                        break;
                                    }
                                }
                                testRect = +this.placeHeat[i + k][j];
                            }
                            if (testOk && (bestRect == -1 || testRect < bestRect)) {
                                bestRect = testRect;
                                rectCoord = new Coordinate(i, j);
                            }
                        }
                    }

                }
                if (bestRect == -1) {
                    r = 1;
                    shipType--;
                } else {
                    //boolean return ignored. Should always be ok, since methods above check
                    placeShip(rectCoord.getX(), rectCoord.getY(), r, shipType);
                }

            }
        } else {
            notTouchingPlace();
        }

    }

    private void notTouchingPlace() {
        for (int shipType = 0; shipType < 5; shipType++) {
            boolean placed = false;
            int t_counter = 0;
            while (!placed) {

                if (t_counter > 300) {
                    int x = this.rGen.nextInt(10);
                    int y = this.rGen.nextInt(10);
                    int z = this.rGen.nextInt(2);
                    while (!this.myFleet.placeShip(x, y, z, shipType)) {
                        x = this.rGen.nextInt(10);
                        y = this.rGen.nextInt(10);
                        z = this.rGen.nextInt(2);
                    }
                    placed = true;
                } else {
                    //true means that there are no touching ships
                    boolean surround = true;
                    int x = this.rGen.nextInt(10);
                    int y = this.rGen.nextInt(10);

                    if (x == 0 || x == 9 || y == 0 || y == 9) {
                        if (x == 0) {
                            //try y coords (up/down)
                            for (int i = 0; i < this.shipLength[shipType]; i++) {

                                if (isShip(x, y + i) || isShip(x + 1, y + i)) {
                                    surround = false;
                                    break;
                                }
                            }
                            if (isShip(x, y - 1) || isShip(x, y + this.shipLength[shipType])) {
                                surround = false;
                            }
                            if (surround && checkCoord(x, y) && checkCoord(x, y + this.shipLength[shipType] - 1)) {
                                if (placeShip(x, y, VERTICAL, shipType)) {
                                    placed = true;
                                }
                            }
                        } else if (x == 9) {
                            //try y coords (up/down)
                            for (int i = 0; i < this.shipLength[shipType]; i++) {

                                if (isShip(x, y + i) || isShip(x - 1, y + i)) {
                                    surround = false;
                                    break;
                                }
                            }
                            if (isShip(x, y - 1) || isShip(x, y + this.shipLength[shipType])) {
                                surround = false;
                            }
                            if (surround && checkCoord(x, y) && checkCoord(x, y + this.shipLength[shipType] + 1)) {
                                if (placeShip(x, y, VERTICAL, shipType)) {
                                    placed = true;
                                }
                            }
                        } else if (y == 0) {
                            for (int i = 0; i < this.shipLength[shipType]; i++) {

                                if (isShip(x + i, y) || isShip(x + i, y + 1)) {
                                    surround = false;
                                    break;
                                }
                            }
                            if (isShip(x - 1, y) || isShip(x + this.shipLength[shipType], y)) {
                                surround = false;
                            }
                            if (surround && checkCoord(x, y) && checkCoord(x + this.shipLength[shipType] - 1, y)) {
                                if (placeShip(x, y, HORIZONTAL, shipType)) {
                                    placed = true;
                                }
                            }
                        } else {
                            for (int i = 0; i < this.shipLength[shipType]; i++) {

                                if (isShip(x + i, y) || isShip(x + i, y - 1)) {
                                    surround = false;
                                    break;
                                }
                            }
                            if (isShip(x - 1, y) || isShip(x + this.shipLength[shipType], y)) {
                                surround = false;
                            }
                            if (surround && checkCoord(x, y) && checkCoord(x + this.shipLength[shipType] - 1, y)) {
                                if (placeShip(x, y, HORIZONTAL, shipType)) {
                                    placed = true;
                                }
                            }
                        }
                    } else {

                        //try x coords (right/left)
                        for (int i = 0; i < this.shipLength[shipType]; i++) {

                            if (isShip(x + i, y) || isShip(x + i, y - 1) || isShip(x + i, y + 1)) {
                                surround = false;
                                break;
                            }
                        }
                        if (isShip(x - 1, y) || isShip(x + this.shipLength[shipType], y)) {
                            surround = false;
                        }
                        if (surround && checkCoord(x, y) && checkCoord(x + this.shipLength[shipType] - 1, y)) {
                            if (placeShip(x, y, HORIZONTAL, shipType)) {
                                placed = true;
                            }
                        }
                        //try y coords (up/down)
                        for (int i = 0; i < this.shipLength[shipType]; i++) {

                            if (isShip(x, y + i) || isShip(x - 1, y + i) || isShip(x + 1, y + i)) {
                                surround = false;
                                break;
                            }
                        }
                        if (isShip(x, y - 1) || isShip(x, y + this.shipLength[shipType])) {
                            surround = false;
                        }
                        if (surround && checkCoord(x, y) && checkCoord(x, y + this.shipLength[shipType] - 1)) {
                            if (placeShip(x, y, VERTICAL, shipType)) {
                                placed = true;
                            }
                        }
                    }
                }

                t_counter++;
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
                for (int k = 0; k < shipLen; k++) {

                    if (this.myShips[i][j + k]) {
                        testOk = false;
                        break;
                    }
                    testRect = +this.theirShots[i][j + k];
                }
                if (testOk && (bestRect == -1 || testRect < bestRect)) {
                    bestRect = testRect;
                    rectCoord = new Coordinate(i, j);
                }
            }
        }
        //try best horizontal rectangle
        for (int i = 0; i <= (10 - shipLen); i++) {
            for (int j = 0; j < (10); j++) {
                int testRect = 0;
                boolean testOk = true;
                for (int k = 0; k < shipLen; k++) {
                    if (this.myShips[i + k][j]) {
                        testOk = false;
                        break;
                    }
                    testRect = +this.theirShots[i + k][j];
                }
                if (testOk && testRect < bestRect) {
                    bestRect = testRect;
                    rectCoord = new Coordinate(i, j);
                    orientation = 0;
                }
            }
        }


        int[] ret = {rectCoord.getX(), rectCoord.getY(), orientation};
        //System.out.println(rectCoord.getX()+" "+rectCoord.getY()+" - "+orientation);
        return ret;
    }

    @Override
    public Fleet getFleet() {
        return this.myFleet;
    }

    private void shotHere(Coordinate c) {
        this.shotsHeat[c.getX()][c.getY()]++;
        this.theirGrid[c.getX()][c.getY()] = true;
    }

    @Override
    public Coordinate makeAttack() {
        //System.out.println(this.hitShips);
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
        double best = -1;
        int bestX = 0;
        int bestY = 0;

        for (int q = 0; q < 100; q++) {
            heat[q % 10][q / 10] *= this.avgHeat[q % 10][q / 10] * this.heatFactor;

            if (heat[q % 10][q / 10] > best) {
                best = heat[q % 10][q / 10];
                bestX = q % 10;
                bestY = q / 10;
            }
        }


        //for method error possibility (early game)
        if (!checkCoord(bestX, bestY)) {
            return makeGuessShot();
        }
        return new Coordinate(bestX, bestY);

    }

    @Override
    public void resultOfAttack(int result) {
        if (result != MISS) {
            this.hitsHeat[this.lastShot.getX()][this.lastShot.getY()]++;
            if (result >= 20) {
                this.hitShips.set(result % 20, null);
                this.availableShots.clear();

            } else {
                //ship hit before
                if (this.hitShips.get(result % 10) != null) {
                    //ship is verticle
                    this.hitShips.get(result % 10)[4] = String.valueOf(Integer.parseInt(this.hitShips.get(result % 10)[4]) + 1);
                    this.hitShips.get(result % 10)[5] = "" + lastShot.getX();
                    this.hitShips.get(result % 10)[6] = "" + lastShot.getY();
                    if (this.hitShips.get(result % 10)[0].equals("1")) {
                        if (this.lastShot.getX() != Integer.parseInt(this.hitShips.get(result % 10)[1])) {
                            //if ship hit horizontal, but was supposed to be verticle, set to horizontal.
                            this.hitShips.get(result % 10)[0] = "0";

                        }

                    } //horizontal
                    else {
                        if (this.lastShot.getY() != Integer.parseInt(this.hitShips.get(result % 10)[2])) {
                            this.hitShips.get(result % 10)[0] = "1";
                        }
                    }
                } //ship never hit before
                else {
                    int shipMod = result % 10;
                    if (shipMod == 0) {
                        this.hitShips.set(shipMod, new String[]{"0", "" + lastShot.getX(), "" + lastShot.getY(), "2", "1", "" + lastShot.getX(), "" + lastShot.getY()});
                    } else if (shipMod == 1 || shipMod == 2) {
                        this.hitShips.set(shipMod, new String[]{"0", "" + lastShot.getX(), "" + lastShot.getY(), "3", "1", "" + lastShot.getX(), "" + lastShot.getY()});
                    } else if (shipMod == 3) {
                        this.hitShips.set(shipMod, new String[]{"0", "" + lastShot.getX(), "" + lastShot.getY(), "4", "1", "" + lastShot.getX(), "" + lastShot.getY()});
                    } else {
                        this.hitShips.set(shipMod, new String[]{"0", "" + lastShot.getX(), "" + lastShot.getY(), "5", "1", "" + lastShot.getX(), "" + lastShot.getY()});
                    }

                }
            }


        }

        buildShots();

    }

    private void buildShots() {
        this.availableShots.clear();
        //System.out.println("building some shots!");
        //indicies of available shots that are 1 away form a hit
        for (int i = 0; i < 5; i++) {
            if (this.hitShips.get(i) != null) {
                String[] curShip = this.hitShips.get(i);
                //check to make sure ship can fit in that direction if there is only one hit
                int ship_left = Integer.parseInt(curShip[3]) - Integer.parseInt(curShip[4]);
                //if the ship has only been hit once, no additional numbers.
                if (curShip[4].equals("1")) {
                    //right-left

                    int y = Integer.parseInt(curShip[2]);
                    int x = Integer.parseInt(curShip[1]);
                    int rspaces = 0;
                    int lspaces = 0;
                    int tspaces = 0;
                    int bspaces = 0;

                    for (int j = -1; j < 2; j += 2) {
                        if (checkCoord(x + (1 * j), y)) {

                            if (j == -1) {
                                lspaces++;
                            } else {
                                rspaces++;
                            }
                            if ((ship_left > 1) && checkCoord(x + (2 * j), y)) {
                                if (j == -1) {
                                    lspaces++;
                                } else {
                                    rspaces++;
                                }
                                if ((ship_left > 2) && checkCoord(x + (3 * j), y)) {
                                    if (j == -1) {
                                        lspaces++;
                                    } else {
                                        rspaces++;
                                    }
                                    if ((ship_left > 3) && checkCoord(x + (4 * j), y)) {
                                        if (j == -1) {
                                            lspaces++;
                                        } else {
                                            rspaces++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (int j = -1; j < 2; j += 2) {
                        if (checkCoord(x, y + (1 * j))) {
                            if (j == -1) {
                                tspaces++;
                            } else {
                                bspaces++;
                            }
                            if ((ship_left > 1) && checkCoord(x, y + (2 * j))) {
                                if (j == -1) {
                                    tspaces++;
                                } else {
                                    bspaces++;
                                }
                                if ((ship_left > 2) && checkCoord(x, y + (3 * j))) {
                                    if (j == -1) {
                                        tspaces++;
                                    } else {
                                        bspaces++;
                                    }
                                    if ((ship_left > 3) && checkCoord(x, y + (4 * j))) {
                                        if (j == -1) {
                                            tspaces++;
                                        } else {
                                            bspaces++;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Coordinate hor = null;
                    Coordinate vert = null;
                    this.cur_hor = 0;
                    this.cur_ver = 0;

                    if ((rspaces + lspaces) >= ship_left) {

                        hor = find_best_fit(x, y, lspaces, rspaces, ship_left, 0);
                    }

                    if ((tspaces + bspaces) >= ship_left) {

                        vert = find_best_fit(x, y, tspaces, bspaces, ship_left, 1);
                    }

                    if (this.cur_ver > this.cur_hor) {
                        this.availableShots.add(0, new Coordinate(vert.getX(), vert.getY()));
                    } else if (this.cur_hor > this.cur_ver) {
                        this.availableShots.add(0, new Coordinate(hor.getX(), hor.getY()));
                    } else {
                        if (vert != null) {
                            this.availableShots.add(0, new Coordinate(vert.getX(), vert.getY()));
                        } else {
                            this.availableShots.add(0, new Coordinate(hor.getX(), hor.getY()));
                        }
                    }

                } //already hit more than once, add which shots you can still make
                else {

                    int ship_length = Integer.parseInt(curShip[3]);
                    if (curShip[0] == "0") {
                        //shot was in +x direc
                        int last_x = Integer.parseInt(curShip[5]);
                        int last_y = Integer.parseInt(curShip[6]);
                        int first_x = Integer.parseInt(curShip[1]);
                        if (last_x > first_x) {
                            if ((checkCoord(last_x + 1, last_y)) && (last_x < (first_x + ship_length))) {
                                this.availableShots.add(0, new Coordinate(last_x + 1, last_y));
                            } //ship hit at both ends 
                            else {
                                for (int k = 0; k < Integer.parseInt(curShip[3]); k++) {
                                    if (checkCoord(last_x - k, last_y)) {
                                        this.availableShots.add(0, new Coordinate(last_x - k, last_y));
                                        break;
                                    }
                                }
                            }
                        } //ship was hit in -x direction
                        else {
                            if ((checkCoord(last_x - 1, last_y)) && (last_x > (first_x - ship_length))) {
                                this.availableShots.add(0, new Coordinate(last_x - 1, last_y));
                            } //ship hit at both ends 
                            else {
                                for (int k = 0; k < Integer.parseInt(curShip[3]); k++) {
                                    if (checkCoord(last_x + k, last_y)) {
                                        this.availableShots.add(0, new Coordinate(last_x + k, last_y));
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        //shot was in +y direc
                        int last_x = Integer.parseInt(curShip[5]);
                        int last_y = Integer.parseInt(curShip[6]);
                        int first_y = Integer.parseInt(curShip[2]);
                        if (last_y > first_y) {
                            if ((checkCoord(last_x, last_y + 1)) && (last_y < (first_y + ship_length))) {
                                this.availableShots.add(0, new Coordinate(last_x, last_y + 1));
                            } //ship hit at both ends 
                            else {
                                for (int k = 0; k < Integer.parseInt(curShip[3]); k++) {
                                    if (checkCoord(last_x, last_y - k)) {
                                        this.availableShots.add(0, new Coordinate(last_x, last_y - k));
                                        break;
                                    }
                                }
                            }
                        } //ship was hit in -y direction
                        else {
                            if ((checkCoord(last_x, last_y - 1)) && (last_y > (first_y - ship_length))) {
                                this.availableShots.add(0, new Coordinate(last_x, last_y - 1));
                            } //ship hit at both ends 
                            else {
                                for (int k = 0; k < Integer.parseInt(curShip[3]); k++) {
                                    if (checkCoord(last_x, last_y + k)) {
                                        this.availableShots.add(0, new Coordinate(last_x, last_y + k));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //only get called for a ship hit 1 time
    private Coordinate find_best_fit(int x, int y, int neg_spaces, int pos_spaces, int needed_spaces, int i) {

        //horizontal
        if (i == 0) {

            double best_rect_val = 0;
            Coordinate best_rect_start = null;
            if (neg_spaces > 0) {
                best_rect_start = new Coordinate(x - 1, y);
            } else {
                best_rect_start = new Coordinate(x + 1, y);
            }

            for (int j = (x - neg_spaces); j < (x + pos_spaces - needed_spaces); j++) {
                double rect_value = 0;
                for (int k = 0; k < needed_spaces; k++) {
                    rect_value += (this.avgHeat[k + j][y] * this.heatFactor);
                }

                if (rect_value > best_rect_val) {
                    best_rect_val = rect_value;
                    best_rect_start = new Coordinate(j, y); //rect contnues in +x direction
                }
            }
            this.cur_hor = best_rect_val;
            //ship pinning
            if (needed_spaces > neg_spaces) {
                return new Coordinate(x + 1, y);
            } else if (needed_spaces > pos_spaces) {
                return new Coordinate(x - 1, y);
            }
            /////
            if (best_rect_start.getX() < (x - 1)) {
                return new Coordinate(x - 1, y);
            } else if (pos_spaces > 0) {
                return new Coordinate(x + 1, y);
            } else {
                return new Coordinate(x - 1, y);
            }
        } //verticle
        else {

            double best_rect_val = 0;
            Coordinate best_rect_start = null;
            if (neg_spaces > 0) {
                best_rect_start = new Coordinate(x, y - 1);
            } else {
                best_rect_start = new Coordinate(x, y + 1);
            }
            for (int j = (y - neg_spaces); j < (y + pos_spaces - needed_spaces); j++) {
                double rect_value = 0;
                for (int k = 0; k < needed_spaces; k++) {
                    rect_value += (this.avgHeat[x][k + j] * this.heatFactor);
                }

                if (rect_value > best_rect_val) {
                    best_rect_val = rect_value;
                    best_rect_start = new Coordinate(x, j); //rect contnues in +x direction
                }
            }
            this.cur_ver = best_rect_val;

            //ship pinning
            if (needed_spaces > neg_spaces) {
                return new Coordinate(x, y + 1);
            } else if (needed_spaces > pos_spaces) {
                return new Coordinate(x, y - 1);
            }
            //
            if (best_rect_start.getY() < (y - 1)) {
                return new Coordinate(x, y - 1);
            } else if (pos_spaces > 0) {
                return new Coordinate(x, y + 1);
            } else {
                return new Coordinate(x, y - 1);
            }
        }
    }

    private boolean checkCoord(int x, int y) {
        if (x < 10 && y < 10 && x >= 0 && y >= 0) {
            if (!this.theirGrid[x][y]) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        this.theirShots[coord.getX()][coord.getY()]++;
    }

    @Override
    //recalculates the heat of each cell
    public void resultOfGame(int result) {
        if (result == WON) {
            this.wins++;
        }
        for (int i = 0; i < 100; i++) {
            if (this.shotsHeat[i / 10][i % 10] != 0) {
                this.avgHeat[i / 10][i % 10] = (double) (this.hitsHeat[i / 10][i % 10]) / (double) (this.shotsHeat[i / 10][i % 10]);
            } else {
                this.avgHeat[i / 10][i % 10] = 0;
            }
        }

    }
}
