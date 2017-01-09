
/**
 * <p>The key interface that every AI must implement to compete in the
 * competition. Your AI MUST implement this interface and override every method
 * in Captain to fully implement your AI (though many methods do not have to do
 * anything in particular). See the documentation for each method below for more
 * details on what you should do in each method.</p>
 *
 * <p>For an example of how to implement this interface see {@link CaptainLoco}
 * or one of the many provided example captains in the 'ExampleCaptains'
 * directory which are entries from previous years.</p>
 *
 * <p>You should NOT modify this interface in any way. Doing so will cause your
 * AI to not work with the program and may cause your entry to be
 * disqualified.</p>
 *
 * @author Seth Dutter - dutters@uwstout.edu
 * @author Seth Berrier - berriers@uwstout.edug
 *
 * @version SPRING.2013
 */
public interface Captain extends Constants {

    /**
     * You should respond to this method by preparing for a new match with one
     * particular opponent. Usually this entails placing your ships on the board
     * (constructing a new 'Fleet') and initializing any internal structures
     * that you will use to keep track of your own moves or your enemies moves.
     * Nothing in particular is required of this function BUT after it is called
     * the system expects that you can return a valid fleet when getFleet() is
     * called. So, be sure to prepare you fleet by placing your pieces on a
     * board.
     *
     * @param numMatches	The number matches you will be engaging in with this
     * opponent.
     * @param numCaptains	The number of opponents you will be facing during the
     * current set of battles.
     * @param opponent	The name of your opponent for this match
     */
    public void initialize(int numMatches, int numCaptains, String opponent);

    /**
     * Return a valid board layout in the form of a Fleet. You should prepare
     * this fleet when initialize is called. You will probably want to have an
     * instance variable of type Fleet to prepare in initialize and then return
     * when this method is called.
     *
     * @return A valid fleet that represents your ship layout for the current
     * match.
     */
    public Fleet getFleet();

    /**
     * It's time to attack! You must pick a valid coordinate on the playfield
     * and return it. The system will use this to attack your opponent and will
     * return the results in the resultOfAttack() method.
     *
     * @return A valid coordinate in the playfield where you want to attack your
     * opponent.
     */
    public Coordinate makeAttack();

    /**
     * After you return an attack coordinate from makeAttack() this method is
     * called to inform you of the result. You may want to remember where you
     * last attacked so you can record the result appropriately. You do not need
     * to do anything specific in this function. It exists so you can learn
     * about your opponents board layout and respond intelligently to the
     * progression of the match.
     *
     * @param result A constant value from Constants that describes a miss or a
     * hit.
     */
    public void resultOfAttack(int result);

    /**
     * INCOMING!!! Your opponent is attacking you! This method will let you know
     * where they attacked so you can record it and adjust your plan. You do not
     * need to respond to this method in any particular way. It exists to let
     * you keep track of your opponent's moves and adjust your strategy to
     * 'learn' how you opponent attacks.
     *
     * @param coord Where you opponent has attacked on the playfield.
     */
    public void opponentAttack(Coordinate coord);

    /**
     * This round is over, how did you do? Result will be either WON or LOST
     * depending on the outcome of this round. You do not need to respond to
     * this method in any particular way. It is feedback to let you know how you
     * did so you can adjust your strategy for the next time you meet this
     * opponent on the glorious field of battle!
     *
     * @param result A Constant value that equals WON or LOST depending on the
     * outcome of this match.
     */
    public void resultOfGame(int result);
}
