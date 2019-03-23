package io.pozhidaev.sisyphusClient.component;

import lombok.Getter;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.stream.Collectors;

/**
 * TUSD Upload exception.
 */
public class FileUploadException extends RuntimeException {

    public FileUploadException(final Type type){
        super(type.message);
    }

    public FileUploadException(final ClientResponse cr) {
        super(prepareMessage(cr));
    }

    private static String prepareMessage(final ClientResponse cr) {
        final String headers = cr.headers().asHttpHeaders().entrySet()
            .stream()
            .map(e -> String.format("%s:\t%s", e.getKey(), Strings.join(e.getValue(), ',')))
            .collect(Collectors.joining("\n"));
        return String.format("%s\n%s", cr.statusCode().getReasonPhrase(), headers);
    }

    public enum Type{
        EMPTY_INTERVALS("Intervals could not be empty.");

        private String message;

        Type (final String message) {
            this.message = message;
        }
    }
}
