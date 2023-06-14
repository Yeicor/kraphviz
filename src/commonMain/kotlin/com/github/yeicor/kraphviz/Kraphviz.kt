package com.github.yeicor.kraphviz

import com.github.yeicor.ktmpwasm.api.Environment
import com.github.yeicor.ktmpwasm.api.parseModule
import com.github.yeicor.ktmpwasm.base.FunctionRef
import com.github.yeicor.ktmpwasm.base.Signature
import com.github.yeicor.ktmpwasm.base.WasmValue
import com.github.yeicor.ktmpwasm.interpreter.Module
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

public object Kraphviz {

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

  /** Parse the graphviz WebAssembly module only once, if required. */
  @OptIn(ExperimentalEncodingApi::class)
  private val module by lazy {
    val mod: Array<Module> = Array(1) { Module() }
    mod[0] =
        parseModule(Base64.decode(Graphviz.WASM_B64.joinToString("")), KraphvizEnvironment(mod))
    mod[0].init()
    mod[0]
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
}

private class KraphvizEnvironment(private val mod_: Array<Module>) : Environment {
  private val mod: Module
    get() = mod_[0]

  override fun lookupFunction(module: String, name: String, signature: Signature?): FunctionRef {
    // println("wasm: lookupFunction(module=$module, name=$name,
    // signature=$signature)")
    if (module == "wasi_snapshot_preview1" && name == "fd_write") {
      return object : FunctionRef {
        override fun call(arguments: List<WasmValue>): WasmValue {
          val fd = arguments[0].toI32()
          val iovsPtr = arguments[1].toI32()
          val iovsLen = arguments[2].toI32()
          val nwrittenPtr = arguments[3].toI32()
          val iovs = mutableListOf<Pair<Int, Int>>()
          for (i in 0 until iovsLen) {
            val ptrRaw = mod.memory.load(iovsPtr + i * 4, 4)
            val ptr = bytesToInt(ptrRaw)
            val lenRaw = mod.memory.load(iovsPtr + i * 4 + 4, 4)
            val len = bytesToInt(lenRaw)
            iovs.add(Pair(ptr, len))
          }
          // println("wasm: fd_write(fd=$fd, iovs=$iovs, nwrittenPtr=$nwrittenPtr)")
          val str =
              iovs.joinToString("") { (ptr, len) -> mod.memory.load(ptr, len).decodeToString() }
          println("wasm: fd_write($fd): $str") // TODO: Collect log output and return it
          mod.memory.store(nwrittenPtr, 4, intToBytes(str.length, 4))
          return WasmValue.wrap(str.length)
        }

        override fun type(): Signature = signature!!

        private fun bytesToInt(bytes: ByteArray): Int {
          var result = 0
          for (i in bytes.indices) {
            result = result or (bytes[i].toUByte().toInt() shl 8 * i)
          }
          return result
        }

        private fun intToBytes(value: Int, size: Int): ByteArray {
          val result = ByteArray(size)
          for (i in 0 until size) {
            result[i] = (value shr 8 * i).toByte()
          }
          return result
        }
      }
    }
    return object : FunctionRef {
      override fun call(arguments: List<WasmValue>): WasmValue {
        throw NotImplementedError(
            "wasm: call of $module.$name with arguments $arguments was not implemented")
      }

      override fun type(): Signature = signature!!
    }
  }
}
