package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.LegalDocument;
import com.ecommerce.cnj70.enums.LegalDocumentType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalDocumentRepository extends MongoRepository<LegalDocument, String> {

    Optional<LegalDocument> findByType(LegalDocumentType type);

    List<LegalDocument> findAllByTypeIn(List<LegalDocumentType> types);

    boolean existsByType(LegalDocumentType type);

    long countByTypeIn(List<LegalDocumentType> types);
}
