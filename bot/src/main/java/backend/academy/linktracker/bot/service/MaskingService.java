package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.properties.MaskingProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MaskingService {

    public String maskQuery(String query, MaskingProperties maskingProperties) {
        if (query == null) return "";
        return Arrays.stream(query.split("&"))
            .map(param -> {
                String[] kv = param.split("=", 2);
                if (kv.length < 2) return param;
                String key = kv[0];
                if (maskingProperties.getKeys().stream().anyMatch(k -> key.toLowerCase().contains(k.toLowerCase()))) {
                    return key + "=***";
                }
                return param;
            })
            .collect(Collectors.joining("&"));
    }

    public String maskHeaders(HttpHeaders headers, MaskingProperties properties) {
        if (headers == null || headers.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : headers.headerNames()) {
            List<String> values = headers.get(key);
            boolean sensitive = properties.getHeaders().stream()
                .anyMatch(s -> s.equalsIgnoreCase(key));
            String maskedValues = sensitive ? "***" : String.join(", ", values);
            sb.append(key).append(": ").append(maskedValues).append("; ");
        }
        if (sb.length() > 0) sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    public String maskURI(String uri, MaskingProperties maskingProperties){
        if (uri == null) return "";
        return Arrays.stream(uri.split("/"))
            .map(param -> {
                if (maskingProperties.getKeys().stream().anyMatch(k -> param.toLowerCase().startsWith(k.toLowerCase()))) {
                    return param + "***";
                }
                return param;
            })
            .collect(Collectors.joining("/"));
    }

}
