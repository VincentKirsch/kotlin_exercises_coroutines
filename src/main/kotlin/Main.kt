package com.example

import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

const val DEBUG_ENABLED = true
const val LAZY = false

enum class Spread{
    EVEN,               // Remainder spread evenly on the first n chunks
    ALL_IN_FIRST,       // Remainder all added to first chunk
    ALL_IN_LAST,        // Remainder all added to last chunk
    ADDITIONAL_CHUNK,   // Remainder all added to an additional chunk
    IGNORED,            // Remainder is discarded
}

fun log (input: String, isDebug: Boolean = false) {
    if (!isDebug || DEBUG_ENABLED) {
        println(buildString {
            append("[")
            append(Thread.currentThread().name)
            append("] ")
            append("[")
            append(LocalDateTime.now().format(formatter))
            append("] ")
            append(input)
        })
    }
}

fun main(): Unit = runBlocking{

    val data = prepareData(53, 5, Spread.ADDITIONAL_CHUNK)

    log("Launching ${data.size} coroutines...")
    val dl: List<Deferred<List<Double>>> = data.map {
        async(start = if(LAZY) CoroutineStart.LAZY else CoroutineStart.DEFAULT) { compute(it) }
    }.toList()
    log("Launched...")

    val delay: Long = 5000

    log("Living my life, sleeping for $delay millis...")
    delay(delay)
    log("Woke up!")

    log("I actually need the results now, calling awaitAll")
    val res = dl.awaitAll()
    log("Received ${res.size} result chunks")
    res.map { log("First square: ${it.first()} - last square: ${it.last()}") }
}

suspend fun compute(data: List<Int>): List<Double> {
    log ("Computing on ${data.size} elements ${data.first()} .. ${data.last()}")
    val fakeDelay = Random.nextLong(500,2500)
    log ("Lots of work, gonna take me $fakeDelay milliseconds !")
    delay(fakeDelay)
    val result: List<Double> = data.map{ it.toDouble() * it.toDouble() }
    log( "Compute finished - last square: " + result.last())
    return result
}

/**
 * Given a number of elements to prepare and a number of chunks, returns a list of chunks.
 * The data are integers from 1 to max.
 * @param max: the total amount of elements
 * @param chunks: the number of chunks the elements should be placed in
 * @param spread: how to deal with the remainder if the number of elements cannot be placed in exactly the number of chunks
 * They can be either:
 * <ul>
 *     <li>Spread.EVEN: spread evenly across the other chunks (default behaviour),1 element more per chunk until all spread</li>
 *     <li>Spread.ADDITIONAL_CHUNK: laced together in an additional chunk, while the rest of the chunks will have the expected size.<b>Please note that it means you weill receive 1 more chunk than requested!</b></li>
 *     <li>Spread.ALL_IN_FIRST: all added to the first chunk, while the remaining chunks will have the expected size</li>
 *     <li>Spread.ALL_IN_LAST: all added to the last chunk, while the remaining chunks will have the expected size</li>
 */
fun prepareData(max: Int, chunks: Int, spread: Spread = Spread.EVEN): List<List<Int>> {

    require(chunks > 0) { "At least 1 chunk is required" }
    require(max > 0) { "At least one element is required" }
    require(chunks <= max) { "Can't ask for more chunks than max elements" }

    log("Going to prepare $max elements in $chunks chunks; possible remainder treatment: $spread")
    val result = mutableListOf<List<Int>>()

    // Could change if spread == Spread.ADDITIONAL_CHUNCK
    var actualChunks = chunks

    val chunkSize = max / actualChunks
    log ("Chunk size: $chunkSize", true)

    val remainder = max % actualChunks
    log ("Remainder: $remainder", true)

    if (remainder > 0 && spread == Spread.ADDITIONAL_CHUNK)
        actualChunks++

    // Create all the data
    val data = (1..max).toList()
    val baseChunkSize = max / chunks

    var nextSliceStart = 0

    repeat(actualChunks) { index ->
        val actualChunkSize = when (spread) {
            Spread.EVEN -> baseChunkSize + if (index < remainder) 1 else 0
            Spread.ADDITIONAL_CHUNK -> if (index == chunks) remainder else baseChunkSize
            Spread.ALL_IN_FIRST -> if (index == 0) baseChunkSize + remainder else baseChunkSize
            Spread.ALL_IN_LAST -> if (index == chunks - 1) baseChunkSize + remainder else baseChunkSize
            Spread.IGNORED -> baseChunkSize // discards remainder
        }

        log("Chunk size: $actualChunkSize - slicing from $nextSliceStart to ${(nextSliceStart+actualChunkSize)} excluded", true)
        val chunk = data.slice(nextSliceStart until (nextSliceStart + actualChunkSize))
        result.add(chunk)

        nextSliceStart += actualChunkSize
        log("Next slice start: $nextSliceStart", true)
        log("Chunk's first element: ${chunk.first()} ; last: ${chunk.last()}", true)
    }

    // Prepare and display a nice message with the repartition of the data
    result.let{
        val output = result
            // Get a map of chunk size -> chunk. There will be 1 (no remainder) or 2 (a remainder) keys in that map.
            .groupBy { it.size }
            .map { "${it.value.size} chunks of ${it.key} elements" }
            // Lots of gymnastics to replace the last comma by an 'and'
            .joinToString().let{
                if (it.contains(", "))
                    it.replaceRange(it.lastIndexOf(", ")..(it.lastIndexOf(",") + 1)," and ")
                else
                    it
            }

        // Data prepared in n chunks of x elements and m chunks of y elements
        log("Data prepared in $output")
    }
    println()

    return result
}
