#!/usr/bin/env python3
"""Check the published Wiki source, bilingual navigation and illustrated feature inventory."""
from pathlib import Path
from urllib.parse import unquote
import argparse
import hashlib
import json
import re
import struct

ROOT = Path(__file__).resolve().parents[1]
WIKI = ROOT / 'wiki'
PREFIX = 'https://raw.githubusercontent.com/wiki/TeutonStudio/FluxBatterie/'

def slug(text):
    text = re.sub(r'[`*\[\]]', '', text).lower().strip()
    return ''.join(c for c in text if c.isalnum() or c in ' -_').replace(' ', '-')

def verify(require_all):
    errors = []
    embedded = set()
    inventory = json.loads((ROOT / 'docs/wiki-media-coverage.json').read_text())
    pages = {p.stem: p.read_text() for p in WIKI.glob('*.md')}
    for name, text in pages.items():
        if text.count('```') % 2:
            errors.append(f'{name}: unclosed code fence')
        for target in re.findall(r'(?<!!)\[[^\]]+\]\(([^)]+)\)', text):
            if '://' in target or target.startswith('mailto:'):
                continue
            dest, _, anchor = unquote(target).partition('#')
            dest = dest.removesuffix('.md') or name
            if dest not in pages:
                errors.append(f'{name}: missing page {target}')
            elif anchor and anchor not in {slug(h) for h in re.findall(r'^#+ (.+)$', pages[dest], re.M)}:
                errors.append(f'{name}: missing heading {target}')
        for url in re.findall(r'!\[[^\]]*\]\(([^)]+)\)', text):
            if not url.startswith(PREFIX):
                errors.append(f'{name}: image outside the Wiki media tree: {url}')
                continue
            path = unquote(url[len(PREFIX):])
            embedded.add(path)
            file = WIKI / path
            if not file.is_file():
                errors.append(f'{name}: missing image {path}')
                continue
            header = file.read_bytes()[:24]
            if not header.startswith(b'\x89PNG\r\n\x1a\n') or min(struct.unpack('>II', header[16:24])) < 100:
                errors.append(f'{name}: expected a usable real PNG: {path}')
        if '/tmp/' in text or '/home/alex/' in text:
            errors.append(f'{name}: local path leaked into reader documentation')
    declared = set()
    for guide in inventory['guides']:
        de, en = guide['de'], guide['en']
        declared.update((de, en))
        if de not in pages or en not in pages:
            errors.append(f'{de}/{en}: missing language partner')
            continue
        if f'[English]({en})' not in pages[de] or f'[Deutsch]({de})' not in pages[en]:
            errors.append(f'{de}/{en}: missing reciprocal language links')
        for entry in ['Home', 'English', '_Sidebar']:
            expected = [de, en] if entry == '_Sidebar' else [de if entry == 'Home' else en]
            if any(f']({p})' not in pages[entry] for p in expected):
                errors.append(f'{entry}: missing guide navigation for {de}/{en}')
        for image in guide['media']:
            if PREFIX + image not in pages[de] or PREFIX + image not in pages[en]:
                errors.append(f'{de}/{en}: missing illustration {image}')
    if set(pages) != declared | {'Home', 'English', '_Sidebar', '_Footer'}:
        errors.append('Guide inventory does not match the actual page set')
    declared_media = {m['path'] for m in inventory['media']}
    if embedded != declared_media:
        errors.append(f'Media provenance mismatch: {embedded ^ declared_media}')
    for medium in inventory['media']:
        file = WIKI / medium['path']
        if file.exists() and hashlib.sha256(file.read_bytes()).hexdigest() != medium['sha256']:
            errors.append(f"Changed screenshot: {medium['path']}")
        if medium['kind'] not in {'in-game-capture', 'synthetic-client-ui'} or medium['modified']:
            errors.append(f"Unsupported or modified image: {medium['path']}")
    pending = []
    for feature in inventory['features']:
        if feature['status'] != 'shown':
            pending.append(feature['id'])
        if not feature['media'] or not feature['scope']:
            errors.append(f"Incomplete feature evidence: {feature['id']}")
        for image in feature['media']:
            if PREFIX + image not in pages.get(feature['page'], ''):
                errors.append(f"Unillustrated feature: {feature['id']}")
    if require_all and pending:
        errors.append('Incomplete feature media: ' + ', '.join(pending))
    readme = (ROOT / 'README.md').read_text()
    if len(readme.split()) > 130 or '/wiki/English' not in readme or '/wiki)' not in readme:
        errors.append('README must be short and link both Wiki languages')
    if inventory['release'] != '0.1.3-beta.2' or '0.1.3-beta.2' not in pages['Home']:
        errors.append('Release baseline mismatch')
    if errors:
        print('\n'.join('FAIL: ' + e for e in errors))
        return 1
    print(f"PASS: {len(pages)} pages, {len(inventory['guides'])} bilingual guide pairs, {len(embedded)} images with provenance.")
    print(f"PASS: {len(inventory['features']) - len(pending)}/{len(inventory['features'])} documented feature illustrations; limits remain explicit.")
    return 0

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--require-all-media', action='store_true')
    raise SystemExit(verify(parser.parse_args().require_all_media))
