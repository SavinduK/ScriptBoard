import { SnippetEntity, StickerEntity, WordFrequencyEntity } from '../types';

export const initialSnippets: SnippetEntity[] = [
  {
    id: 1,
    title: 'Work Email Address',
    content: 'bladeofvolantis@gmail.com',
    isPinned: true,
    category: 'Contact',
    shortcut: '@email',
    timestamp: Date.now() - 3600000 * 24 * 3,
  },
  {
    id: 2,
    title: 'Phone Number',
    content: '+1 (555) 019-2834',
    isPinned: true,
    category: 'Contact',
    shortcut: '@phone',
    timestamp: Date.now() - 3600000 * 24 * 2,
  },
  {
    id: 3,
    title: 'Office Address',
    content: '742 Evergreen Terrace, Sector 4, Tech Park',
    isPinned: true,
    category: 'Address',
    shortcut: 'addr',
    timestamp: Date.now() - 3600000 * 24,
  },
  {
    id: 4,
    title: 'Polite Thank You',
    content: 'Thank you very much for your prompt response! Really appreciate your assistance.',
    isPinned: true,
    category: 'Greetings',
    shortcut: '@thanks',
    timestamp: Date.now() - 3600000 * 12,
  },
  {
    id: 5,
    title: 'Meeting Link',
    content: 'https://meet.google.com/abc-defg-hij',
    isPinned: false,
    category: 'Links',
    shortcut: '',
    timestamp: Date.now() - 3600000 * 4,
  },
  {
    id: 6,
    title: 'Project Git Repository',
    content: 'git clone https://github.com/savindukahandagamage2/KeyPro.git',
    isPinned: false,
    category: 'Development',
    shortcut: '',
    timestamp: Date.now() - 3600000 * 2,
  },
  {
    id: 7,
    title: 'KeyPro Logo Badge',
    content: '/sample_stickers/magic_star.webp',
    isPinned: true,
    category: 'Images',
    shortcut: '',
    timestamp: Date.now() - 3600000,
    imageUri: '/sample_stickers/magic_star.webp',
    isImage: true,
    mimeType: 'image/webp',
  },
];

export const initialStickers: StickerEntity[] = [
  {
    id: 1,
    filePath: '/sample_stickers/happy_smiley.webp',
    name: 'Happy Smiley',
    source: 'gallery',
    dateAdded: Date.now() - 86400000 * 5,
    isFavorite: true,
    isAnimated: false,
    usageCount: 14,
  },
  {
    id: 2,
    filePath: '/sample_stickers/dancing_flame.webp',
    name: 'Dancing Flame',
    source: 'whatsapp',
    dateAdded: Date.now() - 86400000 * 4,
    isFavorite: false,
    isAnimated: true,
    usageCount: 8,
  },
  {
    id: 3,
    filePath: '/sample_stickers/magic_star.webp',
    name: 'Magic Star',
    source: 'gallery',
    dateAdded: Date.now() - 86400000 * 3,
    isFavorite: true,
    isAnimated: true,
    usageCount: 22,
  },
  {
    id: 4,
    filePath: '/sample_stickers/celebration_confetti.webp',
    name: 'Celebration Confetti',
    source: 'imported',
    dateAdded: Date.now() - 86400000 * 2,
    isFavorite: false,
    isAnimated: true,
    usageCount: 5,
  },
  {
    id: 5,
    filePath: '/sample_stickers/pulsing_heart.webp',
    name: 'Pulsing Heart',
    source: 'gallery',
    dateAdded: Date.now() - 86400000 * 1,
    isFavorite: true,
    isAnimated: true,
    usageCount: 19,
  },
  {
    id: 6,
    filePath: '/sample_stickers/happy_smiley.gif',
    name: 'Smiley GIF',
    source: 'gallery',
    dateAdded: Date.now() - 86400000,
    isFavorite: false,
    isAnimated: true,
    usageCount: 3,
  },
];

