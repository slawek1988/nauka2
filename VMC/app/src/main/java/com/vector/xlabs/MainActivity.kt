package com.vector.xlabs

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    val servicesList = MutableLiveData<List<BluetoothGattService>>()
    val serviceToCharMap =
        MutableLiveData<Map<BluetoothGattService, List<BluetoothGattCharacteristic>>>()

    val deviceList = mutableListOf<String>()
    var vectorAddress: String? = null
    var scanResult: ScanResult? = null
    var btGatt: BluetoothGatt? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onResume() {
        super.onResume()
        servicesList.observe(this, androidx.lifecycle.Observer {
//            toast("${it.map { it.uuid.toString() }}")
            serviceToCharMap.value = it
                .map { it to it.characteristics }
                .toMap()
        })

        serviceToCharMap.observe(this, androidx.lifecycle.Observer {
            val mapt = it
            println(mapt)

//            val serialConfigChar = it.values
//                .flatMap { it }
//                .firstOrNull { it.uuid.toString() == "912ffff2-3d4b-11e3-a760-0002a5d5c51b" }

            val interfaceAddress = it.values
                .flatMap { it }
                .firstOrNull { it.uuid.toString() == "912ffff1-3d4b-11e3-a760-0002a5d5c51b" }


            val a = btGatt?.setCharacteristicNotification(interfaceAddress, true)
            if (a == true) println("SOMETHING set Characteristic notification")
            Thread.sleep(100)
            interfaceAddress?.descriptors?.firstOrNull()?.let {

                val b = it.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                if (b == true) println("SOMETHING set value of descriptor to ${BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE}")

                val c = btGatt?.writeDescriptor(it)
                if (c == true) println("SOMETHING write descriptor successful")
            }

            Thread.sleep(500)

            println(UUID.nameUUIDFromBytes(byteArrayOf(0x01, 0x00)))


//            UUID.nameUUIDFromBytes(
//                byteArrayOf(
//                    0x18,
//                    0x1D,
//                    0x00,
//                    0x1B,
//                    0xC5,
//                    0xD5,
//                    0xA5,
//                    0x02,
//                    0x00,
//                    0x60,
//                    0xA7,
//                    0xE3,
//                    0x11,
//                    0x4B,
//                    0x3D,
//                    0xF1,
//                    0xFF,
//                    0x2F,
//                    0x91
//                )
//            )


            interfaceAddress?.value = byteArrayOf(
                0xaa.toByte(),
                0x88.toByte(),
                0x04.toByte(),
                0x01.toByte(),
                0x00.toByte(),
                0x00.toByte(),
                0x45.toByte(),
                0x9d.toByte()

            )
            btGatt?.writeCharacteristic(interfaceAddress)


//            val note = mapt.values.flatMap { it }
//                .firstOrNull { it.uuid.toString() == "912ffff3-3d4b-11e3-a760-0002a5d5c51b" }

//            val notenabled = btGatt?.setCharacteristicNotification(interfaceAddress, true)
//            if (notenabled == true) {
//                toast("NOTIFICATIONS ENABLED")
//            }
//            interfaceAddress?.descriptors?.firstOrNull()?.let {
//            btGatt?.readDescriptor(it)}

//            if (btGatt?.readCharacteristic(serialConfigChar) == true) {
//                toast("Started reading serial port config characteristic")
//            }
//            note?.let {
//                btGatt?.readCharacteristic(it)
//            }

//            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
//            return false;
//        }
//            interfaceAddress?.descriptors?.firstOrNull()?.let {
//                if (btGatt?.readDescriptor(it) == true) {
//                    toast("Started to read interface descriptor")
//                } else {
//                    toast("Failed to read interface descriptor")
//                }
//            }
        })
    }

    @ExperimentalStdlibApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions() {

            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery();
            }
            bluetoothAdapter?.bluetoothLeScanner?.startScan(object : ScanCallback() {
                override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                    toast("On batch scan result ${results?.map { it.device.address }
                        ?.joinToString()}")
                    super.onBatchScanResults(results)
                }

                override fun onScanFailed(errorCode: Int) {
                    toast("On Scan Failed $errorCode")
                    super.onScanFailed(errorCode)
                }

                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    if (result?.device?.name?.toLowerCase()?.contains("vector") == true) {
                        toast("FOUND VECTOR")
                        scanResult = result
                        button.text = "VECTOR FOUND"
                        bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
                    }
                    super.onScanResult(callbackType, result)
                }
            })
        }

        button.setOnClickListener {

            if (btGatt != null) {
                val discStarted = btGatt?.discoverServices()
                toast("SER DISC started $discStarted")
                val ser =
                    btGatt?.getService(UUID.fromString("912ffff0-3d4b-11e3-a760-0002a5d5c51b"))
//                println("SOMETHING service ${ser?.uuid}")


            } else {
                scanResult?.let {
                    toast("Connecting gatt")
                    btGatt = it.device.connectGatt(this, false, btgcallback)
                    btGatt?.let { button.text = "Vector connected" }
                }
            }
        }
    }

    private fun checkPermissions(onOk: () -> Unit) {
        Dexter.withActivity(this@MainActivity)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()) {
                            onOk()
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
            .withErrorListener {
                toast(it.name)
            }
            .check()
    }

    private fun toast(name: String) {
        Toast.makeText(this, name, Toast.LENGTH_SHORT).show()
    }

    private val btgcallback = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            println("SOMETHING - characteristic changed: ")
            characteristic?.value
                ?.forEachIndexed { a, b ->
                    println("SOMETHING -> $a -> $b")
                }
            super.onCharacteristicChanged(gatt, characteristic)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            println("SOMETHING char read")
            super.onCharacteristicRead(gatt, characteristic, status)
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            println("SOMETHING mtu changed")
            super.onMtuChanged(gatt, mtu, status)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            println("SOMETHING read remote rssi")
            super.onReadRemoteRssi(gatt, rssi, status)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            println("SOMETHING char write")
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            println("SOMETHING connection state changed $status $newState")
            println("SOMETHING " + gatt!!.services.map { it.uuid.toString() }.joinToString())
            super.onConnectionStateChange(gatt, status, newState)
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            println("SOMETHING desc read")
            super.onDescriptorRead(gatt, descriptor, status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            println("SOMETHING on Descriptor WRITE")

            super.onDescriptorWrite(gatt, descriptor, status)
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            println("SOMETHING on PHY READ")
            super.onPhyRead(gatt, txPhy, rxPhy, status)
        }

        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            println("SOMETHING on phy update")

            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
            println("SOMETHING ON RELIABLE WEITE COMPLETED")

            super.onReliableWriteCompleted(gatt, status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            btGatt = gatt
            servicesList.postValue(gatt?.services ?: emptyList())
            println("SOMETHING services discovered, status $status  ${gatt?.services
                ?.map { it.uuid.toString() }
                ?.joinToString()}")
            super.onServicesDiscovered(gatt, status)
        }
    }
}