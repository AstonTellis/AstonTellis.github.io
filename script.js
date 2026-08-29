/* ============================================================
   [YOUR NAME] Portfolio — Shared JavaScript
   ============================================================ */

// ── Custom Cursor ────────────────────────────────────────────
const cursor     = document.querySelector('.cursor');
const cursorRing = document.querySelector('.cursor-ring');

if (cursor && cursorRing) {
  let mx = 0, my = 0, rx = 0, ry = 0;

  document.addEventListener('mousemove', e => {
    mx = e.clientX; my = e.clientY;
    cursor.style.left = mx - 5 + 'px';
    cursor.style.top  = my - 5 + 'px';
  });

  // Smoothly lag the ring behind
  (function animateRing() {
    rx += (mx - rx) * 0.12;
    ry += (my - ry) * 0.12;
    cursorRing.style.left = rx - 18 + 'px';
    cursorRing.style.top  = ry - 18 + 'px';
    requestAnimationFrame(animateRing);
  })();

  // Scale on hover
  document.querySelectorAll('a, button, .card, .btn').forEach(el => {
    el.addEventListener('mouseenter', () => {
      cursor.style.transform = 'scale(2.5)';
      cursorRing.style.transform = 'scale(1.6)';
      cursorRing.style.opacity = '0.8';
    });
    el.addEventListener('mouseleave', () => {
      cursor.style.transform = 'scale(1)';
      cursorRing.style.transform = 'scale(1)';
      cursorRing.style.opacity = '0.5';
    });
  });
}

// ── Nav Scroll Effect ────────────────────────────────────────
const nav = document.querySelector('nav');
if (nav) {
  window.addEventListener('scroll', () => {
    nav.classList.toggle('scrolled', window.scrollY > 40);
  });
}

// ── Scroll Fade-In ───────────────────────────────────────────
const observer = new IntersectionObserver(entries => {
  entries.forEach(e => {
    if (e.isIntersecting) {
      e.target.classList.add('visible');
      // Stagger children if present
      e.target.querySelectorAll('.stagger-child').forEach((child, i) => {
        child.style.transitionDelay = `${i * 0.1}s`;
        child.classList.add('visible');
      });
    }
  });
}, { threshold: 0.12 });

document.querySelectorAll('.fade-up').forEach(el => observer.observe(el));

// ── Typing Animation ─────────────────────────────────────────
function typeWriter(element, phrases, speed = 80, pause = 2200) {
  let phraseIdx = 0, charIdx = 0, deleting = false;

  function tick() {
    const phrase = phrases[phraseIdx];
    if (!deleting) {
      element.textContent = phrase.slice(0, ++charIdx);
      if (charIdx === phrase.length) {
        deleting = true;
        setTimeout(tick, pause);
        return;
      }
    } else {
      element.textContent = phrase.slice(0, --charIdx);
      if (charIdx === 0) {
        deleting = false;
        phraseIdx = (phraseIdx + 1) % phrases.length;
      }
    }
    setTimeout(tick, deleting ? speed / 2 : speed);
  }
  tick();
}

// ── Terminal Simulator ───────────────────────────────────────
function initTerminal(terminalEl) {
  if (!terminalEl) return;

  const lines = [
    { delay: 400,  text: '$ whoami',                          type: 'cmd' },
    { delay: 900,  text: 'Aston Tellis — CS & Business Student', type: 'out' },
    { delay: 1400, text: '$ cat interests.txt',               type: 'cmd' },
    { delay: 1900, text: 'FinTech · Product Management · Privacy · Data Analytics', type: 'out' },
    { delay: 2400, text: '$ ls projects/',                    type: 'cmd' },
    { delay: 2900, text: 'hartwell_bank_sql/   hartwell_bank_excel/   blog/', type: 'out' },
    { delay: 3400, text: '$ cat privacy_stance.txt',          type: 'cmd' },
    { delay: 3900, text: '"No tracking. No cookies. No surveillance. Privacy by design."', type: 'out' },
    { delay: 4400, text: '$ ping this-site.com',              type: 'cmd' },
    { delay: 4900, text: 'PONG — 0 bytes of your data collected.', type: 'out', color: '#00ffaa' },
    { delay: 5400, text: '$ _',                               type: 'cursor' },
  ];

  lines.forEach(({ delay, text, type, color }) => {
    setTimeout(() => {
      const line = document.createElement('div');
      line.classList.add('terminal-line', `terminal-${type}`);
      if (color) line.style.color = color;

      if (type === 'cursor') {
        line.innerHTML = '<span class="t-prompt">$ </span><span class="t-cursor">▋</span>';
      } else if (type === 'cmd') {
        line.innerHTML = `<span class="t-prompt">$ </span><span class="t-cmd">${text.slice(2)}</span>`;
      } else {
        line.textContent = text;
      }

      terminalEl.appendChild(line);
      terminalEl.scrollTop = terminalEl.scrollHeight;

      // Animate in
      line.style.opacity = '0';
      line.style.transform = 'translateX(-8px)';
      requestAnimationFrame(() => {
        line.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        line.style.opacity = '1';
        line.style.transform = 'translateX(0)';
      });
    }, delay);
  });
}

