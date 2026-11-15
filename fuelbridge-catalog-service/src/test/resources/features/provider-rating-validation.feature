@Catalog @TS-22
Feature: Provider rating validation
  As the FuelBridge platform
  I want to reject invalid provider ratings
  So that reputations are only built from valid scores of real companies

  Scenario Outline: A score outside the 1 to 5 range is rejected
    When buyer company 101 submits a rating of <score> for provider 202
    Then the catalog responds with status 400
    And no rating is stored for provider 202

    Examples:
      | score |
      | 0     |
      | 6     |
      | -1    |

  Scenario: A score outside the range is rejected when updating
    Given buyer company 101 has rated provider 202 with 3
    When buyer company 101 changes its rating of provider 202 to 9
    Then the catalog responds with status 400

  Scenario: Rating a provider unknown to IAM is rejected
    When buyer company 101 submits a rating of 5 for provider 99
    Then the catalog responds with status 400
    And no rating is stored for provider 99
