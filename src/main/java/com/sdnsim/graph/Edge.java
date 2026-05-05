package com.sdnsim.graph;

/**
 * Edge.java — Immutable weighted directed edge.
 * <p>
 * Represents a single directed edge in the adjacency list graph.
 * Uses a Java 17 {@code record} for compact, immutable storage.
 *
 * @param target destination vertex index (0-based)
 * @param weight edge weight (latency in milliseconds)
 *
 * @author SDN Routing Simulator Project
 */
public record Edge(int target, double weight) {

    /**
     * Compact constructor — validates edge invariants.
     */
    public Edge {
        if (target < 0) {
            throw new IllegalArgumentException("Target vertex index must be >= 0, got: " + target);
        }
        if (weight < 0) {
            throw new IllegalArgumentException("Edge weight must be >= 0, got: " + weight);
        }
    }
}
