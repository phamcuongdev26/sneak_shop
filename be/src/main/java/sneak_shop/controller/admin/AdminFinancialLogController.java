package sneak_shop.controller.admin;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.response.FinancialLogResponse;
import sneak_shop.entity.FinancialLogEntity;
import sneak_shop.repository.FinancialLogRepository;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFinancialLogController {

    private final FinancialLogRepository repository;

    public AdminFinancialLogController(FinancialLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<PageResponse<FinancialLogResponse>> getAll(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer ordersId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<FinancialLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (email != null && !email.isBlank())
                predicates.add(cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%"));
            if (ordersId != null)
                predicates.add(cb.equal(root.get("ordersId"), ordersId));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ApiResponse.ok(PageResponse.from(repository.findAll(spec, pageable).map(FinancialLogResponse::from)));
    }

    @PostMapping
    public ApiResponse<FinancialLogResponse> create(@RequestBody FinancialLogEntity req) {
        req.setId(null);
        return ApiResponse.ok(FinancialLogResponse.from(repository.save(req)));
    }
}
