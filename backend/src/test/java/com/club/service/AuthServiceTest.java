package com.club.service;

import com.club.dao.UserDao;
import com.club.entity.User;
import com.club.util.PasswordUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthServiceTest {
    private static class FakeDao extends UserDao {
        User stored;
        public boolean existByUsername(String username) { return false; }
        public int insert(User user) { stored = user; user.setId(1L); return 1; }
        public User findByUsername(String username) {
            if (stored == null) return null;
            User copy = new User();
            copy.setPassword(stored.getPassword());
            copy.setRole(stored.getRole());
            return copy;
        }
    }

    @Test public void rejectsAdminBeforeDatabaseWrite() {
        FakeDao dao = new FakeDao();
        assertThrows(IllegalArgumentException.class, () ->
                new AuthService(dao).register("attacker", "secret123", "admin", "测试", "13800138000"));
        assertNull(dao.stored);
    }

    @Test public void omittedRoleCreatesStudentWithHashAndCanLogin() {
        FakeDao dao = new FakeDao();
        AuthService service = new AuthService(dao);
        service.register("student", "secret123", null, "测试", "13800138000");
        assertEquals("student", dao.stored.getRole());
        assertTrue(PasswordUtil.isHash(dao.stored.getPassword()));
        assertNull(service.login("student", "wrong"));
        User loggedIn = service.login("student", "secret123");
        assertNotNull(loggedIn);
        assertNull(loggedIn.getPassword());
        assertTrue(PasswordUtil.isHash(dao.stored.getPassword()));
    }

    @Test public void rejectsPlaintextMalformedAndOversizedPasswords() {
        assertFalse(PasswordUtil.matches("secret123", "secret123"));
        assertFalse(PasswordUtil.matches("secret123", "$2a$broken"));
        String password = "中".repeat(25);
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash(password));
        assertFalse(PasswordUtil.matches(password, PasswordUtil.hash("secret123")));
    }

    @Test public void usesRandomSaltAndVerifiesUnicode() {
        String first = PasswordUtil.hash("密码123456");
        String second = PasswordUtil.hash("密码123456");
        assertNotEquals(first, second);
        assertTrue(PasswordUtil.matches("密码123456", first));
    }
}
