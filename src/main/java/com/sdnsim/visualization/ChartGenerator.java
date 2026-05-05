package com.sdnsim.visualization;

import com.sdnsim.benchmark.BenchmarkSuite.BenchmarkPoint;
import com.sdnsim.simulation.VolatilitySimulator.SimulationResult;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * ChartGenerator.java — Professional Benchmark Visualization Suite.
 * <p>
 * Generates publication-quality PNG charts comparing Dijkstra vs. DMMSY
 * algorithm performance across multiple dimensions:
 * <ol>
 *   <li><b>Scaling Comparison</b> — Execution Time vs. Number of Nodes</li>
 *   <li><b>Speedup Ratio</b> — DMMSY speedup factor vs. Number of Nodes</li>
 *   <li><b>Volatility Timeline</b> — Recalculation latency over simulation phases</li>
 * </ol>
 * <p>
 * Uses a dark theme with anti-aliased rendering for a premium visual.
 *
 * @author SDN Routing Simulator Project
 */
public final class ChartGenerator {

    // --- Dark theme colors ---
    private static final Color BG_COLOR = new Color(30, 30, 40);
    private static final Color PLOT_BG_COLOR = new Color(40, 42, 54);
    private static final Color GRID_COLOR = new Color(70, 72, 86);
    private static final Color TEXT_COLOR = new Color(220, 220, 230);
    private static final Color SUBTITLE_COLOR = new Color(180, 180, 190);
    private static final Color DIJKSTRA_COLOR = new Color(255, 85, 85);   // Red
    private static final Color DMMSY_COLOR = new Color(80, 250, 123);     // Green
    private static final Color SPEEDUP_COLOR = new Color(139, 233, 253);  // Cyan
    private static final Color BASELINE_COLOR = new Color(189, 147, 249); // Purple

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 700;

    private ChartGenerator() {
        // Utility class
    }

    /**
     * Create a styled TextTitle with the dark theme.
     */
    private static TextTitle makeTitle(String text, int style, int size, Color color) {
        TextTitle title = new TextTitle(text, new Font("SansSerif", style, size));
        title.setPaint(color);
        return title;
    }

    /**
     * Generate all three charts and save to the output directory.
     */
    public static void generateAllCharts(
            List<BenchmarkPoint> benchmarkData,
            SimulationResult simResult,
            String outputDir) throws IOException {

        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        plotScalingComparison(benchmarkData, outputDir);
        plotSpeedupRatio(benchmarkData, outputDir);
        plotVolatilityTimeline(simResult, outputDir);
    }

    /**
     * Chart 1: Scaling Comparison — Execution Time vs. Number of Nodes.
     * <p>
     * Demonstrates the O(m + n log n) bottleneck of Dijkstra versus
     * the flatter O(m log^{2/3} n) stability of DMMSY.
     */
    public static void plotScalingComparison(
            List<BenchmarkPoint> data, String outputDir) throws IOException {

        XYSeries dijSeries = new XYSeries("Dijkstra  O(m + n log n)");
        XYSeries dmmSeries = new XYSeries("DMMSY  O(m log^{2/3} n)");

        for (BenchmarkPoint bp : data) {
            dijSeries.add(bp.nodes(), bp.dijkstraMs());
            dmmSeries.add(bp.nodes(), bp.dmmsyMs());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(dijSeries);
        dataset.addSeries(dmmSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "SDN Routing: Dijkstra vs. DMMSY -- Scaling Comparison",
                "Number of Nodes",
                "Execution Time (ms)",
                dataset
        );

        chart.addSubtitle(makeTitle(
                "Breaking the 65-Year Sorting Barrier for Single-Source Shortest Paths",
                Font.ITALIC, 12, SUBTITLE_COLOR));

        applyDarkTheme(chart);

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, DIJKSTRA_COLOR);
        renderer.setSeriesPaint(1, DMMSY_COLOR);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesStroke(1, new BasicStroke(2.5f));
        plot.setRenderer(renderer);

        File outFile = new File(outputDir, "scaling_comparison.png");
        ChartUtils.saveChartAsPNG(outFile, chart, WIDTH, HEIGHT);
        System.out.println("    [+] Saved: " + outFile.getPath());
    }

