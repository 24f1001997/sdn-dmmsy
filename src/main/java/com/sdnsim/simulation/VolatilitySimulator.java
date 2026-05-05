package com.sdnsim.simulation;

import com.sdnsim.algorithm.DijkstraSSSP;
import com.sdnsim.algorithm.DmmsySSSP;
import com.sdnsim.graph.AdjacencyListGraph;
import com.sdnsim.graph.TopologyGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * VolatilitySimulator.java — SDN Simulation with High-Volatility Events.
 * <p>
 * Simulates a Software-Defined Networking (SDN) controller that continuously
 * recalculates routing tables when network topology changes due to
 * auto-scaling events (sudden bursts of new containers/VMs).
 *
 * <h2>Simulation Phases</h2>
 * <ol>
 *   <li><b>Baseline</b> — recalculations on the stable topology</li>
 *   <li><b>Volatility</b> — multiple auto-scaling waves, each adding nodes</li>
 *   <li><b>Post-volatility</b> — recalculations on the expanded (stable) topology</li>
 * </ol>
 *
 * @author SDN Routing Simulator Project
 */
public final class VolatilitySimulator {

    /** A single routing recalculation measurement. */
    public record RecalcEvent(
            double simTime,
            String algorithm,
            double wallTimeMs,
            int numNodes,
            int numEdges,
            String eventType
    ) {}

    /** Complete results of a volatility simulation run. */
    public static class SimulationResult {
        public final int baseNodes;
        public final int volatilityNewNodes;
        public final List<RecalcEvent> events = new ArrayList<>();
        public final List<Double> dijkstraTimes = new ArrayList<>();
        public final List<Double> dmmsyTimes = new ArrayList<>();
        /** (simTime, eventType, numNodes, dijkstraMs, dmmsyMs) */
        public final List<double[]> timeline = new ArrayList<>();

        public SimulationResult(int baseNodes, int volatilityNewNodes) {
            this.baseNodes = baseNodes;
            this.volatilityNewNodes = volatilityNewNodes;
        }

        /** Returns peak node count (base + volatility). */
        public int peakNodes() {
            return baseNodes + volatilityNewNodes;
        }

        /** Mean Dijkstra recalculation time in ms. */
        public double dijkstraMeanMs() {
            return dijkstraTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        /** Mean DMMSY recalculation time in ms. */
        public double dmmsyMeanMs() {
            return dmmsyTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        /** Mean speedup (Dijkstra / DMMSY). */
        public double meanSpeedup() {
            double dmm = dmmsyMeanMs();
            return dmm > 0 ? dijkstraMeanMs() / dmm : Double.POSITIVE_INFINITY;
        }
    }

    private VolatilitySimulator() {
        // Utility class
    }

    /**
     * Run a full SDN routing simulation with a high-volatility event.
     *
     * @param baseNodes         initial network size
     * @param volatilityPct     fraction of baseNodes added during volatility (e.g. 0.25)
     * @param numPreEvents      routing recalculations before volatility
     * @param numVolatilityWaves number of auto-scaling bursts
     * @param numPostEvents     recalculations after stabilization
     * @param edgesPerNode      sparsity parameter
     * @param seed              random seed
     * @return simulation results with timing data
     */
    public static SimulationResult runSimulation(
            int baseNodes, double volatilityPct,
            int numPreEvents, int numVolatilityWaves,
            int numPostEvents, int edgesPerNode, long seed) {

        int totalNewNodes = (int) (baseNodes * volatilityPct);
        int nodesPerWave = Math.max(1, totalNewNodes / numVolatilityWaves);

        SimulationResult result = new SimulationResult(baseNodes, totalNewNodes);

        // --- Generate base topology ---
        AdjacencyListGraph graph = TopologyGenerator.generateCloudTopology(
                baseNodes, edgesPerNode, 1.0, 50.0, seed);
        int n = graph.nodeCount();
        int source = 0;

        double simTime = 0.0;

        // Phase 1: Baseline
        for (int i = 0; i < numPreEvents; i++) {
            simTime += 1.0;
            recordRecalculation(graph, n, source, simTime, "baseline", result);
        }

        // Phase 2: Volatility — multiple auto-scaling waves
        for (int wave = 0; wave < numVolatilityWaves; wave++) {
            n = TopologyGenerator.addVolatilityNodes(
                    graph, n, nodesPerWave, edgesPerNode,
                    1.0, 50.0, seed + wave + 100);
            simTime += 1.0;
            recordRecalculation(graph, n, source, simTime, "volatility", result);
        }

        // Phase 3: Post-volatility
        for (int i = 0; i < numPostEvents; i++) {
            simTime += 1.0;
            recordRecalculation(graph, n, source, simTime, "post_volatility", result);
        }

        return result;
    }

    /**
     * Run both algorithms on the current graph and record timing.
     */
    private static void recordRecalculation(
            AdjacencyListGraph graph, int n, int source,
            double simTime, String eventType, SimulationResult result) {

        int edges = graph.edgeCount();

        // --- Dijkstra ---
        long t0 = System.nanoTime();
        DijkstraSSSP.compute(graph, source);
        double dijMs = (System.nanoTime() - t0) / 1_000_000.0;

        // --- DMMSY ---
        t0 = System.nanoTime();
        DmmsySSSP.compute(graph, source);
        double dmmMs = (System.nanoTime() - t0) / 1_000_000.0;

        result.events.add(new RecalcEvent(simTime, "dijkstra", dijMs, n, edges, eventType));
        result.events.add(new RecalcEvent(simTime, "dmmsy", dmmMs, n, edges, eventType));
        result.dijkstraTimes.add(dijMs);
        result.dmmsyTimes.add(dmmMs);
        result.timeline.add(new double[]{simTime, n, dijMs, dmmMs});
    }
}
