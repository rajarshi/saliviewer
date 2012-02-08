package net.guha.apps.renderer;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @cdk Rajarshi Guha 
 */
public class ViewMolecules2D {

    static class ApplicationCloser extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
        }
    }

    public static JFrame multiStructurePanel(Renderer2DPanel[] panels, int ncol,
                                             int cellx, int celly) {
        if (panels.length != 2) return null;
        
        JFrame frame = new JFrame("2D Structure Viewer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        GridLayout layout = new GridLayout(1, 3);
        JPanel spane = new JPanel(layout);
        spane.add(panels[0]);
        spane.add(panels[1]);
       
        frame.getContentPane().add(spane);
        frame.addWindowListener(new ApplicationCloser());
        frame.setSize(cellx * panels.length, celly);
        frame.pack();
        return frame;
    }

    public static JFrame singleStructurePanel(Renderer2DPanel panel, int cellx, int celly) {
        JFrame frame = new JFrame("2D Structure Viewer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(panel);
        frame.addWindowListener(new ApplicationCloser());
        frame.setSize(cellx, celly);
        return frame;
    }

}
