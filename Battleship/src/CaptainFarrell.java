
import java.util.Random;
import java.util.Stack;

// TO DO: Ship placement, Attacking same tiles in SINK mode, games won: possibly inverse attack pattern if losing a lot, 
	//TO DO: new attack pattern?(Matt's didn't work), random attack as well as following pattern

public class CaptainFarrell implements Captain, Constants 
{
    // Called before each game to reset your ship locations.

    @Override
    public void initialize(int numMatches, int numCaptains, String opponent) 
    {
        generator = new Random();
        myFleet = new Fleet();
        x=0;
        y=0;
        a=0;
        xCount=0;
        yCount=0;
        myState = FIRE;
        targetStack = new Stack();
        myBoard = new int[10][10];
        
        // Each type of ship must be placed on the board.
        // the .place methods return whether it was possible to put a ship at the indicated position.
        
       
        while (!myFleet.placeShip(new Coordinate(generator.nextInt(9), 0), generator.nextInt(2), PATROL_BOAT)) 
        {
        }
        /*while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), PATROL_BOAT)) 
        {
        }*/
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), DESTROYER)) 
        {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), SUBMARINE)) 
        {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), BATTLESHIP)) 
        {
        }
        while (!myFleet.placeShip(generator.nextInt(10), generator.nextInt(10), generator.nextInt(2), AIRCRAFT_CARRIER)) 
        {
        }

        
        int i, j;
        for (i = 0; i < 10; i++) 
        {
            for (j = 0; j < 10; j++) 
            {
                myBoard[i][j] = 0;
            }
        }
        
        patternCompute = 0;
        sinkPatternCompute = 0;
    }

    // Passes your ship locations to the main program.
    @Override
    public Fleet getFleet() 
    {
        return myFleet;
    }
    
    // Makes an attack on the opponent
    @Override
    //make random attack every few attacks
    public Coordinate makeAttack() 
    {
    	int type;
    	int x = 0;
    	int y = 0;
    	Coordinate ret = new Coordinate(0 ,0);
    	switch(myState)
    	{
    		case FIRE:
    			




				int[] xCoord = {4,4,5,5,0,1,2,3,6,7,8,9,0,1,2,3,6,7,8,9,
						0,2,0,1,3,4,0,1,2,4,5,6,7,9,5,6,8,9,3,4,
						5,7,8,9,0,1,2,3,5,6,7,8,1,2,3,4,6,7,8,9,
						0,1,8,9,0,1,2,3,6,7,8,9,0,1,2,3,4,5,4,5,
						6,7,8,9,0,1,2,3,4,5,6,7,2,3,4,5,6,7,8,9};
				int[] yCoord = {4,5,5,4,0,1,2,3,6,7,8,9,9,8,7,6,3,2,1,0,
						2,0,4,3,1,0,6,5,4,2,1,0,9,7,9,8,6,5,9,8,
						7,5,4,3,8,7,6,5,3,2,1,0,9,8,7,6,4,3,2,1,
						1,0,9,8,3,2,1,0,9,8,7,6,5,4,3,2,1,0,9,8,
						7,6,5,4,7,6,5,4,3,2,1,0,9,8,7,6,5,4,3,2};
        
    				if(a<100)
    				{   					
    					x = xCoord[xCount];
    					y = yCoord[yCount];
    						while(myBoard[x][y] == 1)
    						{
    							xCount++;
    							yCount++;
    	    					x = xCoord[xCount];
    	    					y = yCoord[yCount];
    						}
    					xCount++;
    					yCount++;
    					a++;
    				}
    				else
    				{
    					xCount=0;
    					yCount=0;
    					a=0;
    				}
    				myBoard[x][y] = 1;
    				myLastShot[0] = x;
    				myLastShot[1] = y;
    				
    				
    				ret = new Coordinate(x, y);

    				break;
    				
    		case SINK:
                type = currentTarget.type;
                int cx = currentTarget.x;
                int cy = currentTarget.y;
                x = -1;
                y = -1;
                int failsafe = 0;

                while (true) 
                {
                    if (sinkPatternCompute >= sinkPatternLengths[type]) 
                    {
                        break;
                    }
                    x = cx + sinkPattern[type][sinkPatternCompute][0];
                    y = cy + sinkPattern[type][sinkPatternCompute][1];
                    if (x > 9 || x < 0 || y > 9 || y < 0) 
                    {
                        sinkPatternCompute++;
                        continue; 
                    }

                    else if (myBoard[x][y] == 0) 
                    {
                        sinkPatternCompute++;
                        break;
                    } 
                    else {
                        failsafe++;
                        sinkPatternCompute++;
                    }
                    if (failsafe > 100) 
                    {
                        break;
                    }
                }

                if (x > 9 || x < 0 || y > 9 || y < 0) 
                {
                    myState = FIRE;
                    x = attackPattern[patternCompute][0];
                    y = attackPattern[patternCompute][1];

                    if (patternCompute == 99) 
                    {
                        patternCompute = 0;
                      
                    } else 
                    {
                        patternCompute++;
                    }
                }
					myLastShot[0] = x;
					myLastShot[1] = y;
					
					myBoard[x][y] = 1;
    				
    				ret = new Coordinate(x, y);

    	}
    	return ret;
    }

    // Informs you of the result of your most recent attack
    @Override
    public void resultOfAttack(int result) 
    {
        int mhs = result - (result % 10);
        int type = result % 10;
        
        switch(myState)
        {
        case FIRE:
        		if (result == MISS)
        		{
        			
        		}
        		if(mhs == HIT_MODIFIER)
        		{
        			currentTarget = new acquireTarget(myLastShot[0], myLastShot[1], type);
        			myState = SINK;
        			sinkPatternCompute = 0;
        		}
        		if(mhs == SUNK_MODIFIER)
        		{
        			
        		}
        		break;
        case SINK:
        		if (result == MISS)
        		{
        			
        		}
        		if(mhs == HIT_MODIFIER)
        		{
        			if(type != currentTarget.type)
        			{
        				newTarget = new acquireTarget(myLastShot[0], myLastShot[1], type);
        				targetStack.push(newTarget);
        			}
        		}
        		if(mhs == SUNK_MODIFIER)
        		{
                    if (type == currentTarget.type) 
                    { 
                        if (targetStack.empty()) 
                        {
                            sinkPatternCompute = 0;
                            myState = FIRE;
                        } else 
                        {
                            currentTarget = (acquireTarget) targetStack.pop();
                            sinkPatternCompute = 0;
                        }
                    } 
                    else 
                    { 
                    
                    }
        		}
        	
        		break;
        }
    }

    // Informs you of the position of an attack against you.
    @Override
    public void opponentAttack(Coordinate coord) 
    {
        // Add code here to process or record opponent attacks
    }

    // Informs you of the result of the game.
    @Override
    public void resultOfGame(int result) 
    {
        // Add code here to process the result of a game
    	
    	/*gamesPlayed++;
    	
    	
    	if(result == 1)
    	{
    		winNumber++;
    	}
    	else
    	{
    		loseNumber++;
    	}*/
    	
    }

    
    
    int[]xCoord;
    int myState;
    int[]yCoord;
    int[][] myBoard;
    int xCount, yCount;
    int x, y, a;
    int winNumber = 0;
    int loseNumber = 0;
    int gamesPlayed = 0;
    Random generator;
    Fleet myFleet;
    int sinkPatternCompute;
    int patternCompute;
    int[] xPreviousAttack;
    int[] yPreviousAttack;
    int xIndex;
    int yIndex;
    int[] myLastShot = {0, 0};
    final static int FIRE = 0;
    final static int SINK = 1;
    Stack targetStack;
    acquireTarget currentTarget, newTarget;
    int[][] attackPattern = {{4, 4}, {4, 5}, {5, 5}, {5, 4}, {0, 0}, {1, 1}, {2, 2}, {3, 3}, {6, 6}, {7, 7}, 
    						{8, 8}, {9, 9}, {0, 9}, {1, 8}, {2, 7}, {3, 6}, {6, 3}, {7, 2}, {8, 1}, {9, 0}, 
    						{0, 2}, {2, 0}, {0, 4}, {1, 3}, {3, 1}, {4, 0}, {0, 6}, {1, 5}, {2, 4}, {4, 2}, 
    						{5, 1}, {6, 0}, {7, 9}, {9, 7}, {5, 9}, {6, 8}, {8, 6}, {9, 5}, {3, 9}, {4, 8}, 
    						{5, 7}, {7, 5}, {8, 4}, {9, 3}, {0, 8}, {1, 7}, {2, 6}, {3, 5}, {5, 3}, {6, 2}, 
    						{7, 1}, {8, 0}, {1, 9}, {2, 8}, {3, 7}, {4, 6}, {6, 4}, {7, 3}, {8, 2}, {9, 1}, 
    						{0, 1}, {1, 0}, {8, 9}, {9, 8}, {0, 3}, {1, 2}, {2, 1}, {3, 0}, {6, 9}, {7, 8}, 
    						{8, 7}, {9, 6}, {0, 5}, {1, 4}, {2, 3}, {3, 2}, {4, 1}, {5, 0}, {4, 9}, {5, 8}, 
    						{6, 7}, {7, 6}, {8, 5}, {9, 4}, {0, 7}, {1, 6}, {2, 5}, {3, 4}, {4, 3}, {5, 2}, 
    						{6, 1}, {7, 0}, {2, 9}, {3, 8}, {4, 7}, {5, 6}, {6, 5}, {7, 4}, {8, 3}, {9, 2}};
    int[] sinkPatternLengths = {4, 8, 8, 12, 16};
    int[][][] sinkPattern = {
            {{1, 0}, {0, 1}, {-1, 0}, {0, -1}},
            {{1, 0}, {2, 0}, {0, 1}, {0, 2}, {-1, 0}, {-2, 0}, {0, -1}, {0, -2}},
            {{1, 0}, {2, 0}, {0, 1}, {0, 2}, {-1, 0}, {-2, 0}, {0, -1}, {0, -2}},
            {{1, 0}, {2, 0}, {3, 0}, {0, 1}, {0, 2}, {0, 3}, {-1, 0}, {-2, 0}, {-3, 0}, {0, -1}, {0, -2}, {0, -3}},
            {{1, 0}, {2, 0}, {3, 0}, {4, 0}, {0, 1}, {0, 2}, {0, 3}, {0, 4}, {-1, 0}, {-2, 0}, {-3, 0}, {-4, 0}, {0, -1}, {0, -2}, {0, -3}, {0, -4}}};
    

    public class acquireTarget {

        public acquireTarget(int x, int y, int type) 
        {
            this.x = x;
            this.y = y;
            this.type = type;
            if (type >= 0 && type <= 4) 
            {
                this.length = this.shipLengths[type];
            } else {
                this.length = -1; 
            }
        }
        public int x, y;
        public int type;
        public int[] shipLengths = {2, 3, 3, 4, 5};
        public int length;
        public int orientation;
    }
}
