import { EmojiPack, EmojiPackItem } from '../types';

export interface EmojiCategory {
  title: string;
  icon: string;
  emojis: string[];
}

export const mathSymbolsCategory: EmojiCategory = {
  title: 'Math',
  icon: '∫',
  emojis: [
    '∫', '∬', '∭', '∮', '∯', '∰',
    '∂', '∇', '∆', 'dx', 'dy', 'dt', 'dz', 'lim', 'd/dx', '∑', '∏', '∐', '∞',
    '√', '∛', '∜', '²', '³', '⁴', 'ⁿ', '⁻¹', '½', '⅓', '¼', '¾', '⅛', '⅜', '⅝', '⅞',
    '+', '−', '±', '∓', '×', '÷', '·', '∘', '*', '/', '%',
    '=', '≠', '≈', '≡', '≢', '∼', '≅', '∝',
    '≤', '≥', '≪', '≫', '<', '>', '≮', '≯',
    '∈', '∉', '∋', '∌', '⊂', '⊃', '⊆', '⊇', '⊄', '⊅', '∪', '∩', '∖',
    '∅', '∀', '∃', '∄', '¬', '∧', '∨', '⊕', '⊗', '⇒', '⇔', '∴', '∵', '∎',
    '⊥', '∥', '∦', '∠', '∡', '∟', '°', '′', '″',
    'ℝ', 'ℕ', 'ℤ', 'ℚ', 'ℂ', 'ℙ'
  ],
};

export const greekLettersCategory: EmojiCategory = {
  title: 'Greek',
  icon: 'Ω',
  emojis: [
    'α', 'β', 'γ', 'δ', 'ε', 'ζ', 'η', 'θ', 'ι', 'κ',
    'λ', 'μ', 'ν', 'ξ', 'ο', 'π', 'ρ', 'σ', 'ς', 'τ',
    'υ', 'φ', 'χ', 'ψ', 'ω',
    'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ',
    'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ',
    'Φ', 'Χ', 'Ψ', 'Ω',
    'ϑ', 'ϕ', 'ϖ', 'ϰ', 'ϱ', 'ϵ'
  ],
};

