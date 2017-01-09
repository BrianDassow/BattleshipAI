
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class CaptainLana implements Captain, Constants {

    protected Random generator;
    protected Fleet myFleet;
    private int[] shipLength = {2, 3, 3, 4, 5};
    private boolean[] shipsAlive;
    private boolean[][] myMatchShots;
    private String lastOpponent = "";
    private double[][][] hitsHeat;
    private int[][][] myHits;
    private int[][] myShots;
    private int[][][] myShipPlaces;
    private Coordinate lastShot;
    private ArrayList<ArrayList<Coordinate>> hitShips;
    private ArrayList<ArrayList<Placement>> theirPlacements;
    private ArrayList<ArrayList<Placement>> myPlacements;
    private Coordinate[] lastFleet;
    private int[] lastDirecs;
    private boolean wasWin;
    private int turnNum;

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {


        this.theirPlacements = new ArrayList<ArrayList<Placement>>();
        this.myPlacements = new ArrayList<ArrayList<Placement>>();
        this.turnNum = 0;
        this.generator = new Random();
        this.myFleet = new Fleet();
        this.shipsAlive = new boolean[]{true, true, true, true, true};
        this.myMatchShots = new boolean[10][10];
        this.hitShips = new ArrayList<ArrayList<Coordinate>>();

        for (int i = 0; i < 10; i++) {
            Arrays.fill(this.myMatchShots[i], false);
        }

        if (!opponent.equals(this.lastOpponent)) {
            this.wasWin = false;
            this.myHits = new int[10][10][5];
            this.myShots = new int[10][10];
            this.lastOpponent = opponent;
            this.hitsHeat = new double[10][10][5];
            this.lastDirecs = new int[5];
            this.lastFleet = new Coordinate[5];
            this.myShipPlaces = new int[10][10][5];
            for (int j = 0; j < 100; j++) {
                this.myShots[j % 10][j / 10] = 2;
                for (int k = 0; k < 5; k++) {
                    this.myHits[j % 10][j / 10][k] = 1;
                }
            }
        }
        for (int s = 0; s < 5; s++) {
            for (int i = 0; i < 100; i++) {
                this.hitsHeat[i % 10][i / 10][s] = (double) this.myHits[i % 10][i / 10][s] / (double) this.myShots[i % 10][i / 10];
            }
        }
        createPlacements(this.hitsHeat, this.theirPlacements);
        createPlacements(this.myShipPlaces, this.myPlacements);
        disPlace(myFleet);
    }

    private void disPlace(Fleet flt) {
        if (!this.wasWin) {
            for (int i = 0; i < 5; i++) {
                int direc = this.myPlacements.get(i).get(0).direc;
                Coordinate pos = this.myPlacements.get(i).get(0).coords[0];
                double best = this.myPlacements.get(i).get(0).score;
                Coordinate[] coords = this.myPlacements.get(i).get(0).coords;
                for (Placement p : this.myPlacements.get(i)) {
                    if (p.score < best) {
                        best = p.score;
                        pos = p.coords[0];
                        direc = p.direc;
                        coords = p.coords;
                    }
                }
                this.lastDirecs[i] = direc;
                this.lastFleet[i] = pos;
                flt.placeShip(pos, direc, i);
                for (Coordinate mark : coords) {
                    this.myShipPlaces[mark.getX()][mark.getY()][i]++;
                }
                for (int j = i; j < 5; j++) {
                    ArrayList<Placement> bad = new ArrayList<Placement>();
                    for (Placement other : this.myPlacements.get(j)) {
                        for (Coordinate no_good : coords) {
                            if (other.contains(no_good)) {
                                bad.add(other);
                            }
                        }
                    }
                    this.myPlacements.get(j).removeAll(bad);
                }
            }
        } else {
            for (int i = 0; i < 5; i++) {
                flt.placeShip(this.lastFleet[i], this.lastDirecs[i], i);
            }
        }
    }

    private void createPlacements(double[][][] scorer, ArrayList<ArrayList<Placement>> list) {
        for (int s = 0; s < 5; s++) {
            int shipLen = this.shipLength[s];
            ArrayList<Placement> places = new ArrayList<Placement>();
            for (int i = 0; i < 11 - shipLen; i++) {
                for (int j = 0; j < 10; j++) {
                    Placement p = new Placement(0, s, new Coordinate(i, j));
                    double score = 0;

                    for (int k = 0; k < shipLen; k++) {
                        score += scorer[i + k][j][s];
                    }
                    p.score = score;
                    places.add(p);
                }
            }
            for (int j = 0; j < 11 - shipLen; j++) {
                for (int i = 0; i < 10; i++) {
                    Placement p = new Placement(1, s, new Coordinate(i, j));
                    double score = 0;

                    for (int k = 0; k < shipLen; k++) {
                        score += scorer[i][j + k][s];
                    }
                    p.score = score;
                    places.add(p);
                }
            }
            list.add(places);
        }
    }

    private void createPlacements(int[][][] scorer, ArrayList<ArrayList<Placement>> list) {
        for (int s = 0; s < 5; s++) {
            int shipLen = this.shipLength[s];
            ArrayList<Placement> places = new ArrayList<Placement>();
            for (int i = 0; i < 11 - shipLen; i++) {
                for (int j = 0; j < 10; j++) {
                    Placement p = new Placement(0, s, new Coordinate(i, j));
                    double score = 0;

                    for (int k = 0; k < shipLen; k++) {
                        score += scorer[i + k][j][s];
                    }
                    p.score = score;
                    places.add(p);
                }
            }
            for (int j = 0; j < 11 - shipLen; j++) {
                for (int i = 0; i < 10; i++) {
                    Placement p = new Placement(1, s, new Coordinate(i, j));
                    double score = 0;

                    for (int k = 0; k < shipLen; k++) {
                        score += scorer[i][j + k][s];
                    }
                    p.score = score;
                    places.add(p);
                }
            }
            list.add(places);
        }
    }

    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    @Override
    public Coordinate makeAttack() {
        this.turnNum++;
        Coordinate shot = new Coordinate(0, 0);
        ArrayList<Coordinate> possibles = new ArrayList<Coordinate>();
        if (!this.hitShips.isEmpty()) {
            for (ArrayList<Coordinate> ship : this.hitShips) {
                int shipMod = ship.get(0).getX();
                ArrayList<Placement> places = this.theirPlacements.get(shipMod);
                for (Placement p : places) {
                    for (Coordinate rd : p.coords) {
                        if (!this.myMatchShots[rd.getX()][rd.getY()]) {
                            possibles.add(rd);
                        }
                    }
                }
            }
        }

        double[][] turnHeat = new double[10][10];
        for (int s = 4; s >= 0; s--) {
            if (this.shipsAlive[s]) {
                ArrayList<Placement> places = this.theirPlacements.get(s);
                for (Placement p : places) {
                    for (Coordinate c : p.coords) {
                        turnHeat[c.getX()][c.getY()] += (7 / (10 + this.turnNum) + this.hitsHeat[c.getX()][c.getY()][s]);
                    }
                }
            }
        }
        int x = 0;
        int y = 0;
        double best = 0;
        if (possibles.isEmpty()) {
            for (int i = 0; i < 100; i++) {
                if (!this.myMatchShots[i % 10][i / 10] && turnHeat[i % 10][i / 10] >= best) {
                    best = turnHeat[i % 10][i / 10];
                    x = i % 10;
                    y = i / 10;
                }
            }
        } else {
            for (Coordinate c : possibles) {
                if (!this.myMatchShots[c.getX()][c.getY()] && turnHeat[c.getX()][c.getY()] >= best) {
                    best = turnHeat[c.getX()][c.getY()];
                    x = c.getX();
                    y = c.getY();
                }
            }
        }
        shot = new Coordinate(x, y);
        this.lastShot = shot;
        this.myMatchShots[shot.getX()][shot.getY()] = true;
        return shot;
    }

    @Override
    public void resultOfAttack(int result) {
        this.myShots[this.lastShot.getX()][this.lastShot.getY()]++;
        if (result == MISS || result == DEFEATED) {
            for (int s = 0; s < 5; s++) {
                if (this.shipsAlive[s]) {
                    ArrayList<Placement> places = this.theirPlacements.get(s);
                    ArrayList<Placement> bads = new ArrayList<Placement>();
                    for (Placement p : places) {
                        if (p.contains(this.lastShot)) {
                            bads.add(p);
                        }
                    }
                    places.removeAll(bads);
                }
            }
        } else {
            int shipMod = result % 10;
            this.myHits[this.lastShot.getX()][this.lastShot.getY()][shipMod] += 2;
            if (this.lastShot.getX() > 0) {
                this.myHits[this.lastShot.getX() - 1][this.lastShot.getY()][shipMod]++;
            }
            if (this.lastShot.getX() < 9) {
                this.myHits[this.lastShot.getX() + 1][this.lastShot.getY()][shipMod]++;
            }
            if (this.lastShot.getY() > 0) {
                this.myHits[this.lastShot.getX()][this.lastShot.getY() - 1][shipMod]++;
            }
            if (this.lastShot.getY() < 9) {
                this.myHits[this.lastShot.getX()][this.lastShot.getY() + 1][shipMod]++;
            }

            if (result >= 20) {

                this.shipsAlive[shipMod] = false;
                ArrayList<Coordinate> bad_ship = new ArrayList<Coordinate>();
                for (ArrayList<Coordinate> ship : this.hitShips) {
                    if (ship.get(0).getX() == shipMod) {
                        bad_ship = ship;
                        break;
                    }
                }
                this.hitShips.remove(bad_ship);
            } else {
                boolean hitBefore = false;
                for (ArrayList<Coordinate> ship : this.hitShips) {
                    if (ship.get(0).getX() == shipMod) {
                        hitBefore = true;
                        //ship.add(this.lastShot);
                        break;
                    }
                }
                if (!hitBefore) {
                    ArrayList<Coordinate> newShip = new ArrayList<Coordinate>();
                    newShip.add(new Coordinate(shipMod, 0));
                    newShip.add(this.lastShot);
                    this.hitShips.add(newShip);
                }
            }
            for (int s = 0; s < 5; s++) {
                if (this.shipsAlive[s] && s != shipMod) {
                    ArrayList<Placement> places = this.theirPlacements.get(s);
                    ArrayList<Placement> bads = new ArrayList<Placement>();
                    for (Placement p : places) {
                        if (p.contains(this.lastShot)) {
                            bads.add(p);
                        }
                    }
                    places.removeAll(bads);
                } else if (this.shipsAlive[s] && s == shipMod) {
                    ArrayList<Placement> places = this.theirPlacements.get(s);
                    ArrayList<Placement> bads = new ArrayList<Placement>();
                    for (Placement p : places) {
                        if (!p.contains(this.lastShot)) {
                            bads.add(p);
                        }
                    }
                    places.removeAll(bads);
                }
            }
        }
    }

    @Override
    public void opponentAttack(Coordinate coord) {
    }

    @Override
    public void resultOfGame(int result) {
        this.wasWin = result == WON;
        if (!this.wasWin) {
            for (ArrayList<Coordinate> ship : this.hitShips) {
                int ind = ship.get(0).getX();
                boolean[][][] stop = new boolean[10][10][5];
                for (Placement p : this.theirPlacements.get(ind)) {
                    for (Coordinate c : p.coords) {
                        if (!this.myMatchShots[c.getX()][c.getY()] && !stop[c.getX()][c.getY()][ind]) {
                            this.hitsHeat[c.getX()][c.getY()][ind]++;
                            stop[c.getX()][c.getY()][ind] = true;
                        }
                    }
                }
            }
        }
    }

    private class Placement {

        public Coordinate[] coords;
        public double score = 0;
        public int[] shipLength = {2, 3, 3, 4, 5};
        public int direc;

        Placement(int direc, int type, Coordinate loc) {
            this.direc = direc;
            coords = new Coordinate[shipLength[type]];
            coords[0] = loc;
            if (direc == 0) {
                for (int i = 1; i < this.shipLength[type]; i++) {
                    coords[i] = new Coordinate(i + loc.getX(), loc.getY());
                }
            } else {
                for (int i = 1; i < this.shipLength[type]; i++) {
                    coords[i] = new Coordinate(loc.getX(), i + loc.getY());
                }
            }
        }

        public boolean contains(Coordinate c) {
            int x = c.getX();
            int y = c.getY();
            if (direc == 0) {
                if (y != coords[0].getY()) {
                    return false;
                }
                if (x >= coords[0].getX() && x <= coords[coords.length - 1].getX()) {
                    return true;
                }
                return false;
            } else {
                if (x != coords[0].getX()) {
                    return false;
                }
                if (y >= coords[0].getY() && y <= coords[coords.length - 1].getY()) {
                    return true;
                }
                return false;
            }
        }

        public String toString() {
            return "" + coords[0] + ", " + coords[1];
        }
    }
}
