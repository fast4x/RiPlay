package it.fast4x.riplay.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipManager {

    /**
     * Comprime un file in un file .zip
     */
    fun zip(inputFile: File, outputFile: File) {
        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zipOut ->
                val entry = ZipEntry(inputFile.name) // Mantiene il nome originale all'interno dello zip
                zipOut.putNextEntry(entry)

                FileInputStream(inputFile).use { fis ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (fis.read(buffer).also { len = it } > 0) {
                        zipOut.write(buffer, 0, len)
                    }
                }
                zipOut.closeEntry()
            }
        }
    }

    /**
     * Decomprime un file .zip
     */
    fun unzip(zipFile: File, outputFile: File) {
        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zipIn ->
                val entry = zipIn.nextEntry
                if (entry != null) {
                    FileOutputStream(outputFile).use { fos ->
                        val buffer = ByteArray(1024)
                        var len: Int
                        while (zipIn.read(buffer).also { len = it } > 0) {
                            fos.write(buffer, 0, len)
                        }
                    }
                    zipIn.closeEntry()
                }
            }
        }
    }
}