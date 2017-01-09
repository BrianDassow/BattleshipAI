
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * CaptainAmerica (continuation of CaptainObvious)
 *
 * @author Josh Loschen Spring 2014
 *
 * The attack method is that it calculates how many ways a ship can fit on each
 * tile then multiplies that number by the heat(probablity) of that ship being
 * on that tile. It does this for each ship that is still alive.
 *
 * The placement method is that it uses 8 different placement methods in the
 * beginning of the match. Every 1/17th of the matches it drops the placement
 * with the least wins
 *
 * The reason it is so many lines is that I found a concept for placement that
 * works but couldn't figure out a mathematical formula for it so I just hard
 * coded alot of switch cases.
 */
public class CaptainAmerica implements Captain {

    private Random generator;
    private Fleet myFleet;
    private int[][] boardAttacks, theirFirstShots, theirShots, myShots, myHits, firstTenShots;//board, misses are 1's and 0's if spot hasn't been fired at and 2's for hits
    private int[][] totalHits2Boat, totalHits3BoatVert, totalHits3BoatHor, subSpots, destSpots, batSpots, airSpots, their2BoatSpots;//the total shots fired on the patrol boat's,submarine's and destroyer's possible placement.
    private int attackX, attackY, numAttacks;
    public int highCrossX, highCrossY, targetX, targetY, numMatches, highHeatX, highHeatY, loopCounter, messageNum/*,secondLeastX,secondLeastY*/;
    public int patBPX, patBPY, destBPX, destBPY, subBPX, subBPY, max;//the boats placement x and y
    private int[] placementWins, attackWins;//indexs:0=patrol boat 1=destroyer 2=sub 3=battleship 4=aircraft carrier
    private ArrayList<int[]> targets;//4th digit is horizontality, 0 for no, 1 for yes, -1 for unknown. 5th digit is length
    private String opponent = "";
    private boolean airCraftAlive, battleshipAlive, subAlive, destroyerAlive, patrolAlive;
    private int gamesAgainst = 0;
    private float[][] hitHeat, crossValuesFloat;
    private square[][] board;
    float[][][] heat;
    float highHeat;
    float[][] twoBoatHeat, subHeat, destHeat, batHeat, airHeat;
    private int placement, drops;
    private boolean[] placementActive, attackActive;
    final static int UNATTACKED = 0;

    @Override
    public Coordinate makeAttack() {
        numAttacks++;
        if (targets.isEmpty()) {//no current targets
            setHighestCrossValueFloat();
            attackX = highCrossX;
            attackY = highCrossY;
        } else {                 //atleast 1 target. Work on first target in list.
            targetX = targets.get(0)[0];
            targetY = targets.get(0)[1];
            if (targets.get(0)[3] == 0) {     //ship is vertical
                setVerticalAttack();
            } else if (targets.get(0)[3] == 1) {//ship is horizontal
                setHorizontalAttack();
            } else {                   // don't know ship orientation
                if (targets.get(0)[2] == PATROL_BOAT) {
                    float up = 0f, down = 0f, right = 0f, left = 0f;
                    int x = targets.get(0)[0];
                    int y = targets.get(0)[1];
                    if (y < 9) {
                        if (boardAttacks[x][y + 1] == UNATTACKED) {
                            up = twoBoatHeat[x][y + 1];
                        }
                    } else {
                        up = -1;
                    }
                    if (y > 0) {
                        if (boardAttacks[x][y - 1] == UNATTACKED) {
                            down = twoBoatHeat[x][y - 1];
                        }
                    } else {
                        down = -1;
                    }
                    if (x > 0) {
                        if (boardAttacks[x - 1][y] == UNATTACKED) {
                            left = twoBoatHeat[x - 1][y];
                        }
                    } else {
                        left = -1;
                    }
                    if (x < 9) {
                        if (boardAttacks[x + 1][y] == UNATTACKED) {
                            right = twoBoatHeat[x + 1][y];
                        }
                    } else {
                        right = -1;
                    }
                    if (right >= left && right >= up && right >= down) {
                        attackX = x + 1;
                        attackY = y;
                    } else if (left >= up && left >= down) {
                        attackX = x - 1;
                        attackY = y;
                    } else if (up >= down) {
                        attackX = x;
                        attackY = y + 1;
                    } else {
                        attackX = x;
                        attackY = y - 1;
                    }
                } else {
                    board[targetX][targetY].getBestOrientation(targets.get(0)[4]);
                }
            }
        }
        if (attackX == -1 || attackY == -1 || attackX == 10 || attackY == 10) {// prevents crashing
            if (targets.isEmpty()) {
                computeCrossValuesFloat(false);
                setHighestCrossValueFloat();
                attackX = highCrossX;
                attackY = highCrossY;
            } else {
                board[targetX][targetY].setDirections();
                board[targetX][targetY].getBestOrientation(targets.get(0)[4]);
            }
        }
        if (boardAttacks[attackX][attackY] != 0 && loopCounter < 20) {//will go here if highHeat and leastHit are same spots
            loopCounter++;
            if (targets.isEmpty()) {
                computeCrossValuesFloat(false);
                setHighestCrossValueFloat();
                attackX = highCrossX;
                attackY = highCrossY;
            } else {
                board[targetX][targetY].setDirections();
                board[targetX][targetY].getBestOrientation(targets.get(0)[4]);
            }
        }
        loopCounter = 0;
        while (boardAttacks[attackX][attackY] != 0) {//now just looking for a shot that hasn't been done yet
            attackX = generator.nextInt(10);
            attackY = generator.nextInt(10);
        }
        highCrossX = attackX;
        highCrossY = attackY;
        myShots[attackX][attackY]++;
        boardAttacks[attackX][attackY] = 1;//update board, this assumes it is a miss, will get updated later
        return new Coordinate(attackX, attackY);
    }

