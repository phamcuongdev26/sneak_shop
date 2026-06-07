package sneak_shop.service;

import org.springframework.stereotype.Component;
import sneak_shop.repository.UserRepository;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class UsernameGenerator {

    private final UserRepository userRepository;

    public UsernameGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generate(String... seeds) {
        String base = "user";
        if (seeds != null) {
            for (String seed : seeds) {
                if (seed != null && !seed.isBlank()) {
                    base = seed;
                    break;
                }
            }
        }

        String normalized = normalize(base);
        if (normalized.isBlank()) {
            normalized = "user";
        }
        if (normalized.length() > 30) {
            normalized = normalized.substring(0, 30);
        }

        String candidate = normalized;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            String suffixText = String.valueOf(suffix++);
            int maxBaseLength = Math.max(1, 30 - suffixText.length());
            String truncated = normalized.length() > maxBaseLength
                    ? normalized.substring(0, maxBaseLength)
                    : normalized;
            candidate = truncated + suffixText;
        }

        return candidate;
    }

    public String generateFromEmail(String email) {
        if (email == null || email.isBlank()) {
            return generate("user");
        }
        String localPart = email.trim();
        int atIndex = localPart.indexOf('@');
        if (atIndex > 0) {
            localPart = localPart.substring(0, atIndex);
        }
        return generate(localPart);
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        normalized = normalized.replace('đ', 'd');
        normalized = normalized.replaceAll("[^a-z0-9._-]+", "");
        normalized = normalized.replaceAll("[._-]{2,}", ".");
        normalized = normalized.replaceAll("^[._-]+|[._-]+$", "");
        return normalized;
    }
}
