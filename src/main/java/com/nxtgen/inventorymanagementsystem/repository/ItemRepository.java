package com.nxtgen.inventorymanagementsystem.repository;

import com.nxtgen.inventorymanagementsystem.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {
    Optional<Item> findByNameIgnoreCaseAndLocationIgnoreCase(String name, String location);
}
