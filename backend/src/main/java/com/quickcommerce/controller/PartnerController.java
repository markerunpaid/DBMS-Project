package com.quickcommerce.controller;

import com.quickcommerce.dto.OrderDtos.OrderDto;
import com.quickcommerce.dto.PartnerDtos.PartnerProfileDto;
import com.quickcommerce.dto.PartnerDtos.StatusRequest;
import com.quickcommerce.security.AuthUser;
import com.quickcommerce.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/partner")
public class PartnerController {

    private final PartnerService partners;

    public PartnerController(PartnerService partners) {
        this.partners = partners;
    }

    @GetMapping("/me")
    public PartnerProfileDto profile(@AuthenticationPrincipal AuthUser me) {
        return partners.profile(me.id());
    }

    @PutMapping("/me/status")
    public PartnerProfileDto setStatus(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody StatusRequest req) {
        return partners.setStatus(me.id(), req.status());
    }

    @GetMapping("/deliveries")
    public List<OrderDto> deliveries(@AuthenticationPrincipal AuthUser me) {
        return partners.deliveries(me.id());
    }

    @PostMapping("/deliveries/{orderId}/pickup")
    public OrderDto pickUp(@AuthenticationPrincipal AuthUser me, @PathVariable Long orderId) {
        return partners.pickUp(me.id(), orderId);
    }

    @PostMapping("/deliveries/{orderId}/deliver")
    public OrderDto deliver(@AuthenticationPrincipal AuthUser me, @PathVariable Long orderId) {
        return partners.deliver(me.id(), orderId);
    }
}
