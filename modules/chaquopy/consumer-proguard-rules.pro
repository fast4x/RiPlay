# === CHAQUOPY / YT-DLP RULES ===

# Mantiene tutte le classi dell'API Chaquopy (fondamentale per la reflection)
-keep class com.chaquo.python.** { *; }

# Mantiene tutte le classi del tuo progetto che potrebbero essere chiamate da Python.
# Sostituisci "it.fast4x.riplay" con il tuo package reale se diverso.
# Se hai creato il modulo chaquopy-helper, mantieni anche quello:
-keep class it.fast4x.riplay.chaquopy.** { *; }
-keep class it.fast4x.riplay.** { *; }

# Evita che R8 rinomini i metodi nativi JNI usati da Chaquopy
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**