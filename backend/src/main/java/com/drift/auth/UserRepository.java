package com.drift.auth;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email); // Spring Data derives a Mongo query from this method name

    boolean existsByEmail(String email);

}