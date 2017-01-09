import java.util.Random;
import java.util.ArrayList;

/**
 * <p>I'm captain Earl Stone and I am the first captain of USS Wisconsin.</p>
 *
 * @author Matthew Levine- levinem0901@my.uwstout.edu
 *
 * @version FALL.2014
 */
public class CaptainEarlStone implements Captain {

    protected final static int UNDETERMINED = -1;
    protected Random generator;

    protected Fleet myFleet;

    protected ArrayList<int[][]> attacks;
    protected int lastAttackCol;
    protected int lastAttackRow;
    protected int[][] attacksAlready = new int[10][10];//saves previous attacks
    protected int[] lastAttack = new int[2];
    
    protected int trainedAssassin = 0;//1 calls the attack pattern to shoot the ship until it dies
    protected int lastHit = 0;//-1 is reverse direction//1 is just hit ship//2 or more is shooting the ship without misssing
    protected int[] currentHit = new int[2];//the last current shot on the enemy ship
    protected int[][] firstHit = new int[5][2];//the first hit on an enemy ship
    //protected int[] firstHit = new int[2];
    protected int direction = UNDETERMINED;//0 = top, 1 = right, 2 = down, 3 = left
    protected int flipDirectionFlag = 0;//1 to flip direction
    
    protected int[][][] pattern = new int[50][10][10];
    protected int randomSpot;
    
    protected int shotx=UNDETERMINED;
    protected int shoty=UNDETERMINED;
    
    protected int patrolFlag = 0;
    protected int destroyerFlag = 0;
    protected int subFlag = 0;
    protected int battleshipFlag = 0;
    protected int carrierFlag = 0;
    protected int currentShip = UNDETERMINED;
    
    protected int[][] opponentAttack = new int[10][10];
    protected int[][] opponentFirstHit = new int[10][10];
    protected int hitFlag =0;
    protected int positionOfTry;//reads where the opponent shoots after first hit on ship. (0 for top, 1 for right, 2 for below, 3 for left)
    protected int orientationOfShips = UNDETERMINED;//0 is horizontal and 1 is vertical, -1 is for first game to decide what way to go
    protected int count;
    protected int stuck = 0;
    
    

    /**
     *
     * @param numMatches	The number matches you will be engaging in with this
     * opponent.
     * @param numCaptains	The number of opponents you will be facing during the
     * current set of battles.
     * @param opponent	The name of your opponent for this match
     */
    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        generator = new Random();
        myFleet = new Fleet();

