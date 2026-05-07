package com.jk.paper_trading_dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PaperTradingDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaperTradingDashboardApplication.class, args);
	}

}
