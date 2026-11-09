package eu.whiskrkit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AttributeStoreTest {

    private val store = AttributeStore()

    @Test
    fun `set stores each supported type`() {
        store.set("s", AttributeValue.Text("pro"))
        store.set("i", AttributeValue.Integer(3))
        store.set("d", AttributeValue.Decimal(1.5))
        store.set("b", AttributeValue.Bool(true))

        assertEquals(
            mapOf(
                "s" to AttributeValue.Text("pro"),
                "i" to AttributeValue.Integer(3),
                "d" to AttributeValue.Decimal(1.5),
                "b" to AttributeValue.Bool(true),
            ),
            store.snapshot(),
        )
    }

    @Test
    fun `setting an existing key overwrites it`() {
        store.set("plan", AttributeValue.Text("free"))
        store.set("plan", AttributeValue.Text("pro"))
        assertEquals(mapOf("plan" to AttributeValue.Text("pro")), store.snapshot())
    }

    @Test
    fun `keys are trimmed and blank keys ignored`() {
        store.set("  plan ", AttributeValue.Text("pro"))
        store.set("   ", AttributeValue.Text("x"))
        assertEquals(mapOf("plan" to AttributeValue.Text("pro")), store.snapshot())
    }

    @Test
    fun `setAll merges and keeps existing keys`() {
        store.set("a", AttributeValue.Integer(1))
        store.setAll(mapOf("b" to "two", "c" to 3, "a" to 10L))
        assertEquals(
            mapOf(
                "a" to AttributeValue.Integer(10),
                "b" to AttributeValue.Text("two"),
                "c" to AttributeValue.Integer(3),
            ),
            store.snapshot(),
        )
    }

    @Test
    fun `setAll converts every supported type and skips unsupported ones`() {
        store.setAll(
            mapOf(
                "s" to "x",
                "bool" to false,
                "int" to 1,
                "long" to 2L,
                "double" to 2.5,
                "float" to 1.5f,
                "list" to listOf(1),
                "nested" to mapOf("a" to 1),
                "date" to java.util.Date(0),
            ),
        )
        assertEquals(
            setOf("s", "bool", "int", "long", "double", "float"),
            store.snapshot().keys,
        )
        assertEquals(AttributeValue.Decimal(1.5), store.snapshot().getValue("float"))
    }

    @Test
    fun `float keeps its decimal form`() {
        assertEquals(AttributeValue.Decimal(1.1), AttributeValue.of(1.1f))
    }

    @Test
    fun `non finite numbers are rejected`() {
        store.setAll(mapOf("nan" to Double.NaN, "inf" to Double.POSITIVE_INFINITY, "ok" to 1.0))
        assertEquals(setOf("ok"), store.snapshot().keys)
        assertEquals(null, AttributeValue.of(Double.NEGATIVE_INFINITY))
        assertEquals(null, AttributeValue.of(Float.NaN))
    }

    @Test
    fun `remove deletes one key and ignores unknown keys`() {
        store.set("a", AttributeValue.Integer(1))
        store.set("b", AttributeValue.Integer(2))
        store.remove("a")
        store.remove("missing")
        assertEquals(setOf("b"), store.snapshot().keys)
    }

    @Test
    fun `clear empties the store`() {
        store.set("a", AttributeValue.Integer(1))
        store.clear()
        assertTrue(store.snapshot().isEmpty())
    }

    @Test
    fun `snapshot is not affected by later changes`() {
        store.set("plan", AttributeValue.Text("free"))
        val snapshot = store.snapshot()
        store.set("plan", AttributeValue.Text("pro"))
        store.clear()
        assertEquals(mapOf("plan" to AttributeValue.Text("free")), snapshot)
    }

    @Test
    fun `concurrent writers lose no update`() {
        val threads = 8
        val perThread = 200
        val pool = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threads)
        repeat(threads) { t ->
            pool.execute {
                start.await()
                repeat(perThread) { i -> store.set("k-$t-$i", AttributeValue.Integer(i.toLong())) }
                done.countDown()
            }
        }
        start.countDown()
        assertTrue(done.await(30, TimeUnit.SECONDS))
        pool.shutdown()
        assertEquals(threads * perThread, store.snapshot().size)
    }

    @Test
    fun `concurrent reads while writing never fail`() {
        val pool = Executors.newFixedThreadPool(4)
        val futures = (0 until 4).map { t ->
            pool.submit {
                repeat(500) { i ->
                    if (t % 2 == 0) store.set("k$i", AttributeValue.Bool(true)) else store.snapshot().size
                    if (i % 100 == 0) store.remove("k$i")
                }
            }
        }
        futures.forEach { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()
    }
}
