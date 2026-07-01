package com.ktds.portal;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * DB 자동 선택 설정.
 *
 * 시작할 때 MariaDB(localhost:3306/portal)에 연결되는지 확인해서,
 *  - 연결되면  → MariaDB 사용(실무감)
 *  - 안 되면   → H2 인메모리 사용(설치 없이 그냥 실행)
 * 수강생이 MariaDB/WSL 세팅을 못 해도 앱이 알아서 H2 로 뜬다. 프로파일·URL 입력 불필요.
 */
@Configuration
public class DataSourceConfig {

    private static final String MARIADB_URL = "jdbc:mariadb://localhost:3306/portal";
    private static final String MARIADB_USER = "portal";
    private static final String MARIADB_PASSWORD = "portal1234";

    @Bean
    public DataSource dataSource() {
        if (isMariaDbReachable()) {
            System.out.println(">> DB = MariaDB (" + MARIADB_URL + ")");
            return DataSourceBuilder.create()
                    .driverClassName("org.mariadb.jdbc.Driver")
                    .url(MARIADB_URL)
                    .username(MARIADB_USER)
                    .password(MARIADB_PASSWORD)
                    .type(HikariDataSource.class)
                    .build();
        }
        System.out.println(">> MariaDB 연결 불가 → H2 인메모리로 실행 (조회: http://localhost:8080/h2-console)");
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:portal;MODE=LEGACY;DB_CLOSE_DELAY=-1")
                .username("sa")
                .password("")
                .type(HikariDataSource.class)
                .build();
    }

    /** MariaDB 에 2초 안에 연결되는지 가볍게 확인(없으면 빠르게 H2 로 넘어가도록). */
    private boolean isMariaDbReachable() {
        DriverManager.setLoginTimeout(2);
        try (Connection ignored =
                     DriverManager.getConnection(MARIADB_URL, MARIADB_USER, MARIADB_PASSWORD)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
