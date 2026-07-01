package com.ktds.portal;

import com.ktds.portal.user.User;
import com.ktds.portal.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 데모용 초기 데이터. 실습 시작 시 사용자 4명을 넣어둔다.
 *  1 김사원(role=1)  2 박팀장(role=2)  3 이임원(role=3)  4 최사원(role=1)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepo;

    public DataSeeder(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;
        userRepo.save(new User("김사원", "kim@ktds.com", 1, "개발1팀"));
        userRepo.save(new User("박팀장", "park@ktds.com", 2, "개발1팀"));
        userRepo.save(new User("이임원", "lee@ktds.com", 3, "경영지원"));
        userRepo.save(new User("최사원", "choi@ktds.com", 1, "개발2팀"));
        System.out.println(">> 초기 사용자 4명 생성 완료");
    }
}
