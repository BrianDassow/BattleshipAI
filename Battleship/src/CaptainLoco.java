
import java.util.Random;


public class CaptainLoco implements Captain {

    protected Random generator;

    protected Fleet myFleet;

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        generator = new Random();
        myFleet = new Fleet();

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
        return new Coordinate(generator.nextInt(10), generator.nextInt(10));
    }

    @Override
    public void resultOfAttack(int result) {
        // Add code here to process the success/failure of attacks
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        // Add code here to process or record opponent attacks
    }

    @Override
    public void resultOfGame(int result) {
        // Add code here to process the result of a game
    }
}
