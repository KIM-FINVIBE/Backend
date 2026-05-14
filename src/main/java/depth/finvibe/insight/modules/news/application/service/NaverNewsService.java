package depth.finvibe.insight.modules.news.application.service;

import depth.finvibe.shared.config.AppConfig;
import depth.finvibe.shared.http.ApiException;
import depth.finvibe.shared.json.Json;
import depth.finvibe.shared.util.Maps;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class NaverNewsService {
    private static final String ECONOMY_QUERY = "경제 금융 증시 주식 투자";

    private final AppConfig config;
    private final HttpClient client;

    public NaverNewsService(AppConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.naverNewsTimeoutMs()))
                .build();
    }

    public Map<String, Object> economyNews(int start, int display) {
        if (!config.naverNewsEnabled()) {
            throw new ApiException(503, "NAVER_NEWS_NOT_CONFIGURED", "네이버 뉴스 API 키가 설정되어 있지 않습니다.");
        }

        int resolvedStart = Math.max(1, Math.min(1000, start));
        int resolvedDisplay = Math.max(1, Math.min(100, display));
        Map<String, Object> payload = requestNews(ECONOMY_QUERY, resolvedStart, resolvedDisplay);
        List<Map<String, Object>> items = normalizeItems(payload.get("items"));
        int total = Maps.intVal(payload, "total", items.size());
        int nextStart = resolvedStart + items.size();
        boolean hasMore = !items.isEmpty() && nextStart <= Math.min(total, 1000);

        return Maps.of(
                "items", items,
                "total", total,
                "start", resolvedStart,
                "display", resolvedDisplay,
                "nextStart", nextStart,
                "hasMore", hasMore,
                "query", ECONOMY_QUERY,
                "provider", "naver"
        );
    }

    private Map<String, Object> requestNews(String query, int start, int display) {
        String url = config.naverNewsBaseUrl()
                + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&display=" + display
                + "&start=" + start
                + "&sort=date";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(config.naverNewsTimeoutMs()))
                .header("X-Naver-Client-Id", config.naverNewsClientId())
                .header("X-Naver-Client-Secret", config.naverNewsClientSecret())
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(502, "NAVER_NEWS_REQUEST_FAILED", naverErrorMessage(response.statusCode(), response.body()));
            }
            return Json.parseObject(response.body());
        } catch (IOException e) {
            throw new ApiException(502, "NAVER_NEWS_NETWORK_ERROR", "네이버 뉴스 API 요청 중 네트워크 오류가 발생했습니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(502, "NAVER_NEWS_INTERRUPTED", "네이버 뉴스 API 요청이 중단되었습니다.");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> normalizeItems(Object value) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (!(value instanceof List<?> list)) {
            return rows;
        }
        int index = 0;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> raw = (Map<String, Object>) map;
            String link = firstNonBlank(Maps.str(raw, "originallink"), Maps.str(raw, "link"));
            String publishedAt = parsePubDate(Maps.str(raw, "pubDate"));
            rows.add(Maps.of(
                    "id", stableId(link, Maps.str(raw, "title"), index++),
                    "title", stripHtml(Maps.str(raw, "title")),
                    "summary", stripHtml(Maps.str(raw, "description")),
                    "description", stripHtml(Maps.str(raw, "description")),
                    "provider", "네이버 뉴스",
                    "publisher", "네이버 뉴스",
                    "url", link,
                    "link", link,
                    "originalLink", Maps.str(raw, "originallink"),
                    "naverLink", Maps.str(raw, "link"),
                    "publishedAt", publishedAt,
                    "createdAt", publishedAt,
                    "keyword", "ECONOMY",
                    "economicSignal", "NEUTRAL",
                    "analysis", "네이버 뉴스 검색 API로 불러온 경제 뉴스입니다.",
                    "likeCount", 0,
                    "discussionCount", 0
            ));
        }
        return rows;
    }

    private String stableId(String link, String title, int index) {
        String seed = firstNonBlank(link, title, "naver-news-" + index);
        return "naver-" + Integer.toUnsignedString(seed.hashCode());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stripHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replaceAll("(?i)<br\\s*/?>", " ")
                .replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String parsePubDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toString();
        } catch (Exception ignored) {
            return value;
        }
    }

    @SuppressWarnings("unchecked")
    private String naverErrorMessage(int statusCode, String body) {
        String upstreamMessage = "";
        try {
            Map<String, Object> payload = Json.parseObject(body);
            upstreamMessage = firstNonBlank(Maps.str(payload, "errorMessage"), Maps.str(payload, "message"));
        } catch (Exception ignored) {
        }
        if (statusCode == 401 || statusCode == 403) {
            return "네이버 뉴스 API 권한이 없거나 키가 올바르지 않습니다.";
        }
        return "네이버 뉴스 API 요청이 실패했습니다. status=" + statusCode + (upstreamMessage.isBlank() ? "" : " message=" + upstreamMessage);
    }
}
