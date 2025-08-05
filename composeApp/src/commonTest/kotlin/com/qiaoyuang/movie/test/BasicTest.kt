package com.qiaoyuang.movie.test

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
open class BasicTest {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @BeforeTest
    open fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @AfterTest
    open fun testDown() {
        mainThreadSurrogate.close()
        Dispatchers.resetMain()
    }
}