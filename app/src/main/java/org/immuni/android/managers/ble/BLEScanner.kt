package org.immuni.android.managers.ble

import android.bluetooth.le.*
import android.os.ParcelUuid
import android.util.Log
import com.bendingspoons.oracle.Oracle
import de.fraunhofer.iis.DistanceEstimate
import de.fraunhofer.iis.Estimator
import de.fraunhofer.iis.Measurement
import de.fraunhofer.iis.ModelProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.immuni.android.api.oracle.model.AscoltoMe
import org.immuni.android.api.oracle.model.AscoltoSettings
import org.immuni.android.db.AscoltoDatabase
import org.immuni.android.db.entity.BLEContactEntity
import org.immuni.android.managers.BluetoothManager
import org.immuni.android.managers.BtIdsManager
import org.koin.core.KoinComponent
import org.koin.core.inject
import kotlin.random.Random

class BLEScanner: KoinComponent {
    private val bluetoothManager: BluetoothManager by inject()
    private val database: AscoltoDatabase by inject()
    private val btIdsManager: BtIdsManager by inject()
    private val oracle: Oracle<AscoltoSettings, AscoltoMe> by inject()
    private val id = Random.nextInt(0, 1000)
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var myScanCallback = MyScanCallback()

    // Distance estimator
    private val distanceEstimator: Estimator by inject()

    fun stop() {
        bluetoothLeScanner.stopScan(myScanCallback)
    }

    fun start() {
        bluetoothLeScanner = bluetoothManager.adapter().bluetoothLeScanner
        val filter = listOf(
            ScanFilter.Builder().apply {
                val serviceUuidString = CGAIdentifiers.ServiceDataUUIDString
                val serviceUuidMaskString = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"
                val parcelUuid = ParcelUuid.fromString(serviceUuidString)
                val parcelUuidMask = ParcelUuid.fromString(serviceUuidMaskString)
                setServiceUuid(parcelUuid, parcelUuidMask)
            }.build()
        )
        bluetoothLeScanner.startScan(
            filter,
            ScanSettings.Builder().apply {
                // with report delay the distance estimator doesn't work
                //setReportDelay(3000)
                setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            }.build(),
            myScanCallback
        )
    }

    private fun calculateDistance(measurement: Measurement) {
        val list = distanceEstimator.push(measurement)
        list.forEach {
            Log.d("BLEScanner", "### Distance in meters between ${it.deviceId1} and ${it.deviceId2} = ${it.distance} meters")
        }
        val now = System.currentTimeMillis()
        storeResults(list.filter { now - it.timestamp < 5000 })
    }

    private var lastStoreTs = 0L
    private fun storeResults(list: List<DistanceEstimate>) {
        // store data once every X seconds
        // to prevent to much database inserts
        val now = System.currentTimeMillis()
        if(now - lastStoreTs < 5000) return
        lastStoreTs = now

        GlobalScope.launch {
            database.bleContactDao().insert(
                *list.map {
                    BLEContactEntity(
                        btId = it.deviceId2
                    )
                }.toTypedArray()
            )
        }
    }

    private fun processResults(results: List<ScanResult>) {

        val btIds = mutableListOf<String>()
        results.forEach { result ->

            val serviceId = ParcelUuid.fromString(CGAIdentifiers.ServiceDataUUIDString)
            val bytesData = result.scanRecord?.serviceData?.get(serviceId)
            val rssi = result.rssi
            //val txPower = result.txPower // API 26+
            bytesData?.let { bytes ->
                val scannedBtId = byteArrayToHex(bytes)
                scannedBtId?.let { btId ->
                    btIds.add(btId)

                    calculateDistance(Measurement(
                        System.currentTimeMillis(),
                        rssi.toFloat(),
                        btIdsManager.getCurrentBtId()?.id ?: "",
                        btId,
                        ModelProvider.MOBILE_DEVICE.DEFAULT
                    ))
                }
            }
        }

        Log.d("BLEScanner", "### SCAN RESULT id=$id ${btIds.joinToString()}")
    }

    inner class MyScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            processResults(results)
        }

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            processResults(listOf(result))
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("BLEScanner", "### onScanFailed id=$id $errorCode")
        }
    }
}

fun byteArrayToHex(a: ByteArray): String? {
    val sb = java.lang.StringBuilder(a.size * 2)
    for (b in a) sb.append(String.format("%02x", b))
    return sb.toString()
}