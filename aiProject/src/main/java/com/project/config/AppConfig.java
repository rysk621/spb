package com.project.config;

import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
public class AppConfig {

	@Bean
	RestTemplate restTemplate() {
		RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
		return restTemplateBuilder
				.setConnectTimeout(Duration.ofSeconds(5))
				.setReadTimeout(Duration.ofSeconds(3))
				.build();
	}

	@Bean
	WebClient client() {
		HttpClient client = HttpClient.create()
				.option((ChannelOption.CONNECT_TIMEOUT_MILLIS), 1000)
				.responseTimeout(Duration.ofSeconds(1));
		return WebClient.builder().clientConnector(new ReactorClientHttpConnector(client)).build();
	}
}
