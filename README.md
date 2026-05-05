# 🌐 SDN Routing Simulator: Dijkstra vs. DMMSY Algorithm

> **Breaking the 65-Year Sorting Barrier for Single-Source Shortest Paths**

A production-grade Software-Defined Networking (SDN) routing simulator that benchmarks the classical **Dijkstra algorithm** against the groundbreaking **2025 DMMSY algorithm** (STOC 2025 Best Paper).

Built with **Java 17**, **CloudSim Plus 8.0**, and **JFreeChart**.

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    SdnSimulatorApp (Entry Point)                │
│          Orchestrates benchmarks, simulation & charts           │
├─────────────┬─────────────┬─────────────┬───────────────────────┤
│  Benchmark  │  CloudSim   │  Volatility │    Visualization      │
│   Suite     │  Plus Sim   │  Simulator  │    (JFreeChart)       │
├─────────────┴──────┬──────┴─────────────┴───────────────────────┤
│              Algorithm Layer                                    │
│    ┌────────────────────┐  ┌──────────────────────────┐         │
│    │  DijkstraSSSP      │  │  DmmsySSSP (BMSSP)       │         │
│    │  O(m + n log n)    │  │  O(m · log^{2/3} n)      │         │
│    └────────────────────┘  └──────────────────────────┘         │
├─────────────────────────────────────────────────────────────────┤
│              Graph Layer                                        │
│    AdjacencyListGraph  ←  TopologyGenerator (Barabási-Albert)   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧠 The Algorithmic Breakthrough

### The Problem: Dijkstra's Sorting Barrier

For 65 years, Dijkstra's algorithm has been the gold standard for Single-Source Shortest Paths (SSSP). It requires a **global priority queue** that imposes a strict total ordering on all unexplored vertices, resulting in:

**O(m + n log n)** time complexity

The `n log n` term comes from the **sorting barrier** — the priority queue must sort all n vertices by distance, which is provably unavoidable in Dijkstra's framework.

### The Solution: DMMSY (STOC 2025 Best Paper)

The DMMSY algorithm by Duan, Mao, Mao, Shu, and Yin **completely bypasses** the sorting requirement through three innovations:

1. **Partial-Sorting Heap** — Bucket-based structure with O(1) amortized insert/decrease-key
2. **Pivot Selection** — Partitions the frontier into localized distance bands
3. **BMSSP (Bounded Multi-Source Shortest Path)** — Recursive subroutine using bounded Bellman-Ford relaxation within each band

**Result:** **O(m · log^{2/3} n)** — strictly faster than Dijkstra on sparse graphs

---

## ⚙️ Prerequisites

| Tool | Version | Required |
|------|---------|----------|
| **JDK** | 17+ | ✅ |
| **Apache Maven** | 3.8+ | ✅ |
| **Git** | Any | Optional |

Verify your setup:
```bash
java --version    # Must be 17+
mvn --version     # Must be 3.8+
```

---

## 🚀 Quick Start

### 1. Build the project
```bash
mvn compile
```

### 2. Run the full simulation
```bash
mvn exec:java -Dexec.mainClass="com.sdnsim.SdnSimulatorApp"
```

Or combine both:
```bash
mvn compile exec:java
```

### 3. View results
- Console output shows benchmark table with timing and correctness
- Charts saved to `output/` directory:
  - `scaling_comparison.png` — Execution time vs. nodes
  - `speedup_ratio.png` — DMMSY speedup factor
  - `volatility_timeline.png` — Auto-scaling event response

---

## 📊 Expected Output

```
+--------------------------------------------------------------+
|   SDN Routing Simulator: Dijkstra vs. DMMSY (STOC 2025)     |
|   Breaking the Sorting Barrier for Shortest Paths            |
|   Java 17 + CloudSim Plus 8.0 + JFreeChart                  |
+--------------------------------------------------------------+

======================================================================
  PHASE 1: Scaling Benchmark -- Dijkstra vs. DMMSY
======================================================================

     Nodes       Edges  Dijkstra(ms)    DMMSY(ms)   Speedup    Status
  --------------------------------------------------------------------
       500       2,988         0.xxx        0.xxx    0.xxx       OK
     1,000       5,988         0.xxx        0.xxx    0.xxx       OK
     ...         ...           ...          ...      ...         OK
   100,000     599,988       xxx.xxx      xxx.xxx    x.xxxx      OK

  == All correctness checks PASSED ==

======================================================================
  PHASE 2: CloudSim Plus SDN Simulation
======================================================================
  ...

======================================================================
  PHASE 3: SDN Volatility Simulation (Auto-Scaling Event)
======================================================================
  ...

======================================================================
  PHASE 4: Generating Visualizations
======================================================================
    [+] Saved: output/scaling_comparison.png
    [+] Saved: output/speedup_ratio.png
    [+] Saved: output/volatility_timeline.png
```

---

## 📁 Project Structure

```
sdn-dmmsy/
├── pom.xml                                    Maven build configuration
├── README.md                                  This file
├── src/main/java/com/sdnsim/
│   ├── SdnSimulatorApp.java                   Entry point (main)
│   ├── algorithm/
│   │   ├── SsspResult.java                    SSSP result container
│   │   ├── DijkstraSSSP.java                  Baseline Dijkstra
│   │   └── DmmsySSSP.java                     DMMSY algorithm
│   ├── graph/
│   │   ├── Edge.java                          Weighted edge record
│   │   ├── AdjacencyListGraph.java            Graph data structure
│   │   └── TopologyGenerator.java             BA topology generator
│   ├── simulation/
│   │   ├── SdnCloudSimRunner.java             CloudSim Plus integration
│   │   └── VolatilitySimulator.java           Auto-scaling simulation
│   ├── benchmark/
│   │   └── BenchmarkSuite.java                Scaling benchmarks
│   └── visualization/
│       └── ChartGenerator.java                JFreeChart charts
└── output/                                    Generated at runtime
    ├── scaling_comparison.png
    ├── speedup_ratio.png
    └── volatility_timeline.png
```

---

## 🔬 How It Works

### 1. Topology Generation
Uses the **Barabási-Albert** preferential attachment model to generate scale-free networks with power-law degree distributions — realistic for cloud/datacenter topologies.

### 2. Algorithm Comparison
Both Dijkstra and DMMSY compute SSSP from the same source vertex on identical topologies. Results are verified for correctness (distance tolerance: 1e-6).

### 3. CloudSim Plus Integration
Route computations are modeled as **Cloudlets** (cloud tasks) within a virtual **Datacenter**. This demonstrates how algorithmic efficiency translates to cloud resource savings.

### 4. Volatility Simulation
Simulates a **massive auto-scaling event** (25% node increase in 5 waves), measuring how quickly each algorithm adapts to topology changes.

---

## 📝 License

MIT License — See individual source files for details.

---

## 📚 References

1. Duan, R., Mao, J., Mao, X., Shu, X., & Yin, L. (2025). "Breaking the Sorting Barrier for Directed Single-Source Shortest Paths." *STOC 2025 Best Paper*.
2. CloudSim Plus 8.0 — [cloudsimplus.org](https://cloudsimplus.org/)
3. Barabási, A. L., & Albert, R. (1999). "Emergence of Scaling in Random Networks." *Science*.
