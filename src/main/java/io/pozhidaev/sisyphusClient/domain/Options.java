package io.pozhidaev.sisyphusClient.domain;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class Options {
    public final static String HEADER_NAME_RESUMABLE = "Tus-Resumable";
    public final static String HEADER_NAME_VERSION = "Tus-Version";
    public final static String HEADER_NAME_EXTENSION = "Tus-Extension";
    public final static String HEADER_NAME_MAX_SIZE = "Tus-Max-Size";

    private String resumable;
    private List<String> version;
    private List<String> extension;
    private Long maxSize;

    private static <T> Optional<T> getZeroElementIfExists(final List<T> list) {
        if (list.size() > 0) {
            return Optional.of(list.get(0));
        }
        return Optional.empty();
    }

    public static Options buildOptions(final ClientResponse.Headers headers) {
        final Options options = new Options();

        getZeroElementIfExists(headers.header(Options.HEADER_NAME_RESUMABLE))
            .ifPresent(options::setResumable);
        getZeroElementIfExists(headers.header(Options.HEADER_NAME_VERSION))
            .map(s -> s.split(","))
            .map(Arrays::asList)
            .ifPresent(options::setVersion);
        getZeroElementIfExists(headers.header(Options.HEADER_NAME_EXTENSION))
            .map(s -> s.split(","))
            .map(Arrays::asList)
            .ifPresent(options::setExtension);
        getZeroElementIfExists(headers.header(Options.HEADER_NAME_MAX_SIZE))
            .map(Long::valueOf)
            .ifPresent(options::setMaxSize);
        return options;
    }
}