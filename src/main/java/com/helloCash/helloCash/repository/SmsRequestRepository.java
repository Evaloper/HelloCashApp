package com.helloCash.helloCash.repository;

import com.helloCash.helloCash.payload.request.SmsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsRequestRepository extends JpaRepository<SmsRequest, Long> {
}
