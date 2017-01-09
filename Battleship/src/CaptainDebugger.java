
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import javax.swing.*;
import javax.swing.event.*;

/**
 * <p>Captain debugger allows you to manually play against a Captain AI to test
 * it out. It provides an interactive playing field in interface to place your
 * ships and then attack your opponent and see their attacks.</p>
 *
 * <p>You do not need to modify or use this class in any way to participate in
 * the competition. It is provided to help you design and debug your own captain
 * AI.</p>
 *
 * @author Andrew Christie
 * @author Seth Dutter - dutters@uwstout.edu
 *
 * @version SPRING.2013
 */
public class CaptainDebugger extends JFrame
        implements MouseListener, ActionListener, ChangeListener, MouseMotionListener, Constants {

    private final int GRID_WIDTH = 10;
    private final int GRID_HEIGHT = 10;
    private CaptainDebuggerCanvas canvas1;
    private CaptainDebuggerCanvas canvas2;
    private Class<?> currentAIClass = null;
    private Captain currentAI = null;
    private JTextField currentAIWins;
    private JTextField currentAILosses;
    private JButton newButton;
    private Ellipse2D[] AIFleet_Shapes;
    private Ellipse2D[] playerFleet_Shapes;
    private Fleet aiFleet;
    private Fleet playerFleet;
    private static CaptainDebugger instance = null;
    private String AIName;
    private boolean gameover = false;
    private boolean playersTurn = true;
    private boolean placeShipsVertical;
    private static boolean addingPlayerShips = false;
    private static int currentPlacingShip = 0;
    private int mouseX = 0;
    private int mouseY = 0;
    private static final long serialVersionUID = 1;

    public CaptainDebugger(File AIFile, String AIName) {
        super(AIName);
        this.AIName = AIName;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 600);

        this.setLayout(new GridLayout(1, 2));

        AIFleet_Shapes = new Ellipse2D[5];
        playerFleet_Shapes = new Ellipse2D[5];
        playerFleet = new Fleet();

        canvas1 = new CaptainDebuggerCanvas(false, null, playerFleet_Shapes);
        canvas2 = new CaptainDebuggerCanvas(true, null, playerFleet_Shapes);

        buildCanvasPanels();
        instance = this;

        setVisible(true);

        loadAI(AIFile);
    }

    private void buildCanvasPanels() {
        JPanel canvas1Panel = new JPanel();
        JPanel canvas2Panel = new JPanel();

        JLabel canvas1Label = new JLabel(AIName + "'s Vision");
        JLabel canvas2Label = new JLabel("Player's Vision");

        canvas1Label.setFont(new Font("Arial", Font.PLAIN, 16));
        canvas2Label.setFont(new Font("Arial", Font.PLAIN, 16));

        canvas1Label.setPreferredSize(new Dimension(300, 50));
        canvas2Label.setPreferredSize(new Dimension(300, 50));

        canvas1Label.setHorizontalAlignment(JLabel.CENTER);
        canvas2Label.setHorizontalAlignment(JLabel.CENTER);

        canvas1Panel.setLayout(new BorderLayout());
        canvas2Panel.setLayout(new BorderLayout());

        canvas1Panel.add(canvas1, BorderLayout.CENTER);
        canvas2Panel.add(canvas2, BorderLayout.CENTER);

        canvas1Panel.add(canvas1Label, BorderLayout.NORTH);
        canvas2Panel.add(canvas2Label, BorderLayout.NORTH);

        canvas1Panel.add(new JPanel(), BorderLayout.WEST);
        canvas2Panel.add(new JPanel(), BorderLayout.WEST);

        canvas1Panel.add(new JPanel(), BorderLayout.EAST);
        canvas2Panel.add(new JPanel(), BorderLayout.EAST);

        JPanel canvas1Controls = new JPanel();
        JPanel canvas2Controls = new JPanel();

        canvas1Controls.setPreferredSize(new Dimension(300, 100));
        canvas2Controls.setPreferredSize(new Dimension(300, 100));

        canvas1Controls.setAlignmentX(CENTER_ALIGNMENT);
        canvas2Controls.setAlignmentX(CENTER_ALIGNMENT);

        JPanel checkBoxPanel = new JPanel();

        JCheckBox canvas1ShowShips = new JCheckBox("Show Ships");
        canvas1ShowShips.setActionCommand("ShowShips");
        canvas1ShowShips.addActionListener(this);
        checkBoxPanel.add(canvas1ShowShips);

        canvas1Controls.add(checkBoxPanel);

        JPanel buttonPanel = new JPanel();
        newButton = new JButton("New Game");
        newButton.setActionCommand("NewGame");
        newButton.addActionListener(this);
        newButton.setEnabled(false);
        buttonPanel.add(newButton);

        JPanel currentAISummary = new JPanel();
        currentAIWins = new JTextField(0);
        currentAIWins.setPreferredSize(new Dimension(60, 30));
        currentAIWins.setEditable(false);
        currentAILosses = new JTextField(0);
        currentAILosses.setPreferredSize(new Dimension(60, 30));
        currentAILosses.setEditable(false);
        currentAISummary.add(new JLabel("AI Record: "));
        currentAISummary.add(currentAIWins);
        currentAISummary.add(new JLabel("-"));
        currentAISummary.add(currentAILosses);

        canvas2Controls.setLayout(new GridLayout(3, 1));
        canvas2Controls.add(currentAISummary);
        canvas2Controls.add(buttonPanel);

        canvas1Panel.add(canvas1Controls, BorderLayout.SOUTH);
        canvas2Panel.add(canvas2Controls, BorderLayout.SOUTH);

        add(canvas1Panel);
        add(canvas2Panel);
    }

    private void loadAI(File currentPath) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        Class<?> selectedClass = null;

        String packageName = "";
        if (Battleship.class.getPackage() != null) {
            packageName = Battleship.class.getPackage().getName() + ".";
        }

        String name = packageName + currentPath.getName();

        try {
            if (name.endsWith(".class") && !name.equals("Captain.class")) {
                selectedClass = cl.loadClass(name.substring(0, name.length() - 6));
            } else {
                return;
            }
        } catch (ClassNotFoundException ex) {
            System.out.println(name + "," + ex);
            System.exit(1);
        }
        if (selectedClass != null) {
            for (Class<?> interf : selectedClass.getInterfaces()) {
                if (interf.getName().equals(packageName + "Captain")) {
                    if (selectedClass != currentAIClass) {
                        currentAIClass = selectedClass;
                        currentAIWins.setText("0");
                        currentAILosses.setText("0");
                    }
                }
            }
        }

        if (currentAIClass == null) {
            JOptionPane.showMessageDialog(null, "Failed to load AI Class file");
            currentAIWins.setText("");
            currentAILosses.setText("");
        } else {
            AIFleet_Shapes = new Ellipse2D[5];
            playerFleet_Shapes = new Ellipse2D[5];
            playerFleet = new Fleet();
            canvas1.ships = playerFleet_Shapes;
            canvas2.ships = playerFleet_Shapes;
            canvas1.grid = new int[GRID_WIDTH][GRID_HEIGHT];
            canvas2.grid = new int[GRID_WIDTH][GRID_HEIGHT];

            newButton.setEnabled(true);
            playersTurn = false;
        }
    }

    public void AITakeTurn() {
        if (!gameover) {
            Coordinate attacktwocoord = currentAI.makeAttack();
            int attacktwo = playerFleet.attacked(attacktwocoord);
            currentAI.resultOfAttack(attacktwo);
            canvas1.grid[attacktwocoord.getX()][attacktwocoord.getY()] = attacktwo;

            if (attacktwo == Constants.DEFEATED) {
                gameover = true;
                currentAIWins.setText("" + (Integer.parseInt(currentAIWins.getText()) + 1));
            }

            playersTurn = true;
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        switch (arg0.getActionCommand()) {
            case "ShowShips":
                canvas1.showShips = ((AbstractButton) arg0.getSource()).getModel().isSelected();
                canvas2.showShips = ((AbstractButton) arg0.getSource()).getModel().isSelected();
                break;
            case "NewGame":
                try {
                    AIFleet_Shapes = new Ellipse2D[5];
                    playerFleet_Shapes = new Ellipse2D[5];
                    playerFleet = new Fleet();
                    canvas1.ships = playerFleet_Shapes;
                    canvas2.ships = playerFleet_Shapes;
                    canvas1.grid = new int[GRID_WIDTH][GRID_HEIGHT];
                    canvas2.grid = new int[GRID_WIDTH][GRID_HEIGHT];

                    currentAI = (Captain) currentAIClass.newInstance();
                    //canvas1.currentCaptain = currentAI;
                    currentAI.initialize(1, 2, "Human");
                    aiFleet = currentAI.getFleet();

                    playersTurn = true;
                    gameover = false;
                    currentPlacingShip = 0;
                } catch (InstantiationException | IllegalAccessException e) {
                    System.out.println(e);
                }
                break;
        }

        canvas1.repaint();
        canvas2.repaint();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        if (SwingUtilities.isRightMouseButton(arg0)) {
            placeShipsVertical = !placeShipsVertical;
        } else if (playersTurn && currentAI != null && !gameover) {
            if (currentPlacingShip < playerFleet_Shapes.length) {
                addShip(new Coordinate(arg0.getX() * GRID_WIDTH / canvas2.getWidth(), arg0.getY() * GRID_HEIGHT / canvas2.getHeight()),
                        placeShipsVertical ? 1 : 0, currentPlacingShip, false);

                if (currentPlacingShip >= playerFleet_Shapes.length) {
                    canvas2.ships = AIFleet_Shapes;
                    playersTurn = Math.random() < .5;
                    if (!playersTurn && !gameover) {
                        AITakeTurn();
                    }
                }
            } else {
                Coordinate attackonecoord = new Coordinate(arg0.getX() * GRID_WIDTH / canvas2.getWidth(), arg0.getY() * GRID_HEIGHT / canvas2.getHeight());
                int attackone = aiFleet.attacked(attackonecoord);
                currentAI.opponentAttack(attackonecoord);
                canvas2.grid[attackonecoord.getX()][attackonecoord.getY()] = attackone;

                // Output Result
                System.out.print(attackonecoord.toString());
                if (attackone != MISS) {
                    switch (attackone) {
                        case HIT_PATROL_BOAT:
                            System.out.println(" - Hit Patrol Boat");
                            break;
                        case SUNK_PATROL_BOAT:
                            System.out.println(" - Sunk Patrol Boat");
                            break;
                        case HIT_DESTROYER:
                            System.out.println(" - Hit Destroyer");
                            break;
                        case SUNK_DESTROYER:
                            System.out.println(" - Sunk Destroyer");
                            break;
                        case HIT_SUBMARINE:
                            System.out.println(" - Hit Submarine");
                            break;
                        case SUNK_SUBMARINE:
                            System.out.println(" - Sunk Submarine");
                            break;
                        case HIT_BATTLESHIP:
                            System.out.println(" - Hit Battleship");
                            break;
                        case SUNK_BATTLESHIP:
                            System.out.println(" - Sunk Battleship");
                            break;
                        case HIT_AIRCRAFT_CARRIER:
                            System.out.println(" - Hit Aircraft Carrier");
                            break;
                        case SUNK_AIRCRAFT_CARRIER:
                            System.out.println(" - Sunk Aircraft Carrier");
                            break;
                    }
                } else {
                    System.out.println(" - Miss");
                }

                if (attackone == Constants.DEFEATED) {
                    gameover = true;
                    currentAILosses.setText("" + (Integer.parseInt(currentAILosses.getText()) + 1));
                } else {
                    playersTurn = false;
                    AITakeTurn();
                }
            }
        }

        canvas1.repaint();
        canvas2.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent arg0) {
        if (playersTurn && currentAI != null && currentPlacingShip < playerFleet_Shapes.length) {
            mouseX = arg0.getX() * GRID_WIDTH / ((CaptainDebuggerCanvas) arg0.getSource()).getWidth();
            mouseY = arg0.getY() * GRID_HEIGHT / ((CaptainDebuggerCanvas) arg0.getSource()).getHeight();

            canvas2.repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent arg0) {
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }

    public static void addShip(Coordinate location, int direction, int shipType, boolean isAI) {
        if (addingPlayerShips) {
            return;
        }

        if (instance != null) {
            int length = 0;
            switch (shipType) {
                case Constants.PATROL_BOAT:
                    length = Constants.PATROL_BOAT_LENGTH;
                    break;
                case Constants.DESTROYER:
                    length = Constants.DESTROYER_LENGTH;
                    break;
                case Constants.SUBMARINE:
                    length = Constants.SUBMARINE_LENGTH;
                    break;
                case Constants.BATTLESHIP:
                    length = Constants.BATTLESHIP_LENGTH;
                    break;
                case Constants.AIRCRAFT_CARRIER:
                    length = Constants.AIRCRAFT_CARRIER_LENGTH;
                    break;
            }
            if (length != 0) {
                if (isAI) {
                    if (direction == Constants.HORIZONTAL) {
                        instance.AIFleet_Shapes[shipType] = new Ellipse2D.Float(location.getX(), location.getY(), length, 1);
                    } else if (direction == Constants.VERTICAL) {
                        instance.AIFleet_Shapes[shipType] = new Ellipse2D.Float(location.getX(), location.getY(), 1, length);
                    }
                } else {
                    boolean shipPlaced;
                    addingPlayerShips = true;

                    shipPlaced = instance.playerFleet.placeShip(location, direction, shipType);

                    if (shipPlaced) {
                        if (direction == Constants.HORIZONTAL) {
                            instance.playerFleet_Shapes[shipType] = new Ellipse2D.Float(location.getX(), location.getY(), length, 1);
                        } else if (direction == Constants.VERTICAL) {
                            instance.playerFleet_Shapes[shipType] = new Ellipse2D.Float(location.getX(), location.getY(), 1, length);
                        }

                        currentPlacingShip++;
                    }

                    addingPlayerShips = false;
                }
            }
        }
    }

    private class CaptainDebuggerCanvas extends JPanel {

        private boolean isDebugCanvas = false;
        private static final long serialVersionUID = 1;
        //private Captain currentCaptain;
        private int[][] grid = new int[GRID_WIDTH][GRID_HEIGHT];
        private Ellipse2D[] ships;
        public boolean showShips = false;

        CaptainDebuggerCanvas(boolean isDebugCanvas, Captain currentCaptain, Ellipse2D[] ships) {
            super(true);
            this.isDebugCanvas = isDebugCanvas;
            //this.currentCaptain = currentCaptain;
            this.ships = ships;
            if (isDebugCanvas) {
                addMouseListener(CaptainDebugger.this);
                addMouseMotionListener(CaptainDebugger.this);
            }
            this.setBackground(Color.CYAN);
        }

        private void processGridCell(Graphics2D g2d, int x, int y, int result) {
            switch (result) {
                case 0:
                    return;
                case -1:
                    g2d.drawOval(x * getWidth() / GRID_WIDTH, y * getHeight() / GRID_HEIGHT, getWidth() / GRID_WIDTH, getHeight() / GRID_HEIGHT);
                    g2d.setColor(Color.BLACK);
                    return;
                case 106:
                    g2d.setColor(Color.BLUE);
                    break;
                default:
                    g2d.setColor(Color.RED);
            }
            g2d.fillOval(x * getWidth() / GRID_WIDTH, y * getHeight() / GRID_HEIGHT, getWidth() / GRID_WIDTH, getHeight() / GRID_HEIGHT);
            g2d.setColor(Color.BLACK);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            Graphics2D g2d;

            g2d = (Graphics2D) g;
            g2d.setStroke(new BasicStroke(3));
            if (showShips || (currentPlacingShip < playerFleet_Shapes.length && isDebugCanvas)) {
                g2d.setColor(Color.GRAY);
                for (Ellipse2D ellipse : ships) {
                    if (ellipse != null) {
                        g2d.fillOval((int) (ellipse.getX() * getWidth() / GRID_WIDTH), (int) (ellipse.getY() * getHeight() / GRID_HEIGHT), (int) (ellipse.getWidth() * getWidth() / GRID_WIDTH), (int) (ellipse.getHeight() * getHeight() / GRID_HEIGHT));
                    }
                }
            }
            g2d.setColor(Color.BLACK);

            if (playersTurn && currentAI != null && currentPlacingShip < playerFleet_Shapes.length && isDebugCanvas) {
                int length = 0;
                switch (currentPlacingShip) {
                    case Constants.PATROL_BOAT:
                        length = Constants.PATROL_BOAT_LENGTH;
                        break;
                    case Constants.DESTROYER:
                        length = Constants.DESTROYER_LENGTH;
                        break;
                    case Constants.SUBMARINE:
                        length = Constants.SUBMARINE_LENGTH;
                        break;
                    case Constants.BATTLESHIP:
                        length = Constants.BATTLESHIP_LENGTH;
                        break;
                    case Constants.AIRCRAFT_CARRIER:
                        length = Constants.AIRCRAFT_CARRIER_LENGTH;
                        break;
                }

                g2d.setColor(Color.LIGHT_GRAY);
                if (placeShipsVertical) {
                    g2d.fillOval((int) (mouseX * getWidth() / GRID_WIDTH), (int) (mouseY * getHeight() / GRID_HEIGHT), (int) (1 * getWidth() / GRID_WIDTH), (int) (length * getHeight() / GRID_HEIGHT));
                } else {
                    g2d.fillOval((int) (mouseX * getWidth() / GRID_WIDTH), (int) (mouseY * getHeight() / GRID_HEIGHT), (int) (length * getWidth() / GRID_WIDTH), (int) (1 * getHeight() / GRID_HEIGHT));
                }
                g2d.setColor(Color.BLACK);
            }

            for (int y = 0; y < GRID_HEIGHT; y++) {
                for (int x = 0; x < GRID_WIDTH; x++) {
                    g2d.drawRect(x * getWidth() / GRID_WIDTH, y * getHeight() / GRID_HEIGHT, getWidth() / GRID_WIDTH, getHeight() / GRID_HEIGHT);
                    this.processGridCell(g2d, x, y, grid[x][y]);
                }
            }
        }
    }
}
