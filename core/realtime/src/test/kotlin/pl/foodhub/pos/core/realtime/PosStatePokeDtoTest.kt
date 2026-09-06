package pl.foodhub.pos.core.realtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosStatePokeDtoTest {
    @Test
    fun `maps each known resource to its RealtimeEvent`() {
        assertEquals(
            RealtimeEvent.OCCUPIED_TABLES,
            parsePosStatePoke("""{"resource":"occupied-tables","placeId":"p1"}"""),
        )
        assertEquals(
            RealtimeEvent.RECEIPT_ISSUED,
            parsePosStatePoke("""{"resource":"receipt-issued","placeId":"p1"}"""),
        )
        assertEquals(
            RealtimeEvent.INVOICE_ISSUED,
            parsePosStatePoke("""{"resource":"invoice-issued","placeId":"p1"}"""),
        )
        assertEquals(RealtimeEvent.ORDER_CREATED, parsePosStatePoke("""{"resource":"order-created","placeId":"p1"}"""))
    }

    @Test
    fun `an unrecognised resource is ignored instead of crashing`() {
        assertNull(parsePosStatePoke("""{"resource":"some-future-resource","placeId":"p1"}"""))
    }

    @Test
    fun `malformed JSON is ignored instead of crashing`() {
        assertNull(parsePosStatePoke("not json"))
    }
}
