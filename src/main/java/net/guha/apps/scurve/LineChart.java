package net.guha.apps.scurve;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;

/**
 * @author Rajarshi Guha 
 */
public class LineChart {

    ChartPanel chartPanel;

    public LineChart(double[] x, double[] y,
                     int width, int height,
                     double[] ylim,
                     String title, String ylab,
                     String annotation) {

        XYSeries curve = new XYSeries("SALI Curve (Type I)");
        for (int i = 0; i < x.length; i++) {
            curve.add(x[i], y[i]);
        }
        XYDataset dataset = new XYSeriesCollection(curve);


        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "SALI Cutoff", ylab, dataset,
                PlotOrientation.VERTICAL,
                false, false, false);

        XYPlot xyplot = chart.getXYPlot();
        NumberAxis xaxis = (NumberAxis) xyplot.getDomainAxis();
        xaxis.setTickUnit(new NumberTickUnit(0.2));

        if (ylim != null) {
            NumberAxis yaxis = (NumberAxis) xyplot.getRangeAxis();
            yaxis.setRange(ylim[0], ylim[1]);
            yaxis.setTickUnit(new NumberTickUnit(0.2));
        }

        if (annotation != null) {
            XYTextAnnotation annot = new XYTextAnnotation(annotation, 0.8, -0.8);
            annot.setFont(new Font("sansserif", Font.BOLD, 16));
            xyplot.addAnnotation(annot);
        }

        chart.setAntiAlias(true);
        chart.getRenderingHints().put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        chartPanel = new ChartPanel(chart, true);
        chartPanel.setPreferredSize(new Dimension(width, height));
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }
}
