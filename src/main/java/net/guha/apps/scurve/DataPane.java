package net.guha.apps.scurve;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * @cdk.author Rajarshi Guha
 * @cdk.svnrev $Revision: 9162 $
 */
public class DataPane extends JPanel {

    JTable table;
    Hashtable chemicalData;
    boolean sortAscending = true;
    int sortColumn = 0;
    int selectedColumn = -1;

    public DataPane(Hashtable chemicalData) {
        this.chemicalData = chemicalData;

        int nrow = chemicalData.size();
        String[] keyArray = getKeys(chemicalData);
        int ncol = 3 + getNumValues(chemicalData, keyArray[0]);
        Object[][] tableData = new Object[nrow][ncol];

        int rowc = 0;
        int nvalues = 0;
        Enumeration keys = chemicalData.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            Object[] value = (Object[]) chemicalData.get(name);
            String smiles = (String) value[0];
            ArrayList values = (ArrayList) value[1];

            tableData[rowc][0] = name;
            tableData[rowc][1] = smiles;
            for (int i = 0; i < values.size(); i++)
                tableData[rowc][2 + i] = values.get(i);
            rowc++;
        }

        Object[] colNames = new Object[ncol];
        colNames[0] = "Name";
        colNames[1] = "SMILES";
        colNames[2] = "Observed";
        for (int i = 3; i < ncol; i++) colNames[i] = "Value " + (i - 2);

        // create the table model from the data. Also, we set the default cell class
        // for any column beyond the first 3 to be Double, since we only expect
        // people to add numbers to new columns
        DefaultTableModel tableModel = new DefaultTableModel(tableData, colNames) {
            public Class getColumnClass(int columnIndex) {
                if (columnIndex > 2) return Double.class;

                Object o = getValueAt(0, columnIndex);
                if (o == null) {
                    return Object.class;
                } else {
                    return o.getClass();
                }
            }
        };
        table = new JTable(tableModel);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(true);

//        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setAutoCreateColumnsFromModel(false);

        sortAllRowsBy(tableModel, sortColumn, sortAscending);

        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setUpdateTableInRealTime(true);
        tableHeader.addMouseListener(new ColumnListener(table));
        tableHeader.setReorderingAllowed(true);

        table.setPreferredScrollableViewportSize(new Dimension(500, 500));
        JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.getViewport().add(table);
        add(scrollPane, BorderLayout.CENTER);

        ExcelAdapter excelAdapter = new ExcelAdapter(table);
    }

    public JTable getTable() {
        return table;
    }

    public int getSelectedColumnIndex() {
        return selectedColumn;
    }

    private void sortAllRowsBy(DefaultTableModel model, int colIndex, boolean ascending) {
        Vector data = model.getDataVector();
        Collections.sort(data, new ColumnSorter(colIndex, ascending));
        model.fireTableStructureChanged();
    }

    private String[] getKeys(Hashtable hash) {
        Enumeration keys = hash.keys();
        String[] keyArray = new String[hash.size()];
        int i = 0;
        while (keys.hasMoreElements()) {
            keyArray[i] = (String) keys.nextElement();
        }
        return keyArray;
    }

    /* Get ths number of numeric values associated with a molecule
    minus 1 (since we know there is always 1 element, viz., the observed activity
    */
    private int getNumValues(Hashtable hash, String key) {
        Object[] value = (Object[]) hash.get(key);
        ArrayList v = (ArrayList) value[1];
        return v.size() - 1;
    }

    // Allow sorting the table by clicking on columns
    class ColumnListener extends MouseAdapter {
        protected JTable table;

        public ColumnListener(JTable t) {
            table = t;
        }

        public void mouseClicked(MouseEvent e) {
            TableColumnModel colModel = table.getColumnModel();
            int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
            int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

            // Single clicks sort the data based on that column
            // double clicks select a column
            if (modelIndex >= 0 && e.getClickCount() == 1) {
//                if (modelIndex == sortColumn) sortAscending = !sortAscending;
//                else sortColumn = modelIndex;
//                sortAllRowsBy((DefaultTableModel) table.getModel(), sortColumn, sortAscending);
            } else if (e.getClickCount() == 2 && modelIndex != -1) {
                table.setColumnSelectionInterval(modelIndex, modelIndex);
                selectedColumn = modelIndex;
            }
        }
    }
}


class ColumnSorter implements Comparator {
    int colIndex;
    boolean ascending;

    ColumnSorter(int colIndex, boolean ascending) {
        this.colIndex = colIndex;
        this.ascending = ascending;
    }

    public int compare(Object a, Object b) {
        Vector v1 = (Vector) a;
        Vector v2 = (Vector) b;
        Object o1 = v1.get(colIndex);
        Object o2 = v2.get(colIndex);

        // Treat empty strains like nulls
        if (o1 instanceof String && ((String) o1).length() == 0) {
            o1 = null;
        }
        if (o2 instanceof String && ((String) o2).length() == 0) {
            o2 = null;
        }

        // Sort nulls so they appear last, regardless
        // of sort order
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return 1;
        } else if (o2 == null) {
            return -1;
        } else if (o1 instanceof Comparable) {
            if (ascending) {
                return ((Comparable) o1).compareTo(o2);
            } else {
                return ((Comparable) o2).compareTo(o1);
            }
        } else {
            if (ascending) {
                return o1.toString().compareTo(o2.toString());
            } else {
                return o2.toString().compareTo(o1.toString());
            }
        }
    }
}