    /**
     * Chart 2: Speedup Ratio — DMMSY speedup factor vs. Number of Nodes.
     * <p>
     * Shows how DMMSY's advantage grows with network scale.
     */
    public static void plotSpeedupRatio(
            List<BenchmarkPoint> data, String outputDir) throws IOException {

        XYSeries speedupSeries = new XYSeries("Speedup (Dijkstra / DMMSY)");
        XYSeries baselineSeries = new XYSeries("1.0x (Break-even)");

        for (BenchmarkPoint bp : data) {
            speedupSeries.add(bp.nodes(), bp.speedup());
            baselineSeries.add(bp.nodes(), 1.0);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(speedupSeries);
        dataset.addSeries(baselineSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "DMMSY Speedup Over Dijkstra",
                "Number of Nodes",
                "Speedup Factor (x)",
                dataset
        );

        chart.addSubtitle(makeTitle(
                "Values > 1.0 indicate DMMSY is faster",
                Font.ITALIC, 12, SUBTITLE_COLOR));

        applyDarkTheme(chart);

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, SPEEDUP_COLOR);
        renderer.setSeriesPaint(1, BASELINE_COLOR);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesStroke(1, new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 4.0f}, 0.0f));
        renderer.setSeriesShapesVisible(1, false);
        plot.setRenderer(renderer);

        File outFile = new File(outputDir, "speedup_ratio.png");
        ChartUtils.saveChartAsPNG(outFile, chart, WIDTH, HEIGHT);
        System.out.println("    [+] Saved: " + outFile.getPath());
    }

    /**
     * Chart 3: Volatility Timeline — Recalculation latency over simulation time.
     * <p>
     * Shows latency spikes during the auto-scaling event and how each
     * algorithm responds to sudden topology changes.
     */
    public static void plotVolatilityTimeline(
            SimulationResult result, String outputDir) throws IOException {

        XYSeries dijSeries = new XYSeries("Dijkstra");
        XYSeries dmmSeries = new XYSeries("DMMSY");

        for (double[] point : result.timeline) {
            // point = [simTime, numNodes, dijkstraMs, dmmsyMs]
            dijSeries.add(point[0], point[2]);
            dmmSeries.add(point[0], point[3]);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(dijSeries);
        dataset.addSeries(dmmSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "SDN High-Volatility Event -- Routing Recalculation Latency",
                "Simulation Time (s)",
                "Recalculation Latency (ms)",
                dataset
        );

        String subtitle = String.format(
                "Base: %,d nodes | +%,d nodes (auto-scaling) | Peak: %,d nodes",
                result.baseNodes, result.volatilityNewNodes, result.peakNodes());
        chart.addSubtitle(makeTitle(subtitle, Font.ITALIC, 12, SUBTITLE_COLOR));

        applyDarkTheme(chart);

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesPaint(0, DIJKSTRA_COLOR);
        renderer.setSeriesPaint(1, DMMSY_COLOR);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesStroke(1, new BasicStroke(2.5f));
        plot.setRenderer(renderer);

        File outFile = new File(outputDir, "volatility_timeline.png");
        ChartUtils.saveChartAsPNG(outFile, chart, WIDTH, HEIGHT);
        System.out.println("    [+] Saved: " + outFile.getPath());
    }

    /**
     * Apply a dark theme to a JFreeChart instance.
     */
    private static void applyDarkTheme(JFreeChart chart) {
        chart.setBackgroundPaint(BG_COLOR);

        // Style the main title
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(TEXT_COLOR);
            chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 18));
        }

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(BG_COLOR);
            chart.getLegend().setItemPaint(TEXT_COLOR);
            chart.getLegend().setItemFont(new Font("SansSerif", Font.PLAIN, 13));
        }

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(PLOT_BG_COLOR);
        plot.setDomainGridlinePaint(GRID_COLOR);
        plot.setRangeGridlinePaint(GRID_COLOR);
        plot.setOutlinePaint(GRID_COLOR);

        // Axis styling
        plot.getDomainAxis().setTickLabelPaint(TEXT_COLOR);
        plot.getDomainAxis().setLabelPaint(TEXT_COLOR);
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 13));
        plot.getDomainAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));

        plot.getRangeAxis().setTickLabelPaint(TEXT_COLOR);
        plot.getRangeAxis().setLabelPaint(TEXT_COLOR);
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 13));
        plot.getRangeAxis().setTickLabelFont(new Font("SansSerif", Font.PLAIN, 11));

        // Anti-aliasing
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
    }
}
