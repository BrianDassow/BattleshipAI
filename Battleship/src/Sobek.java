import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
/*
 * Who will Ryze?
 * Thank you Khan for the class system and most of all the logging system as well as a base for an adaptive strategy. 
 * Thank you CaptainAmerica for making Khan's corner placement easier to implement. 
 * Thank you RunningGazelle and CaptinStanleyHTweedle for the "CheckerBoard" attack idea. (modding the x/y coordinates) 
 * Thank you Mal for implementing more classes in my AI easier. 
 * ~Sobek~ - the incomplete god
 * -Brian Dassow Jr. 2014-
 */
public class Sobek implements Captain, Constants {
	public final int[] SHIP_LENGTHS = new int[] { 2, 3, 3, 4, 5 };
	public final int[] SHIP_HIT_IDS = new int[] { 3, 4, 5, 6, 7 };
	public final boolean LOGGING = false;

	public final int AMOUNT_OF_PLACEMENT_METHODS = 2;
	public final int AMOUNT_OF_ATTACK_METHODS = 5;

	public final int GAME_THRESHOLD_TILL_CHECK = 1300001;
	public final int GAME_THRESHOLD_TILL_RESET = 1000000;
	public final int GAME_THRESHOLD_TILL_SAMPLE = 1003000;

	public final int AMOUNT_OF_WINLOSS_SAMPLE_GAMES = 1000; 
	public final int AMOUNT_OF_TOTAL_SAMPLE_GAMES = 2000; 
	public final int AMOUNT_OF_GAMES_NEEDED_FOR_SWITCH = 100;

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

	int[] shipDirection, adaptiveScore, intelligentScore, mod3AttackHits,
			mod4AttackHits;
	int[][] board, diagBoard;

	long latestTime, beginTime;

	Coordinate lastAttack, killCoordinate, attackCoordinate;
	Coordinate[] coordHit;

	HashMap<Integer, BoardCoordinate> theirPlacementHash, theirAttackHash;
	Opponent myOpponent = new Opponent("");
	PlacementMethod[] allPlacementMethods;
	AttackMethod[] allAttackMethods;
	Ship[] whereAreMyShips;

	public Sobek() {
		placementSampling = new boolean[AMOUNT_OF_PLACEMENT_METHODS];
		attackSampling = new boolean[AMOUNT_OF_ATTACK_METHODS];
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
		killCoordinate = new Coordinate(0, 0);

	}

