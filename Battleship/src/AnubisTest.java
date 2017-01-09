import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

/*
 * Who will Ryze?
 * Thank you Khan for the class system and most of all the logging system as well as a base for an adaptive strategy. 
 * Thank you RunningGazelle and CaptinStanleyHTweedle for the "CheckerBoard" attack idea. (modding the x/y coordinates) 
 * Thank you Mal for implementing more classes in my AI easier to understand. 
 * ~Anubis~ The god risen from the unfinished god Sobek - come back to claim the souls of many that were left untouched.
 * -Brian Dassow Jr. Spring 2015-
 */

public class AnubisTest implements Captain, Constants {
	public final int[] SHIP_LENGTHS = new int[] { 2, 3, 3, 4, 5 };
	public final int[] SHIP_HIT_IDS = new int[] { 3, 4, 5, 6, 7 };
	public final boolean LOGGING = true;

	public final int AMOUNT_OF_PLACEMENT_METHODS = 2;
	public final int AMOUNT_OF_ATTACK_METHODS = 5;

	public final int GAME_THRESHOLD_TILL_CHECK = 1300001;
	public final int GAME_THRESHOLD_TILL_RESET = 5000000;
	public final int GAME_THRESHOLD_TILL_SAMPLE = 1003000;

	public final int AMOUNT_OF_WINLOSS_SAMPLE_GAMES = 300; 
	public final int AMOUNT_OF_TOTAL_SAMPLE_GAMES = 600; 
	public final int AMOUNT_OF_GAMES_NEEDED_FOR_SWITCH = 50;

	public final int NEGATIVE = -1;

	protected Random generator;
	protected Fleet myFleet;

	boolean[] shipSunk, shipHitLastTurn, seeker, shipHOrVGuessed,
			attackSampling, placementSampling;
	boolean isSeeking, wonLastGame, placedShipsSameLocation;

	int wins, losses, cycle, whichShip, focusedShip, hitCycle, hitMod, sunkMod,
			numberOfAttacks, trueTotalGames, totalGames,
			diagnosticGames, winCheck, checkCycle, totalNumberOfMatches,
			currentAttackMethod, currentPlacementMethod, random3, random4,
			random2, index, totalAttacks, totalOpponentAttacks, misses,
			gamesSinceLastPlacementSwitch, gamesSinceLastAttackSwitch,
			checkLastTurnHit, sameLocationCount;

	int[] shipDirection, mod3AttackHits,
			mod4AttackHits;
	int[][] board;

	long latestTime, beginTime;

	Coordinate lastAttack, killCoordinate, attackCoordinate;
	Coordinate[] coordHit;

	HashMap<Integer, BoardCoordinate> theirPlacementHash, theirAttackHash;
	Opponent myOpponent = new Opponent("");
	PlacementMethod[] allPlacementMethods;
	AttackMethod[] allAttackMethods;
	Ship[] whereAreMyShips;

	public AnubisTest() {
		placementSampling = new boolean[AMOUNT_OF_PLACEMENT_METHODS];
		attackSampling = new boolean[AMOUNT_OF_ATTACK_METHODS];
		allAttackMethods = new AttackMethod[AMOUNT_OF_ATTACK_METHODS];
		allPlacementMethods = new PlacementMethod[AMOUNT_OF_PLACEMENT_METHODS];

		

		shipDirection = new int[5];

		shipHOrVGuessed = new boolean[5];
		shipSunk = new boolean[5];
		shipHitLastTurn = new boolean[5];
		seeker = new boolean[5];

		coordHit = new Coordinate[5];

		lastAttack = new Coordinate(0, 0);
		killCoordinate = new Coordinate(0, 0);

	}

