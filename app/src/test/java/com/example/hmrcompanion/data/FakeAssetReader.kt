package com.example.hmrcompanion.data

class FakeAssetReader(private val assetContent: String? = null, private val shouldThrow: Boolean = false) : AssetReader {
    override fun readAsset(filename: String): String {
        if (shouldThrow) {
            throw java.io.FileNotFoundException("Asset not found")
        }
        return assetContent ?: ""
    }
}
