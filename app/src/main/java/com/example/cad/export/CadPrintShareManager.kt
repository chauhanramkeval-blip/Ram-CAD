package com.example.cad.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Handles system printing via Android PrintManager and system sharing via Android FileProvider.
 */
object CadPrintShareManager {

    /**
     * Shares an exported CAD file (PDF, Image, CSV, Report, DXF) to any target app
     * (e.g. Gmail, Drive, Messaging, WhatsApp, Slack, Cloud storage).
     */
    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        subject: String = "CAD Export: ${file.name}"
    ): Boolean {
        return try {
            val uri = getUriForFile(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Exported CAD document from CAD Mobile: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share CAD Document")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Opens an exported file in a compatible third-party viewer (e.g. PDF viewer, Image gallery).
     */
    fun openFile(
        context: Context,
        file: File,
        mimeType: String
    ): Boolean {
        return try {
            val uri = getUriForFile(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Prints a PDF file using Android's native PrintManager and system print spooler.
     * Supports Wi-Fi printers, Mopria/Google Cloud Print, and Android's "Save as PDF" spooler.
     */
    fun printPdf(
        context: Context,
        file: File,
        jobName: String = "CAD_Print_${file.nameWithoutExtension}"
    ): Boolean {
        return try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                ?: return false

            val adapter = object : PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(file.name)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                        .build()
                    callback?.onLayoutFinished(info, newAttributes != oldAttributes)
                }

                override fun onWrite(
                    pages: Array<out PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    if (destination == null) {
                        callback?.onWriteFailed("Destination descriptor is null")
                        return
                    }
                    try {
                        FileInputStream(file).use { input ->
                            FileOutputStream(destination.fileDescriptor).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } >= 0) {
                                    if (cancellationSignal?.isCanceled == true) {
                                        callback?.onWriteCancelled()
                                        return
                                    }
                                    output.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                        callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }

            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}
