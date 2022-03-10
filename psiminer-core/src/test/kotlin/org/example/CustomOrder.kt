package org.example

import org.junit.jupiter.api.MethodDescriptor
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.MethodOrdererContext

class CustomOrder : MethodOrderer {
    override fun orderMethods(context: MethodOrdererContext) {
//        context.methodDescriptors.sortWith(comparator)
    }

    private val comparator = Comparator<MethodDescriptor> { m1, m2 ->
        val index1 = config.indexOf(m1.method.name)
        val index2 = config.indexOf(m2.method.name)

        if (index1 == -1 && index2 == -1) 0
        else if (index1 == -1) 1
        else if (index2 == -1) -1
        else index1 - index2
    }

    private val config = listOf(
        "scriptWithProperties", "scriptWithImports", "scriptWithExtendedClasspath", "simpleEval"
    )
}
