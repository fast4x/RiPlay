# 1. Disattiva Class Merging e Outlining sul tuo fork per preservare l'architettura dei thread
-keep,allowobfuscation class it.fast4x.androidyoutubeplayer.** { *; }

# 2. Impedisce a R8 Full Mode di modificare le firme dei metodi (evita la rimozione o modifica dei parametri)
-keepclassmembers,allowobfuscation class it.fast4x.androidyoutubeplayer.** {
    *** *(...);
}

# 3. Protezione totale delle interfacce JavaScript (In Full Mode serve il blocco esplicito dei membri)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 4. Protegge i metodi di callback nativi da modifiche di ottimizzazione (evita micro-latenze nel rendering)
-keepclassmembers class * implements it.fast4x.androidyoutubeplayer.core.player.listeners.YouTubePlayerListener {
    public void on*(...);
}

# 5. Impedisce a R8 di ottimizzare i tipi di ritorno nei bridge con la WebView
-keepattributes Signature, EnclosingMethod, InnerClasses, AnnotationDefault
