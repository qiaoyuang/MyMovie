package com.qiaoyuang.movie.model

import platform.Foundation.NSLog

internal actual fun log(tag: String, message: String) = NSLog("%@: %@", tag, message)