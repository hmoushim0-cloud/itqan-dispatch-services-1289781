package sa.factory.itqandispatchservi.data.repository

import sa.factory.itqandispatchservi.domain.repository.ItqanDispatchRepository
import sa.factory.itqandispatchservi.hardware.HardwareSensorDriver
import sa.factory.itqandispatchservi.model.ItqanDispatchItem
import sa.factory.itqandispatchservi.model.ItqanDispatchOrder
import sa.factory.itqandispatchservi.model.ItqanDispatchTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ItqanDispatchRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : ItqanDispatchRepository {

    private val _catalog = MutableStateFlow<List<ItqanDispatchItem>>(listOf(
        ItqanDispatchItem("1", "تحليل ثلاثي الفرضيات", "تحليل إحصائي متكامل لثلاثة فروض بحثية مع قياس الصدق والثبات", 850.0, "معتمد", "تحليل إحصائي"),
        ItqanDispatchItem("2", "تتبع ميداني مباشر", "دمج بيانات GPS في الوقت الفعلي مع نتائج التحليل الإحصائي", 420.0, "فوري", "قياس الصدق والثبات"),
        ItqanDispatchItem("3", "معالجة مدفوعات NFC", "قبول مدفوعات معايير Mada EMV L2 عبر قارئ NFC مدمج", 280.0, "جديد", "تتبع GPS")
    ))
    override val catalogFlow: Flow<List<ItqanDispatchItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<ItqanDispatchItem>>(emptyList())
    override val activeCart: StateFlow<List<ItqanDispatchItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<ItqanDispatchTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            ItqanDispatchTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: ItqanDispatchItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: ItqanDispatchItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): ItqanDispatchOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = ItqanDispatchOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
