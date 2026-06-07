package sneak_shop.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sneak_shop.common.exception.AppException;
import sneak_shop.common.exception.ErrorCode;
import sneak_shop.common.response.PageResponse;
import sneak_shop.dto.request.MediaItem;
import sneak_shop.dto.request.ProductRequest;
import sneak_shop.dto.request.ProductVariantRequest;
import sneak_shop.dto.response.ProductResponse;
import sneak_shop.dto.response.ProductResponse.CategorySummary;
import sneak_shop.dto.response.ProductResponse.ColorSummary;
import sneak_shop.dto.response.ProductResponse.VariantSummary;
import sneak_shop.entity.*;
import sneak_shop.enums.ProductStatus;
import sneak_shop.repository.*;
import sneak_shop.security.UserContext;
import sneak_shop.service.ProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductShopRepository shopRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductCategoryMappingRepository mappingRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantColorRepository colorRepository;
    private final ProductImageRepository imageRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    public ProductServiceImpl(ProductRepository productRepository, ProductShopRepository shopRepository, ProductCategoryRepository categoryRepository, ProductCategoryMappingRepository mappingRepository, ProductVariantRepository variantRepository, ProductVariantColorRepository colorRepository, ProductImageRepository imageRepository, OrderItemRepository orderItemRepository, CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.shopRepository = shopRepository;
        this.categoryRepository = categoryRepository;
        this.mappingRepository = mappingRepository;
        this.variantRepository = variantRepository;
        this.colorRepository = colorRepository;
        this.imageRepository = imageRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(
            String keyword, BigDecimal minPrice, BigDecimal maxPrice,
            Integer categoryId, ProductStatus status, int page, int size, String sort
    ) {
        if ("sold".equals(sort) || "rating".equals(sort)) {
            String statusStr = status != null ? status.name() : null;
            Page<ProductEntity> pageResult = "sold".equals(sort)
                    ? productRepository.searchSortBySold(statusStr, minPrice, maxPrice, keyword, categoryId, PageRequest.of(page, size))
                    : productRepository.searchSortByRating(statusStr, minPrice, maxPrice, keyword, categoryId, PageRequest.of(page, size));
            Map<Integer, List<String>> colorsByProductId = loadColorPreviewContext(pageResult.getContent());
            return PageResponse.from(pageResult.map(p -> toListResponse(p, colorsByProductId)));
        }
        Sort sortSpec = switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Page<ProductEntity> pageResult = productRepository.search(
                status, minPrice, maxPrice, keyword, categoryId,
                PageRequest.of(page, size, sortSpec)
        );
        Map<Integer, List<String>> colorsByProductId = loadColorPreviewContext(pageResult.getContent());
        return PageResponse.from(pageResult.map(p -> toListResponse(p, colorsByProductId)));
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        ProductEntity product = productRepository.findBySlug(slug)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
        return toFullResponse(product, loadMetricsContext(List.of(product)));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Integer id) {
        ProductEntity product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
        return toFullResponse(product, loadMetricsContext(List.of(product)));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        String slug = uniqueSlug(toSlug(req.name()));
        String actor = currentUser();
        validateColorImages(req.variants());
        ProductShopEntity shop = resolveShop(req.shopId());
        BigDecimal resolvedPrice = resolveBasePrice(req.price(), req.variants());
        Integer resolvedStock = resolveStockQuantity(req.stockQuantity(), req.variants());
        ProductEntity product = ProductEntity.builder()
                .shop(shop)
                .name(req.name()).slug(slug).description(req.description())
                .price(resolvedPrice)
                .discountPercent(req.discountPercent() != null ? req.discountPercent() : 0)
                .stockQuantity(resolvedStock)
                .coverImageUrl(req.coverImageUrl())
                .sizeGuideNote(req.sizeGuideNote())
                .status(req.status() != null ? req.status() : ProductStatus.active)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        product = productRepository.save(product);
        syncCategories(product, req.categoryIds());
        syncImages(product, req.media());
        syncVariants(product, req.variants());
        return toFullResponse(reloadProduct(product.getId()), loadMetricsContext(List.of(product)));
    }

    @Transactional
    public ProductResponse update(Integer id, ProductRequest req) {
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
        validateColorImages(req.variants());

        String newSlugBase = toSlug(req.name());
        String slug = product.getSlug().equals(newSlugBase) ? product.getSlug() : uniqueSlug(newSlugBase);
        BigDecimal resolvedPrice = resolveBasePrice(req.price(), req.variants());
        Integer resolvedStock = resolveStockQuantity(req.stockQuantity(), req.variants());

        if (req.shopId() != null) product.setShop(resolveShop(req.shopId()));
        product.setName(req.name()); product.setSlug(slug);
        product.setDescription(req.description());
        product.setPrice(resolvedPrice);
        if (req.discountPercent() != null) product.setDiscountPercent(req.discountPercent());
        product.setStockQuantity(resolvedStock);
        if (req.coverImageUrl() != null) product.setCoverImageUrl(req.coverImageUrl());
        if (req.sizeGuideNote() != null) product.setSizeGuideNote(req.sizeGuideNote());
        if (req.status() != null) product.setStatus(req.status());
        product.setUpdatedBy(currentUser());
        product = productRepository.save(product);
        if (req.categoryIds() != null) syncCategories(product, req.categoryIds());
        if (req.media() != null) syncImages(product, req.media());
        if (req.variants() != null) syncVariants(product, req.variants());
        return toFullResponse(reloadProduct(product.getId()), loadMetricsContext(List.of(product)));
    }

    @Transactional
    public void delete(Integer id) {
        ProductEntity product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
        product.setDeleted(true);
        product.setUpdatedBy(currentUser());
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> adminSearch(String keyword, ProductStatus status, Boolean deleted, int page, int size) {
        Page<ProductEntity> pageResult = productRepository.adminSearch(
                deleted, status, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Map<Integer, List<String>> colorsByProductId = loadColorPreviewContext(pageResult.getContent());
        return PageResponse.from(pageResult.map(p -> toListResponse(p, colorsByProductId)));
    }

    @Transactional
    public void restore(Integer id) {
        ProductEntity product = productRepository.findById(id)
                .filter(ProductEntity::isDeleted)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai hoac chua bi xoa"));
        product.setDeleted(false);
        product.setUpdatedBy(currentUser());
        productRepository.save(product);
    }

    @Transactional
    public VariantSummary addVariant(Integer productId, ProductVariantRequest req) {
        ProductEntity product = productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
        validateColorImages(List.of(req));
        if (req.sku() != null && variantRepository.existsBySku(req.sku())) {
            throw new AppException(ErrorCode.CONFLICT, "SKU da ton tai");
        }
        String autoSku = req.sku() != null ? req.sku() : generateVariantSku(product, req.size());
        ProductVariantEntity variant = ProductVariantEntity.builder()
                .product(product)
                .size(req.size())
                .price(req.price())
                .sku(autoSku)
                .build();
        variant = variantRepository.save(variant);

        List<ColorSummary> colorSummaries = List.of();
        if (req.colors() != null && !req.colors().isEmpty()) {
            final ProductVariantEntity savedVariant = variant;
            List<ProductVariantColorEntity> colorEntities = req.colors().stream()
                    .filter(c -> c != null && c.color() != null && !c.color().isBlank())
                    .map(c -> ProductVariantColorEntity.builder()
                            .variant(savedVariant)
                            .color(c.color().trim())
                            .stockQuantity(c.stockQuantity() != null ? c.stockQuantity() : 0)
                            .imageUrl(c.imageUrl())
                            .build())
                    .toList();
            colorEntities = colorRepository.saveAll(colorEntities);
            colorSummaries = colorEntities.stream()
                    .map(c -> new ColorSummary(c.getId(), c.getColor(), c.getStockQuantity(), c.getImageUrl()))
                    .toList();
        }

        return new VariantSummary(variant.getId(), variant.getSize(), colorSummaries, variant.getPrice(), variant.getSku());
    }

    @Transactional
    public VariantSummary updateVariant(Integer productId, Integer variantId, ProductVariantRequest req) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .filter(v -> v.getProduct().getId().equals(productId))
                .filter(v -> !v.getProduct().isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Variant khong ton tai"));
        validateColorImages(List.of(req));

        if (req.sku() != null && !req.sku().equals(variant.getSku()) && variantRepository.existsBySku(req.sku())) {
            throw new AppException(ErrorCode.CONFLICT, "SKU da ton tai");
        }
        variant.setSize(req.size());
        variant.setPrice(req.price());
        variant.setSku(req.sku() != null && !req.sku().isBlank() ? req.sku() : variant.getSku());
        variant = variantRepository.save(variant);

        if (req.colors() != null) {
            colorRepository.deleteByVariantId(variant.getId());
            ProductVariantEntity savedVariant = variant;
            List<ProductVariantColorEntity> colors = req.colors().stream()
                    .filter(c -> c != null && c.color() != null && !c.color().isBlank())
                    .map(c -> ProductVariantColorEntity.builder()
                            .variant(savedVariant)
                            .color(c.color().trim())
                            .stockQuantity(c.stockQuantity() != null ? c.stockQuantity() : 0)
                            .imageUrl(c.imageUrl())
                            .build())
                    .toList();
            colorRepository.saveAll(colors);
        }

        List<ColorSummary> colorSummaries = colorRepository.findByVariantId(variant.getId())
                .stream().map(c -> new ColorSummary(c.getId(), c.getColor(), c.getStockQuantity(), c.getImageUrl()))
                .toList();
        return new VariantSummary(variant.getId(), variant.getSize(), colorSummaries, variant.getPrice(), variant.getSku());
    }

    @Transactional
    public void deleteVariant(Integer productId, Integer variantId) {
        ProductVariantEntity variant = variantRepository.findById(variantId)
                .filter(v -> v.getProduct().getId().equals(productId))
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Variant khong ton tai"));
        colorRepository.deleteByVariantId(variantId);
        variantRepository.delete(variant);
    }

    private void syncCategories(ProductEntity product, List<Integer> categoryIds) {
        mappingRepository.deleteByProductId(product.getId());
        if (categoryIds == null || categoryIds.isEmpty()) return;
        for (Integer catId : categoryIds) {
            categoryRepository.findById(catId).ifPresent(cat ->
                    mappingRepository.save(ProductCategoryMappingEntity.builder().product(product).category(cat).build())
            );
        }
    }

    private ProductResponse toFullResponse(ProductEntity product, ProductMetricsContext metrics) {
        Integer productId = product.getId();
        ProductShopEntity shop = product.getShop();
        List<CategorySummary> categories = product.getCategoryMappings().stream()
                .map(m -> new CategorySummary(
                        m.getCategory().getId(),
                        m.getCategory().getName(),
                        m.getCategory().getSlug()
                ))
                .toList();
        List<VariantSummary> variants = product.getVariants().stream()
                .map(v -> new VariantSummary(
                        v.getId(),
                        v.getSize(),
                        v.getColors().stream()
                                .map(c -> new ColorSummary(c.getId(), c.getColor(), c.getStockQuantity(), c.getImageUrl()))
                                .toList(),
                        v.getPrice(),
                        v.getSku()
                ))
                .toList();
        List<MediaItem> imageUrls = product.getImages().stream()
                .filter(img -> img.getType() == null || !"review".equalsIgnoreCase(img.getType()))
                .map(img -> new MediaItem(img.getId(), img.getImageUrl(), img.getType()))
                .toList();

        return new ProductResponse(
                productId,
                shop != null ? shop.getId() : null,
                shop != null ? shop.getName() : null,
                product.getName(), product.getSlug(), product.getDescription(),
                product.getPrice(), product.getDiscountPercent(), product.getStockQuantity(),
                product.getCoverImageUrl(), product.getSizeGuideNote(),
                imageUrls.isEmpty() ? null : imageUrls,
                product.getVariants().stream()
                        .flatMap(v -> v.getColors().stream())
                        .map(ProductVariantColorEntity::getColor)
                        .distinct()
                        .toList(),
                product.getStatus(), product.getCreatedAt(),
                product.getCreatedBy(), product.getUpdatedBy(),
                categories,
                variants,
                product.getRatingAverage() != null ? product.getRatingAverage().doubleValue() : 0d,
                product.getReviewCount() != null ? product.getReviewCount().longValue() : 0L,
                metrics.soldCountByProductId().getOrDefault(productId, 0L),
                product.isDeleted());
    }

    private ProductResponse toListResponse(ProductEntity product, Map<Integer, List<String>> colorsByProductId) {
        Integer productId = product.getId();
        ProductShopEntity shop = product.getShop();
        List<CategorySummary> categories = product.getCategoryMappings().stream()
                .map(m -> new CategorySummary(
                        m.getCategory().getId(),
                        m.getCategory().getName(),
                        m.getCategory().getSlug()
                ))
                .toList();

        return new ProductResponse(
                productId,
                shop != null ? shop.getId() : null,
                shop != null ? shop.getName() : null,
                product.getName(), product.getSlug(), null,
                product.getPrice(), product.getDiscountPercent(), product.getStockQuantity(),
                product.getCoverImageUrl(), null,
                List.of(),
                colorsByProductId.getOrDefault(productId, List.of()),
                product.getStatus(), product.getCreatedAt(),
                product.getCreatedBy(), product.getUpdatedBy(),
                categories,
                List.of(),
                product.getRatingAverage() != null ? product.getRatingAverage().doubleValue() : 0d,
                product.getReviewCount() != null ? product.getReviewCount().longValue() : 0L,
                0L,
                product.isDeleted());
    }

    private ProductMetricsContext loadMetricsContext(Collection<ProductEntity> products) {
        List<ProductEntity> list = products == null ? List.of() : products.stream()
                .filter(p -> p != null)
                .toList();
        if (list.isEmpty()) {
            return new ProductMetricsContext(Map.of());
        }

        List<Integer> productIds = list.stream().map(ProductEntity::getId).distinct().toList();
        Map<Integer, Long> soldCountByProductId = orderItemRepository.sumSoldByProductIds(productIds).stream()
                .collect(Collectors.toMap(
                        row -> (Integer) row[0],
                        row -> row[1] instanceof Number n ? n.longValue() : 0L
                ));

        return new ProductMetricsContext(soldCountByProductId);
    }

    private Map<Integer, List<String>> loadColorPreviewContext(Collection<ProductEntity> products) {
        List<ProductEntity> list = products == null ? List.of() : products.stream()
                .filter(p -> p != null)
                .toList();
        if (list.isEmpty()) {
            return Map.of();
        }
        List<Integer> productIds = list.stream().map(ProductEntity::getId).distinct().toList();
        return colorRepository.findColorNamesByProductIds(productIds).stream()
                .collect(Collectors.groupingBy(
                        row -> (Integer) row[0],
                        Collectors.mapping(row -> row[1] != null ? row[1].toString() : "", Collectors.toList())
                ));
    }

    private ProductEntity reloadProduct(Integer productId) {
        return productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "San pham khong ton tai"));
    }

    private String toSlug(String name) {
        if (name == null || name.isBlank()) return "san-pham";
        String s = name.toLowerCase()
                .replace("đ", "d")
                .replace("Đ", "d");
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = s.replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return s.isEmpty() ? "san-pham" : s;
    }

    private String uniqueSlug(String base) {
        if (!productRepository.existsBySlug(base)) return base;
        int i = 1;
        while (productRepository.existsBySlug(base + "-" + i)) i++;
        return base + "-" + i;
    }

    private String generateVariantSku(ProductEntity product, String size) {
        String base = product.getSlug().toUpperCase().replace("-", "");
        base = base.substring(0, Math.min(6, base.length()));
        String sizePart = size != null ? size.replaceAll("[^a-zA-Z0-9]", "") : "VAR";
        String suffix = String.valueOf(System.currentTimeMillis()).substring(9);
        return base + "-S" + sizePart + "-" + suffix;
    }

    private void syncImages(ProductEntity product, List<MediaItem> media) {
        if (media == null) return;
        imageRepository.deleteByProductId(product.getId());
        if (media.isEmpty()) return;
        for (int i = 0; i < media.size(); i++) {
            MediaItem item = media.get(i);
            if (item == null || item.url() == null || item.url().isBlank()) continue;
            imageRepository.save(ProductImageEntity.builder()
                    .product(product).imageUrl(item.url())
                    .type(item.type() != null ? item.type() : "image")
                    .sortOrder(i).build());
        }
    }

    private void syncVariants(ProductEntity product, List<ProductVariantRequest> variants) {
        if (variants == null) return;

        List<ProductVariantEntity> existingVariants = variantRepository.findByProductId(product.getId());
        Map<String, ProductVariantEntity> existingBySize = existingVariants.stream()
                .collect(Collectors.toMap(v -> v.getSize().trim().toLowerCase(), v -> v, (a, b) -> a));

        Set<Integer> keptVariantIds = new HashSet<>();

        for (ProductVariantRequest req : variants) {
            if (req == null || req.size() == null || req.size().isBlank()) continue;
            String sizeKey = req.size().trim().toLowerCase();
            ProductVariantEntity variant = existingBySize.get(sizeKey);

            if (variant != null) {
                variant.setPrice(req.price());
                variant = variantRepository.save(variant);
            } else {
                String sku = generateVariantSku(product, req.size());
                variant = ProductVariantEntity.builder()
                        .product(product).size(req.size().trim()).price(req.price()).sku(sku).build();
                variant = variantRepository.save(variant);
            }
            keptVariantIds.add(variant.getId());
            syncColorsForVariant(variant, req.colors());
        }

        for (ProductVariantEntity existing : existingVariants) {
            if (!keptVariantIds.contains(existing.getId())) {
                List<ProductVariantColorEntity> colors = colorRepository.findByVariantId(existing.getId());
                for (ProductVariantColorEntity c : colors) {
                    cartItemRepository.deleteByColorId(c.getId());
                    orderItemRepository.clearColorReference(c.getId());
                }
                colorRepository.deleteByVariantId(existing.getId());
                cartItemRepository.deleteByVariantId(existing.getId());
                orderItemRepository.clearVariantReference(existing.getId());
                variantRepository.deleteVariantById(existing.getId());
            }
        }
    }

    private void syncColorsForVariant(ProductVariantEntity variant, List<ProductVariantRequest.ColorRequest> colors) {
        List<ProductVariantColorEntity> existingColors = colorRepository.findByVariantId(variant.getId());
        Map<String, ProductVariantColorEntity> existingByName = existingColors.stream()
                .collect(Collectors.toMap(c -> c.getColor().trim().toLowerCase(), c -> c, (a, b) -> a));

        Set<Integer> keptColorIds = new HashSet<>();

        if (colors != null) {
            for (ProductVariantRequest.ColorRequest req : colors) {
                if (req == null || req.color() == null || req.color().isBlank()) continue;
                String colorKey = req.color().trim().toLowerCase();
                ProductVariantColorEntity color = existingByName.get(colorKey);

                if (color != null) {
                    color.setStockQuantity(req.stockQuantity() != null ? req.stockQuantity() : 0);
                    color.setImageUrl(req.imageUrl());
                    colorRepository.save(color);
                } else {
                    color = ProductVariantColorEntity.builder()
                            .variant(variant).color(req.color().trim())
                            .stockQuantity(req.stockQuantity() != null ? req.stockQuantity() : 0)
                            .imageUrl(req.imageUrl()).build();
                    colorRepository.save(color);
                }
                keptColorIds.add(color.getId());
            }
        }

        for (ProductVariantColorEntity existing : existingColors) {
            if (!keptColorIds.contains(existing.getId())) {
                cartItemRepository.deleteByColorId(existing.getId());
                orderItemRepository.clearColorReference(existing.getId());
                colorRepository.deleteById(existing.getId());
            }
        }
    }

    private void validateColorImages(List<ProductVariantRequest> variants) {
        if (variants == null) return;
        Map<String, String> colorImages = new HashMap<>();
        for (ProductVariantRequest variant : variants) {
            if (variant == null || variant.colors() == null) continue;
            for (ProductVariantRequest.ColorRequest color : variant.colors()) {
                if (color == null || color.color() == null || color.color().isBlank()) continue;
                String key = normalizeColorKey(color.color());
                String imageUrl = color.imageUrl() != null ? color.imageUrl().trim() : "";
                if (imageUrl.isBlank()) continue;
                String previous = colorImages.putIfAbsent(key, imageUrl);
                if (previous != null && !previous.equals(imageUrl)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Moi mau chi duoc gan mot anh duy nhat");
                }
            }
        }
    }

    private String normalizeColorKey(String value) {
        return value.trim().toLowerCase();
    }

    private BigDecimal resolveBasePrice(BigDecimal fallback, List<ProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) return fallback;
        return variants.stream()
                .filter(v -> v != null && v.price() != null)
                .map(ProductVariantRequest::price)
                .min(BigDecimal::compareTo)
                .orElse(fallback);
    }

    private Integer resolveStockQuantity(Integer fallback, List<ProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            return fallback != null ? fallback : 0;
        }
        int total = variants.stream()
                .filter(v -> v != null && v.colors() != null)
                .flatMap(v -> v.colors().stream())
                .filter(c -> c != null && c.stockQuantity() != null)
                .mapToInt(ProductVariantRequest.ColorRequest::stockQuantity)
                .sum();
        return total;
    }

    private ProductShopEntity resolveShop(Integer shopId) {
        if (shopId == null) return null;
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Shop khong ton tai"));
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserContext ctx) {
            return ctx.fullName();
        }
        return "system";
    }

    private record ProductMetricsContext(
            Map<Integer, Long> soldCountByProductId
    ) {}
}
