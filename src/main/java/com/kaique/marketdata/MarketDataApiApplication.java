package com.kaique.marketdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MarketDataApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketDataApiApplication.class, args);
	}

}
