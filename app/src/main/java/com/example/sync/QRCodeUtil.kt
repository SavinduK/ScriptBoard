package com.example.sync

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.json.JSONObject

object QRCodeUtil {

    /**
     * Generates a square QR code Bitmap from input text content.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        return bitmap
    }

    /**
     * Decodes text encoded inside a Bitmap.
     */
    fun decodeQrBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Formats pairing credentials into a standardized JSON string for QR codes.
     */
    fun buildPairingJson(
        deviceId: String,
        deviceName: String,
        ip: String,
        port: Int,
        token: String
    ): String {
        return JSONObject().apply {
            put("type", "KEYPRO_PAIR")
            put("version", 1)
            put("deviceId", deviceId)
            put("deviceName", deviceName)
            put("ip", ip)
            put("port", port)
            put("token", token)
        }.toString()
    }

    /**
     * Parses QR code JSON or raw connection string into a PairingPayload.
     */
    fun parsePairingString(raw: String): PairingPayload? {
        val clean = raw.trim()
        return try {
            if (clean.startsWith("{")) {
                val json = JSONObject(clean)
                val deviceId = json.optString("deviceId", "").ifEmpty { json.optString("id", "") }
                val name = json.optString("deviceName", "").ifEmpty { json.optString("name", "Remote Device") }
                val ip = json.optString("ip", "")
                val port = json.optInt("port", 8765)
                val token = json.optString("token", "")
                if (ip.isNotBlank() && deviceId.isNotBlank()) {
                    PairingPayload(
                        deviceId = deviceId,
                        deviceName = name,
                        ip = ip,
                        port = if (port > 0) port else 8765,
                        token = token
                    )
                } else null
            } else if (clean.contains(":")) {
                // Support quick format: 192.168.1.50:8765 or 192.168.1.50
                val parts = clean.split(":")
                val ip = parts[0].trim()
                val port = parts.getOrNull(1)?.toIntOrNull() ?: 8765
                PairingPayload(
                    deviceId = "dev_${ip.replace(".", "_")}",
                    deviceName = "Device ($ip)",
                    ip = ip,
                    port = port,
                    token = ""
                )
            } else if (clean.count { it == '.' } == 3) {
                // IP only
                PairingPayload(
                    deviceId = "dev_${clean.replace(".", "_")}",
                    deviceName = "Device ($clean)",
                    ip = clean,
                    port = 8765,
                    token = ""
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
