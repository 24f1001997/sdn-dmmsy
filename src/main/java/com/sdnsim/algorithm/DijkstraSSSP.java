package com.sdnsim.algorithm;

import com.sdnsim.graph.AdjacencyListGraph;
import com.sdnsim.graph.Edge;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * DijkstraSSSP.java — Baseline Dijkstra SSSP Implementation.
 * <p>
 * Standard Dijkstra's algorithm using a binary min-heap ({@link PriorityQueue}).
 * <p>
 * This serves as the <b>reference implementation</b> for:
 * <ol>
 *   <li>Correctness verification of the DMMSY algorithm.</li>
 *   <li>Performance benchmarking — demonstrating the O(m + n log n) sorting
 *       barrier that DMMSY breaks.</li>
 * </ol>
 *
 * <b>Time Complexity:</b>  O(m + n log n) — dominated by priority-queue operations.<br>
 * <b>Space Complexity:</b> O(n + m)
 *
 * @author SDN Routing Simulator Project
 */
public final class DijkstraSSSP {

    private DijkstraSSSP() {
        // Utility class — no instantiation
    }

    /**
     * Compute single-source shortest paths using Dijkstra's algorithm.
     * <p>
     * Uses a lazy-deletion binary heap: stale entries are skipped rather than
     * decreased in-place, giving O((m + n) log n) worst-case but excellent
     * practical performance and simplicity.
     *
     * @param graph    the input graph
     * @param source   source vertex (0-indexed)
     * @return {@link SsspResult} containing distances and predecessors
     */
    public static SsspResult compute(AdjacencyListGraph graph, int source) {
        int n = graph.nodeCount();
        double[] dist = new double[n];
        int[] pred = new int[n];

        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(pred, -1);
        dist[source] = 0.0;

        // Priority queue: [distance, vertex_index]
        // Comparator orders by distance (element [0])
        PriorityQueue<double[]> pq = new PriorityQueue<>(
                Comparator.comparingDouble(a -> a[0])
        );
        pq.offer(new double[]{0.0, source});

        while (!pq.isEmpty()) {
            double[] entry = pq.poll();
            double dU = entry[0];
            int u = (int) entry[1];

            // Lazy deletion: skip if we already found a shorter path to u
            if (dU > dist[u]) {
                continue;
            }

            // --- Relaxation step ---
            // This is where the O(n log n) "sorting barrier" manifests:
            // every relaxation may push to the heap → O(m log n) total pushes.
            List<Edge> neighbors = graph.getNeighbors(u);
            for (Edge edge : neighbors) {
                double newDist = dU + edge.weight();
                if (newDist < dist[edge.target()]) {
                    dist[edge.target()] = newDist;
                    pred[edge.target()] = u;
                    pq.offer(new double[]{newDist, edge.target()});
                }
            }
        }

        return new SsspResult(dist, pred);
    }
}
