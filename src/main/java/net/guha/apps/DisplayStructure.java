package net.guha.apps;

import net.claribole.zgrviewer.ConfigManager;
import net.guha.apps.renderer.ViewMolecules2D;
import net.guha.apps.renderer.Renderer2DPanel;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.layout.TemplateHandler;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.vecmath.Vector2d;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Utility methods to display structure diagrams.
 * 
 * Note that ZGRViewer considers the tail of an edge to be the starting
 * point of an arrow and the head of an edge the ending point of an arrow
 * <p/>
 * In this frame, the head will have smaller activity, the tail higher activity
 *
 * @author Rajarshi Guha
 */
public class DisplayStructure {
    static DecimalFormat activityFormat = new DecimalFormat("############.00");
    private static StructureDiagramGenerator sdg = new StructureDiagramGenerator();

    public static void showNodeStructure(String nodeName) {
        showNodeStructure(nodeName, ConfigManager.depictionX, ConfigManager.depictionY);
    }

    public static void showNodeStructure(String nodeName, int width, int height) {

        Object[] nodeData = (Object[]) ConfigManager.chemicalData.get(nodeName);
        if (nodeData == null) return;

        String smiles = (String) nodeData[0];
        ArrayList data = (ArrayList) nodeData[1];
        Double activity = (Double) data.get(0);

        try {
            IAtomContainer molecule = getMoleculeWithCoordinates(smiles);
            Renderer2DPanel rendererPanel = new Renderer2DPanel(molecule, null,
                    ConfigManager.depictionX, ConfigManager.depictionY, false,
                    nodeName, activity.doubleValue());
            JFrame frame = ViewMolecules2D.singleStructurePanel(rendererPanel, width, height);
            frame.setTitle(nodeName);
            frame.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "SMILES parsing error for '" + smiles + "'");

        }
    }

    public static void showEdgeStructures(String headName, String tailName) {
        showEdgeStructures(headName, tailName, ConfigManager.depictionX, ConfigManager.depictionY);
    }

    public static void showEdgeStructures(String headName, String tailName, int width, int height) {

        Object[] headData = (Object[]) ConfigManager.chemicalData.get(headName);
        if (headData == null) return;

        Object[] tailData = (Object[]) ConfigManager.chemicalData.get(tailName);
        if (tailData == null) return;

        String headSmiles = (String) headData[0];
        Double headActivity = (Double) ((ArrayList) headData[1]).get(0);

        String tailSmiles = (String) tailData[0];
        Double tailActivity = (Double) ((ArrayList) tailData[1]).get(0);

        IAtomContainer head = null;
        IAtomContainer tail = null;
        try {
            head = getMoleculeWithCoordinates(headSmiles);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "SMILES parsing or coordinate generation error '" + headSmiles + "'");
        }
        try {
            tail = getMoleculeWithCoordinates(tailSmiles);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "SMILES parsing or coordinate generation error '" + tailSmiles + "'");
        }

        // we want to highlight the differences between the two molecules
        IAtomContainer headNonCommon = null;
        IAtomContainer tailNonCommon = null;

        if (ConfigManager.highlightNonSS) {
            try {
                IAtomContainer mcss = NonCommonSubstructure.getMCSS(head, tail);
                tailNonCommon = NonCommonSubstructure.getNonCommonSubstructure(tail, mcss);
                headNonCommon = NonCommonSubstructure.getNonCommonSubstructure(head, mcss);
            } catch (CDKException e) {
                JOptionPane.showMessageDialog(null, "Error calculating MCSS");
                return;
            }
        }

        Renderer2DPanel headPanel = new Renderer2DPanel(head, headNonCommon, width, height, false, headName, headActivity.doubleValue());
        Renderer2DPanel tailPanel = new Renderer2DPanel(tail, tailNonCommon, width, height, false, tailName, tailActivity.doubleValue());
        Renderer2DPanel[] panels = new Renderer2DPanel[]{tailPanel, headPanel};
        panels[0].setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        panels[1].setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        JFrame frame = ViewMolecules2D.multiStructurePanel(panels, 2, width + 10, height + 10);
        if (frame != null) {
            frame.setTitle(tailName + " (" + activityFormat.format(tailActivity) + ") " +
                    " -> " + headName + " (" + activityFormat.format(headActivity) + ")");
            frame.setVisible(true);
        }
    }

    private static IMolecule getMoleculeWithCoordinates(String smiles) throws Exception {
        IMolecule molecule = ConfigManager.smilesParser.parseSmiles(smiles);
        sdg.setTemplateHandler(new TemplateHandler(DefaultChemObjectBuilder.getInstance()));
        sdg.setMolecule(molecule);
        sdg.generateCoordinates(new Vector2d(0, 1));
        return sdg.getMolecule();
    }
}
