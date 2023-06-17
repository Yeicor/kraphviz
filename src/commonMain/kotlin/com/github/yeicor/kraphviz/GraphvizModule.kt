package com.github.yeicor.kraphviz

import com.github.yeicor.ktmpwasm.api.parseModule
import com.github.yeicor.ktmpwasm.base.WasmValue
import com.github.yeicor.ktmpwasm.interpreter.Module
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal object GraphvizModule {

  /**
   * Parse the graphviz WebAssembly module with a custom stdout/stderr implementation.
   *
   * TODO: Avoid parsing the module each time... (ktmpwasm API is too limited for now)
   */
  internal fun moduleFor(
      stdout: (List<ByteArray>) -> Unit,
      stderr: (List<ByteArray>) -> Unit
  ): Module {
    @OptIn(ExperimentalEncodingApi::class)
    val moduleBytes = Base64.decode(Graphviz.WASM_B64.joinToString(""))
    val modArr = Array(1) { Module() }
    modArr[0] = parseModule(moduleBytes, GraphvizEnvironment({ modArr[0] }, stdout, stderr))
    return modArr[0].also { it.init() }
  }

  /** Allocate a string in the WebAssembly memory, returning its pointer. */
  internal fun mallocString(module: Module, str: String): Int {
    val strPtr =
        module.lookupFunction("malloc", null).call(listOf(WasmValue.wrap(str.length + 1))).toI32()
    module.memory.store(strPtr, str.length, str.encodeToByteArray())
    module.memory.store(strPtr + str.length, 1, byteArrayOf(0))
    return strPtr
  }

  /** Free a string in the WebAssembly memory, given its pointer. */
  internal fun freeString(module: Module, strPtr: Int) {
    module.lookupFunction("free", null).call(listOf(WasmValue.wrap(strPtr)))
  }
}
