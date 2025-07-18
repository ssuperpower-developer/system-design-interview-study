from pygtrie import CharTrie


class BasicTrie:
    def __init__(self):
        self.trie = CharTrie()

    def insert(self, word, frequency):
        self.trie[word] = frequency

    def search(self, word):
        return self.trie.get(word, 0)

    def starts_with(self, prefix):
        return list(self.trie.keys(prefix))


class TrieToHashConverter:
    def __init__(self, basic_trie):
        self.trie = basic_trie
        self.hash_cache = {}

    def build_cache(self, k=10):
        from heapq import nlargest

        prefix_candidates = {}

        for word in self.trie.trie.keys():
            frequency = self.trie.trie[word]

            for i in range(1, len(word) + 1):
                prefix = word[:i]
                if prefix not in prefix_candidates:
                    prefix_candidates[prefix] = []
                prefix_candidates[prefix].append((frequency, word))

        for prefix, candidates in prefix_candidates.items():
            top_k = nlargest(k, candidates, key=lambda x: x[0])
            self.hash_cache[prefix] = [
                {"word": word, "frequency": freq}
                for freq, word in top_k
            ]
        return self.hash_cache

    def get_suggestions(self, prefix, limit=10):
        return self.hash_cache.get(prefix, [])[:limit]
