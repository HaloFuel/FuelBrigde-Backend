@Catalog @TS-22
Feature: Provider rating management
  As a buyer company
  I want to review and adjust the ratings given to providers
  So that each provider's reputation reflects the latest experience

  Scenario: List the ratings received by a provider
    Given buyer company 101 has rated provider 202 with 5
    And buyer company 102 has rated provider 202 with 3
    And buyer company 101 has rated provider 203 with 4
    When I list the ratings of provider 202
    Then the catalog responds with status 200
    And the listed ratings come from buyer companies "101, 102"

  Scenario: List the ratings given by a buyer company
    Given buyer company 101 has rated provider 202 with 5
    And buyer company 101 has rated provider 203 with 4
    And buyer company 102 has rated provider 202 with 3
    When I list the ratings given by buyer company 101
    Then the catalog responds with status 200
    And the listed ratings are for providers "202, 203"

  Scenario: A buyer updates its rating of a provider
    Given buyer company 101 has rated provider 202 with 2
    When buyer company 101 changes its rating of provider 202 to 4
    Then the catalog responds with status 200
    And the returned rating is 4

  Scenario: Updating a rating that does not exist is rejected
    When buyer company 101 updates the rating with id 999999 for provider 202 to 4
    Then the catalog responds with status 404

  Scenario: A rating cannot be moved to another provider
    Given buyer company 101 has rated provider 202 with 5
    When buyer company 101 tries to move its rating of provider 202 to provider 203
    Then the catalog responds with status 400
