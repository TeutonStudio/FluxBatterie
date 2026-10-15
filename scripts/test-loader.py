#!/usr/bin/env python3
"""Real NeoForge subprocesses: compatible transformer mods and hard startup failures.
Synthetic fixtures deliberately exercise known phases; they are not third-party modpack coverage.
"""
from pathlib import Path
import json
import re
import subprocess
import zipfile

root = Path(__file__).resolve().parent.parent
fixtures = root / 'build/fixtures'
fixtures.mkdir(parents=True, exist_ok=True)

def coremod(name, target, method, desc):
    path = fixtures / f'{name}.jar'
    script = '''var O=Java.type('org.objectweb.asm.Opcodes');
var Insn=Java.type('org.objectweb.asm.tree.InsnNode');
var ASM=Java.type('net.neoforged.coremod.api.ASMAPI');
function initializeCoreMod(){return { fixture: { target: TARGET,
transformer: function(node){node.instructions.insert(new Insn(O.NOP)); ASM.log('INFO','FIXTURE applied: NAME');return node;}}};}'''
    script = script.replace('TARGET', json.dumps({'type':'METHOD','class':target,'methodName':method,'methodDesc':desc})).replace('NAME', name)
    with zipfile.ZipFile(path, 'w') as jar:
        jar.writestr('META-INF/neoforge.mods.toml', f'modLoader="lowcodefml"\nloaderVersion="[1,)"\nlicense="All Rights Reserved"\n[[mods]]\nmodId="{name}"\nversion="1.0"\ndisplayName="{name}"\n')
        jar.writestr('META-INF/coremods.json', json.dumps({name:'fixture.js'}))
        jar.writestr('fixture.js', script)
    return path

positive = coremod('fluxbattery_compatible_fixture', 'sonar.fluxnetworks.common.device.TileFluxDevice', 'getTransferChange', '()J')
negative = coremod('fluxbattery_conflict_fixture', 'sonar.fluxnetworks.common.connection.ServerFluxNetwork', 'onEndServerTick', '()V')

def run(label, args, success, required):
    output = root / f'build/loader-{label}.log'
    cmd = [str(root/'gradlew'), *args, '--offline', '--max-workers=2', '--no-daemon']
    with output.open('w') as log:
        result = subprocess.run(cmd, cwd=root, stdout=log, stderr=subprocess.STDOUT, timeout=300)
    text = output.read_text(errors='replace')
    if success:
        assert result.returncode == 0 and re.search(r'All \d+ required tests passed', text), f'{label} failed: {output}'
    else:
        # FML may report a successful Gradle task despite failing to load mods.
        assert 'all mandatory original-8.0.0 runtime patches verified' not in text, f'{label}: guard accepted unsupported input'
        assert 'Started game test server' not in text and 'Done (' not in text, f'{label}: server started unexpectedly'
    for marker in required:
        assert marker in text, f'{label}: missing {marker!r}; inspect {output}'
    print(f'{label}: passed ({output})', flush=True)

subprocess.run([str(root/'gradlew'), 'compatFixtureJar', '--offline', '--max-workers=2'], cwd=root, check=True)
run('compatible', ['runGameTestServer', f'-PcompatFixture={fixtures/"fluxbattery-mixin-fixture.jar"}', f'-PcompatFixtureExtra={positive}'], True, ['FIXTURE applied: fluxbattery_compatible_fixture', 'all mandatory original-8.0.0 runtime patches verified'])
run('conflict', ['runGameTestServer', f'-PcompatFixture={negative}'], False, ['FIXTURE applied: fluxbattery_conflict_fixture', 'Flux Battery mandatory runtime patch failed', 'changed bytecode in onEndServerTick()V'])
original = root/'libs/FluxNetworks-1.21.1-8.0.0.jar'
wrong_version = fixtures/'FluxNetworks-unsupported-8.0.1.jar'
changed = fixtures/'FluxNetworks-modified-8.0.0.jar'
for target, alter_version in ((wrong_version, True),(changed,False)):
    with zipfile.ZipFile(original) as src, zipfile.ZipFile(target,'w') as dst:
        for info in src.infolist():
            data=src.read(info.filename)
            if alter_version and info.filename == 'META-INF/neoforge.mods.toml':
                data=data.replace(b'8.0.0',b'8.0.1')
            dst.writestr(info,data)
        if not alter_version: dst.writestr('fixture-modified-input.txt','This JAR must be rejected by the runtime hash guard.')
run('unsupported-version',['runGameTestServer',f'-PfluxRuntimeJar={wrong_version}'],False,['8.0.1','8.0.0'])
run('modified-jar',['runGameTestServer',f'-PfluxRuntimeJar={changed}'],False,['Flux Battery requires the unchanged Flux Networks 8.0.0 JAR'])
print('All loader subprocess checks passed')
