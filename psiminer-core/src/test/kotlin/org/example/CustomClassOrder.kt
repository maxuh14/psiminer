package org.example

import org.junit.jupiter.api.ClassDescriptor
import org.junit.jupiter.api.ClassOrderer
import org.junit.jupiter.api.ClassOrdererContext

class CustomClassOrder : ClassOrderer {
    override fun orderClasses(context: ClassOrdererContext) {
        context.classDescriptors.sortWith(comparator)
    }

    private val comparator = Comparator<ClassDescriptor> { c1, c2 ->
        val index1 = config.indexOf(c1.testClass.name)
        val index2 = config.indexOf(c2.testClass.name)

        if (index1 == -1 && index2 == -1) 0
        else if (index1 == -1) 1
        else if (index2 == -1) -1
        else index1 - index2
    }

    private val config = listOf(
        "filter.ConstructorFilterTest",
        "filter.KotlinEmptyMethodsFilterTest",
        "filter.KotlinTreeSizeFilterTest",
        "filter.KotlinAnnotationFilterTest",
        "filter.JavaTreeSizeFilterTest",
        "filter.KotlinCodeLinesFilterTest",
        "filter.KotlinModifierFilterTest",
        "filter.JavaCodeLinesFilterTest",
        "filter.JavaModifierFilterTest",
        "filter.JavaEmptyMethodFilterTest",
        "filter.JavaAnnotationFilterTest",
        "psi.transformations.JavaHideLiteralsTransformationTest",
        "psi.transformations.typeresolve.JavaResolveTypeTransformationTest",
        "psi.transformations.excludenode.JavaExcludeWhiteSpaceTransformationTest",
        "psi.transformations.excludenode.JavaExcludeKeywordTransformationTest",
        "psi.transformations.excludenode.KotlinExcludeWhiteSpaceTransformationTest",
        "labelextractor.methodname.JavaMethodNameLabelExtractorTest",
        "labelextractor.methodname.KotlinMethodNameLabelExtractorTest",
        "CommonTest"
    )
}
