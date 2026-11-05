package com.grupo3.BookVerse.features.users.service;

import com.grupo3.BookVerse.features.users.dto.*;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponseDto createUser(UserRequestDto dto);

    List<UserResponseDto> getAllUsers();

    UserResponseDto getUserByIdExternal(UUID idExternal);

    UserResponseDto updateUser(UUID idExternal, UserUpdateRequestDto dto);

    UserResponseDto updateUserRoles(UUID idExternal, UserRolesUpdateRequestDto dto);

    void deleteUser(UUID idExternal);

    void deactivateOwnAccount(AccountDeactivationRequestDto dto);
}