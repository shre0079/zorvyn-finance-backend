package com.shreyash.zorvyn.finance_dashboard_backend.mapper;


import com.shreyash.zorvyn.finance_dashboard_backend.dtos.CreateUserRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.RegisterRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.UpdateUserRequest;
import com.shreyash.zorvyn.finance_dashboard_backend.dtos.UserResponse;
import com.shreyash.zorvyn.finance_dashboard_backend.entities.User;
import org.mapstruct.*;

/**
 * MapStruct mapper for User entity ↔ DTO conversions.
 *
 * componentModel = "spring" → generates a Spring @Component bean
 * so it can be @Autowired / @RequiredArgsConstructor-injected.
 *
 * NOTE: passwordHash is intentionally excluded from all mappings.
 * Password hashing is handled in AuthService / UserService using BCrypt.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    /**
     * Entity → response DTO.
     * isActive maps to isActive; no rename needed (field names match).
     */
    UserResponse toResponse(User user);

    /**
     * RegisterRequest → entity (used during self-registration).
     * passwordHash and id are set by the service; role default applied in service.
     */
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "isActive",     constant = "true")
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "transactions", ignore = true)
    User toEntity(RegisterRequest request);

    /**
     * CreateUserRequest → entity (ADMIN user creation).
     */
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "isActive",     constant = "true")
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "transactions", ignore = true)
    User toEntity(CreateUserRequest request);

    /**
     * Partial update: copies non-null fields from UpdateUserRequest onto
     * an existing User entity. Fields with null values in the request are
     * ignored (NullValuePropertyMappingStrategy.IGNORE).
     */
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "email",        ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "transactions", ignore = true)
    void partialUpdate(
            @MappingTarget User target,
            UpdateUserRequest source);
}
