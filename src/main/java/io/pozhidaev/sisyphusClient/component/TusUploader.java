package io.pozhidaev.sisyphusClient.component;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class TusUploader {


    private WebClient webClient;

    int maxUploadsContemporaneously;

    @Autowired
    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
    }



    @Bean
    public CommandLineRunner addUpload(){
        return args ->
            webClient
                    .options()
                    .exchange()
                    .map(ClientResponse::headers)
                    .map(this::buildOptions)

                    .block();
    }


    private Options buildOptions(ClientResponse.Headers headers){
        final Options options = new Options();
        options.setResumable(String.join(",", headers.header("Tus-Resumable").get(0)));
        options.setVersion(Arrays.asList(headers.header("Tus-Version").get(0).split(",")));
        options.setExtension(Arrays.asList(headers.header("Tus-Extension").get(0).split(",")));
        return options;
    }


    @Setter
    @ToString
    @NoArgsConstructor
    private class Options{
        private String resumable;
        private List<String> version;
        private List<String> extension;

    }
}
