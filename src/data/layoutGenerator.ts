import { KeyItem, ShiftState } from '../types';

export const ToggleableKeys: Record<string, string> = {
  // Numbers <-> Symbols (Symbols 1 Row 1 <-> Symbols 2 Row 1)
  '1': '~',
  '2': '@',
  '3': '#',
  '4': '$',
  '5': '%',
  '6': '^',
  '7': '&',
  '8': '*',
  '9': '(',
  '0': ')',

  // Symbols 1 Row 2 & 3 <-> Symbols 2 Row 2 & 3
  '@': '$',
  '#': '€',
  '£': '¥',
  '_': '¢',
  '&': '©',
  '-': '®',
  '+': '™',
  '(': '<',
  ')': '>',
  '/': '÷',

  '*': '×',
  '"': '«',
  "'": '»',
  ':': '…',
  ';': '§',
  '!': '¡',
  '?': '¿',

  // Reverse toggleables (Symbols 2 <-> Symbols 1)
  '~': '1',
  '$': '@',
  '€': '#',
  '¥': '£',
  '¢': '_',
  '©': '&',
  '®': '-',
  '™': '+',
  '<': '(',
  '>': ')',
  '÷': '/',

  '×': '*',
  '«': '"',
  '»': "'",
  '…': ':',
  '§': ';',
  '¡': '!',
  '¿': '?',
};

export function getToggleable(key: string): string | null {
  return ToggleableKeys[key] || null;
}

export function isSinhalaConsonant(charOrText: string): boolean {
  if (!charOrText) return false;
  const first = charOrText.charCodeAt(0);
  return (first >= 0x0d9a && first <= 0x0dc6) || charOrText === 'ක්‍ෂ';
}

export function getLaptopKeyRows(isCtrlActive: boolean, isAltActive: boolean): KeyItem[][] {
  const row1: KeyItem[] = [
    { primaryText: 'ESC', action: { type: 'ESCAPE' }, weight: 1.1, isFunctional: true, testTag: 'key_laptop_esc' },
    { primaryText: '/', action: { type: 'INSERT_TEXT', text: '/' }, weight: 0.9, testTag: 'key_laptop_slash' },
    { primaryText: '—', action: { type: 'INSERT_TEXT', text: '—' }, weight: 0.9, testTag: 'key_laptop_dash' },
    { primaryText: 'HOME', action: { type: 'HOME' }, weight: 1.25, isFunctional: true, testTag: 'key_laptop_home' },
    { primaryText: '↑', action: { type: 'CURSOR_UP' }, weight: 0.95, isFunctional: true, testTag: 'key_laptop_up' },
    { primaryText: 'END', action: { type: 'END' }, weight: 1.25, isFunctional: true, testTag: 'key_laptop_end' },
    { primaryText: 'PGUP', action: { type: 'PAGE_UP' }, weight: 1.25, isFunctional: true, testTag: 'key_laptop_pgup' },
  ];

  const row2: KeyItem[] = [
    { primaryText: '↹', action: { type: 'TAB' }, weight: 1.1, isFunctional: true, testTag: 'key_laptop_tab' },
    { primaryText: 'CTRL', action: { type: 'TOGGLE_CTRL' }, weight: 1.1, isFunctional: true, isActiveModifier: isCtrlActive, testTag: 'key_laptop_ctrl' },
    { primaryText: 'ALT', action: { type: 'TOGGLE_ALT' }, weight: 1.1, isFunctional: true, isActiveModifier: isAltActive, testTag: 'key_laptop_alt' },
    { primaryText: '←', action: { type: 'CURSOR_LEFT' }, weight: 1.0, isFunctional: true, testTag: 'key_laptop_left' },
    { primaryText: '↓', action: { type: 'CURSOR_DOWN' }, weight: 0.95, isFunctional: true, testTag: 'key_laptop_down' },
    { primaryText: '→', action: { type: 'CURSOR_RIGHT' }, weight: 1.0, isFunctional: true, testTag: 'key_laptop_right' },
    { primaryText: 'PGDN', action: { type: 'PAGE_DOWN' }, weight: 1.25, isFunctional: true, testTag: 'key_laptop_pgdn' },
  ];

  return [row1, row2];
}

export function getDedicatedNumberRow(holdForSymbols: boolean = true): KeyItem[] {
  const digits = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'];
  const alternates = ['~', '@', '#', '$', '%', '^', '&', '*', '(', ')'];
  return digits.map((digit, idx) => ({
    primaryText: digit,
    secondaryText: holdForSymbols ? alternates[idx] : null,
    action: { type: 'INSERT_TEXT', text: digit },
    weight: 1,
    testTag: `key_dedicated_num_${digit}`,
  }));
}

