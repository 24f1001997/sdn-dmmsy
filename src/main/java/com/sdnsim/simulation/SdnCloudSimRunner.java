package com.sdnsim.simulation;

import com.sdnsim.algorithm.DijkstraSSSP;
import com.sdnsim.algorithm.DmmsySSSP;
import com.sdnsim.graph.AdjacencyListGraph;
import com.sdnsim.graph.TopologyGenerator;

import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * SdnCloudSimRunner.java — CloudSim Plus Integration for SDN Simulation.
 * <p>
 * Creates a CloudSim Plus simulation environment where:
 * <ul>
 *   <li>A Datacenter hosts physical machines (Hosts)</li>
 *   <li>VMs represent SDN network nodes</li>
 *   <li>Cloudlets represent route-computation tasks (SSSP executions)</li>
 * </ul>
 * <p>
 * Each route computation (Dijkstra or DMMSY) is modeled as a Cloudlet
 * with MI (Million Instructions) proportional to the measured wall-clock
 * time, creating a realistic mapping between algorithmic performance
 * and cloud resource consumption.
 *
 * @author SDN Routing Simulator Project
 */
public final class SdnCloudSimRunner {

    /** Results from a CloudSim Plus simulation run. */
    public record CloudSimResult(
            int totalCloudlets,
            double totalSimulationTime,
            double dijkstraTotalMs,
            double dmmsyTotalMs,
            int numHosts,
            int numVms
    ) {}

    private SdnCloudSimRunner() {
        // Utility class
    }

    /**
     * Run a CloudSim Plus simulation that models SDN route computations
     * as cloud workloads.
     *
     * @param baseNodes      initial network size for topology generation
     * @param edgesPerNode   sparsity parameter
     * @param seed           random seed
     * @return CloudSim simulation results
     */
    public static CloudSimResult runCloudSimulation(
            int baseNodes, int edgesPerNode, long seed) {

        // ---------------------------------------------------------------
        // Step 1: Generate network topology and measure SSSP times
        // ---------------------------------------------------------------
        AdjacencyListGraph graph = TopologyGenerator.generateCloudTopology(
                baseNodes, edgesPerNode, 1.0, 50.0, seed);
        int n = graph.nodeCount();
        int source = 0;

        // Measure Dijkstra execution time
        long t0 = System.nanoTime();
        DijkstraSSSP.compute(graph, source);
        double dijkstraMs = (System.nanoTime() - t0) / 1_000_000.0;

        // Measure DMMSY execution time
        t0 = System.nanoTime();
        DmmsySSSP.compute(graph, source);
        double dmmsyMs = (System.nanoTime() - t0) / 1_000_000.0;

        // ---------------------------------------------------------------
        // Step 2: Create CloudSim Plus simulation
        // ---------------------------------------------------------------
        final var simulation = new CloudSimPlus();

        // ---------------------------------------------------------------
        // Step 3: Create Datacenter with Hosts
        //
        // We model the SDN controller infrastructure as a datacenter.
        // Number of hosts scales with network size (1 host per 500 nodes).
        // ---------------------------------------------------------------
        int numHosts = Math.max(2, baseNodes / 500);
        List<HostSimple> hostList = new ArrayList<>();
        for (int i = 0; i < numHosts; i++) {
            // Each host: 16 GB RAM, 10 Gbps BW, 1 TB storage, 8 PEs @ 4000 MIPS
            List<Pe> peList = new ArrayList<>();
            for (int p = 0; p < 8; p++) {
                peList.add(new PeSimple(4000));
            }
            HostSimple host = new HostSimple(16384L, 10000L, 1000000L, peList);
            hostList.add(host);
        }

        new DatacenterSimple(simulation, hostList);

        // ---------------------------------------------------------------
        // Step 4: Create Broker (SDN Controller agent)
        // ---------------------------------------------------------------
        final var broker = new DatacenterBrokerSimple(simulation);

        // ---------------------------------------------------------------
        // Step 5: Create VMs (representing SDN controller instances)
        // ---------------------------------------------------------------
        int numVms = Math.max(2, numHosts * 2);
        List<VmSimple> vmList = new ArrayList<>();
        for (int i = 0; i < numVms; i++) {
            VmSimple vm = new VmSimple(2000, 4); // 2000 MIPS, 4 PEs
            vm.setRam(4096).setBw(2000).setSize(50000);
            vmList.add(vm);
        }
        broker.submitVmList(vmList);

        // ---------------------------------------------------------------
        // Step 6: Create Cloudlets (route-computation tasks)
        //
        // Model each SSSP computation as a Cloudlet with MI proportional
        // to measured wall-clock time. This creates a realistic mapping
        // between algorithmic performance and cloud resource consumption.
        //
        // MI = wall_time_ms * MIPS_PER_MS_FACTOR
        // ---------------------------------------------------------------
        double mipsPerMsFactor = 1000.0; // 1 ms of computation = 1000 MI
        List<CloudletSimple> cloudletList = new ArrayList<>();

        // Dijkstra route computation cloudlets (3 baseline + 5 volatility + 2 post)
        for (int i = 0; i < 10; i++) {
            long length = Math.max(100, (long) (dijkstraMs * mipsPerMsFactor));
            CloudletSimple cl = new CloudletSimple(length, 2);
            cl.setUtilizationModelCpu(new UtilizationModelDynamic(0.8));
            cloudletList.add(cl);
        }

        // DMMSY route computation cloudlets
        for (int i = 0; i < 10; i++) {
            long length = Math.max(100, (long) (dmmsyMs * mipsPerMsFactor));
            CloudletSimple cl = new CloudletSimple(length, 2);
            cl.setUtilizationModelCpu(new UtilizationModelDynamic(0.6));
            cloudletList.add(cl);
        }

        broker.submitCloudletList(cloudletList);

        // ---------------------------------------------------------------
        // Step 7: Run the simulation
        // ---------------------------------------------------------------
        simulation.start();

        double simEndTime = simulation.clock();

        return new CloudSimResult(
                cloudletList.size(),
                simEndTime,
                dijkstraMs,
                dmmsyMs,
                numHosts,
                numVms
        );
    }
}
