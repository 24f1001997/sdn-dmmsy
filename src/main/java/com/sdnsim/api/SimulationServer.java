package com.sdnsim.api;

import com.sdnsim.benchmark.BenchmarkSuite;
import com.sdnsim.benchmark.BenchmarkSuite.BenchmarkPoint;
import com.sdnsim.simulation.SdnCloudSimRunner;
import com.sdnsim.simulation.SdnCloudSimRunner.CloudSimResult;
import com.sdnsim.simulation.VolatilitySimulator;
import com.sdnsim.simulation.VolatilitySimulator.SimulationResult;
import com.sdnsim.simulation.VolatilitySimulator.RecalcEvent;
import com.sdnsim.visualization.ChartGenerator;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * SimulationServer.java — Embedded HTTP server for the SDN Simulator.
 * <p>
 * Serves both the REST API and the static frontend from a single process,
 * making it fully self-contained for cloud deployment.
 * <p>
 * Uses only JDK built-in {@code com.sun.net.httpserver} — no external
 * web framework dependencies.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /api/run}    — Run full simulation and return all results as JSON</li>
 *   <li>{@code GET /api/status} — Check if server is ready</li>
 *   <li>{@code GET /}           — Serve the frontend dashboard (static files)</li>
 * </ul>
 *
 * <h2>Cloud Deployment</h2>
 * <ul>
 *   <li>Reads {@code PORT} from environment variable (defaults to 8085)</li>
 *   <li>Binds to {@code 0.0.0.0} for container networking</li>
 *   <li>Serves frontend from classpath ({@code /static/} inside the JAR)</li>
 *   <li>Single fat JAR: {@code java -jar sdn-dmmsy-simulator-1.0.0.jar}</li>
 * </ul>
 *
 * @author SDN Routing Simulator Project
 */
public final class SimulationServer {

    /** Default port if PORT env var is not set. */
    private static final int DEFAULT_PORT = 8085;

    // Configuration (mirrored from SdnSimulatorApp)
    private static final int[] SCALE_POINTS = {500, 1000, 2500, 5000, 10_000, 25_000, 50_000, 100_000};
    private static final int EDGES_PER_NODE = 3;
    private static final double MIN_WEIGHT = 1.0;
    private static final double MAX_WEIGHT = 50.0;
    private static final long SEED = 42L;
    private static final int SIM_BASE_NODES = 5000;
    private static final double SIM_VOLATILITY_PCT = 0.25;
    private static final String OUTPUT_DIR = "output";

