# Regras ProGuard do CapSafe
# ONNX Runtime (JNI)
-keep class ai.onnxruntime.** { *; }
# Modelos Retrofit/Gson
-keep class br.unirv.capsafe.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
