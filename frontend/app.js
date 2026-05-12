/* ═══════════════════════════════════════════════════════════════════
   SDN Routing Simulator — Dashboard Application Logic
   Connects to the Java API server, fetches simulation data, and
   renders interactive Chart.js visualizations + animated cloud
   architecture diagram.
   ═══════════════════════════════════════════════════════════════════ */

const API_BASE = 'http://localhost:8085';

// ─── State ──────────────────────────────────────────────────────
let simData = null;
let charts = {};
let archAnimFrame = null;

// ─── DOM References ─────────────────────────────────────────────
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

// ─── Init ───────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    $('#btn-rerun').addEventListener('click', rerunSimulation);
    startSimulation();
});

// ═══════════════════════════════════════════════════════════════════
//  SIMULATION FLOW
// ═══════════════════════════════════════════════════════════════════

async function startSimulation() {
    showLoading();
    updateLoadingStatus('Connecting to simulation server...');
    setLoadingBar(5);

    try {
        // Check server status
        const status = await fetchWithRetry(`${API_BASE}/api/status`, 8, 1500);
        if (!status) {
            updateLoadingStatus('Server not reachable. Make sure API is running on port 8085.');
            return;
        }

        setPhase(1, 'active');
        updateLoadingStatus('Running Phase 1: Scaling Benchmark...');
        setLoadingBar(15);

        // Fetch simulation data
        setPhase(2, 'active');
        setPhase(1, 'done');
        updateLoadingStatus('Running Phase 2: CloudSim Plus Simulation...');
        setLoadingBar(35);

        const response = await fetch(`${API_BASE}/api/run`);
        if (!response.ok) throw new Error(`Server returned ${response.status}`);

        setPhase(2, 'done');
        setPhase(3, 'active');
        updateLoadingStatus('Running Phase 3: Volatility Simulation...');
        setLoadingBar(60);

        simData = await response.json();

        setPhase(3, 'done');
        setPhase(4, 'active');
        updateLoadingStatus('Rendering visualizations...');
        setLoadingBar(85);

        // Small delay for visual effect
        await sleep(400);

        setPhase(4, 'done');
        setLoadingBar(100);
        updateLoadingStatus('Complete!');

        await sleep(600);

        hideLoading();
        renderDashboard();

    } catch (err) {
        console.error('Simulation error:', err);
        updateLoadingStatus(`Error: ${err.message}`);
        setLoadingBar(0);
    }
}

async function rerunSimulation() {
    // Destroy existing charts
    Object.values(charts).forEach(c => c.destroy && c.destroy());
    charts = {};
    if (archAnimFrame) cancelAnimationFrame(archAnimFrame);

    // Reset cache on server by calling with force
    simData = null;
    $('#dashboard').classList.add('hidden');
    startSimulation();
}

// ═══════════════════════════════════════════════════════════════════
//  RENDER DASHBOARD
// ═══════════════════════════════════════════════════════════════════

function renderDashboard() {
    $('#dashboard').classList.remove('hidden');
    $('#footer-timestamp').textContent = new Date().toLocaleString();

    renderKPIs();
    renderScalingChart();
    renderSpeedupChart();
    renderVolatilityChart();
    renderBenchmarkTable();
    renderAlgoComparison();
    // Delay architecture render to ensure layout is computed after un-hiding
    requestAnimationFrame(() => {
        requestAnimationFrame(() => {
            renderCloudArchitecture();
        });
    });
}

// ─── KPIs ───────────────────────────────────────────────────────
function renderKPIs() {
    const d = simData;

    animateNumber($('#kpi-nodes-val'), 0, d.volatility.peakNodes, 1200, (v) => v.toLocaleString());
    animateNumber($('#kpi-speedup-val'), 0, d.volatility.meanSpeedup, 1000, (v) => v.toFixed(2) + 'x');
    $('#kpi-hosts-val').textContent = `${d.cloudSim.numHosts} / ${d.cloudSim.numVms}`;
    animateNumber($('#kpi-cloudlets-val'), 0, d.cloudSim.totalCloudlets, 800, (v) => Math.round(v).toString());
    animateNumber($('#kpi-time-val'), 0, d.meta.totalExecutionTimeSec, 1000, (v) => v.toFixed(2) + 's');

    const allCorrect = d.benchmark.every(bp => bp.correct);
    const el = $('#kpi-correct-val');
    el.textContent = allCorrect ? '✓ PASS' : '✗ FAIL';
    el.style.color = allCorrect ? 'var(--accent-green)' : 'var(--accent-red)';
}

