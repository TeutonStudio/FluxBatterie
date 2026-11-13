#!/usr/bin/env python3
"""Exercise the packaged configuration UI in German and English; restore client options."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parent.parent
options = root / 'run-client/options.txt'
original = options.read_bytes() if options.exists() else None
base = original.decode() if original is not None else ''
try:
    for language, suffix in [('de_de', ''), ('en_us', '-en')]:
        current = base
        for key, value in [('lang', language), ('guiScale', '2'), ('overrideWidth', '0'), ('overrideHeight', '0'), ('fullscreen', 'false')]:
            current, count = re.subn(rf'(?m)^{key}:.*$', f'{key}:{value}', current)
            if not count:
                current += f'\n{key}:{value}\n'
        options.parent.mkdir(parents=True, exist_ok=True)
        options.write_text(current)
        log = root / f'build/client-ui-{language}.log'
        with log.open('w') as output:
            result = subprocess.run([str(root / 'gradlew'), 'build', 'runClient', '-PpackagedAddon', '-PclientSmoke',
                f'-PclientSmokeSuffix={suffix}', '--offline', '--no-daemon', '--max-workers=2'],
                cwd=root, stdout=output, stderr=subprocess.STDOUT, timeout=180)
        text = log.read_text(errors='replace')
        assert result.returncode == 0 and 'Flux Battery client UI checks passed:' in text, f'Client UI failed: {log}'
        print(f'Packaged client UI ({language}): passed ({log})', flush=True)
finally:
    if original is None:
        options.unlink(missing_ok=True)
    else:
        options.write_bytes(original)
