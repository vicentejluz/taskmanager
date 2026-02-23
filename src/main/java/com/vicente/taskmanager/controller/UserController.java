package com.vicente.taskmanager.controller;

import com.vicente.taskmanager.dto.filter.UserFilterDTO;
import com.vicente.taskmanager.dto.request.PasswordUpdateRequestDTO;
import com.vicente.taskmanager.dto.request.UpdateUserEnabledRequest;
import com.vicente.taskmanager.dto.request.UserUpdateRequestDTO;
import com.vicente.taskmanager.dto.response.PageResponseDTO;
import com.vicente.taskmanager.dto.response.UserAdminResponseDTO;
import com.vicente.taskmanager.dto.response.UserResponseDTO;
import com.vicente.taskmanager.dto.response.UserUpdateResponseDTO;
import com.vicente.taskmanager.exception.error.StandardError;
import com.vicente.taskmanager.model.entity.User;
import com.vicente.taskmanager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "Users endpoints")
@RestController
@RequestMapping(value = "/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get authenticated user",
            description = "Returns the profile of the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @GetMapping("/users/me")
    public ResponseEntity<UserResponseDTO> getMe(@AuthenticationPrincipal User user) {
        logger.debug("GET /api/v1/users/me getMe called | userId={}", user.getId());
        UserResponseDTO userResponseDTO = userService.getMe(user);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Operation(
            summary = "Update authenticated user",
            description = "Updates the profile information of the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserUpdateResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            )
    })
    @PatchMapping("/users/me")
    public ResponseEntity<UserUpdateResponseDTO> update(
            @Valid @RequestBody UserUpdateRequestDTO request,
            @AuthenticationPrincipal User user
    ) {
        logger.debug("PATCH /api/v1/users/update update called | authenticatedUserId={}", user.getId());
        UserUpdateResponseDTO responseDTO = userService.update(user, request);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(
            summary = "Change authenticated user password",
            description = """
                Changes the password of the currently authenticated user.
                The old password must match the current password.
                The new password must be different from the current one.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Password changed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or business rule violation",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class))
            )
    })
    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordUpdateRequestDTO passwordUpdateRequestDTO
    ) {
        logger.debug("PATCH /api/v1/users/password changePassword called | authenticatedUserId={}", user.getId());
        userService.changePassword(user, passwordUpdateRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Delete authenticated user",
            description = """
            Performs a soft delete of the currently authenticated user account.
            
            Business rules:
            - The user is marked as deleted (soft delete).
            - The account can no longer authenticate after deletion.
            
            Requires USER role.
            """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - USER role required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @DeleteMapping("/users/me/delete")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User user) {
        logger.debug("DELETE /api/v1/users/me/delete delete called | authenticatedUserId={}", user.getId());
        userService.delete(user);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Admin - Enable or disable a user",
            description = """
                Enables or disables a user account by its identifier.
                Requires ADMIN role.
                
                Business rules:
                - Admin users cannot be disabled.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "User status updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or business rule violation",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @PatchMapping("/admin/users/{id}/enabled")
    public ResponseEntity<Void> updateUserEnabled(
            @PathVariable Long id,
            @Valid @RequestBody() UpdateUserEnabledRequest updateUserEnabledRequest
    ) {
        userService.updateUserEnabled(id, updateUserEnabledRequest.enabled());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Admin - Find user by ID",
            description = "Returns a user by its unique identifier. Requires ADMIN role."
    )@ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User found successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserAdminResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid id parameter",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @GetMapping("/admin/users/{id}")
    public ResponseEntity<UserAdminResponseDTO> findById(@PathVariable Long id) {
        logger.debug("GET /api/v1/users/{id} findById called | userId={}", id);
        UserAdminResponseDTO userResponseDTO = userService.findById(id);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Operation(
            summary = "Admin - Find user by email",
            description = """
                Returns a user by its unique email address.
                Requires ADMIN role.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserAdminResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid email parameter",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            )
    })
    @GetMapping("/admin/users/by-email")
    public ResponseEntity<UserAdminResponseDTO> findByEmail(@RequestParam String email) {
        logger.debug("GET /api/v1/users/by-email findByEmail called | email={}", email);
        UserAdminResponseDTO userResponseDTO = userService.findByEmail(email);
        return ResponseEntity.ok(userResponseDTO);
    }

    @Operation(
            summary = "Admin - Find user",
            description =
                    """
                    Retrieves a paginated list of users based on optional filter parameters.
    
                    All filters are optional:
                    - name: Performs a partial match (case-insensitive) on the user's name.
                    - enabled: Filters users by enabled status.
                    - locked-account: Filters users by account lock status.
    
                    If no filters are provided, all users are returned paginated.
    
                    This endpoint requires ADMIN role.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageResponseDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid filter parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StandardError.class)
                    )
            ),
    })
    @GetMapping("/admin/users")
    public ResponseEntity<PageResponseDTO<UserAdminResponseDTO>> find(
            @ParameterObject UserFilterDTO filter,
            @ParameterObject Pageable pageable) {
        logger.debug("GET /api/v1/admin/users find called | filters: name={} enabled={} accountNonLocked={}",
                filter.name(), filter.enabled(), filter.accountNonLocked());
        PageResponseDTO<UserAdminResponseDTO> pageResponseDTO = userService.find(filter, pageable);
        if (pageResponseDTO.content().isEmpty()) {
            logger.debug("GET /api/v1/admin/users returned empty result");
        }
        return ResponseEntity.ok(pageResponseDTO);
    }
}
