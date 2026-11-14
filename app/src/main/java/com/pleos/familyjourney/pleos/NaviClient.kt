package com.pleos.familyjourney.pleos

import com.pleos.familyjourney.data.GeoPoint
import com.pleos.familyjourney.data.Place
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** 화면이 보는 내비 상태. 실제/가짜 구현 모두 이 형태로 내보낸다. */
data class NaviState(
    val connected: Boolean = false,
    val routing: Boolean = false,
    val destinationName: String? = null,
    /** 남은 시간(초). 실제 SDK: onDrivingInfo → destination.duration */
    val remainingSec: Int? = null,
    val remainingMeters: Int? = null,
    /** 0~1, 출발 후 진행률 */
    val progress: Float = 0f,
    val location: GeoPoint? = null,
    /** 차속(km/h). NaviHelper에는 없고 Vehicle SDK에서 받아야 함 → 실제 구현에서는 null */
    val speedKmh: Int? = null,
    /** 아직 지나지 않은 경유지 이름 */
    val upcomingWaypoints: List<String> = emptyList(),
    val arrived: Boolean = false,
    val lastEvent: String = "",
)

/** NaviHelper 래퍼. 화면/ViewModel은 이 인터페이스만 쓴다. */
interface NaviClient {
    val state: StateFlow<NaviState>
    fun initialize()
    fun release()

    /** 목적지 + 순서가 정해진 경유지(최대 2개)를 한 번에 요청 (requestRouteWithWaypoints) */
    fun requestRoute(destination: Place, waypoints: List<Place>)

    /** 경유지 추가. Pleos 내비는 남은 경유지가 최대 2개(FIRST, SECOND)라서 꽉 차면 false */
    fun addWaypoint(place: Place): Boolean

    fun cancelRoute()

    companion object {
        const val MAX_WAYPOINTS = 2
    }
}

/** 시연용 조작(빨리감기 등). 가짜 구현에만 있고 실제 모드에서는 null. */
interface TripSimulator {
    val running: StateFlow<Boolean>
    fun setRunning(run: Boolean)
    fun skipMinutes(minutes: Int)
    fun jumpToArrival()
    fun reset()
}

// ---------------------------------------------------------------------------------------------
// 가짜 구현 (지금은 이것만 사용): 실제 주행 없이 2시간 여행을 몇 분 안에 재생한다.
// ---------------------------------------------------------------------------------------------

