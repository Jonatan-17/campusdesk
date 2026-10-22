/**
 * CampusDesk Interactive Animation & Physics Engine
 * Client: TechNova Solutions
 * Crafted with Pure Vanilla JavaScript (Zero External Dependencies)
 * 
 * Features:
 *  - Unified Full-Screen Cosmic Universe Canvas (Zero division, seamless viewport atmosphere):
 *     * Multi-Cluster Cosmic Gaseous Nebula Clouds
 *     * Flowing Cyber Aurora Energy Wave Ribbons with real-time mouse deflection
 *     * Holographic Rotating Tech Rings & Cyber HUD System
 *     * Living IT Support Network Constellation with traveling data packet pulses
 *     * Floating Upward Quantum Embers & Twinkling Starfield
 *     * Trailing Cosmic Stardust Sparks on cursor motion
 *     * Expanding Holographic Shockwaves on click/tap
 *  - 3D Perspective Card Tilt Physics & Dynamic Specular Glare
 *  - Magnetic Micro-Interactions on CTA buttons and branding logos
 *  - Viewport Reading / Scroll Depth Glowing Progress Bar
 *  - Staggered 3D Scroll Reveal via IntersectionObserver
 *  - Interactive Floating Back-to-Top Glassmorphic Action
 *  - Ambient Interactive Cursor Glow Aura
 *  - Tactile Radial Button Shockwave Ripples
 *  - Smooth Numeric KPI Counters with easeOutExpo curves
 *  - Input Focus Energy Halo Micro-Interactions
 */
'use strict';

