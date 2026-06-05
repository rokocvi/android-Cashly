package com.example.projektmobpravi.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.projektmobpravi.data.local.entity.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun export(context: Context, transactions: List<TransactionEntity>): Boolean {
        val fileName = "transakcije_${System.currentTimeMillis()}.csv"
        val csvContent = buildCsv(transactions)

        return try {
            val outputStream = getOutputStream(context, fileName) ?: return false
            outputStream.use { it.write(csvContent.toByteArray()) }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun buildCsv(transactions: List<TransactionEntity>): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("Datum,Iznos,Kategorija,Opis,Valuta")
        transactions.forEach { t ->
            val date = sdf.format(Date(t.date))
            sb.appendLine("$date,${t.amount},${t.category},${t.note},${t.currency}")
        }
        return sb.toString()
    }

    private fun getOutputStream(context: Context, fileName: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver
                .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }
    }
}
