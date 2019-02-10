package io.pozhidaev.SisyphusClient.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;


@RunWith(SpringRunner.class)
public class ScheduledServiceTest {

    @MockBean
    TusExecutorService tusExecutorService;

    @Test
    public void filesExistingTask() throws IOException {
        final Path source = Files.createTempDirectory("test_s");
        final Path completed = Files.createTempDirectory("test_c");



        final ScheduledService service = new ScheduledService(
            source,
            completed,
            tusExecutorService
        );

        Files.createFile(Paths.get(source.toString(), "test1"));
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
        Files.createFile(Paths.get(source.toString(), "test2"), PosixFilePermissions.asFileAttribute(readOnly));
        Files.createDirectory(Paths.get(source.toString(), "test3"));
        service.filesExistingTask();
    }

    @Test
    public void filesExistingTask_exception() throws IOException {

        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
        final FileAttribute<Set<PosixFilePermission>> fileAttribute = PosixFilePermissions.asFileAttribute(readOnly);

        final Path source = Files.createTempDirectory("test_s");
        final Path completed = Files.createTempDirectory("test_c", fileAttribute);



        final ScheduledService service = new ScheduledService(
            source,
            completed,
            tusExecutorService
        );

        Files.createFile(Paths.get(source.toString(), "test1"));
        service.filesExistingTask();
    }
}