	@Override
	public void initialize(int numMatches, int numCaptains, String opponent) { 
		diagBoard = new int[10][10];
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

		myOpponent.resetStore();
		myOpponent.resetShips();

		for (int i = 0; i < 100; i++) {
			myOpponent.theirPlacementBoard[i].attacked = false;
			myOpponent.theirAttackBoard[i].attacked = false;
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
		if (wonLastGame && sameLocationCount == 0) {
			placedShipsSameLocation = true;
			for (int i = 0; i < 5; i++) {
				myFleet.placeShip(whereAreMyShips[i].getLocation(), whereAreMyShips[i].getDirection(), i);
			}
		} else {
			placedShipsSameLocation = false;
			switch (currentPlacementMethod) {

			case 0:
				cornerPlacement();
				break;
			case 1:
				for (int i = 0; i < 5; i++) {
					placeShipAdaptive(i);
				}
				break;
			}
		}
		whereAreMyShips = myFleet.getFleet();
	}

	// khan/CaptinAmerica
	public void cornerPlacement() { 
		Coordinate bottomLeft = new Coordinate(0, 0);
		Coordinate bottomRight = new Coordinate(0, 9);
		Coordinate topLeft = new Coordinate(9, 0);
		Coordinate topRight = new Coordinate(9, 9);
		Coordinate[] corners = { bottomLeft, bottomRight, topLeft, topRight };

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
		while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10),
				generator.nextInt(2), AIRCRAFT_CARRIER)) {
		}
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
			} else if (orientation == HORIZONTAL
					&& theirAttackHash.get(count).y == 9) {
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
		if (!wonLastGame && checkLastTurnHit < 5) {
			Coordinate lastTurnPlacement = myOpponent.giveLastTurnOppShipCoords();
			if (lastTurnPlacement.getX() != -1) {
				return lastTurnPlacement;
			}
		}

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
			attackCoordinate = new Coordinate(generator.nextInt(10),
					generator.nextInt(10));
		} while (board[attackCoordinate.getX()][attackCoordinate.getY()] != 0 || !myOpponent.canAnyShipBeHere(attackCoordinate.getX(), attackCoordinate.getY()));
		return attackCoordinate;
	}

	public Coordinate modAttack(int modID, int squareID) {
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
			attackCoordinate = new Coordinate(generator.nextInt(10),
					generator.nextInt(10));
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
			myOpponent.theirShipInfo[shipID].calculateIfShipCanBeHorizontalOrVertical();
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
	public void resultOfAttack(int result) {
		totalAttacks++;

		hitMod = result % HIT_MODIFIER;
		sunkMod = result % SUNK_MODIFIER;
		checkLastAttack();
		didHeDie();
		
		if (checkLastTurnHit < 5 && !wonLastGame && !isSeeking) {
			if (hitMod != (checkLastTurnHit)) {
				checkLastTurnHit += 100000;
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
		else if (result == MISS) {
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
			myOpponent.recordHit();
			wins++;
			wonLastGame = true;
		} else {
			if (placedShipsSameLocation) {
				sameLocationCount += 500;
			}

			allPlacementMethods[currentPlacementMethod].recordLoss(totalOpponentAttacks);
			myOpponent.recordHit();
			losses++;
			wonLastGame = false;
		}
		if (sameLocationCount > 0) {
			sameLocationCount--;
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
					log("Release the crocs! Switching to %s with accuracy %.4f, appears better than %s with %.4f after %d games\n", allAttackMethods[bestAttackMethod].name, allAttackMethods[bestAttackMethod].accuracy, allAttackMethods[currentAttackMethod].name, allAttackMethods[currentAttackMethod].accuracy, gamesSinceLastAttackSwitch);
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
				log(" The beginning of the end.");
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
		int shipID, length, vertOrHori, amountOfVPlacements,
				amountOfHPlacements, hAvailSpaces, vAvailSpaces;
		boolean canBeHorizontal, canBeVertical, sunk;
		Coordinate hitLastTurn;

		public ShipInfo(int shipID, int length) {
			this.shipID = shipID;
			this.length = length;
			vertOrHori = -1;
			canBeHorizontal = false;
			canBeVertical = false;
			amountOfHPlacements = amountOfVPlacements = 0;
			hAvailSpaces = vAvailSpaces = 0;
			sunk = false;
			hitLastTurn = new Coordinate(-1, -1);
		}

		public void calculateIfShipCanBeHorizontalOrVertical() {
			boolean horizontalP1, horizontalP2, verticalP1, verticalP2;

			horizontalP1 = true;
			horizontalP2 = true;
			verticalP1 = true;
			verticalP2 = true;

			for (int i = 1; i < SHIP_LENGTHS[shipID]; i++) {
				if ((i + coordHit[shipID].getX()) > -1 && (i + coordHit[shipID].getX() < 10)) {
					if (board[coordHit[shipID].getX() + i][coordHit[shipID].getY()] == SHIP_HIT_IDS[shipID] || board[coordHit[shipID].getX() + i][coordHit[shipID].getY()] == 0) {
						if (horizontalP1) {
							hAvailSpaces++;
						}
					} else {
						horizontalP1 = false;
					}
				}
				if (((i * NEGATIVE) + coordHit[shipID].getX()) > -1 && ((i * NEGATIVE) + coordHit[shipID].getX() < 10)) {
					if (board[coordHit[shipID].getX() + (i * NEGATIVE)][coordHit[shipID].getY()] == SHIP_HIT_IDS[shipID] || board[coordHit[shipID].getX() + (i * NEGATIVE)][coordHit[shipID].getY()] == 0) {
						if (horizontalP2) {
							hAvailSpaces++;
						}
					} else {
						horizontalP2 = false;
					}
				}

				if ((i + coordHit[shipID].getY()) > -1 && (i + coordHit[shipID].getY() < 10)) {
					if (board[coordHit[shipID].getX()][coordHit[shipID].getY() + i] == SHIP_HIT_IDS[shipID]|| board[coordHit[shipID].getX()][coordHit[shipID].getY() + i] == 0) {
						if (verticalP1) {
							vAvailSpaces++;
						}
					} else {
						verticalP1 = false;
					}
				}
				if (((i * NEGATIVE) + coordHit[shipID].getY()) > -1 && ((i * NEGATIVE) + coordHit[shipID].getY() < 10)) {
					if (board[coordHit[shipID].getX()][coordHit[shipID].getY() + (i * NEGATIVE)] == SHIP_HIT_IDS[shipID] || board[coordHit[shipID].getX()][coordHit[shipID].getY() + (i * NEGATIVE)] == 0) {
						if (verticalP2) {
							vAvailSpaces++;
						}
					} else {
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
			return -1;
		}

		public int horizontalVsVertical() {
			if (amountOfHPlacements > amountOfVPlacements) {
				return HORIZONTAL;
			}
			return VERTICAL;

		}

		public boolean isHorizontal() {
			if (vertOrHori == -1 || vertOrHori == HORIZONTAL && hAvailSpaces >= SHIP_LENGTHS[shipID] - 1) {
				canBeHorizontal = true;
				return true;
			}
			canBeHorizontal = false;
			return false;
		}

		public boolean isVertical() {
			if (vertOrHori == -1 || vertOrHori == VERTICAL && vAvailSpaces >= SHIP_LENGTHS[shipID] - 1) {
				canBeVertical = true;
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
			score = 0;
		}

		public void attacked(int x, int y, int ID) {
			if (this.x == x && this.y == y) {
				score += ID;
				attacked = true;
			}
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
				theirShipInfo[i] = new ShipInfo(i, SHIP_LENGTHS[i]);
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
			idStore[storeID] = ID;
			storeX[storeID] = x;
			storeY[storeID] = y;
			storeID++;
		}

		public void resetStore() {
			storeID = 0;
		}

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

			for (int i = 1; i < smallestShipLength; i++) {
				if (i + x < 10) {
					if (board[x + i][y] == 0) {
						if (horizontalP1) {
							hSpaceCount++;
						}
					} else {
						horizontalP1 = false;
					}
				}

				if (((i * NEGATIVE) + x) > -1) {
					if (board[x + (i * NEGATIVE)][y] == 0) {
						if (horizontalP2) {
							hSpaceCount++;
						}
					} else {
						horizontalP2 = false;
					}
				}

				if (i + y < 10) {
					if (board[x][y + i] == 0) {
						if (verticalP1) {
							vSpaceCount++;
						}
					} else {
						verticalP1 = false;
					}
				}

				if (((i * NEGATIVE) + y) > -1) {
					if (board[x][y + (i * NEGATIVE)] == 0) {
						if (verticalP2) {
							vSpaceCount++;
						}
					} else {
						verticalP2 = false;
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
				theirShipInfo[i].hAvailSpaces = 0;
				theirShipInfo[i].vAvailSpaces = 0;
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

		public Coordinate giveLastTurnOppShipCoords() {
			for (int i = checkLastTurnHit; i < 5; i++) {
				if (theirShipInfo[i].hitLastTurn.getX() != -1) {
					checkLastTurnHit = i;
					return theirShipInfo[i].hitLastTurn;
				}
			}
			return new Coordinate(-1, -1);
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