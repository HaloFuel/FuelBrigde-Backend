@Reporting
Feature: Platform and Buyer Analytics
  As a platform stakeholder
  I want to see KPIs for buyers and the whole platform
  So that I can track business performance

  Scenario: Buyer analytics reflect completed and pending payments
    Given buyer company 7 has placed orders with completed and pending payments
    When a stakeholder requests analytics for buyer company 7
    Then the response status code should be 200
    And the total spent should be 100.0
    And there should be 1 completed payment and 1 pending payment

  Scenario: Platform summary reports an outage instead of misleading zero KPIs
    Given the upstream ordering service is unavailable
    When a stakeholder requests the platform summary
    Then the response status code should be 502
