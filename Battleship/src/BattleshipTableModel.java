
import java.io.File;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;

/**
 * <p>The data model for Battleship's captain classes.<p>
 *
 * <p>This is the 'model' side of the model-view paradigm of GUI design for the
 * table in the battleship GUI. You should not need to change anything in this
 * interface to compete in the competition and modifying it may cause the
 * program to stop working!</p>
 *
 * <p>BattleshipTableModel is responsible for the following tasks: <ul>
 * <li>Searching for and loading all the classes that implement captain.</li>
 * <li>Keeping track of the names of the captains.</li> <li>Keeping track of
 * which captains are enabled.</li> <li>Keeping track of how many battles each
 * captain has won.</li> </ul></p>
 *
 * <p>Its primary purpose in managing this information is to provide it to the
 * JTable used in the GUI so that it can display it properly. It extends the
 * DefaultTableModel and overrides several methods provided by the parent so
 * that they are specific to the data it manages (including
 * {@link #isCellEditable(int, int) isCellEditable} and
 * {@link #getColumnClass(int) getColumnClass}).</p>
 *
 * <p>To work with the data provided by this model you should use the constants {@link #NAME_COLUMN_INDEX},
 * {@link #SCORE_COLUMN_INDEX} and {@link #ENABLED_COLUMN_INDEX} to access the
 * proper column. If you want to re-arrange the columns you can simply re-assign
 * these constants but make sure you also change the order of
 * {@link #COLUMN_NAMES} to match the new column indices.</p>
 *
 * @author Seth Dutter - dutters@uwstout.edu
 * @author Seth Berrier - berriers@uwstout.edu
 *
 * @version SPRING.2013
 */
public class BattleshipTableModel extends DefaultTableModel {

    private static final long serialVersionUID = 1;
    /**
     * The names of the columns (must match the indexes set for *_COLUMN_INDEX
     */
    private static final String[] COLUMN_NAMES = {"Enabled", "Captain Name", "Score"};
    /**
     * The index of the names column
     */
    public static final int NAME_COLUMN_INDEX = 1;
    /**
     * The index of the score column
     */
    public static final int SCORE_COLUMN_INDEX = 2;
    /**
     * The index of the enabled column
     */
    public static final int ENABLED_COLUMN_INDEX = 0;
    /**
     * The current mode of the view side of the model-view
     */
    protected Battleship.GUIMode currentMode;
    /**
     * The folder in which to look for captain classes
     */
    protected File pathToCaptainClasses;
    /**
     * The array of captain class instances (The AIs that will do battle)
     */
    protected ArrayList<Captain> captainClasses;
    /**
     * The names of the captain AIs
     */
    protected ArrayList<String> captainNames;

    /**
     * Construct a new BattleshipTableModel that uses the given path to look for
     * captain classes.
     *
     * @param pathToCaptainClasses Where to look for the captain classes.
     */
    BattleshipTableModel(File pathToCaptainClasses) {
        super(COLUMN_NAMES, 0);

        this.pathToCaptainClasses = pathToCaptainClasses;
        captainClasses = new ArrayList<>();
        captainNames = new ArrayList<>();
        currentMode = Battleship.GUIMode.NORMAL_MODE;

        loadCaptains();
    }

    /**
     * Set the current mode of the GUI. This is needed to prevent editing during
     * battle mode.
     *
     * @param currentMode The current mode of the GUI (normal or battle)
     */
    public void setCurrentMode(Battleship.GUIMode currentMode) {
        this.currentMode = currentMode;
    }