const Animations = {
  // Master Canvas & State
  canvas: null,
  ctx: null,
  dpr: 1,
  animFrameId: null,
  isAuthPage: false,
  isLandingPage: false,
  clouds: [],
  auroraWaves: [],
  hudCenter: { xFactor: 0.48, yFactor: 0.22, angle1: 0, angle2: 0, angle3: 0 },
  particles: [],
  dataPackets: [],
  embers: [],
  stardust: [],
  shockwaves: [],
  mouse: {
    x: -1000,
    y: -1000,
    targetX: -1000,
    targetY: -1000,
    prevX: -1000,
    prevY: -1000,
    speed: 0,
    isOnScreen: false
  },
  cursorAura: null,
  progressBar: null,
  backToTopBtn: null,
  isPaused: false,

  /**
   * Initializes all interactive JavaScript animation modules
   */
  init() {
    this.detectPageContext();
    this.setupLandingIntro();
    this.setupNebulaCanvas();
    this.setupCardPhysics3D();
    this.setupMagneticElements();
    this.setupScrollProgress();
    this.setupScrollObserver();
    this.setupBackToTop();
    this.setupCursorAura();
    this.setupButtonRipples();
    this.setupInputInteractions();
    this.animateNumbers();
    this.setupVisibilityHandler();
  },

  /**
   * Identifies the current page layout to tailor effects
   */
  detectPageContext() {
    this.isAuthPage = document.body.classList.contains('auth-page') || 
                      !!document.querySelector('.auth-card') || 
                      !!document.querySelector('.auth-card-wide');
    this.isLandingPage = !!document.querySelector('.landing-hero');
  },

  /**
   * Initializes and manages the unified full-screen interactive universe canvas
   */
  setupNebulaCanvas() {
    // Remove any legacy scoped hero canvas if present to ensure 100% unified background
    const existingHeroCanvas = document.querySelector('.auth-hero-canvas');
    if (existingHeroCanvas) existingHeroCanvas.remove();

    let canvas = document.getElementById('ambient-glow-canvas');
    if (!canvas) {
      canvas = document.createElement('canvas');
      canvas.id = 'ambient-glow-canvas';
      canvas.className = 'ambient-glow-canvas';
      document.body.prepend(canvas);
    }

    this.canvas = canvas;
    this.ctx = canvas.getContext('2d', { alpha: true });
    this.dpr = Math.min(window.devicePixelRatio || 1, 2);

    const resize = () => {
      const w = window.innerWidth;
      const h = window.innerHeight;
      this.canvas.width = Math.floor(w * this.dpr);
      this.canvas.height = Math.floor(h * this.dpr);
      this.canvas.style.width = `${w}px`;
      this.canvas.style.height = `${h}px`;
      this.ctx.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
    };
    resize();
    window.addEventListener('resize', resize, { passive: true });

    // Track mouse coordinates and calculate velocity
    const onMouseMove = (e) => {
      this.mouse.targetX = e.clientX;
      this.mouse.targetY = e.clientY;
      this.mouse.isOnScreen = true;

      const dx = e.clientX - this.mouse.prevX;
      const dy = e.clientY - this.mouse.prevY;
      this.mouse.speed = Math.sqrt(dx * dx + dy * dy);

      if (this.mouse.speed > 4 && this.stardust.length < 50) {
        this.spawnStardust(e.clientX, e.clientY);
      }

      this.mouse.prevX = e.clientX;
      this.mouse.prevY = e.clientY;
    };

    window.addEventListener('mousemove', onMouseMove, { passive: true });
    document.addEventListener('mouseleave', () => {
      this.mouse.isOnScreen = false;
      this.mouse.targetX = -1000;
      this.mouse.targetY = -1000;
    }, { passive: true });

    // Interactive cosmic shockwave ripple on click / tap
    window.addEventListener('pointerdown', (e) => {
      this.spawnShockwave(e.clientX, e.clientY);
    }, { passive: true });

    // Initialize unified visual subsystems
    this.initNebulaClouds();
    this.initAuroraWaves();
    this.initParticles();
    this.initEmbers();

    // Start unified 60fps render loop
    this.startNebulaLoop();
  },

  /**
   * Generates organic drifting cosmic nebula gas clouds across the full screen
   */
  initNebulaClouds() {
    const isMobile = window.innerWidth < 768;
    const cloudCount = isMobile ? 3 : (this.isAuthPage ? 6 : 4);

    const nebulaPalettes = [
      { r: 139, g: 92,  b: 246, alpha: 0.22 }, // Electric violet
      { r: 192, g: 38,  b: 211, alpha: 0.17 }, // Cosmic magenta
      { r: 79,  g: 70,  b: 229, alpha: 0.19 }, // Deep space indigo
      { r: 6,   g: 182, b: 212, alpha: 0.14 }, // Celestial cyan ray
      { r: 168, g: 85,  b: 247, alpha: 0.20 }, // Vivid purple
      { r: 232, g: 121, b: 249, alpha: 0.15 }  // Stellar lilac
    ];

    this.clouds = [];
    for (let i = 0; i < cloudCount; i++) {
      const palette = nebulaPalettes[i % nebulaPalettes.length];
      const baseRadius = Math.random() * 180 + (isMobile ? 140 : 260);
      this.clouds.push({
        x: Math.random() * window.innerWidth,
        y: Math.random() * window.innerHeight,
        baseRadius,
        radius: baseRadius,
        vx: (Math.random() - 0.5) * 0.35,
        vy: (Math.random() - 0.5) * 0.35,
        palette,
        pulseSpeed: Math.random() * 0.015 + 0.008,
        pulsePhase: Math.random() * Math.PI * 2
      });
    }
  },

  /**
   * Generates full-width Cyber Aurora Energy Wave Ribbons
   */
  initAuroraWaves() {
    this.auroraWaves = [
      {
        baseYFactor: 0.42,
        amplitude: 45,
        freq: 0.0028,
        speed: 0.015,
        colorGrad: ['rgba(168, 85, 247, 0.24)', 'rgba(147, 51, 234, 0.0)'],
        stroke: 'rgba(192, 132, 252, 0.32)',
        phase: 0
      },
      {
        baseYFactor: 0.62,
        amplitude: 52,
        freq: 0.0022,
        speed: 0.011,
        colorGrad: ['rgba(6, 182, 212, 0.18)', 'rgba(79, 70, 229, 0.0)'],
        stroke: 'rgba(56, 189, 248, 0.28)',
        phase: Math.PI * 0.7
      },
      {
        baseYFactor: 0.26,
        amplitude: 36,
        freq: 0.0034,
        speed: 0.018,
        colorGrad: ['rgba(236, 72, 153, 0.15)', 'rgba(126, 34, 206, 0.0)'],
        stroke: 'rgba(244, 114, 182, 0.25)',
        phase: Math.PI * 1.3
      },
      {
        baseYFactor: 0.82,
        amplitude: 48,
        freq: 0.0025,
        speed: 0.013,
        colorGrad: ['rgba(147, 51, 234, 0.20)', 'rgba(7, 4, 13, 0.0)'],
        stroke: 'rgba(168, 85, 247, 0.25)',
        phase: Math.PI * 0.4
      }
    ];
  },

  /**
   * Generates twinkling stellar particles and constellation nodes across the full width
   */
  initParticles() {
    const isMobile = window.innerWidth < 768;
    const count = isMobile ? 38 : (this.isAuthPage ? 85 : 65);

    const starColors = [
      '#ffffff',
      'rgba(216, 180, 254, ',
      'rgba(192, 132, 252, ',
      'rgba(56, 189, 248, ',
      'rgba(244, 114, 182, '
    ];

    this.particles = [];
    for (let i = 0; i < count; i++) {
      const baseRadius = Math.random() * 2.0 + 0.8;
      this.particles.push({
        x: Math.random() * window.innerWidth,
        y: Math.random() * window.innerHeight,
        vx: (Math.random() - 0.5) * 0.5,
        vy: (Math.random() - 0.5) * 0.5,
        baseRadius,
        radius: baseRadius,
        baseAlpha: Math.random() * 0.65 + 0.25,
        alpha: 0.5,
        colorPrefix: starColors[Math.floor(Math.random() * starColors.length)],
        twinkleSpeed: Math.random() * 0.03 + 0.015,
        twinkleOffset: Math.random() * Math.PI * 2
      });
    }

    this.dataPackets = [];
  },

  /**
   * Generates floating quantum sparks and upward drifting embers
   */
  initEmbers() {
    const isMobile = window.innerWidth < 768;
    const count = isMobile ? 18 : 34;

    this.embers = [];
    for (let i = 0; i < count; i++) {
      this.embers.push({
        x: Math.random() * window.innerWidth,
        y: Math.random() * window.innerHeight,
        speed: Math.random() * 0.6 + 0.2,
        swayPhase: Math.random() * Math.PI * 2,
        radius: Math.random() * 1.8 + 0.6,
        alpha: Math.random() * 0.6 + 0.2,
        color: Math.random() > 0.5 ? 'rgba(216, 180, 254, ' : 'rgba(56, 189, 248, '
      });
    }
  },

  /**
   * Spawns trailing cosmic stardust sparks on mouse movement
   */
  spawnStardust(x, y) {
    const angle = Math.random() * Math.PI * 2;
    const velocity = Math.random() * 2 + 0.8;
    this.stardust.push({
      x,
      y,
      vx: Math.cos(angle) * velocity,
      vy: Math.sin(angle) * velocity,
      radius: Math.random() * 2.2 + 0.8,
      alpha: 0.9,
      decay: Math.random() * 0.025 + 0.02,
      color: Math.random() > 0.4 ? 'rgba(216, 180, 254, ' : 'rgba(56, 189, 248, '
    });
  },

  /**
   * Spawns an expanding cosmic shockwave ring on user click
   */
  spawnShockwave(x, y) {
    this.shockwaves.push({
      x,
      y,
      radius: 5,
      maxRadius: window.innerWidth < 768 ? 260 : 440,
      alpha: 0.85,
      speed: 7.5
    });
  },

  /**
   * Unified 60fps render loop - draws across the entire seamless viewport
   */
  startNebulaLoop() {
    let tick = 0;

    const render = () => {
      if (this.isPaused) {
        this.animFrameId = requestAnimationFrame(render);
        return;
      }

      tick += 0.02;
      const w = window.innerWidth;
      const h = window.innerHeight;

      // Smooth mouse interpolation (Lerp)
      if (this.mouse.isOnScreen) {
        this.mouse.x += (this.mouse.targetX - this.mouse.x) * 0.08;
        this.mouse.y += (this.mouse.targetY - this.mouse.y) * 0.08;
      }

      this.ctx.clearRect(0, 0, w, h);

      // 1. Render Cosmic Gaseous Nebula Clouds across full screen
      for (let i = 0; i < this.clouds.length; i++) {
        const cloud = this.clouds[i];
        cloud.x += cloud.vx;
        cloud.y += cloud.vy;

        if (cloud.x < -cloud.radius) cloud.x = w + cloud.radius;
        if (cloud.x > w + cloud.radius) cloud.x = -cloud.radius;
        if (cloud.y < -cloud.radius) cloud.y = h + cloud.radius;
        if (cloud.y > h + cloud.radius) cloud.y = -cloud.radius;

        if (this.mouse.isOnScreen) {
          const dx = this.mouse.x - cloud.x;
          const dy = this.mouse.y - cloud.y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist < 450) {
            cloud.x += dx * 0.0018;
            cloud.y += dy * 0.0018;
          }
        }

        const currentRadius = cloud.baseRadius + Math.sin(tick * cloud.pulseSpeed + cloud.pulsePhase) * (cloud.baseRadius * 0.18);
        const { r, g, b, alpha } = cloud.palette;

        const grad = this.ctx.createRadialGradient(
          cloud.x, cloud.y, 0,
          cloud.x, cloud.y, Math.max(10, currentRadius)
        );
        grad.addColorStop(0, `rgba(${r}, ${g}, ${b}, ${alpha})`);
        grad.addColorStop(0.5, `rgba(${r}, ${g}, ${b}, ${alpha * 0.45})`);
        grad.addColorStop(1, 'rgba(7, 4, 13, 0)');

        this.ctx.fillStyle = grad;
        this.ctx.beginPath();
        this.ctx.arc(cloud.x, cloud.y, Math.max(10, currentRadius), 0, Math.PI * 2);
        this.ctx.fill();
      }

      // 2. Render Full-Width Cyber Aurora Wave Ribbons (Flows across the ENTIRE screen)
      if (this.isAuthPage || this.isLandingPage) {
        for (let r = 0; r < this.auroraWaves.length; r++) {
          const wave = this.auroraWaves[r];
          const baseY = h * wave.baseYFactor;

          this.ctx.beginPath();
          this.ctx.moveTo(0, h);

          for (let x = 0; x <= w; x += 10) {
            let y = baseY + 
              Math.sin(x * wave.freq + tick * wave.speed + wave.phase) * wave.amplitude +
              Math.cos(x * (wave.freq * 1.5) + tick * (wave.speed * 1.2)) * (wave.amplitude * 0.35);

            // Real-time mouse deflection across entire width
            if (this.mouse.isOnScreen) {
              const dx = x - this.mouse.x;
              if (Math.abs(dx) < 240) {
                const gaussian = Math.exp(-(dx * dx) / (2 * 90 * 90));
                y += (this.mouse.y - y) * gaussian * 0.35;
              }
            }

            this.ctx.lineTo(x, y);
          }

          this.ctx.lineTo(w, h);
          this.ctx.closePath();

          const grad = this.ctx.createLinearGradient(0, baseY - wave.amplitude, 0, h);
          grad.addColorStop(0, wave.colorGrad[0]);
          grad.addColorStop(0.7, wave.colorGrad[1]);
          grad.addColorStop(1, 'rgba(7, 4, 13, 0)');

          this.ctx.fillStyle = grad;
          this.ctx.fill();

          // Wave Crest Glow
          this.ctx.beginPath();
          for (let x = 0; x <= w; x += 10) {
            let y = baseY + 
              Math.sin(x * wave.freq + tick * wave.speed + wave.phase) * wave.amplitude +
              Math.cos(x * (wave.freq * 1.5) + tick * (wave.speed * 1.2)) * (wave.amplitude * 0.35);

            if (this.mouse.isOnScreen) {
              const dx = x - this.mouse.x;
              if (Math.abs(dx) < 240) {
                const gaussian = Math.exp(-(dx * dx) / (2 * 90 * 90));
                y += (this.mouse.y - y) * gaussian * 0.35;
              }
            }

            if (x === 0) this.ctx.moveTo(x, y);
            else this.ctx.lineTo(x, y);
          }
          this.ctx.strokeStyle = wave.stroke;
          this.ctx.lineWidth = 1.3;
          this.ctx.stroke();
        }

        // 3. Render Holographic Rotating Tech Rings (Seamlessly integrated in upper-center space)
        const hudX = w * (this.isAuthPage ? 0.46 : 0.50);
        const hudY = h * 0.24;
        const hudShiftX = this.mouse.isOnScreen ? (this.mouse.x - hudX) * 0.025 : 0;
        const hudShiftY = this.mouse.isOnScreen ? (this.mouse.y - hudY) * 0.025 : 0;
        const finalHudX = hudX + hudShiftX;
        const finalHudY = hudY + hudShiftY;

        this.hudCenter.angle1 += 0.005;
        this.hudCenter.angle2 -= 0.008;
        this.hudCenter.angle3 += 0.012;

        this.ctx.save();
        this.ctx.translate(finalHudX, finalHudY);

        // Outer Ring
        this.ctx.save();
        this.ctx.rotate(this.hudCenter.angle1);
        this.ctx.beginPath();
        this.ctx.arc(0, 0, 130, 0, Math.PI * 2);
        this.ctx.strokeStyle = 'rgba(168, 85, 247, 0.20)';
        this.ctx.lineWidth = 1.2;
        this.ctx.setLineDash([14, 18, 4, 18]);
        this.ctx.stroke();
        for (let a = 0; a < 4; a++) {
          const angle = (a * Math.PI) / 2;
          this.ctx.beginPath();
          this.ctx.moveTo(Math.cos(angle) * 122, Math.sin(angle) * 122);
          this.ctx.lineTo(Math.cos(angle) * 138, Math.sin(angle) * 138);
          this.ctx.strokeStyle = 'rgba(192, 132, 252, 0.45)';
          this.ctx.lineWidth = 1.8;
          this.ctx.stroke();
        }
        this.ctx.restore();

        // Middle Ring
        this.ctx.save();
        this.ctx.rotate(this.hudCenter.angle2);
        this.ctx.beginPath();
        this.ctx.arc(0, 0, 92, 0, Math.PI * 2);
        this.ctx.strokeStyle = 'rgba(6, 182, 212, 0.22)';
        this.ctx.lineWidth = 1.4;
        this.ctx.setLineDash([28, 12, 8, 12]);
        this.ctx.stroke();
        this.ctx.restore();

        // Inner Dotted Ring
        this.ctx.save();
        this.ctx.rotate(this.hudCenter.angle3);
        this.ctx.beginPath();
        this.ctx.arc(0, 0, 56, 0, Math.PI * 2);
        this.ctx.strokeStyle = 'rgba(216, 180, 254, 0.25)';
        this.ctx.lineWidth = 1.2;
        this.ctx.setLineDash([3, 7]);
        this.ctx.stroke();
        this.ctx.restore();

        // Core Pulse
        const corePulse = Math.sin(tick * 2) * 4;
        const coreGrad = this.ctx.createRadialGradient(0, 0, 0, 0, 0, 25 + corePulse);
        coreGrad.addColorStop(0, 'rgba(192, 132, 252, 0.35)');
        coreGrad.addColorStop(0.6, 'rgba(6, 182, 212, 0.15)');
        coreGrad.addColorStop(1, 'rgba(7, 4, 13, 0)');
        this.ctx.fillStyle = coreGrad;
        this.ctx.beginPath();
        this.ctx.arc(0, 0, 25 + corePulse, 0, Math.PI * 2);
        this.ctx.fill();

        this.ctx.restore();
      }

      // 4. Render Cosmic Shockwaves
      for (let i = this.shockwaves.length - 1; i >= 0; i--) {
        const wave = this.shockwaves[i];
        wave.radius += wave.speed;
        wave.alpha = Math.max(0, 0.85 * (1 - wave.radius / wave.maxRadius));

        this.ctx.save();
        this.ctx.beginPath();
        this.ctx.arc(wave.x, wave.y, wave.radius, 0, Math.PI * 2);
        this.ctx.strokeStyle = `rgba(192, 132, 252, ${wave.alpha})`;
        this.ctx.lineWidth = 2.4;
        this.ctx.shadowBlur = 14;
        this.ctx.shadowColor = 'rgba(192, 132, 252, 0.8)';
        this.ctx.stroke();
        this.ctx.restore();

        // Displace particles gently
        for (let j = 0; j < this.particles.length; j++) {
          const p = this.particles[j];
          const dx = p.x - wave.x;
          const dy = p.y - wave.y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (Math.abs(dist - wave.radius) < 24) {
            const push = 2.4;
            p.x += (dx / (dist || 1)) * push;
            p.y += (dy / (dist || 1)) * push;
          }
        }

        if (wave.radius >= wave.maxRadius || wave.alpha <= 0.01) {
          this.shockwaves.splice(i, 1);
        }
      }

      // 5. Render Constellation Filaments & Traveling Data Packets across FULL screen
      const connectedPairs = [];
      const maxDist = 120;

      for (let i = 0; i < this.particles.length; i++) {
        const p1 = this.particles[i];

        for (let j = i + 1; j < this.particles.length; j++) {
          const p2 = this.particles[j];
          const dx = p1.x - p2.x;
          const dy = p1.y - p2.y;
          const dist = Math.sqrt(dx * dx + dy * dy);

          if (dist < maxDist) {
            connectedPairs.push({ from: p1, to: p2, dist });
            const filamentAlpha = (1 - dist / maxDist) * 0.24;
            this.ctx.strokeStyle = `rgba(168, 85, 247, ${filamentAlpha})`;
            this.ctx.lineWidth = 0.85;
            this.ctx.beginPath();
            this.ctx.moveTo(p1.x, p1.y);
            this.ctx.lineTo(p2.x, p2.y);
            this.ctx.stroke();
          }
        }

        // Connect cursor to nearby stars across full screen
        if (this.mouse.isOnScreen) {
          const mdx = p1.x - this.mouse.x;
          const mdy = p1.y - this.mouse.y;
          const mdist = Math.sqrt(mdx * mdx + mdy * mdy);

          if (mdist < 155) {
            const mouseWebAlpha = (1 - mdist / 155) * 0.55;
            this.ctx.strokeStyle = `rgba(216, 180, 254, ${mouseWebAlpha})`;
            this.ctx.lineWidth = 1.2;
            this.ctx.beginPath();
            this.ctx.moveTo(this.mouse.x, this.mouse.y);
            this.ctx.lineTo(p1.x, p1.y);
            this.ctx.stroke();

            const force = (1 - mdist / 155) * 1.6;
            p1.x += (mdx / (mdist || 1)) * force;
            p1.y += (mdy / (mdist || 1)) * force;
          }
        }
      }

      // Spawn and Render Traveling Data Packets across full screen
      if (connectedPairs.length > 0 && this.dataPackets.length < 14 && Math.random() < 0.28) {
        const pair = connectedPairs[Math.floor(Math.random() * connectedPairs.length)];
        this.dataPackets.push({
          from: pair.from,
          to: pair.to,
          progress: 0,
          speed: (Math.random() * 0.02 + 0.015) * (120 / pair.dist),
          color: Math.random() > 0.4 ? '#38bdf8' : '#e879f9'
        });
      }

      for (let p = this.dataPackets.length - 1; p >= 0; p--) {
        const packet = this.dataPackets[p];
        packet.progress += packet.speed;

        if (packet.progress >= 1) {
          this.dataPackets.splice(p, 1);
          continue;
        }

        const px = packet.from.x + (packet.to.x - packet.from.x) * packet.progress;
        const py = packet.from.y + (packet.to.y - packet.from.y) * packet.progress;

        this.ctx.beginPath();
        this.ctx.arc(px, py, 2.2, 0, Math.PI * 2);
        this.ctx.fillStyle = packet.color;
        this.ctx.shadowBlur = 8;
        this.ctx.shadowColor = packet.color;
        this.ctx.fill();
        this.ctx.shadowBlur = 0;
      }

      // 6. Render Starfield Particles
      for (let i = 0; i < this.particles.length; i++) {
        const p = this.particles[i];
        p.x += p.vx;
        p.y += p.vy;

        if (p.x < -10) p.x = w + 10;
        if (p.x > w + 10) p.x = -10;
        if (p.y < -10) p.y = h + 10;
        if (p.y > h + 10) p.y = -10;

        p.alpha = p.baseAlpha + Math.sin(tick * p.twinkleSpeed + p.twinkleOffset) * 0.25;
        p.alpha = Math.max(0.1, Math.min(1, p.alpha));

        this.ctx.fillStyle = p.colorPrefix === '#ffffff' ? '#ffffff' : `${p.colorPrefix}${p.alpha})`;
        this.ctx.beginPath();
        this.ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        this.ctx.fill();
      }

      // 7. Render Floating Quantum Sparks & Embers (Rising Upward)
      for (let e = 0; e < this.embers.length; e++) {
        const emb = this.embers[e];
        emb.y -= emb.speed;
        emb.x += Math.sin(tick + emb.swayPhase) * 0.45;

        if (emb.y < -10) {
          emb.y = h + 10;
          emb.x = Math.random() * w;
        }

        this.ctx.beginPath();
        this.ctx.arc(emb.x, emb.y, emb.radius, 0, Math.PI * 2);
        this.ctx.fillStyle = `${emb.color}${emb.alpha})`;
        this.ctx.fill();
      }

      // 8. Render Trailing Stardust Sparks
      for (let i = this.stardust.length - 1; i >= 0; i--) {
        const s = this.stardust[i];
        s.x += s.vx;
        s.y += s.vy;
        s.vx *= 0.96;
        s.vy *= 0.96;
        s.alpha -= s.decay;

        if (s.alpha <= 0.02) {
          this.stardust.splice(i, 1);
          continue;
        }

        this.ctx.fillStyle = `${s.color}${s.alpha})`;
        this.ctx.beginPath();
        this.ctx.arc(s.x, s.y, Math.max(0.4, s.radius * s.alpha), 0, Math.PI * 2);
        this.ctx.fill();
      }

      this.animFrameId = requestAnimationFrame(render);
    };

    render();
  },

  /**
   * Interactive 3D Perspective Card Tilt Physics & Dynamic Specular Glare
   */
  setupCardPhysics3D() {
    const cardSelectors = '.card, .kpi-card, .benefit-card, .flow-step-box, .tech-card, .auth-card, .auth-card-wide, .quick-action-card, .category-stat-card, .stat-card, .filter-card';
    // Ticket detail cards hold text and forms: tilting them shifts the content the user is reading
    const cards = [...document.querySelectorAll(cardSelectors)].filter(card => !card.closest('.detail-grid-layout'));

    cards.forEach((card) => {
      card.classList.add('card-tilt-active');

      let glare = card.querySelector('.card-specular-glare');
      if (!glare) {
        glare = document.createElement('div');
        glare.className = 'card-specular-glare';
        card.appendChild(glare);
      }

      let isHovered = false;

      card.addEventListener('mouseenter', () => {
        isHovered = true;
      });

      card.addEventListener('mousemove', (e) => {
        if (!isHovered) return;
        const rect = card.getBoundingClientRect();
        const cardX = e.clientX - rect.left;
        const cardY = e.clientY - rect.top;

        const xRatio = (cardX - rect.width / 2) / (rect.width / 2);
        const yRatio = (cardY - rect.height / 2) / (rect.height / 2);

        const maxTilt = 7;
        const rotX = (-yRatio * maxTilt).toFixed(2);
        const rotY = (xRatio * maxTilt).toFixed(2);

        card.style.transform = `perspective(1000px) rotateX(${rotX}deg) rotateY(${rotY}deg) scale3d(1.02, 1.02, 1.02) translateZ(6px)`;

        card.style.setProperty('--glow-x', `${cardX}px`);
        card.style.setProperty('--glow-y', `${cardY}px`);

        const glareX = ((xRatio + 1) * 50).toFixed(1);
        const glareY = ((yRatio + 1) * 50).toFixed(1);
        glare.style.background = `radial-gradient(circle at ${glareX}% ${glareY}%, rgba(255, 255, 255, 0.16) 0%, rgba(168, 85, 247, 0.08) 40%, transparent 70%)`;
        glare.style.opacity = '1';
      });

      card.addEventListener('mouseleave', () => {
        isHovered = false;
        card.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1) translateZ(0)';
        glare.style.opacity = '0';
      });
    });
  },

  /**
   * Magnetic Micro-Interactions on primary buttons and brand emblems
   */
  setupMagneticElements() {
    const magneticTargets = document.querySelectorAll('.btn-primary, .auth-brand-logo, .brand-icon-box');

    magneticTargets.forEach((target) => {
      target.classList.add('magnetic-element');

      target.addEventListener('mousemove', (e) => {
        const rect = target.getBoundingClientRect();
        const centerX = rect.left + rect.width / 2;
        const centerY = rect.top + rect.height / 2;
        const deltaX = (e.clientX - centerX) * 0.22;
        const deltaY = (e.clientY - centerY) * 0.22;

        target.style.transform = `translate(${deltaX.toFixed(1)}px, ${deltaY.toFixed(1)}px) scale(1.03)`;
      });

      target.addEventListener('mouseleave', () => {
        target.style.transform = 'translate(0px, 0px) scale(1)';
      });
    });
  },

  /**
   * Top dynamic viewport reading & scroll depth indicator
   */
  setupScrollProgress() {
    let progressBar = document.getElementById('campusdesk-scroll-progress');
    if (!progressBar) {
      progressBar = document.createElement('div');
      progressBar.id = 'campusdesk-scroll-progress';
      document.body.prepend(progressBar);
    }
    this.progressBar = progressBar;

    const updateScrollProgress = () => {
      const scrollY = window.scrollY || document.documentElement.scrollTop;
      const docHeight = document.documentElement.scrollHeight - window.innerHeight;
      if (docHeight > 0) {
        const percentage = Math.min(100, Math.max(0, (scrollY / docHeight) * 100));
        this.progressBar.style.width = `${percentage}%`;
      }
    };

    window.addEventListener('scroll', updateScrollProgress, { passive: true });
    updateScrollProgress();
  },

  /**
   * Staggered 3D entrance and viewport reveals via IntersectionObserver
   */
  setupScrollObserver() {
    const revealTargets = document.querySelectorAll(
      '.benefit-card, .flow-step-box, .kpi-card, .card, .table-container, .hero-cta-group, .auth-card, .auth-hero-content'
    );

    if (!('IntersectionObserver' in window)) {
      revealTargets.forEach(el => el.classList.add('is-revealed'));
      return;
    }

    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-revealed');
          observer.unobserve(entry.target);
        }
      });
    }, {
      threshold: 0.12,
      rootMargin: '0px 0px -40px 0px'
    });

    revealTargets.forEach((el, idx) => {
      el.classList.add('scroll-reveal-item');
      el.style.transitionDelay = `${(idx % 6) * 75}ms`;
      observer.observe(el);
    });
  },

  /**
   * Floating glassmorphic Back-to-Top action with smooth cubic easing
   */
  setupBackToTop() {
    let btn = document.getElementById('back-to-top-btn');
    if (!btn) {
      btn = document.createElement('button');
      btn.id = 'back-to-top-btn';
      btn.setAttribute('aria-label', 'Back to top');
      btn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 15l-6-6-6 6"/></svg>`;
      document.body.appendChild(btn);
    }
    this.backToTopBtn = btn;

    const toggleBtn = () => {
      if (window.scrollY > 320) {
        btn.classList.add('visible');
      } else {
        btn.classList.remove('visible');
      }
    };

    window.addEventListener('scroll', toggleBtn, { passive: true });
    btn.addEventListener('click', () => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
    toggleBtn();
  },

  /**
   * Cinematic Enterprise Introduction Sequence for index.html
   * Crafted with 100% Pure Vanilla JavaScript, Canvas hyperspace warp particles,
   * telemetry progress decoding, and seamless iris lens reveal.
   */
  setupLandingIntro() {
    if (!this.isLandingPage) return;

    // Build the high-tech intro overlay
    const introOverlay = document.createElement('div');
    introOverlay.id = 'campusdesk-intro-overlay';
    introOverlay.innerHTML = `
      <canvas id="intro-warp-canvas"></canvas>
      <div class="intro-content-core">
        <div class="intro-emblem-wrapper">
          <div class="intro-ring intro-ring-outer"></div>
          <div class="intro-ring intro-ring-inner"></div>
          <div class="intro-emblem-icon">
            <img src="assets/campuslands-logo.png" alt="Campuslands" class="brand-logo-img">
          </div>
        </div>
        <div class="intro-brand-title">CAMPUSDESK</div>
        <div class="intro-brand-sub">TECHNOVA SOLUTIONS · ENTERPRISE SERVICE MANAGEMENT</div>
        <div class="intro-progress-box">
          <div class="intro-progress-track">
            <div class="intro-progress-fill" id="intro-progress-fill"></div>
          </div>
          <div class="intro-status-text" id="intro-status-text">INITIALIZING QUANTUM DISPATCH ENGINE...</div>
        </div>
      </div>
      <button type="button" class="intro-skip-btn" id="intro-skip-btn">Skip [Esc]</button>
    `;

    document.body.prepend(introOverlay);
    document.body.style.overflow = 'hidden';

    // 1. Setup Warp Canvas
    const canvas = document.getElementById('intro-warp-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d', { alpha: true });
    const dpr = Math.min(window.devicePixelRatio || 1, 2);

    let w = window.innerWidth;
    let h = window.innerHeight;

    const resizeWarp = () => {
      w = window.innerWidth;
      h = window.innerHeight;
      canvas.width = Math.floor(w * dpr);
      canvas.height = Math.floor(h * dpr);
      canvas.style.width = `${w}px`;
      canvas.style.height = `${h}px`;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    resizeWarp();
    window.addEventListener('resize', resizeWarp, { passive: true });

    // 130 Hyperspace Stars streaming outward
    const stars = [];
    const starCount = 130;
    for (let i = 0; i < starCount; i++) {
      stars.push({
        x: (Math.random() - 0.5) * w * 2,
        y: (Math.random() - 0.5) * h * 2,
        z: Math.random() * w,
        color: Math.random() > 0.4 ? 'rgba(192, 132, 252, ' : 'rgba(56, 189, 248, '
      });
    }

    let progress = 0;
    let isDismissed = false;
    let animId = null;

    const telemetryStages = [
      { p: 18, text: 'AUTHENTICATING TELEMETRY NODES...' },
      { p: 44, text: 'SYNCHRONIZING INCIDENT MATRIX...' },
      { p: 76, text: 'VALIDATING ROLE GOVERNANCE POLICIES...' },
      { p: 94, text: 'WORKSPACE SECURED · ACCESS GRANTED' },
      { p: 100, text: 'WELCOME TO CAMPUSDESK ECOSYSTEM' }
    ];

    const progressFill = document.getElementById('intro-progress-fill');
    const statusText = document.getElementById('intro-status-text');

    const dismissIntro = () => {
      if (isDismissed) return;
      isDismissed = true;

      if (animId) cancelAnimationFrame(animId);
      introOverlay.classList.add('dismissing');
      document.body.style.overflow = '';
      document.body.style.overflowY = 'auto';
      document.documentElement.style.overflowY = 'auto';

      setTimeout(() => {
        if (introOverlay.parentNode) {
          introOverlay.remove();
        }
      }, 700);
    };

    // Skip controls
    document.getElementById('intro-skip-btn')?.addEventListener('click', dismissIntro);
    const onKey = (e) => {
      if (e.key === 'Escape') {
        dismissIntro();
        window.removeEventListener('keydown', onKey);
      }
    };
    window.addEventListener('keydown', onKey);

    const startTime = performance.now();
    const duration = 2100; // 2.1s timing

    const loop = (currentTime) => {
      if (isDismissed) return;

      const elapsed = currentTime - startTime;
      progress = Math.min(100, (elapsed / duration) * 100);

      if (progressFill) progressFill.style.width = `${progress}%`;

      for (let s = 0; s < telemetryStages.length; s++) {
        if (progress >= telemetryStages[s].p) {
          if (statusText) statusText.textContent = telemetryStages[s].text;
        }
      }

      ctx.fillStyle = 'rgba(6, 3, 12, 0.28)';
      ctx.fillRect(0, 0, w, h);

      const cx = w / 2;
      const cy = h / 2;
      const warpSpeed = 12 + (progress / 100) * 36;

      for (let i = 0; i < stars.length; i++) {
        const star = stars[i];
        star.z -= warpSpeed;

        if (star.z <= 0) {
          star.x = (Math.random() - 0.5) * w * 2;
          star.y = (Math.random() - 0.5) * h * 2;
          star.z = w;
        }

        const k = 280 / star.z;
        const px = star.x * k + cx;
        const py = star.y * k + cy;

        if (px >= 0 && px <= w && py >= 0 && py <= h) {
          const size = Math.max(0.6, (1 - star.z / w) * 3.2);
          const alpha = Math.min(1, (1 - star.z / w) * 1.4);

          ctx.beginPath();
          ctx.arc(px, py, size, 0, Math.PI * 2);
          ctx.fillStyle = `${star.color}${alpha})`;
          ctx.fill();

          // Star streak motion blur
          const prevK = 280 / (star.z + warpSpeed * 1.5);
          const prevPx = star.x * prevK + cx;
          const prevPy = star.y * prevK + cy;

          ctx.beginPath();
          ctx.moveTo(prevPx, prevPy);
          ctx.lineTo(px, py);
          ctx.strokeStyle = `${star.color}${alpha * 0.7})`;
          ctx.lineWidth = size * 0.9;
          ctx.stroke();
        }
      }

      if (progress >= 100) {
        setTimeout(dismissIntro, 250);
        return;
      }

      animId = requestAnimationFrame(loop);
    };

    animId = requestAnimationFrame(loop);
  },

  /**
   * Ambient Interactive Cursor Glow Aura
   */
  setupCursorAura() {
    if (window.matchMedia('(hover: none)').matches) return;

    // Remove any leftover heart cursor elements
    document.getElementById('heart-cursor-wrapper')?.remove();
    document.querySelectorAll('.mini-heart-spark').forEach(el => el.remove());

    let aura = document.getElementById('interactive-cursor-glow');
    if (!aura) {
      aura = document.createElement('div');
      aura.id = 'interactive-cursor-glow';
      document.body.appendChild(aura);
    }
    this.cursorAura = aura;

    let auraX = -100;
    let auraY = -100;
    let targetX = -100;
    let targetY = -100;

    window.addEventListener('mousemove', (e) => {
      targetX = e.clientX;
      targetY = e.clientY;
      aura.classList.add('active');
    }, { passive: true });

    document.addEventListener('mouseleave', () => {
      aura.classList.remove('active');
    }, { passive: true });

    const renderAura = () => {
      auraX += (targetX - auraX) * 0.16;
      auraY += (targetY - auraY) * 0.16;
      aura.style.transform = `translate3d(${auraX.toFixed(1)}px, ${auraY.toFixed(1)}px, 0)`;
      requestAnimationFrame(renderAura);
    };
    renderAura();

    const interactiveElements = 'a, button, input, select, textarea, .card, .kpi-card, .btn';
    document.addEventListener('mouseover', (e) => {
      if (e.target.closest(interactiveElements)) {
        aura.classList.add('hover-expand');
      }
    }, { passive: true });

    document.addEventListener('mouseout', (e) => {
      if (e.target.closest(interactiveElements)) {
        aura.classList.remove('hover-expand');
      }
    }, { passive: true });
  },

  /**
   * Injects tactile glowing ripple circles when buttons are clicked
   */
  setupButtonRipples() {
    document.addEventListener('click', (e) => {
      const btn = e.target.closest('.btn');
      if (!btn) return;

      const rect = btn.getBoundingClientRect();
      const circle = document.createElement('span');
      const diameter = Math.max(rect.width, rect.height);
      const radius = diameter / 2;

      circle.style.width = circle.style.height = `${diameter}px`;
      circle.style.left = `${e.clientX - rect.left - radius}px`;
      circle.style.top = `${e.clientY - rect.top - radius}px`;
      circle.className = 'btn-ripple';

      const existingRipple = btn.querySelector('.btn-ripple');
      if (existingRipple) existingRipple.remove();

      btn.appendChild(circle);
      setTimeout(() => circle.remove(), 700);
    });
  },

  /**
   * Form Input Focus Energy Halo & Micro-Interactions
   */
  setupInputInteractions() {
    const inputs = document.querySelectorAll('.form-control');
    inputs.forEach((input) => {
      input.addEventListener('focus', () => {
        const parent = input.closest('.form-group');
        if (parent) parent.classList.add('input-focus-pulse');
      });

      input.addEventListener('blur', () => {
        const parent = input.closest('.form-group');
        if (parent) parent.classList.remove('input-focus-pulse');
      });
    });
  },

  /**
   * Smooth animated counter ticker for KPI cards and metrics
   */
  animateNumbers(scope = document) {
    const targets = scope.querySelectorAll('.kpi-value, .category-stat-count, .stat-value');
    targets.forEach((el) => {
      const text = el.textContent.trim();
      const num = parseInt(text.replace(/,/g, ''), 10);
      if (isNaN(num) || num === 0 || el.getAttribute('data-animated') === 'true') return;

      el.setAttribute('data-animated', 'true');
      const duration = 1350;
      const start = 0;
      const startTime = performance.now();

      const easeOutExpo = (x) => (x === 1 ? 1 : 1 - Math.pow(2, -10 * x));

      const step = (currentTime) => {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        const currentVal = Math.round(start + (num - start) * easeOutExpo(progress));
        el.textContent = currentVal.toLocaleString();

        if (progress < 1) {
          requestAnimationFrame(step);
        } else {
          el.textContent = num.toLocaleString();
        }
      };

      requestAnimationFrame(step);
    });
  },

  /**
   * Pauses Canvas loop when tab is in background to save 100% CPU/battery
   */
  setupVisibilityHandler() {
    document.addEventListener('visibilitychange', () => {
      this.isPaused = document.hidden;
    });
  }
};

// Auto-initialize when the DOM is ready
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => Animations.init());
} else {
  Animations.init();
}