export function getQwertyRows(
  shiftState: ShiftState,
  holdForSymbols: boolean = true,
  languageDisplayName: string = 'English',
  includeNumberRow: boolean = false,
  showLanguageSwitchKey: boolean = false
): KeyItem[][] {
  const isUpper = shiftState !== 'OFF';
  const row1Letters = ['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'];
  const row1Digits = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'];
  const row1: KeyItem[] = row1Letters.map((char, idx) => {
    const text = isUpper ? char.toUpperCase() : char;
    return {
      primaryText: text,
      secondaryText: holdForSymbols ? row1Digits[idx] : null,
      action: { type: 'INSERT_TEXT', text },
      weight: 1,
      testTag: `key_char_${char}`,
    };
  });

  const row2Letters = ['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'];
  const row2Symbols = ['@', '#', '$', '%', '&', '-', '+', '(', ')'];
  const row2: KeyItem[] = row2Letters.map((char, idx) => {
    const text = isUpper ? char.toUpperCase() : char;
    return {
      primaryText: text,
      secondaryText: holdForSymbols ? row2Symbols[idx] : null,
      action: { type: 'INSERT_TEXT', text },
      weight: 1,
      testTag: `key_char_${char}`,
    };
  });

  const shiftLabel = shiftState === 'OFF' ? '⇧' : shiftState === 'ONCE' ? '⇪' : '▲';
  const row3Letters = ['z', 'x', 'c', 'v', 'b', 'n', 'm'];
  const row3Symbols = ['*', '"', "'", ':', ';', '!', '?'];
  const row3Middle: KeyItem[] = row3Letters.map((char, idx) => {
    const text = isUpper ? char.toUpperCase() : char;
    return {
      primaryText: text,
      secondaryText: holdForSymbols ? row3Symbols[idx] : null,
      action: { type: 'INSERT_TEXT', text },
      weight: 1,
      testTag: `key_char_${char}`,
    };
  });

  const shiftKey: KeyItem = {
    primaryText: shiftLabel,
    action: { type: 'TOGGLE_SHIFT' },
    weight: 1.4,
    isFunctional: true,
    isActiveModifier: shiftState !== 'OFF',
    testTag: 'key_shift',
  };

  const backspaceKey: KeyItem = {
    primaryText: '⌫',
    action: { type: 'BACKSPACE' },
    weight: 1.4,
    isFunctional: true,
    testTag: 'key_backspace',
  };

  const row3: KeyItem[] = [shiftKey, ...row3Middle, backspaceKey];

  const row4: KeyItem[] = [
    { primaryText: '?123', action: { type: 'SWITCH_TO_SYMBOLS' }, weight: 1.35, isFunctional: true, testTag: 'key_switch_symbols' },
    { primaryText: ',', action: { type: 'INSERT_TEXT', text: ',' }, weight: 0.95, isFunctional: true, testTag: 'key_comma' },
    { primaryText: '☺', action: { type: 'SWITCH_TO_EMOJI' }, weight: 0.95, isFunctional: true, testTag: 'key_emoji' },
  ];

  if (showLanguageSwitchKey) {
    row4.push({
      primaryText: '🌐',
      action: { type: 'SWITCH_LANGUAGE' },
      weight: 0.95,
      isFunctional: true,
      testTag: 'key_switch_lang',
    });
  }

  row4.push({
    primaryText: languageDisplayName,
    action: { type: 'SPACE' },
    weight: showLanguageSwitchKey ? 2.95 : 3.9,
    testTag: 'key_space',
  });

  row4.push({
    primaryText: '.',
    secondaryText: holdForSymbols ? '/' : null,
    action: { type: 'INSERT_TEXT', text: '.' },
    weight: 0.95,
    isFunctional: true,
    testTag: 'key_period',
  });

  row4.push({
    primaryText: '↵',
    action: { type: 'ENTER' },
    weight: 1.45,
    isAccent: true,
    testTag: 'key_enter',
  });

  const top = includeNumberRow ? [getDedicatedNumberRow(holdForSymbols)] : [];
  return [...top, row1, row2, row3, row4];
}