    /**
     * Determine if a specific cell is editable. This is used by JTable to know
     * which cells should be allowed to be edited by the user.
     *
     * @param row The row index of the cell you want to check.
     * @param column The column index of the cell you want to check.
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        switch (currentMode) {
            // Only the enabled column can be edited
            case NORMAL_MODE:
                if (column == ENABLED_COLUMN_INDEX) {
                    return true;
                }
                break;

            // No editing allowed when in battle mode
            case BATTLE_MODE:
                return false;
        }

        return false;
    }

    /**
     * Query the type of the specified column (a child of java.lang.Object). If
     * it is a primitive type (like int or boolean) the boxing class will be
     * returned. This is used by the TableRowSorter to properly compare objects
     * in a column for sorting.
     *
     * @param column The index of the column you want to check.
     */
    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case NAME_COLUMN_INDEX:
                return String.class;
            case SCORE_COLUMN_INDEX:
                return Integer.class;
            case ENABLED_COLUMN_INDEX:
                return Boolean.class;
            default:
                return super.getColumnClass(column);
        }
    }

    /**
     * Get the list of instances of all the found captain classes.
     *
     * @return A polymorphic list of instances of all the found captain classes.
     */
    public ArrayList<Captain> getCaptainClasses() {
        return captainClasses;
    }

    /**
     * Get the list of names of all the found captain classes (without the
     * package name or .class)
     *
     * @return A list of Strings that are the names of all the found captain
     * classes.
     */
    public ArrayList<String> getCaptainNames() {
        return captainNames;
    }

    /**
     * How many captains are disabled.
     *
     * @return A count of the number of disabled captains.
     */
    public int getDisabledCount() {
        int disabledCount = 0;
        for (int row = 0; row < getRowCount(); row++) {
            if (!((Boolean) getValueAt(row, ENABLED_COLUMN_INDEX)).booleanValue()) {
                disabledCount++;
            }
        }

        return disabledCount;
    }

    /**
     * Search the table for a particular captain specified by its name. If the
     * captain is found it will return the row index where that captain's data
     * is stored. Otherwise, it throws a CaptainNotFoundException.
     *
     * @param captainName The name of the captain you want to find.
     * @return The row index where that captain is located.
     * @throws CaptainNotFoundException Thrown if the captain is not in the
     * table.
     */
    public int findCaptain(String captainName) throws CaptainNotFoundException {
        for (int row = 0; row < getRowCount(); row++) {
            String name = (String) getValueAt(row, NAME_COLUMN_INDEX);
            if (name.equals(captainName)) {
                return row;
            }
        }

        throw new CaptainNotFoundException(captainName);
    }

    /**
     * Check if the given captain (specified by its name) is enabled or not.
     *
     * @param captainName The name of the captain you want to check.
     * @return True or false indicating if the captain is enabled
     * @throws CaptainNotFoundException Throw if the captain is not in the
     * table.
     */
    public boolean isCaptainEnabled(String captainName) throws CaptainNotFoundException {
        int row = findCaptain(captainName);
        return ((Boolean) getValueAt(row, ENABLED_COLUMN_INDEX)).booleanValue();
    }

    /**
     * Add a new captain to the table with default data (0 for a score and
     * enabled). If the specified captain name is already in the table it is
     * left alone and the row index where it was found is returned.
     *
     * @param captainName The name of the new captain.
     * @return The row where this captain was added.
     */
    public int addCaptain(String captainName) {
        // First, search for the captain name to see if it's already in the table
        try {
            int row = findCaptain(captainName);
            return row;
        } catch (CaptainNotFoundException e) {
            // Add this name to the names list
            captainNames.add(captainName);

            // Build the new data row
            Object[] newData = new Object[3];
            newData[NAME_COLUMN_INDEX] = captainName;
            newData[SCORE_COLUMN_INDEX] = 0;
            newData[ENABLED_COLUMN_INDEX] = true;

            // Add that row and return its index
            addRow(newData);
            return getRowCount() - 1;
        }
    }

    /**
     * Add to the score of a captain (specified by its name).
     *
     * @param captainName The name of the captain whose score you want to
     * increase.
     * @param addScore The amount to add to that captain's score.
     * @throws CaptainNotFoundException Thrown if the captain is not found in
     * the table.
     */
    public void addCaptainScore(String captainName, int addScore) throws CaptainNotFoundException {
        int row = findCaptain(captainName);
        int oldScore = ((Integer) getValueAt(row, SCORE_COLUMN_INDEX)).intValue();
        setValueAt(oldScore + addScore, row, SCORE_COLUMN_INDEX);
    }

    /**
     * Set the score of a captain (specified by its name).
     *
     * @param captainName The name of the captain whose score you want to set.
     * @param newScore The new value of that captain's score.
     * @throws CaptainNotFoundException Thrown if the captain is not found in
     * the table.
     */
    public void setCaptainScore(String captainName, int newScore) throws CaptainNotFoundException {
        int row = findCaptain(captainName);
        setValueAt(newScore, row, SCORE_COLUMN_INDEX);
    }

    /**
     * Search the directory pathToCaptainClasses for all classes that implement
     * Captain. All these classes are loaded and instanced for participation in
     * the competition. The instances are stored in captainClasses and their
     * names are stored in captainNames.
     */
    protected void loadCaptains() {
        ArrayList<Class<?>> allclasses = new ArrayList<>();
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        captainClasses = new ArrayList<>();

        String packageName = "";
        if (Battleship.class.getPackage() != null) {
            packageName = Battleship.class.getPackage().getName() + ".";
        }

        // Load all classes that aren't "Captain.class"
        for (File file : pathToCaptainClasses.listFiles()) {
            String name = packageName + file.getName();
            try {
                if (name.endsWith(".class") && !name.equals("Captain.class")) {
                    allclasses.add(cl.loadClass(name.substring(0, name.length() - 6)));
                }
            } catch (ClassNotFoundException ex) {
                System.out.println(name + "," + ex);
                System.exit(1);
            }
        }

        // Create create instances of all captains that implement the Captain interface
        try {
            for (Class<?> c : allclasses) {
                for (Class<?> interf : c.getInterfaces()) {
                    if (interf.getName().equals(packageName + "Captain")) {
                        captainClasses.add((Captain) c.newInstance());
                        addCaptain(nameNoPackage(c));
                        break;
                    }
                }
            }
        } catch (InstantiationException | IllegalAccessException ex) {
            System.out.println(ex);
            System.exit(1);
        }
    }

    /**
     * An easy helper function to get the name of a class without any package
     * name.
     *
     * @param c The class whose name you want to retrieve.
     * @return The name of the class without any package name on the front of
     * it.
     */
    public static String nameNoPackage(Class<?> c) {
        if (c.getName().lastIndexOf(".") == -1) {
            return c.getName();
        }
        return c.getName().substring(c.getName().lastIndexOf('.') + 1);
    }
}
