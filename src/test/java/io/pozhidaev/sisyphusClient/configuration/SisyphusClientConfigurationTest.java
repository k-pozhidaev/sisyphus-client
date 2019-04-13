package io.pozhidaev.sisyphusClient.configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.FluxSink;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import static io.pozhidaev.sisyphusClient.utils.Whitebox.setInternalState;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
public class SisyphusClientConfigurationTest {


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

    @Test(expected = RuntimeException.class)
    public void pathFromStringParam_exception() throws IOException {
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        final Path test_sourceFolder = Files.createTempDirectory("pathFromStringParam_exception", PosixFilePermissions.asFileAttribute(readOnly));
        final String tmp = test_sourceFolder.toAbsolutePath().toString();
        sisyphusClientConfiguration.setSourceFolder(tmp);
        sisyphusClientConfiguration.sourceFolder();
    }

    @Test(expected = RuntimeException.class)
    public void pathFromStringParam_exception2() throws IOException {
        Set<PosixFilePermission> readOnly = PosixFilePermissions.fromString("r--r--r--");
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        final Path test_sourceFolder = Files.createTempDirectory("pathFromStringParam_exception", PosixFilePermissions.asFileAttribute(readOnly));
        final String tmp = test_sourceFolder.toAbsolutePath().toString();
        sisyphusClientConfiguration.setSourceFolder(Paths.get(tmp, "test").toString());
        sisyphusClientConfiguration.sourceFolder();
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

    @Test
    public void getChunkSize() {
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setChunkSize(15);
        assertEquals(sisyphusClientConfiguration.chunkSize().get().toString(), Objects.toString(15));
    }

    @Test
    public void tusdUploadBuilder(){
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setIntervals(new Integer[]{1000});
        sisyphusClientConfiguration.setUrl("test://test");
        sisyphusClientConfiguration.tusdUploadBuilder(WebClient::create);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tusdUploadBuilder_exception(){
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setIntervals(new Integer[]{});
        sisyphusClientConfiguration.setUrl("test://test");
        sisyphusClientConfiguration.tusdUploadBuilder(WebClient::create);
    }

    @Test
    public void webClientFactoryMethod(){
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.setUrl("test://test");
        sisyphusClientConfiguration.webClientFactoryMethod().get();
    }

    @Test
    public void logChannel(){
        final SisyphusClientConfiguration sisyphusClientConfiguration = new SisyphusClientConfiguration();
        sisyphusClientConfiguration.logChannel();
    }

    @Test
    public void fileFlow() throws IOException {
        final SubscribableChannel channel = mock(SubscribableChannel.class);
        final SisyphusClientConfiguration configuration = mock(SisyphusClientConfiguration.class);
        when(configuration.sourceFolder()).thenReturn(Files.createTempDirectory("test"));
        when(configuration.logChannel()).thenReturn(channel);
        when(configuration.fileFlow()).thenCallRealMethod();
        when(configuration.filterFiles(null)).thenReturn(Collections.emptyList());
        configuration.fileFlow();
    }

    @Test
    public void filterFiles(){
        final File file1 = mock(File.class);
        when(file1.lastModified()).thenReturn(System.currentTimeMillis() - 59_000);
        final File file2 = mock(File.class);
        when(file2.lastModified()).thenReturn(System.currentTimeMillis() - 60_000);
        final SisyphusClientConfiguration configuration = new SisyphusClientConfiguration();
        assertEquals(configuration.filterFiles(new File[]{file1, file2}).size(), 1);
    }

    @Test
    public void createdFileStream() {
        final SubscribableChannel channel = mock(SubscribableChannel.class);
        final SisyphusClientConfiguration configuration = mock(SisyphusClientConfiguration.class);
        when(configuration.logChannel()).thenReturn(channel);
        when(configuration.createdFileStream()).thenCallRealMethod();
        configuration.createdFileStream().subscribe();
    }

    @Test
    public void ForwardingMessageHandler_handleMessage(){
        final FluxSink<Path> sink = mock(FluxSink.class);
        when(sink.next(Paths.get("/test/test2"))).thenReturn(sink);
        final File file = mock(File.class);
        when(file.getAbsolutePath()).thenReturn("/test/test2");
        final Message message = mock(Message.class);
        when(message.getPayload()).thenReturn(file);
        final SisyphusClientConfiguration.ForwardingMessageHandler handler
            = mock(SisyphusClientConfiguration.ForwardingMessageHandler.class);
        setInternalState(handler, "sink", sink);
        doCallRealMethod().when(handler).handleMessage(message);
        handler.handleMessage(message);

    }
}