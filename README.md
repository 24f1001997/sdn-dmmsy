# 🌐 SDN Routing Simulator: Dijkstra vs. DMMSY Algorithm

> **Breaking the 65-Year Sorting Barrier for Single-Source Shortest Paths**

A production-grade Software-Defined Networking (SDN) routing simulator that benchmarks the classical **Dijkstra algorithm** against the groundbreaking **2025 DMMSY algorithm** (STOC 2025 Best Paper). Features a **live web dashboard** with interactive Chart.js visualizations powered by an embedded REST API server.

Built with **Java 17**, **CloudSim Plus 8.0**, **JFreeChart**, and a **vanilla HTML/CSS/JS frontend**.

---

## 📐 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Frontend (Browser)                            │
│   index.html + styles.css + app.js (Chart.js interactive charts)   │
├────────────────────────────┬────────────────────────────────────────┤
│         REST API           │        HTTP Server (JDK built-in)     │
│  GET /api/status           │        SimulationServer.java          │
│  GET /api/run              │        Port 8085                      │
├────────────────────────────┴────────────────────────────────────────┤
│                    SdnSimulatorApp (CLI Entry Point)                │
│          Orchestrates benchmarks, simulation & charts               │
├──────────────┬──────────────┬──────────────┬────────────────────────┤
│  Benchmark   │  CloudSim    │  Volatility  │    Visualization       │
│   Suite      │  Plus Sim    │  Simulator   │    (JFreeChart)        │
├──────────────┴──────┬───────┴──────────────┴────────────────────────┤
│              Algorithm Layer                                        │
│    ┌────────────────────┐  ┌──────────────────────────┐             │
│    │  DijkstraSSSP      │  │  DmmsySSSP (BMSSP)       │             │
│    │  O(m + n log n)    │  │  O(m · log^{2/3} n)      │             │
│    └────────────────────┘  └──────────────────────────┘             │
├─────────────────────────────────────────────────────────────────────┤
│              Graph Layer                                            │
│    AdjacencyListGraph  ←  TopologyGenerator (Barabási-Albert)       │
└─────────────────────────────────────────────────────────────────────┘
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
| **Git** | Any | Optional |

> **Note:** Apache Maven is **not** required as a system install. The project includes a **Maven Wrapper** (`mvnw.cmd`) that automatically downloads Maven 3.9.9 on first run.

Verify your setup:
```bash
java --version    # Must be 17+
```

If `JAVA_HOME` is not set, point it to your JDK installation:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"   # Adjust path as needed
```

---

## 🚀 Quick Start

### Option A: CLI Mode (Headless)

Run the full 4-phase simulation from the terminal with JFreeChart chart output:

```bash
# Build and run
.\mvnw.cmd compile exec:java
```

This executes `SdnSimulatorApp.main()` which runs:
1. **Scaling Benchmark** — Dijkstra vs DMMSY across 8 scale points (500 → 100K nodes)
2. **CloudSim Plus Simulation** — SDN route computations modeled as Cloud workloads
3. **Volatility Simulation** — 25% auto-scaling burst with 5 injection waves
4. **Chart Generation** — 3 dark-themed PNG charts saved to `output/`

### Option B: Web Dashboard (Interactive)

Launch the embedded API server and open the live frontend:

```bash
# 1. Start the API server (port 8085)
.\mvnw.cmd compile exec:java -Dexec.mainClass="com.sdnsim.api.SimulationServer"

# 2. Open the dashboard in your browser
#    Open frontend/index.html directly, or serve it:
start frontend\index.html
```

The dashboard features:
- **Interactive Chart.js charts** (Scaling, Speedup, Volatility Timeline)
- **KPI strip** with live metrics (Peak Nodes, Speedup, Hosts/VMs, Cloudlets, Total Time, Correctness)
- **Benchmark results table** with all scale points
- **Cloud architecture diagram** rendered on canvas
- **Algorithm deep-dive** with visual complexity comparison
- **Re-run button** to trigger new simulation runs from the UI

**API Endpoints:**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/status` | GET | Server health check, returns `{"status":"ready","running":false}` |
| `/api/run` | GET | Runs all 4 simulation phases and returns full JSON results |

---

## 📊 Expected CLI Output

```
+--------------------------------------------------------------+
|   SDN Routing Simulator: Dijkstra vs. DMMSY (STOC 2025)     |
|   Breaking the Sorting Barrier for Shortest Paths            |
|   Java 17 + CloudSim Plus 8.0 + JFreeChart                  |
+--------------------------------------------------------------+

======================================================================
  PHASE 1: Scaling Benchmark -- Dijkstra vs. DMMSY
======================================================================

     Nodes       Edges    Dijkstra(ms)     DMMSY(ms)   Speedup    Status
  --------------------------------------------------------------------
    [OK] Correctness PASSED (n=500)  -- all 500 distances match.
       500       2,988           3.044        3.080     0.99x        OK
    [OK] Correctness PASSED (n=1000) -- all 1000 distances match.
     1,000       5,988           1.106        2.174     0.51x        OK
      ...         ...             ...          ...       ...         OK
    [OK] Correctness PASSED (n=100000) -- all 100000 distances match.
   100,000     599,988          95.123       57.294     1.66x        OK

  == All correctness checks PASSED ==

======================================================================
  PHASE 2: CloudSim Plus SDN Simulation
======================================================================
  Hosts deployed       : 10
  VMs created          : 20
  Cloudlets executed   : 20

======================================================================
  PHASE 3: SDN Volatility Simulation (Auto-Scaling Event)
======================================================================
  Base topology  : 5,000 nodes
  Volatility     : +1,250 nodes (25% burst)
  Peak size      : 6,250 nodes

======================================================================
  PHASE 4: Generating Visualizations
======================================================================
    [+] Saved: output/scaling_comparison.png
    [+] Saved: output/speedup_ratio.png
    [+] Saved: output/volatility_timeline.png

  Total execution time: ~3 seconds
```