export const standardCategories: EmojiCategory[] = [
  {
    title: 'Smileys',
    icon: '😀',
    emojis: [
      '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃',
      '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😙',
      '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔',
      '🫡', '🤐', '🤨', '😐', '😑', '😶', '🫥', '😏', '😒', '🙄',
      '😬', '🤥', '😌', '😔', '😪', '🤤', '😴', '😷', '🤒', '🤕',
      '🤢', '🤮', '🤧', '🥵', '🥶', '🥴', '😵', '🤯', '🤠', '🥳',
      '🥸', '😎', '🤓', '🧐', '😕', '🫤', '😟', '🙁', '☹️', '😮',
      '😯', '😲', '😳', '🥺', '🥹', '😦', '😧', '😨', '😰', '😥',
      '😢', '😭', '😱', '😖', '😣', '😞', '😓', '😩', '😫', '🥱',
      '😤', '😡', '😠', '🤬', '😈', '👿', '💀', '☠️', '💩', '🤡',
      '👹', '👺', '👻', '👽', '👾', '🤖', '😺', '😸', '😹', '😻'
    ],
  },
  {
    title: 'Gestures',
    icon: '👍',
    emojis: [
      '👍', '👎', '👌', '🤌', '🤏', '✌️', '🤞', '🫰', '🤟', '🤘',
      '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '🫵', '👋', '🤚',
      '🖐️', '✋', '🖖', '🫱', '🫲', '🫸', '🫷', '🫳', '🫴', '👏',
      '🙌', '🫶', '👐', '🤲', '🤝', '🙏', '✍️', '💅', '🤳', '💪',
      '🦾', '🦿', '🦵', '🦶', '👂', '🦻', '👃', '🧠', '🫀', '🫁',
      '👁️', '👀', '👅', '👄', '🫦', '👶', '🧒', '👦', '👧', '🧑'
    ],
  },
  {
    title: 'Animals',
    icon: '🐱',
    emojis: [
      '🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐻‍❄️', '🐨',
      '🐯', '🦁', '🐮', '🐷', '🐽', '🐸', '🐵', '🐒', '🐔', '🐧',
      '🐦', '🐤', '🐣', '🐥', '🦆', '🦅', '🦉', '🦇', '🐺', '🐗',
      '🐴', '🦄', '🐝', '🪱', '🐛', '🦋', '🐌', '🐞', '🐜', '🪰',
      '🐢', '🐍', '🦎', '🦖', '🦕', '🐙', '🦑', '🦐', '🦞', '🦀',
      '🐡', '🐠', '🐟', '🐬', '🐳', '🐋', '🐊', '🐅', '🐆', '🦓'
    ],
  },
  {
    title: 'Food',
    icon: '🍕',
    emojis: [
      '🍏', '🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🫐',
      '🍈', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🥑', '🥦',
      '🌽', '🥕', '🧄', '🧅', '🥔', '🍠', '🥐', '🥯', '🍞', '🥖',
      '🥨', '🧀', '🥚', '🍳', '🧈', '🥞', '🧇', '🥓', '🥩', '🍗',
      '🍖', '🦴', '🌭', '🍔', '🍟', '🍕', '🫓', '🥪', '🥙', '🌮',
      '🌯', '🥗', '🍝', '🍜', '🍲', '🍛', '🍣', '🍱', '🥟', '🍤'
    ],
  },
  {
    title: 'Activities',
    icon: '⚽',
    emojis: [
      '⚽', '🏀', '🏈', '⚾', '🥎', '🎾', '🏐', '🏉', '🥏', '🎱',
      '🪀', '🏓', '🏸', '🏒', '🏑', '🥍', '🏏', '🪃', '🥅', '⛳',
      '🪁', '🏹', '🎣', '🤿', '🥊', '🥋', '🎽', '🛹', '🛼', '🛷',
      '⛸️', '🎿', '🏂', '🏋️', '🤼', '🤸', '🧗', '🏄', '🏊', '🚴',
      '🏆', '🥇', '🥈', '🥉', '🏅', '🎖️', '🎮', '🎲', '♟️', '🎯'
    ],
  },
  {
    title: 'Travel',
    icon: '🚗',
    emojis: [
      '🚗', '🚕', '🚙', '🚌', '🚎', '🏎️', '🚓', '🚑', '🚒', '🚐',
      '🛻', '🚚', '🚛', '🚜', '🛴', '🚲', '🛵', '🏍️', '🛺', '🚨',
      '🚆', '🚇', '🛫', '🛬', '✈️', '🚀', '🛸', '🚁', '🛶', '⛵',
      '🚤', '🛳️', '🚢', '⚓', '⛽', '🚦', '🛑', '🗺️', '🗽', '🗼'
    ],
  },
  {
    title: 'Objects',
    icon: '💻',
    emojis: [
      '💻', '🖥️', '⌨️', '🖱️', '📱', '☎️', '📞', '📺', '📻', '🎙️',
      '⏱️', '⏲️', '⏰', '🕰️', '💡', '🔦', '🕯️', '🔋', '🔌', '💰',
      '🪙', '💵', '💳', '💎', '⚖️', '🔧', '🔨', '📦', '📫', '📜',
      '📊', '📈', '📉', '📁', '📂', '📅', '📋', '📌', '📎', '✂️',
      '🔒', '🔓', '🔑', '🔬', '🔭', '📡', '💉', '💊', '🚪', '🛏️'
    ],
  },
  {
    title: 'Symbols',
    icon: '❤️',
    emojis: [
      '❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔',
      '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '❤️‍🔥', '❤️‍🩹',
      '💯', '💢', '💬', '👁️‍🗨️', '🗯️', '💭', '💤', '🛑', '⛔', '⚠️',
      '✅', '❎', '🌐', '🌀', '🏧', '♿', '🅿️', '🔤', '🔢', '➡️',
      '⬅️', '⬆️', '⬇️', '↗️', '↘️', '↙️', '↖️', '↕️', '↔️', '🔄',
      '▶️', '⏸️', '⏹️', '🔔', '🔕', '📣', '📢', '♠️', '♣️', '♥️'
    ],
  },
  {
    title: 'Flags',
    icon: '🏁',
    emojis: [
      '🏁', '🚩', '🎌', '🏴', '🏳️', '🏳️‍🌈', '🏴‍☠️',
      '🇺🇸', '🇬🇧', '🇨🇦', '🇦🇺', '🇯🇵', '🇩🇪', '🇫🇷', '🇮🇹', '🇪🇸', '🇧🇷',
      '🇮🇳', '🇱🇰', '🇨🇳', '🇰🇷', '🇲🇽', '🇷🇺', '🇿🇦', '🇳🇿', '🇳🇱', '🇸🇪'
    ],
  },
];

