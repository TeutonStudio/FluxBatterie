// NeoForge 21.1.231 / CoreMods 7.0.3. No addon/Kotlin classes are loaded here.
var ASM = Java.type('net.neoforged.coremod.api.ASMAPI');
var O = Java.type('org.objectweb.asm.Opcodes');
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');
var FieldNode = Java.type('org.objectweb.asm.tree.FieldNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var LdcInsnNode = Java.type('org.objectweb.asm.tree.LdcInsnNode');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var Handle = Java.type('org.objectweb.asm.Handle');
var Type = Java.type('org.objectweb.asm.Type');
var TILE = 'sonar/fluxnetworks/common/device/TileFluxDevice';
var ROOT = 'sonar/fluxnetworks/common/connection/';
var ABI = 'de/teutonstudio/fluxbattery/bridge/';
var RUNTIME = ABI + 'FluxRuntimeDevice';
var NETWORK = ROOT + 'FluxNetwork';
var SERVER = ROOT + 'ServerFluxNetwork';
var expected;
function fail(message) { throw new Error('Flux Battery: expected original Flux Networks 8.0.0; ' + message); }
function mapped(value) { return value === null ? null : String(value).split(TILE).join(RUNTIME); }
function method(node, name, desc) {
    for each (var m in node.methods) if (m.name === name && m.desc === desc) return m;
    fail(node.name + ': missing method ' + name + desc);
}
// Executable opcode/member/constant/branch shape, independent of frames and debug info.
function shape(m) {
    var a = m.instructions.toArray(), labels = {}, position = 0, out = ['access:' + m.access];
    for (var i = 0; i < a.length; i++) {
        if (a[i].getType() === 8) labels[a[i].getLabel().toString()] = position;
        if (a[i].getOpcode() >= 0) position++;
    }
    for (var i = 0; i < a.length; i++) {
        var n = a[i], op = n.getOpcode();
        if (op < 0) continue;
        var s = '' + op;
        switch(n.getType()) {
            case 1: s += ':' + n.operand; break;
            case 2: s += ':' + n['var']; break;
            case 3: s += ':' + n.desc; break;
            case 4: s += ':' + n.owner + ':' + n.name + ':' + n.desc; break;
            case 5: s += ':' + n.owner + ':' + n.name + ':' + n.desc + ':' + n.itf; break;
            case 6:
                s += ':' + n.name + ':' + n.desc + ':' + n.bsm.toString();
                for each (var arg in n.bsmArgs) s += ':' + arg.toString();
                break;
            case 7: s += ':' + labels[n.label.getLabel().toString()]; break;
            case 9: s += ':' + n.cst.toString(); break;
            case 10: s += ':' + n['var'] + ':' + n.incr; break;
            case 11:
                s += ':' + n.min + ':' + n.max + ':' + labels[n.dflt.getLabel().toString()];
                for each (var l in n.labels) s += ':' + labels[l.getLabel().toString()];
                break;
            case 12:
                s += ':' + n.keys.toString() + ':' + labels[n.dflt.getLabel().toString()];
                for each (var l in n.labels) s += ':' + labels[l.getLabel().toString()];
                break;
            case 13: s += ':' + n.desc + ':' + n.dims; break;
        }
        out.push(s);
    }
    for each (var block in m.tryCatchBlocks) out.push('try:' + labels[block.start.getLabel().toString()] + ':' + labels[block.end.getLabel().toString()] + ':' + labels[block.handler.getLabel().toString()] + ':' + block.type);
    return out.join('|');
}
function validate(node) {
    var spec = expected[node.name];
    if (!spec) fail('unknown target ' + node.name);
    if (node.superName !== spec.superName) fail(node.name + ': changed superclass ' + node.superName);
    for (var key in spec.methods) {
        var split = key.indexOf('('), m = method(node, key.substring(0,split), key.substring(split));
        if (shape(m) !== spec.methods[key]) fail(node.name + ': changed bytecode in ' + key);
    }
    for each (var field in spec.fields) {
        var found = false;
        for each (var f in node.fields) if (f.name + ':' + f.desc + ':' + f.access === field) found = true;
        if (!found) fail(node.name + ': missing field ' + field);
    }
    for each (var m in node.methods) if (m.name === 'fluxBatteryPatchVersion') fail(node.name + ': already patched');
}
function call(owner, name, desc, iface) { return new MethodInsnNode(iface ? O.INVOKEINTERFACE : O.INVOKEVIRTUAL, owner, name, desc, iface); }
function hook(name, desc) { return new MethodInsnNode(O.INVOKESTATIC, ABI + 'RuntimeHooks', name, desc, false); }
function freshBody(m) { m.instructions.clear(); m.tryCatchBlocks.clear(); if (m.localVariables !== null) m.localVariables.clear(); }
function marker(node) {
    var m = new MethodNode(O.ACC_PUBLIC | O.ACC_STATIC, 'fluxBatteryPatchVersion', '()I', null, null);
    m.instructions.add(new InsnNode(O.ICONST_1)); m.instructions.add(new InsnNode(O.IRETURN)); node.methods.add(m);
}
function remapHandle(h) {
    return new Handle(h.getOwner() === TILE ? O.H_INVOKEINTERFACE : h.getTag(), mapped(h.getOwner()), h.getName(), mapped(h.getDesc()), h.getOwner() === TILE || h.isInterface());
}
function remap(node) {
    for each (var f in node.fields) { f.desc = mapped(f.desc); f.signature = mapped(f.signature); }
    for each (var m in node.methods) {
        m.desc = mapped(m.desc); m.signature = mapped(m.signature);
        if (m.localVariables !== null) for each (var v in m.localVariables) { v.desc = mapped(v.desc); v.signature = mapped(v.signature); }
        for each (var n in m.instructions.toArray()) {
            if (n.getType() === 3) n.desc = mapped(n.desc);
            if (n.getType() === 4 || n.getType() === 5) {
                if (n.getType() === 5 && n.owner === TILE) { n.setOpcode(O.INVOKEINTERFACE); n.itf = true; }
                n.owner = mapped(n.owner); n.desc = mapped(n.desc);
            }
            if (n.getType() === 6) {
                n.desc = mapped(n.desc); n.bsm = remapHandle(n.bsm);
                for (var i=0; i<n.bsmArgs.length; i++) {
                    var arg = n.bsmArgs[i];
                    if (arg instanceof Handle) n.bsmArgs[i] = remapHandle(arg);
                    else if (arg instanceof Type) n.bsmArgs[i] = Type.getType(mapped(arg.getDescriptor()));
                }
            }
        }
    }
}
function tileOverload(node, name, result, removal, statik) {
    var desc = '(L' + TILE + ';' + (removal ? 'Z' : '') + ')' + result;
    var m = new MethodNode(O.ACC_PUBLIC | (statik ? O.ACC_STATIC : 0), name, desc, null, null);
    if (!statik) m.instructions.add(new VarInsnNode(O.ALOAD, 0));
    m.instructions.add(new VarInsnNode(O.ALOAD, statik ? 0 : 1));
    m.instructions.add(new TypeInsnNode(O.CHECKCAST, RUNTIME));
    if (removal) m.instructions.add(new VarInsnNode(O.ILOAD, 2));
    m.instructions.add(new MethodInsnNode(statik ? O.INVOKESTATIC : O.INVOKEVIRTUAL, node.name, name, mapped(desc), false));
    m.instructions.add(new InsnNode(result === 'V' ? O.RETURN : result === 'Z' ? O.IRETURN : O.ARETURN));
    node.methods.add(m);
}
function accessor(node, name, fieldOwner, field, desc) {
    var m = new MethodNode(O.ACC_PUBLIC, name, '()' + desc, null, null);
    m.instructions.add(new VarInsnNode(O.ALOAD,0)); m.instructions.add(new FieldInsnNode(O.GETFIELD,fieldOwner,field,desc));
    m.instructions.add(new InsnNode(desc === 'Z' ? O.IRETURN : O.ARETURN)); node.methods.add(m);
}
function patchServer(node) {
    node.interfaces.add(ABI + 'ServerRuntimeAccess');
    node.fields.add(new FieldNode(O.ACC_PRIVATE, 'fluxBatteryDeleted', 'Z', null, null));
    accessor(node,'fluxBatteryPending',SERVER,'mToAdd','Ljava/util/LinkedList;');
    accessor(node,'fluxBatteryRemovals',SERVER,'mToRemove','Ljava/util/LinkedList;');
    // Return interface descriptor while reading the original concrete HashMap field.
    accessor(node,'fluxBatteryConnections',NETWORK,'mConnectionMap','Ljava/util/HashMap;');
    method(node,'fluxBatteryConnections','()Ljava/util/HashMap;').desc = '()Ljava/util/Map;';
    accessor(node,'fluxBatteryDeleted',SERVER,'fluxBatteryDeleted','Z');
    var setter = new MethodNode(O.ACC_PUBLIC,'fluxBatterySetDeleted','(Z)V',null,null);
    setter.instructions.add(new VarInsnNode(O.ALOAD,0)); setter.instructions.add(new VarInsnNode(O.ILOAD,1));
    setter.instructions.add(new FieldInsnNode(O.PUTFIELD,SERVER,'fluxBatteryDeleted','Z')); setter.instructions.add(new InsnNode(O.RETURN)); node.methods.add(setter);
    var valid = method(node,'isValid','()Z'); freshBody(valid);
    valid.instructions.add(new VarInsnNode(O.ALOAD,0)); valid.instructions.add(new FieldInsnNode(O.GETFIELD,SERVER,'fluxBatteryDeleted','Z'));
    valid.instructions.add(new InsnNode(O.ICONST_1)); valid.instructions.add(new InsnNode(O.IXOR)); valid.instructions.add(new InsnNode(O.IRETURN));
    var del = method(node,'onDelete','()V'); freshBody(del);
    del.instructions.add(new VarInsnNode(O.ALOAD,0)); del.instructions.add(new MethodInsnNode(O.INVOKESPECIAL,NETWORK,'onDelete','()V',false));
    del.instructions.add(new VarInsnNode(O.ALOAD,0)); del.instructions.add(hook('delete','(L'+NETWORK+';)V')); del.instructions.add(new InsnNode(O.RETURN));
    var addition = method(node,'enqueueConnectionAddition','(L'+RUNTIME+';)Z');
    var guard = new InsnList(), cont = new LabelNode();
    guard.add(new VarInsnNode(O.ALOAD,0)); guard.add(new VarInsnNode(O.ALOAD,1)); guard.add(hook('canAdd','(L'+NETWORK+';L'+RUNTIME+';)Z'));
    guard.add(new JumpInsnNode(O.IFNE,cont)); guard.add(new InsnNode(O.ICONST_0)); guard.add(new InsnNode(O.IRETURN)); guard.add(cont); addition.instructions.insert(guard);
    var removal = method(node,'enqueueConnectionRemoval','(L'+RUNTIME+';Z)V');
    guard = new InsnList(); cont = new LabelNode();
    guard.add(new VarInsnNode(O.ALOAD,0)); guard.add(new VarInsnNode(O.ALOAD,1)); guard.add(hook('cancelPending','(L'+NETWORK+';L'+RUNTIME+';)Z'));
    guard.add(new JumpInsnNode(O.IFEQ,cont)); guard.add(new InsnNode(O.RETURN)); guard.add(cont); removal.instructions.insert(guard);
    var identityRemovals = 0;
    for each (var n in removal.instructions.toArray()) if (n.getType() === 5 && n.owner === 'java/util/HashMap' && n.name === 'remove') {
        removal.instructions.insertBefore(n,new VarInsnNode(O.ALOAD,1)); n.desc = '(Ljava/lang/Object;Ljava/lang/Object;)Z'; identityRemovals++;
    }
    if (identityRemovals !== 1) fail('ServerFluxNetwork removal anchor count ' + identityRemovals);
    var sums = 0;
    for each (var n in method(node,'onEndServerTick','()V').instructions.toArray()) if (n.getOpcode() === O.LADD) {
        method(node,'onEndServerTick','()V').instructions.set(n,hook('addNonNegative','(JJ)J')); sums++;
    }
    if (sums !== 1) fail('ServerFluxNetwork demand sum anchor count ' + sums);
}
function patchStatistics(node) {
    var sums = 0;
    for each (var m in node.methods) for each (var n in m.instructions.toArray()) {
        if (n.getOpcode() === O.LADD && (m.name === 'weakerTick' || m.name.indexOf('lambda$') === 0)) {
            m.instructions.set(n,hook('addNonNegative','(JJ)J')); sums++;
        } else if (n.getOpcode() === O.LSUB && m.name.indexOf('lambda$weakTick$') === 0) {
            m.instructions.insertBefore(n,new InsnNode(O.LNEG)); m.instructions.set(n,hook('addNonNegative','(JJ)J')); sums++;
        }
    }
    if (sums !== 5) fail('NetworkStatistics sum anchor count ' + sums);
}
function transform(node) {
    try {
        validate(node); // Validate every required method before changing this class.
        if (node.name === TILE) {
            node.interfaces.add(RUNTIME); // Existing methods fulfill the entire Kotlin ABI.
        } else if (node.name === 'sonar/fluxnetworks/register/Messages') {
            var count = 0;
            for each (var m in node.methods) for each (var n in m.instructions.toArray()) if (n.getType() === 6) {
                for (var i=0;i<n.bsmArgs.length;i++) {
                    var arg = n.bsmArgs[i];
                    if (arg instanceof Handle && arg.getOwner() === TILE && arg.getName() === 'sendBlockUpdate') {
                        n.bsmArgs[i] = remapHandle(arg);
                        for (var j=0;j<n.bsmArgs.length;j++) if (n.bsmArgs[j] instanceof Type) n.bsmArgs[j] = Type.getType(mapped(n.bsmArgs[j].getDescriptor()));
                        count++;
                    }
                }
            }
            if (count !== 1) fail('Messages block update anchor count ' + count);
        } else {
            remap(node);
            if (node.name === NETWORK) {
                node.interfaces.add(ABI + 'FluxRuntimeNetwork');
                tileOverload(node,'enqueueConnectionAddition','Z',false,false);
                tileOverload(node,'enqueueConnectionRemoval','V',true,false);
            }
            if (node.name === ROOT+'PhantomFluxDevice') tileOverload(node,'makeUnloaded','L'+ROOT+'PhantomFluxDevice;',false,true);
            if (node.name === SERVER) patchServer(node);
            if (node.name === ROOT+'NetworkStatistics') patchStatistics(node);
            if (node.name === ROOT+'TransferIterator') {
                // Preserve the old ABI for existing tile-only callers. Flux internally uses the new descriptor.
                var compat = new MethodNode(O.ACC_PUBLIC,'next','()L'+TILE+';',null,null);
                compat.instructions.add(new VarInsnNode(O.ALOAD,0)); compat.instructions.add(call(node.name,'next','()L'+RUNTIME+';',false));
                compat.instructions.add(new TypeInsnNode(O.CHECKCAST,TILE)); compat.instructions.add(new InsnNode(O.ARETURN)); node.methods.add(compat);
            }
        }
        marker(node); // Success marker is LAST. Constructor forces and checks all targets.
        ASM.log('INFO','Flux Battery runtime patch applied: ' + node.name);
    } catch (error) {
        ASM.log('ERROR','Flux Battery mandatory patch failed: ' + node.name + ': ' + error);
        // Persist a concrete diagnostic for the constructor even if NeoForge catches script errors.
        var failure = new MethodNode(O.ACC_PUBLIC | O.ACC_STATIC,'fluxBatteryPatchFailure','()Ljava/lang/String;',null,null);
        failure.instructions.add(new LdcInsnNode(String(error))); failure.instructions.add(new InsnNode(O.ARETURN)); node.methods.add(failure);
    }
    return node;
}
function initializeCoreMod() {
    expected = ASM.loadData('coremods/fluxbattery-8.0.0.json');
    var result = {};
    for (var name in expected) result[name] = { target: {type:'CLASS',name:name.replace(/\//g,'.')}, transformer:transform };
    return result;
}
