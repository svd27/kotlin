public var inc: Int = 0;

public var propInc: Int
    get() = inc++
    set(a: Int) {
        inc++
    }

public var dec: Int = 0;

public var propDec: Int
    get() = dec--
    set(a: Int) {
        dec--
    }

fun box(): String {
    propInc++
    if (inc != 2) return "fail in postfix increment: ${inc} != 2"

    propDec--
    if (dec != -2) return "fail in postfix decrement: ${dec} != -2"

    return "OK"
}

