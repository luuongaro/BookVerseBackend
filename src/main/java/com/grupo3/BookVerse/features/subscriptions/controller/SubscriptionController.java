package com.grupo3.BookVerse.features.subscriptions.controller;

import com.grupo3.BookVerse.features.subscriptions.dto.PremiumSubscriptionCheckoutResponseDto;
import com.grupo3.BookVerse.features.subscriptions.dto.SubscriptionChangeRequestDto;
import com.grupo3.BookVerse.features.subscriptions.dto.SubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.dto.UserSubscriptionResponseDto;
import com.grupo3.BookVerse.features.subscriptions.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(
        name = "Subscriptions",
        description = "Endpoints for retrieving subscription plans, managing authenticated user subscriptions and administering user subscription changes"
)
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get all subscriptions",
            description = "Retrieves all subscription plans available in the system.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<List<SubscriptionResponseDto>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @GetMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get subscription by external id",
            description = "Retrieves a specific subscription plan by its external UUID.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Subscription not found", content = @Content)
    })
    public ResponseEntity<SubscriptionResponseDto> getSubscriptionByIdExternal(
            @Parameter(
                    description = "External UUID of the subscription",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionByIdExternal(idExternal));
    }

    @PostMapping("/upgrade-to-premium")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Start monthly PREMIUM subscription",
            description = "Creates a pending monthly PREMIUM subscription in Mercado Pago and returns the checkout URL.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Monthly PREMIUM subscription checkout created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "User already has a PREMIUM, active or pending subscription",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "PREMIUM subscription plan not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Error communicating with Mercado Pago",
                    content = @Content
            )
    })
    public ResponseEntity<PremiumSubscriptionCheckoutResponseDto>
    upgradeToPremium() {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        subscriptionService
                                .initiateAuthenticatedUserPremiumSubscription()
                );
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get authenticated user subscription",
            description = "Retrieves the subscription currently assigned to the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<SubscriptionResponseDto> getAuthenticatedUserSubscription() {
        return ResponseEntity.ok(subscriptionService.getAuthenticatedUserSubscription());
    }


    @GetMapping("/users/{userIdExternal}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get user subscription",
            description = "Allows an administrator to retrieve the subscription currently assigned to a specific user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User subscription retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied. Administrator role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    public ResponseEntity<SubscriptionResponseDto> getUserSubscription(
            @Parameter(description = "External UUID of the user", required = true)
            @PathVariable UUID userIdExternal) {
        return ResponseEntity.ok(subscriptionService.getUserSubscription(userIdExternal));
    }

    @PostMapping("/downgrade-to-free")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Downgrade authenticated user to FREE",
            description = "Changes the authenticated user's current subscription from PREMIUM to FREE.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User downgraded to FREE successfully"),
            @ApiResponse(responseCode = "400", description = "User already has a FREE subscription", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "FREE subscription plan not found", content = @Content)
    })
    public ResponseEntity<SubscriptionResponseDto> downgradeToFree() {
        return ResponseEntity.ok(subscriptionService.downgradeAuthenticatedUserToFree());
    }

    @PutMapping("/users/{userIdExternal}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Change user subscription",
            description = "Allows an administrator to change the subscription assigned to a specific user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User subscription changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid subscription change or user already has the selected subscription", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied. Administrator role required", content = @Content),
            @ApiResponse(responseCode = "404", description = "User or subscription plan not found", content = @Content)
    })
    public ResponseEntity<SubscriptionResponseDto> changeUserSubscription(
            @Parameter(description = "External UUID of the user", required = true)
            @PathVariable UUID userIdExternal,
            @Valid
            @RequestBody SubscriptionChangeRequestDto request) {
        return ResponseEntity.ok(subscriptionService.changeUserSubscription(userIdExternal, request.getType()));
    }

    @PostMapping("/premium/synchronize")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Synchronize PREMIUM subscription",
            description = "Retrieves the authenticated user's Mercado Pago subscription and synchronizes the subscription status and BookVerse plan.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "PREMIUM subscription synchronized successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Subscription does not contain valid provider information",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User subscription or BookVerse plan not found",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Error communicating with Mercado Pago",
                    content = @Content
            )
    })
    public ResponseEntity<UserSubscriptionResponseDto>
    synchronizePremiumSubscription() {

        return ResponseEntity.ok(
                subscriptionService
                        .synchronizeAuthenticatedUserPremiumSubscription()
        );
    }
}