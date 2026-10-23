package com.odom.motivationlocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DailyNotificationSchedulerTest {

    private val minute = 60 * 1000L
    private val hour = 60 * minute

    private fun at(hourOfDay: Int, minuteOfHour: Int): Long =
        Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 23, hourOfDay, minuteOfHour, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun nextTarget_laterToday() {
        assertEquals(at(10, 0), DailyNotificationScheduler.nextTargetMillis(at(9, 0), 10, 0))
    }

    @Test
    fun nextTarget_alreadyPassed_movesToTomorrow() {
        val target = DailyNotificationScheduler.nextTargetMillis(at(11, 0), 10, 0)
        assertEquals(at(10, 0) + 24 * hour, target)
    }

    @Test
    fun nextTarget_exactlyNow_movesToTomorrow() {
        // 알림을 띄운 직후 재예약할 때 같은 시각으로 다시 걸리면 안 된다
        val target = DailyNotificationScheduler.nextTargetMillis(at(10, 0), 10, 0)
        assertEquals(at(10, 0) + 24 * hour, target)
    }

    @Test
    fun nextAlarm_closeToTarget_usesTargetItself() {
        val now = at(9, 55)
        assertEquals(at(10, 0), DailyNotificationScheduler.nextAlarmMillis(now, at(10, 0)))
    }

    @Test
    fun nextAlarm_farFromTarget_worstCaseDelayStillBeforeFinalApproach() {
        val now = at(10, 1)
        val target = at(10, 0) + 24 * hour
        val alarm = DailyNotificationScheduler.nextAlarmMillis(now, target)
        // 권한 없는 알람이 최대로 늦게 울리는 시각(요청 + 남은 시간 × 0.75)
        val latest = alarm + ((alarm - now) * 0.75).toLong()
        assertTrue(alarm > now)
        assertTrue(latest <= target - DailyNotificationScheduler.FINAL_APPROACH_MILLIS)
    }

    @Test
    fun nextAlarm_chainConvergesToTarget() {
        val target = at(10, 0) + 24 * hour
        var now = at(10, 1)
        var steps = 0
        while (true) {
            val alarm = DailyNotificationScheduler.nextAlarmMillis(now, target)
            steps++
            if (alarm == target) break
            now = alarm // 제때 울렸다고 가정 (가장 많은 단계가 필요한 경우)
        }
        assertTrue("steps=$steps", steps <= 10)
    }
}