export function getSinhalaRows(
  selectedLetter: string = 'ව',
  showLanguageSwitchKey: boolean = false
): KeyItem[][] {
  const sel = selectedLetter || 'ව';

  const row1: KeyItem[] = [
    { primaryText: `${sel}ු`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ු`, modifier: 'ු' }, testTag: 'key_si_vu' },
    { primaryText: `${sel}ඃ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ඃ`, modifier: 'ඃ' }, testTag: 'key_si_visarga' },
    { primaryText: `${sel}ැ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ැ`, modifier: 'ැ' }, testTag: 'key_si_vae' },
    { primaryText: `${sel}ො`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ො`, modifier: 'ො' }, testTag: 'key_si_vo' },
    { primaryText: 'ර', action: { type: 'INSERT_TEXT', text: 'ර' }, isActiveModifier: sel === 'ර', testTag: 'key_si_ra' },
    { primaryText: 'හ', action: { type: 'INSERT_TEXT', text: 'හ' }, isActiveModifier: sel === 'හ', testTag: 'key_si_ha' },
    { primaryText: 'ම', action: { type: 'INSERT_TEXT', text: 'ම' }, isActiveModifier: sel === 'ම', testTag: 'key_si_ma' },
    { primaryText: 'ස', action: { type: 'INSERT_TEXT', text: 'ස' }, isActiveModifier: sel === 'ස', testTag: 'key_si_sa' },
    { primaryText: 'ද', action: { type: 'INSERT_TEXT', text: 'ද' }, isActiveModifier: sel === 'ද', testTag: 'key_si_da' },
    { primaryText: 'ච', action: { type: 'INSERT_TEXT', text: 'ච' }, isActiveModifier: sel === 'ච', testTag: 'key_si_cha' },
  ];

  const row2: KeyItem[] = [
    { primaryText: `${sel}ූ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ූ`, modifier: 'ූ' }, testTag: 'key_si_vuu' },
    { primaryText: `${sel}ා`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ා`, modifier: 'ා' }, testTag: 'key_si_vaa' },
    { primaryText: `${sel}ෑ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ෑ`, modifier: 'ෑ' }, testTag: 'key_si_vaae' },
    { primaryText: `${sel}ෙ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ෙ`, modifier: 'ෙ' }, testTag: 'key_si_ve' },
    { primaryText: 'ෆ', action: { type: 'INSERT_TEXT', text: 'ෆ' }, isActiveModifier: sel === 'ෆ', testTag: 'key_si_fa' },
    { primaryText: 'ශ', action: { type: 'INSERT_TEXT', text: 'ශ' }, isActiveModifier: sel === 'ශ', testTag: 'key_si_sha' },
    { primaryText: 'ඹ', action: { type: 'INSERT_TEXT', text: 'ඹ' }, isActiveModifier: sel === 'ඹ', testTag: 'key_si_mba' },
    { primaryText: 'ෂ', action: { type: 'INSERT_TEXT', text: 'ෂ' }, isActiveModifier: sel === 'ෂ', testTag: 'key_si_ssha' },
    { primaryText: 'ධ', action: { type: 'INSERT_TEXT', text: 'ධ' }, isActiveModifier: sel === 'ධ', testTag: 'key_si_dha' },
    { primaryText: 'ක්‍ෂ', action: { type: 'INSERT_TEXT', text: 'ක්‍ෂ' }, isActiveModifier: sel === 'ක්‍ෂ', testTag: 'key_si_ksha' },
  ];

  const row3: KeyItem[] = [
    { primaryText: `${sel}ි`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ි`, modifier: 'ි' }, testTag: 'key_si_vi' },
    { primaryText: `${sel}ී`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ී`, modifier: 'ී' }, testTag: 'key_si_vii' },
    { primaryText: `${sel}ෘ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ෘ`, modifier: 'ෘ' }, testTag: 'key_si_vru' },
    { primaryText: `${sel}ෟ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ෟ`, modifier: 'ෟ' }, testTag: 'key_si_vlu' },
    { primaryText: 'ය', action: { type: 'INSERT_TEXT', text: 'ය' }, isActiveModifier: sel === 'ය', testTag: 'key_si_ya' },
    { primaryText: 'ව', action: { type: 'INSERT_TEXT', text: 'ව' }, isActiveModifier: sel === 'ව', testTag: 'key_si_va' },
    { primaryText: 'න', action: { type: 'INSERT_TEXT', text: 'න' }, isActiveModifier: sel === 'න', testTag: 'key_si_na' },
    { primaryText: 'ක', action: { type: 'INSERT_TEXT', text: 'ක' }, isActiveModifier: sel === 'ක', testTag: 'key_si_ka' },
    { primaryText: 'ත', action: { type: 'INSERT_TEXT', text: 'ත' }, isActiveModifier: sel === 'ත', testTag: 'key_si_ta' },
    { primaryText: `${sel}ං`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ං`, modifier: 'ං' }, testTag: 'key_si_anusvara' },
  ];

  const row4: KeyItem[] = [
    { primaryText: `${sel}ං`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ං`, modifier: 'ං' }, testTag: 'key_si_vam' },
    { primaryText: `${sel}්`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}්`, modifier: '්' }, testTag: 'key_si_hal' },
    { primaryText: `${sel}ෛ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ෛ`, modifier: 'ෛ' }, testTag: 'key_si_vai' },
    { primaryText: `${sel}්‍ය`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}්‍ය`, modifier: '්‍ය' }, testTag: 'key_si_yansaya' },
    { primaryText: 'ට', action: { type: 'INSERT_TEXT', text: 'ට' }, isActiveModifier: sel === 'ට', testTag: 'key_si_tta' },
    { primaryText: 'ළු', action: { type: 'INSERT_TEXT', text: 'ළු' }, isActiveModifier: sel === 'ළු' || sel === 'ළ', testTag: 'key_si_lu' },
    { primaryText: 'ණ', action: { type: 'INSERT_TEXT', text: 'ණ' }, isActiveModifier: sel === 'ණ', testTag: 'key_si_nna' },
    { primaryText: 'බ', action: { type: 'INSERT_TEXT', text: 'බ' }, isActiveModifier: sel === 'බ', testTag: 'key_si_ba' },
    { primaryText: 'ථ', action: { type: 'INSERT_TEXT', text: 'ථ' }, isActiveModifier: sel === 'ථ', testTag: 'key_si_tha' },
    { primaryText: 'ග', action: { type: 'INSERT_TEXT', text: 'ග' }, isActiveModifier: sel === 'ග', testTag: 'key_si_ga' },
  ];

  const row5: KeyItem[] = [
    { primaryText: `${sel}ෞ`, action: { type: 'INSERT_SINHALA_PILLAM', baseWithPillam: `${sel}ෞ`, modifier: 'ෞ' }, testTag: 'key_si_vau' },
    { primaryText: 'ජ', action: { type: 'INSERT_TEXT', text: 'ජ' }, isActiveModifier: sel === 'ජ', testTag: 'key_si_ja' },
    { primaryText: 'ඩ', action: { type: 'INSERT_TEXT', text: 'ඩ' }, isActiveModifier: sel === 'ඩ', testTag: 'key_si_dda' },
    { primaryText: 'ඪ', action: { type: 'INSERT_TEXT', text: 'ඪ' }, isActiveModifier: sel === 'ඪ', testTag: 'key_si_ddha' },
    { primaryText: 'ඛ', action: { type: 'INSERT_TEXT', text: 'ඛ' }, isActiveModifier: sel === 'ඛ', testTag: 'key_si_kha' },
    { primaryText: 'භ', action: { type: 'INSERT_TEXT', text: 'භ' }, isActiveModifier: sel === 'භ', testTag: 'key_si_bha' },
    { primaryText: 'ප', action: { type: 'INSERT_TEXT', text: 'ප' }, isActiveModifier: sel === 'ප', testTag: 'key_si_pa' },
    { primaryText: 'ල', action: { type: 'INSERT_TEXT', text: 'ල' }, isActiveModifier: sel === 'ල', testTag: 'key_si_la' },
    { primaryText: 'ළ', action: { type: 'INSERT_TEXT', text: 'ළ' }, isActiveModifier: sel === 'ළ', testTag: 'key_si_lla' },
    { primaryText: '⌫', action: { type: 'BACKSPACE' }, weight: 1.0, isFunctional: true, testTag: 'key_backspace' },
  ];

  const row6: KeyItem[] = [
    { primaryText: '?123', action: { type: 'SWITCH_TO_SYMBOLS' }, weight: 1.35, isFunctional: true, testTag: 'key_switch_symbols' },
    { primaryText: ',', action: { type: 'INSERT_TEXT', text: ',' }, weight: 0.95, isFunctional: true, testTag: 'key_comma' },
    { primaryText: '☺', action: { type: 'SWITCH_TO_EMOJI' }, weight: 0.95, isFunctional: true, testTag: 'key_emoji' },
  ];

  if (showLanguageSwitchKey) {
    row6.push({
      primaryText: '🌐',
      action: { type: 'SWITCH_LANGUAGE' },
      weight: 0.95,
      isFunctional: true,
      testTag: 'key_switch_lang',
    });
  }

  row6.push({
    primaryText: 'සිංහල',
    action: { type: 'SPACE' },
    weight: showLanguageSwitchKey ? 2.95 : 3.8,
    testTag: 'key_space',
  });

  row6.push({
    primaryText: '.',
    action: { type: 'INSERT_TEXT', text: '.' },
    weight: 0.95,
    isFunctional: true,
    testTag: 'key_period',
  });

  row6.push({
    primaryText: '↵',
    action: { type: 'ENTER' },
    weight: 1.45,
    isAccent: true,
    testTag: 'key_enter',
  });

  return [row1, row2, row3, row4, row5, row6];
}

export function getSymbols1Rows(
  languageDisplayName: string = 'English',
  holdForSymbols: boolean = true
): KeyItem[][] {
  const row1Chars = ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'];
  const row1: KeyItem[] = row1Chars.map(num => ({
    primaryText: num,
    secondaryText: holdForSymbols ? getToggleable(num) : null,
    action: { type: 'INSERT_TEXT', text: num },
    weight: 1,
    testTag: `key_num_${num}`,
  }));

  const row2Chars = ['@', '#', '£', '_', '&', '-', '+', '(', ')', '/'];
  const row2: KeyItem[] = row2Chars.map(sym => ({
    primaryText: sym,
    secondaryText: holdForSymbols ? getToggleable(sym) : null,
    action: { type: 'INSERT_TEXT', text: sym },
    weight: 1,
    testTag: `key_sym_${sym}`,
  }));

  const row3Chars = ['*', '"', "'", ':', ';', '!', '?'];
  const row3Middle: KeyItem[] = row3Chars.map(sym => ({
    primaryText: sym,
    secondaryText: holdForSymbols ? getToggleable(sym) : null,
    action: { type: 'INSERT_TEXT', text: sym },
    weight: 1,
    testTag: `key_sym_${sym}`,
  }));

  const altSymbolsKey: KeyItem = {
    primaryText: '=\\<',
    action: { type: 'SWITCH_TO_MORE_SYMBOLS' },
    weight: 1.4,
    isFunctional: true,
    testTag: 'key_more_symbols',
  };

  const backspaceKey: KeyItem = {
    primaryText: '⌫',
    action: { type: 'BACKSPACE' },
    weight: 1.4,
    isFunctional: true,
    testTag: 'key_backspace',
  };

  const row3 = [altSymbolsKey, ...row3Middle, backspaceKey];

  const row4: KeyItem[] = [
    { primaryText: 'ABC', action: { type: 'SWITCH_TO_LETTERS' }, weight: 1.35, isFunctional: true, testTag: 'key_switch_abc' },
    { primaryText: ',', action: { type: 'INSERT_TEXT', text: ',' }, weight: 0.95, isFunctional: true, testTag: 'key_comma' },
    { primaryText: '12\n34', action: { type: 'SWITCH_TO_NUMPAD' }, weight: 1.0, isFunctional: true, testTag: 'key_switch_numpad' },
    { primaryText: languageDisplayName, action: { type: 'SPACE' }, weight: 3.8, testTag: 'key_space' },
    { primaryText: '.', action: { type: 'INSERT_TEXT', text: '.' }, weight: 0.95, isFunctional: true, testTag: 'key_period' },
    { primaryText: '↵', action: { type: 'ENTER' }, weight: 1.45, isAccent: true, testTag: 'key_enter' },
  ];

  return [row1, row2, row3, row4];
}

export function getSymbols2Rows(
  languageDisplayName: string = 'English',
  holdForSymbols: boolean = true
): KeyItem[][] {
  const row1Chars = ['~', '`', '|', '\\', '{', '}', '[', ']', '%', '^', '°'];
  const row1: KeyItem[] = row1Chars.map(sym => ({
    primaryText: sym,
    secondaryText: holdForSymbols ? getToggleable(sym) : null,
    action: { type: 'INSERT_TEXT', text: sym },
    weight: 1,
    testTag: `key_sym2_${sym}`,
  }));

  const row2Chars = ['$', '€', '¥', '¢', '©', '®', '™', '§', '<', '>'];
  const row2: KeyItem[] = row2Chars.map(sym => ({
    primaryText: sym,
    secondaryText: holdForSymbols ? getToggleable(sym) : null,
    action: { type: 'INSERT_TEXT', text: sym },
    weight: 1,
    testTag: `key_sym2_${sym}`,
  }));

  const row3Chars = ['×', '«', '»', '…', '÷', '¡', '¿'];
  const row3Middle: KeyItem[] = row3Chars.map(sym => ({
    primaryText: sym,
    secondaryText: holdForSymbols ? getToggleable(sym) : null,
    action: { type: 'INSERT_TEXT', text: sym },
    weight: 1,
    testTag: `key_sym2_${sym}`,
  }));

  const symbols1Key: KeyItem = {
    primaryText: '?123',
    action: { type: 'SWITCH_TO_SYMBOLS' },
    weight: 1.4,
    isFunctional: true,
    testTag: 'key_switch_sym1',
  };

  const backspaceKey: KeyItem = {
    primaryText: '⌫',
    action: { type: 'BACKSPACE' },
    weight: 1.4,
    isFunctional: true,
    testTag: 'key_backspace',
  };

  const row3 = [symbols1Key, ...row3Middle, backspaceKey];

  const row4: KeyItem[] = [
    { primaryText: 'ABC', action: { type: 'SWITCH_TO_LETTERS' }, weight: 1.35, isFunctional: true, testTag: 'key_switch_abc' },
    { primaryText: ',', action: { type: 'INSERT_TEXT', text: ',' }, weight: 0.95, isFunctional: true, testTag: 'key_comma' },
    { primaryText: '12\n34', action: { type: 'SWITCH_TO_NUMPAD' }, weight: 1.0, isFunctional: true, testTag: 'key_switch_numpad' },
    { primaryText: languageDisplayName, action: { type: 'SPACE' }, weight: 3.8, testTag: 'key_space' },
    { primaryText: '.', action: { type: 'INSERT_TEXT', text: '.' }, weight: 0.95, isFunctional: true, testTag: 'key_period' },
    { primaryText: '↵', action: { type: 'ENTER' }, weight: 1.45, isAccent: true, testTag: 'key_enter' },
  ];

  return [row1, row2, row3, row4];
}