    /** MIME types for static file serving. */
    private static final Map<String, String> MIME_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css",  "text/css; charset=utf-8",
            "js",   "application/javascript; charset=utf-8",
            "png",  "image/png",
            "jpg",  "image/jpeg",
            "svg",  "image/svg+xml",
            "ico",  "image/x-icon",
            "json", "application/json"
    );

    // State
    private static volatile boolean running = false;

    private SimulationServer() {}

    public static void main(String[] args) throws IOException {
        // Read port from environment (cloud platforms set PORT)
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException e) {
                System.err.println("[WARN] Invalid PORT env var '" + envPort + "', using default " + DEFAULT_PORT);
            }
        }

        // Bind to 0.0.0.0 for container networking
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(4));

        // API endpoints
        server.createContext("/api/status", SimulationServer::handleStatus);
        server.createContext("/api/run", SimulationServer::handleRun);

        // Static frontend files (served from classpath /static/)
        server.createContext("/", SimulationServer::handleStatic);

        server.start();
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("  SDN Simulator — Cloud-Ready Server");
        System.out.println("  Listening on http://0.0.0.0:" + port);
        System.out.println("  Endpoints:");
        System.out.println("    GET /            — Frontend dashboard");
        System.out.println("    GET /api/status  — Server health check");
        System.out.println("    GET /api/run     — Run full simulation");
        System.out.println("=".repeat(60));
        System.out.println();
    }

    // =================================================================
    //  Static File Handler
    // =================================================================

    /**
     * Serves static files from the classpath {@code /static/} directory.
     * <p>
     * In development, files come from {@code frontend/} copied into
     * {@code target/classes/static/}. In the fat JAR, they are embedded
     * inside the archive.
     */
    private static void handleStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        // Default to index.html for root
        if ("/".equals(path) || path.isBlank()) {
            path = "/index.html";
        }

        // Sanitize: prevent directory traversal
        if (path.contains("..")) {
            exchange.sendResponseHeaders(403, -1);
            return;
        }

        // Load from classpath: /static/index.html, /static/styles.css, etc.
        String resourcePath = "/static" + path;
        InputStream is = SimulationServer.class.getResourceAsStream(resourcePath);

        if (is == null) {
            // 404
            byte[] msg = "404 Not Found".getBytes("UTF-8");
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(404, msg.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(msg);
            }
            return;
        }

        // Determine MIME type
        String ext = "";
        int dot = path.lastIndexOf('.');
        if (dot >= 0) ext = path.substring(dot + 1).toLowerCase();
        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

        // Stream the file
        byte[] data = is.readAllBytes();
        is.close();

        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.getResponseHeaders().add("Cache-Control", "public, max-age=3600");
        exchange.sendResponseHeaders(200, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    // =================================================================
    //  API Handlers
    // =================================================================

    private static void handleStatus(HttpExchange exchange) throws IOException {
        String json = "{\"status\":\"ready\",\"running\":" + running + "}";
        sendJson(exchange, 200, json);
    }

    private static void handleRun(HttpExchange exchange) throws IOException {
        if (running) {
            sendJson(exchange, 409, "{\"error\":\"Simulation already running\"}");
            return;
        }

        running = true;
        try {
            String json = runFullSimulation();
            sendJson(exchange, 200, json);
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        } finally {
            running = false;
        }
    }

    private static String runFullSimulation() throws IOException {
        long totalStart = System.nanoTime();

        // Phase 1: Scaling benchmark
        System.out.println("[API] Running Phase 1: Scaling Benchmark...");
        List<BenchmarkPoint> benchmarkData = BenchmarkSuite.runBenchmarks(
                SCALE_POINTS, EDGES_PER_NODE, MIN_WEIGHT, MAX_WEIGHT, SEED);

        // Phase 2: CloudSim Plus
        System.out.println("[API] Running Phase 2: CloudSim Plus...");
        CloudSimResult csResult = SdnCloudSimRunner.runCloudSimulation(
                SIM_BASE_NODES, EDGES_PER_NODE, SEED);

        // Phase 3: Volatility Simulation
        System.out.println("[API] Running Phase 3: Volatility Simulation...");
        SimulationResult simResult = VolatilitySimulator.runSimulation(
                SIM_BASE_NODES, SIM_VOLATILITY_PCT, 3, 5, 2,
                EDGES_PER_NODE, SEED);

        // Phase 4: Generate charts
        System.out.println("[API] Running Phase 4: Generating Charts...");
        ChartGenerator.generateAllCharts(benchmarkData, simResult, OUTPUT_DIR);

        double totalElapsed = (System.nanoTime() - totalStart) / 1_000_000_000.0;

        // Build JSON response
        StringBuilder sb = new StringBuilder(4096);
        sb.append("{\n");

        // Meta
        sb.append("  \"meta\": {\n");
        sb.append("    \"totalExecutionTimeSec\": ").append(round(totalElapsed, 3)).append(",\n");
        sb.append("    \"chartsGenerated\": 3,\n");
        sb.append("    \"outputDir\": \"./output/\"\n");
        sb.append("  },\n");

        // Phase 1: Benchmark data
        sb.append("  \"benchmark\": [\n");
        for (int i = 0; i < benchmarkData.size(); i++) {
            BenchmarkPoint bp = benchmarkData.get(i);
            sb.append("    {");
            sb.append("\"nodes\":").append(bp.nodes()).append(",");
            sb.append("\"edges\":").append(bp.edges()).append(",");
            sb.append("\"dijkstraMs\":").append(round(bp.dijkstraMs(), 3)).append(",");
            sb.append("\"dmmsyMs\":").append(round(bp.dmmsyMs(), 3)).append(",");
            sb.append("\"speedup\":").append(round(bp.speedup(), 3)).append(",");
            sb.append("\"correct\":").append(bp.correct());
            sb.append("}");
            if (i < benchmarkData.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        // Phase 2: CloudSim data
        sb.append("  \"cloudSim\": {\n");
        sb.append("    \"numHosts\": ").append(csResult.numHosts()).append(",\n");
        sb.append("    \"numVms\": ").append(csResult.numVms()).append(",\n");
        sb.append("    \"totalCloudlets\": ").append(csResult.totalCloudlets()).append(",\n");
        sb.append("    \"totalSimulationTimeSec\": ").append(round(csResult.totalSimulationTime(), 3)).append(",\n");
        sb.append("    \"dijkstraTotalMs\": ").append(round(csResult.dijkstraTotalMs(), 3)).append(",\n");
        sb.append("    \"dmmsyTotalMs\": ").append(round(csResult.dmmsyTotalMs(), 3)).append("\n");
        sb.append("  },\n");

        // Phase 3: Volatility simulation data
        sb.append("  \"volatility\": {\n");
        sb.append("    \"baseNodes\": ").append(simResult.baseNodes).append(",\n");
        sb.append("    \"newNodes\": ").append(simResult.volatilityNewNodes).append(",\n");
        sb.append("    \"peakNodes\": ").append(simResult.peakNodes()).append(",\n");
        sb.append("    \"dijkstraMeanMs\": ").append(round(simResult.dijkstraMeanMs(), 3)).append(",\n");
        sb.append("    \"dmmsyMeanMs\": ").append(round(simResult.dmmsyMeanMs(), 3)).append(",\n");
        sb.append("    \"meanSpeedup\": ").append(round(simResult.meanSpeedup(), 3)).append(",\n");
        sb.append("    \"totalEvents\": ").append(simResult.events.size()).append(",\n");

        // Timeline data
        sb.append("    \"timeline\": [\n");
        for (int i = 0; i < simResult.timeline.size(); i++) {
            double[] point = simResult.timeline.get(i);
            sb.append("      {");
            sb.append("\"simTime\":").append(round(point[0], 2)).append(",");
            sb.append("\"numNodes\":").append((int) point[1]).append(",");
            sb.append("\"dijkstraMs\":").append(round(point[2], 3)).append(",");
            sb.append("\"dmmsyMs\":").append(round(point[3], 3));
            sb.append("}");
            if (i < simResult.timeline.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ],\n");

        // Events detail
        sb.append("    \"events\": [\n");
        for (int i = 0; i < simResult.events.size(); i++) {
            RecalcEvent evt = simResult.events.get(i);
            sb.append("      {");
            sb.append("\"simTime\":").append(round(evt.simTime(), 2)).append(",");
            sb.append("\"algorithm\":\"").append(evt.algorithm()).append("\",");
            sb.append("\"wallTimeMs\":").append(round(evt.wallTimeMs(), 3)).append(",");
            sb.append("\"numNodes\":").append(evt.numNodes()).append(",");
            sb.append("\"numEdges\":").append(evt.numEdges()).append(",");
            sb.append("\"eventType\":\"").append(evt.eventType()).append("\"");
            sb.append("}");
            if (i < simResult.events.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ]\n");
        sb.append("  }\n");

        sb.append("}");

        System.out.println("[API] Simulation complete. Total: " + round(totalElapsed, 2) + "s");
        return sb.toString();
    }

    // --- Helpers ---

    private static void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static double round(double value, int places) {
        double factor = Math.pow(10, places);
        return Math.round(value * factor) / factor;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
