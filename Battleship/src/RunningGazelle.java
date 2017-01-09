
import java.util.ArrayList;
import java.util.Random;

public class RunningGazelle implements Captain, Constants {

    // Single Integer Variables.
    private int X, Y;
    private int lastHitX, lastHitY;
    private int currX, currY;
    private int attackCount = 0;
    private int killStep = 1;
    private int shipDirection = -1;
    private int currentShip = -1;
    private int smallestShipLength = 2;
    private int largestShipLength = 5;
    private int strikeStep = 2;
    // Strings
    private String currentOpponent = " ";
    // Single boolean variables.
    private boolean shipSighted = false;
    private boolean directionSwitch = false;
    private boolean extraShip = false;
    // private boolean collateralDamage = false;
    // Arrays:
    private int[][] attackArray = new int[10][10];
    private int[][] opponentAttack = new int[10][10];
    private ArrayList<Coordinate> newShips = new ArrayList<Coordinate>();
    private ArrayList<Coordinate> spaces = new ArrayList<Coordinate>();
    // battleship data.
    private Ships[] oppShips = new Ships[5];
    protected Random generator;
    protected Fleet myFleet;
    protected int parity;

    private class Ships {

        private int shipType;
        private int shipLength;
        // untouched = 0, hit = 1, sunk = 2;
        private int status;

        public Ships(int shipType, int shipLength, int status) {
            this.shipType = shipType;
            this.shipLength = shipLength;
            this.status = status;
        }

        int getShipType() {
            return shipType;
        }

        int getLength() {
            return shipLength;
        }

        int getStatus() {
            return status;
        }

        void setStatus(int status) {
            if (status >= 0 && status <= 2) {
                this.status = status;
            }
        }
    }

    /*
     * @param numMatches The number matches you will be engaging in with this
     * opponent.
     *
     * @param numCaptains The number of opponents you will be facing during the
     * current set of battles.
     *
     * @param opponent The name of your opponent for this match
     */
    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        oppShips[0] = new Ships(0, 2, 0);
        oppShips[1] = new Ships(1, 3, 0);
        oppShips[2] = new Ships(2, 3, 0);
        oppShips[3] = new Ships(3, 4, 0);
        oppShips[4] = new Ships(4, 5, 0);
        generator = new Random();
        myFleet = new Fleet();
        parity = generator.nextInt(2);
        attackCount = 0;
        smallestShipLength = 2;
        killStep = 1;
        directionSwitch = false;
        shipDirection = -1;
        shipSighted = false;
        strikeStep = 2;

