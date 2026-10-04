package com.quickcommerce.controller;

import com.quickcommerce.dto.AdminDtos.AddStockRequest;
import com.quickcommerce.dto.AdminDtos.AdminStoreDto;
import com.quickcommerce.dto.AdminDtos.CategoryDto;
import com.quickcommerce.dto.AdminDtos.InventoryItemDto;
import com.quickcommerce.dto.AdminDtos.NewProductRequest;
import com.quickcommerce.dto.AdminDtos.PackRequest;
import com.quickcommerce.dto.AdminDtos.PartnerSummaryDto;
import com.quickcommerce.dto.AdminDtos.ProductDto;
import com.quickcommerce.dto.AdminDtos.UpdateStockRequest;
import com.quickcommerce.dto.OrderDtos.OrderDto;
import com.quickcommerce.security.AuthUser;
import com.quickcommerce.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/stores")
    public List<AdminStoreDto> myStores(@AuthenticationPrincipal AuthUser me) {
        return admin.myStores(me.id());
    }

    @GetMapping("/stores/{storeId}/inventory")
    public List<InventoryItemDto> inventory(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId) {
        return admin.storeInventory(me.id(), storeId);
    }

    @PutMapping("/stores/{storeId}/inventory/{productId}")
    public InventoryItemDto updateStock(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId,
                                        @PathVariable Long productId, @Valid @RequestBody UpdateStockRequest req) {
        return admin.updateStock(me.id(), storeId, productId, req.quantity());
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories() {
        return admin.categories();
    }

    @GetMapping("/products")
    public List<ProductDto> catalog() {
        return admin.catalog();
    }

    @PostMapping("/stores/{storeId}/inventory")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemDto addToStore(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId,
                                       @Valid @RequestBody AddStockRequest req) {
        return admin.addToStore(me.id(), storeId, req);
    }

    @PostMapping("/stores/{storeId}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemDto createProduct(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId,
                                          @Valid @RequestBody NewProductRequest req) {
        return admin.createProduct(me.id(), storeId, req);
    }

    @DeleteMapping("/stores/{storeId}/inventory/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromStore(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId,
                                @PathVariable Long productId) {
        admin.removeFromStore(me.id(), storeId, productId);
    }

    @GetMapping("/stores/{storeId}/orders")
    public List<OrderDto> orders(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId) {
        return admin.storeOrders(me.id(), storeId);
    }

    @GetMapping("/stores/{storeId}/partners")
    public List<PartnerSummaryDto> partners(@AuthenticationPrincipal AuthUser me, @PathVariable Long storeId) {
        return admin.storePartners(me.id(), storeId);
    }

    @PostMapping("/orders/{orderId}/pack")
    public OrderDto pack(@AuthenticationPrincipal AuthUser me, @PathVariable Long orderId,
                         @Valid @RequestBody(required = false) PackRequest req) {
        return admin.pack(me.id(), orderId, req == null ? null : req.partnerId());
    }
}