	@Override
	public void initialize(int numMatches, int numCaptains, String opponent) { 
		board = new int[10][10];
		mod3AttackHits = new int[3];
		mod4AttackHits = new int[4];
		generator = new Random();
		myFleet = new Fleet();

		for (int i = 0; i < 5; i++) {
			shipHOrVGuessed[i] = false;
			shipSunk[i] = false;
			seeker[i] = false;
		}

		if (!myOpponent.name.equals(opponent)) {
			placedShipsSameLocation = false;
			sameLocationCount = 0;

			totalNumberOfMatches = numMatches;
			for (int i = 0; i < AMOUNT_OF_PLACEMENT_METHODS; i++) {
				placementSampling[i] = true;
			}

			for (int i = 0; i < AMOUNT_OF_ATTACK_METHODS; i++) {
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

			allPlacementMethods[0] = new PlacementMethod("Corners");
			allPlacementMethods[1] = new PlacementMethod("Adaptive");

			allAttackMethods[0] = new AttackMethod("mod3+2 no intelligent");
			allAttackMethods[1] = new AttackMethod("mod3+2 inteli");
			allAttackMethods[2] = new AttackMethod("mod4+mod3inteli+mod2");
			allAttackMethods[4] = new AttackMethod("mod3inteli+mod2");
			allAttackMethods[3] = new AttackMethod("mod4inteli+mod3Inteli+mod2");

			for (int i = 0; i < 5; i++) {
				shipHitLastTurn[i] = false;
			}
		}

		index = misses = totalAttacks = totalOpponentAttacks = 0;

		mod3AttackHits[0] = 34;
		mod3AttackHits[1] = mod3AttackHits[2] = 33;

		mod4AttackHits[0] = 25;
		mod4AttackHits[1] = 26;
		mod4AttackHits[2] = 25;
		mod4AttackHits[3] = 24;

		checkLastTurnHit = 0;

		myOpponent.resetShips();

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				myOpponent.theirPlacementBoard[i][j].attacked = false;
				myOpponent.theirAttackBoard[i][j].attacked = false;//use this!
			}
		}
		cycle = whichShip = numberOfAttacks = 0;
		focusedShip = -1;
		hitCycle = 1;
		myOpponent.totalOpponentAttacks = 150;
		isSeeking = false;
		myOpponent.sortBoards();

		random2 = generator.nextInt(2);
		random3 = generator.nextInt(3);
		random4 = generator.nextInt(4);

