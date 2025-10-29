package com.team14.chatbot.repository;

import com.team14.chatbot.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findAllByConversationId (String conversationId);
}