// ── Privacy Ticker ───────────────────────────────────────────
function initPrivacyTicker(el) {
  if (!el) return;

  const trackers = [
    { platform: 'Facebook/Meta',  collected: Math.floor(Math.random() * 800 + 200) },
    { platform: 'Google',         collected: Math.floor(Math.random() * 600 + 300) },
    { platform: 'Amazon',         collected: Math.floor(Math.random() * 400 + 100) },
    { platform: 'TikTok',         collected: Math.floor(Math.random() * 900 + 100) },
    { platform: 'This site',      collected: 0 },
  ];

  let idx = 0;

  function update() {
    const t = trackers[idx];
    const isThis = t.platform === 'This site';
    el.innerHTML = `
      <span class="ticker-platform" style="color:${isThis ? '#00ffaa' : '#ff4d6d'}">${t.platform}</span>
      <span class="ticker-sep"> — </span>
      <span class="ticker-count" style="color:${isThis ? '#00ffaa' : '#8a9ab5'}">
        ${isThis
          ? '0 data points collected about you ✓'
          : `~${t.collected} data points collected about you today`}
      </span>
    `;
    idx = (idx + 1) % trackers.length;
  }

  update();
  setInterval(update, 3000);
}

// ── Live Clock ───────────────────────────────────────────────
function initClock(el) {
  if (!el) return;

  function tick() {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('en-GB', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
    });
    const dateStr = now.toLocaleDateString('en-GB', {
      weekday: 'short', day: 'numeric', month: 'short', year: 'numeric'
    });
    el.innerHTML = `<span class="clock-time">${timeStr}</span><span class="clock-date">${dateStr}</span>`;
  }
  tick();
  setInterval(tick, 1000);
}

