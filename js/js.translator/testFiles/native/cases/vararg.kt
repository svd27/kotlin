package foo

import js.*

native
fun testNativeVarargWithFunLit(vararg args: Int, f: (a: IntArray) -> Boolean): Boolean = js.noImpl

fun box(): String {
  if (!(testNativeVarargWithFunLit(1, 2, 3) { args -> args.size == 3 }))
    return "failed when call native function with vararg and fun literal"

  return "OK"
}