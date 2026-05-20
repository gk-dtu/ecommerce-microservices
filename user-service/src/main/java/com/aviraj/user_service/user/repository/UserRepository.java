package com.aviraj.user_service.user.repository;

import com.aviraj.user_service.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

//DAO(data access object)
public interface UserRepository extends JpaRepository<User, Long> {
}