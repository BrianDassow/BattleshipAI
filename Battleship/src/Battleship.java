
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.table.*;

/**
* <p>The graphical user interface and main entry point for the Battleship
* program.</p>
*
* <p>This is the view half of the model-view paradigm for the battleship
* program's GUI (though it does contain some model logic for the sake of
* considerably simpler code). It is also the execution entry point. To
* participate in the competition you will NOT need to edit or engage this class
* but you may find it educational to examine it and understand how it
* works.</p>
*
* <p>This class is responsible for the following tasks:</p>
*
* <ul> <li>Initializing all the GUI elements.</li> <li>Responding to all mouse
* and GUI actions (such as selection changes).</li> <li>Helping the user find
* all the captain classes (either in the default folder or a folder of the
* users choice).</li> <li>Running the actual competition between the enabled
* captains (in a separate thread).</li> <li>Tracking and reporting the progress
* of the current battle.</li> <li>Visualizing the statistics of the battle (as
* regular reports or heat-maps).</li> </ul>
*
* @author Seth Dutter - dutters@uwstout.edu
* @author Seth Berrier - berriers@uwstout.edu
*
* @version SPRING.2013
*/
public class Battleship extends JFrame implements Constants, ActionListener {

                Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
    // Because we implement serializable
    private static final long serialVersionUID = 1;
    /**
     * The labels for the heat-map X-axis
     */
    protected static final String[] X_AXIS_VALS = {"0  ", "1  ", "2  ", "3  ", "4  ", "5  ", "6  ", "7  ", "8  ", "9  "};
    /**
     * The labels for the heat-map Y-axis
     */
    protected static final String[] Y_AXIS_VALS = {"9  ", "8  ", "7  ", "6  ", "5  ", "4  ", "3  ", "2  ", "1  ", "0  "};
    /**
     * A table of blank data used to initialize the heat-maps to be empty.
     */
    protected static final double BLANK_DATA[][] = {
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
    };
    /**
     * The color for heat map values when the map is blank
     */
    protected static final Color blankColor = Color.WHITE;
    /**
     * The color used for minimum values in the heat map
     */
    protected static final Color lowColor = Color.BLUE;
    /**
     * The color used for maximum values in the heat map
     */
    protected static final Color highColor = Color.RED;
    /**
     * these are samples used in the heatmap section.
     */
    private boolean hasRun = false;

    /**
     * The various 'states' of the GUI which provide different behavior.
     */
    public enum GUIMode {

        /**
         * The normal GUI state where buttons behave as expected (and stop is
         * disabled)
         */
        NORMAL_MODE,
        /**
         * The GUI state when a battle is active where all buttons (except stop)
         * are disabled.
         */
        BATTLE_MODE
    }
    /**
     * The current state of the GUI (normal or battle)
     */
    protected GUIMode currentMode;
    /**
     * The main data model visualized by the table
     */
    protected BattleshipTableModel battleModel;
    /**
     * Statistical records of the battle that just took place
     */
    protected HashMap<String, CaptainStatistics> detailedRecords;
    /**
     * The current path where the captain classes can be found
     */
    protected File currentPath;
    /**
     * A separate thread where the actual competition is run
     */
    protected Thread battleThread;
    /**
     * A flag used to signal the battle thread that it should stop
     */
    protected boolean keepGoing;
    /**
     * The number of captains participating in the competition (includes ones
     * that are disabled)
     */
    protected int numCaptains;
    /**
     * Half the number of matches that are requested
     */
    protected int halfNumberOfMatches;
    // The heat maps will be placed in their own panel with these components
    private JPanel heatMapPanel;
    private JLabel placementHeatMapLabel;
    private JLabel attackHeatMapLabel;
    private HeatChart placementHeatChart;
    private HeatChart attackHeatChart;
    // The toolbar at the top of the GUI with its various buttons and elements
    private JToolBar toolbar;
    private JButton getPath;
    private JButton resetDataButton;
    private JButton runCompetition;
    private JButton stopButton;
    private JLabel totalProgress;
    private JLabel currentprogress;
    private JLabel iterationLabel;
    private JProgressBar totalProgressBar;
    private JProgressBar currentProgressBar;
    private JComboBox<String> iterations;
    // The table that visualizes the BattleshipTableModel data
    private JTable table;
    private CheckBoxHeader tableCheckboxHeader;
    private JScrollPane scrollPane;
    // The panel with text statistics about the battle
    private JPanel statsPanel;
    private JComboBox<String> opponentCombo;
    private JTextArea statsText;

