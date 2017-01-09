
/**
 * <p>A set of constants used throughout the Battleship program. These are used
 * to place ship, specify ship types and lengths and determine the results of an
 * attack. Some types of constants can be combined into larger values that
 * encode more information in various decimal digits.</p>
 *
 * <p>An attack code is a two digit number that encodes the type of the ship and
 * the result of an attack: <ul> <li>The 1's digit encodes the type of ship
 * (0=patrol, 1=destroyer, 2=submarine, 3=battleship, 4=carrier)</li> <li>The
 * 10's digit encodes the results of the attack (0=miss, 1=hit, 2=ship sunk)
 * <li>There are two special 1's digits for misses (6) and a full defeat
 * (7)</li> </ul></p>
 *
 * <p>You should not need to change anything in this interface to compete in the
 * competition and modifying it may cause your entry to malfunction.</p>
 *
 * @see Ship Ship
 * @see Fleet Fleet
 * @see Battleship Battleship
 *
 * @author Seth Dutter - dutters@uwstout.edu
 * @author Seth Berrier - berriers@uwstout.edu
 *
 * @version SPRING.2013
 */
public interface Constants {

    /**
     * Orient a ship horizontally on the board
     */
    final static int HORIZONTAL = 0;
    /**
     * Orient a ship vertically on the board
     */
    final static int VERTICAL = 1;
    /**
     * Final result of the round is a win
     */
    final static int WON = 1;
    /**
     * Final result of the round is a loss
     */
    final static int LOST = 0;
    /**
     * Add this to a type code to get a HIT attack code
     */
    final static int HIT_MODIFIER = 10;
    /**
     * Add this to a type code to get a SUNK attack code
     */
    final static int SUNK_MODIFIER = 20;
    /**
     * Attack code for a miss
     */
    final static int MISS = 106;
    /**
     * Attack code for defeating your enemy (sinking their final ship)
     */
    final static int DEFEATED = 107;
    /**
     * The patrol boat type code
     */
    final static int PATROL_BOAT = 0;
    /**
     * The patrol boat's length
     */
    final static int PATROL_BOAT_LENGTH = 2;
    /**
     * The HIT attack code for the patrol boat
     */
    final static int HIT_PATROL_BOAT = HIT_MODIFIER + PATROL_BOAT;
    /**
     * The SUNK attack code for the patrol boat
     */
    final static int SUNK_PATROL_BOAT = SUNK_MODIFIER + PATROL_BOAT;
    /**
     * The destroyer type code
     */
    final static int DESTROYER = 1;
    /**
     * The destroyer's length
     */
    final static int DESTROYER_LENGTH = 3;
    /**
     * The HIT attack code for the destroyer
     */
    final static int HIT_DESTROYER = HIT_MODIFIER + DESTROYER;
    /**
     * The SUNK attack code for the destroyer
     */
    final static int SUNK_DESTROYER = SUNK_MODIFIER + DESTROYER;
    /**
     * The submarine type code
     */
    final static int SUBMARINE = 2;
    /**
     * The submarine's length
     */
    final static int SUBMARINE_LENGTH = 3;
    /**
     * The HIT attack code for the submarine
     */
    final static int HIT_SUBMARINE = HIT_MODIFIER + SUBMARINE;
    /**
     * The SUNK attack code for the submarine
     */
    final static int SUNK_SUBMARINE = SUNK_MODIFIER + SUBMARINE;
    /**
     * The battleship type code
     */
    final static int BATTLESHIP = 3;
    /**
     * The battleship's length
     */
    final static int BATTLESHIP_LENGTH = 4;
    /**
     * The HIT attack code for the battleship
     */
    final static int HIT_BATTLESHIP = HIT_MODIFIER + BATTLESHIP;
    /**
     * The SUNK attack code for the battleship
     */
    final static int SUNK_BATTLESHIP = SUNK_MODIFIER + BATTLESHIP;
    /**
     * The aircraft carrier type code
     */
    final static int AIRCRAFT_CARRIER = 4;
    /**
     * The aircraft carrier's length
     */
    final static int AIRCRAFT_CARRIER_LENGTH = 5;
    /**
     * The HIT attack code for the aircraft carrier
     */
    final static int HIT_AIRCRAFT_CARRIER = HIT_MODIFIER + AIRCRAFT_CARRIER;
    /**
     * The SUNK attack code for the aircraft carrier
     */
    final static int SUNK_AIRCRAFT_CARRIER = SUNK_MODIFIER + AIRCRAFT_CARRIER;
}