export const allEmojiCategories: EmojiCategory[] = [
  ...standardCategories,
  mathSymbolsCategory,
  greekLettersCategory,
];

export const EmojiPackRegistry: { ALL_PACKS: EmojiPack[] } = {
  ALL_PACKS: [
    {
      id: 'runic',
      name: 'Runic Symbols',
      nativeName: 'ᚱᚢᚾᛖᛋ',
      icon: 'ᚱ',
      version: '1.0.0',
      description: 'Elder Futhark and Anglo-Saxon magical runic alphabet symbols with glyph meanings',
      file: 'runic.json',
      sizeBytes: 18200,
      sizeDisplay: '18 KB',
      itemCount: 33,
      category: 'Ancient Scripts',
    },
    {
      id: 'hieroglyphs',
      name: 'Egyptian Hieroglyphs',
      nativeName: '𓀀 𓌃𓂧𓅱𓀁',
      icon: '𓀀',
      version: '1.0.0',
      description: "Ancient Egyptian Hieroglyphs Unicode block (Gardiner's sign list animals, gods, vessels)",
      file: 'hieroglyphs.json',
      sizeBytes: 28400,
      sizeDisplay: '28 KB',
      itemCount: 60,
      category: 'Ancient Scripts',
    },
    {
      id: 'kaomoji',
      name: 'Japanese Kaomoji',
      nativeName: '顔文字',
      icon: '(^‿^)',
      version: '1.0.0',
      description: 'Expressive Japanese text emoticons for cute reactions, table flips and hugs',
      file: 'kaomoji.json',
      sizeBytes: 16500,
      sizeDisplay: '16 KB',
      itemCount: 42,
      category: 'Emoticons',
    },
    {
      id: 'alchemy',
      name: 'Alchemical & Astrological',
      nativeName: '🜁 ☉ ☽',
      icon: '🜁',
      version: '1.0.0',
      description: 'Medieval alchemy elements, astronomical planets and zodiac symbols',
      file: 'alchemy.json',
      sizeBytes: 14200,
      sizeDisplay: '14 KB',
      itemCount: 36,
      category: 'Esoteric Symbols',
    },
  ],
};

