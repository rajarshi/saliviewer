package net.guha.apps.test;

import junit.framework.Assert;
import net.claribole.zgrviewer.ConfigManager;
import net.guha.apps.Renderer2DPanel;
import net.guha.apps.ViewMolecules2D;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.layout.TemplateHandler;
import org.openscience.cdk.smiles.SmilesParser;

import javax.swing.*;

/**
 * @cdk.author Rajarshi Guha
 * @cdk.svnrev $Revision: 9162 $
 */
public class DepictionTest {

    SmilesParser smilesParser;
    StructureDiagramGenerator sdg;

    public DepictionTest() {
        smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        sdg = new StructureDiagramGenerator();
        sdg.setTemplateHandler(new TemplateHandler(DefaultChemObjectBuilder.getInstance()));
    }


    public void runCase1() throws Exception {
        String smiles = "c1cc(CC=CC#N)ccn1";
        IAtomContainer molecule = smilesParser.parseSmiles(smiles);
        sdg.setMolecule((IMolecule) molecule);
        sdg.generateCoordinates();
        molecule = sdg.getMolecule();

        Renderer2DPanel rendererPanel = new Renderer2DPanel(molecule, null,
                ConfigManager.depictionX, ConfigManager.depictionY, false,
                "Blah", 1.23);
        rendererPanel.setName("rendererPanel");
        JFrame frame = ViewMolecules2D.singleStructurePanel(rendererPanel, 300, 300);        
        frame.setVisible(true);
        Assert.assertTrue(true);
    }

    public void runCase2() throws Exception {
        String smiles1 = "c1cc(CC=CC#N)ccn1";
        String smiles2 = "C1CCCCC1";
        IAtomContainer molecule1 = smilesParser.parseSmiles(smiles1);
        IAtomContainer molecule2 = smilesParser.parseSmiles(smiles2);

        sdg.setMolecule((IMolecule) molecule1);
        sdg.generateCoordinates();
        molecule1 = sdg.getMolecule();
        sdg.setMolecule((IMolecule) molecule2);
        sdg.generateCoordinates();
        molecule2 = sdg.getMolecule();

        Renderer2DPanel rp1 = new Renderer2DPanel(molecule1, null,
                ConfigManager.depictionX, ConfigManager.depictionY, false,
                "Blah1", 1.23);
        rp1.setFitToScreen(true);

        Renderer2DPanel rp2 = new Renderer2DPanel(molecule2, null,
                ConfigManager.depictionX, ConfigManager.depictionY, false,
                "Blah2", 1.25);
        rp2.setFitToScreen(true);
        
        JFrame frame = ViewMolecules2D.multiStructurePanel(new Renderer2DPanel[]{rp1, rp2},
                2, 200,200);
        frame.setVisible(true);

    }
    public static void main(String[] args) throws Exception {
        DepictionTest dt = new DepictionTest();
        dt.runCase2();

    }

}
