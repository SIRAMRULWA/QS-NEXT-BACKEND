package za.co.qsnext.employeemanagement.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import za.co.qsnext.employeemanagement.user.Permission;
import za.co.qsnext.employeemanagement.user.Role;
import za.co.qsnext.employeemanagement.user.User;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public UUID getUserId() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        Set<Role> roles = user.getRoles();

        Stream<GrantedAuthority> roleAuthorities =
                roles.stream()
                        .map(role ->
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role.getName()
                                )
                        );

        Stream<GrantedAuthority> permissionAuthorities =
                roles.stream()
                        .flatMap(role ->
                                role.getPermissions().stream()
                        )
                        .map(Permission::getName)
                        .map(SimpleGrantedAuthority::new);

        return Stream.concat(
                        roleAuthorities,
                        permissionAuthorities
                )
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}