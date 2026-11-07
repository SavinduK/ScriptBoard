# KeyPro Dictionaries

This directory contains bundled high-frequency wordlists used for predictive typing, word suggestions, scoring, and typo correction.

## Files

- `en_freq.txt`: High-frequency English dictionary (~82,000 words) formatted as `word<TAB>count` derived from SymSpell / FrequencyWords.
- `si_freq.txt`: Seed Sinhala word frequency dictionary (3,000+ words) formatted as `word<TAB>count`.

## Rebuilding the Sinhala Wordlist

The Sinhala word frequency list (`si_freq.txt`) can be rebuilt or updated using a dump of the Sinhala Wikipedia or a large Sinhala text corpus:

1. Download the latest Sinhala Wikipedia dump XML (`siwiki-latest-pages-articles.xml.bz2`) or corpus text files.
2. Extract text and tokenize into individual words:
   - Accept characters in the Sinhala Unicode range `U+0D80` to `U+0DFF`, plus Zero Width Joiner (ZWJ `U+200D`) and Zero Width Non-Joiner (ZWNJ `U+200C`) for conjunct letters (e.g. `ක්‍ර`, `රු`, `ඥ`).
   - Strip non-Sinhala tokens, numbers, punctuation, and URLs.
3. Count the frequency of each unique word.
4. Filter out rare tokens: drop words with count < 5.
5. Sort words by frequency in descending order.
6. Save the resulting list to `si_freq.txt` with format: `word<TAB>count`.

The `DictionaryRepository` and `WordSuggestionManager` automatically load and index any updated file placed in this folder.
