package sneak_shop.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.entity.ContactEntity;
import sneak_shop.repository.ContactRepository;
import sneak_shop.security.UserContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactRepository contactRepository;
    private final ObjectMapper objectMapper;

    public ContactController(ContactRepository contactRepository, ObjectMapper objectMapper) {
        this.contactRepository = contactRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ApiResponse<Void> submit(
            @AuthenticationPrincipal UserContext ctx,
            @RequestBody Map<String, Object> body
    ) {
        String name = getString(body, "name");
        String email = getString(body, "email");
        String subject = getString(body, "subject");
        String message = getString(body, "message");

        String imageUrlsJson = null;
        Object rawUrls = body.get("imageUrls");
        if (rawUrls instanceof List<?> list && !list.isEmpty()) {
            try {
                imageUrlsJson = objectMapper.writeValueAsString(list);
            } catch (JsonProcessingException ignored) {}
        }

        ContactEntity.ContactEntityBuilder builder = ContactEntity.builder()
                .name(name != null ? name : "")
                .email(email != null ? email : "")
                .subject(subject)
                .message(message)
                .imageUrls(imageUrlsJson);

        if (ctx != null) {
            builder.userId(ctx.id());
        }

        contactRepository.save(builder.build());
        return ApiResponse.ok("Gui lien he thanh cong");
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<ContactResponse>> getMyContacts(
            @AuthenticationPrincipal UserContext ctx,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        PageResponse<ContactResponse> result;
        if (ctx.id() != null) {
            result = PageResponse.from(
                    contactRepository.findByUserIdOrderByCreatedAtDesc(ctx.id(), pageable)
                            .map(ContactResponse::from)
            );
        } else if (email != null && !email.isBlank()) {
            result = PageResponse.from(
                    contactRepository.findByEmailOrderByCreatedAtDesc(email, pageable)
                            .map(ContactResponse::from)
            );
        } else {
            result = PageResponse.from(
                    contactRepository.findAllByOrderByCreatedAtDesc(pageable)
                            .map(ContactResponse::from)
            );
        }

        return ApiResponse.ok(result);
    }

    private String getString(Map<String, Object> body, String key) {
        Object val = body.get(key);
        return val instanceof String s ? s.trim() : null;
    }

    public record ContactResponse(
            Integer id,
            String name,
            String email,
            String subject,
            String message,
            String imageUrls,
            String status,
            String replyText,
            Instant repliedAt,
            Instant createdAt,
            Integer userId
    ) {
        public static ContactResponse from(ContactEntity e) {
            return new ContactResponse(
                    e.getId(), e.getName(), e.getEmail(), e.getSubject(),
                    e.getMessage(), e.getImageUrls(), e.getStatus(),
                    e.getReplyText(), e.getRepliedAt(), e.getCreatedAt(), e.getUserId()
            );
        }
    }
}
