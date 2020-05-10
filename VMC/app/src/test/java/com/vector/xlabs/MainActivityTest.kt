package com.vector.xlabs

import org.junit.Test
import java.lang.StringBuilder
import java.util.*


class MainActivityTest {

    @Test
    fun uuidToHexTest() {
        val hex = "1B:C5:D5:A5:02:00:60:A7:E3:11:4B:3D:F0:FF:2F:91".split(":").reversed().joinToString("")
        val uuid: String = "912ffff0-3d4b-11e3-a760-0002a5d5c51b".toUpperCase().replace("-","")


//        println(UUID.nameUUIDFromBytes(byteArrayOf(0x00, 0x00)))
        println(hex)
        print(uuid)



        assert(hex == uuid)


    }
}