package com.example.mapservice.service.impl;

import com.example.mapservice.mapper.QfieldFacilityMapper;
import com.example.mapservice.service.QfieldMediaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QFieldCloud REST API로 첨부 파일을 받아 전달한다.
 *
 * 호출 흐름:
 *   POST /api/v1/auth/login/            → 토큰 발급 (만료 전까지 재사용)
 *   GET  /api/v1/projects/              → 프로젝트 목록 (source_table 접두어로 프로젝트 식별)
 *   GET  /api/v1/files/{projectId}/{경로}/ → 파일 바이트
 *
 * 접속 정보는 환경변수로만 주입한다(application.yml 에 평문으로 넣지 말 것).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QfieldMediaServiceImpl implements QfieldMediaService {

    private final QfieldFacilityMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${qfield.base-url:https://qfield.sj-lab.co.kr}")
    private String baseUrl;

    @Value("${qfield.username:}")
    private String username;

    @Value("${qfield.password:}")
    private String password;

    /** source_table 이름에 들어 있는 프로젝트 식별자 조각 (예: stylealist_41fc29aa-05da_notes_notes) */
    private static final Pattern PROJECT_PREFIX = Pattern.compile("_([0-9a-f]{8}-[0-9a-f]{4})_");

    /** 발급받은 토큰은 만료 전까지 재사용한다 */
    private volatile String cachedToken;
    private volatile long tokenIssuedAtMillis;
    private static final long TOKEN_TTL_MILLIS = Duration.ofHours(6).toMillis();

    /** source_table 접두어 → 프로젝트 id 캐시 */
    private final Map<String, String> projectIdCache = new ConcurrentHashMap<>();

    @Override
    public MediaContent getFacilityMedia(String totalId, String path) {
        if (username.isBlank() || password.isBlank()) {
            throw new MediaException(MediaError.NOT_CONFIGURED,
                    "QFieldCloud 접속 정보가 설정되지 않았습니다(QFIELD_USERNAME/QFIELD_PASSWORD).");
        }

        String sourceJson = mapper.getFacilityMediaSource(totalId);
        if (sourceJson == null || sourceJson.isBlank()) {
            throw new MediaException(MediaError.FACILITY_NOT_FOUND, "시설물을 찾을 수 없습니다: " + totalId);
        }

        String sourceTable;
        List<String> allowedPaths = new ArrayList<>();
        try {
            JsonNode node = objectMapper.readTree(sourceJson);
            sourceTable = node.path("sourceTable").asText("");
            node.path("paths").forEach(item -> {
                if (item != null && !item.isNull() && !item.asText().isBlank()) {
                    allowedPaths.add(item.asText().trim());
                }
            });
        } catch (Exception e) {
            throw new MediaException(MediaError.UPSTREAM_ERROR, "시설물 첨부 정보를 읽지 못했습니다: " + e.getMessage());
        }

        // 요청된 경로가 이 시설물의 첨부인지 확인 (임의 파일 접근 차단)
        if (!allowedPaths.contains(path)) {
            throw new MediaException(MediaError.PATH_NOT_ALLOWED, "이 시설물의 첨부가 아닙니다: " + path);
        }

        String projectId = resolveProjectId(sourceTable);
        byte[] body = downloadFile(projectId, path);

        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return new MediaContent(body, guessContentType(fileName), fileName);
    }

    /** source_table 이름의 접두어와 앞부분이 같은 QFieldCloud 프로젝트를 찾는다 */
    private String resolveProjectId(String sourceTable) {
        Matcher matcher = PROJECT_PREFIX.matcher(sourceTable == null ? "" : sourceTable);
        if (!matcher.find()) {
            throw new MediaException(MediaError.PROJECT_NOT_FOUND,
                    "source_table 에서 프로젝트 식별자를 찾지 못했습니다: " + sourceTable);
        }
        String prefix = matcher.group(1);

        String cached = projectIdCache.get(prefix);
        if (cached != null) {
            return cached;
        }

        String json = callApi("/api/v1/projects/", "프로젝트 목록 조회");
        try {
            JsonNode projects = objectMapper.readTree(json);
            for (JsonNode project : projects) {
                String id = project.path("id").asText("");
                if (id.startsWith(prefix)) {
                    projectIdCache.put(prefix, id);
                    return id;
                }
            }
        } catch (Exception e) {
            throw new MediaException(MediaError.UPSTREAM_ERROR, "프로젝트 목록을 읽지 못했습니다: " + e.getMessage());
        }

        throw new MediaException(MediaError.PROJECT_NOT_FOUND, "QFieldCloud 에서 프로젝트를 찾지 못했습니다: " + prefix);
    }

    private byte[] downloadFile(String projectId, String path) {
        // 경로의 각 구간만 인코딩한다(슬래시는 그대로 두어야 함)
        StringBuilder encodedPath = new StringBuilder();
        for (String segment : path.split("/")) {
            if (segment.isBlank()) continue;
            if (encodedPath.length() > 0) encodedPath.append('/');
            encodedPath.append(URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"));
        }

        String url = trimmedBaseUrl() + "/api/v1/files/" + projectId + "/" + encodedPath + "/";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Token " + getToken())
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) {
                throw new MediaException(MediaError.FILE_NOT_FOUND, "QFieldCloud 에 파일이 없습니다: " + path);
            }
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                // 토큰이 만료됐을 수 있으므로 한 번만 다시 발급해 재시도
                cachedToken = null;
                HttpRequest retry = HttpRequest.newBuilder(URI.create(url))
                        .header("Authorization", "Token " + getToken())
                        .timeout(Duration.ofSeconds(60))
                        .GET()
                        .build();
                HttpResponse<byte[]> retryResponse = httpClient.send(retry, HttpResponse.BodyHandlers.ofByteArray());
                if (retryResponse.statusCode() / 100 != 2) {
                    throw new MediaException(MediaError.UPSTREAM_ERROR,
                            "QFieldCloud 응답 오류: " + retryResponse.statusCode());
                }
                return retryResponse.body();
            }
            if (response.statusCode() / 100 != 2) {
                throw new MediaException(MediaError.UPSTREAM_ERROR, "QFieldCloud 응답 오류: " + response.statusCode());
            }
            return response.body();
        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaException(MediaError.UPSTREAM_ERROR, "QFieldCloud 요청 실패: " + e.getMessage());
        }
    }

    private String callApi(String apiPath, String what) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(trimmedBaseUrl() + apiPath))
                .header("Authorization", "Token " + getToken())
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new MediaException(MediaError.UPSTREAM_ERROR, what + " 실패: " + response.statusCode());
            }
            return response.body();
        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaException(MediaError.UPSTREAM_ERROR, what + " 실패: " + e.getMessage());
        }
    }

    /** 토큰 발급(캐시 사용). 비밀번호는 로그에 남기지 않는다. */
    private synchronized String getToken() {
        boolean expired = System.currentTimeMillis() - tokenIssuedAtMillis > TOKEN_TTL_MILLIS;
        if (cachedToken != null && !expired) {
            return cachedToken;
        }

        String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                escapeJson(username), escapeJson(password));
        HttpRequest request = HttpRequest.newBuilder(URI.create(trimmedBaseUrl() + "/api/v1/auth/login/"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new MediaException(MediaError.UPSTREAM_ERROR, "QFieldCloud 로그인 실패: " + response.statusCode());
            }
            String token = objectMapper.readTree(response.body()).path("token").asText("");
            if (token.isBlank()) {
                throw new MediaException(MediaError.UPSTREAM_ERROR, "QFieldCloud 로그인 응답에 토큰이 없습니다.");
            }
            cachedToken = token;
            tokenIssuedAtMillis = System.currentTimeMillis();
            log.info("QFieldCloud 토큰을 새로 발급했습니다.");
            return token;
        } catch (MediaException e) {
            throw e;
        } catch (Exception e) {
            throw new MediaException(MediaError.UPSTREAM_ERROR, "QFieldCloud 로그인 실패: " + e.getMessage());
        }
    }

    private String trimmedBaseUrl() {
        return baseUrl.replaceAll("/+$", "");
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * QFieldCloud 는 Content-Type 을 application.force-download 로 돌려주므로
     * 브라우저가 바로 재생할 수 있도록 확장자로 실제 타입을 정한다.
     */
    private static String guessContentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".heic")) return "image/heic";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".m4a")) return "audio/mp4";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".aac")) return "audio/aac";
        return "application/octet-stream";
    }
}
