@TS-20 @Payment
Feature: Payment Refund and Failure
  As a platform operator
  I want refunds and failures to respect the payment's current status
  So that money is never refunded twice or a settled payment marked as failed

  Scenario: Refunding a completed payment succeeds
    Given a payment exists with status "COMPLETED"
    When the operator requests a refund for the payment
    Then the payment response status code should be 200
    And the payment status should be "REFUNDED"

  Scenario: Refunding a payment that is not completed is rejected
    Given a payment exists with status "PENDING"
    When the operator requests a refund for the payment
    Then the payment response status code should be 409
    And the payment status should be "PENDING"

  Scenario: Marking a pending payment as failed succeeds
    Given a payment exists with status "PENDING"
    When the operator marks the payment as failed
    Then the payment response status code should be 200
    And the payment status should be "FAILED"

  Scenario: Marking an already completed payment as failed is rejected
    Given a payment exists with status "COMPLETED"
    When the operator marks the payment as failed
    Then the payment response status code should be 409
    And the payment status should be "COMPLETED"
