package com.grupo3.BookVerse.features.tips.controller;

import com.grupo3.BookVerse.features.tips.dto.TipCheckoutResponseDto;
import com.grupo3.BookVerse.features.tips.dto.TipRequestDto;
import com.grupo3.BookVerse.features.tips.dto.TipResponseDto;
import com.grupo3.BookVerse.features.tips.service.TipService;
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
@RequestMapping("/api/tips")
@RequiredArgsConstructor
@Tag(
        name = "Tips",
        description = "Endpoints for managing tips associated with BookVerse stories"
)
public class TipController {

    private final TipService tipService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Create a new tip",
            description = "Creates a pending tip payment for a story. The sender is obtained from the authenticated user and the receiver from the story author.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Tip payment initiated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request body or user attempted to tip their own story",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Story or story author not found",
                    content = @Content
            )
    })
    public ResponseEntity<TipCheckoutResponseDto> createTip(
            @Valid @RequestBody TipRequestDto tipRequestDto
    ) {
        TipCheckoutResponseDto createdTip =
                tipService.createTip(tipRequestDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdTip);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all approved tips",
            description = "Retrieves all approved tips in the system. Administrator access only.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Approved tips retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden. Administrator role required",
                    content = @Content
            )
    })
    public ResponseEntity<List<TipResponseDto>> getAllTips() {
        return ResponseEntity.ok(
                tipService.getAllTips()
        );
    }

    @GetMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get tip by external id",
            description = "Retrieves a specific tip. Access is restricted to the sender, receiver or an administrator.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tip retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have permission to access this tip",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tip not found",
                    content = @Content
            )
    })
    public ResponseEntity<TipResponseDto> getTipByIdExternal(
            @Parameter(
                    description = "External UUID of the tip",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        return ResponseEntity.ok(
                tipService.getTipByIdExternal(idExternal)
        );
    }

    @GetMapping("/sender/{senderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get approved tips by sender",
            description = "Allows an administrator to retrieve approved tips sent by a specific user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tips retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden. Administrator role required",
                    content = @Content
            )
    })
    public ResponseEntity<List<TipResponseDto>> getTipsBySenderId(
            @Parameter(
                    description = "External UUID of the sender user",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID senderId
    ) {
        return ResponseEntity.ok(
                tipService.getTipsBySenderId(senderId)
        );
    }

    @GetMapping("/receiver/{receiverId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get approved tips by receiver",
            description = "Allows an administrator to retrieve approved tips received by a specific user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tips retrieved successfully"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden. Administrator role required",
                    content = @Content
            )
    })
    public ResponseEntity<List<TipResponseDto>> getTipsByReceiverId(
            @Parameter(
                    description = "External UUID of the receiver user",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID receiverId
    ) {
        return ResponseEntity.ok(
                tipService.getTipsByReceiverId(receiverId)
        );
    }

    @DeleteMapping("/{idExternal}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Delete a pending tip",
            description = "Deletes a tip that has not been approved. Only the sender or an administrator can perform this operation.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Tip deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "An approved tip cannot be deleted",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have permission to delete this tip",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tip not found",
                    content = @Content
            )
    })
    public ResponseEntity<Void> deleteTip(
            @Parameter(
                    description = "External UUID of the tip to delete",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID idExternal
    ) {
        tipService.deleteTip(idExternal);
        return ResponseEntity.noContent().build();
    }
}