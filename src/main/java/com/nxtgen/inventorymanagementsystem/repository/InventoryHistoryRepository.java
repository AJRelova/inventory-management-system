package com.nxtgen.inventorymanagementsystem.repository;

import com.nxtgen.inventorymanagementsystem.entity.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
    List<InventoryHistory> findByItemIdOrderByCreatedAtDesc(Long itemId);
}