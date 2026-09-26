package com.slh.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ReceiptGenerator
 *
 * Generates PDF payment receipts for Sohan's Learning Hub.
 *
 * Flow:
 *
 * PaymentRecord
 *      ↓
 * ReceiptGenerator.generateReceipt()
 *      ↓
 * PDF file
 *
 * The generated receipt is stored locally on the device.
 */
object ReceiptGenerator {

    // ============================================================
    // PAGE CONSTANTS
    // ============================================================

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private const val LEFT = 42f
    private const val RIGHT = 553f

    // ============================================================
    // MAIN METHOD
    // ============================================================

    fun generateReceipt(
        context: Context,
        payment: PaymentRecord
    ): File {

        require(
            payment.status == PaymentStatus.VERIFIED
        ) {
            "Only verified payments can generate a receipt."
        }

        val pdfDocument =
            PdfDocument()

        val pageInfo =
            PdfDocument.PageInfo
                .Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    1
                )
                .create()

        val page =
            pdfDocument.startPage(
                pageInfo
            )

        val canvas =
            page.canvas

        drawReceipt(
            canvas = canvas,
            context = context,
            payment = payment
        )

        pdfDocument.finishPage(
            page
        )

        val receiptDirectory =
            File(
                context.filesDir,
                "Documents/Receipts"
            )

        if (
            !receiptDirectory.exists()
        ) {

            receiptDirectory.mkdirs()
        }

        val safeReceiptNumber =
            payment.receiptNumber
                .replace(
                    "[^A-Za-z0-9_-]".toRegex(),
                    "_"
                )

        val fileName =
            "Receipt_$safeReceiptNumber.pdf"

        val receiptFile =
            File(
                receiptDirectory,
                fileName
            )

        FileOutputStream(
            receiptFile
        ).use { outputStream ->

            pdfDocument.writeTo(
                outputStream
            )
        }

        pdfDocument.close()

