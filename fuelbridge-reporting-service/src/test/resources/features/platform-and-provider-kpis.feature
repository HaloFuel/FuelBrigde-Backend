@Reporting @TS-27
Feature: Platform and provider KPIs
  As a platform stakeholder
  I want consolidated KPIs for the platform and for each provider
  So that I can measure how the marketplace is performing

  Scenario: Platform summary consolidates orders, deliveries and payments
    Given the platform has registered orders, deliveries and payments
    When a stakeholder asks for the platform KPIs
    Then the analytics respond with status 200
    And the platform shows 4 orders of which 1 is pending
    And the platform shows 2 deliveries of which 1 is completed
    And the platform shows 4 payments with a revenue of 350.0

  Scenario: Provider analytics count only the provider's orders and completed payments
    Given the platform has registered orders, deliveries and payments
    When a stakeholder asks for the KPIs of provider 202
    Then the analytics respond with status 200
    And the provider has 3 orders, 2 confirmed and 1 cancelled
    And the provider revenue is 350.0
    And the provider revenue is split into 2 months
