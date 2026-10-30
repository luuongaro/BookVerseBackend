package com.grupo3.BookVerse.features.tips.service;

import com.grupo3.BookVerse.features.tips.dto.TipCheckoutResponseDto;
import com.grupo3.BookVerse.features.tips.dto.TipRequestDto;
import com.grupo3.BookVerse.features.tips.dto.TipResponseDto;

import java.util.List;
import java.util.UUID;

public interface TipService {

    TipCheckoutResponseDto createTip(TipRequestDto tipRequestDto);

    List<TipResponseDto> getAllTips();

    TipResponseDto getTipByIdExternal(UUID idExternal);

    List<TipResponseDto> getTipsBySenderId(UUID senderId);

    List<TipResponseDto> getTipsByReceiverId(UUID receiverId);

    void deleteTip(UUID idExternal);
}