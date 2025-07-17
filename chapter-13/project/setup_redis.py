from src.constants.constant import DATA_PATH
from src.models.trie import BasicTrie,TrieToHashConverter
from src.utils.utils import trim_word_text

import redis



if __name__ == '__main__':
    word_and_frequency = trim_word_text(DATA_PATH/'raw'/'count_1w.txt')
    basic_trie = BasicTrie()
    for word,frequency in word_and_frequency:
        basic_trie.insert(word,frequency)
    basic_trie.search('go')
    basic_trie.search('google')

    converter = TrieToHashConverter(basic_trie)
    trie_cache = converter.build_cache()


    r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

    for prefix, suggestions in trie_cache.items():
        zadd_data = {}
        for suggestion in suggestions:
            zadd_data[suggestion["word"]] = suggestion["frequency"]
        r.zadd(f"trie:{prefix}", zadd_data)


    def get_suggestions(prefix, limit=10):
        """Redis에서 직접 조회 - 진짜 O(1)"""
        return r.zrevrange(f"trie:{prefix}", 0, limit - 1)


    # 조회
    test_prefixes = ["go", "goo", "goog", "googl", "google"]
    for prefix in test_prefixes:
        suggestions = get_suggestions(prefix, 5)
        print(f"{prefix}: {suggestions}")

