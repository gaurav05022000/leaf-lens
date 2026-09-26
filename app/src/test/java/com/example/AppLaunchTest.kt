package com.example

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.os.Build

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AppLaunchTest {

    @Test
    fun testAppLaunch() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
        activity.create().start().resume()
        println("App launched successfully without crashing")
    }
}
