package org.iota.qupla.helper;

import org.iota.qupla.qupla.parser.QuplaModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ModuleLoader load modules from configured {@code --modulepath=... || -Dmodulepath=... as 'MODULE_ROOT[:MODULE_ROOT]'} ('--modulepath' not implmented yet). A
 * {@code MODULE_ROOT} is either a 'rootdir' xor a 'zipFile'. Every modulepath entry must be a valid root resource - otherwise {@link RuntimeException} occur.
 * If no 'modulepath' is configured than 'user.dir' will used.
 * <p>
 * Every concrete module will try to load relative to its module root. The first found module wins the loading game.
 * <p>
 * The {@link ModuleLoader} is influenced by {@link ClassLoader}.
 */
public class ModuleLoader
{
  private static final String SYSTEM_DIR = System.getProperty("user.dir");
  private static final String OPTION_KEY = "modulepath";
  private final Map<String, ModuleResource> moduleCache = new LinkedHashMap<>();
  private final List<ModuleResource> moduleResources;

  public ModuleLoader()
  {
    this(System.getProperty(OPTION_KEY, SYSTEM_DIR));
  }

  public ModuleLoader(String modulePath)
  {
    final String[] modulePathArray = Objects.requireNonNull(modulePath, "'modulePath' must not be null.").split(":");
    moduleResources = Stream.of(modulePathArray).map(ModuleResource::create).collect(Collectors.toList());
    System.out.println("Init module loader: " + moduleResources);
  }

  /**
   * Try to load a module by given module path. The module path must be a 'relative/path/to/root'.
   *
   * @param modulePath not null module path
   * @return present module or nothing
   */
  public Optional<QuplaModule> loadModule(String modulePath)
  {
    Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
    return tryFind(modulePath).map(moduleResource -> moduleResource.loadModule(modulePath));
  }

  /**
   * Try to open a module as stream by given module path. The module path must be a 'relative/path/to/root'.
   *
   * @param modulePath not null module path
   * @return open stream or fail
   */
  public InputStream getModuleAsStream(String modulePath)
  {
    Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
    return tryFind(modulePath)
            .map(moduleResource -> moduleResource.openInputStream(modulePath))
            .orElseThrow(() ->
                    new RuntimeException("Cannot open module stream '" + modulePath + "'."));

  }

  /**
   * Try to get all sub module paths of root module. E.g. by given module resource content.
   * <pre>
   * + root
   * |-- module-1.apl
   * |-- module-2.apl
   * `-- submodule
   *     |-- module-1.apl
   *     |-- module-2.apl
   * </pre>
   *
   * <li>{@code getSubModulePaths(submodule} ... return [submodule/module-1.apl,submodule/module-2.apl]</li>
   * <li>{@code getSubModulePaths(root} ... return [module-1.apl,module-2.apl,submodule]</li>
   * <li>{@code getSubModulePaths(submodule/module-1.apl} ... return []</li>
   *
   * @param submoduleRoot not null submoduleRoot
   * @return maybe empty [sub module path] list
   */
  public Set<String> getSubModulePaths(String submoduleRoot)
  {
    Objects.requireNonNull(submoduleRoot, "'submoduleRoot' must not be null.");
    final Set<String> submodules = new LinkedHashSet<>();
    for (ModuleResource moduleResource : moduleResources)
    {
      if (moduleResource.hasModule(submoduleRoot))
      {
        final List<String> subModuleChilds = moduleResource.getSubModulePaths(submoduleRoot);
        submodules.addAll(subModuleChilds);
        break;// on first module source
      }
    }
    return submodules;
  }

  private Optional<ModuleResource> tryFind(String modulePath)
  {
    if (moduleCache.containsKey(modulePath))
    {
      return Optional.of(moduleCache.get(modulePath));
    }
    final Optional<ModuleResource> firstModuleResource = this.moduleResources.stream()
            .filter(moduleResource -> moduleResource.hasModule(modulePath))
            .findFirst();
    // put to cache
    firstModuleResource.ifPresent(moduleResource -> moduleCache.put(modulePath, moduleResource));

    return firstModuleResource;
  }

  public boolean isSubModule(String modulePath)
  {
    return tryFind(modulePath).map(moduleResource -> moduleResource.isSubModule(modulePath)).orElse(false);
  }

