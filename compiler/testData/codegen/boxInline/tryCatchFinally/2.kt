fun test1() : Int {
    val inlineX = My(111)
    var result = 0
    val res = inlineX.perform<My, Int>{

        try {
            throw RuntimeException()
        } catch (e: RuntimeException) {
            result = -1
        }
        result
    }

    return result
}

fun test2() : Int {
    try {
        val inlineX = My(111)
        var result = 0
        val res = inlineX.perform<My, Int>{
            throw RuntimeException()
        }
        return result
    } catch (e: Exception) {
        return -1
    }
}

inline fun execute() : Int {
    try {
        val inlineX = My(111)
        var result = 0
        val res = inlineX.perform<My, Int>{
            throw RuntimeException()
        }
        return result
    } catch (e: Exception) {
        return -1
    }
}

fun test3() : Int {
    return execute()
}

fun box(): String {
    if (test1() != -1) return "test1: ${test1()}"
    if (test2() != -1) return "test2: ${test2()}"
    if (test3() != -1) return "test3: ${test3()}"

    return "OK"
}