    /**
     * Create a new Battleship GUI and engine.
     */
    public Battleship() {
        // Set default file path
        try {
            currentPath = new File(URLDecoder.decode(Battleship.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8"));
            if (Battleship.class.getPackage() != null) {
                currentPath = new File(currentPath, Battleship.class.getPackage().getName().replace(".", "/"));
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Battleship.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Initialize other members to default values
        halfNumberOfMatches = 2500;
        detailedRecords = new HashMap<>();
        currentMode = GUIMode.NORMAL_MODE;
        tableCheckboxHeader = null;
        battleModel = null;

        // Now, setup the GUI
        initializeGUI();
        remakeBattleshipTableModel();
    }

    /**
     * Build and initialize the Battleship GUI using Java swing components.
     */
    public final void initializeGUI() {
        // Make the toolbar
        toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Button to browse for captain classes
        getPath = new JButton("Browse");
        getPath.setMargin(new Insets(2, 2, 2, 2));
        getPath.addActionListener(this);
        toolbar.add(getPath);
        toolbar.addSeparator();

        resetDataButton = new JButton("Reset");
        resetDataButton.setMargin(new Insets(2, 2, 2, 2));
        resetDataButton.addActionListener(this);
        toolbar.add(resetDataButton);
        toolbar.addSeparator();

        // Button to begin the competition
        runCompetition = new JButton("Run Competition");
        runCompetition.setMargin(new Insets(2, 2, 2, 2));
        runCompetition.addActionListener(this);
        toolbar.add(runCompetition);
        toolbar.addSeparator();

        // Stop button
        stopButton = new JButton("Stop");
        stopButton.setMargin(new Insets(2, 2, 2, 2));
        stopButton.addActionListener(this);
        stopButton.setEnabled(false);
        toolbar.add(stopButton);
        toolbar.addSeparator();

        // Bar to select number of matches
        iterationLabel = new JLabel("Number of matches:  ");
        toolbar.add(iterationLabel);

        String[] comboboxoptions = {"500", "1000", "5000", "10000", "50000", "250000", "1000000"};
        iterations = new JComboBox<>(comboboxoptions);
        iterations.setSelectedIndex(2);
        iterations.addActionListener(this);
        toolbar.add(iterations);
        toolbar.addSeparator();

        // Overall progress bar
        totalProgressBar = new JProgressBar(0, 100);
        totalProgress = new JLabel("Total Progress:  ");
        toolbar.add(totalProgress);
        toolbar.add(totalProgressBar);
        toolbar.addSeparator();

        // Current progress bar
        currentProgressBar = new JProgressBar(0, 100);
        currentprogress = new JLabel("Current Match:  ");
        toolbar.add(currentprogress);
        toolbar.add(currentProgressBar);

        // Layout toolbar
        add(toolbar, BorderLayout.NORTH);

        // Setup the Table
        table = new JTable(new DefaultTableModel());
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        ListSelectionModel tableModel = table.getSelectionModel();
        tableModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selected = table.getSelectedRow();
                if (selected < 0) {
                    updateHeatCharts(null);
                } else {
                    String captainName = (String) table.getValueAt(selected, BattleshipTableModel.NAME_COLUMN_INDEX);
                    updateHeatCharts(captainName);
                    updateStatsText(captainName);
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable target = (JTable) e.getSource();
                    if (target.getSelectedColumn() == BattleshipTableModel.NAME_COLUMN_INDEX) {
                        String captainName = (String) table.getValueAt(target.getSelectedRow(), BattleshipTableModel.NAME_COLUMN_INDEX);

                        @SuppressWarnings("unused")
                        CaptainDebugger cd = new CaptainDebugger(new File(currentPath, captainName + ".class"), captainName);
                    }
                }
            }
        });

        scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        statsPanel = new JPanel();
        BoxLayout myLayout = new BoxLayout(statsPanel, BoxLayout.Y_AXIS);
        statsPanel.setLayout(myLayout);

        JLabel myLabel = new JLabel("Select an opponent:");
        myLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(myLabel);

        opponentCombo = new JComboBox<>();
        opponentCombo.addActionListener(this);
        opponentCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(opponentCombo);

        statsText = new JTextArea("Please run the competition first");
        statsText.setColumns(35);
        statsText.setEnabled(false);
        statsText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        statsText.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(statsText);

        add(statsPanel, BorderLayout.LINE_END);

        // Setup the heatmap panel
        placementHeatChart = new HeatChart(BLANK_DATA, 0.0, 0.0);
        placementHeatChart.setCellSize(new Dimension(40, 40));
        placementHeatChart.setXValues(X_AXIS_VALS);
        placementHeatChart.setYValues(Y_AXIS_VALS);
        placementHeatChart.setHighValueColour(blankColor);
        placementHeatChart.setLowValueColour(blankColor);

        attackHeatChart = new HeatChart(BLANK_DATA, 0.0, 0.0);
        attackHeatChart.setCellSize(new Dimension(40, 40));
        attackHeatChart.setXValues(X_AXIS_VALS);
        attackHeatChart.setYValues(Y_AXIS_VALS);
        attackHeatChart.setHighValueColour(blankColor);
        attackHeatChart.setLowValueColour(blankColor);

        heatMapPanel = new JPanel(new GridBagLayout());
        heatMapPanel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                if (hasRun) {
                    makeHeatGUI();
                }

            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                //do nothing
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
                //do nothing
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
                //do nothing
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
                //do nothing
            }
        });
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        placementHeatMapLabel = new JLabel(new ImageIcon(placementHeatChart.getChartImage()));
        c.gridx = 0;
        c.gridy = 0;
        heatMapPanel.add(placementHeatMapLabel, c);

        c.gridx = 0;
        c.gridy = 1;
        heatMapPanel.add(new JLabel("Placement Pattern"), c);
        heatMapPanel.add(new JSeparator(SwingConstants.VERTICAL), c);

        attackHeatMapLabel = new JLabel(new ImageIcon(attackHeatChart.getChartImage()));
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 1;
        heatMapPanel.add(attackHeatMapLabel, c);

        c.gridx = 2;
        c.gridy = 1;
        heatMapPanel.add(new JLabel("Attack Pattern"), c);

        add(heatMapPanel, BorderLayout.SOUTH);

        setTitle("Battleship AI Competition");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLocation((screenDim.width - 1000) / 2, (screenDim.height - 730) / 2);

        setSize(1000, 700);
        setVisible(true);
    }

    protected void makeHeatGUI() {
       if (table.getSelectedRow() >= 0) {
            String captainName = (String) table.getValueAt(table.getSelectedRow(), BattleshipTableModel.NAME_COLUMN_INDEX);
            CaptainStatistics stats = detailedRecords.get(captainName);
            @SuppressWarnings("unused")
            HeatGUI hg = new HeatGUI(stats.getSamples(), stats.getSampleNames(), captainName);
        }

    }

    /**
     * Execution entry point for the Battleship program. This will initialize
     * the current path where captain's are searched for and then create the
     * Battleship GUI and start it running in a separate class.
     *
     * @param args The usual command line parameters passed to a main method
     * (ignored).
     */
    public static void main(String[] args) {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.out.println(e);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                @SuppressWarnings("unused")
                Battleship bs = new Battleship();
            }
        });
    }

    /**
     * The mechanism for responding to GUI and thread events. It is required by
     * the ActionListener interface and enables us to respond to mouse and GUI
     * actions.
     *
     * @param event The event we will respond to.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        // Open a window to select the path to the Captain classes
        if (event.getSource() == getPath) {
            JFileChooser chooser = new JFileChooser(currentPath);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Class Files", "class");
            chooser.setFileFilter(filter);
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                currentPath = chooser.getSelectedFile();
                if (currentPath.isFile()) {
                    currentPath = currentPath.getParentFile();
                }

                remakeBattleshipTableModel();
            }
        } // Reset the data table
        else if (event.getSource() == resetDataButton) {

            int reply = JOptionPane.showConfirmDialog(this,
                    "This will erase all the data in the table. Are you sure you wish to proceed?",
                    "Erase All Data", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                remakeBattleshipTableModel();
                detailedRecords = new HashMap<>();
                updateHeatCharts(null);
                updateStatsText(null);
                this.hasRun = false;
            }

        } // Begin the competition
        else if (event.getSource() == runCompetition) {
            if (currentPath != null) {
                battleThread = new Thread() {
                    @Override
                    public void run() {
                        keepGoing = true;
                        beginCompetition();
                    }
                };
                battleThread.start();
            }
        } // Stop the competition
        else if (event.getSource() == stopButton) {
            keepGoing = false;
        } // Update the number of rounds that the captains should battle
        else if (event.getSource() == iterations) {
            halfNumberOfMatches = Integer.parseInt((String) iterations.getSelectedItem()) / 2;
        } else if (event.getSource() == opponentCombo) {
            if (table.getSelectedRow() >= 0) {
                String captainName = (String) table.getValueAt(table.getSelectedRow(), BattleshipTableModel.NAME_COLUMN_INDEX);
                updateStatsText(captainName);
            }
        }
    }

    /**
     * Format the table columns to look nice and neat (adjusts width of name
     * column and enabled column).
     */
    protected void fixTableColumns() {
        // Disable column dragging
        table.getTableHeader().setReorderingAllowed(false);

        // Set the 'enabled' column to not be sortable
        ((TableRowSorter<?>) table.getRowSorter()).setSortable(BattleshipTableModel.ENABLED_COLUMN_INDEX, false);

        // Add the check-all header check box
        TableColumn tc = table.getColumnModel().getColumn(BattleshipTableModel.ENABLED_COLUMN_INDEX);
        tc.setCellEditor(table.getDefaultEditor(Boolean.class));
        tc.setCellRenderer(table.getDefaultRenderer(Boolean.class));
        tc.setHeaderRenderer(new CheckBoxHeader(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Object source = e.getSource();
                if (source instanceof AbstractButton == false) {
                    return;
                }
                boolean checked = e.getStateChange() == ItemEvent.SELECTED;

                for (int x = 0, y = table.getRowCount(); x < y; x++) {
                    table.setValueAt(checked, x, BattleshipTableModel.ENABLED_COLUMN_INDEX);
                }
            }
        }));

        tableCheckboxHeader = (CheckBoxHeader) tc.getHeaderRenderer();

        // Shrink the enabled column
        table.getColumnModel().getColumn(BattleshipTableModel.ENABLED_COLUMN_INDEX).setMinWidth(20);
        table.getColumnModel().getColumn(BattleshipTableModel.ENABLED_COLUMN_INDEX).setMaxWidth(20);

        // Find maximum width needed for the name column
        int maxWidth = 0;
        for (int row = 0; row < table.getRowCount(); row++) {
            TableCellRenderer renderer = table.getCellRenderer(row, BattleshipTableModel.NAME_COLUMN_INDEX);
            Component comp = table.prepareRenderer(renderer, row, BattleshipTableModel.NAME_COLUMN_INDEX);
            maxWidth = Math.max(comp.getPreferredSize().width, maxWidth);
        }

        // Set a fixed width for the name column
        table.getColumnModel().getColumn(BattleshipTableModel.NAME_COLUMN_INDEX).setMinWidth(maxWidth);
        table.getColumnModel().getColumn(BattleshipTableModel.NAME_COLUMN_INDEX).setMaxWidth(maxWidth);
    }

    /**
     * <p>Rebuild the battleshipTableModel using the current directory. This
     * achieves many things: <ul> <li>Rescans the indicated directory for
     * captain classes and rebuilds the list.</li> <li>Resets all scores and
     * enabled status to zero and enabled respectively.</li> <li>Rebuilds the
     * opponent combo box box with the latest list of names.</li> <li>Clears the
     * current heat maps back to being blank.</li> </ul></p>
     */
    protected void remakeBattleshipTableModel() {
        battleModel = new BattleshipTableModel(currentPath);
        table.setModel(battleModel);
        fixTableColumns();

        ArrayList<String> captainNames = battleModel.getCaptainNames();
        opponentCombo.removeAllItems();
        for (String s : captainNames) {
            opponentCombo.addItem(s);
        }

        updateHeatCharts(null);
    }

    /**
     * Update the heat maps to display the statistics for a particular captain
     * specified by captainName. If captainName is null or no statistics are
     * available for that captain the charts will be cleared to be empty.
     *
     * @param captainName The name of the captain whose statistics you want to
     * visualize in the heat maps.
     */
    protected void updateHeatCharts(String captainName) {
        if (captainName == null || !detailedRecords.containsKey(captainName)) {
            // Make blank heat maps
            placementHeatChart.setZValues(BLANK_DATA);
            placementHeatChart.setLowValueColour(blankColor);
            placementHeatChart.setHighValueColour(blankColor);

            attackHeatChart.setZValues(BLANK_DATA);
            attackHeatChart.setLowValueColour(blankColor);
            attackHeatChart.setHighValueColour(blankColor);

        } else {
            // Get this captain's statistics
            CaptainStatistics stats = detailedRecords.get(captainName);

            // Make real heat maps from the statistics data
            placementHeatChart.setZValues(stats.getShipPlacement());
            placementHeatChart.setLowValueColour(lowColor);
            placementHeatChart.setHighValueColour(highColor);

            attackHeatChart.setZValues(stats.getAttackPattern());
            attackHeatChart.setLowValueColour(lowColor);
            attackHeatChart.setHighValueColour(highColor);
        }

        // Update the heat map labels with the images we just generated
        placementHeatMapLabel.setIcon(new ImageIcon(placementHeatChart.getChartImage()));
        attackHeatMapLabel.setIcon(new ImageIcon(attackHeatChart.getChartImage()));
    }

    /**
     * Update the statistics text for a particular captain specified by
     * captainName. If captainName is null or no statistics are available for
     * that captain it is assumed that the competition has not been run yet and
     * a suitable message is displayed. If the current opponent cannot be found
     * in the statistics then "---" is displayed. This can happen if the
     * opponent was disabled or if the captainName and the opponent are
     * currently equal.
     *
     * @param captainName The name of the captain whose statistics you want to
     * display as text.
     */
    protected void updateStatsText(String captainName) {
        if (captainName == null || !detailedRecords.containsKey(captainName)) {
            statsText.setText("Please run the competition first");
        } else {
            CaptainStatistics stats = detailedRecords.get(captainName);
            String opponentName = (String) opponentCombo.getSelectedItem();

            String statsResult = stats.getResultsAgainst(opponentName);
            if (statsResult == null) {
                statsText.setText("----");
            } else {
                statsText.setText(statsResult);
            }
        }
    }

    /**
     * Change the 'state' of the GUI to BATTLE_MODE. In this state the GUI
     * elements are disabled and the stop button is enabled. The user cannot
     * interact with anything except the table and the stop button.
     */
    protected void prepareForCompetition() {
        // Check the current mode and update it
        if (currentMode == GUIMode.BATTLE_MODE) {
            return;
        }
        currentMode = GUIMode.BATTLE_MODE;
        if (battleModel != null) {
            battleModel.setCurrentMode(currentMode);
        }
        if (tableCheckboxHeader != null) {
            tableCheckboxHeader.setCurrentMode(currentMode);
        }

        // Erase the previously displayed heat maps
        updateHeatCharts(null);

        // Toggle the GUI elements to be disabled while the competition is running
        getPath.setEnabled(false);
        resetDataButton.setEnabled(false);
        runCompetition.setEnabled(false);
        iterations.setEnabled(false);
        stopButton.setEnabled(true);
        opponentCombo.setEnabled(false);

        // Initialize progress to 0
        totalProgressBar.setValue(0);
        currentProgressBar.setValue(0);

        // Set the table to automatically update the sorting as the model changes
        ((TableRowSorter<?>) table.getRowSorter()).setSortsOnUpdates(true);
    }

    /**
     * Change the 'state' of the GUI to NORMAL_MODE. In this state all elements
     * (except the stop button) are enabled and interaction is 'normal'.
     */
    protected void endCompetition() {
        // Check the current mode and update it
        if (currentMode == GUIMode.NORMAL_MODE) {
            return;
        }
        currentMode = GUIMode.NORMAL_MODE;
        if (battleModel != null) {
            battleModel.setCurrentMode(currentMode);
        }
        if (tableCheckboxHeader != null) {
            tableCheckboxHeader.setCurrentMode(currentMode);
        }

        // Re-enable the appropriate elements and buttons
        getPath.setEnabled(true);
        resetDataButton.setEnabled(true);
        runCompetition.setEnabled(true);
        iterations.setEnabled(true);
        stopButton.setEnabled(false);
        opponentCombo.setEnabled(true);

        this.hasRun = true;
        // Reset progress back to 0
        totalProgressBar.setValue(0);
        currentProgressBar.setValue(0);

        // Turn-off auto sorting of the table
        ((TableRowSorter<?>) table.getRowSorter()).setSortsOnUpdates(false);
    }

    /**
     * Run a competition between all the currently enabled captains. This method
     * can take a long time and will block until it has completed or until it is
     * canceled (by setting keepGoing to false). To keep the user interface
     * responsive and allow the system to change keepGoing it should be run in a
     * separate thread. Note that it will update the progress bars in the GUI
     * and as such needs direct access to the GUI. <p> Conceptually this method
     * belongs in the 'Model' side of the model-view system. We have chosen to
     * keep it here in the 'view' because it needs direct access to the progress
     * bars and it would be considerably more complicated to make this access
     * indirect.
     */
    protected void beginCompetition() {

        // Get the captain classes (if this was in the 'model' side we would not need to do this)
        ArrayList<Captain> captainClasses = ((BattleshipTableModel) table.getModel()).getCaptainClasses();

        // Check the number of enabled captains
        numCaptains = captainClasses.size() - battleModel.getDisabledCount();
        if (numCaptains < 2) {
            JOptionPane.showMessageDialog(this,
                    "You need at least two captains to do battle. Either browse for a folder containing more capatain classes or enable at least two captains.",
                    "Not Enough Captains", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Compute the number of total rounds for keeping track of progress
        int numberofrounds = numCaptains * numCaptains - numCaptains;

        // Enter the BATTLE_MODE GUI state
        prepareForCompetition();

        // Have every captain play every other captain twice
        // Once as first player and once as second player
        int counter = 0;
        for (int i = 0; i < captainClasses.size(); i++) {
            for (int j = i + 1; j < captainClasses.size(); j++) {
                // Do a battle and count all the successful ones
                if (battleCaptains(captainClasses.get(i), captainClasses.get(j))) {
                    counter++;
                }

                // Stop if the user has requested it
                if (!keepGoing) {
                    endCompetition();
                    return;
                }

                // Update the progress bar
                totalProgressBar.setValue(2 * (100 * counter) / numberofrounds);
            }
        }

        // Print out all the detailed statistics to the console
        for (Captain c : captainClasses) {
            if (battleModel.isCaptainEnabled(c.getClass().getName())) {
                detailedRecords.get(c.getClass().getName()).outputStatistics();
            }
        }

        // Return to the NORMAL_MODE GUI state
        endCompetition();
    }

    /**
     * Battle the two captains for halfNumberOfMatches rounds. captainone gets
     * to get first and captaintwo goes second. Later this order will be
     * reversed and they will battle again for halfNumberOfMatches. <p>
     * Conceptually this method belongs in the model side of the model-view
     * paradign (just like beginCompetition) but again it needs direct access to
     * the progress bars and so we keep it here to avoid overly complex code.
     *
     * @param captainone The first captain in the battle (an instance).
     * @param captaintwo The second captain in the battle (also an instance).
     * @return True if the battle ended successfully, false if it was abandoned
     * for some reason.
     */
    protected boolean battleCaptains(Captain captainone, Captain captaintwo) {
        // reset the progress bar
        currentProgressBar.setValue(0);

        // Get the names of the two captains
        String nameOne = BattleshipTableModel.nameNoPackage(captainone.getClass());
        String nameTwo = BattleshipTableModel.nameNoPackage(captaintwo.getClass());

        // Remember their scores (how many matches they have won).
        int scoreOne = 0, scoreTwo = 0;

        // Add them to the detailed records if they haven't been put there already
        if (!detailedRecords.containsKey(nameOne)) {
            detailedRecords.put(nameOne, new CaptainStatistics(nameOne));
        }
        if (!detailedRecords.containsKey(nameTwo)) {
            detailedRecords.put(nameTwo, new CaptainStatistics(nameTwo));
        }

        // Skip this battle if either captain is disabled
        if (!battleModel.isCaptainEnabled(nameOne) || !battleModel.isCaptainEnabled(nameTwo)) {
            return false;
        }

        int[][] startingOneAtk = detailedRecords.get(nameOne).getAttackPattern();
        int[][] startingOnePlc = detailedRecords.get(nameOne).getShipPlacement();

        int[][] startingTwoAtk = detailedRecords.get(nameTwo).getAttackPattern();
        int[][] startingTwoPlc = detailedRecords.get(nameTwo).getShipPlacement();

        for (int i = 0; i < 2 * halfNumberOfMatches; i++) {
            // Initialize the first captain and his fleet
            captainone.initialize(2 * halfNumberOfMatches, numCaptains, nameTwo);

            // Record his ship placement choices
            Fleet fleetone = captainone.getFleet();
            int[][] shipLocs = new int[10][10];
            for (int j = 0; j < 10; j++) {
                for (int k = 0; k < 10; k++) {
                    if (fleetone.isShipAt(new Coordinate(j, k))) {
                        shipLocs[j][k]++;
                    }
                }
            }

            // Add these ship placement choices to the statistics against this particular opponent
            detailedRecords.get(nameOne).addNewGame(shipLocs, nameTwo);

            // Initialize the second captain and her fleet
            captaintwo.initialize(2 * halfNumberOfMatches, numCaptains, nameOne);

            // Record her ship placement choices
            Fleet fleettwo = captaintwo.getFleet();
            shipLocs = new int[10][10];
            for (int j = 0; j < 10; j++) {
                for (int k = 0; k < 10; k++) {
                    if (fleettwo.isShipAt(new Coordinate(j, k))) {
                        shipLocs[j][k]++;
                    }
                }
            }

            // Add these ship placement choices to the statistics against this particular opponent
            detailedRecords.get(nameTwo).addNewGame(shipLocs, nameOne);

            if (i % 2 == 0) {
                // While the user has not requested that we stop ...
                int rounds = 0;
                while (keepGoing) {
                    // Run and keep track of rounds during this match
                    rounds++;

                    // Captain one goes first
                    Coordinate attackonecoord = captainone.makeAttack();           // Ask captain one for his move
                    int attackone = fleettwo.attacked(attackonecoord);                   // Determine the result of that move
                    captainone.resultOfAttack(fleettwo.getLastAttackValue());                                                                    // Inform captain one of the result
                    captaintwo.opponentAttack(attackonecoord);                                                              // Inform captain two of the result

                    // Did captain one win?
                    if (attackone == DEFEATED) {

                        // Give captain one a point
                        scoreOne++;

                        // Record the move
                        detailedRecords.get(nameOne).addRound(nameTwo, true, attackonecoord);

                        // Record the results of the match
                        detailedRecords.get(nameOne).addFinishedGame(nameTwo, true, rounds);
                        detailedRecords.get(nameTwo).addFinishedGame(nameOne, false, rounds);

                        // Tell them the results of this match
                        captainone.resultOfGame(WON);
                        captaintwo.resultOfGame(LOST);

                        // Stop the match
                        break;
                    }

                    // Captain two goes second
                    Coordinate attacktwocoord = captaintwo.makeAttack();           // Ask captain two for her move
                    int attacktwo = fleetone.attacked(attacktwocoord);                   // Determine the result of that move
                    captaintwo.resultOfAttack(fleetone.getLastAttackValue());                                                                    // Inform captain two of the result
                    captainone.opponentAttack(attacktwocoord);                                                              // Inform captain one of the result

                    // Did captain two win?
                    if (attacktwo == DEFEATED) {

                        // Give captain two a point
                        scoreTwo++;

                        // Record the move
                        detailedRecords.get(nameTwo).addRound(nameOne, true, attacktwocoord);

                        // Record the results of the match
                        detailedRecords.get(nameTwo).addFinishedGame(nameOne, true, rounds);
                        detailedRecords.get(nameOne).addFinishedGame(nameTwo, false, rounds);


                        // Tell them the results of this match
                        captaintwo.resultOfGame(WON);
                        captainone.resultOfGame(LOST);

                        // Stop the match
                        break;
                    }


                    // Was the result of either attack a hit?
                    boolean oneHit = (attackone / HIT_MODIFIER == 1 || attackone / HIT_MODIFIER == 2);
                    boolean twoHit = (attacktwo / HIT_MODIFIER == 1 || attacktwo / HIT_MODIFIER == 2);

                    // Record these two moves in the statistics
                    detailedRecords.get(nameOne).addRound(nameTwo, oneHit, attackonecoord);
                    detailedRecords.get(nameTwo).addRound(nameOne, twoHit, attacktwocoord);
                }
            } else {
                // While the user has not requested that we stop ...
                int rounds = 0;
                while (keepGoing) {
                    // Run and keep track of rounds during this match
                    rounds++;

                    // Captain two goes first
                    Coordinate attacktwocoord = captaintwo.makeAttack();           // Ask captain two for her move
                    int attacktwo = fleetone.attacked(attacktwocoord);                   // Determine the result of that move
                    captaintwo.resultOfAttack(fleetone.getLastAttackValue());                                                                    // Inform captain two of the result
                    captainone.opponentAttack(attacktwocoord);                                                              // Inform captain one of the result

                    // Did captain two win?
                    if (attacktwo == DEFEATED) {

                        // Give captain two a point
                        scoreTwo++;

                        // Record the move
                        detailedRecords.get(nameTwo).addRound(nameOne, true, attacktwocoord);

                        // Record the results of the match
                        detailedRecords.get(nameTwo).addFinishedGame(nameOne, true, rounds);
                        detailedRecords.get(nameOne).addFinishedGame(nameTwo, false, rounds);


                        // Tell them the results of this match
                        captaintwo.resultOfGame(WON);
                        captainone.resultOfGame(LOST);

                        // Stop the match
                        break;
                    }

                    // Captain one goes second
                    Coordinate attackonecoord = captainone.makeAttack();           // Ask captain one for his move
                    int attackone = fleettwo.attacked(attackonecoord);                   // Determine the result of that move
                    captainone.resultOfAttack(fleettwo.getLastAttackValue());                                                                    // Inform captain one of the result
                    captaintwo.opponentAttack(attackonecoord);                                                              // Inform captain two of the result

                    // Did captain one win?
                    if (attackone == DEFEATED) {

                        // Give captain one a point
                        scoreOne++;

                        // Record the move
                        detailedRecords.get(nameOne).addRound(nameTwo, true, attackonecoord);

                        // Record the results of the match
                        detailedRecords.get(nameOne).addFinishedGame(nameTwo, true, rounds);
                        detailedRecords.get(nameTwo).addFinishedGame(nameOne, false, rounds);

                        // Tell them the results of this match
                        captainone.resultOfGame(WON);
                        captaintwo.resultOfGame(LOST);

                        // Stop the match
                        break;
                    }

                    // Was the result of either attack a hit?
                    boolean oneHit = (attackone / HIT_MODIFIER == 1 || attackone / HIT_MODIFIER == 2);
                    boolean twoHit = (attacktwo / HIT_MODIFIER == 1 || attacktwo / HIT_MODIFIER == 2);

                    // Record these two moves in the statistics
                    detailedRecords.get(nameOne).addRound(nameTwo, oneHit, attackonecoord);
                    detailedRecords.get(nameTwo).addRound(nameOne, twoHit, attacktwocoord);
                }
            }


            // Has the user requested that we stop?
            if (!keepGoing) {
                return false;
            }

            // update the progress bar
            currentProgressBar.setValue((100 * (i + 1)) / (2 * halfNumberOfMatches));

            if (i % 500 == 0) {
                battleModel.addCaptainScore(nameOne, scoreOne);
                battleModel.addCaptainScore(nameTwo, scoreTwo);
                table.repaint();
                scoreOne = 0;
                scoreTwo = 0;
            }

            if ((i + 1) % (halfNumberOfMatches * .02) == 0) {
                detailedRecords.get(nameOne).addSample(nameTwo, startingOneAtk, startingOnePlc);
                detailedRecords.get(nameTwo).addSample(nameOne, startingTwoAtk, startingTwoPlc);
            }

        }

        // Update the score model and repaint the table
        battleModel.addCaptainScore(nameOne, scoreOne);
        battleModel.addCaptainScore(nameTwo, scoreTwo);
        table.repaint();

        // Battle was successful!
        return true;
    }
}

/**
* A special class for having a 'check all' checkbox in the header of a JTable.
* This comes from a post on coderanch.com by Michael Dunn (see link below). The
* original source this was based on is no longer available. It was updated and
* reworked for our purposes by Seth Berrier.
*
* @author Michael Dunn
* @author Seth Berrier - berriers@uwstout.edu
* @see <a
* href="http://www.coderanch.com/t/343795/GUI/java/Check-Box-JTable-header">Source
* at www.coderanch.com</a>
*/
class CheckBoxHeader extends JCheckBox implements TableCellRenderer, MouseListener {

    // Because we implement serializable
    private static final long serialVersionUID = 1;
    protected CheckBoxHeader rendererComponent;
    protected int column;
    protected boolean mousePressed = false;
    protected Battleship.GUIMode currentMode = Battleship.GUIMode.NORMAL_MODE;

    public CheckBoxHeader(ItemListener itemListener) {
        rendererComponent = this;
        rendererComponent.addItemListener(itemListener);
        setSelected(true);
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                rendererComponent.setForeground(header.getForeground());
                rendererComponent.setBackground(header.getBackground());
                rendererComponent.setFont(header.getFont());
                header.addMouseListener(rendererComponent);
            }
        }
        setColumn(column);
        rendererComponent.setToolTipText("Check/Uncheck All");
        rendererComponent.setAlignmentX(CENTER_ALIGNMENT);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        return rendererComponent;
    }

    public void setCurrentMode(Battleship.GUIMode currentMode) {
        this.currentMode = currentMode;
    }

    protected void setColumn(int column) {
        this.column = column;
    }

    public int getColumn() {
        return column;
    }

    protected void handleClickEvent(MouseEvent e) {
        if (mousePressed) {
            mousePressed = false;
            JTableHeader header = (JTableHeader) (e.getSource());
            JTable tableView = header.getTable();
            TableColumnModel columnModel = tableView.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            int column = tableView.convertColumnIndexToModel(viewColumn);

            if (currentMode != Battleship.GUIMode.BATTLE_MODE
                    && viewColumn == this.column && e.getClickCount() == 1 && column != -1) {
                doClick();
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        handleClickEvent(e);
        ((JTableHeader) e.getSource()).repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mousePressed = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
