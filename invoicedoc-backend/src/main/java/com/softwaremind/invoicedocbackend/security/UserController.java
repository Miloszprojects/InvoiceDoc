package com.softwaremind.invoicedocbackend.security;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.softwaremind.invoicedocbackend.security.dto.UserSummaryDto;

@RestController
@RequestMapping("/v1/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public List<UserSummaryDto> listUsers() {
        CurrentUser current = currentUserProvider.getCurrentUser();
        return userService.listUsersForOrg(current.organizationId());
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','OWNER')")
    public void approveUser(@PathVariable Long id) {
        CurrentUser current = currentUserProvider.getCurrentUser();
        userService.approveUser(id, current);
    }
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivateUser(@PathVariable Long id) {
        CurrentUser current = currentUserProvider.getCurrentUser();
        userService.deactivateUser(id, current);
    }
}
