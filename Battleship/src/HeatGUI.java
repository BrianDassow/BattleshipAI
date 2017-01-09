
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class HeatGUI extends JFrame {

    private ArrayList<ArrayList<int[][]>> heatSample;
    private ArrayList<String> names;
    private JLabel title, placementHeatMapLabel, attackHeatMapLabel;
    private JComboBox<String> versus;
    private JSlider atk, def;
    private HeatChart placementHeatChart;
    private HeatChart attackHeatChart;
    private JPanel heatMapPanel;
    /**
     * The labels for the heat-map X-axis
     */
    protected static final String[] X_AXIS_VALS = {"0  ", "1  ", "2  ", "3  ", "4  ", "5  ", "6  ", "7  ", "8  ", "9  "};
    /**
     * The labels for the heat-map Y-axis
     */
    protected static final String[] Y_AXIS_VALS = {"9  ", "8  ", "7  ", "6  ", "5  ", "4  ", "3  ", "2  ", "1  ", "0  "};
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

    public HeatGUI(ArrayList<ArrayList<int[][]>> heatSample, ArrayList<String> names, String captainName) {
        super("Extended Heatmap for " + captainName);

        this.heatSample = heatSample;
        this.names = names;

        Container container = getContentPane();
        GridBagLayout layout = new GridBagLayout();
        int[] widths = {450, 450};
        int[] heights = {40, 30, 430, 50};
        layout.columnWidths = widths;
        layout.rowHeights = heights;
        container.setLayout(layout);

        //title
        this.title = new JLabel("Extended Heatmap for " + captainName);
        this.title.setFont(this.title.getFont().deriveFont((float) 25));

        GridBagConstraints tgb = new GridBagConstraints();
        tgb.gridwidth = 2;
        tgb.gridx = 0;
        tgb.gridy = 0;
        tgb.anchor = GridBagConstraints.CENTER;

        container.add(title, tgb);

        //Opponent Spinbox
        this.versus = new JComboBox<String>();
        for (String str : this.names) {
            this.versus.addItem(str);
        }
        this.versus.setSize(100, 30);
        this.versus.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent arg0) {
                opponentChanged();

            }
        });
        GridBagConstraints cbc = new GridBagConstraints();
        cbc.gridwidth = 2;
        cbc.gridx = 0;
        cbc.gridy = 1;
        cbc.anchor = GridBagConstraints.CENTER;

        container.add(this.versus, cbc);

        // Setup the heatmap panel
        placementHeatChart = new HeatChart(new double[10][10], 0.0, 0.0);
        placementHeatChart.setCellSize(new Dimension(40, 40));
        placementHeatChart.setXValues(X_AXIS_VALS);
        placementHeatChart.setYValues(Y_AXIS_VALS);
        placementHeatChart.setHighValueColour(blankColor);
        placementHeatChart.setLowValueColour(blankColor);

        attackHeatChart = new HeatChart(new double[10][10], 0.0, 0.0);
        attackHeatChart.setCellSize(new Dimension(40, 40));
        attackHeatChart.setXValues(X_AXIS_VALS);
        attackHeatChart.setYValues(Y_AXIS_VALS);
        attackHeatChart.setHighValueColour(blankColor);
        attackHeatChart.setLowValueColour(blankColor);

        heatMapPanel = new JPanel(new GridBagLayout());
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

        //panel constraints
        GridBagConstraints panc = new GridBagConstraints();
        panc.gridwidth = 2;
        panc.gridx = 0;
        panc.gridy = 2;

        container.add(heatMapPanel, panc);

        //Sliders for selection
        this.atk = new JSlider(SwingConstants.HORIZONTAL);
        this.atk.setMaximum(99);
        this.def = new JSlider(SwingConstants.HORIZONTAL);
        this.def.setMaximum(99);

        ChangeListener cl = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                opponentChanged();

            }
        };

        this.atk.addChangeListener(cl);
        this.atk.setSize(200, 50);
        this.def.addChangeListener(cl);
        this.def.setSize(200, 50);

        GridBagConstraints slider = new GridBagConstraints();
        slider.gridy = 3;
        slider.gridx = 1;
        slider.anchor = GridBagConstraints.CENTER;

        container.add(this.atk, slider);

        slider.gridx = 0;

        container.add(this.def, slider);


        pack();
        setSize(1000, 650);
        setVisible(true);
        setLocationRelativeTo(null);
        opponentChanged();

    }

    protected void opponentChanged() {

        int[][] placement = new int[10][10];
        int[][] attack = new int[10][10];

        placement = this.heatSample.get(this.versus.getSelectedIndex()).get(this.def.getValue() + 100);
        attack = this.heatSample.get(this.versus.getSelectedIndex()).get(this.atk.getValue());

        // Make real heat maps from the statistics data
        placementHeatChart.setZValues(placement);
        placementHeatChart.setLowValueColour(lowColor);
        placementHeatChart.setHighValueColour(highColor);

        attackHeatChart.setZValues(attack);
        attackHeatChart.setLowValueColour(lowColor);
        attackHeatChart.setHighValueColour(highColor);


        // Update the heat map labels with the images we just generated
        placementHeatMapLabel.setIcon(new ImageIcon(placementHeatChart.getChartImage()));
        attackHeatMapLabel.setIcon(new ImageIcon(attackHeatChart.getChartImage()));

    }
}
