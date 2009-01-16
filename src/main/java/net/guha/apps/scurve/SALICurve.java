package net.guha.apps.scurve;

import net.claribole.zgrviewer.ConfigManager;
import net.guha.apps.SALIPair;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

/**
 * @author Rajarshi Guha
 */
public class SALICurve extends JFrame {

    JTabbedPane tabs;
    DataPane dataPane;
    JPanel type1Pane;
    JPanel type2Pane;

    JFrame myThis;
    boolean columnAdded = false;

    public SALICurve(Hashtable chemicalData) {
        setTitle("SALI Curve Viewer");
        setName("salicurvewindow");
        myThis = this;


        tabs = new JTabbedPane();
        dataPane = new DataPane(chemicalData);
        type1Pane = new JPanel();
        type2Pane = new JPanel();
        type2Pane.setEnabled(false);
        tabs.addTab("Chemical Data", dataPane);
        tabs.addTab("Type 1 SALI Curve", type1Pane);
        tabs.addTab("Type 2 SALI Curve", type2Pane);


        JPanel buttonPanel = new JPanel(new FlowLayout());
        final JButton closeButton = new JButton("Close window");
        final JButton plotButton = new JButton("Plot SALI curves");
        final JButton addButton = new JButton("Add column");
        buttonPanel.add(plotButton);
        buttonPanel.add(addButton);
        buttonPanel.add(closeButton);
        ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == closeButton) {
                    myThis.dispose();
                } else if (e.getSource() == plotButton) {
                    generateSaliCurve();
                } else if (e.getSource() == addButton) {
                    JTable table = dataPane.getTable();

                    if (!columnAdded) {
                        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                        columnAdded = true;
                    }

                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    TableColumn col = new TableColumn(model.getColumnCount());
                    col.setHeaderValue("Value " + (model.getColumnCount() + 1 - 3));
                    table.addColumn(col);
                    model.addColumn("Value " + (model.getColumnCount() + 1 - 3));
                }
            }
        };
        closeButton.addActionListener(actionListener);
        plotButton.addActionListener(actionListener);
        addButton.addActionListener(actionListener);
        addButton.setEnabled(false);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        setSize(550, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void generateSaliCurve() {
        JTable table = dataPane.getTable();
        int selectedColumn = dataPane.getSelectedColumnIndex();
        if (selectedColumn <= 2) {
            JOptionPane.showMessageDialog(this,
                    "Must select a column representing the predicted values",
                    "SALI Curve Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (ConfigManager.saliPairs == null) {
            JOptionPane.showMessageDialog(this,
                    "Generate the SALI network before SALI curves",
                    "SALI Curve Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // lets pull out the data - we take the names so that we
        // can access SALI values from the hash table storing the
        // chemical data
        int nrow = table.getRowCount();
        double[] predicted = new double[nrow];
        double[] observed = new double[nrow];
        String[] names = new String[nrow];
        for (int i = 0; i < nrow; i++) {
            Object predictedValue = table.getValueAt(i, selectedColumn);
            if (predictedValue == null) {
                JOptionPane.showMessageDialog(this,
                        "Missing data in the predicted column\nSelect a more appropriate column!",
                        "SALI Curve Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            predicted[i] = ((Double) predictedValue).doubleValue();
            observed[i] = (((Double) table.getValueAt(i, 2))).doubleValue();
            names[i] = ((String) table.getValueAt(i, 0));
        }

        // find the max and min SALI values
        double maxSali = Double.MIN_VALUE;
        double minSali = Double.MAX_VALUE;
        for (int i = 0; i < ConfigManager.saliPairs.length; i++) {
            if (ConfigManager.saliPairs[i].getSali() > maxSali) {
                maxSali = ConfigManager.saliPairs[i].getSali();
            }
            if (ConfigManager.saliPairs[i].getSali() < minSali) {
                minSali = ConfigManager.saliPairs[i].getSali();
            }
        }

        // lets get the SALI cutoff values
        double[] cutoffs = new double[101];

        for (int i = 0; i < 100; i++) {
            cutoffs[i] = 0 + 0.01 * (double) i;
        }

        // lets first do the Type 2 curve - just find out how many
        // edges there are in the graph for a given cutoff
        double[] type2vals = new double[cutoffs.length];
        for (int i = 0; i < cutoffs.length; i++) {
            double cutvalue = minSali + cutoffs[i] * (maxSali - minSali);
            type2vals[i] = 0;
            for (int j = 0; j < ConfigManager.saliPairs.length; j++) {
                if (ConfigManager.saliPairs[j].getSali() >= cutvalue) type2vals[i] += 1;
            }
        }

        // now we do the type 1 sali curve
        double[] type1vals = new double[cutoffs.length];
        for (int i = 0; i < cutoffs.length; i++) {
            double cutvalue = minSali + cutoffs[i] * (maxSali - minSali);
            type1vals[i] = 0;
            int nedge = 0;
            for (int j = 0; j < ConfigManager.saliPairs.length; j++) {
                SALIPair sp = ConfigManager.saliPairs[j];

                if (sp.getSali() < cutvalue) continue;

                String headName = sp.getHead();
                double headAct = sp.getHeadActivity();
                String tailName = sp.getTail();
                double tailAct = sp.getTailActivity();

                double headPred = predicted[getIndexOfName(names, headName)];
                double tailPred = predicted[getIndexOfName(names, tailName)];

                if (headPred == tailPred) continue;
                else if ((headAct > tailAct && headPred > tailPred) ||
                        (headAct < tailAct && headPred < tailPred))
                    type1vals[i] += 1;
                else if ((headAct > tailAct && headPred < tailPred) ||
                        (headAct < tailAct && headPred > tailPred))
                    type1vals[i] -= 1;

                nedge++;
            }
            type1vals[i] = type1vals[i] / (double) nedge;
        }

        // calculate the S_AUC
        double sauc = 0;
        for (int i = 0; i < type1vals.length; i++) sauc += type1vals[i];
        sauc = sauc / type1vals.length;
        sauc = Math.round(sauc * 100.0) / 100.0;

        // OK, lets make the charts. First the type 1
        // then the type 2
        LineChart lineChart = new LineChart(cutoffs, type1vals,
                500, 500,
                new double[]{-1, 1},
                "SALI Curve (Type 1)", "S(X)", "SCI = " + sauc);
        type1Pane.removeAll();
        type1Pane.add(lineChart.getChartPanel(), BorderLayout.CENTER);

        lineChart = new LineChart(cutoffs, type2vals,
                500, 500,
                null,
                "SALI Curve (Type 2)", "Number of Edges", null);
        type2Pane.removeAll();
        type2Pane.add(lineChart.getChartPanel(), BorderLayout.CENTER);

    }

    private int getIndexOfName(String[] names, String name) {
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i])) return (i);
        }
        return (-1);
    }
}
