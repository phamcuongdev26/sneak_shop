package sneak_shop.controller.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.entity.ContactEntity;
import sneak_shop.repository.ContactRepository;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/contacts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminContactController {

    private final ContactRepository contactRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public AdminContactController(ContactRepository contactRepository, JavaMailSender mailSender) {
        this.contactRepository = contactRepository;
        this.mailSender = mailSender;
    }

    @GetMapping
    public ApiResponse<PageResponse<ContactResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContactEntity> contactPage;
        if (status != null && !status.isBlank()) {
            contactPage = contactRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            contactPage = contactRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return ApiResponse.ok(PageResponse.from(contactPage.map(ContactResponse::from)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContactResponse> getOne(@PathVariable Integer id) {
        ContactEntity contact = contactRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay lien he"));
        return ApiResponse.ok(ContactResponse.from(contact));
    }

    @PostMapping("/{id}/reply")
    public ApiResponse<ContactResponse> reply(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body
    ) {
        String replyText = body.get("replyText");
        if (replyText == null || replyText.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Noi dung phan hoi khong duoc de trong");
        }

        ContactEntity contact = contactRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Khong tim thay lien he"));

        contact.setReplyText(replyText);
        contact.setStatus("replied");
        contact.setRepliedAt(Instant.now());
        contactRepository.save(contact);

        sendReplyEmail(contact, replyText);

        return ApiResponse.ok("Da gui phan hoi thanh cong", ContactResponse.from(contact));
    }

    private void sendReplyEmail(ContactEntity contact, String replyText) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(contact.getEmail());
        msg.setSubject("[Sneak Shop] Phan hoi: " + (contact.getSubject() != null ? contact.getSubject() : ""));
        msg.setText("Xin chao " + contact.getName() + ",\n\n"
                + "Cam on ban da lien he voi Sneak Shop. Day la phan hoi cua chung toi:\n\n"
                + replyText + "\n\n"
                + "---\n"
                + "Tran trong,\n"
                + "Doi ngu ho tro Sneak Shop\n"
                + "Email: " + fromEmail);
        mailSender.send(msg);
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
