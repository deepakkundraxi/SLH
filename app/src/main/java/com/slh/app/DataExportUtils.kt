package com.slh.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CSV export for coaching data (students, fees, attendance).
 * Uses the same FileProvider already declared in the manifest.
 */
object DataExportUtils {

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

    private fun csvEscape(value: String?): String {
        val v = value ?: ""
        return if (v.contains(',') || v.contains('"') || v.contains('\n')) {
            "\"${v.replace("\"", "\"\"")}\""
        } else {
            v
        }
    }

    private fun writeCsv(
        context: Context,
        fileName: String,
        header: List<String>,
        rows: List<List<String?>>
    ): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)

        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.appendLine(header.joinToString(",") { csvEscape(it) })
            rows.forEach { row ->
                writer.appendLine(row.joinToString(",") { csvEscape(it) })
            }
        }
        return file
    }

    fun shareFile(
        context: Context,
        file: File,
        chooserTitle: String = "Share export"
    ) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    // ---------- Students ----------

    fun exportStudentsCsv(
        context: Context,
        coachingId: String
    ): File {
        val students = StudentStore.findStudentsByCoaching(coachingId)
        val header = listOf(
            "id", "studentId", "name", "mobile", "address",
            "username", "course", "admissionDate", "status",
            "batchId", "coachingId"
        )
        val rows = students.map { s ->
            listOf(
                s.id, s.studentId, s.name, s.mobile, s.address,
                s.username, s.course, s.admissionDate, s.status.name,
                s.batchId, s.coachingId
            )
        }
        return writeCsv(
            context,
            "students_${coachingId}_${stamp()}.csv",
            header,
            rows
        )
    }

    // ---------- Fees (per-student summary) ----------

    fun exportFeesCsv(
        context: Context,
        coachingId: String
    ): File {
        val students = StudentStore.findStudentsByCoaching(coachingId)
        val header = listOf(
            "studentId", "studentName", "originalFee", "concessionAmount",
            "totalFee", "totalPaid", "remaining", "paymentMode", "coachingId"
        )
        val rows = students.mapNotNull { s ->
            val summary = try {
                FeeStore.getSummary(s.id, coachingId)
            } catch (_: Exception) {
                null
            } ?: return@mapNotNull null

            listOf(
                s.id,
                s.name,
                summary.originalFee.toString(),
                summary.concessionAmount.toString(),
                summary.totalFee.toString(),
                summary.totalPaid.toString(),
                summary.remaining.toString(),
                summary.paymentMode.name,
                coachingId
            )
        }
        return writeCsv(
            context,
            "fees_${coachingId}_${stamp()}.csv",
            header,
            rows
        )
    }

    // ---------- Manual attendance ----------

    fun exportAttendanceCsv(
        context: Context,
        coachingId: String
    ): File {
        val records = AttendanceStatusStore.records.filter {
            it.coachingId == coachingId
        }
        val header = listOf(
            "id", "studentId", "studentName", "date", "status", "coachingId"
        )
        val rows = records.map { r ->
            val student = StudentStore.students.find { it.id == r.studentId }
            listOf(
                r.id,
                r.studentId,
                student?.name ?: "",
                r.date,
                r.status.name,
                coachingId
            )
        }
        return writeCsv(
            context,
            "attendance_${coachingId}_${stamp()}.csv",
            header,
            rows
        )
    }
}
