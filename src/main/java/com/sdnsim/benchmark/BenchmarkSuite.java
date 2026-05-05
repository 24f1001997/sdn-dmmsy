package com.sdnsim.benchmark;

import com.sdnsim.algorithm.DijkstraSSSP;
import com.sdnsim.algorithm.DmmsySSSP;
import com.sdnsim.algorithm.SsspResult;
import com.sdnsim.graph.AdjacencyListGraph;
import com.sdnsim.graph.TopologyGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * BenchmarkSuite.java — Scaling Benchmarks with Correctness Verification.
 * <p>
 * Runs both SSSP algorithms (Dijkstra and DMMSY) across increasing graph
 * sizes, verifying correctness of DMMSY against Dijkstra's reference output
 * and recording execution times for visualization.
 *
 * @author SDN Routing Simulator Project
 */
public final class BenchmarkSuite {

    /** Tolerance for floating-point distance comparison. */
    private static final double DIST_TOLERANCE = 1e-6;

    /** Result of a single benchmark point. */
    public record BenchmarkPoint(
            int nodes,
            int edges,
            double dijkstraMs,
            double dmmsyMs,
            double speedup,
            boolean correct
    ) {}

    private BenchmarkSuite() {
        // Utility class
    }

    /**
     * Run the scaling benchmark across all specified scale points.
     *
     * @param scalePoints   array of node counts to test
     * @param edgesPerNode  BA model sparsity parameter
     * @param minWeight     minimum edge weight
     * @param maxWeight     maximum edge weight
     * @param seed          random seed
     * @return list of benchmark results
     */
    public static List<BenchmarkPoint> runBenchmarks(
            int[] scalePoints, int edgesPerNode,
            double minWeight, double maxWeight, long seed) {

        List<BenchmarkPoint> results = new ArrayList<>();
        boolean allCorrect = true;

        System.out.println();
        System.out.printf("  %8s  %10s  %14s  %12s  %8s  %8s%n",
                "Nodes", "Edges", "Dijkstra(ms)", "DMMSY(ms)", "Speedup", "Status");
        System.out.println("  " + "-".repeat(68));

        for (int n : scalePoints) {
            // Generate topology
            AdjacencyListGraph graph = TopologyGenerator.generateCloudTopology(
                    n, edgesPerNode, minWeight, maxWeight, seed);
            int actualN = graph.nodeCount();
            int numEdges = graph.edgeCount();
            int source = 0;

            // --- Dijkstra ---
            long t0 = System.nanoTime();
            SsspResult dijResult = DijkstraSSSP.compute(graph, source);
            double dijMs = (System.nanoTime() - t0) / 1_000_000.0;

            // --- DMMSY ---
            t0 = System.nanoTime();
            SsspResult dmmResult = DmmsySSSP.compute(graph, source);
            double dmmMs = (System.nanoTime() - t0) / 1_000_000.0;

            // --- Correctness check ---
            boolean correct = verifyCorrectness(
                    dijResult.dist(), dmmResult.dist(), actualN, "(n=" + actualN + ")");
            if (!correct) {
                allCorrect = false;
            }

            double speedup = dmmMs > 0 ? dijMs / dmmMs : Double.POSITIVE_INFINITY;
            String status = correct ? "OK" : "FAIL";

            System.out.printf("  %,8d  %,10d  %14.3f  %12.3f  %7.2fx  %8s%n",
                    actualN, numEdges, dijMs, dmmMs, speedup, status);

            results.add(new BenchmarkPoint(actualN, numEdges, dijMs, dmmMs, speedup, correct));
        }

        System.out.println();
        if (allCorrect) {
            System.out.println("  == All correctness checks PASSED ==");
        } else {
            System.out.println("  == WARNING: Some correctness checks FAILED ==");
        }

        return results;
    }

    /**
     * Compare DMMSY distances against Dijkstra reference.
     *
     * @param dijDist Dijkstra distance array
     * @param dmmDist DMMSY distance array
     * @param n       number of vertices
     * @param label   label for log messages
     * @return true if all distances match within tolerance
     */
    private static boolean verifyCorrectness(
            double[] dijDist, double[] dmmDist, int n, String label) {

        int mismatches = 0;
        for (int i = 0; i < n; i++) {
            double d = dijDist[i];
            double m = dmmDist[i];
            if (d >= Double.MAX_VALUE && m >= Double.MAX_VALUE) continue;
            if (Math.abs(d - m) > DIST_TOLERANCE) {
                mismatches++;
                if (mismatches <= 5) {
                    System.out.printf("    [X] Node %d: Dijkstra=%.6f, DMMSY=%.6f, delta=%.2e%n",
                            i, d, m, Math.abs(d - m));
                }
            }
        }

        if (mismatches == 0) {
            System.out.printf("    [OK] Correctness PASSED %s -- all %d distances match.%n",
                    label, n);
        } else {
            System.out.printf("    [X] Correctness FAILED %s -- %d/%d mismatches.%n",
                    label, mismatches, n);
        }

        return mismatches == 0;
    }
}
