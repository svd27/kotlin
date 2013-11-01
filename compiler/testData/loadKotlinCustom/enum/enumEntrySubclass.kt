package test

enum class E {
    SIMPLE
    SUBCLASS {
        fun foo() = 42
    }
}
