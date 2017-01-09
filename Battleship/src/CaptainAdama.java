
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

//Author Rob Nerison 04/04/2014
//Much thanks to those who aided in the successful completion of this AI--->Josh L., Dr.Dutter & Dennis S.
public class CaptainAdama implements Captain {

    Random generator;
    Fleet myFleet;
    private ArrayList<shipCoord> allCoords;
    private ArrayList<shipCoord> attackVector;
    private ArrayList<shipCoord> hunterSeeker;
    String opponent = "";
    Coordinate[] ptSpots, subSpots, destroyerSpots, battleShipSpots, airCraftSpots;
    Coordinate[][] totalSpots;
    trackCoords[][] board;
    int[][] targeting;
    double[][] values;
    int patRem, destRem, subRem, batRem, airRem, sinceReset, resetHeatVal;
    int oneWins, twoWins, threeWins, fourWins, fiveWins, oneLimit, twoLimit, threeLimit, fourLimit, fiveLimit, set;//how much each placement has won
    boolean placement1, placement2, placement3, placement4, placement5;//placement selection
    int lastAttackX, lastAttackY, numGames;
    final int UNATTACKED = 9999;
    final int MISS = 9998;
    final int HIT = 9997;
    final int UNKNOWN = 3;
    final int check = 5000;//placement

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        if (numGames % 3 == 0 && numGames > 1) {//modifier
            for (int i = 0; i < hunterSeeker.size(); i++) {
                hunterSeeker.get(i).updateHeat();
            }
        }
        if (this.opponent == opponent) {//same opponent
            numGames++;
            sinceReset++;
        } else {                         //new opponent
            oneWins = twoWins = threeWins = fourWins = fiveWins = 0;//placement values
            oneLimit = 1000;
            twoLimit = 2000;
            threeLimit = 3000;
            fourLimit = 4000;
            fiveLimit = 5000;//placement values
            set = 5000;//placement value
            sinceReset = 0;
            resetHeatVal = 50000;//modifier
            targeting = new int[10][10];
            this.opponent = opponent;//update opponent
            numGames = 0;
            board = new trackCoords[10][10];
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    board[x][y] = new trackCoords(x, y);
                    targeting[x][y] = 0;
                }
            }
            hunterSeeker = new ArrayList<shipCoord>();
            generateHunterSeeker();
        }//every game initialization below
        generator = new Random();
        myFleet = new Fleet();
        if (numGames % resetHeatVal == 0) {//resetHeatVal modifier-- 1 million disables
            resetHeatValue();
        }
        placement1 = placement2 = placement3 = placement4 = placement5 = false;//placement values
        int num = generator.nextInt(5000);//placement value
        if (num < oneLimit) {//placement
            setRandom(false);
            placement1 = true;
        } else if (num >= oneLimit && num < twoLimit) {
            oldChrome();
            placement2 = true;
        } else if (num >= twoLimit && num < threeLimit) {
            hide2Ultra();
            placement3 = true;
        } else if (num >= threeLimit && num < fourLimit) {
            variantQuads();
            placement4 = true;
        } else {
            somethingBorrowed();
            placement5 = true;
        }
        if (numGames % check == 0 && numGames > 1) {//end placement
            resetWins();
        }
        patRem = destRem = subRem = batRem = airRem = 0;
        attackVector = new ArrayList<shipCoord>();
        allCoords = new ArrayList<shipCoord>();
        values = new double[10][10];
        generatePossiblePlacements();
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                board[x][y].status = UNATTACKED;
                values[x][y] = 0;
            }
        }
    }

    private void resetWins() {//placement probability
        int total = oneWins + twoWins + threeWins + fourWins + fiveWins;
        oneLimit = (int) (((float) oneWins / (float) total) * (float) set);
        twoLimit = ((int) (((float) twoWins / (float) total) * (float) set) + oneLimit);
        threeLimit = ((int) (((float) threeWins / (float) total) * (float) set) + twoLimit);
        fourLimit = ((int) (((float) fourWins / (float) total) * (float) set) + threeLimit);
        //System.out.println("oneLimit "+oneLimit+" twoLimit"+twoLimit+" threeLimit"+ threeLimit +" fourLimit "+ fourLimit+ "oneWins"+ oneWins+ " twoWins"+ twoWins+" threeWins"+threeWins+ " fourWins"+ fourWins+" fiveWins"+fiveWins);
        oneWins = twoWins = threeWins = fourWins = fiveWins = 0;
    }

    private void generateHunterSeeker() {
        int index = 0;
        for (int ship = 0; ship < 5; ship++) {
            for (int y = 0; y < 10 - getShipLength(ship) + 1; y++) {
                for (int x = 0; x < 10; x++) {
                    hunterSeeker.add(new shipCoord(x, y, 1, ship, index));
                    index++;
                    hunterSeeker.add(new shipCoord(y, x, 0, ship, index));
                    index++;
                }
            }
        }
    }

    public void generatePossiblePlacements() {
        int index = 0;

        for (int ship = 4; ship > -1; ship--) {
            for (int y = 0; y < 10 - getShipLength(ship) + 1; y++) {
                for (int x = 0; x < 10; x++) {
                    allCoords.add(new shipCoord(x, y, 1, ship, index));
                    index++;
                    allCoords.add(new shipCoord(y, x, 0, ship, index));
                    index++;
                }
            }
        }
    }

    class trackCoords {

        int x, y, attacked;
        int status;

        trackCoords(int x, int y) {
            this.x = x;
            this.y = y;
            status = UNATTACKED;
            attacked = 0;
        }
    }

    public class shipCoord {//Defines the coords generated by generatePossiblePlacements

        int x, y, index, orientation, ship;
        double used;
        double heat = 0.2;

        shipCoord(int x, int y, int orientation, int ship, int index) {//allCoords constructor
            this.x = x;
            this.y = y;
            this.index = index;
            this.orientation = orientation;
            this.ship = ship;
            used = 0;//how often this ship coordinate was used
        }

        shipCoord(int x, int y, int orientation, int ship) {//attackVector constructor
            this.x = x;
            this.y = y;
            index = 0;//may be unneeded
            this.orientation = orientation;
            this.ship = ship;
            used = 0;
        }

        void updateHeat() {
            heat = used / (double) sinceReset + 0.00000001;//updates heat for that placement
        }

        void resetHeat() {
            used = 0;//resets heat and used
        }
    }

    void resetHeatValue() {
        sinceReset = 0;
        for (int i = 0; i < hunterSeeker.size(); i++) {
            hunterSeeker.get(i).resetHeat();
        }
    }

    void setValues() {//sets how many ways the remaining ships can be on each tile
        for (int y = 0; y < 10; y++) {//resets board's values to 0
            for (int x = 0; x < 10; x++) {
                values[x][y] = 0;
            }
        }
        int remX, remY, length, orient;
        for (int i = 0; i < allCoords.size(); i++) {
            remX = allCoords.get(i).x;
            remY = allCoords.get(i).y;
            length = getShipLength(allCoords.get(i).ship);
            orient = allCoords.get(i).orientation;
            if (attackVector.size() != 0) {//have ship to attack
                if (attackVector.get(0).ship == allCoords.get(i).ship) {//it is the ship we are trying to sink
                    switch (orient) {
                        case VERTICAL:
                            for (int z = remY; z < remY + length; z++) {
                                values[remX][z] += hunterSeeker.get(allCoords.get(i).index).heat;
                            }
                            break;
                        case HORIZONTAL:
                            for (int z = remX; z < remX + length; z++) {
                                values[z][remY] += hunterSeeker.get(allCoords.get(i).index).heat;
                            }
                            break;
                    }
                }
            } else {// seeking ship to sink
                switch (orient) {
                    case VERTICAL:
                        for (int z = remY; z < remY + length; z++) {
                            values[remX][z] += hunterSeeker.get(allCoords.get(i).index).heat;
                        }
                        break;
                    case HORIZONTAL:
                        for (int z = remX; z < remX + length; z++) {
                            values[z][remY] += hunterSeeker.get(allCoords.get(i).index).heat;
                        }
                        break;
                }
            }
        }
    }

    void removeMiss(int x, int y) {//removes the possible opponent placements that overlap the shot(for each ship).
        int remX, remY, length, orient, ship;
        patRem = subRem = destRem = batRem = airRem = 0;
        for (int i = 0; i < allCoords.size(); i++) {
            ship = allCoords.get(i).ship;
            addBoardPlacements(ship);//new

            remX = allCoords.get(i).x;
            remY = allCoords.get(i).y;
            length = getShipLength(allCoords.get(i).ship);
            orient = allCoords.get(i).orientation;
            if (x == remX && y <= remY + length - 1 && y >= remY && orient == VERTICAL) {//vertical removal
                allCoords.remove(i);
                i--;
                removeBoardPlacements(ship);//new
            } else if (y == remY && x <= remX + length - 1 && x >= remX && orient == HORIZONTAL) {//horizontal removal
                allCoords.remove(i);
                i--;
                removeBoardPlacements(ship);//new
            }//else
        }//for loop
    }

    void removeHit(int x, int y, int hitShip) {
        int remX, remY, length, orient, ship;
        patRem = subRem = destRem = batRem = airRem = 0;
        for (int i = 0; i < allCoords.size(); i++) {
            ship = allCoords.get(i).ship;
            addBoardPlacements(ship);//new

            remX = allCoords.get(i).x;
            remY = allCoords.get(i).y;
            length = getShipLength(ship);
            orient = allCoords.get(i).orientation;
            if (x != remX && y != remY && hitShip == ship) {//vertical removal
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            } else if (y == remY && hitShip == ship && orient == VERTICAL && x != remX) {
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            } else if (x == remX && hitShip == ship && orient == HORIZONTAL && y != remY) {
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            } else if (x == remX && ship == hitShip && remY > y) {//same col above
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            } else if (y == remY && ship == hitShip && x < remX) {
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            } else if (x == remX && (remY + length - 1) < y && ship == hitShip && orient == VERTICAL) {//vertical removal
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            } else if (y == remY && (remX + length - 1) < x && ship == hitShip && orient == HORIZONTAL) {
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            }// above removed same ship that couldn't be on hit square
            //below removes different ships that are on hit square
            if (x == remX && y <= remY + length - 1 && y >= remY && orient == VERTICAL && ship != hitShip) {//vertical removal
                if (allCoords.get(i).ship == PATROL_BOAT) {
                }
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            }
            if (y == remY && x <= remX + length - 1 && x >= remX && orient == HORIZONTAL && ship != hitShip) {//horizontal removal
                if (allCoords.get(i).ship == PATROL_BOAT) {
                }
                allCoords.remove(i);
                removeBoardPlacements(ship);//new
                i--;
            }
        }
    }

    void removeBoardPlacements(int ship) {
        switch (ship) {
            case PATROL_BOAT:
                patRem--;
                break;
            case SUBMARINE:
                subRem--;
                break;
            case DESTROYER:
                destRem--;
                break;
            case BATTLESHIP:
                batRem--;
                break;
            case AIRCRAFT_CARRIER:
                airRem--;
                break;
        }
    }

    void addBoardPlacements(int ship) {
        switch (ship) {
            case PATROL_BOAT:
                patRem++;
                break;
            case SUBMARINE:
                subRem++;
                break;
            case DESTROYER:
                destRem++;
                break;
            case BATTLESHIP:
                batRem++;
                break;
            case AIRCRAFT_CARRIER:
                airRem++;
                break;
        }
    }

    int getShipLength(int ship) {
        switch (ship) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
        }
        return 5;
    }

    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    @Override
    public Coordinate makeAttack() {
        setValues();
        int bestX = generator.nextInt(10);
        int bestY = generator.nextInt(10);
        while (board[bestX][bestY].status != UNATTACKED) {
            bestX = generator.nextInt(10);
            bestY = generator.nextInt(10);
        }
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (board[x][y].status == UNATTACKED) {
                    if (values[x][y] >= values[bestX][bestY]) {//greater than or equal
                        if (values[x][y] > values[bestX][bestY]) {
                            bestX = x;
                            bestY = y;
                        } else {//equal
                            if (targeting[x][y] < targeting[bestX][bestY]) {
                                bestX = x;
                                bestY = y;
                            }
                        }
                    }
                }
            }
        }
        lastAttackX = bestX;
        lastAttackY = bestY;
        board[lastAttackX][lastAttackY].status = MISS;
        board[lastAttackX][lastAttackY].attacked++;//record shot for heat tracking
        targeting[lastAttackX][lastAttackY]++;
        return new Coordinate(lastAttackX, lastAttackY);
    }

    @Override
    public void resultOfAttack(int result) {
        if (result == 106) {
            removeMiss(lastAttackX, lastAttackY);
        }
        int hitShip = result % 10;
        if (result / 10 == 1) {// shot was a hit
            board[lastAttackX][lastAttackY].status = HIT;
            boolean inattackVector = false;//whether or not the hit ship is in list

            for (int i = 0; i < attackVector.size(); i++) {//go through list of hit ships
                if (attackVector.get(i).ship == hitShip) {
                    inattackVector = true;
                }
            }
            if (!inattackVector) {
                attackVector.add(new shipCoord(lastAttackX, lastAttackY, UNKNOWN, hitShip));
            }
            removeHit(lastAttackX, lastAttackY, hitShip);
        }
        if (result / 10 == 2) {//sunk ship
            for (int i = 0; i < attackVector.size(); i++) {//go through list of hit ships
                if (attackVector.get(i).ship == hitShip) {//records the location of the ship for heat tracking
                    attackVector.remove(i);//remove sunk ship
                }
            }
            removeHit(lastAttackX, lastAttackY, hitShip);
            for (int i = 0; i < allCoords.size(); i++) {
                if (allCoords.get(i).ship == hitShip) {
                    hunterSeeker.get(allCoords.get(i).index).used++;
                    allCoords.remove(i);
                }
            }
        }
    }//method

    @Override
    public void opponentAttack(Coordinate coord) {
    }

    @Override
    public void resultOfGame(int result) {
        if (result == LOST) {
            for (int i = 0; i < allCoords.size(); i++) {//if game is lost distributes heat over remaining placements
                hunterSeeker.get(allCoords.get(i).index).used += 1.0 / getShipsLeft(allCoords.get(i).ship);
            }
        } else {             //placement probability
            if (placement1) {
                oneWins++;
            } else if (placement2) {
                twoWins++;
            } else if (placement3) {
                threeWins++;
            } else if (placement4) {
                fourWins++;
            } else {
                fiveWins++;
            }
        }
    }

    private int getShipsLeft(int ship) {
        switch (ship) {
            case PATROL_BOAT:
                return patRem;
            case DESTROYER:
                return destRem;
            case SUBMARINE:
                return subRem;
            case BATTLESHIP:
                return batRem;
            case AIRCRAFT_CARRIER:
                return airRem;
        }
        return 1;
    }

    private void setRandom(boolean allowTouching) {//currently set to false(always)
        int twoX = generator.nextInt(10);
        int twoY = generator.nextInt(10);
        int orientation = generator.nextInt(2);
        if (orientation == HORIZONTAL && twoX == 9) {
            twoX--;
        } else if (orientation == VERTICAL && twoY == 9) {
            twoY--;
        }
        while (!myFleet.placeShip(twoX, twoY, orientation, PATROL_BOAT)) {
        }
        ArrayList<Integer> ships = new ArrayList<Integer>();
        for (int i = 1; i < 5; i++) {
            ships.add(i);
        }
        Collections.shuffle(ships);
        switch (orientation) {
            case HORIZONTAL:
                if (twoX != 0 && twoX != 8 && twoY != 0 && twoY != 9) {//corners
                    for (int i = 0; i < 4; i++) {
                        int ship = ships.get(i);
                        if (!myFleet.placeShip(twoX + 2, twoY + 1, HORIZONTAL, ship)) {//top right Hor
                            if (!myFleet.placeShip(twoX + 2, twoY + 1, VERTICAL, ship)) {//top right Vert
                                if (!myFleet.placeShip(twoX - getShipLength(ship), twoY + 1, HORIZONTAL, ship)) {//top left Hor
                                    if (!myFleet.placeShip(twoX - 1, twoY + 1, VERTICAL, ship)) {//top left Vert
                                        if (!myFleet.placeShip(twoX - getShipLength(ship), twoY - 1, HORIZONTAL, ship)) {//bottom left Hor
                                            if (!myFleet.placeShip(twoX - 1, twoY - getShipLength(ship), VERTICAL, ship)) {//bottom left Vert
                                                if (!myFleet.placeShip(twoX + 2, twoY - 1, HORIZONTAL, ship)) {//bottom right Hor
                                                    if (!myFleet.placeShip(twoX + 2, twoY - getShipLength(ship), VERTICAL, ship)) {
                                                        if (allowTouching) {
                                                            if (!myFleet.placeShip(twoX, twoY + 1, VERTICAL, ship)) {//top
                                                                if (!myFleet.placeShip(twoX, twoY - (getShipLength(ship) + 1), VERTICAL, ship)) {//bottom
                                                                    if (!myFleet.placeShip(twoX + 3, twoY, HORIZONTAL, ship)) {//right
                                                                        if (!myFleet.placeShip(twoX - (getShipLength(ship) + 1), twoY, HORIZONTAL, ship)) {//left
                                                                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {//sides
                    int mod = generator.nextInt(2);
                    for (int i = 0; i < 4; i++) {
                        int ship = ships.get(i);
                        if (!myFleet.placeShip(twoX + 3, twoY, HORIZONTAL, ship)) {//right side
                            if (!myFleet.placeShip(twoX - (getShipLength(ship) + 1), twoY, HORIZONTAL, ship)) {//left side
                                if (!myFleet.placeShip(twoX + mod, twoY + 2, VERTICAL, ship)) {//top
                                    if (!myFleet.placeShip(twoX + ((mod + 1) % 2), twoY - (getShipLength(ship) + 1), VERTICAL, ship)) {//bottom
                                        if (allowTouching) {
                                            if (!myFleet.placeShip(twoX + ((mod + 1) % 2), twoY + 2, VERTICAL, ship)) {//other top
                                                if (!myFleet.placeShip(twoX + mod, twoY - (getShipLength(ship) + 1), VERTICAL, ship)) {//other bottom
                                                    if (!myFleet.placeShip(twoX - 1, twoY + 1, VERTICAL, ship)) {//other right side
                                                        if (!myFleet.placeShip(twoX + 2, twoY - 1, HORIZONTAL, ship)) {//other left side
                                                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            case VERTICAL:
                if (twoX != 0 && twoX != 9 && twoY != 0 && twoY != 8) {//corners
                    for (int i = 0; i < 4; i++) {
                        int ship = ships.get(i);
                        if (!myFleet.placeShip(twoX + 1, twoY + 2, VERTICAL, ship)) {//top right vert
                            if (!myFleet.placeShip(twoX + 1, twoY + 2, HORIZONTAL, ship)) {//top right hor
                                if (!myFleet.placeShip(twoX - 1, twoY + 2, VERTICAL, ship)) {//top left vert
                                    if (!myFleet.placeShip(twoX - (getShipLength(ship)), twoY + 2, HORIZONTAL, ship)) {//top left hor
                                        if (!myFleet.placeShip(twoX - 1, twoY - getShipLength(ship), VERTICAL, ship)) {//bottom left vert
                                            if (!myFleet.placeShip(twoX - getShipLength(ship), twoY - 1, HORIZONTAL, ship)) {//bottom left hor
                                                if (!myFleet.placeShip(twoX + 1, twoY - getShipLength(ship), VERTICAL, ship)) {//bottom right vert
                                                    if (!myFleet.placeShip(twoX + 1, twoY - 1, HORIZONTAL, ship)) {//bottom right hor
                                                        if (allowTouching) {
                                                            if (!myFleet.placeShip(twoX + 3, twoY, HORIZONTAL, ship)) {//right
                                                                if (!myFleet.placeShip(twoX - (getShipLength(ship) + 1), twoY, HORIZONTAL, ship)) {//left
                                                                    if (!myFleet.placeShip(twoX, twoY + 1, VERTICAL, ship)) {//top
                                                                        if (!myFleet.placeShip(twoX + 1, twoY - (getShipLength(ship) + 1), VERTICAL, ship)) {//bottom
                                                                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {//sides
                    int mod = generator.nextInt(2);
                    for (int i = 0; i < 4; i++) {
                        int ship = ships.get(i);
                        if (!myFleet.placeShip(twoX + 2, twoY + mod, HORIZONTAL, ship)) {//right side
                            if (!myFleet.placeShip(twoX - (getShipLength(ship) + 1), twoY + ((mod + 1) % 2), HORIZONTAL, ship)) {//left side
                                if (!myFleet.placeShip(twoX, twoY + 3, VERTICAL, ship)) {//top
                                    if (!myFleet.placeShip(twoX, twoY - (getShipLength(ship) + 1), VERTICAL, ship)) {//bottom
                                        if (allowTouching) {
                                            if (!myFleet.placeShip(twoX + 2, twoY + ((mod + 1) % 2), HORIZONTAL, ship)) {//other right side
                                                if (!myFleet.placeShip(twoX - (getShipLength(ship) + 1), twoY + mod, HORIZONTAL, ship)) {//other left side
                                                    if (!myFleet.placeShip(twoX + 1, twoY + 2, VERTICAL, ship)) {//top right
                                                        if (!myFleet.placeShip(twoX - 1, twoY - getShipLength(ship), VERTICAL, ship)) {//bottom left
                                                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                break;
        }
    }

    void oldChrome() {
        int chromo = generator.nextInt(4);
        int mod = generator.nextInt(8);
        switch (chromo) {
            case 0:
                while (!myFleet.placeShip(1 + mod, 3, VERTICAL, PATROL_BOAT)) {
                }
                while (!myFleet.placeShip(0 + mod, 0, VERTICAL, SUBMARINE)) {
                }
                while (!myFleet.placeShip(2 + mod, 0, VERTICAL, DESTROYER)) {
                }
                while (!myFleet.placeShip(0 + mod, 5, VERTICAL, BATTLESHIP)) {
                }
                while (!myFleet.placeShip(2 + mod, 5, VERTICAL, AIRCRAFT_CARRIER)) {
                }
                break;
            case 1:
                while (!myFleet.placeShip(3, 1 + mod, HORIZONTAL, PATROL_BOAT)) {
                }
                while (!myFleet.placeShip(0, 0 + mod, HORIZONTAL, SUBMARINE)) {
                }
                while (!myFleet.placeShip(0, 2 + mod, HORIZONTAL, DESTROYER)) {
                }
                while (!myFleet.placeShip(5, 0 + mod, HORIZONTAL, BATTLESHIP)) {
                }
                while (!myFleet.placeShip(5, 2 + mod, HORIZONTAL, AIRCRAFT_CARRIER)) {
                }
                break;
            case 2:
                while (!myFleet.placeShip(1 + mod, 5, VERTICAL, PATROL_BOAT)) {
                }
                while (!myFleet.placeShip(0 + mod, 7, VERTICAL, SUBMARINE)) {
                }
                while (!myFleet.placeShip(2 + mod, 7, VERTICAL, DESTROYER)) {
                }
                while (!myFleet.placeShip(2 + mod, 1, VERTICAL, BATTLESHIP)) {
                }
                while (!myFleet.placeShip(0 + mod, 0, VERTICAL, AIRCRAFT_CARRIER)) {
                }
                break;
            case 3:
                while (!myFleet.placeShip(5, 1 + mod, HORIZONTAL, PATROL_BOAT)) {
                }
                while (!myFleet.placeShip(7, 0 + mod, HORIZONTAL, SUBMARINE)) {
                }
                while (!myFleet.placeShip(7, 2 + mod, HORIZONTAL, DESTROYER)) {
                }
                while (!myFleet.placeShip(1, 0 + mod, HORIZONTAL, BATTLESHIP)) {
                }
                while (!myFleet.placeShip(0, 2 + mod, HORIZONTAL, AIRCRAFT_CARRIER)) {
                }
                break;
        }
    }

    void hide2Ultra() {//hide two gets ultra loco

        int hide2 = generator.nextInt(7);

        switch (hide2) {
            case 0:
                while (!myFleet.placeShip(0, generator.nextInt(10), HORIZONTAL, PATROL_BOAT)) {
                }
                break;
            case 1:
                while (!myFleet.placeShip(generator.nextInt(10), 0, VERTICAL, PATROL_BOAT)) {
                }
                break;
            case 2:
                while (!myFleet.placeShip(0, generator.nextInt(10), VERTICAL, PATROL_BOAT)) {
                }
                break;
            case 3:
                while (!myFleet.placeShip(generator.nextInt(10), 0, HORIZONTAL, PATROL_BOAT)) {
                }
                break;
            case 4:
                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), PATROL_BOAT)) {
                }
                break;
            case 5:
                while (!myFleet.placeShip(8, generator.nextInt(10), HORIZONTAL, PATROL_BOAT)) {
                }
                break;
            case 6:
                while (!myFleet.placeShip(9, generator.nextInt(9), VERTICAL, PATROL_BOAT)) {
                }
                break;
        }
        for (int i = 1; i < 5; i++) {
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
            }
        }
    }

    void variantQuads() {
        int totalShips = 5;
        int lefter = generator.nextInt(2);
        while (totalShips > 0) {
            boolean left, top;
            left = top = false;

            if (generator.nextInt(2) == 0) {
                left = true;
            }
            if (generator.nextInt(2) == 0) {
                top = true;
            }

            if (totalShips > 1) {
                if (left) {
                    if (top) {
                        while (!myFleet.placeShip(generator.nextInt(5), generator.nextInt(5) + 5, generator.nextInt(2), totalShips - 1)) {
                        }
                        totalShips--;
                    } else {
                        while (!myFleet.placeShip(generator.nextInt(5), generator.nextInt(5), generator.nextInt(2), totalShips - 1)) {
                        }
                        totalShips--;
                    }
                } else {
                    if (top) {
                        while (!myFleet.placeShip(generator.nextInt(5) + 5, generator.nextInt(5) + 5, generator.nextInt(2), totalShips - 1)) {
                        }
                        totalShips--;
                    } else {
                        while (!myFleet.placeShip(generator.nextInt(5), generator.nextInt(5), generator.nextInt(2), totalShips - 1)) {
                        }
                        totalShips--;
                    }
                }
            }
            if (lefter == 0) {
                while (!myFleet.placeShip(generator.nextInt(5) + 5, generator.nextInt(10), generator.nextInt(2), totalShips - 1)) {
                }
                totalShips--;
            } else {
                while (!myFleet.placeShip(generator.nextInt(5), generator.nextInt(10), generator.nextInt(2), totalShips - 1)) {
                }
                totalShips--;
            }
        }
    }

    void somethingBorrowed() {//thanks Dennis S.

        Coordinate bLeft = new Coordinate(0, 0);
        Coordinate bRight = new Coordinate(0, 9);
        Coordinate tLeft = new Coordinate(9, 0);
        Coordinate tRight = new Coordinate(9, 9);

        Coordinate[] corners = {bLeft, bRight, tLeft, tRight};
        ArrayList<Integer> ships = new ArrayList<Integer>();
        for (int i = 0; i < 5; i++) {
            ships.add(i);
        }
        Collections.shuffle(ships);

        for (int i = 0; i < 4; i++) {

            int ship = ships.get(i);
            int orientation = generator.nextInt(2);
            if (orientation == VERTICAL && corners[i].getY() == 9) {
                if (!myFleet.placeShip(corners[i].getX(), corners[i].getY() - getShipLength(ship) + 1, orientation, ship)) {
                }
            } else if (orientation == HORIZONTAL && corners[i].getX() == 9) {
                if (!myFleet.placeShip(corners[i].getX() - getShipLength(ship) + 1, corners[i].getY(), orientation, ship)) {
                }
            } else {
                if (!myFleet.placeShip(corners[i].getX(), corners[i].getY(), orientation, ship)) {
                }
            }
        }
        boolean set = true;
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ships.get(4))) {
            if (set) {
                set = false;
            }
        }
    }
}
//Prints a formated grid of the board for debugging compensates for the inversion
//System.out.println("The number of ships found" + attackVector.size());
//System.out.println("Values of each tile");
//System.out.print("    0  | 1  | 2  | 3  | 4  | 5  | 6  |  7 |  8 |  9");
//
//	for(int y=0;y<10;y++){
//			System.out.print("\n----------------------------------------------");
//			System.out.print("\n"+y+" | ");
//			for(int x=0;x<10;x++){
//				System.out.print(String.format("%.2f", values[x][y])+" |");
//			}
//	}
//}
//System.out.println("coordinate ("+bestX+","+bestY+")");