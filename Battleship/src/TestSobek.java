import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

//Who will ryze?
//im losing 1 or 2 games to loco in 1M matches due to going over 60 turns.. figure out what is taking so long to kill it off........ ..... .... ... its because my mod 3 + 2 inteli sometimes will take 70+ turns to find the 2 ship (because the mod 3 will hit spots righ tnext to where the mod 2 did)
//not complete. this sucks. ALPHA version.

//look into adding something where it checks if ships can be next to each other(opponents) and if not then why bother attacking a coordinate around a ship.
//also add something where it checks if the opponent keeps their ships in the same position each game if they are winning.. if so then i can just use that to remember where they kept them last game (if i lost it) and use it against them.
public class TestSobek implements Captain, Constants {
	
	
	//whats the difference between boolean and Boolean?????
	
	
	
	/*
	 * Thank you Khan for the class system and most of all the logging system as well as a base for an adaptive strategy. Corners placement method
	 * Thank you CaptainAmerica for making Khan's corner placement easier to implement.
	 * Thank you RunningGazelle and CaptinStanleyHTweedle for the "CheckerBoard" attack idea. (modding the x/y coordinates)
	 * Thank you Mal for implementing more classes in my AI easier.
	 * -Brian Dassow Jr. 2014-
	 */
	
	public final int[] SHIP_LENGTHS = new int[] {2, 3, 3, 4, 5};
    public final int[] SHIP_HIT_IDS = new int[] {3, 4, 5, 6, 7};
	public final boolean LOGGING = false;
	
	//accuracy = (totalGames == 1) ? totalAttacks : accuracy + ((totalAttacks - accuracy) / totalGames); // this actually records how often a particular square gets hit... LOL
	// ^^^^ can be used!!
	
	//THIS MUST BE CORRECT!!!!!!!!!!!!!!!!!!!
	public final int AMOUNT_OF_PLACEMENT_METHODS = 3;
	public final int AMOUNT_OF_ATTACK_METHODS = 5;
	
	public final int PLACEMENT_CORNERS = 0; // placement names
	public final int PLACEMENT_RANDOM = 1;
	
	public final int ATTACK_RANDOM = 0;//attack names fill this in
	
	public final int GAME_THRESHOLD_TILL_CHECK = 1300001;
	public final int GAME_THRESHOLD_TILL_RESET = 50000;
	public final int GAME_THRESHOLD_TILL_SAMPLE = 1003000;
	
	public final int AMOUNT_OF_WINLOSS_SAMPLE_GAMES = 1000; // maybe instead of having this < have something for each individual one. put it in the constructor.
	public final int AMOUNT_OF_TOTAL_SAMPLE_GAMES = 2000; // great idea!!! ^^ because the adaptive methods would need a longer period of time to kick in... while the nonadaptive ones wont
	public final int AMOUNT_OF_GAMES_NEEDED_FOR_SWITCH = 100;
	
	public final int NEGATIVE = -1;
    
    protected Random generator;
	protected Fleet myFleet;
	
	boolean[] shipSunk, shipHitLastTurn, seeker, shipHOrVGuessed, attackSampling, placementSampling;
	boolean isSeeking, wonLastGame, canThisShipBeThere;
	
	int wins, losses, cycle, whichShip, focusedShip, hitCycle, hitMod, sunkMod,
			numberOfAttacks, xCoord, yCoord, trueTotalGames ,totalGames, diagnosticGames, winCheck, 
			checkCycle, totalNumberOfMatches, currentAttackMethod, currentPlacementMethod,
			random3, random4, random2, index, totalAttacks, totalOpponentAttacks, misses, 
			gamesSinceLastPlacementSwitch, gamesSinceLastAttackSwitch, checkLastTurnHit;
	
	int[]  shipDirection, adaptiveScore, intelligentScore, mod3AttackHits, mod4AttackHits;
	int[][] board, diagBoard;
	
	long latestTime, beginTime;
	
	Coordinate lastAttack, tempCoordinate, tempCoordinate2, attackCoordinate;
	Coordinate[] coordHit;
	
	HashMap<Integer, BoardCoordinate> theirPlacementHash, theirAttackHash;
	Opponent myOpponent = new Opponent("");
	PlacementMethod[] allPlacementMethods;
	AttackMethod[] allAttackMethods;
	Ship[] whereAreMyShips;
	
	public TestSobek() {
		placementSampling = new boolean[AMOUNT_OF_PLACEMENT_METHODS];
		attackSampling = new boolean[AMOUNT_OF_ATTACK_METHODS];
		//System.out.println("Amount of methods: " + (AMOUNT_OF_PLACEMENT_METHODS + AMOUNT_OF_ATTACK_METHODS));
		
		//right spot?
		//allMethods = new Methods[AMOUNT_OF_PLACEMENT_METHODS + AMOUNT_OF_ATTACK_METHODS];
		allAttackMethods = new AttackMethod[AMOUNT_OF_ATTACK_METHODS];
		allPlacementMethods = new PlacementMethod[AMOUNT_OF_PLACEMENT_METHODS];
		
		adaptiveScore = new int[100];
		intelligentScore = new int[100];
		
		shipDirection = new int[5];
		
		shipHOrVGuessed = new boolean[5];
		shipSunk = new boolean[5];
		shipHitLastTurn = new boolean[5];
		seeker = new boolean[5];
		
		coordHit = new Coordinate[5];
		
		lastAttack = new Coordinate(0, 0);
		tempCoordinate = new Coordinate(0, 0);
		tempCoordinate2 = new Coordinate(0, 0);
		
		for(int i = 0; i < 5; i++) {//right spot? check
			shipHitLastTurn[i] = false;
		}
	}

	@Override
	public void initialize(int numMatches, int numCaptains, String opponent) { // THE CAN ANYSHIPBEHERE() method is messing stuff up wtf fix. it works better without it which should NOT happen...,......dmglkwenniowgiongik
		totalNumberOfMatches = numMatches;
		diagBoard = new int[10][10];
		board = new int[10][10];
		mod3AttackHits = new int[3];
		mod4AttackHits = new int[4];
		generator = new Random();
		myFleet = new Fleet();
		
		for (int i = 0; i < 5; i++) {//this is conflicting with the resetShips(); that i have make a new one and blah blah blah whatever stargate time
			shipHOrVGuessed[i] = false;
			shipSunk[i] = false;
			//shipHitLastTurn[i] = false;
			seeker[i] = false;
		}
		
		if (!myOpponent.name.equals(opponent)) {
			for(int i = 0; i < AMOUNT_OF_PLACEMENT_METHODS; i++) {
				placementSampling[i] = true;
			}
			
			for(int i = 0; i < AMOUNT_OF_ATTACK_METHODS; i++) {
				attackSampling[i] = true;
			}
			
			
			theirPlacementHash = new HashMap<Integer, BoardCoordinate>();
			theirAttackHash = new HashMap<Integer, BoardCoordinate>();
			myOpponent = new Opponent(opponent);
			wonLastGame = false;
			trueTotalGames = diagnosticGames = winCheck = totalGames = wins = losses = 0;
			checkCycle = 1;
			

        	currentAttackMethod = 0;
        	currentPlacementMethod = 0;
        	
        	allPlacementMethods[0] = new PlacementMethod("Random");
        	allPlacementMethods[1] = new PlacementMethod("Corners");
        	allPlacementMethods[2] = new PlacementMethod("Adaptive");
        	
        	allAttackMethods[0] = new AttackMethod("mod4 no intelligent");
        	allAttackMethods[1] = new AttackMethod("mod4inteli + mod2noninteli");
        	
        	allAttackMethods[2] = new AttackMethod("mod3+2 no intelligent");
        	allAttackMethods[3] = new AttackMethod("mod3inteli + mod2noninteli");


        	allAttackMethods[4] = new AttackMethod("mod3+2 inteli");
        	
		}

		
		index = misses = totalAttacks = totalOpponentAttacks = 0;
		
		
		mod3AttackHits[0] = 34;
		mod3AttackHits[1] = mod3AttackHits[2] = 33;
		
		mod4AttackHits[0] = 25;
		mod4AttackHits[1] = 26;
		mod4AttackHits[2] = 25;
		mod4AttackHits[3] = 24;
		
		checkLastTurnHit = 0;
		canThisShipBeThere = true;
		
		
		
		

		
		myOpponent.resetStore(); // make sure to test this and see if i actually want to keep the store, if not get rid of it all and revert it back to normal
		myOpponent.resetShips();



		
		for (int i = 0; i < 100; i++) {
			myOpponent.theirPlacementBoard[i].attacked = false;
			myOpponent.theirAttackBoard[i].attacked = false; // dont know if neededidk look
		}

		
		cycle = whichShip = numberOfAttacks = 0;
		
		focusedShip = -1;
		hitCycle = 1;
		
		myOpponent.totalOpponentAttacks = 150; // put somewhere else. 

		isSeeking = false;
		
		myOpponent.sortBoards();
		
		random3 = generator.nextInt(3);
		random2 = generator.nextInt(2);
		random4 = generator.nextInt(4);
		
		placeShipController();	
	}
	
