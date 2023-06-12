package com.github.yeicor.kraphviz

import com.github.yeicor.ktmpwasm.api.Environment
import com.github.yeicor.ktmpwasm.api.parseModule
import com.github.yeicor.ktmpwasm.base.FunctionRef
import com.github.yeicor.ktmpwasm.base.Signature
import com.github.yeicor.ktmpwasm.base.WasmValue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

public class Kraphviz {
  private companion object {
    /** Parse the graphviz WebAssembly module only once, if required. */
    @OptIn(ExperimentalEncodingApi::class)
    private val module by lazy {
      val mod =
          parseModule(
              Base64.decode(Graphviz.WASM_B64.joinToString("")),
              object : Environment {
                override fun lookupFunction(
                    module: String,
                    name: String,
                    signature: Signature?
                ): FunctionRef {
                  // println("wasm: lookupFunction(module=$module, name=$name,
                  // signature=$signature)")
                  return object : FunctionRef {
                    override fun call(arguments: List<WasmValue>): WasmValue {
                      println("wasm: call $module.$name${arguments.joinToString(", ", "(", " )")}")
                      return WasmValue.wrap(0)
                    }

                    override fun type(): Signature = signature!!
                  }
                }
              })
      mod.init()
      mod
    }
  }

  /** Allocate a string in the WebAssembly memory, returning its pointer. */
  private fun mallocString(str: String): Int {
    val strPtr =
        module.lookupFunction("malloc", null).call(listOf(WasmValue.wrap(str.length))).toI32()
    module.memory.store(strPtr, str.length, str.encodeToByteArray())
    return strPtr
  }

  /** Free a string in the WebAssembly memory, given its pointer. */
  private fun freeString(strPtr: Int) {
    module.lookupFunction("free", null).call(listOf(WasmValue.wrap(strPtr)))
  }

  /** Render the graphviz dot string to SVG. */
  public fun render(dot: String): String {
    val dotPtr = mallocString(dot)
    val svgPtr =
        module.lookupFunction("render_dot_svg", null).call(listOf(WasmValue.wrap(dotPtr))).toI32()
    freeString(dotPtr)
    require(svgPtr != 0) { "Graphviz failed to render" }
    // Find the end of the SVG string from memory
    var svgLen = 0
    for (svgLenTest in 0..(module.memory.data.size - svgPtr)) {
      if (module.memory.data[svgPtr + svgLenTest].toInt() == 0) {
        svgLen = svgLenTest
        break
      }
    }
    require(svgLen > 0) { "SVG string not found in memory" }
    val svg = module.memory.load(svgPtr, svgLen).decodeToString()
    return svg
  }
}
