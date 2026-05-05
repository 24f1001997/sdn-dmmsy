package com.sdnsim.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AdjacencyListGraph.java — Sparse graph via adjacency lists.
 * <p>
 * Stores a directed graph as {@code List<List<Edge>>} for O(1) neighbor
 * access and O(degree) iteration — optimal for the sparse cloud topologies
 * used in the SDN simulator.
 * <p>
 * Undirected edges are represented as two directed edges (u→v and v→u).
 *
 * @author SDN Routing Simulator Project
 */
public class AdjacencyListGraph {

    private final List<List<Edge>> adjacency;
    private int edgeCount;

    /**
     * Creates an empty graph with {@code numNodes} vertices (0-indexed).
     *
     * @param numNodes number of vertices
     */
    public AdjacencyListGraph(int numNodes) {
        if (numNodes < 0) {
            throw new IllegalArgumentException("numNodes must be >= 0");
        }
        this.adjacency = new ArrayList<>(numNodes);
        for (int i = 0; i < numNodes; i++) {
            this.adjacency.add(new ArrayList<>());
        }
        this.edgeCount = 0;
    }

    /**
     * Adds a directed edge from {@code src} to {@code dst} with the given weight.
     *
     * @param src    source vertex
     * @param dst    destination vertex
     * @param weight edge weight (latency)
     */
    public void addDirectedEdge(int src, int dst, double weight) {
        ensureCapacity(Math.max(src, dst) + 1);
        adjacency.get(src).add(new Edge(dst, weight));
        edgeCount++;
    }

    /**
     * Adds an undirected edge (two directed edges) between {@code u} and {@code v}.
     *
     * @param u      first vertex
     * @param v      second vertex
     * @param weight edge weight
     */
    public void addUndirectedEdge(int u, int v, double weight) {
        ensureCapacity(Math.max(u, v) + 1);
        adjacency.get(u).add(new Edge(v, weight));
        adjacency.get(v).add(new Edge(u, weight));
        edgeCount += 2;
    }

    /**
     * Returns an unmodifiable view of the neighbors of vertex {@code u}.
     *
     * @param u vertex index
     * @return list of outgoing edges
     */
    public List<Edge> getNeighbors(int u) {
        if (u < 0 || u >= adjacency.size()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(adjacency.get(u));
    }

    /**
     * Returns the number of vertices in the graph.
     */
    public int nodeCount() {
        return adjacency.size();
    }

    /**
     * Returns the total number of directed edges.
     */
    public int edgeCount() {
        return edgeCount;
    }

    /**
     * Expands the graph to accommodate at least {@code requiredSize} vertices.
     * New vertices have empty neighbor lists.
     *
     * @param requiredSize minimum number of vertices
     */
    public void ensureCapacity(int requiredSize) {
        while (adjacency.size() < requiredSize) {
            adjacency.add(new ArrayList<>());
        }
    }

    /**
     * Returns the degree (number of outgoing edges) of vertex {@code u}.
     */
    public int degree(int u) {
        if (u < 0 || u >= adjacency.size()) {
            return 0;
        }
        return adjacency.get(u).size();
    }

    /**
     * Finds the minimum positive edge weight in the entire graph.
     * Returns {@link Double#MAX_VALUE} if no edges exist.
     */
    public double minEdgeWeight() {
        double minW = Double.MAX_VALUE;
        for (List<Edge> edges : adjacency) {
            for (Edge e : edges) {
                if (e.weight() > 0 && e.weight() < minW) {
                    minW = e.weight();
                }
            }
        }
        return minW;
    }
}
