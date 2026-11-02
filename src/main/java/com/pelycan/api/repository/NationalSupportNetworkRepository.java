package com.pelycan.api.repository;

import com.pelycan.api.domain.NationalSupportNetwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NationalSupportNetworkRepository extends JpaRepository<NationalSupportNetwork, UUID> {

    List<NationalSupportNetwork> findByActiveTrueOrderByOrderIndexAscNameAsc();

    List<NationalSupportNetwork> findByActiveTrueAndSectionOrderByOrderIndexAscNameAsc(String section);
}
