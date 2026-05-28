"use client";

import { useEffect, useRef } from "react";

const PARTICLES_CDN = "https://cdn.jsdelivr.net/npm/particles.js@2.0.0/particles.min.js";

export default function ParticlesInner() {
  const containerRef = useRef(null);
  const initRef = useRef(false);

  useEffect(() => {
    if (initRef.current) return;
    initRef.current = true;

    const container = containerRef.current;
    if (!container) return;

    const script = document.createElement("script");
    script.src = PARTICLES_CDN;
    script.async = true;

    script.onload = () => {
      window.particlesJS("particles-js", {
        particles: {
          number: { value: 80, density: { enable: true, value_area: 800 } },
          color: { value: "#3b82f6" },
          shape: { type: "circle", stroke: { width: 0, color: "#000000" } },
          opacity: { value: 0.5, random: false, anim: { enable: false } },
          size: { value: 3, random: true, anim: { enable: false } },
          line_linked: { enable: true, distance: 150, color: "#3b82f6", opacity: 0.4, width: 1 },
          move: { enable: true, speed: 3, direction: "none", random: false, straight: false, out_mode: "out", attract: { enable: false } },
        },
        interactivity: {
          detect_on: "window",
          events: { onhover: { enable: true, mode: "repulse" }, onclick: { enable: true, mode: "push" }, resize: true },
          modes: {
            grab: { distance: 400, line_linked: { opacity: 1 } },
            repulse: { distance: 200 },
            push: { particles_nb: 4 },
          },
        },
        retina_detect: true,
      });
    };

    document.body.appendChild(script);

    return () => {
      script.remove();
      const canvas = container.querySelector("canvas");
      if (canvas) canvas.remove();
      delete window.particlesJS;
    };
  }, []);

  return (
    <div id="particles-js" ref={containerRef} className="particle-canvas"
      style={{ position: "fixed", inset: 0, zIndex: 0, pointerEvents: "none" }} />
  );
}
