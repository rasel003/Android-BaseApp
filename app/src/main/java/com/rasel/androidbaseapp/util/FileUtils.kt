package com.rasel.androidbaseapp.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object FileUtils {
    private const val TAG = "FileUtils"
    const val ZIP = "application/zip"
    const val DOC = "application/msword"
    const val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val TEXT = "text/*"
    const val PDF = "application/pdf"
    const val XLS = "application/vnd.ms-excel"
    const val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val PPT = "application/vnd.ms-powerpoint"
    const val PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    const val IMAGE = "image/*"
    const val AUDIO = "audio/*"

    fun Context.getFileName(uri: Uri): String? = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
        else -> uri.path?.let(::File)?.name
    }

    private fun Context.getContentFileName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
        }
    }.getOrNull()

    fun commonDocumentDirPath(folderName: String): File {
        val dir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .toString() + "/" + folderName
            )
        } else {
            File(
                Environment.getExternalStorageDirectory()
                    .toString() + File.separator + Environment.DIRECTORY_DOCUMENTS + "/" + folderName
            )
        }
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    fun commonDownloadDirPath(): File {
        val dir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + ""
            )
        } else {
            File(
                Environment.getExternalStorageDirectory()
                    .toString() + File.separator + Environment.DIRECTORY_DOWNLOADS
            )
        }
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    //creating request-body using uri
    class RequestBodyFromUri(
        private val contentResolver: ContentResolver,
        private val contentUri: Uri
    ) :
        RequestBody() {
        override fun contentType(): MediaType? {
            val contentType = contentResolver.getType(contentUri) ?: return null
            return contentType.toMediaTypeOrNull()
        }

        override fun writeTo(sink: BufferedSink) {
            try {
                val inputStream = contentResolver.openInputStream(contentUri)
                val outputStream = sink.outputStream()
                val fileLength = inputStream!!.available().toLong()
                var uploaded = 0L
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var n: Int
                while (-1 != inputStream.read(buffer).also { n = it }) {

                    // update progress on UI thread
                    outputStream.write(buffer, 0, n)
                    uploaded += n.toLong()
                }
                inputStream.close()
            } catch (e: IOException) {
                Timber.tag(TAG).e(e, "writeTo: %s", e.message)
            }
        }

        companion object {
            private const val DEFAULT_BUFFER_SIZE = 2048
        }
    }


    fun getCustomFileChooserIntent(vararg types: String): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        // Filter to only show results that can be "opened"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, types)
        return intent
    }

    fun getMimeType(file: File): String {
        var type: String? = null
        val url = file.toString()
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)
        if (extension != null) {
            type = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(extension.lowercase(Locale.getDefault()))
        }
        if (type == null) {
            type = "application/*" // fallback type. You might set it to */*
        }
        return type
    }


    private fun getMimeType(fileExtension: String): String {
        // Add MIME types for various file extensions
        return when (fileExtension.lowercase(Locale.ROOT)) {
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            else -> "application/octet-stream" // Default MIME type for other types of files
        }
    }

    fun openDownloadedFile(uri: Uri, context: Context, extension: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(extension))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun getDownloadedFilePath(downloadId: Long, context: Context): Uri? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            /*val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            return cursor.getString(columnIndex)*/

            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            if (statusIndex != -1 && localUriIndex != -1) {
                val status = cursor.getInt(statusIndex)
                if (status == DownloadManager.STATUS_SUCCESSFUL) {

                    val uriString = cursor.getString(localUriIndex)

                    // Convert file:// URI to content:// URI using FileProvider
                    val fileUri = Uri.parse(uriString)
                    return FileProvider.getUriForFile(
                        context,
                        "${context.applicationContext.packageName}.fileprovider",
                        File(fileUri.path!!)
                    )
                } else {
                    Timber.w("Download Unsuccessful, Status Code: $status")
                    Toast.makeText(context, "Download Unsuccessful", Toast.LENGTH_SHORT).show()
                    return null
                }
            } else {
                return null
            }
        }
        return null
    }

    private fun getFileNameFromUrl(url: String): String {
        // Remove query parameters and fragment identifiers
        val cleanUrl = url.split("?")[0].split("#")[0]

        // Split the URL by "/" to get individual components
        val components = cleanUrl.split("/")

        // The last component should be the file name
        val fileName = components.last()

        // If the file name is empty or ends with a trailing slash, try the second last component
        return if (fileName.isEmpty() || fileName.endsWith("/")) {
            components.getOrNull(components.size - 2) ?: "unknown"
        } else {
//            "${Calendar.getInstance().timeInMillis}-$fileName"
            fileName
        }
    }
    fun getExtensionByStringHandling(filename: String): Optional<String>? {
        return Optional.ofNullable(filename)
            .filter { f: String -> f.contains(".") }
            .map { f: String ->
                f.substring(
                    filename.lastIndexOf(".") + 1
                )
            }
    }

    fun downloadFile(
        url: String,
        fileName: String = "",
        context: Context,
        onDownloadComplete: ((downloadId: Long, extension: String) -> Unit)
    ) {

        val uri = Uri.parse(url)
        val folder: String = "BaseApp"
        val fileNameTemp = fileName.ifEmpty { getFileNameFromUrl(url) }
        val extension = MimeTypeMap.getFileExtensionFromUrl(url)

       val downloadedFile = File( "${commonDownloadDirPath()}${File.separator}$folder${File.separator}", fileNameTemp)


        val request = DownloadManager.Request(uri)
            .setTitle(fileNameTemp)
            .setDescription("Downloading...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setDestinationUri(Uri.fromFile(downloadedFile))
//            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS , fileNameTemp)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadID = downloadManager.enqueue(request)

        // Register BroadcastReceiver to listen for download completion
        registerDownloadReceiver(context, downloadID, extension, onDownloadComplete)
    }

    private fun registerDownloadReceiver(context: Context, downloadID: Long, extension: String, onDownloadComplete: (Long, String) -> Unit) {
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null || intent == null) return // Handle potential null context or intent

                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadID) {
                    // Download completed
                    onDownloadComplete(downloadID, extension)
                    try {
                        context.unregisterReceiver(this) // unregister BroadcastReceiver
                    } catch (e: IllegalArgumentException) {
                        // Receiver was already unregistered, ignore
                    }
                }
            }
        }

        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        val receiverFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            0
        }
        context.registerReceiver(onComplete, intentFilter, receiverFlags)
    }
    @JvmStatic
    fun comPressImage(imagePath: String?): File? {
        var outputFile: File? = null
        val mImageFile = File(imagePath)
        val options = BitmapFactory.Options()
        options.inSampleSize = 4
        try {
            val myBitmap =
                BitmapFactory.decodeStream(FileInputStream(mImageFile), null, options)
            val baos = ByteArrayOutputStream()
            myBitmap!!.compress(Bitmap.CompressFormat.PNG, 90, baos)
            val imageBytes = baos.toByteArray()
            val compressedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            // create a new file with these compressed bitmap and saved it to phone memory.
            outputFile = savePicture(compressedBitmap)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return outputFile
    }

    fun getImageFile(imagePath: String?): File? {
        var outputFile: File? = null
        val mImageFile = File(imagePath)
        val options = BitmapFactory.Options()
        options.inSampleSize = 4
        try {
            val myBitmap =
                BitmapFactory.decodeStream(FileInputStream(mImageFile), null, options)
            val baos = ByteArrayOutputStream()
            myBitmap!!.compress(Bitmap.CompressFormat.PNG, 90, baos)
            val imageBytes = baos.toByteArray()
            val compressedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            // create a new file with these compressed bitmap and saved it to phone memory.
            outputFile = savePicture(compressedBitmap)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return outputFile
    }

    private fun savePicture(bitmap: Bitmap): File {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "orko_photo"
        )
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Timber.tag("Directory Failed: ").e("Failed to Make Dir")
            }
        }
        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "orko_$timeStamp.jpg"
        val mediaFile = File(mediaStorageDir.path + File.separator + fileName)
        try {
            val fos = FileOutputStream(mediaFile)
            val bo = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            //fos.write(imageBytes);
            //fos.flush();
            fos.close()
        } catch (e: IOException) {
            Timber.tag("Error").e(e.message!!)
        }
        return mediaFile
    }

    fun getBlurBitmap(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        var sentBitmap = sentBitmap
        val width = (sentBitmap.width * scale).roundToInt()
        val height = (sentBitmap.height * scale).roundToInt()
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, true)

        if(sentBitmap.config == null) return null

        val bitmap = sentBitmap.copy(sentBitmap.config!!, true)
        if (radius < 1) {
            return null
        }
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))
        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yi = 0
        yw = yi
        val stack = Array(div) {
            IntArray(
                3
            )
        }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {

                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] =
                    -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    fun tintAllIcons(menu: Menu?, color: Int) {
        if (menu != null) {
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                tintMenuItemIcon(color, item)
            }
        }
    }

    fun tintMenuItemIcon(color: Int, item: MenuItem) {
        val drawable = item.icon
        if (drawable != null) {
            val wrapped = DrawableCompat.wrap(drawable)
            drawable.mutate()
            DrawableCompat.setTint(wrapped, color)
            item.setIcon(drawable)
        }
    }

    fun changeBackArrowColor(context: AppCompatActivity, color: Int) {
        val res: Int = context.resources.getIdentifier(
            "abc_ic_ab_back_material",
            "drawable",
            context.packageName
        )
        if (res == 0) return
        val upArrow = ContextCompat.getDrawable(context, res)
        if (upArrow != null) {
            upArrow.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            val supportActionBar = context.supportActionBar
            supportActionBar?.setHomeAsUpIndicator(upArrow)
        }
    }
}