---

## 📁 Project Structure

```
sdn-dmmsy/
├── pom.xml                                    Maven build configuration
├── mvnw.cmd                                   Maven Wrapper (no install needed)
├── .mvn/wrapper/
│   └── maven-wrapper.properties               Wrapper config (Maven 3.9.9)
├── README.md                                  This file
│
├── frontend/                                  Web Dashboard (static files)
│   ├── index.html                             Dashboard page with Chart.js
│   ├── styles.css                             Dark theme styles
│   └── app.js                                 Frontend logic & chart rendering
│
├── src/main/java/com/sdnsim/
│   ├── SdnSimulatorApp.java                   CLI entry point (main)
│   │
│   ├── api/
│   │   └── SimulationServer.java              Embedded HTTP server (port 8085)
│   │
│   ├── algorithm/
│   │   ├── SsspResult.java                    SSSP result container (record)
│   │   ├── DijkstraSSSP.java                  Baseline Dijkstra (binary heap)
│   │   └── DmmsySSSP.java                     DMMSY algorithm (BMSSP bands)
│   │
│   ├── graph/
│   │   ├── Edge.java                          Weighted edge record
│   │   ├── AdjacencyListGraph.java            Sparse graph data structure
│   │   └── TopologyGenerator.java             Barabási-Albert generator
│   │
│   ├── simulation/
│   │   ├── SdnCloudSimRunner.java             CloudSim Plus integration
│   │   └── VolatilitySimulator.java           Auto-scaling event simulator
│   │
│   ├── benchmark/
│   │   └── BenchmarkSuite.java                Scaling benchmarks + correctness
│   │
│   └── visualization/
│       └── ChartGenerator.java                JFreeChart dark-theme charts
│
├── src/main/resources/
│   └── logback.xml                            Logging config (suppresses CloudSim noise)
│
└── output/                                    Generated at runtime
    ├── scaling_comparison.png                 Execution time vs nodes
    ├── speedup_ratio.png                      DMMSY speedup factor
    └── volatility_timeline.png                Auto-scaling latency response
```

---

## 🔬 How It Works

### 1. Topology Generation
Uses the **Barabási-Albert** preferential attachment model to generate scale-free networks with power-law degree distributions — realistic for cloud/datacenter topologies.

### 2. Algorithm Comparison
Both Dijkstra and DMMSY compute SSSP from the same source vertex on identical topologies. Results are verified for correctness (distance tolerance: 1e-6).

### 3. CloudSim Plus Integration
Route computations are modeled as **Cloudlets** (cloud tasks) within a virtual **Datacenter** with Hosts, VMs, and PEs. This demonstrates how algorithmic efficiency translates to cloud resource savings.

### 4. Volatility Simulation
Simulates a **massive auto-scaling event** (25% node increase in 5 waves), measuring how quickly each algorithm adapts to sudden topology changes across 3 phases: baseline → volatility → post-volatility.

### 5. Web Dashboard
The frontend connects to the embedded `SimulationServer` via REST API (`/api/run`), receives full JSON results, and renders interactive Chart.js visualizations including a cloud architecture diagram, KPI metrics, benchmark table, and algorithm complexity comparison — all in a premium dark-themed UI.

---

## 🛠 Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 17+ (tested on 23) |
| **Build** | Maven (via Wrapper) | 3.9.9 |
| **Cloud Simulation** | CloudSim Plus | 8.0.0 |
| **Static Charts** | JFreeChart | 1.5.5 |
| **Interactive Charts** | Chart.js | 4.4.6 |
| **Frontend** | HTML + CSS + Vanilla JS | — |
| **API Server** | JDK HttpServer | Built-in |
| **Typography** | Inter, JetBrains Mono | Google Fonts |

---

## 📝 License

MIT License — See individual source files for details.

---

## 📚 References

1. Duan, R., Mao, J., Mao, X., Shu, X., & Yin, L. (2025). "Breaking the Sorting Barrier for Directed Single-Source Shortest Paths." *STOC 2025 Best Paper*.
2. CloudSim Plus 8.0 — [cloudsimplus.org](https://cloudsimplus.org/)
3. Barabási, A. L., & Albert, R. (1999). "Emergence of Scaling in Random Networks." *Science*.
