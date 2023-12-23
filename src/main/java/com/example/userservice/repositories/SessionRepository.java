package com.example.userservice.repositories;

import com.example.userservice.models.Session;
import com.example.userservice.models.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByTokenAndUser_Id(String token, Long userId);

    int countByUser_IdAndSessionStatus(Long userId, SessionStatus sessionStatus);

}