export const commonVocabulary = [
  'the', 'be', 'to', 'of', 'and', 'a', 'in', 'that', 'have', 'i', 'it', 'for', 'not',
  'on', 'with', 'he', 'as', 'you', 'do', 'at', 'this', 'but', 'his', 'by', 'from',
  'they', 'we', 'say', 'her', 'she', 'or', 'an', 'will', 'my', 'one', 'all', 'would',
  'there', 'their', 'what', 'so', 'up', 'out', 'if', 'about', 'who', 'get', 'which',
  'go', 'me', 'when', 'make', 'can', 'like', 'time', 'no', 'just', 'him', 'know',
  'take', 'people', 'into', 'year', 'your', 'good', 'some', 'could', 'them', 'see',
  'other', 'than', 'then', 'now', 'look', 'only', 'come', 'its', 'over', 'think',
  'also', 'back', 'after', 'use', 'two', 'how', 'our', 'work', 'first', 'well',
  'way', 'even', 'new', 'want', 'because', 'any', 'these', 'give', 'day', 'most',
  'us', 'great', 'here', 'please', 'thank', 'thanks', 'today', 'tomorrow', 'yesterday',
  'morning', 'evening', 'night', 'meet', 'meeting', 'call', 'message', 'email',
  'phone', 'address', 'where', 'why', 'let', 'check', 'send', 'received', 'nice',
  'happy', 'awesome', 'perfect', 'help', 'need', 'love', 'really', 'very', 'right',
  'sure', 'tell', 'much', 'before', 'while', 'should', 'going', 'been', 'being',
  'doing', 'having', 'working', 'trying', 'looking', 'welcome', 'sounds', 'good',
  'fine', 'talk', 'later', 'soon', 'ready', 'done', 'start', 'stop', 'again',
  'always', 'never', 'maybe', 'yes', 'yeah', 'okay', 'alright', 'sorry', 'excuse',
  'welcome', 'appreciate', 'regards', 'best', 'cheers', 'update', 'project', 'code',
  'keyboard', 'mobile', 'device', 'quick', 'fast', 'speed', 'test'
];

export const defaultStarterWords = ['I', 'The', 'Thanks', 'Hello', 'Yes', 'Please'];

export const sinhalaVocabulary = [
  'ආයුබෝවන්', 'ස්තූතියි', 'ඔව්', 'නැහැ', 'කොහොමද', 'සුභ', 'උදෑසනක්', 'දවසක්', 'හොඳයි',
  'මම', 'ඔබ', 'අපි', 'ඔවුන්', 'එන්න', 'යන්න', 'කරන්න', 'දන්නවා', 'ලස්සන', 'ශ්‍රී', 'ලංකා',
  'ගෙදර', 'පාසල', 'රස්සාව', 'පොත', 'කෑම', 'වතුර', 'මිතුරා', 'පවුල', 'ආදරය', 'සතුට',
  'කාරුණික', 'කරුණාකර', 'සමාවෙන්න', 'නැවත', 'හමුවෙමු', 'හොඳ', 'නරක', 'අද', 'හෙට',
  'ඊයේ', 'දැන්', 'පසුව', 'වේලාව', 'දිනය', 'සතිය', 'මාසය', 'අවුරුද්ද', 'වැඩ', 'ලිපිනය',
  'දුරකථනය', 'විද්‍යුත්', 'පණිවිඩය', 'ලියන්න', 'කියවන්න', 'බලන්න', 'අහන්න', 'කතා',
  'කරනවා', 'යනවා', 'එනවා', 'කනවා', 'බොනවා', 'නිදාගන්නවා', 'මොකක්ද', 'කොහෙද', 'කවදාද',
  'ඇයි', 'කවුද', 'කොපමණ', 'බොහොම', 'ඉතා', 'නියමයි', 'විශිෂ්ටයි', 'සාදරයෙන්', 'පිළිගන්නවා',
  'ප්‍රශ්නයක්', 'උත්තරය', 'උදව්වක්', 'අවශ්‍යයි', 'පුළුවන්', 'බැහැ', 'සත්‍යය', 'මිතුරන්',
  'කාර්යාලය', 'පරිගණකය', 'යතුරුපුවරුව', 'ජංගම', 'ලෝකය', 'රට', 'නගරය',
  'ගම', 'මඟ', 'පාර', 'වාහනය', 'බස්', 'දුම්රිය', 'ගුවන්', 'යානය', 'මුදල්', 'මිල'
];

export const sinhalaStarterWords = ['ආයුබෝවන්', 'ස්තූතියි', 'හොඳයි', 'සුභ දවසක්', 'කොහොමද'];

export function isSinhalaText(str: string): boolean {
  return /[\u0D80-\u0DFF]/.test(str);
}

export function applyCasing(typedPrefix: string, word: string): string {
  if (!typedPrefix) return word;
  if (typedPrefix.length >= 2 && typedPrefix === typedPrefix.toUpperCase()) {
    return word.toUpperCase();
  }
  if (typedPrefix[0] === typedPrefix[0].toUpperCase()) {
    return word.charAt(0).toUpperCase() + word.slice(1).toLowerCase();
  }
  return word.toLowerCase();
}