// ─── Scaling Chart ──────────────────────────────────────────────
function renderScalingChart() {
    const ctx = $('#chart-scaling').getContext('2d');
    const bp = simData.benchmark;

    charts.scaling = new Chart(ctx, {
        type: 'line',
        data: {
            labels: bp.map(p => formatNodeCount(p.nodes)),
            datasets: [
                {
                    label: 'Dijkstra  O(m + n log n)',
                    data: bp.map(p => p.dijkstraMs),
                    borderColor: '#ff5555',
                    backgroundColor: 'rgba(255, 85, 85, 0.1)',
                    borderWidth: 2.5,
                    pointBackgroundColor: '#ff5555',
                    pointBorderColor: '#ff5555',
                    pointRadius: 4,
                    pointHoverRadius: 7,
                    fill: true,
                    tension: 0.3,
                },
                {
                    label: 'DMMSY  O(m log²ᐟ³ n)',
                    data: bp.map(p => p.dmmsyMs),
                    borderColor: '#50fa7b',
                    backgroundColor: 'rgba(80, 250, 123, 0.08)',
                    borderWidth: 2.5,
                    pointBackgroundColor: '#50fa7b',
                    pointBorderColor: '#50fa7b',
                    pointRadius: 4,
                    pointHoverRadius: 7,
                    fill: true,
                    tension: 0.3,
                }
            ]
        },
        options: chartOptions('Number of Nodes', 'Execution Time (ms)')
    });
}

// ─── Speedup Chart ──────────────────────────────────────────────
function renderSpeedupChart() {
    const ctx = $('#chart-speedup').getContext('2d');
    const bp = simData.benchmark;

    charts.speedup = new Chart(ctx, {
        type: 'line',
        data: {
            labels: bp.map(p => formatNodeCount(p.nodes)),
            datasets: [
                {
                    label: 'Speedup (Dijkstra / DMMSY)',
                    data: bp.map(p => p.speedup),
                    borderColor: '#8be9fd',
                    backgroundColor: 'rgba(139, 233, 253, 0.08)',
                    borderWidth: 2.5,
                    pointBackgroundColor: '#8be9fd',
                    pointBorderColor: '#8be9fd',
                    pointRadius: 5,
                    pointHoverRadius: 8,
                    fill: true,
                    tension: 0.3,
                },
                {
                    label: '1.0x Break-even',
                    data: bp.map(() => 1.0),
                    borderColor: '#bd93f9',
                    borderWidth: 1.5,
                    borderDash: [6, 4],
                    pointRadius: 0,
                    fill: false,
                }
            ]
        },
        options: chartOptions('Number of Nodes', 'Speedup Factor (x)')
    });
}

// ─── Volatility Chart ───────────────────────────────────────────
function renderVolatilityChart() {
    const ctx = $('#chart-volatility').getContext('2d');
    const tl = simData.volatility.timeline;

    charts.volatility = new Chart(ctx, {
        type: 'line',
        data: {
            labels: tl.map(p => p.simTime + 's'),
            datasets: [
                {
                    label: 'Dijkstra',
                    data: tl.map(p => p.dijkstraMs),
                    borderColor: '#ff5555',
                    backgroundColor: 'rgba(255, 85, 85, 0.08)',
                    borderWidth: 2.5,
                    pointBackgroundColor: '#ff5555',
                    pointBorderColor: '#ff5555',
                    pointRadius: 5,
                    pointHoverRadius: 8,
                    fill: true,
                    tension: 0.3,
                },
                {
                    label: 'DMMSY',
                    data: tl.map(p => p.dmmsyMs),
                    borderColor: '#50fa7b',
                    backgroundColor: 'rgba(80, 250, 123, 0.06)',
                    borderWidth: 2.5,
                    pointBackgroundColor: '#50fa7b',
                    pointBorderColor: '#50fa7b',
                    pointRadius: 5,
                    pointHoverRadius: 8,
                    fill: true,
                    tension: 0.3,
                }
            ]
        },
        options: {
            ...chartOptions('Simulation Time (s)', 'Recalculation Latency (ms)'),
            plugins: {
                ...chartOptions('', '').plugins,
                annotation: undefined
            }
        }
    });
}

