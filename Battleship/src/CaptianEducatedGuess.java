

import java.util.Random;
import java.util.Vector;

public class CaptianEducatedGuess implements Captain {

    private Vector<Coordinate> attacksMade;
    private Random generator;
    private Fleet myFleet;
    private boolean goLeft;
    private final int BOARD_WIDTH = 9, BOARD_HEIGHT = 9;
    private Coordinate plannedAttack = null;
    private Coordinate lastCalculatedAttack = null;

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        generator = new Random();
        myFleet = new Fleet();
        attacksMade = new Vector<>();
        goLeft = true;
//		myFleet.placeShip(0, 0, HORIZONTAL, PATROL_BOAT);
//		myFleet.placeShip(2, 0, HORIZONTAL, DESTROYER);
//		myFleet.placeShip(5, 0, HORIZONTAL, SUBMARINE);
//		myFleet.placeShip(0, 1, VERTICAL, BATTLESHIP);
//		myFleet.placeShip(0, 5, VERTICAL, AIRCRAFT_CARRIER);

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

    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    @Override
    public Coordinate makeAttack() {
        Coordinate attack;
        if (plannedAttack != null) {
            attack = plannedAttack;
        } else {
            attack = CalculateNextMove();
        }

        attacksMade.add(attack);
        return attack;
    }

    @Override
    public void resultOfAttack(int result) {
        // If it wasn't a miss and you didn't sink then retry in a random
        // direction

        if (result != MISS && result < SUNK_MODIFIER) {
            Coordinate newAttack = attacksMade.lastElement();

            int change = generator.nextInt(4);
            int iter = 0;
            do {

                iter++;
                if (iter > 4) {
                    //System.out.println("Gave up");
                    plannedAttack = null;
                    return;
                } else {
                    change = (change + 1) % 4;
                }

                switch (change) {
                    case 0:
                        newAttack = new Coordinate(attacksMade.lastElement().getX() - 1, attacksMade.lastElement().getY());
                        //System.out.println("Tried left");
                        break;
                    case 1:
                        newAttack = new Coordinate(attacksMade.lastElement().getX() + 1, attacksMade.lastElement().getY());
                        //System.out.println("Tried right");
                        break;
                    case 2:
                        newAttack = new Coordinate(attacksMade.lastElement().getX(), attacksMade.lastElement().getY() - 1);
                        //System.out.println("Tried down");
                        break;
                    case 3:
                        newAttack = new Coordinate(attacksMade.lastElement().getX(), attacksMade.lastElement().getY() + 1);
                        //System.out.println("Tried up");
                        break;
                }

            } while (attackHasBeenMade(newAttack) || newAttack.getX() < 0 || newAttack.getY() < 0 || newAttack.getY() >= BOARD_HEIGHT || newAttack.getX() >= BOARD_WIDTH);
            //System.out.println("found attack");
            plannedAttack = newAttack;
        } else {
            plannedAttack = null;
        }
    }

    @Override
    public void opponentAttack(Coordinate coord) {
    }

    @Override
    public void resultOfGame(int result) {
    }

    private boolean attackHasBeenMade(Coordinate c) {
        for (Coordinate a : attacksMade) {
            if (a.equals(c)) {
                return true;
            }
        }
        return false;
    }

    private Coordinate CalculateNextMove() {
        if (lastCalculatedAttack == null) {
            lastCalculatedAttack = new Coordinate(BOARD_WIDTH, BOARD_HEIGHT);
            return lastCalculatedAttack;
        }
        Coordinate nextAttack = lastCalculatedAttack;
        while (attackHasBeenMade(nextAttack)) {
            // If at any wall, switch to new horizontal
            if (nextAttack.getX() == 0 || nextAttack.getY() == 0) {
                if (goLeft) {
                    int startY = BOARD_HEIGHT;
                    nextAttack = new Coordinate(BOARD_WIDTH, BOARD_HEIGHT);
                    while (attackHasBeenMade(nextAttack)) {

                        if (nextAttack.getX() == 0) {
                            startY--;
                            nextAttack = new Coordinate(BOARD_WIDTH, startY);
                        }
                        // Start over and move one left
                        if (nextAttack.getX() - 2 < 0) {
                            nextAttack = new Coordinate(BOARD_WIDTH - 1, startY);
                        } else {
                            nextAttack = new Coordinate(nextAttack.getX() - 2, startY);
                        }
                    }

                    goLeft = false;
                } else {
                    int startX = BOARD_WIDTH;
                    nextAttack = new Coordinate(startX, BOARD_HEIGHT);
                    while (attackHasBeenMade(nextAttack)) {

                        if (nextAttack.getY() == 0) {
                            startX--;
                            nextAttack = new Coordinate(startX, BOARD_HEIGHT);
                        } // Start over and move one up
                        else if (nextAttack.getY() - 2 < 0) {
                            nextAttack = new Coordinate(startX, BOARD_HEIGHT - 1);
                        } else {
                            nextAttack = new Coordinate(startX, nextAttack.getY() - 2);
                        }
                    }

                    goLeft = true;
                }

            } else {
                // Go down diagonal
                nextAttack = new Coordinate(nextAttack.getX() - 1, nextAttack.getY() - 1);
                nextAttack = new Coordinate(nextAttack.getX() < 0 ? 0 : nextAttack.getX(), nextAttack.getY() < 0 ? 0 : nextAttack.getY());


            }
        }
        lastCalculatedAttack = nextAttack;
        return nextAttack;
    }
}
