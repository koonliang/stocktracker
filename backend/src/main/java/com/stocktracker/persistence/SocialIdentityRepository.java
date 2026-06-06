package com.stocktracker.persistence;

import com.stocktracker.domain.SocialIdentity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class SocialIdentityRepository implements PanacheRepositoryBase<SocialIdentity, Long> {
  public Optional<SocialIdentity> findByProviderSubject(
      SocialIdentity.Provider provider, String subject) {
    return find("provider = ?1 and providerSubject = ?2", provider, subject).firstResultOptional();
  }
}
