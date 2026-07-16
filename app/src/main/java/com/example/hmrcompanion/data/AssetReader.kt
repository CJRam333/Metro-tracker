package com.example.hmrcompanion.data

import android.content.Context
import java.io.FileNotFoundException
import java.io.InputStreamReader

interface AssetReader {
    fun readAsset(filename: String): String
}

class AndroidAssetReader(private val context: Context) : AssetReader {
    override fun readAsset(filename: String): String {
        return try {
            context.assets.open(filename).use { inputStream ->
                InputStreamReader(inputStream).readText()
            }
        } catch (e: Exception) {
            throw FileNotFoundException("Asset not found or cannot be read: $filename")
        }
    }
}