// ─── Benchmark Table ────────────────────────────────────────────
function renderBenchmarkTable() {
    const tbody = $('#benchmark-tbody');
    tbody.innerHTML = '';

    simData.benchmark.forEach((bp, idx) => {
        const tr = document.createElement('tr');
        tr.style.animationDelay = `${idx * 0.06}s`;
        tr.classList.add('table-row-anim');

        const speedupClass = bp.speedup >= 1.0 ? 'faster' : 'slower';
        const statusClass = bp.correct ? 'status-ok' : 'status-fail';
        const statusText = bp.correct ? '✓ PASS' : '✗ FAIL';

        tr.innerHTML = `
            <td>${bp.nodes.toLocaleString()}</td>
            <td>${bp.edges.toLocaleString()}</td>
            <td>${bp.dijkstraMs.toFixed(3)}</td>
            <td>${bp.dmmsyMs.toFixed(3)}</td>
            <td class="speedup-cell ${speedupClass}">${bp.speedup.toFixed(2)}x</td>
            <td class="${statusClass}">${statusText}</td>
        `;
        tbody.appendChild(tr);
    });
}

// ─── Algorithm Comparison ───────────────────────────────────────
function renderAlgoComparison() {
    const lastBP = simData.benchmark[simData.benchmark.length - 1];
    const maxMs = Math.max(lastBP.dijkstraMs, lastBP.dmmsyMs);

    setTimeout(() => {
        $('#algo-bar-dij').style.width = `${(lastBP.dijkstraMs / maxMs) * 100}%`;
        $('#algo-bar-dmm').style.width = `${(lastBP.dmmsyMs / maxMs) * 100}%`;
    }, 500);

    $('#algo-time-dij').textContent = `${lastBP.dijkstraMs.toFixed(2)} ms`;
    $('#algo-time-dmm').textContent = `${lastBP.dmmsyMs.toFixed(2)} ms`;
}

// ═══════════════════════════════════════════════════════════════════
//  CLOUD ARCHITECTURE DIAGRAM (Canvas)
// ═══════════════════════════════════════════════════════════════════