  /**
   * A ModuleResource represent an entry of the configured 'modulepath' to get access to the contained module entries.
   *
   * @see ModuleDirectory - existing directory
   * @see ModuleZipfile - existing zip file contained a zipped directory
   */
  private interface ModuleResource extends Iterable<String>
  {
    /**
     * Check if the given module path is part of the module resource.
     *
     * @param modulePath not null 'relative/path/to/module-root'
     * @return true if exist otherwise false
     */
    boolean hasModule(String modulePath);

    /**
     * Load the module.
     *
     * @param modulePath not null 'relative/path/to/module-root'
     * @return module or null
     */
    QuplaModule loadModule(String modulePath);

    /**
     * Open module stream if module exist.
     *
     * @param modulePath not null 'relative/path/to/module-root'
     * @return open stream or fail.
     */
    InputStream openInputStream(String modulePath);

    /**
     * Check if the given module path contains other modules.
     *
     * @param modulePath not null 'relative/path/to/module-root'
     * @return true if modulePath is directory. Otherwise false
     */
    Boolean isSubModule(String modulePath);

    /**
     * Get the submodules of the given submodule root.
     *
     * @param submoduleRoot not null 'relative/path/to/module-root'
     * @return maybe empty [module/path] list
     */
    List<String> getSubModulePaths(String submoduleRoot);

    /**
     * Factory function to create concrete {@link ModuleResource}s.
     */
    static ModuleResource create(String modulePath)
    {
      Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
      if (new File(modulePath).isDirectory())
      {
        try
        {
          return new ModuleDirectory(modulePath, new File(modulePath));
        } catch (Exception e)
        {
          throw new RuntimeException("Cannot init moduleName directory=" + modulePath, e);
        }
      } else if (new File(modulePath).isFile())
      {
        try
        {
          return new ModuleZipfile(modulePath, new ZipFile(new File(modulePath)));
        } catch (IOException e)
        {
          throw new RuntimeException("Cannot init moduleName zip file=" + modulePath, e);
        }
      }
      throw new RuntimeException("Unknown moduleName type. " + modulePath + " is no zip file or directory.");
    }

    /**
     * A {@link ModuleDirectory} get access to the Modules of this directory.
     */
    class ModuleDirectory implements ModuleResource
    {

      private final String moduleName;
      private final File rootDir;

      public ModuleDirectory(String modulePath, File directory)
      {
        this.moduleName = Objects.requireNonNull(modulePath, "'moduleName' must not be null");
        this.rootDir = Objects.requireNonNull(directory, "'rootDir' must not be null");
      }

      @Override
      public boolean hasModule(String modulePath)
      {
        Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
        return new File(rootDir, modulePath).exists();
      }

      @Override
      public QuplaModule loadModule(String modulePath)
      {
        Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
        return new QuplaModule(modulePath);
      }

      @Override
      public List<String> getSubModulePaths(String submoduleRoot)
      {
        Objects.requireNonNull(submoduleRoot, "'modulePath' must not be null.");
        if (!isSubModule(submoduleRoot))
        {
          return Collections.emptyList();
        }
        return Stream.of(new File(rootDir, submoduleRoot).listFiles())
                .map(file -> rootDir.toPath().relativize(file.toPath()))
                .map(Object::toString)
                .collect(Collectors.toList());
      }

      @Override
      public InputStream openInputStream(String modulePath)
      {
        Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
        final File moduleFile = new File(rootDir, modulePath);
        try
        {
          return new FileInputStream(moduleFile);
        } catch (FileNotFoundException e)
        {
          throw new RuntimeException("Cannot open directory module '" + moduleFile + "'", e);
        }
      }

      @Override
      public Boolean isSubModule(String modulePath)
      {
        Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
        return new File(rootDir, modulePath).isDirectory();
      }

      @Override
      public String toString()
      {
        return "ModuleDirectory{" +
                "moduleName='" + moduleName + '\'' +
                ", rootDir=" + rootDir +
                '}';
      }

      @Override
      public Iterator<String> iterator()
      {
        final LinkedList<String> files = new LinkedList<>();
        try
        {
          Files.walk(Paths.get("."))
                  .filter(Files::isRegularFile)
                  .map(Path::toString)
                  .forEach(files::add);
        } catch (IOException e)
        {
          e.printStackTrace();
        }
        return files.iterator();
      }
    }

