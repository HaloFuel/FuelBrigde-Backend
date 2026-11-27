@TS-14 @Ordering
Feature: Fuel Order State Guards
  As the Ordering service
  I want every state transition to be validated against the current status
  So that orders cannot move through states the business flow does not allow

  Scenario Outline: Cancelling an order from a cancellable status succeeds
    Given an existing fuel order in status "<status>"
    When a user cancels the existing order
    Then the guard response status code should be 200
    And the existing order status should be "CANCELLED"

    Examples:
      | status    |
      | PENDING   |
      | CONFIRMED |

  Scenario Outline: Cancelling an order from a non-cancellable status is rejected
    Given an existing fuel order in status "<status>"
    When a user cancels the existing order
    Then the guard response status code should be 409
    And the guard error should say "Cannot transition from <status> to CANCELLED"
    And the existing order status should be "<status>"

    Examples:
      | status          |
      | DELIVERED       |
      | CANCELLED       |
      | DISPATCHED      |
      | PENDING_PAYMENT |
      | PAID            |

  Scenario: Cancelling an unknown order returns not found
    When a user cancels order 999999
    Then the guard response status code should be 404

  Scenario: Confirming an order that is not pending is rejected
    Given an existing fuel order in status "CANCELLED"
    When a user confirms the existing order
    Then the guard response status code should be 409
    And the guard error should say "Cannot transition from CANCELLED to CONFIRMED"
    And the existing order status should be "CANCELLED"

  Scenario: Confirming a pending order succeeds
    Given an existing fuel order in status "PENDING"
    When a user confirms the existing order
    Then the guard response status code should be 200
    And the existing order status should be "CONFIRMED"

  Scenario: Marking an order as paid before it was received is rejected
    Given an existing fuel order in status "PENDING"
    When a service marks the existing order as paid
    Then the guard response status code should be 409
    And the guard error should say "Cannot transition from PENDING to PAID"
    And the existing order status should be "PENDING"
