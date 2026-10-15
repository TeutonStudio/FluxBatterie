#!/usr/bin/env python3
"""Run the full regression on a real DedicatedServer, then stop through its console."""
from pathlib import Path
import os
import queue
import signal
import subprocess
import threading
import time

root = Path(__file__).resolve().parent.parent
output = root/'build/server-smoke.log'
process = subprocess.Popen([str(root/'gradlew'), 'runServer', '-PpackagedAddon', '-PdedicatedRegression', '--offline', '--no-daemon', '--max-workers=2'], cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, start_new_session=True)
lines = queue.Queue()
def read():
    for line in process.stdout: lines.put(line)
    lines.put(None)
threading.Thread(target=read, daemon=True).start()
started = False
end = time.monotonic()+180
with output.open('w') as log:
    try:
        while time.monotonic()<end:
            try: line=lines.get(timeout=1)
            except queue.Empty: continue
            if line is None: break
            log.write(line); log.flush()
            if 'Done (' in line and not started:
                started=True
                process.stdin.write('execute positioned 0 100 0 run test runall\n'); process.stdin.flush()
            if 'DEDICATED REGRESSION COMPLETE:' in line or 'DEDICATED REGRESSION FAILED:' in line:
                process.stdin.write('stop\n'); process.stdin.flush()
        else: raise TimeoutError('Dedicated-server smoke timeout')
        result=process.wait(timeout=30)
        text=output.read_text(errors='replace')
        assert started and result==0 and 'all mandatory original-8.0.0 runtime patches verified' in text and 'All dimensions are saved' in text and 'DEDICATED REGRESSION COMPLETE: 14 required tests passed' in text, f'Server smoke failed: {output}'
        print(f'Dedicated Server packaged regression: passed ({output})')
    finally:
        if process.poll() is None:
            os.killpg(process.pid,signal.SIGINT)
