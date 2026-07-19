package com.hibipho.localwatch

import org.junit.Test
import org.junit.Assert.*

class PrecisionTimeSyncTest {

    private fun calculateTruePlaybackPosition(
        hostPosition: Long, 
        hostTimestamp: Long, 
        localTimestamp: Long, 
        rtt: Long
    ): Long {
        val networkDelay = rtt / 2
        val timePassed = localTimestamp - hostTimestamp
        return hostPosition + timePassed + networkDelay
    }

    @Test
    fun testPerfectNetworkSync() {
        val hostPosition = 5000L // 5 seconds into the movie
        val hostTimestamp = 1600000000L
        val localTimestamp = 1600000100L // 100ms passed since host sent it
        val rtt = 40L // 40ms Round Trip Time

        val predictedPosition = calculateTruePlaybackPosition(hostPosition, hostTimestamp, localTimestamp, rtt)
        
        // Expected: 5000 + 100 + (40/2) = 5120
        assertEquals(5120L, predictedPosition)
        println("TEST 1 PASSED: Perfect Network Sync (Position: 5120ms)")
    }

    @Test
    fun testHighLatencySync() {
        val hostPosition = 3600000L // 1 hour into the movie
        val hostTimestamp = 1600000000L
        val localTimestamp = 1600001000L // 1000ms passed (slow network)
        val rtt = 800L // 800ms Round Trip Time

        val predictedPosition = calculateTruePlaybackPosition(hostPosition, hostTimestamp, localTimestamp, rtt)
        
        // Expected: 3600000 + 1000 + 400 = 3601400
        assertEquals(3601400L, predictedPosition)
        println("TEST 2 PASSED: High Latency Sync (Position: 3601400ms)")
    }
    
    @Test
    fun testNegativeDriftCorrection() {
        val hostPosition = 10000L
        val hostTimestamp = 1600000100L
        val localTimestamp = 1600000000L // Local clock is behind host clock by 100ms
        val rtt = 20L 

        val predictedPosition = calculateTruePlaybackPosition(hostPosition, hostTimestamp, localTimestamp, rtt)
        
        // Expected: 10000 + (-100) + 10 = 9910
        assertEquals(9910L, predictedPosition)
        println("TEST 3 PASSED: Clock Drift Correction (Position: 9910ms)")
    }
}
