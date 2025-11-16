import com.example.Spread
import com.example.prepareData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.math.exp
import kotlin.test.assertContains

const val NUMBER_OF_GROUPS_REMAINDER = 2

class MainTest {

    fun expectIllegalArgumentException(max: Int, chunks: Int) {
        assertThrows<IllegalArgumentException> { prepareData(max, chunks) }
    }

    @Test
    fun testMoreChunksThanElements() {
        expectIllegalArgumentException(6, 7)
    }

    @Test
    fun testLessThanOneChunk(){
        expectIllegalArgumentException(55, 0)
        expectIllegalArgumentException(99, -12)
    }
    @Test
    fun testLessThanOneElement(){
        expectIllegalArgumentException(0, 55)
        expectIllegalArgumentException(-99, 12)
    }



    /**
     * If there's no remainder, behavior is always the same:
     *  - number of lists is always equal to the parallel parameter
     *  - If grouping by result list size, there is always be 1 key
     *  - Each list has max / parallel elements
     */
    fun noRemainder(spread: Spread, max: Int = 50, parallel: Int = 5) {
        val data = prepareData(max, parallel, spread)
        // data is a List<List<Double>>.
        // It should have 5 lists of 10 elements as there is no remainder (50 % 5 = 0)

        val groupedBy = data.groupBy { it.size }
        assertEquals(1, groupedBy.keys.size, "Expected 1 keys, got ${groupedBy.keys.size} instead")

        assertEquals(parallel, data.size, "Expected $parallel elements, got ${data.size} instead")

        data.forEach {assertEquals((max/parallel), it.size, "Size of each list should be ${(max/parallel)}, this one is ${it.size}") }
    }

    fun withRemainder(max: Int, parallel: Int, spread: Spread): List<List<Int>> {

        val data = prepareData(max, parallel, spread)
        // data is a List<List<Double>>.
        // It should have 5 lists: 3 of 11 elements, and 2 of 10 elements: 53 % 3 = 3, and we spread this remainder evenly on the lists

        var expectedNumberOfKeys = 2

        var expectedFirstKey = 0
        var expectedSecondKey = 0
        var expectedFirstSize = 0
        var expectedSecondSize = 0

        var expectedLists = parallel
        when(spread) {
            Spread.EVEN -> {
                // If max is 53 and parallel is 5
                // we should have 3 lists of 11 and 2 lists of 10
                expectedFirstKey = (max / parallel) + 1
                expectedFirstSize = max % parallel

                expectedSecondKey = max / parallel
                expectedSecondSize = parallel - (max % parallel)
            }
            Spread.ALL_IN_FIRST -> {
                // If max is 53 and parallel is 5
                // we should have 1 list (the first one) of 13 and 4 lists of 10
                expectedFirstKey = (max / parallel) + (max % parallel)
                expectedFirstSize = 1

                expectedSecondKey = max / parallel
                expectedSecondSize = parallel - 1
            }
            Spread.ALL_IN_LAST -> {
                // If max is 53 and parallel is 5
                // we should have 1 list (the last one) of 13 and 4 lists of 10
                expectedFirstKey = (max / parallel) + (max % parallel)
                expectedFirstSize = 1

                expectedSecondKey = max / parallel
                expectedSecondSize = parallel - 1
            }
            Spread.ADDITIONAL_CHUNK -> {
                // If max is 53 and parallel is 5
                // we should have 5 lists of 10 and 1 list (the last one) of 3
                expectedFirstKey = max / parallel
                expectedFirstSize = parallel

                expectedSecondKey = max % parallel
                expectedSecondSize = 1

                expectedLists = parallel + 1
            }
            Spread.IGNORED -> {
                // Same situation as no remainder
                expectedNumberOfKeys = 1
                expectedFirstKey = parallel
                expectedFirstSize = max / parallel
            }
        }
        assertEquals(expectedLists, data.size, "Expected $expectedLists elements, got ${data.size} instead")

        val groupedBy = data.groupBy { it.size }
        assertEquals(expectedNumberOfKeys, groupedBy.keys.size, "Expected $NUMBER_OF_GROUPS_REMAINDER keys, got ${groupedBy.keys.size} instead")

        assertContains(groupedBy, expectedFirstKey, "Missing key $expectedFirstKey")
        assertEquals(expectedFirstSize,groupedBy[expectedFirstKey]?.size, "Expected $expectedFirstSize lists for $expectedFirstKey, got ${groupedBy[expectedFirstKey]?.size} instead")

        // Will be false if Spread.IGNORED
        if (expectedNumberOfKeys == NUMBER_OF_GROUPS_REMAINDER) {
            assertContains(groupedBy, expectedSecondKey, "Missing key $expectedSecondKey")
            assertEquals(
                expectedSecondSize,
                groupedBy[expectedSecondKey]?.size,
                "Expected $expectedSecondSize lists for $expectedSecondKey, got ${groupedBy[expectedSecondKey]?.size} instead"
            )
        }

        if (spread == Spread.ALL_IN_LAST)
            assertEquals(expectedFirstKey, data.last().size, "Last list should have $expectedFirstKey elements, has ${data.last().size} instead")

        if (spread == Spread.ALL_IN_FIRST)
            assertEquals(expectedFirstKey, data.first().size, "First list should have $expectedFirstKey elements, has ${data.last().size} instead")

        if (spread == Spread.ADDITIONAL_CHUNK)
            assertEquals(expectedSecondKey, data.last().size, "Last list should have $expectedSecondKey elements, has ${data.last().size} instead")
        return data
    }

    @Test
    fun testEvenSpreadWithoutRemainder(){
        noRemainder(Spread.EVEN, 55, 55)
    }

    @Test
    fun testAdditionalSpreadWithoutRemainder(){
        noRemainder(Spread.ADDITIONAL_CHUNK)
    }

    @Test
    fun testAllInFirstSpreadWithoutRemainder(){
        noRemainder(Spread.ALL_IN_FIRST)
    }

    @Test
    fun testAllInLastSpreadWithoutRemainder(){
        noRemainder(Spread.ALL_IN_LAST)
    }

    @Test
    fun testEvenSpreadWithRemainder(){
        withRemainder(53,5, Spread.EVEN)
        withRemainder(33,2, Spread.EVEN)
    }

    @Test
    fun testAdditionalSpreadWithRemainder(){
        withRemainder(53, 5, Spread.ADDITIONAL_CHUNK)
        withRemainder(73, 4, Spread.ADDITIONAL_CHUNK)
    }

    @Test
    fun testAllInFirstSpreadWithRemainder(){
        assertAll(
            {withRemainder(53,5, Spread.ALL_IN_FIRST)},
            {withRemainder(87,9, Spread.ALL_IN_FIRST)},
            )
    }

    @Test
    fun testAllInLastSpreadWithRemainder(){
        withRemainder(53, 5, Spread.ALL_IN_LAST)
        withRemainder(101, 6, Spread.ALL_IN_LAST)
    }

    @Test
    fun testIgnoredSpreadWithRemainder(){
        noRemainder(Spread.IGNORED, 53, 5)
    }

    @Test
    fun testIgnoreSpreadWithoutRemainder(){
        noRemainder(Spread.IGNORED, 50, 5)
    }
}