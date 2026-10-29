package com.grupo3.BookVerse.features.tips.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.payments.domain.PaymentEntity;
import com.grupo3.BookVerse.features.payments.domain.PaymentStatus;
import com.grupo3.BookVerse.features.payments.domain.PaymentType;
import com.grupo3.BookVerse.features.payments.repository.PaymentRepository;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.tips.domain.TipEntity;
import com.grupo3.BookVerse.features.tips.dto.TipRequestDto;
import com.grupo3.BookVerse.features.tips.dto.TipResponseDto;
import com.grupo3.BookVerse.features.tips.mappers.TipMapper;
import com.grupo3.BookVerse.features.tips.repository.TipRepository;
import com.grupo3.BookVerse.features.tips.service.TipService;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TipServiceImpl implements TipService {

    private final TipRepository tipRepository;
    private final TipMapper tipMapper;
    private final StoryRepository storyRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public TipResponseDto createTip(TipRequestDto tipRequestDto) {
        UserEntity sender = getAuthenticatedUser();

        StoryEntity story = findActiveStoryByIdExternal(
                tipRequestDto.getStoryId()
        );

        UserEntity receiver = story.getAuthor();

        validateReceiverExists(receiver);
        validateSenderIsNotReceiver(sender, receiver);

        PaymentEntity payment = PaymentEntity.builder()
                .user(sender)
                .type(PaymentType.TIP)
                .status(PaymentStatus.PENDING)
                .amount(tipRequestDto.getAmount())
                .currency("ARS")
                .provider("MERCADO_PAGO")
                .build();

        PaymentEntity savedPayment =
                paymentRepository.save(payment);

        TipEntity tip = tipMapper.toEntity(tipRequestDto);

        tip.setSender(sender);
        tip.setReceiver(receiver);
        tip.setStory(story);
        tip.setPayment(savedPayment);

        TipEntity savedTip = tipRepository.save(tip);

        return tipMapper.toResponseDto(savedTip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipResponseDto> getAllTips() {
        List<TipEntity> tips =
                tipRepository.findAllByPayment_StatusOrderByCreatedAtDesc(
                        PaymentStatus.APPROVED);

        return tipMapper.toResponseDtoList(tips);
    }

    @Override
    @Transactional(readOnly = true)
    public TipResponseDto getTipByIdExternal(UUID idExternal) {
        TipEntity tip = findTipByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanAccessTip(tip, authenticatedUser);

        return tipMapper.toResponseDto(tip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipResponseDto> getTipsBySenderId(UUID senderId) {
        List<TipEntity> tips =
                tipRepository
                        .findBySender_IdExternalAndPayment_StatusOrderByCreatedAtDesc(
                                senderId,
                                PaymentStatus.APPROVED
                        );

        return tipMapper.toResponseDtoList(tips);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TipResponseDto> getTipsByReceiverId(UUID receiverId) {
        List<TipEntity> tips =
                tipRepository
                        .findByReceiver_IdExternalAndPayment_StatusOrderByCreatedAtDesc(
                                receiverId,
                                PaymentStatus.APPROVED
                        );

        return tipMapper.toResponseDtoList(tips);
    }

    @Override
    @Transactional
    public void deleteTip(UUID idExternal) {
        TipEntity tip = findTipByIdExternal(idExternal);
        UserEntity authenticatedUser = getAuthenticatedUser();

        validateCanDeleteTip(tip, authenticatedUser);

        if (tip.getPayment().getStatus() == PaymentStatus.APPROVED) {
            throw new BadRequestException(
                    "An approved tip cannot be deleted"
            );
        }

        tipRepository.delete(tip);
    }

    private TipEntity findTipByIdExternal(UUID idExternal) {
        return tipRepository.findByIdExternal(idExternal)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Tip not found with idExternal: " + idExternal)
                );
    }

    private StoryEntity findActiveStoryByIdExternal(UUID storyId) {
        return storyRepository.findByIdExternalAndDeletedFalse(storyId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Story not found with idExternal: " + storyId
                        )
                );
    }

    private UserEntity getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof UserEntity user)) {
            throw new AccessDeniedException(
                    "Authenticated user not found"
            );
        }

        return user;
    }

    private void validateSenderIsNotReceiver(
            UserEntity sender,
            UserEntity receiver
    ) {
        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException(
                    "Users cannot send tips to their own stories"
            );
        }
    }

    private void validateCanDeleteTip(
            TipEntity tip,
            UserEntity authenticatedUser
    ) {
        boolean isOwner = tip.getSender() != null
                && tip.getSender().getId().equals(
                authenticatedUser.getId()
        );

        boolean isAdmin = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority())
                );

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException(
                    "You do not have permission to delete this tip"
            );
        }
    }

    private void validateReceiverExists(UserEntity receiver) {
        if (receiver == null) {
            throw new ResourceNotFoundException(
                    "The selected story does not have an author"
            );
        }
    }

    private void validateCanAccessTip(
            TipEntity tip,
            UserEntity authenticatedUser
    ) {
        boolean isSender = tip.getSender() != null
                && tip.getSender().getId().equals(
                authenticatedUser.getId()
        );

        boolean isReceiver = tip.getReceiver() != null
                && tip.getReceiver().getId().equals(
                authenticatedUser.getId()
        );

        boolean isAdmin = authenticatedUser.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority())
                );

        if (!isSender && !isReceiver && !isAdmin) {
            throw new AccessDeniedException(
                    "You do not have permission to access this tip"
            );
        }
    }
}