function renderCloudArchitecture() {
    const canvas = $('#canvas-arch');
    const container = $('#arch-container');
    const dpr = window.devicePixelRatio || 1;

    canvas.width = container.clientWidth * dpr;
    canvas.height = container.clientHeight * dpr;
    canvas.style.width = container.clientWidth + 'px';
    canvas.style.height = container.clientHeight + 'px';

    const ctx = canvas.getContext('2d');

    const W = container.clientWidth;
    const H = container.clientHeight;

    const cs = simData.cloudSim;
    let time = 0;

    // Architecture nodes
    const controllerNode = { x: W / 2, y: 42, label: 'SDN Controller', color: '#8be9fd', radius: 22 };

    const numHosts = cs.numHosts;
    const numVMs = cs.numVms;

    // Host nodes (middle row)
    const hosts = [];
    const hostSpacing = W / (numHosts + 1);
    for (let i = 0; i < numHosts; i++) {
        hosts.push({
            x: hostSpacing * (i + 1),
            y: 130,
            label: `H${i}`,
            color: '#bd93f9',
            radius: 16
        });
    }

    // VM nodes (bottom row)
    const vms = [];
    const vmSpacing = W / (numVMs + 1);
    for (let i = 0; i < numVMs; i++) {
        vms.push({
            x: vmSpacing * (i + 1),
            y: 218,
            label: `VM${i}`,
            color: '#50fa7b',
            radius: 12
        });
    }

    // Cloudlet indicators
    const cloudlets = [];
    const clPerVM = Math.ceil(cs.totalCloudlets / numVMs);

    // Packet particles
    let particles = [];

    function spawnParticle() {
        // Random path: controller -> host -> vm
        const hostIdx = Math.floor(Math.random() * hosts.length);
        const vmIdx = Math.floor(Math.random() * vms.length);

        const path = [
            { x: controllerNode.x, y: controllerNode.y },
            { x: hosts[hostIdx].x, y: hosts[hostIdx].y },
            { x: vms[vmIdx].x, y: vms[vmIdx].y }
        ];

        particles.push({
            path,
            progress: 0,
            speed: 0.008 + Math.random() * 0.008,
            color: Math.random() > 0.5 ? '#ff5555' : '#50fa7b',
            size: 2 + Math.random() * 2
        });
    }

    function draw() {
        time += 0.016;

        // Reset transform and reapply DPR scale each frame
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        ctx.clearRect(0, 0, W, H);

        // ─── Connections ───
        ctx.shadowBlur = 0;
        ctx.lineWidth = 1;

        // Controller -> Hosts
        hosts.forEach(h => {
            const grad = ctx.createLinearGradient(controllerNode.x, controllerNode.y, h.x, h.y);
            grad.addColorStop(0, 'rgba(139, 233, 253, 0.3)');
            grad.addColorStop(1, 'rgba(189, 147, 249, 0.3)');
            ctx.strokeStyle = grad;
            ctx.beginPath();
            ctx.moveTo(controllerNode.x, controllerNode.y + controllerNode.radius);
            ctx.lineTo(h.x, h.y - h.radius);
            ctx.stroke();
        });

        // Hosts -> VMs
        for (let hi = 0; hi < hosts.length; hi++) {
            const h = hosts[hi];
            // Each host connects to 2 VMs
            for (let vi = hi * 2; vi < Math.min(hi * 2 + 2, vms.length); vi++) {
                const vm = vms[vi];
                ctx.strokeStyle = 'rgba(80, 250, 123, 0.15)';
                ctx.beginPath();
                ctx.moveTo(h.x, h.y + h.radius);
                ctx.lineTo(vm.x, vm.y - vm.radius);
                ctx.stroke();
            }
        }

        // ─── Particles ───
        if (Math.random() < 0.08) spawnParticle();

        particles = particles.filter(p => p.progress < 1);

        particles.forEach(p => {
            p.progress += p.speed;
            const pos = getPathPosition(p.path, p.progress);

            ctx.beginPath();
            ctx.arc(pos.x, pos.y, p.size, 0, Math.PI * 2);
            ctx.fillStyle = p.color;
            ctx.shadowColor = p.color;
            ctx.shadowBlur = 8;
            ctx.fill();
            ctx.shadowBlur = 0;
        });

        // ─── Nodes ───
        // Controller
        drawNode(ctx, controllerNode, time);

        // Hosts
        hosts.forEach(h => drawNode(ctx, h, time));

        // VMs
        vms.forEach(vm => drawNode(ctx, vm, time));

        // ─── Labels ───
        ctx.font = '600 11px Inter, sans-serif';
        ctx.textAlign = 'center';

        // Layer labels
        ctx.fillStyle = 'rgba(220, 220, 230, 0.4)';
        ctx.font = '500 9px Inter, sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText('CONTROL PLANE', 8, 20);
        ctx.fillText('PHYSICAL LAYER', 8, 115);
        ctx.fillText('VIRTUAL LAYER', 8, 203);

        // Bottom legend
        ctx.font = '500 10px Inter, sans-serif';
        ctx.textAlign = 'center';

        // Dijkstra route
        ctx.fillStyle = '#ff5555';
        ctx.beginPath();
        ctx.arc(W / 2 - 80, H - 18, 4, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = 'rgba(220, 220, 230, 0.6)';
        ctx.fillText('Dijkstra Route', W / 2 - 40, H - 14);

        // DMMSY route
        ctx.fillStyle = '#50fa7b';
        ctx.beginPath();
        ctx.arc(W / 2 + 50, H - 18, 4, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = 'rgba(220, 220, 230, 0.6)';
        ctx.fillText('DMMSY Route', W / 2 + 90, H - 14);

        // Stats overlay
        ctx.textAlign = 'right';
        ctx.font = '500 10px JetBrains Mono, monospace';
        ctx.fillStyle = 'rgba(139, 233, 253, 0.6)';
        ctx.fillText(`${cs.numHosts} Hosts · ${cs.numVms} VMs · ${cs.totalCloudlets} Cloudlets`, W - 10, H - 14);

        archAnimFrame = requestAnimationFrame(draw);
    }

    draw();

    // Redraw on resize
    const ro = new ResizeObserver(() => {
        const newW = container.clientWidth;
        const newH = container.clientHeight;
        canvas.width = newW * dpr;
        canvas.height = newH * dpr;
        canvas.style.width = newW + 'px';
        canvas.style.height = newH + 'px';
    });
    ro.observe(container);
}

function drawNode(ctx, node, time) {
    const pulse = Math.sin(time * 2 + node.x * 0.01) * 0.15 + 1;

    // Glow
    ctx.beginPath();
    ctx.arc(node.x, node.y, node.radius * pulse * 1.5, 0, Math.PI * 2);
    ctx.fillStyle = hexToRgba(node.color, 0.06);
    ctx.fill();

    // Node body
    ctx.beginPath();
    ctx.arc(node.x, node.y, node.radius, 0, Math.PI * 2);
    ctx.fillStyle = hexToRgba(node.color, 0.15);
    ctx.strokeStyle = hexToRgba(node.color, 0.5);
    ctx.lineWidth = 1.5;
    ctx.fill();
    ctx.stroke();

    // Inner dot
    ctx.beginPath();
    ctx.arc(node.x, node.y, 3, 0, Math.PI * 2);
    ctx.fillStyle = node.color;
    ctx.fill();

    // Label
    ctx.fillStyle = 'rgba(220, 220, 230, 0.8)';
    ctx.font = '600 9px Inter, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(node.label, node.x, node.y + node.radius + 14);
}

function getPathPosition(path, progress) {
    const totalSegments = path.length - 1;
    const segProgress = progress * totalSegments;
    const segIndex = Math.min(Math.floor(segProgress), totalSegments - 1);
    const t = segProgress - segIndex;

    const a = path[segIndex];
    const b = path[segIndex + 1];

    return {
        x: a.x + (b.x - a.x) * t,
        y: a.y + (b.y - a.y) * t
    };
}

// ═══════════════════════════════════════════════════════════════════
//  CHART.JS HELPERS
// ═══════════════════════════════════════════════════════════════════

function chartOptions(xLabel, yLabel) {
    return {
        responsive: true,
        maintainAspectRatio: false,
        animation: {
            duration: 1200,
            easing: 'easeOutQuart'
        },
        interaction: {
            mode: 'index',
            intersect: false,
        },
        plugins: {
            legend: {
                labels: {
                    color: '#9196ab',
                    font: { family: "'Inter', sans-serif", size: 11, weight: 500 },
                    usePointStyle: true,
                    pointStyleWidth: 12,
                    padding: 16,
                },
            },
            tooltip: {
                backgroundColor: 'rgba(18, 20, 31, 0.95)',
                titleColor: '#e8e8ef',
                bodyColor: '#9196ab',
                borderColor: 'rgba(139, 233, 253, 0.2)',
                borderWidth: 1,
                cornerRadius: 8,
                padding: 12,
                titleFont: { family: "'Inter', sans-serif", size: 12, weight: 600 },
                bodyFont: { family: "'JetBrains Mono', monospace", size: 11 },
                displayColors: true,
                boxPadding: 4,
            },
        },
        scales: {
            x: {
                title: {
                    display: true,
                    text: xLabel,
                    color: '#9196ab',
                    font: { family: "'Inter', sans-serif", size: 11, weight: 600 },
                },
                ticks: {
                    color: '#585d72',
                    font: { family: "'Inter', sans-serif", size: 10 },
                    maxRotation: 45,
                },
                grid: {
                    color: 'rgba(40, 42, 58, 0.6)',
                    lineWidth: 0.5,
                },
                border: { color: 'rgba(40, 42, 58, 0.8)' }
            },
            y: {
                title: {
                    display: true,
                    text: yLabel,
                    color: '#9196ab',
                    font: { family: "'Inter', sans-serif", size: 11, weight: 600 },
                },
                ticks: {
                    color: '#585d72',
                    font: { family: "'JetBrains Mono', monospace", size: 10 },
                },
                grid: {
                    color: 'rgba(40, 42, 58, 0.6)',
                    lineWidth: 0.5,
                },
                border: { color: 'rgba(40, 42, 58, 0.8)' }
            }
        }
    };
}

function formatNodeCount(n) {
    if (n >= 1000) return (n / 1000).toFixed(n % 1000 === 0 ? 0 : 1) + 'K';
    return n.toString();
}

// ═══════════════════════════════════════════════════════════════════
//  UTILITIES
// ═══════════════════════════════════════════════════════════════════

function animateNumber(el, from, to, duration, formatter) {
    const start = performance.now();
    function tick(now) {
        const elapsed = now - start;
        const progress = Math.min(elapsed / duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3); // easeOutCubic
        const value = from + (to - from) * eased;
        el.textContent = formatter(value);
        if (progress < 1) requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);
}

async function fetchWithRetry(url, retries, interval) {
    for (let i = 0; i < retries; i++) {
        try {
            const res = await fetch(url);
            if (res.ok) return await res.json();
        } catch (e) {
            // ignore
        }
        if (i < retries - 1) await sleep(interval);
    }
    return null;
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

function hexToRgba(hex, alpha) {
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

// ─── Loading Helpers ────────────────────────────────────────────
function showLoading() {
    const overlay = $('#loading-overlay');
    overlay.classList.remove('fade-out', 'hidden');
}

function hideLoading() {
    const overlay = $('#loading-overlay');
    overlay.classList.add('fade-out');
    setTimeout(() => overlay.classList.add('hidden'), 600);
}

function updateLoadingStatus(msg) {
    $('#loading-status').textContent = msg;
}

function setLoadingBar(pct) {
    $('#loading-bar').style.width = pct + '%';
}

function setPhase(num, state) {
    const el = $(`#phase-${num}`);
    if (!el) return;
    el.classList.remove('active', 'done');
    if (state) el.classList.add(state);
}
