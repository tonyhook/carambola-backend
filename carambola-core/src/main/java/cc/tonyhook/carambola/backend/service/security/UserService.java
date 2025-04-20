package cc.tonyhook.carambola.backend.service.security;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.security.UserRepository;
import cc.tonyhook.carambola.backend.entity.security.User;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT') || hasAuthority('AD_MANAGEMENT') || hasAuthority('AD_OPERATION')")
    public List<User> queryUserList(Query query) {
        List<User> userList = userRepository.findAll();

        userList.removeIf(user -> {
            if (!StringUtils.isEmpty(query.searchValue)) {
                for (String key : query.searchKey) {
                    String value = "";
                    if (key.equals("username")) {
                        value = user.getUsername().toLowerCase();
                    }
                    for (String fragment : query.searchValue.split(" ")) {
                        if (value.contains(fragment.toLowerCase())) {
                            return false;
                        }
                    }
                }
                return true;
            }

            return false;
        });

        return userList;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT') || hasAuthority('AD_MANAGEMENT') || hasAuthority('AD_OPERATION')")
    public List<User> getUserList() {
        List<User> userList = userRepository.findAll();

        return userList;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public User getUser(Integer id) {
        User user = userRepository.findById(id).orElse(null);

        return user;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public User addUser(User newUser) {
        User updatedUser = userRepository.save(newUser);

        return updatedUser;
    }

    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT') || hasAuthority('AD_MANAGEMENT') || hasAuthority('AD_OPERATION')")
    public void updateUser(Integer id, User newUser) {
        userRepository.save(newUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SECURITY_MANAGEMENT')")
    public void removeUser(Integer id) {
        User deletedUser = userRepository.findById(id).orElse(null);

        deletedUser.getRoles().clear();
        userRepository.save(deletedUser);

        userRepository.delete(deletedUser);
    }

}