    /**
     * A {@link ModuleZipfile} get access to the modules of the zipped directory of the zip file.
     */
    class ModuleZipfile implements ModuleResource
    {
      private final String moduleName;
      private final ZipFile zipFile;

      private final Map<String, ZipEntry> zipEntries = new LinkedHashMap<>();
      private boolean isRead = false;

      public ModuleZipfile(String modulePath, ZipFile zipFile)
      {
        this.moduleName = Objects.requireNonNull(modulePath, "'moduleName' must not be null");
        this.zipFile = Objects.requireNonNull(zipFile, "'zipFile' must not be null");
      }

      @Override
      public boolean hasModule(String modulePath)
      {
        Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
        return ensureReading().containsKey(modulePath);
      }

      @Override
      public QuplaModule loadModule(String modulePath)
      {
        Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
        return Optional.ofNullable(ensureReading().get(modulePath))
                .map(zipEntry -> new QuplaModule(zipEntry.getName()))
                .orElseThrow(() -> new RuntimeException("Cannot load module '" + modulePath + "'"));
      }

      @Override
      public List<String> getSubModulePaths(String submoduleRoot)
      {
        Objects.requireNonNull(submoduleRoot, "'modulePath' must not be null.");
        final Path submoduleRootPath = Paths.get(submoduleRoot);
        final List<String> subModuleChilds = new LinkedList<>();
        for (String module : ensureReading().keySet())
        {
          final Path modulePath = Paths.get(module);
          if (modulePath.startsWith(submoduleRootPath))
          {
            final int submoduleDepth = submoduleRootPath.getNameCount();
            final int moduleDepth = modulePath.getNameCount();
            if (moduleDepth - submoduleDepth == 1)
            {
              subModuleChilds.add(module);
            }
          }
        }
        return subModuleChilds;
      }

      @Override
      public InputStream openInputStream(String modulePath)
      {
        Objects.requireNonNull(modulePath, "'modulePath' must not be null.");
        return Optional.ofNullable(ensureReading().get(modulePath))
                .flatMap(zipEntry -> safeOpenStream(zipEntry))
                .orElseThrow(() -> new RuntimeException("Cannot open zip module stream '" + modulePath + "'"));
      }

      private Optional<InputStream> safeOpenStream(ZipEntry zipEntry)
      {
        try
        {
          return Optional.of(zipFile.getInputStream(zipEntry));
        } catch (IOException e)
        {
          e.printStackTrace();
          return Optional.empty();
        }
      }

      @Override
      public Boolean isSubModule(String modulePath)
      {
        final boolean hasSubModules = !getSubModulePaths(modulePath).isEmpty();
        return hasSubModules;
      }

      private Map<String, ZipEntry> ensureReading()
      {
        if (!isRead)
        {
          Enumeration<? extends ZipEntry> entries = zipFile.entries();

          Set<String> zippedDirectories = new LinkedHashSet<>();
          while (entries.hasMoreElements())
          {
            ZipEntry entry = entries.nextElement();
            zipEntries.put(Paths.get(entry.getName()).toString(), entry);

            Optional.ofNullable(Paths.get(entry.getName()).getParent())
                    .ifPresent(parent -> zippedDirectories.add(parent.toString()));
          }
          // ensure that all directories also cached as ZipEntry
          zippedDirectories.forEach(zippedDir -> zipEntries.put(zippedDir, new ZipEntry(zippedDir)));
          isRead = true;
        }
        return zipEntries;
      }

      @Override
      public String toString()
      {
        return "ModuleZipfile{" +
                "moduleName='" + moduleName + '\'' +
                ", zipFile=" + zipFile +
                '}';
      }

      @Override
      public Iterator<String> iterator()
      {
        final LinkedList<String> files = new LinkedList<>();
        try
        {
          FileSystems.newFileSystem(Paths.get(moduleName).toUri(), new LinkedHashMap<>())
                  .getRootDirectories()
                  .forEach(path -> {
                    if(Files.isRegularFile(path)){
                      files.add(path.toString());
                    }
                  });
        } catch (IOException e)
        {
          e.printStackTrace();
        }
        return files.iterator();
      }
    }
  }
}
