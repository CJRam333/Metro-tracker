package com.example.hmrcompanion.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class StationRepositoryTest {

    private fun getRealJsonContent(): String {
        return File("src/main/assets/hmr_stations.json").readText()
    }

    @Test
    fun `test successful parsing of actual JSON`() = runBlocking {
        val assetReader = FakeAssetReader(assetContent = getRealJsonContent())
        val repository = StationRepository(assetReader)

        val lines = repository.getAllLines()
        assertEquals(3, lines.size)

        val redLine = repository.getLine("red")
        assertNotNull(redLine)
        assertEquals(27, redLine?.stations?.size)

        val blueLine = repository.getLine("blue")
        assertNotNull(blueLine)
        assertEquals(23, blueLine?.stations?.size)

        val greenLine = repository.getLine("green")
        assertNotNull(greenLine)
        assertEquals(10, greenLine?.stations?.size)
    }

    @Test
    fun `test parsing Ameerpet station values`() = runBlocking {
        val assetReader = FakeAssetReader(assetContent = getRealJsonContent())
        val repository = StationRepository(assetReader)

        val redLine = repository.getLine("red")
        val ameerpetRed = redLine?.stations?.find { it.name == "Ameerpet" }
        assertNotNull(ameerpetRed)
        assertEquals(17.4374, ameerpetRed!!.lat, 0.0001)
        assertEquals(78.4482, ameerpetRed.lng, 0.0001)
        assertEquals("Blue Line", ameerpetRed.interchange)

        val blueLine = repository.getLine("blue")
        val ameerpetBlue = blueLine?.stations?.find { it.name == "Ameerpet" }
        assertNotNull(ameerpetBlue)
        assertEquals(17.4374, ameerpetBlue!!.lat, 0.0001)
        assertEquals(78.4482, ameerpetBlue.lng, 0.0001)
        assertEquals("Red Line", ameerpetBlue.interchange)
    }

    @Test
    fun `test missing asset throws exception`() {
        val assetReader = FakeAssetReader(shouldThrow = true)
        val repository = StationRepository(assetReader)

        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                repository.getAllLines()
            }
        }
        assertEquals(true, exception.message?.contains("Failed to read hmr_stations.json asset"))
    }

    @Test
    fun `test malformed JSON throws exception`() {
        val assetReader = FakeAssetReader(assetContent = "{ invalid json }")
        val repository = StationRepository(assetReader)

        val exception = assertThrows(Exception::class.java) {
            runBlocking {
                repository.getAllLines()
            }
        }
        assertEquals(true, exception.message?.contains("Failed to parse hmr_stations.json: malformed data"))
    }
}
