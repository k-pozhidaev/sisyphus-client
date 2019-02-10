package io.pozhidaev.SisyphusClient.configuration;

import io.tus.java.client.TusClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class SisyphusClientConfigurationTest {

    @Test
    public void onInit() throws IOException {
        final String test_onInit = Files.createTempDirectory("test_onInit").toString();
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setCompletedFolder(test_onInit + "/c");
        sisyphusClientConfiguration.setSourceFolder(test_onInit + "/s");
    }

    @Test(expected = IOException.class)
    public void onInit_exception() throws IOException {
        final Path test_completedFolder = Files.createTempDirectory("test_onInit_exception");
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setCompletedFolder(test_completedFolder.toString());
        sisyphusClientConfiguration.setSourceFolder(test_completedFolder.toString());
        sisyphusClientConfiguration.onInit();
    }

    @Test
    public void completedFolder() throws IOException {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        final Path test_completedFolder = Files.createTempDirectory("test_completedFolder");
        final String tmp = test_completedFolder.toAbsolutePath().toString();
        sisyphusClientConfiguration.setCompletedFolder(tmp);
        sisyphusClientConfiguration.completedFolder();
        assertEquals(test_completedFolder, sisyphusClientConfiguration.completedFolder());
    }

    @Test
    public void sourceFolder() throws IOException {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        final Path test_sourceFolder = Files.createTempDirectory("test_sourceFolder");
        final String tmp = test_sourceFolder.toAbsolutePath().toString();
        sisyphusClientConfiguration.setSourceFolder(tmp);
        assertEquals(sisyphusClientConfiguration.sourceFolder(), test_sourceFolder);
    }

    @Test
    public void sourceFolder_create() throws IOException {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        final Path test_sourceFolder = Files.createTempDirectory("test_sourceFolder");
        final String tmp = test_sourceFolder.toAbsolutePath().toString();
        sisyphusClientConfiguration.setSourceFolder(tmp+"/test2");
        assertEquals(sisyphusClientConfiguration.sourceFolder(), Paths.get(test_sourceFolder.toString(), "test2"));
    }

    @Test(expected = IOException.class)
    public void pathFromStringParam_exception() throws IOException {
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        final Path test_sourceFolder = Files.createTempDirectory("pathFromStringParam_exception", PosixFilePermissions.asFileAttribute(readOnly));
        final String tmp = test_sourceFolder.toAbsolutePath().toString();
        sisyphusClientConfiguration.setSourceFolder(tmp);
        sisyphusClientConfiguration.sourceFolder();
    }

    @Test
    public void tusUploadConsumer() throws IOException {
        final Path test = Files.createTempFile("test", "");
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.tusUploadConsumer().apply(test);
    }

    @Test(expected = RuntimeException.class)
    public void tusUploadConsumer_exception() {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.tusUploadConsumer().apply(Paths.get("/not_existed_file"));
    }

    @Test
    public void tusClient() throws MalformedURLException {
        final SisyphusClientConfiguration configuration = new SisyphusClientConfiguration();
        configuration.setUrl("http://test/");
        configuration.setToken("token_test");
        final TusClient tusClient = configuration.tusClient();

        assertEquals(Objects.requireNonNull(tusClient.getHeaders()).get("X-Token"), "token_test");
    }

    @Test
    public void getUrl() {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setUrl(sisyphusClientConfiguration.getUrl());
    }

    @Test
    public void getSourceFolder() {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setSourceFolder(sisyphusClientConfiguration.getSourceFolder());
    }

    @Test
    public void getCompletedFolder() {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setCompletedFolder(sisyphusClientConfiguration.getCompletedFolder());
    }

    @Test
    public void getToken() {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setToken(sisyphusClientConfiguration.getToken());

    }
}