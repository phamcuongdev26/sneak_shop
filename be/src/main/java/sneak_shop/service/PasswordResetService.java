package sneak_shop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.entity.UserEntity;
import sneak_shop.repository.UserRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    private record OtpEntry(String otp, Instant expiresAt) {}
    private record ResetTokenEntry(String email, Instant expiresAt) {}

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, ResetTokenEntry> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> registerOtpStore = new ConcurrentHashMap<>();
    private final Map<String, Instant> verifiedRegisterEmailStore = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> emailVerifyOtpStore = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public PasswordResetService(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public void sendRegisterOtp(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.CONFLICT, "Email đã được sử dụng");
        }
        String otp = String.format("%06d", (int)(Math.random() * 1_000_000));
        registerOtpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(300)));
        verifiedRegisterEmailStore.remove(email);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[SNEAK SHOP] Mã xác nhận đăng ký tài khoản");
        msg.setText("Xin chào,\n\n"
                + "Mã OTP xác nhận đăng ký tài khoản Sneak Shop của bạn là:\n\n"
                + "    " + otp + "\n\n"
                + "Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với ai.\n\n"
                + "SNEAK SHOP");
        mailSender.send(msg);
    }

    public void verifyRegisterOtp(String email, String otp) {
        OtpEntry entry = registerOtpStore.get(email);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP đã hết hạn, vui lòng thử lại");
        }
        if (!entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không đúng");
        }
        registerOtpStore.remove(email);
        verifiedRegisterEmailStore.put(email, Instant.now().plusSeconds(600));
    }

    public void consumeVerifiedRegisterEmail(String email) {
        Instant expiresAt = verifiedRegisterEmailStore.get(email);
        if (expiresAt == null || Instant.now().isAfter(expiresAt)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Email chưa được xác thực OTP");
        }
        verifiedRegisterEmailStore.remove(email);
    }

    public void sendEmailVerificationOtp(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Email không tồn tại trong hệ thống"));
        String otp = String.format("%06d", (int) (Math.random() * 1_000_000));
        emailVerifyOtpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(300)));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[SNEAK SHOP] Mã xác thực email");
        msg.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Mã OTP để xác thực email của bạn là:\n\n"
                + "    " + otp + "\n\n"
                + "Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với ai.\n\n"
                + "SNEAK SHOP");
        mailSender.send(msg);
    }

    public void verifyEmailVerificationOtp(String email, String otp) {
        OtpEntry entry = emailVerifyOtpStore.get(email);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP đã hết hạn, vui lòng thử lại");
        }
        if (!entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không đúng");
        }
        emailVerifyOtpStore.remove(email);

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tài khoản không tồn tại"));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public void sendOtp(String email, String phone, String fullName) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Email không tồn tại trong hệ thống"));

        if (!phone.equals(user.getPhone())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thông tin không khớp");
        }
        if (!fullName.trim().equalsIgnoreCase(user.getFullName().trim())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thông tin không khớp");
        }

        String otp = String.format("%06d", (int)(Math.random() * 1_000_000));
        otpStore.put(email, new OtpEntry(otp, Instant.now().plusSeconds(300)));

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[SNEAK SHOP] Mã xác nhận đặt lại mật khẩu");
        msg.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Mã OTP để đặt lại mật khẩu của bạn là:\n\n"
                + "    " + otp + "\n\n"
                + "Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với ai.\n\n"
                + "SNEAK SHOP");
        mailSender.send(msg);
    }

    public String verifyOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP đã hết hạn, vui lòng thử lại");
        }
        if (!entry.otp().equals(otp)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mã OTP không đúng");
        }
        otpStore.remove(email);

        String token = UUID.randomUUID().toString();
        tokenStore.put(token, new ResetTokenEntry(email, Instant.now().plusSeconds(600)));
        return token;
    }

    public void resetPassword(String token, String newPassword) {
        ResetTokenEntry entry = tokenStore.get(token);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Token đã hết hạn, vui lòng bắt đầu lại");
        }
        tokenStore.remove(token);

        UserEntity user = userRepository.findByEmail(entry.email())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tài khoản không tồn tại"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
