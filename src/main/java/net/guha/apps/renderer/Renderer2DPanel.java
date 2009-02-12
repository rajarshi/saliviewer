package net.guha.apps.renderer;

import net.claribole.zgrviewer.ConfigManager;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.controller.ControllerHub;
import org.openscience.cdk.controller.ControllerModel;
import org.openscience.cdk.controller.IViewEventRelay;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemModel;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.RenderingParameters;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.renderer.visitor.IDrawVisitor;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * A JPanel to display 2D depictions.
 *
 * Modified version of RenderPanel.java from the jchempaint-primary branch
 * of the CDK
 * 
 * @author Rajarshi Guha
 */
public class Renderer2DPanel extends JPanel implements IViewEventRelay {
    private org.openscience.cdk.renderer.Renderer renderer;
    private boolean isNewChemModel;
    private ControllerHub hub;
    private ControllerModel controllerModel;
    private boolean shouldPaintFromCache;
    IMolecule molecule;
    boolean fitToScreen = true;
    IDrawVisitor drawVisitor;
    RendererModel rendererModel;

    String title = "NA";
    double activity = -9999.0;
    DecimalFormat activityFormat = new DecimalFormat("############.00");


    /**
     * Create an instance of the rendering panel.
     *
     * This is a simplified constructor that uses defaults for the molecule
     * title and activity. Also it does not allow one to highlight substructures.
     *
     * @param mol molecule to render. Should have 2D coordinates
     * @param x width of the panel
     * @param y height of the panel
     */
    public Renderer2DPanel(IAtomContainer mol, int x, int y) {
        this(mol, null, x, y, false, "NA", -9999.0);
    }

    /**
     * Create an instance of the rendering panel.
     *
     * @param mol molecule to render. Should have 2D coordinates
     * @param needle  A fragment representing a substructure of the above molecule.
     * This substructure will be highlighted in the depiction. If no substructure
     * is to be highlighted, then set this to null
     * @param x width of the panel
     * @param y height of the panel
     * @param withHydrogen Should hydrogens be displayed
     * @param name The name of the molecule
     * @param activity The activity associated with the molecule
     */
    public Renderer2DPanel(IAtomContainer mol, IAtomContainer needle, int x, int y,
                           boolean withHydrogen, String name, double activity) {
        this.title = name;
        this.activity = activity;
        this.molecule = (IMolecule) mol;

        setPreferredSize(new Dimension(x, y));
        setBackground(Color.WHITE);

        IMoleculeSet moleculeSet = DefaultChemObjectBuilder.getInstance().newMoleculeSet();
        moleculeSet.addMolecule(this.molecule);
        IChemModel chemModel = DefaultChemObjectBuilder.getInstance().newChemModel();
        chemModel.setMoleculeSet(moleculeSet);

        RenderingParameters renderParam = new RenderingParameters();
        renderParam.setColorAtomsByType(false);
        renderParam.setShowAromaticity(true);
        renderParam.setUseAntiAliasing(true);
        if (needle!=null) {}


        rendererModel = new RendererModel(renderParam);
        renderer = new org.openscience.cdk.renderer.Renderer(new AWTFontManager());
        
        controllerModel = new ControllerModel();
        hub = new ControllerHub(controllerModel, renderer, chemModel, this);

        if (needle != null)  {
            
        }

        isNewChemModel = true;

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
        this.paintChemModel(g, bounds);
        return image;
    }

    public void paintChemModel(Graphics2D g, Rectangle screenBounds) {

        IChemModel chemModel = hub.getIChemModel();
        if (chemModel != null && chemModel.getMoleculeSet() != null) {

            // determine the size the canvas needs to be in order to fit the model
            Rectangle diagramBounds = renderer.calculateScreenBounds(chemModel);
            if (this.overlaps(screenBounds, diagramBounds)) {
                Rectangle union = screenBounds.union(diagramBounds);
                this.setPreferredSize(union.getSize());
                this.revalidate();
            }
            this.paintChemModel(chemModel, g, screenBounds);
        }
    }

    private boolean overlaps(Rectangle screenBounds, Rectangle diagramBounds) {
        return screenBounds.getMinX() > diagramBounds.getMinX()
                || screenBounds.getMinY() > diagramBounds.getMinY()
                || screenBounds.getMaxX() < diagramBounds.getMaxX()
                || screenBounds.getMaxY() < diagramBounds.getMaxY();
    }


    private void paintChemModel(IChemModel chemModel, Graphics2D g, Rectangle bounds) {
        drawVisitor = new AWTDrawVisitor(g);
        renderer.paintChemModel(chemModel, drawVisitor, bounds, isNewChemModel);
        isNewChemModel = false;

        /*
         * This is dangerous, but necessary to allow fast
         * repainting when scrolling the canvas.
         *
         * I set this to false, but the original code has it set to true.
         * If set to true, then any change in dimensions requires a call to
         * updateView() - by setting to false, we don't need to call updateView()
         */
        this.shouldPaintFromCache = false;
    }

    public void setIsNewChemModel(boolean isNewChemModel) {
        this.isNewChemModel = isNewChemModel;
    }

    public void paint(Graphics g) {
        this.setBackground(renderer.getRenderer2DModel().getBackColor());        
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (this.shouldPaintFromCache) {
//            this.paintFromCache(g2);
        } else {
//            this.paintChemModel(g2, this.getBounds());
            this.paintChemModel(g2, new Rectangle(0, 0, getWidth(), getHeight()));

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

        Rectangle rect = g.getClipBounds();
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

        // draw a box around things
//        String longestString;
//        if (msg.length() > title.length()) longestString = msg;
//        else longestString = title;
//        ypos = (int) (h - 2 * paddingY);
//        g.drawRect((int) (xpos - paddingX * 0.50),
//                (int) (ypos - fontMetrics.getHeight() - paddingY * 1.05),
//                (int) (fontMetrics.stringWidth(longestString) + paddingX * 1.25),
//                fontMetrics.getHeight() * 2);
//        updateView();
    }

//    private void paintFromCache(Graphics2D g) {
//        renderer.repaint(g);
//    }


    public void updateView() {
        this.shouldPaintFromCache = false;
        this.repaint();
    }    
}