class FakeNaviClient(
    private val scope: CoroutineScope,
    private val start: GeoPoint = GeoPoint(37.5663, 126.9779), // 서울시청
) : NaviClient, TripSimulator {

    private data class Target(val name: String, val point: GeoPoint, val isWaypoint: Boolean)

    private val _state = MutableStateFlow(NaviState())
    override val state: StateFlow<NaviState> = _state.asStateFlow()

    private val _running = MutableStateFlow(false)
    override val running: StateFlow<Boolean> = _running.asStateFlow()

    private var current = start
    private var targets = mutableListOf<Target>()
    private var traveledKm = 0.0
    private var ticker: Job? = null

    override fun initialize() {
        _state.update { it.copy(connected = true, location = current, lastEvent = "가짜 내비 연결됨") }
    }

    override fun release() {
        ticker?.cancel()
        _state.update { it.copy(connected = false) }
    }

    override fun requestRoute(destination: Place, waypoints: List<Place>) {
        targets = (waypoints.take(NaviClient.MAX_WAYPOINTS).map { Target(it.name, it.point, true) } +
            Target(destination.name, destination.point, false)).toMutableList()
        traveledKm = 0.0
        current = start
        publish(event = "경로 요청: ${destination.name} (경유 ${waypoints.size}곳)", destination = destination.name)
        setRunning(true)
    }

    override fun addWaypoint(place: Place): Boolean {
        val upcoming = targets.count { it.isWaypoint }
        if (targets.isEmpty() || upcoming >= NaviClient.MAX_WAYPOINTS) {
            publish(event = "경유지 추가 실패: 경유지는 최대 ${NaviClient.MAX_WAYPOINTS}개")
            return false
        }
        // 서→동 경로라서 경도 순으로 끼워 넣으면 자연스러운 순서가 된다.
        val waypoint = Target(place.name, place.point, true)
        val index = targets.indexOfFirst { it.isWaypoint.not() || it.point.lng > place.point.lng }
            .let { if (it < 0) targets.lastIndex else it }
        targets.add(index, waypoint)
        publish(event = "경유지 추가 → 재탐색: ${place.name}")
        return true
    }

    override fun cancelRoute() {
        targets.clear()
        setRunning(false)
        publish(event = "경로 취소")
    }

    override fun setRunning(run: Boolean) {
        val canRun = run && targets.isNotEmpty()
        _running.value = canRun
        ticker?.cancel()
        if (canRun) {
            ticker = scope.launch {
                while (isActive && targets.isNotEmpty()) {
                    delay(TICK_MS)
                    advance(SIM_SEC_PER_TICK)
                }
                _running.value = false
                publish()
            }
        }
        publish()
    }

    override fun skipMinutes(minutes: Int) = advance(minutes * 60)

    override fun jumpToArrival() = advance(Int.MAX_VALUE / 2)

    override fun reset() {
        ticker?.cancel()
        targets.clear()
        current = start
        traveledKm = 0.0
        _running.value = false
        _state.value = NaviState(connected = true, location = current, lastEvent = "시연 초기화")
    }

    private fun advance(simSec: Int) {
        var km = simSec * SPEED_KMH / 3600.0
        var event: String? = null
        while (km > 0 && targets.isNotEmpty()) {
            val next = targets.first()
            val legKm = roadKm(current, next.point)
            if (km >= legKm) {
                km -= legKm
                traveledKm += legKm
                current = next.point
                targets.removeAt(0)
                event = if (next.isWaypoint) "경유지 도착: ${next.name}" else "목적지 도착: ${next.name}"
                if (!next.isWaypoint) {
                    _running.value = false
                    ticker?.cancel()
                    _state.update { it.copy(arrived = true) }
                }
            } else {
                val ratio = km / legKm
                current = GeoPoint(
                    current.lat + (next.point.lat - current.lat) * ratio,
                    current.lng + (next.point.lng - current.lng) * ratio,
                )
                traveledKm += km
                km = 0.0
            }
        }
        publish(event = event)
    }

    private fun publish(event: String? = null, destination: String? = null) {
        var remainingKm = 0.0
        var from = current
        targets.forEach { remainingKm += roadKm(from, it.point); from = it.point }
        val total = traveledKm + remainingKm
        val moving = _running.value && targets.isNotEmpty()
        _state.update {
            it.copy(
                routing = targets.isNotEmpty(),
                destinationName = destination ?: it.destinationName,
                remainingSec = if (targets.isEmpty() && !it.arrived) null else (remainingKm / SPEED_KMH * 3600).toInt(),
                remainingMeters = (remainingKm * 1000).toInt(),
                progress = if (total > 0) (traveledKm / total).toFloat() else it.progress,
                location = current,
                speedKmh = if (moving) SPEED_KMH.toInt() else 0,
                upcomingWaypoints = targets.filter { t -> t.isWaypoint }.map { t -> t.name },
                arrived = if (destination != null) false else it.arrived,
                lastEvent = event ?: it.lastEvent,
            )
        }
    }

    companion object {
        private const val TICK_MS = 1000L
        /** 1초마다 가상 20초 진행 → 2시간 30분 여행이 약 7분 30초 */
        private const val SIM_SEC_PER_TICK = 20
        private const val SPEED_KMH = 85.0
        /** 직선거리 → 도로거리 보정 */
        private const val ROAD_FACTOR = 1.35

        fun roadKm(a: GeoPoint, b: GeoPoint) = haversineKm(a, b) * ROAD_FACTOR

        fun haversineKm(a: GeoPoint, b: GeoPoint): Double {
            val r = 6371.0
            val dLat = Math.toRadians(b.lat - a.lat)
            val dLng = Math.toRadians(b.lng - a.lng)
            val h = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
            return 2 * r * asin(sqrt(h))
        }
    }
}
