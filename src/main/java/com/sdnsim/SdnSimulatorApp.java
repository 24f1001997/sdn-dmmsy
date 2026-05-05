package com.sdnsim;

import com.sdnsim.benchmark.BenchmarkSuite;
import com.sdnsim.benchmark.BenchmarkSuite.BenchmarkPoint;
import com.sdnsim.simulation.SdnCloudSimRunner;
import com.sdnsim.simulation.SdnCloudSimRunner.CloudSimResult;
import com.sdnsim.simulation.VolatilitySimulator;
import com.sdnsim.simulation.VolatilitySimulator.SimulationResult;
import com.sdnsim.visualization.ChartGenerator;

import java.io.IOException;
import java.util.List;

/**
 * SdnSimulatorApp.java — SDN Routing Simulator Entry Point.
 * <p>
 * End-to-end orchestrator that:
 * <ol>
 *   <li>Benchmarks Dijkstra vs. DMMSY across increasing graph sizes.</li>
 *   <li>Verifies DMMSY correctness against Dijkstra reference.</li>
 *   <li>Runs a CloudSim Plus simulation modeling SDN route computations.</li>
 *   <li>Simulates a high-volatility auto-scaling event.</li>
 *   <li>Generates all visualizations to the {@code output/} directory.</li>
 * </ol>
 *
 * <b>Usage:</b>
 * <pre>
 *   mvn compile exec:java
 * </pre>
 *
 * @author SDN Routing Simulator Project
 */
public class SdnSimulatorApp {

    // ===================================================================
    //  Configuration
    // ===================================================================

    /** Scale points for the benchmark suite. */
    private static final int[] SCALE_POINTS = {
            500, 1000, 2500, 5000, 10_000, 25_000, 50_000, 100_000
    };

    /** Barabasi-Albert sparsity parameter (edges per new node). */
    private static final int EDGES_PER_NODE = 3;

    /** Edge weight range [min, max] in milliseconds. */
    private static final double MIN_WEIGHT = 1.0;
    private static final double MAX_WEIGHT = 50.0;

    /** Random seed for reproducibility. */
    private static final long SEED = 42L;

    /** SDN simulation parameters. */
    private static final int SIM_BASE_NODES = 5000;
    private static final double SIM_VOLATILITY_PCT = 0.25;

    /** Output directory for charts. */
    private static final String OUTPUT_DIR = "output";

    // ===================================================================
    //  Main
    // ===================================================================

    public static void main(String[] args) {
        System.out.println();
        System.out.println("+--------------------------------------------------------------+");
        System.out.println("|   SDN Routing Simulator: Dijkstra vs. DMMSY (STOC 2025)     |");
        System.out.println("|   Breaking the Sorting Barrier for Shortest Paths            |");
        System.out.println("|   Java 17 + CloudSim Plus 8.0 + JFreeChart                  |");
        System.out.println("+--------------------------------------------------------------+");

        long totalStart = System.nanoTime();

        // Phase 1: Scaling benchmark
        List<BenchmarkPoint> benchmarkData = runScalingBenchmark();

        // Phase 2: CloudSim Plus SDN simulation
        runCloudSimSimulation();

        // Phase 3: Volatility simulation
        SimulationResult simResult = runVolatilitySimulation();

        // Phase 4: Generate visualizations
        generateVisualizations(benchmarkData, simResult);

        double totalElapsed = (System.nanoTime() - totalStart) / 1_000_000_000.0;

        printHeader("COMPLETE");
        System.out.printf("%n  Total execution time: %.2f seconds%n", totalElapsed);
        System.out.printf("  Output directory    : ./%s/%n", OUTPUT_DIR);
        System.out.println("  Charts generated    : 3");
        System.out.println();
    }

    // ===================================================================
    //  Phase 1: Scaling Benchmark
    // ===================================================================

    private static List<BenchmarkPoint> runScalingBenchmark() {
        printHeader("PHASE 1: Scaling Benchmark -- Dijkstra vs. DMMSY");
        return BenchmarkSuite.runBenchmarks(
                SCALE_POINTS, EDGES_PER_NODE, MIN_WEIGHT, MAX_WEIGHT, SEED);
    }

    // ===================================================================
    //  Phase 2: CloudSim Plus SDN Simulation
    // ===================================================================

    private static void runCloudSimSimulation() {
        printHeader("PHASE 2: CloudSim Plus SDN Simulation");
        System.out.printf("%n  Simulating SDN route computations as Cloud workloads...%n");
        System.out.printf("  Network size : %,d nodes%n", SIM_BASE_NODES);

        CloudSimResult csResult = SdnCloudSimRunner.runCloudSimulation(
                SIM_BASE_NODES, EDGES_PER_NODE, SEED);

        System.out.printf("%n  CloudSim Plus Results:%n");
        System.out.printf("    Hosts deployed       : %d%n", csResult.numHosts());
        System.out.printf("    VMs created          : %d%n", csResult.numVms());
        System.out.printf("    Cloudlets executed    : %d%n", csResult.totalCloudlets());
        System.out.printf("    Simulation end time   : %.2f s%n", csResult.totalSimulationTime());
        System.out.printf("    Dijkstra SSSP time    : %.3f ms%n", csResult.dijkstraTotalMs());
        System.out.printf("    DMMSY SSSP time       : %.3f ms%n", csResult.dmmsyTotalMs());
    }

    // ===================================================================
    //  Phase 3: Volatility Simulation
    // ===================================================================

    private static SimulationResult runVolatilitySimulation() {
        printHeader("PHASE 3: SDN Volatility Simulation (Auto-Scaling Event)");

        int newNodes = (int) (SIM_BASE_NODES * SIM_VOLATILITY_PCT);
        System.out.printf("%n  Base topology : %,d nodes%n", SIM_BASE_NODES);
        System.out.printf("  Volatility    : +%,d nodes (%.0f%% burst)%n",
                newNodes, SIM_VOLATILITY_PCT * 100);
        System.out.printf("  Phases        : baseline -> volatility -> post-volatility%n%n");

        SimulationResult simResult = VolatilitySimulator.runSimulation(
                SIM_BASE_NODES, SIM_VOLATILITY_PCT,
                3, 5, 2,
                EDGES_PER_NODE, SEED);

        System.out.printf("  Results:%n");
        System.out.printf("    Total recalculation events : %d%n", simResult.events.size());
        System.out.printf("    Dijkstra mean latency      : %.3f ms%n", simResult.dijkstraMeanMs());
        System.out.printf("    DMMSY mean latency         : %.3f ms%n", simResult.dmmsyMeanMs());
        System.out.printf("    Mean speedup               : %.2fx%n", simResult.meanSpeedup());
        System.out.printf("    Peak network size          : %,d nodes%n", simResult.peakNodes());

        return simResult;
    }

    // ===================================================================
    //  Phase 4: Visualization
    // ===================================================================

    private static void generateVisualizations(
            List<BenchmarkPoint> benchmarkData, SimulationResult simResult) {
        printHeader("PHASE 4: Generating Visualizations");
        System.out.println();

        try {
            ChartGenerator.generateAllCharts(benchmarkData, simResult, OUTPUT_DIR);
            System.out.printf("%n  All charts saved to: ./%s/%n", OUTPUT_DIR);
        } catch (IOException e) {
            System.err.println("  [ERROR] Failed to generate charts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================================================================
    //  Helpers
    // ===================================================================

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("  " + title);
        System.out.println("=".repeat(70));
    }
}
