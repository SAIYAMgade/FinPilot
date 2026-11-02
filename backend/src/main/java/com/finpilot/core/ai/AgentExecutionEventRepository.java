package com.finpilot.core.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentExecutionEventRepository extends JpaRepository<AgentExecutionEvent, String> {
    List<AgentExecutionEvent> findByInvestigationIdOrderByCreatedAtAsc(String investigationId);
}