        // Each type of ship must be placed on the board.  Note that the .place method return whether it was
        // possible to put a ship at the indicated position.  If the coordinates were not on the board or if
        // it overlapped with a ship you already tried to place it will return false.
        if(orientationOfShips == UNDETERMINED) {
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
        if(orientationOfShips == 1) {
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 1, PATROL_BOAT)) {
         }
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 1, DESTROYER)) {
         }
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 1, SUBMARINE)) {
         }
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 1, BATTLESHIP)) {
         }
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 1, AIRCRAFT_CARRIER)) {
         }
        }
        if(orientationOfShips == 0) {
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 0, PATROL_BOAT)) {
         }
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 0, DESTROYER)) {
         }
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 0, SUBMARINE)) {
         }
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 0, BATTLESHIP)) {
         }
         while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), 0, AIRCRAFT_CARRIER)) {
         }
        }
        int i =1;
        int j =0;
         for(int k=0;k <50; k++) {
               
               pattern[k][j][i] = 2;
               i=i+2;
               if(i==11){
                  i=0;
                  j++;
               }
               if(i==10) {
                  i=1;
                  j++;
               }
               //System.out.println("x is " +j + " y is " +i);
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

    /**
     * @return A valid coordinate on the playing field.
     */
    @Override
    public Coordinate makeAttack() {
      //int shotx=-1;
      //int shoty=-1;
      if(trainedAssassin == 0) {         
         randomSpot = generator.nextInt(50);
         shotx =0;
         shoty = 0;
         do {
            shotx++;
            if(shotx==10){
               shotx = 0;
               shoty++;
            }
            if(shotx==0&& shoty==10) {
               randomSpot = generator.nextInt(50);
               shotx=0;
               shoty=0;
            }
            count++;
            if(count > 5000) {
               //count = 0;
               //shotx =0;
               //shoty =0;
               shotx =generator.nextInt(10);
               shoty =generator.nextInt(10);
               while(attacksAlready[shotx][shoty] == 1){
                  shotx =generator.nextInt(10);
                  shoty =generator.nextInt(10);
                  if(attacksAlready[shotx][shoty] != 1){
                     //System.out.println("Was here");
                     break;
                  }
               }
               //stuck++;
               //System.out.println("we are stuck" +stuck);
               break;
            }
            //System.out.println("pattern = " +pattern[randomSpot][shotx][shoty]);
            //System.out.println("already = " +attacksAlready[shotx][shoty]);
         }while((attacksAlready[shotx][shoty] == 1) || (pattern[randomSpot][shotx][shoty] != 2));
         //System.out.println("shotx = " +shotx + " shoty = " + shoty);
         count = 0;
      }
      else {
         shotx = currentHit[0];
         shoty = currentHit[1];
         
         if(lastHit == 0 && (currentShip !=-1 || firstHit[currentShip][0] != -1)){
          shotx = firstHit[currentShip][0];
          shoty = firstHit[currentShip][1];
         }
         while(count <2) {
            if(direction == 0) {
               shoty++;
               //System.out.println(shoty);
               if(shoty >9 || attacksAlready[shotx][shoty] == 1) {
                  direction++;
                  shoty--;
                  if(lastHit > 1 && attacksAlready[shotx][shoty] == 1){
                     shotx = firstHit[currentShip][0];
                     shoty = firstHit[currentShip][1];
                     direction = 2;
                  }
               }
               //System.out.println(shotx + " " +shoty);
               if(attacksAlready[shotx][shoty] != 1){
                  break;
               } 
            }
            if(direction == 1) {
               shotx++;
               if(shotx >9 || attacksAlready[shotx][shoty] == 1) {
                  direction++;
                  shotx--;
                  if(lastHit > 1 && attacksAlready[shotx][shoty] == 1){
                     shotx = firstHit[currentShip][0];
                     shoty = firstHit[currentShip][1];
                     direction = 3;
                  }
                  
               }
               if(attacksAlready[shotx][shoty] != 1){
                  break;
               } 
            }
            if(direction == 2) {
               shoty--;
               if(shoty <0 || attacksAlready[shotx][shoty] == 1) {
                  direction++;
                  shoty++;
                  if(lastHit > 1 && attacksAlready[shotx][shoty] == 1){
                     shotx = firstHit[currentShip][0];
                     shoty = firstHit[currentShip][1];
                     direction = 0;
                  } 
                  
               }
               if(attacksAlready[shotx][shoty] != 1){
                  break;
               }
            }
            if(direction == 3) {
               shotx--;
               if(shotx <0 || attacksAlready[shotx][shoty] == 1) {
                  direction = 4;
                  shotx++;
                  if(lastHit > 1 && attacksAlready[shotx][shoty] == 1){
                     shotx = firstHit[currentShip][0];
                     shoty = firstHit[currentShip][1];
                     direction = 1;
                  } 
                  
               } 
               if(attacksAlready[shotx][shoty] != 1){
                  break;
               }
            }
            if(direction == 4){
               shotx =generator.nextInt(10);
               shoty =generator.nextInt(10);
               while(attacksAlready[shotx][shoty] == 1){
                  shotx =generator.nextInt(10);
                  shoty =generator.nextInt(10);
                  if(attacksAlready[shotx][shoty] != 1){
                     direction = 0;
                     //System.out.println("Was here");
                     break;
                  }
               }
            }
            count++;
          }
         count=0;
      }
      
      if(attacksAlready[shotx][shoty] == 1){
               shotx =generator.nextInt(10);
               shoty =generator.nextInt(10);
               while(attacksAlready[shotx][shoty] == 1){
                  shotx =generator.nextInt(10);
                  shoty =generator.nextInt(10);
                  if(attacksAlready[shotx][shoty] != 1){
                     direction = 0;
                     //System.out.println("already shot there");
                     break;
                  }
               }
            }

       //System.out.print(shotx);
       //System.out.println(shoty);
      attacksAlready[shotx][shoty] = 1;
      lastAttack[0] = shotx;
      lastAttack[1] = shoty;
      return new Coordinate(shotx, shoty);
    }

    /**
     * I just ignore the results of every attack. It's hard to even remember
     * where I last attacked but I'm LOCO!
     *
     * @param result A code from Constants that tells me all about the results
     * of my last attack.
     */
    @Override
    public void resultOfAttack(int result) {

        if(result >=10 && result <= 14) {
         trainedAssassin = 1;
         if(result == 10 && patrolFlag == 0) {
            firstHit[0][0] = lastAttack[0];
            firstHit[0][1] = lastAttack[1];
            patrolFlag = 1;
            direction = 0;
         }
         if(result == 11 && destroyerFlag == 0) {
            firstHit[1][0] = lastAttack[0];
            firstHit[1][1] = lastAttack[1];
            destroyerFlag = 1;
            direction = 0;
         }
         if(result == 12 && subFlag == 0) {
            firstHit[2][0] = lastAttack[0];
            firstHit[2][1] = lastAttack[1];
            subFlag = 1;
            direction = 0;
         }
         if(result == 13 && battleshipFlag == 0) {
            firstHit[3][0] = lastAttack[0];
            firstHit[3][1] = lastAttack[1];
            battleshipFlag = 1;
            direction = 0;
         }
         if(result == 14 && carrierFlag == 0) {
            firstHit[4][0] = lastAttack[0];
            firstHit[4][1] = lastAttack[1];
            carrierFlag = 1;
            direction = 0;
         }
         if(result%10 == currentShip){
            currentHit[0] = lastAttack[0];
            currentHit[1] = lastAttack[1];
            if(currentHit[0] == 0 && direction == 3){
               currentHit[0] = firstHit[currentShip][0];
               currentHit[1] = firstHit[currentShip][1];
               direction = 1;
            }
            if(currentHit[0] == 9 && direction == 1){
               currentHit[0] = firstHit[currentShip][0];
               currentHit[1] = firstHit[currentShip][1];
               direction = 3;
            }
            if(currentHit[1] == 9 && direction == 0){
               currentHit[0] = firstHit[currentShip][0];
               currentHit[1] = firstHit[currentShip][1];
               direction = 2;
            }
            if(currentHit[1] == 0 && direction == 2){
               currentHit[0] = firstHit[currentShip][0];
               currentHit[1] = firstHit[currentShip][1];
               direction = 0;
            }
            lastHit++;
        }
        else{
         firstHit[result%10][0] = lastAttack[0];
         firstHit[result%10][1] = lastAttack[1];
         if(currentShip == UNDETERMINED) {
            currentShip = result%10;
         }
         currentHit[0] = firstHit[currentShip][0];
         currentHit[1] = firstHit[currentShip][1];
         //direction++;
        }
         
        }
        
        
        if(result >=20 && result <= 24) {
         //trainedAssassin = 0;//stops attacking the ship
         lastHit = 0;
         //if(currentShip !=  UNDETERMINED) {
         firstHit[currentShip][0] = UNDETERMINED;
         firstHit[currentShip][1] = UNDETERMINED;
         //}
         //add switching to other ship
         if(firstHit[0][0] == UNDETERMINED
            && firstHit[1][0] == UNDETERMINED
            && firstHit[2][0] == UNDETERMINED
            && firstHit[3][0] == UNDETERMINED
            && firstHit[4][0] == UNDETERMINED){
            currentHit[0] = UNDETERMINED;
            currentHit[1] = UNDETERMINED;
            currentShip = UNDETERMINED;
            trainedAssassin = 0;
         }
         else {
            int c;
            for(c = 0; c < 5; c++){if(firstHit[c][0] != UNDETERMINED){currentShip = c;} }
            if(currentShip != UNDETERMINED) {
               currentHit[0] = firstHit[currentShip][0];
               currentHit[1] = firstHit[currentShip][1];
               //currentShip = c;
            }
            else {
               currentShip = UNDETERMINED;
               trainedAssassin = 0;
            }
            //System.out.println("currentShip = " + currentShip);   
         }
         direction = 0;
         patrolFlag = 0;
         destroyerFlag = 0;
         subFlag = 0;
         battleshipFlag = 0;
         carrierFlag = 0;

        }
        //missed boat
        if(result == 106) {
         if(lastHit == 0){
            //keep shooting randomly or with the pattern
         }
         if(lastHit == 1){
            lastHit = 0;
            direction++;
         }
         if(lastHit > 1){
            currentHit[0] = firstHit[currentShip][0];
            currentHit[1] = firstHit[currentShip][1];
            if(direction== 0){
               direction = 2;
            }
            if(direction == 1){
               direction = 3;
            }
            lastHit = 0;
         }
         //shoot different way
        }
    }

    /**
     * I am reading there attacks to see where to place mine next
     *
     * @param coord The spot on the board where your opponent just attacked.
     */
    @Override
    public void opponentAttack(Coordinate coord) {
        opponentFirstHit[0][0] = opponentAttack[coord.getX()][coord.getY()];
        //System.out.println(coord);
    }

    /**
     * I use this to reset code for new battle
     *
     * @param result A code from Constants that will equal WON or LOST.
     */
    @Override
    public void resultOfGame(int result) {
      for(int i = 0; i < 10; i++){
         for(int j = 0; j < 10; j++){
            attacksAlready[i][j] = 0;
         }
      }
      int i =1;
      int j =0;
         for(int k=0;k <50; k++) {
               
               pattern[k][j][i] = 2;
               i=i+2;
               if(i==11){
                  i=0;
                  j++;
               }
               if(i==10) {
                  i=1;
                  j++;
               }
               
          }
          //System.out.println("new game"); 
          stuck = 0;
      //System.out.println(result);//1 is a win. 0 is a lose
      trainedAssassin = 0;
      lastHit = 0;
      lastAttack[0] = UNDETERMINED;
      lastAttack[1] = UNDETERMINED;
      currentHit[0] = UNDETERMINED;
      currentHit[1] = UNDETERMINED;
      for(int p = 0; p < 5; p++){
         firstHit[p][0] = UNDETERMINED;
         firstHit[p][1] = UNDETERMINED;
      }
      currentShip = UNDETERMINED;
      shotx=UNDETERMINED;
      shoty=UNDETERMINED;
      flipDirectionFlag = 0;
      patrolFlag = 0;
      destroyerFlag = 0;
      subFlag = 0;
      battleshipFlag = 0;
      carrierFlag = 0;
      
    }
}
