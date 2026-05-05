package com.sdnsim.algorithm;

/**
 * SsspResult.java — Immutable result container for SSSP algorithms.
 * <p>
 * Stores the shortest-path distances and predecessor array computed
 * by either Dijkstra or DMMSY.
 *
 * @param dist shortest-path distances from the source vertex;
 *             {@code Double.MAX_VALUE} indicates unreachable
 * @param pred predecessor array for path reconstruction;
 *             {@code -1} indicates the source or unreachable vertices
 *
 * @author SDN Routing Simulator Project
 */
public record SsspResult(double[] dist, int[] pred) {
}
