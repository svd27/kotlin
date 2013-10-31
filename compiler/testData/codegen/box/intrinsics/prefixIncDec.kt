public var inc: Int = 0;

public var propInc: Int
    get() = ++inc
    set(a: Int) {
        ++inc
    }

public var dec: Int = 0;

public var propDec: Int
    get() = --dec
    set(a: Int) {
        --dec
    }

fun box(): String {
    ++propInc
    if (inc != 3) return "fail in postfix increment: ${inc} != 3"

    --propDec
    if (dec != -3) return "fail in postfix decrement: ${dec} != -3"

    return "OK"
}

