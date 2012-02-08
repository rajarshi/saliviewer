package net.guha.apps.renderer;

import net.claribole.zgrviewer.ConfigManager;
import org.openscience.cdk.controller.IViewEventRelay;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.RingGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.renderer.visitor.IDrawVisitor;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * A JPanel to display 2D depictions.
 * <p/>
 * Modified version of RenderPanel.java from the jchempaint-primary branch
 * of the CDK
 *
 * @author Rajarshi Guha
 */
public class Renderer2DPanel extends JPanel implements IViewEventRelay {

    private AtomContainerRenderer renderer;

    private boolean isNew;

    private boolean isNewChemModel;
    private boolean shouldPaintFromCache;
    IAtomContainer molecule;
    boolean fitToScreen = true;
    IDrawVisitor drawVisitor;

    String title = "NA";
    double activity = -9999.0;
    DecimalFormat activityFormat = new DecimalFormat("############.00");
    private int preferredWidth;
    private int preferredHeight;


    /**
     * Create an instance of the rendering panel.
     * <p/>
     * This is a simplified constructor that uses defaults for the molecule
     * title and activity. Also it does not allow one to highlight substructures.
     *
     * @param mol molecule to render. Should have 2D coordinates
     * @param x   width of the panel
     * @param y   height of the panel
     */
    public Renderer2DPanel(IAtomContainer mol, int x, int y) {
        this(mol, null, x, y, "NA", -9999.0);
    }

    /**
     * Create an instance of the rendering panel.
     *
     * @param mol      molecule to render. Should have 2D coordinates
     * @param needle   A fragment representing a substructure of the above molecule.
     *                 This substructure will be highlighted in the depiction. If no substructure
     *                 is to be highlighted, then set this to null
     * @param x        width of the panel
     * @param y        height of the panel
     * @param name     The name of the molecule
     * @param activity The activity associated with the molecule
     */
    public Renderer2DPanel(IAtomContainer mol, IAtomContainer needle, int x, int y,
                           String name, double activity) {
        this.title = name;
        this.activity = activity;
        this.molecule = mol;
        this.preferredWidth = x;
        this.preferredHeight = y;

        setPreferredSize(new Dimension(x, y));
        setBackground(Color.WHITE);

        java.util.List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
        generators.add(new BasicSceneGenerator());
        generators.add(new RingGenerator());
        generators.add(new BasicBondGenerator());
        generators.add(new BasicAtomGenerator());

        this.renderer = new AtomContainerRenderer(generators, new AWTFontManager());
        isNew = true;

        if (needle != null) {
            RendererModel model = renderer.getRenderer2DModel();
            model.setExternalSelectedPart(needle);
            RendererModel.ExternalHighlightColor param = model.getParameter(RendererModel.ExternalHighlightColor.class);
            param.setValue(Color.GREEN);
        }
    }

    public Image takeSnapshot() {
        return this.takeSnapshot(this.getBounds());
    }

    public Image takeSnapshot(Rectangle bounds) {
        Image image = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getScreenDevices()[0]
                .getDefaultConfiguration()
                .createCompatibleImage(bounds.width, bounds.height);
        Graphics2D g = (Graphics2D) image.getGraphics();
        super.paint(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        paint(g);
//        this.paintChemModel(g, bounds);
        return image;
    }

//    public void paintChemModel(Graphics2D g, Rectangle screenBounds) {
//
//        IChemModel chemModel = renderer.getRenderer2DModel().get;
//        if (chemModel != null && chemModel.getMoleculeSet() != null) {
//            Rectangle diagramBounds = renderer.calculateScreenBounds(chemModel);
//            if (this.overlaps(screenBounds, diagramBounds)) {
//                Rectangle union = screenBounds.union(diagramBounds);
//                this.setPreferredSize(union.getSize());
//                this.revalidate();
//            }
//            this.paintChemModel(chemModel, g, screenBounds);
//        }
//    }

//    private boolean overlaps(Rectangle screenBounds, Rectangle diagramBounds) {
//        return screenBounds.getMinX() > diagramBounds.getMinX()
//                || screenBounds.getMinY() > diagramBounds.getMinY()
//                || screenBounds.getMaxX() < diagramBounds.getMaxX()
//                || screenBounds.getMaxY() < diagramBounds.getMaxY();
//    }
//
//
//    private void paintChemModel(IChemModel chemModel, Graphics2D g, Rectangle bounds) {
//        drawVisitor = new AWTDrawVisitor(g);
//        renderer.paintChemModel(chemModel, drawVisitor, bounds, isNewChemModel);
//        isNewChemModel = false;
//
//        /*
//         * This is dangerous, but necessary to allow fast
//         * repainting when scrolling the canvas.
//         *
//         * I set this to false, but the original code has it set to true.
//         * If set to true, then any change in dimensions requires a call to
//         * updateView() - by setting to false, we don't need to call updateView()
//         */
//        this.shouldPaintFromCache = false;
//    }

    public void setIsNewChemModel(boolean isNewChemModel) {
        this.isNew = isNewChemModel;
    }

    public void paint(Graphics g) {
        super.paint(g);

        if (this.isNew) {
            Rectangle drawArea = new Rectangle(0, 0, this.getWidth(), this.getWidth());
            this.renderer.setup(molecule, drawArea);
            this.isNew = false;
            this.renderer.paint(molecule, new AWTDrawVisitor((Graphics2D) g), drawArea, isNew);
        } else {
            Rectangle drawArea = new Rectangle(0, 0, this.getWidth(), this.getHeight());
            this.renderer.setup(molecule, drawArea);
            this.renderer.paint(molecule, new AWTDrawVisitor((Graphics2D) g), drawArea, false);
        }

        annotateFigure(g);
    }


    /**
     * Adds the molecule title and activity to the depiction.
     *
     * @param g The graphics context
     */
    private void annotateFigure(Graphics g) {
        g.setFont(ConfigManager.defaultFont);

        double w = getSize().width;
        double h = getSize().height;

        int paddingX = (int) (w * 0.015);
        int paddingY = (int) (h * 0.015);

        String msg = "Activity = " + activityFormat.format(activity);
        FontMetrics fontMetrics = g.getFontMetrics();
        int ascent = fontMetrics.getMaxAscent();
        int descent = fontMetrics.getMaxDescent();

        // draw the activity
        int xpos = (int) (0 + paddingX);
        int ypos = (int) (h - paddingY);
        g.drawString(msg, xpos, ypos);

        // draw the name
        ypos = (int) (h - paddingY - fontMetrics.getHeight());
        g.drawString(title, xpos, ypos);

    }

    public void updateView() {
        this.shouldPaintFromCache = false;
        this.repaint();
    }
}
