package com.demo.microservice_2021.newsToKafka.runner;

import com.demo.microservice_2021.configdata.config.NewsToKakfaServiceConfigData;
import com.demo.microservice_2021.newsToKafka.dto.NewsRoot;
import com.demo.microservice_2021.newsToKafka.init.StreamInitializer;
import com.demo.microservice_2021.newsToKafka.service.NewsToKafkaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Component
public class NewsStreamRunner implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(NewsStreamRunner.class);

    private final WebClient webClient;
    private final NewsToKakfaServiceConfigData configData;
    private final StreamInitializer streamInitializer;
    private final NewsToKafkaService newsToKafkaService;

    public NewsStreamRunner(WebClient.Builder webClientBuilder,
                            NewsToKakfaServiceConfigData configData,
                            StreamInitializer streamInitializer, NewsToKafkaService newsToKafkaService) {
        this.streamInitializer = streamInitializer;
        this.newsToKafkaService = newsToKafkaService;
        this.webClient = webClientBuilder.baseUrl(configData.getNewsApiStreamUrl()).build();
        this.configData = configData;
    }

    public Flux<NewsRoot> streamNews(String filter) {
        return Flux.interval(Duration.ofSeconds(3600))
                .flatMap(tick -> fetchNews(filter))
                .onErrorContinue((error, obj) -> System.err.println("Error fetching news: " + error.getMessage()));
    }

    private Flux<NewsRoot> fetchNews(String filter) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("q", filter)
                        .queryParam("apiKey", configData.getNewsApiBearerToken())
                        .build())
                .retrieve()
                .bodyToFlux(NewsRoot.class);
    }

    @Override
    public void run(String... args) throws Exception {
        streamInitializer.init();
        processAndSendNewsToKafka();
    }

    private void processAndSendNewsToKafka() {
        fetchNews(configData.getNewsKeyword()).subscribe(news -> {
            news.getArticles().stream().forEach(article -> {
                LOG.info("Processing article : {}", article);
                newsToKafkaService.streamNewsToKafka(article);
                interrupt(3);
            });
        });
        interrupt(120);
    }

    private static void interrupt(int second) {
        try {
            Thread.sleep(second * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
