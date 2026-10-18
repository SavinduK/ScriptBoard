import React, { useState, useEffect, useRef } from 'react';
import { KeyboardColors } from '../../types';
import { Mic, MicOff, ArrowLeft, Delete, CornerDownLeft, Space } from 'lucide-react';

interface VoiceTypingViewProps {
  colors: KeyboardColors;
  onInsertText: (text: string) => void;
  onBackspace: () => void;
  onEnter: () => void;
  onSpace: () => void;
  onBackToLetters: () => void;
}

export const VoiceTypingView: React.FC<VoiceTypingViewProps> = ({
  colors,
  onInsertText,
  onBackspace,
  onEnter,
  onSpace,
  onBackToLetters,
}) => {
  const [isListening, setIsListening] = useState<boolean>(true);
  const [transcript, setTranscript] = useState<string>('');
  const [statusMessage, setStatusMessage] = useState<string>('Listening... speak now');
  const recognitionRef = useRef<any>(null);

  useEffect(() => {
    // Check SpeechRecognition support
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (SpeechRecognition) {
      try {
        const recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = 'en-US';

        recognition.onstart = () => {
          setIsListening(true);
          setStatusMessage('Listening... speak into your microphone');
        };

        recognition.onresult = (event: any) => {
          let current = '';
          for (let i = event.resultIndex; i < event.results.length; i++) {
            current += event.results[i][0].transcript;
          }
          setTranscript(current);
        };

        recognition.onerror = (e: any) => {
          setStatusMessage(
            e.error === 'not-allowed'
              ? 'Microphone permission denied. Tap simulated phrases below:'
              : `Microphone status: ${e.error || 'ready'}`
          );
        };

        recognition.onend = () => {
          if (isListening) {
            try {
              recognition.start();
            } catch (_err) {}
          }
        };

        recognitionRef.current = recognition;
        recognition.start();
      } catch (_e) {
        setStatusMessage('Voice engine ready. Tap a quick phrase or type:');
      }
    } else {
      setStatusMessage('Web Speech API not supported in this browser. Quick voice simulation:');
    }

    return () => {
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch (_e) {}
      }
    };
  }, []);

  const handleToggleMic = () => {
    if (isListening) {
      setIsListening(false);
      setStatusMessage('Voice recognition paused');
      if (recognitionRef.current) {
        try {
          recognitionRef.current.stop();
        } catch (_e) {}
      }
    } else {
      setIsListening(true);
      setStatusMessage('Listening... speak now');
      if (recognitionRef.current) {
        try {
          recognitionRef.current.start();
        } catch (_e) {}
      }
    }
  };

  const handleSendTranscript = (textToSend?: string) => {
    const text = textToSend || transcript;
    if (text.trim()) {
      onInsertText(text.trim() + ' ');
      setTranscript('');
    }
  };

  return (
    <div
      style={{ backgroundColor: colors.background }}
      className="w-full h-[290px] flex flex-col justify-between p-3 select-none"
    >
      {/* Top Header */}
      <div className="flex items-center justify-between pb-2 border-b border-black/15">
        <button
          type="button"
          onClick={onBackToLetters}
          className="flex items-center space-x-1.5 px-3 py-1 rounded-lg text-xs font-semibold cursor-pointer"
          style={{
            backgroundColor: colors.functionKeyBackground,
            color: colors.functionKeyTextColor,
          }}
        >
          <ArrowLeft size={14} />
          <span>Keyboard</span>
        </button>

        <span
          className="text-xs font-medium"
          style={{ color: colors.letterKeySecondaryTextColor }}
        >
          KeyPro Voice Typing
        </span>

        <button
          type="button"
          onClick={onBackspace}
          className="p-1.5 rounded-lg flex items-center justify-center cursor-pointer"
          style={{
            backgroundColor: colors.functionKeyBackground,
            color: colors.functionKeyTextColor,
          }}
          title="Backspace"
        >
          <Delete size={16} />
        </button>
      </div>

      {/* Middle Animated Mic & Transcript Area */}
      <div className="flex-1 flex flex-col items-center justify-center py-2 px-4 text-center">
        {/* Animated Mic Button */}
        <div className="relative mb-3 flex items-center justify-center">
          {isListening && (
            <div
              className="absolute w-20 h-20 rounded-full animate-ping opacity-30"
              style={{ backgroundColor: colors.enterKeyBackground }}
            />
          )}
          <button
            type="button"
            onClick={handleToggleMic}
            className="w-16 h-16 rounded-full flex items-center justify-center transition-transform active:scale-90 cursor-pointer shadow-lg z-10"
            style={{
              backgroundColor: isListening ? colors.enterKeyBackground : colors.functionKeyBackground,
              color: isListening ? colors.enterKeyTextColor : colors.letterKeyTextColor,
            }}
          >
            {isListening ? <Mic size={28} /> : <MicOff size={28} />}
          </button>
        </div>

        <p
          className="text-xs font-medium mb-2"
          style={{ color: colors.letterKeySecondaryTextColor }}
        >
          {statusMessage}
        </p>

        {/* Live speech preview */}
        <div
          className="w-full max-w-md min-h-[44px] max-h-[64px] px-3 py-1.5 rounded-xl border flex items-center justify-center overflow-y-auto"
          style={{
            backgroundColor: colors.letterKeyBackground,
            borderColor: colors.functionKeyBackground,
            color: transcript ? colors.letterKeyTextColor : colors.letterKeySecondaryTextColor,
          }}
        >
          <span className="text-sm font-medium">
            {transcript || 'Say something like: "Sounds great, see you at 3pm tomorrow!"'}
          </span>
        </div>

        {/* Quick Voice Prompt Shortcuts */}
        <div className="flex flex-wrap items-center justify-center gap-1.5 mt-2.5">
          {['"Hello!"', '"Thanks a lot!"', '"I am on my way"', '"Sounds good"', '"Call you later"'].map(
            (phrase, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => handleSendTranscript(phrase.replace(/"/g, ''))}
                className="px-2 py-0.5 rounded-md text-[11px] font-medium transition-colors hover:opacity-80 cursor-pointer"
                style={{
                  backgroundColor: colors.functionKeyBackground,
                  color: colors.letterKeyTextColor,
                }}
              >
                {phrase}
              </button>
            )
          )}
        </div>
      </div>

      {/* Bottom Controls */}
      <div className="flex items-center justify-between pt-2 border-t border-black/15">
        <button
          type="button"
          onClick={onBackToLetters}
          className="px-4 py-1.5 rounded-lg text-xs font-bold cursor-pointer"
          style={{
            backgroundColor: colors.functionKeyBackground,
            color: colors.functionKeyTextColor,
          }}
        >
          ABC
        </button>

        <div className="flex items-center space-x-2">
          <button
            type="button"
            onClick={onSpace}
            className="px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1 cursor-pointer"
            style={{
              backgroundColor: colors.functionKeyBackground,
              color: colors.letterKeyTextColor,
            }}
          >
            <Space size={14} />
            <span>Space</span>
          </button>

          <button
            type="button"
            onClick={() => handleSendTranscript()}
            disabled={!transcript.trim()}
            className="px-4 py-1.5 rounded-lg text-xs font-bold flex items-center space-x-1 cursor-pointer disabled:opacity-40"
            style={{
              backgroundColor: colors.enterKeyBackground,
              color: colors.enterKeyTextColor,
            }}
          >
            <CornerDownLeft size={14} />
            <span>Insert</span>
          </button>
        </div>
      </div>
    </div>
  );
};
