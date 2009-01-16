package net.guha.apps;

import net.claribole.zgrviewer.ConfigManager;
import net.claribole.zgrviewer.DOTManager;
import net.claribole.zgrviewer.GVLoader;
import net.claribole.zgrviewer.GraphicsManager;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.GraphOnlyFingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.nonotify.NNMolecule;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.cdk.smiles.SmilesParser;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Main class that performs SALI calculations.
 * 
 * @author Rajarshi Guha
 */
public class SALI {
    GraphicsManager graphicsManager;
    GVLoader gvLoader;

    JSlider slider;
    boolean sliderVisible = false;
    double currentCutoff = 0.9;

    double maxSali = Double.MIN_VALUE;
    double minSali = Double.MAX_VALUE;
    double dangerSali;

    public SALI(GraphicsManager graphicsManager, GVLoader gvLoader) {
        this.graphicsManager = graphicsManager;
        this.gvLoader = gvLoader;
    }

    public void generateSALIMatrix(Hashtable chemicalData) {
        int nmol = chemicalData.size();

        // convert our hash table to arrays for easy access
        String[] names = new String[nmol];
        String[] smiles = new String[nmol];
        double[] activities = new double[nmol];
        Enumeration keys = chemicalData.keys();
        int counter = 0;
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object[] value = (Object[]) chemicalData.get(key);
            names[counter] = key;
            smiles[counter] = (String) value[0];

            ArrayList values = (ArrayList) value[1];
            activities[counter] = ((Double) values.get(0)).doubleValue();
            counter++;
        }

        ConfigManager.saliPairs = new SALIPair[nmol * (nmol - 1) / 2];

        graphicsManager.gp.setMessage("Evaluating CDK Fingerprints");
        graphicsManager.gp.setProgress(0);
        graphicsManager.gp.setVisible(true);

        SmilesParser sp = new SmilesParser(NoNotificationChemObjectBuilder.getInstance());

        // generate fingerprints
        BitSet[] fingerprints = new BitSet[nmol];
        IFingerprinter fingerPrinter = null;
        if (ConfigManager.fpType.equals("standard"))
            fingerPrinter = new Fingerprinter(ConfigManager.fpLength, ConfigManager.fpDepth);
        else if (ConfigManager.fpType.equals("extended"))
            fingerPrinter = new ExtendedFingerprinter(ConfigManager.fpLength, ConfigManager.fpDepth);
        else if (ConfigManager.fpType.equals("graph"))
            fingerPrinter = new GraphOnlyFingerprinter(ConfigManager.fpLength, ConfigManager.fpDepth);
        else if (ConfigManager.fpType.equals(""))
            fingerPrinter = new Fingerprinter(ConfigManager.fpLength, ConfigManager.fpDepth);

        for (int i = 0; i < nmol; i++) {
            NNMolecule mol = null;
            try {
                mol = (NNMolecule) sp.parseSmiles(smiles[i]);
            } catch (InvalidSmilesException e) {
                displayErrorMessage("Couldn't parse SMILES: " + smiles[i] + "\nAborting SALI calculation");
            }

            try {
                fingerprints[i] = fingerPrinter.getFingerprint(mol);
            } catch (Exception e) {
                displayErrorMessage("Couldn't generate fingerprint for : " + smiles[i] + "\nAborting SALI calculation");
            }
            graphicsManager.gp.setProgress(100 * i / nmol);
        }
        graphicsManager.gp.setProgress(100);
        graphicsManager.gp.setVisible(false);

        graphicsManager.gp.setMessage("Evaluating SALI Matrix");
        graphicsManager.gp.setProgress(0);
        graphicsManager.gp.setVisible(true);
        counter = 0;
        for (int i = 0; i < nmol - 1; i++) {
            for (int j = i + 1; j < nmol; j++) {
                double sim;
                try {
                    sim = Tanimoto.calculate(fingerprints[i], fingerprints[j]);
                } catch (CDKException e) {
                    displayErrorMessage("Error evaluating similarity between\n" + smiles[i] + " and " + smiles[j]);
                    return;
                }

                double salim;
                if (sim == 1.0) salim = -1.0;
                else salim = Math.abs(activities[i] - activities[j]) / (1.0 - sim);

                if (salim > maxSali) maxSali = salim;
                if (salim < minSali) minSali = salim;

                if (activities[i] <= activities[j])
                    ConfigManager.saliPairs[counter++] = new SALIPair(names[i], names[j], activities[i], activities[j], salim, sim);
                else
                    ConfigManager.saliPairs[counter++] = new SALIPair(names[j], names[i], activities[j], activities[i], salim, sim);

                graphicsManager.gp.setProgress(counter * 2 * 100 / (nmol * (nmol - 1)));
            }
        }
        graphicsManager.gp.setProgress(100);
        graphicsManager.gp.setVisible(false);

