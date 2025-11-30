package com.softwaremind.invoicedocbackend.security;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.softwaremind.invoicedocbackend.security.dto.UserSummaryDto;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserSummaryDto> listUsersForOrg(Long orgId) {
        return userRepository.findAllByOrganizationId(orgId).stream()
                .map(u -> new UserSummaryDto(
                        u.getId(),
                        u.getEmail(),
                        u.getFullName(),
                        u.getRole(),
                        u.isApprovedByOwner()
                ))
                .toList();
    }

    public void approveUser(Long idToApprove, CurrentUser currentUser) {
        changeUserApproval(idToApprove, currentUser, true);
    }

    public void deactivateUser(Long idToDeactivate, CurrentUser currentUser) {
        changeUserApproval(idToDeactivate, currentUser, false);
    }

    private void changeUserApproval(Long targetUserId, CurrentUser currentUser, boolean approved) {
        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (user.getOrganization() == null
                || !user.getOrganization().getId().equals(currentUser.organizationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "NO_ACCESS_TO_USER");
        }

        if (user.getRole() == UserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ADMIN_ALWAYS_ACTIVE"
            );
        }

        if (user.getId().equals(currentUser.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CANNOT_CHANGE_SELF_STATUS"
            );
        }

        if (approved) {
            if (currentUser.role() != UserRole.ADMIN
                    && currentUser.role() != UserRole.OWNER) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ONLY_OWNER_OR_ADMIN");
            }
        } else {
            if (currentUser.role() != UserRole.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ONLY_ADMIN_CAN_DEACTIVATE");
            }
        }

        user.setApprovedByOwner(approved);
        userRepository.save(user);
    }
}
