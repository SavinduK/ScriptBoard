// Audio and Haptic feedback utility
let audioCtx: AudioContext | null = null;

function getAudioContext(): AudioContext | null {
  if (typeof window === 'undefined') return null;
  if (!audioCtx) {
    const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (AudioContextClass) {
      audioCtx = new AudioContextClass();
    }
  }
  if (audioCtx && audioCtx.state === 'suspended') {
    audioCtx.resume();
  }
  return audioCtx;
}

export function playKeySound(type: 'letter' | 'space' | 'enter' | 'backspace' | 'fn' = 'letter') {
  try {
    const ctx = getAudioContext();
    if (!ctx) return;

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();

    const now = ctx.currentTime;

    let freq = 850;
    let duration = 0.018;

    if (type === 'space' || type === 'enter') {
      freq = 320;
      duration = 0.035;
    } else if (type === 'backspace') {
      freq = 480;
      duration = 0.025;
    } else if (type === 'fn') {
      freq = 650;
      duration = 0.02;
    }

    osc.type = 'sine';
    osc.frequency.setValueAtTime(freq, now);
    osc.frequency.exponentialRampToValueAtTime(freq * 0.4, now + duration);

    gain.gain.setValueAtTime(0.08, now);
    gain.gain.exponentialRampToValueAtTime(0.001, now + duration);

    osc.connect(gain);
    gain.connect(ctx.destination);

    osc.start(now);
    osc.stop(now + duration);
  } catch (_e) {
    // audio context ignored if browser restricts
  }
}

export function triggerHaptic() {
  try {
    if (typeof window !== 'undefined' && 'vibrate' in navigator) {
      navigator.vibrate(15);
    }
  } catch (_e) {}
}

export function performKeyPressFeedback(
  soundEnabled: boolean,
  hapticEnabled: boolean,
  keyType: 'letter' | 'space' | 'enter' | 'backspace' | 'fn' = 'letter'
) {
  if (soundEnabled) {
    playKeySound(keyType);
  }
  if (hapticEnabled) {
    triggerHaptic();
  }
}