	public void placeShipController() {
		
		
		/*REVIEW THIS THERES STILL STUFF I NEED FROM HERE
		if (totalGames < 10 ) {//|| shipPlacementCycle == 1330
			for (int i = 0; i < 5; i++) {
			//	System.out.println("in here?" + totalGames + " " + diagnosticGames + " " + wins);
				//placeShipRandom(i);
			}
			cornerPlacement();
		}
		else if (wonLastGame) {
			for (int i = 0; i < 5; i++) {
			//	System.out.println("in here?2   " + totalGames + " " + diagnosticGames + " " + wins);
				myFleet.placeShip(whereAreMyShips[i].getLocation(), whereAreMyShips[i].getDirection(), i);
				//placeShipRandom(i);
			}
		} else {
			for (int i = 0; i < 5; i++) {
				//System.out.println("in here?3    "+ totalGames + " " + diagnosticGames + " " + wins);
				placeShipAdaptive3(i);
				
			//	placeShipRandom(i);
			}
			//cornerPlacement();
			// place2ShipCorner();
		}
		
		*/    
		if (wonLastGame) {
			for (int i = 0; i < 5; i++) {
			//	System.out.println("in here?2   " + totalGames + " " + diagnosticGames + " " + wins);
				myFleet.placeShip(whereAreMyShips[i].getLocation(), whereAreMyShips[i].getDirection(), i);
				//placeShipRandom(i);
			}
		}
		else {
		switch (currentPlacementMethod) {
		case 0: 
			for (int i = 0; i < 5; i++) {
				placeShipRandom(i);
			}	
			break;
		case 1:
				cornerPlacement();
			break;
		case 2:
			for (int i = 0; i < 5; i++) {
				placeShipAdaptive3(i);
			}
			break;
		
		}
		
		
		}
		
		
		
		
		
		whereAreMyShips = myFleet.getFleet();
		
	}

	public void place2ShipCorner() {
		
	}
	
	public void uniformPlacement() {
		
	}
	
