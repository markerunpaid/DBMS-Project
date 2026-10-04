package com.quickcommerce.security;

/**
 * The logged-in principal. {@code id} is the primary key in the table that matches
 * {@code role}: customer_id, employee_id or partner_id.
 */
public record AuthUser(Long id, Role role) {
}