// Built-in pack items fallback data so they load offline without network requirement
export const BuiltInPackItems: Record<string, EmojiPackItem[]> = {
  runic: [
    { symbol: 'ᚠ', name: 'Fehu', meaning: 'Wealth, cattle, mobile property' },
    { symbol: 'ᚢ', name: 'Uruz', meaning: 'Wild ox, untamed strength' },
    { symbol: 'ᚦ', name: 'Thurisaz', meaning: 'Giant, thorn, reactive force' },
    { symbol: 'ᚨ', name: 'Ansuz', meaning: 'Odin, divine breath, speech' },
    { symbol: 'ᚱ', name: 'Raidho', meaning: 'Ride, journey, rhythm' },
    { symbol: 'ᚲ', name: 'Kenaz', meaning: 'Torch, beacon, revelation' },
    { symbol: 'ᚷ', name: 'Gebo', meaning: 'Gift, exchange, partnership' },
    { symbol: 'ᚹ', name: 'Wunjo', meaning: 'Joy, harmony, fellowship' },
    { symbol: 'ᚺ', name: 'Hagalaz', meaning: 'Hailstone, sudden crisis' },
    { symbol: 'ᚾ', name: 'Nauthiz', meaning: 'Need, necessity, friction' },
    { symbol: 'ᛁ', name: 'Isa', meaning: 'Ice, stillness, crystallization' },
    { symbol: 'ᛃ', name: 'Jera', meaning: 'Year, harvest, cycles' },
    { symbol: 'ᛇ', name: 'Eihwaz', meaning: 'Yew tree, axis mundi, endurance' },
    { symbol: 'ᛈ', name: 'Perthro', meaning: 'Fate, dice cup, mystery' },
    { symbol: 'ᛉ', name: 'Algiz', meaning: 'Elk, protection, higher self' },
    { symbol: 'ᛋ', name: 'Sowilo', meaning: 'Sun, lightning, victory' },
    { symbol: 'ᛏ', name: 'Tiwaz', meaning: 'Tyr, justice, honor, victory' },
    { symbol: 'ᛒ', name: 'Berkano', meaning: 'Birch goddess, rebirth, sanctuary' },
    { symbol: 'ᛖ', name: 'Ehwaz', meaning: 'Horse, trust, swift momentum' },
    { symbol: 'ᛗ', name: 'Mannaz', meaning: 'Humankind, social order, intellect' },
    { symbol: 'ᛚ', name: 'Laguz', meaning: 'Water, intuition, tides' },
    { symbol: 'ᛝ', name: 'Ingwaz', meaning: 'Seed, hearth fire, fertile potential' },
    { symbol: 'ᛟ', name: 'Othala', meaning: 'Heritage, ancestral land' },
    { symbol: 'ᛞ', name: 'Dagaz', meaning: 'Day, dawn, awakening' },
  ],
  hieroglyphs: [
    { symbol: '𓀀', name: 'Man seated', meaning: 'Person, self, soul' },
    { symbol: '𓀁', name: 'Man praising', meaning: 'Worship, praise' },
    { symbol: '𓀂', name: 'Man bowing', meaning: 'Respect, deference' },
    { symbol: '𓀃', name: 'Man jubilating', meaning: 'Celebration, feast' },
    { symbol: '𓀇', name: 'Soldier', meaning: 'Guardian, army' },
    { symbol: '𓀎', name: 'Scribe', meaning: 'Wisdom, writing' },
    { symbol: '𓀟', name: 'Pharaoh', meaning: 'King, supreme ruler' },
    { symbol: '𓁐', name: 'Goddess Isis', meaning: 'Divine protection' },
    { symbol: '𓁹', name: 'Eye of Horus', meaning: 'Wadjet, healing, wholeness' },
    { symbol: '𓂉', name: 'Crown of Osiris', meaning: 'Eternity' },
    { symbol: '𓂊', name: 'White Crown', meaning: 'Upper Egypt' },
    { symbol: '𓃀', name: 'Foot / Leg', meaning: 'Place, step' },
    { symbol: '𓃠', name: 'Cat', meaning: 'Bastet, agility' },
    { symbol: '𓃰', name: 'Elephant', meaning: 'Power, majesty' },
    { symbol: '𓄿', name: 'Vulture', meaning: 'Aleph, maternal care' },
    { symbol: '𓅓', name: 'Owl', meaning: 'Letter M, twilight watch' },
    { symbol: '𓅛', name: 'Ibis', meaning: 'Thoth, wisdom, mathematics' },
    { symbol: '𓅱', name: 'Quail chick', meaning: 'Sound W, vitality' },
    { symbol: '𓆓', name: 'Cobra', meaning: 'Uraeus, sovereignty' },
    { symbol: '𓆗', name: 'Horned Viper', meaning: 'Letter F' },
    { symbol: '𓆤', name: 'Bee', meaning: 'Lower Egypt, royalty' },
    { symbol: '𓆟', name: 'Nile Fish', meaning: 'Bounty' },
    { symbol: '𓇳', name: 'Ra Sun disk', meaning: 'Light, divinity, day' },
    { symbol: '𓇽', name: 'Lotus flower', meaning: 'Creation, rebirth' },
    { symbol: '𓈖', name: 'Water waves', meaning: 'Letter N, river of life' },
    { symbol: '𓉐', name: 'House / Temple', meaning: 'Per, domain' },
    { symbol: '𓊖', name: 'Town with crossroad', meaning: 'City, sanctuary' },
    { symbol: '𓋹', name: 'Ankh', meaning: 'Key of life, eternity' },
    { symbol: '𓌃', name: 'Medew', meaning: 'Divine speech' },
    { symbol: '𓍯', name: 'Lasso / Knot', meaning: 'Unity, binding' },
  ],
  kaomoji: [
    { symbol: '(^‿^)', name: 'Happy', meaning: 'Gentle smile' },
    { symbol: '(づ｡◕‿‿◕｡)づ', name: 'Hug', meaning: 'Warm squeeze' },
    { symbol: '(╯°□°)╯︵ ┻━┻', name: 'Table Flip', meaning: 'Total rage' },
    { symbol: '┬─┬ノ( º _ ºノ)', name: 'Put Back', meaning: 'Calm restoration' },
    { symbol: '¯\\_(ツ)_/¯', name: 'Shrug', meaning: 'Who knows?' },
    { symbol: '(◕‿◕✿)', name: 'Flower Joy', meaning: 'Blossoming day' },
    { symbol: '(¬‿¬)', name: 'Smirk', meaning: 'Knowing look' },
    { symbol: '(ಥ﹏ಥ)', name: 'Crying', meaning: 'Tears of joy/pain' },
    { symbol: '(ง\'̀-\'́)ง', name: 'Fight', meaning: 'Ready to brawl' },
    { symbol: '(｡♥‿♥｡)', name: 'Love', meaning: 'Heart eyes' },
    { symbol: '(=^･ω･^=)', name: 'Cat Smile', meaning: 'Playful kitty' },
    { symbol: '(•‿•)', name: 'Simple Joy', meaning: 'Peace of mind' },
    { symbol: '(★‿★)', name: 'Starstruck', meaning: 'Awe and wonder' },
    { symbol: '(¬_¬)', name: 'Suspicious', meaning: 'Doubtful glance' },
    { symbol: 'ʕ•ᴥ•ʔ', name: 'Teddy Bear', meaning: 'Cute creature' },
    { symbol: '(☞ﾟヮﾟ)☞', name: 'Point', meaning: 'You got it!' },
    { symbol: '＼(＾O＾)／', name: 'Cheering', meaning: 'Hooray!' },
    { symbol: '(>_<)', name: 'Frustrated', meaning: 'Ouch/annoyed' },
  ],
  alchemy: [
    { symbol: '🜁', name: 'Air', meaning: 'Hot and wet element' },
    { symbol: '🜂', name: 'Fire', meaning: 'Hot and dry element' },
    { symbol: '🜃', name: 'Earth', meaning: 'Cold and dry element' },
    { symbol: '🜄', name: 'Water', meaning: 'Cold and wet element' },
    { symbol: '☉', name: 'Sol / Gold', meaning: 'The royal sun metal' },
    { symbol: '☽', name: 'Luna / Silver', meaning: 'The intuitive moon metal' },
    { symbol: '☿', name: 'Mercury / Quicksilver', meaning: 'The fluid mind metal' },
    { symbol: '♀', name: 'Venus / Copper', meaning: 'Beauty, harmony' },
    { symbol: '♂', name: 'Mars / Iron', meaning: 'Force, raw strength' },
    { symbol: '♃', name: 'Jupiter / Tin', meaning: 'Expansion, benevolence' },
    { symbol: '♄', name: 'Saturn / Lead', meaning: 'Time, limitation, weight' },
    { symbol: '🜍', name: 'Sulfur', meaning: 'Soul, fiery active principle' },
    { symbol: '🜔', name: 'Salt', meaning: 'Body, physical base' },
    { symbol: '🜖', name: 'Vitriol', meaning: 'Green lion, hidden stone' },
    { symbol: '♈', name: 'Aries', meaning: 'Celestial Ram' },
    { symbol: '♉', name: 'Taurus', meaning: 'Celestial Bull' },
    { symbol: '♊', name: 'Gemini', meaning: 'The Twins' },
    { symbol: '♋', name: 'Cancer', meaning: 'The Crab' },
    { symbol: '♌', name: 'Leo', meaning: 'The Lion' },
    { symbol: '♍', name: 'Virgo', meaning: 'The Maiden' },
    { symbol: '♎', name: 'Libra', meaning: 'The Scales' },
    { symbol: '♏', name: 'Scorpio', meaning: 'The Scorpion' },
    { symbol: '♐', name: 'Sagittarius', meaning: 'The Archer' },
    { symbol: '♑', name: 'Capricorn', meaning: 'The Sea-Goat' },
    { symbol: '♒', name: 'Aquarius', meaning: 'The Water Bearer' },
    { symbol: '♓', name: 'Pisces', meaning: 'The Fishes' },
  ],
};
