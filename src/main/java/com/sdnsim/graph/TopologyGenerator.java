package com.sdnsim.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * TopologyGenerator.java — Sparse Cloud Network Topology Generator.
 * <p>
 * Generates large-scale, highly sparse network topologies that simulate
 * real-world cloud/datacenter/SDN environments using the Barabási–Albert
 * preferential attachment model.
 * <p>
 * The BA model produces power-law degree distributions characteristic of
 * real-world networks: most nodes have few connections, while a small
 * number of "hub" nodes have many — mirroring load balancers, core
 * switches, and aggregation routers in cloud architectures.
 *
 * @author SDN Routing Simulator Project
 */
public final class TopologyGenerator {

    private TopologyGenerator() {
        // Utility class — no instantiation
    }

    /**
     * Generates a sparse, connected cloud-network topology using the
     * Barabási–Albert preferential attachment algorithm.
     * <p>
     * Each new node connects to {@code edgesPerNode} existing nodes
     * with probability proportional to their current degree, producing
     * a scale-free network.
     *
     * @param numNodes     total number of nodes (routers/switches/VMs)
     * @param edgesPerNode number of edges each new node attaches to existing nodes
     *                     (BA model {@code m} parameter); default 3 yields avg degree ≈ 6
     * @param minWeight    minimum edge weight (latency in ms)
     * @param maxWeight    maximum edge weight (latency in ms)
     * @param seed         random seed for reproducibility
     * @return the generated {@link AdjacencyListGraph}
     */
    public static AdjacencyListGraph generateCloudTopology(
            int numNodes, int edgesPerNode,
            double minWeight, double maxWeight, long seed) {

        if (numNodes < 2) numNodes = 2;
        edgesPerNode = Math.min(edgesPerNode, numNodes - 1);

        Random rng = new Random(seed);
        AdjacencyListGraph graph = new AdjacencyListGraph(numNodes);

        // -----------------------------------------------------------
        // Barabási–Albert preferential attachment
        //
        // Start with a complete graph on (edgesPerNode + 1) nodes,
        // then add remaining nodes one at a time, each connecting
        // to edgesPerNode existing nodes with probability proportional
        // to degree (preferential attachment).
        // -----------------------------------------------------------

        int m0 = edgesPerNode + 1; // initial complete graph size
        if (m0 > numNodes) m0 = numNodes;

        // Phase 1: Build initial complete graph on m0 nodes
        for (int i = 0; i < m0; i++) {
            for (int j = i + 1; j < m0; j++) {
                double w = minWeight + rng.nextDouble() * (maxWeight - minWeight);
                w = Math.round(w * 10000.0) / 10000.0; // 4 decimal places
                graph.addUndirectedEdge(i, j, w);
            }
        }

        // Degree-sum array for preferential attachment sampling
        // repeated[] stores node IDs proportional to their degree
        List<Integer> repeated = new ArrayList<>();
        for (int i = 0; i < m0; i++) {
            int deg = graph.degree(i);
            for (int d = 0; d < deg; d++) {
                repeated.add(i);
            }
        }

        // Phase 2: Add remaining nodes via preferential attachment
        for (int newNode = m0; newNode < numNodes; newNode++) {
            // Select edgesPerNode distinct targets proportional to degree
            boolean[] chosen = new boolean[numNodes];
            int connected = 0;

            int maxAttempts = edgesPerNode * 20; // safety bound
            int attempts = 0;

            while (connected < edgesPerNode && attempts < maxAttempts) {
                attempts++;
                if (repeated.isEmpty()) break;

                int target = repeated.get(rng.nextInt(repeated.size()));
                if (target == newNode || chosen[target]) continue;

                chosen[target] = true;
                double w = minWeight + rng.nextDouble() * (maxWeight - minWeight);
                w = Math.round(w * 10000.0) / 10000.0;
                graph.addUndirectedEdge(newNode, target, w);

                // Update repeated list for future preferential attachment
                repeated.add(newNode);
                repeated.add(target);

                connected++;
            }

            // Fallback: if preferential attachment didn't connect enough,
            // connect to random existing nodes
            if (connected < edgesPerNode) {
                for (int t = 0; t < newNode && connected < edgesPerNode; t++) {
                    if (!chosen[t]) {
                        chosen[t] = true;
                        double w = minWeight + rng.nextDouble() * (maxWeight - minWeight);
                        w = Math.round(w * 10000.0) / 10000.0;
                        graph.addUndirectedEdge(newNode, t, w);
                        repeated.add(newNode);
                        repeated.add(t);
                        connected++;
                    }
                }
            }
        }

        return graph;
    }

    /**
     * Simulate a high-volatility auto-scaling event by injecting new nodes.
     * <p>
     * Models the sudden spin-up of containers/VMs in a cloud environment,
     * forcing the SDN controller to recalculate all routing tables.
     *
     * @param graph           existing graph (mutated in place)
     * @param currentN        current number of nodes
     * @param numNewNodes     number of new nodes to inject
     * @param edgesPerNewNode edges per new node
     * @param minWeight       minimum edge weight
     * @param maxWeight       maximum edge weight
     * @param seed            random seed
     * @return new total node count
     */
    public static int addVolatilityNodes(
            AdjacencyListGraph graph, int currentN, int numNewNodes,
            int edgesPerNewNode, double minWeight, double maxWeight, long seed) {

        Random rng = new Random(seed);
        int newN = currentN + numNewNodes;
        graph.ensureCapacity(newN);

        for (int i = 0; i < numNewNodes; i++) {
            int newNode = currentN + i;
            int k = Math.min(edgesPerNewNode, currentN + i);
            boolean[] chosen = new boolean[newN];
            int connected = 0;

            while (connected < k) {
                int target = rng.nextInt(currentN + i);
                if (chosen[target]) continue;
                chosen[target] = true;

                double w = minWeight + rng.nextDouble() * (maxWeight - minWeight);
                w = Math.round(w * 10000.0) / 10000.0;
                graph.addUndirectedEdge(newNode, target, w);
                connected++;
            }
        }

        return newN;
    }
}
