package com.qiaoyuang.movie.test

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
open class BasicTest {

    @BeforeTest
    @OptIn(DelicateCoroutinesApi::class)
    open fun setUp() {
        val mainThreadSurrogate = newSingleThreadContext("UI thread")
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @AfterTest
    open fun testDown() {
        Dispatchers.resetMain()
    }
}