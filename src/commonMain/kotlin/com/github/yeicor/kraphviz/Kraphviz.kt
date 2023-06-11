package com.github.yeicor.kraphviz

import com.github.yeicor.kraphviz.wasm.Graphviz
import com.github.yeicor.ktmpwasm.api.Environment
import com.github.yeicor.ktmpwasm.api.parseModule
import com.github.yeicor.ktmpwasm.base.FunctionRef
import com.github.yeicor.ktmpwasm.base.Signature
import com.github.yeicor.ktmpwasm.base.WasmValue
import com.github.yeicor.ktmpwasm.interpreter.Module
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Kraphviz {
  internal companion object {
    /** Parse the graphviz WebAssembly module only once, if required. */
    @OptIn(ExperimentalEncodingApi::class)
    val module by lazy {
      var mod: Module? = null
      mod =
          parseModule(
              Base64.decode(Graphviz.WASM_B64.joinToString("")),
              object : Environment {
                override fun lookupFunction(
                  module: String,
                  name: String,
                  signature: Signature?
                ): FunctionRef {
                  println(
                      "wasm: lookupFunction(module=$module, name=$name, signature=$signature)")
                  if (module == "wasi_snapshot_preview1" && name == "fd_write") {
                    return object : FunctionRef {
                      override fun call(arguments: List<WasmValue>): WasmValue {
                        val fd = arguments[0].toI32()
                        if (fd != 1 && fd != 2) {
                          throw Error("Only stdout and stderr fd_write calls are supported")
                        }
                        val ioVecPtr = arguments[1].toI32()
                        val ioVecLen = arguments[2].toI32()
                        val nWrittenPtr = arguments[3].toI32()
                        val strBuilder = StringBuilder()
                        for (i in 0 until ioVecLen) {
                          val baseAddr = ioVecPtr + i * 4 * 2
                          val ptr = mod!!.memory.loadI32(baseAddr, 0)
                          val len = mod!!.memory.loadI32(baseAddr, 4)
                          strBuilder.append(mod!!.memory.load(ptr, len).decodeToString())
                        }
                        val str = strBuilder.toString()
                        val strByteSize = str.encodeToByteArray().size
                        mod!!.memory.storeI32(nWrittenPtr, 0, strByteSize)
                        println("wasm: fd_write($fd, $ioVecPtr, $ioVecLen) = $str")
                        return WasmValue.wrap(strByteSize)
                      }

                      override fun type(): Signature = signature!!
                    }
                  }
                  return object : FunctionRef {
                    override fun call(arguments: List<WasmValue>): WasmValue {
                      println(
                          "wasm: call $module.$name${arguments.joinToString(", ", "(", " )")}")
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
  internal fun mallocString(str: String): Int {
    val strPtr =
        module.lookupFunction("malloc", null).call(listOf(WasmValue.wrap(str.length))).toI32()
    module.memory.store(strPtr, str.length, str.encodeToByteArray())
    return strPtr
  }

  /** Free a string in the WebAssembly memory, given its pointer. */
  internal fun freeString(strPtr: Int) {
    module.lookupFunction("free", null).call(listOf(WasmValue.wrap(strPtr)))
  }

  /** Render the graphviz dot string to SVG. */
  fun render(dot: String): String {
    val dotPtr = mallocString(dot)
    val result =
        module
            .lookupFunction("main_api", null)
            .call(listOf(WasmValue.wrap(dotPtr), WasmValue.wrap(dot.length)))
            .toI32()
    freeString(dotPtr)
    require(result == 0) { "Graphviz failed to render" }
    // Find the end of the SVG string from memory
    var svgLen = 0
    for (svgLenTest in 0..(module.memory.data.size - dotPtr)) {
      if (module.memory.data[dotPtr + svgLenTest].toInt() == 0) {
        svgLen = svgLenTest
        break
      }
    }
    require(svgLen > 0) { "SVG string not found in memory" }
    val svg = module.memory.load(dotPtr, svgLen).decodeToString()
    return svg
  }
}
