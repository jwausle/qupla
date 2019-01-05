package org.iota.qupla.qupla.helper;

import org.iota.qupla.helper.ModuleLoader;
import org.iota.qupla.qupla.parser.QuplaModule;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ModuleLoaderTest {
    private ModuleLoader underTest;

    @Test
    public void loadAllQuplaModulesTest() {
        underTest = createModuleLoader(System.getProperty("user.dir") + "/src/main/resources");

        final Set<String> quplaModule = underTest.getSubModulePaths("Qupla");
        Assert.assertEquals(13, quplaModule.size());
        assertQuplaModule(quplaModule);

        final Set<String> mathModules = underTest.getSubModulePaths("Qupla/Math");
        Assert.assertEquals(18, mathModules.size());
        assertQuplaMathModule(mathModules);
    }

    @Test
    public void loadAllZippedQuplaModulesTest() throws IOException {
        Path zippedFile = zip(System.getProperty("user.dir") + "/src/main/resources/Qupla");
        underTest = createModuleLoader(zippedFile.toString());

        final Set<String> quplaModule = underTest.getSubModulePaths("Qupla");
        Assert.assertEquals(13, quplaModule.size());
        assertQuplaModule(quplaModule);

        final Set<String> mathModules = underTest.getSubModulePaths("Qupla/Math");
        Assert.assertEquals(18, mathModules.size());
        assertQuplaMathModule(mathModules);
    }

    @Test
    public void when_module_parse_Examples_from_zipModules_then_succ() throws IOException {
        final Path quplaZip = zip(System.getProperty("user.dir") + "/src/main/resources/Qupla");
        final Path examplesZip = zip(System.getProperty("user.dir") + "/src/main/resources/Examples");

        underTest = new ModuleLoader(quplaZip.toString() + ":" + examplesZip.toString());
        final QuplaModule moduleExample = QuplaModule.parse("Examples", underTest);

        final Optional<QuplaModule> examples = underTest.loadModule("Examples");
        Assert.assertTrue(examples.isPresent());
        examples.ifPresent(module -> {
            module.analyze();
        });
    }
    
    private void assertQuplaMathModule(Set<String> mathModules) {
        Assert.assertTrue("Expect Qupla/Math/fullmul.qpl", mathModules.contains("Qupla/Math/fullmul.qpl"));
        Assert.assertTrue("Expect Qupla/Math/halfadd.qpl", mathModules.contains("Qupla/Math/halfadd.qpl"));
        Assert.assertTrue("Expect Qupla/Math/cmp.qpl", mathModules.contains("Qupla/Math/cmp.qpl"));
        Assert.assertTrue("Expect Qupla/Math/mul.qpl", mathModules.contains("Qupla/Math/mul.qpl"));
        Assert.assertTrue("Expect Qupla/Math/neg.qpl", mathModules.contains("Qupla/Math/neg.qpl"));
        Assert.assertTrue("Expect Qupla/Math/mod.qpl", mathModules.contains("Qupla/Math/mod.qpl"));
        Assert.assertTrue("Expect Qupla/Math/max.qpl", mathModules.contains("Qupla/Math/max.qpl"));
        Assert.assertTrue("Expect Qupla/Math/sign.qpl", mathModules.contains("Qupla/Math/sign.qpl"));
        Assert.assertTrue("Expect Qupla/Math/incr.qpl", mathModules.contains("Qupla/Math/incr.qpl"));
        Assert.assertTrue("Expect Qupla/Math/div.qpl", mathModules.contains("Qupla/Math/div.qpl"));
        Assert.assertTrue("Expect Qupla/Math/divmod.qpl", mathModules.contains("Qupla/Math/divmod.qpl"));
        Assert.assertTrue("Expect Qupla/Math/decr.qpl", mathModules.contains("Qupla/Math/decr.qpl"));
        Assert.assertTrue("Expect Qupla/Math/fulladd.qpl", mathModules.contains("Qupla/Math/fulladd.qpl"));
        Assert.assertTrue("Expect Qupla/Math/abs.qpl", mathModules.contains("Qupla/Math/abs.qpl"));
        Assert.assertTrue("Expect Qupla/Math/sub.qpl", mathModules.contains("Qupla/Math/sub.qpl"));
        Assert.assertTrue("Expect Qupla/Math/min.qpl", mathModules.contains("Qupla/Math/min.qpl"));
        Assert.assertTrue("Expect Qupla/Math/add.qpl", mathModules.contains("Qupla/Math/add.qpl"));
    }

    private void assertQuplaModule(Set<String> quplaModule) {
        Assert.assertTrue(quplaModule.contains("Qupla/Math"));
        Assert.assertTrue(quplaModule.contains("Qupla/all.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/as.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/bool.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/break.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/equal.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/lshift.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/map.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/print.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/quorum.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/rshift.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/types.qpl"));
        Assert.assertTrue(quplaModule.contains("Qupla/unequal.qpl"));
    }

    public static Path zip(String srcDirectory) throws IOException {
        final Path zipFile = Files.createTempFile("qupla", ".zip");
        final Path srcDirectoryPath = Paths.get(srcDirectory);
        final Path srcDirectoryName = srcDirectoryPath.getFileName();

        try (ZipOutputStream zipStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walk(srcDirectoryPath)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(srcDirectoryName.resolve(srcDirectoryPath.relativize(path)).toString());
                        try {
                            zipStream.putNextEntry(zipEntry);
                            Files.copy(path, zipStream);
                            zipStream.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        }
        return zipFile;
    }


    private ModuleLoader createModuleLoader(String modulepath) {
        return new ModuleLoader(modulepath);
    }

}
