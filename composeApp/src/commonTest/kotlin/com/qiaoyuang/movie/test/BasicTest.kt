package com.qiaoyuang.movie.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
open class BasicTest {

    protected val mainThreadSurrogate = StandardTestDispatcher()

    @BeforeTest
    open fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @AfterTest
    open fun tearDown() {
        Dispatchers.resetMain()
    }
}