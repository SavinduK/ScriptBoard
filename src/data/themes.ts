import { CustomThemeConfig, KeyboardColors, KeyboardThemeType } from '../types';

export const GboardDark: KeyboardColors = {
  background: '#1E1F24',
  toolbarBackground: '#191A1E',
  toolbarIconTint: '#C7C7CF',
  toolbarIconActiveTint: '#8AB4F8',
  letterKeyBackground: '#33353D',
  letterKeyTextColor: '#E8EAED',
  letterKeySecondaryTextColor: '#9AA0A6',
  functionKeyBackground: '#282A30',
  functionKeyTextColor: '#E8EAED',
  enterKeyBackground: '#8AB4F8',
  enterKeyTextColor: '#041E49',
  laptopRowBackground: '#151619',
  laptopKeyBackground: '#25272E',
  laptopKeyTextColor: '#D3E3FD',
  laptopKeyActiveBackground: '#8AB4F8',
  laptopKeyActiveTextColor: '#041E49',
  popupBackground: '#2D2F36',
  popupTextColor: '#FFFFFF',
  chipBackground: '#2C2E35',
  chipTextColor: '#E8EAED',
  isDark: true,
};

export const GboardLight: KeyboardColors = {
  background: '#ECEFF1',
  toolbarBackground: '#DFE3E6',
  toolbarIconTint: '#444746',
  toolbarIconActiveTint: '#0B57D0',
  letterKeyBackground: '#FFFFFF',
  letterKeyTextColor: '#1F1F1F',
  letterKeySecondaryTextColor: '#747775',
  functionKeyBackground: '#D6DBDF',
  functionKeyTextColor: '#1F1F1F',
  enterKeyBackground: '#0B57D0',
  enterKeyTextColor: '#FFFFFF',
  laptopRowBackground: '#DDE2E6',
  laptopKeyBackground: '#F1F3F5',
  laptopKeyTextColor: '#0B57D0',
  laptopKeyActiveBackground: '#0B57D0',
  laptopKeyActiveTextColor: '#FFFFFF',
  popupBackground: '#FFFFFF',
  popupTextColor: '#1F1F1F',
  chipBackground: '#FFFFFF',
  chipTextColor: '#1F1F1F',
  isDark: false,
};

export const CyberNavy: KeyboardColors = {
  background: '#0A111E',
  toolbarBackground: '#060B14',
  toolbarIconTint: '#80D8FF',
  toolbarIconActiveTint: '#00E5FF',
  letterKeyBackground: '#132238',
  letterKeyTextColor: '#E1F5FE',
  letterKeySecondaryTextColor: '#00B0FF',
  functionKeyBackground: '#0F1A2A',
  functionKeyTextColor: '#80D8FF',
  enterKeyBackground: '#00E5FF',
  enterKeyTextColor: '#04192B',
  laptopRowBackground: '#060D17',
  laptopKeyBackground: '#16253C',
  laptopKeyTextColor: '#00E5FF',
  laptopKeyActiveBackground: '#00E5FF',
  laptopKeyActiveTextColor: '#04192B',
  popupBackground: '#12243C',
  popupTextColor: '#E1F5FE',
  chipBackground: '#112239',
  chipTextColor: '#E1F5FE',
  isDark: true,
};

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  let clean = hex.replace('#', '');
  if (clean.length === 3) {
    clean = clean.split('').map(c => c + c).join('');
  }
  const num = parseInt(clean, 16);
  return {
    r: (num >> 16) & 255,
    g: (num >> 8) & 255,
    b: num & 255,
  };
}

export function buildCustomTheme(config: CustomThemeConfig): KeyboardColors {
  const bgRgb = hexToRgb(config.background);
  const luminance = (bgRgb.r * 0.299 + bgRgb.g * 0.587 + bgRgb.b * 0.114) / 255;
  const isDark = luminance < 0.5;

  const enterRgb = hexToRgb(config.enterKeyBackground);
  const enterLuminance = (enterRgb.r * 0.299 + enterRgb.g * 0.587 + enterRgb.b * 0.114) / 255;
  const enterTextColor = enterLuminance < 0.55 ? '#FFFFFF' : '#000000';

  return {
    background: config.background,
    toolbarBackground: isDark ? `${config.background}E6` : `${config.background}F0`,
    toolbarIconTint: isDark ? '#C7C7CF' : '#444746',
    toolbarIconActiveTint: config.enterKeyBackground,
    letterKeyBackground: config.letterKeyBackground,
    letterKeyTextColor: config.letterKeyTextColor,
    letterKeySecondaryTextColor: isDark ? '#9AA0A6' : '#747775',
    functionKeyBackground: isDark ? `${config.letterKeyBackground}CC` : `${config.letterKeyBackground}E0`,
    functionKeyTextColor: config.letterKeyTextColor,
    enterKeyBackground: config.enterKeyBackground,
    enterKeyTextColor: enterTextColor,
    laptopRowBackground: isDark ? `${config.background}B3` : `${config.background}D9`,
    laptopKeyBackground: config.letterKeyBackground,
    laptopKeyTextColor: config.enterKeyBackground,
    laptopKeyActiveBackground: config.enterKeyBackground,
    laptopKeyActiveTextColor: enterTextColor,
    popupBackground: isDark ? '#25262B' : '#FAFAFA',
    popupTextColor: isDark ? '#FFFFFF' : '#000000',
    chipBackground: config.letterKeyBackground,
    chipTextColor: config.letterKeyTextColor,
    isDark,
  };
}

export const DefaultCustomThemeConfig: CustomThemeConfig = {
  background: '#1B263B',
  letterKeyBackground: '#2E3D59',
  letterKeyTextColor: '#E0E1DD',
  enterKeyBackground: '#00B4D8',
};

export const DefaultCustomTheme = buildCustomTheme(DefaultCustomThemeConfig);

export function getTheme(
  type: KeyboardThemeType,
  customConfig?: CustomThemeConfig
): KeyboardColors {
  switch (type) {
    case 'GBOARD_DARK':
      return GboardDark;
    case 'GBOARD_LIGHT':
      return GboardLight;
    case 'CYBER_NAVY':
      return CyberNavy;
    case 'CUSTOM':
      return customConfig ? buildCustomTheme(customConfig) : DefaultCustomTheme;
    default:
      return GboardDark;
  }
}
