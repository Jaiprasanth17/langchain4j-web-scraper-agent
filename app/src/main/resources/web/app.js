// === LangChain4j Web Scraper Agent - Frontend ===

let isRunning = false;
let timerInterval = null;
let startTime = null;

function startScrape() {
    const url = document.getElementById('urlInput').value.trim();
    const cssSelector = document.getElementById('selectorInput').value.trim();
    const maxResults = parseInt(document.getElementById('maxResults').value) || 15;

    if (!url) {
        alert('Please enter a URL to scrape');
        return;
    }

    // Validate URL
    try {
        new URL(url);
    } catch (e) {
        alert('Please enter a valid URL (e.g., https://news.ycombinator.com)');
        return;
    }

    if (isRunning) return;
    isRunning = true;

    // Reset UI
    resetUI();
    showSection('statusSection');
    showSection('logSection');

    // Disable button
    const btn = document.getElementById('scrapeBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner" style="width:16px;height:16px;border-width:2px;"></span> Scraping...';

    // Start timer
    startTime = Date.now();
    timerInterval = setInterval(updateTimer, 100);

    // Log start
    addLog('step', 'Starting scrape for: ' + url);
    if (cssSelector) {
        addLog('info', 'CSS Selector: ' + cssSelector);
    }
    addLog('info', 'Max results: ' + maxResults);
    updateProgress(10);

    // Simulate progress steps
    setTimeout(() => addLog('info', 'Initializing AI agent...'), 500);
    setTimeout(() => { addLog('info', 'Launching headless browser...'); updateProgress(20); }, 1500);
    setTimeout(() => { addLog('step', 'Checking robots.txt...'); updateProgress(30); }, 3000);
    setTimeout(() => { addLog('info', 'Navigating to target URL...'); updateProgress(40); }, 5000);
    setTimeout(() => { addLog('info', 'Waiting for page to load...'); updateProgress(50); }, 7000);
    setTimeout(() => { addLog('step', 'Extracting content...'); updateProgress(60); }, 10000);
    setTimeout(() => { addLog('info', 'Taking screenshot...'); updateProgress(70); }, 15000);
    setTimeout(() => { addLog('info', 'Processing results...'); updateProgress(80); }, 20000);

    // Make API call
    fetch('/api/scrape', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            url: url,
            cssSelector: cssSelector || null,
            maxResults: maxResults
        })
    })
    .then(response => response.json())
    .then(data => {
        clearInterval(timerInterval);
        updateProgress(100);

        if (data.status === 'success') {
            handleSuccess(data);
        } else {
            handleError(data);
        }
    })
    .catch(err => {
        clearInterval(timerInterval);
        handleError({ error: 'Network error: ' + err.message });
    })
    .finally(() => {
        isRunning = false;
        const btn = document.getElementById('scrapeBtn');
        btn.disabled = false;
        btn.innerHTML = '<span class="btn-icon">&#9654;</span> Start Scraping';
    });
}

function handleSuccess(data) {
    // Update status
    document.getElementById('statusText').textContent = 'Scrape completed successfully!';
    document.getElementById('spinner').className = 'spinner done';
    document.getElementById('duration').textContent = formatDuration(data.durationMs);

    addLog('success', 'Scrape completed in ' + formatDuration(data.durationMs));

    // Show results
    showSection('resultsSection');
    const resultsOutput = document.getElementById('resultsOutput');
    try {
        // Try to parse and pretty-print the JSON
        const jsonStart = data.result.indexOf('[');
        const jsonEnd = data.result.lastIndexOf(']');
        if (jsonStart !== -1 && jsonEnd !== -1) {
            const jsonStr = data.result.substring(jsonStart, jsonEnd + 1);
            const parsed = JSON.parse(jsonStr);
            resultsOutput.textContent = JSON.stringify(parsed, null, 2);
            addLog('success', 'Extracted ' + parsed.length + ' items');
        } else {
            resultsOutput.textContent = data.result;
        }
    } catch (e) {
        resultsOutput.textContent = data.result;
    }

    // Show screenshot if available
    if (data.screenshotPath) {
        showSection('screenshotSection');
        const img = document.getElementById('screenshotImg');
        img.src = data.screenshotPath;
        img.onerror = function() {
            document.getElementById('screenshotSection').style.display = 'none';
        };
        addLog('info', 'Screenshot saved');
    }
}

function handleError(data) {
    document.getElementById('statusText').textContent = 'Scrape failed';
    document.getElementById('spinner').className = 'spinner error';

    addLog('error', 'Error: ' + (data.error || 'Unknown error'));

    showSection('errorSection');
    document.getElementById('errorOutput').textContent = data.error || 'An unknown error occurred';
}

// === UI Helpers ===

function resetUI() {
    document.getElementById('resultsSection').style.display = 'none';
    document.getElementById('screenshotSection').style.display = 'none';
    document.getElementById('errorSection').style.display = 'none';
    document.getElementById('logOutput').innerHTML = '';
    document.getElementById('statusText').textContent = 'Initializing agent...';
    document.getElementById('spinner').className = 'spinner';
    document.getElementById('duration').textContent = '';
    document.getElementById('progressFill').style.width = '0%';
}

function showSection(id) {
    document.getElementById(id).style.display = 'block';
}

function addLog(type, message) {
    const logOutput = document.getElementById('logOutput');
    const entry = document.createElement('div');
    entry.className = 'log-entry';

    const now = new Date();
    const time = now.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });

    entry.innerHTML = '<span class="log-time">' + time + '</span><span class="log-' + type + '">' + escapeHtml(message) + '</span>';
    logOutput.appendChild(entry);

    // Auto-scroll
    const container = document.getElementById('logContainer');
    container.scrollTop = container.scrollHeight;
}

function updateProgress(percent) {
    document.getElementById('progressFill').style.width = percent + '%';
}

function updateTimer() {
    if (startTime) {
        const elapsed = Date.now() - startTime;
        document.getElementById('duration').textContent = formatDuration(elapsed);
    }
}

function formatDuration(ms) {
    if (ms < 1000) return ms + 'ms';
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    if (minutes > 0) {
        return minutes + 'm ' + remainingSeconds + 's';
    }
    return seconds + '.' + Math.floor((ms % 1000) / 100) + 's';
}

function toggleLog() {
    const container = document.getElementById('logContainer');
    container.style.display = container.style.display === 'none' ? 'block' : 'none';
}

function copyResults() {
    const text = document.getElementById('resultsOutput').textContent;
    navigator.clipboard.writeText(text).then(() => {
        const btn = event.target;
        const original = btn.textContent;
        btn.textContent = 'Copied!';
        setTimeout(() => btn.textContent = original, 1500);
    });
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// === Keyboard shortcut ===
document.addEventListener('keydown', function(e) {
    if (e.ctrlKey && e.key === 'Enter') {
        startScrape();
    }
});
