package com.pelycan.api.repository;

import com.pelycan.api.domain.Helpline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HelplineRepository extends JpaRepository<Helpline, Integer> {

    List<Helpline> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