        // since pairs with a sali of Infinity were marked with -1, we
        // update them to have the max sali value observed
        for (int i = 0; i < ConfigManager.saliPairs.length; i++) {
            if (ConfigManager.saliPairs[i].getSali() == -1)
                ConfigManager.saliPairs[i].setSali(maxSali);
        }

        // now sort the list of pairs so we can do quick lookups later on
        Arrays.sort(ConfigManager.saliPairs);

        // finally, find out at which SALI value, do we have 200 or more edges
        // we use this value to highlight the slider so that the user has some
        // visual indication that when going below that SALI cutoff a very complex
        // network will be generated which may take time
        int n = 0;
        for (int i = ConfigManager.saliPairs.length - 1; i >= 0; i--) {
            if (++n >= 200) {
                dangerSali = ConfigManager.saliPairs[i].getSali();
                break;
            }
        }
        generateNetwork(0.9);
    }

    public void generateNetwork(double cutoff) {
        if (sliderVisible) slider.setEnabled(false);

        if (cutoff < 0 || cutoff > 1) {
            JOptionPane.showMessageDialog(graphicsManager.mainView.getFrame(),
                    "Cutoff must be between 0 and 1", "SALI Network Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        double cutValue = minSali + cutoff * (maxSali - minSali);

        // for the valid pairs, we need to bin the sali values
        // so that we can color the edges based on the differences
        // in the activities.
        ArrayList salilist = new ArrayList();
        for (int i = ConfigManager.saliPairs.length - 1; i >= 0; i--) {
            SALIPair pair = ConfigManager.saliPairs[i];
            if (pair.getSali() > cutValue) salilist.add(new Double(pair.getSali()));
        }
        double localMaxSali = Double.MIN_VALUE;
        double localMinSali = Double.MAX_VALUE;
        for (int i = 0; i < salilist.size(); i++) {
            double v = ((Double) salilist.get(i)).doubleValue();
            if (v > localMaxSali) localMaxSali = v;
            if (v < localMinSali) localMinSali = v;
        }

        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("digraph G {\n");
        int n = 0;
        for (int i = ConfigManager.saliPairs.length - 1; i >= 0; i--) {
            SALIPair saliPair = ConfigManager.saliPairs[i];

            String arrowstyle = "arrowhead=\"normal\"";
            String edgestyle = "style=\"normal\"";

            if (saliPair.getSimilarity() == 1.0) {
                arrowstyle = "arrowhead=\"diamond\"";
                edgestyle = "style=\"dashed\"";
            }

            if (saliPair.getSali() > cutValue) {
                String color = getColor(saliPair.getSali(), localMaxSali, localMinSali);

                if (ConfigManager.smallerIsBetter) {
                    stringBuffer.append(saliPair.getTail()).append(" -> ").append(saliPair.getHead()).
                            append(" [color=\"" + color + "\"," + arrowstyle + "," + edgestyle + "]").
                            append(";\n");
                } else {
                    stringBuffer.append(saliPair.getHead()).append(" -> ").append(saliPair.getTail()).
                            append(" [color=\"" + color + "\"," + arrowstyle + "," + edgestyle + "]").
                            append(";\n");
                }
                n++;
            }
        }
        stringBuffer.append("}");

        File dotfile = new File(ConfigManager.m_TmpDir + "/sali.dot");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(dotfile));
            writer.write(stringBuffer.toString());
            writer.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(graphicsManager.mainView.getFrame(),
                    "Error writing initial network", "SALI Network Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        graphicsManager.reset();
        gvLoader.loadFile(dotfile, DOTManager.DOT_PROGRAM, false);


        double k = 100 * dangerSali / maxSali;
        k = Math.ceil(k / 10) * 10;
        if (!sliderVisible) displayCutoffSlider((int) k);
        else {
            setSliderLabel(slider, (int) k);
            slider.setEnabled(true);
        }
    }

    private void setSliderLabel(JSlider slider, int dangerPercentage) {
        // make the label table
        Hashtable labelTable = new Hashtable();
        for (int i = 0; i <= 100; i += 20)
            labelTable.put(new Integer(i), new JLabel(Integer.toString(i)));
        labelTable.put(new Integer(dangerPercentage),
                new JLabel("<html><bold><font color=\"red\">" +
                        Integer.toString(dangerPercentage) +
                        "</font></bold></html>")
        );
        slider.setLabelTable(labelTable);
    }

    public void displayCutoffSlider(int dangerPercentage) {
        sliderVisible = true;

        final JFrame sliderFrame = new JFrame("SALI Zoomer");
        JPanel sliderPanel = new JPanel(new BorderLayout());
        slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 90);
        setSliderLabel(slider, dangerPercentage);
        slider.addChangeListener(new CutoffChangeListener());
        slider.setMinorTickSpacing(5);
        slider.setMajorTickSpacing(20);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        sliderPanel.add(slider, BorderLayout.CENTER);
        sliderFrame.setContentPane(sliderPanel);
        sliderFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        sliderFrame.pack();

        sliderFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                sliderVisible = false;
                sliderFrame.dispose();
            }
        });
        sliderFrame.setVisible(true);
    }

    private String getColor(double sali, double maxSali, double minSali) {
        double percent;

        if (maxSali == minSali) percent = 100.0;
        else percent = Math.round(sali - minSali) * 100 / (maxSali - minSali);
        if (percent > 100) percent = 100;

        double position = Math.floor((100 - percent) / 10) * 10 / 100;

        int red = (int) (175 * position);
        int blue = (int) (175 * position);
        int green = (int) (175 * position);

        Color color = new Color(red, green, blue);

        if (red == 0 && blue == 0 && green == 0) return "#000000";

        return "#" + Integer.toHexString(color.getRGB() & 0x00ffffff);
    }

    private void displayErrorMessage(String msg) {
        graphicsManager.gp.setProgress(100);
        graphicsManager.gp.setVisible(false);
        JOptionPane.showMessageDialog(graphicsManager.mainView.getFrame(),
                msg, "SALI Calculation Error", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean loadData(Container container) {
        /*
        Data format should be

        smiles name activity  val1 val2 ...

        separator is whitespace
        first line, if it starts with a '#' is a header line
        val1 val2 ... can be any numeric value
        each line must have the sume number of tokens (not checked for)

        We set the chemicalData hash table with
        key => molecule name
        value => Object[] { smiles, list of values }

        List of values will have the observed activity as the first element
         */

        boolean firstLine = true;
        int nfield = 0;
        Pattern pattern = Pattern.compile("^\\d");

        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Assay Data");
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setCurrentDirectory(ConfigManager.m_PrjDir);
        int returnVal = fc.showOpenDialog(container);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File dataFile = fc.getSelectedFile();
            ConfigManager.chemicalData = new Hashtable();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(dataFile));
                String line;
                // format is: title smiles activity
                while ((line = reader.readLine()) != null) {
                    String[] tokens = line.split("\\s+");

                    if (tokens.length < 3) {
                        if (!ConfigManager.skipBadEntries) {
                            ConfigManager.chemicalData = null;
                            return false;
                        }
                        // ok, we're supposed to skip bad entries
                        continue;
                    }

                    if (firstLine) {
                        nfield = tokens.length - 2;
                        firstLine = false;
                    }

                    // before storing the name we want to transform it so
                    // that it doesn't start with a digit or contain any dashes
                    String smiles;
                    String name;
                    String activity;
                    if (ConfigManager.firstColumnIsSmiles) {
                        smiles = tokens[0];
                        name = tokens[1];
                        activity = tokens[2];
                    } else {
                        smiles = tokens[1];
                        name = tokens[2];
                        activity = tokens[2];
                    }
                    name = name.replace("-", "_");
                    if (pattern.matcher(name).find()) {
                        name = "_" + name;
                    }

                    ArrayList values = new ArrayList();
                    values.add(new Double(activity));
                    for (int i = 3; i < tokens.length; i++) values.add(new Double(tokens[i]));

                    Object[] value = new Object[]{smiles, values};
                    ConfigManager.chemicalData.put(name, value);
                }
                reader.close();
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }

        JOptionPane.showMessageDialog(null, "Loaded " + ConfigManager.chemicalData.size() + " molecules with " + nfield + " data fields\n\n" +
                "You can now generate the SALI matrix and network\nfrom the SALI menu option",
                "SALI Viewer: Information", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    class CutoffChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent changeEvent) {
            int value = slider.getValue();
            currentCutoff = value / 100.0;
            generateNetwork(currentCutoff);
        }
    }
}


