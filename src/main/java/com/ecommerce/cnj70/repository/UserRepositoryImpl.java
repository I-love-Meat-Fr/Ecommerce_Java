package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<User> searchByKeyword(String q, Pageable pageable) {
        String safe = Pattern.quote(q.trim());
        Pattern emailPattern = Pattern.compile(safe, Pattern.CASE_INSENSITIVE);
        Pattern namePattern = Pattern.compile(safe, Pattern.CASE_INSENSITIVE);

        Criteria criteria = new Criteria().orOperator(
                Criteria.where("email").regex(emailPattern),
                Criteria.where("fullName").regex(namePattern)
        );

        Query query = Query.query(criteria).with(pageable);
        long total = mongoTemplate.count(Query.query(criteria), User.class);
        List<User> content = mongoTemplate.find(query, User.class);

        return new PageImpl<>(content, pageable, total);
    }
}