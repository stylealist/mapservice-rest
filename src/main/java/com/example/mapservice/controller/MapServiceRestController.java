package com.example.mapservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Slf4j
public class MapServiceRestController {
    Environment env;

    @Autowired
    public MapServiceRestController(Environment env) {
        this.env = env;
    }

    @GetMapping(value = "/welcome", produces = "text/plain;charset=UTF-8")
    public String welcome() {
        return "[SJ-LAB] 지도 및 시설물 관리 서비스(mapservice-rest)에 정상적으로 연결되었습니다.";
    }

    @GetMapping("/message")
    public String message(@RequestHeader("first-request") String header){
        log.info(header);
        return "Hello World in First Service";
    }
    @GetMapping(value = "/check", produces = "text/plain;charset=UTF-8")
    public String check(HttpServletRequest request){
        log.info("Server Port={}",request.getServerPort());
        return String.format("[SJ-LAB] API 게이트웨이 및 지도/시설물 백엔드 서비스(mapservice-rest)가 정상적으로 연결되어 원활히 운영 중입니다. (포트: %s)"
                ,env.getProperty("local.server.port"));
    }
}
