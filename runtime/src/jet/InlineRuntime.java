/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jet;

import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

@AssertInvisibleInResolver
public class InlineRuntime {

    public static <R> R invoke(Function0<R> f0) {
        return f0.invoke();
    }

    public static <P1, R> R invoke(P1 p1, Function1<P1, R> f1) {
        return f1.invoke(p1);
    }

    public static <P1, P2, R> R invoke(P1 p1, P2 p2, Function2<P1, P2, R> f2) {
        return f2.invoke(p1, p2);
    }

    public static <P1, P2, P3, R> R invoke(P1 p1, P2 p2, P3 p3, Function3<P1, P2, P3, R> f3) {
        return f3.invoke(p1, p2, p3);
    }

    public static <P1, P2, P3, P4, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, Function4<P1, P2, P3, P4, R> f4) {
        return f4.invoke(p1, p2, p3, p4);
    }

    public static <P1, P2, P3, P4, P5, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, Function5<P1, P2, P3, P4, P5, R> f5) {
        return f5.invoke(p1, p2, p3, p4, p5);
    }

    public static <P1, P2, P3, P4, P5, P6, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, Function6<P1, P2, P3, P4, P5, P6, R> f6) {
        return f6.invoke(p1, p2, p3, p4, p5, p6);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, Function7<P1, P2, P3, P4, P5, P6, P7, R> f7) {
        return f7.invoke(p1, p2, p3, p4, p5, p6, p7);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, Function8<P1, P2, P3, P4, P5, P6, P7, P8, R> f8) {
        return f8.invoke(p1, p2, p3, p4, p5, p6, p7, p8);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, Function9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R> f9) {
        return f9.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, Function10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> f10) {
        return f10.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, Function11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> f11) {
        return f11.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, Function12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> f12) {
        return f12.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, Function13<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> f13) {
        return f13.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, Function14<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> f14) {
        return f14.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, Function15<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> f15) {
        return f15.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, Function16<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> f16) {
        return f16.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, Function17<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> f17) {
        return f17.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, Function18<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> f18) {
        return f18.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, Function19<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> f19) {
        return f19.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, P20 p20, Function20<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> f20) {
        return f20.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, P20 p20, P21 p21, Function21<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> f21) {
        return f21.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21);
    }

    public static <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> R invoke(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, P20 p20, P21 p21, P22 p22, Function22<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> f22) {
        return f22.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22);
    }

    public static <T,R> R invoke(T t, ExtensionFunction0<T, R> f0) {
        return f0.invoke(t);
    }

    public static <T, P1, R> R invoke(T t, P1 p1, ExtensionFunction1<T, P1, R> f1) {
        return f1.invoke(t, p1);
    }

    public static <T, P1, P2, R> R invoke(T t, P1 p1, P2 p2, ExtensionFunction2<T, P1, P2, R> f2) {
        return f2.invoke(t, p1, p2);
    }

    public static <T, P1, P2, P3, R> R invoke(T t, P1 p1, P2 p2, P3 p3, ExtensionFunction3<T, P1, P2, P3, R> f3) {
        return f3.invoke(t, p1, p2, p3);
    }

    public static <T, P1, P2, P3, P4, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, ExtensionFunction4<T, P1, P2, P3, P4, R> f4) {
        return f4.invoke(t, p1, p2, p3, p4);
    }

    public static <T, P1, P2, P3, P4, P5, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, ExtensionFunction5<T, P1, P2, P3, P4, P5, R> f5) {
        return f5.invoke(t, p1, p2, p3, p4, p5);
    }

    public static <T, P1, P2, P3, P4, P5, P6, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, ExtensionFunction6<T, P1, P2, P3, P4, P5, P6, R> f6) {
        return f6.invoke(t, p1, p2, p3, p4, p5, p6);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, ExtensionFunction7<T, P1, P2, P3, P4, P5, P6, P7, R> f7) {
        return f7.invoke(t, p1, p2, p3, p4, p5, p6, p7);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, ExtensionFunction8<T, P1, P2, P3, P4, P5, P6, P7, P8, R> f8) {
        return f8.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, ExtensionFunction9<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, R> f9) {
        return f9.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, ExtensionFunction10<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, R> f10) {
        return f10.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, ExtensionFunction11<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, R> f11) {
        return f11.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, ExtensionFunction12<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, R> f12) {
        return f12.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, ExtensionFunction13<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, R> f13) {
        return f13.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, ExtensionFunction14<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, R> f14) {
        return f14.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, ExtensionFunction15<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, R> f15) {
        return f15.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, ExtensionFunction16<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, R> f16) {
        return f16.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, ExtensionFunction17<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, R> f17) {
        return f17.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, ExtensionFunction18<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, R> f18) {
        return f18.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, ExtensionFunction19<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, R> f19) {
        return f19.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, P20 p20, ExtensionFunction20<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, R> f20) {
        return f20.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, P20 p20, P21 p21, ExtensionFunction21<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, R> f21) {
        return f21.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21);
    }

    public static <T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> R invoke(T t, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5, P6 p6, P7 p7, P8 p8, P9 p9, P10 p10, P11 p11, P12 p12, P13 p13, P14 p14, P15 p15, P16 p16, P17 p17, P18 p18, P19 p19, P20 p20, P21 p21, P22 p22, ExtensionFunction22<T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22, R> f22) {
        return f22.invoke(t, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22);
    }

}