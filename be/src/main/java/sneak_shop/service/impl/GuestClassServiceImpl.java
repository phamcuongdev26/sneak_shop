package sneak_shop.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.GuestClassRequest;
import sneak_shop.dto.response.GuestClassResponse;
import sneak_shop.entity.GuestClassEntity;
import sneak_shop.repository.GuestClassRepository;
import sneak_shop.repository.ProductRepository;
import sneak_shop.repository.UserRepository;
import sneak_shop.service.GuestClassService;

import java.util.List;

@Service
public class GuestClassServiceImpl implements GuestClassService {

    private final GuestClassRepository guestClassRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public GuestClassServiceImpl(GuestClassRepository guestClassRepository,
                                 UserRepository userRepository,
                                 ProductRepository productRepository) {
        this.guestClassRepository = guestClassRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<GuestClassResponse> getAll(int page, int size) {
        return PageResponse.from(
                guestClassRepository.findAllByOrderByIdDesc(
                        PageRequest.of(page, size)).map(GuestClassResponse::from));
    }

    @Transactional(readOnly = true)
    public List<GuestClassResponse> getByUser(Integer userId) {
        return guestClassRepository.findByUserIdOrderByTotalAmountDesc(userId)
                .stream().map(GuestClassResponse::from).toList();
    }

    @Transactional
    public GuestClassResponse create(GuestClassRequest req) {
        var user = userRepository.findById(req.userId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        var product = productRepository.findById(req.productId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));

        GuestClassEntity entity = GuestClassEntity.builder()
                .user(user)
                .rankName(req.rankName())
                .product(product)
                .totalAmount(req.totalAmount())
                .description(req.description())
                .color(req.color())
                .build();
        return GuestClassResponse.from(guestClassRepository.save(entity));
    }

    @Transactional
    public GuestClassResponse update(Integer id, GuestClassRequest req) {
        GuestClassEntity entity = guestClassRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "GuestClass khong ton tai"));
        var user = userRepository.findById(req.userId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User khong ton tai"));
        var product = productRepository.findById(req.productId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));

        entity.setUser(user);
        entity.setRankName(req.rankName());
        entity.setProduct(product);
        entity.setTotalAmount(req.totalAmount());
        entity.setDescription(req.description());
        entity.setColor(req.color());
        return GuestClassResponse.from(guestClassRepository.save(entity));
    }

    @Transactional
    public void delete(Integer id) {
        if (!guestClassRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "GuestClass khong ton tai");
        }
        guestClassRepository.deleteById(id);
    }
}
