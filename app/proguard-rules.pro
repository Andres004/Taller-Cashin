# Reglas de ProGuard/R8 (minify está desactivado por ahora).
# Si se activa en release, conservar las clases @Serializable:
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class org.ucb.cashin.**$$serializer { *; }
-keepclassmembers class org.ucb.cashin.** { *** Companion; }
