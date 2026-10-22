package com.grupo3.BookVerse.auth.service;

import com.grupo3.BookVerse.auth.dtos.AuthRequest;
import com.grupo3.BookVerse.auth.dtos.AuthResponse;
import com.grupo3.BookVerse.auth.dtos.RegisterRequest;
import com.grupo3.BookVerse.features.users.dto.UserResponseDto;

public interface AuthService {

    AuthResponse authenticate(AuthRequest request);

    UserResponseDto register(RegisterRequest request);
}