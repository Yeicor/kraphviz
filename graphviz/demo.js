
// This demo loads the wasm module and runs it to ensure it works.

// noinspection NodeCoreCodingAssistance
const fs = require('fs');

const wasmFile = fs.readFileSync('./graphviz.wasm');

WebAssembly.instantiate(wasmFile, {
    wasi_snapshot_preview1: {
        clock_time_get: () => { console.log("clock_time_get", JSON.stringify(arguments)); return performance.now(); },
        proc_exit: () => { console.log("proc_exit", JSON.stringify(arguments)); return 0; },
        fd_write: () => { console.log("fd_write", JSON.stringify(arguments)); return 0; },
        fd_read: () => { console.log("fd_read", JSON.stringify(arguments)); return 0; },
        fd_close: () => { console.log("fd_close", JSON.stringify(arguments)); return 0; },
        fd_seek: () => { console.log("fd_seek", JSON.stringify(arguments)); return 0; },
        environ_sizes_get: () => { console.log("environ_sizes_get", JSON.stringify(arguments)); return 0; },
        environ_get: () => { console.log("environ_get", JSON.stringify(arguments)); return 0; }
    },
    env: {
        __syscall_faccessat: () => { console.log("__syscall_faccessat", JSON.stringify(arguments)); return 0; },
        __syscall_stat64: () => { console.log("__syscall_stat64", JSON.stringify(arguments)); return 0; },
        __syscall_newfstatat: () => { console.log("__syscall_newfstatat", JSON.stringify(arguments)); return 0; },
        __syscall_unlinkat: () => { console.log("__syscall_unlinkat", JSON.stringify(arguments)); return 0; }
    }
}).then(result => {
    const exports = result.instance.exports;
    console.log(exports);
    const dotStr = 'digraph { a -> b; }';
    const dotPtr =  mallocString(exports, dotStr);
    let svgPtr = exports.render_dot_svg(dotPtr);
    let svgStr = '';
    let svgView = new Uint8Array(exports.memory.buffer, svgPtr, 1);
    while (svgView[0] !== 0) {
        svgStr += String.fromCharCode(svgView[0]);
        svgPtr++;
        svgView = new Uint8Array(exports.memory.buffer, svgPtr, 1);
    }
    console.log(svgStr);
})

function mallocString(exports, str) {
    const ptr = exports.malloc(str.length + 1);
    const view = new Uint8Array(exports.memory.buffer, ptr, str.length + 1);
    for (let i = 0; i < str.length; i++) {
        view[i] = str.charCodeAt(i);
    }
    return ptr;
}