export function getNumpadRows(holdForSymbols: boolean = true): KeyItem[][] {
  const row1: KeyItem[] = [
    { primaryText: '7', secondaryText: holdForSymbols ? '&' : null, action: { type: 'INSERT_TEXT', text: '7' }, weight: 1, testTag: 'np_7' },
    { primaryText: '8', secondaryText: holdForSymbols ? '*' : null, action: { type: 'INSERT_TEXT', text: '8' }, weight: 1, testTag: 'np_8' },
    { primaryText: '9', secondaryText: holdForSymbols ? '(' : null, action: { type: 'INSERT_TEXT', text: '9' }, weight: 1, testTag: 'np_9' },
    { primaryText: '/', action: { type: 'INSERT_TEXT', text: '/' }, weight: 1, isFunctional: true, testTag: 'np_div' },
    { primaryText: '⌫', action: { type: 'BACKSPACE' }, weight: 1, isFunctional: true, testTag: 'np_backspace' },
  ];

  const row2: KeyItem[] = [
    { primaryText: '4', secondaryText: holdForSymbols ? '$' : null, action: { type: 'INSERT_TEXT', text: '4' }, weight: 1, testTag: 'np_4' },
    { primaryText: '5', secondaryText: holdForSymbols ? '%' : null, action: { type: 'INSERT_TEXT', text: '5' }, weight: 1, testTag: 'np_5' },
    { primaryText: '6', secondaryText: holdForSymbols ? '^' : null, action: { type: 'INSERT_TEXT', text: '6' }, weight: 1, testTag: 'np_6' },
    { primaryText: '*', action: { type: 'INSERT_TEXT', text: '*' }, weight: 1, isFunctional: true, testTag: 'np_mul' },
    { primaryText: 'DEL', action: { type: 'DELETE_FORWARD' }, weight: 1, isFunctional: true, testTag: 'np_del' },
  ];

  const row3: KeyItem[] = [
    { primaryText: '1', secondaryText: holdForSymbols ? '~' : null, action: { type: 'INSERT_TEXT', text: '1' }, weight: 1, testTag: 'np_1' },
    { primaryText: '2', secondaryText: holdForSymbols ? '@' : null, action: { type: 'INSERT_TEXT', text: '2' }, weight: 1, testTag: 'np_2' },
    { primaryText: '3', secondaryText: holdForSymbols ? '#' : null, action: { type: 'INSERT_TEXT', text: '3' }, weight: 1, testTag: 'np_3' },
    { primaryText: '-', action: { type: 'INSERT_TEXT', text: '-' }, weight: 1, isFunctional: true, testTag: 'np_minus' },
    { primaryText: '(', action: { type: 'INSERT_TEXT', text: '(' }, weight: 1, isFunctional: true, testTag: 'np_op' },
  ];

  const row4: KeyItem[] = [
    { primaryText: 'ABC', action: { type: 'SWITCH_TO_LETTERS' }, weight: 1.2, isFunctional: true, testTag: 'np_abc' },
    { primaryText: '0', secondaryText: holdForSymbols ? ')' : null, action: { type: 'INSERT_TEXT', text: '0' }, weight: 1, testTag: 'np_0' },
    { primaryText: '.', action: { type: 'INSERT_TEXT', text: '.' }, weight: 1, testTag: 'np_dot' },
    { primaryText: '+', action: { type: 'INSERT_TEXT', text: '+' }, weight: 1, isFunctional: true, testTag: 'np_plus' },
    { primaryText: '↵', action: { type: 'ENTER' }, weight: 1.2, isAccent: true, testTag: 'np_enter' },
  ];

  return [row1, row2, row3, row4];
}

