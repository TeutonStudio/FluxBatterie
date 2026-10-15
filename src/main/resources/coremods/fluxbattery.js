// Early bootstrap: only JDK/ASM/NeoForge classes may be resolved here.
var ASM = Java.type('net.neoforged.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
function fail(message) {
    // The constructor verifies the success marker; swallowed script errors cannot enable the mod.
    throw new Error('Flux Battery: incompatible Flux Networks; expected original 8.0.0: ' + message);
}
function initializeCoreMod() {
    return { fluxbattery_prototype: {
        target: { type: 'CLASS', name: 'sonar.fluxnetworks.common.connection.FluxNetwork' },
        transformer: function(node) {
            if (node.methods.stream().anyMatch(function(m) { return m.name === 'fluxBatteryPatchVersion'; })) fail('already patched FluxNetwork');
            node.interfaces.add('de/voxelteuton/fluxbattery/bridge/LoadTimePatched');
            var method = new MethodNode(Opcodes.ACC_PUBLIC, 'fluxBatteryPatchVersion', '()I', null, null);
            method.instructions.add(new InsnNode(Opcodes.ICONST_1));
            method.instructions.add(new InsnNode(Opcodes.IRETURN));
            node.methods.add(method);
            ASM.log('INFO', 'Flux Battery prototype: FluxNetwork extended before definition; no Kotlin classes resolved');
            return node;
        }
    }};
}
