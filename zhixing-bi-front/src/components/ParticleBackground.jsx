"use client";

import { useState, useEffect } from "react";
import ParticlesInner from "./ParticlesInner";

export default function ParticleBackground() {
  const [mounted, setMounted] = useState(false);
  useEffect(() => { setMounted(true); }, []);
  if (!mounted) return null;
  return <ParticlesInner />;
}
