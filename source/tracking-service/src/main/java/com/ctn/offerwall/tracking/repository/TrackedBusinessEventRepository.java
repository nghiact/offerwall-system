package com.ctn.offerwall.tracking.repository;

import com.ctn.offerwall.tracking.domain.TrackedBusinessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface TrackedBusinessEventRepository extends JpaRepository<TrackedBusinessEvent, UUID>, JpaSpecificationExecutor<TrackedBusinessEvent> {
}
