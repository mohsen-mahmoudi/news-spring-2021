package com.demo.microservice_2021.newsToKafka.transformer;

import com.demo.microservice_2021.kafka.avro.model.NewsAvroModel;
import com.demo.microservice_2021.newsToKafka.dto.Article;
import org.springframework.stereotype.Component;

@Component
public class NewsArticleToAvroTransformer {

    public NewsAvroModel getNewsAvroModelFromStatus(Article article) {
        return NewsAvroModel.newBuilder()
                .setId(article.getSource().getId().hashCode())
                .setUserId(article.getSource().getName().hashCode())
                .setCreatedAt(System.currentTimeMillis())
                .setText(article.getTitle())
                .build();
    }
}