    protected float compute3(int x, int y) {
        float score = 0;
        if (airCraftAlive) {
            switch (board[x][y].left) {
                case 0:
                    if (board[x][y].right == 4) {
                        score += airHeat[x][y];
                    }
                    break;
                case 1:
                    if (board[x][y].right == 4) {
                        score += (2 * airHeat[x][y]);
                    } else if (board[x][y].right == 3) {
                        score += airHeat[x][y];
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += airHeat[x][y];
                            break;
                        case 3:
                            score += (2 * airHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * airHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += airHeat[x][y];
                            break;
                        case 2:
                            score += (2 * airHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * airHeat[x][y]);
                            break;
                        case 4:
                            score += (4 * airHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += airHeat[x][y];
                            break;
                        case 1:
                            score += (2 * airHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * airHeat[x][y]);
                            break;
                        case 3:
                            score += (4 * airHeat[x][y]);
                            break;
                        case 4:
                            score += (5 * airHeat[x][y]);
                            break;
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    if (board[x][y].up == 4) {
                        score += airHeat[x][y];
                    }
                    break;
                case 1:
                    if (board[x][y].up == 4) {
                        score += (2 * airHeat[x][y]);
                    } else if (board[x][y].up == 3) {
                        score += airHeat[x][y];
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += airHeat[x][y];
                            break;
                        case 3:
                            score += (2 * airHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * airHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += airHeat[x][y];
                            break;
                        case 2:
                            score += (2 * airHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * airHeat[x][y]);
                            break;
                        case 4:
                            score += (4 * airHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += airHeat[x][y];
                            break;
                        case 1:
                            score += (2 * airHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * airHeat[x][y]);
                            break;
                        case 3:
                            score += (4 * airHeat[x][y]);
                            break;
                        case 4:
                            score += (5 * airHeat[x][y]);
                            break;
                    }
                    break;
            }
        }
        if (battleshipAlive) {
            switch (board[x][y].left) {
                case 0:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            score += batHeat[x][y];
                            break;
                        case 4:
                            score += batHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += batHeat[x][y];
                            break;
                        case 3:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * batHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += batHeat[x][y];
                            break;
                        case 2:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * batHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * batHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            score += batHeat[x][y];
                            break;
                        case 1:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * batHeat[x][y]);
                            break;
                        case 3:
                            score += (4 * batHeat[x][y]);
                            break;
                        case 4:
                            score += (4 * batHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += batHeat[x][y];
                            break;
                        case 1:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * batHeat[x][y]);
                            break;
                        case 3:
                            score += (4 * batHeat[x][y]);
                            break;
                        case 4:
                            score += (4 * batHeat[x][y]);
                            break;
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            score += batHeat[x][y];
                            break;
                        case 4:
                            score += batHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += batHeat[x][y];
                            break;
                        case 3:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * batHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += batHeat[x][y];
                            break;
                        case 2:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * batHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * batHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            score += batHeat[x][y];
                            break;
                        case 1:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * batHeat[x][y]);
                            break;
                        case 3:
                            score += (4 * batHeat[x][y]);
                            break;
                        case 4:
                            score += (4 * batHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += batHeat[x][y];
                            break;
                        case 1:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * batHeat[x][y]);
                            break;
                        case 3:
                            score += (4 * batHeat[x][y]);
                            break;
                        case 4:
                            score += (4 * batHeat[x][y]);
                            break;
                    }
                    break;
            }
        }
        if (subAlive) {
            switch (board[x][y].left) {
                case 0:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += subHeat[x][y];
                            break;
                        case 3:
                            score += subHeat[x][y];
                            break;
                        case 4:
                            score += subHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += subHeat[x][y];
                            break;
                        case 2:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;

                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += subHeat[x][y];
                            break;
                        case 3:
                            score += subHeat[x][y];
                            break;
                        case 4:
                            score += subHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += subHeat[x][y];
                            break;
                        case 2:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
            }
        }
        if (destroyerAlive) {
            switch (board[x][y].left) {
                case 0:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += destHeat[x][y];
                            break;
                        case 3:
                            score += destHeat[x][y];
                            break;
                        case 4:
                            score += destHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += destHeat[x][y];
                            break;
                        case 2:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += destHeat[x][y];
                            break;
                        case 3:
                            score += destHeat[x][y];
                            break;
                        case 4:
                            score += destHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += destHeat[x][y];
                            break;
                        case 2:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
            }
        }
        if (patrolAlive) {
            switch (board[x][y].left) {
                case 0:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += twoBoatHeat[x][y];
                            break;
                        case 2:
                            score += twoBoatHeat[x][y];
                            break;
                        case 3:
                            score += twoBoatHeat[x][y];
                            break;
                        case 4:
                            score += twoBoatHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].right) {
                        case 0:
                            score += twoBoatHeat[x][y];
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            score += twoBoatHeat[x][y];
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            score += twoBoatHeat[x][y];
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += twoBoatHeat[x][y];
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += twoBoatHeat[x][y];
                            break;
                        case 2:
                            score += twoBoatHeat[x][y];
                            break;
                        case 3:
                            score += twoBoatHeat[x][y];
                            break;
                        case 4:
                            score += twoBoatHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].up) {
                        case 0:
                            score += twoBoatHeat[x][y];
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            score += twoBoatHeat[x][y];
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            score += twoBoatHeat[x][y];
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += twoBoatHeat[x][y];
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y]);
                            break;
                    }
                    break;
            }
        }
        return score;
    }

    class square {

        int x, y;
        int left, right, up, down;

        square(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void setDirections() {
            left = right = up = down = 0;
            int i = 1;
            while (x + i < 10 && i < 5) {
                if (boardAttacks[x + i][y] == 0) {
                    right++;
                    i++;
                } else {
                    break;
                }
            }
            i = 1;
            while (x - i > -1 && i < 5) {
                if (boardAttacks[x - i][y] == 0) {
                    left++;
                    i++;
                } else {
                    break;
                }
            }
            i = 1;
            while (y - i > -1 && i < 5) {
                if (boardAttacks[x][y - i] == 0) {
                    down++;
                    i++;
                } else {
                    break;
                }
            }
            i = 1;
            while (y + i < 10 && i < 5) {
                if (boardAttacks[x][y + i] == 0) {
                    up++;
                    i++;;
                } else {
                    break;
                }
            }
        }

        public void getBestOrientation(int shipLength) {
            int hShips = Math.min(right + left + 2 - shipLength, shipLength);
            int vShips = Math.min(up + down + 2 - shipLength, shipLength);
            if (hShips > vShips) {
                if (right > left) {
                    attackX = targetX + 1;
                    attackY = targetY;
                    right = 0;
                } else if (left > right) {
                    attackX = targetX - 1;
                    attackY = targetY;
                    left = 0;
                } else if (heat[targetX + 1][targetY][targets.get(0)[2]] > heat[targetX - 1][targetY][targets.get(0)[2]]) {
                    attackX = targetX + 1;
                    attackY = targetY;
                    right = 0;
                } else {
                    attackX = targetX - 1;
                    attackY = targetY;
                    left = 0;
                }
            } else {                  //more space vertically
                if (up > down) {
                    attackX = targetX;
                    attackY = targetY + 1;
                    up = 0;
                } else if (down > up) {
                    attackX = targetX;
                    attackY = targetY - 1;
                    down = 0;
                } else if (heat[targetX][targetY + 1][targets.get(0)[2]] > heat[targetX][targetY - 1][targets.get(0)[2]]) {
                    attackX = targetX;
                    attackY = targetY + 1;
                    up = 0;
                } else {
                    attackX = targetX;
                    attackY = targetY - 1;
                    down = 0;
                }
            }
        }
    }

    protected float compute4(int x, int y) {//weighted and takes into account surrounding tiles
        float score = 0;
        if (airCraftAlive) {
            switch (board[x][y].left) {
                case 0:
                    if (board[x][y].right == 4) {
                        score += (airHeat[x][y] + airHeat[x + 1][y] + airHeat[x + 2][y] + airHeat[x + 3][y] + airHeat[x + 4][y]);
                    }
                    break;
                case 1:
                    if (board[x][y].right == 4) {
                        score += (2 * (airHeat[x][y] + airHeat[x + 1][y] + airHeat[x + 2][y] + airHeat[x + 3][y]) + airHeat[x - 1][y] + airHeat[x + 4][y]);
                    } else if (board[x][y].right == 3) {
                        score += (airHeat[x][y] + airHeat[x + 1][y] + airHeat[x + 2][y] + airHeat[x + 3][y] + airHeat[x - 1][y]);
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += (airHeat[x - 2][y] + airHeat[x - 1][y] + airHeat[x][y] + airHeat[x + 1][y] + airHeat[x + 2][y]);
                            break;
                        case 3:
                            score += (2 * (airHeat[x][y] + airHeat[x - 1][y] + airHeat[x + 1][y] + airHeat[x + 2][y]) + (airHeat[x - 2][y] + airHeat[x + 3][y]));
                            break;
                        case 4:
                            score += ((airHeat[x - 2][y] + airHeat[x + 4][y]) + 2 * (airHeat[x - 1][y] + airHeat[x + 3][y]) + 3 * (airHeat[x][y] + airHeat[x + 1][y] + airHeat[x + 2][y]));
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += (airHeat[x - 3][y] + airHeat[x - 2][y] + airHeat[x - 1][y] + airHeat[x][y] + airHeat[x + 1][y]);
                            break;//one
                        case 2:
                            score += (2 * (airHeat[x - 2][y] + airHeat[x - 1][y] + airHeat[x][y] + airHeat[x + 1][y]) + (airHeat[x - 3][y] + airHeat[x + 2][y]));
                            break;//2
                        case 3:
                            score += ((airHeat[x - 3][y] + airHeat[x + 3][y]) + 2 * (airHeat[x - 2][y] + airHeat[x + 2][y]) + 3 * (airHeat[x - 1][y] + airHeat[x][y] + airHeat[x + 1][y]));
                            break;//3
                        case 4:
                            score += ((airHeat[x - 3][y] + airHeat[x + 4][y]) + 2 * (airHeat[x - 2][y] + airHeat[x + 3][y]) + 3 * (airHeat[x - 1][y] + airHeat[x + 2][y]) + 4 * (airHeat[x][y] + airHeat[x + 1][y]));
                            break;//4
                    }
                    break;
                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += (airHeat[x][y] + airHeat[x - 1][y] + airHeat[x - 2][y] + airHeat[x - 3][y] + airHeat[x - 4][y]);
                            break;//1
                        case 1:
                            score += (2 * (airHeat[x - 3][y] + airHeat[x - 2][y] + airHeat[x - 1][y] + airHeat[x][y]) + (airHeat[x - 4][y] + airHeat[x + 1][y]));
                            break;//2
                        case 2:
                            score += ((airHeat[x + 2][y] + airHeat[x - 4][y]) + 2 * (airHeat[x - 3][y] + airHeat[x + 1][y]) + 3 * (airHeat[x - 2][y] + airHeat[x - 1][y] + airHeat[x][y]));
                            break;//3
                        case 3:
                            score += ((airHeat[x - 4][y] + airHeat[x + 3][y]) + 2 * (airHeat[x - 3][y] + airHeat[x + 2][y]) + 3 * (airHeat[x - 2][y] + airHeat[x + 1][y]) + 4 * (airHeat[x - 1][y] + airHeat[x][y]));
                            break;//4
                        case 4:
                            score += ((airHeat[x - 4][y] + airHeat[x + 4][y]) + 2 * (airHeat[x - 3][y] + airHeat[x + 3][y]) + 3 * (airHeat[x - 2][y] + airHeat[x + 2][y]) + 4 * (airHeat[x - 1][y] + airHeat[x + 1][y]) + 5 * airHeat[x][y]);
                            break;//5
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    if (board[x][y].up == 4) {
                        score += (airHeat[x][y] + airHeat[x][y + 1] + airHeat[x][y + 2] + airHeat[x][y + 3] + airHeat[x][y + 4]);
                    }
                    break;
                case 1:
                    if (board[x][y].up == 4) {
                        score += (2 * (airHeat[x][y] + airHeat[x][y + 1] + airHeat[x][y + 2] + airHeat[x][y + 3]) + (airHeat[x][y - 1] + airHeat[x][y + 4]));
                    }//2
                    else if (board[x][y].up == 3) {
                        score += (airHeat[x][y - 1] + airHeat[x][y] + airHeat[x][y + 1] + airHeat[x][y + 2] + airHeat[x][y + 3]);
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += (airHeat[x][y - 2] + airHeat[x][y - 1] + airHeat[x][y] + airHeat[x][y + 1] + airHeat[x][y + 2]);
                            break;//1
                        case 3:
                            score += (2 * (airHeat[x][y + 2] + airHeat[x][y - 1] + airHeat[x][y] + airHeat[x][y + 1]) + (airHeat[x][y - 2] + airHeat[x][y + 3]));
                            break;//2
                        case 4:
                            score += ((airHeat[x][y + 4] + airHeat[x][y - 2]) + 2 * (airHeat[x][y - 1] + airHeat[x][y + 3]) + 3 * (airHeat[x][y] + airHeat[x][y + 1] + airHeat[x][y + 2]));
                            break;//done 3
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += (airHeat[x][y - 3] + airHeat[x][y - 2] + airHeat[x][y - 1] + airHeat[x][y] + airHeat[x][y + 1]);
                            break;//1 done
                        case 2:
                            score += (2 * (airHeat[x][y - 2] + airHeat[x][y - 1] + airHeat[x][y] + airHeat[x][y + 1]) + (airHeat[x][y - 3] + airHeat[x][y + 2]));
                            break;//2 done
                        case 3:
                            score += ((airHeat[x][y - 3] + airHeat[x][y + 3]) + 2 * (airHeat[x][y - 2] + airHeat[x][y + 2]) + 3 * (airHeat[x][y - 1] + airHeat[x][y] + airHeat[x][y + 1]));
                            break;//3 done
                        case 4:
                            score += ((airHeat[x][y - 3] + airHeat[x][y + 4]) + 2 * (airHeat[x][y - 2] + airHeat[x][y + 3]) + 3 * (airHeat[x][y - 1] + airHeat[x][y + 2]) + 4 * (airHeat[x][y] + airHeat[x][y + 1]));
                            break;//4
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += (airHeat[x][y] + airHeat[x][y - 1] + airHeat[x][y - 2] + airHeat[x][y - 3] + airHeat[x][y - 4]);
                            break;//1 done
                        case 1:
                            score += (2 * (airHeat[x][y - 3] + airHeat[x][y - 2] + airHeat[x][y - 1] + airHeat[x][y]) + (airHeat[x][y - 4] + airHeat[x][y + 1]));
                            break;//2 done
                        case 2:
                            score += ((airHeat[x][y + 2] + airHeat[x][y - 4]) + 2 * (airHeat[x][y - 3] + airHeat[x][y + 1]) + 3 * (airHeat[x][y - 2] + airHeat[x][y - 1] + airHeat[x][y]));
                            break;//3 done
                        case 3:
                            score += ((airHeat[x][y - 4] + airHeat[x][y + 3]) + 2 * (airHeat[x][y - 3] + airHeat[x][y + 2]) + 3 * (airHeat[x][y - 2] + airHeat[x][y + 1]) + 4 * (airHeat[x][y - 1] + airHeat[x][y]));
                            break;//4 done
                        case 4:
                            score += ((airHeat[x][y - 4] + airHeat[x][y + 4]) + 2 * (airHeat[x][y - 3] + airHeat[x][y + 3]) + 3 * (airHeat[x][y - 2] + airHeat[x][y + 2]) + 4 * (airHeat[x][y - 1] + airHeat[x][y + 1]) + 5 * airHeat[x][y]);
                            break;// 5 done 
                    }
                    break;
            }
        }
        if (battleshipAlive) {
            switch (board[x][y].left) {
                case 0:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            score += (batHeat[x][y] + batHeat[x + 1][y] + batHeat[x + 2][y] + batHeat[x + 3][y]);
                            break;//1 done
                        case 4:
                            score += (batHeat[x][y] + batHeat[x + 1][y] + batHeat[x + 2][y] + batHeat[x + 3][y]);
                            break;//1 done
                    }
                    break;
                case 1:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += (batHeat[x - 1][y] + batHeat[x][y] + batHeat[x + 1][y] + batHeat[x + 2][y]);
                            break;//1 done
                        case 3:
                            score += (batHeat[x - 1][y] + batHeat[x + 3][y]) + 2 * (batHeat[x][y] + batHeat[x + 1][y] + batHeat[x + 2][y]);
                            break;//2 done
                        case 4:
                            score += (batHeat[x - 1][y] + batHeat[x + 3][y]) + 2 * (batHeat[x][y] + batHeat[x + 1][y] + batHeat[x + 2][y]);
                            break;//2
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += (batHeat[x - 2][y] + batHeat[x - 1][y] + batHeat[x][y] + batHeat[x + 1][y]);
                            break;//1 done
                        case 2:
                            score += ((batHeat[x - 2][y] + batHeat[x + 2][y]) + 2 * (batHeat[x - 1][y] + batHeat[x][y] + batHeat[x + 1][y]));
                            break;//2 done
                        case 3:
                            score += ((batHeat[x - 2][y] + batHeat[x + 3][y]) + 2 * (batHeat[x - 1][y] + batHeat[x + 2][y]) + 3 * (batHeat[x][y] + batHeat[x + 1][y]));
                            break;//3 done
                        case 4:
                            score += ((batHeat[x - 2][y] + batHeat[x + 3][y]) + 2 * (batHeat[x - 1][y] + batHeat[x + 2][y]) + 3 * (batHeat[x][y] + batHeat[x + 1][y]));
                            break;//3 done
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            score += (batHeat[x - 3][y] + batHeat[x - 2][y] + batHeat[x - 1][y] + batHeat[x][y]);
                            break;//1 done
                        case 1:
                            score += ((batHeat[x - 3][y] + batHeat[x + 1][y]) + 2 * (batHeat[x - 2][y] + batHeat[x - 1][y] + batHeat[x][y]));
                            break;//2 done
                        case 2:
                            score += ((batHeat[x - 3][y] + batHeat[x + 2][y]) + 2 * (batHeat[x - 2][y] + batHeat[x + 1][y]) + 3 * (batHeat[x - 1][y] + batHeat[x][y]));
                            break;//3 done 
                        case 3:
                            score += ((batHeat[x - 3][y] + batHeat[x + 3][y]) + 2 * (batHeat[x - 2][y] + batHeat[x + 2][y]) + 3 * (batHeat[x - 1][y] + batHeat[x + 1][y]) + 4 * batHeat[x][y]);
                            break;//4 done
                        case 4:
                            score += ((batHeat[x - 3][y] + batHeat[x + 3][y]) + 2 * (batHeat[x - 2][y] + batHeat[x + 2][y]) + 3 * (batHeat[x - 1][y] + batHeat[x + 1][y]) + 4 * batHeat[x][y]);
                            break;// 4 done
                    }
                    break;
                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += (batHeat[x - 3][y] + batHeat[x - 2][y] + batHeat[x - 1][y] + batHeat[x][y]);
                            break;//1 done
                        case 1:
                            score += ((batHeat[x - 3][y] + batHeat[x + 1][y]) + 2 * (batHeat[x - 2][y] + batHeat[x - 1][y] + batHeat[x][y]));
                            break;// 2  done
                        case 2:
                            score += ((batHeat[x - 3][y] + batHeat[x + 2][y]) + 2 * (batHeat[x - 2][y] + batHeat[x + 1][y]) + 3 * (batHeat[x - 1][y] + batHeat[x][y]));
                            break;//3 done
                        case 3:
                            score += ((batHeat[x - 3][y] + batHeat[x + 3][y]) + 2 * (batHeat[x - 2][y] + batHeat[x + 2][y]) + 3 * (batHeat[x - 1][y] + batHeat[x + 1][y]) + 4 * batHeat[x][y]);
                            break;//4 done
                        case 4:
                            score += ((batHeat[x - 3][y] + batHeat[x + 3][y]) + 2 * (batHeat[x - 2][y] + batHeat[x + 2][y]) + 3 * (batHeat[x - 1][y] + batHeat[x + 1][y]) + 4 * batHeat[x][y]);
                            break;//4 done
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            score += (batHeat[x][y] + batHeat[x][y + 1] + batHeat[x][y + 2] + batHeat[x][y + 3]);
                            break;//1 done
                        case 4:
                            score += (batHeat[x][y] + batHeat[x][y + 1] + batHeat[x][y + 2] + batHeat[x][y + 3]);
                            break;//1 done
                    }
                    break;
                case 1:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += (batHeat[x][y - 1] + batHeat[x][y] + batHeat[x][y + 1] + batHeat[x][y + 2]);
                            break;//1 done
                        case 3:
                            score += ((batHeat[x][y - 1] + batHeat[x][y + 3]) + 2 * (batHeat[x][y] + batHeat[x][y + 1] + batHeat[x][y + 2]));
                            break;// 2  done
                        case 4:
                            score += ((batHeat[x][y - 1] + batHeat[x][y + 3]) + 2 * (batHeat[x][y] + batHeat[x][y + 1] + batHeat[x][y + 2]));
                            break;// 2  done
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += (batHeat[x][y - 2] + batHeat[x][y - 1] + batHeat[x][y] + batHeat[x][y + 1]);
                            break;//1 done
                        case 2:
                            score += ((batHeat[x][y - 2] + batHeat[x][y + 2]) + 2 * (batHeat[x][y - 1] + batHeat[x][y] + batHeat[x][y + 1]));
                            break;// 2  done
                        case 3:
                            score += ((batHeat[x][y - 2] + batHeat[x][y + 3]) + 2 * (batHeat[x][y - 1] + batHeat[x][y + 2]) + 3 * (batHeat[x][y] + batHeat[x][y + 1]));
                            break;//3 done
                        case 4:
                            score += ((batHeat[x][y - 2] + batHeat[x][y + 3]) + 2 * (batHeat[x][y - 1] + batHeat[x][y + 2]) + 3 * (batHeat[x][y] + batHeat[x][y + 1]));
                            break;//3 done
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            score += (batHeat[x][y - 3] + batHeat[x][y - 2] + batHeat[x][y - 1] + batHeat[x][y]);
                            break;//1 done
                        case 1:
                            score += ((batHeat[x][y - 3] + batHeat[x][y + 1]) + 2 * (batHeat[x][y - 2] + batHeat[x][y - 1] + batHeat[x][y]));
                            break;//2 done
                        case 2:
                            score += ((batHeat[x][y - 3] + batHeat[x][y + 2]) + 2 * (batHeat[x][y - 2] + batHeat[x][y + 1]) + 3 * (batHeat[x][y - 1] + batHeat[x][y]));
                            break;//3 done 
                        case 3:
                            score += ((batHeat[x][y - 3] + batHeat[x][y + 3]) + 2 * (batHeat[x][y - 2] + batHeat[x][y + 2]) + 3 * (batHeat[x][y - 1] + batHeat[x][y + 1]) + 4 * batHeat[x][y]);
                            break;//4 done
                        case 4:
                            score += ((batHeat[x][y - 3] + batHeat[x][y + 3]) + 2 * (batHeat[x][y - 2] + batHeat[x][y + 2]) + 3 * (batHeat[x][y - 1] + batHeat[x][y + 1]) + 4 * batHeat[x][y]);
                            break;//4 done
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += batHeat[x][y];
                            break;
                        case 1:
                            score += (2 * batHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * batHeat[x][y]);
                            break;
                        case 3:
                            score += ((batHeat[x][y - 3] + batHeat[x][y + 3]) + 2 * (batHeat[x][y - 2] + batHeat[x][y + 2]) + 3 * (batHeat[x][y - 1] + batHeat[x][y + 1]) + 4 * batHeat[x][y]);
                            break;//4 done
                        case 4:
                            score += ((batHeat[x][y - 3] + batHeat[x][y + 3]) + 2 * (batHeat[x][y - 2] + batHeat[x][y + 2]) + 3 * (batHeat[x][y - 1] + batHeat[x][y + 1]) + 4 * batHeat[x][y]);
                            break;//4 done
                    }
                    break;
            }
        }
        if (subAlive) {
            switch (board[x][y].left) {
                case 0:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += subHeat[x][y];
                            break;
                        case 3:
                            score += subHeat[x][y];
                            break;
                        case 4:
                            score += subHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += subHeat[x][y];
                            break;
                        case 2:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;

                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += subHeat[x][y];
                            break;
                        case 3:
                            score += subHeat[x][y];
                            break;
                        case 4:
                            score += subHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += subHeat[x][y];
                            break;
                        case 2:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += subHeat[x][y];
                            break;
                        case 1:
                            score += (2 * subHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * subHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * subHeat[x][y]);
                            break;
                    }
                    break;
            }
        }
        if (destroyerAlive) {
            switch (board[x][y].left) {
                case 0:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += destHeat[x][y];
                            break;
                        case 3:
                            score += destHeat[x][y];
                            break;
                        case 4:
                            score += destHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += destHeat[x][y];
                            break;
                        case 2:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            score += destHeat[x][y];
                            break;
                        case 3:
                            score += destHeat[x][y];
                            break;
                        case 4:
                            score += destHeat[x][y];
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += destHeat[x][y];
                            break;
                        case 2:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (2 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += destHeat[x][y];
                            break;
                        case 1:
                            score += (2 * destHeat[x][y]);
                            break;
                        case 2:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 3:
                            score += (3 * destHeat[x][y]);
                            break;
                        case 4:
                            score += (3 * destHeat[x][y]);
                            break;
                    }
                    break;
            }
        }
        if (patrolAlive) {
            switch (board[x][y].left) {
                case 0:
                    switch (board[x][y].right) {
                        case 0:
                            break;
                        case 1:
                            score += (twoBoatHeat[x][y] + twoBoatHeat[x + 1][y]);
                            break;
                        case 2:
                            score += (twoBoatHeat[x][y] + twoBoatHeat[x + 1][y]);
                            break;
                        case 3:
                            score += (twoBoatHeat[x][y] + twoBoatHeat[x + 1][y]);
                            break;
                        case 4:
                            score += (twoBoatHeat[x][y] + twoBoatHeat[x + 1][y]);
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].right) {
                        case 0:
                            score += (twoBoatHeat[x - 1][y] + twoBoatHeat[x][y]);
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].right) {
                        case 0:
                            score += (twoBoatHeat[x - 1][y] + twoBoatHeat[x][y]);
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].right) {
                        case 0:
                            score += (twoBoatHeat[x - 1][y] + twoBoatHeat[x][y]);
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].right) {
                        case 0:
                            score += (twoBoatHeat[x - 1][y] + twoBoatHeat[x][y]);
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x - 1][y] + twoBoatHeat[x + 1][y]));
                            break;
                    }
                    break;
            }
            switch (board[x][y].down) {
                case 0:
                    switch (board[x][y].up) {
                        case 0:
                            break;
                        case 1:
                            score += (twoBoatHeat[x][y] + twoBoatHeat[x][y + 1]);
                            break;
                        case 2:
                            score += (twoBoatHeat[x][y] + twoBoatHeat[x][y + 1]);
                            break;
                        case 3:
                            score += (twoBoatHeat[x][y] + twoBoatHeat[x][y + 1]);
                            break;
                        case 4:
                            score += (twoBoatHeat[x][y] + twoBoatHeat[x][y + 1]);
                            break;
                    }
                    break;
                case 1:
                    switch (board[x][y].up) {
                        case 0:
                            score += (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y]);
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                    }
                    break;
                case 2:
                    switch (board[x][y].up) {
                        case 0:
                            score += (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y]);
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                    }
                    break;
                case 3:
                    switch (board[x][y].up) {
                        case 0:
                            score += (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y]);
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                    }
                    break;
                case 4:
                    switch (board[x][y].up) {
                        case 0:
                            score += (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y]);
                            break;
                        case 1:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 2:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 3:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                        case 4:
                            score += (2 * twoBoatHeat[x][y] + (twoBoatHeat[x][y - 1] + twoBoatHeat[x][y + 1]));
                            break;
                    }
                    break;
            }
        }
        return score;
    }

    private void setHorizontalAttack() {
        attackY = targetY;// if doing horizontal attack the y's will be the same no matter what
        int leftX = -1, rightX = -1;
        for (int i = 0; i < targets.get(0)[4]; i++) {
            if (targetX + i < 10) {//check for index out of bounds
                if (boardAttacks[targetX + i][targetY] == 0) {//looking in positive x direction
                    if (noMissesBetween(i, 1, targetX, targetY)) {
                        rightX = targetX + i;
                        break;//get out of for loop, stop looking once find closest spot.
                    }
                }
            }
        }
        for (int i = 0; i < targets.get(0)[4]; i++) {
            if (targetX - i > -1) {//check for index out of bounds
                if (boardAttacks[targetX - i][targetY] == 0) {//looking in positive y direction
                    if (noMissesBetween(i, 3, targetX, targetY)) {
                        leftX = targetX - i;
                        break;//get out of for loop, stop looking once find closest spot.
                    }
                }
            }
        }
        if (leftX > -1 && rightX > -1) {//both sides open
            switch (targets.get(0)[2]) {     // needs to be updated for one heat array
                case PATROL_BOAT:
                    if (twoBoatHeat[leftX][targetY] > twoBoatHeat[rightX][targetY]) {
                        attackX = leftX;
                    } else {
                        attackX = rightX;
                    }
                    break;
                case SUBMARINE:
                    if (subHeat[leftX][targetY] > subHeat[rightX][targetY]) {
                        attackX = leftX;
                    } else {
                        attackX = rightX;
                    }
                    break;
                case DESTROYER:
                    if (destHeat[leftX][targetY] > destHeat[rightX][targetY]) {
                        attackX = leftX;
                    } else {
                        attackX = rightX;
                    }
                    break;
                case BATTLESHIP:
                    if (batHeat[leftX][targetY] > batHeat[rightX][targetY]) {
                        attackX = leftX;
                    } else {
                        attackX = rightX;
                    }
                    break;
                case AIRCRAFT_CARRIER:
                    if (airHeat[leftX][targetY] > airHeat[rightX][targetY]) {
                        attackX = leftX;
                    } else {
                        attackX = rightX;
                    }
                    break;
            }
        } else if (rightX > -1) {//only right is open
            attackX = rightX;
        } else {       // only left is open
            attackX = leftX;
        }
    }

    private void setVerticalAttack() {
        attackX = targetX;// if doing vertical attack the x's will be the same no matter what
        int downY = -1, upY = -1;
        for (int i = 0; i < targets.get(0)[4]; i++) {
            if (targetY + i < 10) {//check for index out of bounds
                if (boardAttacks[targetX][targetY + i] == 0) {//looking in positive y direction
                    if (noMissesBetween(i, 0, targetX, targetY)) {
                        upY = targetY + i;
                        break;//get out of for loop, stop looking once find closest spot.
                    }
                }
            }
        }
        for (int i = 0; i < targets.get(0)[4]; i++) {
            if (targetY - i > -1) {//check for index out of bounds
                if (boardAttacks[targetX][targetY - i] == 0) {//looking in positive y direction
                    if (noMissesBetween(i, 2, targetX, targetY)) {
                        downY = targetY - i;
                        break;//get out of for loop, stop looking once find closest spot.
                    }
                }
            }
        }
        if (downY > -1 && upY > -1) {//both sides open
            switch (targets.get(0)[2]) {     // needs to be updated for one heat array
                case PATROL_BOAT:
                    if (twoBoatHeat[targetX][upY] > twoBoatHeat[targetX][downY]) {
                        attackY = upY;
                    } else {
                        attackY = downY;
                    }
                    break;
                case SUBMARINE:
                    if (subHeat[targetX][upY] > subHeat[targetX][downY]) {
                        attackY = upY;
                    } else {
                        attackY = downY;
                    }
                    break;
                case DESTROYER:
                    if (destHeat[targetX][upY] > destHeat[targetX][downY]) {
                        attackY = upY;
                    } else {
                        attackY = downY;
                    }
                    break;
                case BATTLESHIP:
                    if (batHeat[targetX][upY] > batHeat[targetX][downY]) {
                        attackY = upY;
                    } else {
                        attackY = downY;
                    }
                    break;
                case AIRCRAFT_CARRIER:
                    if (airHeat[targetX][upY] > airHeat[targetX][downY]) {
                        attackY = upY;
                    } else {
                        attackY = downY;
                    }
                    break;
            }
        } else if (downY > -1) {//only down is open
            attackY = downY;
        } else {       // only up is open
            attackY = upY;
        }
    }

    private void updateHitHeat() {
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                twoBoatHeat[x][y] = ((float) their2BoatSpots[x][y] / (float) myShots[x][y]);
                destHeat[x][y] = ((float) destSpots[x][y] / (float) myShots[x][y]);
                subHeat[x][y] = ((float) subSpots[x][y] / (float) myShots[x][y]);
                batHeat[x][y] = ((float) batSpots[x][y] / (float) myShots[x][y]);
                airHeat[x][y] = ((float) airSpots[x][y] / (float) myShots[x][y]);
            }
        }
    }

    private void computeCrossValuesFloat(boolean doingAttack1) {
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (boardAttacks[x][y] == 0) {
                    crossValuesFloat[x][y] = compute3(x, y);
                } else {
                    crossValuesFloat[x][y] = 0;
                }
            }
        }
    }

    private void computeCrossValuesSurroundingSquares() {
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (boardAttacks[x][y] == 0) {
                    crossValuesFloat[x][y] = compute4(x, y);
                } else {
                    crossValuesFloat[x][y] = 0;
                }
            }
        }
    }

    private void computeCrossValuesMiss(int missX, int missY) {
        int z = 1;
        if (!airCraftAlive) {
            while (missX + z < 10 && z <= max) {
                if (boardAttacks[missX + z][missY] == 0) {
                    crossValuesFloat[missX + z][missY] = compute4(missX + z, missY);
                } else {
                    break;//stop counting once hit something other than 1
                }
                z++;
            }
            z = 1;
            while (missX - z > -1 && z <= max) {
                if (boardAttacks[missX - z][missY] == 0) {
                    crossValuesFloat[missX - z][missY] = compute4(missX - z, missY);
                } else {
                    break;//stop counting once hit something other than 1
                }
                z++;
            }
            z = 1;
            while (missY + z < 10 && z <= max) {
                if (boardAttacks[missX][missY + z] == 0) {
                    crossValuesFloat[missX][missY + z] = compute4(missX, missY + z);
                } else {
                    break;//stop counting once hit something other than 1
                }
                z++;
            }
            z = 1;
            while (missY - z > -1 && z <= max) {
                if (boardAttacks[missX][missY - z] == 0) {
                    crossValuesFloat[missX][missY - z] = compute4(missX, missY - z);
                } else {
                    break;//stop counting once hit something other than 1
                }
                z++;
            }
        } else {
            while (missX + z < 10 && z <= max) {
                if (boardAttacks[missX + z][missY] == 0) {
                    crossValuesFloat[missX + z][missY] = compute3(missX + z, missY);
                } else {
                    break;//stop counting once hit something other than 1
                }
                z++;
            }
            z = 1;
            while (missX - z > -1 && z <= max) {
                if (boardAttacks[missX - z][missY] == 0) {
                    crossValuesFloat[missX - z][missY] = compute3(missX - z, missY);
                } else {
                    break;//stop counting once hit something other than 1
                }
                z++;
            }
            z = 1;
            while (missY + z < 10 && z <= max) {
                if (boardAttacks[missX][missY + z] == 0) {
                    crossValuesFloat[missX][missY + z] = compute3(missX, missY + z);
                } else {
                    break;//stop counting once hit something other than 1
                }
                z++;
            }
            z = 1;
            while (missY - z > -1 && z <= max) {
                if (boardAttacks[missX][missY - z] == 0) {
                    crossValuesFloat[missX][missY - z] = compute3(missX, missY - z);
                } else {
                    break;//stop counting once hit something other than 1
                }
                z++;
            }
        }
    }

    private void setHighestCrossValueFloat() {
        while (boardAttacks[highCrossX][highCrossY] != 0) {
            highCrossX = generator.nextInt(10);
            highCrossY = generator.nextInt(10);
        }
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (boardAttacks[x][y] == 0) {//haven't already attacked at these coordinates
                    if (crossValuesFloat[x][y] >= crossValuesFloat[highCrossX][highCrossY]) {//if this spot is better or equal to
                        if (crossValuesFloat[x][y] > crossValuesFloat[highCrossX][highCrossY]) {//if strictly better then update highest spot
                            highCrossX = x;
                            highCrossY = y;
                        } else {
                            if (myShots[x][y] < myShots[highCrossX][highCrossY]) {
                                highCrossX = x;
                                highCrossY = y;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean noMissesBetween(int i, int direction, int targetX, int targetY) {
        switch (direction) { // direction: 0=up 1=right 2=down 3=left
            case 0:
                for (int j = 1; j < i; j++) {
                    if (boardAttacks[targetX][targetY + j] == 1) {
                        return false;
                    }
                }
                break;
            case 1:
                for (int j = 1; j < i; j++) {
                    if (boardAttacks[targetX + j][targetY] == 1) {
                        return false;
                    }
                }
                break;
            case 2:
                for (int j = 1; j < i; j++) {
                    if (boardAttacks[targetX][targetY - j] == 1) {
                        return false;
                    }
                }
                break;
            case 3:
                for (int j = 1; j < i; j++) {
                    if (boardAttacks[targetX - j][targetY] == 1) {
                        return false;
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void resultOfAttack(int result) {
        int ship = result % 10;
        int i = 1;
        while (attackX + i < 10 && i < 5) {
            if (boardAttacks[attackX + i][attackY] == 0) {
                board[attackX + i][attackY].left = i - 1;
                i++;
            } else {
                break;
            }
        }
        i = 1;
        while (attackX - i > -1 && i < 5) {
            if (boardAttacks[attackX - i][attackY] == 0) {
                board[attackX - i][attackY].right = i - 1;
                i++;
            } else {
                break;
            }
        }
        i = 1;
        while (attackY - i > -1 && i < 5) {
            if (boardAttacks[attackX][attackY - i] == 0) {
                board[attackX][attackY - i].up = i - 1;
                i++;
            } else {
                break;
            }
        }
        i = 1;
        while (attackY + i < 10 && i < 5) {
            if (boardAttacks[attackX][attackY + i] == 0) {
                board[attackX][attackY + i].down = i - 1;
                i++;
            } else {
                break;
            }
        }
        if (result > 9 && result < 20) {//attack was a hit
            switch (ship) {
                case 0:
                    their2BoatSpots[attackX][attackY]++;
                    break;
                case 1:
                    destSpots[attackX][attackY]++;
                    break;
                case 2:
                    subSpots[attackX][attackY]++;
                    break;
                case 3:
                    batSpots[attackX][attackY]++;
                    break;
                case 4:
                    airSpots[attackX][attackY]++;
                    break;
            }
            boardAttacks[attackX][attackY] = 2;// 2 = hit, 1 = miss, 0 = unattacked
            myHits[attackX][attackY]++;
            if (targets.isEmpty()) {
                int length = getLength(ship);
                int[] newTarget = {attackX, attackY, ship, -1, length};
                targets.add(newTarget);
            } else if (!shipInTargets(ship)) {//if ship has been hit but not yet targeted
                int length = getLength(ship);
                int[] newTarget = {attackX, attackY, ship, -1, length};
                targets.add(newTarget);
            } else {//ship has been hit and is already in targets
                int index = findShipIndex(ship);
                if (targets.get(index)[3] == -1) {//if orientation is unknown set it
                    if (attackX == targets.get(index)[0]) {//attack and target have same x value
                        targets.get(index)[3] = 0;
                    } else if (attackY == targets.get(index)[1]) {//attack and target have same y value// maybe comment out if check and see if still runs fine
                        targets.get(index)[3] = 1;
                    }
                }
            }
        } else if (result > 19 && result < 29) {//attack sunk a ship
            switch (ship) {
                case PATROL_BOAT:
                    patrolAlive = false;
                    their2BoatSpots[attackX][attackY]++;
                    break;
                case DESTROYER:
                    destroyerAlive = false;
                    destSpots[attackX][attackY]++;
                    break;
                case SUBMARINE:
                    subAlive = false;
                    subSpots[attackX][attackY]++;
                    break;
                case BATTLESHIP:
                    battleshipAlive = false;
                    batSpots[attackX][attackY]++;
                    break;
                case AIRCRAFT_CARRIER:
                    airCraftAlive = false;
                    airSpots[attackX][attackY]++;
                    break;
            }
            if (airCraftAlive) {
                max = 5;
            } else if (battleshipAlive) {
                max = 4;
            } else if (subAlive || destroyerAlive) {
                max = 3;
            } else {
                max = 2;
            }
            if (airCraftAlive) {
                computeCrossValuesFloat(false);
            } else {
                computeCrossValuesSurroundingSquares();
            }
            targets.remove(findShipIndex(ship));
        } else {//attack was a miss.If have hit spots on either side of target spot 
            computeCrossValuesMiss(attackX, attackY); //and still haven't hit the ship then we know the direction of the ship.
            if (!targets.isEmpty()) {//if have a target check to get horizontality
                if (missesOnBothSides()) {
                    if (targets.get(0)[3] == -1) {
                        targets.get(0)[3] = 0;
                    }
                } else if (missesAboveAndBelow()) {
                    if (targets.get(0)[3] == -1) {
                        targets.get(0)[3] = 1;
                    }
                }
            }
        }
    }

    private boolean missesAboveAndBelow() {//used to compute crossValues in compute method
        int x = targets.get(0)[0];
        int y = targets.get(0)[1];
        if (y == 0) {
            if (boardAttacks[x][y + 1] == 1) {
                return true;
            }
        } else if (y > 0 && y < 9) {
            if (boardAttacks[x][y + 1] == 1 && boardAttacks[x][y - 1] == 1) {
                return true;
            }
        } else if (y == 9) {
            if (boardAttacks[x][y - 1] == 1) {
                return true;
            }
        }
        return false;
    }

    private boolean missesOnBothSides() {//used to compute crossValues in compute method
        int x = targets.get(0)[0];
        int y = targets.get(0)[1];
        if (x == 0) {
            if (boardAttacks[x + 1][y] == 1) {
                return true;
            }
        } else if (x > 0 && x < 9) {
            if (boardAttacks[x + 1][y] == 1 && boardAttacks[x - 1][y] == 1) {
                return true;
            }
        } else if (x == 9) {
            if (boardAttacks[x - 1][y] == 1) {
                return true;
            }
        }
        return false;
    }

    private int getLength(int ship) {
        switch (ship) {
            case 0:
                return 2;
            case 1:
                return 3;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
        }
        return 5;
    }

    private int findShipIndex(int ship) {//linear search to find index of a ship in target list
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i)[2] == ship) {
                return i;
            }
        }
        return -1;
    }

    private boolean shipInTargets(int ship) {//checks to see if the ship that was hit has already been targeted
        boolean isIn = false;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i)[2] == ship) {
                isIn = true;
            }
        }
        return isIn;
    }

    @Override
    public Fleet getFleet() {
        return myFleet;
    }

    @Override
    public void opponentAttack(Coordinate coord) {
        this.theirShots[coord.getX()][coord.getY()]++;
        if (numAttacks < 7) {
            theirFirstShots[coord.getX()][coord.getY()]++;
        }
    }

    @Override
    public void resultOfGame(int result) {
        if (result == 1) {
            switch (placement) {
                case 0:
                    placementWins[0]++;
                    break;
                case 1:
                    placementWins[1]++;
                    break;
                case 2:
                    placementWins[2]++;
                    break;
                case 3:
                    placementWins[3]++;
                    break;
                case 4:
                    placementWins[4]++;
                    break;
                case 5:
                    placementWins[5]++;
                    break;
                case 6:
                    placementWins[6]++;
                    break;
                case 7:
                    placementWins[7]++;
                    break;
                case 8:
                    placementWins[8]++;
                    break;
                case 9:
                    placementWins[9]++;
                    break;
            }
        }
    }

    public void calculate2BoatSpotVert() {//placement method
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (y < 9) {
                    totalHits2Boat[x][y] = theirFirstShots[x][y] + theirFirstShots[x][y + 1] + theirShots[x][y] + theirShots[x][y + 1];
                } else {
                    totalHits2Boat[x][y] = Integer.MAX_VALUE;
                }
            }
        }
    }

    public void calculate2BoatSpotHor() {//placement method
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (x < 9) {
                    totalHits2Boat[x][y] = theirFirstShots[x][y] + theirFirstShots[x + 1][y] + theirShots[x][y] + theirShots[x + 1][y];
                } else {
                    totalHits2Boat[x][y] = Integer.MAX_VALUE;
                }
            }
        }
    }
    //this one checks vertically

    public void calculateDestroyerSpotVert() {//placement method 
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (y < 8) {
                    totalHits3BoatVert[x][y] = theirFirstShots[x][y] + theirFirstShots[x][y + 1] + theirFirstShots[x][y + 2] + theirShots[x][y] + theirShots[x][y + 1] + theirShots[x][y + 2];
                } else {
                    totalHits3BoatVert[x][y] = Integer.MAX_VALUE;
                }
                if (x == patBPX && (y == patBPY || y == patBPY - 1 || y == patBPY - 2 || y == patBPY + 1)) {
                    totalHits3BoatVert[x][y] = Integer.MAX_VALUE;//number so big that it definately won't be chosen.
                }
            }
        }
    }

    public void calculateDestroyerSpotHor() {//placement method 
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (x < 8) {
                    totalHits3BoatVert[x][y] = theirFirstShots[x][y] + theirFirstShots[x + 1][y] + theirFirstShots[x + 2][y] + theirShots[x][y] + theirShots[x + 1][y] + theirShots[x + 2][y];
                } else {
                    totalHits3BoatVert[x][y] = Integer.MAX_VALUE;
                }
                if ((x == patBPX - 1 || x == patBPX || x == patBPX + 1 || x == patBPX - 2) && (y == patBPY)) {//if(x==patBPX && (y==patBPY ||y==patBPY-1||y==patBPY-2||y==patBPY+1)){
                    totalHits3BoatVert[x][y] = Integer.MAX_VALUE;//number so big that it definately won't be chosen.
                }
            }
        }
    }

    public void calculateSubSpotHor() {//placement method 
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (x < 8) {
                    totalHits3BoatHor[x][y] = theirFirstShots[x][y] + theirFirstShots[x + 1][y] + theirFirstShots[x + 2][y] + theirShots[x][y] + theirShots[x + 1][y] + theirShots[x + 2][y];
                } else {
                    totalHits3BoatHor[x][y] = Integer.MAX_VALUE;
                }
                if (((x == patBPX) || (x == patBPX - 1) || (x == patBPX - 2)) && ((y == patBPY) || (y == patBPY + 1))) {//make sure won't run into patrol boat
                    totalHits3BoatHor[x][y] = Integer.MAX_VALUE;//num so big this spot will never get chosen
                }
                if (((x == destBPX) || x == (destBPX - 1) || (x == destBPX - 2)) && ((y == destBPY) || (y == destBPY + 1) || (y == destBPY + 2))) {//won't run into destroyer
                    totalHits3BoatHor[x][y] = Integer.MAX_VALUE;
                }
            }
        }
    }

    public void calculateSubSpotVert() {//placement method 
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (y < 8) {
                    totalHits3BoatHor[x][y] = theirFirstShots[x][y] + theirFirstShots[x][y + 1] + theirFirstShots[x][y + 2] + theirShots[x][y] + theirShots[x][y + 1] + theirShots[x][y + 2];
                } else {
                    totalHits3BoatHor[x][y] = Integer.MAX_VALUE;
                }
                if (((y == patBPY) || (y == patBPY - 1) || (y == patBPY - 2)) && ((x == patBPX) || (x == patBPX + 1))) {//make sure won't run into patrol boat
                    totalHits3BoatHor[x][y] = Integer.MAX_VALUE;//num so big this spot will never get chosen
                }
                if (((y == destBPY) || y == (destBPY - 1) || (y == destBPY - 2)) && ((x == destBPX) || (x == destBPX + 1) || (x == destBPX + 2))) {//won't run into destroyer
                    totalHits3BoatHor[x][y] = Integer.MAX_VALUE;
                }
            }
        }
    }

    public void findBest2BoatSpot() {//placement method
        int bestX = 0;
        int bestY = 0;
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (totalHits2Boat[x][y] < totalHits2Boat[bestX][bestY]) {
                    bestX = x;
                    bestY = y;
                }
            }
        }
        patBPX = bestX;
        patBPY = bestY;
    }

    public void findBestDestroyerSpot() {//placement method, destroyer is placed vertical
        int bestX = 7;
        int bestY = 7;
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (totalHits3BoatVert[x][y] < totalHits3BoatVert[bestX][bestY]) {
                    bestX = x;
                    bestY = y;
                }
            }
        }
        destBPX = bestX;
        destBPY = bestY;
    }

    public void findBestSubSpot() {//placement method, sub is placed vertical
        int bestX = 5;
        int bestY = 5;
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (totalHits3BoatHor[x][y] < totalHits3BoatHor[bestX][bestY]) {
                    bestX = x;
                    bestY = y;
                }
            }
        }
        subBPX = bestX;
        subBPY = bestY;
    }

    public int leastHitColumn() {
        int[] col = {0, 0, 0, 0, 0, 0, 0, 0};
        int bestCol = 0;
        for (int x = 0; x < 8; x++) {
            col[x] = theirShots[x][0] * 2 + theirShots[x][1] * 2 + theirShots[x][2] * 2 + theirShots[x][3] + theirShots[x][4] + theirShots[x][5] + theirShots[x][6] + theirShots[x][7] * 2 + theirShots[x][8] * 2 + theirShots[x][9] * 2;
            col[x] += (theirFirstShots[x][0] * 2 + theirFirstShots[x][1] * 2 + theirFirstShots[x][2] * 2 + theirFirstShots[x][3] + theirFirstShots[x][4] + theirFirstShots[x][5] + theirFirstShots[x][6] + theirFirstShots[x][7] * 2 + theirFirstShots[x][8] * 2 + theirFirstShots[x][9] * 2);
            col[x] += (theirShots[x + 2][0] * 2 + theirShots[x + 2][1] * 2 + theirShots[x + 2][2] * 2 + theirShots[x + 2][3] + theirShots[x + 2][4] + theirShots[x + 2][5] + theirShots[x + 2][6] + theirShots[x + 2][7] * 2 + theirShots[x + 2][8] * 2 + theirShots[x + 2][9] * 2);
            col[x] += (theirFirstShots[x + 2][0] * 2 + theirFirstShots[x + 2][1] * 2 + theirFirstShots[x + 2][2] * 2 + theirFirstShots[x + 2][3] + theirFirstShots[x + 2][4] + theirFirstShots[x + 2][5] + theirFirstShots[x + 2][6] + theirFirstShots[x + 2][7] * 2 + theirFirstShots[x + 2][8] * 2 + theirFirstShots[x + 2][9] * 2);
        }
        for (int i = 1; i < 8; i++) {
            if (col[i] < col[bestCol]) {
                bestCol = i;
            }
        }
        return bestCol;
    }

    public int leastHitRow() {
        int[] row = {0, 0, 0, 0, 0, 0, 0, 0};
        int bestRow = 0;
        for (int y = 0; y < 8; y++) {
            row[y] = theirShots[0][y] * 2 + theirShots[1][y] * 2 + theirShots[2][y] * 2 + theirShots[3][y] + theirShots[4][y] + theirShots[5][y] + theirShots[6][y] + theirShots[7][y] * 2 + theirShots[8][y] * 2 + theirShots[9][y] * 2;
            row[y] += (theirFirstShots[0][y] * 2 + theirFirstShots[1][y] * 2 + theirFirstShots[2][y] * 2 + theirFirstShots[3][y] + theirFirstShots[4][y] + theirFirstShots[5][y] + theirFirstShots[6][y] + theirFirstShots[7][y] * 2 + theirFirstShots[8][y] * 2 + theirFirstShots[9][y] * 2);
            row[y] += (theirShots[0][y + 2] * 2 + theirShots[1][y + 2] * 2 + theirShots[2][y + 2] * 2 + theirShots[3][y + 2] + theirShots[4][y + 2] + theirShots[5][y + 2] + theirShots[6][y + 2] + theirShots[7][y + 2] * 2 + theirShots[8][y + 2] * 2 + theirShots[9][y + 2] * 2);
            row[y] += (theirFirstShots[0][y + 2] * 2 + theirFirstShots[1][y + 2] * 2 + theirFirstShots[2][y + 2] * 2 + theirFirstShots[3][y + 2] + theirFirstShots[4][y + 2] + theirFirstShots[5][y + 2] + theirFirstShots[6][y + 2] + theirFirstShots[7][y + 2] * 2 + theirFirstShots[8][y + 2] * 2 + theirFirstShots[9][y + 2] * 2);
        }
        for (int i = 1; i < 8; i++) {
            if (row[i] < row[bestRow]) {
                bestRow = i;
            }
        }
        return bestRow;
    }

    public void HEATMF() {
        calculate2BoatSpotVert();
        findBest2BoatSpot();
        calculateDestroyerSpotVert();
        findBestDestroyerSpot();
        calculateSubSpotHor();
        findBestSubSpot();
        while (!myFleet.placeShip(patBPX, patBPY, VERTICAL, PATROL_BOAT)) {
        }
        if (patBPX == destBPX && patBPY == destBPY) {
            destBPX = (destBPX + 5) % 10;
            destBPY = (destBPY + 5) % 10;
        }
        while (!myFleet.placeShip(destBPX, destBPY, VERTICAL, DESTROYER)) {
        }
        if (gamesAgainst > 2) {
            while (!myFleet.placeShip(subBPX, subBPY, HORIZONTAL, SUBMARINE)) {
            }
        } else {
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {
            }
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), BATTLESHIP)) {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), AIRCRAFT_CARRIER)) {
        }
    }

    public void HEATMF2() {
        calculate2BoatSpotHor();
        findBest2BoatSpot();
        calculateDestroyerSpotHor();
        findBestDestroyerSpot();
        calculateSubSpotVert();
        findBestSubSpot();
        while (!myFleet.placeShip(patBPX, patBPY, HORIZONTAL, PATROL_BOAT)) {
        }
        if (patBPX == destBPX && patBPY == destBPY) {
            destBPX = (destBPX + 5) % 10;
            destBPY = (destBPY + 5) % 10;
        }
        while (!myFleet.placeShip(destBPX, destBPY, HORIZONTAL, DESTROYER)) {
        }
        if (gamesAgainst > 2) {
            while (!myFleet.placeShip(subBPX, subBPY, VERTICAL, SUBMARINE)) {
            }
        } else {
            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {
            }
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), BATTLESHIP)) {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), AIRCRAFT_CARRIER)) {
        }
    }

    public void setShipsVertical() {
        switch (generator.nextInt(2)) {
            case 0:
                calculate2BoatSpotVert();
                findBest2BoatSpot();
                calculateDestroyerSpotVert();
                findBestDestroyerSpot();
                while (!myFleet.placeShip(patBPX, patBPY, VERTICAL, PATROL_BOAT)) {
                }
                while (!myFleet.placeShip(destBPX, destBPY, VERTICAL, DESTROYER)) {
                }
                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), VERTICAL, SUBMARINE)) {
                }
                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), VERTICAL, BATTLESHIP)) {
                }
                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), VERTICAL, AIRCRAFT_CARRIER)) {
                }
                break;
            case 1:
                vertSet(leastHitColumn());
                break;
        }
    }

    public void setShipsHorizontal() {
        switch (generator.nextInt(2)) {
            case 0:
                calculate2BoatSpotHor();
                findBest2BoatSpot();
                calculateDestroyerSpotHor();
                findBestDestroyerSpot();
                while (!myFleet.placeShip(patBPX, patBPY, HORIZONTAL, PATROL_BOAT)) {
                }
                while (!myFleet.placeShip(destBPX, destBPY, HORIZONTAL, DESTROYER)) {
                }
                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), HORIZONTAL, SUBMARINE)) {
                }
                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), HORIZONTAL, BATTLESHIP)) {
                }
                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), HORIZONTAL, AIRCRAFT_CARRIER)) {
                }
                break;
            case 1:
                horSet(leastHitRow());
        }
    }

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) {
        if (this.opponent == opponent) {//facing the same opponent
            gamesAgainst++;
            if (gamesAgainst % 5 == 0) {
                updateHitHeat();
            }
            if (gamesAgainst % 10000 == 0) {
                for (int x = 0; x < 10; x++) {
                    for (int y = 0; y < 10; y++) {
                        theirShots[x][y] = theirFirstShots[x][y] = 0;
                        their2BoatSpots[x][y] = myHits[x][y] = airSpots[x][y] = 1;
                        subSpots[x][y] = destSpots[x][y] = batSpots[x][y] = 1;
                        myHits[x][y] = 1;
                        myShots[x][y] = 2;
                    }
                }
            }
        } else {//                        facing a new opponent
            max = 5;
            heat = new float[10][10][5];
            board = new square[10][10];
            this.numMatches = numMatches;
            theirShots = new int[10][10];
            subSpots = new int[10][10];
            destSpots = new int[10][10];
            batSpots = new int[10][10];
            airSpots = new int[10][10];
            their2BoatSpots = new int[10][10];
            theirFirstShots = new int[10][10];
            firstTenShots = new int[10][10];
            hitHeat = new float[10][10];
            twoBoatHeat = new float[10][10];
            subHeat = new float[10][10];
            destHeat = new float[10][10];
            batHeat = new float[10][10];
            airHeat = new float[10][10];
            myShots = new int[10][10];
            myHits = new int[10][10];
            totalHits2Boat = new int[10][10];
            totalHits3BoatVert = new int[10][10];
            totalHits3BoatHor = new int[10][10];
            placementActive = new boolean[9];
            attackActive = new boolean[2];
            placementWins = new int[9];
            attackWins = new int[2];
            drops = attackWins[0] = attackWins[1] = 0;
            for (int i = 0; i < 9; i++) {
                placementActive[i] = true;
                placementWins[i] = 0;
            }
            attackActive[0] = attackActive[1] = true;
            for (int x = 0; x < 10; x++) {
                for (int y = 0; y < 10; y++) {
                    subSpots[x][y] = destSpots[x][y] = batSpots[x][y] = airSpots[x][y] = their2BoatSpots[x][y] = firstTenShots[x][y] = theirShots[x][y] = myHits[x][y] = myShots[x][y] = 0;
                    twoBoatHeat[x][y] = subHeat[x][y] = destHeat[x][y] = batHeat[x][y] = airHeat[x][y] = hitHeat[x][y] = 0.2f;
                    board[x][y] = new square(x, y);
                    for (int ship = 0; ship < 5; ship++) {
                        heat[x][y][ship] = 0.2f;
                    }
                }
            }
            this.opponent = opponent;
            gamesAgainst = 0;
        }
        //doingRandom = false;
        airCraftAlive = battleshipAlive = subAlive = destroyerAlive = patrolAlive = true;
        generator = new Random();
        myFleet = new Fleet();
        boardAttacks = new int[10][10];
        crossValuesFloat = new float[10][10];
        numAttacks = 0;
        highCrossX = highCrossY = generator.nextInt(10);
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                boardAttacks[x][y] = 0;
                crossValuesFloat[x][y] = 0;
                board[x][y].left = Math.min(4, x);
                board[x][y].right = Math.min(4, 9 - x);
                board[x][y].down = Math.min(4, y);
                board[x][y].up = Math.min(4, 9 - y);
            }
        }
        targets = new ArrayList<int[]>();
        int check = numMatches / 17;//adjusts how quickly losing placement patterns are dropped
        if (gamesAgainst > 2 && gamesAgainst % check == 0) {
            drops++;
            dropLeastWinsPlacement();
        }
        placement = generator.nextInt(9);
        if (gamesAgainst > 10) {//start first 10 games with ships in corners
            while (!placementActive[placement]) {
                placement = generator.nextInt(9);
            }
            switch (placement) {
                case 0:
                    setShipsVertical();
                    break;//places on spots with low heat vertically
                case 1:
                    setShipsHorizontal();
                    break;//places on low heat spots horizontally
                case 2:
                    if (gamesAgainst % 2 == 0) {
                        HEATMF();
                    } else {
                        HEATMF2();
                    }
                    break;//only focus is ships on low heat
                case 3:
                    setRandom(true);
                    break;//true allows ships to touch
                case 4:
                    setRandom(false);
                    break;// don't allow ships to touch
                case 5:
                    fillCorner();
                    break;//all 4 ships directed at 2 boat in corner
                case 6:
                    ChromosomePlacement();
                    break;
                case 7:
                    ChromosomeVert();
                    break;
                case 8:
                    ChromosomeHor();
                    break;
                case 9:
                    CornerPlacement();
                    break;//ships in corners,taken from Kahn
            }
        } else {
            CornerPlacement();
        }
        computeCrossValuesFloat(false);
    }

    private void ChromosomeVert() {
        int mod = generator.nextInt(8);
        switch (generator.nextInt(3)) {
            case 0:
                vertSet(mod);
                break;
            case 1:
                vertSet(mod);
                break;
            case 2:
                int mod4 = generator.nextInt(3);
                int mod5 = generator.nextInt(2);
                switch (gamesAgainst % 4) {
                    case 0:
                        while (!myFleet.placeShip(0, 0 + mod4, VERTICAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(2, 0 + mod4 + mod5, HORIZONTAL, BATTLESHIP)) {/*System.out.println("41"*/

                        }
                        while (!myFleet.placeShip(0, 3 + mod4, VERTICAL, AIRCRAFT_CARRIER)) {/*System.out.println("41"*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(8) + 2, generator.nextInt(10), VERTICAL, SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(8) + 2, generator.nextInt(10), VERTICAL, DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 1:
                        while (!myFleet.placeShip(0, 6 + mod4, VERTICAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(2, 6 + mod4 + mod5, HORIZONTAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0, 0 + mod4, VERTICAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(8) + 2, generator.nextInt(10), VERTICAL, SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(8) + 2, generator.nextInt(10), VERTICAL, DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 2:
                        while (!myFleet.placeShip(9, 6 + mod4, VERTICAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(4, 6 + mod4 + mod5, HORIZONTAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(9, 0 + mod4, VERTICAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(8), generator.nextInt(10), VERTICAL, SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(8), generator.nextInt(10), VERTICAL, DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 3:
                        while (!myFleet.placeShip(9, 0 + mod4, VERTICAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(4, 0 + mod4 + mod5, HORIZONTAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(9, 3 + mod4, VERTICAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(8), generator.nextInt(10), VERTICAL, SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(8), generator.nextInt(10), VERTICAL, DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                }
                break;
        }
    }

    private void dropLeastWinsPlacement() {
        if (9 - drops > 0) {
            int index = 0;
            while (!placementActive[index]) {
                index++;
            }
            for (int i = 0; i < 9; i++) {
                if (placementActive[i]) {
                    if (placementWins[i] < placementWins[index]) {
                        index = i;
                    }
                }
            }
            placementActive[index] = false;
        }
    }

    private void ChromosomeHor() {
        int mod = generator.nextInt(8);
        switch (generator.nextInt(3)) {
            case 0:
                horSet(mod);
                break;
            case 1:
                horSet(mod);
                break;
            case 2:
                int mod2 = generator.nextInt(3);
                int mod3 = generator.nextInt(2);
                switch (generator.nextInt(4)) {
                    case 0:
                        while (!myFleet.placeShip(0 + mod2, 0, HORIZONTAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0 + mod2 + mod3, 2, VERTICAL, BATTLESHIP)) {/*System.out.println("41"*/

                        }
                        while (!myFleet.placeShip(3 + mod2, 0, HORIZONTAL, AIRCRAFT_CARRIER)) {/*System.out.println("41"*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(8) + 2, HORIZONTAL, SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(8) + 2, HORIZONTAL, DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 1:
                        while (!myFleet.placeShip(6 + mod2, 0, HORIZONTAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(6 + mod2 + mod3, 2, VERTICAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0 + mod2, 0, HORIZONTAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(8), HORIZONTAL, SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(8), HORIZONTAL, DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 2:
                        while (!myFleet.placeShip(6 + mod2, 9, HORIZONTAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(6 + mod2 + mod3, 4, VERTICAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0 + mod2, 9, HORIZONTAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(8), HORIZONTAL, SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(8), HORIZONTAL, DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 3:
                        while (!myFleet.placeShip(0 + mod2, 9, HORIZONTAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0 + mod2 + mod3, 4, VERTICAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(3 + mod2, 9, HORIZONTAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(8) + 2, HORIZONTAL, SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(8) + 2, HORIZONTAL, DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                }
                break;
        }
    }

    private void ChromosomePlacement() {
        int mod = generator.nextInt(8);
        switch (generator.nextInt(11)) {
            case 0:
                vertSet(mod);
                break;
            case 1:
                horSet(mod);
                break;
            case 2:
                vertSet(mod);
                break;
            case 3:
                horSet(mod);
                break;
            case 4:
                horSet(leastHitRow());
                break;
            case 5:
                horSet(leastHitRow());
                break;
            case 6:
                vertSet(leastHitColumn());
                break;
            case 7:
                vertSet(leastHitColumn());
                break;
            case 8:
                int mod2 = generator.nextInt(3);
                int mod3 = generator.nextInt(2);
                switch (gamesAgainst % 4) {
                    case 0:
                        while (!myFleet.placeShip(0 + mod2, 0, HORIZONTAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0 + mod2 + mod3, 2, VERTICAL, BATTLESHIP)) {/*System.out.println("41"*/

                        }
                        while (!myFleet.placeShip(3 + mod2, 0, HORIZONTAL, AIRCRAFT_CARRIER)) {/*System.out.println("41"*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 1:
                        while (!myFleet.placeShip(6 + mod2, 0, HORIZONTAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(6 + mod2 + mod3, 2, VERTICAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0 + mod2, 0, HORIZONTAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 2:
                        while (!myFleet.placeShip(6 + mod2, 9, HORIZONTAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(6 + mod2 + mod3, 4, VERTICAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0 + mod2, 9, HORIZONTAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 3:
                        while (!myFleet.placeShip(0 + mod2, 9, HORIZONTAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0 + mod2 + mod3, 4, VERTICAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(3 + mod2, 9, HORIZONTAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                }
                break;
            case 9:
                int mod4 = generator.nextInt(3);
                int mod5 = generator.nextInt(2);
                switch (gamesAgainst % 4) {
                    case 0:
                        while (!myFleet.placeShip(0, 0 + mod4, VERTICAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(2, 0 + mod4 + mod5, HORIZONTAL, BATTLESHIP)) {/*System.out.println("41"*/

                        }
                        while (!myFleet.placeShip(0, 3 + mod4, VERTICAL, AIRCRAFT_CARRIER)) {/*System.out.println("41"*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 1:
                        while (!myFleet.placeShip(0, 6 + mod4, VERTICAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(2, 6 + mod4 + mod5, HORIZONTAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(0, 0 + mod4, VERTICAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 2:
                        while (!myFleet.placeShip(9, 6 + mod4, VERTICAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(4, 6 + mod4 + mod5, HORIZONTAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(9, 0 + mod4, VERTICAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                    case 3:
                        while (!myFleet.placeShip(9, 0 + mod4, VERTICAL, PATROL_BOAT)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(4, 0 + mod4 + mod5, HORIZONTAL, BATTLESHIP)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(9, 3 + mod4, VERTICAL, AIRCRAFT_CARRIER)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) {/*System.out.println("41")*/

                        }
                        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                        }
                        break;
                }
                break;
            case 10:
                int mod6 = generator.nextInt(2);
                switch (gamesAgainst % 8) {
                    case 0:
                        while (!myFleet.placeShip(8, 0 + mod6, VERTICAL, PATROL_BOAT)) {
                            System.out.println("1");
                        }
                        while (!myFleet.placeShip(9, 2 + mod6, VERTICAL, BATTLESHIP)) {
                            System.out.println("2");
                        }
                        while (!myFleet.placeShip(7, 2 + mod6, VERTICAL, AIRCRAFT_CARRIER)) {
                            System.out.println("3");
                        }
                        while (!myFleet.placeShip(4 + mod6, 0, HORIZONTAL, SUBMARINE)) {
                            System.out.println("4");
                        }
                        if (mod6 == 1) {
                            while (!myFleet.placeShip(4, 2, HORIZONTAL, DESTROYER)) {
                                System.out.println("5");
                            }
                        } else {
                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("6");*/

                            }
                        }
                        break;
                    case 1:
                        while (!myFleet.placeShip(8 - mod6, 1, HORIZONTAL, PATROL_BOAT)) {
                            System.out.println("7");
                        }
                        while (!myFleet.placeShip(4 - mod6, 2, HORIZONTAL, BATTLESHIP)) {
                            System.out.println("8");
                        }
                        while (!myFleet.placeShip(3 - mod6, 0, HORIZONTAL, AIRCRAFT_CARRIER)) {
                            System.out.println("9");
                        }
                        while (!myFleet.placeShip(9, 3 - mod6, VERTICAL, SUBMARINE)) {
                            System.out.println("10");
                        }
                        if (mod6 == 1) {
                            while (!myFleet.placeShip(7, 3, VERTICAL, DESTROYER)) {
                                System.out.println("11");
                            }
                        } else {
                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                            }
                        }
                        break;
                    case 2:
                        while (!myFleet.placeShip(8, 7 + mod6, VERTICAL, PATROL_BOAT)) {
                            System.out.println("12");
                        }
                        while (!myFleet.placeShip(7, 3 + mod6, VERTICAL, BATTLESHIP)) {
                            System.out.println("13");
                        }
                        while (!myFleet.placeShip(9, 2 + mod6, VERTICAL, AIRCRAFT_CARRIER)) {
                            System.out.println("14");
                        }
                        while (!myFleet.placeShip(4 + mod6, 9, HORIZONTAL, SUBMARINE)) {
                            System.out.println("15");
                        }
                        if (mod6 == 1) {
                            while (!myFleet.placeShip(4, 7, HORIZONTAL, DESTROYER)) {
                                System.out.println("16");
                            }
                        } else {
                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                            }
                        }
                        break;
                    case 3:
                        while (!myFleet.placeShip(7 + mod6, 8, HORIZONTAL, PATROL_BOAT)) {
                            System.out.println("17");
                        }
                        while (!myFleet.placeShip(3 + mod6, 9, HORIZONTAL, BATTLESHIP)) {
                            System.out.println("18");
                        }
                        while (!myFleet.placeShip(2 + mod6, 7, HORIZONTAL, AIRCRAFT_CARRIER)) {
                            System.out.println("19");
                        }
                        while (!myFleet.placeShip(9, 4 + mod6, VERTICAL, SUBMARINE)) {
                            System.out.println("20");
                        }
                        if (mod6 == 1) {
                            while (!myFleet.placeShip(7, 4, VERTICAL, DESTROYER)) {
                                System.out.println("21");
                            }
                        } else {
                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                            }
                        }
                        break;
                    case 4:
                        while (!myFleet.placeShip(1, 7 + mod6, VERTICAL, PATROL_BOAT)) {
                            System.out.println("22");
                        }
                        while (!myFleet.placeShip(0, 3 + mod6, VERTICAL, BATTLESHIP)) {
                            System.out.println("23");
                        }
                        while (!myFleet.placeShip(2, 2 + mod6, VERTICAL, AIRCRAFT_CARRIER)) {
                            System.out.println("24");
                        }
                        while (!myFleet.placeShip(3 - mod6, 9, HORIZONTAL, SUBMARINE)) {
                            System.out.println("25");
                        }
                        if (mod6 == 1) {
                            while (!myFleet.placeShip(3, 7, HORIZONTAL, DESTROYER)) {
                                System.out.println("27");
                            }
                        } else {
                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                            }
                        }
                        break;
                    case 5:
                        while (!myFleet.placeShip(0 + mod6, 8, HORIZONTAL, PATROL_BOAT)) {
                            System.out.println("28");
                        }
                        while (!myFleet.placeShip(2 + mod6, 7, HORIZONTAL, BATTLESHIP)) {
                            System.out.println("29");
                        }
                        while (!myFleet.placeShip(2 + mod6, 9, HORIZONTAL, AIRCRAFT_CARRIER)) {
                            System.out.println("30");
                        }
                        while (!myFleet.placeShip(0, 4 + mod6, VERTICAL, SUBMARINE)) {
                            System.out.println("31");
                        }
                        if (mod6 == 1) {
                            while (!myFleet.placeShip(2, 4, VERTICAL, DESTROYER)) {
                                System.out.println("32");
                            }
                        } else {
                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                            }
                        }
                        break;
                    case 6:
                        while (!myFleet.placeShip(1, 0 + mod6, VERTICAL, PATROL_BOAT)) {
                            System.out.println("33");
                        }
                        while (!myFleet.placeShip(0, 2 + mod6, VERTICAL, BATTLESHIP)) {
                            System.out.println("34");
                        }
                        while (!myFleet.placeShip(2, 2 + mod6, VERTICAL, AIRCRAFT_CARRIER)) {
                            System.out.println("35");
                        }
                        while (!myFleet.placeShip(3 - mod6, 0, HORIZONTAL, SUBMARINE)) {
                            System.out.println("36");
                        }
                        if (mod6 == 1) {
                            while (!myFleet.placeShip(3, 2, HORIZONTAL, DESTROYER)) {
                                System.out.println("37");
                            }
                        } else {
                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                            }
                        }
                        break;
                    case 7:
                        while (!myFleet.placeShip(0 + mod6, 1, HORIZONTAL, PATROL_BOAT)) {
                            System.out.println("38");
                        }
                        while (!myFleet.placeShip(2 + mod6, 0, HORIZONTAL, BATTLESHIP)) {
                            System.out.println("39");
                        }
                        while (!myFleet.placeShip(2 + mod6, 2, HORIZONTAL, AIRCRAFT_CARRIER)) {
                            System.out.println("40");
                        }
                        while (!myFleet.placeShip(0, 3 - mod6, VERTICAL, SUBMARINE)) {
                            System.out.println("41");
                        }
                        if (mod6 == 1) {
                            while (!myFleet.placeShip(2, 3, VERTICAL, DESTROYER)) {
                                System.out.println("42");
                            }
                        } else {
                            while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) {/*System.out.println("41")*/

                            }
                        }
                        break;
                }
        }
    }

    private void setRandom(boolean allowTouching) {
        if (generator.nextInt(7) == 0 && drops >= 8 && placement != 5) {
            CornerPlacement();//corner placement to even out random's tendency for middle
        } else {
            int twoX = generator.nextInt(10);
            int twoY = generator.nextInt(10);
            int orientation = generator.nextInt(2);
            if (orientation == HORIZONTAL && twoX == 9) {
                twoX--;
            } else if (orientation == VERTICAL && twoY == 9) {
                twoY--;
            }
            while (!myFleet.placeShip(twoX, twoY, orientation, PATROL_BOAT)) {
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
                            if (!myFleet.placeShip(twoX + 2, twoY + 1, HORIZONTAL, ship)) {//top right Hor
                                if (!myFleet.placeShip(twoX + 2, twoY + 1, VERTICAL, ship)) {//top right Vert
                                    if (!myFleet.placeShip(twoX - getLength(ship), twoY + 1, HORIZONTAL, ship)) {//top left Hor
                                        if (!myFleet.placeShip(twoX - 1, twoY + 1, VERTICAL, ship)) {//top left Vert
                                            if (!myFleet.placeShip(twoX - getLength(ship), twoY - 1, HORIZONTAL, ship)) {//bottom left Hor
                                                if (!myFleet.placeShip(twoX - 1, twoY - getLength(ship), VERTICAL, ship)) {//bottom left Vert
                                                    if (!myFleet.placeShip(twoX + 2, twoY - 1, HORIZONTAL, ship)) {//bottom right Hor
                                                        if (!myFleet.placeShip(twoX + 2, twoY - getLength(ship), VERTICAL, ship)) {
                                                            if (allowTouching) {
                                                                if (!myFleet.placeShip(twoX, twoY + 1, VERTICAL, ship)) {//top
                                                                    if (!myFleet.placeShip(twoX, twoY - (getLength(ship) + 1), VERTICAL, ship)) {//bottom
                                                                        if (!myFleet.placeShip(twoX + 3, twoY, HORIZONTAL, ship)) {//right
                                                                            if (!myFleet.placeShip(twoX - (getLength(ship) + 1), twoY, HORIZONTAL, ship)) {//left
                                                                                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {//sides
                        int mod = generator.nextInt(2);
                        for (int i = 0; i < 4; i++) {
                            int ship = ships.get(i);
                            if (!myFleet.placeShip(twoX + 3, twoY, HORIZONTAL, ship)) {//right side
                                if (!myFleet.placeShip(twoX - (getLength(ship) + 1), twoY, HORIZONTAL, ship)) {//left side
                                    if (!myFleet.placeShip(twoX + mod, twoY + 2, VERTICAL, ship)) {//top
                                        if (!myFleet.placeShip(twoX + ((mod + 1) % 2), twoY - (getLength(ship) + 1), VERTICAL, ship)) {//bottom
                                            if (allowTouching) {
                                                if (!myFleet.placeShip(twoX + ((mod + 1) % 2), twoY + 2, VERTICAL, ship)) {//other top
                                                    if (!myFleet.placeShip(twoX + mod, twoY - (getLength(ship) + 1), VERTICAL, ship)) {//other bottom
                                                        if (!myFleet.placeShip(twoX - 1, twoY + 1, VERTICAL, ship)) {//other right side
                                                            if (!myFleet.placeShip(twoX + 2, twoY - 1, HORIZONTAL, ship)) {//other left side
                                                                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                case VERTICAL:
                    if (twoX != 0 && twoX != 9 && twoY != 0 && twoY != 8) {//corners
                        for (int i = 0; i < 4; i++) {
                            int ship = ships.get(i);
                            if (!myFleet.placeShip(twoX + 1, twoY + 2, VERTICAL, ship)) {//top right vert
                                if (!myFleet.placeShip(twoX + 1, twoY + 2, HORIZONTAL, ship)) {//top right hor
                                    if (!myFleet.placeShip(twoX - 1, twoY + 2, VERTICAL, ship)) {//top left vert
                                        if (!myFleet.placeShip(twoX - (getLength(ship)), twoY + 2, HORIZONTAL, ship)) {//top left hor
                                            if (!myFleet.placeShip(twoX - 1, twoY - getLength(ship), VERTICAL, ship)) {//bottom left vert
                                                if (!myFleet.placeShip(twoX - getLength(ship), twoY - 1, HORIZONTAL, ship)) {//bottom left hor
                                                    if (!myFleet.placeShip(twoX + 1, twoY - getLength(ship), VERTICAL, ship)) {//bottom right vert
                                                        if (!myFleet.placeShip(twoX + 1, twoY - 1, HORIZONTAL, ship)) {//bottom right hor
                                                            if (allowTouching) {
                                                                if (!myFleet.placeShip(twoX + 3, twoY, HORIZONTAL, ship)) {//right
                                                                    if (!myFleet.placeShip(twoX - (getLength(ship) + 1), twoY, HORIZONTAL, ship)) {//left
                                                                        if (!myFleet.placeShip(twoX, twoY + 1, VERTICAL, ship)) {//top
                                                                            if (!myFleet.placeShip(twoX + 1, twoY - (getLength(ship) + 1), VERTICAL, ship)) {//bottom
                                                                                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            } else {
                                                                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {//sides
                        int mod = generator.nextInt(2);
                        for (int i = 0; i < 4; i++) {
                            int ship = ships.get(i);
                            if (!myFleet.placeShip(twoX + 2, twoY + mod, HORIZONTAL, ship)) {//right side
                                if (!myFleet.placeShip(twoX - (getLength(ship) + 1), twoY + ((mod + 1) % 2), HORIZONTAL, ship)) {//left side
                                    if (!myFleet.placeShip(twoX, twoY + 3, VERTICAL, ship)) {//top
                                        if (!myFleet.placeShip(twoX, twoY - (getLength(ship) + 1), VERTICAL, ship)) {//bottom
                                            if (allowTouching) {
                                                if (!myFleet.placeShip(twoX + 2, twoY + ((mod + 1) % 2), HORIZONTAL, ship)) {//other right side
                                                    if (!myFleet.placeShip(twoX - (getLength(ship) + 1), twoY + mod, HORIZONTAL, ship)) {//other left side
                                                        if (!myFleet.placeShip(twoX + 1, twoY + 2, VERTICAL, ship)) {//top right
                                                            if (!myFleet.placeShip(twoX - 1, twoY - getLength(ship), VERTICAL, ship)) {//bottom left
                                                                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
            }
        }
    }

    void fillCorner() {
        if (drops >= 8 && generator.nextInt(4) == 0) {
            setRandom(true);
        } else {
            ArrayList<Integer> ships = new ArrayList<Integer>();
            for (int i = 1; i < 5; i++) {
                ships.add(i);
            }
            Collections.shuffle(ships);
            int orientation = generator.nextInt(2);
            switch (generator.nextInt(4)) {
                case 0://bottom right
                    while (!myFleet.placeShip(8 + orientation, 0, orientation, PATROL_BOAT)) {
                        System.out.println("1");
                    }
                    while (!myFleet.placeShip(7 - getLength(ships.get(0)) + orientation, 0, HORIZONTAL, ships.get(0))) {
                        System.out.println("2");
                    }
                    while (!myFleet.placeShip(8 - getLength(ships.get(1)), 1, HORIZONTAL, ships.get(1))) {
                        System.out.println("3");
                    }
                    while (!myFleet.placeShip(8, 2, VERTICAL, ships.get(2))) {
                        System.out.println("4");
                    }
                    while (!myFleet.placeShip(9, 2 + orientation, VERTICAL, ships.get(3))) {
                        System.out.println("5");
                    }
                    break;
                case 1://bottom left
                    while (!myFleet.placeShip(0, 0, orientation, PATROL_BOAT)) {
                        System.out.println("6");
                    }
                    while (!myFleet.placeShip(3 - orientation, 0, HORIZONTAL, ships.get(0))) {
                        System.out.println("7");
                    }
                    while (!myFleet.placeShip(2, 1, HORIZONTAL, ships.get(1))) {
                        System.out.println("8");
                    }
                    while (!myFleet.placeShip(1, 2, VERTICAL, ships.get(2))) {
                        System.out.println("9");
                    }
                    while (!myFleet.placeShip(0, 2 + orientation, VERTICAL, ships.get(3))) {
                        System.out.println("10");
                    }
                    break;
                case 2://top right
                    while (!myFleet.placeShip(8 + orientation, 9 - orientation, orientation, PATROL_BOAT)) {
                        System.out.println("11");
                    }
                    while (!myFleet.placeShip(7 - getLength(ships.get(0)) + orientation, 9, HORIZONTAL, ships.get(0))) {
                        System.out.println("12");
                    }
                    while (!myFleet.placeShip(8 - getLength(ships.get(1)), 8, HORIZONTAL, ships.get(1))) {
                        System.out.println("13");
                    }
                    while (!myFleet.placeShip(8, 8 - getLength(ships.get(2)), VERTICAL, ships.get(2))) {
                        System.out.println("14");
                    }
                    while (!myFleet.placeShip(9, 8 - getLength(ships.get(3)) - orientation, VERTICAL, ships.get(3))) {
                        System.out.println("15");
                    }
                    break;
                case 3://top left
                    while (!myFleet.placeShip(0, 9 - orientation, orientation, PATROL_BOAT)) {
                        System.out.println("16");
                    }
                    while (!myFleet.placeShip(3 - orientation, 9, HORIZONTAL, ships.get(0))) {
                        System.out.println("17");
                    }
                    while (!myFleet.placeShip(2, 8, HORIZONTAL, ships.get(1))) {
                        System.out.println("18");
                    }
                    while (!myFleet.placeShip(1, 8 - getLength(ships.get(2)), VERTICAL, ships.get(2))) {
                        System.out.println("19");
                    }
                    while (!myFleet.placeShip(0, 8 - getLength(ships.get(3)) - orientation, VERTICAL, ships.get(3))) {
                        System.out.println("20");
                    }
                    break;
            }
        }
    }

    void vertSet(int mod) {
        if (generator.nextInt(2) == 0) {
            while (!myFleet.placeShip(mod + 1, 5, VERTICAL, PATROL_BOAT)) {
                System.out.println("11 ");
            }
            while (!myFleet.placeShip(mod, 7, VERTICAL, SUBMARINE)) {
                System.out.println("12");
            }
            while (!myFleet.placeShip(mod + 2, 7, VERTICAL, DESTROYER)) {
                System.out.println("13");
            }
            while (!myFleet.placeShip(mod, 1, VERTICAL, BATTLESHIP)) {
                System.out.println("14");
            }
            while (!myFleet.placeShip(mod + 2, 0, VERTICAL, AIRCRAFT_CARRIER)) {
                System.out.println("15");
            }
        } else {
            while (!myFleet.placeShip(mod + 1, 3, VERTICAL, PATROL_BOAT)) {
                System.out.println("11 ");
            }
            while (!myFleet.placeShip(mod, 0, VERTICAL, SUBMARINE)) {
                System.out.println("12");
            }
            while (!myFleet.placeShip(mod + 2, 0, VERTICAL, DESTROYER)) {
                System.out.println("13");
            }
            while (!myFleet.placeShip(mod, 5, VERTICAL, BATTLESHIP)) {
                System.out.println("14");
            }
            while (!myFleet.placeShip(mod + 2, 5, VERTICAL, AIRCRAFT_CARRIER)) {
                System.out.println("15");
            }
        }
    }

    void horSet(int mod) {
        if (generator.nextInt(2) == 0) {
            while (!myFleet.placeShip(5, mod + 1, HORIZONTAL, PATROL_BOAT)) {
                System.out.println("11 ");
            }
            while (!myFleet.placeShip(7, mod, HORIZONTAL, SUBMARINE)) {
                System.out.println("12");
            }
            while (!myFleet.placeShip(7, mod + 2, HORIZONTAL, DESTROYER)) {
                System.out.println("13");
            }
            while (!myFleet.placeShip(1, mod + 2, HORIZONTAL, BATTLESHIP)) {
                System.out.println("14");
            }
            while (!myFleet.placeShip(0, mod, HORIZONTAL, AIRCRAFT_CARRIER)) {
                System.out.println("15");
            }
        } else {
            while (!myFleet.placeShip(3, mod + 1, HORIZONTAL, PATROL_BOAT)) {
                System.out.println("11 ");
            }
            while (!myFleet.placeShip(0, mod, HORIZONTAL, SUBMARINE)) {
                System.out.println("12");
            }
            while (!myFleet.placeShip(0, mod + 2, HORIZONTAL, DESTROYER)) {
                System.out.println("13");
            }
            while (!myFleet.placeShip(5, mod, HORIZONTAL, BATTLESHIP)) {
                System.out.println("14");
            }
            while (!myFleet.placeShip(5, mod + 2, HORIZONTAL, AIRCRAFT_CARRIER)) {
                System.out.println("15");
            }
        }
    }

    public void CornerPlacement() {
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
                if (!myFleet.placeShip(corners[i].getX(), corners[i].getY() - getLength(ship) + 1, orientation, ship)) {
                    System.out.println("failed to place ship " + ship + " at coordinates " + corners[i].getX() + "," + corners[i].getY() + "scenario 1");
                    while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), ship)) {
                    }
                }
            } else if (orientation == HORIZONTAL && corners[i].getX() == 9) {
                if (!myFleet.placeShip(corners[i].getX() - getLength(ship) + 1, corners[i].getY(), orientation, ship)) {
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
}