        // Resetting variables for a new opponent.
        if (!(currentOpponent.equals(opponent))) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    opponentAttack[i][j] = 0;
                }
            }
        }
        /*
         // Resetting attackArray to zeros for the new game.
         for (int i = 0; i < 10; i++) {
         for (int j = 0; j < 10; j++) {
         attackArray[i][j] = 0;
         }
         }
         */
        // Each type of ship must be placed on the board. Note that the .place
        // method return whether it was
        // possible to put a ship at the indicated position. If the coordinates
        // were not on the board or if
        // it overlapped with a ship you already tried to place it will return
        // false.
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), PATROL_BOAT)) {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), BATTLESHIP)) {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), AIRCRAFT_CARRIER)) {
        }

    }

    /**
     * Return my fleet that was constructed in initialize. This is a method you
     * probably don't need to change.
     *
     * @return A valid fleet representing my ship placements for this round.
     */
    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    // Method used for calculating the possible places a ship could be.
    private void calcStats() {
        int spaceCount = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (attackArray[i][j] == 0) {
                    spaceCount++;
                }
                if (spaceCount >= smallestShipLength) {
                    spaces.add(new Coordinate(i, (j - (smallestShipLength / 2))));
                    spaceCount--;
                }
            }
            spaceCount = 0;
        }
        spaceCount = 0;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if (attackArray[j][i] == 0) {
                    spaceCount++;
                }
                if (spaceCount >= smallestShipLength) {
                    spaces.add(new Coordinate(j - (smallestShipLength / 2), (i)));
                    spaceCount--;
                }
            }
            spaceCount = 0;
        }
    }

    /*
     * @return A valid coordinate on the playing field.
     */
    @Override
    public Coordinate makeAttack() {

        attackCount++;
        // If statement to tell of a ship has been sighted.
        if (shipSighted) {
            return KO();
        } else {
            if (newShips.size() > 0) {
                lastHitX = newShips.get(0).getX();
                lastHitY = newShips.get(0).getY();
                newShips.remove(0);
                shipDirection = -1;
                killStep = 1;
                extraShip = true;
                shipSighted = true;
                return KO();

            } else if (attackCount < 49) {
                return checkerAttack();
            } else {
                calcShips();
                spaces.clear();
                calcStats();
                if (spaces.size() > 0) {
                    int randomValue = generator.nextInt(spaces.size());
                    int ranX = spaces.get(randomValue).getX(), ranY = spaces.get(randomValue).getY();
                    currX = ranX;
                    currY = ranY;
                    if (attackArray[currX][currY] == 0) {
                        attackArray[currX][currY] = 1;
                        return new Coordinate(currX, currY);
                    } else {
                        randomAttack();
                    }
                } else {
                    return new Coordinate(generator.nextInt(10), generator.nextInt(10));

                }
            }
        }
        return randomAttack();
    }

    private Coordinate randomAttack() {
        int x, y;
        turnOffKill();
        int count = 0;
        do {
            x = ((generator.nextInt(10)));
            y = ((generator.nextInt(10)));
            count++;
            if (count > 300) {
                return new Coordinate(generator.nextInt(10), generator.nextInt(10));
            }
        } while (attackArray[x][y] == 1);
        attackArray[x][y] = 1;
        currX = x;
        currY = y;
        return new Coordinate(currX, currY);
    }

    // checkerAttack when called will only aim for squares on half the board.
    // will also have a stronger tendency to hit the middle (averaging numbers).
    // @return Coordinate of the new attack.
    private Coordinate checkerAttack() {
        int x, y;
        do {
            x = (generator.nextInt(10));
            y = (generator.nextInt(10));
        } while (((((x + y) % 2) != (parity))) || (attackArray[x][y] == 1));
        // Setting the value
        attackArray[x][y] = 1;
        currX = x;
        currY = y;
        return new Coordinate(currX, currY);

    }

    // Method for finishing off ships.
    private Coordinate KO() {
        switch (shipDirection) {
            case -1:
                switch (killStep) {
                    case 1:
                        if ((lastHitY - 1) >= 0 && attackArray[lastHitX][lastHitY - 1] == 0) {
                            killStep++;
                            currX = lastHitX;
                            currY = (lastHitY - 1);
                        } else {
                            killStep++;
                            return KO();
                        }
                        break;
                    case 2:
                        if ((lastHitX - 1) >= 0 && attackArray[lastHitX - 1][lastHitY] == 0) {
                            killStep++;
                            currX = (lastHitX - 1);
                            currY = lastHitY;
                        } else {
                            killStep++;
                            return KO();
                        }
                        break;
                    case 3:
                        if ((lastHitY + 1) <= 9 && attackArray[lastHitX][lastHitY + 1] == 0) {
                            killStep++;
                            currX = lastHitX;
                            currY = (lastHitY + 1);
                        } else {
                            killStep++;
                            return KO();
                        }
                        break;
                    case 4:
                        if ((lastHitX + 1) <= 9 && attackArray[lastHitX + 1][lastHitY] == 0) {
                            killStep++;
                            currX = lastHitX + 1;
                            currY = lastHitY;
                        } else {
                            return randomAttack();
                        }
                        break;
                }
                break;
            case 1:
                if ((lastHitY - (killStep - shipDirection)) >= 0 && attackArray[lastHitX][(lastHitY - (killStep - shipDirection))] == 0) {
                    currX = lastHitX;
                    currY = (lastHitY - (killStep - shipDirection));
                    killStep++;
                } else {
                    if (!directionSwitch) {
                        switchDirections();
                        directionSwitch = true;
                        return KO();
                    } else {
                        return lastStrike();
                    }
                }
                break;
            case 2:
                if ((lastHitX - (killStep - shipDirection)) >= 0 && attackArray[(lastHitX - (killStep - shipDirection))][lastHitY] == 0) {
                    currX = (lastHitX - (killStep - shipDirection));
                    currY = lastHitY;
                    killStep++;
                } else {
                    if (!directionSwitch) {
                        switchDirections();
                        directionSwitch = true;
                        return KO();
                    } else {
                        return lastStrike();
                    }
                }
                break;
            case 3:
                if ((lastHitY + (killStep - shipDirection)) <= 9 && attackArray[lastHitX][(lastHitY + (killStep - shipDirection))] == 0) {
                    currX = lastHitX;
                    currY = (lastHitY + (killStep - shipDirection));
                    killStep++;
                } else {
                    if (!directionSwitch) {
                        switchDirections();
                        directionSwitch = true;
                        return KO();
                    } else {
                        return lastStrike();
                    }
                }
                break;
            case 4:
                if ((lastHitX + (killStep - shipDirection)) <= 9 && attackArray[(lastHitX + (killStep - shipDirection))][lastHitY] == 0) {
                    currX = (lastHitX + (killStep - shipDirection));
                    currY = lastHitY;
                    killStep++;
                } else {
                    if (!directionSwitch) {
                        switchDirections();
                        directionSwitch = true;
                        return KO();
                    } else {
                        return lastStrike();
                    }
                }
                break;
        }
        if (attackArray[currX][currY] == 0) {
            attackArray[currX][currY] = 1;
            return new Coordinate(currX, currY);
        } else {
            return lastStrike();
        }

    }

    private Coordinate lastStrike() {
        int lastX = lastHitX, lastY = lastHitY;
        if ((strikeStep / 2) > 5) {
            return randomAttack();
        }
        if (strikeStep % 2 == 0) {
            if (shipDirection % 2 == 0) {
                if (lastX + (strikeStep / 2) <= 9 && attackArray[lastX + (strikeStep / 2)][lastY] == 0) {
                    lastX = lastX + (strikeStep / 2);
                    strikeStep++;
                } else {
                    strikeStep++;
                    return lastStrike();
                }
            } else {
                if (lastY + (strikeStep / 2) <= 9 && attackArray[lastX][lastY + (strikeStep / 2)] == 0) {
                    lastY = lastY + (strikeStep / 2);
                    strikeStep++;
                } else {
                    strikeStep++;
                    return lastStrike();
                }
            }
        } else {
            if (shipDirection % 2 == 0) {
                if (lastX - (strikeStep / 2) >= 0 && attackArray[lastX - (strikeStep / 2)][lastY] == 0) {
                    lastX = lastX - (strikeStep / 2);
                    strikeStep++;
                } else {
                    strikeStep++;
                    return lastStrike();
                }
            } else {
                if (lastY - (strikeStep / 2) >= 0 && attackArray[lastX][lastY - (strikeStep / 2)] == 0) {
                    lastY = lastY - (strikeStep / 2);
                    strikeStep++;
                } else {
                    strikeStep++;
                    return lastStrike();
                }
            }
        }
        currX = lastX;
        currY = lastY;
        attackArray[currX][currY] = 1;
        return new Coordinate(currX, currY);
    }

    // Code for switching the direction of attack for various reasons.
    private void switchDirections() {
        if (shipDirection != -1) {
            if (shipDirection == 1) {
                shipDirection = 3;
                killStep = 4;
            } else if (shipDirection == 2) {
                shipDirection = 4;
                killStep = 5;
            } else if (shipDirection == 3) {
                shipDirection = 1;
                killStep = 3;
            } else if (shipDirection == 4) {
                shipDirection = 2;
                killStep = 4;
            }
        }
    }

    // Code to reset the variables for the KO() method.
    private void turnOffKill() {
        shipSighted = false;
        shipDirection = -1;
        killStep = 1;
        strikeStep = 2;
        extraShip = false;
    }

    @Override
    public void resultOfAttack(int result) {
        if (result >= 10 && result < 20) {
            // System.out.println("I see you :P");
            if (shipSighted) {
                if (currentShip == result % 10 || extraShip == true) {
                    if (shipDirection == -1) {
                        shipDirection = killStep - 1;
                        killStep++;
                    }
                } else {
                    newShips.add(new Coordinate(currX, currY));
                }
            } else {
                shipSighted = true;
                currentShip = result % 10;
                killStep = 1;
                lastHitX = currX;
                lastHitY = currY;
            }
        } else if (result >= 20 && result < 30) {
            turnOffKill();
            calcShips();
            int shipSunk = result % 20;

            // Code for adjusting the value when a ship is sunk.
            for (int i = 0; i < 5; i++) {
                if (oppShips[i].getShipType() == shipSunk) {
                    oppShips[i].setStatus(2);
                }
            }


        } else if (result == 106 && shipSighted) {
            if (!directionSwitch && shipDirection != -1) {
                directionSwitch = true;
                switchDirections();
            }

        }
    }

    void calcShips() {
        // Code for finding the smallest ship length.
        smallestShipLength = 5;
        for (int i = 0; i < 5; i++) {
            if (oppShips[i].getStatus() == 0) {
                if (oppShips[i].getLength() <= smallestShipLength) {
                    smallestShipLength = oppShips[i].getLength();
                }
            }
        }

        //Code for finding the largest ship.
        largestShipLength = 2;
        for (int i = 0; i < 5; i++) {
            if (oppShips[i].getStatus() == 0) {
                if (oppShips[i].getLength() >= largestShipLength) {
                    largestShipLength = oppShips[i].getLength();
                }
            }
        }
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        // Add code here to process or record opponent attacks
        opponentAttack[coord.getX()][coord.getY()] = +1;
    }

    @Override
    public void resultOfGame(int result) {
        attackCount = 0;
        // Add code here to process the result of a game
        for (int i = 0; i < newShips.size(); i++) {
            newShips.remove(newShips.size() - 1);
        }

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                attackArray[i][j] = 0;
            }
        }
    }
}