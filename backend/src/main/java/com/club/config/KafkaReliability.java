package com.club.config;
import org.springframework.context.annotation.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
@Configuration public class KafkaReliability {
 @Bean public DefaultErrorHandler kafkaErrors(){return new DefaultErrorHandler(new FixedBackOff(5000,FixedBackOff.UNLIMITED_ATTEMPTS));}
}
