package com.example.demo.mapper;

import com.example.demo.domain.UserAccount;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    List<UserAccount> findAll(@Param("keyword") String keyword,
                              @Param("role") String role,
                              @Param("active") Boolean active,
                              @Param("limit") int limit,
                              @Param("offset") int offset,
                              @Param("sortField") String sortField,
                              @Param("sortDirection") String sortDirection);

    long countAll(@Param("keyword") String keyword,
                  @Param("role") String role,
                  @Param("active") Boolean active);

    UserAccount findById(Long id);
    UserAccount findByUsername(String username);
    void insert(UserAccount user);
    int update(UserAccount user);
    int deleteById(Long id);
    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}
