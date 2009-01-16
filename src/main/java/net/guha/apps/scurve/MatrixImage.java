package net.guha.apps.scurve;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;

/**
 * @cdk.author Rajarshi Guha
 * @cdk.svnrev $Revision: 9162 $
 */
public class MatrixImage {

    ChartPanel chartPanel;

    public MatrixImage(double[][] data,                     
                     String title, String ylab,
                     String annotation) {

        DefaultXYZDataset dataset = new DefaultXYZDataset();
        
        XYSeries curve = new XYSeries("SALI Curve (Type I)");
//        for (int i = 0; i < x.length; i++) {
//            curve.add(x[i], y[i]);
//        }
//        XYDataset dataset = new XYSeriesCollection(curve);
//
//
//        JFreeChart chart = ChartFactory.createXYLineChart(
//                title,
//                "SALI Cutoff", ylab, dataset,
//                PlotOrientation.VERTICAL,
//                false, false, false);
//
//        XYPlot xyplot = chart.getXYPlot();
//        NumberAxis xaxis = (NumberAxis) xyplot.getDomainAxis();
//        xaxis.setTickUnit(new NumberTickUnit(0.2));
//
//
//
//        chart.setAntiAlias(true);
//        chart.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        chartPanel = new ChartPanel(chart, true);
//        chartPanel.setPreferredSize(new Dimension(width, height));
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }
}
