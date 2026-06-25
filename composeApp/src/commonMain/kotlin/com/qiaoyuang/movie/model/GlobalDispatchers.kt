package com.qiaoyuang.movie.model

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.QualifierValue

sealed interface GlobalDispatchers : Qualifier {

    data object DEFAULT : GlobalDispatchers {
        override val value: QualifierValue = "GlobalDispatchers-Default"
    }

    data object IO : GlobalDispatchers {
        override val value: QualifierValue = "GlobalDispatchers-IO"
    }
}