	public void cornerPlacement() { //khan/CaptinAmerica change this to make it your own style at least... figure out a way of improving it.
        Coordinate bottomLeft = new Coordinate(0, 0);
        Coordinate bottomRight = new Coordinate(0, 9);
        Coordinate topLeft = new Coordinate(9, 0);
        Coordinate topRight = new Coordinate(9, 9);
        Coordinate[] corners = {bottomLeft, bottomRight, topLeft, topRight};
        
        ArrayList<Integer> ships = new ArrayList<Integer>();
        
        for (int i = 0; i < 4; i++) {
            ships.add(i);
        }
        
        Collections.shuffle(ships);
        
        for (int i = 0; i < 4; i++) {
            int ship = ships.get(i);
            int orientation = generator.nextInt(2);
            if (orientation == VERTICAL && corners[i].getY() == 9) {
                if (!myFleet.placeShip(corners[i].getX(), corners[i].getY() - SHIP_LENGTHS[ship] + 1, orientation, ship)) {
                    System.out.println("failed to place ship " + ship + " at coordinates " + corners[i].getX() + "," + corners[i].getY() + "scenario 1");
                    while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                    }
                }
            } else if (orientation == HORIZONTAL && corners[i].getX() == 9) {
                if (!myFleet.placeShip(corners[i].getX() - SHIP_LENGTHS[ship] + 1, corners[i].getY(), orientation, ship)) {
                    System.out.println("failed to place ship " + ship + " at coordinates " + corners[i].getX() + "," + corners[i].getY() + "scenario 2");
                    while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                    }
                }
            } else {
                if (!myFleet.placeShip(corners[i].getX(), corners[i].getY(), orientation, ship)) {
                    System.out.println("failed to place ship " + ship + " at coordinates " + corners[i].getX() + "," + corners[i].getY() + "scenario 3");
                    while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                    }
                }
            }
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), AIRCRAFT_CARRIER)) {
        }
    }


	public void placeShipRandom(int shipID) {
		while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), shipID)) {
		}
	}

	/*
	public void placeShipAdaptive(int shipID) {
		boolean placed = false;
		for (int i = 99; i > -1; i--) {
			for (int j = 0; j < 100; j++) {
				if (myOpponent.theirAttackBoard[j].score == adaptiveScore[i]) {
					if (myFleet.placeShip(myOpponent.theirAttackBoard[j].x, myOpponent.theirAttackBoard[j].y, generator.nextInt(2), shipID)) {
						placed = true;
						break;
					}
				}
			}
			if (placed == true) {
				placed = false;
				break;
			}
		}
	}*/
	
	public void placeShipAdaptive2(int shipID) {
		int count = 99;
		while(!myFleet.placeShip(theirAttackHash.get(count).x, theirAttackHash.get(count).y, generator.nextInt(2), shipID)) {
			count--;
		}
	}
	
	public void placeShipAdaptive3(int shipID) {
	//	Coordinate cooroo = myOpponent.getBestSquares(shipID);
		int count = 99;
		while(!myFleet.placeShip(theirAttackHash.get(count).x, theirAttackHash.get(count).y, generator.nextInt(2), shipID)) {
			count--;
		}
	}

	@Override
	public Fleet getFleet() {
		return myFleet;
	}

	@Override
	public Coordinate makeAttack() { // redo this. like i can probably take ut the do while  redid it. (maybe check if i can again)
		do {
			tempCoordinate = makeAttackController();			
		} while (tempCoordinate.getX() > 9 || tempCoordinate.getY() > 9 || tempCoordinate.getX() < 0 || tempCoordinate.getY() < 0 || board[tempCoordinate.getX()][tempCoordinate.getY()] != 0 || !canThisShipBeThere);
		
		lastAttack = tempCoordinate;
		numberOfAttacks++;
		board[lastAttack.getX()][lastAttack.getY()] = 1;
		return lastAttack;
	}
	
	public Coordinate makeAttackController() { // cananyShipBehere() doesnt work because its looking for xCoord and yCoord... LOL okay fix it. in fact fix tis whole this because right now its a pile of crap actually that wasnt the fix because theyall used xCoord... for some reason it just isnt working.
		//int checkLastHitTurn = 0;
		
		//printBoard1();
		
		if (!wonLastGame && checkLastTurnHit < 5) {//need something to check if this actually works! (like if they are placing their ships in the same spot.)
			//System.out.println(checkLastHitTurn);
			Coordinate zx = myOpponent.giveLastTurnOppShipCoords();
			
			if (zx.getX() != -1) {
				return zx;
			}
			//if(myOpponent.giveLastTurnOppShipCoords().getX() != -1) {
				
			//}
			
		}
		
		if (focusedShip != -1) {
			isSeeking = true;
			return iWillFindYou(focusedShip); 
		}
		
		if(myOpponent.findLargestShip() < 4 && myOpponent.findLargestShip() != 5) { // put this stuff somewhere else?
			//System.out.println("lol?");
			mod4AttackHits[0] = 0;
			mod4AttackHits[1] = 0;
			mod4AttackHits[2] = 0;
			mod4AttackHits[3] = 0;
		}
		if(myOpponent.findLargestShip() < 3 && !(myOpponent.findLargestShip() > 4)) { // this prob isnt right jsyk ^v
			mod3AttackHits[0] = 0;
			mod3AttackHits[1] = 0;
			mod3AttackHits[2] = 0;
		}
		
				switch (currentAttackMethod) {
				case 0:
					
					if (mod4AttackHits[random4] > 0) {
						return modAttack(4, random4);
					}
					else if (mod3AttackHits[random3] > 0) {
						return intelligentModAttack(3, random3);
					}
					return modAttack(2, random2);
				case 1:
					if (mod4AttackHits[random4] > 0) {
						return intelligentModAttack(4, random4);
					}
					else if (mod3AttackHits[random3] > 0) {
						return intelligentModAttack(3, random3);
					}
					return modAttack(2, random2);
				case 2: 
					if (mod3AttackHits[random3] > 0) {
							return modAttack(3, random3);
						}
						return modAttack(2, random2);
				case 3:
					if (mod3AttackHits[random3] > 0) {
						return intelligentModAttack(3, random3);
					}
					return modAttack(2, random2);

				case 4:
					if (mod3AttackHits[random3] > 0) {
						return intelligentModAttack(3, random3);
					}
					return intelligentModAttack(2, random2);
				}
				System.out.println("never gets here does it? if so lol! lasngiw: " + currentAttackMethod);

				return makeAttackController();
	}
	
	public Coordinate randomAttack() {
		do {
			xCoord = generator.nextInt(10);
			yCoord = generator.nextInt(10);  
		} while (!myOpponent.canAnyShipBeHere(xCoord, yCoord));
		return new Coordinate(xCoord, yCoord);
    }
	
	public Coordinate modAttack(int modID, int squareID) {  // should maybe change the parameter to squareID
		do {
			xCoord = generator.nextInt(10);
			yCoord = generator.nextInt(10);
		} while (board[xCoord][yCoord] != 0 || ((xCoord + yCoord) % modID) != squareID || !myOpponent.canAnyShipBeHere(xCoord, yCoord));
		return new Coordinate(xCoord, yCoord);
	}
	
	public Coordinate intelligentAttack() {
		do {
			attackCoordinate = myOpponent.getAttackCoordinate(index++);
		} while (board[attackCoordinate.getX()][attackCoordinate.getY()] == 0);
		return attackCoordinate;
	}
	
	public Coordinate intelligentModAttack(int modID, int squareID) {
		do {
			attackCoordinate = myOpponent.getAttackCoordinate(index++);
		} while (board[attackCoordinate.getX()][attackCoordinate.getY()] != 0 || ((attackCoordinate.getX() + attackCoordinate.getY()) % modID) != squareID || !myOpponent.canAnyShipBeHere(attackCoordinate.getX(), attackCoordinate.getY()));
		return attackCoordinate;
	}

	public Coordinate probabilityAttack() {
		// add stuff noob
		return new Coordinate(xCoord, yCoord);
	}
                                                  
	public Coordinate iWillFindYou(int shipID) {
		if (!shipHOrVGuessed[shipID]) {
			myOpponent.theirShipInfo[shipID].calculateIfShipCanBeHorizontalOrVertical();
			if (myOpponent.theirShipInfo[shipID].horizontalVsVertical() == VERTICAL) {
				cycle = cycle + 2;
			}
			shipHOrVGuessed[shipID] = true;
		}
		
		return andIWillKillYou(shipID);
		
	}
	
	public Coordinate andIWillKillYou(int shipID) {
	//	Coordinate tempCoordinate2 = new Coordinate(-1, -1);
		//System.out.println("hAvailSpace: " + myOpponent.theirShipInfo[focusedShip].hAvailSpaces + " vAvailSpace: " + myOpponent.theirShipInfo[focusedShip].vAvailSpaces + " Cycle: " + cycle);
	//	System.out.println("im confused." + cycle + "SHIP: " + shipID);//also add this somewhere if it misses on its first shot horizontal / vertical subtract one from the spaces int and see if its still a viable spot for the ship to be... FIX THIS SHIT(all of it...its not working wtf)	
		switch (cycle) {
		case 0:
			if (myOpponent.theirShipInfo[shipID].canBeHorizontal && myOpponent.theirShipInfo[shipID].isHorizontal()) {
				tempCoordinate2 = new Coordinate(coordHit[shipID].getX() + hitCycle, coordHit[shipID].getY());
				cycle = 0;
				break;
			}
		case 1:
			if (myOpponent.theirShipInfo[shipID].canBeHorizontal && myOpponent.theirShipInfo[shipID].isHorizontal()) {
				tempCoordinate2 = new Coordinate(coordHit[shipID].getX() + (NEGATIVE*hitCycle), coordHit[shipID].getY());
				cycle = 1;
				break;
			}
		case 2:
			if (myOpponent.theirShipInfo[shipID].canBeVertical && myOpponent.theirShipInfo[shipID].isVertical()) {
				tempCoordinate2 = new Coordinate(coordHit[shipID].getX(), coordHit[shipID].getY() + hitCycle);
				cycle = 2;
				break;
			}
		case 3:
			if (myOpponent.theirShipInfo[shipID].canBeVertical && myOpponent.theirShipInfo[shipID].isVertical()) {
				tempCoordinate2 = new Coordinate(coordHit[shipID].getX(), coordHit[shipID].getY() + (NEGATIVE*hitCycle));
				cycle = 3;
			}
			else {
				cycle = 0;
				return andIWillKillYou(shipID);
			}
			break;
		case 4://delete this when ready
			System.out.println("This should not happen. X: " + tempCoordinate.getX() + " Y: " + tempCoordinate.getY() + " board: " + board[tempCoordinate.getX()][tempCoordinate.getY()]);
			
			//printBoard2();
			cycle = -1;
			hitCycle = 0;
			
				while(totalGames != 10000000) {
					
				}
			
			break;
		}
		if (tempCoordinate2.getX() > 9 || tempCoordinate2.getY() > 9 || tempCoordinate2.getX() < 0 || tempCoordinate2.getY() < 0 || !myOpponent.canThisShipActuallyBeHere(focusedShip)) {
			//	if (board[tempCoordinate.getX()][tempCoordinate.getY()] != SHIP_HIT_IDS[focusedShip]) {//check this might be wrong
				if (cycle == 3) {
					cycle = 0;
				}
				else {
					cycle++;
				}
				hitCycle = 1;
				//makeAttack();
			//	}
				return andIWillKillYou(shipID);
			}
		hitCycle++;
		return tempCoordinate2;
	}
	
	public void didHeDie() {
		if (isSeeking) {
			if (hitMod != focusedShip) {
				if (cycle < 2) {
					myOpponent.theirShipInfo[focusedShip].hAvailSpaces--;
				} else {
					myOpponent.theirShipInfo[focusedShip].vAvailSpaces--;
				}
				if (cycle == 3) {
					cycle = 0;
				} else {
					cycle++;
				}
				hitCycle = 1;
			}
		}
	}

	@Override
	public void resultOfAttack(int result) {// nned to add something where it checks to see if the ship can still be there when im using willkillyou() (check the spaces)
		totalAttacks++;
		
		hitMod = result % HIT_MODIFIER;
		sunkMod = result % SUNK_MODIFIER;
		checkLastAttack();
		didHeDie();
		
		if (checkLastTurnHit < 5 && !wonLastGame && !isSeeking) { // i can put the ++ in here if i want. also put a boolean statement in here so i can see if they are actually doing this. LUL
			if (hitMod != (checkLastTurnHit)) {
			//	System.out.println("not yay " + lastAttack +" ShipID: "+ (checkLastHitTurn));
				checkLastTurnHit += 100000;
			}
			else {
			//	System.out.println("yay! " + lastAttack +" ShipID: "+ (checkLastHitTurn));
			}
			checkLastTurnHit++;
		}
		
		if (hitMod < 5) {
			board[lastAttack.getX()][lastAttack.getY()] = SHIP_HIT_IDS[hitMod];
			myOpponent.store(lastAttack.getX(), lastAttack.getY(), NEGATIVE);

			shipHitLastTurn[hitMod] = true;
			
			if (seeker[hitMod] != true) {
				seeker[hitMod] = true;
				coordHit[hitMod] = lastAttack;
			}
			else if (myOpponent.theirShipInfo[hitMod].vertOrHori == -1) {
				myOpponent.theirShipInfo[hitMod].vertOrHori = myOpponent.theirShipInfo[hitMod].calculateHorizontalOrVerticalShip();
			}
			if (focusedShip == -1) {
				focusedShip = hitMod;
			}
		}
		if (sunkMod < 5) {
			refocus(sunkMod);
		}
		// make this better? why is it else if??
		else if (result == MISS) { // play around with this, for some reason if i dont do the (result...) it works better against mal sometimes
			misses++;
			myOpponent.recordMiss(lastAttack.getX(), lastAttack.getY(), 1);	
		}
		index = 0;
	}

	public void refocus(int shipID) {
		myOpponent.theirShipInfo[shipID].sunk = true;
		shipSunk[shipID] = true;
		seeker[shipID] = false;
		focusedShip = -1;
		cycle = 0;
		hitCycle = 1;

		for (int i = 0; i < 5; i++) {
			if (seeker[i] == true) {
				focusedShip = i;
				break;
			}
		}
		if (focusedShip == -1) {
			isSeeking = false;
		}
	}

	@Override
	public void opponentAttack(Coordinate coord) {
		totalOpponentAttacks++; // total opponenetattacks is -- in record opponentAttack dafuq?????? lol one is for the opponent class and one is global... maybe change this shit so its not so confusing?
		myOpponent.recordOpponentAttack(coord.getX(), coord.getY());
	}

	@Override
	public void resultOfGame(int result) { //remember: when giving statistics to the attack/placement methods make sure that i won the game to record the attackMethod statistics / also make sure to only record the placement statistics when i lose a game (i might only need to do 10 games to do so) although if i find another way MAYBE i can record it all the time... maybe look at khan for reference
	//	myOpponent.displayShipStats();
		totalGames++;
		trueTotalGames++;
		diagnosticGames++;
		gamesSinceLastPlacementSwitch++;
		gamesSinceLastAttackSwitch++;

		allAttackMethods[currentAttackMethod].totalGames++;
		allPlacementMethods[currentPlacementMethod].totalGames++;
		allAttackMethods[currentAttackMethod].trueTotalGames++;
		allPlacementMethods[currentPlacementMethod].trueTotalGames++;
		
		//allAttackMethods[currentAttackMethod].updateAccuracy(totalAttacks);
		
		
		allAttackMethods[currentAttackMethod].recordWin(totalAttacks);// better when it always records??? check this out.... !!
		//allPlacementMethods[currentPlacementMethod].recordLoss(totalOpponentAttacks);  doesnt work that well to always record this.
		

		if (result == WON) {

			//allAttackMethods[currentAttackMethod].recordWin(totalAttacks);
			myOpponent.recordHit(); // for some reason the recordhitormiss() is better when it always records... loook into this... ugh
			wins++;
			wonLastGame = true;
		//	myOpponent.checkShipPlacements();
		} else {
			allPlacementMethods[currentPlacementMethod].recordLoss(totalOpponentAttacks);
			myOpponent.recordHit();
			losses++;
			wonLastGame = false;
		}
		

		//remember to do this: when checking to see if i hit something make sure its NOT SEEKING while figuring out the average hit (because if it is seeking then it will mess with the average hit... although might not need to worry about this because if it checks the amount of hits/misses after the entire game then it wont matter) blah blah blah

		
		if(placementSampling[currentPlacementMethod]) {//maybe this will do better after the attack sampling? try it different ways
			if (allPlacementMethods[currentPlacementMethod].losses >= AMOUNT_OF_WINLOSS_SAMPLE_GAMES || allPlacementMethods[currentPlacementMethod].totalGames >= AMOUNT_OF_TOTAL_SAMPLE_GAMES) {
				placementSampling[currentPlacementMethod] = false;
				log("Placement Method " + allPlacementMethods[currentPlacementMethod].name + " has accuracy %6.3f attacks.\n", allPlacementMethods[currentPlacementMethod].accuracy);
	            if (currentPlacementMethod < AMOUNT_OF_PLACEMENT_METHODS-1) {
	            	currentPlacementMethod++;
	            }
	            else {
	            	currentPlacementMethod = findBestPlacementMethod();
	            	log("Chose Placement Method " + allPlacementMethods[currentPlacementMethod].name);
	            }
			}
		}
			else {
	         if (gamesSinceLastPlacementSwitch >= AMOUNT_OF_GAMES_NEEDED_FOR_SWITCH) {
	        	 int bestPlacementMethod = findBestPlacementMethod();
	             if (bestPlacementMethod != currentPlacementMethod) { 
	            	 log("Reload the boats! Switching to %s with accuracy %.4f, appears better than %s with %.4f after %d games\n", allPlacementMethods[bestPlacementMethod].name, allPlacementMethods[bestPlacementMethod].accuracy, allPlacementMethods[currentPlacementMethod].name, allPlacementMethods[currentPlacementMethod].accuracy, gamesSinceLastPlacementSwitch);
	            	 currentPlacementMethod = bestPlacementMethod;
	            	 gamesSinceLastPlacementSwitch = 0;
	             }
	         }
	     }
		
		if(attackSampling[currentAttackMethod]) {
			if (allAttackMethods[currentAttackMethod].wins >= AMOUNT_OF_WINLOSS_SAMPLE_GAMES || allAttackMethods[currentAttackMethod].totalGames >= AMOUNT_OF_TOTAL_SAMPLE_GAMES) {
				attackSampling[currentAttackMethod] = false;
				log("Attack Method " + allAttackMethods[currentAttackMethod].name + " has accuracy %6.3f attacks.\n", allAttackMethods[currentAttackMethod].accuracy);
	            if (currentAttackMethod < AMOUNT_OF_ATTACK_METHODS-1) {
	            	currentAttackMethod++;
	            }
	            else {
	            	currentAttackMethod = findBestAttackMethod();
	            	log("Chose Attack Method " + allAttackMethods[currentAttackMethod].name);
	            }
			}
		}
			else {
	         if (gamesSinceLastAttackSwitch >= AMOUNT_OF_GAMES_NEEDED_FOR_SWITCH) {
	        	 int bestAttackMethod = findBestAttackMethod();
	             if (bestAttackMethod != currentAttackMethod) { 
	            	 log("Reload the Cannons! Switching to %s with accuracy %.4f, appears better than %s with %.4f after %d games\n", allAttackMethods[bestAttackMethod].name, allAttackMethods[bestAttackMethod].accuracy, allAttackMethods[currentAttackMethod].name, allAttackMethods[currentAttackMethod].accuracy, gamesSinceLastAttackSwitch);
	            	 currentAttackMethod = bestAttackMethod;
	            	 gamesSinceLastAttackSwitch = 0;
	             }
	         }
	     }

        if (LOGGING) {
        	if (trueTotalGames == 1) {
                log(" The beginning of the end.");
                beginTime = System.currentTimeMillis();
                latestTime = beginTime;
            }
            if (trueTotalGames == totalNumberOfMatches) {
                log("Final Statistics: wins: %5.2f%%, sec=%.2f\n", 100.0 * wins / totalNumberOfMatches, (System.currentTimeMillis() - beginTime) / 1000.0);
                for (int i = 0; i < AMOUNT_OF_PLACEMENT_METHODS; i++) {
                    log("Placement %30s: used %6.2f%%, accuracy %.4f\n",
                            allPlacementMethods[i].name, 100.0 * allPlacementMethods[i].trueTotalGames / totalNumberOfMatches, allPlacementMethods[i].accuracy);
                }
                System.out.println();
                for (int i = 0; i < AMOUNT_OF_ATTACK_METHODS; i++) {
                    log("Attack %30s: used %6.2f%%, accuracy: %6.2f\n",
                            allAttackMethods[i].name, 100.0 * allAttackMethods[i].trueTotalGames / totalNumberOfMatches, allAttackMethods[i].accuracy);
                }
            } 
            else if (trueTotalGames % 25000 == 0) {
            	long now = System.currentTimeMillis();
            	log("%d UPDATE Overall: %.2f%%, sec=%.2f, Placement -%s-: placementav: %.2f, Attack -%s-: attackav: %.2f, attackpav: %.5f%%\n", trueTotalGames, wins *100.0 / trueTotalGames, (now - latestTime) / 1000.0, allPlacementMethods[currentPlacementMethod].name, allPlacementMethods[currentPlacementMethod].accuracy, allAttackMethods[currentAttackMethod].name, allAttackMethods[currentAttackMethod].accuracy, allAttackMethods[currentAttackMethod].accuracyPercent);
                latestTime = now;  
            }
        }
        
        
        if(totalGames == GAME_THRESHOLD_TILL_RESET) {
			resetPlacementAndAttackMethods();
			currentAttackMethod = 0;
			currentPlacementMethod = 0;
			totalGames = 0; // i need two totalGames... one for checking for things like this and another to know the TRUE amount of games (one that doesnt change until new opponent.)
			//myOpponent.resetBoards(); 
		}
        
        if (diagnosticGames == GAME_THRESHOLD_TILL_CHECK) { // come back to this i guess...
			diagnosticGames = 0;
			checkGame();
			
		}
        
        
	}
	//thank you Khan
	// the Object.... arguments thing is for the printf and the regular one is for the normal system.out!!!!
	public void log(String message, Object... arguments) {
        if (LOGGING) {
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SS");
            String timeStamp = formatter.format(new Date());
            if (arguments.length == 0) {
                System.out.println(myOpponent.name + "|" + timeStamp + "|" + message);
            } else {
                System.out.printf(myOpponent.name + "|" + timeStamp + "|" + message, arguments); 
            }
        }
    }
	
	public void resetPlacementAndAttackMethods() {
		for(int i = 0; i < AMOUNT_OF_PLACEMENT_METHODS; i++) {
			allPlacementMethods[i].reset();
			placementSampling[i] = true;
		}
		
		for(int i = 0; i < AMOUNT_OF_ATTACK_METHODS; i++) {
			allAttackMethods[i].reset();
			attackSampling[i] = true;
		}
	}
	
	public int findBestPlacementMethod() {
		double best = 0.0;
		int bestMethod = -1;
		for (int i = 0; i < AMOUNT_OF_PLACEMENT_METHODS; i++) {
			if (allPlacementMethods[i].accuracy > best) {
				best = allPlacementMethods[i].accuracy;
				bestMethod = i;
			}
		}
		return bestMethod;
	}
	
	public int findBestAttackMethod() {
		double best = 100.0;
		int bestMethod = -1;
		for (int i = 0; i < AMOUNT_OF_ATTACK_METHODS; i++) {
			if (allAttackMethods[i].accuracy < best) {
				best = allAttackMethods[i].accuracy;
				bestMethod = i;
			}
		}
		return bestMethod;
	}
	
	
	
	
	//for check Game also have some sort of sampling ... because even if i am winning over half the games MAYBE i can do better? (like figure out which one is the best)
	public void checkGame() { // check to make sure they arent storing where the ships were last time and using that against me. (ooo something i can do. :P) .. try.		
		if (((GAME_THRESHOLD_TILL_CHECK)-(wins-winCheck)) > ((GAME_THRESHOLD_TILL_CHECK)/2)) { // if true then i have lost more than half of my games at the check
			//shipPlacementCycle++;
			/*
			attackCycle++;
		}
		else {
			lastBestCycle = attackCycle; // lol to  keep track of the last best... actually keep use of this somewher idc where omg noob
		}
		if (attackCycle == 3) {
			//shipPlacementCycle = 0;
			attackCycle = 0;
		}*/
		}
		checkCycle++;	
		winCheck = wins;	
	}
	
	public void checkLastAttack() {
		//for(int i = 0; i < 3; i++) {
			if ((lastAttack.getX() + lastAttack.getY()) % 3 == random3) {
				mod3AttackHits[random3]--;
				//break;
			//}
		}
		//for(int i = 0; i < 4; i++) {
			if ((lastAttack.getX() + lastAttack.getY()) % 4 == random4) {
				mod4AttackHits[random4]--;
			//	break;
		//	}
		}
	}
	



