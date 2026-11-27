@Reporting @TS-27
Feature: Analytics without operations
  As a platform stakeholder
  I want analytics for companies without operations to return empty KPIs
  So that dashboards show zero values instead of failing

  Scenario: A provider without orders gets empty analytics
    When a stakeholder asks for the KPIs of provider 999
    Then the analytics respond with status 200
    And the provider has 0 orders, 0 confirmed and 0 cancelled
    And the provider revenue is 0.0
    And the provider revenue is split into 0 months

  Scenario: A buyer without orders gets empty analytics
    When a stakeholder asks for the KPIs of buyer company 999
    Then the analytics respond with status 200
    And the buyer has 0 orders and spent 0.0