export function getCompleteLaptopRows(
  shiftState: ShiftState = 'OFF',
  isCtrlActive: boolean = false,
  isAltActive: boolean = false,
  languageDisplayName: string = 'English'
): KeyItem[][] {
  const isShift = shiftState !== 'OFF';
  const isCaps = shiftState === 'CAPS_LOCKED';

  // Row 0: Function keys (Esc, F1 - F12, Del)
  const row0: KeyItem[] = [
    { primaryText: 'Esc', action: { type: 'ESCAPE' }, weight: 1.15, isFunctional: true, testTag: 'pc_esc' },
  ];
  for (let num = 1; num <= 12; num++) {
    row0.push({
      primaryText: `F${num}`,
      action: { type: 'FUNCTION_KEY', fNumber: num },
      weight: 1.0,
      isFunctional: true,
      testTag: `pc_f${num}`,
    });
  }
  row0.push({ primaryText: 'Del', action: { type: 'DELETE_FORWARD' }, weight: 1.15, isFunctional: true, testTag: 'pc_del' });

  // Row 1: Number row (`/~, 1/!, 2/@, ..., =/+, Backspace)
  const numPairs = [
    ['`', '~'], ['1', '!'], ['2', '@'], ['3', '#'], ['4', '$'],
    ['5', '%'], ['6', '^'], ['7', '&'], ['8', '*'], ['9', '('],
    ['0', ')'], ['-', '_'], ['=', '+'],
  ];
  const row1: KeyItem[] = [];
  numPairs.forEach(([normal, shifted]) => {
    const primary = isShift ? shifted : normal;
    const secondary = isShift ? normal : shifted;
    row1.push({
      primaryText: primary,
      secondaryText: secondary,
      action: { type: 'INSERT_TEXT', text: primary },
      weight: 1.0,
      testTag: `pc_key_${normal}`,
    });
  });
  row1.push({ primaryText: '⌫', action: { type: 'BACKSPACE' }, weight: 1.6, isFunctional: true, testTag: 'key_backspace' });

  // Row 2: Tab, Q, W, E, R, T, Y, U, I, O, P, [{, ]}, \|
  const row2Letters = ['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p'];
  const row2: KeyItem[] = [
    { primaryText: '↹ Tab', action: { type: 'TAB' }, weight: 1.45, isFunctional: true, testTag: 'pc_tab' },
  ];
  row2Letters.forEach(letter => {
    const char = isShift || isCaps ? letter.toUpperCase() : letter.toLowerCase();
    row2.push({
      primaryText: char,
      action: { type: 'INSERT_TEXT', text: char },
      weight: 1.0,
      testTag: `pc_key_${letter}`,
    });
  });
  const symPairsRow2 = [['[', '{'], [']', '}'], ['\\', '|']];
  symPairsRow2.forEach(([normal, shifted]) => {
    const primary = isShift ? shifted : normal;
    const secondary = isShift ? normal : shifted;
    row2.push({
      primaryText: primary,
      secondaryText: secondary,
      action: { type: 'INSERT_TEXT', text: primary },
      weight: normal === '\\' ? 1.25 : 1.0,
      testTag: `pc_key_${normal === '\\' ? 'backslash' : normal}`,
    });
  });

  // Row 3: Caps Lock, A, S, D, F, G, H, J, K, L, ;:, '", Enter
  const row3Letters = ['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'];
  const row3: KeyItem[] = [
    { primaryText: 'Caps', action: { type: 'TOGGLE_SHIFT' }, weight: 1.7, isFunctional: true, isActiveModifier: isCaps, testTag: 'pc_caps' },
  ];
  row3Letters.forEach(letter => {
    const char = isShift || isCaps ? letter.toUpperCase() : letter.toLowerCase();
    row3.push({
      primaryText: char,
      action: { type: 'INSERT_TEXT', text: char },
      weight: 1.0,
      testTag: `pc_key_${letter}`,
    });
  });
  const symPairsRow3 = [[';', ':'], ["'", '"']];
  symPairsRow3.forEach(([normal, shifted]) => {
    const primary = isShift ? shifted : normal;
    const secondary = isShift ? normal : shifted;
    row3.push({
      primaryText: primary,
      secondaryText: secondary,
      action: { type: 'INSERT_TEXT', text: primary },
      weight: 1.0,
      testTag: `pc_key_${normal === "'" ? 'quote' : 'semicolon'}`,
    });
  });
  row3.push({ primaryText: '↵ Enter', action: { type: 'ENTER' }, weight: 2.05, isAccent: true, testTag: 'key_enter' });

  // Row 4: Shift, Z, X, C, V, B, N, M, ,<, .>, /?, ↑, Shift
  const row4Letters = ['z', 'x', 'c', 'v', 'b', 'n', 'm'];
  const row4: KeyItem[] = [
    { primaryText: '⇧ Shift', action: { type: 'TOGGLE_SHIFT' }, weight: 2.1, isFunctional: true, isActiveModifier: isShift, testTag: 'pc_shift_left' },
  ];
  row4Letters.forEach(letter => {
    const char = isShift || isCaps ? letter.toUpperCase() : letter.toLowerCase();
    row4.push({
      primaryText: char,
      action: { type: 'INSERT_TEXT', text: char },
      weight: 1.0,
      testTag: `pc_key_${letter}`,
    });
  });
  const symPairsRow4 = [[',', '<'], ['.', '>'], ['/', '?']];
  symPairsRow4.forEach(([normal, shifted]) => {
    const primary = isShift ? shifted : normal;
    const secondary = isShift ? normal : shifted;
    row4.push({
      primaryText: primary,
      secondaryText: secondary,
      action: { type: 'INSERT_TEXT', text: primary },
      weight: 1.0,
      testTag: `pc_key_${normal === ',' ? 'comma' : normal === '.' ? 'period' : 'slash'}`,
    });
  });
  row4.push({ primaryText: '↑', action: { type: 'CURSOR_UP' }, weight: 1.15, isFunctional: true, testTag: 'pc_arrow_up' });
  row4.push({ primaryText: '⇧ Shift', action: { type: 'TOGGLE_SHIFT' }, weight: 1.7, isFunctional: true, isActiveModifier: isShift, testTag: 'pc_shift_right' });

  // Row 5: Ctrl, Fn, Windows key (toggles back to normal keyboard), Alt, Space, Alt, Ctrl, ←, ↓, →
  const row5: KeyItem[] = [
    { primaryText: 'Ctrl', action: { type: 'TOGGLE_CTRL' }, weight: 1.35, isFunctional: true, isActiveModifier: isCtrlActive, testTag: 'pc_ctrl_left' },
    { primaryText: 'Fn', action: { type: 'FUNCTION_KEY', fNumber: 1 }, weight: 1.1, isFunctional: true, testTag: 'pc_fn' },
    { primaryText: '❖', secondaryText: 'Win', action: { type: 'SWITCH_TO_LETTERS' }, weight: 1.1, isFunctional: true, testTag: 'pc_win' },
    { primaryText: 'Alt', action: { type: 'TOGGLE_ALT' }, weight: 1.25, isFunctional: true, isActiveModifier: isAltActive, testTag: 'pc_alt_left' },
    { primaryText: languageDisplayName, action: { type: 'SPACE' }, weight: 4.6, testTag: 'key_space' },
    { primaryText: 'Alt', action: { type: 'TOGGLE_ALT' }, weight: 1.25, isFunctional: true, isActiveModifier: isAltActive, testTag: 'pc_alt_right' },
    { primaryText: 'Ctrl', action: { type: 'TOGGLE_CTRL' }, weight: 1.25, isFunctional: true, isActiveModifier: isCtrlActive, testTag: 'pc_ctrl_right' },
    { primaryText: '←', action: { type: 'CURSOR_LEFT' }, weight: 1.15, isFunctional: true, testTag: 'pc_arrow_left' },
    { primaryText: '↓', action: { type: 'CURSOR_DOWN' }, weight: 1.15, isFunctional: true, testTag: 'pc_arrow_down' },
    { primaryText: '→', action: { type: 'CURSOR_RIGHT' }, weight: 1.15, isFunctional: true, testTag: 'pc_arrow_right' },
  ];

  return [row0, row1, row2, row3, row4, row5];
}

export function getExtendedPcRows(
  shiftState: ShiftState = 'OFF',
  isCtrlActive: boolean = false,
  isAltActive: boolean = false,
  languageDisplayName: string = 'English'
): KeyItem[][] {
  return getCompleteLaptopRows(shiftState, isCtrlActive, isAltActive, languageDisplayName);
}
