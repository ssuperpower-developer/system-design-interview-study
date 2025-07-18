import json
import pathlib

from ..constants.constant import DATA_PATH

def trim_word_text(file_path:pathlib.Path):
    with open(file_path,'r+') as fp:
        data = [line.split() for line in fp]
        word_freq = [(word, int(freq)) for word, freq in data]

        return word_freq


def serialize_trie(trie_cache):
    with open(DATA_PATH/'intermediate'/'trie.json', 'w', encoding='utf-8') as f:
        json.dump(trie_cache, f, ensure_ascii=False, indent=2)

from pathlib import Path


def parse_directory_structure(current_path: Path, structure: dict):
    if files := structure.pop('files', None):
        for file in files:
            (current_path / file).touch(exist_ok=True)

    for folder in structure.keys():
        (current_path / folder).mkdir(exist_ok=True)
        parse_directory_structure(current_path / folder, structure[folder])


PROJECT_STRUCTURE = {
    "src": {
        "constants": {
            "files": ["__init__.py"]
        },
        "utils": {
            "files": ["__init__.py"]
        },
        "models": {
            "files": ["__init__.py", "base_model.py"]
        },
        "files": ["__init__.py"]
    },
    "test": {},
    "data": {
        "raw": {},
        "intermediate": {},
        "result": {},
        "persistent": {}
    },
    "files": ["main.py"]
}

if __name__ == '__main__':
    parse_directory_structure(current_path=Path(),structure=PROJECT_STRUCTURE)