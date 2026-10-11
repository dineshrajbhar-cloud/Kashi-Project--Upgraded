package com.kashi.portal.user;

import com.kashi.portal.common.PageResponse;
import com.kashi.portal.common.Paging;
import com.kashi.portal.common.ResourceNotFoundException;
import com.kashi.portal.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.from(user);
    }

    public PageResponse<UserResponse> getUsers(int page, int size) {
        Pageable pageable = Paging.of(page, size, "createdAt,desc", Set.of("createdAt"));
        return PageResponse.from(userRepository.findAll(pageable).map(UserResponse::from));
    }
}
