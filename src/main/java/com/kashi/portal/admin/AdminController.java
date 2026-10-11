package com.kashi.portal.admin;

import com.kashi.portal.common.PageResponse;
import com.kashi.portal.user.UserService;
import com.kashi.portal.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    @Operation(summary = "List registered users, newest first (ADMIN)",
            description = "Returns safe user details only. Password hashes are never exposed.")
    public PageResponse<UserResponse> getUsers(
            @Parameter(description = "Page number, starting at 0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Items per page (1 to 50)")
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return userService.getUsers(page, size);
    }
}
