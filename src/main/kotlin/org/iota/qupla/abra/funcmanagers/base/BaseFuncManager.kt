package org.iota.qupla.abra.funcmanagers.base

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap

import org.iota.qupla.abra.block.AbraBlockBranch
import org.iota.qupla.abra.block.AbraBlockLut
import org.iota.qupla.qupla.context.QuplaToAbraContext

open class BaseFuncManager(var funcName: String) {
    var branch: AbraBlockBranch? = null
    var context: QuplaToAbraContext? = null
    var instances = HashMap<String, AbraBlockBranch?>()
    var lut: AbraBlockLut? = null
    var name: String? = null
    private var reuse: Int = 0
    var size: Int = 0
    var sorted = mutableListOf<Int>()

    protected open fun createBaseInstances() {}

    protected fun createBestFuncFunc() {
        // determine factors that make up this one as integer array
        var exist = size / 2
        if (exist * 2 == size && sorted.contains(exist)) {
            branch = generateFuncFunc(size, arrayOf(exist, exist))
            return
        }

        exist = size / 3
        if (exist * 3 == size && sorted.contains(exist)) {
            branch = generateFuncFunc(size, arrayOf(exist, exist, exist))
            return
        }

        var highest = sorted.size - 1
        var remain = size
        val factors = ArrayList<Int>()
        while (remain > 0) {
            var value = sorted[highest]
            while (value > remain) {
                highest--
                value = sorted[highest]
            }
            remain -= value
            factors.add(value)
        }

        branch = generateFuncFunc(size, factors.toTypedArray())
    }

    protected open fun createInstance() {
        branch = AbraBlockBranch()
        branch!!.name = name
        branch!!.size = size
    }

    protected fun createStandardBaseInstances() {
        generateLut()

        if (lut == null) {
            return
        }

        // for 1, 2, and 3 trit sizes functions directly use LUT
        for (i in 1..3) {
            generateLutFunc(i)
        }

        // for higher powers of 3 and their double sizes
        // functions are composed of smaller functions
        var i = 3
        while (i <= 2187) {
            // TODO Is generateFuncFunc always not null here 'generateFuncFunc(...)!!'
            saveBranch(generateFuncFunc(i * 2, arrayOf(i, i))!!)
            saveBranch(generateFuncFunc(i * 3, arrayOf(i, i, i))!!)
            i *= 3
        }
    }

    fun find(context: QuplaToAbraContext, size: Int): AbraBlockBranch? {
        this.context = context
        this.size = size
        name = funcName + SEPARATOR + size
        return findInstance()
    }

    protected fun findInstance(): AbraBlockBranch? {
        val instance = instances[name]
        if (instance != null) {
            reuse++
            return instance
        }

        if (instances.size == 0) {
            createBaseInstances()
            if (instances.size != 0) {
                // retry find now that we're initialized
                return findInstance()
            }
        }

        createInstance()
        // TODO Is branch always not null here 'branch!!'
        return saveBranch(branch!!)
    }

    protected open fun generateFuncFunc(inputSize: Int, inputSizes: Array<Int>): AbraBlockBranch? {
        // generate function that use smaller functions
        return null
    }

    protected open fun generateLut() {}

    protected open fun generateLutFunc(inputSize: Int) {
        // generate function that use LUTs
    }

    protected fun saveBranch(branch: AbraBlockBranch): AbraBlockBranch? {
        instances.set(branch.name?: "", branch)
        sorted.add(branch.size)
        Collections.sort(sorted)
        context?.abraModule?.addBranch(branch)
        return branch
    }

    companion object {
        val SEPARATOR = "_"
    }
}
