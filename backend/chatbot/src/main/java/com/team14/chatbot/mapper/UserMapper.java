package com.team14.chatbot.mapper;


import com.team14.chatbot.dto.request.UserCreationRequest;
import com.team14.chatbot.dto.request.UserUpdationRequest;
import com.team14.chatbot.dto.response.UserResponse;
import com.team14.chatbot.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser (UserCreationRequest userCreationRequest);
    UserResponse toUserResponse (User user);
    void update (@MappingTarget User user, UserUpdationRequest request);
}
