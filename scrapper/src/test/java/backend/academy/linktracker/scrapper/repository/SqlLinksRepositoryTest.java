package backend.academy.linktracker.scrapper.repository;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test-sql"})
public class SqlLinksRepositoryTest extends BaseLinksRepositoryTest {}
