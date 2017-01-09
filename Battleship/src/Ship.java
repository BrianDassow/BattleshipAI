
/**
 * A representation of a single piece for the game Battleship. This class works
 * in conjunction with {@link Constants} to allow easy examination and
 * comparison of pieces in the game. It is used extensively by {@link Fleet}.
 * You should not need to change anything in this interface to compete in the
 * competition and modifying it may cause your entry to be disqualified!
 *
 * @author Seth Dutter - dutters@uwstout.edu
 * @author Seth Berrier - berriers@uwstout.edu
 *
 * @version SPRING.2013
 */
public class Ship implements Constants {

    protected static final int HIT = 1;
    /**
     * Where this ship is located on the board (it's left-most or top-most peg
     * hole)
     */
    protected Coordinate location;
    /**
     * The length of this ship
     */
    protected int length;
    /**
     * How this ship is oriented (VERTICAL or HORIZONTAL)
     */
    protected int direction;
    /**
     * Which peg holes are filled/where has this ship been hit (Usually
     * array.length = length).
     */
    protected int[] shipData;
    /**
     * Has this ship been sunk?
     */
    protected boolean sunk;
    /**
     * What is this ship's model (PATROL_BOAT, DESTROYER, SUBMARINE, BATTLESHIP
     * or AIRCRAFT_CARRIER)
     */
    protected int model;

    /**
     * Construct a new ship with the given location, direction and model.
     *
     * @param location	The position of the left-most or top-most peg hole for
     * this ship
     * @param direction	The direction of this ship (VERTICAL or HORIZONTAL)
     * @param model	The model of this ship (PATROL_BOAT, DESTROYER, SUBMARINE,
     * BATTLESHIP or AIRCRAFT_CARRIER)
     */
    public Ship(Coordinate location, int direction, int model) {

        // Store local information about ship
        this.location = location;
        this.model = model;
        this.direction = direction;

        // Default to NOT sunk
        sunk = false;

        // Store the length of the ship based on it's model
        switch (model) {
            case PATROL_BOAT:
                length = PATROL_BOAT_LENGTH;
                break;
            case DESTROYER:
                length = DESTROYER_LENGTH;
                break;
            case SUBMARINE:
                length = SUBMARINE_LENGTH;
                break;
            case BATTLESHIP:
                length = BATTLESHIP_LENGTH;
                break;
            default:
                length = AIRCRAFT_CARRIER_LENGTH;
                break;
        }

        // Create an array to store which portions of the ship have been hit
        shipData = new int[length];
    }

    /**
     * Get the location of this ship.
     *
     * @return This ship's location.
     */
    public Coordinate getLocation() {
        return location;
    }

    /**
     * Get the length of this ship.
     *
     * @return This ship's length.
     */
    public int getLength() {
        return length;
    }

    /**
     * Get the direction of this ship.
     *
     * @return This ship's direction.
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Get the model of this ship.
     *
     * @return This ship's model.
     */
    public int getModel() {
        return model;
    }

    /**
     * Is this ship still floating or has it been sunk?
     *
     * @return True if the ship has been sunk (false otherwise).
     */
    public boolean isSunk() {
        return sunk;
    }

    /**
     * Test if this ship is fully on the game board. If any part of the ship is
     * outside the bounds of the board this method will return false.
     *
     * @return True only if the entire length of this ship is within the bounds
     * of the game board (false otherwise).
     */
    public boolean isValid() {
        if (direction == HORIZONTAL) {
            Coordinate farEnd = new Coordinate(location.getX() + length - 1, location.getY());
            if (location.greaterThan(new Coordinate(-1, -1)) && farEnd.lessThan(new Coordinate(10, 10))) {
                return true;
            } else {
                return false;
            }
        } else {
            Coordinate farEnd = new Coordinate(location.getX(), location.getY() + length - 1);
            if (location.greaterThan(new Coordinate(-1, -1)) && farEnd.lessThan(new Coordinate(10, 10))) {
                return true;
            } else {
                return false;
            }
        }

    }

    /**
     * Test to see if a given attack coordinate will hit any part of this ship.
     *
     * @param coord The coordinate on the board to test.
     * @return True only if the coordinate hits some spot on this ship (false
     * otherwise).
     */
    public boolean isOnShip(Coordinate coord) {
        if (this.direction == HORIZONTAL) {
            if (location.getY() == coord.getY()) {
                for (int i = 0; i < length; i++) {
                    if (location.getX() + i == coord.getX()) {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        } else {
            if (location.getX() == coord.getX()) {
                for (int i = 0; i < length; i++) {
                    if (location.getY() + i == coord.getY()) {
                        return true;
                    }
                }
                return false;
            } else {
                return false;
            }
        }
    }

    /**
     * Test to see if two ships overlap on the game board.
     *
     * @param ship The second ship to compare with this ship.
     * @return True only if this ship overlaps in at least one space with the
     * given ship (false otherwise).
     */
    public boolean intersectsShip(Ship ship) {
        boolean doesIntersect = false;
        if (direction == HORIZONTAL) {
            for (int i = 0; i < length; i++) {
                if (ship.isOnShip(new Coordinate(location.getX() + i, location.getY()))) {
                    doesIntersect = true;
                }
            }
        } else {
            for (int i = 0; i < length; i++) {
                if (ship.isOnShip(new Coordinate(location.getX(), location.getY() + i))) {
                    doesIntersect = true;
                }
            }
        }
        return doesIntersect;
    }

    /**
     * Attack this ship at the specified coordinate and return a value from
     * {@link Constants} to describe the result.
     *
     * @param coord The space on the board you are attacking.
     * @return A result code from {@link Constants} describing the result of
     * this attack.
     */
    public int attacked(Coordinate coord) {
        if (this.direction == HORIZONTAL) {
            if (location.getY() == coord.getY()) {
                for (int i = 0; i < length; i++) {
                    if (location.getX() + i == coord.getX()) {
                        shipData[i] = HIT;
                        for (int j = 0; j < length; j++) {
                            if (shipData[j] != HIT) {
                                return model + HIT_MODIFIER;
                            }
                        }
                        this.sunk = true;
                        return model + SUNK_MODIFIER;
                    }
                }
                return MISS;
            } else {
                return MISS;
            }
        } else {
            if (location.getX() == coord.getX()) {
                for (int i = 0; i < length; i++) {
                    if (location.getY() + i == coord.getY()) {
                        shipData[i] = HIT;
                        for (int j = 0; j < length; j++) {
                            if (shipData[j] != HIT) {
                                return model + HIT_MODIFIER;
                            }
                        }
                        this.sunk = true;
                        return model + SUNK_MODIFIER;
                    }
                }
                return MISS;
            } else {
                return MISS;
            }
        }
    }
}