        return receiptFile
    }

    // ============================================================
    // RECEIPT DRAWING
    // ============================================================

    private fun drawReceipt(
        canvas: Canvas,
        context: Context,
        payment: PaymentRecord
    ) {

        val profile =
            CoachingProfileStore.get(
                payment.coachingId
            )

        val coachingName =
            profile?.name
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Sohan's Learning Hub"

        val shortName =
            profile?.shortName
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "SLH"

        val tagline =
            profile?.tagline
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Learn Today. Succeed Tomorrow."

        val phone =
            profile?.phone
                ?.takeIf {
                    it.isNotBlank()
                }

        val email =
            profile?.email
                ?.takeIf {
                    it.isNotBlank()
                }

        val address =
            profile?.address
                ?.takeIf {
                    it.isNotBlank()
                }

        // ========================================================
        // PAINTS
        // ========================================================

        val titlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.BLACK

                textSize =
                    24f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        val subtitlePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.DKGRAY

                textSize =
                    11f
            }

        val headingPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.BLACK

                textSize =
                    15f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        val normalPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.BLACK

                textSize =
                    11f
            }

        val boldPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.BLACK

                textSize =
                    11f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        val smallPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.DKGRAY

                textSize =
                    9f
            }

        val amountPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.BLACK

                textSize =
                    17f

                typeface =
                    Typeface.create(
                        Typeface.DEFAULT,
                        Typeface.BOLD
                    )
            }

        val linePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.GRAY

                strokeWidth =
                    1f
            }

        val boxPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color =
                    android.graphics.Color.LTGRAY

                style =
                    Paint.Style.STROKE

                strokeWidth =
                    1f
            }

        // ========================================================
        // HEADER BORDER
        // ========================================================

        canvas.drawRect(
            LEFT,
            35f,
            RIGHT,
            175f,
            boxPaint
        )

        // ========================================================
        // LOGO / SHORT NAME
        // ========================================================

        val logoBitmap =
            loadLogoBitmap(
                context = context,
                logoUri = profile?.logoUri
            )

        val hasLogo = logoBitmap != null

        if (logoBitmap != null) {
            // Draw coaching logo on the left of the header.
            val logoSize = 56
            val logoLeft = (LEFT + 14f).toInt()
            val logoTop = 48
            val destRect = Rect(
                logoLeft,
                logoTop,
                logoLeft + logoSize,
                logoTop + logoSize
            )
            canvas.drawBitmap(
                logoBitmap,
                null,
                destRect,
                null
            )
            logoBitmap.recycle()
        } else {
            // Fallback: short name text when no logo is set.
            canvas.drawText(
                shortName,
                LEFT + 18f,
                78f,
                titlePaint
            )
        }

        // ========================================================
        // COACHING NAME
        // ========================================================

        val nameX =
            if (hasLogo) {
                LEFT + 18f + 64f
            } else {
                LEFT + 18f
            }

        canvas.drawText(
            coachingName,
            nameX,
            105f,
            headingPaint
        )

        // ========================================================
        // TAGLINE
        // ========================================================

        canvas.drawText(
            tagline,
            nameX,
            125f,

            subtitlePaint
        )

        var headerY =
            143f

        if (
            phone != null
        ) {

            canvas.drawText(

                "Phone: $phone",

                LEFT + 18f,

                headerY,

                smallPaint
            )

            headerY += 12f
        }

        if (
            email != null
        ) {

            canvas.drawText(

                "Email: $email",

                LEFT + 18f,

                headerY,

                smallPaint
            )

            headerY += 12f
        }

        if (
            address != null
        ) {

            drawWrappedText(

                canvas = canvas,

                text =
                    "Address: $address",

                x =
                    LEFT + 18f,

                startY =
                    headerY,

                maxWidth =
                    475f,

                paint =
                    smallPaint,

                lineHeight =
                    11f,

                maxLines =
                    2
            )
        }

        // ========================================================
        // RECEIPT TITLE
        // ========================================================

        canvas.drawText(

            "PAYMENT RECEIPT",

            LEFT,

            210f,

            titlePaint
        )

        canvas.drawLine(

            LEFT,

            220f,

            RIGHT,

            220f,

            linePaint
        )

        // ========================================================
        // RECEIPT META
        // ========================================================

        var y =
            250f

        drawLabelValue(

            canvas,
            "Receipt No.",
            payment.receiptNumber,
            LEFT,
            y,
            normalPaint,
            boldPaint
        )

        drawLabelValue(

            canvas,
            "Payment Date",
            payment.paymentDate,
            315f,
            y,
            normalPaint,
            boldPaint
        )

        y += 30f

        drawLabelValue(

            canvas,
            "Payment Status",
            "VERIFIED",
            LEFT,
            y,
            normalPaint,
            boldPaint
        )

        drawLabelValue(

            canvas,
            "Payment Mode",
            paymentModeText(
                payment.paymentMode
            ),
            315f,
            y,
            normalPaint,
            boldPaint
        )

        // ========================================================
        // STUDENT INFORMATION
        // ========================================================

        y += 45f

        canvas.drawText(

            "STUDENT DETAILS",

            LEFT,

            y,

            headingPaint
        )

        y += 22f

        canvas.drawRect(

            LEFT,

            y - 16f,

            RIGHT,

            y + 102f,

            boxPaint
        )

        drawLabelValue(

            canvas,
            "Student Name",
            payment.studentName,
            LEFT + 12f,
            y + 10f,
            normalPaint,
            boldPaint
        )

        drawLabelValue(

            canvas,
            "Student ID",
            payment.studentCode,
            315f,
            y + 10f,
            normalPaint,
            boldPaint
        )

        y += 32f

        drawLabelValue(

            canvas,
            "Course",
            payment.course.ifBlank {
                "-"
            },
            LEFT + 12f,
            y + 10f,
            normalPaint,
            boldPaint
        )

        drawLabelValue(

            canvas,
            "Batch",
            if (
                payment.batchId.isBlank()
            ) {
                "-"
            } else {
                payment.batchId
            },
            315f,
            y + 10f,
            normalPaint,
            boldPaint
        )

        // ========================================================
        // PAYMENT DETAILS
        // ========================================================

        y += 70f

        canvas.drawText(

            "PAYMENT DETAILS",

            LEFT,

            y,

            headingPaint
        )

        y += 22f

        // Table border

        canvas.drawRect(

            LEFT,

            y - 18f,

            RIGHT,

            y + 164f,

            boxPaint
        )

        // Table headers

        canvas.drawText(

            "Description",

            LEFT + 12f,

            y,

            boldPaint
        )

        canvas.drawText(

            "Amount",

            455f,

            y,

            boldPaint
        )

        canvas.drawLine(

            LEFT,

            y + 8f,

            RIGHT,

            y + 8f,

            linePaint
        )

        y += 32f

        // Total Fee

        canvas.drawText(

            "Total Fee",

            LEFT + 12f,

            y,

            normalPaint
        )

        canvas.drawText(

            paymentMoney(
                payment.totalFee
            ),

            455f,

            y,

            normalPaint
        )

        y += 28f

        // Previous paid

        canvas.drawText(

            "Previous Paid",

            LEFT + 12f,

            y,

            normalPaint
        )

        canvas.drawText(

            paymentMoney(
                payment.previousPaidAmount
            ),

            455f,

            y,

            normalPaint
        )

        y += 28f

        // Current payment

        canvas.drawText(

            "Current Payment",

            LEFT + 12f,

            y,

            boldPaint
        )

        canvas.drawText(

            paymentMoney(
                payment.amount
            ),

            455f,

            y,

            amountPaint
        )

        y += 28f

        // Remaining

        canvas.drawText(

            "Remaining Balance",

            LEFT + 12f,

            y,

            boldPaint
        )

        canvas.drawText(

            paymentMoney(
                payment.remainingAmount
            ),

            455f,

            y,

            boldPaint
        )

        y += 28f

        // ========================================================
        // TRANSACTION REFERENCE
        // ========================================================

        canvas.drawText(

            "Transaction / Reference",

            LEFT + 12f,

            y,

            normalPaint
        )

        drawWrappedText(

            canvas = canvas,

            text =
                payment.transactionReference
                    .ifBlank {
                        "-"
                    },

            x =
                455f,

            startY =
                y,

            maxWidth =
                85f,

            paint =
                normalPaint,

            lineHeight =
                11f,

            maxLines =
                2
        )

        // ========================================================
        // AMOUNT RECEIVED BOX
        // ========================================================

        y += 55f

        canvas.drawRect(

            LEFT,

            y,

            RIGHT,

            y + 68f,

            boxPaint
        )

        canvas.drawText(

            "AMOUNT RECEIVED",

            LEFT + 15f,

            y + 27f,

            headingPaint
        )

        canvas.drawText(

            paymentMoney(
                payment.amount
            ),

            LEFT + 15f,

            y + 51f,

            amountPaint
        )

        // ========================================================
        // REMARK
        // ========================================================

        y += 92f

        if (
            payment.remark.isNotBlank()
        ) {

            canvas.drawText(

                "Remark",

                LEFT,

                y,

                boldPaint
            )

            y += 17f

            y =
                drawWrappedText(

                    canvas = canvas,

                    text =
                        payment.remark,

                    x =
                        LEFT,

                    startY =
                        y,

                    maxWidth =
                        500f,

                    paint =
                        normalPaint,

                    lineHeight =
                        14f,

                    maxLines =
                        3
                ) + 10f
        }

        // ========================================================
        // RECEIPT FOOTER
        // ========================================================

        val footerY =
            PAGE_HEIGHT - 125f

        canvas.drawLine(

            LEFT,

            footerY,

            RIGHT,

            footerY,

            linePaint
        )

        canvas.drawText(

            "Authorized Signature",

            410f,

            footerY + 35f,

            boldPaint
        )

        canvas.drawLine(

            405f,

            footerY + 50f,

            RIGHT,

            footerY + 50f,

            linePaint
        )

        canvas.drawText(

            "Authorized by Coaching Admin",

            390f,

            footerY + 68f,

            smallPaint
        )

        // ========================================================
        // GENERATED INFO
        // ========================================================

        val generatedAt =
            SimpleDateFormat(

                "dd-MM-yyyy HH:mm",

                Locale.getDefault()
            )
                .format(
                    Date()
                )

        canvas.drawText(

            "Generated: $generatedAt",

            LEFT,

            footerY + 35f,

            smallPaint
        )

        canvas.drawText(

            "This is a computer-generated receipt.",

            LEFT,

            footerY + 52f,

            smallPaint
        )

        canvas.drawText(

            "Please retain this receipt for your records.",

            LEFT,

            footerY + 69f,

            smallPaint
        )
    }

    // ============================================================
    // LABEL + VALUE
    // ============================================================

    private fun drawLabelValue(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        labelPaint: Paint,
        valuePaint: Paint
    ) {

        canvas.drawText(

            "$label:",

            x,

            y,

            labelPaint
        )

        canvas.drawText(

            value.take(42),

            x + 95f,

            y,

            valuePaint
        )
    }

    // ============================================================
    // WRAPPED TEXT
    // ============================================================

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
        maxLines: Int
    ): Float {

        if (
            text.isBlank()
        ) {

            return startY
        }

        val words =
            text.split(
                " "
            )

        var currentLine =
            ""

        var y =
            startY

        var lineCount =
            0

        for (
        word in words
        ) {

            val testLine =
                if (
                    currentLine.isBlank()
                ) {

                    word

                } else {

                    "$currentLine $word"
                }

            if (
                paint.measureText(
                    testLine
                ) <= maxWidth
            ) {

                currentLine =
                    testLine

            } else {

                if (
                    lineCount >= maxLines
                ) {

                    break
                }

                canvas.drawText(

                    currentLine,

                    x,

                    y,

                    paint
                )

                y +=
                    lineHeight

                lineCount++

                currentLine =
                    word
            }
        }

        if (
            currentLine.isNotBlank() &&
            lineCount < maxLines
        ) {

            canvas.drawText(

                currentLine,

                x,

                y,

                paint
            )

            y +=
                lineHeight
        }

        return y
    }

    // ============================================================
    // PAYMENT MODE
    // ============================================================

    private fun paymentModeText(
        mode: PaymentMode
    ): String {

        return when (mode) {

            PaymentMode.CASH ->
                "Cash"

            PaymentMode.UPI ->
                "UPI"

            PaymentMode.BANK_TRANSFER ->
                "Bank Transfer"

            PaymentMode.CHEQUE ->
                "Cheque"

            PaymentMode.OTHER ->
                "Other"
        }
    }

    // ============================================================
    // MONEY
    // ============================================================

    private fun paymentMoney(
        amount: Double
    ): String {

        return "₹" +
                String.format(
                    Locale.US,
                    "%.2f",
                    amount
                )
    }

    // ============================================================
    // LOGO LOADER
    // ============================================================

    /**
     * Loads coaching logo from content/file URI for PDF drawing.
     * Returns null if URI is blank, unreadable, or decoding fails.
     */
    private fun loadLogoBitmap(
        context: Context,
        logoUri: String?
    ): Bitmap? {

        if (logoUri.isNullOrBlank()) {
            return null
        }

        return try {
            val uri = Uri.parse(logoUri)
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            null
        }
    }
}