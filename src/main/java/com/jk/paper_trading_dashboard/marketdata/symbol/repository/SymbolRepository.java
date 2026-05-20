package com.jk.paper_trading_dashboard.marketdata.symbol.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jk.paper_trading_dashboard.marketdata.symbol.domain.AssetType;
import com.jk.paper_trading_dashboard.marketdata.symbol.domain.Symbol;

public interface SymbolRepository extends JpaRepository<Symbol, UUID> {

  Optional<Symbol> findBySymbol(String symbol);

  List<Symbol> findAllBySymbolIn(Collection<String> symbols);

  @Query("""
      SELECT s
      FROM Symbol s
      WHERE (:query IS NULL
        OR LOWER(s.symbol) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(s.displayName) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:assetType IS NULL OR s.assetType = :assetType)
        AND (:active IS NULL OR s.active = :active)
        AND (:tradable IS NULL OR s.tradable = :tradable)
      """)
  Page<Symbol> search(
      @Param("query") String query,
      @Param("assetType") AssetType assetType,
      @Param("active") Boolean active,
      @Param("tradable") Boolean tradable,
      Pageable pageable);
}
