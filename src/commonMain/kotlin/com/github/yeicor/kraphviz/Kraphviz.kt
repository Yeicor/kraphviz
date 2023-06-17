package com.github.yeicor.kraphviz

import com.github.yeicor.ktmpwasm.base.WasmValue

public class Kraphviz(
    stdout: (List<ByteArray>) -> Unit = {},
    stderr: (List<ByteArray>) -> Unit = {
      print(it.joinToString("") { vec -> vec.decodeToString() })
    }
) {
  /** Load the module only once per instance */
  private val module = GraphvizModule.moduleFor(stdout, stderr)

  /**
   * Render a string in the [graphviz dot language](https://www.graphviz.org/doc/info/lang.html) to
   * SVG. The returned as a string is the complete SVG document.
   */
  public fun render(dot: String): String {
    val dotPtr = GraphvizModule.mallocString(module, dot)
    val svgPtr =
        module.lookupFunction("render_dot_svg", null).call(listOf(WasmValue.wrap(dotPtr))).toI32()
    GraphvizModule.freeString(module, dotPtr)
    require(svgPtr != 0)
    var svgLen = 0
    for (svgLenTest in 0..(module.memory.data.size - svgPtr)) {
      if (module.memory.data[svgPtr + svgLenTest].toInt() == 0) {
        svgLen = svgLenTest
        break
      }
    }
    require(svgLen > 0) { "SVG string not found in memory" }
    val svg = module.memory.load(svgPtr, svgLen).decodeToString()
    GraphvizModule.freeString(module, svgPtr) // Already copied out of memory, so free it
    return svg
  }
}
