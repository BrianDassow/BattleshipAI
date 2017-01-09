
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.TreeSet;

/** Fall 2014
 * @author Rob Nerison: strategy(with a little help from Dr. Dutter!)
 * @author Josh Loschen (formerly known as Captain America): coding and implementation of all strategy updates and modifications to current revision.  Also a major/primary contributor to last years submission's code and implementation. 
 * This AI is the direct successor to CaptainAdama from Spring 2014. It is the defending student champ. With hopes to retain its current title.
 * This AI shares most of the original features of CaptainAdama but has been significantly updated to take into account that StanleyHTweedle's placement is freaking amazing, as well as other subtle mods to increase accuracy and effectiveness of both the attack and the placement strategies.
 * Specific modifications of the original CaptainAdama that should be noted, as credit should be given where credit is due:
 *    1) Borrowed considerable insight from StanleyHTweedle's placement thus replacing 2 of the previous 5 placement strategies.
 *    2) Although our rendition of the StanleyH placement borrows the primary concepts as Dr. Dutter had initially designed them -- it is indeed highly modified and in our opinion significantly improved.
 * Again, many thanks to Dr. Dutter as this AI would not have been possible without your expertise, insight, advice, and wicked placement strategy.
 * Good luck to All!
 */

public class CaptainAdama2 implements Captain {
    final int[] shipLengths = new int[]{2, 3, 3, 4, 5};
    final int fleetSize = 10000;
    protected Random generator;
    Fleet myFleet;
    int numOpponentAttacks;
    private ArrayList<shipCoord> allCoords;
    private ArrayList<shipCoord> attackVector;
    private ArrayList<shipCoord> hunterSeeker;
    String opponent = "";
    Coordinate[] ptSpots, subSpots, destroyerSpots, battleShipSpots, airCraftSpots;
    Coordinate[][] totalSpots;
    trackCoords[][] board;
    int[][] targeting;
    int[][] theirAttacks;
    double[][] values;
    TreeSet<fleetPlacement> possibleFleets;
    int  sinceReset, resetHeatVal;
    int oneWins, twoWins, threeWins, fourWins, fiveWins, oneLimit, twoLimit, threeLimit, fourLimit, fiveLimit, set;//how much each placement has won
    int attackOneLimit,attackOneWins;/*number of wins for not checking same placement*/
    int attackTwoLimit,attackTwoWins;//does check for same placement
    int setAttack;// max number of attackRandom number
    int numA;//the random number that is checked to determine which attack  
    boolean placement1, placement2, placement3, placement4, placement5;//placement selection
    boolean attackOne,attackTwo;
    int lastAttackX, lastAttackY, numGames,maxForPlacement,turnNumber;
    final int UNATTACKED = 9999;
    final int MISS = 9998;
    final int HIT = 9997;
    final int UNKNOWN = 3;
    final int check = 5000;//placement
    boolean didAFleetPlacement,checkingFirst,didEven;
    fleetPlacement placement,placementA,placementB,placementC;
    ArrayList<fleetPlacement> placementList;
    int thePlacement;
    private ArrayList<FirstHit> firstHits0,firstHits1,firstHits2,firstHits3;
    private boolean[] firstPossible = {false,false,false,false};
    int checkFirst = 0;
    int bestIndex;
    int[][][] myPlacements;
    int[] remainingShips;
    boolean spotsTaken[][];//used in making sure ships aren't placed next to each other
    int minHor,minVert;
    
    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
    	myFleet = new Fleet();
    	didAFleetPlacement = false;
        if (numGames % 3 == 0 && numGames > 1) {//modifier
            for (int i = 0; i < hunterSeeker.size(); i++) {
                hunterSeeker.get(i).updateHeat();
            }
        }
        if (this.opponent == opponent) {//same opponent
            numGames++;
            sinceReset++;
        }else{                         //new opponent
           attackOneWins = attackTwoWins =  oneWins = twoWins = threeWins = fourWins = fiveWins = 0;//placement values
           oneLimit = 1000;
           twoLimit = 2000;
           threeLimit = 3000;
           fourLimit = 4000;
           fiveLimit = 5000;//placement values
           set = 5000;//placement value
           setAttack = 5000;
           attackOneLimit = 2500;
           maxForPlacement = 5000;
           sinceReset = 0;
           resetHeatVal = 50000;//modifier
           targeting = new int[10][10];
           remainingShips = new int[5];
           this.opponent = opponent;//update opponent
           numGames = 0;
           board = new trackCoords[10][10];
           theirAttacks = new int[10][10];
           for (int x = 0; x < 10; x++) {
               for (int y = 0; y < 10; y++) {
                   board[x][y] = new trackCoords(x, y);
                   targeting[x][y] = 0;
                   theirAttacks[x][y] = 0; 
               }
           }
           myPlacements = new int[10][10][10];
           spotsTaken =  new boolean[10][10];
           for(int x = 0; x < 10; x++){
        		for(int y = 0; y < 10; y++){
        			for(int ship = 0;ship < 5; ship++){
            			myPlacements[x][y][ship] = 0;
        			}
        		}
        	}
            hunterSeeker = new ArrayList<shipCoord>();
            firstHits0 = new ArrayList<FirstHit>();
            firstHits1 = new ArrayList<FirstHit>();
            firstHits2 = new ArrayList<FirstHit>();
            firstHits3 = new ArrayList<FirstHit>();
            placementList = new ArrayList<fleetPlacement>();
            generateHunterSeeker();
            generateFleets();
        }//every game initialization below
        numOpponentAttacks=0;
        didEven = attackOne = attackTwo = false;
        generator = new Random();
        turnNumber = 0;
        if (numGames % resetHeatVal == 0) {//resetHeatVal modifier-- 1 million disables
            resetHeatValue();
        }
        attackOne = attackTwo = placement1 = placement2 = placement3 = placement4 = placement5 = false;//placement values
	        int num = generator.nextInt(set);//placement value
	        if (num < oneLimit) {//placement
	        	tweedlePlacement();
	            placement1 = true;
	        } else if (num >= oneLimit && num < twoLimit) {
	            if(generator.nextInt(3)==0){
	            	myFleet = fillCorner();
	            }else{
	            	myFleet = oldChrome();
	            }
	            placement2 = true;
	        } else if (num >= twoLimit && num < threeLimit) {
	        	//myFleet = placeFleetEven();
	        	tweedlePlacement2();
	            placement3 = true;
	        } else if (num >= threeLimit && num < fourLimit) {
	        	 myFleet = setRandom3(false);
	            placement4 = true;
	        } else {
	        	tweedlePlacement2();
	            placement5 = true;
	        }
        if (numGames % check == 0 && numGames > 1) {//end placement
            resetWins();
            resetAttackWins();
        }
        numA = generator.nextInt(setAttack);
        if(numA < attackOneLimit){
        	//do nothing, don't check for same ship placement
        	checkingFirst = false;
        	attackOne = true;
        }else{
 	       firstPossible[numGames%4] = false;//don't check for this game cuz writing into it
 	       firstPossible[(numGames + 1)%4] = true;
 	       firstPossible[(numGames + 2)%4] = true;
 	       firstPossible[(numGames + 3)%4] = true;
 	       checkingFirst = true;
 	       checkFirst = (numGames+3)%4;//check previous placement first
        	attackTwo = true;
        }
        minVert = minHor =  remainingShips[PATROL_BOAT] = remainingShips[SUBMARINE]= remainingShips[DESTROYER] = remainingShips[BATTLESHIP]= remainingShips[AIRCRAFT_CARRIER]= 0;
        attackVector = new ArrayList<shipCoord>();
        allCoords = new ArrayList<shipCoord>();
        values = new double[10][10];  
        generatePossiblePlacements();
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                board[x][y].status = UNATTACKED;
                values[x][y] = 0;
                spotsTaken[x][y]=false;
            }
        }
        setValues();
    }
    
    void tweedlePlacement(){
    	this.didAFleetPlacement= true;
	    placement = possibleFleets.pollLast();
        placementA = possibleFleets.pollLast();
        placementB = possibleFleets.pollLast();
        placementC=possibleFleets.pollLast();
        thePlacement = generator.nextInt(4);
        switch(thePlacement){
	        case 0:
        	for (int ship = 0; ship < 5; ship++) {
                myFleet.placeShip(placement.thisPlacement[ship] % 10, (placement.thisPlacement[ship] / 10) % 10, (placement.thisPlacement[ship] / 100), ship);
            }
	        break;
	        case 1:
        	for (int ship = 0; ship < 5; ship++) {
                myFleet.placeShip(placementA.thisPlacement[ship] % 10, (placementA.thisPlacement[ship] / 10) % 10, (placementA.thisPlacement[ship] / 100), ship);
            }
		    break;
	        case 2:
        	for (int ship = 0; ship < 5; ship++) {
                myFleet.placeShip(placementB.thisPlacement[ship] % 10, (placementB.thisPlacement[ship] / 10) % 10, (placementB.thisPlacement[ship] / 100), ship);
            }
        	break;
	        case 3:
        	for (int ship = 0; ship < 5; ship++) {
                myFleet.placeShip(placementC.thisPlacement[ship] % 10, (placementC.thisPlacement[ship] / 10) % 10, (placementC.thisPlacement[ship] / 100), ship);
            }
        }
    }
    
    void tweedlePlacement2(){
    	this.didAFleetPlacement= true;
	    placement = possibleFleets.pollLast();
        placementA = possibleFleets.pollLast();
        placementB = possibleFleets.pollLast();
        placementC = possibleFleets.pollLast();
        Fleet one = new Fleet();
        Fleet two = new Fleet();
        Fleet three = new Fleet();
        Fleet four = new Fleet();
    	for (int ship = 0; ship < 5; ship++) {
            one.placeShip(placement.thisPlacement[ship] % 10, (placement.thisPlacement[ship] / 10) % 10, (placement.thisPlacement[ship] / 100), ship);
            two.placeShip(placementA.thisPlacement[ship] % 10, (placementA.thisPlacement[ship] / 10) % 10, (placementA.thisPlacement[ship] / 100), ship);
            three.placeShip(placementB.thisPlacement[ship] % 10, (placementB.thisPlacement[ship] / 10) % 10, (placementB.thisPlacement[ship] / 100), ship);
            four.placeShip(placementB.thisPlacement[ship] % 10, (placementB.thisPlacement[ship] / 10) % 10, (placementB.thisPlacement[ship] / 100), ship);
        }
    	int score1 = timesPlaced(one);
    	int score2 = timesPlaced(two);
    	int score3 = timesPlaced(three);
    	int score4 = timesPlaced(four);
    	if(score4 < score3 && score4 < score2 && score4 < score1){
    		thePlacement = 3;
    		myFleet = four;
    	}else if(score3 < score2 && score3 < score1){
    		thePlacement = 2;
    		myFleet = three;
    	}else if(score2 < score1){
    		thePlacement = 1;
    		myFleet = two;
    	}else{
    		thePlacement = 0;
    		myFleet = one;
    	}	
    }
    
    int timesPlaced(Fleet f){
    	int total = 0;
    	for(int ship = 0; ship < 5;ship++){
    		Coordinate spot = f.getFleet()[ship].location;
    		int x = spot.getX();
    		int y = spot.getY();
    		int orient = f.getFleet()[ship].direction;
        	int length = this.shipLengths[ship];
            switch (orient) {
    	        case VERTICAL:
	            for (int z = y; z < y + length; z++) {
	            	total += myPlacements[x][z][ship];
	            }
    	            break;
    	        case HORIZONTAL:
	            for (int z = x; z < x + length; z++) {
	            	total += myPlacements[z][y][ship];
	            }
    	    }
    	}
    	return total;
    }
    
    private void resetWins() {//placement probability
    	if(oneWins < 5){
    		int diff = 5 - oneWins;
    		oneWins = 5;
    		set += diff;
    	}
    	if(twoWins < 5){
    		int diff = 5 - twoWins;
    		twoWins = 5;
    		set += diff;
    	}
    	if(threeWins < 5){
    		int diff = 5 - threeWins;
    		threeWins = 5;
    		set += diff;
    	}
    	if(fourWins < 5){
    		int diff = 5 - fourWins;
    		fourWins = 5;
    		set += diff;
    	}
    	if(fiveWins < 5){
    		int diff = 5 - fiveWins;
    		fiveWins = 5;
    		set += diff;
    	}
        int total = oneWins + twoWins + threeWins + fourWins + fiveWins;   
        oneLimit = (int) (((float) oneWins / (float) total) * (float) set);
        twoLimit = ((int) (((float) twoWins / (float) total) * (float) set) + oneLimit);
        threeLimit = ((int) (((float) threeWins / (float) total) * (float) set) + twoLimit);
        fourLimit = ((int) (((float) fourWins / (float) total) * (float) set) + threeLimit);
        oneWins = twoWins = threeWins = fourWins = fiveWins = 0;
    }
    
    private void resetAttackWins(){
    	if(attackOneWins < 5){
    		int diff = 5 - attackOneWins;
    		attackOneWins = 5;
    		setAttack += diff;
    	}
    	if(attackTwoWins < 5){
    		int diff = 5 - attackTwoWins;
    		attackTwoWins = 5;
    		setAttack += diff;
    	}
        int total = attackOneWins+attackTwoWins;   
        attackOneLimit = (int) (((float) attackOneWins / (float) total) * (float) setAttack);
      attackOneWins = attackTwoWins = 0;
    }
    
    private void generateHunterSeeker() {
        int index = 0;
        for (int ship = 0; ship < 5; ship++) {
            for (int y = 0; y < 10 - shipLengths[ship] + 1; y++) {
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
    	remainingShips[PATROL_BOAT]=remainingShips[SUBMARINE] = remainingShips[DESTROYER] = remainingShips[BATTLESHIP] = remainingShips[AIRCRAFT_CARRIER] = 0;
        int index = 0;
        for (int ship = 4; ship > -1; ship--) {
            for (int y = 0; y < 10 - shipLengths[ship] + 1; y++) {
                for (int x = 0; x < 10; x++) {
                    allCoords.add(new shipCoord(x, y, 1, ship, index));
                    index++;
                    allCoords.add(new shipCoord(y, x, 0, ship, index));
                    index++;
                    remainingShips[ship]+=2;
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
    
    public class FirstHit{
    	int ship;
    	Coordinate spot;
    	FirstHit(int x, int y, int ship){
    		spot = new Coordinate(x,y);
    		this.ship = ship;
    	}
    }
    
    class fleetPlacement {
        int[] thisPlacement;
        int score,identifier;

        fleetPlacement(int[] thisPlacement, int identifier) {
            this.thisPlacement = thisPlacement;
            score = 500;
            this.identifier = identifier;
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
        void updateHeat() {
            heat = used / (double) sinceReset + 0.00000001;//updates heat for that placement
        }
    }
    
    void resetHeatValue() {
        sinceReset = 0;
        for (int i = 0; i < hunterSeeker.size(); i++) {
            hunterSeeker.get(i).used = 0;
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
            length = shipLengths[allCoords.get(i).ship];
            orient = allCoords.get(i).orientation;
            switch (orient) {
            case VERTICAL:
                for (int z = remY; z < remY + length; z++) {
                    values[remX][z] += hunterSeeker.get(allCoords.get(i).index).heat/(float)this.remainingShips[allCoords.get(i).ship];
                }
                break;
            case HORIZONTAL:
                for (int z = remX; z < remX + length; z++) {
                    values[z][remY] += hunterSeeker.get(allCoords.get(i).index).heat/(float)this.remainingShips[allCoords.get(i).ship];
                }
            }
        }
    }
    
    void removeHeat(int x, int y, int orient,int ship,int i){
    	int length = this.shipLengths[ship];
        switch (orient) {
	        case VERTICAL:
	            for (int z = y; z < y + length; z++) {
	                values[x][z] -= hunterSeeker.get(allCoords.get(i).index).heat;
	            }
	            break;
	        case HORIZONTAL:
	            for (int z = x; z < x + length; z++) {
	                values[z][y] -= hunterSeeker.get(allCoords.get(i).index).heat;
	            }
	    }
    }
    
    void removeMiss(int x, int y) {//removes the possible opponent placements that overlap the shot(for each ship).
        int remX, remY, length, orient, ship;
        for (int i = 0; i < allCoords.size(); i++) {
            ship = allCoords.get(i).ship;
            remX = allCoords.get(i).x;
            remY = allCoords.get(i).y;
            length = shipLengths[allCoords.get(i).ship];
            orient = allCoords.get(i).orientation;
            if (x == remX && y <= remY + length - 1 && y >= remY && orient == VERTICAL) {//vertical removal
            	removeHeat(remX,remY,VERTICAL,ship,i);
                allCoords.remove(i);
                i--;
                remainingShips[ship]--;
            } else if (y == remY && x <= remX + length - 1 && x >= remX && orient == HORIZONTAL) {//horizontal removal
            	removeHeat(remX,remY,HORIZONTAL,ship,i);
                allCoords.remove(i);
                i--;
                remainingShips[ship]--;
            }//else
        }//for loop
    }
    
    void removeHit(int x, int y, int hitShip) {
        int remX, remY, length, orient, ship;
        for (int i = 0; i < allCoords.size(); i++) {
            ship = allCoords.get(i).ship;
            remX = allCoords.get(i).x;
            remY = allCoords.get(i).y;
            length = shipLengths[ship];
            orient = allCoords.get(i).orientation;
            if (x != remX && y != remY && hitShip == ship) {//vertical removal
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            } else if (y == remY && hitShip == ship && orient == VERTICAL && x != remX) {
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            } else if (x == remX && hitShip == ship && orient == HORIZONTAL && y != remY) {
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            } else if (x == remX && ship == hitShip && remY > y) {//same col above
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            } else if (y == remY && ship == hitShip && x < remX) {
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            } else if (x == remX && (remY + length - 1) < y && ship == hitShip && orient == VERTICAL) {//vertical removal
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            } else if (y == remY && (remX + length - 1) < x && ship == hitShip && orient == HORIZONTAL) {
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            }// above removed same ship that couldn't be on hit square
            if (x == remX && y <= remY + length - 1 && y >= remY && orient == VERTICAL && ship != hitShip) {//vertical removal
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            }
            if (y == remY && x <= remX + length - 1 && x >= remX && orient == HORIZONTAL && ship != hitShip) {//horizontal removal
                allCoords.remove(i);
                remainingShips[ship]--;
                i--;
            }
        }
    } 
    
	public void recordShipPlacement(int x, int y , int orientation, int ship){
		if(orientation == VERTICAL){	
			for(int i = y; i < shipLengths[ship]+y;i++){
				myPlacements[x][i][ship]++;
			}
		}else{
			for(int i = x; i < shipLengths[ship]+x;i++){
				myPlacements[i][y][ship]++;
			}
		}
	}
	
    @Override
    public Coordinate makeAttack() {
    	turnNumber++;
    	int bestX,bestY;
    	if(checkingFirst){//NEW->attack at a spot that was a hit in the last match(beats StanleyHTweedle's placement)
        	Coordinate best = findFirst(checkFirst);
        	bestX = best.getX();
        	bestY = best.getY();
        }else{
	        bestX = generator.nextInt(10);
	        bestY = generator.nextInt(10);
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
	                        	if(this.attackVector.size()>0){
	                        		if(openAround(x,y)>openAround(bestX,bestY)){
	                        			bestX=x;
	                        			bestY=y;
	                        		}
	                        	}else{
		                            if (targeting[x][y] < targeting[bestX][bestY]) {
		                                bestX = x;
		                                bestY = y;
		                            }
	                        	}
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
    
    public Coordinate findFirst(int firstIndex){
    	Coordinate best = null;
        switch(firstIndex){
	        case 0:
	        	if(firstHits0.size()>0){
	        		best = firstHits0.remove(0).spot;
	        	}
	        break;
	        case 1:
	        	if(firstHits1.size()>0){
	        		best = firstHits1.remove(0).spot;
	        	}
		    break;
	        case 2:
	        	if(firstHits2.size()>0){
	        		best = firstHits2.remove(0).spot;
	        	}
		    break;
	        case 3:
	        	if(firstHits3.size()>0){
	        		best = firstHits3.remove(0).spot;
	        	}
	    }
        if(best == null){
        	best = randomShot();
        }
        return best;
    }
    
    int openAround(int x,int y){
    	int total = 0;
    	if(x < 9){
    		if(this.board[x+1][y].status == UNATTACKED){
    			total++;
    		}
    	}
    	if(x > 0){
    		if(this.board[x-1][y].status == UNATTACKED){
    			total++;
    		}
    	}
    	if(y < 9){
    		if(this.board[x][y+1].status == UNATTACKED){
    			total++;
    		}
    	}
    	if(y > 0){
    		if(this.board[x][y-1].status == UNATTACKED){
    			total++;
    		}
    	}
    	return total;
    }
    
    Coordinate randomShot(){
    	Coordinate shot = new Coordinate(generator.nextInt(10),generator.nextInt(10));
    	while(board[shot.getX()][shot.getY()].status != this.UNATTACKED){
    		shot = new Coordinate(generator.nextInt(10),generator.nextInt(10));
    	}
    	return shot;
    }
    
    public void incrementFirst(){
    	this.firstPossible[checkFirst] = false;
    	int count = 0;
    	while(!firstPossible[checkFirst]){
    		checkFirst++;
    		checkFirst = checkFirst % 4;
    		if(count == 4){
    			checkingFirst = false;
    			break;
    		}
    		count++;
    	}
    }
    
    @Override
    public void resultOfAttack(int result) {
    	int hitShip = result % 10;
        if (result == 106) {
            removeMiss(lastAttackX, lastAttackY);
            if(checkingFirst){
            	incrementFirst();
            }
        }else if (result / 10 == 1) {// shot was a hit
            board[lastAttackX][lastAttackY].status = HIT;
            boolean inattackVector = false;//whether or not the hit ship is in list
            for (int i = 0; i < attackVector.size(); i++) {//go through list of hit ships
                if (attackVector.get(i).ship == hitShip) {
                    inattackVector = true;
                }
            }
            if (!inattackVector) {
            	updateFirstHits(lastAttackX,lastAttackY,hitShip);
                attackVector.add(new shipCoord(lastAttackX, lastAttackY, UNKNOWN, hitShip,attackVector.size()));
                switch(numGames % 4){//NEW-> record first hit if ship isn't being targeted
	                case 0:
	                	firstHits0.add(new FirstHit(lastAttackX,lastAttackY,hitShip));
	                break;
	                case 1:
	                	firstHits1.add(new FirstHit(lastAttackX,lastAttackY,hitShip));
	                break;
	                case 2:
	                	firstHits2.add(new FirstHit(lastAttackX,lastAttackY,hitShip));
	                break;
	                case 3:
	                	firstHits3.add(new FirstHit(lastAttackX,lastAttackY,hitShip));
                }
            }
            removeHit(lastAttackX, lastAttackY, hitShip);
        }else if (result / 10 == 2) {//sunk ship
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
                    //i--;
                }
            }
        }
        setValues();
    }
    
    private void updateFirstHits(int x, int y, int ship) {
		if(firstPossible[0]){
			for(int i = 0; i < firstHits0.size();i++){
				if(firstHits0.get(i).ship == ship){
					if(firstHits0.get(i).spot.getX() != x && firstHits0.get(i).spot.getY() != y){
						firstPossible[0]=false;
					}
				}
			}
		}
		if(firstPossible[1]){
			for(int i = 0; i < firstHits1.size();i++){
				if(firstHits1.get(i).ship == ship){
					if(firstHits1.get(i).spot.getX() != x && firstHits1.get(i).spot.getY() != y){
						firstPossible[0]=false;
					}
				}
			}
		}
		if(firstPossible[2]){
			for(int i = 0; i < firstHits2.size();i++){
				if(firstHits2.get(i).ship == ship){
					if(firstHits2.get(i).spot.getX() != x && firstHits2.get(i).spot.getY() != y){
						firstPossible[2]=false;
					}
				}
			}
		}
		if(firstPossible[3]){
			for(int i = 0; i < firstHits3.size();i++){
				if(firstHits3.get(i).ship == ship){
					if(firstHits3.get(i).spot.getX() != x && firstHits3.get(i).spot.getY() != y){
						firstPossible[3]=false;
					}
				}
			}
		}
		if(!firstPossible[0] && !firstPossible[1] && !firstPossible[2] && !firstPossible[3]){
			checkingFirst = false;
		}
	}
    
	@Override
    public void opponentAttack(Coordinate coord) {
    	numOpponentAttacks++;
    }
	
    @Override
    public void resultOfGame(int result) {
        switch(numGames % 4){
	        case 0:
	        	firstHits1.clear();
	        break;
	        case 1:
	        	firstHits2.clear();
	        break;
	        case 2:
	        	firstHits3.clear();
	        break;
	        case 3:
	        	firstHits0.clear();
        }
        if (result == LOST) {
            for (int i = 0; i < allCoords.size(); i++) {//if game is lost distributes heat over remaining placements
                hunterSeeker.get(allCoords.get(i).index).used += 1.0 / /*getShipsLeft(allCoords.get(i).ship)*/remainingShips[allCoords.get(i).ship];
            }
        } else {     //placement probability
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
            if(attackOne){
            	attackOneWins++;
            }else{
            	attackTwoWins++;
            }
        }
        if(didAFleetPlacement){
	        switch(thePlacement){
	        	case 0:
			        if (result == LOST) {
			            placement.score = numOpponentAttacks;
			        } else if (numOpponentAttacks > placement.score) {
			            placement.score = numOpponentAttacks;
			        }
		        break;
	        	case 1:
			        if (result == LOST) {
			            placementA.score = numOpponentAttacks;
			        } else if (numOpponentAttacks > placementA.score) {
			            placementA.score = numOpponentAttacks;
			        }
		        break;
	        	case 2:
			        if (result == LOST) {
			            placementB.score = numOpponentAttacks;
			        } else if (numOpponentAttacks > placementB.score) {
			            placementB.score = numOpponentAttacks;
			        }
			    break;
	        	case 3:
			        if (result == LOST) {
			            placementC.score = numOpponentAttacks;
			        } else if (numOpponentAttacks > placementA.score) {
			            placementC.score = numOpponentAttacks;
			        }
	        }
	        possibleFleets.add(placement);
	        possibleFleets.add(placementA);
	        possibleFleets.add(placementB);
	        possibleFleets.add(placementC);
        }
        if(numGames%100==0){//TODO can mess with this number
            addFleetToTree(this.placeFleetEven(),possibleFleets.pollFirst().identifier,false);
        }
    }
    
    Fleet oldChrome() {
    	Fleet f = new Fleet();
    	Random g = new Random();
        int chromo = g.nextInt(4);
        int mod = g.nextInt(8);
        switch (chromo) {
            case 0:
                f.placeShip(1 + mod, 3, VERTICAL, PATROL_BOAT);
                f.placeShip(0 + mod, 0, VERTICAL, SUBMARINE);
                f.placeShip(2 + mod, 0, VERTICAL, DESTROYER);
                f.placeShip(0 + mod, 5, VERTICAL, BATTLESHIP);
                f.placeShip(2 + mod, 5, VERTICAL, AIRCRAFT_CARRIER);
                break;
            case 1:
                f.placeShip(3, 1 + mod, HORIZONTAL, PATROL_BOAT);
                f.placeShip(0, 0 + mod, HORIZONTAL, SUBMARINE);
                f.placeShip(0, 2 + mod, HORIZONTAL, DESTROYER);
                f.placeShip(5, 0 + mod, HORIZONTAL, BATTLESHIP);
                f.placeShip(5, 2 + mod, HORIZONTAL, AIRCRAFT_CARRIER);
                break;
            case 2:
                f.placeShip(1 + mod, 5, VERTICAL, PATROL_BOAT);
                f.placeShip(0 + mod, 7, VERTICAL, SUBMARINE);
                f.placeShip(2 + mod, 7, VERTICAL, DESTROYER);
                f.placeShip(2 + mod, 1, VERTICAL, BATTLESHIP);
                f.placeShip(0 + mod, 0, VERTICAL, AIRCRAFT_CARRIER);
                break;
            case 3:
                f.placeShip(5, 1 + mod, HORIZONTAL, PATROL_BOAT);
                f.placeShip(7, 0 + mod, HORIZONTAL, SUBMARINE);
                f.placeShip(7, 2 + mod, HORIZONTAL, DESTROYER);
                f.placeShip(1, 0 + mod, HORIZONTAL, BATTLESHIP);
                f.placeShip(0, 2 + mod, HORIZONTAL, AIRCRAFT_CARRIER);
        }
        return f;
    }
    
	Fleet fillCorner() {
		Random g = new Random();
		Fleet f = new Fleet();
	    ArrayList<Integer> ships = new ArrayList<Integer>();
	    for (int i = 1; i < 5; i++) {
	        ships.add(i);
	    }
	    Collections.shuffle(ships);
	    int orientation = g.nextInt(2);
	    switch (g.nextInt(4)) {
	        case 0://bottom right
	            f.placeShip(8 + orientation, 0, orientation, PATROL_BOAT);
	            f.placeShip(7 -shipLengths[ships.get(0)] + orientation, 0, HORIZONTAL, ships.get(0));
	            f.placeShip(8 - shipLengths[ships.get(1)], 1, HORIZONTAL, ships.get(1));
	            f.placeShip(8, 2, VERTICAL, ships.get(2));
	            f.placeShip(9, 2 + orientation, VERTICAL, ships.get(3));
	            break;
	        case 1://bottom left
	            f.placeShip(0, 0, orientation, PATROL_BOAT);
	            f.placeShip(3 - orientation, 0, HORIZONTAL, ships.get(0));
	            f.placeShip(2, 1, HORIZONTAL, ships.get(1));
	            f.placeShip(1, 2, VERTICAL, ships.get(2));
	            f.placeShip(0, 2 + orientation, VERTICAL, ships.get(3));
	            break;
	        case 2://top right
	            f.placeShip(8 + orientation, 9 - orientation, orientation, PATROL_BOAT);
	            f.placeShip(7 - shipLengths[ships.get(0)] + orientation, 9, HORIZONTAL, ships.get(0));
	            f.placeShip(8 - shipLengths[ships.get(1)], 8, HORIZONTAL, ships.get(1));
	            f.placeShip(8, 8 - shipLengths[ships.get(2)], VERTICAL, ships.get(2));
	            f.placeShip(9, 8 - shipLengths[ships.get(3)] - orientation, VERTICAL, ships.get(3));
	            break;
	        case 3://top left
	            f.placeShip(0, 9 - orientation, orientation, PATROL_BOAT);
	            f.placeShip(3 - orientation, 9, HORIZONTAL, ships.get(0));
	            f.placeShip(2, 8, HORIZONTAL, ships.get(1));
	            f.placeShip(1, 8 - shipLengths[ships.get(2)], VERTICAL, ships.get(2));
	            f.placeShip(0, 8 - shipLengths[ships.get(3)] - orientation, VERTICAL, ships.get(3));
	        }
	    return f;
	}
	
	Fleet fillCorner2() {
		Random g = new Random();
		Fleet f = new Fleet();
	    ArrayList<Integer> ships = new ArrayList<Integer>();
	    for (int i = 1; i < 5; i++) {
	        ships.add(i);
	    }
	    Collections.shuffle(ships);
	    int orientation = g.nextInt(2);
	    switch (g.nextInt(4)) {
	        case 0://bottom right
	            if(!f.placeShip(8 + orientation, 0, orientation, PATROL_BOAT)){System.out.println("1");}
	            if(!f.placeShip(8 -shipLengths[ships.get(0)] + orientation, 0, HORIZONTAL, ships.get(0))){System.out.println("2");}
	            if(!f.placeShip(9, 1 + orientation, VERTICAL, ships.get(3))){System.out.println("4");}
	            f.placeShip(9 - shipLengths[ships.get(1)], 1, HORIZONTAL, ships.get(1));
	            while(!f.placeShip(g.nextInt(10), g.nextInt(10), g.nextInt(2), ships.get(2))){
	            }
	            break;
	        case 1://bottom left
	        	if(!f.placeShip(0, 0, orientation, PATROL_BOAT)){System.out.println("5");}
	        	if(!f.placeShip(2 - orientation, 0, HORIZONTAL, ships.get(0))){System.out.println("6");}
	        	if(!f.placeShip(0, 1 + orientation, VERTICAL, ships.get(3))){System.out.println("7");}
	        	f.placeShip(1, 1, HORIZONTAL, ships.get(1));
	            while(!f.placeShip(g.nextInt(10), g.nextInt(10), g.nextInt(2), ships.get(2))){
	            }
	            break;
	        case 2://top right
	        	if(!f.placeShip(8 + orientation, 9 - orientation, orientation, PATROL_BOAT)){System.out.println("8");}
	        	if(!f.placeShip(8 - shipLengths[ships.get(0)] + orientation, 9, HORIZONTAL, ships.get(0))){System.out.println("9");}
	        	if(!f.placeShip(9, 9 - shipLengths[ships.get(3)] - orientation, VERTICAL, ships.get(3))){System.out.println("11");}
	        	f.placeShip(9 - shipLengths[ships.get(1)], 8, HORIZONTAL, ships.get(1));
	            while(!f.placeShip(g.nextInt(10), g.nextInt(10), g.nextInt(2), ships.get(2))){
	            }
	            break;
	        case 3://top left
	        	if(!f.placeShip(0, 9 - orientation, orientation, PATROL_BOAT)){System.out.println("12");}
	        	if(!f.placeShip(2 - orientation, 9, HORIZONTAL, ships.get(0))){System.out.println("13");}
	        	if(!f.placeShip(0, 9 - shipLengths[ships.get(3)] - orientation, VERTICAL, ships.get(3))){System.out.println("15");}
	        	f.placeShip(1, 8, HORIZONTAL, ships.get(1));
	            while(!f.placeShip(g.nextInt(10), g.nextInt(10), g.nextInt(2), ships.get(2))){
	            }
	        }
	    return f;
	}
	
    @Override
    public Fleet getFleet() {
		for(int ship = 0;ship<5;ship++){
			recordShipPlacement(myFleet.getFleet()[ship].location.getX(),myFleet.getFleet()[ship].location.getY(),myFleet.getFleet()[ship].direction,myFleet.getFleet()[ship].model);
		}
        return myFleet;
    }
    
    public void generateFleets() {
        possibleFleets = new TreeSet<>(new Comparator<fleetPlacement>() {
            @Override
            public int compare(fleetPlacement fp1, fleetPlacement fp2) {
                if (fp1.score != fp2.score) {
                    return fp1.score - fp2.score;
                } else {
                    return fp1.identifier - fp2.identifier;
                }
            }
        });
        for(int i = 0; i < 200;i++){//TODO can mess with how much of each placement type goes into the array of 10,000 placements
            addFleetToTree(setRandom3(true),i,true);
        }
        for(int i = 200; i < 225; i++){
        	addFleetToTree(oldChrome(),i,true);
        }
        for(int i = 225; i < 240; i++){
        	addFleetToTree(fillCorner(),i,true);
        }
        for (int i = 240; i < fleetSize-20; i++) {
            addFleetToTree(this.placeFleetEven(),i,true);
        }
        for (int i = fleetSize-20; i < fleetSize; i++) {
            addFleetToTree(fillCorner2(),i,true);
        }
        for(int sx = 0; sx < 10; sx++){
     		for(int sy = 0; sy < 10; sy++){
     			for(int ship = 0;ship < 5; ship++){
         			myPlacements[sx][sy][ship] = 0;
     			}
     		}
     	}
    }
    
    void addFleetToTree(Fleet f,int i,boolean record){
    	int[] temp = new int[5];
        for (int ship = 0; ship < 5; ship++) {
            temp[ship] = f.fleet[ship].location.getX() + 10 * f.fleet[ship].location.getY() + 100 * f.fleet[ship].direction;
        }
        if(record){
	        for(int ship = 0;ship<5;ship++){
				recordShipPlacement(f.getFleet()[ship].location.getX(),f.getFleet()[ship].location.getY(),f.getFleet()[ship].direction,f.getFleet()[ship].model);
			}
        }
        for(int sx = 0;sx<10;sx++){
        	for(int sy = 0; sy < 10;sy++){
        		this.spotsTaken[sx][sy]=false;
        	}
        }
        possibleFleets.add(new fleetPlacement(temp, i));
    }
    
    public Fleet placeFleetEven(){
    	Fleet newFleet = new Fleet();
    	Random g = new Random();
        for(int ship =4; ship>=0;ship--){
        	Coordinate vertSquare = findLeastPlacedSquareVertEven(ship);
        	Coordinate horSquare = findLeastPlacedSquareHorEven(ship);
        	if(minVert < minHor){
        		if(!newFleet.placeShip(vertSquare.getX(),vertSquare.getY(), VERTICAL, ship)){
					if(!newFleet.placeShip(horSquare.getX(),horSquare.getY(), HORIZONTAL, ship)){
	        			while (!newFleet.placeShip(g.nextInt(10),g.nextInt(10),g.nextInt(2),ship)) {
	        	        }
	        		}else{takeSpots(horSquare.getX(),horSquare.getY(), HORIZONTAL, shipLengths[ship]);}
	        	}else{takeSpots(vertSquare.getX(),vertSquare.getY(), VERTICAL, shipLengths[ship]);}
        	}else{
        		if(!newFleet.placeShip(horSquare.getX(),horSquare.getY(), HORIZONTAL, ship)){
					if(!newFleet.placeShip(vertSquare.getX(),vertSquare.getY(), VERTICAL, ship)){
	        			while (!newFleet.placeShip(g.nextInt(10),g.nextInt(10),g.nextInt(2),ship)) {
	        	        }
	        		}else{takeSpots(vertSquare.getX(),vertSquare.getY(), VERTICAL, shipLengths[ship]);}
	        	}else{takeSpots(horSquare.getX(),horSquare.getY(), HORIZONTAL, shipLengths[ship]);}
        	}
        }
        return newFleet;
    }
    
    public Coordinate findLeastPlacedSquareVertEven(int ship){
		int min = Integer.MAX_VALUE;
		int bestSquare = 0;
		switch(shipLengths[ship]){
			case 2:
				for(int x = 0;x< 10;x++){
					for(int y = 0; y <= 10-shipLengths[ship]; y++){
						if(isValidSpot(x,y,VERTICAL,2)){
							if(myPlacements[x][y][ship]+myPlacements[x][y+1][ship] < min){
								min = myPlacements[x][y][ship]+myPlacements[x][y+1][ship];
								bestSquare = x*10+y;
							}
						}
					}
				}
			break;
			case 3:
				for(int x = 0;x< 10;x++){
					for(int y = 0; y <= 10-shipLengths[ship]; y++){
						if(isValidSpot(x,y,VERTICAL,3)){
							if(myPlacements[x][y][ship]+myPlacements[x][y+1][ship] + myPlacements[x][y+2][ship]< min){
								min = myPlacements[x][y][ship]+myPlacements[x][y+1][ship] + myPlacements[x][y+2][ship];
								bestSquare = x*10+y;
							}
						}
					}
				}
			break;
			case 4:
				for(int x = 0;x< 10;x++){
					for(int y = 0; y <= 10-shipLengths[ship]; y++){
						if(isValidSpot(x,y,VERTICAL,4)){
							if(myPlacements[x][y][ship]+myPlacements[x][y+1][ship] + myPlacements[x][y+2][ship] + myPlacements[x][y+3][ship] < min){
								min = myPlacements[x][y][ship]+myPlacements[x][y+1][ship] + myPlacements[x][y+2][ship] + myPlacements[x][y+3][ship];
								bestSquare = x*10+y;
							}
						}
					}
				}
			break;
			case 5:
				for(int x = 0;x< 10;x++){
					for(int y = 0; y <= 10-shipLengths[ship]; y++){
						if(isValidSpot(x,y,VERTICAL,5)){
							if(myPlacements[x][y][ship]+myPlacements[x][y+1][ship] + myPlacements[x][y+2][ship] + myPlacements[x][y+3][ship] +myPlacements[x][y+4][ship] < min){
								min = myPlacements[x][y][ship]+myPlacements[x][y+1][ship] + myPlacements[x][y+2][ship] + myPlacements[x][y+3][ship] +myPlacements[x][y+4][ship];
								bestSquare = x*10+y;
							}
						}
					}
				}
		}
		minVert= min;
		return new Coordinate(bestSquare /10,bestSquare%10);
	}
    
	public Coordinate findLeastPlacedSquareHorEven(int ship){
		int min = Integer.MAX_VALUE;
		int bestSquare = 0;
		switch(shipLengths[ship]){
			case 2:
				for(int y = 0;y< 10;y++){
					for(int x = 0; x <= 10-shipLengths[ship]; x++){
						if(isValidSpot(x,y,HORIZONTAL,2)){
							if(myPlacements[x][y][ship]+myPlacements[x+1][y][ship] < min){
								min = myPlacements[x][y][ship]+myPlacements[x+1][y][ship];
								bestSquare = x*10+y;
							}
						}
					}
				}
			break;
			case 3:
				for(int y = 0;y< 10;y++){
					for(int x = 0; x <= 10-shipLengths[ship]; x++){
						if(isValidSpot(x,y,HORIZONTAL,3)){
							if(myPlacements[x][y][ship]+myPlacements[x+1][y][ship] + myPlacements[x+2][y][ship]< min){
								min = myPlacements[x][y][ship]+myPlacements[x+1][y][ship] + myPlacements[x+2][y][ship];
								bestSquare = x*10+y;
							}
						}
					}
				}
			
			break;
			case 4:
				for(int y = 0;y< 10;y++){
					for(int x = 0; x <= 10-shipLengths[ship]; x++){
						if(isValidSpot(x,y,HORIZONTAL,4)){
							if(myPlacements[x][y][ship]+myPlacements[x+1][y][ship] + myPlacements[x+2][y][ship] + myPlacements[x+3][y][ship] < min){
								min = myPlacements[x][y][ship]+myPlacements[x+1][y][ship] + myPlacements[x+2][y][ship] + myPlacements[x+3][y][ship];
								bestSquare = x*10+y;
							}
						}
					}
				}
			break;
			case 5:
				for(int y = 0;y< 10;y++){
					for(int x = 0; x <= 10-shipLengths[ship]; x++){
						if(isValidSpot(x,y,HORIZONTAL,5)){
							if(myPlacements[x][y][ship]+myPlacements[x+1][y][ship] + myPlacements[x+2][y][ship] + myPlacements[x+3][y][ship] +myPlacements[x+4][y][ship] < min){
								min = myPlacements[x][y][ship]+myPlacements[x+1][y][ship] + myPlacements[x+2][y][ship] + myPlacements[x+3][y][ship] +myPlacements[x+4][y][ship];
								bestSquare = x*10+y;
							}
						}
					}
				}
		}
		minHor = min;
		return new Coordinate(bestSquare /10,bestSquare%10);
	}
	
	private void takeSpots(int x, int y, int orientation, int length){
		if(orientation == VERTICAL){
			for(int i = 0; i < length;i++){
				spotsTaken[x][y+i] = true;
			}
		}else{
			for(int i = 0; i < length;i++){
				spotsTaken[x+i][y] = true;
			}
		}
	}
	
	private boolean isValidSpot(int x, int y,int orientation, int length){
		if(orientation == VERTICAL){
			for(int i = 0;i< length;i++){
				if(spotsTaken[x][y+i]){
					return false;
				}
			}
		}else{
			for(int i = 0;i< length;i++){
				if(spotsTaken[x+i][y]){
					return false;
				}
			}
		}
		return true;
	}
	
	private Fleet setRandom3(boolean allowTouching) {//currently set to false(always)
		Fleet fleet = new Fleet();
		Random g = new Random();
		Coordinate spot;// = findLeastPlacedSquareShip(0);
		int orientation;
		if(numGames%2==0){
			spot = findLeastPlacedSquareVertEven(0);
			orientation = VERTICAL;
		}else{
			spot = findLeastPlacedSquareHorEven(0);
			orientation = HORIZONTAL;
		}
		int twoX = spot.getX();
		int twoY = spot.getY();
		if(orientation == VERTICAL){
			fleet.placeShip(twoX, twoY, VERTICAL, PATROL_BOAT);
		}else{
			fleet.placeShip(twoX, twoY, HORIZONTAL, PATROL_BOAT);
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
                        if (!fleet.placeShip(twoX + 2, twoY + 1, HORIZONTAL,ship)) {//top right Hor
                            if (!fleet.placeShip(twoX + 2, twoY + 1,VERTICAL,ship)) {//top right Vert
                                if (!fleet.placeShip(twoX - shipLengths[ship], twoY + 1, HORIZONTAL,ship)) {//top left Hor
                                    if (!fleet.placeShip(twoX - 1, twoY + 1, VERTICAL,ship)) {//top left Vert
                                        if (!fleet.placeShip(twoX - shipLengths[ship], twoY - 1, HORIZONTAL,ship)) {//bottom left Hor
                                            if (!fleet.placeShip(twoX - 1, twoY - shipLengths[ship], VERTICAL,ship)) {//bottom left Vert
                                                if (!fleet.placeShip(twoX + 2, twoY - 1, HORIZONTAL,ship)) {//bottom right Hor
                                                    if (!fleet.placeShip(twoX + 2, twoY - shipLengths[ship], VERTICAL,ship)) {
                                                        Coordinate square = findLeastPlacedSquareHorEven(ship);
                                    			        if(!fleet.placeShip(square.getX(),square.getY(),HORIZONTAL,ship)){
                                    			        	square = findLeastPlacedSquareVertEven(ship);
                                			        		if(!fleet.placeShip(square.getX(),square.getY(),VERTICAL,ship)){
                                			        			while (!fleet.placeShip(g.nextInt(10), g.nextInt(10), g.nextInt(2), ship)) {
                                			        			}
                                			        		}else{this.takeSpots(square.getX(),square.getY(),VERTICAL,shipLengths[ship]);}
                                    			        }else{this.takeSpots(square.getX(),square.getY(),HORIZONTAL,shipLengths[ship]);}
                                                    }else{this.takeSpots(twoX + 2, twoY - shipLengths[ship], VERTICAL,shipLengths[ship]);}
                                                }else{this.takeSpots(twoX + 2, twoY - 1, HORIZONTAL,shipLengths[ship]);}
                                            }else{this.takeSpots(twoX - 1, twoY - shipLengths[ship], VERTICAL,shipLengths[ship]);}
                                        }else{this.takeSpots(twoX - shipLengths[ship], twoY - 1, HORIZONTAL,shipLengths[ship]);}
                                    }else{this.takeSpots(twoX - 1, twoY + 1, VERTICAL,shipLengths[ship]);}
                                }else{this.takeSpots(twoX - shipLengths[ship], twoY + 1, HORIZONTAL,shipLengths[ship]);}
                            }else{this.takeSpots(twoX + 2, twoY + 1,VERTICAL,shipLengths[ship]);}
                        }else{this.takeSpots(twoX + 2, twoY + 1, HORIZONTAL,shipLengths[ship]);}
                    }
                } else {//sides
                    int mod;// = g.nextInt(2);
                	if(twoX < 5){//left side of board
                		mod = 0;
                	}else{
                		mod = 1;
                	}
                    for (int i = 0; i < 4; i++) {
                        int ship = ships.get(i);
                        if (!fleet.placeShip(twoX + 3, twoY, HORIZONTAL,ship)) {//right side
                            if (!fleet.placeShip(twoX - (shipLengths[ship] + 1), twoY, HORIZONTAL,ship)) {//left side
                                if (!fleet.placeShip(twoX + mod, twoY + 2,VERTICAL,ship)) {//top
                                    if (!fleet.placeShip(twoX + mod, twoY - (shipLengths[ship] + 1), VERTICAL,ship)) {//bottom
                                       	Coordinate square = findLeastPlacedSquareHorEven(ship);
                    			        if(!fleet.placeShip(square.getX(),square.getY(),HORIZONTAL,ship)){
                    			        	square = findLeastPlacedSquareVertEven(ship);
                			        		if(!fleet.placeShip(square.getX(),square.getY(),VERTICAL,ship)){
                			        			while (!fleet.placeShip(g.nextInt(10), g.nextInt(10), g.nextInt(2), ship)) {
                			        			}
                    			        	}else{this.takeSpots(square.getX(),square.getY(),VERTICAL,shipLengths[ship]);}
                                        }else{this.takeSpots(square.getX(),square.getY(),HORIZONTAL,shipLengths[ship]);}
                                    }else{this.takeSpots(twoX + mod, twoY - (shipLengths[ship] + 1), VERTICAL,shipLengths[ship]);}
                                }else{this.takeSpots(twoX + mod, twoY + 2,VERTICAL,shipLengths[ship]);}
                            }else{this.takeSpots(twoX - (shipLengths[ship] + 1), twoY, HORIZONTAL,shipLengths[ship]);}
                        }else{this.takeSpots(twoX + 3, twoY, HORIZONTAL,shipLengths[ship]);}
                    }
                }
                break;
            case VERTICAL:
                if (twoX != 0 && twoX != 9 && twoY != 0 && twoY != 8) {//corners
                    for (int i = 0; i < 4; i++) {
                        int ship = ships.get(i);
                        if (!fleet.placeShip(twoX + 1, twoY + 2,VERTICAL,ship)) {//top right vert
                            if (!fleet.placeShip(twoX + 1, twoY + 2, HORIZONTAL,ship)) {//top right hor
                                if (!fleet.placeShip(twoX - 1, twoY + 2, VERTICAL,ship)) {//top left vert
                                    if (!fleet.placeShip(twoX - (shipLengths[ship]), twoY + 2, HORIZONTAL,ship)) {//top left hor
                                        if (!fleet.placeShip(twoX - 1, twoY - shipLengths[ship], VERTICAL,ship)) {//bottom left vert
                                            if (!fleet.placeShip(twoX - shipLengths[ship], twoY - 1, HORIZONTAL,ship)) {//bottom left hor
                                                if (!fleet.placeShip(twoX + 1, twoY - shipLengths[ship], VERTICAL,ship)) {//bottom right vert
                                                    if (!fleet.placeShip(twoX + 1, twoY - 1, HORIZONTAL,ship)) {//bottom right hor
                                                        Coordinate square = findLeastPlacedSquareVertEven(ship);
                                    			        if(!fleet.placeShip(square.getX(),square.getY(),HORIZONTAL,ship)){
                                    			        	square = findLeastPlacedSquareHorEven(ship);
                                			        		if(!fleet.placeShip(square.getX(),square.getY(),VERTICAL,ship)){
                                			        			while (!fleet.placeShip(g.nextInt(10), g.nextInt(10), g.nextInt(2), ship)) {
                                			        			}
                                    			        	}else{this.takeSpots(square.getX(),square.getY(),VERTICAL,shipLengths[ship]);}
                                    			        }else{this.takeSpots(square.getX(),square.getY(),HORIZONTAL,shipLengths[ship]);}
                                                    }else{this.takeSpots(twoX + 1, twoY - 1, HORIZONTAL,shipLengths[ship]);}
                                                }else{this.takeSpots(twoX + 1, twoY - shipLengths[ship], VERTICAL,shipLengths[ship]);}
                                            }else{this.takeSpots(twoX - shipLengths[ship], twoY - 1, HORIZONTAL,shipLengths[ship]);}
                                        }else{this.takeSpots(twoX - 1, twoY - shipLengths[ship], VERTICAL,shipLengths[ship]);}
                                    }else{this.takeSpots(twoX - (shipLengths[ship]), twoY + 2, HORIZONTAL,shipLengths[ship]);}
                                }else{this.takeSpots(twoX - 1, twoY + 2, VERTICAL,shipLengths[ship]);}
                            }else{this.takeSpots(twoX + 1, twoY + 2, HORIZONTAL,shipLengths[ship]);}
                        }else{this.takeSpots(twoX + 1, twoY + 2,VERTICAL,shipLengths[ship]);}
                    }
                } else {//sides
                    int mod;// = g.nextInt(2);
                	if(twoY < 5){//bottom of board
                		mod = 0;
                	}else{
                		mod = 1;
                	}
                    for (int i = 0; i < 4; i++) {
                        int ship = ships.get(i);
                        if (!fleet.placeShip(twoX + 2, twoY + mod,HORIZONTAL,ship)) {//right side
                            if (!fleet.placeShip(twoX - (shipLengths[ship] + 1), twoY + mod/*((mod + 1) % 2)*/, HORIZONTAL,ship)) {//left side
                                if (!fleet.placeShip(twoX, twoY + 3, VERTICAL,ship)) {//top
                                    if (!fleet.placeShip(twoX, twoY - (shipLengths[ship] + 1), VERTICAL,ship)) {//bottom
                                        Coordinate square = findLeastPlacedSquareVertEven(ship);
                    			        if(!fleet.placeShip(square.getX(),square.getY(),HORIZONTAL,ship)){
                    			        	square = findLeastPlacedSquareHorEven(ship);
                			        		if(!fleet.placeShip(square.getX(),square.getY(),VERTICAL,ship)){
                			        			while (!fleet.placeShip(g.nextInt(10),g.nextInt(10),g.nextInt(2),ship)) {
                			        			}
                    			        	}else{this.takeSpots(square.getX(),square.getY(),VERTICAL,shipLengths[ship]);}
                                        }else{this.takeSpots(square.getX(),square.getY(),HORIZONTAL,shipLengths[ship]);}
                                    }else{this.takeSpots(twoX, twoY - (shipLengths[ship] + 1), VERTICAL,shipLengths[ship]);}
                                }else{this.takeSpots(twoX, twoY + 3, VERTICAL,shipLengths[ship]);}
                            }else{this.takeSpots(twoX - (shipLengths[ship] + 1), twoY + mod/*((mod + 1) % 2)*/, HORIZONTAL,shipLengths[ship]);}
                        }else{this.takeSpots(twoX + 2, twoY + mod,HORIZONTAL,shipLengths[ship]);}
                    }
                }
        }
        return fleet;
    }
}