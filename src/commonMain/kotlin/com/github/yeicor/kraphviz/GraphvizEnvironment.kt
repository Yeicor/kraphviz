package com.github.yeicor.kraphviz

import com.github.yeicor.ktmpwasm.api.Type
import com.github.yeicor.ktmpwasm.base.Environment
import com.github.yeicor.ktmpwasm.base.FunctionRef
import com.github.yeicor.ktmpwasm.base.Signature
import com.github.yeicor.ktmpwasm.base.WasmValue
import com.github.yeicor.ktmpwasm.interpreter.Module

internal class GraphvizEnvironment(
    private val getModule: () -> Module,
    private val stdout: (List<ByteArray>) -> Unit,
    private val stderr: (List<ByteArray>) -> Unit
) : Environment {
  override fun lookupFunction(module: String, name: String, signature: Signature?): FunctionRef {
    // println("wasm: lookupFunction(module=$module, name=$name, signature=$signature)")
    if (module == "wasi_snapshot_preview1" && name == "fd_write") {
      return FdWriteFunction(getModule, stdout, stderr)
    }
    return object : FunctionRef {
      override fun call(arguments: List<WasmValue>) =
          TODO("wasm: call of $module.$name with arguments $arguments was not implemented")

      override fun type(): Signature = signature!!
    }
  }
}

internal class FdWriteFunction(
    private val getModule: () -> Module,
    private val stdout: (List<ByteArray>) -> Unit,
    private val stderr: (List<ByteArray>) -> Unit
) : FunctionRef {
  override fun call(arguments: List<WasmValue>): WasmValue {
    val fd = arguments[0].toI32()
    val iovsPtr = arguments[1].toI32()
    val iovsLen = arguments[2].toI32()
    val nwrittenPtr = arguments[3].toI32()
    val mod = getModule()
    val iovs = mutableListOf<Pair<Int, Int>>()
    for (i in 0 until iovsLen) {
      val ptrRaw = mod.memory.load(iovsPtr + i * 4, 4)
      val ptr = bytesToInt(ptrRaw)
      val lenRaw = mod.memory.load(iovsPtr + i * 4 + 4, 4)
      val len = bytesToInt(lenRaw)
      // println("wasm: fd_write iov[$i] ptr=$ptr len=$len data=${mod.memory.load(ptr,
      // len).decodeToString()}")
      iovs.add(Pair(ptr, len))
    }
    val data = iovs.map { (ptr, len) -> mod.memory.load(ptr, len) }
    when (fd) {
      1 -> stdout(data)
      2 -> stderr(data)
      else -> throw Exception("wasm: fd_write(fd=$fd) not implemented")
    }
    val writtenBytes = data.sumOf { it.size }
    mod.memory.store(nwrittenPtr, 4, intToBytes(writtenBytes, 4))
    return WasmValue.wrap(nwrittenPtr)
  }

  override fun type(): Signature =
      Signature(Type.I32, listOf(Type.I32, Type.I32, Type.I32, Type.I32))

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
