package com.stocktracker.dto;

import java.util.List;

public final class NonProdAuthDtos {
  private NonProdAuthDtos() {}

  public record SocialExchangeRequest(String code, String redirectUri) {}

  public record DemoUserListItem(int slot, String label, String email) {}

  public record DemoUserSummary(int slot, String label) {}

  public record DemoUserCatalogResponse(List<DemoUserListItem> users, int maxUsers, boolean canCreate) {}

  public record DemoUserCreateRequest(String label) {}

  public record DemoUserLoginResponse(String token, UserResponse user, DemoUserSummary demoUser) {}
}

