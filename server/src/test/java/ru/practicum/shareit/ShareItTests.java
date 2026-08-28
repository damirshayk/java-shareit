package ru.practicum.shareit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ShareItTests {

	@Autowired
	private UserRepository userRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void entityEqualityShouldHandleTransientIdsAndHibernateProxy() {
		User firstTransient = new User(null, "First", "first@example.com");
		User secondTransient = new User(null, "Second", "second@example.com");
		int hashCodeBeforePersisting = firstTransient.hashCode();

		assertThat(firstTransient).isNotEqualTo(secondTransient);
		firstTransient.setId(100L);
		assertThat(firstTransient.hashCode()).isEqualTo(hashCodeBeforePersisting);

		User persisted = userRepository.saveAndFlush(new User(null, "User", "user@example.com"));
		entityManager.detach(persisted);
		User proxy = entityManager.getReference(User.class, persisted.getId());

		assertThat(persisted).isEqualTo(proxy);
		assertThat(proxy).isEqualTo(persisted);
		assertThat(proxy.hashCode()).isEqualTo(persisted.hashCode());
	}

}
