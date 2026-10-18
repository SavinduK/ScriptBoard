import { LanguagePack } from '../types';

export const LanguagePackRegistry: { ALL_PACKAGES: LanguagePack[] } = {
  ALL_PACKAGES: [
    {
      id: 'en',
      name: 'English',
      nativeName: 'English',
      flag: '🇺🇸',
      version: '1.0.0',
      description: 'Standard English QWERTY touch layout with number hints & smart suggestions',
      file: 'en.json',
      sizeBytes: 25000,
      sizeDisplay: '25 KB',
      wordCount: 100,
      isBuiltIn: true,
    },
    {
      id: 'si',
      name: 'Sinhala',
      nativeName: 'සිංහල',
      flag: '🇱🇰',
      version: '1.2.0',
      description: 'Official 5-row Wijesekara touch layout with dynamic vowel modifiers (පිල්ලම්)',
      file: 'si.json',
      sizeBytes: 45200,
      sizeDisplay: '45 KB',
      wordCount: 50,
      isBuiltIn: false,
    },
  ],
};
