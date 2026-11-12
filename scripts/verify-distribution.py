#!/usr/bin/env python3
"""Check the shipped addon, pristine Flux input and generated runtime classpaths."""
from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parent.parent
original = root / 'libs/FluxNetworks-1.21.1-8.0.0.jar'
assert hashlib.sha256(original.read_bytes()).hexdigest() == '29e511b2d76f85bd247aa213a92bb059ecc037fcc3fed82baffcff97b638d3a6'
version = next(line.split('=', 1)[1].strip() for line in (root / 'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
addon = root / f'build/libs/fluxbattery-{version}.jar'
assert list((root / 'build/libs').glob('*.jar')) == [addon], 'unexpected extra distribution JAR'
with zipfile.ZipFile(addon) as jar:
    entries = jar.namelist()
    for name in entries:
        if name.endswith('.json'):
            json.loads(jar.read(name))
    assert len(entries) == len(set(entries)), 'duplicate ZIP entries'
    assert not any(n.startswith('sonar/') for n in entries), 'upstream replacement class in addon'
    assert 'de/teutonstudio/fluxbattery/bridge/FluxRuntimeDevice.class' in entries
    assert 'LICENSE' in entries
    assert 'THIRD_PARTY_NOTICES.txt' in entries
    assert jar.read('LICENSE') == (root / 'LICENSE').read_bytes()
    assert jar.read('THIRD_PARTY_NOTICES.txt') == (root / 'THIRD_PARTY_NOTICES.txt').read_bytes()
    metadata = jar.read('META-INF/neoforge.mods.toml').decode()
    assert 'license="MIT"' in metadata
    assert f'version="{version}"' in metadata
    assert (root / 'bridge/LICENSE').read_text() in jar.read('THIRD_PARTY_NOTICES.txt').decode()
    assert 'META-INF/coremods.json' in entries
    assert 'coremods/fluxbattery.js' in entries
    assert 'coremods/fluxbattery-8.0.0.json' in entries
    assert not any(n.startswith(('de/teutonstudio/fluxbattery/tools/', 'de/teutonstudio/fluxbattery/compat/')) for n in entries)
    assert 'de/teutonstudio/fluxbattery/bridge/LoadTimePatched.class' not in entries
for path in (root / 'build/moddev').rglob('*'):
    if path.is_file() and path.suffix in ('.txt', '.args'):
        text = path.read_text(errors='replace')
        assert '8.0.0-fluxbattery.1.jar' not in text, f'stale bridge runtime dependency: {path}'
print('Distribution verified: single addon, no sonar classes, pristine Flux 8.0.0 input')
print('Addon SHA-256:', hashlib.sha256(addon.read_bytes()).hexdigest())