public class PlacementMethod {
	int wins, losses, totalGames, trueTotalGames;
	String name;
	double accuracy;
	int totalAttacks;
	public PlacementMethod(String name) {
		this.name = name;
		trueTotalGames = wins = losses = totalGames = 0;
		accuracy = 100;
	}
	
	public void recordHitOrMiss() {
		
	}
	
	public void recordLoss(int totalAttacks) {
		this.totalAttacks += totalAttacks;
		allAttackMethods[currentAttackMethod].losses++;
		
		
		//totalGames++;
		losses++;
		//accuracy += totalAttacks;
		accuracy = (double)this.totalAttacks / (double)losses;

	}
	
	//do i even need this?
	public void reset() {
		totalAttacks = totalGames = wins = losses = 0;
		accuracy = 100; //this can maybe be changed
	}
	
}


	public class AttackMethod {
		int wins, losses, totalGames, trueTotalGames;
		int totalAttacks;
		String name;
		int totalMisses;
		
		double accuracy, accuracyPercent;
		
		public AttackMethod(String name) {
			totalMisses = 0;
			this.name = name;
			totalAttacks = 0;
			trueTotalGames = wins = losses = totalGames = 0;//change accuracy to a double or something
			accuracy = accuracyPercent = 0;
		}
		
		public void recordHitOrMiss() {
			
		}
		
		public void recordWin(int totalAttacks) {//test to see if the accuracy percentage is better at winning or  the amount of attacks... do this after everything is done also while im at it test to see if the percent/notpercent work better if they always record/if they only record in here.
			this.totalAttacks += totalAttacks;
			
			allPlacementMethods[currentPlacementMethod].wins++;
			//totalGames++;
			wins++;
			accuracy = (double)this.totalAttacks / (double)wins;
			
			
			//accuracy = ((accuracy + (totalAttacks/100)));
		//	System.out.println("AM: " + accuracy);
			//accuracy += totalAttacks;
		//	accuracy = something!;
		}
		
		public void updateAccuracy(int totalAttacks) {//test this i guess as well. (just dont forget to take out the this.totalAttacks += totalattacks above or it messes up
		//	totalMisses += misses; this isnt catching all of the misses for some reason....
			//this.totalAttacks += totalAttacks;
			accuracyPercent = (((double)this.totalAttacks - (double)totalMisses)/(double)this.totalAttacks)*100.0; //this isnt working because its only recording my wins if i also record it when i lose it will then be correct.
		}
		
		//do i even need this?
		public void reset() {
			totalAttacks = totalGames = wins = losses = 0;
			accuracy = 0; //this can maybe be changed
		}
		
	}
	
	



	public class ShipInfo {
	    int shipID, length, vertOrHori, amountOfVPlacements, amountOfHPlacements, hAvailSpaces, vAvailSpaces;
	    boolean canBeHorizontal, canBeVertical, sunk;
	    Coordinate hitLastTurn;
	    
	    
	    public ShipInfo(int shipID, int length, int vertOrHori) {
	    	this.shipID = shipID;
	        this.length = length;
	        this.vertOrHori = vertOrHori;
	        canBeHorizontal = false;
	        canBeVertical = false;
	        amountOfHPlacements = amountOfVPlacements = 0;
	        hAvailSpaces = vAvailSpaces = 0;
	        sunk = false;
	        hitLastTurn = new Coordinate(-1, -1);
	    }
	    //this method and others are messing up with each other i believe. check them all.
	    public void calculateIfShipCanBeHorizontalOrVertical() {
        	boolean horizontalP1, horizontalP2, verticalP1, verticalP2;

        	horizontalP1 = true;
        	horizontalP2 = true;
        	verticalP1 = true;
        	verticalP2 = true;
        	
        	for(int i = 1; i < SHIP_LENGTHS[shipID]; i++) {
	    		if ((i+coordHit[shipID].getX()) > -1 && (i+coordHit[shipID].getX() < 10)) {
	    			if (board[coordHit[shipID].getX() + i][coordHit[shipID].getY()] == SHIP_HIT_IDS[shipID] || board[coordHit[shipID].getX() + i][coordHit[shipID].getY()] == 0) {
	    				if (horizontalP1) {
	    					hAvailSpaces++;
	    				}
	    			}
	    			else {
	    				horizontalP1 = false;
	    			}
	    		}
	    		if (((i*NEGATIVE)+coordHit[shipID].getX()) > -1 && ((i*NEGATIVE)+coordHit[shipID].getX() < 10)) {
	    			if (board[coordHit[shipID].getX() + (i*NEGATIVE)][coordHit[shipID].getY()] == SHIP_HIT_IDS[shipID] || board[coordHit[shipID].getX() + (i*NEGATIVE)][coordHit[shipID].getY()] == 0) {
	    				if (horizontalP2) {
	    					hAvailSpaces++;
	    				}
	    			}
	    			else {
	    				horizontalP2 = false;
	    			}
	    		}
	    		
	    		
	    		if ((i+coordHit[shipID].getY()) > -1 && (i+coordHit[shipID].getY() < 10)) {
	    			if (board[coordHit[shipID].getX()][coordHit[shipID].getY() + i] == SHIP_HIT_IDS[shipID] || board[coordHit[shipID].getX()][coordHit[shipID].getY() + i] == 0) {
	    				if (verticalP1) {
	    					vAvailSpaces++;
	    				}
	    			}
	    			else {
	    				verticalP1 = false;
	    			}
	    		}
	    		if (((i*NEGATIVE)+coordHit[shipID].getY()) > -1 && ((i*NEGATIVE)+coordHit[shipID].getY() < 10)) {
	    			if (board[coordHit[shipID].getX()][coordHit[shipID].getY() + (i*NEGATIVE)] == SHIP_HIT_IDS[shipID] || board[coordHit[shipID].getX()][coordHit[shipID].getY() + (i*NEGATIVE)] == 0) {
	    				if (verticalP2) {
	    					vAvailSpaces++;
	    				}
	    			}
	    			else {
	    				verticalP2 = false;
	    			}
	    		}
	    		
	    		
	    	}

        	if (hAvailSpaces >= SHIP_LENGTHS[shipID] - 1) {
        		canBeHorizontal = true;
        	}
        	
        	if (vAvailSpaces >= SHIP_LENGTHS[shipID] - 1) {
        		canBeVertical = true;
        	}
        	
        	
        	if (canBeHorizontal == true && canBeVertical == false && vertOrHori == -1) {
        		amountOfHPlacements++;
        		vertOrHori = HORIZONTAL;
        	}
        	
        	else if (canBeHorizontal == false && canBeVertical == true && vertOrHori == -1) {
        		amountOfVPlacements++;
        		vertOrHori = VERTICAL;
        	}
        	
        }
	    
	    
	    public int calculateHorizontalOrVerticalShip() {
	    	for(int i = 1 - length; i < length; i++) {
	    		if ((i+coordHit[shipID].getX()) > -1 && (i+coordHit[shipID].getX() < 10) && i != 0) {
	    			if (board[coordHit[shipID].getX() + i][coordHit[shipID].getY()] == SHIP_HIT_IDS[shipID]) {
	    				amountOfHPlacements++;
	    				canBeVertical = false;
	    				return HORIZONTAL;
	    			}
	    		}
	    	
	    		if ((i+coordHit[shipID].getY()) > -1 && (i+coordHit[shipID].getY() < 10) && i != 0) {
	    			if (board[coordHit[shipID].getX()][coordHit[shipID].getY() + i] == SHIP_HIT_IDS[shipID]) {
	    				amountOfVPlacements++;
	    				canBeHorizontal = false;
	    				return VERTICAL;
	    			}
	    		}
	    	}
	    	System.out.println("THISSHOULDNTHAPPEN!!!");
			return -1;
	    }
	    
	    public int horizontalVsVertical() { // maybe just look at the past like... 500 games or so for this? maybe itll make it better? check it out. also check to see if i can predict if i should place my ships horizontal / vertical depending on which way the enemy attacks.
	    	if (amountOfHPlacements > amountOfVPlacements) {
	    		return HORIZONTAL;
	    	}
			return VERTICAL;
	    	
	    }
	    
	    public boolean isHorizontal() {
	    	if (vertOrHori == -1 || vertOrHori == HORIZONTAL &&  hAvailSpaces >= SHIP_LENGTHS[shipID] - 1) {
        		return true;
        	}		
	    	canBeHorizontal = false;
	    	return false;
	    }
	    
	    public boolean isVertical() {
	    	if (vertOrHori == -1 || vertOrHori == VERTICAL && vAvailSpaces >= SHIP_LENGTHS[shipID] - 1) {	
	    		return true;
	    	}
	    	canBeVertical = false;
	    	return false;
	    }
	}
	public class BoardCoordinate {
		boolean attacked;
		int x, y, score;
		
		public BoardCoordinate(int x, int y) {
			attacked = false;
			this.x = x;
			this.y = y;
			score = 50000000;
		}
		
		

		/*
		public void attacked(int x, int y) {
			if (this.x == x && this.y == y) {
				score = (score) - (5000); // having the totalOpponentAttacks on helps for some ais but not others.!!!! (totalopponentattacks * 5000) or this

			}
		}*/

		public void attacked(int x, int y, int ID) {
			if (this.x == x && this.y == y) {
				score = (score) + (ID*5000); // can make multiple methods of attack if need be... also :(totalopponentattacks * 5000) or this (need another method for this, but it helps against running gazzell
				attacked = true;
			}
		}

		public void reset() {
			score = 50000000;	
		}
		
		@Override
		public String toString() {
	        return String.format("(%2d, %2d)", x, y);
	    }
	}
	
    public class Opponent {
    	BoardCoordinate[] theirAttackBoard, theirPlacementBoard;
    	ShipInfo[] theirShipInfo;
    	String name;
    	
     	int totalOpponentAttacks, storeID;
    	int[] storeX, storeY, idStore;
    	int[][] oppAttackBoard;
    	
    	
    	
        public Opponent(String name) {
        	theirShipInfo = new ShipInfo[5];
                 
        	idStore = new int[100];
        	storeX = new int[100];
        	storeY = new int[100];

        	oppAttackBoard = new int[10][10];	
        	
        	theirAttackBoard = new BoardCoordinate[100];
    		theirPlacementBoard = new BoardCoordinate[100];
        	this.name = name;

        	initializeOpponent();
        }
        
        public void initializeOpponent() {
        	for (int i = 0; i < 100; i++) {
				theirAttackBoard[i] = new BoardCoordinate(i % 10, i / 10);
				theirPlacementBoard[i] = new BoardCoordinate(i % 10, i / 10);
        	}
        	 for (int i = 0; i < 5; i++) {
             	theirShipInfo[i] = new ShipInfo(i, SHIP_LENGTHS[i], -1);
             }
        }
        
        public void resetBoards() {
        	for (int i = 0; i < 100; i++) {
				theirAttackBoard[i].reset();
				theirPlacementBoard[i].reset();
			}
		}
        
        public void sortBoards() {
        	boolean[] placementCoordinatePut = new boolean[100];
        	boolean[] attackCoordinatePut = new boolean[100];
        	
        	for (int i = 0; i < 100; i++) {
    			intelligentScore[i] = theirPlacementBoard[i].score;
    			adaptiveScore[i] = theirAttackBoard[i].score;
    		}
        	
    		Arrays.sort(intelligentScore);
    		Arrays.sort(adaptiveScore);
    		
    		for (int i = 0; i < 100; i++) {
				for (int j = 0; j < 100; j++) {
					if (theirPlacementBoard[j].score == intelligentScore[i] && !placementCoordinatePut[j]) {
						theirPlacementHash.put(i, theirPlacementBoard[j]);
						placementCoordinatePut[j] = true;
						break;
					}	
				}
			}
    		
    		for (int i = 0; i < 100; i++) {
				for (int j = 0; j < 100; j++) {
					if (theirAttackBoard[j].score == adaptiveScore[i] && !attackCoordinatePut[j]) {
						theirAttackHash.put(i, theirAttackBoard[j]);
						attackCoordinatePut[j] = true;
						break;
					}
				}
			}
        }
        
        public void recordOpponentAttack(int x, int y) {
    		totalOpponentAttacks--;
    		oppAttackBoard[x][y] = 1;
    		for (int i = 0; i < 100; i++) {
    			theirAttackBoard[i].attacked(x, y, -1);
    		}
        }

        public void recordHit() {
        	for (int i = 0; i < 100; i++) {
        		for (int j = 0; j < storeID; j++) {
        			theirPlacementBoard[i].attacked(storeX[j], storeY[j], idStore[j]);
        		}
			}
        }
        
        
        public void recordMiss(int x, int y, int ID) {
        	for (int i = 0; i < 100; i++) {
        		theirPlacementBoard[i].attacked(x, y, ID);
			}
        }
        
        public void store(int x, int y, int ID) {
        	idStore[storeID] = ID; // if ID is -1 that means we hit a ship, can use this?
        	storeX[storeID] = x;
        	storeY[storeID] = y;
        	storeID++;
        }
        
        
        public void resetStore() {
        	storeID = 0;
        } 
        
        
        //need to make sure this is actually working correctly
        public boolean canAnyShipBeHere(int x, int y) {
        	
        	boolean horizontalP1, horizontalP2, verticalP1, verticalP2;
        	int smallestShipLength = findSmallestShip();
        	int hSpaceCount, vSpaceCount;
        	hSpaceCount = 0;
        	vSpaceCount = 0;
        	horizontalP1 = true;
        	horizontalP2 = true;
        	verticalP1 = true;
        	verticalP2 = true;
        	
        	for(int i = 1; i < smallestShipLength; i++) {
	    		if ((i+x) > -1 && (i+x < 10)) {
	    			if (board[x + i][y] == 0) {
	    				if (horizontalP1) {
	    					hSpaceCount++;
	    				}
	    			}
	    			else {
	    				horizontalP1 = false;
	    			}
	    		}
	    		if (((i*NEGATIVE)+x) > -1 && ((i*NEGATIVE)+x < 10)) {
	    			if (board[x + (i*NEGATIVE)][y] == 0) {
	    				if (horizontalP2) {
	    					hSpaceCount++;
	    				}
	    			}
	    			else {
	    				horizontalP2 = false;
	    			}
	    		}
	    		
	    		
	    		if ((i+y) > -1 && (i+y < 10)) {
	    			if (board[x][y + i] == 0) {
	    				if (verticalP1) {
	    					vSpaceCount++;
	    				}
	    			}
	    			else {
	    				verticalP1 = false;
	    			}
	    		}
	    		if (((i*NEGATIVE)+y) > -1 && ((i*NEGATIVE)+y < 10)) {
	    			if (board[x][y + (i*NEGATIVE)] == 0) {
	    				if (verticalP2) {
	    					vSpaceCount++;
	    				}
	    			}
	    			else {
	    				verticalP2 = false;
	    			}
	    		}
	    		
	    		
	    	}
        //	System.out.println("1:   SmallestShIpL: " + smallestShipLength + " hSpaceCount: " + hSpaceCount + " vSpaceCOunt: " + vSpaceCount);
        	if (hSpaceCount >= smallestShipLength - 1 || vSpaceCount >= smallestShipLength - 1) {
        		//System.out.println("2:    SmallestShIpL: " + smallestShipLength + " hSpaceCount: " + hSpaceCount + " vSpaceCOunt: " + vSpaceCount);
        		return true;
        	}
        	return false;
        }
        
        public int findSmallestShip() {
        	for(int i = 0; i < 5; i++) {
        		if (theirShipInfo[i].sunk == false) { 
        			return SHIP_LENGTHS[i];
        		}
        	}
        	System.out.println("thisshould not happen");
        	return 2;
        }
        public int findLargestShip() {
        	for(int i = 4; i >= 0; i--) {
        		if (theirShipInfo[i].sunk == false) { 
        			return SHIP_LENGTHS[i];
        		}
        	}
        	System.out.println("thisshould not happen");
        	return 2;
        }
        
        public void resetShips() {
        	for (int i = 0; i < 5; i++) {
            	theirShipInfo[i].vertOrHori = -1;
            	theirShipInfo[i].canBeHorizontal = false;
            	theirShipInfo[i].canBeVertical = false;
            	theirShipInfo[i].hAvailSpaces = 0;
            	theirShipInfo[i].vAvailSpaces = 0;
            	theirShipInfo[i].sunk = false;
            	
            	if (shipHitLastTurn[i]) {
            		theirShipInfo[i].hitLastTurn = coordHit[i];
            	}
            	else {
            		theirShipInfo[i].hitLastTurn = new Coordinate(-1, -1);
            	}
            	shipHitLastTurn[i] = false;
            	coordHit[i] = new Coordinate(-1, -1);
            }
        }
        
        public void displayShipStats() {
        	for (int i = 0; i < 5; i++) {
            	System.out.println("Ship ID: " + i + " Number of times H: " + theirShipInfo[i].amountOfHPlacements + " Number of times V: " + theirShipInfo[i].amountOfVPlacements);
            }
	    }
        
        public boolean canThisShipActuallyBeHere(int shipID) {
			if (board[tempCoordinate2.getX()][tempCoordinate2.getY()] == SHIP_HIT_IDS[shipID] || board[tempCoordinate2.getX()][tempCoordinate2.getY()] == 0) {
				canThisShipBeThere = true;
				return true;
			}
			canThisShipBeThere = false;
			return false;	
        }
        
        public Coordinate giveLastTurnOppShipCoords() {
        	for(int i = checkLastTurnHit; i < 5; i++) {
        		//System.out.println(theirShipInfo[i].hitLastTurn + " i:  " + i);
        		if (theirShipInfo[i].hitLastTurn.getX() != -1) {
        			checkLastTurnHit = i;
        			return theirShipInfo[i].hitLastTurn;
        		}
        		
        	}
        	//System.out.println("thisshouldnthappen.jpg");
        	return new Coordinate(-1, -1);
        }
        
        public Coordinate getAttackCoordinate(int ID) {
          	return new Coordinate(theirPlacementHash.get(ID).x, theirPlacementHash.get(ID).y);
        }
        
        public Coordinate getBestSquares(int shipID) { // there are two ways i can go about this... either go through the whole board and see which spaces are the best (long way) or pick a few squares from the "best" (the top 5 say) and place the ship there
      		for (int i = 99; i > -1; i--) {
        		for (int j = 0; j < SHIP_LENGTHS[shipID]; j++) {

        		}
        	}
        	//System.out.println("this should not happen"); // GET BACK TO THIS!!!!
			return attackCoordinate;
        }
        
        //this method is to check if opponent is placing ships next to each other (i believe, i forget )
     
        public void checkShipPlacements() {
        	for (int i = 0; i < 5; i++) {
        		for (int j = 0; j < 10; j++) {
        			for (int l = 0; l < 10; l++) {
        				if (board[j][l] == SHIP_HIT_IDS[i]) {
        					if (j+1 < 10) {
        						if (board[j+1][l] != SHIP_HIT_IDS[i] && board[j+1][l] != 0 && board[j+1][l] != 1) {
        						//	System.out.println("LOL0: "+" shipHitID: "+SHIP_HIT_IDS[i]+" i: "+i+ " Board: " + board[j+1][l]);
        						}
        					}
        					if (l+1 < 10) {
        						if (board[j][l+1] != SHIP_HIT_IDS[i] && board[j][l+1] != 0 && board[j][l+1] != 1) {
        						//	System.out.println("LOL1: " +" shipHitID: "+SHIP_HIT_IDS[i]+" i: "+i+  " Board: " + board[j][l+1]);
        						}
        					}
        					if (j-1 > -1){
        						if (board[j-1][l] != SHIP_HIT_IDS[i] && board[j-1][l] != 0 && board[j-1][l] != 1) {
        						//	System.out.println("LOL2: " +" shipHitID: "+SHIP_HIT_IDS[i]+" i: "+i+  " Board: " + board[j-1][l]);
        						}
        					}
        					if (l-1 > -1) {
        						if (board[j][l-1] != SHIP_HIT_IDS[i] && board[j][l-1] != 0 && board[j][l-1] != 1) {
        						//	System.out.println("LOL3: "+" shipHitID: "+SHIP_HIT_IDS[i]+" i: "+i+  " Board: " + board[j][l-1]);
        						}
        					}
        				}
        			}
        		}
        	}
        }
    }  
	
