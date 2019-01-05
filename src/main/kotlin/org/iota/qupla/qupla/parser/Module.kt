package org.iota.qupla.qupla.parser

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import org.iota.qupla.exception.CodeException
import org.iota.qupla.qupla.context.QuplaAnyNullContext
import org.iota.qupla.qupla.expression.base.BaseExpr
import org.iota.qupla.qupla.statement.ExecStmt
import org.iota.qupla.qupla.statement.FuncStmt
import org.iota.qupla.qupla.statement.ImportStmt
import org.iota.qupla.qupla.statement.LutStmt
import org.iota.qupla.qupla.statement.TemplateStmt
import org.iota.qupla.qupla.statement.TypeStmt
import org.iota.qupla.qupla.statement.UseStmt

class Module : BaseExpr {

    var currentSource: Source? = null

    val execs = ArrayList<ExecStmt>()
    val funcs = ArrayList<FuncStmt>()
    val imports = ArrayList<ImportStmt>()
    val luts = ArrayList<LutStmt>()
    val modules = ArrayList<Module>()
    val sources = ArrayList<Source>()
    val templates = ArrayList<TemplateStmt>()
    val types = ArrayList<TypeStmt>()
    val uses = ArrayList<UseStmt>()

    constructor(name: String) {
        this.name = name
        BaseExpr.currentModule = this
    }

    constructor(subModules: Collection<Module>) {
        for (subModule in subModules) {
            funcs.addAll(subModule.funcs)
            imports.addAll(subModule.imports)
            luts.addAll(subModule.luts)
            types.addAll(subModule.types)
            execs.addAll(subModule.execs)
        }

        name = "{SINGLE_MODULE}"
    }

    private fun addReferencedModules(module: Module) {
        if (!modules.contains(module)) {
            modules.add(module)
        }

        for (referenced in module.modules) {
            if (!modules.contains(referenced)) {
                modules.add(referenced)
            }
        }
    }

    override fun analyze() {
        for (imp in imports) {
            imp.analyze()
            // TODO Is .importModule always not null here '.importModule!!.'
            addReferencedModules(imp.importModule!!)
        }

        for (type in types) {
            type.analyze()
        }

        for (lut in luts) {
            lut.analyze()
        }

        // first analyze all normal function signatures
        for (func in funcs) {
            if (func.use == null) {
                func.analyzeSignature()
            }
        }

        for (template in templates) {
            template.analyze()
        }

        // this will instantiate the templated types/functions
        for (use in uses) {
            use.analyze()
        }

        // now that we know all functions and their properties
        // we can finally analyze their bodies
        for (func in funcs) {
            func.analyze()
        }

        for (exec in execs) {
            exec.analyze()
        }

        // determine which functions short-circuit on any null parameter
        QuplaAnyNullContext().eval(this)
    }

    fun checkDuplicateName(items: ArrayList<out BaseExpr>, symbol: BaseExpr) {
        for (item in items) {
            if (item.name == symbol.name) {
                symbol.error("Already defined: " + symbol.name)
            }
        }
    }

    override fun clone(): BaseExpr {
        throw CodeException("clone WTF?")
    }

    fun entities(classId: Class<*>): ArrayList<out BaseExpr> {
        if (classId == FuncStmt::class.java) {
            return funcs
        }

        if (classId == LutStmt::class.java) {
            return luts
        }

        if (classId == TemplateStmt::class.java) {
            return templates
        }

        if (classId == TypeStmt::class.java) {
            return types
        }

        if (classId == UseStmt::class.java) {
            return uses
        }

        throw CodeException("entities WTF?")
    }

    private fun parseSource(source: File) {
        val pathName = getPathName(source)
        BaseExpr.logLine("Source: $pathName")
        val tokenizer = Tokenizer()
        tokenizer.module = this
        tokenizer.readFile(source)
        sources.add(Source(tokenizer, pathName))
    }

    private fun parseSources(library: File) {
        val files = library.listFiles()
        if (files == null) {
            error(null, "parseLibrarySources WTF?")
            return
        }

        for (next in files) {
            if (next.isDirectory) {
                // recursively parse all sources
                parseSources(next)
                continue
            }

            val path = next.path
            if (path.endsWith(".qpl")) {
                parseSource(next)
            }
        }

        currentSource = null
    }

    override fun toString(): String {
        return "module $name"
    }

    companion object {
        val allModules = HashMap<String, Module>()
        val loading = HashMap<String, Module>()
        var projectRoot: String? = null

        private fun getPathName(file: File): String {
            try {
                if (projectRoot == null) {
                    projectRoot = File(".").canonicalPath
                }

                val pathName = file.canonicalPath
                if (!pathName.startsWith(projectRoot!!)) {
                    throw CodeException("Not in project folder: " + file.path)
                }

                // normalize path name by removing root and using forward slash separators
                return pathName.substring(projectRoot!!.length + 1).replace('\\', '/')
            } catch (e: IOException) {
                e.printStackTrace()
                throw CodeException("Cannot getCanonicalPath for: " + file.path)
            }

        }

        fun parse(name: String): Module {
            val library = File(name)
            if (!library.exists() || !library.isDirectory) {
                throw CodeException("Invalid module name: $name")
            }

            val pathName = getPathName(library)
            val existingModule = allModules[pathName]
            if (existingModule != null) {
                // already loaded library module, do nothing
                return existingModule
            }

            BaseExpr.logLine("Module: $pathName")
            if (loading.containsKey(pathName)) {
                throw CodeException("Import dependency cycle detected")
            }

            val module = Module(pathName)
            loading[pathName] = module
            module.parseSources(library)
            module.analyze()
            loading.remove(pathName)
            allModules[pathName] = module
            return module
        }
    }
}
