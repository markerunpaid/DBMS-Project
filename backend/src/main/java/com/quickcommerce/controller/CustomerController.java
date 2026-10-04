package com.quickcommerce.controller;

import com.quickcommerce.dto.CustomerDtos.AddressDto;
import com.quickcommerce.dto.CustomerDtos.CouponDto;
import com.quickcommerce.dto.CustomerDtos.NearestStoreDto;
import com.quickcommerce.dto.CustomerDtos.NewAddressRequest;
import com.quickcommerce.dto.CustomerDtos.StoreItemDto;
import com.quickcommerce.dto.OrderDtos.OrderDto;
import com.quickcommerce.dto.OrderDtos.PlaceOrderRequest;
import com.quickcommerce.dto.OrderDtos.ReceiptDto;
import com.quickcommerce.security.AuthUser;
import com.quickcommerce.service.CustomerService;
import com.quickcommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService customers;
    private final OrderService orders;

    public CustomerController(CustomerService customers, OrderService orders) {
        this.customers = customers;
        this.orders = orders;
    }

    @GetMapping("/addresses")
    public List<AddressDto> addresses(@AuthenticationPrincipal AuthUser me) {
        return customers.addresses(me.id());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressDto addAddress(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody NewAddressRequest req) {
        return customers.addAddress(me.id(), req);
    }

    @GetMapping("/stores/nearest")
    public List<NearestStoreDto> nearestStores(@AuthenticationPrincipal AuthUser me, @RequestParam Long addressId) {
        return customers.nearestStores(me.id(), addressId);
    }

    @GetMapping("/stores/{storeId}/items")
    public List<StoreItemDto> storeItems(@PathVariable Long storeId) {
        return customers.storeItems(storeId);
    }

    @GetMapping("/coupons")
    public List<CouponDto> coupons(@AuthenticationPrincipal AuthUser me) {
        return customers.usableCoupons(me.id());
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto placeOrder(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PlaceOrderRequest req) {
        return orders.place(me.id(), req);
    }

    @GetMapping("/orders")
    public List<OrderDto> myOrders(@AuthenticationPrincipal AuthUser me) {
        return orders.myOrders(me.id());
    }

    @GetMapping("/orders/{orderId}")
    public OrderDto myOrder(@AuthenticationPrincipal AuthUser me, @PathVariable Long orderId) {
        return orders.myOrder(me.id(), orderId);
    }

    @GetMapping("/orders/{orderId}/receipt")
    public ReceiptDto receipt(@AuthenticationPrincipal AuthUser me, @PathVariable Long orderId) {
        return orders.receipt(me.id(), orderId);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public OrderDto cancel(@AuthenticationPrincipal AuthUser me, @PathVariable Long orderId) {
        return orders.cancel(me.id(), orderId);
    }
}
