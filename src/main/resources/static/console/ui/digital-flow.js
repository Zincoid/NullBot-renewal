(() => {
    'use strict';

    const canvas = document.getElementById('digital-flow');
    if (!canvas || typeof canvas.getContext !== 'function') return;

    const CHARS = '0123456789ABCDEF';
    const FONT_SIZE = 12;
    const CELL_W = 15;
    const CELL_H = 17;
    const MARGIN_WIDTH = 140;
    const HOT_RADIUS = 100;

    const ctx = canvas.getContext('2d');
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    let cells = [];
    let width = 0;
    let height = 0;
    let resizeTimer = 0;
    let lastTime = 0;
    let mouseX = -9999;
    let mouseY = -9999;

    function randomChar() {
        return CHARS[Math.floor(Math.random() * CHARS.length)];
    }

    function buildCells() {
        cells = [];
        const columns = [];
        for (let x = 4; x < MARGIN_WIDTH; x += CELL_W) columns.push(x);
        for (let x = width - MARGIN_WIDTH + 4; x < width - 4; x += CELL_W) columns.push(x);

        for (const x of columns) {
            for (let y = CELL_H; y < height - 4; y += CELL_H) {
                cells.push({
                    x,
                    y,
                    v: randomChar(),
                    next: Math.random() * 4,
                    alpha: 0.30 + Math.random() * 0.18
                });
            }
        }
    }

    function draw() {
        const isDark = document.documentElement.dataset.theme === 'dark';
        const color = isDark ? '75,85,99' : '107,114,128';

        ctx.clearRect(0, 0, width, height);
        ctx.font = `bold ${FONT_SIZE}px "Courier New", monospace`;
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';

        for (const cell of cells) {
            ctx.fillStyle = `rgba(${color},${cell.alpha.toFixed(3)})`;
            ctx.fillText(cell.v, cell.x, cell.y);
        }

        // Fade out toward the content area, keep the outer edge solid.
        const fadeW = Math.min(MARGIN_WIDTH, width * 0.15);
        ctx.save();
        ctx.globalCompositeOperation = 'destination-out';
        const gradient = ctx.createLinearGradient(0, 0, width, 0);
        gradient.addColorStop(0, 'rgba(0,0,0,0)');
        gradient.addColorStop(fadeW / width, 'rgba(0,0,0,1)');
        gradient.addColorStop(1 - fadeW / width, 'rgba(0,0,0,1)');
        gradient.addColorStop(1, 'rgba(0,0,0,0)');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, width, height);
        ctx.restore();
    }

    function tick(now) {
        const dt = Math.min((now - lastTime) / 1000, 0.1);
        lastTime = now;

        for (const cell of cells) {
            const dx = cell.x - mouseX;
            const dy = cell.y - mouseY;
            const distance = Math.sqrt(dx * dx + dy * dy);
            const factor = distance < HOT_RADIUS ? 1 - distance / HOT_RADIUS : 0;

            // Slow drift far from the pointer, rapid flicker near it.
            const speed = 0.3 + (1 - factor) * 4;
            cell.next -= dt;
            if (cell.next <= 0) {
                cell.v = randomChar();
                cell.next = speed * (0.1 + Math.random() * 0.3);
                cell.alpha = 0.26 + factor * 0.5 + Math.random() * 0.1;
            }
        }

        draw();
        requestAnimationFrame(tick);
    }

    function resize() {
        const rect = canvas.getBoundingClientRect();
        if (!rect.width || !rect.height) return;

        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        width = rect.width;
        height = rect.height;
        canvas.width = Math.round(width * dpr);
        canvas.height = Math.round(height * dpr);
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
        buildCells();
        if (reducedMotion) draw();
    }

    function onResize() {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(resize, 300);
    }

    function onMouseMove(event) {
        const rect = canvas.getBoundingClientRect();
        mouseX = event.clientX - rect.left;
        mouseY = event.clientY - rect.top;
    }

    document.addEventListener('console:themechange', () => {
        if (reducedMotion) draw();
    });

    resize();
    if (!reducedMotion) {
        lastTime = performance.now();
        animationId = requestAnimationFrame(tick);
    }
    window.addEventListener('resize', onResize);
    window.addEventListener('mousemove', onMouseMove, {passive: true});
})();