		placeShipController();
	}

	public void placeShipController() {
		switch (currentPlacementMethod) {
			case 0:
				for (int i = 0; i < 5; i++) {
					placeShipAdaptive(i);
				}
				break;
			case 1:
				cornerPlacement2p0();
				break;
			}
		whereAreMyShips = myFleet.getFleet();
	}
	
	public void cornerPlacement2p0() {
		int howmuch = 2;
		ArrayList<Coordinate> Coordinates = new ArrayList<Coordinate>();
		Coordinate coordinate = new Coordinate(0 + generator.nextInt(howmuch), 0 + generator.nextInt(howmuch));
		Coordinates.add(coordinate);
		coordinate = new Coordinate(0 + generator.nextInt(howmuch), 9 - generator.nextInt(howmuch));
		Coordinates.add(coordinate);
		coordinate = new Coordinate(9 - generator.nextInt(howmuch), 0 + generator.nextInt(howmuch));
		Coordinates.add(coordinate);
		coordinate = new Coordinate(9 - generator.nextInt(howmuch), 9 - generator.nextInt(howmuch));
		Coordinates.add(coordinate);
		
		Collections.shuffle(Coordinates);
		
		for (int i = 0; i < 4; i++) {
			int orientation = generator.nextInt(2);
			if (orientation == VERTICAL && Coordinates.get(i).getY() + SHIP_LENGTHS[i] > 9) {
				if (!myFleet.placeShip(Coordinates.get(i).getX(), Coordinates.get(i).getY() - SHIP_LENGTHS[i] + 1, orientation, i)) {
					System.out.println("failed to place ship " + i + " at coordinates " + Coordinates.get(i).getX() + "," + Coordinates.get(i).getY() + "scenario 1");
					while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
					}
				}
			} else if (orientation == HORIZONTAL && Coordinates.get(i).getX() + SHIP_LENGTHS[i] > 9) {
				if (!myFleet.placeShip(Coordinates.get(i).getX() - SHIP_LENGTHS[i] + 1, Coordinates.get(i).getY(), orientation, i)) {
					System.out.println("failed to place ship " + i + " at coordinates " + Coordinates.get(i).getX() + "," + Coordinates.get(i).getY() + "scenario 2");
					while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
					}
				}
			} else {
				if (!myFleet.placeShip(Coordinates.get(i).getX(), Coordinates.get(i).getY(), orientation, i)) {
					System.out.println("failed to place ship " + i + " at coordinates " + Coordinates.get(i).getX() + "," + Coordinates.get(i).getY() + "scenario 3");
					while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), i)) {
					}
				}
			}
			
		}
		placeShipAdaptive(4);
	}
	

	public void placeShipRandom(int shipID) {
		while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10),
				generator.nextInt(2), shipID)) {
		}
	}

	public void placeShipAdaptive(int shipID) {
		boolean shipPlaced = false;
		int count = 99;
		while (!shipPlaced) {
			int orientation = generator.nextInt(2);
			if (orientation == VERTICAL && theirAttackHash.get(count).y == 9) {
				if (myFleet.placeShip(theirAttackHash.get(count).x, theirAttackHash.get(count).y - SHIP_LENGTHS[shipID] + 1, orientation, shipID)) {
					shipPlaced = true;
				}
				count--;
			} else if (orientation == HORIZONTAL && theirAttackHash.get(count).y == 9) {
				if (myFleet.placeShip(theirAttackHash.get(count).x - SHIP_LENGTHS[shipID] + 1, theirAttackHash.get(count).y, orientation, shipID)) {
					shipPlaced = true;
				}
				count--;
			} else {
				if (myFleet.placeShip(theirAttackHash.get(count).x, theirAttackHash.get(count).y, orientation, shipID)) {
					shipPlaced = true;
				}
				count--;
			}
		}
	}

	@Override
	public Fleet getFleet() {
		return myFleet;
	}

	@Override
	public Coordinate makeAttack() {
		do {
			lastAttack = makeAttackController();
		} while (lastAttack.getX() > 9 || lastAttack.getY() > 9 || lastAttack.getX() < 0 || lastAttack.getY() < 0 || board[lastAttack.getX()][lastAttack.getY()] != 0);

		numberOfAttacks++;
		board[lastAttack.getX()][lastAttack.getY()] = 1;
		return lastAttack;
	}

	public Coordinate makeAttackController() {
		if (focusedShip != -1) {
			isSeeking = true;
			return iWillFindYou(focusedShip);
		}
		
		switch (currentAttackMethod) {
			case 0:
				if (mod3AttackHits[random3] > 0) {
					return modAttack(3, random3);
				}
				return modAttack(2, random2);	
				
			case 1:
				if (mod3AttackHits[random3] > 0) {
					return intelligentModAttack(3, random3);
				}
				return modAttack(2, random2);
			
			case 2:
				if (mod4AttackHits[random4] > 0) {
					return intelligentModAttack(4, random4);
				} else if (mod3AttackHits[random3] > 0) {
					return intelligentModAttack(3, random3);
				}
				return modAttack(2, random2);
				
			case 3:
				if (mod4AttackHits[random4] > 0) {
					return modAttack(4, random4);
				} else if (mod3AttackHits[random3] > 0) {
					return intelligentModAttack(3, random3);
				}
				return modAttack(2, random2);	

			case 4:
				if (mod3AttackHits[random3] > 0) {
					return intelligentModAttack(3, random3);
				}
				return intelligentModAttack(2, random2);
		}
		
		System.out.println("THIS SHOULD NEVER EVER HAPPEN!!");
		return makeAttackController();
	}

	public Coordinate randomAttack() {
		do {
			attackCoordinate = new Coordinate(generator.nextInt(10), generator.nextInt(10));
		} while (board[attackCoordinate.getX()][attackCoordinate.getY()] != 0 || !myOpponent.canAnyShipBeHere(attackCoordinate.getX(), attackCoordinate.getY()));
		return attackCoordinate;
	}

	public Coordinate modAttack(int modID, int squareID) {//can it not be up to 999 can you create something so it can look through that? kthx also itll be a little faster..
		int count = 0;
		do {
			if (count > 999) {
				if (modID == 3) {
					mod3AttackHits[squareID] = 0;
				} else {
					mod4AttackHits[squareID] = 0;
				}
				return makeAttackController();
			}
			attackCoordinate = new Coordinate(generator.nextInt(10), generator.nextInt(10));
			count++;
		} while (board[attackCoordinate.getX()][attackCoordinate.getY()] != 0 || ((attackCoordinate.getX() + attackCoordinate.getY()) % modID) != squareID || !myOpponent.canAnyShipBeHere(attackCoordinate.getX(), attackCoordinate.getY()));
		return attackCoordinate;
	}

	public Coordinate intelligentModAttack(int modID, int squareID) {
		do {
			if (index < 100) {
				attackCoordinate = myOpponent.getAttackCoordinate(index++);
			} else {
				if (modID == 3) {
					mod3AttackHits[squareID] = 0;
				} else {
					mod4AttackHits[squareID] = 0;
				}
				index = 0;
				return makeAttackController();
			}
		} while (board[attackCoordinate.getX()][attackCoordinate.getY()] != 0 || ((attackCoordinate.getX() + attackCoordinate.getY()) % modID) != squareID || !myOpponent.canAnyShipBeHere(attackCoordinate.getX(), attackCoordinate.getY()));
		return attackCoordinate;
	}

	public Coordinate intelligentAttack() {
		do {
			attackCoordinate = myOpponent.getAttackCoordinate(index++);
		} while (board[attackCoordinate.getX()][attackCoordinate.getY()] != 0);
		return attackCoordinate;
	}

	public Coordinate iWillFindYou(int shipID) {
		if (!shipHOrVGuessed[shipID]) {
			myOpponent.theirShipInfo[shipID].canShipBeHorizontalOrVertical();
			if (myOpponent.theirShipInfo[shipID].horizontalVsVertical() == VERTICAL) {
				cycle = cycle + 2;
			}
			shipHOrVGuessed[shipID] = true;
		}
		return andIWillKillYou(shipID);
	}

	public Coordinate andIWillKillYou(int shipID) {
		switch (cycle) {
		case 0:
			if (myOpponent.theirShipInfo[shipID].isHorizontal()) {
				killCoordinate = new Coordinate(coordHit[shipID].getX() + hitCycle, coordHit[shipID].getY());
				cycle = 0;
				break;
			}
		case 1:
			if (myOpponent.theirShipInfo[shipID].isHorizontal()) {
				killCoordinate = new Coordinate(coordHit[shipID].getX() + (NEGATIVE * hitCycle), coordHit[shipID].getY());
				cycle = 1;
				break;
			}
		case 2:
			if (myOpponent.theirShipInfo[shipID].isVertical()) {
				killCoordinate = new Coordinate(coordHit[shipID].getX(), coordHit[shipID].getY() + hitCycle);
				cycle = 2;
				break;
			}
		case 3:
			if (myOpponent.theirShipInfo[shipID].isVertical()) {
				killCoordinate = new Coordinate(coordHit[shipID].getX(), coordHit[shipID].getY() + (NEGATIVE * hitCycle));
				cycle = 3;
			} else {
				cycle = 0;
				return andIWillKillYou(shipID);
			}
			break;
		}
		if (killCoordinate.getX() > 9 || killCoordinate.getY() > 9 || killCoordinate.getX() < 0 || killCoordinate.getY() < 0 || !myOpponent.canThisShipActuallyBeHere(killCoordinate, shipID)) {
			if (cycle == 3) {
				cycle = 0;
			} else {
				cycle++;
			}
			hitCycle = 1;
			return andIWillKillYou(shipID);
		}
		hitCycle++;
		return killCoordinate;
	}

	public void didHeDie() {
		if (isSeeking) {
			if (hitMod != focusedShip) {
				if (cycle < 2) {
				//	myOpponent.theirShipInfo[focusedShip].hAvailSpaces--;
				} else {
				//	myOpponent.theirShipInfo[focusedShip].vAvailSpaces--;
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
	public void resultOfAttack(int result) {
		totalAttacks++;
		hitMod = result % HIT_MODIFIER;
		sunkMod = result % SUNK_MODIFIER;
		checkLastAttack();
		didHeDie();

		if (hitMod < 5) {
			myOpponent.theirShipInfo[hitMod].shipPlacement[lastAttack.getX()][lastAttack.getY()].score++;
			myOpponent.theirPlacementBoard[lastAttack.getX()][lastAttack.getY()].attacked(-1);
			board[lastAttack.getX()][lastAttack.getY()] = SHIP_HIT_IDS[hitMod];

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
		else if (result == MISS) {
			misses++;
			myOpponent.theirPlacementBoard[lastAttack.getX()][lastAttack.getY()].attacked(1);//maybe change this back to recordmiss/record hit just becase it looks nicer
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
		totalOpponentAttacks++;
		myOpponent.recordOpponentAttack(coord.getX(), coord.getY());
	}

	@Override
	public void resultOfGame(int result) {
		totalGames++;
		trueTotalGames++;
		diagnosticGames++;
		gamesSinceLastAttackSwitch++;
		gamesSinceLastPlacementSwitch++;

		allAttackMethods[currentAttackMethod].totalGames++;
		allAttackMethods[currentAttackMethod].trueTotalGames++;
		allPlacementMethods[currentPlacementMethod].totalGames++;
		allPlacementMethods[currentPlacementMethod].trueTotalGames++;

		allAttackMethods[currentAttackMethod].recordWin(totalAttacks);
		
		if (result == WON) {
			wins++;
			wonLastGame = true;
		} else {
			allPlacementMethods[currentPlacementMethod].recordLoss(totalOpponentAttacks);
			losses++;
			wonLastGame = false;
		}
		
		if (attackSampling[currentAttackMethod]) {
			if (allAttackMethods[currentAttackMethod].wins >= AMOUNT_OF_WINLOSS_SAMPLE_GAMES || allAttackMethods[currentAttackMethod].totalGames >= AMOUNT_OF_TOTAL_SAMPLE_GAMES) {
				attackSampling[currentAttackMethod] = false;
				log("Attack Method " + allAttackMethods[currentAttackMethod].name + " has accuracy %6.3f attacks.\n", allAttackMethods[currentAttackMethod].accuracy);
				if (currentAttackMethod < AMOUNT_OF_ATTACK_METHODS - 1) {
					currentAttackMethod++;
				} else {
					currentAttackMethod = findBestAttackMethod();
					log("Chose Attack Method " + allAttackMethods[currentAttackMethod].name);
				}
			}
		} else {
			if (gamesSinceLastAttackSwitch >= AMOUNT_OF_GAMES_NEEDED_FOR_SWITCH) {
				int bestAttackMethod = findBestAttackMethod();
				if (bestAttackMethod != currentAttackMethod) {
					log("New undead have come to us! Switching to %s with accuracy %.4f, appears better than %s with %.4f after %d games\n", allAttackMethods[bestAttackMethod].name, allAttackMethods[bestAttackMethod].accuracy, allAttackMethods[currentAttackMethod].name, allAttackMethods[currentAttackMethod].accuracy, gamesSinceLastAttackSwitch);
					currentAttackMethod = bestAttackMethod;
					gamesSinceLastAttackSwitch = 0;
				}
			}
		}

		if (placementSampling[currentPlacementMethod]) {
			if (allPlacementMethods[currentPlacementMethod].losses >= AMOUNT_OF_WINLOSS_SAMPLE_GAMES || allPlacementMethods[currentPlacementMethod].totalGames >= AMOUNT_OF_TOTAL_SAMPLE_GAMES) {
				placementSampling[currentPlacementMethod] = false;
				log("Placement Method " + allPlacementMethods[currentPlacementMethod].name + " has accuracy %6.3f attacks.\n", allPlacementMethods[currentPlacementMethod].accuracy);
				if (currentPlacementMethod < AMOUNT_OF_PLACEMENT_METHODS - 1) {
					currentPlacementMethod++;
				} else {
					currentPlacementMethod = findBestPlacementMethod();
					log("Chose Placement Method " + allPlacementMethods[currentPlacementMethod].name);
				}
			}
		} else {
			if (gamesSinceLastPlacementSwitch >= AMOUNT_OF_GAMES_NEEDED_FOR_SWITCH) {
				int bestPlacementMethod = findBestPlacementMethod();
				if (bestPlacementMethod != currentPlacementMethod) {
					log("Cleanse the Nile! Switching to %s with accuracy %.4f, appears better than %s with %.4f after %d games\n", allPlacementMethods[bestPlacementMethod].name, allPlacementMethods[bestPlacementMethod].accuracy, allPlacementMethods[currentPlacementMethod].name, allPlacementMethods[currentPlacementMethod].accuracy, gamesSinceLastPlacementSwitch);
					currentPlacementMethod = bestPlacementMethod;
					gamesSinceLastPlacementSwitch = 0;
				}
			}
		}

		if (LOGGING) {
			if (trueTotalGames == 1) {
				log(" The end of time is near, Anubis awakens.");
				beginTime = System.currentTimeMillis();
				latestTime = beginTime;
			}
			if (trueTotalGames == totalNumberOfMatches) {
				log("Final Statistics: wins: %5.2f%%, sec=%.2f\n", 100.0 * wins / totalNumberOfMatches, (System.currentTimeMillis() - beginTime) / 1000.0);
				for (int i = 0; i < AMOUNT_OF_PLACEMENT_METHODS; i++) {
					log("Placement %30s: used %6.2f%%, accuracy %.4f\n", allPlacementMethods[i].name, 100.0 * allPlacementMethods[i].trueTotalGames / totalNumberOfMatches, allPlacementMethods[i].accuracy);
				}
				System.out.println();
				for (int i = 0; i < AMOUNT_OF_ATTACK_METHODS; i++) {
					log("Attack %30s: used %6.2f%%, accuracy: %6.2f\n", allAttackMethods[i].name, 100.0 * allAttackMethods[i].trueTotalGames / totalNumberOfMatches,allAttackMethods[i].accuracy);
				}
			} else if (trueTotalGames % 25000 == 0) {
				long now = System.currentTimeMillis();
				log("%d UPDATE Overall: %.2f%%, sec=%.2f, Placement -%s-: placementav: %.2f, Attack -%s-: attackav: %.2f, attackpav: %.5f%%\n", trueTotalGames, wins * 100.0 / trueTotalGames, (now - latestTime) / 1000.0, allPlacementMethods[currentPlacementMethod].name,allPlacementMethods[currentPlacementMethod].accuracy, allAttackMethods[currentAttackMethod].name,allAttackMethods[currentAttackMethod].accuracy, allAttackMethods[currentAttackMethod].accuracyPercent);
				latestTime = now;
			}
		}

		if (totalGames == GAME_THRESHOLD_TILL_RESET) {
			resetPlacementAndAttackMethods();
			myOpponent.resetBoards();
			currentAttackMethod = 0;
			currentPlacementMethod = 0;
			totalGames = 0;
		}

		if (diagnosticGames == GAME_THRESHOLD_TILL_CHECK) {
			diagnosticGames = 0;
			checkGame();
		}
	}

	// thank you Khan
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
		for (int i = 0; i < AMOUNT_OF_PLACEMENT_METHODS; i++) {
			allPlacementMethods[i].reset();
			placementSampling[i] = true;
		}

		for (int i = 0; i < AMOUNT_OF_ATTACK_METHODS; i++) {
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

	public void checkGame() {
		if (((GAME_THRESHOLD_TILL_CHECK) - (wins - winCheck)) > ((GAME_THRESHOLD_TILL_CHECK) / 2)) {
		}
		checkCycle++;
		winCheck = wins;
	}

	public void checkLastAttack() {
		if ((lastAttack.getX() + lastAttack.getY()) % 3 == random3) {
			mod3AttackHits[random3]--;
		}
		if ((lastAttack.getX() + lastAttack.getY()) % 4 == random4) {
			mod4AttackHits[random4]--;
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

		public void recordLoss(int totalAttacks) {
			this.totalAttacks += totalAttacks;
			allAttackMethods[currentAttackMethod].losses++;
			losses++;
			accuracy = (double) this.totalAttacks / (double) losses;
		}

		public void reset() {
			totalAttacks = totalGames = wins = losses = 0;
			accuracy = 100;
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
			trueTotalGames = wins = losses = totalGames = 0;
			accuracy = accuracyPercent = 0;
		}

		public void recordWin(int totalAttacks) {
			this.totalAttacks += totalAttacks;
			allPlacementMethods[currentPlacementMethod].wins++;
			wins++;
			accuracy = (double) this.totalAttacks / (double) wins;
		}

		public void updateAccuracy(int totalAttacks) {
			accuracyPercent = (((double) this.totalAttacks - (double) totalMisses) / (double) this.totalAttacks) * 100.0;
		}

		public void reset() {
			totalAttacks = totalGames = wins = losses = 0;
			accuracy = 0;
		}

	}

	public class ShipInfo {
		BoardCoordinate[][] shipPlacement;
		int shipID, length, vertOrHori, amountOfVPlacements,
				amountOfHPlacements, positiveHAvailSpaces, negativeHAvailSpaces, positiveVAvailSpaces, negativeVAvailSpaces;
		boolean canBeHorizontal, canBeVertical, sunk;
		Coordinate hitLastTurn;
		

		public ShipInfo(int shipID, int length) {
			shipPlacement = new BoardCoordinate[10][10];
			this.shipID = shipID;
			this.length = length;
			vertOrHori = -1;
			canBeHorizontal = false;
			canBeVertical = false;
			amountOfHPlacements = amountOfVPlacements = 0;
			negativeHAvailSpaces = positiveVAvailSpaces = negativeVAvailSpaces = 0;
			sunk = false;
			hitLastTurn = new Coordinate(-1, -1);
			
			initializeShip();
		}
		
		public void initializeShip() {
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					shipPlacement[i][j] = new BoardCoordinate(i, j, (i*10)+j);
				}
			}
		}

		public void canShipBeHorizontalOrVertical() {
			for (int i = 1 - length; i < length; i++) {//negative h and positive because then i can see how many spaces it has left when hunting it
				if ((i + coordHit[shipID].getX()) > -1 && (i + coordHit[shipID].getX() < 10) && i != 0) {
					if (board[coordHit[shipID].getX() + i][coordHit[shipID].getY()] == SHIP_HIT_IDS[shipID] || board[coordHit[shipID].getX() + i][coordHit[shipID].getY()] == 0) {
						if (i < 0) {
							negativeHAvailSpaces++;
						}
						else {
							positiveHAvailSpaces++;
						}
					}
				}

				if ((i + coordHit[shipID].getY()) > -1 && (i + coordHit[shipID].getY() < 10) && i != 0) {
					if (board[coordHit[shipID].getX()][coordHit[shipID].getY() + i] == SHIP_HIT_IDS[shipID]|| board[coordHit[shipID].getX()][coordHit[shipID].getY() + i] == 0) {
						if (i < 0) {
							negativeVAvailSpaces++;
						}
						else {
							positiveVAvailSpaces++;
						}
					}
				}
			}
			
			if ((negativeHAvailSpaces + positiveHAvailSpaces) >= SHIP_LENGTHS[shipID] - 1) {
				canBeHorizontal = true;
			}
			
			if ((negativeVAvailSpaces + positiveVAvailSpaces) >= SHIP_LENGTHS[shipID] - 1) {
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
			for (int i = 1 - length; i < length; i++) {
				if ((i + coordHit[shipID].getX()) > -1 && (i + coordHit[shipID].getX() < 10) && i != 0) {
					if (board[coordHit[shipID].getX() + i][coordHit[shipID].getY()] == SHIP_HIT_IDS[shipID]) {
						amountOfHPlacements++;
						canBeVertical = false;
						return HORIZONTAL;
					}
				}

				if ((i + coordHit[shipID].getY()) > -1 && (i + coordHit[shipID].getY() < 10) && i != 0) {
					if (board[coordHit[shipID].getX()][coordHit[shipID].getY() + i] == SHIP_HIT_IDS[shipID]) {
						amountOfVPlacements++;
						canBeHorizontal = false;
						return VERTICAL;
					}
				}
			}
			System.out.println("THISSHOULDNTHAPPEN!!!");
			return 1;
		}

		public int horizontalVsVertical() {
			if (amountOfHPlacements > amountOfVPlacements) {
				return HORIZONTAL;
			}
			return VERTICAL;

		}

		public boolean isHorizontal() {
			if (vertOrHori == -1 || vertOrHori == HORIZONTAL && (negativeHAvailSpaces + positiveHAvailSpaces) >= SHIP_LENGTHS[shipID] - 1) {
				canBeHorizontal = true;
				return true;
			}
			canBeHorizontal = false;
			return false;
		}

		public boolean isVertical() {
			if (vertOrHori == -1 || vertOrHori == VERTICAL && (negativeVAvailSpaces + positiveVAvailSpaces) >= SHIP_LENGTHS[shipID] - 1) {
				canBeVertical = true;
				return true;
			}
			canBeVertical = false;
			return false;
		}
	}

	public class BoardCoordinate {
		boolean attacked;
		int x, y, score, boardID;

		public BoardCoordinate(int x, int y, int ID) {
			boardID = ID;
			attacked = false;
			this.x = x;
			this.y = y;
			score = 0;
		}

		public void attacked(int ID) {
			score += ID;
			attacked = true;
		}

		public void reset() {
			score = 0;
		}

		@Override
		public String toString() {
			return String.format("(%2d, %2d)", x, y);
		}
	}

	public class Opponent {
		BoardCoordinate[][] theirAttackBoard, theirPlacementBoard;
		ShipInfo[] theirShipInfo;
		String name;

		int totalOpponentAttacks;
		int[][] oppAttackBoard;

		public Opponent(String name) {
			this.name = name;
			theirShipInfo = new ShipInfo[5];
			oppAttackBoard = new int[10][10];
			theirAttackBoard = new BoardCoordinate[10][10];
			theirPlacementBoard = new BoardCoordinate[10][10];
			
			initializeOpponent();
		}

		public void initializeOpponent() {
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					theirAttackBoard[i][j] = new BoardCoordinate(i, j, (i*10)+j);
					theirPlacementBoard[i][j] = new BoardCoordinate(i, j, (i*10)+j);
				}
			}
			for (int i = 0; i < 5; i++) {
				theirShipInfo[i] = new ShipInfo(i, SHIP_LENGTHS[i]);
			}
		}

		public void resetBoards() {
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					theirAttackBoard[i][j].reset();
					theirPlacementBoard[i][j].reset();
				}
			}
		}
		
		public void sortBoards() {
			BoardCoordinate[] theirPlacementScores = new BoardCoordinate[100];
			BoardCoordinate[] theirAttackScores = new BoardCoordinate[100];
			
			for(int i = 0; i < 10; i++) {
				for (int j = 0; j < 10; j++) {
					theirPlacementScores[theirPlacementBoard[i][j].boardID] = theirPlacementBoard[i][j];
					theirAttackScores[theirAttackBoard[i][j].boardID] = theirAttackBoard[i][j];
				}
			}
			
			insertionSort(theirPlacementScores, 100);
			insertionSort(theirAttackScores, 100);
			
			for (int i = 0; i < 100; i++) {
				theirPlacementHash.put(i, theirPlacementBoard[theirPlacementScores[i].x][theirPlacementScores[i].y]);
				theirAttackHash.put(i, theirAttackBoard[theirAttackScores[i].x][theirAttackScores[i].y]);
			}
		}
		
		public void insertionSort(BoardCoordinate array[], int n) {
			int i, j;
			BoardCoordinate tempCoordinate;
			for (i = 1; i < n; i++) {
				j = i;
				while (j > 0 && array[j - 1].score > array[j].score) {
					tempCoordinate = array[j];
					array[j] = array[j-1];
					array[j-1] = tempCoordinate;
					j--;
				}
			}
		}

		public void recordOpponentAttack(int x, int y) {
			totalOpponentAttacks--;
			oppAttackBoard[x][y] = 1;
			theirAttackBoard[x][y].attacked(-1);
		}

		public boolean canAnyShipBeHere(int x, int y) {
			int hSpaceCount = 0, vSpaceCount = 0, smallestShipLength = findSmallestShip();

			for (int i = 1 - smallestShipLength; i < smallestShipLength; i++) {
				if ((i + x) > -1 && (i + x) < 10 && i != 0) {
					if (board[x + i][y] == 0) {
						hSpaceCount++;
					}
				}
				
				if ((i + y) > -1 && (i + y < 10) && i != 0) {
					if (board[x][y + i] == 0) {
						vSpaceCount++;
					}
				}
			}
			if (hSpaceCount >= smallestShipLength - 1 || vSpaceCount >= smallestShipLength - 1) {
				return true;
			}
			return false;
		}

		public int findSmallestShip() {
			for (int i = 0; i < 5; i++) {
				if (theirShipInfo[i].sunk == false) {
					return SHIP_LENGTHS[i];
				}
			}
			return -1;
		}

		public int findLargestShip() {
			for (int i = 4; i > -1; i--) {
				if (theirShipInfo[i].sunk == false) {
					return SHIP_LENGTHS[i];
				}
			}
			return -1;
		}

		public void resetShips() {
			for (int i = 0; i < 5; i++) {
				theirShipInfo[i].vertOrHori = -1;
				theirShipInfo[i].canBeHorizontal = false;
				theirShipInfo[i].canBeVertical = false;
				theirShipInfo[i].negativeHAvailSpaces = 0;
				theirShipInfo[i].positiveHAvailSpaces = 0;
				theirShipInfo[i].negativeVAvailSpaces = 0;
				theirShipInfo[i].positiveVAvailSpaces = 0;
				theirShipInfo[i].sunk = false;

				if (shipHitLastTurn[i]) {
					theirShipInfo[i].hitLastTurn = coordHit[i];
				} else {
					theirShipInfo[i].hitLastTurn = new Coordinate(-1, -1);
				}
				shipHitLastTurn[i] = false;
				coordHit[i] = new Coordinate(-1, -1);
			}
		}

		public boolean canThisShipActuallyBeHere(Coordinate coord, int shipID) {
			if (board[coord.getX()][coord.getY()] == SHIP_HIT_IDS[shipID] || board[coord.getX()][coord.getY()] == 0) {
				return true;
			}
			return false;
		}

		public Coordinate getAttackCoordinate(int ID) {
			return new Coordinate(theirPlacementHash.get(ID).x, theirPlacementHash.get(ID).y);
		}
	}
}
	/*\
   // \\
  // L \\
 //S   A\\
//L     B\\
\*_______*/