// ── Particle Canvas ──────────────────────────────────────────
function initParticles(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return;
  const ctx = canvas.getContext('2d');

  function resize() {
    canvas.width  = canvas.parentElement.offsetWidth;
    canvas.height = canvas.parentElement.offsetHeight;
  }
  resize();
  window.addEventListener('resize', resize);

  const particles = Array.from({ length: 80 }, () => ({
    x: Math.random() * canvas.width,
    y: Math.random() * canvas.height,
    vx: (Math.random() - 0.5) * 0.4,
    vy: (Math.random() - 0.5) * 0.4,
    r:  Math.random() * 1.5 + 0.3,
    a:  Math.random() * 0.5 + 0.1,
  }));

  let mouseX = -9999, mouseY = -9999;
  canvas.addEventListener('mousemove', e => {
    const rect = canvas.getBoundingClientRect();
    mouseX = e.clientX - rect.left;
    mouseY = e.clientY - rect.top;
  });

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    particles.forEach(p => {
      // Push away from mouse
      const dx = p.x - mouseX, dy = p.y - mouseY;
      const dist = Math.sqrt(dx * dx + dy * dy);
      if (dist < 120) {
        p.vx += dx / dist * 0.15;
        p.vy += dy / dist * 0.15;
      }

      p.vx *= 0.98; p.vy *= 0.98;
      p.x += p.vx; p.y += p.vy;

      if (p.x < 0) p.x = canvas.width;
      if (p.x > canvas.width) p.x = 0;
      if (p.y < 0) p.y = canvas.height;
      if (p.y > canvas.height) p.y = 0;

      ctx.beginPath();
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(0, 212, 255, ${p.a})`;
      ctx.fill();
    });

    // Draw connections
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x;
        const dy = particles[i].y - particles[j].y;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 100) {
          ctx.beginPath();
          ctx.moveTo(particles[i].x, particles[i].y);
          ctx.lineTo(particles[j].x, particles[j].y);
          ctx.strokeStyle = `rgba(0, 212, 255, ${(1 - dist / 100) * 0.12})`;
          ctx.lineWidth = 0.5;
          ctx.stroke();
        }
      }
    }

    requestAnimationFrame(draw);
  }
  draw();
}

// ── Init everything on DOM ready ─────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  // Typing animation
  const typingEl = document.getElementById('typing-text');
  if (typingEl) {
    typeWriter(typingEl, [
      'CS & Business Student.',
      'FinTech Enthusiast.',
      'Data Analyst.',
      'Privacy Advocate.',
      'Product Thinker.',
      'Problem Solver.',
    ]);
  }

  // Terminal
  initTerminal(document.getElementById('terminal-body'));

  // Privacy ticker
  initPrivacyTicker(document.getElementById('privacy-ticker'));

  // Clock
  initClock(document.getElementById('live-clock'));

  // Particles
  initParticles('particle-canvas');

  // Active nav link
  const currentPage = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-links a').forEach(link => {
    if (link.getAttribute('href') === currentPage) link.classList.add('active');
  });
});

// ═══════════════════════════════════════════════════════════
// LOADING SCREEN
// ═══════════════════════════════════════════════════════════
(function() {
  const ls = document.getElementById('loading-screen');
  if (!ls) return;
  // Hide after 1.4s (animation completes)
  setTimeout(() => ls.classList.add('hidden'), 1400);
})();

// ═══════════════════════════════════════════════════════════
// PAGE TRANSITIONS
// ═══════════════════════════════════════════════════════════
(function() {
  const curtain = document.createElement('div');
  curtain.className = 'page-transition';
  document.body.appendChild(curtain);

  // Animate out on load
  curtain.classList.add('leaving');
  setTimeout(() => curtain.classList.remove('leaving'), 400);

  // Animate in on internal link click
  document.querySelectorAll('a[href]').forEach(link => {
    const href = link.getAttribute('href');
    if (!href || href.startsWith('#') || href.startsWith('http') ||
        href.startsWith('mailto') || href.startsWith('tel')) return;

    link.addEventListener('click', e => {
      e.preventDefault();
      curtain.classList.add('entering');
      setTimeout(() => { window.location.href = href; }, 380);
    });
  });
})();

// ═══════════════════════════════════════════════════════════
// DARK / LIGHT MODE TOGGLE
// ═══════════════════════════════════════════════════════════
(function() {
  // Inject toggle button
  const btn = document.createElement('button');
  btn.className = 'theme-toggle';
  btn.setAttribute('aria-label', 'Toggle dark/light mode');
  btn.setAttribute('title', 'Toggle dark/light mode');
  document.body.appendChild(btn);

  const saved = localStorage.getItem('theme') || 'dark';
  const apply = mode => {
    document.body.classList.toggle('light-mode', mode === 'light');
    btn.textContent = mode === 'light' ? '🌙' : '☀️';
    localStorage.setItem('theme', mode);
  };

  apply(saved);

  btn.addEventListener('click', () => {
    const isLight = document.body.classList.contains('light-mode');
    apply(isLight ? 'dark' : 'light');
  });
})();

// ═══════════════════════════════════════════════════════════
// KEYBOARD NAVIGATION SUPPORT
// ═══════════════════════════════════════════════════════════
(function() {
  // Detect keyboard vs mouse navigation
  document.addEventListener('keydown', e => {
    if (e.key === 'Tab') {
      document.body.classList.add('keyboard-nav');
    }
  });
  document.addEventListener('mousedown', () => {
    document.body.classList.remove('keyboard-nav');
  });

  // Inject skip-to-content link
  const skip = document.createElement('a');
  skip.href = '#main-content';
  skip.className = 'skip-link';
  skip.textContent = 'Skip to main content';
  document.body.prepend(skip);

  // Keyboard shortcuts
  document.addEventListener('keydown', e => {
    // Press H → home, P → projects, S → skills, B → blog, C → contact
    if (e.altKey) return;
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
    const map = { 'h': 'index.html', 'p': 'projects.html', 's': 'skills.html', 'b': 'blog.html', 'c': 'contact.html' };
    if (map[e.key]) {
      window.location.href = map[e.key];
    }
  });
})();

// ═══════════════════════════════════════════════════════════
// GOATCOUNTER — Privacy-respecting page analytics
// Replace 'your-goatcounter-code' with your actual code
// after signing up at goatcounter.com (free)
// ═══════════════════════════════════════════════════════════
// Uncomment the lines below once you have your GoatCounter code:
// window.goatcounter = { path: location.pathname };
// (function() {
//   const s = document.createElement('script');
//   s.src = 'https://[your-code].goatcounter.com/count/script.js';
//   s.async = true;
//   document.head.appendChild(s);
// })();

// ═══════════════════════════════════════════════════════════
// CV DOWNLOAD COUNTER — Supabase integration
// ═══════════════════════════════════════════════════════════
// This logs every CV download to a free Supabase table.
// Setup: create a Supabase project → create a table called
// 'cv_downloads' with columns: id (int8), downloaded_at (timestamptz)
// → replace the URL and key below with your Supabase project details.

const SUPABASE_URL = '[your-supabase-url]';
const SUPABASE_KEY = '[your-supabase-anon-key]';

async function logCVDownload() {
  if (SUPABASE_URL.includes('[')) return; // not configured yet
  try {
    await fetch(`${SUPABASE_URL}/rest/v1/cv_downloads`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'apikey': SUPABASE_KEY,
        'Authorization': `Bearer ${SUPABASE_KEY}`,
        'Prefer': 'return=minimal'
      },
      body: JSON.stringify({ downloaded_at: new Date().toISOString() })
    });
    console.log('CV download logged.');
  } catch (err) {
    console.warn('CV download log failed:', err);
  }
}

// Attach to CV download buttons
document.querySelectorAll('.cv-download-btn').forEach(btn => {
  btn.addEventListener('click', () => logCVDownload());
});
