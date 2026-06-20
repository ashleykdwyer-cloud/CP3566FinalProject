package com.example.fraud.repo;

import com.example.fraud.model.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// The id type is String because a Rule's @Id is its code ("R1".."R4").
@Repository
public interface RuleRepository extends JpaRepository<Rule, String> { }
