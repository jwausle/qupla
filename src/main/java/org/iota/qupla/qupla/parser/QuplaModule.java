package org.iota.qupla.qupla.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.iota.qupla.Qupla;
import org.iota.qupla.exception.CodeException;
import org.iota.qupla.helper.ModuleLoader;
import org.iota.qupla.qupla.context.QuplaAnyNullContext;
import org.iota.qupla.qupla.expression.base.BaseExpr;
import org.iota.qupla.qupla.statement.ExecStmt;
import org.iota.qupla.qupla.statement.FuncStmt;
import org.iota.qupla.qupla.statement.ImportStmt;
import org.iota.qupla.qupla.statement.LutStmt;
import org.iota.qupla.qupla.statement.TemplateStmt;
import org.iota.qupla.qupla.statement.TypeStmt;
import org.iota.qupla.qupla.statement.UseStmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class QuplaModule extends BaseExpr
{
  public static final HashMap<String, QuplaModule> allModules = new HashMap<>();
  public static final HashMap<String, QuplaModule> loading = new HashMap<>();
  public static String projectRoot;

  public QuplaSource currentSource;

  public final ArrayList<ExecStmt> execs = new ArrayList<>();
  public final ArrayList<FuncStmt> funcs = new ArrayList<>();
  public final ArrayList<ImportStmt> imports = new ArrayList<>();
  public final ArrayList<LutStmt> luts = new ArrayList<>();
  public final ArrayList<QuplaModule> modules = new ArrayList<>();
  public final ArrayList<QuplaSource> sources = new ArrayList<>();
  public final ArrayList<TemplateStmt> templates = new ArrayList<>();
  public final ArrayList<TypeStmt> types = new ArrayList<>();
  public final ArrayList<UseStmt> uses = new ArrayList<>();

  public QuplaModule(final String name)
  {
    this.name = name;
    currentModule = this;
  }

  public QuplaModule(final Collection<QuplaModule> subModules)
  {
    for (final QuplaModule subModule : subModules)
    {
      funcs.addAll(subModule.funcs);
      imports.addAll(subModule.imports);
      luts.addAll(subModule.luts);
      types.addAll(subModule.types);
      execs.addAll(subModule.execs);
      templates.addAll(subModule.templates);
    }

    name = "{SINGLE_MODULE}";
  }

  public static QuplaModule parse(String name, ModuleLoader moduleLoader)
  {

    final QuplaModule existingModule = allModules.get(name);
    if (existingModule != null)
    {
      // already loaded library module, do nothing
      return existingModule;
    }


    final String pathName = name;

    Qupla.log("QuplaModule: " + pathName);
    if (loading.containsKey(pathName))
    {
      throw new CodeException("Import dependency cycle detected");
    }

    final QuplaModule module = moduleLoader.loadModule(name)
            .orElseThrow(() -> new RuntimeException("Cannot load module '" + name + "'"));
    loading.put(pathName, module);
    module.parseSources(moduleLoader, pathName);
    module.analyze();
    loading.remove(pathName);
    allModules.put(pathName, module);
    return module;
  }

  private void addReferencedModules(final QuplaModule module)
  {
    if (!modules.contains(module))
    {
      modules.add(module);
    }

    for (final QuplaModule referenced : module.modules)
    {
      if (!modules.contains(referenced))
      {
        modules.add(referenced);
      }
    }
  }

  @Override
  public void analyze()
  {
    analyzeEntities(imports);
    for (final ImportStmt imp : imports)
    {
      addReferencedModules(imp.importModule);
    }

    analyzeEntities(types);
    analyzeEntities(luts);

    // first analyze all normal function signatures
    for (final FuncStmt func : funcs)
    {
      if (!func.wasAnalyzed() && func.use == null)
      {
        func.analyzeSignature();
      }
    }

    analyzeEntities(templates);
    analyzeEntities(uses);
    analyzeEntities(execs);

    // now that we know all functions and their properties
    // we can finally analyze their bodies
    for (int i = 0; i < funcs.size(); i++)
    {
      final FuncStmt func = funcs.get(i);
      func.analyze();
    }

    // determine which functions short-circuit on any null parameter
    new QuplaAnyNullContext().eval(this);
  }

  private void analyzeEntities(final ArrayList<? extends BaseExpr> items)
  {
    for (final BaseExpr item : items)
    {
      if (!item.wasAnalyzed())
      {
        item.analyze();
      }
    }
  }

  public void checkDuplicateName(final ArrayList<? extends BaseExpr> items, final BaseExpr symbol)
  {
    for (final BaseExpr item : items)
    {
      if (item.name.equals(symbol.name))
      {
        symbol.error("Already defined: " + symbol.name);
      }
    }
  }

  @Override
  public BaseExpr clone()
  {
    throw new CodeException("clone WTF?");
  }

  final public ArrayList<? extends BaseExpr> entities(final Class classId)
  {
    if (classId == FuncStmt.class)
    {
      return funcs;
    }

    if (classId == LutStmt.class)
    {
      return luts;
    }

    if (classId == TemplateStmt.class)
    {
      return templates;
    }

    if (classId == TypeStmt.class)
    {
      return types;
    }

    if (classId == UseStmt.class)
    {
      return uses;
    }

    throw new CodeException("entities WTF?");
  }

  private void parseSource(ModuleLoader moduleLoader, final String source)
  {
    Qupla.log("QuplaSource: " + source);
    final Tokenizer tokenizer = new Tokenizer();
    tokenizer.module = this;
    tokenizer.read(moduleLoader.getModuleAsStream(source));
    sources.add(new QuplaSource(tokenizer, source, moduleLoader));
  }

  private void parseSources(ModuleLoader moduleLoader, final String library)
  {
    Set<String> submodules = moduleLoader.getSubModulePaths(library);
    if (submodules == null)
    {
      error(null, "parseLibrarySources WTF?");
      return;
    }

    for (final String next : submodules)
    {
      if (moduleLoader.isSubModule(next))
      {
        // recursively parse all sources
        parseSources(moduleLoader, next);
        continue;
      }

      final String path = next;
      if (path.endsWith(".qpl"))
      {
        parseSource(moduleLoader, next);
      }
    }

    currentSource = null;
  }

  @Override
  public String toString()
  {
    return "module " + name;
  }
}
