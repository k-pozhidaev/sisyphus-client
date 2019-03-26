package io.pozhidaev.sisyphusClient.domain;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Optional;

@Setter
@ToString
@NoArgsConstructor
public class Options {
    public final static String HEADER_NAME_RESUMABLE = "Tus-Resumable";
    public final static String HEADER_NAME_VERSION = "Tus-Version";
    public final static String HEADER_NAME_EXTENSION = "Tus-Extension";
    public final static String HEADER_NAME_MAX_SIZE = "Tus-Max-Size";

    private String resumable;
    private List<String> version;
    private List<String> extension;
    private Long maxSize;


    public static <T> Optional<T> getZeroElementIfExists(final List<T> list) {
        if (list.size() > 0) {
            return Optional.of(list.get(0));
        }
        return Optional.empty();
    }
}