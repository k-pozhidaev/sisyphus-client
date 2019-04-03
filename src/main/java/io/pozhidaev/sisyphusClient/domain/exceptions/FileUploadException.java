package io.pozhidaev.sisyphusClient.domain.exceptions;

import org.apache.logging.log4j.util.Strings;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.stream.Collectors;

/**
 * TUSD Upload exception.
 */
public class FileUploadException extends RuntimeException {

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

}
