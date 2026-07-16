# Native (JNI) methods are bound by name, e.g.
# Java_com_sailens_data_source_ml_NativeYuvInputPreprocessor_nativePreprocessYuvToFloat.
# The declaring classes and their native methods must keep their names under R8, otherwise the
# runtime fails with UnsatisfiedLinkError. (proguard-android-optimize.txt already keeps native
# methods globally; these rules are explicit, scoped insurance.)
-keepclasseswithmembernames,includedescriptorclasses class com.sailens.data.source.ml.** {
    native <methods>;
}

# NativeMlLibrary owns System.loadLibrary + the availability flag; keep it intact.
-keep class com.sailens.data.source.ml.NativeMlLibrary { *; }
