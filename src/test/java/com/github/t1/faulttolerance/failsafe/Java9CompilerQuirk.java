package com.github.t1.faulttolerance.failsafe;

public class Java9CompilerQuirk {
    // public void foo() {
    //     bar(this::baz);
    // }
    //
    // private static String bar(Consumer<? extends Number> fallback) {
    //     return "a";
    // }
    //
    // private static String bar(Function<? extends Number, ?> fallback) {
    //     return "b";
    // }
    //
    // private String baz(Number e) {
    //     return null;
    // }
}