public void printBoard1() { // this shows the actual position
		int p = 0;
		for (int i = 0; i < 10; i++) {// this can move but keep it here until i
			for (int j = 0; j < 10; j++) {// this can move but keep it here until i
				System.out.print(diagBoard[i][j]);
				if (diagBoard[i][j] == 1) {
					p++;
				}
			}
			System.out.println();
		}
		System.out.println();
		System.out.println("1 is shown: " + p);
		System.out.println();
	}
	

	
public void printBoard2() { // this shows the actual position
		
		for (int i = 0; i < 10; i++) {// this can move but keep it here until i
			for (int j = 0; j < 10; j++) {// this can move but keep it here until i
			System.out.print(board[i][j]);
			}
			System.out.println();
		}
		System.out.println();
	}
}

    /*\
   // \\
  // L \\
 //S   A\\
//L     B\\
\*_______*/


//also, make it so that it takes out placement/attack methods as the game goes on... maybe. (like if its doing bad why keep it?)

/*
The attack method is that it calculates how many ways a ship can fit on each
* tile then multiplies that number by the heat(probablity) of that ship being
* on that tile. It does this for each ship that is still alive.*/ //do this?




// placement method ideas : 
//4 outside (but not corners) 
// just the inside (no corners)
//best squares
//best squares for certain ships and place others in obvious spots


//also: for the method where i take a few shots from last turn and see if their ships are in the same place: i can also store where i missed last turn as well so i dont attack there. Lul why didnt i think of that earlier? (prob because no one really uses that)
//also: for me : when i am ermebering where i put my ships last game if i won... check to see for the next like 10 games if i lose or not, if so dont use that method for another 1k games.



//TO COUNTER ADAPTIVE STRATEGY: LOOK AT THE PLACES I HAVENT ATTACKED A LOT AND ATTACK THOSE PLACES FOR MY NEXT SHOT.

//NEW IDEA FOR STORING CURRENT PLACEMENT AND ATTACK SCORES... use a decay method so that it basically only looks at the past 500 games or so and not the OVERALL total (or maybe both)
//also dont forget to take out the freaking same placement after your turn... especially if youre losing quickly off of it (make it so if i use the same placement and lose right away change it or something idk)